package thredds.catalog2.simpleImpl;

import thredds.catalog.DataFormatType;
import thredds.catalog2.Access;
import thredds.catalog2.Service;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class AccessImpl implements Access
{
  private Service service;
  private String uriPath;
  private DataFormatType dataFormat;
  private long dataSize;

  public AccessImpl( Service service, String uriPath )
  {
    if ( service == null ) throw new IllegalArgumentException( "Service must not be null.");
    if ( uriPath == null ) throw new IllegalArgumentException( "Path must not be null.");
    this.service = service;
    this.uriPath = uriPath;
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
  public String getUriPath()
  {
    return uriPath;
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
}
