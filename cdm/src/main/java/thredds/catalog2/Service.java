package thredds.catalog2;

import thredds.catalog.ServiceType;

import java.net.URI;
import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface Service
{
  public String getName();
  public String getDescription();
  public ServiceType getType();
  public URI getBaseUri();
  public String getSuffix();
  public List<Property> getProperties();
}
