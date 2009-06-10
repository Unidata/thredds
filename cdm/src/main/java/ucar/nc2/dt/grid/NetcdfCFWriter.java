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
package ucar.nc2.dt.grid;

import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPointImpl;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateRange;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.transform.AbstractCoordTransBuilder;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.ma2.Range;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.DataType;
import ucar.ma2.Array;

import java.io.IOException;
import java.util.*;
import java.text.ParseException;

/**
 * Write a CF compliant Netcdf-3 file from any gridded dataset. The datasets can optionally be subsetted by a lat/lon
 * bounding box and/or a time range.
 *
 * @author caron
 */
public class NetcdfCFWriter {
  static private long maxSize = 2 * 1000 * 1000 * 1000; // 2 Gb
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfCFWriter.class);


  /**
   * Write a CF compliant Netcdf-3 file from any gridded dataset.
   *
   * @param location    write to this location on disk
   * @param gds         A gridded dataset
   * @param gridList    the list of grid names to be written, must not be empty. Full name (not short).
   * @param llbb        optional lat/lon bounding box
   * @param range       optional time range
   * @param addLatLon   should 2D lat/lon variables be added, if its a projection coordainte system?
   * @param horizStride x,y stride
   * @param stride_z    not implemented yet
   * @param stride_time not implemented yet
   * @throws IOException           if write or read error
   * @throws InvalidRangeException if subset is illegal
   */
  public void makeFile(String location, ucar.nc2.dt.GridDataset gds, List<String> gridList,
          LatLonRect llbb, DateRange range,
          boolean addLatLon,
          int horizStride, int stride_z, int stride_time)
          throws IOException, InvalidRangeException {
    makeFile(location, gds, gridList, llbb, horizStride, null, range, stride_time, addLatLon);
  }

  public void makeFile(String location, ucar.nc2.dt.GridDataset gds, List<String> gridList,
          LatLonRect llbb, int horizStride,
          Range zRange,
          DateRange dateRange, int stride_time,
          boolean addLatLon)
          throws IOException, InvalidRangeException {

    FileWriter writer = new FileWriter(location, false);
    NetcdfDataset ncd = (NetcdfDataset) gds.getNetcdfFile();

    // global attributes
    for (Attribute att : gds.getGlobalAttributes())
      writer.writeGlobalAttribute(att);

    writer.writeGlobalAttribute(new Attribute("Conventions", "CF-1.0"));
    writer.writeGlobalAttribute(new Attribute("History",
            "Translated to CF-1.0 Conventions by Netcdf-Java CDM (NetcdfCFWriter)\n" +
                    "Original Dataset = " + gds.getLocationURI() + "; Translation Date = " + new Date()));

    ArrayList<Variable> varList = new ArrayList<Variable>();
    ArrayList<String> varNameList = new ArrayList<String>();
    ArrayList<CoordinateAxis> axisList = new ArrayList<CoordinateAxis>();

    // add each desired Grid to the new file
    long total_size = 0;
    for (String gridName : gridList) {
      if (varNameList.contains(gridName))
        continue;

      varNameList.add(gridName);

      GridDatatype grid = gds.findGridDatatype(gridName);
      GridCoordSystem gcsOrg = grid.getCoordinateSystem();
      CoordinateAxis1DTime timeAxis = gcsOrg.getTimeAxis1D();

      // make subset if needed
      Range timeRange = null;
      if ((dateRange != null) && (timeAxis != null)) {
        int startIndex = timeAxis.findTimeIndexFromDate(dateRange.getStart().getDate());
        int endIndex = timeAxis.findTimeIndexFromDate(dateRange.getEnd().getDate());
        if (startIndex < 0)
          throw new InvalidRangeException("start time=" + dateRange.getStart().getDate() + " must be >= " + timeAxis.getTimeDate(0));
        if (endIndex < 0)
          throw new InvalidRangeException("end time=" + dateRange.getEnd().getDate() + " must be >= " + timeAxis.getTimeDate(0));
        timeRange = new Range(startIndex, endIndex);
      }

      if ((null != timeRange) || (zRange != null) || (llbb != null) || (horizStride > 1)) {
        grid = grid.makeSubset(timeRange, zRange, llbb, 1, horizStride, horizStride);
      }

      Variable gridV = (Variable) grid.getVariable();
      varList.add(gridV);
      total_size += gridV.getSize() * gridV.getElementSize();

      // add coordinate axes
      GridCoordSystem gcs = grid.getCoordinateSystem();
      for (CoordinateAxis axis : gcs.getCoordinateAxes()) {
        if (!varNameList.contains(axis.getName())) {
          varNameList.add(axis.getName());
          varList.add(axis); // LOOK dont we have to subset these ??
          axisList.add(axis);
        }
      }

      // add coordinate transform variables
      for (CoordinateTransform ct : gcs.getCoordinateTransforms()) {
        Variable v = ncd.findVariable(ct.getName());
        if (!varNameList.contains(ct.getName()) && (null != v)) {
          varNameList.add(ct.getName());
          varList.add(v);
        }
      }

      // optional lat/lon
      if (addLatLon) {
        Projection proj = gcs.getProjection();
        if ((null != proj) && !(proj instanceof LatLonProjection)) {
          addLatLon2D(ncd, varList, proj, gcs.getXHorizAxis(), gcs.getYHorizAxis());
          addLatLon = false;
        }
      }
    }

    // check size is ok
    if (total_size > maxSize) {
      log.info("Reject request size = {} Mbytes", total_size);
      throw new IllegalArgumentException("Request too big=" + total_size+" Mbytes, max="+maxSize);
    }

    writer.writeVariables(varList);

    // now add CF annotations as needed - dont change original ncd or gds
    NetcdfFileWriteable ncfile = writer.getNetcdf();
    Group root = ncfile.getRootGroup();
    for (String gridName : gridList) {
      GridDatatype grid = gds.findGridDatatype(gridName);
      Variable newV = root.findVariable(gridName);
      if (newV == null) {
        log.warn("NetcdfCFWriter cant find "+gridName+" in gds "+gds.getLocationURI());
        continue;
      }

      // annotate Variable for CF
      StringBuilder sbuff = new StringBuilder();
      GridCoordSystem gcs = grid.getCoordinateSystem();
      for (Variable axis : gcs.getCoordinateAxes()) {
        sbuff.append(axis.getName()).append(" ");
      }
      if (addLatLon)
        sbuff.append("lat lon");
      newV.addAttribute(new Attribute("coordinates", sbuff.toString()));

      // looking for coordinate transform variables
      for (CoordinateTransform ct : gcs.getCoordinateTransforms()) {
        Variable v = ncd.findVariable(ct.getName());
        if (ct.getTransformType() == TransformType.Projection)
          newV.addAttribute(new Attribute("grid_mapping", v.getName()));
      }
    }

    for (CoordinateAxis axis : axisList) {
      Variable newV = root.findVariable(axis.getShortName()); // LOOK ???
      if ((axis.getAxisType() == AxisType.Height) || (axis.getAxisType() == AxisType.Pressure) || (axis.getAxisType() == AxisType.GeoZ)) {
        if (null != axis.getPositive())
          newV.addAttribute(new Attribute("positive", axis.getPositive()));
      }
      if (axis.getAxisType() == AxisType.Lat) {
        newV.addAttribute(new Attribute("units", "degrees_north"));
        newV.addAttribute(new Attribute("standard_name", "latitude"));
      }
      if (axis.getAxisType() == AxisType.Lon) {
        newV.addAttribute(new Attribute("units", "degrees_east"));
        newV.addAttribute(new Attribute("standard_name", "longitude"));
      }
      if (axis.getAxisType() == AxisType.GeoX) {
        newV.addAttribute(new Attribute("standard_name", "projection_x_coordinate"));
      }
      if (axis.getAxisType() == AxisType.GeoY) {
        newV.addAttribute(new Attribute("standard_name", "projection_y_coordinate"));
      }
    }

    // coordinate transform variables : must convert false easting, northing to km
    List<Variable> ctvList = new ArrayList<Variable>();
    for (ucar.nc2.dt.GridDataset.Gridset gridSet : gds.getGridsets()) {
      ucar.nc2.dt.GridCoordSystem gcs = gridSet.getGeoCoordSystem();
      ProjectionCT pct = gcs.getProjectionCT();
      if (pct != null) {
        Variable v = root.findVariable(pct.getName()); // look for the ctv
        if ((v != null) && !ctvList.contains(v)) {
          convertProjectionCTV((NetcdfDataset) gds.getNetcdfFile(), v);
          ctvList.add(v);
        }
      }
    }

    // LOOK not dealing with crossing the seam

    writer.finish(); // this writes the data to the new file.
  }

  private void convertProjectionCTV(NetcdfDataset ds, Variable ctv) {
    Attribute att = ctv.findAttribute(_Coordinate.TransformType);
    if ((null != att) && att.getStringValue().equals("Projection")) {
      Attribute east = ctv.findAttribute("false_easting");
      Attribute north = ctv.findAttribute("false_northing");
      if ((null != east) || (null != north)) {
        double scalef = AbstractCoordTransBuilder.getFalseEastingScaleFactor(ds, ctv);
        if (scalef != 1.0) {
          convertAttribute(ctv, east, scalef);
          convertAttribute(ctv, north, scalef);
        }
      }
    }
  }

  private void convertAttribute(Variable ctv, Attribute att, double scalef) {
    if (att == null) return;
    double val = scalef * att.getNumericValue().doubleValue();
    ctv.addAttribute(new Attribute(att.getName(), val));
  }

  private void addLatLon2D(NetcdfFile ncfile, List<Variable> varList, Projection proj,
          CoordinateAxis xaxis, CoordinateAxis yaxis) throws IOException {

    double[] xData = (double[]) xaxis.read().get1DJavaArray(double.class);
    double[] yData = (double[]) yaxis.read().get1DJavaArray(double.class);

    List<Dimension> dims = new ArrayList<Dimension>();
    dims.add(yaxis.getDimension(0));
    dims.add(xaxis.getDimension(0));

    Variable latVar = new Variable(ncfile, null, null, "lat");
    latVar.setDataType(DataType.DOUBLE);
    latVar.setDimensions(dims);
    latVar.addAttribute(new Attribute("units", "degrees_north"));
    latVar.addAttribute(new Attribute("long_name", "latitude coordinate"));
    latVar.addAttribute(new Attribute("standard_name", "latitude"));
    latVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));

    Variable lonVar = new Variable(ncfile, null, null, "lon");
    lonVar.setDataType(DataType.DOUBLE);
    lonVar.setDimensions(dims);
    lonVar.addAttribute(new Attribute("units", "degrees_east"));
    lonVar.addAttribute(new Attribute("long_name", "longitude coordinate"));
    lonVar.addAttribute(new Attribute("standard_name", "longitude"));
    lonVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));

    int nx = xData.length;
    int ny = yData.length;

    // create the data
    ProjectionPointImpl projPoint = new ProjectionPointImpl();
    LatLonPointImpl latlonPoint = new LatLonPointImpl();
    double[] latData = new double[nx * ny];
    double[] lonData = new double[nx * ny];
    for (int i = 0; i < ny; i++) {
      for (int j = 0; j < nx; j++) {
        projPoint.setLocation(xData[j], yData[i]);
        proj.projToLatLon(projPoint, latlonPoint);
        latData[i * nx + j] = latlonPoint.getLatitude();
        lonData[i * nx + j] = latlonPoint.getLongitude();
      }
    }
    Array latDataArray = Array.factory(DataType.DOUBLE, new int[]{ny, nx}, latData);
    latVar.setCachedData(latDataArray, false);

    Array lonDataArray = Array.factory(DataType.DOUBLE, new int[]{ny, nx}, lonData);
    lonVar.setCachedData(lonDataArray, false);

    varList.add(latVar);
    varList.add(lonVar);
  }

  public static void test1() throws IOException, InvalidRangeException, ParseException {
    String fileIn = "C:/data/ncmodels/NAM_CONUS_80km_20051206_0000.nc";
    String fileOut = "C:/temp/cf3.nc";

    ucar.nc2.dt.GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(fileIn);

    NetcdfCFWriter writer = new NetcdfCFWriter();

    List<String> gridList = new ArrayList<String>();
    gridList.add("RH");
    gridList.add("T");

    DateFormatter format = new DateFormatter();
    Date start = format.isoDateTimeFormat("2005-12-06T18:00:00Z");
    Date end = format.isoDateTimeFormat("2005-12-07T18:00:00Z");

    writer.makeFile(fileOut, gds, gridList,
            new LatLonRect(new LatLonPointImpl(37, -109), 400, 7),
            new DateRange(start, end),
            true,
            1, 1, 1);

  }

  public static void main(String args[]) throws IOException, InvalidRangeException, ParseException {
    String fileIn = "dods://motherlode.ucar.edu/repository/entry/show/output:data.opendap/entryid:c41a3a26-57e5-4b15-b8b1-a8762b6f02c7/dodsC/entry";
    String fileOut = "C:/temp/testCF.nc";

    ucar.nc2.dt.GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(fileIn);

    NetcdfCFWriter writer = new NetcdfCFWriter();

    List<String> gridList = new ArrayList<String>();
    gridList.add("Z_sfc");

    DateFormatter format = new DateFormatter();
    Date start = format.isoDateTimeFormat("2003-06-01T03:00:00Z");
    Date end = format.isoDateTimeFormat("2004-01-01T00:00:00Z");

    writer.makeFile(fileOut, gds, gridList, null,
            // new LatLonRect(new LatLonPointImpl(30, -109), 10, 50),
            new DateRange(start, end),
            true,
            1, 1, 1);

  }


}

