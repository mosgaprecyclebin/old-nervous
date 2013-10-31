package ch.ethz.nervous;

public final class TracePath {
    static {
        System.loadLibrary("tracepath");
    }

    /**
     * TracePath to the specified host and port via UDP.
     * @return a new-line separated path of the form ttl:ip-address
     */
    public native String getPath(String host, int port);
}
