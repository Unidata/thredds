package thredds.catalog2;

import thredds.catalog.MetadataType;

import java.net.URI;
import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface Metadata
{
  public boolean isInherited();

  public boolean isExternalRef();
  public String getTitle();
  public URI getDocUri();

  public boolean isUnknownObject();
  public Object getContent();

  public boolean isThredds();
  public ThreddsMetadata getThreddsMetadata();
}
