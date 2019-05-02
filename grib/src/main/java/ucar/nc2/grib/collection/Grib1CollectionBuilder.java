/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import javax.annotation.Nonnull;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionConfig.GribConfig;
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
import ucar.nc2.grib.grib1.*;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
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
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1CollectionBuilder.class);

  private final FeatureCollectionConfig.GribConfig gribConfig;
  private Grib1Customizer cust;

  Grib1CollectionBuilder(String name, MCollection dcm, org.slf4j.Logger logger) {
    super(true, name, dcm, logger);

    FeatureCollectionConfig config = (FeatureCollectionConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_CONFIG);
    gribConfig = config.gribConfig;
  }

  // read all records in all files,
  // divide into groups based on GDS hash and optionally the runtime
  // each group has an arraylist of all records that belong to it.
  // for each group, call rectilizer to derive the coordinates and variables
  @Override
  public List<Grib1CollectionWriter.Group> makeGroups(List<MFile> allFiles, boolean singleRuntime, Formatter errlog) throws IOException {
    Map<GroupAndRuntime, Grib1CollectionWriter.Group> gdsMap = new HashMap<>();

    logger.debug("Grib1CollectionBuilder {}: makeGroups", name);
    int fileno = 0;
    GribRecordStats statsAll = new GribRecordStats(); // debugging

    logger.debug(" dcm={}", dcm);

    // place each record into its group
    try (CloseableIterator<MFile> iter = dcm.getFileIterator()) { // not sorted
      if (iter == null)
        return new ArrayList<>(); // empty

      while (iter.hasNext()) {
        MFile mfile = iter.next();
        Grib1Index index;
        try {
          if (Grib.debugGbxIndexOnly) {
            index = (Grib1Index) GribIndex.open(true, mfile);
            if (index == null) continue;
          } else {
            // here is where gbx9 files get recreated
            index = (Grib1Index) GribIndex.readOrCreateIndexFromSingleFile(true, mfile, CollectionUpdateType.test, logger);
          }
          allFiles.add(mfile);  // add on success

        } catch (IOException ioe) {
          logger.error("Grib1CollectionBuilder " + name + " : reading/Creating gbx9 index for file " + mfile.getPath() + " failed", ioe);
          continue;
        }
        if (index == null) {
          logger.error("Grib1CollectionBuilder " + name + " : reading/Creating gbx9 index for file " + mfile.getPath() + " failed");
          continue;
        }

        for (Grib1Record gr : index.getRecords()) { // we are using entire Grib1Record - likely this is the memory bottleneck for how big a collection can handle
          if (this.cust == null) {
            cust = Grib1Customizer.factory(gr, null);
            cust.setTimeUnitConverter(gribConfig.getTimeUnitConverter());
          }
          if (filterIntervals(gr, gribConfig.intvFilter)) {
            statsAll.filter++;
            continue; // skip
          }

          gr.setFile(fileno); // each record tracks which file it belongs to
          Grib1Gds gds = gr.getGDS();  // use GDS to group records
          int hashCode = gribConfig.convertGdsHash(gds.hashCode());  // allow external config to muck with gdsHash. Why? because of error in encoding and we need exact hash matching
          if (0 == hashCode) continue; // skip this group
          //GdsHashObject gdsHashObject = new GdsHashObject(gr.getGDS(), hashOverride);

          CalendarDate runtimeDate = gr.getReferenceDate();
          long runtime = singleRuntime ? runtimeDate.getMillis() : 0;  // seperate Groups for each runtime, if singleRuntime is true
          GroupAndRuntime gar = new GroupAndRuntime(hashCode, runtime);
          Grib1CollectionWriter.Group g = gdsMap.get(gar);
          if (g == null) {
            g = new Grib1CollectionWriter.Group(gr.getGDSsection(), hashCode, runtimeDate);
            gdsMap.put(gar, g);
          }
          g.records.add(gr);
          g.runtimes.add(runtimeDate.getMillis());
        }
        fileno++;
        statsAll.recordsTotal += index.getRecords().size();
      }
    }

    // rectilyze each group independently
    List<Grib1CollectionWriter.Group> groups = new ArrayList<>(gdsMap.values());
    for (Grib1CollectionWriter.Group g : groups) {
      GribRecordStats stats = new GribRecordStats(); // debugging
      Grib1Rectilyser rect = new Grib1Rectilyser(g.records, g.hashCode);
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
  private boolean filterIntervals(Grib1Record gr, FeatureCollectionConfig.GribIntvFilter intvFilter) {
    Grib1SectionProductDefinition pdss = gr.getPDSsection();
    Grib1ParamTime ptime = gr.getParamTime(cust);
    if (!ptime.isInterval()) return false;

    int[] intv = ptime.getInterval();
    if (intv == null) return false;
    int haveLength = intv[1] - intv[0];

    // keep zero length intervals unless configured otherwise
    if (haveLength == 0 && intvFilter != null && intvFilter.isZeroExcluded())
      return true;

    if (intvFilter != null && intvFilter.hasFilter()) {
      int center = pdss.getCenter();
      int subcenter = pdss.getSubCenter();
      int version = pdss.getTableVersion();
      int param = pdss.getParameterNumber();
      int id = (center << 8) + (subcenter << 16) + (version << 24) + param;

      return intvFilter.filter(id, intv[0], intv[1], Integer.MIN_VALUE);
    }

    return false;
  }

  @Override
  protected boolean writeIndex(String name, String indexFilepath, CoordinateRuntime masterRuntime,
                   List<? extends GribCollectionBuilder.Group> groups, List<MFile> files, CalendarDateRange dateRange) throws IOException {
    Grib1CollectionWriter writer = new Grib1CollectionWriter(dcm, logger);
    List<Grib1CollectionWriter.Group> groups2 = new ArrayList<>();
    for (Object g : groups) groups2.add((Grib1CollectionWriter.Group) g);  // why copy ?
    File indexFileInCache = GribIndexCache.getFileOrCache(indexFilepath);
    return writer.writeIndex(name, indexFileInCache, masterRuntime, groups2, files, type, dateRange);
  }

  public static class VariableBag implements Comparable<VariableBag> {
    final Grib1Record first;
    final Grib1Variable gv;

    final List<Grib1Record> atomList = new ArrayList<>(100); // not sorted
    CoordinateND<Grib1Record> coordND;
    CalendarPeriod timeUnit;

    List<Integer> coordIndex; // index into List<Coordinate>
    long pos;
    int length;

    private VariableBag(Grib1Record first, Grib1Variable gv) {
      this.first = first;
      this.gv = gv;
    }

    @Override
    public int compareTo(@Nonnull VariableBag o) {
      return Grib1Utils.extractParameterCode(first).compareTo(Grib1Utils.extractParameterCode(o.first));
    }

    @Override
    public String toString() {
      return "VariableBag{" +
              ", variable=" + gv.makeVariableName(new GribConfig()) +
              ", coordND=" + coordND +
              ", timeUnit=" + timeUnit +
              ", coordIndex=" + coordIndex +
              ", pos=" + pos +
              ", length=" + length +
              '}';
    }
  }

  // for a single group, create multidimensional (rectangular) variables
  private class Grib1Rectilyser {
    private final int hashCode;
    private final List<Grib1Record> records;
    private List<VariableBag> gribvars;
    private List<Coordinate> coords;

    Grib1Rectilyser(List<Grib1Record> records, int hashCode) {
      this.records = records;
      this.hashCode = hashCode;
      //int gdsHash = gribConfig.convertGdsHash(gdsHashObject.hashCode());
      //gdsHashOverride = (gdsHash == gdsHashObject.hashCode()) ? 0 : gdsHash;
    }

    public void make(FeatureCollectionConfig.GribConfig config, GribRecordStats counter, Formatter info) {
      CalendarPeriod userTimeUnit = config.userTimeUnit;

      // assign each record to unique variable using cdmVariableHash()
      Map<Grib1Variable, VariableBag> vbHash = new HashMap<>(100);
      for (Grib1Record gr : records) {
        Grib1Variable cdmHash;
        try {
          cdmHash =  new Grib1Variable(cust, gr, hashCode, gribConfig.useTableVersion, gribConfig.intvMerge, gribConfig.useCenter);
        } catch (Throwable t) {
          logger.warn("Exception on record ", t);
          continue; // keep going
        }
        VariableBag bag = vbHash.get(cdmHash);
        if (bag == null) {
          bag = new VariableBag(gr, cdmHash);
          vbHash.put(cdmHash, bag);
        }
        bag.atomList.add(gr);
      }
      gribvars = new ArrayList<>(vbHash.values());
      Collections.sort(gribvars); // make it deterministic by sorting

      // create dense coordinates for each variable
      for (VariableBag vb : gribvars) {
        Grib1SectionProductDefinition pdss = vb.first.getPDSsection();
        Grib1ParamTime ptime = vb.first.getParamTime(cust);

        int unit = cust.convertTimeUnit(pdss.getTimeUnit());
        vb.timeUnit = userTimeUnit == null ? Grib2Utils.getCalendarPeriod(unit) : userTimeUnit; // so can override the code // ok for GRIB1
        CoordinateND.Builder<Grib1Record> coordNBuilder = new CoordinateND.Builder<>();

        boolean isTimeInterval = ptime.isInterval();
        /* if (isDense) { // time is runtime X time coord
          coordNBuilder.addBuilder(new CoordinateRuntime.Builder1(vb.timeUnit));
          if (isTimeInterval)
            coordNBuilder.addBuilder(new CoordinateTimeIntv.Builder1(cust, unit, vb.timeUnit, null)); // null refdate not ok
          else
            coordNBuilder.addBuilder(new CoordinateTime.Builder1(cust, pdss.getTimeUnit(), vb.timeUnit, null)); // null refdate not ok

        } else {  */
          // time is kept as 2D coordinate, separate list of times for each runtime
        CoordinateTime2D.Builder1 builder2D = new CoordinateTime2D.Builder1(isTimeInterval, cust, vb.timeUnit, unit);
        coordNBuilder.addBuilder(builder2D);
        //}

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
      CoordinateSharer<Grib1Record> sharify = new CoordinateSharer<>(config.unionRuntimeCoord, logger);
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

      // track stats
      counter.recordsUnique += tot_used;
      counter.dups += tot_dups;
      counter.vars += gribvars.size();
      counter.recordsTotal += total;
    }

    // debugging only
    public void showInfo(Formatter f, Grib1Customizer cust1) {
      //f.format("%nVariables%n");
      //f.format("%n  %3s %3s %3s%n", "time", "vert", "ens");
      GribRecordStats all = new GribRecordStats();

      for (VariableBag vb : gribvars) {
        f.format("Variable %s (%d)%n", Grib1Iosp.makeVariableName(cust, gribConfig, vb.first.getPDSsection()), vb.hashCode());
        vb.coordND.showInfo(f, all);
        //f.format("  %3d %3d %3d %s records = %d density = %f hash=%d", vb.timeCoordIndex, vb.vertCoordIndex, vb.ensCoordIndex,
        //        vname, vb.atomList.size(), vb.recordMap.density(), vb.cdmHash);
        f.format("%n");
      }
      f.format("%n all= %s", all.show());
    }
  }

}
