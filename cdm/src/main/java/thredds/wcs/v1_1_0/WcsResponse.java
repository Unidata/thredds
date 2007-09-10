package thredds.wcs.v1_1_0;

import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public abstract class WcsResponse
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( WcsResponse.class );

  protected static final Namespace wcsNS = Namespace.getNamespace( "http://www.opengis.net/wcs/1.1" );
  protected static final Namespace owcsNS = Namespace.getNamespace( "http://www.opengis.net/wcs/1.1/ows" );
  protected static final Namespace owsNS = Namespace.getNamespace( "http://www.opengis.net/ows" );

  private XMLOutputter xmlOutputter;

  public WcsResponse()
  {
    xmlOutputter = new XMLOutputter( Format.getPrettyFormat() );
  }
}
