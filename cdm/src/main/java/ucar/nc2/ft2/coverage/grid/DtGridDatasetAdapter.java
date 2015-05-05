/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import ucar.nc2.Attribute;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.TransformType;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDataset.Gridset;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.util.Parameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapt ucar.nc2.dt.GridDataset to ucar.nc2.ft.cover.grid.GridCoverageDatasetIF
 *
 * @author caron
 * @since 5/1/2015
 */
public class DtGridDatasetAdapter implements GridCoverageDatasetIF {
  private GridDataset proxy;

  public DtGridDatasetAdapter(GridDataset proxy) {
    this.proxy = proxy;
  }

  @Override
  public String getName() {
    return proxy.getLocation();
  }

  public List<GridCoverage> getGrids() {
    List<GridCoverage> result = new ArrayList<>();
    for (GridDatatype dtGrid : proxy.getGrids())
      result.add(new Grid(dtGrid));
    return result;
  }

  @Override
  public List<Attribute> getGlobalAttributes() {
    return proxy.getGlobalAttributes();
  }

  @Override
  public List<ucar.nc2.ft2.coverage.grid.GridCoordSys> getCoordSys() {
    List<ucar.nc2.ft2.coverage.grid.GridCoordSys> result = new ArrayList<>();
    for (Gridset gset : proxy.getGridsets()) {
      result.add(new CoordSys(gset.getGeoCoordSystem()));
    }
    return result;
  }

  @Override
  public List<GridCoordTransform> getCoordTransforms() {
    List<ucar.nc2.ft2.coverage.grid.GridCoordTransform> result = new ArrayList<>();
    for (Gridset gset : proxy.getGridsets()) {
      ucar.nc2.dt.GridCoordSystem gcs = gset.getGeoCoordSystem();
      for (ucar.nc2.dataset.CoordinateTransform ct : gcs.getCoordinateTransforms()) {
        result.add(new Transform(ct)); // LOOK duplicates
      }
    }
    return result;
  }

  @Override
  public List<GridCoordAxis> getCoordAxes() {
    List<ucar.nc2.ft2.coverage.grid.GridCoordAxis> result = new ArrayList<>();
    for (Gridset gset : proxy.getGridsets()) {
      ucar.nc2.dt.GridCoordSystem gcs = gset.getGeoCoordSystem();
      for (ucar.nc2.dataset.CoordinateAxis axis : gcs.getCoordinateAxes()) {
        result.add(new Axis(axis)); // LOOK duplicates
      }
    }
    return result;
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
    ucar.nc2.dataset.CoordinateAxis dtCoordAxis;

    Axis(ucar.nc2.dataset.CoordinateAxis dtCoordAxis) {
      this.dtCoordAxis = dtCoordAxis;
      setName(dtCoordAxis.getFullName());
      setDataType(dtCoordAxis.getDataType());
      setAxisType(dtCoordAxis.getAxisType());
      setUnits(dtCoordAxis.getUnitsString());
      setDescription(dtCoordAxis.getDescription());

      setNvalues(dtCoordAxis.getSize());
      if (dtCoordAxis instanceof CoordinateAxis1D) { // LOOK for grid, should always be true
        CoordinateAxis1D axis1D = (CoordinateAxis1D) dtCoordAxis;
        setMin(axis1D.getMinValue());
        setMax(axis1D.getMaxValue());
        setIsRegular(axis1D.isRegular());
      }
    }
  }


}
