/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;

/**
 * Abstract superclass for TableConfigurer implementations
 * @author caron
 * @since Jan 21, 2009
 */
public abstract class TableConfigurerImpl implements TableConfigurer {

  @Override
  public String getConvName() {
    return convName;
  }

  @Override
  public void setConvName(String convName) {
    this.convName = convName;
  }

  @Override
  public String getConvUsed() {
    return convUsed;
  }

  @Override
  public void setConvUsed(String convUsed) {
    this.convUsed = convUsed;
  }

  private String convName, convUsed;

  /* protected Variable findVariableWithStandardNameAndNotDimension(NetcdfDataset ds, String standard_name, Dimension outer, Formatter errlog) {
    for (Variable v : ds.getVariables()) {
      String stdName = ds.findAttValueIgnoreCase(v, CF.STANDARD_NAME, null);
      if ((stdName != null) && stdName.equals(standard_name) && v.getRank() > 0 && !v.getDimension(0).equals(outer))
        return v;
    }
    return null;
  }

  protected String matchAxisTypeAndDimension(NetcdfDataset ds, AxisType type, final Dimension outer) {
    Variable var = CoordSysEvaluator.findCoordByType(ds, type, new CoordSysEvaluator.Predicate() {
      public boolean match(CoordinateAxis axis) {
        if ((outer == null) && (axis.getRank() == 0))
          return true;
        if ((outer != null) && (axis.getRank() == 1) && (outer.equals(axis.getDimension(0))))
          return true;
        return false;
      }
    });
    if (var == null) return null;
    return var.getShortName();
  } */

  protected String matchAxisTypeAndDimension(NetcdfDataset ds, AxisType type, final Dimension outer, final Dimension inner) {
    Variable var = CoordSysEvaluator.findCoordByType(ds, type, new CoordSysEvaluator.Predicate() {
      @Override
      public boolean match(CoordinateAxis axis) {
        return ((axis.getRank() == 2) && outer.equals(axis.getDimension(0)) && inner.equals(axis.getDimension(1)));
      }
    });
    if (var == null) return null;
    return var.getShortName();
  }

  protected String matchAxisTypeAndDimension(NetcdfDataset ds, AxisType type, final Dimension outer, final Dimension middle, final Dimension inner) {
    Variable var = CoordSysEvaluator.findCoordByType(ds, type, new CoordSysEvaluator.Predicate() {
      @Override
      public boolean match(CoordinateAxis axis) {
        return ((axis.getRank() == 3) && outer.equals(axis.getDimension(0)) && middle.equals(axis.getDimension(1)) && inner.equals(axis.getDimension(2)));
      }
    });
    if (var == null) return null;
    return var.getShortName();
  }

  protected CoordinateAxis findZAxisNotStationAlt(NetcdfDataset ds) {
    CoordinateAxis z = CoordSysEvaluator.findCoordByType(ds, AxisType.Height, new NotStationAlt());
    if (z != null) return z;

    z = CoordSysEvaluator.findCoordByType(ds, AxisType.Pressure, new NotStationAlt());
    if (z != null) return z;

    z = CoordSysEvaluator.findCoordByType(ds, AxisType.GeoZ, new NotStationAlt());
    return z;
  }

  // search for an axis which is  not the station altitude
  private static class NotStationAlt implements CoordSysEvaluator.Predicate {

    @Override
    public boolean match(CoordinateAxis axis) {
      Attribute stdName = axis.findAttribute(CF.STANDARD_NAME);
       if (stdName == null) return true;
       String val = stdName.getStringValue();
       return !CF.SURFACE_ALTITUDE.equals(val) && !CF.STATION_ALTITUDE.equals(val);
    }
  }

}
