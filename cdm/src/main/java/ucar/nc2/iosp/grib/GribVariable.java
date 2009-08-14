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
package ucar.nc2.iosp.grib;

import ucar.grib.Index;
import ucar.grib.Parameter;
import ucar.grib.TableLookup;
import ucar.grib.grib1.Grib1Lookup;
import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.unidata.util.StringUtil;
import ucar.grid.GridParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * A Variable for a Grib dataset.
 *
 * @author caron
 * @deprecated
 */
public class GribVariable {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GribVariable.class);

  private String name, desc, vname;
  int levelType1; // from the Index.GridRecord;

  //private Index.GribRecord firstRecord;
  private TableLookup lookup;
  private boolean isGrib1;

  private GribHorizCoordSys hcs;
  private GribCoordSys vcs; // maximal strategy (old way)
  private GribTimeCoord tcs;
  private GribVertCoord vc;
  private List<Index.GribRecord> records = new ArrayList<Index.GribRecord>(); // becomes empty after initiazing

  private int nlevels, ntimes;
  private GribRecordLW[] recordTracker;
  private int decimalScale = 0;
  private boolean hasVert = false;
  private boolean showRecords = false, showGen = false;

  GribVariable(String name, String desc, GribHorizCoordSys hcs, TableLookup lookup) {
    this.name = name; // used to get unique grouping of products
    this.desc = desc;
    this.hcs = hcs;
    this.lookup = lookup;
    isGrib1 = (lookup instanceof Grib1Lookup);
  }

  void addProduct(Index.GribRecord record) {
    if (records.size() == 0)
      levelType1 = record.levelType1;
    records.add(record);
  }

  List<Index.GribRecord> getRecords() {
    return records;
  }

  Index.GribRecord getFirstRecord() {
    return records.get(0);
  }

  GribHorizCoordSys getHorizCoordSys() {
    return hcs;
  }

  GribCoordSys getVertCoordSys() {
    return vcs;
  }

  GribVertCoord getVertCoord() {
    return vc;
  }

  boolean hasVert() {
    return hasVert;
  }

  void setVarName(String vname) {
    this.vname = vname;
  }

  void setVertCoordSys(GribCoordSys vcs) {
    this.vcs = vcs;
  }

  void setVertCoord(GribVertCoord vc) {
    this.vc = vc;
  }

  void setTimeCoord(GribTimeCoord tcs) {
    this.tcs = tcs;
  }

  int getVertNlevels() {
    return (vcs == null) ? vc.getNLevels() : vcs.getNLevels();
  }

  String getVertName() {
    return (vcs == null) ? vc.getVariableName() : vcs.getVerticalName();
  }

  String getVertLevelName() {
    return (vcs == null) ? vc.getLevelName() : vcs.getVerticalName();
  }

  boolean getVertIsUsed() {
    return (vcs == null) ? !vc.dontUseVertical : !vcs.dontUseVertical;
  }

  int getVertIndex(Index.GribRecord p) {
    return (vcs == null) ? vc.getIndex(p) : vcs.getIndex(p);
  }

  int getNTimes() {
    return (tcs == null) ? 1 : tcs.getNTimes();
  }

  /* String getSearchName() {
    Parameter param = lookup.getParameter( firstRecord);
    String vname = lookup.getLevelName( firstRecord);
    return param.getDescription() + " @ " + vname;
  } */

  Variable makeVariable(NetcdfFile ncfile, Group g, boolean useDesc) {
    assert records.size() > 0 : "no records for this variable";
    Index.GribRecord firstRecord = records.get(0);

    nlevels = getVertNlevels();
    ntimes = tcs.getNTimes();
    decimalScale = firstRecord.decimalScale;

    if (vname == null)
      vname = useDesc ? desc : name;
    vname = StringUtil.replace(vname, ' ', "_");

    Variable v = new Variable(ncfile, g, null, vname);
    v.setDataType(DataType.FLOAT);

    String dims = tcs.getName();
    if (getVertIsUsed()) {
      dims = dims + " " + getVertName();
      hasVert = true;
    }

    if (hcs.isLatLon())
      dims = dims + " lat lon";
    else
      dims = dims + " y x";

    v.setDimensions(dims);
    GridParameter param = lookup.getParameter(firstRecord);

    v.addAttribute(new Attribute("units", param.getUnit()));
    v.addAttribute(new Attribute("long_name", Index2NC.makeLongName(firstRecord, lookup)));
    v.addAttribute(new Attribute("missing_value", lookup.getFirstMissingValue()));
    if (!hcs.isLatLon()) {
      if (GribServiceProvider.addLatLon) v.addAttribute(new Attribute("coordinates", "lat lon"));
      v.addAttribute(new Attribute("grid_mapping", hcs.getGridName()));
    }

    int[] paramId = lookup.getParameterId(firstRecord);
    if (paramId[0] == 1) {
      v.addAttribute(new Attribute("GRIB_param_name", param.getDescription()));
      v.addAttribute(new Attribute("GRIB_center_id", paramId[1]));
      v.addAttribute(new Attribute("GRIB_table_id", paramId[2]));
      v.addAttribute(new Attribute("GRIB_param_number", paramId[3]));
    } else {
      v.addAttribute(new Attribute("GRIB_param_discipline", lookup.getDisciplineName(firstRecord)));
      v.addAttribute(new Attribute("GRIB_param_category", lookup.getCategoryName(firstRecord)));
      v.addAttribute(new Attribute("GRIB_param_name", param.getName()));
    }
    v.addAttribute(new Attribute("GRIB_param_id", Array.factory(int.class, new int[]{paramId.length}, paramId)));
    v.addAttribute(new Attribute("GRIB_product_definition_type", lookup.getProductDefinitionName(firstRecord)));
    v.addAttribute(new Attribute("GRIB_level_type", firstRecord.levelType1));

    //if (pds.getTypeSecondFixedSurface() != 255 )
    //  v.addAttribute( new Attribute("GRIB2_type_of_second_fixed_surface", pds.getTypeSecondFixedSurfaceName()));

    /* String coordSysName = getVertIsUsed() ? getVertName() :
        (hcs.isLatLon() ? "latLonCoordSys" : "projectionCoordSys");
    v.addAttribute( new Attribute(_Coordinate.Systems", coordSysName)); */

    v.setSPobject(this);

    if (showRecords)
      System.out.println("Variable " + getName());

    recordTracker = new GribRecordLW[ntimes * nlevels];
    for (Index.GribRecord p : records) {
      if (showRecords)
        System.out.println(" " + vc.getVariableName() +
            " (type=" + p.levelType1 + "," + p.levelType2 + ")  value=" + p.levelValue1 + "," + p.levelValue2 +
            " # genProcess=" + p.typeGenProcess);
      if (showGen && !isGrib1 && !p.typeGenProcess.equals("2"))
        System.out.println(" " + getName() + " genProcess=" + p.typeGenProcess);
      //System.out.println(" "+getName()+ " genProcess="+p.typeGenProcess);

      int level = getVertIndex(p);
      if (!getVertIsUsed() && level > 0) {
        log.warn("inconsistent level encoding=" + level);
        level = 0; // inconsistent level encoding ??
      }
      int time = tcs.getIndex(p);
      // System.out.println("time="+time+" level="+level);
      if (level < 0) {
        log.warn("NOT FOUND record; level=" + level + " time= " + time + " for " + getName() + " file=" + ncfile.getLocation() + "\n"
            + "   " + getVertLevelName() + " (type=" + p.levelType1 + "," + p.levelType2 + ")  value=" + p.levelValue1 + "," + p.levelValue2 + "\n");

        getVertIndex(p); // allow breakpoint
        continue;
      }

      if (time < 0) {
        log.warn("NOT FOUND record; level=" + level + " time= " + time + " for " + getName() + " file=" + ncfile.getLocation() + "\n"
            + " forecastTime= " + p.forecastTime + " date= " + tcs.getValidTime(p) + "\n");

        tcs.getIndex(p); // allow breakpoint
        continue;
      }

      int recno = time * nlevels + level;
      if (recordTracker[recno] == null)
        recordTracker[recno] = new GribRecordLW(p);
      else {
        GribRecordLW q = recordTracker[recno];
        if (!p.typeGenProcess.equals( q.typeGenProcess)) { // LOOK what the hell is this?
          log.warn("Duplicate record; level=" + level + " time= " + time + " for " + getName() + " file=" + ncfile.getLocation() + "\n"
            + "   " + getVertLevelName() + " (type=" + p.levelType1 + "," + p.levelType2 + ")  value=" + p.levelValue1 + "," + p.levelValue2 + "\n"
            + "   gen=" + p.typeGenProcess+" prev="+ q.typeGenProcess);
        }
        recordTracker[recno] = new GribRecordLW(p); // replace it with latest one
        // System.out.println("   gen="+p.typeGenProcess+" "+q.typeGenProcess+"=="+lookup.getTypeGenProcessName(p));
      }
    }

    // let all references to Index go, to reduce retained size
    records.clear();

    return v;
  }

  void dumpMissing() {
    //System.out.println("  " +name+" ntimes (across)= "+ ntimes+" nlevs (down)= "+ nlevels+":");
    System.out.println("  " + name);
    for (int j = 0; j < nlevels; j++) {
      System.out.print("   ");
      for (int i = 0; i < ntimes; i++) {
        boolean missing = recordTracker[i * nlevels + j] == null;
        System.out.print(missing ? "-" : "X");
      }
      System.out.println();
    }
  }

  int dumpMissingSummary() {
    if (nlevels == 1) return 0;

    int count = 0;
    int total = nlevels * ntimes;

    for (int i = 0; i < total; i++)
      if (recordTracker[i] == null) count++;

    System.out.println("  MISSING= " + count + "/" + total + " " + name);
    return count;
  }

  public GribRecordLW findRecord(int time, int level) {
    return recordTracker[time * nlevels + level];
  }

  public String getName() {
    return name;
  }

  public String getParamName() {
    return desc;
  }

  public int getDecimalScale() {
    return decimalScale;
  }

  public boolean equals(Object oo) {
    if (this == oo) return true;
    if (!(oo instanceof GribVariable)) return false;
    GribVariable that = (GribVariable) oo;

    if (!getName().equals(that.getName())) return false;
    if (!hcs.getID().equals(that.hcs.getID())) return false;
    if (levelType1 != that.levelType1) return false;

    return true;
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37 * result + name.hashCode();
      result += 37 * result + levelType1;
      result += 37 * result + hcs.getID().hashCode();
      hashCode = result;
    }
    return hashCode;
  }

  private int hashCode = 0;


  public String dump() {
    DateFormatter formatter = new DateFormatter();
    StringBuilder sbuff = new StringBuilder();
    sbuff.append(name).append(" ").append(records.size()).append("\n");
    for (Index.GribRecord record : records) {
      sbuff.append(" level = ").append(record.levelType1).append(" ").append(record.levelValue1);
      if (null != record.getValidTime())
        sbuff.append(" time = ").append(formatter.toDateTimeString(record.getValidTime()));
      sbuff.append("\n");
    }
    return sbuff.toString();
  }
}