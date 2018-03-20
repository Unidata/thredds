/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.table;

public interface TableRow {

    // return the value of this row at the specified column
  public Object getValueAt( int col);

    // for use in Comparator: return -1 (less than) 0 (equal) or 1 (greater than other)
  public int compare( TableRow other, int col);

  public void setNextSort( int[] nextSort);
  public Object getUserObject();

}