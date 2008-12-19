package thredds.server.controller;

import org.springframework.web.servlet.mvc.AbstractCommandController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.validation.BindException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.NumberFormat;
import java.io.PrintWriter;
import java.util.Map;
import java.util.HashMap;

import thredds.server.config.TdsContext;

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

  private TdsContext tdsContext;

  public CatalogServiceController() {}
  
  public void setTdsContext( TdsContext tdsContext) { this.tdsContext = tdsContext; }
  public TdsContext getTdsContext() { return this.tdsContext; }

//  @Override
//  protected void initBinder( HttpServletRequest request, ServletRequestDataBinder binder )
//          throws Exception
//  {
//    super.initBinder( request, binder );
//    NumberFormat nf = NumberFormat.getInstance( request.getLocale() );
//    binder.registerCustomEditor( java.lang.Integer.class,
//                                 new CustomNumberEditor( java.lang.Integer.class, nf, true ) );
//  }
//
  protected ModelAndView handle( HttpServletRequest request, HttpServletResponse response,
                                 Object command, BindException errors )
          throws Exception
  {
    CatalogServiceRequest csr = (CatalogServiceRequest) command;
    Map model = new HashMap();
    model.put( "path", request.getPathInfo());
    model.put( "catalog", csr.getCatalog());
    model.put( "dataset", csr.getDataset());
    model.put( "command", csr.getCommand());
    model.put( "view", csr.getView());
    model.put( "debug", csr.isDebug());

    return new ModelAndView( "catServiceReq", model);
  }
}
