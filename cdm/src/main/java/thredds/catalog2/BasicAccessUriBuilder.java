package thredds.catalog2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class BasicAccessUriBuilder implements AccessUriBuilder
{
  private Logger log = LoggerFactory.getLogger( getClass());

  @Override
  public URI buildAccessUri( Access access, URI docBaseUri )
  {
    if ( access == null )
      throw new IllegalArgumentException( "Access must not be null.");

    // Determine the base URI of the service.
    URI baseServiceUri = access.getService().getBaseUri();
    if ( ! baseServiceUri.isAbsolute())
    {
      if ( docBaseUri == null )
        throw new IllegalStateException( "Document base URI must not be null if service base URI is not absolute.");
      else
        baseServiceUri = docBaseUri.resolve( baseServiceUri );
    }

    // Build access URI using string concatenation of
    //    service.base + access.urlPath + service.suffix
    // [From "Constructing URLs" section of InvCatalog spec:
    //    http://www.unidata.ucar.edu/projects/THREDDS/tech/catalog/v1.0.2/InvCatalogSpec.html#constructingURLs ]
    StringBuilder sb = new StringBuilder( baseServiceUri.toString());
    sb.append( access.getUriPath() );
    String suffix = access.getService().getSuffix();
    if ( suffix != null && (! suffix.equals( "" )))
      sb.append( suffix );

    try
    {
      return new URI( sb.toString());
    }
    catch ( URISyntaxException e )
    {
      log.error( "buildAccessUri(): URI syntax exception [" + sb.toString() + "].", e );
      return null;
    }
  }
}
