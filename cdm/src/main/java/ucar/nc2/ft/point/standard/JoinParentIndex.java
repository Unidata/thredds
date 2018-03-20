/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard;

import java.io.IOException;

import ucar.ma2.ArrayStructure;
import ucar.ma2.StructureData;
import ucar.nc2.Variable;
import ucar.nc2.dataset.StructureDS;
import ucar.nc2.dataset.VariableDS;

/**
 * Join data from a row of a Structure, whose index is passed in as the value of a member variable of the
 * leaf StructureData (cursor.table[0]).
 *
 * @author caron
 * @since Jan 22, 2009
 */
public class JoinParentIndex implements Join {
  StructureDS parentStructure;
  ArrayStructure parentData;
  String parentIndex;

  /**
   * Constructor.
   * @param parentStructure  get data from this Structure
   * @param parentIndex name of member variable in leaf StructureData
   */
  public JoinParentIndex(StructureDS parentStructure, String parentIndex) {
    this.parentStructure = parentStructure;
    this.parentIndex = parentIndex;

    try {
      parentData = (ArrayStructure) parentStructure.read(); // cache entire ArrayStructure  LOOK
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public StructureData getJoinData(Cursor cursor) {
    StructureData sdata = cursor.tableData[0]; // LOOK ??
    int index = sdata.getScalarInt(parentIndex);
    return parentData.getStructureData(index);
  }

  @Override
  public VariableDS findVariable(String axisName) {
    return (VariableDS) parentStructure.findVariable(axisName);
  }

  @Override
  public Variable getExtraVariable() {
    return null;
  }

  @Override
  public String toString() {
    return "JoinParentIndex{" +
        "parentStructure=" + parentStructure +
        ", parentIndex='" + parentIndex +
        '}';
  }
}
