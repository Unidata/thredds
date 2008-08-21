package thredds.server.wcs;

import ucar.nc2.util.DiskCache2;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

import thredds.server.wcs.Version;

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

  /**
   * Set whether generated files are deleted immediately after sent (true) or handled by cache (false).
   *
   * Note: currently (2008-03-05), each request generates a unique file. So caching doesn't make much sense. 
   *
   * @param deleteImmediately if true, delete immediately, otherwise allow cache to handle.   
   * @return this VersionHandler
   */
  public VersionHandler setDeleteImmediately( boolean deleteImmediately);

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
