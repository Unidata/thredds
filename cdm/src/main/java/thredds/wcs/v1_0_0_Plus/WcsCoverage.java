package thredds.wcs.v1_0_0_Plus;

import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.unidata.geoloc.EPSG_OGC_CF_Helper;

import java.util.List;
import java.util.Collections;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class WcsCoverage
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( WcsCoverage.class );

  private WcsDataset dataset;
  private String coverageId;
  private GridDatatype coverage;

  private GridCoordSystem coordSys;

  private String nativeCRS;

  private String defaultRequestCrs;
  private String allowedCoverageFormat;

  private String rangeSetAxisName;
  private List<String> rangeSetAxisValues;

  public WcsCoverage( WcsDataset dataset, String coverageId)
          throws WcsException
  {
    this.dataset = dataset;
    this.coverageId = coverageId;

    if ( ! this.dataset.isAvailableCoverageName( this.coverageId))
    {
      log.error( "WcsCoverage(): requested coverage <" + coverageId + "> not available.");
      throw new WcsException( WcsException.Code.CoverageNotDefined, "", "Requested coverage <" + coverageId + "> not available.");
    }

    this.coverage = this.dataset.getAvailableCoverage( this.coverageId);
    if ( this.coverage == null )
    { // Redundant check ??
      log.error( "WcsCoverage(): requested coverage <" + coverageId + "> not available." );
      throw new WcsException( WcsException.Code.CoverageNotDefined, "", "Requested coverage <" + coverageId + "> not available." );
    }

    this.coordSys = coverage.getCoordinateSystem();

    this.nativeCRS = EPSG_OGC_CF_Helper.getWcs1_0CrsId( coordSys.getProjection() );

    this.defaultRequestCrs = "OGC:CRS84";
    this.allowedCoverageFormat = "application/x-netcdf";

    this.rangeSetAxisName = "Vertical";
    CoordinateAxis1D zaxis = coordSys.getVerticalAxis();
    for ( int z = 0; z < zaxis.getSize(); z++ )
      rangeSetAxisValues.add( zaxis.getCoordName( z ).trim());
  }

  public GridCoordSystem getCoordinateSystem() { return coordSys; }
  public String getDefaultRequestCrs() { return defaultRequestCrs; }
  public String getNativeCrs() { return nativeCRS; }
  public String getAllowedCoverageFormat() { return allowedCoverageFormat; }
  public String getRangeSetAxisName() { return rangeSetAxisName; }

  public boolean isRangeSetAxisValue( String value )
  {
    return rangeSetAxisValues.contains( value );
  }
  public List<String> getRangeSetAxisValueList()
  {
    return Collections.unmodifiableList( rangeSetAxisValues );

  }

}
