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
package ucar.nc2.ft2.coverage.adapter;

import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.util.Misc;
import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.*;

/**
 * Adapt a DtCoverageDataset to a ucar.nc2.ft2.coverage.CoverageCollection
 * Forked from ucar.nc2.dt.GeoGridDataset
 *
 * @author caron
 * @since 5/1/2015
 */
public class DtCoverageAdapter implements CoverageReader, CoordAxisReader {

  public static FeatureDatasetCoverage factory(DtCoverageDataset proxy, Formatter errlog) {
    DtCoverageAdapter reader = new DtCoverageAdapter(proxy);

    AttributeContainerHelper atts = new AttributeContainerHelper(proxy.getName());
    atts.addAll(proxy.getGlobalAttributes());

    List<Coverage> pgrids = new ArrayList<>();
    for (DtCoverage dtGrid : proxy.getGrids())
      pgrids.add(makeCoverage(dtGrid, reader));

    Set<String> transformNames = new HashSet<>();
    List<CoverageTransform> transforms = new ArrayList<>();
    for (DtCoverageDataset.Gridset gset : proxy.getGridsets()) {
      DtCoverageCS gcs = gset.getGeoCoordSystem();
      for (ucar.nc2.dataset.CoordinateTransform ct : gcs.getCoordTransforms())
        if (!transformNames.contains(ct.getName())) {
          transforms.add(makeTransform(ct));
          transformNames.add(ct.getName());
        }
    }

    List<CoverageCoordSys> pcoordSys = new ArrayList<>();
    List<CoverageCoordAxis> axes = new ArrayList<>();

    Set<String> allAxisNameSet = new HashSet<>();
    for (DtCoverageDataset.Gridset gset : proxy.getGridsets()) {
      DtCoverageCS gcs = gset.getGeoCoordSystem();
      for (ucar.nc2.dataset.CoordinateAxis axis : gcs.getCoordAxes()) {
        if (!allAxisNameSet.contains(axis.getFullName())) {
          ucar.nc2.util.Optional<CoverageCoordAxis> opt = makeCoordAxis(gcs, axis, reader);
          if (opt.isPresent()) {
            axes.add(opt.get());
            allAxisNameSet.add(axis.getFullName());
          } else {
            errlog.format("%s%n", opt.getErrorMessage());
          }
        }
      }
      pcoordSys.add(makeCoordSys(gset.getGeoCoordSystem(), allAxisNameSet));
    }

    CoverageCollection cd = new CoverageCollection(proxy.getName(), proxy.getCoverageType(), atts,
            null, null, proxy.getCalendarDateRange(),
            pcoordSys, transforms, axes, pgrids, reader);

    return new FeatureDatasetCoverage(reader.getLocation(), reader, cd);
  }

  private static Coverage makeCoverage(DtCoverage dt, DtCoverageAdapter reader) {
    return new Coverage(dt.getName(), dt.getDataType(), dt.getAttributes(), dt.getCoordinateSystem().getName(), dt.getUnitsString(), dt.getDescription(), reader, dt);
  }

  private static CoverageCoordSys makeCoordSys(DtCoverageCS dt, Set<String> allAxisNameSet) {
    List<String> transformNames = new ArrayList<>();
    for (CoordinateTransform ct : dt.getCoordTransforms())
      transformNames.add(ct.getName());
    List<String> axisNames = new ArrayList<>();
    for (CoordinateAxis axis : dt.getCoordAxes())
      if (allAxisNameSet.contains(axis.getFullName())) // some may have been rejected
        axisNames.add(axis.getFullName());

    return new CoverageCoordSys(dt.getName(), axisNames, transformNames, dt.getCoverageType());
  }

  private static CoverageTransform makeTransform(ucar.nc2.dataset.CoordinateTransform dt) {
    AttributeContainerHelper atts = new AttributeContainerHelper(dt.getName());
    for (Parameter p : dt.getParameters())
      atts.addAttribute(new Attribute(p));
    //   public CoverageCoordTransform(String name, AttributeContainerHelper attributes, boolean isHoriz) {
    return new CoverageTransform(dt.getName(), atts, dt.getTransformType() == TransformType.Projection);
  }

  private static ucar.nc2.util.Optional<CoverageCoordAxis> makeCoordAxis(DtCoverageCS gcs, ucar.nc2.dataset.CoordinateAxis dtCoordAxis, DtCoverageAdapter reader) {
    String name = dtCoordAxis.getFullName();
    DataType dataType = dtCoordAxis.getDataType();
    AxisType axisType = dtCoordAxis.getAxisType();
    String units = dtCoordAxis.getUnitsString();
    String description = dtCoordAxis.getDescription();
    AttributeContainer atts = dtCoordAxis.getAttributeContainer();

    if (axisType == null)
      return ucar.nc2.util.Optional.empty("Coordinate "+name+" has no axisType");

    CoverageCoordAxis.DependenceType dependenceType;
    String dependsOn = null;
    if (dtCoordAxis.isIndependentCoordinate())
      dependenceType = CoverageCoordAxis.DependenceType.independent;

    else if (dtCoordAxis.isScalar())
      dependenceType = CoverageCoordAxis.DependenceType.scalar;

    else if (dtCoordAxis instanceof CoordinateAxis2D) {
      dependenceType = CoverageCoordAxis.DependenceType.twoD;
      Formatter f = new Formatter();
      for (Dimension d : dtCoordAxis.getDimensions())  // LOOK axes may not exist
        f.format("%s ", d.getFullName());
      dependsOn = f.toString().trim();

    } else {
      Dimension d0 = dtCoordAxis.getDimension(0);
      if (dtCoordAxis.getRank() == 1 && null == gcs.findCoordAxis(d0.getShortName())) { // use it as independent
        dependenceType = CoverageCoordAxis.DependenceType.independent;

      } else {
        dependenceType = CoverageCoordAxis.DependenceType.dependent;
        Formatter f = new Formatter();
        for (Dimension d : dtCoordAxis.getDimensions())
          f.format("%s ", d.getFullName());
        dependsOn = f.toString().trim();
      }
    }

    int ncoords = (int) dtCoordAxis.getSize();
    CoverageCoordAxis.Spacing spacing;
    double startValue = 0.0;
    double endValue = 0.0;
    double resolution = 0.0;
    double[] values;

    // 1D case
    if (dtCoordAxis instanceof CoordinateAxis1D) {
      CoordinateAxis1D axis1D = (CoordinateAxis1D) dtCoordAxis;
      startValue = axis1D.getCoordValue(0);
      endValue = axis1D.getCoordValue((int) dtCoordAxis.getSize() - 1);

      if (axis1D.isRegular() || axis1D.isScalar()) {
        spacing = CoverageCoordAxis.Spacing.regularPoint;
        values = null;
        resolution = (ncoords == 1) ? 0.0 : (endValue - startValue) / (ncoords-1);

      } else if (!dtCoordAxis.isInterval()) {
        spacing = CoverageCoordAxis.Spacing.irregularPoint;
        values = axis1D.getCoordValues();
        resolution = (ncoords == 1) ? 0.0 : (endValue - startValue) / (ncoords-1);

      } else if (dtCoordAxis.isContiguous()) {
        spacing = CoverageCoordAxis.Spacing.contiguousInterval;
        values = axis1D.getCoordEdges();
        resolution = (endValue - startValue) / ncoords;

      } else {
        spacing = CoverageCoordAxis.Spacing.discontiguousInterval;
        values = new double[2 * ncoords];
        double[] bounds1 = axis1D.getBound1();
        double[] bounds2 = axis1D.getBound2();
        int count = 0;
        for (int i = 0; i < ncoords; i++) {
          values[count++] = bounds1[i];
          values[count++] = bounds2[i];
        }
        resolution = (endValue - startValue) / ncoords;
      }

      CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder();
      builder.name = name;
      builder.units = units;
      builder.description = description;
      builder.dataType = dataType;
      builder.axisType = axisType;
      builder.attributes = atts;
      builder.dependenceType = dependenceType;
      builder.setDependsOn(dependsOn);
      builder.spacing = spacing;
      builder.ncoords = ncoords;
      builder.startValue = startValue;
      builder.endValue = endValue;
      builder.resolution = resolution;
      builder.values = values;
      builder.reader = reader;
      builder.isSubset = false;

      if (axisType == AxisType.TimeOffset)
        return ucar.nc2.util.Optional.of(new TimeOffsetAxis(builder));
      else
       return ucar.nc2.util.Optional.of(new CoverageCoordAxis1D(builder));
    }

    // TwoD case
    if (!(dtCoordAxis instanceof CoordinateAxis2D))
      throw new IllegalStateException("Dont know what to do with axis " + dtCoordAxis);

    CoordinateAxis2D axis2D = (CoordinateAxis2D) dtCoordAxis;

    if (!dtCoordAxis.isInterval()) {
      spacing = CoverageCoordAxis.Spacing.irregularPoint;
      values = axis2D.getCoordValues();

    } else if (dtCoordAxis.isContiguous()) {
      spacing = CoverageCoordAxis.Spacing.contiguousInterval;
      ArrayDouble.D3 bounds = axis2D.getCoordBoundsArray();
      if (bounds == null) throw new IllegalStateException("No bounds array");
      int[] shape = bounds.getShape();
      int count = 0;
      values = new double[ncoords+1];
      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          values[count++] = bounds.get(i, j, 0);
        }
      }
      values[count] = bounds.get(shape[0]-1, shape[1]-1, 1); // last edge

    } else {
      spacing = CoverageCoordAxis.Spacing.discontiguousInterval;
      ArrayDouble.D3 bounds = axis2D.getCoordBoundsArray();
      if (bounds == null) throw new IllegalStateException("No bounds array");
      int[] shape = bounds.getShape();
      int count = 0;
      values = new double[2 * ncoords];
      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          values[count++] = bounds.get(i, j, 0);
          values[count++] = bounds.get(i, j, 1);
        }
      }
    }

    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder();
    builder.name = name;
    builder.units = units;
    builder.description = description;
    builder.dataType = dataType;
    builder.axisType = axisType;
    builder.attributes = dtCoordAxis.getAttributeContainer();
    builder.dependenceType = dependenceType;
    builder.setDependsOn(dependsOn);
    builder.spacing = spacing;
    builder.ncoords = ncoords;
    builder.startValue = startValue;
    builder.endValue = endValue;
    builder.resolution = resolution;
    builder.values = values;
    builder.reader = reader;
    builder.isSubset = false;
    builder.shape = dtCoordAxis.getShape();

    // Fmrc Time
    if (axisType == AxisType.Time) {
      builder.setDependsOn(dtCoordAxis.getDimension(0).getFullName());  // only the first dimension
      return ucar.nc2.util.Optional.of( new FmrcTimeAxis2D(builder));
    }

    // 2D Lat Lon
    if (axisType == AxisType.Lat || axisType == AxisType.Lon) {
      return ucar.nc2.util.Optional.of( new LatLonAxis2D(builder));
    }

    throw new IllegalStateException("Dont know what to do with axis " + dtCoordAxis);
  }

  ////////////////////////
  private final DtCoverageDataset proxy;

  private DtCoverageAdapter(DtCoverageDataset proxy) {
    this.proxy = proxy;
  }


  //////////////////////////////////////////////////////////////////////////////////////
  // CoverageReader

  @Override
  public void close() throws IOException {
    proxy.close();
  }

  @Override
  public String getLocation() {
    return proxy.getLocation();
  }

  @Override
  public GeoReferencedArray readData(Coverage coverage, SubsetParams params, boolean canonicalOrder) throws IOException, InvalidRangeException {
    DtCoverage grid = (DtCoverage) coverage.getUserObject();
    CoverageCoordSys orgCoordSys = coverage.getCoordSys();
    ucar.nc2.util.Optional<CoverageCoordSys> opt = orgCoordSys.subset(params);
    if (!opt.isPresent())
      throw new InvalidRangeException(opt.getErrorMessage());

    CoverageCoordSys subsetCoordSys = opt.get();
    List<RangeIterator> rangeIters = subsetCoordSys.getRanges();
    List<Range> ranges = new ArrayList<>();

    boolean hasComposite = false;
    for (RangeIterator ri : rangeIters) {
      if (ri instanceof RangeComposite) hasComposite = true;
      else ranges.add( (Range) ri);
    }

    if (!hasComposite) {
      Array data = grid.readDataSection(new Section(ranges), canonicalOrder);
      return new GeoReferencedArray(coverage.getName(), coverage.getDataType(), data, subsetCoordSys);
    }

    // Could use an Array composite here, if we had one
    Array result = Array.factory(coverage.getDataType(), subsetCoordSys.getShape());
    System.out.printf(" read %s result shape=%s%n", coverage.getName(), Misc.showInts(result.getShape()));
    int[] origin = new int[result.getRank()]; // all zeroes

    ranges.add(null); // make room for last
    int n = rangeIters.size();
    RangeComposite comp = (RangeComposite) rangeIters.get(n-1);
    for (RangeIterator ri : comp.getRanges()) {
      // read the data
      ranges.set(n - 1, (Range) ri.setName(comp.getName())); // add last
      Section want = new Section(ranges);
      System.out.printf("  composite read section=%s%n", want);
      Array data = grid.readDataSection(want, canonicalOrder);

      //where does it go?
      Section dataSection = new Section(data.getShape());
      Section sectionInResult = dataSection.shiftOrigin(origin);
      System.out.printf("  sectionInResult=%s%n", sectionInResult);

      // copy it there
      IndexIterator resultIter = result.getRangeIterator(sectionInResult.getRanges());
      IndexIterator dataIter = data.getIndexIterator();
      while (dataIter.hasNext())
        resultIter.setDoubleNext( dataIter.getDoubleNext()); // look converting to double

      // get ready for next
      origin[n-1] += data.getShape()[n-1]; // look assumes compose only in last dimension; could be generalized
    }

    return new GeoReferencedArray(coverage.getName(), coverage.getDataType(), result, subsetCoordSys);
  }


  //////////////////////////////////////////////////////////////////////////////////////
  // CoordAxisReader

  /*
 * regular: regularly spaced points or intervals (start, end, npts), edges halfway between coords
 * irregularPoint: irregular spaced points (values, npts), edges halfway between coords
 * contiguousInterval: irregular contiguous spaced intervals (values, npts), values are the edges, and there are npts+1, coord halfway between edges
 * discontinuousInterval: irregular discontiguous spaced intervals (values, npts), values are the edges, and there are 2*npts
 */

  /*
    The coordAxis describes the subset wanted.
    LOOK this just reads the entire set of values from the original...
   */
  @Override
  public double[] readCoordValues(CoverageCoordAxis coordAxis) throws IOException {
    ucar.nc2.dataset.CoordinateAxis dtCoordAxis = proxy.getNetcdfDataset().findCoordinateAxis(coordAxis.getName());
    if (dtCoordAxis == null) throw new IllegalStateException("Cants find Coordinate Axis "+coordAxis.getName());

    if (dtCoordAxis instanceof CoordinateAxis1D) {

      CoordinateAxis1D axis1D = (CoordinateAxis1D) dtCoordAxis;

      switch (coordAxis.getSpacing()) {
        case irregularPoint:
          return axis1D.getCoordValues();

        case contiguousInterval:
          return axis1D.getCoordEdges();

        case discontiguousInterval:
          int n = (int) dtCoordAxis.getSize();
          double[] result = new double[2 * n];
          double[] bounds1 = axis1D.getBound1();
          double[] bounds2 = axis1D.getBound2();
          int count = 0;
          for (int i = 0; i < n; i++) {
            result[count++] = bounds1[i];
            result[count++] = bounds2[i];
          }
          return result;
      }
    }

    // twoD case i guess
    Array data = dtCoordAxis.read();

    double[] result = new double[(int) data.getSize()];
    int count = 0;
    while (data.hasNext())
      result[count++] = data.nextDouble();

    return result;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /*



    //DtCoverageCS gcs = grid.getCoordinateSystem();
    CoverageCoordSys ccsys = coverage.getCoordSys();

    List<Range> section = new ArrayList<>();
    for (String key : params.getKeys()) {
      switch (key) {

        case SubsetParams.time: { // CalendarDate
          CalendarDate cdate = (CalendarDate) params.get(key);
          CoverageCoordAxis taxisOrg = ccsys.getTimeAxis();
          if (taxisOrg != null && !taxisOrg.isScalar()) {
            CoverageCoordAxis taxisWant = taxisOrg.subset(params);
            section.add(new Range(AxisType.Time.toString(), taxisWant.getMinIndex(), taxisWant.getMaxIndex()));

          if (taxis != null && !taxis.isScalar()) {
            if (taxis instanceof CoordinateAxis1DTime) {
              CoordinateAxis1DTime time1d = (CoordinateAxis1DTime) taxis;
              int wantIndex = time1d.findTimeIndexFromCalendarDate(cdate);               // LOOK change to closest
              section.add(new Range(AxisType.Time.toString(), wantIndex, wantIndex));

            } else if (taxis instanceof CoordinateAxis2D) {      // LOOK what to do ?
              // CoordinateAxis2D time2d = (CoordinateAxis2D) taxis;
              // time = time2d.findTimeIndexFromCalendarDate(cdate);
            }
          }
          break;
        }

        case SubsetParams.timeRange: { // CalendarDate
          CalendarDateRange dateRange = (CalendarDateRange) params.get(key);
          CoordinateAxis taxis = gcs.getTimeAxis();
          if (taxis != null && !taxis.isScalar()) {
            if (taxis instanceof CoordinateAxis1DTime) {
              CoordinateAxis1DTime time1d = (CoordinateAxis1DTime) taxis;
              int startIndex = time1d.findTimeIndexFromCalendarDate(dateRange.getStart());   // LOOK change to closest
              int endIndex = time1d.findTimeIndexFromCalendarDate(dateRange.getEnd());
              Integer stride = params.getInteger(SubsetParams.timeStride);
              if (stride == null)
                section.add(new Range(AxisType.Time.toString(), startIndex, endIndex));
              else
                section.add(new Range(AxisType.Time.toString(), startIndex, endIndex, stride));
            }
          }
          break;
        }

        case SubsetParams.timePresent: {
          CoordinateAxis taxis = gcs.getTimeAxis();
          if (taxis != null && !taxis.isScalar()) {
            int wantIndex = (int) taxis.getSize() - 1;
            section.add(new Range(AxisType.Time.toString(), wantIndex, wantIndex));
          }
          break;
        }

        case SubsetParams.runtime: { // CalendarDate
          CalendarDate cdate = (CalendarDate) params.get(key);
          CoordinateAxis1DTime rtaxis = gcs.getRunTimeAxis();
          if (rtaxis != null && !rtaxis.isScalar()) {
            int wantIndex = rtaxis.findTimeIndexFromCalendarDate(cdate);                   // LOOK change to closest
            section.add(new Range(AxisType.RunTime.toString(), wantIndex, wantIndex));
          }
          break;
        }

        case SubsetParams.runtimeRange: { // CalendarDateRange
          CalendarDateRange dateRange = (CalendarDateRange) params.get(key);
          CoordinateAxis1DTime rtaxis = gcs.getRunTimeAxis();
          if (rtaxis != null && !rtaxis.isScalar()) {
            int startIndex = rtaxis.findTimeIndexFromCalendarDate(dateRange.getStart());   // LOOK change to closest
            int endIndex = rtaxis.findTimeIndexFromCalendarDate(dateRange.getEnd());
            section.add(new Range(AxisType.RunTime.toString(), startIndex, endIndex));
          }
          break;
        }

        case SubsetParams.vertCoord: { // double
          CoordinateAxis1D zaxis = gcs.getVerticalAxis();
          if (zaxis != null && !zaxis.isScalar()) {
            double zval = params.getDouble(key);
            int wantIndex = zaxis.findCoordElement(zval);
            if (wantIndex < 0) {
              System.out.println("HEY");
              zaxis.findCoordElement(zval);
            }
            section.add(new Range(AxisType.GeoZ.toString(), wantIndex, wantIndex));
          }
          break;
        }

        case SubsetParams.vertIndex: {    // LOOK should we allow this ??
          CoordinateAxis1D zaxis = gcs.getVerticalAxis();
          if (zaxis != null && !zaxis.isScalar()) {
            int wantIndex = params.getInteger(key);
            section.add(new Range(AxisType.GeoZ.toString(), wantIndex, wantIndex));
          }
          break;
        }

        case SubsetParams.ensCoord: { // double
          CoordinateAxis1D eaxis = gcs.getEnsembleAxis();
          if (eaxis != null && !eaxis.isScalar()) {
            int wantIndex = eaxis.findCoordElement(params.getDouble(key));
            section.add(new Range(AxisType.Ensemble.toString(), wantIndex, wantIndex));
          }
          break;
        }

        case SubsetParams.projBB: { // ProjRect
          if (!ccsys.getHorizCoordSys().hasProjection) continue;
          ProjectionRect projRect = (ProjectionRect) params.get(key);
          ccsys.getHorizCoordSys().subset(params);
          break;
        }

      }
    }
   */

}
