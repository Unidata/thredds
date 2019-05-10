/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.table;

public interface TableRow {

    // return the value of this row at the specified column
    Object getValueAt(int col);

    // for use in Comparator: return -1 (less than) 0 (equal) or 1 (greater than other)
    int compare(TableRow other, int col);

  void setNextSort(int[] nextSort);
  Object getUserObject();

}