package thredds.catalog2.builder;

import thredds.catalog.ServiceType;
import thredds.catalog2.Service;

import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface ServiceBuilder extends Service
{
  public void setName( String name );
  public void setDescription( String description );
  public void setType( ServiceType type );
  public void setBaseUri( URI baseUri );
  public void setSuffix( String suffix );

  public void addProperty( String name, String value );

  public ServiceBuilder addService();
  public ServiceBuilder addService( int index );

  public void finish();
}
