/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dt.point;

import ucar.ma2.DataType;
import ucar.ma2.Section;
import ucar.nc2.VariableSimpleIF;

/**
 * Point Data Variables for CFPointWriter
 *
 * @deprecated use ucar.nc2.ft.point
 * @author caron
 * @since Oct 22, 2008
 */
public class PointObVar {
  private String name;
  private String units;
  private String desc;
  private DataType dtype;
  private int len; // allow 1D

  public PointObVar() {}

  public PointObVar(String name, String units, String desc, DataType dtype, int len) {
    this.name = name;
    this.units = units;
    this.desc = desc;
    this.dtype = dtype;
    this.len = len;
  }

  public PointObVar(VariableSimpleIF v) {
    setName(v.getShortName());
    setUnits(v.getUnitsString());
    setDesc(v.getDescription());
    setDataType(v.getDataType());
    //if (v.getRank() > 0) setLen( v.getShape()[0]);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUnits() {
    return units;
  }

  public void setUnits(String units) {
    this.units = units;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public DataType getDataType() {
    return dtype;
  }

  public void setDataType(DataType dtype) {
    this.dtype = dtype;
  }

  public int getLen() {
    return len;
  }

  public void setLen(int len) {
    this.len = len;
  }
  
}