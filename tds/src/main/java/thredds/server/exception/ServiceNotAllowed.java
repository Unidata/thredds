/* Copyright */
package thredds.server.exception;

/**
 * Service is disAllowed
 *
 * @author caron
 * @since 5/1/2015
 */
public class ServiceNotAllowed extends RuntimeException {
  public ServiceNotAllowed(String message) {
    super(message);
  }
}
