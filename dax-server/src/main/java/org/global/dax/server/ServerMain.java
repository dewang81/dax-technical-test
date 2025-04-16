package org.global.dax.server;

import org.global.dax.shared.Cache;
import org.global.dax.shared.ProtocolHandler;
import org.global.dax.shared.SharedClassExample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.global.dax.shared.Constants.MAX_THREADS;
import static org.global.dax.shared.Constants.PORT;

public final class ServerMain {
    private static final Logger LOG = LoggerFactory.getLogger(ServerMain.class);

    private final Cache cache = new Cache();
    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);

    private ServerSocket serverSocket;

    public static void main(String[] args) {
        LOG.info("{} {}!", message(), SharedClassExample.sharedString());

        new ServerMain().startServer();
    }

    public static String message() {
        return "hello, this is the server";
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            LOG.info("Server started listening on the port: {}", PORT);

            //keep listening continuously for messages from client
            //noinspection InfiniteLoopStatement
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            LOG.error("Failed to start the server at port: {} due to {}", PORT, e.getMessage());
        } finally {
            stopServer();
        }
    }

    private void stopServer() {
        LOG.info("Stopping the server to listen at port: {}", PORT);
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        LOG.info("Server is stopped to listen at port: {}", PORT);
    }

    protected void handleClient(Socket clientSocket) {
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                LOG.info("Command received from the client: {}", inputLine);
                String response = ProtocolHandler.handleClientRequest(inputLine, cache);
                LOG.info("Sending response to the client: {}", response);
                out.write(response + "\n");
                out.flush();
            }
        } catch (Exception e) {
            LOG.error("Unable to read the command from the client due to {}", e.getMessage());
        }
    }
}
