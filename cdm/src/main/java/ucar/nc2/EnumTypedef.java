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

package ucar.nc2;

import java.util.*;

/**
 * Enumeration Typedef map ints to Strings.
 * For netcdf-4 enumeration types.
 *
 * @author caron
 */
public class EnumTypedef {
  private String name;
  private Map<Integer, String> map;

  public EnumTypedef(String name, Map<Integer, String> map) {
    this.name = name;
    this.map = map;
  }

  public String getName() { return name; }
  public String getShortName() { return name; }
  public List<String> getEnumStrings() {
    return new ArrayList<String>(map.values());
  }
  public String lookupEnumString(int e) {
    String result = map.get(e);
    return (result == null) ? "Unknown enum value= "+e : result;
  }

  /** String representation.
   * @param strict if true, write in strict adherence to CDL definition.
   * @return CDL representation.
   */
  public String writeCDL(boolean strict) {
    StringBuilder buff = new StringBuilder();
    String name = strict ? NetcdfFile.escapeName(getName()) : getName();    
    buff.append("  enum ").append(name).append(" { ");
    int count = 0;
    List keyset = Arrays.asList(map.keySet().toArray());
    Collections.sort(keyset);
    for (Object key : keyset) {
      String s = map.get(key);
      if (0 < count++) buff.append(", ");
      if (strict)
        buff.append( NetcdfFile.escapeName(s)).append(" = ").append(key);
      else
        buff.append("'").append(s).append("' = ").append(key);
    }
    buff.append("};");
    return buff.toString();
  }

}
