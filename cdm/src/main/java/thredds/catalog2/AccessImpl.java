package thredds.catalog2;

import thredds.catalog.DataFormatType;
import thredds.catalog.ServiceType;

import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class AccessImpl implements Access
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( AccessImpl.class );

  private Service service;
  private String uriPath;
  private DataFormatType dataFormat;
  private long dataSize;

  public AccessImpl( Service service, String uriPath, DataFormatType dataFormat, long dataSize )
  {
    if ( service == null ) throw new IllegalArgumentException( "Service must not be null.");
    if ( uriPath == null ) throw new IllegalArgumentException( "Path must not be null.");
    this.service = service;
    this.uriPath = uriPath;
    this.dataFormat = dataFormat;
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
