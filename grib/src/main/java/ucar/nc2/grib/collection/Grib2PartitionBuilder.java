package ucar.nc2.grib.collection;

import com.google.protobuf.ByteString;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionManager;
import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import thredds.inventory.partition.PartitionManager;
import ucar.arr.Coordinate;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.*;
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
public class Grib2PartitionBuilder extends ucar.nc2.grib.collection.Grib2CollectionBuilder {
  public static final String MAGIC_START = "Grib2Partition0Index";
  static private final boolean trace = false;

  // called by tdm
  static public boolean update(PartitionManager tpc, org.slf4j.Logger logger) throws IOException {
    Grib2PartitionBuilder builder = new Grib2PartitionBuilder(tpc.getCollectionName(), new File(tpc.getRoot()), tpc, logger);
    if (!builder.needsUpdate()) return false;
    builder.readOrCreateIndex(CollectionManager.Force.always);
    builder.gc.close();
    return true;
  }

  // read in the index, create if it doesnt exist or is out of date
  static public Grib2Partition factory(PartitionManager tpc, CollectionManager.Force force, org.slf4j.Logger logger) throws IOException {
    Grib2PartitionBuilder builder = new Grib2PartitionBuilder(tpc.getCollectionName(), new File(tpc.getRoot()), tpc, logger);
    builder.readOrCreateIndex(force);
    return builder.tp;
  }

  // make the index
  static public boolean makePartitionIndex(PartitionManager tpc, Formatter errlog, org.slf4j.Logger logger) throws IOException {
    Grib2PartitionBuilder builder = new Grib2PartitionBuilder(tpc.getCollectionName(), new File(tpc.getRoot()), tpc, logger);
    builder.tp.close();
    return builder.createPartitionedIndex(errlog);
  }

  //////////////////////////////////////////////////////////////////////////////////

  private final PartitionManager tpc; // defines the partition
  private Grib2Partition tp;  // build this object

  protected Grib2PartitionBuilder(String name, File directory, PartitionManager tpc, org.slf4j.Logger logger) {
    super(tpc, logger);
    this.name = name;
    this.directory = directory;

    FeatureCollectionConfig.GribConfig config = null;
    if (tpc != null)
      config = (FeatureCollectionConfig.GribConfig) tpc.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    this.tp = new Grib2Partition(name, directory, config, logger);
    this.gc = tp;
    this.tpc = tpc;
  }

  private boolean readOrCreateIndex(CollectionManager.Force ff) throws IOException {
    File idx = gc.getIndexFile();

    // force new index or test for new index needed
    boolean force = ((ff == CollectionManager.Force.always) || (ff == CollectionManager.Force.test && needsUpdate(idx.lastModified())));

    // otherwise, we're good as long as the index file exists and can be read
    if (force || !idx.exists() || !readIndex(idx.getPath())) {
      logger.info("{}: createIndex {}", gc.getName(), idx.getPath());
      if (createPartitionedIndex(null)) {  // write index
        return readIndex(idx.getPath()); // read back in index
      }
    }
    return false;
  }

  // LOOK not sure if this works
  private boolean needsUpdate(long collectionLastModified) throws IOException {
    CollectionManager.ChangeChecker cc = Grib2Index.getChangeChecker();
    for (MCollection dcm : tpc.makePartitions()) { // LOOK not really right, since we dont know if these files are the same as in the index
      File idxFile = ucar.nc2.grib.GribCollection.getIndexFile(dcm);
      if (!idxFile.exists())
        return true;
      if (collectionLastModified < idxFile.lastModified())
        return true;
      try (CloseableIterator<MFile> iter = dcm.getFileIterator()) {
        while (iter.hasNext()) {
          if (cc.hasChangedSince(iter.next(), idxFile.lastModified())) return true;
        }
      }

    }
    return false;
  }

  private boolean readIndex(String filename) throws IOException {
    return readIndex(new RandomAccessFile(filename, "r"));
  }

  private boolean readIndex(RandomAccessFile indexRaf) throws IOException {
    try {
      this.tp = Grib2PartitionBuilderFromIndex.createTimePartitionFromIndex(this.name, this.directory, indexRaf, gc.getGribConfig(), logger);
      this.gc = tp;
      return true;
    } catch (IOException ioe) {
      return false;
    }
  }

  ///////////////////////////////////////////////////
  // create the index

  public boolean createPartitionedIndex(Formatter f) throws IOException {
    long start = System.currentTimeMillis();
    if (f == null) f = new Formatter(); // info will be discarded

    // create partitions
    for (MCollection dcm : tpc.makePartitions()) {
      tp.addPartition(dcm);
    }

    List<PartitionCollection.Partition> bad = new ArrayList<>();
    for (PartitionCollection.Partition tpp : tp.getPartitions()) {
      try {
        tpp.gc = tpp.makeGribCollection(CollectionManager.Force.nocheck);    // use index if it exists  LOOK force ??
        if (trace) logger.debug(" Open partition {}", tpp.getDcm().getCollectionName());
      } catch (Throwable t) {
        logger.error(" Failed to open partition " + tpp.getName(), t);
        bad.add(tpp);
      }
    }

    // remove ones that failed
    for (PartitionCollection.Partition tpp : bad)
      tp.removePartition(tpp);

    // choose the "canonical" partition, aka prototype
    int n = tp.getPartitions().size();
    if (n == 0) {
      f.format("ERR Nothing in this partition = %s%n", tp.getName());
      logger.error(" Nothing in this partition = {}", tp.getName());
      return false;
    }
    int idx = tpc.getProtoIndex(n);
    PartitionCollection.Partition canon = tp.getPartitions().get(idx);
    f.format(" INFO Using canonical partition %s%n", canon.getDcm().getCollectionName());
    logger.info("     Using canonical partition {}", canon.getDcm().getCollectionName());

    // alternately one could choose a partition that has all the groups or maximum # variables
    // checkGroups(canon, f, true);

    // check consistency across vert and ens coords
    // also replace variables  in canonGc with partitioned variables
    // partition index is used - do not resort partitions
    if (!comparePartitions(canon, f)) {
      f.format(" ERR Partition check failed, index not written on %s%n", tp.getName());
      logger.error(" Partition check failed, index not written on {} errors = \n{}", tp.getName(), f.toString());
      return false;
    }

    // make the time coordinates, place results into canon
    createPartitionedTimeCoordinates(canon.gc, f);

    // ready to write the index file
    writeIndex(canon.gc, f);

    // close open gc's
    for (PartitionCollection.Partition tpp : tp.getPartitions()) {
      tpp.gc.close();
    }
    canon.gc.close();

    long took = System.currentTimeMillis() - start;
    f.format(" INFO CreatePartitionedIndex took %d msecs%n", took);
    return true;
  }

  /* private class Groups {
    GribCollection.GroupHcs g;
    List<TimePartition.Partition> parts = new ArrayList<>();
  }

  private void checkGroups(TimePartition.Partition canon, Formatter f, boolean show) throws IOException {
    List<TimePartition.Partition> partitions = tp.getPartitions();

    Map<Integer, Groups> map = new HashMap<>(100);

    for (TimePartition.Partition tpp : partitions) {
      GribCollection gc = tpp.gc;
      for (GribCollection.GroupHcs g : gc.getGroups()) {
        Groups gs = map.get(g.gdsHash);
        if (gs == null) {
          gs = new Groups();
          map.put(g.gdsHash, gs);
        }
        gs.parts.add(tpp);
      }
    }
  }   */

  // consistency check on variables : compare each variable to corresponding one in proto
  // also set the groupno and partno for each partition
  private boolean comparePartitions(PartitionCollection.Partition canon, Formatter f) throws IOException {
    List<PartitionCollection.Partition> partitions = tp.getPartitions();
    int npart = partitions.size();
    boolean ok = true;

    // for each group in canonical Partition
    GribCollection canonGc = canon.gc;
    for (GribCollection.GroupHcs canonGroup : canonGc.getGroups()) {
      String gname = canonGroup.getId();
      String gdesc = canonGroup.getDescription();
      if (trace) f.format(" Check Group %s%n", gname);

      // hash proto variables for quick lookup
      Map<Integer, PartitionCollection.VariableIndexPartitioned> canonVars = new HashMap<>(2 * canonGroup.varIndex.size());
      List<GribCollection.VariableIndex> varIndexP = new ArrayList<>(canonGroup.varIndex.size());
      for (GribCollection.VariableIndex vi : canonGroup.varIndex) {
        PartitionCollection.VariableIndexPartitioned vip = tp.makeVariableIndexPartitioned(vi, npart);
        varIndexP.add(vip);
        if (canonVars.containsKey(vi.cdmHash)) {
          f.format(" WARN Duplicate variable hash %s%n", vi.toStringShort());
        }
        canonVars.put(vi.cdmHash, vip);
      }
      canonGroup.varIndex = varIndexP;// replace variable list with VariableIndexPartitioned in the group

      // for each partition
      for (int partno = 0; partno < npart; partno++) {
        PartitionCollection.Partition tpp = partitions.get(partno);
        if (trace) f.format(" Check Partition %s%n", tpp.getName());

        // get group corresponding to canonGroup
        GribCollection gc = tpp.gc;
        int groupIdx = gc.findGroupIdxById(canonGroup.getId());
        if (groupIdx < 0) {
          f.format(" INFO Cant find canonical group %s in partition %s%n", gname, tpp.getName());
          //ok = false;
          continue;
        }
        GribCollection.GroupHcs group = gc.getGroup(groupIdx); // note this will be the same as the canonGroup when partition == canonPartition

        // for each variable in otherPartition group
        for (int varIdx = 0; varIdx < group.varIndex.size(); varIdx++) {
          GribCollection.VariableIndex viFromOtherPartition = group.varIndex.get(varIdx);
          if (trace) f.format(" Check %s%n", viFromOtherPartition.toStringShort());
          int flag = 0;

          PartitionCollection.VariableIndexPartitioned viCanon = canonVars.get(viFromOtherPartition.cdmHash); // match with proto variable hash
          if (viCanon == null) {
            f.format(" WARN Cant find %s from %s / %s in proto %s - add to canon%n", viFromOtherPartition.toStringShort(), tpp.getName(), gdesc, canon.getName());

            //////////////// add it to the canonGroup
            viCanon = tp.makeVariableIndexPartitioned(viFromOtherPartition, npart);
            varIndexP.add(viCanon); // this is the canonGorup list of vars
            if (canonVars.containsKey(viFromOtherPartition.cdmHash))
              f.format(" WARN Duplicate variable hash %s%n", viFromOtherPartition.toStringShort());
            canonVars.put(viFromOtherPartition.cdmHash, viCanon); // replace with its evil twin

            // reparent its vertical coordinate
            VertCoord vc = viCanon.getVertCoord();
            if (vc != null) {
              viCanon.vertIdx = VertCoord.findCoord(canonGroup.vertCoords, vc); // share coordinates or add
            }
            // reparent its ens coordinate
            EnsCoord ec = viCanon.getEnsCoord();
            if (ec != null) {
              viCanon.ensIdx = EnsCoord.findCoord(canonGroup.ensCoords, ec); // share coordinates or add
            }
            // i think you dont need to reparent time coordinate because those are made into timePartition coordidates
          }

          //compare vert coordinates
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
          }

          viCanon.setPartitionIndex(partno, groupIdx, varIdx, flag);
        } // loop over variable
      } // loop over partition
    } // loop over group

    if (ok) {
      f.format(" INFO Partition check OK%n");
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

  private boolean createPartitionedTimeCoordinates(GribCollection canonGc, Formatter f) throws IOException {
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
      } */

      // store results in first group
      canonGroup.timeCoordPartitions = unionList;
    }

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
  private boolean writeIndex(GribCollection canonGc, Formatter f) throws IOException {
    File file = tp.getIndexFile();
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
      indexBuilder.setName(tp.getName());

      for (GribCollection.GroupHcs g : canonGc.getGroups())
        indexBuilder.addGroups(writeGroupProto(g));

      indexBuilder.setCenter(canonGc.getCenter());
      indexBuilder.setSubcenter(canonGc.getSubcenter());
      indexBuilder.setMaster(canonGc.getMaster());
      indexBuilder.setLocal(canonGc.getLocal());
      indexBuilder.setTopDir(gc.getDirectory().getPath());

      // dont need files - these are stored in the partition objects

      for (PartitionCollection.Partition p : tp.getPartitions()) {
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

    b.setGds(ByteString.copyFrom(g.rawGds));
    b.setGdsHash(g.gdsHash);

    for (GribCollection.VariableIndex vb : g.varIndex) {
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

    for (int i = 0; i < g.timeCoordPartitions.size(); i++)
      b.addTimeCoordUnions(writeTimeCoordUnionProto(g.timeCoordPartitions.get(i), i));

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

    for (int idx : vp.groupno)
      b.addGroupno(idx);
    for (int idx : vp.varno)
      b.addVarno(idx);
    for (int idx : vp.flag)
      b.addFlag(idx);

    return b.build();
  }

  protected GribCollectionProto.TimeCoordUnion writeTimeCoordUnionProto(TimeCoordUnion tcu, int index) throws IOException {
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
  }

  private GribCollectionProto.Partition writePartitionProto(String name, PartitionCollection.Partition p) throws IOException {
    GribCollectionProto.Partition.Builder b = GribCollectionProto.Partition.newBuilder();

    b.setFilename(p.getIndexFilename());
    b.setName(name);
    if (p.getLastModified() > 0)
      b.setLastModified(p.getLastModified());

    return b.build();
  }
}
