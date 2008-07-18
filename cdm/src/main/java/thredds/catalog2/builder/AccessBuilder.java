package thredds.catalog2.builder;

import thredds.catalog2.Service;
import thredds.catalog2.Access;
import thredds.catalog.DataFormatType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface AccessBuilder extends Access
{
  public void setServiceName( String name );
  public void setService( Service service );
  public void setUrlPath( String urlPath );
  public void setDataFormat( DataFormatType dataFormat );
  public void setDataSize( long bytes );
  
  public void finish();
}
