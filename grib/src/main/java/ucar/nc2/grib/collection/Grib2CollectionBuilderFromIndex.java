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
import ucar.sparr.CoordinateTwoTimer;
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
        required string name = 1;         // must be unique - index filename is name.ncx
        required string topDir = 2;       // filenames are reletive to this
        repeated MFile mfiles = 3;        // list of grib MFiles
        repeated Dataset dataset = 4;
        repeated Gds gds = 5;             // unique Gds, shared amongst datasets

        required int32 center = 6;      // these 4 fields are to get a GribTable object
        required int32 subcenter = 7;
        required int32 master = 8;
        required int32 local = 9;       // grib1 table Version

        optional int32 genProcessType = 10;
        optional int32 genProcessId = 11;
        optional int32 backProcessId = 12;

        repeated Parameter params = 20;      // not used yet

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
      if (dir != null && !dir.getCanonicalPath().equals(protoDir.getCanonicalPath())) {
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

      gc.horizCS = new ArrayList<>(proto.getGdsCount());
      for (int i = 0; i < proto.getGdsCount(); i++)
         readGds(proto.getGds(i));
      gc.horizCS = Collections.unmodifiableList(gc.horizCS); // must be in order

      gc.datasets = new ArrayList<>(proto.getDatasetCount());
      for (int i = 0; i < proto.getDatasetCount(); i++)
         readDataset(proto.getDataset(i));

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
    group.addVariable(vi);
    return vi;
  }

  private void readGds(GribCollectionProto.Gds p) {
    byte[] rawGds = p.getGds().toByteArray();
    Grib2SectionGridDefinition gdss = new Grib2SectionGridDefinition(rawGds);
    Grib2Gds gds = gdss.getGDS();
    int gdsHash = (p.getGdsHash() != 0) ? p.getGdsHash() : gds.hashCode();
    String nameOverride = p.hasNameOverride() ? p.getNameOverride() : null;
    gc.addHorizCoordSystem(gds.makeHorizCoordSys(), rawGds, gdsHash, nameOverride);
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
  private PartitionCollection.Dataset readDataset(GribCollectionProto.Dataset p) {

    GribCollection.Dataset ds = gc.makeDataset(p.getType());

    ds.groups = new ArrayList<>(p.getGroupsCount());
    for (int i = 0; i < p.getGroupsCount(); i++)
      ds.groups.add( readGroup( p.getGroups(i)));
    ds.groups = Collections.unmodifiableList(ds.groups);

    return ds;
  }

  /*
message Group {
  required uint32 gdsIndex = 1;       // index into GribCollection.gds array
  repeated Variable variables = 2;    // list of variables
  repeated Coord coords = 3;          // list of coordinates
  repeated int32 fileno = 4;          // the component files that are in this group, index into gc.mfiles

  repeated Parameter params = 20;      // not used yet
  extensions 100 to 199;
}
 */
  protected GribCollection.GroupHcs readGroup(GribCollectionProto.Group p) {
    GribCollection.GroupHcs group = gc.makeGroup();

    int gdsIndex = p.getGdsIndex();
    group.horizCoordSys = gc.getHorizCS(gdsIndex);

    // read coords before variables
    group.coords = new ArrayList<>();
    for (int i = 0; i < p.getCoordsCount(); i++)
      group.coords.add(readCoord(p.getCoords(i)));

    group.filenose = new int[p.getFilenoCount()];
    for (int i = 0; i < p.getFilenoCount(); i++)
      group.filenose[i] = p.getFileno(i);

    for (int i = 0; i < p.getVariablesCount(); i++)
      readVariable(group, p.getVariables(i));

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

        case time2D:
          CoordinateTime2D t2d = (CoordinateTime2D) coord;
          if (timeCoord > 0) t2d.setName("time" + timeCoord);
          timeCoord++;
          t2d.setTimeUnit(Grib2Utils.getCalendarPeriod(tables.convertTimeUnit(t2d.getCode())));
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

      case time2D:
        dates = new ArrayList<>(pc.getMsecsCount());
        for (Long msec : pc.getMsecsList())
          dates.add(CalendarDate.of(msec));
        CoordinateRuntime runtime = new CoordinateRuntime(dates);

        List<Coordinate> times = new ArrayList<>(pc.getTimesCount());
        for (GribCollectionProto.Coord coordp : pc.getTimesList())
          times.add( readCoord(coordp));
        return new CoordinateTime2D(null, runtime, times, code);
    }
    throw new IllegalStateException("Unknown Coordinate type = " + type);
  }

  /*
message Variable {
   required uint32 discipline = 1;
   required bytes pds = 2;          // raw pds
   required fixed32 cdmHash = 3;

   required uint64 recordsPos = 4;  // offset of SparseArray message for this Variable
   required uint32 recordsLen = 5;  // size of SparseArray message for this Variable

   repeated uint32 coordIdx = 6;    // indexes into Group.coords

   // optionally keep stats
   optional float density = 7;
   optional uint32 ndups = 8;
   optional uint32 nrecords = 9;
   optional uint32 missing = 10;

   repeated uint32 invCount = 15;      // for Coordinate TwoTimer, only 2D vars
   repeated uint32 time2runtime = 16;  // time index to runtime index, only 1D vars
   repeated Parameter params = 20;    // not used yet

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

    // LOOK  invCount, time2runtime
    Coordinate runtime = result.getCoordinate(Coordinate.Type.runtime);
    Coordinate time = result.getCoordinate(Coordinate.Type.time);
    if (time == null) time = result.getCoordinate(Coordinate.Type.timeIntv);

    // 2d only
    List<Integer> invCountList = pv.getInvCountList();
    if (invCountList.size() > 0) {
      result.twot = new CoordinateTwoTimer(invCountList);
      result.twot.setSize(runtime.getSize(), time.getSize());
      result.isTwod = true;
    }

    // 1d only
    List<Integer> time2runList = pv.getTime2RuntimeList();
    if (time2runList.size() > 0) {
      result.time2runtime = new int[time2runList.size()];
      int count = 0;
      for (int idx : time2runList) result.time2runtime[count++] = idx;
      result.isTwod = false;
    }

    return readVariableExtensions(group, pv, result);
  }

}

