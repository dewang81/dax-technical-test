package org.global.dax.client;

import org.global.dax.shared.SharedClassExample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;

import static org.global.dax.shared.Constants.*;

public final class ClientMain {
    private static final Logger LOG = LoggerFactory.getLogger(ClientMain.class);

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public ClientMain() {
    }

    //for Testing purposes
    public ClientMain(Socket clientSocket,
                      PrintWriter writer,
                      BufferedReader reader) {
        this.clientSocket = clientSocket;
        this.out = writer;
        this.in = reader;
    }

    public static void main(String[] args) {
        LOG.info("{} {}!", message(), SharedClassExample.sharedString());

        new ClientMain().startClient();
    }

    public static String message() {
        return "hello, this is the client";
    }

    private void startClient() {
        int attempt = 0;
        while (!startConnection(attempt)) {
            //simple retry with exponential backoff
            if (++attempt >= MAX_RETRIES) {
                LOG.error("Unable to connect to the server after {} attempts", MAX_RETRIES);
                return;
            }
            retryConnection(attempt);
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (!scanner.hasNextLine()) {
                break;
            }

            String command = scanner.nextLine();
            if (command.equalsIgnoreCase("exit")) {
                break;
            }

            sendCommand(command);
            LOG.info("Response received [{}] from server [{}:{}]", getServerResponse(), HOST, PORT);
        }
    }

    private boolean startConnection(int attempt) {
        try {
            LOG.info("Client attempting [{}] to connect to the server [{}:{}]", attempt, HOST, PORT);
            clientSocket = new Socket(HOST, PORT);
            clientSocket.setSoTimeout(READ_TIMEOUT_MS);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            LOG.info("Client is connected to the server [{}:{}]", HOST, PORT);
            return true;
        } catch (IOException e) {
            LOG.info("Client failed to connected to the server [{}:{}] for attempt {} due to {}", HOST, PORT, attempt, e.getMessage());
            stopConnection();
            return false;
        }
    }

    public void stopConnection() {
        LOG.info("Disconnecting the client from the server [{}:{}]", HOST, PORT);
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }

        if (out != null) {
            out.close();
        }

        if (clientSocket != null) {
            try {
                clientSocket.close();
            } catch (IOException ignored) {
            }
        }
        LOG.info("Client is disconnected from the server [{}:{}]", HOST, PORT);
    }

    private void retryConnection(int attempt) {
        try {
            long timeout = TIMEOUT_MS * attempt;
            LOG.info("Retrying the connection after {} ms", timeout);
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendCommand(String command) {
        if (out != null) {
            LOG.info("Sending command [{}] to the server [{}:{}]", command, HOST, PORT);
            out.write(command + "\n");
            out.flush();
        }
    }

    private String getServerResponse() {
        if (in != null) {
            try {
                return in.readLine();
            } catch (SocketTimeoutException e) {
                LOG.error("Client timed out while waiting for the response from server due to {}", e.getMessage());
            }
            catch (IOException e) {
                LOG.error("Exception occurred when receiving the message from server due to {}", e.getMessage());
            }
        }
        return "";
    }
}
