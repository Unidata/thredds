package thredds.server.wcs;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Each implementation handles a different version of WCS requests. 
 *
 * @author edavis
 * @since Aug 14, 2007 2:10:12 PM
 */
public interface VersionHandler
{
  public Version getVersion();
  
  public void handleKVP( HttpServlet servlet,
                         HttpServletRequest req,
                         HttpServletResponse res )
          throws ServletException, IOException;

  public void makeServiceException( HttpServletResponse res,
                                    String code, String locator,
                                    String message )
          throws IOException;

  public void makeServiceException( HttpServletResponse res,
                                    String code, String locator,
                                    Throwable t )
          throws IOException;

}
