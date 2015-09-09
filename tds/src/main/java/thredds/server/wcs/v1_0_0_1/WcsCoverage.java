/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.wcs.v1_0_0_1;

import thredds.server.wcs.Request;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.ft2.coverage.writer.CFGridCoverageWriter2;
import ucar.nc2.geotiff.GeotiffWriter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.NamedObject;
import ucar.nc2.util.Optional;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ogc.EPSG_OGC_CF_Helper;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WcsCoverage {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WcsCoverage.class);

  // ToDo WCS 1.0Plus - change FROM coverage for each parameter TO coverage for each coordinate system
  private WcsDataset wcsDataset;
  private CoverageDataset dataset;
  private Coverage coverage;
  private CoverageCoordSys coordSys;
  private String nativeCRS;

  private String defaultRequestCrs;

  private List<Request.Format> supportedCoverageFormatList;

  private WcsRangeField range;

  public WcsCoverage(@Nonnull Coverage coverage, @Nonnull CoverageCoordSys coordSys, @Nonnull WcsDataset wcsDataset) {
    this.wcsDataset = wcsDataset;
    this.coverage = coverage;
    this.coordSys = coordSys;

    dataset = wcsDataset.getDataset();
    ProjectionImpl proj = coordSys.getProjection();
    this.nativeCRS = EPSG_OGC_CF_Helper.getWcs1_0CrsId(proj);

    this.defaultRequestCrs = "OGC:CRS84";

    this.supportedCoverageFormatList = new ArrayList<>();
    //this.supportedCoverageFormatList.add("application/x-netcdf");
    this.supportedCoverageFormatList.add(Request.Format.GeoTIFF);
    this.supportedCoverageFormatList.add(Request.Format.GeoTIFF_Float);
    this.supportedCoverageFormatList.add(Request.Format.NetCDF3);

    CoverageCoordAxis zaxis = coordSys.getZAxis();
    WcsRangeField.Axis vertAxis;
    if (zaxis != null && zaxis.getDependenceType() == CoverageCoordAxis.DependenceType.independent) {
      List<String> names = new ArrayList<>();
      for (NamedObject val :  ((CoverageCoordAxis1D)zaxis).getCoordValueNames())
        names.add(val.getName());
      vertAxis = new WcsRangeField.Axis("Vertical", zaxis.getName(), zaxis.getDescription(), true, names);
    } else
      vertAxis = null;

    range = new WcsRangeField(this.getName(), this.getLabel(),
            this.getDescription(), vertAxis);
  }


  public String getName() {
    return coverage.getName();
  }

  public String getLabel() {
    return coverage.getDescription();
  }

  public String getDescription() {
    return coverage.getDescription();
  }

  public CoverageCoordSys getCoordinateSystem() {
    return coordSys;
  }

  public boolean hasMissingData() {
    return coverage.hasMissing();
  }

  public String getDefaultRequestCrs() {
    return defaultRequestCrs;
  }

  public String getNativeCrs() {
    return nativeCRS;
  }

  public List<Request.Format> getSupportedCoverageFormatList() {
    return supportedCoverageFormatList;
  }

  public boolean isSupportedCoverageFormat(Request.Format covFormat) {
    return supportedCoverageFormatList.contains(covFormat);
  }

  public WcsRangeField getRangeField() {
    return range;
  }

  /* public Range getRangeSetAxisRange(double minValue, double maxValue) {
    if (minValue > maxValue) {
      log.error("getRangeSetAxisRange(): Min is greater than max <" + minValue + ", " + maxValue + ">.");
      throw new IllegalArgumentException("Min is greater than max <" + minValue + ", " + maxValue + ">.");
    }
    GridCoordAxis zaxis = dataset.getZAxis(coordSys);
    if (zaxis != null) {
      int minIndex = zaxis.findCoordElement(minValue);
      int maxIndex = zaxis.findCoordElement(maxValue);

      if (minIndex == -1 || maxIndex == -1)
        return null;

      try {
        return new Range(minIndex, maxIndex);
      } catch (InvalidRangeException e) {
        return null;
      }
    } else
      return null;
  } */

  static private DiskCache2 diskCache = null;

  static public void setDiskCache(DiskCache2 _diskCache) {
    diskCache = _diskCache;
  }

  static private DiskCache2 getDiskCache() {
    if (diskCache == null) {
      log.error("getDiskCache(): Disk cache has not been set.");
      throw new IllegalStateException("Disk cache must be set before calling GetCoverage.getDiskCache().");
    }
    return diskCache;
  }

  public File writeCoverageDataToFile(Request.Format format, LatLonRect bboxLatLonRect, VerticalRange verticalRange, CalendarDateRange timeRange) throws WcsException {
    /* Get the height range.
    Range zRange = null;
    try {
      zRange = verticalRange != null ? verticalRange.getRange(this.coordSys) : null;
    } catch (InvalidRangeException e) {
      log.error("writeCoverageDataToFile(): Failed to subset coverage <" + this.coverage.getName() + "> along vertical range <" + verticalRange + ">: " + e.getMessage());
      throw new WcsException(WcsException.Code.CoverageNotDefined, "Vertical", "Failed to subset coverage [" + this.coverage.getName() + "] along vertical range.");
    }

    // Get the time range.
    Range tRange = null;
    if (timeRange != null) {
      GridCoordAxisTime timeAxis = dataset.getTimeAxis(coordSys);
      int startIndex = timeAxis.findTimeIndexFromCalendarDate(timeRange.getStart());
      int endIndex = timeAxis.findTimeIndexFromCalendarDate(timeRange.getEnd());

      if (startIndex < 0 || startIndex > timeAxis.getSize() - 1 || endIndex < 0 || endIndex > timeAxis.getSize() - 1) {
        CalendarDateRange cdr = timeAxis.getCalendarDateRange();
        String availStart = cdr.getStart().toString();
        String availEnd = cdr.getEnd().toString();
        String msg = "Requested temporal range [" + timeRange.getStart() + " - " + timeRange.getEnd()
                + "] not in available range [" + availStart + " - " + availEnd + "].";
        log.debug("writeCoverageDataToFile(): " + msg);
        throw new WcsException(WcsException.Code.CoverageNotDefined, "Time", msg);
      }

      try {
        tRange = new Range(startIndex, endIndex);
      } catch (InvalidRangeException e) {
        log.error("writeCoverageDataToFile(): Failed to subset coverage [" + this.coverage.getName() + "] along time axis [" + timeRange + "]: " + e.getMessage());
        throw new WcsException(WcsException.Code.CoverageNotDefined, "Time", "Failed to subset coverage [" + this.coverage.getName() + "] along time axis [" + timeRange + "].");
      }
    } */

    /////////
    try {
      if (format == Request.Format.GeoTIFF || format == Request.Format.GeoTIFF_Float) {
        File dir = new File(getDiskCache().getRootDirectory());
        File tifFile = File.createTempFile("WCS", ".tif", dir);
        if (log.isDebugEnabled())
          log.debug("writeCoverageDataToFile(): tifFile=" + tifFile.getPath());

        try {
          //GridCoverage subset = this.coverage.makeSubset(tRange, zRange, bboxLatLonRect, 1, 1, 1);  // LOOK do you need to subset it?
          GeoReferencedArray array = coverage.readData(new SubsetParams());

          GeotiffWriter writer = new GeotiffWriter(tifFile.getPath());
          writer.writeGrid(array, format == Request.Format.GeoTIFF);

          writer.close();
          //} catch (InvalidRangeException e) {
        //  log.error("writeCoverageDataToFile(): Failed to subset coverage <" + this.coverage.getName() + "> along time axis <" + timeRange + ">: " + e.getMessage());
       //   throw new WcsException(WcsException.Code.CoverageNotDefined, "", "Failed to subset coverage [" + this.coverage.getName() + "].");
        } catch (Throwable e) {
          log.error("writeCoverageDataToFile(): Failed to write file for requested coverage <" + this.coverage.getName() + ">: ", e);
          throw new WcsException(WcsException.Code.UNKNOWN, "", "Problem creating coverage [" + this.coverage.getName() + "].");
        }

        return tifFile;

      } else if (format == Request.Format.NetCDF3) {
        File dir = new File(getDiskCache().getRootDirectory());
        File outFile = File.createTempFile("WCS", ".nc", dir);
        if (log.isDebugEnabled())
          log.debug("writeCoverageDataToFile(): ncFile=" + outFile.getPath());

        SubsetParams subset = new SubsetParams();
        NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, outFile.getAbsolutePath());
        // LOOK could test file size
        Optional<Long> estimatedSizeo = CFGridCoverageWriter2.writeOrTestSize(this.wcsDataset.getDataset(), Collections.singletonList(this.coverage.getName()),
                subset, true, false, writer);
        if (!estimatedSizeo.isPresent())
          throw new InvalidRangeException("Request contains no data: " + estimatedSizeo.getErrorMessage());

        return outFile;

      } else {
        log.error("writeCoverageDataToFile(): Unsupported response encoding format [" + format + "].");
        throw new WcsException(WcsException.Code.InvalidFormat, "Format", "Unsupported response encoding format [" + format + "].");
      }
    } catch (InvalidRangeException e) {
      log.error("writeCoverageDataToFile(): Failed to subset coverage <" + this.coverage.getName() + ">: " + e.getMessage());
      throw new WcsException(WcsException.Code.CoverageNotDefined, "", "Failed to subset coverage [" + this.coverage.getName() + "].");
    } catch (IOException e) {
      log.error("writeCoverageDataToFile(): Failed to create or write temporary file for requested coverage <" + this.coverage.getName() + ">: " + e.getMessage());
      throw new WcsException(WcsException.Code.UNKNOWN, "", "Problem creating coverage [" + this.coverage.getName() + "].");
    }
  }

  public static class VerticalRange {
    private double min, max;
    private int stride;
    private boolean singlePoint = false;

    public VerticalRange(double point, int stride) {
      this(point, point, stride);
      this.singlePoint = true;

    }

    public VerticalRange(double minimum, double maximum, int stride) {
      if (minimum > maximum) {
        log.error("VerticalRange(): Minimum <" + minimum + "> is greater than maximum <" + maximum + ">.");
        throw new IllegalArgumentException("VerticalRange minimum <" + minimum + "> greater than maximum <" + maximum + ">.");
      }
      if (stride < 1) {
        log.error("VerticalRange(): stride <" + stride + "> less than one (1 means all points).");
        throw new IllegalArgumentException("VerticalRange stride <" + stride + "> less than one (1 means all points).");
      }
      this.min = minimum;
      this.max = maximum;
      this.stride = stride;
    }

    public double getMinimum() {
      return min;
    }

    public double getMaximum() {
      return max;
    }

    public int getStride() {
      return stride;
    }

    public boolean isSinglePoint() {
      return singlePoint;
    }

    public String toString() {
      return "[min=" + min + ",max=" + max + ",stride=" + stride + "]";
    }

    /* public Range getRange(GridCoordSys gcs) throws InvalidRangeException {
      if (gcs == null) {
        log.error("getRange(): GridCoordSystem must be non-null.");
        throw new IllegalArgumentException("GridCoordSystem must be non-null.");
      }
      CoordinateAxis1D vertAxis = gcs.getVerticalAxis();
      if (vertAxis == null) {
        log.error("getRange(): GridCoordSystem must have vertical axis.");
        throw new IllegalArgumentException("GridCoordSystem must have vertical axis.");
      }
      if (!vertAxis.isNumeric()) {
        log.error("getRange(): GridCoordSystem must have numeric vertical axis to support min/max range.");
        throw new IllegalArgumentException("GridCoordSystem must have numeric vertical axis to support min/max range.");
      }
      int minIndex = vertAxis.findCoordElement(min);
      int maxIndex = vertAxis.findCoordElement(max);
      if (minIndex == -1 || maxIndex == -1) {
        log.error("getRange(): GridCoordSystem vertical axis does not contain min/max points.");
        throw new IllegalArgumentException("GridCoordSystem vertical axis does not contain min/max points.");
      }

      if (vertAxis.getPositive().equalsIgnoreCase(ucar.nc2.constants.CF.POSITIVE_DOWN))
        return new Range(maxIndex, minIndex, stride);
      else
        return new Range(minIndex, maxIndex, stride);
    } */
  }
}
