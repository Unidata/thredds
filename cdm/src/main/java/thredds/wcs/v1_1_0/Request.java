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

  // GetCoverage request info

  // Dataset
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

  public Request() {}
  public Request( Operation operation, String negotiatedVersion,
                  List<GetCapabilities.Section> sections,
                  GetCapabilities.ServiceId serviceId,
                  GetCapabilities.ServiceProvider serviceProvider,
                  GridDataset dataset )
  {
    this.operation = operation;
    this.negotiatedVersion = negotiatedVersion;
    this.sections = sections;
    this.serviceId = serviceId;
    this.serviceProvider = serviceProvider;
    this.dataset = dataset;
  }

  public Operation getOperation() { return operation; }

  public List<GetCapabilities.Section> getSections()
  {
    return Collections.unmodifiableList( sections );
  }

  public GetCapabilities.ServiceId getServiceId()
  {
    return this.serviceId;
  }

  public GetCapabilities.ServiceProvider getServiceProvider()
  {
    return this.serviceProvider;
  }

  public GridDataset getDataset()
  {
    return dataset;
  }
}
