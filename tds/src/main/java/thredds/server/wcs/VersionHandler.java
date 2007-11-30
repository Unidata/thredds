package thredds.server.wcs;

import ucar.nc2.util.DiskCache2;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Each implementation handles a different version of WCS requests. 
 *
 * @author edavis
 * @since 4.0
 */
public interface VersionHandler
{
  public Version getVersion();

  public VersionHandler setDiskCache( DiskCache2 diskCache );

  public void handleKVP( HttpServlet servlet,
                         HttpServletRequest req,
                         HttpServletResponse res )
          throws ServletException, IOException;

  public void handleExceptionReport( HttpServletResponse res,
                                     String code, String locator,
                                     String message )
          throws IOException;

  public void handleExceptionReport( HttpServletResponse res,
                                     String code, String locator,
                                     Throwable t )
          throws IOException;

}
