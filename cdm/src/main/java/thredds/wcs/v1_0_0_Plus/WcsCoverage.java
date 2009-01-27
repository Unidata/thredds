/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
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
import ucar.ma2.Array;

import java.util.*;
import java.io.File;
import java.io.IOException;

import ucar.nc2.units.DateRange;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.geotiff.GeotiffWriter;

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

  private List<WcsRequest.Format> supportedCoverageFormatList;

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

    this.supportedCoverageFormatList = new ArrayList<WcsRequest.Format>();
    //this.supportedCoverageFormatList = "application/x-netcdf";
    this.supportedCoverageFormatList.add( WcsRequest.Format.GeoTIFF);
    this.supportedCoverageFormatList.add( WcsRequest.Format.GeoTIFF_Float);
    this.supportedCoverageFormatList.add( WcsRequest.Format.NetCDF3);
  }

  GridDataset.Gridset getGridset() { return coverage; }

  public String getName() { return this.name; }
  public String getLabel() { return this.label; }
  public String getDescription() { return this.description; }
  public GridCoordSystem getCoordinateSystem() { return coordSys; }

  public String getDefaultRequestCrs() { return defaultRequestCrs; }
  public String getNativeCrs() { return nativeCRS; }
  public List<WcsRequest.Format> getSupportedCoverageFormatList()
  {
    return Collections.unmodifiableList( supportedCoverageFormatList );
  }
  public boolean isSupportedCoverageFormat( WcsRequest.Format covFormat)
  {
    return this.supportedCoverageFormatList.contains( covFormat );
  }

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

  public File writeCoverageDataToFile( WcsRequest.Format format, LatLonRect bboxLatLonRect, AxisSubset vertSubset, List<String> rangeSubset, DateRange timeRange)
          throws WcsException
  {
    boolean zRangeDone = false;
    boolean tRangeDone = false;

    try
    {
      // Get the height range.
      Range zRange = vertSubset != null ? vertSubset.getRange() : null;
      zRangeDone = true;

      // Get the time range.
      Range tRange = null;
      if ( timeRange != null )
      {
        CoordinateAxis1DTime timeAxis = this.coordSys.getTimeAxis1D();
        int startIndex = timeAxis.findTimeIndexFromDate( timeRange.getStart().getDate() );
        int endIndex = timeAxis.findTimeIndexFromDate( timeRange.getEnd().getDate() );
        tRange = new Range( startIndex, endIndex );
        tRangeDone = true;
      }

      if ( format == WcsRequest.Format.GeoTIFF || format == WcsRequest.Format.GeoTIFF_Float )
      {
        if ( rangeSubset.size() != 1 )
        {
          String msg = "GeoTIFF response encoding only available for single range field selection [" + rangeSubset + "].";
          log.error( "writeCoverageDataToFile(): " + msg );
          throw new WcsException( WcsException.Code.InvalidParameterValue, "RangeSubset", msg );
        }
        String reqRangeFieldName = rangeSubset.get( 0);

        File dir = new File( getDiskCache().getRootDirectory() );
        File tifFile = File.createTempFile( "WCS", ".tif", dir );
        if ( log.isDebugEnabled() )
          log.debug( "writeCoverageDataToFile(): tifFile=" + tifFile.getPath() );

        WcsRangeField rangeField = this.range.get( reqRangeFieldName );
        GridDatatype subset = rangeField.getGridDatatype()
                .makeSubset( tRange, zRange, bboxLatLonRect, 1, 1, 1 );
        Array data = subset.readDataSlice( 0, 0, -1, -1 );

        GeotiffWriter writer = new GeotiffWriter( tifFile.getPath() );
        writer.writeGrid( this.dataset.getDataset(), subset, data, format == WcsRequest.Format.GeoTIFF );

        writer.close();

        return tifFile;
      }
      else if ( format == WcsRequest.Format.NetCDF3 )
      {
        File dir = new File( getDiskCache().getRootDirectory() );
        File ncFile = File.createTempFile( "WCS", ".nc", dir );
        if ( log.isDebugEnabled() )
          log.debug( "writeCoverageDataToFile(): ncFile=" + ncFile.getPath() );

        //GridDatatype gridDatatype = this.coverage.getGridDatatype().makeSubset( );

        NetcdfCFWriter writer = new NetcdfCFWriter();
        this.coordSys.getVerticalAxis().isNumeric();
        writer.makeFile( ncFile.getPath(), this.dataset.getDataset(),
                         rangeSubset,
                         bboxLatLonRect, 1,
                         zRange,
                         timeRange, 1,
                         true );
        return ncFile;
      }
      else
      {
        log.error( "writeCoverageDataToFile(): Unsupported response encoding format [" + format + "]." );
        throw new WcsException( WcsException.Code.InvalidFormat, "Format", "Unsupported response encoding format [" + format + "]." );
      }
    }
    catch ( InvalidRangeException e )
    {
      String msg = "Failed to subset coverage [" + this.getName();
      if ( ! zRangeDone )
        msg += "] along vertical axis [" + vertSubset + "]. ";
      else if ( ! tRangeDone )
        msg += "] along time axis [" + timeRange + "]. ";
      else
        msg += "] in horizontal plane [" + bboxLatLonRect + "]. ";
      log.error( "writeCoverageDataToFile(): " + msg + e.getMessage() );
      throw new WcsException( WcsException.Code.CoverageNotDefined, "", msg );
    }
    catch ( IOException e )
    {
      log.error( "writeCoverageDataToFile(): Failed to write file for requested coverage <" + this.getName() + ">: " + e.getMessage() );
      throw new WcsException( WcsException.Code.UNKNOWN, "", "Problem creating coverage [" + this.getName() + "]." );
    }
  }

}
