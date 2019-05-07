package ucar.nc2.grib.grib2.tools;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.Formatter;
import ucar.nc2.grib.grib2.table.EccodesCodeTable;
import ucar.nc2.grib.grib2.table.Grib2CodeTableInterface;
import ucar.nc2.grib.grib2.table.WmoCodeFlagTables;
import ucar.nc2.grib.grib2.table.WmoCodeFlagTables.TableType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.wmo.Util;

public class EcmwfCodeTableCompare {

  private static boolean verbose = true;

  // Compare 2 tables, print report.
  public static void compareTables(Grib2CodeTableInterface t1, Grib2CodeTableInterface t2, Formatter f) {
    int extra = 0;
    int conflict = 0;
    f.format("%s%n", t2.getName());
    for (Grib2CodeTableInterface.Entry p1 : t1.getEntries()) {
      if (t1.getEntry(p1.getCode()) == null) {
        f.format(" ERROR %s missing own code %d%n", t1.getShortName(), p1.getCode());
      }
      Grib2CodeTableInterface.Entry p2 = t2.getEntry(p1.getCode());
      if (p2 == null) {
        extra++;
        if (verbose) {
          f.format("  %s missing %s%n", t2.getShortName(), p1);
        }
      } else {
        if (!Util.equivilantName(p1.getName(), p2.getName())) {
          f.format("  p1=%10s %s%n", p1.getCode(), p1.getName());
          f.format("  p2=%10s %s%n", p2.getCode(), p2.getName());
          conflict++;
        }
      }
    }

    int missing = 0;
    for (Grib2CodeTableInterface.Entry p2 : t2.getEntries()) {
      if (t2.getEntry(p2.getCode()) == null) {
        f.format(" ERROR %s missing own code %d%n", t2.getShortName(), p2.getCode());
      }
      Grib2CodeTableInterface.Entry p1 = t1.getEntry(p2.getCode());
      if (p1 == null) {
        missing++;
        f.format("  %s missing %s%n", t1.getShortName(), p2);
        t1.getEntry(p2.getCode());
      }
    }
    if (conflict > 0 || missing > 0) {
      f.format(" ***Conflicts=%d extra=%d missing=%s%n", conflict, extra, missing);
    }
  }

  public static void readCodeTable(int discipline, int category) {
    WmoCodeFlagTables tables = WmoCodeFlagTables.getInstance();
    String tableName = String.format("%d.%d", discipline, category);
    TableType type = tables.getTableType(tableName);
    if (type == null) {
      System.out.printf("================================================================%n");
      System.out.printf("   No WMO table that matches ECMWF table %s%n", tableName);
      return;
    }
    if (type == TableType.code) {
      Grib2CodeTableInterface latest = WmoCodeFlagTables.getInstance().getCodeTable(discipline, category);
      System.out.printf("================================================================%n");
      System.out.printf("%s%n", latest.getName());

      for (int version = 21; version >= 0; version--) {
        Grib2CodeTableInterface next = EccodesCodeTable.factory(version, discipline, category);
        if (next == null) {
          System.out.printf("Missing version %d%n", version);
        } else {
          Formatter out = new Formatter();
          compareTables(latest, next, out);
          System.out.printf("%s", out);
          latest = next;
        }
        if (!verbose) break;
      }
    }
  }

  public static void main(String[] args) {
    final String PATH = "/usr/local/google/home/jlcaron/github/thredds/grib/src/main/resources/resources/grib2/ecmwf/tables/21";
    System.out.printf("EcmwfCodeTableCompare on %s%n", CalendarDate.present());
    System.out.printf("  ECMWF = %s%n", PATH);
    System.out.printf("  WMO   = %s%n", WmoCodeFlagTables.standard.getResourceName());

    File root = new File(PATH);
    ImmutableList<String> children = ImmutableList.copyOf(root.list());
    children.stream().sorted().forEach(filename -> {
      if (!filename.startsWith("4.2.") && !filename.startsWith("4.1.")) {
        String[] split = filename.split("\\.");
        try {
          int discipline = Integer.parseInt(split[0]);
          int category = Integer.parseInt(split[1]);
          readCodeTable(discipline, category);
        } catch (Exception e) {
          System.out.printf("================================================================%n");
          System.out.printf("  Unparsed ECMWF table %s%n", filename);
        }
      }
    });
  }

}
