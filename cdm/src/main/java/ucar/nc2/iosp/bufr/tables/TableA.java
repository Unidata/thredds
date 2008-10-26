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
package ucar.nc2.iosp.bufr.tables;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * BUFR Table A - Data categories
 *
 * @author caron
 * @since Sep 25, 2008
 */
public class TableA {
  private String name;
  private String location;
  private Map<Short, String> map = new HashMap<Short, String>(100);

  public String getName() { return name; }
  public String getLocation() { return location; }
  public void show( Formatter out) {
    Collection<Short> keys = map.keySet();
    List<Short> sortKeys = new ArrayList(keys);
    Collections.sort( sortKeys);

    for (Short key : sortKeys) {
      out.format(" %3d : %s %n", key, map.get(key));
    }
  }

  public String getDataCategory(short cat) {
    String catName = map.get(cat);
    return (catName == null) ? "Unknown Data Category ("+cat+")" : catName;
  }

  TableA(String name, String location, Map<Short, String> map) {
    this.name = name;
    this.location = location;
    this.map = map;
  }

}
