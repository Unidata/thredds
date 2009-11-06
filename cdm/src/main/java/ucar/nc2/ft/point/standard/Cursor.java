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

import ucar.ma2.StructureData;

/**
 * Keeps track of the iteration through nested tables
 * @author caron
 * @since Jan 24, 2009
 */
public class Cursor {
  StructureData[] tableData;
  int[] recnum;
  int currentIndex;

  Cursor(int nlevels) {
    tableData = new StructureData[nlevels];
    recnum = new int[nlevels];
  }

  private int getParentIndex() { // skip null structureData, to allow dummy tables to be inserted, eg FslWindProfiler
    int indx = currentIndex;
    while ((tableData[indx] == null) && (indx < tableData.length-1)) indx++;
    return indx;
  }

  StructureData getParentStructure() {
    return tableData[getParentIndex()];
  }

  int getParentRecnum() {
    return recnum[getParentIndex()];
  }

  Cursor copy() {
    Cursor clone = new Cursor(tableData.length);
    //clone.what = what; // not a copy !!
    clone.currentIndex = currentIndex;
    System.arraycopy(this.tableData, 0, clone.tableData, 0, tableData.length);
    System.arraycopy(this.recnum, 0, clone.recnum, 0, tableData.length);
    return clone;
  }
}
