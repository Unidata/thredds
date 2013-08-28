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
package thredds.wcs.v1_0_0_Plus;

import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPoint;
import org.jdom2.Element;
import org.jdom2.Namespace;

/**
 * Represent the incoming WCS 1.0.0+ request.
 *
 * @author edavis
 * @since 4.0
 */
public abstract class WcsRequest
{
  protected static final Namespace wcsNS = Namespace.getNamespace( "http://www.opengis.net/wcs" );
  protected static final Namespace gmlNS = Namespace.getNamespace( "gml", "http://www.opengis.net/gml" );
  protected static final Namespace xlinkNS = Namespace.getNamespace( "xlink", "http://www.w3.org/1999/xlink" );

  // General request info
  private Operation operation;
  private String version;

  // Dataset
  private WcsDataset dataset;

  public enum Operation
  {
    GetCapabilities, DescribeCoverage, GetCoverage
  }

  public enum RequestEncoding
  {
    GET_KVP, POST_XML, POST_SOAP
  }

  public enum Format
  {
    NONE( ""),
    GeoTIFF("image/tiff"),
    GeoTIFF_Float("image/tiff"),
    NetCDF3("application/x-netcdf");

    private String mimeType;
    Format( String mimeType) { this.mimeType = mimeType; }

    public String getMimeType() { return mimeType; }
    
    public static Format getFormat( String mimeType )
    {
      for ( Format curSection : Format.values() )
      {
        if ( curSection.mimeType.equals( mimeType ) )
          return curSection;
      }
      throw new IllegalArgumentException( "No such instance <" + mimeType + ">." );
    }
  }
      /*
  All( "" ),

  Service( "WCS_Capabilities/Service" ),

  Capability( "WCS_Capabilities/Capability" ),

  ContentMetadata( "WCS_Capabilities/ContentMetadata" );

  private final String altId;

  Section( String altId )
  {
    this.altId = altId;
  }

  public String toString()
  {
    return altId;
  }

  public static Section getSection( String altId )
  {
    for ( Section curSection : Section.values() )
    {
      if ( curSection.altId.equals( altId ) )
        return curSection;
    }
    throw new IllegalArgumentException( "No such instance <" + altId + ">." );
  }
     */
  WcsRequest( Operation operation, String version, WcsDataset dataset )
  {
    this.operation = operation;
    this.version = version;
    this.dataset = dataset;

    if ( operation == null )
      throw new IllegalArgumentException( "Non-null operation required." );
    if ( this.dataset == null )
      throw new IllegalArgumentException( "Non-null dataset required." );
  }

  public Operation getOperation() { return operation; }
  public String getVersion() { return version; }
  public WcsDataset getDataset() { return dataset; }

  protected Element genCoverageOfferingBriefElem( String elemName,
                                                  String covName, String covLabel, String covDescription,
                                                  GridCoordSystem gridCoordSys )
  {

    // <CoverageOfferingBrief>
    Element briefElem = new Element( elemName, wcsNS );

    // <CoverageOfferingBrief>/gml:metaDataProperty [0..*]
    // <CoverageOfferingBrief>/gml:description [0..1]
    // <CoverageOfferingBrief>/gml:name [0..*]
    // <CoverageOfferingBrief>/metadataLink [0..*]

    // <CoverageOfferingBrief>/description [0..1]
    // <CoverageOfferingBrief>/name [1]
    // <CoverageOfferingBrief>/label [1]
    if ( covDescription != null && covDescription.length() != 0)
      briefElem.addContent( new Element( "description", wcsNS ).addContent( covDescription ) );
    briefElem.addContent( new Element( "name", wcsNS ).addContent( covName ) );
    briefElem.addContent( new Element( "label", wcsNS ).addContent( covLabel ) );

    // <CoverageOfferingBrief>/lonLatEnvelope [1]
    briefElem.addContent( genLonLatEnvelope( gridCoordSys ) );

    // ToDo Add keywords capabilities.
    // <CoverageOfferingBrief>/keywords [0..*]  /keywords [1..*] and /type [0..1]

    return briefElem;
  }

  protected Element genLonLatEnvelope( GridCoordSystem gcs )
  {
    // <CoverageOfferingBrief>/lonLatEnvelope
    Element lonLatEnvelopeElem = new Element( "lonLatEnvelope", wcsNS );
    lonLatEnvelopeElem.setAttribute( "srsName", "urn:ogc:def:crs:OGC:1.3:CRS84" );

    LatLonRect llbb = gcs.getLatLonBoundingBox();
    LatLonPoint llpt = llbb.getLowerLeftPoint();
    LatLonPoint urpt = llbb.getUpperRightPoint();

    // <CoverageOfferingBrief>/lonLatEnvelope/gml:pos
    String firstPosition = llpt.getLongitude() + " " + llpt.getLatitude();
    double lon = llpt.getLongitude() + llbb.getWidth();
    String secondPosition = lon + " " + urpt.getLatitude();
// ToDo WCS 1.0Plus - Deal with conversion to meters. (Yikes!!)
    CoordinateAxis1D vertAxis = gcs.getVerticalAxis();
    if ( vertAxis != null )
    {
      // See verAxis.getUnitsString()
      double zeroIndexValue = vertAxis.getCoordValue( 0 );
      double sizeIndexValue = vertAxis.getCoordValue( ( (int) vertAxis.getSize() ) - 1 );
      if ( vertAxis.getPositive().equals( ucar.nc2.constants.CF.POSITIVE_UP))
      {
        firstPosition += " " + zeroIndexValue;
        secondPosition += " " + sizeIndexValue;
      }
      else
      {
        firstPosition += " " + sizeIndexValue;
        secondPosition += " " + zeroIndexValue;
      }
    }
    
    lonLatEnvelopeElem.addContent(
            new Element( "pos", gmlNS ).addContent( firstPosition ) );
    lonLatEnvelopeElem.addContent(
            new Element( "pos", gmlNS ).addContent( secondPosition ) );

    // <CoverageOfferingBrief>/lonLatEnvelope/gml:timePostion [2]
    if ( gcs.hasTimeAxis() )
    {
      lonLatEnvelopeElem.addContent(
              new Element( "timePosition", gmlNS).addContent(
                      gcs.getCalendarDateRange().getStart().toString()) );
      lonLatEnvelopeElem.addContent(
              new Element( "timePosition", gmlNS).addContent(
                      gcs.getCalendarDateRange().getEnd().toString()) );
    }

    return lonLatEnvelopeElem;
  }

}
