// $Id:HashMapLRU.java 63 2006-07-12 21:50:51Z edavis $
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
package ucar.nc2.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A HashMap that removes the oldest member when it exceeds the maximum number of entries.
 *
 * @see java.util.LinkedHashMap
 *
 * @author caron
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
 */
public class HashMapLRU extends LinkedHashMap {
  private int max_entries = 100;

  /**
   * Constructor.
   * @param initialCapacity start with this size
   * @param max_entries dont exceed this number of entries.
   */
  public HashMapLRU(int initialCapacity, int max_entries) {
    super(initialCapacity, (float) .50, true);
    this.max_entries = max_entries;
  }

   protected boolean removeEldestEntry(Map.Entry eldest) {
      return size() > max_entries;
   }

}