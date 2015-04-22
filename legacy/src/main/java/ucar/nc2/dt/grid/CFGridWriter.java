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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.FileWriter2;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.ACDD;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.TransformType;
import ucar.nc2.dataset.transform.AbstractCoordTransBuilder;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategyGrib;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionPointImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.Parameter;

/**
 * Write a CF compliant Netcdf-3 or netcdf-4 file (classic mode only) from any gridded dataset.
 * The datasets can optionally be subset by a lat/lon bounding box and/or a time range.
 *
 * @deprecated use CFGridWriter2
 * @author caron
 */
public class CFGridWriter {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CFGridWriter.class);

  /**
   * Write a netcdf-3 file from a subset of a grid dataset
   *
   * @param location write new file
   * @param gds      from this grid dataset
   * @param gridList just these grids
   * @param llbb     horiz subset, may be null
   * @param range    time subset, may be null
   * @throws IOException
   * @throws InvalidRangeException
   */
  static public void makeFile(String location, ucar.nc2.dt.GridDataset gds, List<String> gridList, LatLonRect llbb, CalendarDateRange range)
          throws IOException, InvalidRangeException {
    CFGridWriter writer = new CFGridWriter();
    writer.makeFile(location, gds, gridList, llbb, range, false, 1, 1, 1);
  }

  static public void makeFileVersioned(String location, ucar.nc2.dt.GridDataset gds, List<String> gridList, LatLonRect llbb,
                                       CalendarDateRange dateRange, NetcdfFileWriter.Version version)
          throws IOException, InvalidRangeException {
    CFGridWriter writer = new CFGridWriter();
    writer.makeOrTestSize(location, gds, gridList, llbb, 1, null, dateRange, 1, false, false, version);
  }

  /**
   * Write a netcdf-3 file from a subset of a grid dataset, as long as it doesnt exceed a certain file size.
   *
   * @param gds         from this grid dataset
   * @param gridList    just these grids
   * @param llbb        horiz subset, may be null
   * @param zRange      vertical subset, may be null
   * @param dateRange   time subset, may be null
   * @param stride_time time may be strided, -1 if want all
   * @param addLatLon   optionally add a lat/lon coordinate (if dataset uses projection coords)
   * @return file size
   * @throws IOException
   * @throws InvalidRangeException
   */
  public long makeGridFileSizeEstimate(ucar.nc2.dt.GridDataset gds, List<String> gridList,
                                       LatLonRect llbb, int horizStride,
                                       Range zRange,
                                       CalendarDateRange dateRange, int stride_time,
                                       boolean addLatLon) throws IOException, InvalidRangeException {

    return makeOrTestSize(null, gds, gridList, llbb, horizStride, zRange, dateRange, stride_time, addLatLon, true, NetcdfFileWriter.Version.netcdf3);
  }

  /**
   * Write a netcdf-3 file from a subset of a grid dataset (projection coordinates), as long as it doesnt exceed a certain file size.
   *
   * @param gds         from this grid dataset
   * @param gridList    just these grids
   * @param projBB      horiz subset in Projection coords, may be null
   * @param zRange      vertical subset, may be null
   * @param dateRange   time subset, may be null
   * @param stride_time time may be strided, -1 if want all
   * @param addLatLon   optionally add a lat/lon coordinate (if dataset uses projection coords)
   * @return file size
   * @throws IOException
   * @throws InvalidRangeException
   */
  public long makeGridFileSizeEstimate(ucar.nc2.dt.GridDataset gds, List<String> gridList,
                                       ProjectionRect projBB, int horizStride,
                                       Range zRange,
                                       CalendarDateRange dateRange, int stride_time,
                                       boolean addLatLon) throws IOException, InvalidRangeException {

    return makeOrTestSize(null, gds, gridList, projBB, horizStride, zRange, dateRange, stride_time, addLatLon, true, NetcdfFileWriter.Version.netcdf3);
  }


  /**
   * Write a CF compliant Netcdf-3 file from any gridded dataset.
   *
   * @param location    write to this location on disk
   * @param gds         A gridded dataset
   * @param gridList    the list of grid names to be written, must not be empty. Full name (not short).
   * @param llbb        optional lat/lon bounding box
   * @param range       optional time range
   * @param addLatLon   should 2D lat/lon variables be added, if its a projection coordinate system?
   * @param horizStride x,y stride
   * @param stride_z    not implemented yet
   * @param stride_time not implemented yet
   * @throws IOException           if write or read error
   * @throws InvalidRangeException if subset is illegal
   */
  public void makeFile(String location, ucar.nc2.dt.GridDataset gds, List<String> gridList,
                       LatLonRect llbb, CalendarDateRange range,
                       boolean addLatLon,
                       int horizStride, int stride_z, int stride_time)
          throws IOException, InvalidRangeException {
    makeFile(location, gds, gridList, llbb, horizStride, null, range, stride_time, addLatLon, NetcdfFileWriter.Version.netcdf3);
  }

  public long makeFile(String location, ucar.nc2.dt.GridDataset gds, List<String> gridList,
                       LatLonRect llbb, int horizStride, Range zRange,
                       CalendarDateRange dateRange, int stride_time,
                       boolean addLatLon)
          throws IOException, InvalidRangeException {

    return makeOrTestSize(location, gds, gridList, llbb, horizStride, zRange, dateRange, stride_time, addLatLon, false, NetcdfFileWriter.Version.netcdf3);
  }

  public long makeFile(String location, ucar.nc2.dt.GridDataset gds, List<String> gridList,
                       LatLonRect llbb, int horizStride, Range zRange,
                       CalendarDateRange dateRange, int stride_time,
                       boolean addLatLon, NetcdfFileWriter.Version version)
          throws IOException, InvalidRangeException {

    return makeOrTestSize(location, gds, gridList, llbb, horizStride, zRange, dateRange, stride_time, addLatLon, false, version);
  }

  public long makeFile(String location, ucar.nc2.dt.GridDataset gds, List<String> gridList,
                       ProjectionRect projRect, int horizStride,
                       Range zRange,
                       CalendarDateRange dateRange, int stride_time,
                       boolean addLatLon)
          throws IOException, InvalidRangeException {

    return makeOrTestSize(location, gds, gridList, projRect, horizStride, zRange, dateRange, stride_time, addLatLon, false, NetcdfFileWriter.Version.netcdf3);
  }

  public long makeFile(String location, ucar.nc2.dt.GridDataset gds, List<String> gridList,
                       ProjectionRect llbb, int horizStride,
                       Range zRange,
                       CalendarDateRange dateRange, int stride_time,
                       boolean addLatLon, NetcdfFileWriter.Version version)
          throws IOException, InvalidRangeException {

    return makeOrTestSize(location, gds, gridList, llbb, horizStride, zRange, dateRange, stride_time, addLatLon, false, version);
  }

  private long makeOrTestSize(String location, ucar.nc2.dt.GridDataset gds, List<String> gridList,
                              LatLonRect llbb, int horizStride,
                              Range zRange,
                              CalendarDateRange dateRange, int stride_time,
                              boolean addLatLon, boolean testSizeOnly,
                              NetcdfFileWriter.Version version)
          throws IOException, InvalidRangeException {

    NetcdfDataset ncd = (NetcdfDataset) gds.getNetcdfFile();
    LatLonRect resultBB = null;

    ArrayList<Variable> varList = new ArrayList<>();
    ArrayList<String> varNameList = new ArrayList<>();
    ArrayList<CoordinateAxis> axisList = new ArrayList<>();

    // add each desired Grid to the new file
    long total_size = 0;
    for (String gridName : gridList) {
      if (varNameList.contains(gridName))
        continue;

      varNameList.add(gridName);

      GridDatatype grid = gds.findGridDatatype(gridName);
      GridCoordSystem gcsOrg = grid.getCoordinateSystem();
      LatLonRect gridBB = gcsOrg.getLatLonBoundingBox();
      CoordinateAxis1DTime timeAxis = gcsOrg.getTimeAxis1D();
      CoordinateAxis1D vertAxis = gcsOrg.getVerticalAxis();
      boolean global = gcsOrg.isGlobalLon();

      // make subset if needed
      Range timeRange = makeTimeRange(dateRange, timeAxis, stride_time);
      Range zRangeUse = makeVerticalRange(zRange, vertAxis);

      if ((null != timeRange) || (zRangeUse != null) || (llbb != null) || (horizStride > 1)) {
        grid = grid.makeSubset(timeRange, zRangeUse, llbb, 1, horizStride, horizStride);
        LatLonRect subsetGridBB = grid.getCoordinateSystem().getLatLonBoundingBox();
        if (resultBB == null)
          resultBB = subsetGridBB;
        else
          resultBB.extend(subsetGridBB);
      } else {
        if (resultBB == null) {
          resultBB = gridBB;
        } else {
          resultBB.extend(gridBB);
        }
      }

      Variable gridV = grid.getVariable();
      varList.add(gridV);
      total_size += gridV.getSize() * gridV.getElementSize();

      // add coordinate axes
      GridCoordSystem gcs = grid.getCoordinateSystem();
      addCoordinateAxis(gcs, varNameList, varList, axisList);

      // add coordinate transform variables
      addCoordinateTransform(gcs, ncd, varNameList, varList);

      //Add Variables from the formula_terms
      total_size += processTransformationVars(varList, varNameList, ncd, gds, grid, timeRange, zRangeUse, llbb, 1, horizStride, horizStride, axisList);

      // optional lat/lon
      if (addLatLon) {
        Projection proj = gcs.getProjection();
        if ((null != proj) && !(proj instanceof LatLonProjection)) {
          addLatLon2D(ncd, varList, proj, gcs.getXHorizAxis(), gcs.getYHorizAxis());
          addLatLon = false;
        }
      }
    }

    if (testSizeOnly)
      return total_size;

    // check size is ok
    boolean isLargeFile = isLargeFile(total_size);

    //Default chunking strategy for NCSS will be used if chunking = null
    Nc4Chunking chunking = null;
    if (version == NetcdfFileWriter.Version.netcdf4) {
      //version = NetcdfFileWriter.Version.netcdf4_classic;
      //use grib chunking as default --> one chunk for each (y,x)-slide
      chunking = new Nc4ChunkingStrategyGrib(5, true);
    }
    NetcdfFileWriter writer = NetcdfFileWriter.createNew(version, location, chunking);
    //writer.setLargeFile(isLargeFile);
    writeGlobalAttributes(writer, gds, resultBB);

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
    return 0; // ok
  }

  private long makeOrTestSize(String location, ucar.nc2.dt.GridDataset gds, List<String> gridList,
                              ProjectionRect projRect, int horizStride,
                              Range zRange,
                              CalendarDateRange dateRange, int stride_time,
                              boolean addLatLon, boolean testSizeOnly,
                              NetcdfFileWriter.Version version)
          throws IOException, InvalidRangeException {

    NetcdfDataset ncd = (NetcdfDataset) gds.getNetcdfFile();

    ArrayList<Variable> varList = new ArrayList<>();
    ArrayList<String> varNameList = new ArrayList<>();
    ArrayList<CoordinateAxis> axisList = new ArrayList<>();

    LatLonRect resultBB = null;
    // add each desired Grid to the new file
    long total_size = 0;
    for (String gridName : gridList) {
      if (varNameList.contains(gridName))
        continue;

      varNameList.add(gridName);

      //GridDatatype grid = gds.findGridDatatype(gridName);
      GeoGrid grid = (GeoGrid) gds.findGridDatatype(gridName);
      GridCoordSystem gcsOrg = grid.getCoordinateSystem();
      CoordinateAxis1DTime timeAxis = gcsOrg.getTimeAxis1D();
      CoordinateAxis1D vertAxis = gcsOrg.getVerticalAxis();

      // make subset if needed
      Range timeRange = makeTimeRange(dateRange, timeAxis, stride_time);

      if (gcsOrg.getXHorizAxis().getRank() > 1 || gcsOrg.getYHorizAxis().getRank() > 1) {
        throw new IllegalArgumentException("Coordinate systems with 2D horizontal axis are not supported");
      }

      CoordinateAxis1D xAxis = (CoordinateAxis1D) gcsOrg.getXHorizAxis();
      double[] xCoords = xAxis.getCoordValues();

      CoordinateAxis1D yAxis = (CoordinateAxis1D) gcsOrg.getYHorizAxis();
      double[] yCoords = yAxis.getCoordValues();

      Range x_range = null;
      Range y_range = null;
      if (projRect != null) {

        // use the intersection
        ProjectionRect fullBB = new ProjectionRect(xCoords[0], yCoords[0], xCoords[xCoords.length - 1], yCoords[yCoords.length - 1]);
        if (!projRect.intersects(fullBB))
          throw new InvalidRangeException("BBOX must intersect grid BBOX, minx=" + xCoords[0] + ", miny=" + yCoords[0] + ", maxx=" + xCoords[xCoords.length - 1] + ", maxy=" + yCoords[yCoords.length - 1]);
        ProjectionRect.intersect(fullBB, projRect, projRect);

        ProjectionPoint lowerLeft = projRect.getLowerLeftPoint();
        ProjectionPoint upperRigth = projRect.getUpperRightPoint();
        double minx = lowerLeft.getX();
        double miny = lowerLeft.getY();
        double maxx = upperRigth.getX();
        double maxy = upperRigth.getY();

        //y_range
        int minyCoord = yAxis.findCoordElement(miny);
        int maxyCoord = yAxis.findCoordElement(maxy);

        //x_range
        int minxCoord = xAxis.findCoordElement(minx);
        int maxxCoord = xAxis.findCoordElement(maxx);

        y_range = new Range(minyCoord, maxyCoord, horizStride);
        x_range = new Range(minxCoord, maxxCoord, horizStride);
      }

      Range zRangeUse = makeVerticalRange(zRange, vertAxis);

      if ((null != timeRange) || (zRangeUse != null) || (projRect != null) || (horizStride > 1)) {
        grid = grid.subset(timeRange, zRangeUse, y_range, x_range);
      }

      LatLonRect gridBB = grid.getCoordinateSystem().getLatLonBoundingBox();
      if (resultBB == null)
        resultBB = gridBB;
      else
        resultBB.extend(gridBB);

      Variable gridV = grid.getVariable();
      varList.add(gridV);
      total_size += gridV.getSize() * gridV.getElementSize();

      GridCoordSystem gcs = grid.getCoordinateSystem();
      // add coordinate axes
      addCoordinateAxis(gcs, varNameList, varList, axisList);

      //add coordinate transforms
      addCoordinateTransform(gcs, ncd, varNameList, varList);

      //Add Variables from the formula_terms
      total_size += processTransformationVars(varList, varNameList, ncd, gds, grid, timeRange, zRangeUse, y_range, x_range, 1, horizStride, horizStride);

      // optional lat/lon
      if (addLatLon) {
        Projection proj = gcs.getProjection();
        if ((null != proj) && !(proj instanceof LatLonProjection)) {
          addLatLon2D(ncd, varList, proj, gcs.getXHorizAxis(), gcs.getYHorizAxis());
          addLatLon = false;
        }
      }
    }

    if (testSizeOnly)
      return total_size;

    // check size is ok
    boolean isLargeFile = isLargeFile(total_size);

    //Default chunking strategy for NCSS
    Nc4Chunking chunking = null;

    if (version == NetcdfFileWriter.Version.netcdf4) {
      //version = NetcdfFileWriter.Version.netcdf4_classic;
      //use grib chunking as default --> one chunk for each (y,x)-slide
      chunking = new Nc4ChunkingStrategyGrib(5, true);
    }

    //NetcdfFileWriter writer = NetcdfFileWriter.createNew(version, location, null);
    NetcdfFileWriter writer = NetcdfFileWriter.createNew(version, location, chunking);
    writer.setLargeFile(isLargeFile);
    if (resultBB != null)
      writeGlobalAttributes(writer, gds, resultBB);

    // use fileWriter to copy the variables
    FileWriter2 fileWriter = new FileWriter2(writer);
    for (Variable v : varList)
      fileWriter.addVariable(v);

    addCFAnnotations(writer, gds, gridList, ncd, axisList, addLatLon);

    writer.create();
    //updateGeospatialRanges(writer,  llrec);

    // use fileWriter to copy the data
    fileWriter.copyVarData(varList, null, null);


    writer.close();

    //updateGeospatialRanges(location,  llrec);

    return 0; // ok
  }

	/*
   * Looks for the attributes in the unidata discovery conventions and, if present, updates their values
	 * to the values in the new axes. 
	 * 
	 * @param writer
	 * @param llRect
	 * @throws IOException 
	 *
	private void  updateGeospatialRanges(NetcdfFileWriter writer, LatLonRect llRect) throws IOException{
		//Flush before updating...
		writer.flush();
		
		//should we add them if they are not present??
        Attribute from = writer.findGlobalAttribute(ACDD.LAT_MIN);
        if(from !=null){
			updateAttribute(writer, from, ACDD.LAT_MIN, llRect.getLatMin());
		}

        from = writer.findGlobalAttribute(ACDD.LAT_MAX);
        if(from !=null){
			updateAttribute(writer, from, ACDD.LAT_MAX, llRect.getLatMax());
		}

        from = writer.findGlobalAttribute(ACDD.LON_MIN);
        if(from !=null){
			updateAttribute(writer, from, ACDD.LON_MIN, llRect.getLonMin());
		}

        from = writer.findGlobalAttribute(ACDD.LON_MAX);
        if(from !=null){
			updateAttribute(writer, from, ACDD.LON_MAX, llRect.getLonMax());
		}	
	}
	
	
	private void updateAttribute(NetcdfFileWriter writer, Attribute from, String attName, double value) throws IOException{

		if( from.getDataType() == DataType.FLOAT )
			writer.updateAttribute(null, new Attribute( attName, (float) value));
		else
			writer.updateAttribute(null, new Attribute( attName, value));		
		
	}  */

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
        varList.add(axis);
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


  /**
   * Process the coordinate transformations (formula_terms) and adds the variables needed for performing that transformation to the list of variables in the new file.
   * Also, subsets the grids variables, if needed.
   *
   * @param varList
   * @param varNameList
   * @param ncd
   * @param gds
   * @param grid
   * @param timeRange
   * @param zRangeUse
   * @param llbb
   * @param z_stride
   * @param y_stride
   * @param x_stride
   * @return size of the added variables
   * @throws InvalidRangeException
   */
  private long processTransformationVars(ArrayList<Variable> varList, ArrayList<String> varNameList, NetcdfDataset ncd, ucar.nc2.dt.GridDataset gds,
                                         GridDatatype grid, Range timeRange, Range zRangeUse, LatLonRect llbb, int z_stride, int y_stride, int x_stride,
                                         List<CoordinateAxis> axisList) throws InvalidRangeException {

    List<Range> yxRanges = new ArrayList<Range>(2);

    if (llbb == null) {
      yxRanges.add(null);
      yxRanges.add(null);
    } else {
      yxRanges = grid.getCoordinateSystem().getRangesFromLatLonRect(llbb);
    }

    return processTransformationVars(varList, varNameList, ncd, gds, grid, timeRange, zRangeUse, yxRanges.get(0), yxRanges.get(1), z_stride, y_stride, x_stride);
  }


  private long processTransformationVars(ArrayList<Variable> varList, ArrayList<String> varNameList, NetcdfDataset ncd, ucar.nc2.dt.GridDataset gds,
                                         GridDatatype grid, Range timeRange, Range zRangeUse, Range yRange, Range xRange, int z_stride, int y_stride, int x_stride) throws InvalidRangeException {

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
                List<Range> ranges = new ArrayList<Range>();
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

  private boolean isLargeFile(long total_size) {
    boolean isLargeFile = false;
    long maxSize = 2 * 1000 * 1000 * 1000;
    if (total_size > maxSize) {
      log.info("Request size = {} Mbytes", total_size / 1000 / 1000);
      isLargeFile = true;
    }
    return isLargeFile;
  }

  private void writeGlobalAttributes(NetcdfFileWriter writer, ucar.nc2.dt.GridDataset gds, LatLonRect llbb) {
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
            "Translated to CF-1.0 Conventions by Netcdf-Java CDM (CFGridWriter)\n" +
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
        log.warn("NetcdfCFWriter cant find " + gridName + " in gds " + gds.getLocation());
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
        newV.addAttribute(new Attribute("standard_name", "latitude"));
      }
      if (axis.getAxisType() == AxisType.Lon) {
        newV.addAttribute(new Attribute(CDM.UNITS, "degrees_east"));
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
        Variable v = writer.findVariable(pct.getName()); // look for the ctv
        if ((v != null) && !ctvList.contains(v)) {
          convertProjectionCTV((NetcdfDataset) gds.getNetcdfFile(), v);
          ctvList.add(v);
        }
      }
    }
  }

}

