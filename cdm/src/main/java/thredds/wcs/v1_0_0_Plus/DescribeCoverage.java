package thredds.wcs.v1_0_0_Plus;

import org.jdom.Namespace;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import java.net.URI;
import java.util.List;
import java.io.PrintWriter;
import java.io.IOException;

import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPoint;
import thredds.datatype.DateRange;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DescribeCoverage extends WcsRequest
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( DescribeCoverage.class );

  private URI serverURI;
  private List<String> coverages;

  private Document describeCoverageDoc;

  public DescribeCoverage( Operation operation, String version, String datasetPath, GridDataset dataset,
                           URI serverURI, List<String> coverages )
  {
    super( operation, version, datasetPath, dataset );

    this.serverURI = serverURI;
    this.coverages = coverages;
    if ( this.serverURI == null )
      throw new IllegalArgumentException( "Non-null server URI required." );
    if ( this.coverages == null )
      throw new IllegalArgumentException( "Non-null coverage list required." );
    if ( this.coverages.size() < 1 )
      throw new IllegalArgumentException( "Coverage list must contain at least one ID <" + this.coverages.size() + ">." );
    String badCovIds = "";
    for ( String curCov : coverages )
    {
      if ( ! this.isAvailableCoverageName( curCov))
        badCovIds += (badCovIds.length() > 0 ? ", " : "") + curCov;
    }
    if ( badCovIds.length() > 0 )
      throw new IllegalArgumentException("Coverage ID list contains one or more unknown IDs <" + badCovIds + ">." );
  }

  public Document getDescribeCoverageDoc()
  {
    if ( this.describeCoverageDoc == null )
      describeCoverageDoc = generateDescribeCoverageDoc();
    return describeCoverageDoc;
  }

  public void writeDescribeCoverageDoc( PrintWriter pw )
          throws IOException
  {
    XMLOutputter xmlOutputter = new XMLOutputter( org.jdom.output.Format.getPrettyFormat() );
    xmlOutputter.output( getDescribeCoverageDoc(), pw );
  }

  public Document generateDescribeCoverageDoc()
  {
    // CoverageDescription (wcs) [1]
    Element coverageDescriptionsElem = new Element( "CoverageDescription", wcsNS );
    coverageDescriptionsElem.addNamespaceDeclaration( gmlNS );
    coverageDescriptionsElem.addNamespaceDeclaration( xlinkNS );
    coverageDescriptionsElem.setAttribute( "version", this.getVersion() );
    // ToDo Consider dealing with "updateSequence"
    // coverageDescriptionsElem.setAttribute( "updateSequence", this.getCurrentUpdateSequence() );

    for ( String curCoverageId : this.coverages )
      coverageDescriptionsElem.addContent( genCoverageOfferingElem( curCoverageId ) );

    return new Document( coverageDescriptionsElem );
  }

  public Element genCoverageOfferingElem( String covId )
  {
    // ToDo WCS 1.0Plus - Change GridDatatype to GridDataset.Gridset
    GridDatatype coverage = this.getAvailableCoverage( covId );
    GridCoordSystem gcs = coverage.getCoordinateSystem();

    // CoverageDescription/CoverageOffering (wcs) [1..*]
    Element covDescripElem = genCoverageOfferingBriefElem( "CoverageOffering", covId,
                                                           coverage.getDescription(), gcs );

    // CoverageDescription/CoverageOffering/domainSet [1]
    covDescripElem.addContent( genDomainSetElem( gcs));

    // CoverageDescription/CoverageOffering/rangeSet [1]

    // CoverageDescription/CoverageOffering/supportedCRSs [1]
    // CoverageDescription/CoverageOffering/supportedFormats [1]
    // CoverageDescription/CoverageOffering/supportedInterpolations [0..1]

    //************************

    // CoverageDescriptions/CoverageDescription/Title (ows) [0..1]
    // CoverageDescriptions/CoverageDescription/Abstract (ows) [0..1]
    // CoverageDescriptions/CoverageDescription/Keywords (ows) [0..*]
    // CoverageDescriptions/CoverageDescription/Keywords/Keyword (ows) [0..*]
    // CoverageDescriptions/CoverageDescription/Identifier (wcs) [1]
    covDescripElem.addContent( new Element( "Identifier", wcsNS ).addContent( covId ) );
    // CoverageDescriptions/CoverageDescription/Metadata (ows) [0..*]
    // CoverageDescriptions/CoverageDescription/Domain (wcs) [1]
    // CoverageDescriptions/CoverageDescription/Range (wcs) [1]
    // CoverageDescriptions/CoverageDescription/SupportedCRS (wcs) [1..*] - URI
    // CoverageDescriptions/CoverageDescription/SupportedFormat (wcs) [1..*] - MIME Type (e.g., "application/x-netcdf")

    return covDescripElem;
  }

  public Element genDomainSetElem( GridCoordSystem gridCoordSys)
  {
    // ../domainSet
    Element domainSetElem = new Element( "domainSet", wcsNS);

    // ../domainSet/spatialDomain [0..1] AND/OR temporalDomain [0..1]
    domainSetElem.addContent( genSpatialDomainElem( gridCoordSys) );
    if ( gridCoordSys.hasTimeAxis() )
    {
      domainSetElem.addContent( genTemporalDomainElem( gridCoordSys.getDateRange() ) );
    }

    return domainSetElem;
  }

  private Element genSpatialDomainElem( GridCoordSystem gridCoordSys )
  {
    // ../domainSet/spatialDomain
    Element spatialDomainElem = new Element( "spatialDomain", wcsNS );
    // TODO if ( gcs.)
    // ../domainSet/spatialDomain/gml:Envelope [1..*]
    // ../domainSet/spatialDomain/gml:Envelope@srsName [0..1] (URI)
    // ../domainSet/spatialDomain/gml:Envelope/gml:pos [2] (space seperated list of double values)
    // ../domainSet/spatialDomain/gml:Envelope/gml:pos@dimension [0..1]  (number of entries in list)
    //this.

    // ../domainSet/spatialDomain/gml:RectifiedGrid [0..*]  OR gml:RectifiedGrid
    // ../domainSet/spatialDomain/gml:RectifiedGrid@srsName [0..1] (URI)
    // ../domainSet/spatialDomain/gml:RectifiedGrid@attribute [1] (positive integer)
    // ../domainSet/spatialDomain/gml:RectifiedGrid/gml:limits [1]
    // ../domainSet/spatialDomain/gml:RectifiedGrid/gml:limits/gml:GridEnvelope [1]
    // ../domainSet/spatialDomain/gml:RectifiedGrid/gml:limits/gml:GridEnvelope/gml:low [1] (integer list)
    // ../domainSet/spatialDomain/gml:RectifiedGrid/gml:limits/gml:GridEnvelope/gml:high [1] (integer list)
    // ../domainSet/spatialDomain/gml:RectifiedGrid/gml:axisName [1..*] (string)
    // ../domainSet/spatialDomain/gml:RectifiedGrid/gml:origin [1]
    // ../domainSet/spatialDomain/gml:RectifiedGrid/gml:origin/gml:pos [1] (space seperated list of double values)
    // ../domainSet/spatialDomain/gml:RectifiedGrid/gml:origin/gml:pos@dimension [0..1]  (number of entries in list)
    // ../domainSet/spatialDomain/gml:RectifiedGrid/gml:offsetVector [1..*] (space seperated list of double values)
    // ../domainSet/spatialDomain/gml:RectifiedGrid/gml:offsetVector@dimension [0..1]  (number of entries in list)

    // ../domainSet/spatialDomain/gml:Polygon [0..*]

    return spatialDomainElem;
  }

  protected Element genEnvelopeElem( GridCoordSystem gcs )
  {
    // spatialDomain/Envelope
    Element lonLatEnvelopeElem = new Element( "Envelope", wcsNS );
    lonLatEnvelopeElem.setAttribute( "srsName", "urn:ogc:def:crs:EPSG:6.3:???"); // "urn:ogc:def:crs:OGC:1.3:CRS84" );

    LatLonRect llbb = gcs.getLatLonBoundingBox();
    LatLonPoint llpt = llbb.getLowerLeftPoint();
    LatLonPoint urpt = llbb.getUpperRightPoint();

    // <CoverageOfferingBrief>/lonLatEnvelope/gml:pos
    lonLatEnvelopeElem.addContent(
            new Element( "pos", gmlNS ).addContent( llpt.getLongitude() + " " + llpt.getLatitude() ) );
    double lon = llpt.getLongitude() + llbb.getWidth();
    lonLatEnvelopeElem.addContent(
            new Element( "pos", gmlNS ).addContent( lon + " " + urpt.getLatitude() ) );
// ToDo Add vertical
//    CoordinateAxis1D vertAxis = gcs.getVerticalAxis();
//    if ( vertAxis != null )
//    {
//      // ToDo Deal with conversion to meters. Yikes!!
//      // See verAxis.getUnitsString()
//      lonLatEnvelopeElem.addContent(
//              new Element( "pos", gmlNS).addContent(
//                      vertAxis.getCoordValue( 0) + " " +
//                      vertAxis.getCoordValue( ((int)vertAxis.getSize()) - 1)));
//    }
// ToDo Add vertical

    // <CoverageOfferingBrief>/lonLatEnvelope/gml:timePostion [2]
    if ( gcs.hasTimeAxis() )
    {
      lonLatEnvelopeElem.addContent(
              new Element( "timePosition", gmlNS ).addContent(
                      gcs.getDateRange().getStart().toDateTimeStringISO() ) );
      lonLatEnvelopeElem.addContent(
              new Element( "timePosition", gmlNS ).addContent(
                      gcs.getDateRange().getEnd().toDateTimeStringISO() ) );
    }

    return lonLatEnvelopeElem;
  }

  private Element genTemporalDomainElem( DateRange dateRange )
  {
    Element temporalDomainElem = new Element( "temporalDomain", wcsNS );

    return temporalDomainElem;
  }
}