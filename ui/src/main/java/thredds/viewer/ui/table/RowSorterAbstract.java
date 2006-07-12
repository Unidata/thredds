// $Id$
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

import ucar.util.prefs.PreferencesExt;

import java.io.*;
import java.util.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

/**
 * Superclass for implementations of RowSorter, used with JTreeTableSorted to create
 *  application-controlled sorting.
 *
 * @see TreeTableModelSorted
 * @see RowSorter
 *
 * @author John Caron
 * @version $Id$
 */

public abstract class RowSorterAbstract implements RowSorter {

  protected String[] columnNames;
  protected int ncolumns;

  protected TreeTableModelSorted model;
  protected JTreeTableSorted table;
  protected ArrayList rowList = new ArrayList();

    // sorting
  private int startSort = 0;
  protected int[] sortNext;
  private int[] sortBreak;

  private boolean debug = false;

  protected RowSorterAbstract( String [] colNames) {
    columnNames = colNames;
    ncolumns = columnNames.length;

      // sorting
    sortNext = new int[ ncolumns];
    for (int i=0; i< ncolumns-1; i++)
      sortNext[i] = i+1;
    sortNext[ncolumns-1] = -1;
    sortBreak = new int[ ncolumns];
    for (int i=0; i< ncolumns; i++)
      sortBreak[i] = -1;

      // the main delegate
    model = new TreeTableModelSorted(columnNames, rowList);
    model.setSorter( this);
    table = new JTreeTableSorted(model, true);
  }

  private PreferencesExt store;
  /**
    Restore the state from the last saved in the PreferencesExt.
    @param PersistentStore store ok if null or empty
  */
  public void restoreState(PreferencesExt store) {
    this.store = store;
    table.restoreState( store);
  }
  /**
   * Save state to the PreferencesExt.
   */
  public void saveState() {
    table.saveState( store);
  }

  public JTreeTableSorted getComponent() { return table; }
  public TreeTableModelSorted getModel() { return table.getModel(); }

  // public void saveState() { table.saveState(); }

  // must be an array of TableRow objects
  protected void setRows(ArrayList list) {
    rowList = list;
    table.setRows(list);
  }

  // implement RowSorter
  public java.util.ArrayList sort(int sortCol, boolean reverse, java.util.ArrayList docs) {
    setupSort( sortCol, reverse);
    if (startSort < 0) return docs;
    java.util.Collections.sort( docs, new TableRowAbstract.Sorter(startSort, false));
    return docs;
  }

  public boolean isBreak(TableRow last, TableRow current) {
    TableRow lastRow = (TableRow) last;
    TableRow currentRow = (TableRow) current;

    currentRow.setNextSort( sortBreak);
    int ret = currentRow.compare( lastRow, startSort);
    currentRow.setNextSort( sortNext);
    return (0 != ret);
  }

  private void setupSort(int sortCol, boolean reverse) {

      // col -> model
    int[] col2Model = table.getModelIndex();
    if (debug) {
      System.out.println("RowSorter sort ");
      System.out.print(" col->model = ");
      for (int i=0; i<col2Model.length; i++)
        System.out.print(" "+col2Model[i]);
      System.out.println();
    }

      // compute sortNext array
    for (int i=0; i< ncolumns; i++)
      sortNext[i] = -1;
    startSort = col2Model[0];
    int next = startSort;
    for (int i=1; i<col2Model.length; i++) {
      sortNext[next] = col2Model[i];
      next = col2Model[i];
    }
    if (debug) {
      System.out.print(" sortNext ("+startSort+") = ");
      for (int i=0; i<ncolumns; i++)
        System.out.print(" "+sortNext[i]);
      System.out.println();
    }

      // compute sortBreak
    for (int i=0; i< ncolumns; i++)
      sortBreak[i] = sortNext[i];
    sortBreak[sortCol] = -1;
    if (debug) {
      System.out.print(" sortBreak = ");
      for (int i=0; i<ncolumns; i++)
        System.out.print(" "+sortBreak[i]);
      System.out.println();
    }

      /** heres how you do it if you want the sortCol to be the primary sort
        // sort order
      int [] sortOrder = new int[ n];
      sortOrder[0] = sortModelCol;
      int sortNo = 1;
      for (int i=0; i<n; i++)
        if (col2Model[i] != sortModelCol)
          sortOrder[sortNo++] = col2Model[i];

        // compute next
      for (int i=0; i<n-1; i++)
        sortNext[sortOrder[i]] = sortOrder[i+1];
      sortNext[sortOrder[n-1]] = -1;

      if (debug) {
        System.out.print(" col->model = ");
        for (int i=0; i<n; i++)
          System.out.print(" "+col2Model[i]);
        System.out.println();

        System.out.print(sortModelCol+" sortOrder = ");
        for (int i=0; i<n; i++)
          System.out.print(" "+sortOrder[i]);
        System.out.println();

        System.out.print(sortModelCol+" sortNext = ");
        for (int i=0; i<n; i++)
          System.out.print(" "+sortNext[i]);
        System.out.println();
      } */
  }

}

/* Change History:
   $Log: RowSorterAbstract.java,v $
   Revision 1.2  2004/09/24 03:26:43  caron
   merge nj22

   Revision 1.1  2002/12/13 00:55:09  caron
   pass 2

   Revision 1.1.1.1  2001/03/24 03:11:17  caron
   initial checkin

*/