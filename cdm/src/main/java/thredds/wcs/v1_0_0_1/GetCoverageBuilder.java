package thredds.wcs.v1_0_0_1;

import thredds.wcs.Request;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.units.DateRange;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class GetCoverageBuilder extends WcsRequestBuilder
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( GetCoverageBuilder.class );

  GetCoverageBuilder( String versionString,
                      Request.Operation operation,
                      GridDataset dataset,
                      String datasetPath )
  {
    super( versionString, operation, dataset, datasetPath );
  }

  private String coverageId, crs, responseCRS;
  private Request.BoundingBox bbox;
  private DateRange timeRange;
  private WcsCoverage.VerticalRange verticalRange;  // parameter
  private Request.Format format;

  public String getCoverageId() { return coverageId; }
  public void setCoverageId( String coverageId ) { this.coverageId = coverageId; }

  public String getCrs() { return crs; }
  public void setCrs( String crs ) { this.crs = crs; }

  public String getResponseCRS() { return responseCRS; }
  public void setResponseCRS( String responseCRS ) { this.responseCRS = responseCRS; }

  public Request.BoundingBox getBbox() { return bbox; }
  public void setBbox( Request.BoundingBox bbox ) { this.bbox = bbox; }

  public DateRange getTimeRange() { return timeRange; }
  public void setTimeRange( DateRange timeRange ) { this.timeRange = timeRange; }

  public WcsCoverage.VerticalRange getVerticalRange() { return verticalRange; }
  public void setVerticalRange( WcsCoverage.VerticalRange verticalRange ) { this.verticalRange = verticalRange; }

  public Request.Format getFormat() { return format; }
  public void setFormat( Request.Format format ) { this.format = format; }

  public GetCoverage buildGetCoverage()
          throws WcsException
  {
    return new GetCoverage( this.getOperation(),
                            this.getVersionString(),
                            this.getWcsDataset(),
                            coverageId,
                            crs, responseCRS,
                            bbox, timeRange, verticalRange,
                            format);
  }

}
