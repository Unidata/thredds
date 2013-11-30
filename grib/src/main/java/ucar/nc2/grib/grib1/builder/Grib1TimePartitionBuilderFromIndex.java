package ucar.nc2.grib.grib1.builder;

import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib1.Grib1TimePartition;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Read Grib1TimePartition from ncx partition index
 *
 * @author caron
 * @since 11/29/13
 */
public class Grib1TimePartitionBuilderFromIndex extends Grib1CollectionBuilderFromIndex {

      // read in the index, open raf
  static public GribCollection createTimePartitionFromIndex(String name, File directory, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    File idxFile = GribCollection.getIndexFile(name, directory);
    RandomAccessFile raf = new RandomAccessFile(idxFile.getPath(), "r");
    return createTimePartitionFromIndex(name, directory, raf, config, logger);
  }

  // read in the index, index raf already open
  static public Grib1TimePartition createTimePartitionFromIndex(String name, File directory, RandomAccessFile raf,
                                                                FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    Grib1TimePartitionBuilderFromIndex builder = new Grib1TimePartitionBuilderFromIndex(name, directory, config, logger);
    if (builder.readIndex(raf)) {
      return builder.tp;
    }
    throw new IOException("Reading index failed");
  }


  //////////////////////////////////////////////////////////////////////////////////

  private final Grib1TimePartition tp;  // build this object

  private Grib1TimePartitionBuilderFromIndex(String name, File directory, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) {
    super(null, false, logger);
    this.tp = new Grib1TimePartition(name, directory, config, logger);
    this.gc = tp;
  }

  @Override
  public String getMagicStart() {
    return Grib1TimePartitionBuilder.MAGIC_START;
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

}
