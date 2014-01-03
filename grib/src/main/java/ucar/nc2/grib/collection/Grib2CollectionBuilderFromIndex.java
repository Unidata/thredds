package ucar.nc2.grib.collection;

import com.google.protobuf.ExtensionRegistry;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.*;
import ucar.sparr.Coordinate;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.stream.NcStream;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Build a GribCollection object for Grib-2 files. Only from ncx files.
 * No updating, no nuthin.
 * Data file is not opened.
 *
 * @author caron
 * @since 11/9/13
 */
public class Grib2CollectionBuilderFromIndex extends GribCollectionBuilder {

  // read in the index, open raf and leave open in the GribCollection
  static public GribCollection readFromIndex(String idxFilename, File directory, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    File idxFile = GribCollection.getIndexFile(idxFilename, directory);
    RandomAccessFile raf = new RandomAccessFile(idxFile.getPath(), "r");
    return readFromIndex(idxFilename, directory, raf, config, logger);
  }

  // read in the index, index raf already open
  static public GribCollection readFromIndex(String idxFilename, File directory, RandomAccessFile raf, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {

    Grib2CollectionBuilderFromIndex builder = new Grib2CollectionBuilderFromIndex(idxFilename, directory, config, logger);
    if (!builder.readIndex(raf))
      throw new IOException("Reading index failed"); // or return null ??

    if (builder.gc.getFiles().size() == 0) {
      logger.warn("Grib2CollectionBuilderFromIndex {}: has no files, force recreate ", builder.gc.getName());
      return null;
    }

    return builder.gc;
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
        logger.error("Grib2CollectionBuilderFromIndex {}: invalid index magic", gc.getName());
        return false;
      }

      gc.version = raf.readInt();
      boolean versionOk = isSingleFile ? gc.version >= Grib2CollectionBuilder.minVersionSingle : gc.version >= Grib2CollectionBuilder.version;
      if (!versionOk) {
        logger.warn("Grib2CollectionBuilderFromIndex {}: index found version={}, want version= {} on file {}", gc.getName(), gc.version, Grib2CollectionBuilder.version, raf.getLocation());
        return false;
      }

      long skip = raf.readLong();
      raf.skipBytes(skip);

      int size = NcStream.readVInt(raf);
      if ((size < 0) || (size > 100 * 1000 * 1000)) {
        logger.warn("Grib2CollectionBuilderFromIndex {}: invalid index size", gc.getName());
        return false;
      }

      byte[] m = new byte[size];
      raf.readFully(m);

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
       */

      // see https://developers.google.com/protocol-buffers/docs/reference/java-generated#extension */
      ExtensionRegistry registry = ExtensionRegistry.newInstance();
      PartitionCollectionProto.registerAllExtensions(registry);
      GribCollectionProto.GribCollection proto = GribCollectionProto.GribCollection.parseFrom(m, registry);

      // need to read this first to get this.tables initialized
      gc.center = proto.getCenter();
      gc.subcenter = proto.getSubcenter();
      gc.master = proto.getMaster();
      gc.local = proto.getLocal();
      gc.genProcessType = proto.getGenProcessType();
      gc.genProcessId = proto.getGenProcessId();
      gc.backProcessId = proto.getBackProcessId();
      gc.local = proto.getLocal();
      this.tables = Grib2Customizer.factory(gc.center, gc.subcenter, gc.master, gc.local);

      if (!gc.name.equals(proto.getName())) {
        logger.info("Grib2CollectionBuilderFromIndex {}: has different name= {} than index= {} ", raf.getLocation(), gc.getName(), proto.getName());
      }
      File dir = gc.getDirectory();
      File protoDir = new File(proto.getTopDir());
      if (dir != null && protoDir != null && !dir.getCanonicalPath().equals(protoDir.getCanonicalPath())) {
        logger.info("Grib2CollectionBuilderFromIndex {}: has different directory= {} than index= {} ", gc.getName(), dir.getCanonicalPath(), protoDir.getCanonicalPath());
        //return false;
      }
      if (gc.getDirectory() == null)
        gc.setDirectory(protoDir);

      int n = proto.getMfilesCount();
      List<MFile> files = new ArrayList<>(n);
      for (int i = 0; i < n; i++) {
        ucar.nc2.grib.collection.GribCollectionProto.MFile mf = proto.getMfiles(i);
        files.add(new GribCollectionBuilder.GcMFile(dir, mf.getFilename(), mf.getLastModified()));
      }
      gc.setFiles(files);
      //if (dcm != null) dcm.setFiles(files);  // LOOK !!

      gc.groups = new ArrayList<GribCollection.GroupHcs>(proto.getGroupsCount());
      for (int i = 0; i < proto.getGroupsCount(); i++)
        gc.groups.add(readGroup(proto.getGroups(i), gc.makeGroup()));
      gc.groups = Collections.unmodifiableList(gc.groups);

      return readExtensions(proto);

    } catch (Throwable t) {
      logger.error("Error reading index " + raf.getLocation(), t);
      t.printStackTrace();
      return false;
    }
  }

  protected boolean readExtensions(GribCollectionProto.GribCollection proto) {
    return true;
  }

  protected GribCollection.VariableIndex readVariableExtensions(GribCollection.GroupHcs group, GribCollectionProto.Variable pv, GribCollection.VariableIndex vi) {
    return vi;
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
 */
  protected GribCollection.GroupHcs readGroup(GribCollectionProto.Group p, GribCollection.GroupHcs group) {

    if (p.hasGds()) {
      byte[] rawGds = p.getGds().toByteArray();
      Grib2SectionGridDefinition gdss = new Grib2SectionGridDefinition(rawGds);
      Grib2Gds gds = gdss.getGDS();
      int gdsHash = (p.getGdsHash() != 0) ? p.getGdsHash() : gds.hashCode();  // LOOK
      group.setHorizCoordSystem(gds.makeHorizCoordSys(), rawGds, gdsHash, null);    // LOOK nameOverrride
    }

    // read coords before variables
    group.coords = new ArrayList<>();
    for (int i = 0; i < p.getCoordsCount(); i++)
      group.coords.add(readCoord(p.getCoords(i)));

    group.filenose = new int[p.getFilenoCount()];
    for (int i = 0; i < p.getFilenoCount(); i++)
      group.filenose[i] = p.getFileno(i);

    for (int i = 0; i < p.getVariablesCount(); i++)
      group.addVariable(readVariable(group, p.getVariables(i)));

    // assign names, units to coordinates
    CalendarDate firstRef = null;
    int timeCoord = 0;
    List<CoordinateVert> vertCoords = new ArrayList<>();
    for (Coordinate coord : group.coords) {
      Coordinate.Type type = coord.getType();
      switch (type) {
        case runtime:
          firstRef = ((CoordinateRuntime) coord).getFirstDate();
          break;

        case time:
          CoordinateTime tc = (CoordinateTime) coord;
          if (timeCoord > 0) tc.setName("time" + timeCoord);
          timeCoord++;
          tc.setTimeUnit(Grib2Utils.getCalendarPeriod(tables.convertTimeUnit(tc.getCode())));
          //tc.setRefDate(firstRef);
          break;

        case timeIntv:
          CoordinateTimeIntv tci = (CoordinateTimeIntv) coord;
          if (timeCoord > 0) tci.setName("time" + timeCoord);
          timeCoord++;
          tci.setTimeUnit(Grib2Utils.getCalendarPeriod(tables.convertTimeUnit(tci.getCode())));
          //tci.setRefDate(firstRef);
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
    Map<String, Integer> map = new HashMap<>(2 * vertCoords.size());

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
  private Coordinate readCoord(GribCollectionProto.Coord pc) {
    int typei = pc.getType();
    int code = pc.getCode();
    String unit = pc.hasUnit() ? pc.getUnit() : null;  // LOOK
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
        for (int i = 0; i < pc.getValuesCount(); i++) {
          int val1 = (int) pc.getValues(i);
          int val2 = (int) pc.getBound(i);
          tinvs.add(new TimeCoord.Tinv(val1, val2));
        }
        return new CoordinateTimeIntv(tinvs, code);

      case vert:
        boolean isLayer = pc.getValuesCount() == pc.getBoundCount();
        List<VertCoord.Level> levels = new ArrayList<>(pc.getValuesCount());
        for (int i = 0; i < pc.getValuesCount(); i++) {
          double val1 = pc.getValues(i);
          double val2 = isLayer ? pc.getBound(i) : GribNumbers.UNDEFINEDD;
          levels.add(new VertCoord.Level(val1, val2, isLayer));
        }
        return new CoordinateVert(levels, code);
    }
    throw new IllegalStateException("Unknown Coordinate type = " + type);
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
 */
  protected GribCollection.VariableIndex readVariable(GribCollection.GroupHcs group, GribCollectionProto.Variable pv) {
    int discipline = pv.getDiscipline();

    byte[] rawPds = pv.getPds().toByteArray();
    Grib2SectionProductDefinition pdss = new Grib2SectionProductDefinition(rawPds);
    Grib2Pds pds = null;
    try {
      pds = pdss.getPDS();
    } catch (IOException e) {
      e.printStackTrace();  // cant happen
      logger.error("Grib2CollectionBuilderFromIndex: failed to read PDS");
    }

    int cdmHash = pv.getCdmHash();
    long recordsPos = pv.getRecordsPos();
    int recordsLen = pv.getRecordsLen();
    List<Integer> index = pv.getCoordIdxList();

    GribCollection.VariableIndex result = gc.makeVariableIndex(group, cdmHash, discipline, rawPds, pds, index, recordsPos, recordsLen);
    result.density = pv.getDensity();
    result.ndups = pv.getNdups();
    result.nrecords = pv.getNrecords();
    result.missing = pv.getMissing();

    return readVariableExtensions(group, pv, result);
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

