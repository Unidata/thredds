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

package ucar.nc2;

import net.jcip.annotations.Immutable;

import java.util.*;

/**
 * Enumeration Typedef map integers to Strings.
 * For ENUM1, ENUM2, ENUM4 enumeration types.
 * Immutable.
 *
 * @author caron
 */
@Immutable
public class EnumTypedef {
  private final String name;
  private final Map<Integer, String> map;
  private ArrayList<String> enumStrings;

  public EnumTypedef(String name, Map<Integer, String> map) {
    this.name = name;
    this.map = map;
  }

  public String getName() { return name; }
  public String getShortName() { return name; }
  public List<String> getEnumStrings() {
    if (enumStrings != null) {
      enumStrings = new ArrayList<String>(map.values());
      Collections.sort(enumStrings);
    }
    return enumStrings;
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
    List<Object> keyset = Arrays.asList(map.keySet().toArray());
    //Collections.sort(keyset);
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
