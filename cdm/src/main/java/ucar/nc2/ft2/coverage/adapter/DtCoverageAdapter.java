/* Copyright */
package ucar.nc2.ft2.coverage.adapter;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapt ucar.nc2.dt.GeoGridDataset to ucar.nc2.ft2.coverage.CoverageDataset
 *
 * @author caron
 * @since 5/1/2015
 */
public class DtCoverageAdapter implements CoverageReader, CoordAxisReader {

  public static CoverageDataset factory(DtCoverageDataset proxy) {
    AttributeContainerHelper atts = new AttributeContainerHelper(proxy.getName());
    atts.addAll(proxy.getGlobalAttributes());

    List<Coverage> pgrids = new ArrayList<>();
    for (DtCoverage dtGrid : proxy.getGrids())
      pgrids.add( makeCoverage(dtGrid));

    List<CoverageCoordSys> pcoordSys = new ArrayList<>();
    for (DtCoverageDataset.Gridset gset : proxy.getGridsets())
      pcoordSys.add(makeCoordSys(gset.getGeoCoordSystem()));

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

    DtCoverageAdapter reader = new DtCoverageAdapter(proxy);

    Set<String> axisNames = new HashSet<>();
    List<CoverageCoordAxis> axes = new ArrayList<>();
    for (DtCoverageDataset.Gridset gset : proxy.getGridsets()) {
      DtCoverageCS gcs = gset.getGeoCoordSystem();
      for (ucar.nc2.dataset.CoordinateAxis axis : gcs.getCoordAxes())
        if (!axisNames.contains(axis.getFullName())) {
          axes.add( makeCoordAxis(axis, reader));
          axisNames.add(axis.getFullName());
        }
    }

    return new CoverageDataset(proxy.getName(), proxy.getCoverageType(), atts,
            proxy.getBoundingBox(), proxy.getProjBoundingBox(), proxy.getCalendarDateRange(),
            pcoordSys, transforms, axes, pgrids, reader);
  }

  private static Coverage makeCoverage(DtCoverage dt) {
    return new Coverage(dt.getName(), dt.getDataType(), dt.getAttributes(), dt.getCoordinateSystem().getName(), dt.getUnitsString(), dt.getDescription());
  }

  private static CoverageCoordSys makeCoordSys(DtCoverageCS dt) {
    List<String> transformNames = new ArrayList<>();
    for (CoordinateTransform ct : dt.getCoordTransforms())
      transformNames.add(ct.getName());
    List<String> axisNames = new ArrayList<>();
    for (CoordinateAxis axis : dt.getCoordAxes()) // LOOK should be just the grid axes ?
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

  private static CoverageCoordAxis makeCoordAxis(ucar.nc2.dataset.CoordinateAxis dtCoordAxis, DtCoverageAdapter reader) {
    String name = dtCoordAxis.getFullName();
    DataType dataType = dtCoordAxis.getDataType();
    AxisType axisType = dtCoordAxis.getAxisType();
    String units = dtCoordAxis.getUnitsString();
    String description = dtCoordAxis.getDescription();

    CoverageCoordAxis.DependenceType dependenceType;
    String dependsOn = null;
    if (dtCoordAxis.isIndependentCoordinate())
      dependenceType = CoverageCoordAxis.DependenceType.independent;
    else if (dtCoordAxis.isScalar())
      dependenceType = CoverageCoordAxis.DependenceType.scalar;
    else {
      dependenceType = CoverageCoordAxis.DependenceType.dependent;
      dependsOn = dtCoordAxis.getDimension(0).toString();
    }

    Calendar cal = axisType.isTime() ? dtCoordAxis.getCalendarFromAttribute() : null;

    int ncoords = (int) dtCoordAxis.getSize();
    CoverageCoordAxis.Spacing spacing = null;
    double startValue = 0.0;
    double endValue = 0.0;
    double resolution = 0.0;
    double[] values = null;

    // 1D case
    if (dtCoordAxis instanceof CoordinateAxis1D) {
      CoordinateAxis1D axis1D = (CoordinateAxis1D) dtCoordAxis;
      startValue = axis1D.getCoordValue(0);
      endValue = axis1D.getCoordValue((int) dtCoordAxis.getSize() - 1);

      if (axis1D.isRegular() || axis1D.isScalar()) {
        spacing = CoverageCoordAxis.Spacing.regular;

      } else if (!dtCoordAxis.isInterval()) {
        spacing = CoverageCoordAxis.Spacing.irregularPoint;
        values = axis1D.getCoordValues();

      } else if (dtCoordAxis.isContiguous()) {
        spacing = CoverageCoordAxis.Spacing.contiguousInterval;
        values = axis1D.getCoordEdges();

      } else {
        spacing = CoverageCoordAxis.Spacing.discontiguousInterval;
        values = new double[2*ncoords];
        double[] bounds1 = axis1D.getBound1();
        double[] bounds2 = axis1D.getBound2();
        int count = 0;
        for (int i=0; i<ncoords; i++) {
          values[count++] = bounds1[i];
          values[count++] = bounds2[i];
        }
      }

      return new CoverageCoordAxis1D(name, units, description, dataType, axisType, dtCoordAxis.getAttributes(), dependenceType, dependsOn, spacing,
                    ncoords, startValue, endValue, resolution, values, reader);

    }

    // Fmrc Time
    if (dtCoordAxis instanceof CoordinateAxis2D && axisType == AxisType.Time) {

      spacing = CoverageCoordAxis.Spacing.regular;

      return new FmrcTimeAxis2D(name, units, description, dataType, axisType, dtCoordAxis.getAttributes(), dependenceType, dependsOn, spacing,
                    ncoords, startValue, endValue, resolution, values, reader);
    }

    // 2D Lat Lon Time
    if (dtCoordAxis instanceof CoordinateAxis2D && (axisType == AxisType.Lat || axisType == AxisType.Lat )) {

      spacing = CoverageCoordAxis.Spacing.regular;

      return new LatLonAxis2D(name, units, description, dataType, axisType, dtCoordAxis.getAttributes(), dependenceType, dependsOn, spacing,
                    ncoords, startValue, endValue, resolution, values, reader);
    }

    throw new IllegalStateException("DOnt know what to do with axis "+dtCoordAxis.getFullName());
  }

  ////////////////////////
  private final DtCoverageDataset proxy;

  private DtCoverageAdapter(DtCoverageDataset proxy) {
    this.proxy = proxy;
  }

  @Override
  public void close() throws IOException {
    proxy.close();
  }

  @Override
  public GeoReferencedArray readData(Coverage coverage, SubsetParams subset) throws IOException {  // LOOK incomplete
    DtCoverage grid = proxy.findGridByName(coverage.getName());
    DtCoverageCS gcs = grid.getCoordinateSystem();

    int ens = -1;
    int level = -1;
    int time = -1;
    int runtime = -1;

    for (String key : subset.getKeys()) {
      switch (key) {

        case SubsetParams.date: { // CalendarDate
          CalendarDate cdate = (CalendarDate) subset.get(key);
          CoordinateAxis taxis = gcs.getTimeAxis();
          if (taxis != null) {
            if (taxis instanceof CoordinateAxis1DTime) {
              CoordinateAxis1DTime time1d = (CoordinateAxis1DTime) taxis;
              time = time1d.findTimeIndexFromCalendarDate(cdate);

            } else if (taxis instanceof CoordinateAxis2D) {
              CoordinateAxis2D time2d = (CoordinateAxis2D) taxis;
              // time = time2d.findTimeIndexFromCalendarDate(cdate);
            }
          }
          break;
        }

        case SubsetParams.latestTime: {
          CoordinateAxis taxis = gcs.getTimeAxis();
          if (taxis != null) time = (int) taxis.getSize() - 1;
          break;
        }

        case SubsetParams.vertCoord: { // double
          CoordinateAxis1D zaxis = gcs.getVerticalAxis();
          if (zaxis != null) level = zaxis.findCoordElement(subset.getDouble(key));
          break;
        }

        case SubsetParams.vertIndex: {
          CoordinateAxis1D zaxis = gcs.getVerticalAxis();
          if (zaxis != null) level = subset.getInteger(key);
          break;
        }

        case SubsetParams.ensCoord: { // double
          CoordinateAxis1D eaxis = gcs.getEnsembleAxis();
          if (eaxis != null) ens = eaxis.findCoordElement(subset.getDouble(key));
          break;
        }
      }
    }

    //int rt_index, int e_index, int t_index, int z_index, int y_index, int x_index
    Array data = grid.readDataSlice(runtime, ens, time, level, -1, -1);
    return new GeoReferencedArray(coverage.getName(), coverage.getDataType(), data, null); // LOOK getCoordSys());
  }


    /*
   * regular: regularly spaced points or intervals (start, end, npts), edges halfway between coords
   * irregularPoint: irregular spaced points (values, npts), edges halfway between coords
   * contiguousInterval: irregular contiguous spaced intervals (values, npts), values are the edges, and there are npts+1, coord halfway between edges
   * discontinuousInterval: irregular discontiguous spaced intervals (values, npts), values are the edges, and there are 2*npts
   */
    @Override
    public double[] readValues(CoverageCoordAxis coordAxis) throws IOException {
      ucar.nc2.dataset.CoordinateAxis dtCoordAxis = proxy.getNetcdfDataset().findCoordinateAxis(coordAxis.getName());

      Array data = dtCoordAxis.read();

      double [] result = new double[ (int) data.getSize()];
      int count = 0;
      while (data.hasNext())
        result[count++] = data.nextDouble();

      return result;

      /*

      switch (coordAxis.getSpacing()) {
        case irregularPoint:
          return dtCoordAxis.getCoordValues();
        case contiguousInterval:
          return dtCoordAxis.getCoordEdges();
        case discontiguousInterval:
          int n = (int) dtCoordAxis.getSize();
          double[] result = new double[2 * n];
          double[] bounds1 = dtCoordAxis.getBound1();
          double[] bounds2 = dtCoordAxis.getBound2();
          int count = 0;
          for (int i = 0; i < n; i++) {
            result[count++] = bounds1[i];
            result[count++] = bounds2[i];
          }
          return result;
      }
      return null; */
    }

}
