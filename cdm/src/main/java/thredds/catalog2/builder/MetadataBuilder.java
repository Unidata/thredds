package thredds.catalog2.builder;

import thredds.catalog2.Metadata;

import java.util.List;
import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface MetadataBuilder extends ThreddsBuilder
{
  public void setContainedContent( boolean isContainedContent );
  public boolean isContainedContent();

  public void setTitle( String title );
  public String getTitle();

  public void setExternalReference( URI externalReference );
  public URI getExternalReference();

  public void setContent( String content );
  public String getContent();

  public Metadata build() throws BuilderException;
}
