package compressedtexture.dktxtools.ktx;

import java.io.IOException;

public class KTXFormatException extends IOException {

    private static final long serialVersionUID = 1L;

    public KTXFormatException(String message) {
        this(message, null);
    }

    public KTXFormatException(String message, Throwable t) {
        super(message, t);
    }

}
