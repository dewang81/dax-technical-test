package org.global.dax.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.net.Socket;

@ExtendWith(MockitoExtension.class)
class ClientMainTest {
    private static class TestClientSocket extends Socket {
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public TestClientSocket(InputStream inputStream, OutputStream outputStream) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }

        @Override
        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public OutputStream getOutputStream() {
            return outputStream;
        }
    }

    @Test
    void shouldSupportMessageExampleTest() {
        final var result = ClientMain.message();
        assertThat(result).isNotBlank();
    }

    @Test
    void shouldSendCommandAndReceiveResponseUsingTestClientSocket() {
        String value = "test123";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(value.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        TestClientSocket clientSocket = new TestClientSocket(inputStream, outputStream);

        ClientMain client = new ClientMain(
                clientSocket,
                new PrintWriter(new OutputStreamWriter(outputStream)),
                new BufferedReader(new InputStreamReader(inputStream))
        );

        client.sendCommand("GET abcd");
        String response = client.getServerResponse();
        assertEquals(value, response);
    }
}
