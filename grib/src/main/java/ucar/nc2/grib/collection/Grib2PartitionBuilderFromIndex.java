package ucar.nc2.grib.collection;

import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.grib.grib2.Grib2Gds;
import ucar.nc2.grib.grib2.Grib2SectionGridDefinition;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read Grib2Partition From ncx2 Index
 *
 * @author John
 * @since 12/7/13
 */
public class Grib2PartitionBuilderFromIndex extends Grib2CollectionBuilderFromIndex {

    // read in the index, open raf
  static public GribCollection createTimePartitionFromIndex(String name, File directory, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    File idxFile = ucar.nc2.grib.GribCollection.getIndexFile(name, directory);
    RandomAccessFile raf = new RandomAccessFile(idxFile.getPath(), "r");
    return createTimePartitionFromIndex(name, directory, raf, config, logger);
  }

  // read in the index, index raf already open
  static public PartitionCollection createTimePartitionFromIndex(String name, File directory, RandomAccessFile raf,
           FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    Grib2PartitionBuilderFromIndex builder = new Grib2PartitionBuilderFromIndex(name, directory, config, logger);
    if (builder.readIndex(raf)) {
      return builder.pc;
    }
    throw new IOException("Reading index failed");
  }


  //////////////////////////////////////////////////////////////////////////////////

  //private final PartitionManager tpc; // defines the partition
  private PartitionCollection pc;  // build this object

  private Grib2PartitionBuilderFromIndex(String name, File directory, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) {
    super(null, directory, config, logger);
    this.pc = new Grib2Partition(name, directory, config, logger);
    this.gc = pc;
  }

  @Override
  public String getMagicStart() {
    return Grib2PartitionBuilder.MAGIC_START;
  }

  ///////////////////////////////////////////////////////////////////////////
  // reading ncx

  /*
  extend GribCollection {
    repeated Gds gds = 100;
    repeated Dataset dataset = 101;
    repeated Partition partitions = 102;
    repeated Parameter pparams = 103;      // not used yet
  }
   */
  @Override
  protected boolean readExtensions(GribCollectionProto.GribCollection proto) {

    List<ucar.nc2.grib.collection.PartitionCollectionProto.Gds> gdsList = proto.getExtension(PartitionCollectionProto.gds);
    for (ucar.nc2.grib.collection.PartitionCollectionProto.Gds gdsProto : gdsList)
      makeGds(gdsProto);

    List<ucar.nc2.grib.collection.PartitionCollectionProto.Dataset> dsList = proto.getExtension(PartitionCollectionProto.dataset);
    for (ucar.nc2.grib.collection.PartitionCollectionProto.Dataset dsProto : dsList)
      makeDataset(dsProto);

    List<ucar.nc2.grib.collection.PartitionCollectionProto.Partition> partList = proto.getExtension(PartitionCollectionProto.partitions);
    for (ucar.nc2.grib.collection.PartitionCollectionProto.Partition partProto : partList)
      makePartition(partProto);

    return partList.size() > 0;
  }

      /*
  message Gds {
    optional bytes gds = 1;             // all variables in the group use the same GDS
    optional sint32 gdsHash = 2 [default = 0];
    optional string nameOverride = 3;         // only when user overrides default name
  }
   */
  private void makeGds(PartitionCollectionProto.Gds p) {
    byte[] rawGds = p.getGds().toByteArray();
    Grib2SectionGridDefinition gdss = new Grib2SectionGridDefinition(rawGds);
    Grib2Gds gds = gdss.getGDS();
    int gdsHash = (p.getGdsHash() != 0) ? p.getGdsHash() : gds.hashCode();
    String nameOverride = p.getNameOverride();
    pc.addHorizCoordSystem(gds.makeHorizCoordSys(), rawGds, gdsHash, nameOverride);
  }

    /*
  message Dataset {
      enum Type {
        TwoD = 0;
        Best = 1;
        Analysis = 2;
      }

    required Type type = 1;
    repeated Group groups = 2;      // separate group for each GDS
  }
   */
  private PartitionCollection.Dataset makeDataset(PartitionCollectionProto.Dataset p) {

    PartitionCollection.Dataset ds = pc.makeDataset(p.getType());

    ds.groups = new ArrayList<>(p.getGroupsCount());
    for (int i = 0; i < p.getGroupsCount(); i++)
      ds.groups.add( readGroup( p.getGroups(i)));
    ds.groups = Collections.unmodifiableList(ds.groups);

    return ds;
  }

  /*
  extend Group {
    required uint32 gdsIndex = 100;       // index into TimePartition.gds
    repeated uint32 run2part = 101;       // partitions only: run index to partition index map
    repeated Parameter gparams = 102;      // not used yet
  }
   */
  private GribCollection.GroupHcs readGroup(GribCollectionProto.Group p) {

    GribCollection.GroupHcs group = pc.makeGroup();

    super.readGroup(p, group);

    // extensions
    int gdsIndex = p.getExtension(PartitionCollectionProto.gdsIndex);
    group.horizCoordSys = pc.getHorizCS(gdsIndex);

    group.run2part = p.getExtension(PartitionCollectionProto.run2Part);

    return group;
  }

  /*
  extend Variable {
    repeated PartitionVariable partition = 100;
    repeated Parameter vparams = 101;    // not used yet
  }
   */
  @Override
  protected GribCollection.VariableIndex readVariableExtensions(GribCollectionProto.Variable proto, GribCollection.VariableIndex vi) {
    List<PartitionCollectionProto.PartitionVariable> pvList = proto.getExtension(PartitionCollectionProto.partition);

    PartitionCollection.VariableIndexPartitioned vip = pc.makeVariableIndexPartitioned(vi, pvList.size());
    /* vip.density = vi.density;   // ??
    vip.missing = vi.missing;
    vip.ndups = vi.ndups;
    vip.nrecords = vi.nrecords;  */

    for (PartitionCollectionProto.PartitionVariable pv : pvList) {
      vip.addPartition(pv.getPartno(), pv.getGroupno(), pv.getVarno(), pv.getFlag(), pv.getNdups(),
              pv.getNrecords(), pv.getMissing(), pv.getDensity());
    }

    return vip;
  }

  /*
message Partition {
  required string name = 1;       // name is used in TDS - eg the subdirectory when generated by TimePartitionCollections
  required string filename = 2;   // the gribCollection.ncx2 file
  required string directory = 3;   // top directory
  optional uint64 lastModified = 4;
}
   */
  private PartitionCollection.Partition makePartition(PartitionCollectionProto.Partition proto) {

    return pc.addPartition(proto.getName(), proto.getFilename(),
            proto.getLastModified(), proto.getDirectory());
  }


    /*

   gc.horizCS = new ArrayList<>(proto.getGdsCount());
  for (int i = 0; i < proto.getGdsCount(); i++)
    readGds(proto.getGds(i));
  gc.horizCS = Collections.unmodifiableList(gc.horizCS);

  gc.datasets = new ArrayList<>(proto.getDatasetCount());
  for (int i = 0; i < proto.getDatasetCount(); i++)
    readDataset(proto.getDataset(i));
  gc.datasets = Collections.unmodifiableList(gc.datasets);

  if (!readPartitions(proto, proto.getTopDir())) {
    logger.warn("Grib2CollectionBuilderFromIndex {}: has no partitions, force recreate ", gc.getName());
    return false;
  }

  protected TimeCoordUnion readTimePartition(GribCollectionProto.TimeCoordUnion pc, int timeIndex) {
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
      tc.setIndex( timeIndex);
      return tc;

    } else {
      List<Integer> coords = new ArrayList<Integer>(pc.getValuesCount());
      for (float value : pc.getValuesList())
        coords.add((int) value);
      TimeCoordUnion tc = new TimeCoordUnion(pc.getCode(), pc.getUnit(), coords, partition, index);
      tc.setIndex( timeIndex);
      return tc;
    }
  } */

}
