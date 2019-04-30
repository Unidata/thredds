/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import javax.annotation.Nonnull;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import ucar.nc2.grib.GribIndex;
import ucar.nc2.grib.GribIndexCache;
import ucar.nc2.grib.coord.Coordinate;
import ucar.nc2.grib.coord.CoordinateEns;
import ucar.nc2.grib.coord.CoordinateND;
import ucar.nc2.grib.coord.CoordinateRuntime;
import ucar.nc2.grib.coord.CoordinateSharer;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.nc2.grib.coord.CoordinateVert;
import ucar.nc2.grib.coord.GribRecordStats;
import ucar.nc2.grib.coord.VertCoordType;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.grib2.table.Grib2Tables;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
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
  private final FeatureCollectionConfig.GribConfig gribConfig;
  private Grib2Tables cust;

  // LOOK prob name could be dcm.getCollectionName()
  Grib2CollectionBuilder(String name, MCollection dcm, org.slf4j.Logger logger) {
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
    GribRecordStats statsAll = new GribRecordStats(); // debugging

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
          if (Grib.debugGbxIndexOnly) {
             index = (Grib2Index) GribIndex.open(false, mfile);
           } else {
             // this is where gbx9 files get recreated
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
            this.cust = Grib2Tables.factory(gr);
            cust.setTimeUnitConverter(gribConfig.getTimeUnitConverter());
          }
          if (filterIntervals(gr, gribConfig.intvFilter)) {
            statsAll.filter++;
            continue; // skip
          }

          gr.setFile(fileno); // each record tracks which file it belongs to
          Grib2Gds gds = gr.getGDS();  // use GDS to group records
          int hashCode = gribConfig.convertGdsHash(gds.hashCode());  // allow external config to muck with gdsHash. Why? because of error in encoding and we need exact hash matching
          if (0 == hashCode) continue; // skip this group
          // GdsHashObject gdsHashObject = new GdsHashObject(gr.getGDS(), hashCode);

          CalendarDate runtimeDate = gr.getReferenceDate();
          long runtime = singleRuntime ? runtimeDate.getMillis() : 0;  // seperate Groups for each runtime, if singleRuntime is true
          GroupAndRuntime gar = new GroupAndRuntime(hashCode, runtime);
          Grib2CollectionWriter.Group g = gdsMap.get(gar);
          if (g == null) {
            g = new Grib2CollectionWriter.Group(gr.getGDSsection(), hashCode, runtimeDate);
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
      GribRecordStats stats = new GribRecordStats(); // debugging
      Grib2Rectilyser rect = new Grib2Rectilyser(g.records, g.hashCode);
      rect.make(gribConfig, stats, errlog);
      g.gribVars = rect.gribvars;
      g.coords = rect.coords;

      statsAll.add(stats);
    }

    // debugging and validation
    if (logger.isDebugEnabled()) logger.debug(statsAll.show());

    return groups;
  }

  // true means discard
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

    // discard zero length intervals if so configured
    if (haveLength == 0 && intvFilter != null && intvFilter.isZeroExcluded())
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

      // true means discard
      return intvFilter.filter(id, intv[0], intv[1], prob);
    }

    return false;
  }

  @Override
  protected boolean writeIndex(String name, String indexFilepath, CoordinateRuntime masterRuntime,
                    List<? extends GribCollectionBuilder.Group> groups, List<MFile> files, CalendarDateRange dateRange) throws IOException {
    Grib2CollectionWriter writer = new Grib2CollectionWriter(dcm, logger);
    List<Grib2CollectionWriter.Group> groups2 = new ArrayList<>();
    for (Object g : groups) groups2.add((Grib2CollectionWriter.Group) g); // copy to change GribCollectionBuilder.Group ->  Grib2CollectionWriter.Group
    File indexFileInCache = GribIndexCache.getFileOrCache(indexFilepath);
    return writer.writeIndex(name, indexFileInCache, masterRuntime, groups2, files, type, dateRange);
  }

  static class VariableBag implements Comparable<VariableBag> {
    public final Grib2Record first;
    public final Grib2Variable gv;

    final List<Grib2Record> atomList = new ArrayList<>(100); // not sorted
    CoordinateND<Grib2Record> coordND;
    CalendarPeriod timeUnit;

    List<Integer> coordIndex;
    long pos;
    int length;

    private VariableBag(Grib2Record first, Grib2Variable gv) {
      this.first = first;
      this.gv = gv;
    }

    @Override
    public int compareTo(@Nonnull VariableBag o) {
      return Grib2Utils.getVariableName(first).compareTo(Grib2Utils.getVariableName(o.first));
    }
  }

  private class Grib2Rectilyser {
    private final int hashCode;
    private final List<Grib2Record> records;
    private List<VariableBag> gribvars;
    private List<Coordinate> coords;

    Grib2Rectilyser(List<Grib2Record> records, int hashCode) {
      this.records = records;
      this.hashCode = hashCode;
      /* int gdsHash = gribConfig.convertGdsHash(gdsHashObject.hashCode());
      gdsHashOverride = (gdsHash == gdsHashObject.hashCode()) ? 0 : gdsHash; */
    }

    public void make(FeatureCollectionConfig.GribConfig config, GribRecordStats counter, Formatter info) {
      CalendarPeriod userTimeUnit = config.userTimeUnit;

      // assign each record to unique variable using cdmVariableHash()
      Map<Grib2Variable, VariableBag> vbHash = new HashMap<>(100);
      for (Grib2Record gr : records) {
        Grib2Variable gv;
        try {
          gv = new Grib2Variable(cust, gr, hashCode, gribConfig.intvMerge, gribConfig.useGenType);

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
          CoordinateTime2D.Builder2 builder2D = new CoordinateTime2D.Builder2(isTimeInterval, cust, vb.timeUnit, code);
          coordNBuilder.addBuilder(builder2D);

        if (vb.first.getPDS().isEnsemble())
          coordNBuilder.addBuilder(new CoordinateEns.Builder2(0));

        VertCoordType vertUnit = cust.getVertUnit(pdsFirst.getLevelType1());
        if (vertUnit.isVerticalCoordinate())
          coordNBuilder.addBuilder(new CoordinateVert.Builder2(pdsFirst.getLevelType1(), cust.getVertUnit(pdsFirst.getLevelType1())));

        // populate the coordinates with the inventory of data
        for (Grib2Record gr : vb.atomList)
          coordNBuilder.addRecord(gr);

        // done, build coordinates and sparse array indicating which records to use
        vb.coordND = coordNBuilder.finish(vb.atomList, info);
      }

      // make shared coordinates across variables
      CoordinateSharer<Grib2Record> sharify = new CoordinateSharer<>(config.unionRuntimeCoord, logger);
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
        vb.coordND = sharify.reindexCoordND(vb.coordND);
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

    public void showInfo(Formatter f, Grib2Tables tables) {
      GribRecordStats all = new GribRecordStats();

      for (VariableBag vb : gribvars) {
        f.format("Variable %s (%d)%n", tables.getVariableName(vb.first), vb.gv.hashCode());
        vb.coordND.showInfo(f, all);
        f.format("%n");
      }
      f.format("%n all= %s", all.show());
    }
  }

}
