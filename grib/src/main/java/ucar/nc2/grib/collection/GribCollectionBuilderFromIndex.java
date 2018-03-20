/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import thredds.featurecollection.FeatureCollectionConfig;
import com.google.protobuf.ExtensionRegistry;
import thredds.inventory.MFile;
import ucar.coord.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.*;
import ucar.nc2.stream.NcStream;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.time.CalendarPeriod;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.*;

/**
 * Superclass to read GribCollection From ncx file.
 *
 * @author caron
 * @since 2/20/14
 */
abstract class GribCollectionBuilderFromIndex {
  static protected final boolean debug = false;
  static protected final boolean stackTrace = true;

  protected GribCollectionMutable gc;
  protected final org.slf4j.Logger logger;
  protected final FeatureCollectionConfig config;
  protected GribTables tables;

  protected abstract GribHorizCoordSystem readGds(GribCollectionProto.Gds p);
  protected abstract GribTables makeCustomizer() throws IOException;
  protected abstract String getLevelNameShort(int levelCode);
  protected abstract int getVersion();
  protected abstract int getMinVersion();

  protected GribCollectionBuilderFromIndex(GribCollectionMutable gc, FeatureCollectionConfig config, org.slf4j.Logger logger) {
    this.logger = logger;
    this.config = config;
    this.gc = gc;
  }

  protected abstract String getMagicStart();

  protected boolean readIndex(RandomAccessFile raf) throws IOException {

    gc.setIndexRaf(raf);
    try {
      raf.order(RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);

      //// header message
      if (!NcStream.readAndTest(raf, getMagicStart().getBytes(CDM.utf8Charset))) {
        raf.seek(0);
        NcStream.readAndTest(raf, getMagicStart().getBytes(CDM.utf8Charset)); // debug
        logger.warn("GribCollectionBuilderFromIndex {}: invalid index raf={}", gc.getName(), raf.getLocation());
        throw new IllegalStateException();   // temp debug
        // return false;
      }

      gc.version = raf.readInt();
      if (gc.version < getVersion()) {
        logger.debug("GribCollectionBuilderFromIndex {}: index found version={}, current version= {} on file {}", gc.getName(), gc.version, Grib2CollectionWriter.version, raf.getLocation());
        // throw new IllegalStateException();   // temp debug
        if (gc.version < getMinVersion()) return false;
      }

      // these are the variable records
      long skip = raf.readLong();
      raf.skipBytes(skip);
      if (debug) System.out.printf("GribCollectionBuilderFromIndex %s (%s) records len = %d%n", raf.getLocation(), getMagicStart(), skip);

      int size = NcStream.readVInt(raf);
      if ((size < 0) || (size > 300 * 1000 * 1000)) { // ncx bigger than 300 MB?
        logger.warn("GribCollectionBuilderFromIndex {}: invalid index size", gc.getName(), raf.getLocation());
        throw new IllegalStateException();   // temp debug
        //return false;
      }
      if (debug) System.out.printf("GribCollectionBuilderFromIndex proto len = %d%n", size);

      byte[] m = new byte[size];
      raf.readFully(m);

      /*
      message GribCollection {
        required string name = 1;         // must be unique - index filename is name.ncx
        required string topDir = 2;       // filenames are reletive to this
        repeated MFile mfiles = 3;        // list of grib MFiles
        repeated Dataset dataset = 4;
        repeated Gds gds = 5;             // unique Gds, shared amongst datasets
        required Coord masterRuntime = 21;  // list of runtimes in this GC

        required int32 center = 6;      // these 4 fields are to get a GribTable object
        required int32 subcenter = 7;
        required int32 master = 8;
        required int32 local = 9;       // grib1 table Version

        optional int32 genProcessType = 10;
        optional int32 genProcessId = 11;
        optional int32 backProcessId = 12;

        // repeated Parameter params = 20;      // not used
        FcConfig config = 21;
        uint64 startTime = 22; // calendar date, first valid time
        uint64 endTime = 23;   // calendar date, last valid time

        extensions 100 to 199;
      }
       */

      GribCollectionProto.GribCollection proto = GribCollectionProto.GribCollection.parseFrom(m);

      // need to read this first to get this.tables initialized
      gc.center = proto.getCenter();
      gc.subcenter = proto.getSubcenter();
      gc.master = proto.getMaster();
      gc.local = proto.getLocal();
      gc.genProcessType = proto.getGenProcessType();
      gc.genProcessId = proto.getGenProcessId();
      gc.backProcessId = proto.getBackProcessId();
      gc.local = proto.getLocal();
      this.tables = makeCustomizer();
      gc.cust = this.tables;

      if (!gc.name.equals(proto.getName())) {
        logger.info("GribCollectionBuilderFromIndex raf {}: has different name= '{}' than stored in ncx= '{}' ", raf.getLocation(), gc.getName(), proto.getName());
      }

      // directory always taken from proto, since ncx2 file may be moved, or in cache, etc  LOOK
      gc.directory = gc.setOrgDirectory(proto.getTopDir());
      gc.indexVersion = proto.getVersion();

      gc.setCalendarDateRange(proto.getStartTime(), proto.getEndTime());

      int fsize = 0;
      int n = proto.getMfilesCount();
      Map<Integer, MFile> fileMap = new HashMap<>(2 * n);
      for (int i = 0; i < n; i++) {
        ucar.nc2.grib.collection.GribCollectionProto.MFile mf = proto.getMfiles(i);
        fileMap.put(mf.getIndex(), new GcMFile(gc.directory, mf.getFilename(), mf.getLastModified(), mf.getLength(), mf.getIndex()));
        fsize += mf.getFilename().length();
      }
      gc.setFileMap(fileMap);
      if (debug) System.out.printf("GribCollectionBuilderFromIndex files len = %d%n", fsize);

      gc.masterRuntime = (CoordinateRuntime) readCoord(proto.getMasterRuntime());

      gc.datasets = new ArrayList<>(proto.getDatasetCount());
      for (int i = 0; i < proto.getDatasetCount(); i++)
        readDataset(proto.getDataset(i));

      return readExtensions(proto);

    } catch (Throwable t) {
      logger.warn("Error reading index " + raf.getLocation(), t);
      if (stackTrace) t.printStackTrace();
      return false;
    }
  }

  protected boolean readExtensions(GribCollectionProto.GribCollection proto) {
    return true;
  }

  protected GribCollectionMutable.VariableIndex readVariableExtensions(GribCollectionMutable.GroupGC group, GribCollectionProto.Variable pv, GribCollectionMutable.VariableIndex vi) {
    group.addVariable(vi);
    return vi;
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
  private PartitionCollectionMutable.Dataset readDataset(GribCollectionProto.Dataset p) {
    GribCollectionImmutable.Type type = GribCollectionImmutable.Type.valueOf(p.getType().toString());
    GribCollectionMutable.Dataset ds = gc.makeDataset(type);

    List<GribCollectionMutable.GroupGC> groups = new ArrayList<>(p.getGroupsCount());
    for (int i = 0; i < p.getGroupsCount(); i++)
      groups.add(readGroup(p.getGroups(i)));
    ds.groups = Collections.unmodifiableList(groups);

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
  protected GribCollectionMutable.GroupGC readGroup(GribCollectionProto.Group p) {
    GribCollectionMutable.GroupGC group = gc.makeGroup();

    group.horizCoordSys = readGds( p.getGds());

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
    // CalendarDate firstRef = null;
    int reftimeCoord = 0;
    int timeCoord = 0;
    int ensCoord = 0;
    List<CoordinateVert> vertCoords = new ArrayList<>();
    List<CoordinateTime2D> time2DCoords = new ArrayList<>();
    Map<CoordinateRuntime, CoordinateRuntime> runtimes = new HashMap<>();
    for (Coordinate coord : group.coords) {
      Coordinate.Type type = coord.getType();
      switch (type) {
        case runtime:
          CoordinateRuntime reftime = (CoordinateRuntime) coord;
          if (reftimeCoord > 0) reftime.setName("reftime" + reftimeCoord);
          reftimeCoord++;
          runtimes.put(reftime, reftime);
          break;

        case time:
          CoordinateTime tc = (CoordinateTime) coord;
          if (timeCoord > 0) tc.setName("time" + timeCoord);
          timeCoord++;
          break;

        case timeIntv:
          CoordinateTimeIntv tci = (CoordinateTimeIntv) coord;
          if (timeCoord > 0) tci.setName("time" + timeCoord);
          timeCoord++;
          break;

        case time2D:
          CoordinateTime2D t2d = (CoordinateTime2D) coord;
          if (timeCoord > 0) t2d.setName("time" + timeCoord);
          timeCoord++;
          time2DCoords.add(t2d);
          break;

        case vert:
          vertCoords.add((CoordinateVert) coord);
          break;

        case ens:
          CoordinateEns ce = (CoordinateEns) coord;
          if (ensCoord > 0) ce.setName("ens" + ensCoord);
          ensCoord++;
          break;
      }
    }
    assignVertNames(vertCoords);
    assignRuntimeNames(runtimes, time2DCoords, group.getId() + "-" + (group.isTwoD ? "TwoD" : "Best"));

    return group;
  }

  public void assignVertNames(List<CoordinateVert> vertCoords) {
    Map<String, Integer> map = new HashMap<>(2 * vertCoords.size());

    // assign name
    for (CoordinateVert vc : vertCoords) {
      String shortName = getLevelNameShort(vc.getCode()).toLowerCase();
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

  public void assignRuntimeNames(Map<CoordinateRuntime, CoordinateRuntime> runtimes, List<CoordinateTime2D> time2DCoords, String groupId) {

    // assign same name to internal time2D runtime as matched the external runtime
    for (CoordinateTime2D t2d : time2DCoords) {
      CoordinateRuntime runtime2D = t2d.getRuntimeCoordinate();
      CoordinateRuntime runtime = runtimes.get(runtime2D);
      if (runtime == null)
        System.out.printf("HEY assignRuntimeNames failed on %s group %s%n", t2d.getName(), groupId);
      else
        runtime2D.setName(runtime.getName());
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
    Coordinate.Type type = convertAxisType(pc.getAxisType());
    int code = pc.getCode();
    String unit = pc.getUnit();
    if (unit.length() == 0) unit = null; // LOOK may be null

    switch (type) {
      case runtime:
        if (unit == null)
          throw new IllegalStateException("Null units");
        CalendarDateUnit cdUnit = CalendarDateUnit.of(null, unit);
        return new CoordinateRuntime(pc.getMsecsList(), cdUnit.getCalendarPeriod());

      case time:
        List<Integer> offs = new ArrayList<>(pc.getValuesCount());
        for (float val : pc.getValuesList())
          offs.add((int) val);
        CalendarDate refDate = CalendarDate.of(pc.getMsecs(0));
        if (unit == null)
           throw new IllegalStateException("Null units");
        CalendarPeriod timeUnit = CalendarPeriod.of(unit);
        return new CoordinateTime(code, timeUnit, refDate, offs, readTime2Runtime(pc));

      case timeIntv:
        List<TimeCoord.Tinv> tinvs = new ArrayList<>(pc.getValuesCount());
        for (int i = 0; i < pc.getValuesCount(); i++) {
          int val1 = (int) pc.getValues(i);
          int val2 = (int) pc.getBound(i);
          tinvs.add(new TimeCoord.Tinv(val1, val2));
        }
        refDate = CalendarDate.of(pc.getMsecs(0));
        if (unit == null)
           throw new IllegalStateException("Null units");
        CalendarPeriod timeUnit2 = CalendarPeriod.of(unit);
        return new CoordinateTimeIntv(code, timeUnit2, refDate, tinvs, readTime2Runtime(pc));

      case time2D:
        if (unit == null)
           throw new IllegalStateException("Null units");
        CalendarPeriod timeUnit3 = CalendarPeriod.of(unit);
        CoordinateRuntime runtime = new CoordinateRuntime(pc.getMsecsList(), timeUnit3);

        List<Coordinate> times = new ArrayList<>(pc.getTimesCount());
        for (GribCollectionProto.Coord coordp : pc.getTimesList())
          times.add(readCoord(coordp));
        boolean isOrthogonal = pc.getIsOrthogonal();
        boolean isRegular = pc.getIsRegular();
        if (isOrthogonal)
          return new CoordinateTime2D(code, timeUnit3, null, runtime, (CoordinateTimeAbstract) times.get(0), null, readTime2Runtime(pc));
        else if (isRegular)
          return new CoordinateTime2D(code, timeUnit3, null, runtime, times, null, readTime2Runtime(pc));
        else
          return new CoordinateTime2D(code, timeUnit3, null, runtime, times, readTime2Runtime(pc));

      case vert:
        boolean isLayer = pc.getValuesCount() == pc.getBoundCount();
        List<VertCoord.Level> levels = new ArrayList<>(pc.getValuesCount());
        for (int i = 0; i < pc.getValuesCount(); i++) {
          if (isLayer)
            levels.add(new VertCoord.Level(pc.getValues(i), pc.getBound(i)));
          else
            levels.add(new VertCoord.Level(pc.getValues(i)));
        }
        return new CoordinateVert(code, tables.getVertUnit(code), levels);

      case ens:
        List<EnsCoord.Coord> ecoords = new ArrayList<>(pc.getValuesCount());
        for (int i = 0; i < pc.getValuesCount(); i++) {
          double val1 = pc.getValues(i);
          double val2 = pc.getBound(i);
          ecoords.add(new EnsCoord.Coord((int)val1, (int)val2));
        }
        return new CoordinateEns(code, ecoords);


    }
    throw new IllegalStateException("Unknown Coordinate type = " + type);
  }

  private int[] readTime2Runtime(GribCollectionProto.Coord pc) {
    if (pc.getTime2RuntimeCount() > 0) {
      int[] time2runtime = new int[pc.getTime2RuntimeCount()];
      for (int i=0; i<pc.getTime2RuntimeCount(); i++)
        time2runtime[i] = pc.getTime2Runtime(i);
      return time2runtime;
    }
    return null;
  }

  protected GribCollectionMutable.VariableIndex readVariable(GribCollectionMutable.GroupGC group, GribCollectionProto.Variable pv) {
    int discipline = pv.getDiscipline();

    byte[] rawPds = pv.getPds().toByteArray();

        // extra id info
    int nids = pv.getIdsCount();
    int center = (nids > 0) ? pv.getIds(0) : 0;
    int subcenter = (nids > 1) ? pv.getIds(1) : 0;

    long recordsPos = pv.getRecordsPos();
    int recordsLen = pv.getRecordsLen();
    List<Integer> index = pv.getCoordIdxList();

    GribCollectionMutable.VariableIndex result = gc.makeVariableIndex(group, tables, discipline, center, subcenter, rawPds, index, recordsPos, recordsLen);
    result.ndups = pv.getNdups();
    result.nrecords = pv.getNrecords();
    result.nmissing = pv.getMissing();

    return readVariableExtensions(group, pv, result);
  }

  static public Coordinate.Type convertAxisType(GribCollectionProto.GribAxisType type) {
    switch (type) {
      case runtime:
        return Coordinate.Type.runtime;
      case time:
        return Coordinate.Type.time;
      case time2D:
        return Coordinate.Type.time2D;
      case timeIntv:
        return Coordinate.Type.timeIntv;
      case ens:
        return Coordinate.Type.ens;
      case vert:
        return Coordinate.Type.vert;
    }
    throw new IllegalStateException("illegal axis type " + type);
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // these objects are created from the ncx index. lame - should only be in the builder i think
  private Set<String> hcsNames = new HashSet<>(5);

  protected String makeHorizCoordSysName(GdsHorizCoordSys hcs) {
    // default id
    String base = hcs.makeId();
    // ensure uniqueness
    String tryit = base;
    int count = 1;
    while (hcsNames.contains(tryit)) {
      count++;
      tryit = base + "-" + count;
    }
    hcsNames.add(tryit);
    return tryit;
  }

}
