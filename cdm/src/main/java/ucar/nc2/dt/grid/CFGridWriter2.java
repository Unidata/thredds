/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.dt.grid;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.*;
import ucar.nc2.constants.*;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.transform.AbstractCoordTransBuilder;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Write CF compliant gridded data
 * version 2
 *
 * @author caron
 * @since 6/18/2014
 */
public class CFGridWriter2 {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CFGridWriter2.class);

  /**
   * Compute the size of the file without writing
   *
   * @param gds              the GridDataset
   * @param gridList         the list of variables to be written, or null for all
   * @param llbb             the lat/lon bounding box, or null for all
   * @param projRect         the projection bounding box, or null for all
   * @param horizStride      the x and y stride
   * @param zRange           the z stride
   * @param dateRange        date range, or null for all
   * @param stride_time      the time stride
   * @param addLatLon        add 2D lat/lon coordinates if needed
   * @return total bytes written
   * @throws IOException
   * @throws InvalidRangeException
   */
  static public long makeSizeEstimate(ucar.nc2.dt.GridDataset gds, List<String> gridList,
                                       LatLonRect llbb, ProjectionRect projRect, int horizStride, Range zRange,
                                       CalendarDateRange dateRange, int stride_time, boolean addLatLon) throws IOException, InvalidRangeException {

    CFGridWriter2 writer2 = new CFGridWriter2();
    return writer2.writeOrTestSize(gds, gridList, llbb, projRect, horizStride, zRange, dateRange, stride_time, addLatLon, true, null);
  }

  /**
   * Write a netcdf/CF file from a GridDataset
   *
   * @param gds              the GridDataset
   * @param gridList         the list of variables to be written, or null for all
   * @param llbb             the lat/lon bounding box, or null for all
   * @param projRect         the projection bounding box, or null for all
   * @param horizStride      the x and y stride
   * @param zRange           the z stride
   * @param dateRange        date range, or null for all
   * @param stride_time      the time stride
   * @param addLatLon        add 2D lat/lon coordinates if needed
   * @param writer           this does the actual writing
   * @return total bytes written
   * @throws IOException
   * @throws InvalidRangeException
   */
  static public long writeFile(ucar.nc2.dt.GridDataset gds, List<String> gridList,
                                       LatLonRect llbb, ProjectionRect projRect, int horizStride, Range zRange,
                                       CalendarDateRange dateRange, int stride_time, boolean addLatLon, NetcdfFileWriter writer) throws IOException, InvalidRangeException {

    CFGridWriter2 writer2 = new CFGridWriter2();
    return writer2.writeOrTestSize(gds, gridList, llbb, projRect, horizStride, zRange, dateRange, stride_time, addLatLon, false, writer);
  }

  /**
   *
   * @param gds              the GridDataset
   * @param gridList         the list of variables to be written, or null for all
   * @param llbb             the lat/lon bounding box, or null for all
   * @param projRect         the projection bounding box, or null for all
   * @param horizStride      the x and y stride
   * @param zRange           the z stride
   * @param dateRange        date range, or null for all
   * @param stride_time      the time stride
   * @param addLatLon        add 2D lat/lon coordinates if needed
   * @param testSizeOnly     dont write, just return size
   * @param writer           this does the actual writing
   * @return total bytes written
   * @throws IOException
   * @throws InvalidRangeException
   */
  private long writeOrTestSize(ucar.nc2.dt.GridDataset gds, List<String> gridList,
                              LatLonRect llbb, ProjectionRect projRect, int horizStride, Range zRange,
                              CalendarDateRange dateRange, int stride_time,
                              boolean addLatLon, boolean testSizeOnly,
                              NetcdfFileWriter writer) throws IOException, InvalidRangeException {

    NetcdfDataset ncd = (NetcdfDataset) gds.getNetcdfFile();

    List<Variable> varList = new ArrayList<>();         // could make these Sets
    List<String> varNameList = new ArrayList<>();
    List<CoordinateAxis> axisList = new ArrayList<>();

    if (gridList == null) {   // want all of them
      gridList = new ArrayList<>();
      for (GridDatatype grid : gds.getGrids())
        gridList.add(grid.getName());
    }

    LatLonRect resultBB = null;
    long total_size = 0;

    // add each desired Grid to the new file   LOOK Might be better to group by coordSys
    for (String gridName : gridList) {
      if (varNameList.contains(gridName))  // already contains
        continue;
      GridDatatype gridOrg = gds.findGridDatatype(gridName);
      if (gridOrg == null) {
        log.debug("writeOrTestSize cant find grid %s - skipping%n", gridName);
        continue;
      }

      GridCoordSystem gcsOrg = gridOrg.getCoordinateSystem();
      // boolean isGlobal = gcsOrg.isGlobalLon();
      varNameList.add(gridName);

      // make subset as needed
      Range timeRange = makeTimeRange(dateRange, gcsOrg.getTimeAxis1D(), stride_time);
      Range zRangeUse = makeVerticalRange(zRange, gcsOrg.getVerticalAxis());             // LOOK no vert stride

      GridDatatype gridWant;
      List<Range> yxRanges = new ArrayList<>(2);
      if (projRect != null) {
        makeHorizRange(gcsOrg, projRect, horizStride, yxRanges);
        gridWant = gridOrg.makeSubset(null, null, timeRange, zRangeUse, yxRanges.get(0), yxRanges.get(1));

      } else {

        if (llbb == null) {
          yxRanges.add(null);
          yxRanges.add(null);
        } else {
          yxRanges = gcsOrg.getRangesFromLatLonRect(llbb);
        }

        if ((null != timeRange) || (zRangeUse != null) || (llbb != null) || (horizStride > 1)) {
          gridWant = gridOrg.makeSubset(timeRange, zRangeUse, llbb, 1, horizStride, horizStride);
        } else
          gridWant = gridOrg;
      }

      GridCoordSystem gcsWant = gridWant.getCoordinateSystem();
      LatLonRect gridBB = gcsWant.getLatLonBoundingBox();
      if (resultBB == null)
        resultBB = gridBB;
      else
        resultBB.extend(gridBB);

      Variable gridV = gridWant.getVariable(); // LOOK  WTF ??
      varList.add(gridV);
      total_size += gridV.getSize() * gridV.getElementSize();

      // add coordinate axes
      addCoordinateAxis(gcsWant, varNameList, varList, axisList);

      // add coordinate transform variables
      addCoordinateTransform(gcsWant, ncd, varNameList, varList);

      //Add Variables from the formula_terms
      total_size += processTransformationVars(varList, varNameList, ncd, gds, gridWant, timeRange, zRangeUse, yxRanges.get(0), yxRanges.get(1), horizStride, horizStride);

      // optional lat/lon
      if (addLatLon) {
        Projection proj = gcsWant.getProjection();
        if ((null != proj) && !(proj instanceof LatLonProjection)) {
          total_size += addLatLon2D(ncd, varList, proj, gcsWant.getXHorizAxis(), gcsWant.getYHorizAxis());
          addLatLon = false; // ??
        }
      }
    }

    if (testSizeOnly)
      return total_size;

    ////////////////////////////////////////////////////////////////////
    // start writing here

    // check size is ok
    boolean isLargeFile = isLargeFile(total_size);
    writer.setLargeFile(isLargeFile);
    if (resultBB != null)
      addGlobalAttributes(writer, gds, resultBB);

    // use fileWriter to copy the variables
    FileWriter2 fileWriter = new FileWriter2(writer);
    for (Variable v : varList)
      fileWriter.addVariable(v);

    addCFAnnotations(writer, gds, gridList, ncd, axisList, addLatLon);

    writer.create();

    // use fileWriter to copy the data
    fileWriter.copyVarData(varList, null, null);
    //updateGeospatialRanges(writer, llrect );
    writer.close();

    // this writes the data to the new file.
    return total_size; // ok
  }

  private boolean isLargeFile(long total_size) {
    boolean isLargeFile = false;
    long maxSize = 2 * 1000 * 1000 * 1000;  // LOOK why not use exact
    if (total_size > maxSize) {
      log.debug("Request size = {} Mbytes", total_size / 1000 / 1000);
      isLargeFile = true;
    }
    return isLargeFile;
  }

  private void makeHorizRange(GridCoordSystem gcsOrg, ProjectionRect projRect, int horizStride, List<Range> yxRanges) throws InvalidRangeException {
    if (gcsOrg.getXHorizAxis().getRank() > 1 || gcsOrg.getYHorizAxis().getRank() > 1) {
      throw new IllegalArgumentException("Coordinate systems with 2D horizontal axis are not supported");
    }

    CoordinateAxis1D xAxis = (CoordinateAxis1D) gcsOrg.getXHorizAxis();
    double[] xCoords = xAxis.getCoordValues();

    CoordinateAxis1D yAxis = (CoordinateAxis1D) gcsOrg.getYHorizAxis();
    double[] yCoords = yAxis.getCoordValues();


    ProjectionRect fullBB = new ProjectionRect(xCoords[0], yCoords[0], xCoords[xCoords.length - 1], yCoords[yCoords.length - 1]);

    if (!projRect.intersects(fullBB)) {
      throw new InvalidRangeException("BBOX must intersect grid BBOX, minx=" + xCoords[0] + ", miny=" + yCoords[0] + ", maxx=" + xCoords[xCoords.length - 1] + ", maxy=" + yCoords[yCoords.length - 1]);
    }

    ProjectionRect.intersect(fullBB, projRect, projRect);

    ProjectionPoint lowerLeft = projRect.getLowerLeftPoint();
    ProjectionPoint upperRigth = projRect.getUpperRightPoint();
    double minX = lowerLeft.getX();
    double minY = lowerLeft.getY();
    double maxX = upperRigth.getX();
    double maxY = upperRigth.getY();

    int minY_idx = yAxis.findCoordElement(minY);
    int maxY_idx = yAxis.findCoordElement(maxY);

    // When yAxis is increasing, "minY_idx < maxY_idx == true".
    // When yAxis is decreasing, "minY_idx > maxY_idx == true".
    // To handle both cases, we need to use the min() and max() functions.
    Range yRange = new Range(Math.min(minY_idx, maxY_idx), Math.max(minY_idx, maxY_idx), horizStride);

    int minX_idx = xAxis.findCoordElement(minX);
    int maxX_idx = xAxis.findCoordElement(maxX);
    Range xRange = new Range(Math.min(minX_idx, maxX_idx), Math.max(minX_idx, maxX_idx), horizStride);

    yxRanges.add(0, yRange);
    yxRanges.add(1, xRange);
  }

  private void addGlobalAttributes(NetcdfFileWriter writer, ucar.nc2.dt.GridDataset gds, LatLonRect llbb) {
    // global attributes
    for (Attribute att : gds.getGlobalAttributes()) {
      if (att.getShortName().equals(CDM.FILE_FORMAT)) continue;
      if (att.getShortName().equals(_Coordinate._CoordSysBuilder)) continue;
      writer.addGroupAttribute(null, att);
    }

    Attribute att = gds.findGlobalAttributeIgnoreCase(CDM.CONVENTIONS);
    if (att == null || !att.getStringValue().startsWith("CF-"))  // preserve previous version of CF Convention if it exists
      writer.addGroupAttribute(null, new Attribute(CDM.CONVENTIONS, "CF-1.0"));

    writer.addGroupAttribute(null, new Attribute("History",
            "Translated to CF-1.0 Conventions by Netcdf-Java CDM (CFGridWriter2)\n" +
                    "Original Dataset = " + gds.getLocation() + "; Translation Date = " + CalendarDate.present()));

    // this will replace any existing
    writer.addGroupAttribute(null, new Attribute(ACDD.LAT_MIN, llbb.getLatMin()));
    writer.addGroupAttribute(null, new Attribute(ACDD.LAT_MAX, llbb.getLatMax()));
    writer.addGroupAttribute(null, new Attribute(ACDD.LON_MIN, llbb.getLonMin()));
    writer.addGroupAttribute(null, new Attribute(ACDD.LON_MAX, llbb.getLonMax()));
  }

  private void addCFAnnotations(NetcdfFileWriter writer, ucar.nc2.dt.GridDataset gds, List<String> gridList, NetcdfDataset ncd,
                                List<CoordinateAxis> axisList, boolean addLatLon) {

    //Group root = ncfile.getRootGroup();
    for (String gridName : gridList) {
      GridDatatype grid = gds.findGridDatatype(gridName);
      Variable newV = writer.findVariable(gridName);
      if (newV == null) {
        log.debug("NetcdfCFWriter cant find " + gridName + " in gds " + gds.getLocation());
        continue;
      }

      // annotate Variable for CF
      StringBuilder sbuff = new StringBuilder();
      GridCoordSystem gcs = grid.getCoordinateSystem();
      for (Variable axis : gcs.getCoordinateAxes()) {
        sbuff.append(axis.getFullName()).append(" ");
      }
      if (addLatLon)
        sbuff.append("lat lon");
      newV.addAttribute(new Attribute(CF.COORDINATES, sbuff.toString()));

      // looking for coordinate transform variables
      for (CoordinateTransform ct : gcs.getCoordinateTransforms()) {
        Variable v = ncd.findVariable(ct.getName());
        if (ct.getTransformType() == TransformType.Projection)
          newV.addAttribute(new Attribute(CF.GRID_MAPPING, v.getFullName()));
      }
    }

    for (CoordinateAxis axis : axisList) {
      Variable newV = writer.findVariable(axis.getFullNameEscaped());
      if ((axis.getAxisType() == AxisType.Height) || (axis.getAxisType() == AxisType.Pressure) || (axis.getAxisType() == AxisType.GeoZ)) {
        if (null != axis.getPositive())
          newV.addAttribute(new Attribute(CF.POSITIVE, axis.getPositive()));
      }
      if (axis.getAxisType() == AxisType.Lat) {
        newV.addAttribute(new Attribute(CDM.UNITS, "degrees_north"));
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, "latitude"));
      }
      if (axis.getAxisType() == AxisType.Lon) {
        newV.addAttribute(new Attribute(CDM.UNITS, "degrees_east"));
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, "longitude"));
      }
      if (axis.getAxisType() == AxisType.GeoX) {
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, "projection_x_coordinate"));
      }
      if (axis.getAxisType() == AxisType.GeoY) {
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, "projection_y_coordinate"));
      }
    }

    // coordinate transform variables : must convert false easting, northing to km
    List<Variable> ctvList = new ArrayList<>();
    for (ucar.nc2.dt.GridDataset.Gridset gridSet : gds.getGridsets()) {
      ucar.nc2.dt.GridCoordSystem gcs = gridSet.getGeoCoordSystem();
      ProjectionCT pct = gcs.getProjectionCT();
      if (pct != null) {
        Variable v = writer.findVariable(pct.getName()); // look for the ctv
        if ((v != null) && !ctvList.contains(v)) {
          convertProjectionCTV((NetcdfDataset) gds.getNetcdfFile(), v);
          ctvList.add(v);
        }
      }
    }
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
    ctv.addAttribute(new Attribute(att.getShortName(), val));
  }

  private long addLatLon2D(NetcdfFile ncfile, List<Variable> varList, Projection proj,
                           CoordinateAxis xaxis, CoordinateAxis yaxis) throws IOException {

    double[] xData = (double[]) xaxis.read().get1DJavaArray(double.class);
    double[] yData = (double[]) yaxis.read().get1DJavaArray(double.class);

    List<Dimension> dims = new ArrayList<>();
    dims.add(yaxis.getDimension(0));
    dims.add(xaxis.getDimension(0));

    Variable latVar = new Variable(ncfile, null, null, "lat");
    latVar.setDataType(DataType.DOUBLE);
    latVar.setDimensions(dims);
    latVar.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
    latVar.addAttribute(new Attribute(CDM.LONG_NAME, "latitude coordinate"));
    latVar.addAttribute(new Attribute(CF.STANDARD_NAME, "latitude"));
    latVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));

    Variable lonVar = new Variable(ncfile, null, null, "lon");
    lonVar.setDataType(DataType.DOUBLE);
    lonVar.setDimensions(dims);
    lonVar.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
    lonVar.addAttribute(new Attribute(CDM.LONG_NAME, "longitude coordinate"));
    lonVar.addAttribute(new Attribute(CF.STANDARD_NAME, "longitude"));
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

    long size = 0;
    size += latVar.getSize() * latVar.getElementSize();
    size += lonVar.getSize() * lonVar.getElementSize();
    return size;
  }

  private Range makeVerticalRange(Range zRange, CoordinateAxis1D vertAxis) throws InvalidRangeException {
    return (zRange != null) && (vertAxis != null) && (vertAxis.getSize() > 1) ? zRange : null;
  }

  private Range makeTimeRange(CalendarDateRange dateRange, CoordinateAxis1DTime timeAxis, int stride_time) throws InvalidRangeException {

    Range timeRange = null;
    if ((dateRange != null) && (timeAxis != null)) {
      int startIndex = timeAxis.findTimeIndexFromCalendarDate(dateRange.getStart());
      int endIndex = timeAxis.findTimeIndexFromCalendarDate(dateRange.getEnd());
      if (startIndex < 0)
        throw new InvalidRangeException("start time=" + dateRange.getStart() + " must be >= " + timeAxis.getCalendarDate(0));
      if (endIndex < 0)
        throw new InvalidRangeException("end time=" + dateRange.getEnd() + " must be >= " + timeAxis.getCalendarDate(0));
      if (stride_time <= 1) stride_time = 1;
      timeRange = new Range(startIndex, endIndex, stride_time);
    }

    return timeRange;

  }

  private void addCoordinateAxis(GridCoordSystem gcs, List<String> varNameList, List<Variable> varList, List<CoordinateAxis> axisList) {

    for (CoordinateAxis axis : gcs.getCoordinateAxes()) {
      if (!varNameList.contains(axis.getFullName())) {
        varNameList.add(axis.getFullName());
        varList.add(axis);                    // LOOK axis hasnt been subset yet !!
        axisList.add(axis);
        //if (timeAxis != null && timeAxis.isInterval()) {
        // LOOK gotta add the bounds  !!!
        //}
      }
    }

  }

  private void addCoordinateTransform(GridCoordSystem gcs, NetcdfFile ncd, List<String> varNameList, List<Variable> varList) {

    for (CoordinateTransform ct : gcs.getCoordinateTransforms()) {
      Variable v = ncd.findVariable(ct.getName());
      if (!varNameList.contains(ct.getName()) && (null != v)) {
        varNameList.add(ct.getName());
        varList.add(v);
      }
    }

  }


  /*
   * Process the coordinate transformations (formula_terms) and adds the variables needed for performing that transformation.
   * Subsets the grids variables, if needed.
   * Return size of variables added
   */
  private long processTransformationVars(List<Variable> varList, List<String> varNameList, NetcdfDataset ncd, ucar.nc2.dt.GridDataset gds,
                                         GridDatatype grid, Range timeRange, Range zRangeUse, Range yRange, Range xRange, int y_stride, int x_stride) throws InvalidRangeException {

    long varsSize = 0L;
    List<CoordinateTransform> cctt = grid.getCoordinateSystem().getCoordinateTransforms();
    for (CoordinateTransform ct : cctt) {
      Parameter param = ct.findParameterIgnoreCase(CF.FORMULA_TERMS);

      if (param != null) {
        String[] varStrings = param.getStringValue().split(" ");
        for (int i = 1; i < varStrings.length; i += 2) {
          Variable paramVar = ncd.findVariable(varStrings[i].trim());

          if (!varNameList.contains(varStrings[i]) && (null != paramVar)) {

            if (gds.findGridDatatype(paramVar.getFullName()) != null) {
              //Subset if needed
              if ((null != timeRange) || (zRangeUse != null) || (x_stride > 1 && y_stride > 1) || (yRange != null || xRange != null)) {
                GridDatatype complementaryGrid = gds.findGridDatatype(paramVar.getFullName());
                complementaryGrid = complementaryGrid.makeSubset(null, null, timeRange, zRangeUse, yRange, xRange);
                paramVar = complementaryGrid.getVariable();
              }
            } else {
              //Also have to subset the var if it is not a grid but has vertical dimension (the dimensionless vars in the formula) and zRangeUse != null
              if (zRangeUse != null && paramVar.getRank() == 1) {
                List<Range> ranges = new ArrayList<>();
                ranges.add(zRangeUse);
                paramVar = paramVar.section(ranges);
              }
            }
            varNameList.add(paramVar.getFullName());
            varsSize += paramVar.getSize() * paramVar.getElementSize();
            varList.add(paramVar);
          }

        }
      }
    }

    return varsSize;
  }
}
