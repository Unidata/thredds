package ucar.nc2.grib.collection;

import com.google.protobuf.ByteString;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MCollection;
import thredds.inventory.partition.PartitionManager;
import ucar.ma2.Section;
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

    try {
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
      result.sortPartitions(); // after this cannot change

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
      result.makeHorizCS();

      if (!makeDatasetBest(ds2D, errlog)) {
        errlog.format(" ERR makeDatasetAnalysis failed, index not written on %s%n", result.getName());
        logger.error(" makeDatasetAnalysis failed, index not written on {} errors = \n{}", result.getName(), errlog.toString());
      }

      // ready to write the index file
      writeIndex(result, errlog);

    } finally {
      // close open gc's
      for (PartitionCollection.Partition tpp : result.getPartitions()) {
        tpp.gc.close();
      }
    }

    long took = System.currentTimeMillis() - start;
    errlog.format(" INFO CreatePartitionedIndex took %d msecs%n", took);
    return true;
  }

  private class GroupPartitions {
    GribCollection.GroupHcs resultGroup;
    GribCollection.GroupHcs[] componentGroups; // one for each partition; may be null if group is not in the partition
    int[] componentGroupIndex;                 // one for each partition; the index into the partition.ds2d.groups() array
    int npart;

    GroupPartitions(GribCollection.GroupHcs resultGroup, int npart) {
      this.resultGroup = resultGroup;
      this.npart = npart;
      this.componentGroups = new GribCollection.GroupHcs[npart];
      this.componentGroupIndex = new int[npart];
    }

    void makeVariablePartitions() {
      // find unique variables across all partitions
      Map<Integer, GribCollection.VariableIndex> varMap = new HashMap<>(2 * resultGroup.variList.size());
      for (GribCollection.GroupHcs group : componentGroups) {
        if (group == null) continue;
        for (GribCollection.VariableIndex vi : group.variList)
          varMap.put(vi.cdmHash, vi); // this will use the last one found
      }
      for (GribCollection.VariableIndex vi : varMap.values()) {
        // convert each VariableIndex to VariableIndexPartitioned in result. note not using canon
        result.makeVariableIndexPartitioned(resultGroup, vi, npart); // this adds to resultGroup
      }
    }
  }

  // a list of unique groups across all partitions
  // as well as component groups for each group
  private List<GroupPartitions> makeGroupPartitions(PartitionCollection.Dataset ds2D, int npart) throws IOException {
    Map<Integer, GroupPartitions> groupMap = new HashMap<>(40);
    int countPartition = 0;
    for (PartitionCollection.Partition tpp : result.getPartitions()) {
      GribCollection gc = tpp.gc;
      GribCollection.Dataset ds2dp = gc.getDataset2D();

      int groupIdx = 0;
      for (GribCollection.GroupHcs g : ds2dp.groups) { // for each group in the partition
        GroupPartitions gs = groupMap.get(g.horizCoordSys.getGdsHash());
        if (gs == null) {
          gs = new GroupPartitions(ds2D.addGroupCopy(g), npart);
          groupMap.put(g.horizCoordSys.getGdsHash(), gs);
        }
        gs.componentGroups[countPartition] = g;
        gs.componentGroupIndex[countPartition] = groupIdx++;
      }
      countPartition++;
    }
    return new ArrayList<>(groupMap.values());
  }

  private PartitionCollection.Dataset makeDataset2D(PartitionCollection.Partition canon2, Formatter f) throws IOException {
    FeatureCollectionConfig.GribConfig config = (FeatureCollectionConfig.GribConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    FeatureCollectionConfig.GribIntvFilter intvMap = (config != null) ? config.intvFilter : null;

    PartitionCollection.Dataset ds2D = result.makeDataset(GribCollectionProto.Dataset.Type.TwoD);

    List<PartitionCollection.Partition> partitions = result.getPartitions();
    int npart = partitions.size();
    List<GroupPartitions> groupPartitions = makeGroupPartitions(ds2D, npart);

    boolean ok = true;

     // do each group
    for (GroupPartitions gp : groupPartitions) {
      GribCollection.GroupHcs resultGroup = gp.resultGroup;
      gp.makeVariablePartitions();

      String gname = resultGroup.getId();
      String gdesc = resultGroup.getDescription();

      // make a single runtime coordinate
      CoordinateBuilder runtimeAllBuilder = new CoordinateRuntime.Builder();

      // for each partition
      for (int partno = 0; partno < npart; partno++) {
        GribCollection.GroupHcs group = gp.componentGroups[partno];
        if (group == null) {  // missing group in this partition
          f.format(" INFO canonical group %s not in partition %s%n", gname, partitions.get(partno).getName());
          continue;
        }
        int groupIdx = gp.componentGroupIndex[partno];

        // keep track of all runtime coordinates across the partitions
        for (Coordinate coord : group.coords) {
          if (coord.getType() == Coordinate.Type.runtime)
            runtimeAllBuilder.addAll(coord);
        }

        // for each variable in this group/Partition, put reference to it in the VariableIndexPartitioned
        for (int varIdx = 0; varIdx < group.variList.size(); varIdx++) {
          GribCollection.VariableIndex vi = group.variList.get(varIdx);
          if (trace) f.format(" Check %s%n", vi.toStringShort());
          int flag = 0;
          PartitionCollection.VariableIndexPartitioned vip = (PartitionCollection.VariableIndexPartitioned) resultGroup.findVariableByHash(vi.cdmHash);
          vip.addPartition(partno, groupIdx, varIdx, flag, vi);

          /* PartitionCollection.VariableIndexPartitioned viResult = resultVarMap.get(viFromOtherPartition.cdmHash); // match with proto variable hash
          if (viResult == null) {
            f.format(" WARN Cant find %s from %s / %s in proto %s - add%n", viFromOtherPartition.toStringShort(), tpp.getName(), gdesc, canon.getName());

            //////////////// add it to the canonGroup
            viResult = result.makeVariableIndexPartitioned(resultGroup, viFromOtherPartition, npart);
            resultVarMap.put(viFromOtherPartition.cdmHash, viResult);
          } */

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

        } // loop over variable
      } // loop over partition

      // each VariableIndexPartitioned now has its list of  PartitionForVariable

      // we have a complete set of runtimes
      Coordinate runtimeCoord = runtimeAllBuilder.finish();
      resultGroup.coords.add(runtimeCoord);

      // for each run, which partition ??
      resultGroup.run2part = new int[runtimeCoord.getSize()];
      for (int partno = 0; partno < npart; partno++) {
        GribCollection.GroupHcs group = gp.componentGroups[partno];
        if (group == null) continue;
        for (Coordinate coord : group.coords) {
          if (coord.getType() == Coordinate.Type.runtime) {
            CoordinateRuntime runCoord = (CoordinateRuntime) coord;
            for (Object val : coord.getValues()) {
              int idx = runtimeCoord.getIndex(val);
              resultGroup.run2part[idx] = partno;     // note that later partitions will override earlier if they have the same runtime
            }
          }
        }
      }

      // overall set of unique coordinates
      boolean isDense = (config != null) && "dense".equals(config.getParameter("CoordSys"));
      CoordinateSharer sharify = new CoordinateSharer(!isDense);

      // for each variable, create union of coordinates across the partitions
      for (GribCollection.VariableIndex viResult : resultGroup.variList) {
        // loop over partitions, make union coordinate, time filter intervals
        CoordinateUnionizer unionizer = new CoordinateUnionizer(viResult.getVarid(), intvMap);
        for (int partno = 0; partno < npart; partno++) {
          GribCollection.GroupHcs group = gp.componentGroups[partno];
          if (group == null) continue; // tolerate missing groups
          GribCollection.VariableIndex vi = group.findVariableByHash(viResult.cdmHash);
          if (vi == null) continue; // tolerate missing variables
          unionizer.addCoords(vi.getCoordinates());
        }

        viResult.coords = unionizer.finish();
        sharify.addCoords(viResult.coords);
      }

      // create a list of common coordinates, put them into the group, and now variables just reference those by index
      sharify.finish();
      resultGroup.coords = sharify.getUnionCoords();
      // redo the variables against the shared coordinates
      for (GribCollection.VariableIndex viResult : resultGroup.variList) {
        viResult.coordIndex = sharify.reindex2shared(viResult.coords);
      }

      // figure out missing data for each variable in the twoD time array
      for (GribCollection.VariableIndex viResult : resultGroup.variList) {
        Coordinate cr = viResult.getCoordinate(Coordinate.Type.runtime);
        Coordinate ct = viResult.getCoordinate(Coordinate.Type.time);
        if (ct == null) ct = viResult.getCoordinate(Coordinate.Type.timeIntv);

        if (cr == null) {
          logger.error("Missing runtime coordinate vi="+viResult.toStringShort());
          continue;
        }
        if (ct == null) {
          logger.error("Missing time coordinate vi="+viResult.toStringShort());
          continue;
        }

        CoordinateTwoTimer twot = new CoordinateTwoTimer(cr.getSize(), ct.getSize());
        Map<Object, Integer> ctMap = new HashMap<>(2*ct.getSize());  // time coord val -> index in ct
        for (int i=0; i<ct.getSize(); i++) ctMap.put(ct.getValue(i), i);

        // loop over runtimes
        int runIdx = 0;
        for (int partno : resultGroup.run2part) {
          //PartitionCollection.Partition tpp = partitions.get(partno);
          //GribCollection.Dataset ds = tpp.gc.getDataset2D();
          //GribCollection.GroupHcs group = ds.findGroupById(resultGroup.getId());

          // get the partition/group/variable for this run
          GribCollection.GroupHcs group = gp.componentGroups[partno];
          if (group == null) continue; // tolerate missing groups
          GribCollection.VariableIndex vi = group.findVariableByHash(viResult.cdmHash);
          if (vi == null) continue; // tolerate missing variables

          Coordinate ctP = vi.getCoordinate(Coordinate.Type.time);
          if (ctP == null) ctP = vi.getCoordinate(Coordinate.Type.timeIntv);

          // we need the sparse array for this component vi
          vi.readRecords();
          SparseArray<GribCollection.Record> sa = vi.getSparseArray();
          Section s = new Section(sa.getShape());
          Section.Iterator iter = s.getIterator(sa.getShape());

          // run through all the inventory in this component vi
          int[] index = new int[sa.getRank()];
          while (iter.hasNext()) {
            int linearIndex = iter.next(index);
            if (sa.getContent(linearIndex) == null) continue;

            // convert value in component vi to index in result
            int timeIdxP = index[1];
            Object val = ctP.getValue(timeIdxP);
            Integer timeIdxR = ctMap.get(val);
            if (timeIdxR != null) {
              twot.add(runIdx, timeIdxR);
            }
          }

          runIdx++;
        }

        viResult.twot = twot;
        viResult.isTwod = true;

        /* Formatter ff = new Formatter(System.out);
        ff.format("Variable %s%n", viResult.toStringShort());
        twot.showMissing(ff); */
      }

    } // loop over groups

    return ds2D;
  }

  private boolean makeDatasetBest(GribCollection.Dataset ds2D, Formatter f) throws IOException {
    GribCollection.Dataset dsa = result.makeDataset(GribCollectionProto.Dataset.Type.Best);

    int npart = result.getPartitions().size();
    boolean ok = true;

     // do each group
    for (GribCollection.GroupHcs group2D : ds2D.groups) {
      GribCollection.GroupHcs groupB = dsa.addGroupCopy(group2D);  // make copy of group, add to dataset
      groupB.run2part = group2D.run2part;                          // use same run -> partition map

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
          CoordinateTime timeBest = (CoordinateTime) groupB.coords.get(timeIdx); // LOOK do we know these have same index ??
          vip.time2runtime = makeTime2RuntimeMap(runOffset, time2d, timeBest, ((PartitionCollection.VariableIndexPartitioned) vi2d).twot);

        } else {
          timeIdx = vip.getCoordinateIndex(Coordinate.Type.timeIntv);
          CoordinateTimeIntv time2d = (CoordinateTimeIntv) group2D.coords.get(timeIdx);
          CoordinateTimeIntv timeBest = (CoordinateTimeIntv) groupB.coords.get(timeIdx);
          vip.time2runtime = makeTime2RuntimeMap(runOffset, time2d, timeBest, ((PartitionCollection.VariableIndexPartitioned) vi2d).twot);
        }

        // do not remove runtime coordinate, just set isTwoD
        vip.isTwod = false;

        /* remove runtime coordinate from list
        List<Integer> result = new ArrayList<>();
        for (Integer idx : vip.coordIndex) {
          Coordinate c = groupB.coords.get(idx);
          if (c.getType() != Coordinate.Type.runtime) result.add(idx);
        }
        vip.coordIndex = result; */
      }

    } // loop over groups

    return ok;
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

        repeated Parameter params = 20;      // not used yet

        extensions 100 to 199;
      }

      extend GribCollection {
        repeated Partition partitions = 100;
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

      indexBuilder.setCenter(pc.getCenter());
      indexBuilder.setSubcenter(pc.getSubcenter());
      indexBuilder.setMaster(pc.getMaster());
      indexBuilder.setLocal(pc.getLocal());

      indexBuilder.setGenProcessId(pc.getGenProcessId());
      indexBuilder.setGenProcessType(pc.getGenProcessType());
      indexBuilder.setBackProcessId(pc.getBackProcessId());

      //gds
      for (GribCollection.HorizCoordSys hcs : pc.horizCS)
        indexBuilder.addGds(writeGdsProto(hcs));

      // dataset
      for (GribCollection.Dataset ds : pc.datasets)
        indexBuilder.addDataset(writeDatasetProto(pc, ds));

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
  message Dataset {
    required Type type = 1;
    repeated Group groups = 2;
  }
   */
  private GribCollectionProto.Dataset writeDatasetProto(PartitionCollection pc, PartitionCollection.Dataset ds) throws IOException {
    GribCollectionProto.Dataset.Builder b = GribCollectionProto.Dataset.newBuilder();

    b.setType(ds.type);

    for (GribCollection.GroupHcs group : ds.groups)
      b.addGroups(writeGroupProto(pc, group));

    return b.build();
  }

  /*
   message Group {
    required uint32 gdsIndex = 1;       // index into GribCollection.gds array
    repeated Variable variables = 2;    // list of variables
    repeated Coord coords = 3;          // list of coordinates
    repeated int32 fileno = 4;          // the component files that are in this group, index into gc.mfiles

    repeated Parameter params = 20;      // not used yet
    extensions 100 to 199;
   }
    extend Group {
      repeated uint32 run2part = 100;       // runtime index to partition index
    }
   */
  private GribCollectionProto.Group writeGroupProto(PartitionCollection pc, GribCollection.GroupHcs g) throws IOException {
    GribCollectionProto.Group.Builder b = GribCollectionProto.Group.newBuilder();

    b.setGdsIndex(pc.findHorizCS(g.horizCoordSys));

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

    if (g.filenose != null)
      for (Integer fileno : g.filenose)
        b.addFileno(fileno);


    // extensions
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
     required bytes pds = 2;          // raw pds
     required fixed32 cdmHash = 3;

     required uint64 recordsPos = 4;  // offset of SparseArray message for this Variable
     required uint32 recordsLen = 5;  // size of SparseArray message for this Variable

     repeated uint32 coordIdx = 6;    // indexes into Group.coords

     // optionally keep stats
     optional float density = 7;
     optional uint32 ndups = 8;
     optional uint32 nrecords = 9;
     optional uint32 missing = 10;

     repeated uint32 invCount = 15;      // for Coordinate TwoTimer, only 2D vars
     repeated uint32 time2runtime = 16;  // time index to runtime index, only 1D vars
     repeated Parameter params = 20;    // not used yet

     extensions 100 to 199;
   }
  extend Variable {
    repeated PartitionVariable partition = 100;
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

    if (vp.twot != null) { // only for 2D
      for (int invCount : vp.twot.getCount())
        b.addInvCount(invCount);
    }

    if (vp.time2runtime != null) { // only for 1D
      for (int idx : vp.time2runtime)
        b.addTime2Runtime(idx);
    }

        // extensions
    List<PartitionCollectionProto.PartitionVariable> pvarList = new ArrayList<>();
    for (PartitionCollection.PartitionForVariable pvar : vp.partList)
      pvarList.add(writePartitionVariableProto(pvar));
    b.setExtension(PartitionCollectionProto.partition, pvarList);

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

  protected GribCollectionProto.Parameter writeParamProto(Parameter param) throws IOException {
    GribCollectionProto.Parameter.Builder b = GribCollectionProto.Parameter.newBuilder();

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
