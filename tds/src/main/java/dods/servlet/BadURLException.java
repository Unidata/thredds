package dods.servlet;


import dods.dap.DODSException;
/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jul 25, 2004
 * Time: 2:12:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class BadURLException extends  DODSException {
   public BadURLException(String msg) {
        super(msg);
    }
    public BadURLException() {
        super("");
    }
}
