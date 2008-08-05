package thredds.catalog2.simpleImpl;

import thredds.catalog.DataFormatType;
import thredds.catalog2.Access;
import thredds.catalog2.Service;
import thredds.catalog2.builder.AccessBuilder;
import thredds.catalog2.builder.ServiceBuilder;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class AccessImpl implements Access, AccessBuilder
{
  private ServiceImpl service;
  private String urlPath;
  private DataFormatType dataFormat;
  private long dataSize;

  private boolean finished = false;

  protected AccessImpl( ServiceImpl service, String urlPath )
  {
    if ( service == null ) throw new IllegalArgumentException( "Service must not be null.");
    if ( urlPath == null ) throw new IllegalArgumentException( "Path must not be null.");
    this.service = service;
    this.urlPath = urlPath;
  }

  @Override
  public void setService( ServiceBuilder service )
  {
    if ( this.finished ) throw new IllegalStateException( "This AccessBuilder has been finished()." );
    if ( service == null ) throw new IllegalArgumentException( "Service must not be null." );
    this.service = (ServiceImpl) service;
  }

  @Override
  public void setUrlPath( String urlPath )
  {
    if ( this.finished ) throw new IllegalStateException( "This AccessBuilder has been finished()." );
    if ( urlPath == null ) throw new IllegalArgumentException( "Path must not be null." );
    this.urlPath = urlPath;
  }

  @Override
  public void setDataFormat( DataFormatType dataFormat )
  {
    if ( this.finished ) throw new IllegalStateException( "This AccessBuilder has been finished()." );
    this.dataFormat = dataFormat != null ? dataFormat : DataFormatType.NONE;
  }

  @Override
  public void setDataSize( long dataSize )
  {
    if ( this.finished ) throw new IllegalStateException( "This AccessBuilder has been finished()." );
    if ( dataSize > -1 ) throw new IllegalArgumentException( "Value must be zero or greater, or -1 if unknown.");
    this.dataSize = dataSize;
  }

  @Override
  public Service getService()
  {
    if ( !this.finished ) throw new IllegalStateException( "This Access has escaped its AccessBuilder before finish() was called." );
    return service;
  }

  @Override
  public ServiceBuilder getServiceBuilder()
  {
    if ( this.finished ) throw new IllegalStateException( "This AccessBuilder has been finished()." );
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
  public boolean isFinished()
  {
    return this.finished;
  }

  @Override
  public Access finish()
  {
    if ( this.finished)
      return this;

    this.service.finish();

    this.finished = true;
    return this;
  }
}
