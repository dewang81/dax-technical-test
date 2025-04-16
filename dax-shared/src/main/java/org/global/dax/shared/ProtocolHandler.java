package org.global.dax.shared;

import static org.global.dax.shared.Constants.MAX_KEY_SIZE;
import static org.global.dax.shared.Constants.MAX_VALUE_SIZE;

public class ProtocolHandler {
    public static String handleClientRequest(String input, Cache cache) {
        try {
            String[] parts = input.trim().split(" ");
            String command = parts[0].toUpperCase();

            return switch (command) {
                case "ADD" -> {
                    if (parts.length != 3) {
                        yield "ERROR Invalid ADD format";
                    }
                    else if (parts[1].trim().getBytes().length > MAX_KEY_SIZE) {
                        yield "ERROR key too large";
                    }
                    else if (parts[2].trim().getBytes().length > MAX_VALUE_SIZE) {
                        yield "ERROR value too large";
                    }

                    cache.put(parts[1].trim(), parts[2].trim());
                    yield "OK";
                }
                case "GET" -> {
                    if (parts.length != 2) {
                        yield "ERROR Invalid GET format";
                    }

                    if (parts[1].trim().equalsIgnoreCase("ALL")) {
                        yield String.join(",", cache.getAllKeys());
                    }
                    else {
                        String value = cache.get(parts[1]);
                        yield value != null ? value : "";
                    }
                }
                case "DELETE" -> {
                    if (parts.length != 2) {
                        yield "ERROR Invalid DELETE format";
                    }

                    yield cache.remove(parts[1].trim()) ? "OK" : "ERROR Invalid key";
                }
                case "HEARTBEAT" -> "OK";
                default -> "ERROR Unknown command";
            };
        } catch (Exception e) {
            return "ERROR Invalid input [" + input + "]";
        }
    }
}
