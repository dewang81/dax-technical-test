package org.global.dax.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

@ExtendWith(MockitoExtension.class)
public class ServerMainTest {
    private ServerMain server;
    private ServerMain.ClientContext clientContext;
    private SocketChannel mockChannel;
    private SelectionKey mockKey;

    @BeforeEach
    void setup() {
        server = new ServerMain();
        clientContext = new ServerMain.ClientContext();
        mockChannel = mock(SocketChannel.class);
        mockKey = mock(SelectionKey.class);
    }

    @Test
    void shouldSupportMessageExampleTest() {
        final var result = ServerMain.message();
        assertThat(result).isNotBlank();
        assertEquals("hello, this is the server", result);
    }

    @Test
    void testHandleReadAndWriteIntegration() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException, InterruptedException {
        String command = "ADD key1 value1\n";
        String expectedResponse = "OK\n";

        // Simulate SocketChannel.read() writing into the buffer
        when(mockChannel.read(any(ByteBuffer.class))).thenAnswer(invocation -> {
            ByteBuffer buf = invocation.getArgument(0);
            buf.put(command.getBytes());
            return command.length();
        });
        when(mockChannel.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 12345));

        // Simulate handleRead
        Method handleReadMethod = server.getClass()
                .getDeclaredMethod(
                        "handleRead",
                        SelectionKey.class,
                        Selector.class,
                        SocketChannel.class,
                        ServerMain.ClientContext.class
                );
        handleReadMethod.setAccessible(true);
        handleReadMethod.invoke(server, mockKey, mockKey.selector(), mockChannel, clientContext);

        ByteBuffer responseBuf = null;
        long timeout = System.currentTimeMillis() + 1000; // wait up to 1 second

        while (System.currentTimeMillis() < timeout) {
            responseBuf = clientContext.getWriteQueue().peek();
            if (responseBuf != null) break;
            Thread.sleep(10); // brief wait before retrying
        }

        assertNotNull(responseBuf);
        byte[] bytes = new byte[responseBuf.remaining()];
        responseBuf.get(bytes);
        assertEquals(expectedResponse.trim(), new String(bytes).trim());
    }

    @Test
    void testHandleWriteComplete() throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap("test\n".getBytes());
        clientContext.addWriteByteBuffer(buffer);

        when(mockChannel.write(any(ByteBuffer.class))).thenAnswer(invocation -> {
            ByteBuffer buf = invocation.getArgument(0);
            buf.position(buf.limit()); // simulate full write
            return buf.limit();
        });

        Method handleWriteMethod = server.getClass()
                .getDeclaredMethod(
                        "handleWrite",
                        SelectionKey.class,
                        SocketChannel.class,
                        ServerMain.ClientContext.class
                );
        handleWriteMethod.setAccessible(true);
        handleWriteMethod.invoke(server, mockKey, mockChannel, clientContext);

        assertTrue(clientContext.getWriteQueue().isEmpty());
    }
}
