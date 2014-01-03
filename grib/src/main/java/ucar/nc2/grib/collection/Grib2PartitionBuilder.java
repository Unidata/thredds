package ucar.nc2.grib.collection;

import com.google.protobuf.ByteString;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MCollection;
import thredds.inventory.partition.PartitionManager;
import ucar.ma2.Section;
import ucar.nc2.grib.TimeCoord;
import ucar.sparr.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.stream.NcStream;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Builds Grib2 PartitionCollections (version 2)
 *
 * @author John
 * @since 12/7/13
 */
public class Grib2PartitionBuilder extends Grib2CollectionBuilder {
  public static final String MAGIC_START = "Grib2Partition2Index";  // was Grib2Partition0Index
  static private final boolean trace = false;

  /* called by tdm: update partition, test children partitions
  static public boolean update(PartitionManager tpc, org.slf4j.Logger logger) throws IOException {
    Grib2PartitionBuilder builder = new Grib2PartitionBuilder(tpc.getCollectionName(), new File(tpc.getRoot()), tpc, logger);
    if (!builder.needsUpdate()) return false;
    builder.readOrCreateIndex(CollectionUpdateType.always, CollectionUpdateType.test, null);
    builder.gc.close();
    return true;
  }  */

  // read in the index, create if it doesnt exist or is out of date (depends on force)
  static public Grib2Partition factory(PartitionManager tpc, CollectionUpdateType forcePartition, CollectionUpdateType forceChildren,
                                       Formatter errlog, org.slf4j.Logger logger) throws IOException {
    Grib2PartitionBuilder builder = new Grib2PartitionBuilder(tpc.getCollectionName(), new File(tpc.getRoot()), tpc, logger);
    builder.readOrCreateIndex(forcePartition, forceChildren, errlog);
    return builder.result;
  }

  // recreate if partition doesnt exist or is out of date (depends on force). return true if it was recreated
  static public boolean recreateIfNeeded(PartitionManager tpc, CollectionUpdateType forcePartition, CollectionUpdateType forceChildren,
                                         Formatter errlog, org.slf4j.Logger logger) throws IOException {
    Grib2PartitionBuilder builder = new Grib2PartitionBuilder(tpc.getCollectionName(), new File(tpc.getRoot()), tpc, logger);
    boolean recreated = builder.readOrCreateIndex(forcePartition, forceChildren, errlog);
    builder.result.close();
    return recreated;
  }

  /* make the index
  static public boolean makePartitionIndex(PartitionManager tpc, Formatter errlog, org.slf4j.Logger logger) throws IOException {
    Grib2PartitionBuilder builder = new Grib2PartitionBuilder(tpc.getCollectionName(), new File(tpc.getRoot()), tpc, logger);
    builder.result.close();
    return builder.createPartitionedIndex(errlog);
  } */

  //////////////////////////////////////////////////////////////////////////////////

  private final PartitionManager partitionManager; // defines the partition
  private Grib2Partition result;  // build this object

  protected Grib2PartitionBuilder(String name, File directory, PartitionManager tpc, org.slf4j.Logger logger) {
    super(tpc, logger);
    this.name = name;
    this.directory = directory;

    FeatureCollectionConfig.GribConfig config = null;
    if (tpc != null)
      config = (FeatureCollectionConfig.GribConfig) tpc.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    this.result = new Grib2Partition(name, directory, config, logger);
    this.gc = result;
    this.partitionManager = tpc;
  }

  // return true if the partition was recreated
  private boolean readOrCreateIndex(CollectionUpdateType forcePartition, CollectionUpdateType forceChildren, Formatter errlog) throws IOException {
    File idx = gc.getIndexFile();

    // force new index or test for new index needed
    boolean force = ((forcePartition == CollectionUpdateType.always) || (forcePartition == CollectionUpdateType.test && needsUpdate(idx.lastModified())));

    // otherwise, we're good as long as the index file exists and can be read
    if (force || !idx.exists() || !readIndex(idx.getPath())) {
      if (forcePartition == CollectionUpdateType.never) throw new IOException("failed to read " + idx.getPath());

      logger.info("{}: createIndex {}", gc.getName(), idx.getPath());
      if (createPartitionedIndex(forceChildren, errlog)) {  // write index
        return readIndex(idx.getPath()); // read back in index
      }
    }
    return false;
  }

  private boolean needsUpdate(long collectionLastModified) throws IOException {
    for (MCollection dcm : partitionManager.makePartitions()) {
      File idxFile = ucar.nc2.grib.collection.GribCollection.getIndexFile(dcm);
      if (!idxFile.exists())
        return true;
      if (collectionLastModified < idxFile.lastModified())
        return true;
    }
    return false;
  }

  private boolean readIndex(String filename) throws IOException {
    return readIndex(new RandomAccessFile(filename, "r"));
  }

  private boolean readIndex(RandomAccessFile indexRaf) throws IOException {
    try {
      this.result = (Grib2Partition) Grib2PartitionBuilderFromIndex.createTimePartitionFromIndex(this.name, this.directory, indexRaf, gc.getGribConfig(), logger);
      this.gc = result;
      return true;
    } catch (IOException ioe) {
      return false;
    }
  }

  ///////////////////////////////////////////////////
  // create the index

  private boolean createPartitionedIndex(CollectionUpdateType forceChildren, Formatter errlog) throws IOException {
    long start = System.currentTimeMillis();
    if (errlog == null) errlog = new Formatter(); // info will be discarded

    // create partitions
    for (MCollection dcm : partitionManager.makePartitions()) {
      result.addPartition(dcm);
    }

    List<PartitionCollection.Partition> bad = new ArrayList<>();
    for (PartitionCollection.Partition tpp : result.getPartitions()) {
      try {
        tpp.gc = tpp.makeGribCollection(forceChildren);  // here we read in all collections at once. can we avoid this?
        if (trace) logger.debug(" Open partition {}", tpp.getDcm().getCollectionName());
      } catch (Throwable t) {
        logger.error(" Failed to open partition " + tpp.getName(), t);
        bad.add(tpp);
      }
    }

    // remove ones that failed
    for (PartitionCollection.Partition tpp : bad)
      result.removePartition(tpp);

    // choose the "canonical" partition, aka prototype
    int n = result.getPartitions().size();
    if (n == 0) {
      errlog.format("ERR Nothing in this partition = %s%n", result.getName());
      logger.error(" Nothing in this partition = {}", result.getName());
      return false;
    }
    int idx = partitionManager.getProtoIndex(n);
    PartitionCollection.Partition canon = result.getPartitions().get(idx);
    errlog.format(" INFO Using canonical partition %s%n", canon.getDcm().getCollectionName());
    logger.info("     Using canonical partition {}", canon.getDcm().getCollectionName());

     // alternately one could choose a partition that has all the groups or maximum # variables
    // List<GroupAndPartitions> groupList = makeAllGroups();

    // copy info from canon gribCollection to result partitionCollection
    result.copyInfo(canon.gc);

    // check consistency across vert and ens coords
    // create partitioned variables
    // partition index is used - do not resort partitions
    PartitionCollection.Dataset ds2D = makeDataset2D(canon, errlog);
    if (ds2D == null) {
      errlog.format(" ERR makeDataset2D failed, index not written on %s%n", result.getName());
      logger.error(" makeDataset2D failed, index not written on {} errors = \n{}", result.getName(), errlog.toString());
      return false;
    }

    // this finishes the 2D stuff
    result.finish();

    if (!makeDatasetBest(ds2D, errlog)) {
      errlog.format(" ERR makeDatasetAnalysis failed, index not written on %s%n", result.getName());
      logger.error(" makeDatasetAnalysis failed, index not written on {} errors = \n{}", result.getName(), errlog.toString());
    }

    // ready to write the index file
    writeIndex(result, errlog);

    // close open gc's
    for (PartitionCollection.Partition tpp : result.getPartitions()) {
      tpp.gc.close();
    }
    canon.gc.close();

    long took = System.currentTimeMillis() - start;
    errlog.format(" INFO CreatePartitionedIndex took %d msecs%n", took);
    return true;
  }

  private class GroupPartitions {
    GribCollection.GroupHcs g;
    List<PartitionCollection.Partition> parts = new ArrayList<>();

    private GroupPartitions(GribCollection.GroupHcs g) {
      this.g = g;
    }
  }

  // LOOK could use this group list to make sure all groups are in result
  // could do same for all vars - eg dont have canon
  private List<GroupPartitions> makeAllGroups() throws IOException {
    Map<Integer, GroupPartitions> groupMap = new HashMap<>(40);
    for (PartitionCollection.Partition tpp : result.getPartitions()) {
      GribCollection gc = tpp.gc;
      for (GribCollection.GroupHcs g : gc.groups) {
        GroupPartitions gs = groupMap.get(g.horizCoordSys.getGdsHash());
        if (gs == null) {
          gs = new GroupPartitions(g);
          groupMap.put(g.horizCoordSys.getGdsHash(), gs);
        }
        gs.parts.add(tpp);
      }
    }
    return new ArrayList<>(groupMap.values());
  }

  private PartitionCollection.Dataset makeDataset2D(PartitionCollection.Partition canon, Formatter f) throws IOException {
    PartitionCollection.Dataset ds2D = result.makeDataset(PartitionCollectionProto.Dataset.Type.TwoD);

    FeatureCollectionConfig.GribConfig config = (FeatureCollectionConfig.GribConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    FeatureCollectionConfig.GribIntvFilter intvMap = (config != null) ? config.intvFilter : null;

    List<PartitionCollection.Partition> partitions = result.getPartitions();
    int npart = partitions.size();
    boolean ok = true;

     // do each group
    GribCollection canonGc = canon.gc;
    for (GribCollection.GroupHcs canonGroup2 : canonGc.groups) {
      // make copy of group
      GribCollection.GroupHcs resultGroup = ds2D.addGroupCopy(canonGroup2);

      String gname = canonGroup2.getId();
      String gdesc = canonGroup2.getDescription();
      if (trace) f.format(" Check Group %s%n", gname);

      // hash proto variables for quick lookup
      Map<Integer, PartitionCollection.VariableIndexPartitioned> resultVarMap = new HashMap<>(2 * canonGroup2.variList.size());
      for (GribCollection.VariableIndex vi : canonGroup2.variList) {
        // convert each VariableIndex to VariableIndexPartitioned
        PartitionCollection.VariableIndexPartitioned vip = result.makeVariableIndexPartitioned(resultGroup, vi, npart);
        if (resultVarMap.containsKey(vi.cdmHash)) {
          f.format(" ERR Duplicate variable hash %s%n", vi.toStringShort());
          logger.error("Duplicate variable hash " + vi.toStringShort());
        }
        resultVarMap.put(vi.cdmHash, vip);
      }

      // make a single runtime coordinate
      CoordinateBuilder runtimeAllBuilder = new CoordinateRuntime.Builder();

      // for each partition
      for (int partno = 0; partno < npart; partno++) {
        PartitionCollection.Partition tpp = partitions.get(partno);
        if (trace) f.format(" Check Partition %s%n", tpp.getName());

        // get group corresponding to canonGroup
        GribCollection gc = tpp.gc;
        int groupIdx = gc.findGroupIdxById(canonGroup2.getId());
        if (groupIdx < 0) {
          f.format(" INFO canonical group %s in not partition %s%n", gname, tpp.getName());
          continue;
        }
        GribCollection.GroupHcs group = gc.getGroup(groupIdx);

        // keep track of all runtime coordinates across the partitions
        for (Coordinate coord : group.coords) {
          if (coord.getType() == Coordinate.Type.runtime)
            runtimeAllBuilder.addAll(coord);
        }

        // for each variable in otherPartition group
        for (int varIdx = 0; varIdx < group.variList.size(); varIdx++) {
          GribCollection.VariableIndex viFromOtherPartition = group.variList.get(varIdx);
          if (trace) f.format(" Check %s%n", viFromOtherPartition.toStringShort());
          int flag = 0;

          PartitionCollection.VariableIndexPartitioned viResult = resultVarMap.get(viFromOtherPartition.cdmHash); // match with proto variable hash
          if (viResult == null) {
            f.format(" WARN Cant find %s from %s / %s in proto %s - add%n", viFromOtherPartition.toStringShort(), tpp.getName(), gdesc, canon.getName());

            //////////////// add it to the canonGroup
            viResult = result.makeVariableIndexPartitioned(resultGroup, viFromOtherPartition, npart);
            resultVarMap.put(viFromOtherPartition.cdmHash, viResult);
          }

          /* compare vert coordinates
          VertCoord vc1 = viCanon.getVertCoord();
          VertCoord vc2 = viFromOtherPartition.getVertCoord();
          if ((vc1 == null) != (vc2 == null)) {
            vc1 = viCanon.getVertCoord();   // debug
            vc2 = viFromOtherPartition.getVertCoord();
            f.format(" ERR Vert coordinates existence on group %s (%s) on %s in %s (exist %s) doesnt match %s (exist %s)%n",
                    gname, gdesc, viFromOtherPartition.toStringShort(),
                    tpp.getName(), (vc2 == null), canon.getName(), (vc1 == null));
            ok = false; // LOOK could remove vi ?

          } else if ((vc1 != null) && !vc1.equalsData(vc2)) {
            f.format(" WARN Vert coordinates values on %s in %s dont match%n", viFromOtherPartition.toStringShort(), tpp.getName());
            f.format("    canon vc = %s%n", vc1);
            f.format("    this vc = %s%n", vc2);
            flag |= PartitionCollection.VERT_COORDS_DIFFER;
          }

          //compare ens coordinates
          EnsCoord ec1 = viCanon.getEnsCoord();
          EnsCoord ec2 = viFromOtherPartition.getEnsCoord();
          if ((ec1 == null) != (ec2 == null)) {
            f.format(" ERR Ensemble coordinates existence on %s in %s doesnt match%n", viFromOtherPartition.toStringShort(), tpp.getName());
            ok = false; // LOOK could remove vi ?
          } else if ((ec1 != null) && !ec1.equalsData(ec2)) {
            f.format(" WARN Ensemble coordinates values on %s in %s dont match%n", viFromOtherPartition.toStringShort(), tpp.getName());
            f.format("    canon ec = %s%n", ec1);
            f.format("    this ec = %s%n", ec2);
            flag |= PartitionCollection.ENS_COORDS_DIFFER;
          }  */

          viResult.addPartition(partno, groupIdx, varIdx, flag, viFromOtherPartition);
        } // loop over variable
      } // loop over partition

      // each VariableIndexPartitioned now has its list of  PartitionForVariable

      Coordinate runtimeCoord = runtimeAllBuilder.finish();
      resultGroup.run2part = new ArrayList<>(runtimeCoord.getSize());
      for (int i = 0; i < runtimeCoord.getSize(); i++)
        resultGroup.run2part.add(i); // LOOK wrong
      resultGroup.coords.add(runtimeCoord);

      // overall set of unique coordinates
      CoordinateUniquify uniquify = new CoordinateUniquify();

      // for each variable, create union of coordinates
      for (GribCollection.VariableIndex viResult : resultGroup.variList) {

        // loop over partitions, make union coordinate, time filter intervals
        CoordinateUnionizer unionizer = new CoordinateUnionizer(viResult.getVarid(), intvMap);
        for (PartitionCollection.Partition tpp : partitions) {
          GribCollection.GroupHcs group = tpp.gc.findGroupById(resultGroup.getId());
          if (group == null) continue; // tolerate missing groups
          GribCollection.VariableIndex vi = group.findVariableByHash(viResult.cdmHash);
          if (vi == null) continue; // tolerate missing variables
          unionizer.addCoords(vi.getCoordinates());
        }

        viResult.coords = unionizer.finish();
        uniquify.addCoords(viResult.coords);
      }

      uniquify.finish();
      resultGroup.coords = uniquify.getUnionCoords();

      // redo the variables against the shared coordinates
      for (GribCollection.VariableIndex viResult : resultGroup.variList) {
        viResult.coordIndex = uniquify.reindex(viResult.coords);
      }

      // figure out missing
      for (GribCollection.VariableIndex viResult : resultGroup.variList) {
        Coordinate cr = viResult.getCoordinate(Coordinate.Type.runtime);
        Coordinate ct = viResult.getCoordinate(Coordinate.Type.time);
        if (ct == null) ct = viResult.getCoordinate(Coordinate.Type.timeIntv);

        CoordinateTwoTimer twot = new CoordinateTwoTimer(cr.getSize(), ct.getSize());
        Map<Object, Integer> ctMap = new HashMap<>(2*ct.getSize());
        for (int i=0; i<ct.getSize(); i++) ctMap.put(ct.getValue(i), i);

        int runIdx = 0;
        for (PartitionCollection.Partition tpp : partitions) {
          GribCollection.GroupHcs group = tpp.gc.findGroupById(resultGroup.getId());
          if (group == null) continue; // tolerate missing groups
          GribCollection.VariableIndex vi = group.findVariableByHash(viResult.cdmHash);
          if (vi == null) continue; // tolerate missing variables

          Coordinate ctP = vi.getCoordinate(Coordinate.Type.time);
          if (ctP == null) ctP = vi.getCoordinate(Coordinate.Type.timeIntv);

          vi.readRecords();
          SparseArray<GribCollection.Record> sa = vi.getSparseArray();
          Section s = new Section(sa.getShape());
          Section.Iterator iter = s.getIterator(sa.getShape());

          // run through all the inventory in this partition
          int[] index = new int[sa.getRank()];
          while (iter.hasNext()) {
            int linearIndex = iter.next(index);
            if (sa.getContent(linearIndex) == null) continue;

            // convert value in partition to index in result
            int timeIdxP = index[1];
            Object val = ctP.getValue(timeIdxP);
            Integer timeIdxR = ctMap.get(val);
            if (timeIdxR != null) {
              twot.add(runIdx, timeIdxR);
            }
          }

          runIdx++;   // still assuming one run per partition LOOK
        }

        ((PartitionCollection.VariableIndexPartitioned)viResult).twot = twot;

        /* Formatter ff = new Formatter(System.out);
        ff.format("Variable %s%n", viResult.toStringShort());
        twot.showMissing(ff); */
      }

    } // loop over groups

    return ds2D;
  }

  private boolean makeDatasetBest(PartitionCollection.Dataset ds2D, Formatter f) throws IOException {
    PartitionCollection.Dataset dsa = result.makeDataset(PartitionCollectionProto.Dataset.Type.Best);

    List<PartitionCollection.Partition> partitions = result.getPartitions();
    int npart = partitions.size();
    boolean ok = true;

     // do each group
    for (GribCollection.GroupHcs group2D : ds2D.groups) {
      GribCollection.GroupHcs groupB = dsa.addGroupCopy(group2D);  // make copy of group, add to dataset
      groupB.run2part = group2D.run2part;                          // use same run -> partition map

      String gname = groupB.getId();
      String gdesc = groupB.getDescription();

      // runtime offsets
      CoordinateRuntime rtc = null;
      for (Coordinate coord : group2D.coords)
       if (coord.getType() == Coordinate.Type.runtime)
         rtc = (CoordinateRuntime) coord;
      assert rtc != null;
      List<Double> runOffset = rtc.getRuntimesUdunits();

      // transfer coordinates, order is preserved, so variable index ok
      for (Coordinate coord : group2D.coords) {
        if (coord.getType() == Coordinate.Type.time) {
          Coordinate best = convertBestTimeCoordinate(runOffset, (CoordinateTime) coord);
          groupB.coords.add(best);
        } else if (coord.getType() == Coordinate.Type.timeIntv) {
            Coordinate best = convertBestTimeCoordinateIntv(runOffset, (CoordinateTimeIntv) coord);
            groupB.coords.add(best);
        } else {
          groupB.coords.add(coord);
        }
      }

      // transfer variables
      for (GribCollection.VariableIndex vi2d : group2D.variList) {
        // copy vi and add to groupB
        PartitionCollection.VariableIndexPartitioned vip = result.makeVariableIndexPartitioned(groupB, vi2d, npart);
        int timeIdx = vip.getCoordinateIndex(Coordinate.Type.time);
        if (timeIdx >= 0) {
          CoordinateTime time2d = (CoordinateTime) group2D.coords.get(timeIdx);
          CoordinateTime timeBest = (CoordinateTime) groupB.coords.get(timeIdx);
          vip.time2runtime = makeTime2RuntimeMap(runOffset, time2d, timeBest, ((PartitionCollection.VariableIndexPartitioned) vi2d).twot);

        } else {
          timeIdx = vip.getCoordinateIndex(Coordinate.Type.timeIntv);
          CoordinateTimeIntv time2d = (CoordinateTimeIntv) group2D.coords.get(timeIdx);
          CoordinateTimeIntv timeBest = (CoordinateTimeIntv) groupB.coords.get(timeIdx);
          vip.time2runtime = makeTime2RuntimeMap(runOffset, time2d, timeBest, ((PartitionCollection.VariableIndexPartitioned) vi2d).twot);
        }
        vip.coordIndex = vip.coordIndex.subList(1, vip.coordIndex.size()); // remove runtime co9ordinate - always first one
      }

    } // loop over groups

    return ok;
  }

  // make the union of all the offsets from base date
  private CoordinateTime convertBestTimeCoordinate(List<Double> runOffsets, CoordinateTime timeCoord) {
    Set<Integer> values = new HashSet<>();
    for (double runOffset : runOffsets) {
      for (Integer val : timeCoord.getOffsetSorted())
        values.add((int) (runOffset + val)); // LOOK possible roundoff
    }

    List<Integer> offsetSorted = new ArrayList<>(values.size());
    for (Object val : values) offsetSorted.add( (Integer) val);
    Collections.sort(offsetSorted);
    return new CoordinateTime(offsetSorted, timeCoord.getCode());
  }

  // make the union of all the offsets from base date
  private CoordinateTimeIntv convertBestTimeCoordinateIntv(List<Double> runOffsets, CoordinateTimeIntv timeCoord) {
    Set<TimeCoord.Tinv> values = new HashSet<>();
    for (double runOffset : runOffsets) {
      for (TimeCoord.Tinv val : timeCoord.getTimeIntervals())
        values.add( val.offset(runOffset)); // LOOK possible roundoff
    }

    List<TimeCoord.Tinv> offsetSorted = new ArrayList<>(values.size());
    for (Object val : values) offsetSorted.add( (TimeCoord.Tinv) val);
    Collections.sort(offsetSorted);
    return new CoordinateTimeIntv(offsetSorted, timeCoord.getCode());
  }

  /**
   * calculate which partition to use, based on missing
   * @param runOffsets for each runtime, the offset from base time
   * @param timeCoord  original time coordinate offset from 2D dataset
   * @param coordBest  best time coordinate, from convertBestTimeCoordinate
   * @param twot       variable missing array
   * @return           for each time in coordBest, which runtime to use
   */
  private int[] makeTime2RuntimeMap(List<Double> runOffsets, CoordinateTime timeCoord, CoordinateTime coordBest, CoordinateTwoTimer twot) {
    int[] result = new int[ coordBest.getSize()];

    Map<Integer, Integer> map = new HashMap<>();  // lookup coord val to index
    int count = 0;
    for (Integer val : coordBest.getOffsetSorted()) map.put(val, count++);

    int runIdx = 0;
    for (double runOffset : runOffsets) {
      int timeIdx = 0;
      for (Integer val : timeCoord.getOffsetSorted()) {
        if (twot.getCount(runIdx, timeIdx) > 0) { // skip missing
          Integer bestVal = (int) (runOffset + val);
          Integer bestValIdx = map.get(bestVal);
          if (bestValIdx == null) throw new IllegalStateException();
          result[bestValIdx] = runIdx+1; // use this partition; later ones override; one based so 0 = missing
        }

        timeIdx++;
      }
      runIdx++;
    }
    return result;
  }

  private int[] makeTime2RuntimeMap(List<Double> runOffsets, CoordinateTimeIntv timeCoord, CoordinateTimeIntv coordBest, CoordinateTwoTimer twot) {
    int[] result = new int[ coordBest.getSize()];

    Map<TimeCoord.Tinv, Integer> map = new HashMap<>();  // lookup coord val to index
    int count = 0;
    for (TimeCoord.Tinv val : coordBest.getTimeIntervals()) map.put(val, count++);

    int runIdx = 0;
    for (double runOffset : runOffsets) {
      int timeIdx = 0;
      for (TimeCoord.Tinv val : timeCoord.getTimeIntervals()) {
        if (twot.getCount(runIdx, timeIdx) > 0) { // skip missing;
          TimeCoord.Tinv bestVal = val.offset(runOffset);
          Integer bestValIdx = map.get(bestVal);
          if (bestValIdx == null) throw new IllegalStateException();
          result[bestValIdx] = runIdx+1; // use this partition; later ones override; one based so 0 = missing
        }

        timeIdx++;
      }
      runIdx++;
    }
    return result;
  }

  //////////////////////////////////////////////////////////

  public String getMagicStart() {
    return MAGIC_START;
  }

  // writing ncx2
  /*
  MAGIC_START
  version
  sizeRecords
  VariableRecords (sizeRecords bytes)
  sizeIndex
  GribCollectionIndex (sizeIndex bytes)
  */
  private boolean writeIndex(PartitionCollection pc, Formatter f) throws IOException {
    File file = pc.getIndexFile();
    if (file.exists()) {
      if (!file.delete())
        logger.error("gc2tp cant delete " + file.getPath());
    }

    try (RandomAccessFile raf = new RandomAccessFile(file.getPath(), "rw")) {
      raf.order(RandomAccessFile.BIG_ENDIAN);

      //// header message
      raf.write(getMagicStart().getBytes(CDM.utf8Charset));
      raf.writeInt(version);
      raf.writeLong(0); // no record section

      /*
      message GribCollection {
        required string name = 1;       // must be unique - index filename is name.ncx
        required string topDir = 2;   // filenames are reletive to this
        repeated MFile mfiles = 3;    // list of grib MFiles
        repeated Group groups = 4;      // separate group for each GDS

        required int32 center = 6;      // these 4 fields are to get a GribTable object
       required int32 subcenter = 7;
        required int32 master = 8;
        required int32 local = 9;       // grib1 table Version

        optional int32 genProcessType = 10;   // why ??
        optional int32 genProcessId = 11;
        optional int32 backProcessId = 12;

        extensions 100 to 199;
      }


      extend GribCollection {
        repeated Gds gds = 100;
        repeated Dataset dataset = 101;
        repeated Partition partitions = 102;
        repeated Parameter pparams = 103;      // not used yet
      }
       */

      GribCollectionProto.GribCollection.Builder indexBuilder = GribCollectionProto.GribCollection.newBuilder();
      indexBuilder.setName(pc.getName());
      indexBuilder.setTopDir(pc.getDirectory().getPath()); // LOOK

      // mfiles are the partition indexes
      for (PartitionCollection.Partition part : pc.partitions) {
        GribCollectionProto.MFile.Builder b = GribCollectionProto.MFile.newBuilder();
        b.setFilename(part.getIndexFilename());
        b.setLastModified(part.getLastModified());
        indexBuilder.addMfiles(b.build());
      }

      // dont use groups

      indexBuilder.setCenter(pc.getCenter());
      indexBuilder.setSubcenter(pc.getSubcenter());
      indexBuilder.setMaster(pc.getMaster());
      indexBuilder.setLocal(pc.getLocal());

      indexBuilder.setGenProcessId(pc.getGenProcessId());
      indexBuilder.setGenProcessType(pc.getGenProcessType());
      indexBuilder.setBackProcessId(pc.getBackProcessId());

      List<PartitionCollectionProto.Gds> gdsProtoList = new ArrayList<>();
      for (GribCollection.HorizCoordSys hcs : pc.gdsList)
        gdsProtoList.add(writeGdsProto(hcs));
      indexBuilder.setExtension(PartitionCollectionProto.gds, gdsProtoList);

      List<PartitionCollectionProto.Dataset> dsProtoList = new ArrayList<>();
      for (PartitionCollection.Dataset ds : pc.datasets)
        dsProtoList.add(writeDatasetProto(pc, ds));
      indexBuilder.setExtension(PartitionCollectionProto.dataset, dsProtoList);

      List<PartitionCollectionProto.Partition> partProtoList = new ArrayList<>();
      for (PartitionCollection.Partition part : pc.partitions)
        partProtoList.add(writePartitionProto(part));
      indexBuilder.setExtension(PartitionCollectionProto.partitions, partProtoList);

      GribCollectionProto.GribCollection index = indexBuilder.build();
      byte[] b = index.toByteArray();
      NcStream.writeVInt(raf, b.length); // message size
      raf.write(b);  // message  - all in one gulp
      f.format("Grib2PartitionIndex= %d bytes file size =  %d bytes%n%n", b.length, raf.length());
    }

    return true;
  }

  /*
  message Gds {
    optional bytes gds = 1;             // all variables in the group use the same GDS
    optional sint32 gdsHash = 2 [default = 0];
    optional string nameOverride = 3;  // only when user overrides default name
  }
   */
  private PartitionCollectionProto.Gds writeGdsProto(GribCollection.HorizCoordSys hcs) throws IOException {
    PartitionCollectionProto.Gds.Builder b = PartitionCollectionProto.Gds.newBuilder();

    b.setGds(ByteString.copyFrom(hcs.getRawGds()));
    b.setGdsHash(hcs.getGdsHash());
    if (hcs.getNameOverride() != null)
      b.setNameOverride(hcs.getNameOverride());

    return b.build();
  }

  /*
  message Dataset {
    required Type type = 1;
    repeated Group groups = 2;
  }
   */
  private PartitionCollectionProto.Dataset writeDatasetProto(PartitionCollection pc, PartitionCollection.Dataset ds) throws IOException {
    PartitionCollectionProto.Dataset.Builder b = PartitionCollectionProto.Dataset.newBuilder();

    b.setType(ds.type);

    for (GribCollection.GroupHcs group : ds.groups)
      b.addGroups(writeGroupProto(pc, group));

    return b.build();
  }

  /*
  message Group {
    optional bytes gds = 1;             // all variables in the group use the same GDS
    optional sint32 gdsHash = 2 [default = 0];
    optional string nameOverride = 3;         // only when user overrides default name

    repeated Variable variables = 4;    // list of variables
    repeated Coord coords = 5;          // list of coordinates
    repeated int32 fileno = 7;          // the component files that are in this group, index into gc.files

    extensions 100 to 199;
  }
  extend Group {
    required uint32 gdsIndex = 100;       // index into TimePartition.gds
    repeated uint32 run2part = 101;       // partitions only: run index to partition index map
    repeated Parameter gparams = 102;      // not used yet
  }
   */
  private GribCollectionProto.Group writeGroupProto(PartitionCollection pc, GribCollection.GroupHcs g) throws IOException {
    GribCollectionProto.Group.Builder b = GribCollectionProto.Group.newBuilder();

    // dont store gds directly, use GdsIndex

    for (GribCollection.VariableIndex vb : g.variList) {
      b.addVariables(writeVariableProto((PartitionCollection.VariableIndexPartitioned) vb));
    }

    for (Coordinate coord : g.coords) {
      switch (coord.getType()) {
        case runtime:
          b.addCoords(writeCoordProto((CoordinateRuntime) coord));
          break;
        case time:
          b.addCoords(writeCoordProto((CoordinateTime) coord));
          break;
        case timeIntv:
          b.addCoords(writeCoordProto((CoordinateTimeIntv) coord));
          break;
        case vert:
          b.addCoords(writeCoordProto((CoordinateVert) coord));
          break;
      }
    }

    // LOOK probably not used?
    if (g.filenose != null)
      for (Integer fileno : g.filenose)
        b.addFileno(fileno);

    // extensions
    b.setExtension(PartitionCollectionProto.gdsIndex, pc.findIndex(g.horizCoordSys));

    List<Integer> run2partList = new ArrayList<>();
    if (g.run2part != null) {
      for (int part : g.run2part) run2partList.add(part);
      b.setExtension(PartitionCollectionProto.run2Part, run2partList);
    }

    return b.build();
  }

  /*
  message Variable {
     required uint32 discipline = 1;
     required bytes pds = 2;
     required fixed32 cdmHash = 3;

     required uint64 recordsPos = 4;  // offset of SparseArray message for this Variable
     required uint32 recordsLen = 5;  // size of SparseArray message for this Variable (could be in stream instead)

     repeated uint32 coordIdx = 6;    // index into Group.coords

     // optionally keep stats
     optional float density = 7;
     optional uint32 ndups = 8;
     optional uint32 nrecords = 9;
     optional uint32 missing = 10;

     extensions 100 to 199;
   }
   extend Variable {
     repeated PartitionVariable partition = 100;
     repeated Parameter vparams = 101;    // not used yet
   }
   */
  private GribCollectionProto.Variable writeVariableProto(PartitionCollection.VariableIndexPartitioned vp) throws IOException {

    GribCollectionProto.Variable.Builder b = GribCollectionProto.Variable.newBuilder();

    b.setDiscipline(vp.discipline);
    b.setPds(ByteString.copyFrom(vp.rawPds));
    b.setCdmHash(vp.cdmHash);

    b.setRecordsPos(vp.recordsPos);
    b.setRecordsLen(vp.recordsLen);

    for (int idx : vp.coordIndex)
      b.addCoordIdx(idx);

    b.setDensity(vp.density);
    b.setNdups(vp.ndups);
    b.setNrecords(vp.nrecords);
    b.setMissing(vp.missing);

    // extensions
    List<PartitionCollectionProto.PartitionVariable> pvarList = new ArrayList<>();
    for (PartitionCollection.PartitionForVariable pvar : vp.partList)
      pvarList.add(writePartitionVariableProto(pvar));
    b.setExtension(PartitionCollectionProto.partition, pvarList);

    if (vp.twot != null) { // only for 2D
      List<Integer> invCountList = new ArrayList<>(vp.twot.getCount().length);
      for (int count : vp.twot.getCount()) invCountList.add(count);
      b.setExtension(PartitionCollectionProto.invCount, invCountList);
    }

    if (vp.time2runtime != null) { // only for 1D
      List<Integer> list = new ArrayList<>(vp.time2runtime.length);
      for (int idx : vp.time2runtime) list.add(idx);
      b.setExtension(PartitionCollectionProto.time2Runtime, list);
    }

    return b.build();
  }

  /*
  message PartitionVariable {
    required uint32 groupno = 1;
    required uint32 varno = 2;
    required uint32 flag = 3;
    required uint32 partno = 4;

    // optionally keep stats
    optional float density = 7;
    optional uint32 ndups = 8;
    optional uint32 nrecords = 9;
    optional uint32 missing = 10;
  }
   */
  private PartitionCollectionProto.PartitionVariable writePartitionVariableProto(PartitionCollection.PartitionForVariable pvar) throws IOException {
    PartitionCollectionProto.PartitionVariable.Builder pb = PartitionCollectionProto.PartitionVariable.newBuilder();
    pb.setPartno(pvar.partno);
    pb.setGroupno(pvar.groupno);
    pb.setVarno(pvar.varno);
    pb.setFlag(pvar.flag);
    pb.setNdups(pvar.ndups);
    pb.setNrecords(pvar.nrecords);
    pb.setMissing(pvar.missing);
    pb.setDensity(pvar.density);

    return pb.build();
  }

  /*
message Partition {
  required string name = 1;       // name is used in TDS - eg the subdirectory when generated by TimePartitionCollections
  required string filename = 2;   // the gribCollection.ncx2 file
  required string directory = 3;   // top directory
  optional uint64 lastModified = 4;
}
  }
   */
  private PartitionCollectionProto.Partition writePartitionProto(PartitionCollection.Partition p) throws IOException {
    PartitionCollectionProto.Partition.Builder b = PartitionCollectionProto.Partition.newBuilder();

    b.setFilename(p.getIndexFilename());
    b.setName(p.getName());
    b.setDirectory(p.getDirectory());
    b.setLastModified(p.getLastModified());

    return b.build();
  }

  protected PartitionCollectionProto.Parameter writeParamProto(Parameter param) throws IOException {
    PartitionCollectionProto.Parameter.Builder b = PartitionCollectionProto.Parameter.newBuilder();

    b.setName(param.getName());
    if (param.isString())
      b.setSdata(param.getStringValue());
    else {
      for (int i = 0; i < param.getLength(); i++)
        b.addData(param.getNumericValue(i));
    }

    return b.build();
  }

}
