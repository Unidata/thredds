package thredds.catalog2.simpleImpl;

import thredds.catalog2.Property;

import java.net.URI;
import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ExternalRefMetadataImpl extends AbstractMetadataImpl
{
  private org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( ExternalRefMetadataImpl.class );

  private String title;
  private URI externalRef;

  public ExternalRefMetadataImpl( String title, URI externalRef )
  {
    if ( title == null ) throw new IllegalArgumentException( "Title may not be null.");
    if ( externalRef == null ) throw new IllegalArgumentException( "The external reference may not be null.");
    this.title = title;
    this.externalRef = externalRef;
  }

  public boolean isExternalRef()
  {
    return true;
  }

  public String getTitle()
  {
    return this.title;
  }

  public URI getDocUri()
  {
    return this.externalRef;
  }

  public boolean isUnknownObject()
  {
    return false;
  }

  public Object getContent()
  {
    throw new IllegalStateException( "This metadata does not contain unrecognized content.");
  }

  public boolean isThredds()
  {
    return false;
  }
  //public List<Documentation> getDocumentation();
  public List<Property> getProperties()
  {
    throw new IllegalStateException( "This metadata does not contain THREDDS content." );
  }
}
