/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

package ucar.util.prefs.ui;

import ucar.util.prefs.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.*;
import javax.swing.table.*;

/**
 * BeanTableSorted adds sorting functionality to a BeanTable.
 * It throws a UIChangeEvent, property = "sort" just before a sort is going to happen.
 *
 * @see ucar.util.prefs.ui.BeanTable
 * @see ucar.util.prefs.PreferencesExt
 * @author John Caron
 */

public class BeanTableSorted extends BeanTable {
      // for HeaderRenderer
  //private Icon sortDownIcon = getIcon( "/resources/icons/SortDown.gif");
  //private Icon sortUpIcon = getIcon( "/resources/icons/SortUp.gif");

  //private boolean reverse = false;
 // private int sortCol = -1;

  private TableSorter sortedModel;

  public BeanTableSorted( Class bc, PreferencesExt pstore, boolean canAddDelete) {
    super( bc, pstore, canAddDelete);

    sortedModel = new TableSorter(model);
    jtable.setModel( sortedModel);
    sortedModel.setTableHeader(jtable.getTableHeader());

    restoreState(); // ??
  }

    // so it can be ovverriden in BeanTableSorted
  protected int modelIndex(int viewIndex) {
    return sortedModel.modelIndex(viewIndex);
  }

  protected int viewIndex(int rowIndex) {
    return sortedModel.viewIndex(rowIndex);
  }

}