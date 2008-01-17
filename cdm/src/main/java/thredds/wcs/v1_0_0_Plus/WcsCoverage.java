package thredds.wcs.v1_0_0_Plus;

import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.NetcdfCFWriter;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.ogc.EPSG_OGC_CF_Helper;
import ucar.ma2.Range;
import ucar.ma2.InvalidRangeException;

import java.util.*;
import java.io.File;
import java.io.IOException;

import ucar.nc2.units.DateRange;

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

  private GridDataset.Gridset coverage;
  private WcsDataset dataset;

  private String name;
  private String label;
  private String description;

  private GridCoordSystem coordSys;
  private String nativeCRS;

  private String defaultRequestCrs;

  private String allowedCoverageFormat;

  private HashMap<String, WcsRangeField> range;

  public WcsCoverage( GridDataset.Gridset coverage, WcsDataset dataset)
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
    this.coordSys = coverage.getGeoCoordSystem();
    if ( this.coordSys == null )
    {
      log.error( "WcsCoverage(): Coverage must have non-null coordinate system." );
      throw new IllegalArgumentException( "Non-null coordinate system required." );
    }

    this.name = this.coordSys.getName();
    this.label = this.coordSys.getName();

    this.range = new HashMap<String, WcsRangeField>();
    StringBuffer descripSB = new StringBuffer( "All parameters on the \"")
            .append( this.name).append( "\" coordinate system: ");
    for ( GridDatatype curField : this.coverage.getGrids() )
    {
      String stdName = curField.findAttValueIgnoreCase( "standard_name", "" );
      descripSB.append( stdName.equals( "" ) ? curField.getName() : stdName )
              .append( "," );

      WcsRangeField field = new WcsRangeField( curField );
      range.put( field.getName(), field );
    }
    descripSB.setCharAt( descripSB.length() - 1, '.' );
    this.description = descripSB.toString();

    this.nativeCRS = EPSG_OGC_CF_Helper.getWcs1_0CrsId( this.coordSys.getProjection() );

    this.defaultRequestCrs = "OGC:CRS84";

    this.allowedCoverageFormat = "application/x-netcdf";
  }

  GridDataset.Gridset getGridset() { return coverage; }

  public String getName() { return this.name; }
  public String getLabel() { return this.label; }
  public String getDescription() { return this.description; }
  public GridCoordSystem getCoordinateSystem() { return coordSys; }

  public String getDefaultRequestCrs() { return defaultRequestCrs; }
  public String getNativeCrs() { return nativeCRS; }
  public String getAllowedCoverageFormat() { return allowedCoverageFormat; }

  public boolean isRangeFieldName( String fieldName ) { return range.containsKey( fieldName ); }
  public Set<String> getRangeFieldNames() { return range.keySet(); }
  public Collection<WcsRangeField> getRange() { return range.values(); }

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

  public File writeCoverageDataToFile( LatLonRect bboxLatLonRect, AxisSubset vertSubset, List<String> rangeSubset, DateRange timeRange)
          throws WcsException
  {
    File ncFile = getDiskCache().getCacheFile( this.dataset.getDatasetPath() + "-" + this.getName() + ".nc" );
    Range zRange = null;
    try
    {
      zRange = vertSubset != null ? vertSubset.getRange() : null;
    }
    catch ( InvalidRangeException e )
    {
      log.error( "writeCoverageDataToFile(): Failed to subset coverage <" + this.getName() + "> along vertical range <" + vertSubset + ">: " + e.getMessage() );
      throw new WcsException( WcsException.Code.CoverageNotDefined, "BBOX", "Failed to subset coverage <" + this.getName() + "> along vertical range." );
    }

    //GridDatatype gridDatatype = this.coverage.getGridDatatype().makeSubset( );

    NetcdfCFWriter writer = new NetcdfCFWriter();
    try
    {
      this.coordSys.getVerticalAxis().isNumeric();
      writer.makeFile( ncFile.getPath(), this.dataset.getDataset(),
                       rangeSubset,
                       bboxLatLonRect, 1,
                       zRange,
                       timeRange, 1,
                       true );
    }
    catch ( InvalidRangeException e )
    {
      log.error( "writeCoverageDataToFile(): Failed to subset coverage <" + this.getName() + ">: " + e.getMessage() );
      throw new WcsException( WcsException.Code.CoverageNotDefined, "", "Failed to subset coverage <" + this.getName() + ">." );
    }
    catch ( IOException e )
    {
      log.error( "writeCoverageDataToFile(): Failed to write file for requested coverage <" + this.getName() + ">: " + e.getMessage() );
      throw new WcsException( WcsException.Code.UNKNOWN, "", "Problem creating coverage <" + this.getName() + ">." );
    }
    return ncFile;
  }
}
