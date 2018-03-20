/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard;

import ucar.ma2.StructureData;

/**
 * Keeps track of the iteration through nested tables
 * 0 is always innermost nested table, 1 is parent of 0, 2 is parent of 1, etc
 *
 * @author caron
 * @since Jan 24, 2009
 */
public class Cursor {
  StructureData[] tableData; // current struct data
  int[] recnum;              // current recnum
  int currentIndex;          // the "parent index", current iteration is over its children.

  Cursor(int nlevels) {
    tableData = new StructureData[nlevels];
    recnum = new int[nlevels];
  }

  /* t.kunicki 11/25/10
  // Flattened data can now accurately access parent structure
  private int getParentIndex() {
    int maxIndex = tableData.length - 1;
    int parentIndex = currentIndex < maxIndex ? currentIndex + 1 : currentIndex;
    while (tableData[parentIndex] == null && parentIndex < maxIndex) parentIndex++;
    return parentIndex;
   }
  // end t.kunicki 11/25/10 */

  StructureData getParentStructure() {
    return tableData[getParentIndex()];
  }

  private int getParentIndex() { // skip null structureData, to allow dummy tables to be inserted, eg FslWindProfiler
    int indx = currentIndex;
    while ((tableData[indx] == null || tableData[indx].getMembers().size() == 0) && (indx < tableData.length-1)) indx++;
    return indx;
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
