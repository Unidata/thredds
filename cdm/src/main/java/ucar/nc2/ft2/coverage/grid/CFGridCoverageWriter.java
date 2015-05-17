/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.nc2.constants.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Describe
 *
 * @author caron
 * @since 5/8/2015
 */
public class CFGridCoverageWriter {

  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CFGridCoverageWriter.class);

  /**
   * Compute the size of the file without writing
   *
   * @param gds         the GridDataset
   * @param gridList    the list of variables to be written, or null for all
   * @param llbb        the lat/lon bounding box, or null for all
   * @param projRect    the projection bounding box, or null for all
   * @param horizStride the x and y stride
   * @param zRange      the z stride
   * @param dateRange   date range, or null for all
   * @param stride_time the time stride
   * @param addLatLon   add 2D lat/lon coordinates if needed
   * @return total bytes written
   * @throws IOException
   * @throws InvalidRangeException
   *
  static public long makeSizeEstimate(GridCoverageDataset gds, List<String> gridList,
                                      LatLonRect llbb, ProjectionRect projRect, int horizStride, Range zRange,
                                      CalendarDateRange dateRange, int stride_time, boolean addLatLon) throws IOException, InvalidRangeException {

    CFGridCoverageWriter writer2 = new CFGridCoverageWriter();
    return writer2.writeOrTestSize(gds, gridList, llbb, projRect, horizStride, zRange, dateRange, stride_time, addLatLon, true, null);
  } */

  /**
   * Write a netcdf/CF file from a GridDataset

   * @param gdsOrg       the GridCoverageDataset
   * @param gridNames    the list of variables to be written, or null for all
   * @param subset       defines the requested subset
   * @param addLatLon    add 2D lat/lon coordinates if needed
   * @param writer       this does the actual writing
   * @return total bytes written
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static long writeFile(GridCoverageDataset gdsOrg, List<String> gridNames,
                               GridSubset subset,
                               boolean addLatLon,
                               NetcdfFileWriter writer) throws IOException, InvalidRangeException {

    CFGridCoverageWriter writer2 = new CFGridCoverageWriter();
    return writer2.writeOrTestSize(gdsOrg, gridNames, subset, addLatLon, false, writer);
  }


  /**
   * @param gdsOrg       the GridCoverageDataset
   * @param gridNames    the list of variables to be written, or null for all
   * @param subset       the desired subset
   * @param addLatLon    add 2D lat/lon coordinates if needed
   * @param testSizeOnly dont write, just return size
   * @param writer       this does the actual writing
   * @return total bytes written
   * @throws IOException
   * @throws InvalidRangeException
   */
  private long writeOrTestSize(GridCoverageDataset gdsOrg, List<String> gridNames, GridSubset subset, boolean addLatLon, boolean testSizeOnly,
                               NetcdfFileWriter writer) throws IOException, InvalidRangeException {

    // construct the subsetted dataset
    GridDatasetHelper helper = new GridDatasetHelper(gdsOrg, gridNames);
    GridCoverageDataset subsetDataset = helper.subset(subset);

    long total_size = 0;
    for (GridCoverage grid : subsetDataset.getGrids()) {
      total_size += subsetDataset.getSizeInBytes(grid);
    }

    if (testSizeOnly)
      return total_size;

    ////////////////////////////////////////////////////////////////////

    // check size is ok
    boolean isLargeFile = isLargeFile(total_size);
    writer.setLargeFile(isLargeFile);

    addGlobalAttributes(subsetDataset, writer);

    // dimensions and independent axes
    Map<String, Dimension> dimHash = new HashMap<>();
    for (GridCoordAxis axis : subsetDataset.getCoordAxes()) {
      if (!(axis.getDependenceType() == GridCoordAxis.DependenceType.independent)) continue;
      Dimension d = writer.addDimension(null, axis.getName(), (int) axis.getNvalues());
      dimHash.put(axis.getName(), d);

      Variable v = writer.addVariable(null, axis.getName(), axis.getDataType(), axis.getName());
      for (Attribute att : axis.getAttributes())
        v.addAttribute(att);
    }

    // grids
    for (GridCoverage grid : subsetDataset.getGrids()) {                        // LOOK need to deal with runtime(time), runtime(runtime, time)
      Variable v = writer.addVariable(null, grid.getName(), grid.getDataType(), subsetDataset.getIndependentAxisNames(grid));
      for (Attribute att : grid.getAttributes())
        v.addAttribute(att);
    }

    // coordTransforms
    for (GridCoordTransform ct : subsetDataset.getCoordTransforms()) {
      Variable ctv = writer.addVariable(null, ct.getName(), DataType.INT, ""); // scaler coordinate transform variable - container for transform info
      for (Attribute att : ct.getAttributes())
        ctv.addAttribute(att);
    }


    addCFAnnotations(subsetDataset, writer, addLatLon);

    // finish define mode
    writer.create();

     // write the coordinates to the new file.
    for (GridCoordAxis axis : subsetDataset.getCoordAxes()) {
      Variable v = writer.findVariable(axis.getName());
      //double[] vals = axis.getValues();
      //Array data = Array.makeFromJavaArray(axis.getCoords());
      writer.write(v, axis.getCoords());
    }


    /* write the data to the new file.
    for (GridCoverage grid : subsetDataset.getGrids()) {
         Variable v = writer.findVariable(grid.getName());
         Array data = grid.readData(new GridSubset());
         Array reshape = data.reshape(v.getShape());
         writer.write(v, reshape);
    }  */

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


  private void addGlobalAttributes(GridCoverageDataset gds, NetcdfFileWriter writer) {
    // global attributes
    for (Attribute att : gds.getGlobalAttributes()) {
      if (att.getShortName().equals(CDM.FILE_FORMAT)) continue;
      if (att.getShortName().equals(_Coordinate._CoordSysBuilder)) continue;
      writer.addGroupAttribute(null, att);
    }

    Attribute att = gds.findAttributeIgnoreCase(CDM.CONVENTIONS);
    if (att == null || !att.getStringValue().startsWith("CF-"))  // preserve previous version of CF Convention if it exists
      writer.addGroupAttribute(null, new Attribute(CDM.CONVENTIONS, "CF-1.0"));

    writer.addGroupAttribute(null, new Attribute("History",
            "Translated to CF-1.0 Conventions by Netcdf-Java CDM (CFGridCoverageWriter)\n" +
                    "Original Dataset = " + gds.getName() + "; Translation Date = " + CalendarDate.present()));

    if (gds.getLatLonBoundingBox() != null) {
      LatLonRect llbb = gds.getLatLonBoundingBox();
      // this will replace any existing
      writer.addGroupAttribute(null, new Attribute(ACDD.LAT_MIN, llbb.getLatMin()));
      writer.addGroupAttribute(null, new Attribute(ACDD.LAT_MAX, llbb.getLatMax()));
      writer.addGroupAttribute(null, new Attribute(ACDD.LON_MIN, llbb.getLonMin()));
      writer.addGroupAttribute(null, new Attribute(ACDD.LON_MAX, llbb.getLonMax()));
    }
  }

  private void addCFAnnotations(GridCoverageDataset gds, NetcdfFileWriter writer, boolean addLatLon) {

    //Group root = ncfile.getRootGroup();
    for (GridCoverage grid : gds.getGrids()) {
      GridCoordSys gcs = gds.findCoordSys(grid.getCoordSysName());

      Variable newV = writer.findVariable(grid.getName());
      if (newV == null) {
        log.debug("NetcdfCFWriter cant find " + grid.getName() + " in gds " + gds.getName());
        continue;
      }

      // annotate Variable for CF
      StringBuilder sbuff = new StringBuilder();
      sbuff.append(gds.getAxisNames(grid));
      if (addLatLon) sbuff.append("lat lon");
      newV.addAttribute(new Attribute(CF.COORDINATES, sbuff.toString()));

      // looking for coordinate transform variables
      for (String ctname : gcs.getTransformNames()) {
        GridCoordTransform ct = gds.findCoordTransform(ctname);
        if (ct.isHoriz)
          newV.addAttribute(new Attribute(CF.GRID_MAPPING, ctname));
      }
    }

    for (GridCoordAxis axis : gds.getCoordAxes()) {
      Variable newV = writer.findVariable(axis.getName());
      /* if ((axis.getAxisType() == AxisType.Height) || (axis.getAxisType() == AxisType.Pressure) || (axis.getAxisType() == AxisType.GeoZ)) {
        if (null != axis.getPositive())
          newV.addAttribute(new Attribute(CF.POSITIVE, axis.getPositive()));
      } */
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

    /* coordinate transform variables : must convert false easting, northing to km
    List<Variable> ctvList = new ArrayList<>();
    for (GridCoordSys coordSys : gds.getCoordSys()) {
      Projection pct = gds.getProjection(coordSys);
      if (pct != null) {
        Variable v = writer.findVariable(pct.getName()); // look for the ctv
        if ((v != null) && !ctvList.contains(v)) {
          convertProjectionCTV((NetcdfDataset) gds.getNetcdfFile(), v);
          ctvList.add(v);
        }
      }
    } */
  }

  /* private void convertProjectionCTV(NetcdfDataset ds, Variable ctv) {
    Attribute att = ctv.findAttribute(_Coordinate.TransformType);
    if ((null != att) && att.getStringValue().equals("Projection")) {
      Attribute east = ctv.findAttribute("false_easting");
      Attribute north = ctv.findAttribute("false_northing");
      if ((null != east) || (null != north)) {
        double scalef = AbstractTransformBuilder.getFalseEastingScaleFactor(ds, ctv);
        if (scalef != 1.0) {
          convertAttribute(ctv, east, scalef);
          convertAttribute(ctv, north, scalef);
        }
      }
    }
  }  */

 /*  private void convertAttribute(Variable ctv, Attribute att, double scalef) {
    if (att == null) return;
    double val = scalef * att.getNumericValue().doubleValue();
    ctv.addAttribute(new Attribute(att.getShortName(), val));
  }

  private long addLatLon2D(NetcdfFile ncfile, List<Variable> varList, Projection proj,
                           GridCoordAxis xaxis, GridCoordAxis yaxis) throws IOException {

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

  private Range makeVerticalRange(Range zRange, GridCoordAxis vertAxis) throws InvalidRangeException {
    return (zRange != null) && (vertAxis != null) && (vertAxis.getSize() > 1) ? zRange : null;
  }

  private Range makeTimeRange(CalendarDateRange dateRange, GridCoordAxis timeAxis, int stride_time) throws InvalidRangeException {

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

  private void addGridCoordAxis(GridCoordSys gcs, List<String> varNameList, List<GridCoordAxis> axisList) {

    for (String axisName : gcs.getAxisNames()) {
      if (!varNameList.contains(axisName)) {
        varNameList.add(axisName);
        axisList.add( gds.findCoordAxis(axisName));
      }
    }

  }

  private void addGridCoordTransform(GridCoordSys gcs, List<String> varNameList, List<GridCoordTransform> varList) {

    for (String ctName : gcs.getTransformNames()) {
      if (!varNameList.contains(ctName)) {
        varNameList.add(ctName);
        varList.add(gds.findCoordTransform(ctName));
      }
    }

  }    */


  /*
   * Process the coordinate transformations (formula_terms) and adds the variables needed for performing that transformation.
   * Subsets the grids variables, if needed.
   * Return size of variables added
   *
  private long processTransformationVars(List<Variable> varList, List<String> varNameList, NetcdfDataset ncd,
                                         GridCoverage grid, Range timeRange, Range zRangeUse, Range yRange, Range xRange, int y_stride, int x_stride) throws InvalidRangeException {

    long varsSize = 0L;
    List<GridCoordTransform> cctt = grid.getCoordinateSystem().getGridCoordTransforms();
    for (GridCoordTransform ct : cctt) {
      Parameter param = ct.findParameterIgnoreCase(CF.FORMULA_TERMS);

      if (param != null) {
        String[] varStrings = param.getStringValue().split(" ");
        for (int i = 1; i < varStrings.length; i += 2) {
          Variable paramVar = ncd.findVariable(varStrings[i].trim());

          if (!varNameList.contains(varStrings[i]) && (null != paramVar)) {

            if (gds.findGridCoverage(paramVar.getFullName()) != null) {
              //Subset if needed
              if ((null != timeRange) || (zRangeUse != null) || (x_stride > 1 && y_stride > 1) || (yRange != null || xRange != null)) {
                GridCoverage complementaryGrid = gds.findGridCoverage(paramVar.getFullName());
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
  } */
}

