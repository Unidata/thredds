package thredds.wcs.v1_0_0_Plus;

import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.NetcdfCFWriter;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.geoloc.EPSG_OGC_CF_Helper;
import ucar.unidata.geoloc.LatLonRect;
import ucar.ma2.Range;
import ucar.ma2.InvalidRangeException;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;

import thredds.datatype.DateRange;

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
  private WcsDataset dataset;

  private GridCoordSystem coordSys;

  private String nativeCRS;

  private String defaultRequestCrs;
  private String allowedCoverageFormat;

  private String rangeSetAxisName;
  private List<String> rangeSetAxisValues;

  public WcsCoverage( GridDatatype coverage, WcsDataset dataset)
  {
    this.dataset = dataset;
    if ( this.dataset == null )
    {
      log.error( "WcsCoverage(): non-null dataset required." );
      throw new IllegalArgumentException( "Non-null dataset required." );
    }

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

  static private DiskCache2 diskCache = null;
  static public void setDiskCache( DiskCache2 _diskCache ) { diskCache = _diskCache; }
  static private DiskCache2 getDiskCache()
  {
    if ( diskCache == null )
    {
      log.error( "getDiskCache(): Disk cache has not been set." );
      throw new IllegalStateException( "Disk cache must be set before calling GetCoverage.getDiskCache()." );
    }
    return diskCache;
  }

  public File writeCoverageDataToFile( LatLonRect bboxLatLonRect, VerticalRange verticalRange, DateRange timeRange)
          throws WcsException
  {
    File ncFile = getDiskCache().getCacheFile( this.dataset.getDatasetPath() + "-" + this.coverage.getName() + ".nc" );

    //GridDatatype gridDatatype = this.coverage.getGridDatatype().makeSubset( );

    NetcdfCFWriter writer = new NetcdfCFWriter();
    try
    {
      this.coverage.getCoordinateSystem().getVerticalAxis().isNumeric();
      writer.makeFile( ncFile.getPath(), this.dataset.getDataset(),
                       Collections.singletonList( this.coverage.getName() ),
                       bboxLatLonRect, 1,
                       verticalRange.getRange( this.coordSys),
                       timeRange, 1,
                       true );
    }
    catch ( InvalidRangeException e )
    {
      log.error( "writeCoverageDataToFile(): Failed to subset coverage <" + this.coverage.getName() + ">: " + e.getMessage() );
      throw new WcsException( WcsException.Code.CoverageNotDefined, "", "Failed to subset coverage <" + this.coverage.getName() + ">." );
    }
    catch ( IOException e )
    {
      log.error( "writeCoverageDataToFile(): Failed to write file for requested coverage <" + this.coverage.getName() + ">: " + e.getMessage() );
      throw new WcsException( WcsException.Code.UNKNOWN, "", "Problem creating coverage <" + this.coverage.getName() + ">." );
    }
    return ncFile;

  }

  public static class VerticalRange
  {
    private double min, max;
    private int stride;

    public VerticalRange( double minimum, double maximum, int stride )
    {
      if ( minimum > maximum )
        throw new IllegalArgumentException( "VerticalRange minimum <" + minimum + "> greater than maximum <" + maximum + ">." );
      if ( stride < 1 )
        throw new IllegalArgumentException( "VerticalRange stride <" + stride + "> less than one (1 means all point)." );
      this.min = minimum;
      this.max = maximum;
      this.stride = stride;
    }

    public double getMinimum() { return min; }
    public double getMaximum() { return max; }
    public int getStride() { return stride; }

    public Range getRange( GridCoordSystem gcs )
    {
      if ( gcs == null )
        throw new IllegalArgumentException( "GridCoordSystem must be non-null." );
      int minIndex = gcs.getVerticalAxis().findCoordElement( min );
      int maxIndex = gcs.getVerticalAxis().findCoordElement( max );
      if ( minIndex == -1 || maxIndex == -1 )
        return null;
      try
      {
        return new Range( minIndex, maxIndex, stride );
      }
      catch ( InvalidRangeException e )
      {
        return null;
      }
    }
  }

}
