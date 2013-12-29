package ucar.nc2.grib.collection;

import com.google.protobuf.ByteString;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MCollection;
import thredds.inventory.partition.PartitionManager;
import ucar.ma2.Section;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
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

  // called by tdm: update partition, test children partitions
  static public boolean update(PartitionManager tpc, org.slf4j.Logger logger) throws IOException {
    Grib2PartitionBuilder builder = new Grib2PartitionBuilder(tpc.getCollectionName(), new File(tpc.getRoot()), tpc, logger);
    if (!builder.needsUpdate()) return false;
    builder.readOrCreateIndex(CollectionUpdateType.always, CollectionUpdateType.test, null);
    builder.gc.close();
    return true;
  }

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
    PartitionCollection.Dataset ds = result.makeDataset(PartitionCollectionProto.Dataset.Type.TwoD);
    if (!makeVariables2D(canon, ds, errlog)) {
      errlog.format(" ERR Partition check failed, index not written on %s%n", result.getName());
      logger.error(" Partition check failed, index not written on {} errors = \n{}", result.getName(), errlog.toString());
      return false;
    }

    // this finishes the 2D stuff
    result.finish();

    // makeBest(errlog);

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

  private boolean makeVariables2D(PartitionCollection.Partition canon, PartitionCollection.Dataset ds2D, Formatter f) throws IOException {
    FeatureCollectionConfig.GribConfig config = (FeatureCollectionConfig.GribConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    FeatureCollectionConfig.GribIntvFilter intvMap = (config != null) ? config.intvFilter : null;
    /* if (intvMap != null && filterOut(gr, intvMap)) {
      statsAll.filter++;
      continue; // skip
    } */

    List<PartitionCollection.Partition> partitions = result.getPartitions();
    int npart = partitions.size();
    boolean ok = true;

    ds2D.groups = new ArrayList<>();

     // do each group
    GribCollection canonGc = canon.gc;
    for (GribCollection.GroupHcs canonGroup2 : canonGc.groups) {
      // make copy of group, dont modify canonGroup
      GribCollection.GroupHcs resultGroup = canon.gc.new GroupHcs(canonGroup2);
      ds2D.groups.add(resultGroup);

      String gname = canonGroup2.getId();
      String gdesc = canonGroup2.getDescription();
      if (trace) f.format(" Check Group %s%n", gname);

      // hash proto variables for quick lookup
      Map<Integer, PartitionCollection.VariableIndexPartitioned> resultVarMap = new HashMap<>(2 * canonGroup2.variList.size());
      List<GribCollection.VariableIndex> resultVarList = new ArrayList<>(canonGroup2.variList.size());
      for (GribCollection.VariableIndex vi : canonGroup2.variList) {
        // convert each VariableIndex to VariableIndexPartitioned
        PartitionCollection.VariableIndexPartitioned vip = result.makeVariableIndexPartitioned(resultGroup, vi, npart);
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

      // each VariableIndexPartitioned now has its list of  PartitionForVariable

      Coordinate runtimeCoord = runtimeAllBuilder.finish();
      resultGroup.run2part = new ArrayList<>(runtimeCoord.getSize());
      for (int i = 0; i < resultGroup.run2part.size(); i++)
        resultGroup.run2part.set(i,i); // LOOK wrong
      resultGroup.coords = new ArrayList<>();
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

        Formatter ff = new Formatter(System.out);
        ff.format("Variable %s%n", viResult.toStringShort());
        twot.showMissing(ff);
      }

    } // loop over groups

    return ok;
  }

    /* true means remove
  private boolean filterOut(Grib2Record gr, FeatureCollectionConfig.GribIntvFilter intvFilter) {
    int[] intv = tables.getForecastTimeIntervalOffset(gr);
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
      return intvFilter.filterOut(id, haveLength, prob);
    }
    return false;
  }  */


  /* private PartitionCollection.Dataset makeBest(Grib2Partition partition, Formatter f) throws IOException {
    PartitionCollection.Dataset best = partition.makeDataset(PartitionCollectionProto.Dataset.Type.Best);

    List<PartitionCollection.Partition> partitions = result.getPartitions();
    int npart = partitions.size();
    boolean ok = true;

    // do each group
    for (GribCollection.GroupHcs groupAll : partition.getGroups()) {
      GribCollection.GroupHcs bestGroup = best.addGroup(groupAll);
      bestGroup.coords = new ArrayList<>();

      List<Coordinate> timeCoords = new ArrayList<>();

      for (Coordinate coord : groupAll.coords) {
        // for each time, timeIntv, make a "best"
        Coordinate bestCoord = new CoordinateTime();
        timeCoords.add(bestCoord);
      }

      for (GribCollection.VariableIndex vi : groupAll.variList) {
        Coordinate viTime = vi.getCoordinate(Coordinate.Type.time);
        Coordinate viRuntime = vi.getCoordinate(Coordinate.Type.runtime);
        CoordinateTwoDTime twodTime = new CoordinateTwoDTime(viRuntime, viTime);

        // loop over partitions, make union coordinate
        for (int partno = 0; partno < npart; partno++) {
          PartitionCollection.Partition tpp = partitions.get(partno);
          GribCollection.GroupHcs group = tpp.gc.findGroupById(groupAll.getId());
          if (group == null) continue; // tolerate missing groups
          GribCollection.VariableIndex viPart = group.findVariableByHash(vi.cdmHash);
          if (viPart == null) continue; // tolerate missing variables
          twodTime.addCoords(vi.getCoordinates());
        }
        viResult.coords = twodTime.finish();
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

    return ok;
  }   */


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
    for (int part : g.run2part) run2partList.add(part);
    b.setExtension(PartitionCollectionProto.run2Part, run2partList);

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
