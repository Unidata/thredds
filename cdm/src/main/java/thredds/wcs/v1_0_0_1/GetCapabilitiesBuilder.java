package thredds.wcs.v1_0_0_1;

import thredds.wcs.Request;
import ucar.nc2.dt.GridDataset;

import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class GetCapabilitiesBuilder extends WcsRequestBuilder
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( GetCapabilitiesBuilder.class );

  GetCapabilitiesBuilder( String versionString,
                          Request.Operation operation,
                          GridDataset dataset,
                          String datasetPath )
  {
    super( versionString, operation, dataset, datasetPath );
  }

  private URI serverUri;
  private GetCapabilities.Section section;
  private String updateSequence;
  private GetCapabilities.ServiceInfo serviceInfo;

  public URI getServerUri() { return this.serverUri; }
  public GetCapabilitiesBuilder setServerUri( URI serverUri )
  { this.serverUri = serverUri; return this; }

  public GetCapabilities.Section getSection() { return this.section; }
  public GetCapabilitiesBuilder setSection( GetCapabilities.Section section )
  { this.section = section; return this; }

  public String getUpdateSequence() { return updateSequence; }
  public GetCapabilitiesBuilder setUpdateSequence( String updateSequence )
  { this.updateSequence = updateSequence; return this; }

  public GetCapabilities.ServiceInfo getServiceInfo() { return serviceInfo; }
  public GetCapabilitiesBuilder setServiceInfo( GetCapabilities.ServiceInfo serviceInfo )
  { this.serviceInfo = serviceInfo; return this; }

  public GetCapabilities buildGetCapabilities()
  {
    // Check GetCapabilities requirements.
    if ( this.serverUri == null )
      throw new IllegalStateException( "Null server URI not allowed." );
    if ( this.section == null )
      throw new IllegalStateException( "Null section not allowed." );

    return new GetCapabilities( this.getOperation(), this.getVersionString(),
                                this.getWcsDataset(),
                                this.serverUri, this.section,
                                this.updateSequence, this.serviceInfo );
  }
}
