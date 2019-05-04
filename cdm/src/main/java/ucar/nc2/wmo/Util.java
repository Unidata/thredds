/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
  public static String cleanUnit(String unit) {
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
      StringUtil2.removeAll(sb, "^[]");
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
  public static String cleanName(String name) {
    if (name == null) return null;
    int pos = name.indexOf("(see");
    if (pos < 0) pos = name.indexOf("(See");
    if (pos > 0) name = name.substring(0,pos);

    name = StringUtil2.replace(name, '/', "-");
    StringBuilder sb = new StringBuilder(name);
    StringUtil2.replace(sb, '+', "plus");
    StringUtil2.removeAll(sb, ".;,=[]()/*\"");
    return StringUtil2.collapseWhitespace(sb.toString().trim());
  }

  public static String cleanupDescription(String desc) {
    if (desc == null) return null;
    int pos = desc.indexOf("(see");
    if (pos > 0) desc = desc.substring(0,pos);

    StringBuilder sb = new StringBuilder(desc.trim());
    StringUtil2.removeAll(sb, ".;,=[]()/*");
    return sb.toString().trim();
  }

  /**
   * Compare two names from tables, trying to ignore superfulous characters.
   * @return true if these are equivilent
   */
  public static boolean equivilantName(String name1, String name2) {
    if (name1 == null || name2 == null) return (name1 == name2);
    String name1clean = cleanName(name1).toLowerCase();
    String name2clean = cleanName(name2).toLowerCase();
    if (name1.equals(name2)) return true;

    StringBuilder sb1 = new StringBuilder(name1clean);
    StringUtil2.removeAll(sb1, " -’'");
    StringBuilder sb2 = new StringBuilder(name2clean);
    StringUtil2.removeAll(sb2, " -’'");
    return sb1.toString().equals(sb2.toString());
  }

  /** The given unit is "unitless". */
  public static boolean isUnitless(String unit) {
    if (unit == null) return true;
    String munge = unit.toLowerCase().trim();
    munge = StringUtil2.remove(munge, '(');
    return munge.length()  == 0 ||
        munge.startsWith("numeric") || munge.startsWith("non-dim") || munge.startsWith("see") ||
        munge.startsWith("proportion") || munge.startsWith("code") || munge.startsWith("0=") ||
        munge.equals("1") ;
  }

  public static void main(String[] args) {
    System.out.printf("clean '/s' = %s%n", cleanUnit("/s"));
  }
}
