// $Id: GridTable.java,v 1.1 2004/09/30 00:33:43 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
package ucar.nc2.ui.grid;

import ucar.nc2.dataset.grid.GeoGrid;
import thredds.viewer.ui.table.JTableSorted;
import thredds.viewer.ui.table.TableRowAbstract;
import thredds.viewer.ui.event.*;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Put the fields of a GeoGrid dataset in a JTable.
 * Uses ActionSourceListener for events.
 *
 * @see thredds.viewer.ui.event.ActionSourceListener
 * @author John Caron
 * @version $Id: GridTable.java,v 1.1 2004/09/30 00:33:43 caron Exp $
 */

public class GridTable {
  private JTableSorted table;
  private ArrayList list = null;
  private ActionSourceListener actionSource;

  private boolean eventOK = true;
  private boolean debug = false;

  public GridTable(String actionName) {
      // the main delegate
    table = new JTableSorted(colName, list);

         // event management
    actionSource = new ActionSourceListener(actionName) {
      public void actionPerformed( ActionValueEvent e) {
        String want = e.getValue().toString();
        int count = 0;
        Iterator iter = list.iterator();
        while(iter.hasNext()) {
          Row row = (Row) iter.next();
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

  public void setDataset(java.util.List fields) {
    list = new ArrayList(40);
    if (fields == null) return;

    java.util.Iterator iter = fields.iterator();
    while (iter.hasNext()) {
      GeoGrid gg = (GeoGrid) iter.next();
      list.add( new Row(gg));
    }
    table.setList(list);
  }

  public JPanel getPanel() { return table; }

      /** better way to do event management */
  public ActionSourceListener getActionSourceListener() { return actionSource; }

  /// inner classes

  private static String[] colName = {"Name", "Dimensions", "Units", "Long Name"};
  private class Row extends TableRowAbstract {
    GeoGrid gg;
    String dims;

    Row( GeoGrid gg) {
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
      }
      return "error";
    }
  }

}

/* Change History:
   $Log: GridTable.java,v $
   Revision 1.1  2004/09/30 00:33:43  caron
   *** empty log message ***

   Revision 1.3  2004/09/24 03:26:42  caron
   merge nj22

   Revision 1.2  2003/03/17 21:12:42  john
   new viewer

   Revision 1.1  2002/12/13 00:55:08  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:51  caron
   import sources

   Revision 1.11  2001/05/10 18:54:03  caron
   add windrenderer

   Revision 1.10  2001/04/30 23:40:23  caron
   fix event bug

   Revision 1.9  2000/08/18 04:16:26  russ
   Licensed under GNU LGPL.

   Revision 1.8  2000/05/16 23:15:04  caron
   use ActionSource event handling

   Revision 1.7  2000/05/09 20:43:09  caron
   change deprecated Format method

   Revision 1.6  2000/02/10 17:50:38  caron
   cleanup

*/