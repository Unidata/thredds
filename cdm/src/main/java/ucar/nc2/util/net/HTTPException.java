package ucar.nc2.util.net;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: dmh
 * Date: May 20, 2010
 * Time: 12:04:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class HTTPException extends IOException {

    public HTTPException() {
        super();
    }

    public HTTPException(java.lang.String message) {
        super(message);
    }

    public HTTPException(java.lang.String message, java.lang.Throwable cause) {
        super(message, cause);
    }

    public HTTPException(java.lang.Throwable cause) {
        super(cause);
    }
}
