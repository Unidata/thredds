package ucar.nc2.grib.collection;

import com.google.protobuf.ByteString;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionManager;
import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import thredds.inventory.partition.PartitionManager;
import ucar.sparr.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.grib2.Grib2Index;
import ucar.nc2.stream.NcStream;
import ucar.nc2.util.CloseableIterator;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Description
 *
 * @author John
 * @since 12/7/13
 */
public class Grib2PartitionBuilder extends Grib2CollectionBuilder {
  public static final String MAGIC_START = "Grib2Partition0Index";
  static private final boolean trace = false;

  // called by tdm: update partition, test children partitions
  static public boolean update(PartitionManager tpc, org.slf4j.Logger logger) throws IOException {
    Grib2PartitionBuilder builder = new Grib2PartitionBuilder(tpc.getCollectionName(), new File(tpc.getRoot()), tpc, logger);
    if (!builder.needsUpdate()) return false;
    builder.readOrCreateIndex(CollectionManager.Force.always, CollectionManager.Force.test, null);
    builder.gc.close();
    return true;
  }

  // read in the index, create if it doesnt exist or is out of date (depends on force)
  static public Grib2Partition factory(PartitionManager tpc, CollectionManager.Force forcePartition, CollectionManager.Force forceChildren,
                                       Formatter errlog, org.slf4j.Logger logger) throws IOException {
    Grib2PartitionBuilder builder = new Grib2PartitionBuilder(tpc.getCollectionName(), new File(tpc.getRoot()), tpc, logger);
    builder.readOrCreateIndex(forcePartition, forceChildren, errlog);
    return builder.result;
  }

  // recreate if partition doesnt exist or is out of date (depends on force). return true if it was recreated
  static public boolean recreateIfNeeded(PartitionManager tpc, CollectionManager.Force forcePartition, CollectionManager.Force forceChildren,
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
  private boolean readOrCreateIndex(CollectionManager.Force forcePartition, CollectionManager.Force forceChildren, Formatter errlog) throws IOException {
    File idx = gc.getIndexFile();

    // force new index or test for new index needed
    boolean force = ((forcePartition == CollectionManager.Force.always) || (forcePartition == CollectionManager.Force.test && needsUpdate(idx.lastModified())));

    // otherwise, we're good as long as the index file exists and can be read
    if (force || !idx.exists() || !readIndex(idx.getPath())) {
      if (forcePartition == CollectionManager.Force.never) throw new IOException("failed to read "+idx.getPath());

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
      this.result = Grib2PartitionBuilderFromIndex.createTimePartitionFromIndex(this.name, this.directory, indexRaf, gc.getGribConfig(), logger);
      this.gc = result;
      return true;
    } catch (IOException ioe) {
      return false;
    }
  }

  ///////////////////////////////////////////////////
  // create the index

  private boolean createPartitionedIndex(CollectionManager.Force forceChildren, Formatter errlog) throws IOException {
    long start = System.currentTimeMillis();
    if (errlog == null) errlog = new Formatter(); // info will be discarded

    // create partitions
    for (MCollection dcm : partitionManager.makePartitions()) {
      result.addPartition(dcm);
    }

    List<PartitionCollection.Partition> bad = new ArrayList<>();
    for (PartitionCollection.Partition tpp : result.getPartitions()) {
      try {
        tpp.gc = tpp.makeGribCollection(forceChildren);                     // here we read in all collections at once. can we avoid this?
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
    result.set(canon.gc);

    // alternately one could choose a partition that has all the groups or maximum # variables
    // List<GroupAndPartitions> groupList = makeAllGroups();

    // check consistency across vert and ens coords
    // also replace variables  in canonGc with partitioned variables
    // partition index is used - do not resort partitions
    if (!makeAllVariables(canon, errlog)) {
      errlog.format(" ERR Partition check failed, index not written on %s%n", result.getName());
      logger.error(" Partition check failed, index not written on {} errors = \n{}", result.getName(), errlog.toString());
      return false;
    }

    // make the time coordinates, place results into canon
    //createPartitionedTimeCoordinates(canon.gc, f);

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

  // LOOK use this group list to make sure all groups are in result
  // could do same for all vars - eg dont have canon
  private List<GroupPartitions> makeAllGroups() throws IOException {
    Map<Integer, GroupPartitions> groupMap = new HashMap<>(40);
    for (PartitionCollection.Partition tpp : result.getPartitions()) {
      GribCollection gc = tpp.gc;
      for (GribCollection.GroupHcs g : gc.getGroups()) {
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

  private boolean makeAllVariables(PartitionCollection.Partition canon, Formatter f) throws IOException {
    List<PartitionCollection.Partition> partitions = result.getPartitions();
    int npart = partitions.size();
    boolean ok = true;

    // do each group
    GribCollection canonGc = canon.gc;
    for (GribCollection.GroupHcs canonGroup2 : canonGc.getGroups()) {
      GribCollection.GroupHcs resultGroup = canon.gc.new GroupHcs(canonGroup2);
      result.groups.add(resultGroup);       // use copy of group, dont modify canonGroup

      String gname = canonGroup2.getId();
      String gdesc = canonGroup2.getDescription();
      if (trace) f.format(" Check Group %s%n", gname);

      // hash proto variables for quick lookup
      Map<Integer, PartitionCollection.VariableIndexPartitioned> resultVarMap = new HashMap<>(2 * canonGroup2.variList.size());
      List<GribCollection.VariableIndex> resultVarList = new ArrayList<>(canonGroup2.variList.size());
      for (GribCollection.VariableIndex vi : canonGroup2.variList) {
        PartitionCollection.VariableIndexPartitioned vip = result.makeVariableIndexPartitioned(vi, npart);
        resultVarList.add(vip);
        if (resultVarMap.containsKey(vi.cdmHash)) {
          f.format(" ERR Duplicate variable hash %s%n", vi.toStringShort());
          logger.error("Duplicate variable hash " + vi.toStringShort());
        }
        resultVarMap.put(vi.cdmHash, vip);
      }
      resultGroup.variList = resultVarList;

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
        GribCollection.GroupHcs group = gc.getGroup(groupIdx); // note this will be the same as the canonGroup when partition == canonPartition

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
            viResult = result.makeVariableIndexPartitioned(viFromOtherPartition, npart);
            resultVarList.add(viResult);
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

      Coordinate runtimeCoord = runtimeAllBuilder.finish();
      resultGroup.run2part = new int[runtimeCoord.getSize()];
      for (int i=0; i<resultGroup.run2part.length; i++) resultGroup.run2part[i] = i; // LOOK wrong
      resultGroup.coords = new ArrayList<>();
      resultGroup.coords.add(runtimeCoord);

      // overall set of unique coordinates
      CoordinateUniquify uniquify = new CoordinateUniquify();

      // for each variable, create union of coordinates
      for (GribCollection.VariableIndex viResult : resultGroup.variList) {

        // loop over partitions, make union coordinate
        CoordinateUnionizer unionizer = new CoordinateUnionizer();
        for (int partno = 0; partno < npart; partno++) {
          PartitionCollection.Partition tpp = partitions.get(partno);
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

        // calc size / runtime
        int totalSize = 1;
        for (int idx : viResult.coordIndex)
          totalSize *= resultGroup.coords.get(idx).getSize();
        viResult.setTotalSize(totalSize, totalSize / runtimeCoord.getSize());
       }

    } // loop over groups

    if (ok) {
      return true;
    } else {
      return false;
    }
  }

  private class PartGroup {
    GribCollection.GroupHcs group;
    PartitionCollection.Partition tpp;

    private PartGroup(GribCollection.GroupHcs group, PartitionCollection.Partition tpp) {
      this.group = group;
      this.tpp = tpp;
    }
  }

  /* private boolean createPartitionedTimeCoordinates(GribCollection canonGc, Formatter f) throws IOException {
    List<PartitionCollection.Partition> partitions = tp.getPartitions();
    boolean ok = true;

    // for each group in canonical Partition
    for (GribCollection.GroupHcs canonGroup : canonGc.getGroups()) {
      String gname = canonGroup.getId();
      if (trace) f.format(" Check Group %s%n", gname);

      // get list of corresponding groups from all the time partition, so we dont have to keep looking it up
      List<PartGroup> partGroups = new ArrayList<>(partitions.size());
      for (PartitionCollection.Partition tpp : partitions) {
        GribCollection.GroupHcs gg = tpp.gc.findGroupById(gname);
        if (gg == null) {
          f.format(" INFO TimeCoordinates: Cant find canonical group %s in partition %s%n", gname, tpp.getName());
          // logger.info(" TimeCoordinates: Cant find canonical  group {} in partition {}", gname, tpp.getName());
          continue;
        } else {
          partGroups.add(new PartGroup(gg, tpp));
        }
      }

      // unique time coordinate unions
      List<TimeCoordUnion> unionList = new ArrayList<>();

      // for each variable in canonical Partition
      for (GribCollection.VariableIndex viCanon : canonGroup.varIndex) {
        if (trace) f.format(" Check variable %s%n", viCanon.toStringShort());
        TimeCoord tcCanon = viCanon.getTimeCoord();

        List<TimeCoord> tcPartitions = new ArrayList<>(partGroups.size()); // access by index position

        // for each partition, get the time index
        for (PartGroup pg : partGroups) {
          // get corresponding variable in this partition
          GribCollection.VariableIndex vi2 = pg.group.findVariableByHash(viCanon.cdmHash);
          if (vi2 == null) {  // apparently not in the partition - asssume this is ok
            f.format(" INFO TimeCoordinates: Missing %s in partition %s / %s%n", viCanon.toStringShort(), pg.tpp.getName(), gname);
            tcPartitions.add(null);
          } else {
            if (vi2.timeIdx < 0 || vi2.timeIdx >= vi2.group.timeCoords.size()) { // hy would this ever happen ??
              f.format(" ERR TimeCoordinates: timeIdx out of range var= %s on partition %s%n", vi2.toStringShort(), pg.tpp.getName());
              logger.error(" timeIdx out of range var= {} on partition {}", vi2.toStringShort(), pg.tpp.getName());
              tcPartitions.add(null);
            } else {
              TimeCoord tc2 = vi2.getTimeCoord();
              if (tc2.isInterval() != tcCanon.isInterval()) {
                f.format(" ERR TimeCoordinates: timeIdx wrong interval type var= %s on partition %s%n", vi2.toStringShort(), pg.tpp.getName());
                logger.error(" timeIdx wrong interval type var= {} on partition {}", vi2.toStringShort(), pg.tpp.getName());
                tcPartitions.add(null);
              } else {
                tcPartitions.add(tc2);
              }
            }
          }
        }

        // union of time coordinates
        TimeCoordUnion union = new TimeCoordUnion(tcCanon.getCode(), tcPartitions, tcCanon);

        // store result in the first group
        viCanon.partTimeCoordIdx = TimeCoordUnion.findUnique(unionList, union); // this merges identical TimeCoordUnion
      }

      /* turn TimeIndex into TimeCoord
      for (int tidx = 0; tidx <unionList.size(); tidx++) {
        TimeCoordUnion union = unionList.get(tidx);
        f.format(" %s %d: timeIndexList=", firstGroup.hcs.getName(), tidx);
        for (int idx : union.) f.format("%d,",idx);
        f.format("%n");
      }

      // store results in first group
      canonGroup.timeCoordPartitions = unionList;
    }

    return ok;
  } */

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
  private boolean writeIndex(GribCollection canonGc, Formatter f) throws IOException {
    File file = result.getIndexFile();
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

      GribCollectionProto.GribCollectionIndex.Builder indexBuilder = GribCollectionProto.GribCollectionIndex.newBuilder();
      indexBuilder.setName(result.getName());

      for (GribCollection.GroupHcs g : canonGc.getGroups())
        indexBuilder.addGroups(writeGroupProto(g));

      indexBuilder.setCenter(canonGc.getCenter());
      indexBuilder.setSubcenter(canonGc.getSubcenter());
      indexBuilder.setMaster(canonGc.getMaster());
      indexBuilder.setLocal(canonGc.getLocal());
      indexBuilder.setTopDir(gc.getDirectory().getPath());

      // dont need files - these are stored in the partition objects

      for (PartitionCollection.Partition p : result.getPartitions()) {
        indexBuilder.addPartitions(writePartitionProto(p.getName(), p));
      }

      GribCollectionProto.GribCollectionIndex index = indexBuilder.build();
      byte[] b = index.toByteArray();
      NcStream.writeVInt(raf, b.length); // message size
      raf.write(b);  // message  - all in one gulp
      f.format("Grib2PartitionIndex= %d bytes file size =  %d bytes%n%n", b.length, raf.length());

    }

    return true;
  }

  private GribCollectionProto.Group writeGroupProto(GribCollection.GroupHcs g) throws IOException {
    GribCollectionProto.Group.Builder b = GribCollectionProto.Group.newBuilder();

    b.setGds(ByteString.copyFrom(g.horizCoordSys.getRawGds()));
    b.setGdsHash(g.horizCoordSys.getGdsHash());

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

    for (int i = 0; i < g.run2part.length; i++)
      b.addRun2Part(g.run2part[i]);

    return b.build();
  }

  private GribCollectionProto.Variable writeVariableProto(PartitionCollection.VariableIndexPartitioned vp) throws IOException {
    GribCollectionProto.Variable.Builder b = GribCollectionProto.Variable.newBuilder();

    b.setDiscipline(vp.discipline);
    b.setPds(ByteString.copyFrom(vp.rawPds));
    b.setCdmHash(vp.cdmHash);

    b.setRecordsPos(vp.recordsPos);
    b.setRecordsLen(vp.recordsLen);

    for (int idx : vp.coordIndex)
      b.addCoordIdx(idx);

    for (PartitionCollection.PartVar pvar : vp.partList) {
      GribCollectionProto.PartVar.Builder pb = GribCollectionProto.PartVar.newBuilder();
      pb.setPartno(pvar.partno);
      pb.setGroupno(pvar.groupno);
      pb.setVarno(pvar.varno);
      pb.setFlag(pvar.flag);
      pb.setNdups(pvar.ndups);
      pb.setNrecords(pvar.nrecords);
      pb.setMissing(pvar.missing);
      pb.setDensity(pvar.density);
      b.addPartition(pb);
    }

    b.setDensity(vp.density);
    b.setNdups(vp.ndups);
    b.setNrecords(vp.nrecords);
    b.setMissing(vp.missing);

    return b.build();
  }

  /* protected GribCollectionProto.TimeCoordUnion writeTimeCoordUnionProto(TimeCoordUnion tcu, int index) throws IOException {
    GribCollectionProto.TimeCoordUnion.Builder b = GribCollectionProto.TimeCoordUnion.newBuilder();
    b.setCode(index);
    b.setUnit(tcu.getUnits());

    if (tcu.isInterval()) {
      for (TimeCoord.Tinv tinv : tcu.getIntervals()) {
        b.addValues((float) tinv.getBounds1());
        b.addBound((float) tinv.getBounds2());
      }
    } else {
      for (int value : tcu.getCoords())
        b.addValues((float) value);
    }
    for (TimeCoordUnion.Val val : tcu.getValues()) {
      b.addPartition(val.getPartition());
      b.addIndex(val.getIndex());
    }

    return b.build();
  } */

  private GribCollectionProto.Partition writePartitionProto(String name, PartitionCollection.Partition p) throws IOException {
    GribCollectionProto.Partition.Builder b = GribCollectionProto.Partition.newBuilder();

    b.setFilename(p.getIndexFilename());
    b.setName(name);
    if (p.getLastModified() > 0)
      b.setLastModified(p.getLastModified());

    return b.build();
  }
}
