package thredds.server.views;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.io.OutputStream;
import java.io.File;

import thredds.servlet.ServletUtil;

/**
 *  Render the response to a request for a local file including byte range requests.
 *
 * @author edavis
 * @since 4.0
 */
public class FileView extends AbstractView
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( FileView.class );

  protected void renderMergedOutputModel( Map model, HttpServletRequest req, HttpServletResponse res )
          throws Exception
  {
    if ( model == null || model.isEmpty() )
      throw new IllegalArgumentException( "Model must not be null or empty." );
    if ( ! model.containsKey( "file" ) )
      throw new IllegalArgumentException( "Model must contain \"file\" key." );
    Object o = model.get( "file" );
    if ( ! ( o instanceof File ) )
      throw new IllegalArgumentException( "Object mapped by \"file\" key  must be a File." );
    File file = (File) o;

    // Check that file exists and is not a directory.
    if ( ! file.isFile() )
    {
      // ToDo Send error or throw exception to be handled by Spring exception handling stuff.
//      throw new IllegalArgumentException( "File must exist and not be a directory." );
      ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, 0 );
      res.sendError( HttpServletResponse.SC_BAD_REQUEST );
      return;
    }

    // Check if content type is specified.
    String contentType = null;
    if ( model.containsKey( "contentType"))
    {
      o = model.get( "contentType");
      if ( o instanceof String )
        contentType = (String) o;
    }

    // Set the type of the file
    String filename = file.getPath();
    if ( null == contentType )
    {
      if ( filename.endsWith( ".html" ) )
        contentType = "text/html; charset=utf-8";
      else if ( filename.endsWith( ".xml" ) )
        contentType = "application/xml; charset=utf-8";
      else if ( filename.endsWith( ".txt" ) || ( filename.endsWith( ".log" ) ) )
        contentType = "text/plain; charset=utf-8";
      else if ( filename.indexOf( ".log." ) > 0 )
        contentType = "text/plain; charset=utf-8";
      else if ( filename.endsWith( ".nc" ) )
        contentType = "application/x-netcdf";
      else
        contentType = this.getServletContext().getMimeType( filename );

      if ( contentType == null )
        contentType = "application/octet-stream";
    }

    res.setContentType( "application/xml" );
    res.setCharacterEncoding( "UTF-8" );
    OutputStream os = null;
    try
    {
      os = res.getOutputStream();
      // Return catalog as XML response.
//      InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( false );
//      catFactory.writeXML( cat, os );
    }
    finally
    {
      if ( os != null )
        os.close();
    }
  }
}