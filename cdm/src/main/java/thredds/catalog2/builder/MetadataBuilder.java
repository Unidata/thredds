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
  public boolean isContainedContent();

  public String getTitle();

  public URI getExternalReference();

  public String getContent();

  public Metadata build() throws BuilderException;
}
