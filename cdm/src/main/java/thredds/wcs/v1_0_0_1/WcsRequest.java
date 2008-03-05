package thredds.wcs.v1_0_0_1;

import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPoint;
import org.jdom.Element;
import org.jdom.Namespace;

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
    NONE( "" ),
    GeoTIFF( "image/tiff" ),
    GeoTIFF_Float( "image/tiff" ),
    NetCDF3( "application/x-netcdf" );

    private String mimeType;

    Format( String mimeType ) { this.mimeType = mimeType; }

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
    if ( covDescription != null && ! covDescription.equals( ""))
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
// ToDo WCS 1.0Plus - Add vertical (Deal with conversion to meters. Yikes!!)
//    CoordinateAxis1D vertAxis = gcs.getVerticalAxis();
//    if ( vertAxis != null )
//    {
//      // See verAxis.getUnitsString()
//      firstPosition += " " + vertAxis.getCoordValue( 0);
//      secondPostion += " " + vertAxis.getCoordValue( ((int)vertAxis.getSize()) - 1);
//    }
    
    lonLatEnvelopeElem.addContent(
            new Element( "pos", gmlNS ).addContent( firstPosition ) );
    lonLatEnvelopeElem.addContent(
            new Element( "pos", gmlNS ).addContent( secondPosition ) );

    // <CoverageOfferingBrief>/lonLatEnvelope/gml:timePostion [2]
    if ( gcs.hasTimeAxis() )
    {
      lonLatEnvelopeElem.addContent(
              new Element( "timePosition", gmlNS).addContent(
                      gcs.getDateRange().getStart().toDateTimeStringISO()) );
      lonLatEnvelopeElem.addContent(
              new Element( "timePosition", gmlNS).addContent(
                      gcs.getDateRange().getEnd().toDateTimeStringISO()) );
    }

    return lonLatEnvelopeElem;
  }

}
