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
  public URI buildAccessUri( Access access )
  {
    if ( access == null )
      throw new IllegalArgumentException( "Access must not be null.");
    URI baseUri = access.getService().getFullyResolvedBaseUri();
    StringBuilder sb = new StringBuilder( baseUri.toString());
    sb.append( access.getUriPath() );
    String suffix = access.getService().getSuffix();
    if ( suffix != null && (! suffix.equals( "" )))
      sb.append( suffix );
    URI result = null;
    try
    {
      result = new URI( sb.toString());
      return result;
    }
    catch ( URISyntaxException e )
    {
      log.error( "buildAccessUri(): URI syntax exception [" + sb.toString() + "].", e );
      return null;
    }
  }
}
