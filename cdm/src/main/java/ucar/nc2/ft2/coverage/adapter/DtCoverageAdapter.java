/* Copyright */
package ucar.nc2.ft2.coverage.adapter;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.dataset.*;
import ucar.nc2.ft2.coverage.grid.*;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapt ucar.nc2.dt.GeoGridDataset to ucar.nc2.ft2.coverage.grid.GridCoverageDataset
 *
 * @author caron
 * @since 5/1/2015
 */
public class DtCoverageAdapter extends GridCoverageDataset {
  private DtCoverageDataset proxy;

  public DtCoverageAdapter(DtCoverageDataset proxy) {
    this.proxy = proxy;
    setName(proxy.getLocation());
    setCoverageType(proxy.getCoverageType());
    setGlobalAttributes(proxy.getGlobalAttributes());
    setLatLonBoundingBox(proxy.getBoundingBox());
    setCalendarDateRange(proxy.getCalendarDateRange());
    setProjBoundingBox(proxy.getProjBoundingBox());

    List<GridCoverage> pgrids = new ArrayList<>();
    for (DtCoverage dtGrid : proxy.getGrids())
      pgrids.add(new Grid(dtGrid));
    setGrids(pgrids);

    List<ucar.nc2.ft2.coverage.grid.GridCoordSys> pcoordSys = new ArrayList<>();
    for (DtCoverageDataset.Gridset gset : proxy.getGridsets())
      pcoordSys.add(new CoordSys(gset.getGeoCoordSystem()));
    setCoordSys(pcoordSys);

    Set<String> transformNames = new HashSet<>();
    List<GridCoordTransform> transforms = new ArrayList<>();
    for (DtCoverageDataset.Gridset gset : proxy.getGridsets()) {
      DtCoverageCS gcs = gset.getGeoCoordSystem();
      for (ucar.nc2.dataset.CoordinateTransform ct : gcs.getCoordTransforms())
        if (!transformNames.contains(ct.getName())) {
          transforms.add(new Transform(ct));
          transformNames.add(ct.getName());
        }
    }
    setCoordTransforms(transforms);

    Set<String> axisNames = new HashSet<>();
    List<GridCoordAxis> axes = new ArrayList<>();
    for (DtCoverageDataset.Gridset gset : proxy.getGridsets()) {
      DtCoverageCS gcs = gset.getGeoCoordSystem();
      for (ucar.nc2.dataset.CoordinateAxis axis : gcs.getCoordAxes())
        if (!axisNames.contains(axis.getFullName())) {
          if (axis instanceof CoordinateAxis1D)
            axes.add(new Axis1D((CoordinateAxis1D) axis));
          else
            axes.add(new Axis(axis));
          axisNames.add(axis.getFullName());
        }
    }
    setCoordAxes(axes);
  }

  @Override
  public void close() throws IOException {
    proxy.close();
  }

  private class Grid extends GridCoverage {
    DtCoverage dtGrid;

    Grid(DtCoverage dtGrid) {
      this.dtGrid = dtGrid;
      setName(dtGrid.getName());
      setAtts(dtGrid.getAttributes());
      setDataType(dtGrid.getDataType());
      setCoordSysName(dtGrid.getCoordinateSystem().getName());
      setUnits(dtGrid.getUnitsString());
      setDescription(dtGrid.getDescription());
    }

    @Override
    public Array readData(GridSubset subset) throws IOException {  // LOOK incomplete
      GridCS gcs = (GridCS) dtGrid.getCoordinateSystem();
      int ens = -1;
      int level = -1;
      int time = -1;
      int runtime = -1;

      for (String key : subset.getKeys()) {
        switch (key) {

          case GridSubset.date: // CalendarDate
            CoordinateAxis1DTime taxis = gcs.getTimeAxis();
            if (taxis != null) time = taxis.findTimeIndexFromCalendarDate((CalendarDate) subset.get(key));
            break;

          case GridSubset.vertCoord: // double
            CoordinateAxis1D zaxis = gcs.getVerticalAxis();
            if (zaxis != null) level = zaxis.findCoordElement(subset.getDouble(key));
            break;

          case GridSubset.ensCoord: // double
            CoordinateAxis1D eaxis = gcs.getEnsembleAxis();
            if (eaxis != null) ens = eaxis.findCoordElement(subset.getDouble(key));
            break;
        }
      }
      //int rt_index, int e_index, int t_index, int z_index, int y_index, int x_index
      return dtGrid.readDataSlice(runtime, ens, time, level, -1, -1);
    }

    public Array readSubset(List<Range> ranges) throws IOException, InvalidRangeException {
      return dtGrid.readSubset(ranges);
    }
  }

  private class CoordSys extends GridCoordSys {
    DtCoverageCS dtCoordSys;

    CoordSys(DtCoverageCS dtCoordSys) {
      this.dtCoordSys = dtCoordSys;
      setType(dtCoordSys.getCoverageType());
      setName(dtCoordSys.getName());
      for (CoordinateTransform ct : dtCoordSys.getCoordTransforms())
        addTransformName(ct.getName());
      for (CoordinateAxis axis : dtCoordSys.getCoordAxes()) // LOOK should be just the grid axes
        addAxisName(axis.getFullName());
    }
  }

  private class Transform extends GridCoordTransform {
    ucar.nc2.dataset.CoordinateTransform dtCoordTransform;

    Transform(ucar.nc2.dataset.CoordinateTransform dtCoordTransform) {
      super(dtCoordTransform.getName());
      this.dtCoordTransform = dtCoordTransform;
      setIsHoriz(dtCoordTransform.getTransformType() == TransformType.Projection);
      for (Parameter p : dtCoordTransform.getParameters())
        addAttribute(new Attribute(p));
    }
  }

  private class Axis1D extends GridCoordAxis {
    ucar.nc2.dataset.CoordinateAxis1D dtCoordAxis;
    Spacing spacing;

    Axis1D(ucar.nc2.dataset.CoordinateAxis1D dtCoordAxis) {
      this.dtCoordAxis = dtCoordAxis;
      setName(dtCoordAxis.getFullName());
      setDataType(dtCoordAxis.getDataType());
      setAxisType(dtCoordAxis.getAxisType());
      setUnits(dtCoordAxis.getUnitsString());
      setDescription(dtCoordAxis.getDescription());
      if (dtCoordAxis.isIndependentCoordinate())
        setDependenceType(DependenceType.independent);
      else if (dtCoordAxis.isScalar())
        setDependenceType(DependenceType.scalar);
      else {
        setDependenceType(DependenceType.dependent);
        setDependsOn(dtCoordAxis.getDimension(0).toString());
      }
      AttributeContainerHelper atts = new AttributeContainerHelper("dtCoordAxis");
      for (Attribute patt : dtCoordAxis.getAttributes())
        atts.addAttribute(patt);
      setAttributes(atts);

      setNcoords(dtCoordAxis.getSize());
      setMinIndex(0);
      setMaxIndex(dtCoordAxis.getSize() - 1);

      setStartValue(dtCoordAxis.getCoordValue(0));
      setEndValue(dtCoordAxis.getCoordValue((int) dtCoordAxis.getSize() - 1));
      if (dtCoordAxis.isRegular())
        spacing = Spacing.regular;
      else if (!dtCoordAxis.isInterval())
        spacing = Spacing.irregularPoint;
      else if (dtCoordAxis.isContiguous())
        spacing = Spacing.contiguousInterval;
      else
        spacing = Spacing.discontiguousInterval;
      setSpacing(spacing);
    }

    /*
   * regular: regularly spaced points or intervals (start, end, npts), edges halfway between coords
   * irregularPoint: irregular spaced points (values, npts), edges halfway between coords
   * contiguousInterval: irregular contiguous spaced intervals (values, npts), values are the edges, and there are npts+1, coord halfway between edges
   * discontinuousInterval: irregular discontiguous spaced intervals (values, npts), values are the edges, and there are 2*npts
   */
    @Override
    public double[] readValues() {
      switch (spacing) {
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

  private class Axis extends GridCoordAxis {
    ucar.nc2.dataset.CoordinateAxis dtCoordAxis;
    Spacing spacing = Spacing.regular;

    Axis(ucar.nc2.dataset.CoordinateAxis dtCoordAxis) {
      this.dtCoordAxis = dtCoordAxis;
      setName(dtCoordAxis.getFullName());
      setDataType(dtCoordAxis.getDataType());
      setAxisType(dtCoordAxis.getAxisType());
      setUnits(dtCoordAxis.getUnitsString());
      setDescription(dtCoordAxis.getDescription());
      if (dtCoordAxis.isIndependentCoordinate())
        setDependenceType(DependenceType.independent);
      else if (dtCoordAxis.isScalar())
        setDependenceType(DependenceType.scalar);
      else if (dtCoordAxis instanceof CoordinateAxis2D) {
        setDependenceType(DependenceType.twoD);
        setDependsOn(dtCoordAxis.getDimensionsString());
      } else {
        setDependenceType(DependenceType.dependent);
        setDependsOn(dtCoordAxis.getDimension(0).toString());
      }
      AttributeContainerHelper atts = new AttributeContainerHelper("dtCoordAxis");
      for (Attribute patt : dtCoordAxis.getAttributes())
        atts.addAttribute(patt);
      setAttributes(atts);

      setNcoords(dtCoordAxis.getSize());
      setMinIndex(0);
      setMaxIndex(dtCoordAxis.getSize() - 1);

      /* setStartValue(dtCoordAxis.getCoordValue(0));
      setEndValue(dtCoordAxis.getCoordValue((int) dtCoordAxis.getSize() - 1));
      if (dtCoordAxis.isRegular())
        spacing = Spacing.regular;
      else if (!dtCoordAxis.isInterval())
        spacing = Spacing.irregularPoint;
      else if (dtCoordAxis.isContiguous())
        spacing = Spacing.contiguousInterval;
      else
        spacing = Spacing.discontiguousInterval; */
      setSpacing(spacing);
    }

    /*
   * regular: regularly spaced points or intervals (start, end, npts), edges halfway between coords
   * irregularPoint: irregular spaced points (values, npts), edges halfway between coords
   * contiguousInterval: irregular contiguous spaced intervals (values, npts), values are the edges, and there are npts+1, coord halfway between edges
   * discontinuousInterval: irregular discontiguous spaced intervals (values, npts), values are the edges, and there are 2*npts
   */
    @Override
    public double[] readValues() {
      /* switch (spacing) {
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
      } */
      return null;
    }
  }

}
