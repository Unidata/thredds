/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2;

import javax.annotation.Nullable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib2.table.Grib2Tables;
import ucar.nc2.grib.grib2.table.WmoParamTable;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.wmo.Util;
import ucar.unidata.util.StringUtil2;

import java.util.Formatter;
import java.util.List;

/**
 * Static utilities for Grib-2
 *
 * @author caron
 * @since 3/29/11
 */
public class Grib2Utils {

  public static String clean(String s) {
    StringBuilder sb = new StringBuilder(s);
    StringUtil2.replace(sb, "/. ", "-p_");
    StringUtil2.removeAll(sb, "(),;");
    char c = sb.charAt(0);
    if (Character.isLetter(c)) {
      if (Character.isLowerCase(c))
        sb.setCharAt(0, Character.toUpperCase(c));
    } else {
      sb.insert(0, 'N');
    }

    return sb.toString().trim();
  }

  public static String cleanupHeader(byte[] raw) {
    String result = StringUtil2.cleanup(raw);
    int pos = result.indexOf("data");
    if (pos > 0) result = result.substring(pos);
    return result;
  }

  public static String getVariableName(Grib2Record gr) {
    GribTables.Parameter p = WmoParamTable.getParameter(gr.getDiscipline(), gr.getPDS().getParameterCategory(), gr.getPDS().getParameterNumber());
    String s = (p == null) ? null : p.getName();
    if (s == null)
      s = "U"+ gr.getDiscipline()+"-"+gr.getPDS().getParameterCategory()+"-" + gr.getPDS().getParameterNumber();
    return s;
  }

  @Nullable
  public static CalendarPeriod getCalendarPeriod(int timeUnit) {

    switch (timeUnit) { // code table 4.4
      case 0:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Minute);
      case 1:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Hour);
      case 2:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Day);
      case 3:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Month);
      case 4:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Year);
      case 5:
        return CalendarPeriod.of(10, CalendarPeriod.Field.Year);
      case 6:
        return CalendarPeriod.of(30, CalendarPeriod.Field.Year);
      case 7:
        return CalendarPeriod.of(100, CalendarPeriod.Field.Year);
      case 10:
        return CalendarPeriod.of(3, CalendarPeriod.Field.Hour);
      case 11:
        return CalendarPeriod.of(6, CalendarPeriod.Field.Hour);
      case 12:
        return CalendarPeriod.of(12, CalendarPeriod.Field.Hour);
      case 13:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Second);
      default:
        return null;
    }
  }

  /**
   * Check to see if this pds is a layer variable
   * @param pds record to check
   * @return true if a layer
   */
  public static boolean isLayer(Grib2Pds pds) {
    return pds.getLevelType2() != 255 && pds.getLevelType2() != 0;
  }

  public static boolean isLatLon(int gridTemplate, int center) {
    return ((gridTemplate < 4) || ((gridTemplate >= 40) && (gridTemplate < 44)));
  }

  //////////////////////////////////////////////////////////////////////////////////
  // pretty much lame stuff
  // possibly move to Customizer

  // check if grid template is "Curvilinear Orthogonal", (NCEP 204) methods below only used when thats true
  public static boolean isCurvilinearOrthogonal(int gridTemplate, int center) {
    return ((center == 7) && (gridTemplate == 204));
  }

  // isLatLon2D is true, check parameter to see if its a 2D lat/lon coordinate
  @Nullable
  public static LatLon2DCoord getLatLon2DcoordType(int discipline, int category, int parameter) {
    if ((discipline != 0) || (category != 2) || (parameter < 198 || parameter > 203)) return null;
    switch (parameter) {
      case 198:
        return LatLon2DCoord.U_Latitude;
      case 199:
         return LatLon2DCoord.U_Longitude;
      case 200:
        return LatLon2DCoord.V_Latitude;
      case 201:
        return LatLon2DCoord.V_Longitude;
      case 202:
       return LatLon2DCoord.P_Latitude;
      case 203:
       return LatLon2DCoord.P_Longitude;
    }
    return null;
  }

  public enum LatLonCoordType {U, V, P }
  public enum LatLon2DCoord {
    U_Latitude, U_Longitude, V_Latitude, V_Longitude, P_Latitude, P_Longitude;

    public AxisType getAxisType() {
      return this.name().contains("Latitude") ? AxisType.Lat : AxisType.Lon;
    }
  }

  /**
   * This looks for snippets in the variable name/desc as to whether it wants U, V, or P 2D coordinates
   * @param desc variable name/desc
   * @return  U, V, or P for normal variables, null for the coordinates themselves
   */
  public static LatLonCoordType getLatLon2DcoordType(String desc) {
    LatLonCoordType type;
    if (desc.contains("u-component")) type = LatLonCoordType.U;
    else if (desc.contains("v-component")) type = LatLonCoordType.V;
    else if (desc.contains("Latitude of") || desc.contains("Longitude of")) type = null;
    else type = LatLonCoordType.P;
    return type;
  }

  // Compare 2 tables, print report.
  public static void compareTables(String name1, String name2, List<? extends GribTables.Parameter> test, Grib2Tables reference, Formatter f) {

    int extra = 0;
    int udunits = 0;
    int conflict = 0;
    f.format("Table 1 : %s%n", name1);
    f.format("Table 2 : %s%n", name2);
    for (GribTables.Parameter p1 : test) {
      GribTables.Parameter  p2 = reference.getParameter(p1.getDiscipline(), p1.getCategory(), p1.getNumber());
      if (p2 == null) {
        if (p1.getCategory() < 192 && p1.getNumber() < 192) {
          extra++;
          f.format("  WMO missing %s%n", p1);
        }

      } else {
        String p1n = Util.cleanName(p1.getName());
        String p2n = Util.cleanName(p2.getName());

        if (!p1n.equalsIgnoreCase(p2n)) {
          f.format("  p1=%10s %40s %15s %15s %s%n", p1.getId(), p1.getName(), p1.getUnit(), p1.getAbbrev(), p1.getDescription());
          f.format("  p2=%10s %40s %15s %15s %s%n%n", p2.getId(), p2.getName(), p2.getUnit(), p2.getAbbrev(), p2.getDescription());
          conflict++;
        }

        if (!p1.getUnit().equalsIgnoreCase(p2.getUnit())) {
          String cu1 = Util.cleanUnit(p1.getUnit());
          String cu2 = Util.cleanUnit(p2.getUnit());

          // eliminate common non-udunits
          boolean isUnitless1 = Util.isUnitless(cu1);
          boolean isUnitless2 = Util.isUnitless(cu2);

          if (isUnitless1 != isUnitless2) {
            f.format("  ud=%10s %s != %s for %s (%s)%n%n", p1.getId(), cu1, cu2, p1.getId(), p1.getName());
            udunits++;

          } else if (!isUnitless1) {

            try {
              SimpleUnit su1 = SimpleUnit.factoryWithExceptions(cu1);
              if (!su1.isCompatible(cu2)) {
                f.format("  ud=%10s %s (%s) != %s for %s (%s)%n%n", p1.getId(), cu1, su1, cu2, p1.getId(), p1.getName());
                udunits++;
              }
            } catch (Exception e) {
              f.format("  udunits cant parse=%10s %15s %15s%n", p1.getId(), cu1, cu2);
            }
          }

        }
      }
    }
    f.format("Conflicts=%d extra=%d udunits=%d%n%n", conflict, extra, udunits);

    f.format("Parameters in %s not in %s%n", name1, name2);
    int local = 0;
    for (GribTables.Parameter p1 : test) {
      GribTables.Parameter  p2 = reference.getParameter(p1.getDiscipline(), p1.getCategory(), p1.getNumber());
      if (p2 == null) {
        local++;
        f.format("  %s%n", p1);
      }
    }
    f.format(" missing=%d%n%n", local);
  }

}
