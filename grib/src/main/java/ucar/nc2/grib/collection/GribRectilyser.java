/*
 *
 *  * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package ucar.nc2.grib.collection;


import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.MFile;
import ucar.coord.*;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.VertCoord;
//import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.time.CalendarPeriod;

import java.io.IOException;
import java.util.*;

/**
 * Turn a collection of GribRecords into a rectangular array.
 * Do seperately for each Group
 * Helper class for GribCollectionBuilder.
 * T is Grib1Record or Grib2Record
 *
 * @author caron
 * @since 11/26/2013
 */
public abstract class GribRectilyser<T> {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribRectilyser.class);

  private final GribTables cust;
  private final int gdsHash;

  protected final boolean intvMerge;
  protected final boolean useGenType;

  private final List<T> records;
  private List<VariableBag> gribvars;
  private List<Coordinate> coords;

  public GribRectilyser(GribTables cust, List<T> records, int gdsHash, Map<String, Boolean> pdsConfig) {
    this.cust = cust;
    this.records = records;
    this.gdsHash = gdsHash;

    intvMerge = assignValue(pdsConfig, "intvMerge", true);
    // useTableVersion = assignValue(pdsConfig, "useTableVersion", true);  // only for GRIB1
    useGenType = assignValue(pdsConfig, "useGenType", false);
  }

  private boolean assignValue(Map<String, Boolean> pdsHash, String key, boolean value) {
    if (pdsHash != null) {
      Boolean b = pdsHash.get(key);
      if (b != null) value = b;
    }
    return value;
  }

  public List<T> getRecords() {
    return records;
  }

  public List<VariableBag> getGribvars() {
    return gribvars;
  }

  public List<Coordinate> getCoordinates() {
    return coords;
  }

  List<MFile> files = null; // temp debug
  public void make(FeatureCollectionConfig.GribConfig config, List<MFile> files, Counter counter, Formatter info) throws IOException {
    this.files = files;
    boolean isDense = (config != null) && "dense".equals(config.getParameter("CoordSys"));

    // assign each record to unique variable using cdmVariableHash()
    Map<Integer, VariableBag> vbHash = new HashMap<>(100);
    for (T gr : records) {
      int cdmHash = cdmVariableHash(gr, gdsHash);
      VariableBag bag = vbHash.get(cdmHash);
      if (bag == null) {
        bag = new VariableBag(gr, cdmHash);
        vbHash.put(cdmHash, bag);
      }
      bag.atomList.add(gr);
    }
    gribvars = new ArrayList<>(vbHash.values());
    Collections.sort(gribvars); // make it deterministic by sorting

    // create coordinates for each variable
    for (VariableBag vb : gribvars) {
      int timeUnit = getTimeUnit(vb);
      vb.timeUnit = Grib2Utils.getCalendarPeriod(timeUnit);
      vb.coordND = new CoordinateND<T>();

      boolean isTimeInterval = vb.first.getPDS().isTimeInterval();
      if (isDense) { // time is runtime X time coord
        vb.coordND.addBuilder(new CoordinateRuntime.Builder<T>());
        if (isTimeInterval)
          vb.coordND.addBuilder(new CoordinateTimeIntv.Builder(cust, timeUnit, vb.timeUnit, null)); // LOOK null refdate not ok
        else
          vb.coordND.addBuilder(new CoordinateTime.Builder( timeUnit, vb.timeUnit, null)); // LOOK null refdate not ok

      } else {  // time is kept as 2D coordinate, separate list of times for each runtime
        vb.coordND.addBuilder(new CoordinateRuntime.Builder());
        vb.coordND.addBuilder(new CoordinateTime2D.Builder(isTimeInterval, cust, vb.timeUnit, timeUnit));
      }

      int levelType = getLevelType(vb);
      VertCoord.VertUnit vertUnit = Grib2Utils.getLevelUnit(levelType);
      if (vertUnit.isVerticalCoordinate())
        vb.coordND.addBuilder( makeBuilder(Coordinate.Type.vert, levelType));

      // populate the coordinates with the inventory of data
      for (T gr : vb.atomList)
        vb.coordND.addRecord(gr);

      // done, build coordinates and sparse array indicating which records to use
      vb.coordND.finish(vb.atomList, info);
    }

    // make shared coordinates across variables
    CoordinateSharer<T> sharify = new CoordinateSharer<T>(isDense);
    for (VariableBag vb : gribvars) {
      sharify.addCoords(vb.coordND.getCoordinates());
    }
    sharify.finish();
    this.coords = sharify.getUnionCoords();

    int tot_used = 0;
    int tot_dups = 0;
    int total = 0;

    // redo the variables against the shared coordinates
    for (VariableBag vb : gribvars) {
      vb.coordND = sharify.reindex(vb.coordND);
      vb.coordIndex = sharify.reindex2shared(vb.coordND.getCoordinates());
      tot_used += vb.coordND.getSparseArray().countNotMissing();
      tot_dups += vb.coordND.getSparseArray().getNduplicates();
      total += vb.coordND.getSparseArray().getTotalSize();
     }

    counter.recordsUnique += tot_used;
    counter.dups += tot_dups;
    counter.vars += gribvars.size();
    counter.recordsTotal += total;
  }

  public void showInfo(Formatter f, GribTables tables) {
    //f.format("%nVariables%n");
    //f.format("%n  %3s %3s %3s%n", "time", "vert", "ens");
    Counter all = new Counter();

    for (VariableBag vb : gribvars) {
      f.format("Variable %s (%d)%n", tables.getVariableName(vb.first), vb.cdmHash);
      vb.coordND.showInfo(f, all);
      //f.format("  %3d %3d %3d %s records = %d density = %f hash=%d", vb.timeCoordIndex, vb.vertCoordIndex, vb.ensCoordIndex,
      //        vname, vb.atomList.size(), vb.recordMap.density(), vb.cdmHash);
      f.format("%n");
    }
    f.format("%n all= %s", all.show());
  }

  public class VariableBag implements Comparable<VariableBag> {
    public T first;
    public int cdmHash;

    public List<T> atomList = new ArrayList<>(100); // not sorted
    public CoordinateND<T> coordND;
    CalendarPeriod timeUnit;

    public List<Integer> coordIndex;
    long pos;
    int length;

    private VariableBag(T first, int cdmHash) {
      this.first = first;
      this.cdmHash = cdmHash;
    }

    @Override
    public int compareTo(VariableBag o) {
      return Grib2Utils.getVariableName(first).compareTo(Grib2Utils.getVariableName(o.first));
    }
  }

  /**
   * A hash code to group records into a CDM variable
   * Herein lies the semantics of a variable object identity.
   * Read it and weep.
   *
   * @param gr the Grib record
   * @param gdsHash can override the gdsHash
   * @return this record's hash code, identical hash means belongs to the same variable
   */
  public abstract int cdmVariableHash(T gr, int gdsHash);
  public abstract int getTimeUnit(VariableBag vb);
  public abstract int getLevelType(VariableBag vb);
  public abstract CoordinateBuilder makeBuilder(Coordinate.Type type, int levelType);

  private class Grib2 extends GribRectilyser<Grib2Record> {
    Grib2Customizer cust2;
    public Grib2(GribTables cust, List<Grib2Record> records, int gdsHash, Map<String, Boolean> pdsConfig) {
      super(cust, records, gdsHash, pdsConfig);
      this.cust2 = (Grib2Customizer) cust;
    }

    @Override
    public CoordinateBuilder makeBuilder(Coordinate.Type type, int levelType) {
      switch (type) {
        case vert: return new CoordinateVert.Builder(levelType);
      }
    }

    @Override
    public int getTimeUnit(VariableBag vb) {
      Grib2Pds pdsFirst = vb.first.getPDS();
      return cust2.convertTimeUnit(pdsFirst.getTimeUnit());
    }

    @Override
    public int getLevelType(VariableBag vb) {
      Grib2Pds pdsFirst = vb.first.getPDS();
      return pdsFirst.getLevelType1();
    }

    @Override
    /**
     * A hash code to group records into a CDM variable
     * Herein lies the semantics of a variable object identity.
     * Read it and weep.
     *
     * @param gr the Grib record
     * @param gdsHash can override the gdsHash
     * @return this record's hash code, identical hash means belongs to the same variable
     */
    public int cdmVariableHash(Grib2Record gr, int gdsHash) {
      Grib2SectionGridDefinition gdss = gr.getGDSsection();
      Grib2Pds pds2 = gr.getPDS();

      int result = 17;

      if (gdsHash == 0)
        result += result * 37 + gdss.getGDS().hashCode(); // the horizontal grid
      else
        result += result * 37 + gdsHash;

      result += result * 37 + gr.getDiscipline();
      result += result * 37 + pds2.getLevelType1();
      if (Grib2Utils.isLayer(pds2)) result += result * 37 + 1;

      result += result * 37 + pds2.getParameterCategory();
      result += result * 37 + pds2.getTemplateNumber();

      if (pds2.isTimeInterval()) {
        if (!intvMerge) {
          double size = 0;
          try {
            size = cust2.getForecastTimeIntervalSizeInHours(pds2); // LOOK using an Hour here, but will need to make this configurable
          } catch (Throwable t) {
            logger.error("bad", t);
            if (files != null)
              logger.error("Failed on file = "+files.get(gr.getFile()));
          }
          result += result * (int) (37 + (1000 * size)); // create new variable for each interval size - default not
        }
        result += result * 37 + pds2.getStatisticalProcessType(); // create new variable for each stat type
      }

      if (pds2.isSpatialInterval()) {
         result += result * 37 + pds2.getStatisticalProcessType(); // template 15
       }

       result += result * 37 + pds2.getParameterNumber();

      int ensDerivedType = -1;
      if (pds2.isEnsembleDerived()) {  // a derived ensemble must have a derivedForecastType
        Grib2Pds.PdsEnsembleDerived pdsDerived = (Grib2Pds.PdsEnsembleDerived) pds2;
        ensDerivedType = pdsDerived.getDerivedForecastType(); // derived type (table 4.7)
        result += result * 37 + ensDerivedType;

      } else if (pds2.isEnsemble()) {
        result += result * 37 + 1;
      }

      // each probability interval generates a separate variable; could be a dimension instead
      int probType = -1;
      if (pds2.isProbability()) {
        Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds2;
        probType = pdsProb.getProbabilityType();
        result += result * 37 + pdsProb.getProbabilityHashcode();
      }

      // if this uses any local tables, then we have to add the center id, and subcenter if present
      if ((pds2.getParameterCategory() > 191) || (pds2.getParameterNumber() > 191) || (pds2.getLevelType1() > 191)
              || (pds2.isTimeInterval() && pds2.getStatisticalProcessType() > 191)
              || (ensDerivedType > 191) || (probType > 191)) {
        Grib2SectionIdentification id = gr.getId();
        result += result * 37 + id.getCenter_id();
        if (id.getSubcenter_id() > 0)
          result += result * 37 + id.getSubcenter_id();
      }

      /* LOOK may be need to be variable specific, using
      <useGen><variable></useGen> ??
       */
      // only use the GenProcessType when "error" 2/8/2012 LOOK WTF ??
      int genType = pds2.getGenProcessType();
      if (useGenType && (genType == 6 || genType == 7)) {
        result += result * 37 + genType;
      }

      return result;
    }

  }
}
