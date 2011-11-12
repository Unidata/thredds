package opendap.dap;
import ucar.nc2.util.net.LogStream;

import java.io.IOException;

public class ConstraintException extends RuntimeException {

    public ConstraintException() {
        super();
    }

    public ConstraintException(java.lang.String message) {
        super(message);
    }

    public ConstraintException(java.lang.String message, java.lang.Throwable cause) {
        super(message, cause);
    }

    public ConstraintException(java.lang.Throwable cause) {
        super(cause);
    }
}
