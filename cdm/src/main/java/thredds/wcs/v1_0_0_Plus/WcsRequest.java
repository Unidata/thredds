package thredds.wcs.v1_0_0_Plus;

import java.util.*;

import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
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
  private String datasetPath;
  private GridDataset dataset;
  // ToDo WCS 1.0Plus - change GridDatatype to GridDataset.Gridset
  private HashMap<String,GridDatatype> availableCoverages;

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
    NONE, GeoTIFF, GeoTIFF_Float, NetCDF3
  }

  WcsRequest( Operation operation, String version, String datasetPath, GridDataset dataset )
  {
    this.operation = operation;
    this.version = version;
    this.datasetPath = datasetPath;
    this.dataset = dataset;
    // ToDo WCS 1.0Plus - change GridDatatype to GridDataset.Gridset
    this.availableCoverages = new HashMap<String,GridDatatype>();

    // ToDo WCS 1.0PlusPlus - compartmentalize coverage to hide GridDatatype vs GridDataset.Gridset ???
    // ToDo WCS 1.0Plus - change FROM coverage for each parameter TO coverage for each coordinate system
    // This is WCS 1.0 coverage for each parameter
    for ( GridDatatype curGridDatatype : this.dataset.getGrids() )
    {
      GridCoordSystem gcs = curGridDatatype.getCoordinateSystem();
      if ( ! gcs.isRegularSpatial())
        continue;
      this.availableCoverages.put( curGridDatatype.getName(), curGridDatatype );
    }
    // ToDo WCS 1.0Plus - change FROM coverage for each parameter TO coverage for each coordinate system
    // This is WCS 1.1 style coverage for each coordinate system
    // for ( GridDataset.Gridset curGridSet : this.dataset.getGridsets())
    // {
    //   GridCoordSystem gcs = curGridSet.getGeoCoordSystem();
    //   if ( !gcs.isRegularSpatial() )
    //     continue;
    //   this.availableCoverages.put( gcs.getName(), curGridSet );
    // }

    if ( operation == null )
      throw new IllegalArgumentException( "Non-null operation required." );
    if ( this.datasetPath == null )
      throw new IllegalArgumentException( "Non-null dataset path required." );
    if ( this.dataset == null )
      throw new IllegalArgumentException( "Non-null dataset required." );
  }

  public Operation getOperation() { return operation; }
  public String getVersion() { return version; }
  public String getDatasetPath() { return datasetPath; }
  public GridDataset getDataset() { return dataset; }

  public boolean isAvailableCoverageName( String name )
  {
    return availableCoverages.containsKey( name);
  }

  // ToDo WCS 1.0Plus - change response type to GridDataset.Gridset
  public GridDatatype getAvailableCoverage( String name )
  {
    return availableCoverages.get( name);
  }

  // ToDo WCS 1.0Plus - change response type to GridDataset.Gridset
  public Collection<GridDatatype> getAvailableCoverageCollection()
  {
    return Collections.unmodifiableCollection( availableCoverages.values());
  }

  protected Element genCoverageOfferingBriefElem( String elemName, String covName, String covLabel,
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
