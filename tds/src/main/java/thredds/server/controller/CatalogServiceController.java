package thredds.server.controller;

import org.springframework.web.servlet.mvc.AbstractCommandController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.validation.BindException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.NumberFormat;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogServiceController extends AbstractCommandController
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( CatalogServiceController.class );

  public CatalogServiceController() {}

  @Override
  protected void initBinder( HttpServletRequest request, ServletRequestDataBinder binder )
          throws Exception
  {
    super.initBinder( request, binder );
    NumberFormat nf = NumberFormat.getInstance( request.getLocale() );
    binder.registerCustomEditor( java.lang.Integer.class,
                                 new CustomNumberEditor( java.lang.Integer.class, nf, true ) );
  }

  protected ModelAndView handle( HttpServletRequest request, HttpServletResponse response,
                                 Object command, BindException errors )
          throws Exception
  {
    return null;
  }
}
