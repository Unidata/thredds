/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: RowSorter.java 50 2006-07-12 16:30:06Z caron $


package ucar.ui.table;

/**
 * An interface used with JTreeTableSorted to create application-controlled sorting.
 *
 * @see TreeTableModelSorted
 * @see RowSorterAbstract
 *
 * @author John Caron
 */


public interface RowSorter {

  /** May make a copy of the Array, or return the original, but must not copy the
   *  rows themselves.
   */
  java.util.ArrayList sort(int sortCol, boolean reverse, java.util.ArrayList docs);

  boolean isBreak(TableRow last, TableRow current);

}