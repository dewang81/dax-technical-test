package org.global.dax.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import org.global.dax.shared.Cache;
import org.global.dax.shared.ProtocolHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.net.Socket;

@ExtendWith(MockitoExtension.class)
public class ServerMainTest {
    private ByteArrayInputStream inputStream;
    private ByteArrayOutputStream outputStream;

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

    @BeforeEach
    void setup() {
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldSupportMessageExampleTest() {
        final var result = ServerMain.message();
        assertThat(result).isNotBlank();
    }

    @Test
    void shouldHandleClientCommandSuccessfully() {
        // Given a simple command (GET abcd) and mocked response from ProtocolHandler
        String command = "GET abcd\n";
        String expectedResponse = "VALUE someValue";

        inputStream = new ByteArrayInputStream(command.getBytes());
        TestClientSocket clientSocket = new TestClientSocket(inputStream, outputStream);

        // Use a spy so we can override cache
        try (MockedStatic<ProtocolHandler> mockedHandler = mockStatic(ProtocolHandler.class)) {
            mockedHandler.when(() -> ProtocolHandler.handleClientRequest(eq("GET abcd"), any(Cache.class)))
                    .thenReturn(expectedResponse);

            // When
            var serverMain = new ServerMain();
            serverMain.handleClient(clientSocket);

            // Then
            String written = outputStream.toString().trim();
            assertThat(written).isEqualTo(expectedResponse);
        }
    }

    @Test
    void shouldHandleEmptyInputGracefully() {
        inputStream = new ByteArrayInputStream(new byte[0]);
        TestClientSocket clientSocket = new TestClientSocket(inputStream, outputStream);

        // Should not throw
        var serverMain = new ServerMain();
        serverMain.handleClient(clientSocket);

        assertThat(outputStream.toString()).isBlank();
    }
}
