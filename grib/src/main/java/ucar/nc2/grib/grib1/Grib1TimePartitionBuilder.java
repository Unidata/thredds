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

  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1TimePartitionBuilder.class);
  static private final int versionTP = 2;
  static private final boolean trace = false;

    // read in the index, create if it doesnt exist or is out of date
  static public Grib1TimePartition factory(TimePartitionCollection tpc, CollectionManager.Force force, Formatter f) throws IOException {
    Grib1TimePartitionBuilder builder = new Grib1TimePartitionBuilder(tpc.getCollectionName(), new File(tpc.getRoot()), tpc);
    builder.readOrCreateIndex(force, f);
    return builder.tp;
  }

  // read in the index, index raf already open
  static public Grib1TimePartition createFromIndex(String name, File directory, RandomAccessFile raf) throws IOException {
    Grib1TimePartitionBuilder builder = new Grib1TimePartitionBuilder(name, directory, null);
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
   * @param f put status messagess here
   * @return true if index was written
   * @throws IOException on error
   */
  static public boolean writeIndexFile(TimePartitionCollection tpc, CollectionManager.Force force, Formatter f) throws IOException {
    Grib1TimePartitionBuilder builder = null;
    try {
      builder = new Grib1TimePartitionBuilder(tpc.getCollectionName(), new File(tpc.getRoot()), tpc);
      return builder.readOrCreateIndex(force, f);

    } finally {
      if ((builder != null) && (builder.tp != null))
        builder.tp.close();
    }
  }

  //////////////////////////////////////////////////////////////////////////////////

  private final TimePartitionCollection tpc; // defines the partition
  private final Grib1TimePartition tp;  // build this object

  private Grib1TimePartitionBuilder(String name, File directory, TimePartitionCollection tpc) {
    this.tp = new Grib1TimePartition(name, directory);
    this.gc = tp;
    this.tpc = tpc;
  }

  private boolean readOrCreateIndex(CollectionManager.Force ff, Formatter f) throws IOException {
    File idx = gc.getIndexFile();

     // force new index or test for new index needed
    boolean force =
             ((ff == CollectionManager.Force.always) ||
             (ff == CollectionManager.Force.test && idx.exists() && needsUpdate(idx.lastModified(), f)));

    // otherwise, we're good as long as the index file exists and can be read
    if (force || !idx.exists() || !readIndex(idx.getPath()) )  {
      logger.info("TimePartitionBuilder createIndex {}", idx.getPath());
      createPartitionedIndex(f);   // write out index
      readIndex(idx.getPath()); // read back in index
      return true;
    }
    return false;
  }

  private boolean needsUpdate(long collectionLastModified, Formatter f) throws IOException {
    CollectionManager.ChangeChecker cc = Grib1Index.getChangeChecker();
    for (CollectionManager dcm : tpc.makePartitions()) { // LOOK not really right, since we dont know if these files are the same as in the index
      File idxFile = new File(dcm.getRoot(), dcm.getCollectionName() + GribCollection.IDX_EXT);
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

  private boolean createPartitionedIndex(Formatter f) throws IOException {
    long start = System.currentTimeMillis();

    // create partitions based on TimePartitionCollections object
    for (CollectionManager dcm : tpc.makePartitions()) {
      tp.addPartition(dcm);
    }

    List<TimePartition.Partition> bad = new ArrayList<TimePartition.Partition>();
    for (TimePartition.Partition dc : tp.getPartitions()) {
      try {
        dc.getGribCollection(f);         // ensure collection has been read successfully
        if (trace) f.format(" Open partition %s%n", dc.getDcm().getCollectionName());
      } catch (Throwable t) {
        logger.error(" Failed to open partition " + dc.getName(), t);
        f.format(" FAIL on partition %s (remove) %n", dc.getDcm().getCollectionName());
        bad.add(dc);
      }
    }

    // remove ones that failed
    for (TimePartition.Partition p : bad)
      tp.removePartition(p);

    // choose the "canonical" partition, aka prototype
    int n = tp.getPartitions().size();
    if (n == 0) {
      logger.error(" Nothing in this partition = "+tp.getName());
      f.format(" FAIL Partition empty collection = %s%n", tp.getName());
      return false;
    }
    int idx = tpc.getProtoIndex(n);
    TimePartition.Partition canon = tp.getPartitions().get(idx);
    f.format(" Using canonical partition %s%n", canon.getDcm().getCollectionName());

    // check consistency across vert and ens coords
    if (!checkPartitions(canon, f)) {
      logger.error(" Partition check failed, index not written = "+tp.getName());
      f.format(" FAIL Partition check collection = %s%n", tp.getName());
      return false;
    }

    // make the time coordinates, place results into canon
    createPartitionedTimeCoordinates(canon, f);

    // ready to write the index file
    writeIndex(canon, f);

    long took = System.currentTimeMillis() - start;
    f.format(" CreatePartitionedIndex took %d msecs%n", took);
    return true;
  }

  // consistency check on variables : compare each variable to corresponding one in proto
  // also set the groupno and partno for each partition
  private boolean checkPartitions(TimePartition.Partition canon, Formatter f) throws IOException {
    List<TimePartition.Partition> partitions = tp.getPartitions();
    int npart = partitions.size();
    boolean ok = true;

    // for each group in canonical Partition
    for (GribCollection.GroupHcs firstGroup : canon.getGribCollection(f).getGroups()) {
      String gname = firstGroup.getGroupName();
      if (trace) f.format(" Check Group %s%n",  gname);

      // hash proto variables for quick lookup
      Map<Integer, GribCollection.VariableIndex> check = new HashMap<Integer, GribCollection.VariableIndex>(firstGroup.varIndex.size());
      List<GribCollection.VariableIndex> varIndexP = new ArrayList<GribCollection.VariableIndex>(firstGroup.varIndex.size());
      for (GribCollection.VariableIndex vi : firstGroup.varIndex) {
        TimePartition.VariableIndexPartitioned vip = tp.makeVariableIndexPartitioned(vi, npart);
        varIndexP.add(vip);
        check.put(vi.cdmHash, vip); // replace with its evil twin
      }
      firstGroup.varIndex = varIndexP;// replace with its evil twin

      // for each partition
      for (int partno = 0; partno < npart; partno++) {
        TimePartition.Partition tpp = partitions.get(partno);
        if (trace) f.format(" Check Partition %s%n",  tpp.getName());

        // get corresponding group
        GribCollection gc = tpp.getGribCollection(f);
        int groupIdx = gc.findGroupIdx(firstGroup.getGroupName());
        if (groupIdx < 0) {
          f.format(" Cant find group %s in partition %s%n", gname, tpp.getName());
          ok = false;
          continue;
        }
        GribCollection.GroupHcs group = gc.getGroup(groupIdx);

        // for each variable in partition group
        for (int varIdx = 0; varIdx < group.varIndex.size(); varIdx++) {
          GribCollection.VariableIndex vi2 = group.varIndex.get(varIdx);
          if (trace) f.format(" Check variable %s%n",  vi2);

          GribCollection.VariableIndex vi1 = check.get(vi2.cdmHash); // compare with proto variable
          if (vi1 == null) {
            f.format("   WARN Cant find variable %s from %s in proto - ignoring that variable%n",  vi2, tpp.getName());
            continue; // we can tolerate this
          }
          ((TimePartition.VariableIndexPartitioned)vi1).setPartitionIndex(partno, groupIdx, varIdx);

          //compare vert coordinates
          VertCoord vc1 = vi1.getVertCoord();
          VertCoord vc2 = vi2.getVertCoord();
          if ((vc1 == null) != (vc2 == null)) {
            f.format("   ERR Vert coordinates existence on variable %s in %s doesnt match%n",  vi2, tpp.getName());
            ok = false;
          } else if ((vc1 != null) && !vc1.equalsData(vc2)) {
            f.format("   ERR Vert coordinates values on variable %s in %s dont match%n",  vi2, tpp.getName());
            f.format("    canon vc = %s%n", vc1);
            f.format("    this vc = %s%n", vc2);
            ok = false;
          }

          //compare ens coordinates
          EnsCoord ec1 = vi1.getEnsCoord();
          EnsCoord ec2 = vi2.getEnsCoord();
          if ((ec1 == null) != (ec2 == null)) {
            f.format("   ERR Ensemble coordinates existence on variable %s in %s doesnt match%n",  vi2, tpp.getName());
            ok = false;
          } else if ((ec1 != null) && !ec1.equalsData(ec2)) {
            f.format("   ERR Ensemble coordinates values on variable %s in %s dont match%n",  vi2, tpp.getName());
            f.format("    canon ec = %s%n", ec1);
            f.format("    this ec = %s%n", ec2);
            ok = false;
          }

        } // loop over variable
      } // loop over partition
    } // loop over group

    if (ok)
      f.format("  Partition check: vert, ens coords OK%n");
    return ok;
  }

  private class PartGroup {
    GribCollection.GroupHcs group;
    TimePartition.Partition tpp;

    private PartGroup(GribCollection.GroupHcs group, TimePartition.Partition tpp) {
      this.group = group;
      this.tpp = tpp;
    }
  }

  private boolean createPartitionedTimeCoordinates(TimePartition.Partition canon, Formatter f) throws IOException {
    List<TimePartition.Partition> partitions = tp.getPartitions();
    boolean ok = true;

    // for each group in canonical Partition
    for (GribCollection.GroupHcs firstGroup : canon.getGribCollection(f).getGroups()) {
      String gname = firstGroup.getGroupName();
      if (trace) f.format(" Check Group %s%n",  gname);

      // get list of corresponding groups from all the time partition, so we dont have to keep looking it up
      List<PartGroup> pgList = new ArrayList<PartGroup>(partitions.size());
      for (TimePartition.Partition dc : partitions) {
        GribCollection.GroupHcs gg = dc.getGribCollection(f).findGroup(gname);
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
            f.format("   WARN Cant find variable %s in partition %s / %s%n", viCanon, pg.tpp.getName(), pg.group.getGroupName());
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
        TimeCoordUnion union = new TimeCoordUnion(tcPartitions, tcCanon);

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

  // writing ncx
  /*
  MAGIC_START
  version
  sizeRecords
  VariableRecords (sizeRecords bytes)
  sizeIndex
  GribCollectionIndex (sizeIndex bytes)
  */
  private boolean writeIndex(TimePartition.Partition canon, Formatter f) throws IOException {
    File file = tp.getIndexFile();
    if (file.exists()) {
      if (!file.delete())
        logger.error("Cant delete "+file.getPath());
    }

    RandomAccessFile raf = new RandomAccessFile(file.getPath(), "rw");
    raf.order(RandomAccessFile.BIG_ENDIAN);
    try {
      //// header message
      raf.write(MAGIC_START.getBytes("UTF-8"));
      raf.writeInt(versionTP);
      raf.writeLong(0); // no record section

      GribCollectionProto.GribCollectionIndex.Builder indexBuilder = GribCollectionProto.GribCollectionIndex.newBuilder();
      indexBuilder.setName(tp.getName());

      GribCollection canonGc = canon.getGribCollection(f);
      for (GribCollection.GroupHcs g : canonGc.getGroups())
        indexBuilder.addGroups(writeGroupProto(g));

      indexBuilder.setCenter(canonGc.center);
      indexBuilder.setSubcenter(canonGc.subcenter);
      indexBuilder.setMaster(canonGc.master);
      indexBuilder.setLocal(canonGc.local);

      for (TimePartition.Partition p : tp.getPartitions()) {
        indexBuilder.addPartitions(writePartitionProto(p.getName(), (TimePartition.Partition) p));
      }

      GribCollectionProto.GribCollectionIndex index = indexBuilder.build();
      byte[] b = index.toByteArray();
      NcStream.writeVInt(raf, b.length); // message size
      raf.write(b);  // message  - all in one gulp
      f.format("GribCollectionTimePartitionedIndex= %d bytes%n", b.length);

    } finally {
      f.format("file size =  %d bytes%n", raf.length());
      raf.close();
    }

    return true;
  }

  private GribCollectionProto.Group writeGroupProto(GribCollection.GroupHcs g) throws IOException {
    GribCollectionProto.Group.Builder b = GribCollectionProto.Group.newBuilder();

    b.setGds(ByteString.copyFrom(g.rawGds));

    for (GribCollection.VariableIndex vb : g.varIndex)
      b.addVariables(writeVariableProto( (TimePartition.VariableIndexPartitioned) vb));

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

    b.setFilename(p.getFilename());
    b.setName(name);

    return b.build();
  }

  ///////////////////////////////////////////////////////////////////////////
  // reading ncx

  protected int getVersion() {
    return versionTP;
  }

  @Override
  protected boolean readPartitions(GribCollectionProto.GribCollectionIndex proto) {
    for (int i = 0; i < proto.getPartitionsCount(); i++) {
      GribCollectionProto.Partition pp = proto.getPartitions(i);
      tp.addPartition(pp.getName(), pp.getFilename());
    }
    return  proto.getPartitionsCount() > 0;
  }

  @Override
  protected void readTimePartitions(GribCollection.GroupHcs group, GribCollectionProto.Group proto) {
    List<TimeCoord> list = new ArrayList<TimeCoord>(proto.getTimeCoordUnionsCount());
    for (int i = 0; i < proto.getTimeCoordUnionsCount(); i++) {
      GribCollectionProto.TimeCoordUnion tpu = proto.getTimeCoordUnions(i);
      list.add(readTimePartition(tpu));
    }
    group.timeCoords = list;
  }

  protected TimeCoordUnion readTimePartition(GribCollectionProto.TimeCoordUnion pc) {
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
      return new TimeCoordUnion(pc.getCode(), pc.getUnit(), coords, partition, index);

    } else {
      List<Integer> coords = new ArrayList<Integer>(pc.getValuesCount());
      for (float value : pc.getValuesList())
        coords.add((int) value);
      return new TimeCoordUnion(pc.getCode(), pc.getUnit(), coords, partition, index);
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

    return tp.makeVariableIndex(group, tableVersion, discipline, category, param, levelType, isLayer, intvType, intvName,
            ensDerivedType, probType, probabilityName, cdmHash, timeIdx, vertIdx, ensIdx, recordsPos, recordsLen);
  }

  public static void main(String[] args) throws IOException {
    Formatter f = new Formatter();
    String indexName = (args.length > 0) ? args[0] : "F:/nomads/NOMADS-cfsrr-timeseries.ncx";
    RandomAccessFile raf = new RandomAccessFile(indexName, "r");
    Grib1TimePartition gtc = Grib1TimePartitionBuilder.createFromIndex("test", null, raf);
    gtc.showIndex(f);
    System.out.printf("%s%n", f);
  }
}
