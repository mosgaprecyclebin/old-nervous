package ch.ethz.nervous;

public final class TracePath {
    static {
        System.loadLibrary("tracepath");
    }

    public native String getPath();
}
