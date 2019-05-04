package ucar.nc2.grib.grib2.tools;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.Formatter;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib2.table.EccodesParamTable;
import ucar.nc2.grib.grib2.table.Grib2ParamTableInterface;
import ucar.nc2.grib.grib2.table.WmoCodeFlagTables;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.wmo.Util;

/**
 * ECMWF param tables read from resources/grib2/ecmwf/tables/<latest>.
 * EcmwfParamTableCompare is used to compare with WMO.
 * No significant differences.
*/
public class EcmwfParamTableCompare {

  private static boolean verbose = false;

  // Compare 2 tables, print report.
  public static void compareTables(Grib2ParamTableInterface t1, Grib2ParamTableInterface t2, Formatter f) {
    int extra = 0;
    int udunits = 0;
    int conflict = 0;
    f.format("%s%n", t2.getName());
    for (GribTables.Parameter p1 : t1.getParameters()) {
      if (t1.getParameter(p1.getNumber()) == null) {
        f.format(" ERROR %s missing own parameter %d%n", t1.getShortName(), p1.getNumber());
      }
      GribTables.Parameter p2 = t2.getParameter(p1.getNumber());
      if (p2 == null) {
        extra++;
        if (verbose) {
          f.format("  %s missing %s%n", t2.getShortName(), p1);
        }
      } else {
        if (!Util.equivilantName(p1.getName(), p2.getName())) {
          f.format("  p1=%10s %s%n", p1.getId(), p1.getName());
          f.format("  p2=%10s %s%n", p2.getId(), p2.getName());
          conflict++;
        }

        if (!p1.getUnit().equalsIgnoreCase(p2.getUnit())) {
          String cu1 = Util.cleanUnit(p1.getUnit());
          String cu2 = Util.cleanUnit(p2.getUnit());

          // eliminate common non-udunits
          boolean isUnitless1 = Util.isUnitless(cu1);
          boolean isUnitless2 = Util.isUnitless(cu2);

          if (isUnitless1 != isUnitless2) {
            f.format("  unitless for %10s %s != %s%n", p1.getId(), cu1, cu2);
            udunits++;

          } else if (!isUnitless1) {

            try {
              SimpleUnit su1 = SimpleUnit.factoryWithExceptions(cu1);
              if (!su1.isCompatible(cu2)) {
                f.format("  incompatible for %10s %s (%s) != %s (org %s)%n", p1.getId(), cu1, su1,
                    cu2, p2.getUnit());
                udunits++;
              }
            } catch (Exception e) {
              f.format("  udunits cant parse=%10s %15s %15s%n", p1.getId(), cu1, cu2);
            }
          }

        }
      }
    }

    int missing = 0;
    for (GribTables.Parameter p2 : t2.getParameters()) {
      if (t2.getParameter(p2.getNumber()) == null) {
        f.format(" ERROR %s missing own parameter %d%n", t2.getShortName(), p2.getNumber());
      }
      GribTables.Parameter p1 = t1.getParameter(p2.getNumber());
      if (p1 == null) {
        missing++;
        f.format("  %s missing %s%n", t1.getShortName(), p2);
      }
    }
    if (conflict > 0 || udunits > 0 || missing > 0) {
      f.format(" ***Conflicts=%d extra=%d udunits=%d missing=%s%n", conflict, extra, udunits,
          missing);
    }
  }

  public static void readParamTable(int discipline, int category) {
    Grib2ParamTableInterface latest = WmoCodeFlagTables.getInstance().getParamTable(discipline, category);
    System.out.printf("================================================================%n");
    System.out.printf("%s%n", latest.getName());

    for (int version = 21; version >= 0; version--) {
      Grib2ParamTableInterface next = EccodesParamTable.factory(version, discipline, category);
      if (next == null) {
        System.out.printf("Missing version %d%n", version);
      } else {
        Formatter out = new Formatter();
        EcmwfParamTableCompare.compareTables(latest, next, out);
        System.out.printf("%s", out);
        latest = next;
      }
      if (!verbose) break;
    }
  }

  public static void main(String[] args) {
    final String PATH = "/usr/local/google/home/jlcaron/github/thredds/grib/src/main/resources/resources/grib2/ecmwf/tables/21";
    System.out.printf("EcmwfParamTableCompare on %s%n", CalendarDate.present());
    System.out.printf("  ECMWF = %s%n", PATH);
    System.out.printf("  WMO   = %s%n", WmoCodeFlagTables.standard.getResourceName());

    File root = new File(PATH);
    ImmutableList<String> children = ImmutableList.copyOf(root.list());
    children.stream().sorted().forEach(filename -> {
      if (filename.startsWith("4.2.")) {
        String[] split = filename.split("\\.");
        int discipline = Integer.parseInt(split[2]);
        int category = Integer.parseInt(split[3]);
        readParamTable(discipline, category);
      }
    });
  }
}
