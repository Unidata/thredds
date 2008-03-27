package thredds.server.controller;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogController extends AbstractController
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( CatalogController.class );

  protected ModelAndView handleRequestInternal( HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse ) throws Exception
  {
    return null;
  }
}
