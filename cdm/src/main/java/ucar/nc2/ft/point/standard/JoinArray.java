/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard;

import java.io.IOException;

import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataFactory;
import ucar.nc2.Variable;
import ucar.nc2.dataset.VariableDS;

/**
 * Join data from an element of an Array, whose index is passed in as cursor.recnum[0].
 * @author caron
 * @since Feb 25, 2009
 */
public class JoinArray implements Join {
  public enum Type { 
    modulo,  // use cursor.recnum[0] % param
    divide, // use cursor.recnum[0] / param
    level ,  // use cursor.recnum[param]
    raw ,  // use cursor.recnum[0]
    scalar // use 0
  }

  VariableDS v;
  Array data;
  Type type;
  int param;

  /**
   * Constructor.
   * @param v get data from this Variable
   * @param type how to use the parameter
   * @param param optional parameter
   */
  public JoinArray(VariableDS v, Type type, int param) {
    this.v = v;
    this.type = type;
    this.param = param;

    try {
      data = v.read();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Variable getExtraVariable() {
    return v;
  }


  @Override
  public StructureData getJoinData(Cursor cursor) {
    int recnum = -1;
    switch (type) {
      case modulo:
        recnum = cursor.recnum[0] % param;
        break;
      case divide:
        recnum = cursor.recnum[0] / param;
        break;
      case level:
        recnum = cursor.recnum[param];
        break;
      case raw:
        recnum = cursor.recnum[0];
        break;
      case scalar:
        recnum = 0;
        break;
    }
    return StructureDataFactory.make(v.getShortName(), data.getObject(recnum));
  }

  @Override
  public VariableDS findVariable(String varName) {
    return (varName.equals(v.getFullName())) ? v : null;
  }

  @Override
  public String toString() {
    return "JoinArray{" +
        "v=" + v.getFullName() +
        ", type=" + type +
        ", param=" + param +
        '}';
  }
}
