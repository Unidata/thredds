/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import com.google.protobuf.ByteString;
import javax.annotation.Nullable;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import thredds.inventory.partition.PartitionManager;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.GribIndexCache;
import ucar.nc2.grib.coord.CalendarDateFactory;
import ucar.nc2.grib.coord.Coordinate;
import ucar.nc2.grib.coord.CoordinateBuilder;
import ucar.nc2.grib.coord.CoordinateEns;
import ucar.nc2.grib.coord.CoordinatePartitionUnionizer;
import ucar.nc2.grib.coord.CoordinateRuntime;
import ucar.nc2.grib.coord.CoordinateSharer;
import ucar.nc2.grib.coord.CoordinateSharerBest;
import ucar.nc2.grib.coord.CoordinateTime;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.nc2.grib.coord.CoordinateTimeAbstract;
import ucar.nc2.grib.coord.CoordinateTimeIntv;
import ucar.nc2.grib.coord.CoordinateVert;
import ucar.nc2.stream.NcStream;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * superclass to build Grib1/2 PartitionCollections
 *
 * @author caron
 * @since 2/21/14
 */
abstract class GribPartitionBuilder {

  private final PartitionManager partitionManager; // defines the partition
  protected final String name;            // collection name
  protected final org.slf4j.Logger logger;
  protected PartitionCollectionMutable result;  // build this object

  GribPartitionBuilder(String name, PartitionManager tpc, org.slf4j.Logger logger) {
    this.name = name;
    this.partitionManager = tpc;
    this.logger = logger;
  }

  boolean updateNeeded(CollectionUpdateType ff) throws IOException {
    if (ff == CollectionUpdateType.never) return false;
    if (ff == CollectionUpdateType.always) return true;

    File collectionIndexFile = GribIndexCache.getExistingFileOrCache(partitionManager.getIndexFilename(GribCdmIndex.NCX_SUFFIX));
    if (collectionIndexFile == null) return true;

    if (ff == CollectionUpdateType.nocheck) return false;

    // now check children
    return needsUpdate(ff, collectionIndexFile);
  }

  // LOOK need an option to only scan latest last partition or something
  private boolean needsUpdate(CollectionUpdateType ff, File collectionIndexFile) throws IOException {
    long collectionLastModified = collectionIndexFile.lastModified();
    Set<String> newFileSet = new HashSet<>();
    for (MCollection dcm : partitionManager.makePartitions(CollectionUpdateType.test)) {
      String partitionIndexFilename = StringUtil2.replace(dcm.getIndexFilename(GribCdmIndex.NCX_SUFFIX), '\\', "/");
      File partitionIndexFile = GribIndexCache.getExistingFileOrCache(partitionIndexFilename);
      if (partitionIndexFile == null)                                 // make sure each partition has an index
        return true;
      if (collectionLastModified < partitionIndexFile.lastModified())  // and the partition index is earlier than the collection index
        return true;
      newFileSet.add(partitionIndexFilename);
    }

    if (ff == CollectionUpdateType.testIndexOnly) return false;

    // now see if any files were deleted
    GribCdmIndex reader = new GribCdmIndex(logger);
    List<MFile> oldFiles = new ArrayList<>();
    reader.readMFiles(collectionIndexFile.toPath(), oldFiles);
    Set<String> oldFileSet = new HashSet<>();
    for (MFile oldFile : oldFiles) {
      if (!newFileSet.contains(oldFile.getPath()))
        return true;  // got deleted - must recreate the index
      oldFileSet.add(oldFile.getPath());
    }

    // now see if any files were added
    for (String newFilename : newFileSet) {
      if (!oldFileSet.contains(newFilename))
        return true;  // got added - must recreate the index
    }

    return false;
  }

  ///////////////////////////////////////////////////
  // build the index

  // return true if changed, exception on failure
  boolean createPartitionedIndex(CollectionUpdateType forcePartition, Formatter errlog) throws IOException {
    if (errlog == null) errlog = new Formatter(); // info will be discarded

    // create partitions from the partitionManager
    for (MCollection dcmp : partitionManager.makePartitions(forcePartition)) {
      dcmp.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, partitionManager.getAuxInfo(FeatureCollectionConfig.AUX_CONFIG));
      result.addPartition(dcmp);
    }
    result.sortPartitions(); // after this the partition list is immutable

    // choose the "canonical" partition, aka prototype
    // only used in copyInfo
    int n = result.getPartitionSize();
    if (n == 0) {
      errlog.format("ERR Nothing in this partition = %s%n", result.showLocation());
      throw new IllegalStateException("Nothing in this partition =" + result.showLocation());
    }
    int idx = partitionManager.getProtoIndex(n);
    PartitionCollectionMutable.Partition canon = result.getPartition(idx);
    logger.debug("     Using canonical partition {}", canon.getDcm().getCollectionName());

    try (GribCollectionMutable gc = canon.makeGribCollection()) {  // LOOK open/close canonical partition
      if (gc == null)
        throw new IllegalStateException("canon.makeGribCollection failed on =" + result.showLocation() + " " + canon.getName() + "; errs=" + errlog);

      // copy info from canon gribCollection to result partitionCollection
      result.copyInfo(gc);
      result.isPartitionOfPartitions = (gc instanceof PartitionCollectionMutable);

      result.dateRange = gc.dateRange;
    }

    // check consistency across vert and ens coords
    // create partitioned variables
    // partition index is used - do not resort partitions
    GribCollectionMutable.Dataset ds2D = makeDataset2D(errlog);
    if (ds2D == null) {
      errlog.format(" ERR makeDataset2D failed, index not written on %s%n", result.showLocation());
      throw new IllegalStateException("makeDataset2D failed, index not written on =" + result.showLocation() + "; errs=" + errlog);
    }

    // Make Best for a TwoD
    if (ds2D.gctype == GribCollectionImmutable.Type.TwoD)
      makeDatasetBest(ds2D, false);
    //else if (ds2D.gctype == GribCollectionImmutable.Type.MRUTC)
    //  makeTime2runtime(ds2D, false);

    // ready to write the index file
    return writeIndex(result, errlog);
  }

  // each dataset / group has one of these, across all partitions
  private class GroupPartitions {
    final GribCollectionMutable.GroupGC resultGroup;
    final GribCollectionMutable.GroupGC[] componentGroups; // one for each partition; may be null if group is not in the partition
    final int[] componentGroupIndex;                 // one for each partition; the index into the partition.ds2d.groups() array
    final int npart;

    GroupPartitions(GribCollectionMutable.GroupGC resultGroup, int npart) {
      this.resultGroup = resultGroup;
      this.npart = npart;
      this.componentGroups = new GribCollectionMutable.GroupGC[npart];
      this.componentGroupIndex = new int[npart];
    }

    void makeVariableIndexPartitioned() {
      // find unique variables across all partitions
      Map<GribCollectionMutable.VariableIndex, GribCollectionMutable.VariableIndex> varMap = new HashMap<>(2 * resultGroup.variList.size());
      for (GribCollectionMutable.GroupGC group : componentGroups) {
        if (group == null) continue;
        for (GribCollectionMutable.VariableIndex vi : group.variList)
          varMap.put(vi, vi); // this will use the last one found
      }
      for (GribCollectionMutable.VariableIndex vi : varMap.values()) {
        // convert each VariableIndex to VariableIndexPartitioned in result. note not using canon vi, but last one found
        result.makeVariableIndexPartitioned(resultGroup, vi, npart); // this adds to resultGroup
      }
    }
  }

  @Nullable
  private GribCollectionMutable.Dataset makeDataset2D(Formatter f) throws IOException {
    FeatureCollectionConfig config = (FeatureCollectionConfig) partitionManager.getAuxInfo(FeatureCollectionConfig.AUX_CONFIG);
    FeatureCollectionConfig.GribIntvFilter intvMap = (config != null) ? config.gribConfig.intvFilter : null;
    GribCollectionMutable.Dataset ds2D = result.makeDataset(GribCollectionImmutable.Type.TwoD);
    int npart = result.getPartitionSize();

    // make a list of unique groups across all partitions as well as component groups for each group
    List<CoordinateRuntime> masterRuntimes = new ArrayList<>();
    Map<Object, GroupPartitions> groupMap = new HashMap<>(40);  // gdsHashObject, GroupPartition
    CoordinateBuilder runtimeAllBuilder = new CoordinateRuntime.Builder2(null); // ok to use Builder2 for both grib1 and grib2 because not extracting

    int countPartition = 0;
    CalendarDateRange dateRangeAll = null;
    boolean rangeOverlaps = false;
    for (PartitionCollectionMutable.Partition tpp : result.getPartitions()) {
      try (GribCollectionMutable gc = tpp.makeGribCollection()) {  // LOOK open/close each child partition. could leave open ? they are NOT in cache
        if (gc == null) continue; // skip if they dont exist

        // note its not recursive, maybe leave open, or cache; actually we keep a pointer to the partition's group in the GroupPartitions
        CoordinateRuntime partRuntime = gc.masterRuntime;
        runtimeAllBuilder.addAll(partRuntime);  // make a complete set of runtime Coordinates
        masterRuntimes.add(partRuntime);        // make master runtimes

        GribCollectionMutable.Dataset ds2dp = gc.getDatasetCanonical(); // the twoD or GC dataset

        // date ranges must not overlap in order to use MRUTP
        if (dateRangeAll == null)
          dateRangeAll = gc.dateRange;
        else if (!rangeOverlaps) {
          rangeOverlaps = dateRangeAll.intersects(gc.dateRange);
          dateRangeAll = dateRangeAll.extend(gc.dateRange);
        }

        /* see if its only got one time coord
        if (ds2dp.gctype == GribCollectionImmutable.Type.SRC) {
          for (GribCollectionMutable.GroupGC group : ds2dp.getGroups()) {
            for (Coordinate coord : group.getCoordinates()) { // all time coords must have only one time
              if (coord instanceof CoordinateTime2D) {
                CoordinateTime2D coord2D = (CoordinateTime2D) coord;
                if (coord2D.getNtimes() > 1)
                  allAre1D = false;

            } else if (coord instanceof CoordinateTimeAbstract && coord.getSize() > 1)
                allAre1D = false;
            }
          }
        } else if (ds2dp.gctype == GribCollectionImmutable.Type.MRC || ds2dp.gctype == GribCollectionImmutable.Type.TwoD) {
          allAre1D = false;
        } */

        int groupIdx = 0;
        for (GribCollectionMutable.GroupGC g : ds2dp.groups) { // for each group in the partition
          GroupPartitions gs = groupMap.get(g.getGdsHash());
          if (gs == null) {
            gs = new GroupPartitions(ds2D.addGroupCopy(g), npart);
            groupMap.put(g.getGdsHash(), gs);
          }
          gs.componentGroups[countPartition] = g;
          gs.componentGroupIndex[countPartition] = groupIdx++;
        }
      } // close the gc
      countPartition++;
    } // loop over partition

    List<GroupPartitions> groupPartitions = new ArrayList<>(groupMap.values());
    result.masterRuntime = (CoordinateRuntime) runtimeAllBuilder.finish();
    if (result.isPartitionOfPartitions) // cache calendar dates for efficiency
      CoordinateTimeAbstract.cdf = new CalendarDateFactory(result.masterRuntime);

    if (!rangeOverlaps)
      ds2D.gctype = GribCollectionImmutable.Type.MRUTP;
      //else if (allAre1D)
      //  ds2D.gctype = GribCollectionImmutable.Type.MRSTP;
    else
      ds2D.gctype = GribCollectionImmutable.Type.TwoD;

    // create run2part: for each run, which partition to use
    result.run2part = new int[result.masterRuntime.getSize()];
    int partIdx = 0;
    for (CoordinateRuntime partRuntime : masterRuntimes) {
      for (Object val : partRuntime.getValues()) {
        int idx = result.masterRuntime.getIndex(val);
        result.run2part[idx] = partIdx;     // note that later partitions will override earlier if they have the same runtime
      }
      partIdx++;
    }

    // do each horiz group
    for (GroupPartitions gp : groupPartitions) {
      GribCollectionMutable.GroupGC resultGroup = gp.resultGroup;
      gp.makeVariableIndexPartitioned();

      String gname = resultGroup.getId();

      // for each partition in this gorup
      for (int partno = 0; partno < npart; partno++) {
        GribCollectionMutable.GroupGC group = gp.componentGroups[partno];
        if (group == null) {  // missing group in this partition
          f.format(" INFO canonical group %s not in partition %s%n", gname, result.getPartition(partno).getName());
          continue;
        }
        int groupIdx = gp.componentGroupIndex[partno];

        // for each variable in this Partition, add reference to it in the vip
        for (int varIdx = 0; varIdx < group.variList.size(); varIdx++) {
          GribCollectionMutable.VariableIndex vi = group.variList.get(varIdx);
          PartitionCollectionMutable.VariableIndexPartitioned vip = (PartitionCollectionMutable.VariableIndexPartitioned) resultGroup.findVariableByHash(vi);
          vip.addPartition(partno, groupIdx, varIdx, vi.ndups, vi.nrecords, vi.nmissing, vi);
        } // loop over variable
      } // loop over partition

      // each VariableIndexPartitioned now has its list of PartitionForVariable

      // overall set of unique coordinates
      boolean isDense = false; // (config != null) && "dense".equals(config.gribConfig.getParameter("CoordSys"));  // for now, assume non-dense
      CoordinateSharer sharify = new CoordinateSharer(isDense, logger);

      // for each variable, create union of coordinates across the partitions
      for (GribCollectionMutable.VariableIndex viResult : resultGroup.variList) {
        PartitionCollectionMutable.VariableIndexPartitioned vip = (PartitionCollectionMutable.VariableIndexPartitioned) viResult;
        vip.finish(); // create the SA, remove list LOOK, could do it differently

        // loop over partitions, make union coordinate; also time filter the intervals
        CoordinatePartitionUnionizer unionizer = new CoordinatePartitionUnionizer(viResult, intvMap, logger);
        for (int partno = 0; partno < npart; partno++) {
          GribCollectionMutable.GroupGC group = gp.componentGroups[partno];
          if (group == null) continue; // tolerate missing groups
          GribCollectionMutable.VariableIndex vi = group.findVariableByHash(viResult);
          if (vi == null) continue; // tolerate missing variables
          try {
            PartitionCollectionMutable.Partition part = ds2D.gctype.isUniqueTime() ? null : result.getPartition(partno);
            unionizer.addCoords(vi.getCoordinates(), part);
          } catch (IllegalStateException e) {
            logger.error(e.getMessage() + " on dataset " + name);
            return null;
          }
        }  // loop over partition

        viResult.coords = unionizer.finish();  // the viResult coordinates have been ortho/regularized
        sharify.addCoords(viResult.coords);
      } // loop over variable

      // create a list of common coordinates, put them into the group, and now variables just reference those by index
      sharify.finish();
      resultGroup.coords = sharify.getUnionCoords();

      // debug
      List<CoordinateTime2D> time2DCoords = new ArrayList<>();
      Map<CoordinateRuntime, CoordinateRuntime> runtimes = new HashMap<>();
      for (Coordinate coord : resultGroup.coords) {
        Coordinate.Type type = coord.getType();
        switch (type) {
          case runtime:
            CoordinateRuntime reftime = (CoordinateRuntime) coord;
            runtimes.put(reftime, reftime);
            break;

          case time2D:
            CoordinateTime2D t2d = (CoordinateTime2D) coord;
            time2DCoords.add(t2d);
            break;
        }
      }
      for (CoordinateTime2D t2d : time2DCoords) {
        CoordinateRuntime runtime2D = t2d.getRuntimeCoordinate();
        CoordinateRuntime runtime = runtimes.get(runtime2D);
        if (runtime == null)
          logger.warn("HEY assignRuntimeNames failed on {} group {}", t2d.getName(), resultGroup.getId());
      } // end debug


      for (GribCollectionMutable.VariableIndex viResult : resultGroup.variList) {
        // redo the variables against the shared coordinates
        viResult.coordIndex = sharify.reindex2shared(viResult.coords); // ok
        viResult.coords = null; // dont use anymore, now use coordIndex into group coordinates
      }

    } // loop over groups

    CoordinateTimeAbstract.cdf = null;
    return ds2D;
  }


  /* LOOK heres a place where one could "post process" and combine, instead of at coverage level.
  // this would benefit iosp, ie the netcdf API
  private void makeTime2runtime(GribCollectionMutable.Dataset ds2D, boolean isComplete) throws IOException {

    for (GribCollectionMutable.GroupGC group2D : ds2D.groups) {
      // for each time2D, add time2runtime
      for (Coordinate coord : group2D.coords) {
        if (coord instanceof CoordinateTime2D) {
          ((CoordinateTime2D) coord).makeTime2runtime(result.masterRuntime);
        }
      }
    }
  } */

  private void makeDatasetBest(GribCollectionMutable.Dataset ds2D, boolean isComplete) {
    GribCollectionMutable.Dataset dsBest = result.makeDataset(isComplete ? GribCollectionImmutable.Type.BestComplete : GribCollectionImmutable.Type.Best);

    int npart = result.getPartitionSize();

    // for each 2D group
    for (GribCollectionMutable.GroupGC group2D : ds2D.groups) {
      GribCollectionMutable.GroupGC groupB = dsBest.addGroupCopy(group2D);  // make copy of group, add to Best dataset
      groupB.isTwoD = false;

      // for each time2D, create the best time coordinates
      HashMap<Coordinate, CoordinateTimeAbstract> map2DtoBest = new HashMap<>(); // associate 2D coord with best
      CoordinateSharerBest sharer = new CoordinateSharerBest();
      for (Coordinate coord : group2D.coords) {
        if (coord instanceof CoordinateRuntime) continue; // skip it
        if (coord instanceof CoordinateTime2D) {
          CoordinateTimeAbstract best = ((CoordinateTime2D) coord).makeBestTimeCoordinate(result.masterRuntime);
          if (!isComplete) best = best.makeBestFromComplete();
          sharer.addCoordinate(best);
          map2DtoBest.put(coord, best);
        } else {
          sharer.addCoordinate(coord);
        }
      }
      groupB.coords = sharer.finish();  // these are the unique coords for group Best

      // transfer variables to Best group, set shared Coordinates
      for (GribCollectionMutable.VariableIndex vi2d : group2D.variList) {
        // copy vi2d and add to groupB
        PartitionCollectionMutable.VariableIndexPartitioned vip = result.makeVariableIndexPartitioned(groupB, vi2d, npart);
        vip.finish();

        // set shared coordinates
        List<Coordinate> newCoords = new ArrayList<>();
        for (Integer groupIndex : vi2d.coordIndex) {
          Coordinate coord2D = group2D.coords.get(groupIndex);
          if (coord2D instanceof CoordinateRuntime) continue; // skip runtime;
          if (coord2D instanceof CoordinateTime2D) {
            newCoords.add(map2DtoBest.get(coord2D)); // add the best coordinate for that CoordinateTime2D
          } else {
            newCoords.add(coord2D);
          }
        }
        vip.coordIndex = sharer.reindex(newCoords);
      }

    } // loop over groups
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  protected abstract String getMagicStart();

  protected abstract int getVersion();

  private GribCollectionWriter writer;

  // writing ncx2
  // The Groups are GribCollection.GroupGC, not GribXCollectionWriter.Group, so we cant inheritit most of the methods from GribXCollectionWriter

  /*
  MAGIC_START
  version
  sizeRecords
  VariableRecords (sizeRecords bytes)
  sizeIndex
  GribCollectionIndex (sizeIndex bytes)
  */
  protected boolean writeIndex(PartitionCollectionMutable pc, Formatter f) throws IOException {
    File idxFile = GribIndexCache.getFileOrCache(partitionManager.getIndexFilename(GribCdmIndex.NCX_SUFFIX));
    if (idxFile.exists()) {
      RandomAccessFile.eject(idxFile.getPath());
      if (!idxFile.delete())
        logger.error("gc2tp cant delete " + idxFile.getPath());
    }

    writer = new GribCollectionWriter(null, null);

    try (RandomAccessFile raf = new RandomAccessFile(idxFile.getPath(), "rw")) {
      raf.order(RandomAccessFile.BIG_ENDIAN);

      //// header message
      raf.write(getMagicStart().getBytes(CDM.utf8Charset));
      raf.writeInt(getVersion());
      raf.writeLong(0); // no record section

      /*
      message GribCollection {
        required string name = 1;         // must be unique - index filename is name.ncx
        required string topDir = 2;       // filenames are reletive to this
        repeated MFile mfiles = 3;        // list of grib MFiles
        repeated Dataset dataset = 4;
        repeated Gds gds = 5;             // unique Gds, shared amongst datasets

        required int32 center = 6;      // these 4 fields are to get a GribTable object
        required int32 subcenter = 7;
        required int32 master = 8;
        required int32 local = 9;       // grib1 table Version

        optional int32 genProcessType = 10;
        optional int32 genProcessId = 11;
        optional int32 backProcessId = 12;

        // repeated Parameter params = 20;      // not used
        FcConfig config = 21;
        uint64 startTime = 22; // calendar date, first valid time
        uint64 endTime = 23;   // calendar date, last valid time

        extensions 100 to 199;
      }

      extend GribCollection {
        repeated Partition partitions = 100;
      }
       */

      GribCollectionProto.GribCollection.Builder indexBuilder = GribCollectionProto.GribCollection.newBuilder();
      indexBuilder.setName(pc.getName());
      Path topDir = pc.directory.toPath();
      String pathS = StringUtil2.replace(topDir.toString(), '\\', "/");
      indexBuilder.setTopDir(pathS);

      // mfiles are the partition indexes
      int count = 0;
      for (PartitionCollectionMutable.Partition part : pc.partitions) {
        GribCollectionProto.MFile.Builder b = GribCollectionProto.MFile.newBuilder();
        String pathRS = makeReletiveFilename(pc, part); // reletive to pc.directory
        b.setFilename(pathRS);
        b.setLastModified(part.getLastModified());
        b.setLength(part.fileSize);
        b.setIndex(count++);
        indexBuilder.addMfiles(b.build());
      }

      indexBuilder.setCenter(pc.center);
      indexBuilder.setSubcenter(pc.subcenter);
      indexBuilder.setMaster(pc.master);
      indexBuilder.setLocal(pc.local);

      indexBuilder.setGenProcessId(pc.genProcessId);
      indexBuilder.setGenProcessType(pc.genProcessType);
      indexBuilder.setBackProcessId(pc.backProcessId);

      indexBuilder.setStartTime(pc.dateRange.getStart().getMillis());
      indexBuilder.setEndTime(pc.dateRange.getEnd().getMillis());

      indexBuilder.setMasterRuntime(writer.writeCoordProto(pc.masterRuntime));

      // dataset
      for (GribCollectionMutable.Dataset ds : pc.datasets)
        indexBuilder.addDataset(writeDatasetProto(pc, ds));

      // extensions
      if (pc.run2part != null) {
        for (int part : pc.run2part)
          indexBuilder.addRun2Part(part);
      }
      for (PartitionCollectionMutable.Partition part : pc.partitions)
        indexBuilder.addPartitions(writePartitionProto(pc, part));
      indexBuilder.setIsPartitionOfPartitions(pc.isPartitionOfPartitions);

      // write it out
      GribCollectionProto.GribCollection index = indexBuilder.build();
      byte[] b = index.toByteArray();
      NcStream.writeVInt(raf, b.length); // message size
      raf.write(b);  // message  - all in one gulp
      f.format("Grib2PartitionIndex= %d bytes file size =  %d bytes%n%n", b.length, raf.length());
    }

    return true;
  }

  private String makeReletiveFilename(PartitionCollectionMutable pc, PartitionCollectionMutable.Partition part) {
    Path topDir = pc.directory.toPath();
    Path partPath = new File(part.getDirectory(), part.getFilename()).toPath();
    Path pathRelative = topDir.relativize(partPath);
    return StringUtil2.replace(pathRelative.toString(), '\\', "/");
  }

  /*
  message Dataset {
    required Type type = 1;
    repeated Group groups = 2;
  }
   */
  private GribCollectionProto.Dataset writeDatasetProto(PartitionCollectionMutable pc, GribCollectionMutable.Dataset ds) throws IOException {
    GribCollectionProto.Dataset.Builder b = GribCollectionProto.Dataset.newBuilder();

    GribCollectionProto.Dataset.Type type = GribCollectionProto.Dataset.Type.valueOf(ds.gctype.toString());
    b.setType(type);

    for (GribCollectionMutable.GroupGC group : ds.groups)
      b.addGroups(writeGroupProto(pc, group));

    return b.build();
  }

  /*
  message Group {
    Gds gds = 1;                             // use this to build the HorizCoordSys
    repeated Variable variables = 2;         // list of variables
    repeated Coord coords = 3;               // list of coordinates
    repeated uint32 fileno = 4 [packed=true]; // the component files that are in this group, key into gc.mfiles
  }
   */
  private GribCollectionProto.Group writeGroupProto(PartitionCollectionMutable pc, GribCollectionMutable.GroupGC g) throws IOException {
    GribCollectionProto.Group.Builder b = GribCollectionProto.Group.newBuilder();

    b.setGds(GribCollectionWriter.writeGdsProto(g.horizCoordSys.getRawGds(), g.horizCoordSys.getPredefinedGridDefinition()));

    for (GribCollectionMutable.VariableIndex vb : g.variList) {
      b.addVariables(writeVariableProto((PartitionCollectionMutable.VariableIndexPartitioned) vb));
    }

    for (Coordinate coord : g.coords) {
      switch (coord.getType()) {
        case runtime:
          b.addCoords(writer.writeCoordProto((CoordinateRuntime) coord));
          break;
        case time:
          b.addCoords(writer.writeCoordProto((CoordinateTime) coord));
          break;
        case timeIntv:
          b.addCoords(writer.writeCoordProto((CoordinateTimeIntv) coord));
          break;
        case time2D:
          b.addCoords(writer.writeCoordProto((CoordinateTime2D) coord));
          break;
        case vert:
          b.addCoords(writer.writeCoordProto((CoordinateVert) coord));
          break;
        case ens:
          b.addCoords(writer.writeCoordProto((CoordinateEns) coord));
          break;
      }
    }

    if (g.filenose != null)
      for (Integer fileno : g.filenose)
        b.addFileno(fileno);

    return b.build();
  }

  /*
  message Variable {
   uint32 discipline = 1;
   bytes pds = 2;                   // raw pds
   repeated uint32 ids = 3 [packed=true];         // extra info not in pds; grib2 id section

   uint64 recordsPos = 4;  // offset of SparseArray message for this Variable
   uint32 recordsLen = 5;  // size of SparseArray message for this Variable

   repeated uint32 coordIdx = 6 [packed=true];    // indexes into Group.coords

   // optionally keep stats
   uint32 ndups = 8;
   uint32 nrecords = 9;
   uint32 missing = 10;

   // partition only
   repeated PartitionVariable partVariable = 100;
   }
   */
  private GribCollectionProto.Variable writeVariableProto(PartitionCollectionMutable.VariableIndexPartitioned vp) {

    GribCollectionProto.Variable.Builder b = GribCollectionProto.Variable.newBuilder();

    b.setDiscipline(vp.discipline);
    b.setPds(ByteString.copyFrom(vp.rawPds));

    // extra id info
    b.addIds(vp.center);
    b.addIds(vp.subcenter);

    b.setRecordsPos(vp.recordsPos);
    b.setRecordsLen(vp.recordsLen);

    for (int idx : vp.coordIndex)
      b.addCoordIdx(idx);

    b.setNdups(vp.ndups);
    b.setNrecords(vp.nrecords);
    b.setMissing(vp.nmissing);

    /* if (vp.twot != null) { // only for 2D
      for (int invCount : vp.twot.getCount())
        b.addInvCount(invCount);
    }

    if (vp.time2runtime != null) { // only for 1D
      for (int idx=0; idx < vp.time2runtime.getN(); idx++)
        b.addTime2Runtime(vp.time2runtime.get(idx));
    } */

    // extensions
    if (vp.nparts > 0 && vp.partnoSA != null) {
      for (int i = 0; i < vp.nparts; i++) // PartitionCollection.PartitionForVariable2D pvar : vp.getPartitionForVariable2D())
        b.addPartVariable(writePartitionVariableProto(vp.partnoSA.get(i), vp.groupnoSA.get(i), vp.varnoSA.get(i), vp.nrecords, vp.ndups, vp.nmissing));  // LOOK was it finished ??
    }

    return b.build();
  }

  /*
  message PartitionVariable {
    uint32 groupno = 1;
    uint32 varno = 2;
    uint32 flag = 3;
    uint32 partno = 4;

    // optionally keep stats
    uint32 ndups = 8;
    uint32 nrecords = 9;
    uint32 missing = 10;
  }
   */
  private GribCollectionProto.PartitionVariable writePartitionVariableProto(int partno, int groupno, int varno, int nrecords, int ndups, int nmissing) {
    GribCollectionProto.PartitionVariable.Builder pb = GribCollectionProto.PartitionVariable.newBuilder();
    pb.setPartno(partno);
    pb.setGroupno(groupno);
    pb.setVarno(varno);
    pb.setNdups(ndups);
    pb.setNrecords(nrecords);
    pb.setMissing(nmissing);

    return pb.build();
  }

  /*
message Partition {
  string name = 1;       // name is used in TDS - eg the subdirectory when generated by TimePartitionCollections
  string filename = 2;   // the gribCollection.ncx file, reletive to gc.
  string directory = 3;  // top directory NOT USED
  uint64 lastModified = 4;
  int64 length = 5;
  int64 partitionDate = 6;  // partition date added 11/25/14
  }
   */
  private GribCollectionProto.Partition writePartitionProto(PartitionCollectionMutable pc, PartitionCollectionMutable.Partition p) {
    GribCollectionProto.Partition.Builder b = GribCollectionProto.Partition.newBuilder();
    String pathRS = makeReletiveFilename(pc, p); // reletive to pc.directory
    b.setFilename(pathRS);
    b.setName(p.name);
    // b.setDirectory(p.directory);
    b.setLastModified(p.lastModified);
    b.setLength(p.fileSize);
    if (p.partitionDate != null)
      b.setPartitionDate(p.partitionDate.getMillis());  // LOOK what about calendar ??

    return b.build();
  }

}
