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
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import ucar.coord.*;
import ucar.nc2.grib.GribIndex;
import ucar.nc2.grib.grib1.*;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.CloseableIterator;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Builds indexes for collections of Grib1 files.
 * May create GC or PC
 *
 * @author John
 * @since 2/5/14
 */
public class Grib1CollectionBuilder extends GribCollectionBuilder {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1CollectionBuilder.class);

  private final boolean intvMerge;
  private final boolean useGenType;
  private final boolean useTableVersion;
  private final boolean useCenter;

  private FeatureCollectionConfig.GribConfig gribConfig;
  private Grib1Customizer cust;

  // LOOK prob name could be dcm.getCollectionName()
  public Grib1CollectionBuilder(String name, MCollection dcm, org.slf4j.Logger logger) {
    super(true, name, dcm, logger);

    FeatureCollectionConfig config = (FeatureCollectionConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_CONFIG);
    gribConfig = config.gribConfig;
    Map<String, Boolean> pdsConfig = config.gribConfig.pdsHash;
    useTableVersion = assignValue(pdsConfig, "useTableVersion", true);
    intvMerge = assignValue(pdsConfig, "intvMerge", true);
    useCenter = assignValue(pdsConfig, "useCenter", true);
    useGenType = assignValue(pdsConfig, "useGenType", false);
  }

  private boolean assignValue(Map<String, Boolean> pdsHash, String key, boolean value) {
    if (pdsHash != null) {
      Boolean b = pdsHash.get(key);
      if (b != null) value = b;
    }
    return value;
  }

  // read all records in all files,
  // divide into groups based on GDS hash
  // each group has an arraylist of all records that belong to it.
  // for each group, run rectlizer to derive the coordinates and variables
  @Override
  public List<Grib1CollectionWriter.Group> makeGroups(List<MFile> allFiles, Formatter errlog) throws IOException {
    Map<GroupAndRuntime, Grib1CollectionWriter.Group> gdsMap = new HashMap<>();

    logger.debug("Grib2CollectionBuilder {}: makeGroups", name);
    int fileno = 0;
    Counter statsAll = new Counter(); // debugging

    logger.debug(" dcm={}", dcm);
    FeatureCollectionConfig config = (FeatureCollectionConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_CONFIG);
    //Map<Integer, Integer> gdsConvert = config.gribConfig.gdsHash;
    Map<String, Boolean> pdsConvert = config.gribConfig.pdsHash;
    FeatureCollectionConfig.GribIntvFilter intvMap = config.gribConfig.intvFilter;

    // place each record into its group
    int totalRecords = 0;
    try (CloseableIterator<MFile> iter = dcm.getFileIterator()) { // not sorted
      while (iter.hasNext()) {
        MFile mfile = iter.next();
        Grib1Index index;
        try {                  // LOOK here is where gbx9 files get recreated; do not make collection index
          index = (Grib1Index) GribIndex.readOrCreateIndexFromSingleFile(true, false, mfile, config.gribConfig, CollectionUpdateType.test, logger);
          allFiles.add(mfile);  // add on success

        } catch (IOException ioe) {
          logger.error("Grib2CollectionBuilder " + name + " : reading/Creating gbx9 index for file " + mfile.getPath() + " failed", ioe);
          continue;
        }
        int n = index.getNRecords();
        totalRecords += n;

        for (Grib1Record gr : index.getRecords()) { // we are using entire Grib2Record - memory limitations
          if (this.cust == null) {
            cust = Grib1Customizer.factory(gr, null);
            cust.setTimeUnitConverter(config.gribConfig.getTimeUnitConverter());
          }
          if (intvMap != null && filterOut(gr, intvMap)) {
            statsAll.filter++;
            continue; // skip
          }

          gr.setFile(fileno); // each record tracks which file it belongs to
          int gdsHash = gr.getGDSsection().getGDS().hashCode();  // use GDS hash code to group records
          gdsHash = gribConfig.convertGdsHash(gdsHash);  // allow external config to muck with gdsHash. Why? because of error in encoding and we need exact hash matching
          if (0 == gdsHash)
            continue; // skip this group

          CalendarDate runtime = gr.getReferenceDate();
          GroupAndRuntime gar = new GroupAndRuntime(gdsHash, runtime.getMillis());
          Grib1CollectionWriter.Group g = gdsMap.get(gar);
          if (g == null) {
            g = new Grib1CollectionWriter.Group(gr.getGDSsection(), gdsHash, runtime);
            gdsMap.put(gar, g);
          }
          g.records.add(gr);
        }
        fileno++;
        statsAll.recordsTotal += index.getRecords().size();
      }
    }

    // rectilyze each group independently
    List<Grib1CollectionWriter.Group> groups = new ArrayList<>(gdsMap.values());
    for (Grib1CollectionWriter.Group g : groups) {
      Counter stats = new Counter(); // debugging
      Grib1Rectilyser rect = new Grib1Rectilyser(g.records, g.gdsHash, pdsConvert);
      rect.make(config.gribConfig, stats, errlog);
      g.gribVars = rect.gribvars;
      g.coords = rect.coords;

      statsAll.add(stats);

      // look for group name overrides
      //if (config.gribConfig.gdsNamer != null)
      //  g.nameOverride = config.gribConfig.gdsNamer.get(g.gdsHash);
    }

    // debugging and validation
    if (logger.isDebugEnabled()) logger.debug(statsAll.show());

    return groups;
  }

      // true means remove
  private boolean filterOut(Grib1Record gr, FeatureCollectionConfig.GribIntvFilter intvFilter) {
    Grib1SectionProductDefinition pdss = gr.getPDSsection();
    Grib1ParamTime ptime = pdss.getParamTime(cust);
    if (!ptime.isInterval()) return false;

    int[] intv = ptime.getInterval();
    if (intv == null) return false;
    int haveLength = intv[1] - intv[0];

    // HACK
    if (haveLength == 0 && intvFilter.isZeroExcluded()) {  // discard 0,0
      if ((intv[0] == 0) && (intv[1] == 0)) {
        //f.format(" FILTER INTV [0, 0] %s%n", gr);
        return true;
      }
      return false;

    } else if (intvFilter.hasFilter()) {
      int center = pdss.getCenter();
      int subcenter = pdss.getSubCenter();
      int version = pdss.getTableVersion();
      int param = pdss.getParameterNumber();
      int id = (center << 8) + (subcenter << 16) + (version << 24) + param;

      return !intvFilter.filterOk(id, haveLength, Integer.MIN_VALUE);
    }
    return false;
  }

  @Override
  protected boolean writeIndex(String name, String indexFilepath, CoordinateRuntime masterRuntime, List<? extends GribCollectionBuilder.Group> groups, List<MFile> files) throws IOException {
    Grib1CollectionWriter writer = new Grib1CollectionWriter(dcm, logger);
    List<Grib1CollectionWriter.Group> groups2 = new ArrayList<>();
    for (Object g : groups) groups2.add((Grib1CollectionWriter.Group) g);
    File indexFileInCache = GribCdmIndex.getFileInCache(indexFilepath);
    return writer.writeIndex(name, indexFileInCache, masterRuntime, groups2, files);
  }

  public static class VariableBag implements Comparable<VariableBag> {
    Grib1Record first;
    int cdmHash;

    public List<Grib1Record> atomList = new ArrayList<>(100); // not sorted
    public CoordinateND<Grib1Record> coordND;
    CalendarPeriod timeUnit;

    public List<Integer> coordIndex;
    long pos;
    int length;

    private VariableBag(Grib1Record first, int cdmHash) {
      this.first = first;
      this.cdmHash = cdmHash;
    }

    @Override
    public int compareTo(VariableBag o) {
      return Grib1Utils.extractParameterCode(first).compareTo(Grib1Utils.extractParameterCode(o.first));
    }
  }

  private class Grib1Rectilyser {

    private final int gdsHash;
    private final List<Grib1Record> records;
    private List<VariableBag> gribvars;
    private List<Coordinate> coords;

    Grib1Rectilyser(List<Grib1Record> records, int gdsHash, Map<String, Boolean> pdsConfig) {
      this.records = records;
      this.gdsHash = gdsHash;
    }

    public void make(FeatureCollectionConfig.GribConfig config, Counter counter, Formatter info) throws IOException {
      boolean isDense = "dense".equals(config.getParameter("CoordSys"));
      CalendarPeriod userTimeUnit = config.getUserTimeUnit();

      // assign each record to unique variable using cdmVariableHash()
      Map<Integer, VariableBag> vbHash = new HashMap<>(100);
      for (Grib1Record gr : records) {
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
        Grib1SectionProductDefinition pdss = vb.first.getPDSsection();
        Grib1ParamTime ptime = pdss.getParamTime(cust);

        int unit = cust.convertTimeUnit(pdss.getTimeUnit());
        vb.timeUnit = userTimeUnit == null ? Grib2Utils.getCalendarPeriod(unit) : userTimeUnit; // so can override the code // ok for GRIB1
        CoordinateND.Builder<Grib1Record> coordNBuilder = new CoordinateND.Builder<>();

        boolean isTimeInterval = ptime.isInterval();
        if (isDense) { // time is runtime X time coord  LOOK isDense not implemented
          coordNBuilder.addBuilder(new CoordinateRuntime.Builder1(vb.timeUnit));
          if (isTimeInterval)
            coordNBuilder.addBuilder(new CoordinateTimeIntv.Builder1(cust, unit, vb.timeUnit, null)); // LOOK null refdate not ok
          else
            coordNBuilder.addBuilder(new CoordinateTime.Builder1(cust, pdss.getTimeUnit(), vb.timeUnit, null)); // LOOK null refdate not ok

        } else {  // time is kept as 2D coordinate, separate list of times for each runtime
          coordNBuilder.addBuilder(new CoordinateRuntime.Builder1(vb.timeUnit));
          coordNBuilder.addBuilder(new CoordinateTime2D.Builder1(isTimeInterval, cust, vb.timeUnit, unit));
        }

        if (vb.first.getPDSsection().isEnsemble())
          coordNBuilder.addBuilder(new CoordinateEns.Builder1(cust, 0));

        if (cust.isVerticalCoordinate(pdss.getLevelType()))
          coordNBuilder.addBuilder(new CoordinateVert.Builder1(cust, pdss.getLevelType()));

        // populate the coordinates with the inventory of data
        for (Grib1Record gr : vb.atomList)
          coordNBuilder.addRecord(gr);

        // done, build coordinates and sparse array indicating which records to use
        vb.coordND = coordNBuilder.finish(vb.atomList, info);
      }

      // make shared coordinates across variables
      CoordinateSharer<Grib1Record> sharify = new CoordinateSharer<>(isDense);
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
        tot_dups += vb.coordND.getSparseArray().getNdups();
        total += vb.coordND.getSparseArray().getTotalSize();
       }

      counter.recordsUnique += tot_used;
      counter.dups += tot_dups;
      counter.vars += gribvars.size();
      counter.recordsTotal += total;
    }

    public void showInfo(Formatter f, Grib1Customizer tables) {
      //f.format("%nVariables%n");
      //f.format("%n  %3s %3s %3s%n", "time", "vert", "ens");
      Counter all = new Counter();

      for (VariableBag vb : gribvars) {
        f.format("Variable %s (%d)%n", Grib1Iosp.makeVariableName(cust, vb.first.getPDSsection()), vb.cdmHash);
        vb.coordND.showInfo(f, all);
        //f.format("  %3d %3d %3d %s records = %d density = %f hash=%d", vb.timeCoordIndex, vb.vertCoordIndex, vb.ensCoordIndex,
        //        vname, vb.atomList.size(), vb.recordMap.density(), vb.cdmHash);
        f.format("%n");
      }
      f.format("%n all= %s", all.show());
    }
  }

  private int cdmVariableHash(Grib1Record gr, int gdsHash) {
    return cdmVariableHash(cust, gr, gdsHash, useTableVersion, intvMerge, useCenter);
  }

  // use defaults
  public static int cdmVariableHash(Grib1Customizer cust, Grib1Record gr) {
    return cdmVariableHash(cust, gr, 0, true, true, true);
  }


  /**
   * A hash code to group records into a CDM variable
   * Herein lies the semantics of a variable object identity.
   * Read it and weep.
   *
   * @param gdsHash can override the gdsHash
   * @return this records hash code, to group like records into a variable
   */
  public static int cdmVariableHash(Grib1Customizer cust, Grib1Record gr, int gdsHash, boolean useTableVersion, boolean intvMerge, boolean useCenter) {
    int result = 17;

    Grib1SectionGridDefinition gdss = gr.getGDSsection();
    if (gdsHash == 0)
      result += result * 37 + gdss.getGDS().hashCode(); // the horizontal grid
    else
      result += result * 37 + gdsHash;

    Grib1SectionProductDefinition pdss = gr.getPDSsection();
    result += result * 37 + pdss.getLevelType();
    if (cust.isLayer(pdss.getLevelType())) result += result * 37 + 1;

    result += result * 37 + pdss.getParameterNumber();
    if (useTableVersion)  // LOOK must make a different variable name
      result += result * 37 + pdss.getTableVersion();

    Grib1ParamTime ptime = pdss.getParamTime(cust);
    if (ptime.isInterval()) {
      if (!intvMerge) result += result * 37 + ptime.getIntervalSize();  // create new variable for each interval size
      if (ptime.getStatType() != null) result += result * 37 + ptime.getStatType().ordinal(); // create new variable for each stat type
    }

    // LOOK maybe we should always add ??
    // if this uses any local tables, then we have to add the center id, and subcenter if present
    if (useCenter && pdss.getParameterNumber() > 127) {
      result += result * 37 + pdss.getCenter();
      if (pdss.getSubCenter() > 0)
        result += result * 37 + pdss.getSubCenter();
    }
    return result;
  }

}
