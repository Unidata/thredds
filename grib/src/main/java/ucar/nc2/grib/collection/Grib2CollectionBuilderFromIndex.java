package ucar.nc2.grib.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.*;
import ucar.arr.Coordinate;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.stream.NcStream;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Build a GribCollection object for Grib-2 files. Only from ncx files.
 * No updating, no nuthin.
 *
 * @author caron
 * @since 11/9/13
 */
public class Grib2CollectionBuilderFromIndex extends GribCollectionBuilder {

  // read in the index, open raf
  static public GribCollection readFromIndex(String name, File directory, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    File idxFile = GribCollection.getIndexFile(name, directory);
    RandomAccessFile raf = new RandomAccessFile(idxFile.getPath(), "r");
    return readFromIndex(name, directory, raf, config, logger);
  }

  // read in the index, index raf already open
  static public GribCollection readFromIndex(String name, File directory, RandomAccessFile raf, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilderFromIndex builder = new Grib2CollectionBuilderFromIndex(name, directory, config, logger);
    if (builder.readIndex(raf))
      return builder.gc;
    throw new IOException("Reading index failed"); // or return null ??
  }

  ////////////////////////////////////////////////////////////////

  protected GribCollection gc;
  protected Grib2Customizer tables; // only gets created in makeAggGroups

  protected Grib2CollectionBuilderFromIndex(String name, File directory, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) {
    super(null, false, logger);
    this.gc = new Grib2Collection(name, directory, config);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // reading

  protected String getMagicStart() {
    return Grib2CollectionBuilder.MAGIC_START;
  }

  protected boolean readIndex(RandomAccessFile raf) {

    gc.setIndexRaf(raf); // LOOK leaving the raf open in the GribCollection
    try {
      raf.order(RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);

      //// header message
      if (!NcStream.readAndTest(raf, getMagicStart().getBytes())) {
        raf.seek(0);
        NcStream.readAndTest(raf, getMagicStart().getBytes()); // debug
        logger.error("Grib2Collection {}: invalid index magic", gc.getName());
        return false;
      }

      gc.version = raf.readInt();
      boolean versionOk = isSingleFile ? gc.version >= Grib2CollectionBuilder.minVersionSingle : gc.version >= Grib2CollectionBuilder.version;
      if (!versionOk) {
        logger.warn("Grib2Collection {}: index found version={}, want version= {} on file {}", gc.getName(), gc.version, Grib2CollectionBuilder.version, raf.getLocation());
        return false;
      }

      long skip = raf.readLong();
      raf.skipBytes(skip);

      int size = NcStream.readVInt(raf);
      if ((size < 0) || (size > 100 * 1000 * 1000)) {
        logger.warn("Grib2Collection {}: invalid index size", gc.getName());
        return false;
      }

      byte[] m = new byte[size];
      raf.readFully(m);

      GribCollectionProto.GribCollectionIndex proto = GribCollectionProto.GribCollectionIndex.parseFrom(m);

      gc.center = proto.getCenter();
      gc.subcenter = proto.getSubcenter();
      gc.master = proto.getMaster();
      gc.local = proto.getLocal();
      gc.genProcessType = proto.getGenProcessType();
      gc.genProcessId = proto.getGenProcessId();
      gc.backProcessId = proto.getBackProcessId();
      gc.local = proto.getLocal();
      this.tables = Grib2Customizer.factory(gc.center, gc.subcenter, gc.master, gc.local);

      File dir = gc.getDirectory();
      File protoDir = new File(proto.getTopDir());
      if (dir != null && protoDir != null && !dir.getCanonicalPath().equals(protoDir.getCanonicalPath())) {
        logger.info("Grib2Collection {}: has different directory= {} than index= {} ", gc.getName(), dir.getCanonicalPath(), protoDir.getCanonicalPath());
        //return false;
      }
      gc.setDirectory(new File(proto.getTopDir()));

      // see if its a partition
      boolean isPartition = proto.getPartitionsCount() > 0;

      // get mfile list
      if (!isPartition) {
        int n = proto.getMfilesCount();
        if (n == 0) {
          logger.warn("Grib2Collection {}: has no files, force recreate ", gc.getName());
          return false;
        } else {
          List<MFile> files = new ArrayList<MFile>(n);
          for (int i = 0; i < n; i++) {
            ucar.nc2.grib.collection.GribCollectionProto.MFile mf = proto.getMfiles(i);
            files.add(new GribCollectionBuilder.GcMFile(dir, mf.getFilename(), mf.getLastModified()));
          }
          gc.setFiles(files);
          //if (dcm != null) dcm.setFiles(files);  // LOOK !!
        }
      }

      gc.groups = new ArrayList<GribCollection.GroupHcs>(proto.getGroupsCount());
      for (int i = 0; i < proto.getGroupsCount(); i++)
        gc.groups.add(readGroup(proto.getGroups(i), gc.makeGroup()));
      gc.groups = Collections.unmodifiableList(gc.groups);

      gc.params = new ArrayList<Parameter>(proto.getParamsCount());
      for (int i = 0; i < proto.getParamsCount(); i++)
        gc.params.add(readParam(proto.getParams(i)));

      if (!readPartitions(proto, proto.getTopDir())) {
        logger.warn("Time2Partition {}: has no partitions, force recreate ", gc.getName());
        return false;
      }

      return true;

    } catch (Throwable t) {
      logger.error("Error reading index " + raf.getLocation(), t);
      t.printStackTrace();
      return false;
    }
  }

  protected boolean readPartitions(GribCollectionProto.GribCollectionIndex proto, String dirname) {
    return true;
  }

  protected void readTimePartitions(GribCollection.GroupHcs group, GribCollectionProto.Group proto) {
    // NOOP
  }

    /*
message Group {
  optional bytes gds = 1;             // all variables in the group use the same GDS
  optional sint32 gdsHash = 2 [default = 0];
  optional string nameOverride = 3;         // only when user overrides default name

  repeated Variable variables = 4;    // list of variables
  repeated Coord coords = 5;          // list of coordinates
  repeated Parameter params = 6;      // group attributes  used ??
  repeated int32 fileno = 7;          // the component files that are in this group, index into gc.files

  // partitions
  repeated TimeCoordUnion timeCoordUnions = 10; // partitions only
}
   */
  GribCollection.GroupHcs readGroup(GribCollectionProto.Group p, GribCollection.GroupHcs group) throws IOException {

    byte[] rawGds = p.getGds().toByteArray();
    Grib2SectionGridDefinition gdss = new Grib2SectionGridDefinition(rawGds);
    Grib2Gds gds = gdss.getGDS();
    int gdsHash = (p.getGdsHash() != 0) ? p.getGdsHash() : gds.hashCode();
    group.setHorizCoordSystem(gds.makeHorizCoordSys(), rawGds, gdsHash);

    group.varIndex = new ArrayList<>();
    for (int i = 0; i < p.getVariablesCount(); i++)
      group.varIndex.add( readVariable(p.getVariables(i), group));

    group.coords = new ArrayList<>();
    for (int i = 0; i < p.getCoordsCount(); i++)
       group.coords.add( readCoord(p.getCoords(i)));

    group.filenose = new int[p.getFilenoCount()];
    for (int i = 0; i < p.getFilenoCount(); i++)
      group.filenose[i] = p.getFileno(i);

    readTimePartitions(group, p);

    // assign names, units to coordinates
    CalendarDate firstRef = null;
    int timeCoord = 0;
    List<CoordinateVert> vertCoords = new ArrayList<>();
    for (Coordinate coord : group.coords) {
      Coordinate.Type type = coord.getType();
      switch (type) {
        case runtime:
          firstRef = ((CoordinateRuntime)coord).getFirstDate();
          break;

        case time:
          CoordinateTime tc = (CoordinateTime) coord;
          if (timeCoord > 0) tc.setName("time"+timeCoord);
          timeCoord++;
          tc.setTimeUnit(Grib2Utils.getCalendarPeriod( tables.convertTimeUnit( tc.getCode())));
          tc.setRefDate(firstRef);
          break;

        case timeIntv:
          CoordinateTimeIntv tci = (CoordinateTimeIntv) coord;
          if (timeCoord > 0) tci.setName("time"+timeCoord);
          timeCoord++;
          tci.setTimeUnit(Grib2Utils.getCalendarPeriod( tables.convertTimeUnit( tci.getCode())));
          tci.setRefDate(firstRef);
          break;

        case vert:
          vertCoords.add((CoordinateVert) coord);
          break;
      }
    }
    assignVertNames(vertCoords);

    return group;
  }

  public void assignVertNames(List<CoordinateVert> vertCoords) {
    Map<String, Integer> map = new HashMap<>(2*vertCoords.size());

    // assign name
    for (CoordinateVert vc : vertCoords) {
      String shortName = tables.getLevelNameShort(vc.getCode());
      if (vc.isLayer()) shortName = shortName + "_layer";

      Integer countName = map.get(shortName);
      if (countName == null) {
        map.put(shortName, 0);
      } else {
        countName++;
        map.put(shortName, countName);
        shortName = shortName + countName;
      }

      vc.setName(shortName);
    }

  }


  private Parameter readParam(GribCollectionProto.Parameter pp) throws IOException {
    if (pp.hasSdata())
      return new Parameter(pp.getName(), pp.getSdata());

    int count = 0;
    double[] vals = new double[pp.getDataCount()];
    for (double val : pp.getDataList())
      vals[count++] = val;

    return new Parameter(pp.getName(), vals);
  }

    /*
  message Coord {
    required int32 type = 1;   // Coordinate.Type.oridinal
    required int32 code = 2;   // time unit; level type
    required string unit = 3;
    repeated float values = 4;
    repeated float bound = 5; // only used if interval, then = (value, bound)
    repeated int64 msecs = 6; // calendar date
  }
   */
  private Coordinate readCoord(GribCollectionProto.Coord pc) throws IOException {
    int typei = pc.getType();
    int code = pc.getCode();
    String unit = pc.hasUnit() ? pc.getUnit() : null;
    Coordinate.Type type = Coordinate.Type.values()[typei];
    switch (type) {
      case runtime:
        List<CalendarDate> dates = new ArrayList<>(pc.getMsecsCount());
        for (Long msec : pc.getMsecsList())
          dates.add(CalendarDate.of(msec));
        return new CoordinateRuntime(dates);

      case time:
        List<Integer> offs = new ArrayList<>(pc.getValuesCount());
        for (float val : pc.getValuesList())
          offs.add((int) val);
        return new CoordinateTime(offs, code);

      case timeIntv:
        List<TimeCoord.Tinv> tinvs = new ArrayList<>(pc.getValuesCount());
        for (int i=0; i<pc.getValuesCount(); i++) {
          int val1 = (int) pc.getValues(i);
          int val2 = (int) pc.getBound(i);
          tinvs.add( new TimeCoord.Tinv(val1, val2));
        }
        return new CoordinateTimeIntv(tinvs, code);

      case vert:
        VertCoord.VertUnit vertUnit = Grib2Utils.getLevelUnit(code);
        boolean isLayer = vertUnit.isLayer();
        List<VertCoord.Level> levels = new ArrayList<>(pc.getValuesCount());
        for (int i=0; i<pc.getValuesCount(); i++) {
          double val1 = pc.getValues(i);
          double val2 =  isLayer ? pc.getBound(i) : GribNumbers.UNDEFINEDD;
          levels.add( new VertCoord.Level(val1, val2, isLayer));
        }
        return new CoordinateVert(levels, code);
    }
    throw new IllegalStateException("Unknow Coordinate type = "+type);
  }

    /*
message Variable {
  required uint32 discipline = 1;
  required bytes pds = 2;
  required fixed32 cdmHash = 3;

  required uint64 recordsPos = 4;  // offset of SparseArray message for this Variable
  required uint32 recordsLen = 5;  // size of SparseArray message for this Variable (could be in stream instead)

  repeated uint32 coordIdx = 6;    // index into Group.coords

  // partitions
  repeated uint32 groupno = 10;
  repeated uint32 varno = 11;
  repeated int32 flag = 12;
}
   */
  protected GribCollection.VariableIndex readVariable(GribCollectionProto.Variable pv, GribCollection.GroupHcs group) {
    byte[] rawPds = pv.getPds().toByteArray();
    Grib2SectionProductDefinition pdss = new Grib2SectionProductDefinition(rawPds);
    Grib2Pds pds = null;
    try {
      pds = pdss.getPDS();
    } catch (IOException e) {
      e.printStackTrace();  // cant happen
      logger.error("failed to read PDS");
    }

    int discipline = pv.getDiscipline();
    int cdmHash = pv.getCdmHash();
    long recordsPos = pv.getRecordsPos();
    int recordsLen = pv.getRecordsLen();
    List<Integer> index = pv.getCoordIdxList();

    return gc.makeVariableIndex(group, cdmHash, discipline, rawPds, pds, index, recordsPos, recordsLen);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////
 // static private final Logger logger = LoggerFactory.getLogger(Grib2CollectionBuilderFromIndex.class);

  /*
  private static GribCollection doOne(File dir, String filename, FeatureCollectionConfig config) throws IOException {
    long start = System.currentTimeMillis();
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    GribCollection gc = Grib2CollectionBuilderFromIndex.createFromIndex("test", dir, raf, config.gribConfig, logger);
    long took = System.currentTimeMillis() - start;
    System.out.printf("that took %s msecs%n", took);
    return gc;
  }

 private static Grib2TimePartition doOnePart(File dir, String filename, FeatureCollectionConfig config) throws IOException {
    long start = System.currentTimeMillis();
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    Grib2TimePartition tp = Grib2TimePartitionBuilderFromIndex.createTimePartitionFromIndex("test", dir, raf, config.gribConfig, logger);
    //GribCollection gc = Grib2TimePartitionBuilderFromIndex.createFromIndex("test", dir, raf, config.gribConfig, logger);
    long took = System.currentTimeMillis() - start;
    System.out.printf("that took %s msecs%n", took);
    return tp;
  }

  public static void main(String[] args) throws IOException {

    File cat = new File("B:/ndfd/catalog.xml");
    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(cat);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    long start = System.currentTimeMillis();

    File dir = new File("B:/ndfd/200901/");
    FeatureCollectionConfig config = FeatureCollectionReader.readFeatureCollection(doc.getRootElement());

    Grib2TimePartition tp = doOnePart(dir, "B:/ndfd/200901/ncdc1Year-200901.ncx", config);

    for (TimePartition.Partition part : tp.getPartitions()) {
      long start2 = System.currentTimeMillis();
      GribCollection gc = part.getGribCollection();
      System.out.printf("%s%n", gc);
      gc.close();
      long took2 = System.currentTimeMillis() - start2;
      System.out.printf("that took %s msecs%n", took2);
    }
    tp.close();

    long took = System.currentTimeMillis() - start;
    System.out.printf("that all took %s msecs%n", took);
  } */
}

