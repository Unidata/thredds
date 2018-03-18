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
 *
 */
package ucar.nc2.ft2.coverage.writer;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.*;
import ucar.nc2.constants.*;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Optional;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.LatLonProjection;

import java.io.IOException;
import java.util.*;

/**
 * Write CF Compliant Grid file from a Coverage.
 * First, single coverage only.
 * - The idea is to subset the coordsys, use that for the file's metadata.
 * - Then subset the grid, and write out the data. Check that the grid's metadata matches.
 *
 * @author caron
 * @since 5/8/2015
 */
public class CFGridCoverageWriter2 {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CFGridCoverageWriter2.class);
  static private final boolean show = false;

  static private final String BOUNDS = "_bounds";
  static private final String BOUNDS_DIM = "bounds_dim"; // dimension of length 2, can be used by any bounds coordinate

  /**
   * Write a netcdf/CF file from a CoverageDataset

   * @param gdsOrg            the CoverageDataset
   * @param gridNames         the list of coverage names to be written, or null for all
   * @param subset            defines the requested subset, or null to include everything in gdsOrg
   * @param tryToAddLatLon2D  add 2D lat/lon coordinates, if possible
   * @param testSizeOnly      dont write, just return expected size
   * @param writer            this does the actual writing, may be null if testSizeOnly=true
   * @return  the total number of bytes that the variables in the output file occupy. This is NOT the same as the
   *          size of the the whole output file, but it's close.
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static ucar.nc2.util.Optional<Long> writeOrTestSize(CoverageCollection gdsOrg, List<String> gridNames,
          SubsetParams subset, boolean tryToAddLatLon2D, boolean testSizeOnly, NetcdfFileWriter writer)
          throws IOException, InvalidRangeException {
    CFGridCoverageWriter2 writer2 = new CFGridCoverageWriter2();
    return writer2.writeFile(gdsOrg, gridNames, subset, tryToAddLatLon2D, testSizeOnly, writer);
  }

  private ucar.nc2.util.Optional<Long> writeFile(CoverageCollection gdsOrg, List<String> gridNames,
          SubsetParams subsetParams, boolean tryToAddLatLon2D, boolean testSizeOnly, NetcdfFileWriter writer)
          throws IOException, InvalidRangeException {
    if (gridNames == null) {  // want all of them
      gridNames = new LinkedList<>();

      for (Coverage coverage : gdsOrg.getCoverages()) {
        gridNames.add(coverage.getName());
      }
    }

    if (subsetParams == null) {
      subsetParams = new SubsetParams();
    }

    if (writer == null) {
      if (testSizeOnly) {
        writer = NetcdfFileWriter.createNew(null, false);  // null location. It's ok; we'll never write the file.
      } else {
        throw new NullPointerException("writer must be non-null when testSizeOnly == false.");
      }
    }

    // We need global attributes, subsetted axes, transforms, and the coverages with attributes and referencing
    // subsetted axes.
    Optional<CoverageCollection> opt = CoverageSubsetter2.makeCoverageDatasetSubset(gdsOrg, gridNames, subsetParams);
    if (!opt.isPresent())
      return ucar.nc2.util.Optional.empty(opt.getErrorMessage());

    CoverageCollection subsetDataset = opt.get();

    ////////////////////////////////////////////////////////////////////

    addGlobalAttributes(subsetDataset, writer);
    addDimensions(subsetDataset, writer);
    addCoordinateAxes(subsetDataset, writer);
    addCoverages(subsetDataset, writer);
    addCoordTransforms(subsetDataset, writer);

    boolean shouldAddLatLon2D = shouldAddLatLon2D(tryToAddLatLon2D, subsetDataset);
    if (shouldAddLatLon2D) {
      addLatLon2D(subsetDataset, writer);
    }

    addCFAnnotations(subsetDataset, writer, shouldAddLatLon2D);

    long totalSizeOfVars = 0;
    // This is a hack to get the root group of writer's underlying NetcdfFile. See the method's Javadoc.
    Group rootGroup = writer.addGroup(null, null);

    // In this class, we've only added vars to the root group, so this is all we need to worry about for size calc.
    for (Variable var : rootGroup.getVariables()) {
      totalSizeOfVars += var.getSize() * var.getElementSize();
    }

    if (!testSizeOnly) {
      // Actually create file and write variable data to it.
      writer.setLargeFile(isLargeFile(totalSizeOfVars));
      writer.create();

      writeCoordinateData(subsetDataset, writer);
      writeCoverageData(gdsOrg, subsetParams, subsetDataset, writer);

      if (shouldAddLatLon2D) {
        writeLatLon2D(subsetDataset, writer);
      }

      writer.close();
    }

    return Optional.of(totalSizeOfVars);
  }

  /**
   * Returns {@code true} if we should add 2D latitude & longitude variables to the output file.
   * This method could return {@code false} for several reasons:
   *
   * <ul>
   *     <li>{@code !tryToAddLatLon2D}</li>
   *     <li>{@code subsetDataset.getHorizCoordSys().isLatLon2D()}</li>
   *     <li>{@code !subsetDataset.getHorizCoordSys().isProjection()}</li>
   *     <li>{@code subsetDataset.getHorizCoordSys() instanceof LatLonProjection}</li>
   * </ul>
   *
   * @param tryToAddLatLon2D  attempt to add 2D lat/lon vars to output file.
   * @param subsetDataset     subsetted version of the original CoverageCollection.
   * @return  {@code true} if we should add 2D latitude & longitude variables to the output file.
   */
  private boolean shouldAddLatLon2D(boolean tryToAddLatLon2D, CoverageCollection subsetDataset) {
    if (!tryToAddLatLon2D) {  // We don't even want 2D lat/lon vars.
      return false;
    }

    HorizCoordSys horizCoordSys = subsetDataset.getHorizCoordSys();
    if (horizCoordSys.isLatLon2D()) {  // We already have 2D lat/lon vars.
      return false;
    }
    if (!horizCoordSys.isProjection()) {  // CRS doesn't contain a projection, meaning we can't calc 2D lat/lon vars.
      return false;
    }

    Projection proj = horizCoordSys.getTransform().getProjection();
    if (proj instanceof LatLonProjection) {  // Projection is a "fake"; we already have lat/lon.
      return false;
    }

    return true;
  }

  private boolean isLargeFile(long total_size) {
    boolean isLargeFile = false;
    long maxSize = Integer.MAX_VALUE;
    if (total_size > maxSize) {
      logger.debug("Request size = {} Mbytes", total_size / 1000 / 1000);
      isLargeFile = true;
    }
    return isLargeFile;
  }

  private void addGlobalAttributes(CoverageCollection gds, NetcdfFileWriter writer) {
    // global attributes
    for (Attribute att : gds.getGlobalAttributes()) {
      if (att.getShortName().equals(CDM.FILE_FORMAT)) continue;
      if (att.getShortName().equals(_Coordinate._CoordSysBuilder)) continue;
      writer.addGroupAttribute(null, att);
    }

    Attribute att = gds.findAttributeIgnoreCase(CDM.CONVENTIONS);
    if (att == null || !att.getStringValue().startsWith("CF-"))  // preserve prev version of CF Convention if exists
      writer.addGroupAttribute(null, new Attribute(CDM.CONVENTIONS, "CF-1.0"));

    writer.addGroupAttribute(null, new Attribute("History",
            "Translated to CF-1.0 Conventions by Netcdf-Java CDM (CFGridCoverageWriter2)\n" +
                    "Original Dataset = " + gds.getName() + "; Translation Date = " + CalendarDate.present()));

    LatLonRect llbb = gds.getLatlonBoundingBox();
    if (llbb != null) {
      // this will replace any existing
      writer.addGroupAttribute(null, new Attribute(ACDD.LAT_MIN, llbb.getLatMin()));
      writer.addGroupAttribute(null, new Attribute(ACDD.LAT_MAX, llbb.getLatMax()));
      writer.addGroupAttribute(null, new Attribute(ACDD.LON_MIN, llbb.getLonMin()));
      writer.addGroupAttribute(null, new Attribute(ACDD.LON_MAX, llbb.getLonMax()));
    }
  }

  private void addDimensions(CoverageCollection subsetDataset, NetcdfFileWriter writer) {
    // each independent coordinate is a dimension
    Map<String, Dimension> dimHash = new HashMap<>();
    for (CoverageCoordAxis axis : subsetDataset.getCoordAxes()) {
      if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.independent) {
        Dimension d = writer.addDimension(null, axis.getName(), axis.getNcoords());
        dimHash.put(axis.getName(), d);
      }

      if (axis.isInterval()) {
        if (null == dimHash.get(BOUNDS_DIM)) {
          Dimension d = writer.addDimension(null, BOUNDS_DIM, 2);
          dimHash.put(BOUNDS_DIM, d);
        }
      }
    }
  }

  private void addCoordinateAxes(CoverageCollection subsetDataset, NetcdfFileWriter writer) {
    for (CoverageCoordAxis axis : subsetDataset.getCoordAxes()) {
      String dims;

      if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.independent) {
        dims = axis.getName();

      } else if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.scalar) {
        dims = "";

      } else {
        dims = axis.getDependsOn();
      }

      boolean hasBounds = false;
      if (axis.isInterval()) {
        Variable vb = writer.addVariable(null, axis.getName()+BOUNDS, axis.getDataType(), dims+" "+BOUNDS_DIM);
        vb.addAttribute(new Attribute(CDM.UNITS, axis.getUnits()));
        hasBounds = true;
      }

      Variable v = writer.addVariable(null, axis.getName(), axis.getDataType(), dims);
      addVariableAttributes(v, axis.getAttributes());
      v.addAttribute(new Attribute(CDM.UNITS, axis.getUnits())); // override what was in att list
      if (hasBounds)
        v.addAttribute(new Attribute(CF.BOUNDS, axis.getName()+BOUNDS));
      if (axis.getAxisType() == AxisType.TimeOffset)
        v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_OFFSET));
    }
  }

  private void addCoverages(CoverageCollection subsetDataset, NetcdfFileWriter writer) {
    for (Coverage grid : subsetDataset.getCoverages()) {
      Variable v = writer.addVariable(null, grid.getName(), grid.getDataType(), grid.getIndependentAxisNamesOrdered());
      addVariableAttributes(v, grid.getAttributes());
    }
  }

  private void addVariableAttributes(Variable v, List<Attribute> atts) {
    for (Attribute att : atts) {
      if (att.getShortName().startsWith("_Coordinate")) continue;
      if (att.getShortName().startsWith("_Chunk")) continue;
      v.addAttribute(att);
    }
  }

  private void addCoordTransforms(CoverageCollection subsetDataset, NetcdfFileWriter writer) {
    for (CoverageTransform ct : subsetDataset.getCoordTransforms()) {
      // scalar coordinate transform variable - container for transform info
      Variable ctv = writer.addVariable(null, ct.getName(), DataType.INT, "");

      for (Attribute att : ct.getAttributes())
        ctv.addAttribute(att);
    }
  }

  private void addLatLon2D(CoverageCollection subsetDataset, NetcdfFileWriter writer) {
    HorizCoordSys horizCoordSys = subsetDataset.getHorizCoordSys();
    CoverageCoordAxis1D xAxis = horizCoordSys.getXAxis();
    CoverageCoordAxis1D yAxis = horizCoordSys.getYAxis();

    Dimension xDim = writer.findDimension(xAxis.getName());
    Dimension yDim = writer.findDimension(yAxis.getName());
    assert xDim != null : "We should've added X dimension in addDimensions().";
    assert yDim != null : "We should've added Y dimension in addDimensions().";

    List<Dimension> dims = Arrays.asList(yDim, xDim);

    Variable latVar = writer.addVariable("lat", DataType.DOUBLE, dims);
    latVar.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
    latVar.addAttribute(new Attribute(CF.STANDARD_NAME, CF.LATITUDE));
    latVar.addAttribute(new Attribute(CDM.LONG_NAME, "latitude coordinate"));
    latVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));

    Variable lonVar = writer.addVariable("lon", DataType.DOUBLE, dims);
    lonVar.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
    lonVar.addAttribute(new Attribute(CF.STANDARD_NAME, CF.LONGITUDE));
    lonVar.addAttribute(new Attribute(CDM.LONG_NAME, "longitude coordinate"));
    lonVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
  }

  private void addCFAnnotations(CoverageCollection gds, NetcdfFileWriter writer, boolean shouldAddLatLon2D) {
    for (Coverage grid : gds.getCoverages()) {
      CoverageCoordSys gcs = grid.getCoordSys();

      Variable newV = writer.findVariable(grid.getName());
      if (newV == null) {
        logger.error("CFGridCoverageWriter2 cant find " + grid.getName() + " in writer ");
        continue;
      }

      // annotate Variable for CF
      Formatter coordsAttribValFormatter = new Formatter();
      for (String axisName : grid.getCoordSys().getAxisNames()) {
        coordsAttribValFormatter.format("%s ", axisName);
      }

      if (shouldAddLatLon2D) {
        assert writer.findVariable("lat") != null : "We should've added lat variable in addLatLon2D()";
        assert writer.findVariable("lon") != null : "We should've added lon variable in addLatLon2D()";
        coordsAttribValFormatter.format("lat lon");
      }

      newV.addAttribute(new Attribute(CF.COORDINATES, coordsAttribValFormatter.toString()));

      // add reference to coordinate transform variables
      CoverageTransform ct = gcs.getHorizTransform();
      if (ct != null && ct.isHoriz())
        newV.addAttribute(new Attribute(CF.GRID_MAPPING, ct.getName()));

      // LOOK what about vertical ?
    }

    for (CoverageCoordAxis axis : gds.getCoordAxes()) {
      Variable newV = writer.findVariable(axis.getName());
      if (newV == null) {
        logger.error("CFGridCoverageWriter2 cant find " + axis.getName() + " in writer ");
        continue;
      }

      // LOOK: Commented out because CoverageCoordAxis doesn't have any info about "positive" wrt vertical axes.
      // To fix, we'd need to add that metadata when building the CRS.
      /* if ((axis.getAxisType() == AxisType.Height) || (axis.getAxisType() == AxisType.Pressure) ||
            (axis.getAxisType() == AxisType.GeoZ)) {
        if (null != axis.getPositive())
          newV.addAttribute(new Attribute(CF.POSITIVE, axis.getPositive()));
      } */

      if (axis.getAxisType() == AxisType.Lat) {
        newV.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, CF.LATITUDE));
      }
      if (axis.getAxisType() == AxisType.Lon) {
        newV.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, CF.LONGITUDE));
      }
      if (axis.getAxisType() == AxisType.GeoX) {
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_X_COORDINATE));
      }
      if (axis.getAxisType() == AxisType.GeoY) {
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_Y_COORDINATE));
      }
      if (axis.getAxisType() == AxisType.Ensemble) {
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, CF.ENSEMBLE));
      }
    }
  }

  private void writeCoordinateData(CoverageCollection subsetDataset, NetcdfFileWriter writer)
          throws IOException, InvalidRangeException {
    for (CoverageCoordAxis axis : subsetDataset.getCoordAxes()) {
      Variable v = writer.findVariable(axis.getName());
      if (v != null) {
        if (show) System.out.printf("CFGridCoverageWriter2 write axis %s%n", v.getNameAndDimensions());
        writer.write(v, axis.getCoordsAsArray());
      } else {
        logger.error("CFGridCoverageWriter2 No variable for %s%n", axis.getName());
      }

      if (axis.isInterval()) {
        Variable vb = writer.findVariable(axis.getName() + BOUNDS);
        writer.write(vb, axis.getCoordBoundsAsArray());
      }
    }
  }

  private void writeCoverageData(CoverageCollection gdsOrg, SubsetParams subsetParams,
          CoverageCollection subsetDataset, NetcdfFileWriter writer) throws IOException, InvalidRangeException {
    for (Coverage coverage : subsetDataset.getCoverages()) {
      // we need to call readData on the original
      Coverage coverageOrg = gdsOrg.findCoverage(coverage.getName());
      GeoReferencedArray array = coverageOrg.readData(subsetParams);

      // test conform to whatever axis.getCoordsAsArray() returns
      checkConformance(coverage, array, gdsOrg.getName());

      Variable v = writer.findVariable(coverage.getName());
      if (show) System.out.printf("CFGridCoverageWriter2 write coverage %s%n", v.getNameAndDimensions());
      writer.write(v, array.getData());
    }
  }

  private void writeLatLon2D(CoverageCollection subsetDataset, NetcdfFileWriter writer)
          throws IOException, InvalidRangeException {
    HorizCoordSys horizCoordSys = subsetDataset.getHorizCoordSys();
    CoverageCoordAxis1D xAxis = horizCoordSys.getXAxis();
    CoverageCoordAxis1D yAxis = horizCoordSys.getYAxis();

    Projection proj = horizCoordSys.getTransform().getProjection();
    ProjectionPointImpl projPoint = new ProjectionPointImpl();
    LatLonPointImpl latlonPoint = new LatLonPointImpl();

    double[] xData = (double[]) xAxis.getCoordsAsArray().get1DJavaArray(DataType.DOUBLE);
    double[] yData = (double[]) yAxis.getCoordsAsArray().get1DJavaArray(DataType.DOUBLE);

    int numX = xData.length;
    int numY = yData.length;

    double[] latData = new double[numX * numY];
    double[] lonData = new double[numX * numY];

    // create the data
    for (int i = 0; i < numY; i++) {
      for (int j = 0; j < numX; j++) {
        projPoint.setLocation(xData[j], yData[i]);
        proj.projToLatLon(projPoint, latlonPoint);
        latData[i * numX + j] = latlonPoint.getLatitude();
        lonData[i * numX + j] = latlonPoint.getLongitude();
      }
    }

    Variable latVar = writer.findVariable("lat");
    assert latVar != null : "We should have added lat var in addLatLon2D().";
    Array latDataArray = Array.factory(DataType.DOUBLE, new int[] { numY, numX }, latData);
    writer.write(latVar, latDataArray);

    Variable lonVar = writer.findVariable("lon");
    assert lonVar != null : "We should have added lon var in addLatLon2D().";
    Array lonDataArray = Array.factory(DataType.DOUBLE, new int[] { numY, numX }, lonData);
    writer.write(lonVar, lonDataArray);
  }

  private void checkConformance(Coverage gridSubset, GeoReferencedArray geo, String where) {
    CoverageCoordSys csys = gridSubset.getCoordSys();

    CoverageCoordSys csysData = geo.getCoordSysForData();

    //System.out.printf("    csys=%s%n", csys);
    //System.out.printf("csysData=%s%n", csysData);

    Section s = new Section(csys.getShape());
    Section so = new Section(csysData.getShape());

    boolean ok = s.conformal(so);

    int[] dataShape = geo.getData().getShape();
    //System.out.printf("dataShape=%s%n", Misc.showInts(dataShape));
    Section sdata = new Section(dataShape);
    boolean ok2 = s.conformal(sdata);

    if (!ok || !ok2)
      logger.warn("CFGridCoverageWriter2 checkConformance fails " +where);
  }
}
