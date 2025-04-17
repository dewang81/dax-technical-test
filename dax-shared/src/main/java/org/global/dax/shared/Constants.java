package org.global.dax.shared;

public class Constants {
    public static final int PORT = 9090;

    public static final int NUM_SHARDS = 8;
    public static final int MAX_THREADS = 10;
    public static final int MAX_RETRIES = 3;

    public static final int MAX_WRITE_QUEUE_SIZE = 100;
    public static final int BUFFER_SIZE = 1024;
    public static final int MAX_KEY_SIZE = 4;
    public static final int MAX_VALUE_SIZE = 2096;

    public static final int READ_TIMEOUT_MS = 1000;
    public static final long TIMEOUT_MS = 1000L;
    public static final long WAKEUP_TIMEOUT_MS = 500L;

    public static final String HOST = "127.0.0.1";
}
