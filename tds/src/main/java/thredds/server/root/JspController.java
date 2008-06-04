package thredds.server.root;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import thredds.server.config.TdsContext;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class JspController extends AbstractController
{
//  private static org.slf4j.Logger log =
//          org.slf4j.LoggerFactory.getLogger( JspController.class );

  private TdsContext tdsContext;

  public void setTdsContext( TdsContext tdsContext )
  {
    this.tdsContext = tdsContext;
  }

  protected ModelAndView handleRequestInternal( HttpServletRequest req, HttpServletResponse res )
          throws Exception
  {
    tdsContext.getJspRequestDispatcher().forward( req, res);
    return null;
  }
}