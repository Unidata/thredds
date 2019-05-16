/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.table;

/**
 * An interface used with JTreeTableSorted to create application-controlled sorting on
 *  "threads". Threads are sequences of related messages, visallay indicated by indenting
 *  all the messages in the thread except the "top" one.
 *
 * @see TreeTableModelSorted
 * @see JTableSorted
 *
 * @author John Caron
 */

public interface ThreadSorter extends RowSorter {

  int getIndentCol();
  boolean isTopThread(TableRow row);

}