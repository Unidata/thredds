package thredds.wcs.v1_0_0_Plus;

import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.unidata.geoloc.EPSG_OGC_CF_Helper;
import ucar.ma2.Range;
import ucar.ma2.InvalidRangeException;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

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

  // ToDo WCS 1.0Plus - change FROM coverage for each parameter TO coverage for each coordinate system
  private GridDatatype coverage;

  private GridCoordSystem coordSys;

  private String nativeCRS;

  private String defaultRequestCrs;
  private String allowedCoverageFormat;

  private String rangeSetAxisName;
  private List<String> rangeSetAxisValues;

  public WcsCoverage( GridDatatype coverage)
  {
    this.coverage = coverage;
    if ( this.coverage == null )
    {
      log.error( "WcsCoverage(): non-null coverage required." );
      throw new IllegalArgumentException( "Non-null coverage required." );
    }
    this.coordSys = coverage.getCoordinateSystem();

    this.nativeCRS = EPSG_OGC_CF_Helper.getWcs1_0CrsId( coordSys.getProjection() );

    this.defaultRequestCrs = "OGC:CRS84";
    this.allowedCoverageFormat = "application/x-netcdf";

    this.rangeSetAxisName = "Vertical";
    CoordinateAxis1D zaxis = coordSys.getVerticalAxis();
    if ( zaxis != null )
    {
      rangeSetAxisValues = new ArrayList<String>();
      for ( int z = 0; z < zaxis.getSize(); z++ )
        rangeSetAxisValues.add( zaxis.getCoordName( z ).trim() );
    }
    else
      rangeSetAxisValues = Collections.emptyList();
  }

  GridDatatype getGridDatatype() { return coverage; }

  public String getName() { return coverage.getName(); }
  public String getLabel() { return coverage.getDescription(); }
  public String getDescription() { return coverage.getInfo(); }
  public GridCoordSystem getCoordinateSystem() { return coordSys; }
  public boolean hasMissingData() { return coverage.hasMissingData(); }

  public String getDefaultRequestCrs() { return defaultRequestCrs; }
  public String getNativeCrs() { return nativeCRS; }
  public String getAllowedCoverageFormat() { return allowedCoverageFormat; }
  public String getRangeSetAxisName() { return rangeSetAxisName; }

  public boolean isRangeSetAxisValue( String value )
  {
    return rangeSetAxisValues.contains( value );
  }
  public boolean isRangeSetAxisNumeric()
  {
    return coordSys.getVerticalAxis().isNumeric();
  }
  public List<String> getRangeSetAxisValueList()
  {
    return Collections.unmodifiableList( rangeSetAxisValues );
  }

  public Range getRangeSetAxisRange( double minValue, double maxValue)
  {
    if ( minValue > maxValue )
    {
      log.error( "getRangeSetAxisRange(): Min is greater than max <" + minValue + ", " + maxValue + ">." );
      throw new IllegalArgumentException( "Min is greater than max <" + minValue + ", " + maxValue + ">." );
    }
    CoordinateAxis1D zaxis = coordSys.getVerticalAxis();
    if ( zaxis != null )
    {
      int minIndex = zaxis.findCoordElement( minValue);
      int maxIndex = zaxis.findCoordElement( maxValue);

      if ( minIndex == -1 || maxIndex == -1 )
        return null;

      try
      {
        return new Range( minIndex, maxIndex);
      }
      catch ( InvalidRangeException e )
      {
        return null;
      }
    }
    else
      return null;
  }
}
