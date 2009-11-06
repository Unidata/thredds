/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 * 
 * Portions of this software were developed by the Unidata Program at the 
 * University Corporation for Atmospheric Research.
 * 
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 * 
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft.point.standard;

import ucar.nc2.dataset.VariableDS;
import ucar.ma2.StructureData;
import ucar.ma2.Array;
import ucar.ma2.StructureDataFactory;

import java.io.IOException;

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

  public VariableDS findVariable(String varName) {
    return (varName.equals(v.getName())) ? v : null;
  }

  @Override
  public String toString() {
    return "JoinArray{" +
        "v=" + v.getName() +
        ", type=" + type +
        ", param=" + param +
        '}';
  }
}
