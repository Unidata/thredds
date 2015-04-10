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
import ucar.nc2.grib.GribIndexCache;
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
class Grib2CollectionBuilder extends GribCollectionBuilder {
  // static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2CollectionBuilder.class);

  private FeatureCollectionConfig.GribConfig gribConfig;
  private Grib2Customizer cust;

  // LOOK prob name could be dcm.getCollectionName()
  public Grib2CollectionBuilder(String name, MCollection dcm, org.slf4j.Logger logger) {
    super(false, name, dcm, logger);

    FeatureCollectionConfig config = (FeatureCollectionConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_CONFIG);
    gribConfig = config.gribConfig;
  }

  // read all records in all files,
  // divide into groups based on GDS hash and runtime
  // each group has an arraylist of all records that belong to it.
  // for each group, run rectlizer to derive the coordinates and variables
  @Override
  public List<Grib2CollectionWriter.Group> makeGroups(List<MFile> allFiles, boolean singleRuntime, Formatter errlog) throws IOException {
    Map<GroupAndRuntime, Grib2CollectionWriter.Group> gdsMap = new HashMap<>();

    logger.debug("Grib2CollectionBuilder {}: makeGroups", name);
    int fileno = 0;
    Counter statsAll = new Counter(); // debugging

    logger.debug(" dcm={}", dcm);

    // place each record into its group
    int totalRecords = 0;
    try (CloseableIterator<MFile> iter = dcm.getFileIterator()) { // not sorted
      if (iter == null)
        return new ArrayList<>(); // empty

      while (iter.hasNext()) {
        MFile mfile = iter.next();
        Grib2Index index;

        try {
          if (GribIosp.debugGbxIndexOnly) {
             index = (Grib2Index) GribIndex.open(false, mfile);
           } else {
             // LOOK here is where gbx9 files get recreated
             index = (Grib2Index) GribIndex.readOrCreateIndexFromSingleFile(false, mfile, CollectionUpdateType.test, logger);
           }
          allFiles.add(mfile);  // add on success

        } catch (IOException ioe) {
          logger.error("Grib2CollectionBuilder " + name + " : reading/Creating gbx9 index for file " + mfile.getPath() + " failed", ioe);
          continue;
        }
        if (index == null) {
          logger.error("Grib2CollectionBuilder " + name + " : reading/Creating gbx9 index for file " + mfile.getPath() + " failed");
          continue;
        }
        int n = index.getNRecords();
        totalRecords += n;

        for (Grib2Record gr : index.getRecords()) { // we are using entire Grib2Record - memory limitations
          if (this.cust == null) {
            this.cust = Grib2Customizer.factory(gr);
            cust.setTimeUnitConverter(gribConfig.getTimeUnitConverter());
          }
          if (filterIntervals(gr, gribConfig.intvFilter)) {
            statsAll.filter++;
            continue; // skip
          }

          gr.setFile(fileno); // each record tracks which file it belongs to
          Grib2Gds gdsHashObject = gr.getGDS();  // use GDS to group records
          int gdsHash = gribConfig.convertGdsHash(gdsHashObject.hashCode());  // allow external config to muck with gdsHash. Why? because of error in encoding and we need exact hash matching
          if (0 == gdsHash) continue; // skip this group

          CalendarDate runtimeDate = gr.getReferenceDate();
          long runtime = singleRuntime ? runtimeDate.getMillis() : 0;  // seperate Groups for each runtime, if singleRuntime is true
          GroupAndRuntime gar = new GroupAndRuntime(gdsHashObject, runtime);
          Grib2CollectionWriter.Group g = gdsMap.get(gar);
          if (g == null) {
            g = new Grib2CollectionWriter.Group(gr.getGDSsection(), gdsHashObject, runtimeDate);
            gdsMap.put(gar, g);
          }
          g.records.add(gr);
          g.runtimes.add(runtimeDate.getMillis());
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
      Grib2Rectilyser rect = new Grib2Rectilyser(g.records, g.gdsHashObject);
      rect.make(gribConfig, stats, errlog);
      g.gribVars = rect.gribvars;
      g.coords = rect.coords;

      statsAll.add(stats);
    }

    // debugging and validation
    if (logger.isDebugEnabled()) logger.debug(statsAll.show());

    return groups;
  }

  // true means remove
  private boolean filterIntervals(Grib2Record gr, FeatureCollectionConfig.GribIntvFilter intvFilter) {
    // hack a whack - filter out records with unknown time units
    int timeUnit = gr.getPDS().getTimeUnit();
    if (Grib2Utils.getCalendarPeriod(timeUnit) == null) {
      logger.info("Skip record with unknown time Unit= {}", timeUnit);
      return true;
    }

    int[] intv = cust.getForecastTimeIntervalOffset(gr);
    if (intv == null) return false;   // not an interval
    int haveLength = intv[1] - intv[0];

        // discard zero length intervals (default)
    if (haveLength == 0 && (intvFilter == null || intvFilter.isZeroExcluded()))
      return true;

    // HACK
    if (intvFilter != null && intvFilter.hasFilter()) {
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
    for (Object g : groups) groups2.add((Grib2CollectionWriter.Group) g); // copy to change GribCollectionBuilder.Group ->  Grib2CollectionWriter.Group
    File indexFileInCache = GribIndexCache.getFileOrCache(indexFilepath);
    return writer.writeIndex(name, indexFileInCache, masterRuntime, groups2, files, type);
  }

  static class VariableBag implements Comparable<VariableBag> {
    public Grib2Record first;
    public Grib2Variable gv;

    public List<Grib2Record> atomList = new ArrayList<>(100); // not sorted
    public CoordinateND<Grib2Record> coordND;
    CalendarPeriod timeUnit;

    public List<Integer> coordIndex;
    long pos;
    int length;

    private VariableBag(Grib2Record first, Grib2Variable gv) {
      this.first = first;
      this.gv = gv;
    }

    @Override
    public int compareTo(VariableBag o) {
      return Grib2Utils.getVariableName(first).compareTo(Grib2Utils.getVariableName(o.first));
    }
  }

  private class Grib2Rectilyser {
    private final int gdsHashOverride;
    private final List<Grib2Record> records;
    private List<VariableBag> gribvars;
    private List<Coordinate> coords;

    Grib2Rectilyser(List<Grib2Record> records, Object gdsHashObject) {
      this.records = records;
      int gdsHash = gribConfig.convertGdsHash(gdsHashObject.hashCode());
      gdsHashOverride = (gdsHash == gdsHashObject.hashCode()) ? 0 : gdsHash;
    }

    public void make(FeatureCollectionConfig.GribConfig config, Counter counter, Formatter info) throws IOException {
      CalendarPeriod userTimeUnit = config.userTimeUnit;

      // assign each record to unique variable using cdmVariableHash()
      Map<Grib2Variable, VariableBag> vbHash = new HashMap<>(100);
      for (Grib2Record gr : records) {
        Grib2Variable gv;
        try {
          gv = new Grib2Variable(cust, gr, gdsHashOverride, gribConfig.intvMerge, gribConfig.useGenType);

        } catch (Throwable t) {
          logger.warn("Exception on record ", t);
          continue; // keep going
        }
        VariableBag bag = vbHash.get(gv);
        if (bag == null) {
          bag = new VariableBag(gr, gv);
          vbHash.put(gv, bag);
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
        CoordinateND.Builder<Grib2Record> coordNBuilder = new CoordinateND.Builder<>();

        boolean isTimeInterval = vb.first.getPDS().isTimeInterval();
        /* if (isDense) { // time is runtime X time coord
          coordNBuilder.addBuilder(new CoordinateRuntime.Builder2(vb.timeUnit));
          if (isTimeInterval)
            coordNBuilder.addBuilder(new CoordinateTimeIntv.Builder2(cust, code, vb.timeUnit, null)); // null refdate not ok
          else
            coordNBuilder.addBuilder(new CoordinateTime.Builder2(code, vb.timeUnit, null)); // null refdate not ok

        } else { */
         // time is kept as 2D coordinate, separate list of times for each runtime
         // coordNBuilder.addBuilder(new CoordinateRuntime.Builder2(vb.timeUnit));  LOOK removed - does it mess with SRC case ??
          CoordinateTime2D.Builder2 builder2D = new CoordinateTime2D.Builder2(isTimeInterval, cust, vb.timeUnit, code);
          coordNBuilder.addBuilder(builder2D);
          //coordNBuilder.addBuilder(builder2D.getTimeBuilder());
        //}

        if (vb.first.getPDS().isEnsemble())
          coordNBuilder.addBuilder(new CoordinateEns.Builder2(0));

        VertCoord.VertUnit vertUnit = cust.getVertUnit(pdsFirst.getLevelType1());
        if (vertUnit.isVerticalCoordinate())
          coordNBuilder.addBuilder(new CoordinateVert.Builder2(pdsFirst.getLevelType1(), cust.getVertUnit(pdsFirst.getLevelType1())));

        // populate the coordinates with the inventory of data
        for (Grib2Record gr : vb.atomList)
          coordNBuilder.addRecord(gr);

        // done, build coordinates and sparse array indicating which records to use
        vb.coordND = coordNBuilder.finish(vb.atomList, info);
      }

      // make shared coordinates across variables
      CoordinateSharer<Grib2Record> sharify = new CoordinateSharer<>(config.unionRuntimeCoord);
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
        vb.coordND = sharify.reindexCoordND(vb.coordND);              // LOOK consider orthogonalizing the Time2D
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

    public void showInfo(Formatter f, Grib2Customizer tables) {
      //f.format("%nVariables%n");
      //f.format("%n  %3s %3s %3s%n", "time", "vert", "ens");
      Counter all = new Counter();

      for (VariableBag vb : gribvars) {
        f.format("Variable %s (%d)%n", tables.getVariableName(vb.first), vb.gv.hashCode());
        vb.coordND.showInfo(f, all);
        //f.format("  %3d %3d %3d %s records = %d density = %f hash=%d", vb.timeCoordIndex, vb.vertCoordIndex, vb.ensCoordIndex,
        //        vname, vb.atomList.size(), vb.recordMap.density(), vb.cdmHash);
        f.format("%n");
      }
      f.format("%n all= %s", all.show());
    }
  }

}
