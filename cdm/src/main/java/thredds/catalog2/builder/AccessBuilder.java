package thredds.catalog2.builder;

import thredds.catalog2.Access;
import thredds.catalog.DataFormatType;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface AccessBuilder extends ThreddsBuilder
{
  public ServiceBuilder getServiceBuilder();
  public void setServiceBuilder( ServiceBuilder service );

  public String getUrlPath();
  public void setUrlPath( String urlPath );

  public DataFormatType getDataFormat();
  public void setDataFormat( DataFormatType dataFormat );

  public long getDataSize();
  public void setDataSize( long bytes );

  public Access build() throws BuilderException;
}
