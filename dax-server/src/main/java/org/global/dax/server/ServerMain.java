package org.global.dax.server;

import org.global.dax.shared.ProtocolUtils;
import org.global.dax.shared.ShardedCache;
import org.global.dax.shared.SharedClassExample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.global.dax.shared.Constants.*;

public final class ServerMain {
    private static final Logger LOG = LoggerFactory.getLogger(ServerMain.class);

    //a wrapper for client state
    static class ClientContext {
        private final ByteBuffer readBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        private final Queue<ByteBuffer> writeQueue = new LinkedList<>();

        public ByteBuffer getReadBuffer() {
            return readBuffer;
        }

        public Queue<ByteBuffer> getWriteQueue() {
            return writeQueue;
        }

        public void addWriteByteBuffer(ByteBuffer byteBuffer) {
            //check for write buffer size to avoid unbounded memory usage
            if (writeQueue.size() >= MAX_WRITE_QUEUE_SIZE) {
                throw new IllegalStateException("Write queue exceeded capacity");
            }
            writeQueue.add(byteBuffer);
        }
    }

    private final ShardedCache cache = new ShardedCache();
    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);

    public static void main(String[] args) {
        LOG.info("{} {}!", message(), SharedClassExample.sharedString());

        try {
            new ServerMain().startServer();
        } catch (IOException e) {
            LOG.error("Failed to start the NIO Server on PORT: {} due to {}", PORT, e.getMessage(), e);
        }
    }

    public static String message() {
        return "hello, this is the server";
    }

    public void startServer() throws IOException {
        //Non-blocking I/O using selector and channels
        try (Selector selector = Selector.open();
             ServerSocketChannel serverChannel = ServerSocketChannel.open()) {

            serverChannel.bind(new InetSocketAddress(PORT));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            LOG.info("NIO Server started on port: {}", PORT);

            //keep on listening continuously for the messages from the clients
            //noinspection InfiniteLoopStatement
            while (true) {
                //support multiple concurrent clients over a single thread with the NIO multiplexing approach
                // Selector wakeup + prevents CPU spinning due to spurious wakeup and is safer in real-world systems.
                if (selector.select(WAKEUP_TIMEOUT_MS) == 0) {
                    continue;
                }

                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    SelectableChannel channelObj = key.channel();

                    try {
                        if (!key.isValid()) {
                            continue;
                        }

                        if (key.isAcceptable()) { //handles OP_ACCEPT without blocking
                            if (channelObj instanceof ServerSocketChannel channel) {
                                handleAccept(selector, channel);
                            } else {
                                throw new IOException("Unknown channel [" + channelObj.getClass() + "]");
                            }
                        } else if (key.isReadable()) { //handles OP_READ without blocking
                            if (channelObj instanceof SocketChannel client && key.attachment() instanceof ClientContext ctx) {
                                handleRead(key, selector, client, ctx);
                            } else {
                                throw new IOException("Unknown channel [" + channelObj.getClass() + "]");
                            }
                        } else if (key.isWritable()) { //handles OP_WRITE without blocking
                            if (channelObj instanceof SocketChannel client && key.attachment() instanceof ClientContext ctx) {
                                handleWrite(key, client, ctx);
                            } else {
                                throw new IOException("Unknown channel [" + channelObj.getClass() + "]");
                            }
                        }
                    } catch (CancelledKeyException | ClosedChannelException e) {
                        LOG.warn("Channel closed unexpectedly: {}", e.getMessage());
                        key.cancel();  // Only cancel on error
                    } catch (IOException e) {
                        LOG.error("I/O error: {}", e.getMessage(), e);
                        if (channelObj instanceof SocketChannel client) {
                            closeChannel(client);
                        }
                        key.cancel();  // Only cancel on error
                    }
                }
            }
        }
    }

    private void handleAccept(Selector selector,
                              ServerSocketChannel channel) throws IOException {
        SocketChannel client = channel.accept();
        client.configureBlocking(false);

        SelectionKey key = client.register(selector, SelectionKey.OP_READ);
        key.attach(new ClientContext());
        LOG.info("Accepted connection from the client [{}]", client.getRemoteAddress());
    }

    private void handleRead(SelectionKey key,
                            Selector selector,
                            SocketChannel client,
                            ClientContext ctx) throws IOException {
        String clientAddress = client.getRemoteAddress().toString();

        ByteBuffer buffer = ctx.getReadBuffer();
        buffer.clear();

        int bytesRead = client.read(buffer);
        if (bytesRead == -1) {
            LOG.info("Disconnecting the client [{}]", clientAddress);
            closeChannel(client);
            key.cancel();
            return;
        }

        buffer.flip();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        String input = new String(data).trim();
        LOG.info("Received the command [{}] from the client [{}]", input, clientAddress);

        //offload slower/expensive logic as an async logic to a worker thread pool, when the response is ready,
        //we enqueue it and wake up the selector to register interest in writing
        executor.submit(() -> {
            try {
                String response = ProtocolUtils.handleClientRequest(input, cache);
                ByteBuffer responseBuffer = ByteBuffer.wrap((response + "\n").getBytes());
                LOG.info("Sending the response [{}] to the client [{}]", response, clientAddress);

                synchronized (ctx) {
                    ctx.addWriteByteBuffer(responseBuffer);
                }

                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                selector.wakeup(); // required if you're modifying key from a different thread
            } catch (IllegalStateException e) {
                LOG.warn("Backpressure for [{}]: {}", clientAddress, e.getMessage());
                try {
                    client.close();
                } catch (IOException ex) {
                    LOG.error("Error closing client [{}]: {}", clientAddress, ex.getMessage());
                }
                key.cancel();
            } catch (Exception e) {
                LOG.error("Worker thread error: {}", e.getMessage(), e);
            }
        });
    }

    private void handleWrite(SelectionKey key,
                             SocketChannel client,
                             ClientContext ctx) throws IOException {
        Queue<ByteBuffer> queue = ctx.getWriteQueue();

        while (!queue.isEmpty()) {
            ByteBuffer buf = queue.peek();
            client.write(buf);
            if (buf.hasRemaining()) {
                //stop for partial writing
                break;
            }
            queue.poll(); // Done with this buffer
        }

        if (queue.isEmpty()) {
            // Nothing more to write, remove OP_WRITE interest
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    private void closeChannel(SocketChannel client) {
        try {
            LOG.info("Closing connection to client [{}]", client.getRemoteAddress());
            client.close();
        } catch (IOException ignored) {}
    }
}
