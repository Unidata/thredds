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
import ucar.nc2.grib.VertCoord;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.CloseableIterator;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Builds indexes for collections of Grib2 files.
 * May create GC or PC
 *
 * @author John
 * @since 2/5/14
 */
public class Grib2CollectionBuilder extends GribCollectionBuilder {
  // static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2CollectionBuilder.class);

  private final boolean intvMerge;
  private final boolean useGenType;

  private FeatureCollectionConfig.GribConfig gribConfig;
  private Grib2Customizer cust;

  // LOOK prob name could be dcm.getCollectionName()
  public Grib2CollectionBuilder(String name, MCollection dcm, org.slf4j.Logger logger) {
    super(false, name, dcm, logger);

    FeatureCollectionConfig config = (FeatureCollectionConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_CONFIG);
    gribConfig = config.gribConfig;
    Map<String, Boolean> pdsConfig = gribConfig.pdsHash;
    intvMerge = assignValue(pdsConfig, "intvMerge", true);
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
  public List<Grib2CollectionWriter.Group> makeGroups(List<MFile> allFiles, Formatter errlog) throws IOException {
    Map<GroupAndRuntime, Grib2CollectionWriter.Group> gdsMap = new HashMap<>();

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
        Grib2Index index;
        try {                  // LOOK here is where gbx9 files get recreated; do not make collection index
          index = (Grib2Index) GribIndex.readOrCreateIndexFromSingleFile(false, false, mfile, config.gribConfig, CollectionUpdateType.test, logger);
          allFiles.add(mfile);  // add on success

        } catch (IOException ioe) {
          logger.error("Grib2CollectionBuilder " + name + " : reading/Creating gbx9 index for file " + mfile.getPath() + " failed", ioe);
          continue;
        }
        int n = index.getNRecords();
        totalRecords += n;

        for (Grib2Record gr : index.getRecords()) { // we are using entire Grib2Record - memory limitations
          if (this.cust == null) {
            this.cust = Grib2Customizer.factory(gr);
            cust.setTimeUnitConverter(config.gribConfig.getTimeUnitConverter());
          }
          if (filterOut(gr, intvMap)) {
            statsAll.filter++;
            continue; // skip
          }

          gr.setFile(fileno); // each record tracks which file it belongs to
          int gdsHash = gr.getGDSsection().getGDS().hashCode();  // use GDS hash code to group records
          gdsHash = gribConfig.convertGdsHash(gdsHash);  // allow external config to muck with gdsHash. Why? because of error in encoding and we need exact hash matching
          if (0 == gdsHash) continue; // skip this group

          CalendarDate runtime = gr.getReferenceDate();
          GroupAndRuntime gar = new GroupAndRuntime(gdsHash, runtime.getMillis());
          Grib2CollectionWriter.Group g = gdsMap.get(gar);
          if (g == null) {
            g = new Grib2CollectionWriter.Group(gr.getGDSsection(), gdsHash, runtime);
            gdsMap.put(gar, g);
          }
          g.records.add(gr);
        }
        fileno++;
        statsAll.recordsTotal += index.getRecords().size();
      }
    }

    if (totalRecords == 0) {
      logger.warn("No records found in files. Check Grib1/Grib2 for collection {}. If wrong, delete gbx9.", name);
      throw new IllegalStateException("No records found in dataset "+name);
    }

    // rectilyze each group independently
    List<Grib2CollectionWriter.Group> groups = new ArrayList<>(gdsMap.values());
    for (Grib2CollectionWriter.Group g : groups) {
      Counter stats = new Counter(); // debugging
      Grib2Rectilyser rect = new Grib2Rectilyser(g.records, g.gdsHash, pdsConvert);
      rect.make(config.gribConfig, stats, errlog);
      g.gribVars = rect.gribvars;
      g.coords = rect.coords;

      statsAll.add(stats);

      // look for group name overrides
      if (config.gribConfig.gdsNamer != null)
        g.nameOverride = config.gribConfig.gdsNamer.get(g.gdsHash);
    }

    // debugging and validation
    if (logger.isDebugEnabled()) logger.debug(statsAll.show());

    return groups;
  }

  // true means remove
  private boolean filterOut(Grib2Record gr, FeatureCollectionConfig.GribIntvFilter intvFilter) {
    // hack a whack - filter out records with unknown time units
    int timeUnit = gr.getPDS().getTimeUnit();
    if (Grib2Utils.getCalendarPeriod(timeUnit) == null) {
      logger.info("Skip record with unknown time Unit= {}", timeUnit);
      return true;
    }

    if (intvFilter == null) return false;

    int[] intv = cust.getForecastTimeIntervalOffset(gr);
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
      int discipline = gr.getIs().getDiscipline();
      Grib2Pds pds = gr.getPDS();
      int category = pds.getParameterCategory();
      int number = pds.getParameterNumber();
      int id = (discipline << 16) + (category << 8) + number;

      int prob = Integer.MIN_VALUE;
      if (pds.isProbability()) {
        Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds;
        prob = (int) (1000 * pdsProb.getProbabilityUpperLimit());
      }

      // true means use, false means discard
      return !intvFilter.filterOk(id, haveLength, prob);
    }
    return false;
  }




  @Override
  protected boolean writeIndex(String name, String indexFilepath, CoordinateRuntime masterRuntime, List<? extends GribCollectionBuilder.Group> groups, List<MFile> files) throws IOException {
    Grib2CollectionWriter writer = new Grib2CollectionWriter(dcm, logger);
    List<Grib2CollectionWriter.Group> groups2 = new ArrayList<>();
    for (Object g : groups) groups2.add((Grib2CollectionWriter.Group) g);
    File indexFileInCache = GribCollection.getFileInCache(indexFilepath);
    return writer.writeIndex(name, indexFileInCache, masterRuntime, groups2, files);
  }

  class VariableBag implements Comparable<VariableBag> {
    public Grib2Record first;
    public int cdmHash;

    public List<Grib2Record> atomList = new ArrayList<>(100); // not sorted
    public CoordinateND<Grib2Record> coordND;
    CalendarPeriod timeUnit;

    public List<Integer> coordIndex;
    long pos;
    int length;

    private VariableBag(Grib2Record first, int cdmHash) {
      this.first = first;
      this.cdmHash = cdmHash;
    }

    @Override
    public int compareTo(VariableBag o) {
      return Grib2Utils.getVariableName(first).compareTo(Grib2Utils.getVariableName(o.first));
    }
  }

  private class Grib2Rectilyser {
    private final int gdsHash;
    private final List<Grib2Record> records;
    private List<VariableBag> gribvars;
    private List<Coordinate> coords;

    Grib2Rectilyser(List<Grib2Record> records, int gdsHash, Map<String, Boolean> pdsConfig) {
      this.records = records;
      this.gdsHash = gdsHash;
    }

    public void make(FeatureCollectionConfig.GribConfig config, Counter counter, Formatter info) throws IOException {
      boolean isDense = "dense".equals(config.getParameter("CoordSys"));
      CalendarPeriod userTimeUnit = config.getUserTimeUnit();

      // assign each record to unique variable using cdmVariableHash()
      Map<Integer, VariableBag> vbHash = new HashMap<>(100);
      for (Grib2Record gr : records) {
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
        Grib2Pds pdsFirst = vb.first.getPDS();
        int code = cust.convertTimeUnit(pdsFirst.getTimeUnit());
        vb.timeUnit = userTimeUnit == null ? Grib2Utils.getCalendarPeriod(code) : userTimeUnit;   // so can override the code in config  "timeUnit"
        vb.coordND = new CoordinateND<>();

        boolean isTimeInterval = vb.first.getPDS().isTimeInterval();
        if (isDense) { // time is runtime X time coord
          vb.coordND.addBuilder(new CoordinateRuntime.Builder2(vb.timeUnit));
          if (isTimeInterval)
            vb.coordND.addBuilder(new CoordinateTimeIntv.Builder2(cust, code, vb.timeUnit, null)); // LOOK null refdate not ok
          else
            vb.coordND.addBuilder(new CoordinateTime.Builder2(code, vb.timeUnit, null)); // LOOK null refdate not ok

        } else {  // time is kept as 2D coordinate, separate list of times for each runtime
          vb.coordND.addBuilder(new CoordinateRuntime.Builder2(vb.timeUnit));
          vb.coordND.addBuilder(new CoordinateTime2D.Builder2(isTimeInterval, cust, vb.timeUnit, code));
        }

        if (vb.first.getPDS().isEnsemble())
          vb.coordND.addBuilder(new CoordinateEns.Builder2(0));

        VertCoord.VertUnit vertUnit = Grib2Utils.getLevelUnit(pdsFirst.getLevelType1());
        if (vertUnit.isVerticalCoordinate())
          vb.coordND.addBuilder(new CoordinateVert.Builder2(pdsFirst.getLevelType1()));

        // populate the coordinates with the inventory of data
        for (Grib2Record gr : vb.atomList)
          vb.coordND.addRecord(gr);

        // done, build coordinates and sparse array indicating which records to use
        vb.coordND.finish(vb.atomList, info);
      }

      // make shared coordinates across variables
      CoordinateSharer<Grib2Record> sharify = new CoordinateSharer<>(isDense);
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

    public void showInfo(Formatter f, Grib2Customizer tables) {
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
  }

  private int cdmVariableHash(Grib2Record gr, int gdsHash) {
    return cdmVariableHash(cust, gr, gdsHash, intvMerge, useGenType, logger);
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
  public static int cdmVariableHash(Grib2Customizer cust, Grib2Record gr, int gdsHash, boolean intvMerge, boolean useGenType, org.slf4j.Logger logger) {
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
          size = cust.getForecastTimeIntervalSizeInHours(pds2); // LOOK using an Hour here, but will need to make this configurable
        } catch (Throwable t) {
          if (logger != null) logger.error("Failed on file = "+gr.getFile(), t);
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

    // always use the GenProcessType when "error" (6 or 7) 2/8/2012
    int genType = pds2.getGenProcessType();
    if (genType == 6 || genType == 7 || (useGenType && genType > 0)) {
      result += result * 37 + genType;
    }

    int addHash = cust.addVariableHash(gr);
    if (addHash != 0)
      result += result * 37 + addHash;

    return result;
  }

}
