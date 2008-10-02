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
public interface AccessBuilder
{
  public ServiceBuilder getServiceBuilder();
  public void setServiceBuilder( ServiceBuilder service );

  public String getUrlPath();
  public void setUrlPath( String urlPath );

  public DataFormatType getDataFormat();
  public void setDataFormat( DataFormatType dataFormat );

  public long getDataSize();
  public void setDataSize( long bytes );
  
  public boolean isFinished();
  public Access finish() throws BuildException;
}
