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

import ucar.unidata.util.StringUtil2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * The possible Grib2Customizers.
 * Configured by standardTableMap.txt
 *
 * @author caron
 * @since 8/1/2014
 */
public class Grib2Table {
  static public enum Type {wmo, dss, gempak, gsd, kma, ncep, ndfd, mrms}
  static public final String tableMapPath = "resources/grib2/standardTableMap.txt";
  static public List<Grib2Table> tables = null;
  static private Grib2Table standardTable = null;

  static private List<Grib2Table> init() {
    List<Grib2Table> result = new ArrayList<>();
    ClassLoader cl = Grib2Table.class.getClassLoader();
    try (InputStream is = cl.getResourceAsStream(tableMapPath)) {
      if (is == null) throw new IllegalStateException("Cant find " + tableMapPath);
      BufferedReader dataIS = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF8")));
      int count = 0;
      while (true) {
        String line = dataIS.readLine();
        if (line == null) break;
        if (line.startsWith("#")) continue;
        count++;

        String[] flds = line.split(";");
        if (flds.length < 7) {
          System.out.printf("%d BAD format == %s%n", count, line);
          continue;
        }

        int fldidx = 0;
        try {
          int center = Integer.parseInt(flds[fldidx++].trim());
          int subcenter = Integer.parseInt(flds[fldidx++].trim());
          int master = Integer.parseInt(flds[fldidx++].trim());
          int local = Integer.parseInt(flds[fldidx++].trim());
          int genProcess = Integer.parseInt(flds[fldidx++].trim());
          String typeName = StringUtil2.remove(flds[fldidx++].trim(), '"');
          String name = StringUtil2.remove(flds[fldidx++].trim(), '"');
          String resource = (flds.length > 7) ? StringUtil2.remove(flds[fldidx++].trim(), '"') : null;
          Type type = Type.valueOf(typeName);
          Grib2Table table = new Grib2Table(name, center, subcenter, master, local, genProcess, resource, type);
          result.add(table);

        } catch (Exception e) {
          System.out.printf("%d %d BAD line == %s : %s%n", count, fldidx, line, e.getMessage());
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    standardTable = new Grib2Table("WMO", 0,-1,-1,-1,-1, WmoCodeTable.standard.getResourceName(), Grib2Table.Type.wmo);
    result.add(standardTable);
    return result;
  }

  public static Grib2Table getTable(Grib2Table.Id id) {
    if (tables == null)
      tables = init();

    // first match wins
    for (Grib2Table table : tables) {
      if (table.id.match(id)) return table;
    }

    return standardTable;
  }

  static public List<Grib2Table> getTables() {
    if (tables == null)
      tables = init();

    return tables;
  }

    ///////////////////////////////////////////////////////////////////////////////////////
  public final String name;
  public final Type type;
  public final Id id;
  private String path;

  Grib2Table(String name, int center, int subCenter, int masterVersion, int localVersion, int genProcessId, String path, Type type) {
    this.name = name;
    this.path = path;
    this.type = type;
    this.id = new Id(center, subCenter, masterVersion, localVersion, genProcessId);
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getName() {
    return name;
  }

  public Type getType() {
    return type;
  }

  public Id getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Grib2Table that = (Grib2Table) o;

    if (!id.equals(that.id)) return false;
    if (!name.equals(that.name)) return false;
    if (path != null ? !path.equals(that.path) : that.path != null) return false;
    if (type != that.type) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + id.hashCode();
    result = 31 * result + (path != null ? path.hashCode() : 0);
    return result;
  }

  static public class Id {
    public final int center, subCenter, masterVersion, localVersion, genProcessId;

    public Id(int center, int subCenter, int masterVersion, int localVersion, int genProcessId) {
      this.center = center;
      this.subCenter = subCenter;
      this.masterVersion = masterVersion;
      this.localVersion = localVersion;
      this.genProcessId = genProcessId;
    }

    boolean match(Id id) {
      if (id.center != center) return false; // must match center
      if (subCenter != -1 && id.subCenter != subCenter) return false;
      if (masterVersion != -1 && id.masterVersion != masterVersion) return false;
      if (localVersion != -1 && id.localVersion != localVersion) return false;
      if (genProcessId != -1 && id.genProcessId != genProcessId) return false;
      return true;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Id that = (Id) o;

      if (center != that.center) return false;
      if (genProcessId != that.genProcessId) return false;
      if (localVersion != that.localVersion) return false;
      if (masterVersion != that.masterVersion) return false;
      if (subCenter != that.subCenter) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = center;
      result = 31 * result + subCenter;
      result = 31 * result + masterVersion;
      result = 31 * result + localVersion;
      result = 31 * result + genProcessId;
      return result;
    }
  }

}
