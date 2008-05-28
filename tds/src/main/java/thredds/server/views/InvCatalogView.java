package thredds.server.views;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.io.PrintWriter;
import java.io.OutputStream;

import thredds.catalog.InvCatalog;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.servlet.ServletUtil;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class InvCatalogView extends AbstractView
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( InvCatalogView.class );

  protected void renderMergedOutputModel( Map model, HttpServletRequest req, HttpServletResponse res )
          throws Exception
  {
    if ( model == null || model.isEmpty() || model.size() > 1 )
      throw new IllegalArgumentException( "Model must not be null and must contain only one entry.");
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