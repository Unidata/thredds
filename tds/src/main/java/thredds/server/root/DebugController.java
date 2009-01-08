package thredds.server.root;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import thredds.servlet.DebugHandler;
import thredds.server.config.TdsContext;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DebugController extends AbstractController
{
  private TdsContext tdsContext;

  public void setTdsContext( TdsContext tdsContext )
  {
    this.tdsContext = tdsContext;
  }

  protected ModelAndView handleRequestInternal( HttpServletRequest req, HttpServletResponse res )
          throws Exception
  {
    String path = req.getServletPath();
    if ( path == null || path.equals( "" ) )
      path = req.getPathInfo();

    if ( path.equals( "/debug" ) || path.equals( "/debug/" ) )
    {
      DebugHandler.doDebug( null, req, res );
    }
    return null; // ToDo 
  }
}