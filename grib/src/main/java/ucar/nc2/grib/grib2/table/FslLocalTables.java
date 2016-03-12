/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.grib2.table;

import ucar.nc2.constants.CDM;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib2.Grib2Parameter;
import ucar.nc2.util.TableParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * FSL/GSD (center 59)
 * genProcessId 125 = HRRR
 * genProcessId 116 = FIM
 *
 * @author caron
 * @see "http://ruc.noaa.gov/hrrr/GRIB2Table.txt"
 * @since 2/1/12
 */
public class FslLocalTables extends NcepLocalTables {
  public static final int center_id = 59;
  // private static final String hrrrTable = "resources/grib2/noaa_gsd/Fsl-hrrr.csv";
  private static final String fimTable = "resources/grib2/noaa_gsd/fim.gribtable";   // look not used right now
  private static final String hrrrTable = "resources/grib2/noaa_gsd/Fsl-hrrr2.csv";
  private static Map<String, FslLocalTables> tables = new HashMap<>();

  // not a singleton
  public static FslLocalTables getCust(Grib2Table table) {
    FslLocalTables cust = tables.get(getPath(table));
    if (cust != null) return cust;
    cust = new FslLocalTables(table);
    tables.put(table.getPath(), cust);
    return cust;
  }

  static private String getPath(Grib2Table grib2Table) {
    if (grib2Table.getId().genProcessId == 125)
      return fimTable;
    else
      return hrrrTable;
  }

  private FslLocalTables(Grib2Table grib2Table) {
    super(grib2Table);   // default resource path
    grib2Table.setPath(getPath(grib2Table));
    initLocalTable(null);
  }

  @Override
  public String getTablePath(int discipline, int category, int number) {
    if ((category <= 191) && (number <= 191)) return super.getTablePath(discipline, category, number);
    return grib2Table.getPath();
  }

  // must override since we are subclassing NcepLocalTables
  @Override
  public List<GribTables.Parameter> getParameters() {
    List<GribTables.Parameter> result = new ArrayList<>();
    for (Grib2Parameter p : local.values()) result.add(p);
    Collections.sort(result, new ParameterSort());
    return result;
  }

  @Override
  public GribTables.Parameter getParameterRaw(int discipline, int category, int number) {
    return local.get(makeParamId(discipline, category, number));
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
      case 103:
        return "ExREF";
      case 105:
        return "RAP/ RUC";
      case 106:
        return "Developmental Testbed Center Winter Field Experiment, WRF-ARW";
      case 112:
        return "Developmental Testbed Center Winter Field Experiment, WRF-NMM";
      case 116:
        return "Flow-following Finite-volume Icosahedral Model (FIM)";
      case 125:
        return "High-Resolution Rapid Refresh";
      default:
        return null;
    }
  }

  @Override
  public String getLevelNameShort(int id) {
    switch (id) {
      case 200:
        return "Entire_atmosphere";
      default:
        return super.getLevelNameShort(id);
    }
  }

  @Override

  public String getLevelName(int id) {
    switch (id) {
      case 200:
        return "Entire atmosphere layer";
      default:
        return super.getLevelName(id);
    }
  }

  public String getStatisticNameShort(int id) {
    switch (id) {
      case 255:
        return "Interval";
    }
    return super.getStatisticNameShort(id);
  }

  protected void initLocalTable(Formatter f) {
    if (grib2Table.getPath().equals(hrrrTable))
      local = readCsv(hrrrTable, f);
    else
      local = readFim(fimTable, f);
  }

  // debugging
  @Override
  public void lookForProblems(Formatter f) {
    initLocalTable(f);
  }

  private Map<Integer, Grib2Parameter> readCsv(String resourcePath, Formatter f) {
    boolean header = true;
    Map<Integer, Grib2Parameter> result = new HashMap<>(100);

    ClassLoader cl = getClass().getClassLoader();
    try (InputStream is = cl.getResourceAsStream(resourcePath)) {
      if (is == null) throw new IllegalStateException("Cant find " + resourcePath);
      BufferedReader br = new BufferedReader(new InputStreamReader(is, CDM.utf8Charset));
      HashMap<String, Grib2Parameter> names = new HashMap<>(200);

      while (true) {
        String line = br.readLine();
        if (line == null) break;
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
        //RecordNumber,	TableNumber,	DisciplineNumber,	CategoryNumber,	ParameterNumber,	WGrib2Name,	NCLName,				FieldType,			Description,													Units,

        String recordNumber = flds[0].trim();
        int tableNumber = Integer.parseInt(flds[1].trim());
        int disciplineNumber = Integer.parseInt(flds[2].trim());
        int categoryNumber = Integer.parseInt(flds[3].trim());
        int parameterNumber = Integer.parseInt(flds[4].trim());

        String WGrib2Name = flds[5].trim();
        String NCLName = flds[6].trim();
        String FieldType = flds[7].trim();
        String Description = flds[8].trim();
        String Units = flds[9].trim();
        if (f != null)
          f.format("%3s %3d %3d %3d %3d %-10s %-25s %-30s %-100s %-20s%n", recordNumber, tableNumber, disciplineNumber, categoryNumber, parameterNumber,
                  WGrib2Name, NCLName, FieldType, Description, Units);

        String name = !WGrib2Name.equals("var") ? WGrib2Name : FieldType;
        Grib2Parameter s = new Grib2Parameter(disciplineNumber, categoryNumber, parameterNumber, name, Units, null, Description);
        // s.desc = Description;
        result.put(makeParamId(disciplineNumber, categoryNumber, parameterNumber), s);
        if (f != null) f.format(" %s%n", s);
        if (categoryNumber > 191 || parameterNumber > 191) {
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

  /*
#                                                         iparm ztype   iz1   iz2  itr iscale  ABBREV     UNITS
#
          1         2         3         4         5         6         7         8         9        10        11
012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
u-component of wind - b                                      33   109                       1   US3D_B       m/s
u-component of wind - b                                      33   109                       1   US3D_B       m/s
v-component of wind - b                                      34   109                       1   VS3D_B       m/s
height - b                                                    7   109                       1   PH3D_B       gpm
pressure - b                                                  1   109                       0   PR3D_B        Pa
virtual potential temperature - b                           189   109                       2   TH3D_B         K
temperature - b                                              11   109                       1   TK3D_B         K
dew-point temperature - b                                    17   109                       1   TD3D_B         K
water vapor mixing ratio - b                                 53   109                       5   QV3D_B
vertical velocity - b                                        39   109                       1   WS3D_B      Pa/s
cloud water mixing ratio - b                                153   109                       6   QW3D_B     Kg/Kg
ozone mixing ratio - b                                      154   109                       8   OZ3D_B     Kg/Kg

   */

  // this doesnt work - its a GRIB1 table
  private Map<Integer, Grib2Parameter> readFim(String resourcePath, Formatter f) {
    Map<Integer, Grib2Parameter> result = new HashMap<>(100);

    ClassLoader cl = getClass().getClassLoader();
    try (InputStream is = cl.getResourceAsStream(resourcePath)) {
      if (is == null) throw new IllegalStateException("Cant find " + resourcePath);

      if (f != null) f.format("%50s == %s, %s, %-20s, %-20s%n", "desc", "param", "ztype", "abbrev", "units");
      List<TableParser.Record> recs = TableParser.readTable(is, "56,63i,68i,93,102,112", 50000);
      for (TableParser.Record record : recs) {
        String desc = ((String) record.get(0)).trim();
        int param = (Integer) record.get(1);
        int ztype = (Integer) record.get(2);
        String abbrev = ((String) record.get(4)).trim();
        String units = ((String) record.get(5)).trim();

        if (f != null) f.format("%50s == %3d, %3d, %-20s, %-20s%n", desc, param, ztype, abbrev, units);

        Grib2Parameter gp = new Grib2Parameter(0, 0, param, abbrev, units, null, desc);
        result.put(makeParamId(0, 0, param), gp);
      }

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }

    return result;
  }

  public static void main(String[] args) {
    FslLocalTables hrrr = new FslLocalTables(new Grib2Table("GSD_HRRR", 59, -1, -1, -1, 125, null, Grib2Table.Type.gsd));
    // FslLocalTables fim = new FslLocalTables(0,0,1,116);
    Formatter f = new Formatter();
    Grib2Parameter.compareTables("FSL-HRRR", "Standard WMO version", hrrr.getParameters(), Grib2Customizer.factory(0, 0, 0, 0, 0), f);
    System.out.printf("%s%n", f);

    Formatter f2 = new Formatter();
    Grib2Parameter.compareTables("FSL-HRRR", "NCEP Table", hrrr.getParameters(), Grib2Customizer.factory(7, 0, 0, 0, 0), f2);
    System.out.printf("%s%n", f2);

  }
}
