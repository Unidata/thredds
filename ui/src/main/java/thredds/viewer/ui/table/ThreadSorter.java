// $Id: ThreadSorter.java,v 1.2 2004/09/24 03:26:43 caron Exp $
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

/**
 * An interface used with JTreeTableSorted to create application-controlled sorting on
 *  "threads". Threads are sequences of related messages, visallay indicated by indenting
 *  all the messages in the thread except the "top" one.
 *
 * @see TreeTableModelSorted
 * @see JTableSorted
 *
 * @author John Caron
 * @version $Id: ThreadSorter.java,v 1.2 2004/09/24 03:26:43 caron Exp $
 */

public interface ThreadSorter extends RowSorter {

  public int getIndentCol( );
  public boolean isTopThread( TableRow row);

}