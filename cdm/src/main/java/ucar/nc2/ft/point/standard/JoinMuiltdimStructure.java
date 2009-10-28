/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.StructureDS;
import ucar.ma2.ArrayStructure;
import ucar.ma2.StructureData;

import java.io.IOException;

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

  public StructureData getJoinData(Cursor cursor) {
    int recnum = cursor.recnum[0] / dimLength;
    return parentData.getStructureData(recnum);
  }

  public VariableDS findVariable(String axisName) {
    return (VariableDS) parentStructure.findVariable(axisName);
  }

    @Override
  public String toString() {
    return "JoinMuiltdimStructure{" +
        "parentStructure=" + parentStructure +
        ", dimLength='" + dimLength +
        '}';
    }
}
