package thredds.catalog2.simpleImpl;

import thredds.catalog.DataFormatType;
import thredds.catalog2.Access;
import thredds.catalog2.Service;
import thredds.catalog2.builder.*;

import java.util.List;
import java.util.ArrayList;

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

  private boolean isBuilt = false;

  protected AccessImpl() {}

  public void setServiceBuilder( ServiceBuilder service )
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This AccessBuilder has been built." );
    if ( service == null ) throw new IllegalArgumentException( "Service must not be null." );
    this.service = (ServiceImpl) service;
  }

  public void setUrlPath( String urlPath )
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This AccessBuilder has been built." );
    if ( urlPath == null ) throw new IllegalArgumentException( "Path must not be null." );
    this.urlPath = urlPath;
  }

  public void setDataFormat( DataFormatType dataFormat )
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This AccessBuilder has been built." );
    this.dataFormat = dataFormat != null ? dataFormat : DataFormatType.NONE;
  }

  public void setDataSize( long dataSize )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This AccessBuilder has been built." );
    if ( dataSize < -1 )
      throw new IllegalArgumentException( "Value must be zero or greater, or -1 if unknown.");
    this.dataSize = dataSize;
  }

  public Service getService()
  {
    if ( !this.isBuilt ) throw new IllegalStateException( "This Access has escaped its AccessBuilder before build() was called." );
    return service;
  }

  public ServiceBuilder getServiceBuilder()
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This AccessBuilder has been built." );
    return service;
  }

  public String getUrlPath()
  {
    return urlPath;
  }
  
  public DataFormatType getDataFormat()
  {
    return dataFormat;
  }

  public long getDataSize()
  {
    return dataSize;
  }

  public boolean isBuildable( List<BuilderFinishIssue> issues )
  {
    if ( this.isBuilt )
      return true;

    List<BuilderFinishIssue> localIssues = new ArrayList<BuilderFinishIssue>();

    //ToDo Check invariants
    if ( this.service == null )
      localIssues.add( new BuilderFinishIssue( "The Service may not be null.", this ));
    if ( this.urlPath == null )
      localIssues.add( new BuilderFinishIssue( "The urlPath may not be null.", this ) );

    // Check subordinates.
    this.service.isBuildable( localIssues );

    if ( localIssues.isEmpty() )
      return true;

    issues.addAll( localIssues );
    return false;
  }

  public Access build() throws BuilderException
  {
    if ( this.isBuilt )
      return this;

    List<BuilderFinishIssue> issues = new ArrayList<BuilderFinishIssue>();
    if ( !isBuildable( issues ) )
      throw new BuilderException( issues );

    // Check subordinates.
    this.service.build();

    this.isBuilt = true;
    return this;
  }
}
