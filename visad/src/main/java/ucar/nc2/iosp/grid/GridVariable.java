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

package ucar.nc2.iosp.grid;

import ucar.ma2.DataType;

import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil2;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;


/**
 * A Variable for a Grid dataset.
 *
 * @author caron
 */
public class GridVariable {

  /**
   * logger
   */
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GridVariable.class);

  private final String filename;

  /**
   * disambiguated name == parameter name [ + suffix] [ + level]
   */
  private final String name;

  /**
   * variable name
   */
  public String vname;

  /**
   * first grid record
   */
  protected GridRecord firstRecord;

  /**
   * lookup table
   */
  protected GridTableLookup lookup;

  /**
   * horizontal coord system
   */
  private GridHorizCoordSys hcs;

  /**
   * time coord system
   */
  protected GridTimeCoord tcs = null;

  /**
   * ensemble coord system
   */
  protected GridEnsembleCoord ecs = null;

  /**
   * vertical coordinate
   */
  protected GridVertCoord vc = null;

  /**
   * list of records that make up this variable
   */
  private List<GridRecord> records = new ArrayList<>();  // GridRecord

  /**
   * number of levels
   */
  protected int nlevels;

  /**
   * number of Ensembles
   */
  private int nens;

  /**
   * number of times
   */
  protected int ntimes;

  /**
   * record tracker
   */
  protected GridRecord[] recordTracker;

  /**
   * flag for having a vertical coordinate
   */
  private boolean hasVert = false;

  /**
   * Create a new GridVariable
   *
   * @param name   name with level
   * @param hcs    horizontal coordinate system
   * @param lookup lookup table
   */
  protected GridVariable(String filename, String name, GridHorizCoordSys hcs, GridTableLookup lookup) {
    this.filename = filename;  //for debugging
    this.name = name;  // used to get unique grouping of products
    this.hcs = hcs;
    this.lookup = lookup;
  }

  /**
   * Add in a new product
   *
   * @param record grid  to add
   */
  void addProduct(GridRecord record) {
    records.add(record);
    if (firstRecord == null) {
      firstRecord = record;
    }
  }

  /**
   * Get the list of grids
   *
   * @return grid records
   */
  List<GridRecord> getRecords() {
    return records;
  }

  /**
   * get the first grid record
   *
   * @return the first in the list
   */
  GridRecord getFirstRecord() {
    return records.get(0);
  }

  /**
   * Get the horizontal coordinate system
   *
   * @return the horizontal coordinate system
   */
  GridHorizCoordSys getHorizCoordSys() {
    return hcs;
  }

  /**
   * Get the vertical coordinate
   *
   * @return the vertical coordinate
   */
  GridVertCoord getVertCoord() {
    return vc;
  }

  /**
   * Does this have a vertical dimension
   *
   * @return true if has a vertical dimension
   */
  boolean hasVert() {
    return hasVert;
  }

  /**
   * Set the variable name
   *
   * @param vname the variable name
   */
  void setVarName(String vname) {
    this.vname = vname;
  }

  /**
   * Set the vertical coordinate
   *
   * @param vc the vertical coordinate
   */
  void setVertCoord(GridVertCoord vc) {
    this.vc = vc;
  }

  /**
   * Set the time coordinate
   *
   * @param tcs the time coordinate
   */
  void setTimeCoord(GridTimeCoord tcs) {
    this.tcs = tcs;
  }

  /**
   * Set the Ensemble coordinate
   *
   * @param ecs the Ensemble coordinate
   */
  void setEnsembleCoord(GridEnsembleCoord ecs) {
    this.ecs = ecs;
  }

  /**
   * Get the number of Ensemble
   *
   * @return the number of Ensemble
   */
  public int getNEnsembles() {
    return (ecs == null) ? 1 : ecs.getNEnsembles();
  }

  /*
   * Get the product definition number of Ensemble
   *
   * @return the product definition number of Ensemble
   *
  public int getPDN() {
    return (ecs == null) ? -1 : ecs.getPDN();
  }

  /*
   * Get the types of Ensemble
   *
   * @return the types of Ensemble
   *
  public int[] getEnsTypes() {
    return (ecs == null) ? null : ecs.getEnsType();
  } */

  /*
   * Get the Index of Ensemble
   *
   * @param record GridRecord
   * @return the Index of Ensemble
   *
  int getEnsembleIndex(GridRecord record) {
    return (ecs == null) ? 1 : ecs.getIndex(record);
  } */

  /**
   * Does this have a Ensemble coordinate
   *
   * @return true if has a Ensemble coordinate
   */
  boolean hasEnsemble() {
    return (ecs != null);
  }

  protected boolean isEnsemble() {
    return false;
  }

  /**
   * Get the number of vertical levels
   *
   * @return the number of vertical levels
   */
  int getVertNlevels() {
    return vc.getNLevels();
  }

  /**
   * Get the name of the vertical dimension
   *
   * @return the name of the vertical dimension
   */
  String getVertName() {
    return vc.getVariableName();
  }

  /**
   * Get the name of the vertical level
   *
   * @return the name of the vertical level
   */
  String getVertLevelName() {
    return  vc.getLevelName();
  }

  /**
   * Is vertical used?
   *
   * @return true if vertical used
   */
  boolean getVertIsUsed() {
    return vc.isVertDimensionUsed();
  }

  /**
   * Get the index in the vertical for the particular grid
   *
   * @param p grid to check
   * @return the index
   */
  protected int getVertIndex(GridRecord p) {
    return vc.getIndex(p);
  }

  /**
   * Get the number of times
   *
   * @return the number of times
   */
  int getNTimes() {
    return (tcs == null) ? 1 : tcs.getNTimes();
  }

  /*
   * is this a time interval variable
   *
   * @return true if uses time intervals
   *
  boolean isInterval() {
    if (firstRecord instanceof GribGridRecord) {
      GribGridRecord ggr = (GribGridRecord) firstRecord;
      return ggr.isInterval();
    }
    return false;
  }

  String getIntervalTypeName() {
    if (firstRecord instanceof GribGridRecord) {
      GribGridRecord ggr = (GribGridRecord) firstRecord;
      return ggr.getStatisticalProcessTypeName();
    }
    return null;
  } */

  protected void addExtraAttributes(GridParameter param, Variable v) {
    int icf = hcs.getGds().getInt(GridDefRecord.VECTOR_COMPONENT_FLAG);
    String flag = GridCF.VectorComponentFlag.of( icf);
    v.addAttribute(new Attribute(GridDefRecord.VECTOR_COMPONENT_FLAG, flag));
  }


  protected boolean trackRecords(int time, int level, GridRecord p, RandomAccessFile raf, boolean messSent) {
    int recno = time * nlevels + level;

    if (recordTracker[recno] == null) {
      recordTracker[recno] = p;
      if (log.isDebugEnabled()) log.debug(" " + vc.getVariableName() + " (type=" + p.getLevelType1() + "," + p.getLevelType2() + ")  value="
              + p.getLevel1() + "," + p.getLevel2());
    }

    return false;
  }

  /**
   * Make the netcdf variable. If vname is not already set, use useName as name
   *
   * @param ncfile  netCDF file
   * @param g       group
   * @param useName use this as the variable name
   * @param raf read from here
   * @return the netcdf variable
   */
  Variable makeVariable(NetcdfFile ncfile, Group g, String useName, RandomAccessFile raf) {
    assert records.size() > 0 : "no records for this variable";

    this.nlevels = getVertNlevels();
    this.ntimes = tcs.getNTimes();
    if (vname == null) {
      useName = StringUtil2.replace(useName, ' ', "_");
      this.vname = useName;
    }

    Variable v = new Variable(ncfile, g, null, vname);
    v.setDataType(DataType.FLOAT);

    Formatter dims = new Formatter();

    if (hasEnsemble()) {
      dims.format("ens ");
    }

    dims.format("%s ", tcs.getName());

    if (getVertIsUsed()) {
      dims.format("%s ",  getVertName());
      hasVert = true;
    }

    if (hcs.isLatLon()) {
      dims.format("lat lon");
    } else {
      dims.format("y x");
    }

    v.setDimensions(dims.toString());

    // add attributes
    GridParameter param = lookup.getParameter(firstRecord);
    if (param == null) return null;

    String unit = param.getUnit();
    if (unit == null) unit = "";
    v.addAttribute(new Attribute("units", unit));

    v.addAttribute(new Attribute("long_name", makeLongName()));
    v.addAttribute(new Attribute("missing_value", lookup.getFirstMissingValue()));
    if (!hcs.isLatLon()) {
      if (GridServiceProvider.addLatLon)
        v.addAttribute(new Attribute("coordinates", "lat lon"));
      v.addAttribute(new Attribute("grid_mapping", hcs.getGridName()));
    }

    addExtraAttributes(param, v);

    v.setSPobject(this);

    int nrecs = ntimes * nlevels;
    if (hasEnsemble()) nrecs *= ecs.getNEnsembles();
    recordTracker = new GridRecord[nrecs];

    if (log.isDebugEnabled()) log.debug("Record Assignment for Variable " + getName());
    boolean oneSent = false;

    for (GridRecord p : records) {
      int level = getVertIndex(p);
      if (!getVertIsUsed() && (level > 0)) {
        log.warn("inconsistent level encoding=" + level);
        level = 0;  // inconsistent level encoding ??
      }

      int time = tcs.findIndex(p);
      // System.out.println("time="+time+" level="+level);
      if (level < 0) {
        log.warn("LEVEL NOT FOUND record; level=" + level + " time= "
                + time + " for " + getName() + " file="
                + ncfile.getLocation() + "\n" + "   "
                + getVertLevelName() + " (type=" + p.getLevelType1()
                + "," + p.getLevelType2() + ")  value="
                + p.getLevel1() + "," + p.getLevel2() + "\n");

        getVertIndex(p);  // allow breakpoint
        continue;
      }

      if (time < 0) {
        log.warn("TIME NOT FOUND record; level=" + level + " time= "
                + time + " for " + getName() + " file="
                + ncfile.getLocation() + "\n" + " validTime= "
                + p.getValidTime() + "\n");

        tcs.findIndex(p);  // allow breakpoint
        continue;
      }

      oneSent = trackRecords(time, level, p, raf, oneSent);
    }

    // let all references to Index go, to reduce retained size LOOK
    records.clear();

    return v;
  }


  //////////////////////////////////////
  // debugging


  public void showRecord(int recnum, Formatter f) {
    if ((recnum < 0) || (recnum > recordTracker.length - 1)) {
      f.format("%d out of range [0,%d]%n", recnum, recordTracker.length - 1);
      return;
    }
    GridRecord gr = recordTracker[recnum];

    if (hasEnsemble()) {
      // recnum = ens * (ntimes * nlevels) + (time * nlevels) + level
      int ens = recnum / (nlevels * ntimes);
      int tmp = recnum - ens *(nlevels * ntimes);
      int time = tmp / nlevels;
      int level = tmp % nlevels;
      f.format("recnum=%d (record hash=%d) ens=%d time=%s(%d) level=%f(%d)%n", recnum, gr.hashCode(), ens, tcs.getCoord(time), time, vc.getCoord(level), level);
    }  else {
      int time = recnum / nlevels;
      int level = recnum % nlevels;
      f.format("recnum=%d (record hash=%d) time=%s(%d) level=%f(%d)%n", recnum, gr.hashCode(), tcs.getCoord(time), time, vc.getCoord(level), level);
    }
  }


  /**
   * Dump out the missing data
   * @param f write to this
   */
  public void showMissing(Formatter f) {
    //System.out.println("  " +name+" ntimes (across)= "+ ntimes+" nlevs (down)= "+ nlevels+":");
    int count = 0, total = 0;
    f.format("  %s%n", name);
    for (int j = 0; j < nlevels; j++) {
      f.format("   ");
      for (int i = 0; i < ntimes; i++) {
        boolean missing = recordTracker[i * nlevels + j] == null;
        f.format("%s", missing ? "-" : "X");
        if (missing) count++;
        total++;
      }
      f.format("%n");
    }
    f.format("  MISSING= %d / %d for %s%n",count,total, name);
  }

  /**
   * Dump out the missing data as a summary
   *
   * @param f write to this
   * @return number of missing levels
   */
  public int showMissingSummary(Formatter f) {
    int count = 0;
    int total = recordTracker.length;

    for (int i = 0; i < total; i++) {
      if (recordTracker[i] == null)
        count++;
    }

    f.format("  MISSING= %d / %d for %s%n", count, total, name);
    return count;
  }

  /**
   * Find the grid record for the time and level indices
   * Canonical ordering is ens, time, level
   * @param ens   ensemble index
   * @param time  time index
   * @param level level index
   * @return the record or null
   */
  public GridRecord findRecord(int ens, int time, int level) {
    if (hasEnsemble()) {
      return recordTracker[ens * (ntimes * nlevels) + (time * nlevels) + level];
    } else {
      return recordTracker[time * nlevels + level];
    }
  }

  /**
   * Check for equality
   *
   * @param oo object in question
   * @return true if they are equal
   */
  public boolean equals(Object oo) {
    if (this == oo) {
      return true;
    }
    if (!(oo instanceof GridVariable)) {
      return false;
    }
    return hashCode() == oo.hashCode();
  }

  /**
   * Get the name
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Override Object.hashCode() to implement equals.
   *
   * @return equals;
   */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37 * result + name.hashCode();
      result += 37 * result + firstRecord.getLevelType1();
      result += 37 * result + hcs.getID().hashCode();
      hashCode = result;
    }
    return hashCode;
  }

  /**
   * hash code
   */
  private volatile int hashCode = 0;

  @Override
  public String toString() {
    return vname == null ? name : vname;
  }

  /**
   * Dump this variable
   *
   * @return the variable
   */
  public String dump() {
    DateFormatter formatter = new DateFormatter();
    Formatter sbuff = new Formatter();
    sbuff.format("%s %d %n", name, records.size());
    for (GridRecord record : records) {
      sbuff.format(" level = %d %f", record.getLevelType1(), record.getLevel1());
      if (null != record.getValidTime())
        sbuff.format(" time = %s", formatter.toDateTimeString(record.getValidTime()));
      sbuff.format("%n");
    }
    return sbuff.toString();
  }

  /**
   * Make a long name for the variable
   *
   * @return long variable name
   */
  protected String makeLongName() {
    Formatter f = new Formatter();
    GridParameter param = lookup.getParameter(firstRecord);
    if (param == null) return null;

    f.format("%s", param.getDescription());

    String levelName = makeLevelName(firstRecord, lookup);
    if (levelName.length() != 0)
      f.format(" @ %s", levelName);

    return f.toString();
  }

  /**
   * Make the level name
   *
   * @param gr     grid record
   * @param lookup lookup table
   * @return name for the level
   */
  public String makeLevelName(GridRecord gr, GridTableLookup lookup) {
    return lookup.getLevelName(gr);
  }

  public String makeIntervalName() {
    // get information from timeCoord
    if (tcs.getConstantInterval() < 0)
      return " Mixed Intervals";
    else
      return tcs.getConstantInterval() + " " + tcs.getTimeUnit() + " Intervals";
  }


}

