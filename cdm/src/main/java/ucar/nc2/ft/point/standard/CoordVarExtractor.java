/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard;

import ucar.ma2.StructureData;

/**
 * Abstract superclass for extracting coordinate values from nested tables.
 *
 * @author caron
 * @since Jan 26, 2009
 */

public abstract class CoordVarExtractor {
  protected String axisName, memberName;
  protected int nestingLevel;

  protected CoordVarExtractor(String axisName, int nestingLevel) {
    this.axisName = axisName;
    int pos = axisName.indexOf(".");
    memberName = (pos > 0) ? axisName.substring(pos+1) : axisName;
    this.nestingLevel = nestingLevel;
  }

  public abstract double getCoordValue(StructureData sdata);

  public abstract long getCoordValueLong(StructureData sdata);

  public abstract String getCoordValueString(StructureData sdata);

  public abstract String getUnitsString();

  public abstract boolean isString();

  public abstract boolean isInt();

  public double getCoordValue(StructureData[] tableData) {
    return getCoordValue(tableData[nestingLevel]);
  }

  public String getCoordValueString(StructureData[] tableData) {
    return getCoordValueString(tableData[nestingLevel]);
  }

  public String getCoordValueAsString(StructureData sdata) {
    if (isString()) return getCoordValueString(sdata);
    if (isInt()) {
      long val = getCoordValueLong( sdata);
      return Long.toString(val);
    }

    double dval = getCoordValue( sdata);
    return Double.toString(dval);
  }

  protected abstract boolean isMissing(StructureData tableData);

  public boolean isMissing(StructureData[] tableData) {
    return isMissing(tableData[nestingLevel]);
  }

  public String toString() {
    return axisName + " nestingLevel= " + nestingLevel;
  }
}
