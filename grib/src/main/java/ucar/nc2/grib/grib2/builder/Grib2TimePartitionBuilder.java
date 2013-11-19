/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib.grib2.builder;

import com.google.protobuf.ByteString;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionManager;
import thredds.inventory.CollectionManagerRO;
import thredds.inventory.MFile;
import thredds.inventory.partition.PartitionManager;
import thredds.inventory.partition.TimePartitionCollection;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib2.Grib2Index;
import ucar.nc2.grib.grib2.Grib2TimePartition;
import ucar.nc2.stream.NcStream;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Builds Collections of Grib2 Time Partitioned Collections.
 * TimePartition objects from special ncx file, which starts with TimePartion.MAGIC_START.
 * Writes index files from TimePartitionCollections, from which it builds collections of GribCollection.
 *
 * @author caron
 * @since 4/28/11
 */
public class Grib2TimePartitionBuilder extends Grib2CollectionBuilder {
  public static final String MAGIC_START = "Grib2Partition0Index";
  static private final boolean trace = false;

  // called by tdm
  static public boolean update(PartitionManager tpc, org.slf4j.Logger logger) throws IOException {
    Grib2TimePartitionBuilder builder = new Grib2TimePartitionBuilder(tpc.getCollectionName(), new File(tpc.getRoot()), tpc, logger);
    if (!builder.needsUpdate()) return false;
    builder.readOrCreateIndex(CollectionManager.Force.always);
    builder.gc.close();
    return true;
  }

    // read in the index, create if it doesnt exist or is out of date
  static public Grib2TimePartition factory(PartitionManager tpc, CollectionManager.Force force, org.slf4j.Logger logger) throws IOException {
    Grib2TimePartitionBuilder builder = new Grib2TimePartitionBuilder(tpc.getCollectionName(), new File(tpc.getRoot()), tpc, logger);
    builder.readOrCreateIndex(force);
    return builder.tp;
  }

  /**
   * write new index if needed
   *
   * @param tpc use this collection
   * @param force force index
   * @return true if index was written
   * @throws IOException on error
   *
  static public boolean writeIndexFile(TimePartitionCollection tpc, CollectionManager.Force force, org.slf4j.Logger logger) throws IOException {
    Grib2TimePartitionBuilder builder = null;
    try {
      builder = new Grib2TimePartitionBuilder(tpc.getCollectionName(), new File(tpc.getRoot()), tpc, logger);
      return builder.readOrCreateIndex(force);

    } finally {
      if ((builder != null) && (builder.tp != null))
        builder.tp.close();
    }
  } */

  //////////////////////////////////////////////////////////////////////////////////

  private final PartitionManager tpc; // defines the partition
  private Grib2TimePartition tp;  // build this object

  protected Grib2TimePartitionBuilder(String name, File directory, PartitionManager tpc, org.slf4j.Logger logger) {
    super(tpc, false, logger);
    this.name = name;
    this.directory = directory;

    FeatureCollectionConfig.GribConfig config = null;
    if (tpc != null) config = (FeatureCollectionConfig.GribConfig) tpc.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    this.tp = new Grib2TimePartition(name, directory, config, logger);
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
      if (createPartitionedIndex(null)) {  // write index
        return readIndex(idx.getPath()); // read back in index
      }
    }
    return false;
  }

  // LOOK not sure if this works
  private boolean needsUpdate(long collectionLastModified) throws IOException {
    CollectionManager.ChangeChecker cc = Grib2Index.getChangeChecker();
    for (CollectionManagerRO dcm : tpc.makePartitions()) { // LOOK not really right, since we dont know if these files are the same as in the index
      File idxFile = GribCollection.getIndexFile(dcm);
      if (!idxFile.exists())
        return true;
      if (collectionLastModified < idxFile.lastModified())
        return true;
      for (MFile mfile : dcm.getFiles()) {
        if (cc.hasChangedSince(mfile, idxFile.lastModified()))
          return true;
      }
    }
    return false;
  }

  private boolean readIndex(String filename) throws IOException {
    return readIndex( new RandomAccessFile(filename, "r") );
  }

  private boolean readIndex(RandomAccessFile indexRaf) throws IOException {
    try {
      this.tp = Grib2TimePartitionBuilderFromIndex.createTimePartitionFromIndex(this.name, this.directory, indexRaf, logger);
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

    // create partitions based on TimePartitionCollections object
    for (CollectionManagerRO dcm : tpc.makePartitions()) {
      tp.addPartition(dcm);
    }

    List<TimePartition.Partition> bad = new ArrayList<>();
    for (TimePartition.Partition tpp : tp.getPartitions()) {
      try {
        tpp.gc = tpp.makeGribCollection(CollectionManager.Force.nocheck);    // use index if it exists  LOOK force ??
        if (trace) logger.debug(" Open partition {}", tpp.getDcm().getCollectionName());
      } catch (Throwable t) {
        logger.error(" Failed to open partition " + tpp.getName(), t);
        bad.add(tpp);
      }
    }

    // remove ones that failed
    for (TimePartition.Partition tpp : bad)
      tp.removePartition(tpp);

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
    // also replace variables  in canonGc with partitioned variables
    // partition index is used - do not resort partitions
    if (f == null) f = new Formatter();
    checkGroups(canon, f, true);
    GribCollection canonGc = checkPartitions(canon, f);
    if (canonGc == null) {
      logger.error(" Partition check failed, index not written on {} errors = \n{}", tp.getName(), f.toString());
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
  private GribCollection checkPartitions(TimePartition.Partition canon, Formatter f) throws IOException {
    List<TimePartition.Partition> partitions = tp.getPartitions();
    int npart = partitions.size();
    boolean ok = true;

    // for each group in canonical Partition
    GribCollection canonGc = canon.gc;
    for (GribCollection.GroupHcs firstGroup : canonGc.getGroups()) {
      String gname = firstGroup.getId();
      String gdesc = firstGroup.getDescription();
      if (trace) f.format(" Check Group %s%n",  gname);

      // hash proto variables for quick lookup
      Map<Integer, GribCollection.VariableIndex> check = new HashMap<>(2*firstGroup.varIndex.size());
      List<GribCollection.VariableIndex> varIndexP = new ArrayList<>(firstGroup.varIndex.size());
      for (GribCollection.VariableIndex vi : firstGroup.varIndex) {
        TimePartition.VariableIndexPartitioned vip = tp.makeVariableIndexPartitioned(vi, npart);
        varIndexP.add(vip);
        if (check.containsKey(vi.cdmHash))
          System.out.println("HEY DUPLICATE");
        check.put(vi.cdmHash, vip); // replace with its evil twin
      }
      firstGroup.varIndex = varIndexP;// replace with its evil twin

      // for each partition
      for (int partno = 0; partno < npart; partno++) {
        TimePartition.Partition tpp = partitions.get(partno);
        if (trace) f.format(" Check Partition %s%n",  tpp.getName());

        // get corresponding group
        GribCollection gc = tpp.gc;
        int groupIdx = gc.findGroupIdxById(firstGroup.getId());
        if (groupIdx < 0) {
          f.format(" Cant find canonical group %s in partition %s%n", gname, tpp.getName());
          //ok = false;
          continue;
        }
        GribCollection.GroupHcs group = gc.getGroup(groupIdx);

        // for each variable in partition group
        for (int varIdx = 0; varIdx < group.varIndex.size(); varIdx++) {
          GribCollection.VariableIndex vi2 = group.varIndex.get(varIdx);
          if (trace) f.format(" Check %s%n",  vi2.toStringShort());
          int flag = 0;

          GribCollection.VariableIndex vi1 = check.get(vi2.cdmHash); // compare with proto variable (vi1)
          if (vi1 == null) {
            f.format("   WARN Cant find %s from %s in proto - ignoring that variable%n",  vi2.toStringShort(), tpp.getName());
            continue; // we can tolerate this
          }

          //compare vert coordinates
          VertCoord vc1 = vi1.getVertCoord();
          VertCoord vc2 = vi2.getVertCoord();
          if ((vc1 == null) != (vc2 == null)) {
            vc1 = vi1.getVertCoord();   // debug
            vc2 = vi2.getVertCoord();
            f.format("   ERR Vert coordinates existence on group %s (%s) on %s in %s (exist %s) doesnt match %s (exist %s)%n",  gname, gdesc, vi2.toStringShort(),
                    tpp.getName(), (vc2 == null), canon.getName(), (vc1 == null));
            ok = false;

          } else if ((vc1 != null) && !vc1.equalsData(vc2)) {
            f.format("   WARN Vert coordinates values on %s in %s dont match%n",  vi2.toStringShort(), tpp.getName());
            f.format("    canon vc = %s%n", vc1);
            f.format("    this vc = %s%n", vc2);
            flag |= TimePartition.VERT_COORDS_DIFFER;
          }

          //compare ens coordinates
          EnsCoord ec1 = vi1.getEnsCoord();
          EnsCoord ec2 = vi2.getEnsCoord();
          if ((ec1 == null) != (ec2 == null)) {
            f.format("   ERR Ensemble coordinates existence on %s in %s doesnt match%n",  vi2.toStringShort(), tpp.getName());
            ok = false;
          } else if ((ec1 != null) && !ec1.equalsData(ec2)) {
            f.format("   WARN Ensemble coordinates values on %s in %s dont match%n",  vi2.toStringShort(), tpp.getName());
            f.format("    canon ec = %s%n", ec1);
            f.format("    this ec = %s%n", ec2);
            flag |= TimePartition.ENS_COORDS_DIFFER;
          }

          ((TimePartition.VariableIndexPartitioned)vi1).setPartitionIndex(partno, groupIdx, varIdx, flag);
        } // loop over variable
      } // loop over partition
    } // loop over group

    if (ok) {
      f.format("  Partition check OK%n");
      return canonGc;
    } else {
      return null;
    }
  }

  private class Groups {
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
      List<PartGroup> pgList = new ArrayList<>(partitions.size());
      for (TimePartition.Partition tpp : partitions) {
        GribCollection.GroupHcs gg = tpp.gc.findGroupById(gname);
        if (gg == null) {
          logger.warn(" Cant find group {} in partition {}", gname, tpp.getName());
          continue;
        } else
          pgList.add(new PartGroup(gg, tpp));
      }

      // unique time coordinate unions
      List<TimeCoordUnion> unionList = new ArrayList<TimeCoordUnion>();

      // for each variable in canonical Partition
      for (GribCollection.VariableIndex viCanon : firstGroup.varIndex) {
        if (trace) f.format(" Check variable %s%n", viCanon);
        TimeCoord tcCanon = viCanon.getTimeCoord();

        List<TimeCoord> tcPartitions = new ArrayList<>(pgList.size());

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
  private boolean writeIndex(GribCollection canonGc, Formatter f) throws IOException {
    File file = tp.getIndexFile();
    if (file.exists()) {
      if (!file.delete())
        logger.error("gc2tp cant delete "+file.getPath());
    }

    RandomAccessFile raf = new RandomAccessFile(file.getPath(), "rw");
    raf.order(RandomAccessFile.BIG_ENDIAN);
    try {
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
      indexBuilder.setDirName(gc.getDirectory().getPath());

      // dont need files - these are stored in the partition objects

      for (TimePartition.Partition p : tp.getPartitions()) {
        indexBuilder.addPartitions(writePartitionProto(p.getName(), p));
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

}

