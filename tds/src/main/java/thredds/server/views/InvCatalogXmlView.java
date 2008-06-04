package thredds.server.views;

import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.io.OutputStream;

import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class InvCatalogXmlView extends AbstractView
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( InvCatalogXmlView.class );

  protected void renderMergedOutputModel( Map model, HttpServletRequest req, HttpServletResponse res )
          throws Exception
  {
    if ( model == null || model.isEmpty() )
      throw new IllegalArgumentException( "Model must not be null or empty.");
    if ( ! model.containsKey( "catalog" ))
      throw new IllegalArgumentException( "Model must contain \"catalog\" key.");
    Object o = model.get("catalog");
    if ( ! ( o instanceof InvCatalogImpl ) )
      throw new IllegalArgumentException( "Model must contain an InvCatalogImpl object.");
    InvCatalogImpl cat = (InvCatalogImpl) o;

    res.setContentType( "application/xml" );
    res.setCharacterEncoding( "UTF-8" );
    OutputStream os = null;
    try
    {
      os = res.getOutputStream();
      // Return catalog as XML response.
      InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( false );
      catFactory.writeXML( cat, os );
    }
    finally
    {
      if ( os != null )
        os.close();
    }
  }
}