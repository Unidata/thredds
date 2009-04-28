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
package ucar.nc2.ui.grid;

import ucar.nc2.dt.GridDatatype;
import thredds.viewer.ui.table.JTableSorted;
import thredds.viewer.ui.table.TableRowAbstract;
import thredds.viewer.ui.event.*;

import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Put the fields of a GridDatatype dataset in a JTable.
 * Uses ActionSourceListener for events.
 *
 * @see thredds.viewer.ui.event.ActionSourceListener
 *
 * @author caron
 */

public class GridTable {
  private JTableSorted table;
  private ArrayList<Row> list = null;
  private ActionSourceListener actionSource;

  private boolean eventOK = true;
  private boolean debug = false;

  public GridTable(String actionName) {
      // the main delegate
    table = new JTableSorted(colName, list);

         // event management
    actionSource = new ActionSourceListener(actionName) {
      public void actionPerformed( ActionValueEvent e) {
        if (list == null) return;
        String want = e.getValue().toString();
        int count = 0;
        for (Row row : list) {
          if (want.equals(row.gg.getName())) {
            eventOK = false;
            table.setSelected(count);
            eventOK = true;
            break;
          }
          count++;
        }
      }
    };

     // send event when selected row changes
    table.addListSelectionListener( new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (eventOK && !e.getValueIsAdjusting()) {
          // new variable is selected
          Row row = (Row) table.getSelected();
          if (row != null) {
            if (debug) System.out.println(" GridTable new gg = "+ row.gg.getName());
            actionSource.fireActionValueEvent(ActionSourceListener.SELECTED, row.gg.getName());
          }
        }
      }
    });
  }

  public void setDataset(java.util.List<GridDatatype> fields) {
    if (fields == null) return;

    list = new ArrayList<Row>(fields.size());
    for (GridDatatype gg : fields)
      list.add(new Row(gg));

    table.setList(list);
  }

  public JPanel getPanel() { return table; }

      /* better way to do event management */
  public ActionSourceListener getActionSourceListener() { return actionSource; }

  /// inner classes

  private static String[] colName = {"Name", "Dimensions", "Units", "Long Name"};
  private class Row extends TableRowAbstract {
    GridDatatype gg;
    String dims;

    Row( GridDatatype gg) {
      this.gg = gg;
      dims = "("+gg.getCoordinateSystem().getName()+")";
    }

    public Object getUserObject() { return gg; }
    public Object getValueAt( int col) {
      switch (col) {
        case 0: return gg.getName();
        case 1: return dims;
        case 2: return gg.getUnitsString();
        case 3: return gg.getDescription();
        default: return "error";
      }
    }
  }

}