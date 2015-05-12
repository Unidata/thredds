/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.TransformType;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDataset.Gridset;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.*;

/**
 * Adapt ucar.nc2.dt.GridDataset to ucar.nc2.ft.cover.grid.GridCoverageDatasetIF
 *
 * @author caron
 * @since 5/1/2015
 */
public class DtGridDatasetAdapter extends GridCoverageDataset {
  private GridDataset proxy;

  public DtGridDatasetAdapter(GridDataset proxy) {
    this.proxy = proxy;
    setName(proxy.getLocation());
    setGlobalAttributes(proxy.getGlobalAttributes());
    setLatLonBoundingBox(proxy.getBoundingBox());
    setCalendarDateRange(proxy.getCalendarDateRange());
    setProjBoundingBox(proxy.getProjBoundingBox());

    List<GridCoverage> pgrids = new ArrayList<>();
    for (GridDatatype dtGrid : proxy.getGrids())
      pgrids.add(new Grid(dtGrid));
    setGrids(pgrids);

    List<ucar.nc2.ft2.coverage.grid.GridCoordSys> pcoordSys = new ArrayList<>();
    for (Gridset gset : proxy.getGridsets())
      pcoordSys.add(new CoordSys(gset.getGeoCoordSystem()));
    setCoordSys(pcoordSys);

    Set<String> transformNames = new HashSet<>();
    List<GridCoordTransform> transforms = new ArrayList<>();
    for (Gridset gset : proxy.getGridsets()) {
      ucar.nc2.dt.GridCoordSystem gcs = gset.getGeoCoordSystem();
      for (ucar.nc2.dataset.CoordinateTransform ct : gcs.getCoordinateTransforms())
        if (!transformNames.contains(ct.getName())) {
          transforms.add(new Transform(ct));
          transformNames.add(ct.getName());
        }
    }
    setCoordTransforms(transforms);

    Set<String> axisNames = new HashSet<>();
    List<GridCoordAxis> axes = new ArrayList<>();
    for (Gridset gset : proxy.getGridsets()) {
      ucar.nc2.dt.GridCoordSystem gcs = gset.getGeoCoordSystem();
      for (ucar.nc2.dataset.CoordinateAxis axis : gcs.getCoordinateAxes())
        if (!axisNames.contains(axis.getFullName())) {
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
    ucar.nc2.dt.GridDatatype dtGrid;

    Grid(ucar.nc2.dt.GridDatatype dtGrid) {
      this.dtGrid = dtGrid;
      setName(dtGrid.getName());
      setAtts(dtGrid.getAttributes());
      setDataType(dtGrid.getDataType());
      setCoordSysName(dtGrid.getCoordinateSystem().getName());
      setUnits(dtGrid.getUnitsString());
      setDescription(dtGrid.getDescription());
    }

    @Override
    public Array readData(GridSubset subset) throws IOException {
      GridCoordSystem gcs = dtGrid.getCoordinateSystem();
      int ens = -1;
      int level = -1;
      int time = -1;
      int runtime = -1;

      for (String key : subset.getKeys()) {
        double val = subset.getDouble(key);
        switch (key) {
          case "E":
            CoordinateAxis1D ensAxis = gcs.getEnsembleAxis();
            if (ensAxis != null) ens = ensAxis.findCoordElement(val);
            break;

          case "T":
            CoordinateAxis1D taxis = gcs.getVerticalAxis();
            if (taxis != null) time = taxis.findCoordElement(val);
            break;

          case "R":
            CoordinateAxis1D raxis = gcs.getRunTimeAxis();
            if (raxis != null) runtime = raxis.findCoordElement(val);
            break;

          case "Z":
            CoordinateAxis1D zaxis = gcs.getVerticalAxis();
            if (zaxis != null) level = zaxis.findCoordElement(val);
            break;
        }
      }
      //int rt_index, int e_index, int t_index, int z_index, int y_index, int x_index
      return dtGrid.readDataSlice(runtime, ens, time, level, -1, -1);
    }
  }

  private class CoordSys extends GridCoordSys {
    ucar.nc2.dt.GridCoordSystem dtCoordSys;

    CoordSys(ucar.nc2.dt.GridCoordSystem dtCoordSys) {
      this.dtCoordSys = dtCoordSys;
      setName(dtCoordSys.getName());
      for (CoordinateTransform ct : dtCoordSys.getCoordinateTransforms())
        addTransformName(ct.getName());
      for (CoordinateAxis axis : dtCoordSys.getCoordinateAxes()) // LOOK should be just the grid axes
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

  private class Axis extends GridCoordAxis {
    ucar.nc2.dataset.CoordinateAxis1D dtCoordAxis;
    Spacing spacing;

    Axis(ucar.nc2.dataset.CoordinateAxis dtCoordAxis) {
      this.dtCoordAxis = (CoordinateAxis1D) dtCoordAxis;
      setName(dtCoordAxis.getFullName());
      setDataType(dtCoordAxis.getDataType());
      setAxisType(dtCoordAxis.getAxisType());
      setUnits(dtCoordAxis.getUnitsString());
      setDescription(dtCoordAxis.getDescription());
      setIndependent(dtCoordAxis.isCoordinateVariable());

      AttributeContainerHelper atts = new AttributeContainerHelper("dtCoordAxis");
      for (Attribute patt : dtCoordAxis.getAttributes())
        atts.addAttribute(patt);
      setAttributes(atts);

      setNvalues(dtCoordAxis.getSize());

      CoordinateAxis1D axis1D = (CoordinateAxis1D) dtCoordAxis;
      setStartValue(axis1D.getCoordValue(0));
      setEndValue(axis1D.getCoordValue((int) axis1D.getSize() - 1));
      if (axis1D.isRegular())
        spacing = Spacing.regular;
      else if (!axis1D.isInterval())
        spacing = Spacing.irregularPoint;
      else if (axis1D.isContiguous())
        spacing = Spacing.contiguousInterval;
      else
        spacing = Spacing.discontiguousInterval;
      setSpacing(spacing.ordinal());
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


}
