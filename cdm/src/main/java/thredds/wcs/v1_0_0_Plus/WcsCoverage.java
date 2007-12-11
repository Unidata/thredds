package thredds.wcs.v1_0_0_Plus;

import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.NetcdfCFWriter;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.geoloc.*;
import ucar.ma2.Range;
import ucar.ma2.InvalidRangeException;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;
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
  private LatLonRect latLonBoundingBox; // Estimate when native CRS is  not lat/lon.

  private String allowedCoverageFormat;

  private WcsRangeField range;

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
    if ( this.coordSys == null )
    {
      log.error( "WcsCoverage(): Coverage must have non-null coordinate system." );
      throw new IllegalArgumentException( "Non-null coordinate system required." );
    }

    this.nativeCRS = EPSG_OGC_CF_Helper.getWcs1_0CrsId( this.coordSys.getProjection() );

    this.defaultRequestCrs = "OGC:CRS84";
    this.latLonBoundingBox = this.coordSys.getLatLonBoundingBox();

    this.allowedCoverageFormat = "application/x-netcdf";

    CoordinateAxis1D zaxis = this.coordSys.getVerticalAxis();
    WcsRangeField.Axis vertAxis;
    if ( zaxis != null )
    {
      List<String> vals = new ArrayList<String>();
      for ( int z = 0; z < zaxis.getSize(); z++ )
        vals.add( zaxis.getCoordName( z ).trim() );
      vertAxis = new WcsRangeField.Axis( "Vertical", zaxis.getName(),
                                         zaxis.getDescription(),
                                         zaxis.isNumeric(), vals );
    }
    else
      vertAxis = null;

    range = new WcsRangeField( this.getName(), this.getLabel(),
                               this.getDescription(), vertAxis );
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

  public WcsRangeField getRangeField() { return range; }

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
    Range zRange = null;
    try
    {
      zRange = verticalRange != null ? verticalRange.getRange( this.coordSys ) : null;
    }
    catch ( InvalidRangeException e )
    {
      log.error( "writeCoverageDataToFile(): Failed to subset coverage <" + this.coverage.getName() + "> along vertical range <" + verticalRange + ">: " + e.getMessage() );
      throw new WcsException( WcsException.Code.CoverageNotDefined, "Vertical", "Failed to subset coverage <" + this.coverage.getName() + "> along vertical range." );
    }

    //GridDatatype gridDatatype = this.coverage.getGridDatatype().makeSubset( );

    NetcdfCFWriter writer = new NetcdfCFWriter();
    try
    {
      this.coordSys.getVerticalAxis().isNumeric();
      writer.makeFile( ncFile.getPath(), this.dataset.getDataset(),
                       Collections.singletonList( this.coverage.getName() ),
                       bboxLatLonRect, 1,
                       zRange,
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
      {
        log.error( "VerticalRange(): Minimum <" + minimum + "> is greater than maximum <" + maximum + ">." );
        throw new IllegalArgumentException( "VerticalRange minimum <" + minimum + "> greater than maximum <" + maximum + ">." );
      }
      if ( stride < 1 )
      {
        log.error( "VerticalRange(): stride <" + stride + "> less than one (1 means all points)." );
        throw new IllegalArgumentException( "VerticalRange stride <" + stride + "> less than one (1 means all points)." );
      }
      this.min = minimum;
      this.max = maximum;
      this.stride = stride;
    }

    public double getMinimum() { return min; }
    public double getMaximum() { return max; }
    public int getStride() { return stride; }
    public String toString()
    {
      return "[min="+ min + ",max=" + max + ",stride=" + stride + "]";
    }

    public Range getRange( GridCoordSystem gcs )
            throws InvalidRangeException
    {
      if ( gcs == null )
      {
        log.error("getRange(): GridCoordSystem must be non-null.");
        throw new IllegalArgumentException( "GridCoordSystem must be non-null." );
      }
      CoordinateAxis1D vertAxis = gcs.getVerticalAxis();
      if ( vertAxis == null )
      {
        log.error( "getRange(): GridCoordSystem must have vertical axis." );
        throw new IllegalArgumentException( "GridCoordSystem must have vertical axis." );
      }
      if ( ! vertAxis.isNumeric())
      {
        log.error( "getRange(): GridCoordSystem must have numeric vertical axis to support min/max range." );
        throw new IllegalArgumentException( "GridCoordSystem must have numeric vertical axis to support min/max range." );
      }
      int minIndex = vertAxis.findCoordElement( min );
      int maxIndex = vertAxis.findCoordElement( max );
      if ( minIndex == -1 || maxIndex == -1 )
      {
        log.error( "getRange(): GridCoordSystem vertical axis does not contain min/max points." );
        throw new IllegalArgumentException( "GridCoordSystem vertical axis does not contain min/max points." );
      }

      if ( vertAxis.getPositive().equalsIgnoreCase( CoordinateAxis.POSITIVE_DOWN ) )
        return new Range( maxIndex, minIndex, stride );
      else
        return new Range( minIndex, maxIndex, stride );
    }
  }
}
