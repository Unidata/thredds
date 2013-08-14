/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.grib1;

import com.google.protobuf.ByteString;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionManager;
import thredds.inventory.MFile;
import thredds.inventory.TimePartitionCollection;
import ucar.nc2.grib.*;
import ucar.nc2.stream.NcStream;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Builds Collections of Grib1 Time Partitioned.
 *
 * @author caron
 * @since 1/7/12
 */
public class Grib1TimePartitionBuilder extends Grib1CollectionBuilder {
  public static final String MAGIC_START = "Grib1Partition0Index";

  //static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1TimePartitionBuilder.class);
  //static private final int versionTP = 4;
  static private final boolean trace = false;

  // called by tdm
  static public boolean update(TimePartitionCollection tpc, org.slf4j.Logger logger) throws IOException {
    Grib1TimePartitionBuilder builder = new Grib1TimePartitionBuilder(tpc.getCollectionName(), new File(tpc.getRoot()), tpc, logger);
    if (!builder.needsUpdate()) return false;
    builder.readOrCreateIndex(CollectionManager.Force.always);
    builder.gc.close();
    return true;
  }

  // read in the index, create if it doesnt exist or is out of date
  static public Grib1TimePartition factory(TimePartitionCollection tpc, CollectionManager.Force force, org.slf4j.Logger logger) throws IOException {
    Grib1TimePartitionBuilder builder = new Grib1TimePartitionBuilder(tpc.getCollectionName(), new File(tpc.getRoot()), tpc, logger);
    builder.readOrCreateIndex(force);
    return builder.tp;
  }

  // read in the index, index raf already open
  static public Grib1TimePartition createFromIndex(String name, File directory, RandomAccessFile raf, org.slf4j.Logger logger) throws IOException {
    Grib1TimePartitionBuilder builder = new Grib1TimePartitionBuilder(name, directory, null, logger);
    if (builder.readIndex(raf)) {
      return builder.tp;
    }
    throw new IOException("Reading index failed");
  }

  /**
   * write new index if needed
   *
   * @param tpc use this collection
   * @param force force index
   * @return true if index was written
   * @throws IOException on error
   */
  static public boolean writeIndexFile(TimePartitionCollection tpc, CollectionManager.Force force, org.slf4j.Logger logger) throws IOException {
    Grib1TimePartitionBuilder builder = null;
    try {
      builder = new Grib1TimePartitionBuilder(tpc.getCollectionName(), new File(tpc.getRoot()), tpc, logger);
      return builder.readOrCreateIndex(force);

    } finally {
      if ((builder != null) && (builder.tp != null))
        builder.tp.close();
    }
  }

  //////////////////////////////////////////////////////////////////////////////////

  private final TimePartitionCollection tpc; // defines the partition
  private final Grib1TimePartition tp;  // build this object

  private Grib1TimePartitionBuilder(String name, File directory, TimePartitionCollection tpc, org.slf4j.Logger logger) {
    super(tpc, false, logger);
    FeatureCollectionConfig.GribConfig config = (tpc == null) ? null :  (FeatureCollectionConfig.GribConfig) tpc.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    this.tp = new Grib1TimePartition(name, directory, config, logger);
    this.gc = tp;
    this.tpc = tpc;
  }

  private boolean readOrCreateIndex(CollectionManager.Force ff) throws IOException {
    File idx = gc.getIndexFile();

     // force new index or test for new index needed
    boolean force = ((ff == CollectionManager.Force.always) || (ff == CollectionManager.Force.test && needsUpdate(idx.lastModified())));

    // otherwise, we're good as long as the index file exists and can be read
    if (force || !idx.exists() || !readIndex(idx.getPath()) )  {
      logger.info("{}: createIndex {}", gc.getName(), idx.getPath());
      createPartitionedIndex();   // write out index
      readIndex(idx.getPath()); // read back in index
      return true;
    }
    return false;
  }

  private boolean needsUpdate(long collectionLastModified) throws IOException {
    CollectionManager.ChangeChecker cc = Grib1Index.getChangeChecker();
    for (CollectionManager dcm : tpc.makePartitions()) { // LOOK not really right, since we dont know if these files are the same as in the index
      File idxFile = GribCollection.getIndexFile(dcm);
      if (!idxFile.exists()) return true;
      if (collectionLastModified < idxFile.lastModified()) return true;
      for (MFile mfile : dcm.getFiles()) {
        if (cc.hasChangedSince(mfile, idxFile.lastModified())) return true;
      }
    }
    return false;
  }

  ///////////////////////////////////////////////////
  // create the index

  private boolean createPartitionedIndex() throws IOException {
    long start = System.currentTimeMillis();

    // create partitions based on TimePartitionCollections object
    for (CollectionManager dcm : tpc.makePartitions()) {
      tp.addPartition(dcm);
    }

    List<TimePartition.Partition> bad = new ArrayList<TimePartition.Partition>();
    for (TimePartition.Partition tpp : tp.getPartitions()) {
      try {
        tpp.gc = tpp.makeGribCollection(CollectionManager.Force.always);    // force all partitions to be recreated
        logger.debug(" Open partition {}", tpp.getDcm().getCollectionName());
      } catch (Throwable t) {
        logger.error(" Failed to open partition " + tpp.getName(), t);
        bad.add(tpp);  // LOOK may be a file leak ?
      }
    }

    // remove ones that failed
    for (TimePartition.Partition p : bad)
      tp.removePartition(p);

    // choose the "canonical" partition, aka prototype
    int n = tp.getPartitions().size();
    if (n == 0) {
      logger.error(" Nothing in this partition = "+tp.getName());
      return false;
    }
    int idx = tpc.getProtoIndex(n);
    TimePartition.Partition canon = tp.getPartitions().get(idx);
    logger.info(" Using canonical partition {}", canon.getDcm().getCollectionName());

    // check consistency across vert and ens coords
    // also replace variables  in canonGc with partitoned variables
    Formatter f = new Formatter();
    GribCollection canonGc = checkPartitions(canon, f);
    if (canonGc == null) {
      logger.error(" Partition check failed, index not written on {} message = {}", tp.getName(), f.toString());
      f.format(" FAIL Partition check collection = %s%n", tp.getName());
      return false;
    }

    // make the time coordinates, place results into canon
    createPartitionedTimeCoordinates(canonGc, f);

    // ready to write the index file
    writeIndex(canonGc, f);

    // close open gc's
    for (TimePartition.Partition tpp : tp.getPartitions()) {
      tpp.gc.close();
    }
    canonGc.close();

    long took = System.currentTimeMillis() - start;
    f.format(" CreatePartitionedIndex took %d msecs%n", took);
    return true;
  }

  // consistency check on variables : compare each variable to corresponding one in proto
  // also set the groupno and partno for each partition
  // also replace the variables with partition variables
  private GribCollection checkPartitions(TimePartition.Partition canon, Formatter f) throws IOException {
    List<TimePartition.Partition> partitions = tp.getPartitions();
    int npart = partitions.size();
    boolean ok = true;

    // for each group in canonical Partition
    GribCollection canonGc = canon.gc;
    for (GribCollection.GroupHcs canonGroup : canonGc.getGroups()) {
      String gname = canonGroup.getId();
      if (trace) f.format(" Check Group %s%n",  gname);

      // hash proto variables for quick lookup
      Map<Integer, GribCollection.VariableIndex> check = new HashMap<Integer, GribCollection.VariableIndex>(canonGroup.varIndex.size());
      List<GribCollection.VariableIndex> varIndexP = new ArrayList<GribCollection.VariableIndex>(canonGroup.varIndex.size());
      for (GribCollection.VariableIndex vi : canonGroup.varIndex) {
        TimePartition.VariableIndexPartitioned vip = tp.makeVariableIndexPartitioned(vi, npart);
        varIndexP.add(vip);
        check.put(vi.cdmHash, vip); // replace with its evil twin
      }
      canonGroup.varIndex = varIndexP;// replace with its evil twin

      // for each partition
      for (int partno = 0; partno < npart; partno++) {
        TimePartition.Partition tpp = partitions.get(partno);
        if (trace) f.format(" Check Partition %s%n",  tpp.getName());

        // get corresponding group
        GribCollection gc = tpp.gc;
        int groupIdx = gc.findGroupIdxById(canonGroup.getId());
        if (groupIdx < 0) {
          f.format(" Cant find group %s (%d) in partition %s%n", gname, canonGroup.hashCode(), tpp.getName());
          // ok = false;
          continue;
        }
        GribCollection.GroupHcs group = gc.getGroup(groupIdx);

        // for each variable in partition group
        for (int varIdx = 0; varIdx < group.varIndex.size(); varIdx++) {
          GribCollection.VariableIndex vi2 = group.varIndex.get(varIdx);
          if (trace) f.format(" Check variable %s%n",  vi2);
          int flag = 0;

          GribCollection.VariableIndex vi1 = check.get(vi2.cdmHash); // compare with proto variable
          if (vi1 == null) {
            f.format("   WARN Cant find variable %s from %s in proto - ignoring that variable%n",  vi2, tpp.getName());
            continue; // we can tolerate this
          }

          //compare vert coordinates
          VertCoord vc1 = vi1.getVertCoord();
          VertCoord vc2 = vi2.getVertCoord();
          if ((vc1 == null) != (vc2 == null)) {
            f.format("   ERR Vert coordinates existence on variable %s in %s doesnt match%n",  vi2, tpp.getName());
            ok = false;
          } else if ((vc1 != null) && !vc1.equalsData(vc2)) {
            f.format("   WARN Vert coordinates values on variable %s in %s dont match%n",  vi2, tpp.getName());
            f.format("    canon vc = %s%n", vc1);
            f.format("    this vc = %s%n", vc2);
            flag |= TimePartition.VERT_COORDS_DIFFER;
          }

          //compare ens coordinates
          EnsCoord ec1 = vi1.getEnsCoord();
          EnsCoord ec2 = vi2.getEnsCoord();
          if ((ec1 == null) != (ec2 == null)) {
            f.format("   ERR Ensemble coordinates existence on variable %s in %s doesnt match%n",  vi2, tpp.getName());
            ok = false;
          } else if ((ec1 != null) && !ec1.equalsData(ec2)) {
            f.format("   WARN Ensemble coordinates values on variable %s in %s dont match%n",  vi2, tpp.getName());
            f.format("    canon ec = %s%n", ec1);
            f.format("    this ec = %s%n", ec2);
            flag |= TimePartition.ENS_COORDS_DIFFER;
          }

          ((TimePartition.VariableIndexPartitioned)vi1).setPartitionIndex(partno, groupIdx, varIdx, flag);
        } // loop over variable
      } // loop over partition
    } // loop over group

    if (ok) {
      f.format("  Partition check: vert, ens coords OK%n");
      return canonGc;
    } else {
      canonGc.close();
      return null;
    }
  }

  private class PartGroup {
    GribCollection.GroupHcs group;
    TimePartition.Partition tpp;

    private PartGroup(GribCollection.GroupHcs group, TimePartition.Partition tpp) {
      this.group = group;
      this.tpp = tpp;
    }
  }

  private boolean createPartitionedTimeCoordinates(GribCollection canonGc, Formatter f) throws IOException {
    List<TimePartition.Partition> partitions = tp.getPartitions();
    boolean ok = true;

    // for each group in canonical Partition
    for (GribCollection.GroupHcs firstGroup : canonGc.getGroups()) {
      String gname = firstGroup.getId();
      if (trace) f.format(" Check Group %s%n",  gname);

      // get list of corresponding groups from all the time partition, so we dont have to keep looking it up
      List<PartGroup> pgList = new ArrayList<PartGroup>(partitions.size());
      for (TimePartition.Partition dc : partitions) {
        GribCollection.GroupHcs gg = dc.gc.findGroupById(gname);
        if (gg == null)
          logger.error(" Cant find group {} in partition {}", gname, dc.getName());
        else
          pgList.add(new PartGroup(gg, dc));
      }

      // unique time coordinate unions
      List<TimeCoordUnion> unionList = new ArrayList<TimeCoordUnion>();

      // for each variable in canonical Partition
      for (GribCollection.VariableIndex viCanon : firstGroup.varIndex) {
        if (trace) f.format(" Check variable %s%n", viCanon);
        TimeCoord tcCanon = viCanon.getTimeCoord();

        List<TimeCoord> tcPartitions = new ArrayList<TimeCoord>(pgList.size());

        // for each partition, get the time index
        for (PartGroup pg : pgList) {
          // get corresponding variable
          GribCollection.VariableIndex vi2 = pg.group.findVariableByHash(viCanon.cdmHash);
          if (vi2 == null) {  // apparently not in the file
            f.format("   WARN Cant find variable %s in partition %s / %s%n", viCanon, pg.tpp.getName(), pg.group.getId());
            tcPartitions.add(null);
          } else {
            if (vi2.timeIdx < 0 || vi2.timeIdx >= pg.group.timeCoords.size()) {
              logger.error(" timeIdx out of range var= {} on partition {}", vi2, pg.tpp.getName());
              tcPartitions.add(null);
            } else {
              TimeCoord tc2 = vi2.getTimeCoord();
              if (tc2.isInterval() != tcCanon.isInterval()) {
                logger.error(" timeIdx wrong interval type var= {} on partition {}", vi2, pg.tpp.getName());
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
      firstGroup.timeCoordPartitions = unionList;
    }

    return ok;
  }

  //////////////////////////////////////////////////////////

  @Override
  public String getMagicStart() {
    return MAGIC_START;
  }

  // writing time partition ncx
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
        logger.error(" gc1tp cant delete "+file.getPath());
    }

    RandomAccessFile raf = new RandomAccessFile(file.getPath(), "rw");
    raf.order(RandomAccessFile.BIG_ENDIAN);
    try {
      //// header message
      raf.write(MAGIC_START.getBytes("UTF-8"));
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
      indexBuilder.setDirName(gc.getDirectory().getPath());

      // dont need files - these are stored in the partition objects

      for (TimePartition.Partition part : tp.getPartitions()) {
        indexBuilder.addPartitions(writePartitionProto(part.getName(), part));
      }

      GribCollectionProto.GribCollectionIndex index = indexBuilder.build();
      byte[] b = index.toByteArray();
      NcStream.writeVInt(raf, b.length); // message size
      raf.write(b);  // message  - all in one gulp
      f.format("GribCollectionTimePartitionedIndex= %d bytes file size =  %d bytes%n%n", b.length, raf.length());

    } finally {
      raf.close();
    }

    return true;
  }

  private GribCollectionProto.Group writeGroupProto(GribCollection.GroupHcs g) throws IOException {
    GribCollectionProto.Group.Builder b = GribCollectionProto.Group.newBuilder();

    b.setGds(ByteString.copyFrom(g.rawGds));
    b.setGdsHash(g.gdsHash);

    for (GribCollection.VariableIndex vb : g.varIndex) {
      b.addVariables(writeVariableProto( (TimePartition.VariableIndexPartitioned) vb));
    }

    for (int i = 0; i < g.timeCoordPartitions.size(); i++)
      b.addTimeCoordUnions(writeTimeCoordUnionProto(g.timeCoordPartitions.get(i), i));

    List<VertCoord> vertCoords = g.vertCoords;
    for (int i = 0; i < vertCoords.size(); i++)
      b.addVertCoords(writeCoordProto(vertCoords.get(i), i));

    List<EnsCoord> ensCoords = g.ensCoords;
    for (int i = 0; i < ensCoords.size(); i++)
      b.addEnsCoords(writeCoordProto(ensCoords.get(i), i));
    return b.build();
  }

  private GribCollectionProto.Variable writeVariableProto(TimePartition.VariableIndexPartitioned v) throws IOException {
  //private GribCollectionProto.Variable writeVariableProto(GribCollection.VariableIndex v) throws IOException {
    GribCollectionProto.Variable.Builder b = GribCollectionProto.Variable.newBuilder();

    b.setDiscipline(v.discipline);
    b.setCategory(v.category);
    b.setParameter(v.parameter);
    b.setLevelType(v.levelType);
    b.setIsLayer(v.isLayer);
    b.setIntervalType(v.intvType);
    if (v.intvName != null)
      b.setIntvName(v.intvName);
    b.setCdmHash(v.cdmHash);
    b.setRecordsPos(0);
    b.setRecordsLen(0);
    b.setTimeIdx(v.partTimeCoordIdx); // note
    if (v.vertIdx >= 0)
      b.setVertIdx(v.vertIdx);
    if (v.ensIdx >= 0)
      b.setEnsIdx(v.ensIdx);
    if (v.ensDerivedType >= 0)
      b.setEnsDerivedType(v.ensDerivedType); // derived type (table 4.7)
    if (v.probabilityName != null)
      b.setProbabilityName(v.probabilityName);
    if (v.probType >= 0)
      b.setProbabilityType(v.probType);
    for (int idx : v.groupno)
      b.addGroupno(idx);
    for (int idx : v.varno)
      b.addVarno(idx);
    for (int idx : v.flag)
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

  private GribCollectionProto.Partition writePartitionProto(String name, TimePartition.Partition p) throws IOException {
    GribCollectionProto.Partition.Builder b = GribCollectionProto.Partition.newBuilder();

    b.setFilename(p.getIndexFilename());
    b.setName(name);
    if (p.getLastModified() > 0)
      b.setLastModified(p.getLastModified());

    return b.build();
  }

  ///////////////////////////////////////////////////////////////////////////
  // reading ncx

  @Override
  protected boolean readPartitions(GribCollectionProto.GribCollectionIndex proto, String directory) {
    for (int i = 0; i < proto.getPartitionsCount(); i++) {
      GribCollectionProto.Partition pp = proto.getPartitions(i);
      tp.addPartition(pp.getName(), pp.getFilename(), pp.getLastModified(), directory);
    }
    return  proto.getPartitionsCount() > 0;
  }

  @Override
  protected void readTimePartitions(GribCollection.GroupHcs group, GribCollectionProto.Group proto) {
    List<TimeCoord> list = new ArrayList<TimeCoord>(proto.getTimeCoordUnionsCount());
    for (int i = 0; i < proto.getTimeCoordUnionsCount(); i++) {
      GribCollectionProto.TimeCoordUnion tpu = proto.getTimeCoordUnions(i);
      list.add(readTimePartition(tpu, i));
    }
    group.timeCoords = list;
  }

  /*
   protected TimeCoord readTimePartition(GribCollectionProto.TimeCoordUnion pc, int timeIndex) {
    int[] partition = new int[pc.getPartitionCount()];
    int[] index = new int[pc.getPartitionCount()];  // better be the same
    for (int i = 0; i < pc.getPartitionCount(); i++) {
      partition[i] = pc.getPartition(i);
      index[i] = pc.getIndex(i);
    }

    if (pc.getBoundCount() > 0) {  // its an interval
      List<TimeCoord.Tinv> coords = new ArrayList<TimeCoord.Tinv>(pc.getValuesCount());
      for (int i = 0; i < pc.getValuesCount(); i++)
        coords.add(new TimeCoord.Tinv((int) pc.getValues(i), (int) pc.getBound(i)));
      TimeCoordUnion tc =  new TimeCoordUnion(pc.getCode(), pc.getUnit(), coords, partition, index);
      return tc.setIndex( timeIndex);

    } else {
      List<Integer> coords = new ArrayList<Integer>(pc.getValuesCount());
      for (float value : pc.getValuesList())
        coords.add((int) value);
      TimeCoordUnion tc = new TimeCoordUnion(pc.getCode(), pc.getUnit(), coords, partition, index);
      return tc.setIndex( timeIndex);
    }
  }

   */

 protected TimeCoord readTimePartition(GribCollectionProto.TimeCoordUnion pc, int timeIndex) {
    int[] partition = new int[pc.getPartitionCount()];
    int[] index = new int[pc.getPartitionCount()];  // better be the same
    for (int i = 0; i < pc.getPartitionCount(); i++) {
      partition[i] = pc.getPartition(i);
      index[i] = pc.getIndex(i);
    }

    if (pc.getBoundCount() > 0) {  // its an interval
      List<TimeCoord.Tinv> coords = new ArrayList<TimeCoord.Tinv>(pc.getValuesCount());
      for (int i = 0; i < pc.getValuesCount(); i++)
        coords.add(new TimeCoord.Tinv((int) pc.getValues(i), (int) pc.getBound(i)));
      TimeCoordUnion tc =  new TimeCoordUnion(pc.getCode(), pc.getUnit(), coords, partition, index);
      return tc.setIndex( timeIndex);

    } else {
      List<Integer> coords = new ArrayList<Integer>(pc.getValuesCount());
      for (float value : pc.getValuesList())
        coords.add((int) value);
      TimeCoordUnion tc = new TimeCoordUnion(pc.getCode(), pc.getUnit(), coords, partition, index);
      return tc.setIndex( timeIndex);
    }
  }

  @Override
  protected GribCollection.VariableIndex readVariable(GribCollectionProto.Variable pv, GribCollection.GroupHcs group) {
    int discipline = pv.getDiscipline();
    int category = pv.getCategory();
    int param = pv.getParameter();
    int levelType = pv.getLevelType();
    int intvType = pv.getIntervalType();
    String intvName = pv.getIntvName();
    boolean isLayer = pv.getIsLayer();
    int ensDerivedType = pv.getEnsDerivedType();
    int probType = pv.getProbabilityType();
    String probabilityName = pv.getProbabilityName();
    int cdmHash = pv.getCdmHash();
    long recordsPos = pv.getRecordsPos();
    int recordsLen = pv.getRecordsLen();
    int timeIdx = pv.getTimeIdx();
    int vertIdx = pv.getVertIdx();
    int ensIdx = pv.getEnsIdx();
    int tableVersion = pv.getTableVersion();
    List<Integer> groupnoList = pv.getGroupnoList();
    List<Integer> varnoList = pv.getVarnoList();
    List<Integer> flagList = pv.getFlagList();

    return tp.makeVariableIndex(group, tableVersion, discipline, category, param, levelType, isLayer, intvType, intvName,
            ensDerivedType, probType, probabilityName, -1, cdmHash, timeIdx, vertIdx, ensIdx, recordsPos, recordsLen,
            groupnoList, varnoList, flagList);
  }

  public static void main(String[] args) throws IOException {
    Formatter f = new Formatter();
    String indexName = (args.length > 0) ? args[0] : "F:/nomads/NOMADS-cfsrr-timeseries.ncx";
    RandomAccessFile raf = new RandomAccessFile(indexName, "r");
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1TimePartitionBuilder.class);
    Grib1TimePartition gtc = Grib1TimePartitionBuilder.createFromIndex("test", null, raf, logger);
    gtc.showIndex(f);
    System.out.printf("%s%n", f);
  }
}
