package thredds.util;

import javax.servlet.http.HttpServletRequest;

/**
 * Utilities for extracting path information from request.
 *
 * @author edavis
 * @since 4.0
 */
public class TdsPathUtils
{
  private TdsPathUtils() {}
  
  public static String extractPath( HttpServletRequest req )
  {
    // For "/<servletPath>/*" style servlet mappings.
    String catPath = req.getPathInfo();
    if ( catPath == null )
      // For "*.xml" style servlet mappings.
      catPath = req.getServletPath();
    if ( catPath.equals( "" ) )
      return null;

    if ( catPath.startsWith( "/" ) )
      catPath = catPath.substring( 1 );

    return catPath;
  }
}
