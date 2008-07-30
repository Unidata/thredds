package thredds.catalog2.simpleImpl;

import thredds.catalog.DataFormatType;
import thredds.catalog2.Access;
import thredds.catalog2.Service;
import thredds.catalog2.builder.AccessBuilder;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class AccessImpl implements AccessBuilder
{
  private Service service;
  private String urlPath;
  private DataFormatType dataFormat;
  private long dataSize;

  public AccessImpl( Service service, String urlPath )
  {
    if ( service == null ) throw new IllegalArgumentException( "Service must not be null.");
    if ( urlPath == null ) throw new IllegalArgumentException( "Path must not be null.");
    this.service = service;
    this.urlPath = urlPath;
  }

  @Override
  public void setService( Service service )
  {
    if ( service == null ) throw new IllegalArgumentException( "Service must not be null." );
    this.service = service;
  }

  @Override
  public void setUrlPath( String urlPath )
  {
    if ( urlPath == null ) throw new IllegalArgumentException( "Path must not be null." );
    this.urlPath = urlPath;
  }

  public void setDataFormat( DataFormatType dataFormat )
  {
    this.dataFormat = dataFormat != null ? dataFormat : DataFormatType.NONE;
  }
  public void setDataSize( long dataSize )
  {
    this.dataSize = dataSize;
  }

  @Override
  public Service getService()
  {
    return service;
  }

  @Override
  public String getUrlPath()
  {
    return urlPath;
  }
  
  @Override
  public DataFormatType getDataFormat()
  {
    return dataFormat;
  }

  @Override
  public long getDataSize()
  {
    return dataSize;
  }

  @Override
  public Access finish()
  {
    return this;
  }
}
