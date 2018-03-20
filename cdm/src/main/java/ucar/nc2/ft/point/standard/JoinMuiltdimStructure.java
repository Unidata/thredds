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
 * Join data from a row of a Structure, whose index is passed in as recnum[0] / dimLen
 *
 * @author caron
 * @since May 29, 2009
 */
public class JoinMuiltdimStructure implements Join {
  StructureDS parentStructure;
  ArrayStructure parentData;
  int dimLength;

  /**
   * Constructor.
   * @param parentStructure  get data from this Structure
   * @param dimLength structure index is recnum % dimlength
   */
  public JoinMuiltdimStructure(StructureDS parentStructure, int dimLength) {
    this.parentStructure = parentStructure;
    this.dimLength = dimLength;

    try {
      parentData = (ArrayStructure) parentStructure.read(); // cache entire ArrayStructure  LOOK
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public StructureData getJoinData(Cursor cursor) {
    int recnum = cursor.recnum[0] / dimLength;
    return parentData.getStructureData(recnum);
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
    return "JoinMuiltdimStructure{" +
        "parentStructure=" + parentStructure +
        ", dimLength='" + dimLength +
        '}';
    }
}
