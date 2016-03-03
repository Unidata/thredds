/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.wmo;

import ucar.unidata.util.StringUtil2;

/**
 * Uitlities common to WMO table parsing
 *
 * @author caron
 * @since 3/1/13
 */
public class Util {

  /**
   * Clean up strings to be used for unit string
   * @param unit original
   * @return cleaned up
   */
  static public String cleanUnit(String unit) {
    if (unit == null) return null;
    // These specific words become dimensionless
    if (unit.equalsIgnoreCase("Proportion") || unit.equalsIgnoreCase("Numeric"))
      unit = "";
    // So does '-'
    else if (unit.equalsIgnoreCase("-")) {
      unit = "";
    // Make sure degree(s) true gets concatenated with '_'
    } else if (unit.startsWith("degree") && unit.endsWith("true")) {
      unit = unit.replace(' ', '_');
    // And only do the rest of the conversion if it's not a "* table *" entry
    } else if (!unit.contains(" table ")) {
      if (unit.startsWith("/")) unit = "1" + unit;
      unit = unit.trim();
      unit = StringUtil2.remove(unit, "**");
      StringBuilder sb = new StringBuilder(unit);
      StringUtil2.remove(sb, "^[]");
      StringUtil2.replace(sb, ' ', ".");
      StringUtil2.replace(sb, '*', ".");
      unit = sb.toString();
    }
    return unit;
  }

  /**
   * Clean up strings to be used in Netcdf Object names
   * @param name original name
   * @return cleaned up name
   */
  static public String cleanName(String name) {
    if (name == null) return null;
    int pos = name.indexOf("(see");
    if (pos < 0) pos = name.indexOf("(See");
    if (pos > 0) name = name.substring(0,pos);

    name = StringUtil2.replace(name, '/', "-");
    StringBuilder sb = new StringBuilder(name);
    StringUtil2.replace(sb, '+', "plus");
    StringUtil2.remove(sb, ".;,=[]()/*\"");
    return StringUtil2.collapseWhitespace(sb.toString().trim());
  }


  static public String cleanupDescription(String desc) {
    if (desc == null) return null;
    int pos = desc.indexOf("(see");
    if (pos > 0) desc = desc.substring(0,pos);

    StringBuilder sb = new StringBuilder(desc.trim());
    StringUtil2.remove(sb, ".;,=[]()/*");
    return sb.toString().trim();
  }

}
