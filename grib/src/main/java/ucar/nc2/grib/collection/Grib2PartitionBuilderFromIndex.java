package ucar.nc2.grib.collection;

import thredds.featurecollection.FeatureCollectionConfig;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;

/**
 * Description
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
  static public Grib2Partition createTimePartitionFromIndex(String name, File directory, RandomAccessFile raf,
           FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    Grib2PartitionBuilderFromIndex builder = new Grib2PartitionBuilderFromIndex(name, directory, config, logger);
    if (builder.readIndex(raf)) {
      return builder.tp;
    }
    throw new IOException("Reading index failed");
  }


  //////////////////////////////////////////////////////////////////////////////////

  //private final PartitionManager tpc; // defines the partition
  private final Grib2Partition tp;  // build this object

  private Grib2PartitionBuilderFromIndex(String name, File directory, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) {
    super(null, directory, config, logger);
    this.tp = new Grib2Partition(name, directory, config, logger);
    this.gc = tp;
  }

  @Override
  public String getMagicStart() {
    return Grib2PartitionBuilder.MAGIC_START;
  }

  ///////////////////////////////////////////////////////////////////////////
  // reading ncx

  @Override
  protected boolean readPartitions(GribCollectionProto.GribCollectionIndex proto, String dirname) {
    for (int i = 0; i < proto.getPartitionsCount(); i++) {
      GribCollectionProto.Partition pp = proto.getPartitions(i);
      tp.addPartition(pp.getName(), pp.getFilename(), pp.getLastModified(), dirname);
    }
    return proto.getPartitionsCount() > 0;
  }

 @Override
  protected void readGroupPartitionInfo(GribCollection.GroupHcs group, GribCollectionProto.Group proto) {
   group.run2part = new int[proto.getRun2PartCount()];
    for (int i = 0; i < proto.getRun2PartCount(); i++) {
      group.run2part[i] = proto.getRun2Part(i);
    }
  }

  /* protected TimeCoordUnion readTimePartition(GribCollectionProto.TimeCoordUnion pc, int timeIndex) {
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

  @Override
  protected GribCollection.VariableIndex readVariable(GribCollectionProto.Variable pv, GribCollection.GroupHcs group) {
    GribCollection.VariableIndex vi = super.readVariable(pv, group);

    return tp.makeVariableIndexPartitioned(vi, pv.getPartitionList());
  }

}
