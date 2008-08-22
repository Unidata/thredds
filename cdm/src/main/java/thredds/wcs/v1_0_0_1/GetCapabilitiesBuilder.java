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
  public void setServerUri( URI serverUri ) { this.serverUri = serverUri; }

  public GetCapabilities.Section getSection() { return this.section; }
  public void setSection( GetCapabilities.Section section ) { this.section = section; }

  public String getUpdateSequence() { return updateSequence; }
  public void setUpdateSequence( String updateSequence ) { this.updateSequence = updateSequence; }

  public GetCapabilities.ServiceInfo getServiceInfo() { return serviceInfo; }
  public void setServiceInfo( GetCapabilities.ServiceInfo serviceInfo ) { this.serviceInfo = serviceInfo; }

  public GetCapabilities buildGetCapabilities()
  {
    // Check GetCapabilities requirements.
    if ( this.serverUri == null )
      throw new IllegalStateException( "Null server URI not allowed." );
    if ( this.section == null )
      throw new IllegalStateException( "Null section list not allowed (may be empty)." );

    return new GetCapabilities( this.getOperation(), this.getVersionString(),
                                this.getWcsDataset(),
                                this.serverUri, this.section,
                                this.updateSequence, this.serviceInfo );
  }
}
