/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
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
