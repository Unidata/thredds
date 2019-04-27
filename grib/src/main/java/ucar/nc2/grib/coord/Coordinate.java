/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.coord;

import ucar.nc2.constants.AxisType;
import ucar.nc2.util.Counters;
import ucar.nc2.util.Indent;

import java.util.Formatter;
import java.util.List;

/**
 * Abstract coordinate
 *
 * @author caron
 * @since 11/24/13
 */
public interface Coordinate {
  /** Coordinate types */
  enum Type {
    runtime(0, AxisType.RunTime),
    time(1, AxisType.Time),
    timeIntv(1, AxisType.Time),
    vert(3, AxisType.Height),
    time2D(1, AxisType.TimeOffset),
    ens(2, AxisType.Ensemble);  // cant change order, protobuf uses the ordinal

    public final int order;
    public final AxisType axisType;

    Type(int order, AxisType axisType) {
      this.order = order;
      this.axisType = axisType;
    }
  }

  List<?> getValues(); // get sorted list of values
  Object getValue(int idx);  // get the ith value
  int getIndex(Object val);  // LOOK assumes the values are unique;
  int getSize();             // how many values ??

  int getCode();
  Type getType();
  String getName();
  String getUnit();
  int getNCoords();             // how many coords ??

  void showInfo(Formatter info, Indent indent);
  void showCoords(Formatter info);
  Counters calcDistributions();
  int estMemorySize();          // estimated memory size in bytes (debugging)
}
