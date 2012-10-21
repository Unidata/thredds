/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib.grib2.table;

import ucar.nc2.grib.grib2.Grib2Parameter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

/**
 * FSL (center 59)
 * @see "http://ruc.noaa.gov/hrrr/GRIB2Table.txt"
 *
 * @author caron
 * @since 2/1/12
 */
public class FslLocalTables extends LocalTables {
  private static final String tableName = "resources/grib2/local/Fsl-hrrr.csv";
  //private static final String tableName2 = "resources/grib2/local/Fsl-hrrr2.csv";
  private static boolean debug = false;

  static FslLocalTables localFactory(int center, int subCenter, int masterVersion, int localVersion) {
    return new FslLocalTables(center, subCenter, masterVersion, localVersion);
  }

  ////
  private String tableNameUsed;

  private FslLocalTables(int center, int subCenter, int masterVersion, int localVersion) {
    super(center, subCenter, masterVersion, localVersion);
  }

  @Override
  public String getTablePath(int discipline, int category, int number) {
    if ((category <= 191) && (number <= 191)) return super.getTablePath(discipline, category, number);
    return tableNameUsed;
  }

  // LOOK  maybe combine grib1, grib2 and bufr ??
  @Override
  public String getSubCenterName(int center, int subcenter) {

    switch (subcenter) {
      case 0:
        return null;
      case 1:
        return "FSL/FRD Regional Analysis and Prediction Branch";
      case 2:
        return "FSL/FRD Local Analysis and Prediction Branch";
    }
    return super.getSubCenterName(center, subcenter);
  }

  @Override
  public String getGeneratingProcessName(int genProcess) {
    switch (genProcess) {
      case 106:
         return "Developmental Testbed Center Winter Field Experiment, WRF-ARW";
      case 112:
        return "Developmental Testbed Center Winter Field Experiment, WRF-NMM";
      case 116:
        return "FIM Model from NOAA/ESRL/Global Systems Division";
      case 125:
        return "High-Resolution Rapid Refresh";
      default:
        return null;
    }
  }

  @Override
  public String getLevelNameShort(int id) {
    if (id < 192) return super.getLevelNameShort(id);
    switch (id) {
      case 200:
        return "Entire_atmosphere";
      default:
        return null;
    }
  }

  @Override

  public String getLevelName(int id) {
    if (id < 192) return super.getLevelName(id);
    switch (id) {
      case 200:
        return "Entire atmosphere layer";
      default:
        return null;
    }
  }

  public String getIntervalNameShort(int id) {
    switch (id) {
      case 255: return "Interval";
    }
    return super.getIntervalNameShort(id);
  }

  @Override
  protected void initLocalTable() {
    local = initLocalTable(tableName, null); // (this.localVersion == 0) ? tableName2 : tableName, null);
  }

  // debugging
  @Override
  public void lookForProblems(Formatter f) {
    initLocalTable(tableNameUsed, f);
  }

  private Map<Integer, Grib2Parameter> initLocalTable(String resourcePath, Formatter f) {
    this.tableNameUsed = resourcePath;
    boolean header = true;
    Map<Integer, Grib2Parameter> result = new HashMap<Integer, Grib2Parameter>(100);

    ClassLoader cl = getClass().getClassLoader();
    InputStream is = cl.getResourceAsStream(resourcePath);
    if (is == null) throw new IllegalStateException("Cant find " + resourcePath);
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    HashMap<String, Grib2Parameter> names = new HashMap<String, Grib2Parameter>(200);
    try {
      while (true) {
        String line = br.readLine();
        if (line.startsWith("Record")) break;
      }

      while (true) {
        String line = br.readLine();
        if (line == null) break;
        if ((line.length() == 0) || line.startsWith("#")) continue;
        String[] flds = line.split(",");

        /* System.out.printf("%d == ", flds.length);
        for (String s : flds) System.out.printf("%s,", s.trim());
        System.out.printf("%n"); */

        // RecordNumber,	TableNumber,	DisciplineNumber,	CategoryNumber,	ParameterNumber,	WGrib2Name,	NCLName,				FieldType,			Description,													Units,

        String RecordNumber = flds[0].trim();
        int TableNumber = Integer.parseInt(flds[1].trim());
        int DisciplineNumber = Integer.parseInt(flds[2].trim());
        int CategoryNumber = Integer.parseInt(flds[3].trim());
        int ParameterNumber = Integer.parseInt(flds[4].trim());

        String WGrib2Name = flds[5].trim();
        String NCLName = flds[6].trim();
        String FieldType = flds[7].trim();
        String Description = flds[8].trim();
        String Units = flds[9].trim();
        if (debug) System.out.printf("%3s %3d %3d %3d %3d %-10s %-25s %-30s %-100s %-20s%n", RecordNumber,	TableNumber,	DisciplineNumber,	CategoryNumber,	ParameterNumber,
                WGrib2Name,	NCLName, FieldType, Description, Units);

        String name = (WGrib2Name != null && !WGrib2Name.equals("var")) ? WGrib2Name : FieldType;
        Grib2Parameter s = new Grib2Parameter(DisciplineNumber, CategoryNumber, ParameterNumber, name, Units, null, Description);
        s.desc = Description;
        result.put(makeHash(DisciplineNumber, CategoryNumber, ParameterNumber), s);
        if (debug) System.out.printf(" %s%n", s);
          if (CategoryNumber > 191 || ParameterNumber > 191) {
          Grib2Parameter dup = names.get(s.getName());
          if (dup != null && f != null) {
            if (header) f.format("Problems in table %s %n", resourcePath);
            header = false;
            f.format(" DUPLICATE NAME %s and %s (%s)%n", s.getId(), dup.getId(), s.getName());
          }
        }
        names.put(s.getName(), s);
      }

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }

    return result;
  }

  public static void main(String[] args) {
    FslLocalTables t = new FslLocalTables(59,0,0,0);
    Formatter f = new Formatter();
    Grib2Parameter.compareTables("FSL-HRRR", "Standard WMO version 8", t.getParameters(), Grib2Customizer.factory(0,0,0,0), f);
    System.out.printf("%s%n", f);

    Formatter f2 = new Formatter();
    Grib2Parameter.compareTables("FSL-HRRR", "NCEP Table", t.getParameters(), Grib2Customizer.factory(7,0,0,0), f2);
    System.out.printf("%s%n", f2);

  }
}
