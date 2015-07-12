/* Copyright */
package ucar.nc2.ft2.coverage.adapter;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.ft2.coverage.CoverageSubset;
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
public class DtCoverageAdapter extends CoverageDataset {

  public static CoverageDataset factory(DtCoverageDataset proxy) {

    AttributeContainerHelper atts = new AttributeContainerHelper(proxy.getName());
    atts.addAll(proxy.getGlobalAttributes());

    List<Coverage> pgrids = new ArrayList<>();
    for (DtCoverage dtGrid : proxy.getGrids())
      pgrids.add(new CoverageAdaptor(dtGrid));

    List<CoverageCoordSys> pcoordSys = new ArrayList<>();
    for (DtCoverageDataset.Gridset gset : proxy.getGridsets())
      pcoordSys.add(makeCoordSys(gset.getGeoCoordSystem()));

    Set<String> transformNames = new HashSet<>();
    List<CoverageCoordTransform> transforms = new ArrayList<>();
    for (DtCoverageDataset.Gridset gset : proxy.getGridsets()) {
      DtCoverageCS gcs = gset.getGeoCoordSystem();
      for (ucar.nc2.dataset.CoordinateTransform ct : gcs.getCoordTransforms())
        if (!transformNames.contains(ct.getName())) {
          transforms.add(makeTransform(ct));
          transformNames.add(ct.getName());
        }
    }

    Set<String> axisNames = new HashSet<>();
    List<CoverageCoordAxis> axes = new ArrayList<>();
    for (DtCoverageDataset.Gridset gset : proxy.getGridsets()) {
      DtCoverageCS gcs = gset.getGeoCoordSystem();
      for (ucar.nc2.dataset.CoordinateAxis axis : gcs.getCoordAxes())
        if (!axisNames.contains(axis.getFullName())) {
          axes.add(coordAxisFactory(axis));
          axisNames.add(axis.getFullName());
        }
    }

    return new DtCoverageAdapter(proxy, atts, pcoordSys, transforms, axes, pgrids);
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


  private static CoverageCoordTransform makeTransform(ucar.nc2.dataset.CoordinateTransform dt) {
    AttributeContainerHelper atts = new AttributeContainerHelper(dt.getName());
    for (Parameter p : dt.getParameters())
      atts.addAttribute(new Attribute(p));
    //   public CoverageCoordTransform(String name, AttributeContainerHelper attributes, boolean isHoriz) {
    return new CoverageCoordTransform(dt.getName(), atts, dt.getTransformType() == TransformType.Projection);
  }

  ////////////////////////
  private final DtCoverageDataset proxy;

  private DtCoverageAdapter(DtCoverageDataset proxy, AttributeContainerHelper atts,
                            List<CoverageCoordSys> coordSys, List<CoverageCoordTransform> coordTransforms,
                            List<CoverageCoordAxis> coordAxes, List<Coverage> coverages) {

    super(proxy.getName(), atts, proxy.getBoundingBox(), proxy.getProjBoundingBox(), proxy.getCalendarDateRange(), coordSys, coordTransforms,
            coordAxes, coverages, proxy.getCoverageType());
    this.proxy = proxy;
    // LOOK proxy.getCoverageType();
  }

  @Override
  public void close() throws IOException {
    proxy.close();
  }

  private static class CoverageAdaptor extends Coverage {
    private final DtCoverage dtGrid;

    CoverageAdaptor(DtCoverage dtGrid) {
      super(dtGrid.getName(), dtGrid.getDataType(), dtGrid.getAttributes(), dtGrid.getCoordinateSystem().getName(), dtGrid.getUnitsString(), dtGrid.getDescription());
      this.dtGrid = dtGrid;
    }

    @Override
    public ArrayWithCoordinates readData(CoverageSubset subset) throws IOException {  // LOOK incomplete
      DtCoverageCS gcs = dtGrid.getCoordinateSystem();
      int ens = -1;
      int level = -1;
      int time = -1;
      int runtime = -1;

      for (String key : subset.getKeys()) {
        switch (key) {

          case CoverageSubset.date: { // CalendarDate
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

          case CoverageSubset.latestTime: {
            CoordinateAxis taxis = gcs.getTimeAxis();
            if (taxis != null) time = (int) taxis.getSize() - 1;
            break;
          }

          case CoverageSubset.vertCoord: { // double
            CoordinateAxis1D zaxis = gcs.getVerticalAxis();
            if (zaxis != null) level = zaxis.findCoordElement(subset.getDouble(key));
            break;
          }

          case CoverageSubset.vertIndex: {
            CoordinateAxis1D zaxis = gcs.getVerticalAxis();
            if (zaxis != null) level = subset.getInteger(key);
            break;
          }

          case CoverageSubset.ensCoord: { // double
            CoordinateAxis1D eaxis = gcs.getEnsembleAxis();
            if (eaxis != null) ens = eaxis.findCoordElement(subset.getDouble(key));
            break;
          }
        }
      }
      //int rt_index, int e_index, int t_index, int z_index, int y_index, int x_index
      Array data = dtGrid.readDataSlice(runtime, ens, time, level, -1, -1);
      return new ArrayWithCoordinates(data, getCoordSys());
    }

    public Array readSubset(List<Range> ranges) throws IOException, InvalidRangeException {
      return dtGrid.readSubset(ranges);
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static CoverageCoordAxis coordAxisFactory(ucar.nc2.dataset.CoordinateAxis dtCoordAxis) {
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

    AttributeContainerHelper atts = new AttributeContainerHelper(name);
    for (Attribute patt : dtCoordAxis.getAttributes())
      atts.addAttribute(patt);

    int ncoords = (int) dtCoordAxis.getSize();
    CoverageCoordAxis.Spacing spacing = null;
    double startValue = 0.0;
    double endValue = 0.0;
    double resolution = 0.0;

    if (dtCoordAxis instanceof CoordinateAxis1D) {
      CoordinateAxis1D dtCoordAxis1D = (CoordinateAxis1D) dtCoordAxis;
      startValue = dtCoordAxis1D.getCoordValue(0);
      endValue = dtCoordAxis1D.getCoordValue((int) dtCoordAxis.getSize() - 1);

      spacing = null;
      if (dtCoordAxis1D.isRegular())
        spacing = CoverageCoordAxis.Spacing.regular;
      else if (!dtCoordAxis.isInterval())
        spacing = CoverageCoordAxis.Spacing.irregularPoint;
      else if (dtCoordAxis.isContiguous())
        spacing = CoverageCoordAxis.Spacing.contiguousInterval;
      else
        spacing = CoverageCoordAxis.Spacing.discontiguousInterval;
    }

    if (dtCoordAxis instanceof CoordinateAxis1DTime)
      return new Axis1DTime((CoordinateAxis1DTime) dtCoordAxis, name, units, description, dataType, axisType, atts, dependenceType, dependsOn, spacing,
              ncoords, startValue, endValue, resolution, null);

    else if (dtCoordAxis instanceof CoordinateAxis1D)
      return new Axis1D((CoordinateAxis1D) dtCoordAxis, name, units, description, dataType, axisType, atts, dependenceType, dependsOn, spacing,
              ncoords, startValue, endValue, resolution, null);
    else
       return new Axis(dtCoordAxis, name, units, description, dataType, axisType, atts, dependenceType, dependsOn, spacing,
               ncoords, startValue, endValue, resolution, null);
   }

  private static class Axis1DTime extends CoverageCoordAxisTime {
    ucar.nc2.dataset.CoordinateAxis1DTime dtCoordAxis;

    Axis1DTime(ucar.nc2.dataset.CoordinateAxis1DTime dtCoordAxis, String name, String units, String description, DataType dataType, AxisType axisType, AttributeContainer attributes,
           DependenceType dependenceType, String dependsOn, Spacing spacing, int ncoords, double startValue, double endValue, double resolution,
           double[] values) {

      super(name, units, description, dataType, axisType, attributes, dependenceType, dependsOn, spacing, ncoords, startValue, endValue, resolution, values,
              dtCoordAxis.getCalendar());

      this.dtCoordAxis = dtCoordAxis;
    }
  }



  private static class Axis1D extends CoverageCoordAxis {
    ucar.nc2.dataset.CoordinateAxis1D dtCoordAxis;

    Axis1D(ucar.nc2.dataset.CoordinateAxis1D dtCoordAxis, String name, String units, String description, DataType dataType, AxisType axisType, AttributeContainer attributes,
                               DependenceType dependenceType, String dependsOn, Spacing spacing, int ncoords, double startValue, double endValue, double resolution,
                               double[] values) {

      super(name, units, description, dataType, axisType, attributes, dependenceType, dependsOn, spacing,
              ncoords, startValue, endValue, resolution, values);

      this.dtCoordAxis = dtCoordAxis;
    }

    /*
   * regular: regularly spaced points or intervals (start, end, npts), edges halfway between coords
   * irregularPoint: irregular spaced points (values, npts), edges halfway between coords
   * contiguousInterval: irregular contiguous spaced intervals (values, npts), values are the edges, and there are npts+1, coord halfway between edges
   * discontinuousInterval: irregular discontiguous spaced intervals (values, npts), values are the edges, and there are 2*npts
   */
    @Override
    public double[] readValues() {
      switch (getSpacing()) {
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
      return null;
    }
  }

  private static class Axis extends CoverageCoordAxis {
    ucar.nc2.dataset.CoordinateAxis dtCoordAxis;

    Axis(ucar.nc2.dataset.CoordinateAxis dtCoordAxis, String name, String units, String description, DataType dataType, AxisType axisType, AttributeContainer attributes,
                               DependenceType dependenceType, String dependsOn, Spacing spacing, int ncoords, double startValue, double endValue, double resolution,
                               double[] values) {

      super(name, units, description, dataType, axisType, attributes, dependenceType, dependsOn, spacing, ncoords, startValue, endValue, resolution, values);

      this.dtCoordAxis = dtCoordAxis;
    }

  /*
   * regular: regularly spaced points or intervals (start, end, npts), edges halfway between coords
   * irregularPoint: irregular spaced points (values, npts), edges halfway between coords
   * contiguousInterval: irregular contiguous spaced intervals (values, npts), values are the edges, and there are npts+1, coord halfway between edges
   * discontinuousInterval: irregular discontiguous spaced intervals (values, npts), values are the edges, and there are 2*npts
   *
    @Override
    public double[] readValues() {
      switch (getSpacing()) {
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
      return null;
    }  */
  }

}
