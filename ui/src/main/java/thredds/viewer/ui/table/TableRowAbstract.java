// $Id: TableRowAbstract.java 50 2006-07-12 16:30:06Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package thredds.viewer.ui.table;

public abstract class TableRowAbstract implements TableRow {
  protected int tieBreakerCol = -1;      // set this to column used to break ties
  protected int[] tryNext;               // set this to list of columns used to break ties

  public abstract Object getValueAt( int col);
  public abstract Object getUserObject();

  public void setNextSort( int[] nextSort) { tryNext = nextSort; }

  private int compareTie(TableRow other, int col) {
    if (tieBreakerCol >= 0) {
      if (col == tieBreakerCol)
        return 0;
      else
        return compare( other, tieBreakerCol);
    }

    if ((tryNext != null) && (col < tryNext.length) ) {
      int next = tryNext[col];
      // System.out.println(" tryNext "+next+" "+col);
      if (next < 0)
        return 0;
      else
        return compare( other, next);
    }

    return 0;
  }

  protected void setTryNext(int[] t) { tryNext = t; }

    // default sort : use the strings returned by getStringValueAt
    // for other behavior, override this; use compareXXX routines.
  public int compare( TableRow other, int col) {
    String s1 = getValueAt(col).toString();
    String s2 = other.getValueAt(col).toString();
    int ret = s1.compareToIgnoreCase( s2);

    // break ties
    if (ret == 0)
      return compareTie( other, col);
    return ret;
  }

    // for use by the subclass
  protected int compareBoolean(TableRow other,  int col, boolean b1, boolean b2) {
    // break ties
    if (b1 == b2)
      return compareTie( other, col);
    return b1 ? 1 : -1;
  }

  protected int compareInt(TableRow other,  int col, int i1, int i2) {
    int ret = i1 - i2;
    // break ties
    if (ret == 0)
      return compareTie( other, col);
    return ret;
  }

  protected int compareLong(TableRow other,  int col, long i1, long i2) {
    int ret = (int) (i1 - i2);
    // break ties
    if (ret == 0)
      return compareTie( other, col);
    return ret;
  }

  protected int compareDouble(TableRow other,  int col, double d1, double d2) {
    int ret;
    if (d1 < d2)
      ret = -1;
    else if (d1 == d2)
      ret = 0;
    else
      ret = 1;

    // break ties
    if (ret == 0)
      return compareTie( other, col);
    return ret;
  }

  protected int compareDate(TableRow other,  int col, java.util.Date d1, java.util.Date d2) {
    int ret;
    if (d2 == null)
      ret = 1;
    else if (d1 == null)
      ret = -1;
    else
      ret = d1.compareTo(d2);

    // break ties
    if (ret == 0)
      return compareTie( other, col);
    return ret;
  }

  protected int compareString(TableRow other,  int col, String s1, String s2, boolean ignoreCase) {
    int ret;
    if (s2 == null)
      ret = 1;
    else if (s1 == null)
      ret = -1;
    else if (ignoreCase)
      ret = s1.compareToIgnoreCase( s2);
    else
      ret = s1.compareTo( s2);

    // break ties
    if (ret == 0)
      return compareTie( other, col);
    return ret;
  }

  public String toString() { return ""; }

  static public class Sorter implements java.util.Comparator {
    private int col;
    private boolean reverse;

    public Sorter( int col, boolean reverse) {
      this.col = col;
      this.reverse = reverse;
    }

    public int compare(Object o1, Object o2) {
      TableRow row1 = (TableRow) o1;
      TableRow row2 = (TableRow) o2;
      return reverse ? row2.compare(row1, col) : row1.compare(row2, col);
    }

    public boolean equals(Object obj) { return this == obj; }
  }

}