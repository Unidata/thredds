package opendap.dap.parser;

import java.io.IOException;

public class ParseException extends IOException {

    public ParseException() {
        super();
    }

    public ParseException(java.lang.String message) {
        super(message);
    }

    public ParseException(java.lang.String message, java.lang.Throwable cause) {
        super(message, cause);
    }

    public ParseException(java.lang.Throwable cause) {
        super(cause);
    }
}
