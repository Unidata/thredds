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
    this( bc, pstore, canAddDelete, null, null);
  }

  public BeanTableSorted( Class bc, PreferencesExt pstore, boolean canAddDelete, String header, String tooltip) {
    super( bc, pstore, canAddDelete, header, tooltip);

    sortedModel = new TableSorter(model);
    jtable.setModel( sortedModel);
    sortedModel.setTableHeader(jtable.getTableHeader());

    restoreState(); // ??
  }

  protected int modelIndex(int viewIndex) {
    return sortedModel.modelIndex(viewIndex);
  }

  protected int viewIndex(int rowIndex) {
    return sortedModel.viewIndex(rowIndex);
  }

}