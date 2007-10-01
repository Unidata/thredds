package thredds.wcs.v1_1_0;

import java.util.List;
import java.util.Collections;

import ucar.nc2.dt.GridDataset;

/**
 * Represent the incoming WCS 1.1.0 request.
 *
 * @author edavis
 * @since 4.0
 */
public class Request
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( Request.class );

  // General request info
  private Operation operation;
  private String negotiatedVersion;

  private String expectedVersion = "1.1.0"; 

  // GetCapabilities request info
  private List<GetCapabilities.Section> sections;
  private GetCapabilities.ServiceId serviceId;
  private GetCapabilities.ServiceProvider serviceProvider;

  // DescribeCoverage request info
  private List<String> identifierList;

  // GetCoverage request info
  private String identifier;

  // Dataset
  private String datasetPath;
  private GridDataset dataset;


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

  static public Request getGetCapabilitiesRequest( Operation operation,
                                                   String negotiatedVersion,
                                                   List<GetCapabilities.Section> sections,
                                                   GetCapabilities.ServiceId serviceId,
                                                   GetCapabilities.ServiceProvider serviceProvider,
                                                   String datasetPath,
                                                   GridDataset dataset)
  {
    Request req = new Request( operation, negotiatedVersion, datasetPath, dataset );
    if ( ! operation.equals( Operation.GetCapabilities ) )
      throw new IllegalArgumentException( "The \"" + operation.toString() + "\" operation not supported by this method.");
    req.sections = sections;
    req.serviceId = serviceId;
    req.serviceProvider = serviceProvider;

    if ( req.sections == null )
      throw new IllegalArgumentException( "Non-null section list required.");
    if ( req.serviceId == null )
      throw new IllegalArgumentException( "Non-null service ID required.");
    if ( req.serviceProvider == null )
      throw new IllegalArgumentException( "Non-null service provider expected.");

    return req;
  }

  static public Request getDescribeCoverageRequest( Operation operation,
                                                    String negotiatedVersion,
                                                    List<String> identifiers,
                                                    String datasetPath,
                                                    GridDataset dataset)
  {
    Request req = new Request( operation, negotiatedVersion, datasetPath, dataset );
    if ( !operation.equals( Operation.DescribeCoverage ) )
      throw new IllegalArgumentException( "The \"" + operation.toString() + "\" operation not supported by this method." );
    req.identifierList = identifiers;

    return req;
  }

  static public Request getGetCoverageRequest( Operation operation,
                                               String negotiatedVersion,
                                               String identifier,
                                               String datasetPath,
                                               GridDataset dataset)
  {
    Request req = new Request( operation, negotiatedVersion, datasetPath, dataset );
    if ( !operation.equals( Operation.GetCoverage ) )
      throw new IllegalArgumentException( "The \"" + operation.toString() + "\" operation not supported by this method." );
    req.identifier = identifier;

    return req;
  }

  Request( Operation operation, String negotiatedVersion, String datasetPath, GridDataset dataset)
  {
    this.operation = operation;
    this.negotiatedVersion = negotiatedVersion;
    this.datasetPath = datasetPath;
    this.dataset = dataset;

    if ( operation == null )
      throw new IllegalArgumentException( "Non-null operation required." );
    if ( this.negotiatedVersion == null )
      throw new IllegalArgumentException( "Non-null negotiated version required." );
    if ( ! this.negotiatedVersion.equals( expectedVersion) )
      throw new IllegalArgumentException( "Version <" + negotiatedVersion + "> not as expected <" + expectedVersion + ">." );
    if ( this.datasetPath == null )
      throw new IllegalArgumentException( "Non-null dataset path required." );
    if ( this.dataset == null )
      throw new IllegalArgumentException( "Non-null dataset required." );
  }

  // ---------- General getters
  public Operation getOperation() { return operation; }

  public String getDatasetPath() { return datasetPath; }
  public GridDataset getDataset() { return dataset; }


  // ---------- GetCapabilities getters
  public List<GetCapabilities.Section> getSections()
  {
    return Collections.unmodifiableList( sections );
  }

  public GetCapabilities.ServiceId getServiceId() { return serviceId; }
  public GetCapabilities.ServiceProvider getServiceProvider() { return serviceProvider; }

  // ---------- DescribeCoverage getters
  public List<String> getIdentifierList() { return identifierList; }

  // ---------- GetCoverage getters
  public String getIdentifier() { return identifier; }

}
