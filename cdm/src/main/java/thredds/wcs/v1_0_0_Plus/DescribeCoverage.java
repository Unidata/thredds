package thredds.wcs.v1_0_0_Plus;

import org.jdom.Namespace;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Attribute;
import org.jdom.output.XMLOutputter;

import java.net.URI;
import java.util.List;
import java.io.PrintWriter;
import java.io.IOException;

import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.VariableSimpleIF;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.EPSG_OGC_CF_Helper;
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
    covDescripElem.addContent( genRangeSetElem( coverage));

    // CoverageDescription/CoverageOffering/supportedCRSs [1]
    covDescripElem.addContent( genSupportedCRSsElem( coverage));

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
    this.genEnvelopeElem( gridCoordSys);

    // ../domainSet/spatialDomain/gml:RectifiedGrid [0..*]
    this.genRectifiedGridElem( gridCoordSys);

    // ../domainSet/spatialDomain/gml:Polygon [0..*]

    return spatialDomainElem;
  }

  protected Element genRectifiedGridElem( GridCoordSystem gcs )
  {
    // ../spatialDomain/gml:RectifiedGrid
    Element rectifiedGridElem = new Element( "RectifiedGrid", gmlNS);
    
    // ../spatialDomain/gml:RectifiedGrid@srsName [0..1] (URI)
    rectifiedGridElem.setAttribute( "srsName", "EPSG:" );
//    String gridMapping = aGridset.getGrids().get( 0 ).findAttributeIgnoreCase( "grid_mapping" ).getStringValue();
//    VariableSimpleIF gridMapVar = gridDs.getDataVariable( gridMapping );
//
//    gcsMsg.append( "    GridDataset GridMap <" ).append( gridMapVar.getName() ).append( "> Params:\n" );
//    for ( ucar.nc2.Attribute curAtt : gridMapVar.getAttributes() )
//    {
//      gcsMsg.append( "      " ).append( curAtt.toString() ).append( "\n" );
//    }

    // ../spatialDomain/gml:RectifiedGrid@attribute [1] (positive integer)
    // ../spatialDomain/gml:RectifiedGrid/gml:limits [1]
    // ../spatialDomain/gml:RectifiedGrid/gml:limits/gml:GridEnvelope [1]
    // ../spatialDomain/gml:RectifiedGrid/gml:limits/gml:GridEnvelope/gml:low [1] (integer list)
    // ../spatialDomain/gml:RectifiedGrid/gml:limits/gml:GridEnvelope/gml:high [1] (integer list)
    // ../spatialDomain/gml:RectifiedGrid/gml:axisName [1..*] (string)
    // ../spatialDomain/gml:RectifiedGrid/gml:origin [1]
    // ../spatialDomain/gml:RectifiedGrid/gml:origin/gml:pos [1] (space seperated list of double values)
    // ../spatialDomain/gml:RectifiedGrid/gml:origin/gml:pos@dimension [0..1]  (number of entries in list)
    // ../spatialDomain/gml:RectifiedGrid/gml:offsetVector [1..*] (space seperated list of double values)
    // ../spatialDomain/gml:RectifiedGrid/gml:offsetVector@dimension [0..1]  (number of entries in list)

    return rectifiedGridElem;
  }

  protected Element genEnvelopeElem( GridCoordSystem gcs )
  {
    // spatialDomain/Envelope
    Element envelopeElem;
    if ( gcs.hasTimeAxis() )
      envelopeElem = new Element( "EnvelopeWithTimePeriod", wcsNS );
    else
      envelopeElem = new Element( "Envelope", wcsNS );

    // spatialDomain/Envelope@srsName [0..1] (URI)
    envelopeElem.setAttribute( "srsName", "urn:ogc:def:crs:OGC:1.3:CRS84" );

    LatLonRect llbb = gcs.getLatLonBoundingBox();
    LatLonPoint llpt = llbb.getLowerLeftPoint();
    LatLonPoint urpt = llbb.getUpperRightPoint();

    double lon = llpt.getLongitude() + llbb.getWidth();
    int posDim = 2;
    String firstPosition = llpt.getLongitude() + " " + llpt.getLatitude();
    String secondPosition = lon + " " + urpt.getLatitude();
// ToDo WCS 1.0Plus - Add vertical (Deal with conversion to meters. Yikes!!)
//    CoordinateAxis1D vertAxis = gcs.getVerticalAxis();
//    if ( vertAxis != null )
//    {
//      // See verAxis.getUnitsString()
//      posDim++;
//      firstPosition += " " + vertAxis.getCoordValue( 0);
//      secondPosition += " " + vertAxis.getCoordValue( ((int)vertAxis.getSize()) - 1);
//    }
    String posDimString = Integer.toString( posDim );

    // spatialDomain/Envelope/gml:pos [2] (space seperated list of double values)
    // spatialDomain/Envelope/gml:pos@dimension [0..1]  (number of entries in list)
    envelopeElem.addContent(
            new Element( "pos", gmlNS )
                    .addContent( firstPosition )
                    .setAttribute( new Attribute( "dimension", posDimString) ) );
    envelopeElem.addContent(
            new Element( "pos", gmlNS )
                    .addContent( secondPosition )
                    .setAttribute( new Attribute( "dimension", posDimString ) ) );

    // spatialDomain/Envelope/gml:timePostion [2]
    if ( gcs.hasTimeAxis() )
    {
      envelopeElem.addContent(
              new Element( "timePosition", gmlNS).addContent(
                      gcs.getDateRange().getStart().toDateTimeStringISO()));
      envelopeElem.addContent(
              new Element( "timePosition", gmlNS).addContent(
                      gcs.getDateRange().getEnd().toDateTimeStringISO()));
    }

    return envelopeElem;
  }

  private Element genTemporalDomainElem( DateRange dateRange )
  {
    Element temporalDomainElem = new Element( "temporalDomain", wcsNS );

    return temporalDomainElem;
  }

  public Element genRangeSetElem( GridDatatype coverage )
  {
    Element rangeSetElem = new Element( "rangeSet", wcsNS);
    Element innerRangeSetElem = new Element( "RangeSet", gmlNS);
    if ( "" != null )
      innerRangeSetElem.addContent(
              new Element( "description").addContent(
                      "" ) );
    innerRangeSetElem.addContent(
            new Element( "name", wcsNS).addContent( ""));
    innerRangeSetElem.addContent(
            new Element( "label", wcsNS ).addContent( "" ) );
    //for ( double z : coverage.getCoordinateSystem().getVerticalAxis().getCoordValues())
    innerRangeSetElem.addContent(
            new Element( "axisDescription", wcsNS ).addContent( "" ) );

    return rangeSetElem.addContent( innerRangeSetElem);
  }

  public Element genSupportedCRSsElem( GridDatatype coverage )
  {
    Element supportedCRSsElem = new Element( "supportedCRSs", wcsNS);

    supportedCRSsElem.addContent(
            new Element( "requestCRSs", wcsNS)
                    .addContent( "OGC:CRS84"));

    String nativeCRS = EPSG_OGC_CF_Helper.getWcs1_0CrsId( coverage.getCoordinateSystem().getProjection());
    supportedCRSsElem.addContent(
            new Element( "responseCRSs", wcsNS)
                    .addContent( nativeCRS));

    return supportedCRSsElem;
  }
}