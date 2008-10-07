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
public interface ThreddsMetadata extends Metadata
{
  public List<Property> getProperties();
  public String getServiceName();

  public List<Documentation> getDocumentation();

  public interface Documentation
  {
    public String getContent();
    public String getDocType();
  }
}