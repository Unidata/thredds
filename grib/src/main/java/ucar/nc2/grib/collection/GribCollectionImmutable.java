/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import com.google.common.base.MoreObjects;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.MFile;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.grib.*;
import ucar.nc2.grib.coord.Coordinate;
import ucar.nc2.grib.coord.CoordinateEns;
import ucar.nc2.grib.coord.CoordinateRuntime;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.nc2.grib.coord.CoordinateTimeAbstract;
import ucar.nc2.grib.coord.SparseArray;
import ucar.nc2.grib.coord.TimeCoordIntvValue;
import ucar.nc2.grib.coord.VertCoordValue;
import ucar.nc2.grib.grib1.Grib1Variable;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.table.Grib2Tables;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.nc2.util.cache.FileCacheable;
import ucar.nc2.wmo.CommonCodeTable;
import ucar.unidata.io.RandomAccessFile;

import javax.annotation.concurrent.Immutable;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * An Immutable GribCollection, corresponds to one index (ncx) file.
 * The index file has already been read; it is opened and the closed when a variable is first accessed to read in the record array (sa).
 * <p>
 * Could use the Proto equivalents, and eliminate GribCollectionMutable ?
 *
 * @author caron
 * @since 11/10/2014
 */
@Immutable
public abstract class GribCollectionImmutable implements Closeable, FileCacheable {
  private static final Logger logger = LoggerFactory.getLogger(GribCollectionImmutable.class);
  public static int countGC; // debug

  public enum Type {    // must match with GribCollectionProto.Dataset.Type
    SRC,               // GC: Single Runtime Collection                [ntimes]
    MRC,              // GC: Multiple Runtime Collection              [nruns, ntimes]
    // MRSTC,             // GC: Multiple Runtime Single Time Collection  [nruns, 1]
    MRUTC,             // GC: Multiple Runtime Unique Time Collection  [ntimes]

    // MRSTP,            // PC: Multiple Runtime Single Time Partition   [nruns, 1]
    TwoD,            // PC: TwoD time partition                      [nruns, ntimes]
    Best,             // PC: Best time partition                      [ntimes]
    BestComplete,     // PC: Best complete time partition (not done)  [ntimes]
    MRUTP;            // PC: Multiple Runtime Unique Time Partition   [ntimes]

    public boolean isSingleRuntime() {
      return this == SRC;
    }

    public boolean isUniqueTime() {
      return this == MRUTC || this == MRUTP || this == SRC;
    }

    public boolean isTwoD() {
      return this == MRC || this == TwoD;
    }
  }

  ////////////////////////////////////////////////////////////////
  protected final String name; // collection name; index filename must be directory/name.ncx2
  protected final File directory;
  protected final FeatureCollectionConfig config;
  public final boolean isGrib1;
  protected final Info info;

  protected final List<Dataset> datasets;
  protected final CoordinateRuntime masterRuntime;
  protected final CalendarDateRange dateRange;

  protected final Map<Integer, MFile> fileMap; // all the files used in the GC; key is the index in original collection, GC has subset of them
  protected final GribTables cust;
  protected final String indexFilename;       // full path of index Filename

  private FileCacheIF objCache = null;  // optional object cache - used in the TDS

  GribCollectionImmutable(GribCollectionMutable gc) {
    countGC++;

    this.config = gc.config;
    this.name = gc.name;
    this.directory = gc.directory;
    this.isGrib1 = gc.isGrib1;
    this.info = new Info(gc);

    List<Dataset> work = new ArrayList<>(gc.datasets.size());
    for (GribCollectionMutable.Dataset gcDataset : gc.datasets) {
      work.add(new Dataset(gcDataset.gctype, gcDataset.groups));
    }
    this.datasets = Collections.unmodifiableList(work);

    // this.horizCS = Collections.unmodifiableList(gc.horizCS);
    this.masterRuntime = gc.masterRuntime;
    this.fileMap = gc.fileMap;
    this.cust = gc.cust;
    this.dateRange = gc.dateRange;

    if (gc.indexFilename != null) {
      indexFilename = gc.indexFilename;

    } else {
      File indexFile = GribCdmIndex.makeIndexFile(name, directory);
      File indexFileInCache = GribIndexCache.getExistingFileOrCache(indexFile.getPath());
      if (indexFileInCache == null)
        throw new IllegalStateException(indexFile.getPath() + " does not exist, nor in cache");
      indexFilename = indexFileInCache.getPath();
    }
  }

  // overridden in PartitionCollection
  protected VariableIndex makeVariableIndex(GroupGC group, GribCollectionMutable.VariableIndex mutableVar) {
    return new VariableIndex(group, mutableVar);
  }

  public List<Dataset> getDatasets() {
    return datasets;
  }

  public Dataset getDataset(int idx) {
    return datasets.get(idx);
  }

  public Dataset getDatasetCanonical() {
    for (Dataset ds : datasets) {
      if (ds.gctype != GribCollectionImmutable.Type.Best) return ds;
    }
    throw new IllegalStateException("GC.getDatasetCanonical failed on=" + name);
  }

  public String getName() {
    return name;
  }

  public File getDirectory() {
    return directory;
  }

  public CoordinateRuntime getMasterRuntime() {
    return masterRuntime;
  }

  public CalendarDate getMasterFirstDate() {
    return masterRuntime.getFirstDate();
  }

  public int getVersion() {
    return info.version;
  }

  public int getCenter() {
    return info.center;
  }

  public int getSubcenter() {
    return info.subcenter;
  }

  public int getMaster() {
    return info.master;
  }

  public int getLocal() {
    return info.local;
  }

  public int getGenProcessType() {
    return info.genProcessType;
  }

  public int getGenProcessId() {
    return info.genProcessId;
  }

  public int getBackProcessId() {
    return info.backProcessId;
  }

  /*
    String val = CommonCodeTable.getCenterName(gribCollection.getCenter(), 2);
    ncfile.addAttribute(null, new Attribute(GribUtils.CENTER, val == null ? Integer.toString(gribCollection.getCenter()) : val));
    val = gribTable.getSubCenterName(gribCollection.getCenter(), gribCollection.getSubcenter());
    ncfile.addAttribute(null, new Attribute(GribUtils.SUBCENTER, val == null ? Integer.toString(gribCollection.getSubcenter()) : val));
    ncfile.addAttribute(null, new Attribute(GribUtils.TABLE_VERSION, gribCollection.getMaster() + "," + gribCollection.getLocal())); // LOOK

    addGlobalAttributes(ncfile);

    ncfile.addAttribute(null, new Attribute(CDM.CONVENTIONS, "CF-1.6"));
    ncfile.addAttribute(null, new Attribute(CDM.HISTORY, "Read using CDM IOSP GribCollection v3"));
    ncfile.addAttribute(null, new Attribute(CF.FEATURE_TYPE, FeatureType.GRID.name()));
    ncfile.addAttribute(null, new Attribute(CDM.FILE_FORMAT, getFileTypeId()));
   */

  public AttributeContainerHelper getGlobalAttributes() {
    AttributeContainerHelper result = new AttributeContainerHelper(name);
    String val = CommonCodeTable.getCenterName(getCenter(), 2);
    result.addAttribute(new Attribute(GribUtils.CENTER, val == null ? Integer.toString(getCenter()) : val));
    val = cust.getSubCenterName(getCenter(), getSubcenter());
    result.addAttribute(new Attribute(GribUtils.SUBCENTER, val == null ? Integer.toString(getSubcenter()) : val));
    result.addAttribute(new Attribute(GribUtils.TABLE_VERSION, getMaster() + "," + getLocal())); // LOOK

    addGlobalAttributes(result);  // add subclass atts

    result.addAttribute(new Attribute(CDM.CONVENTIONS, "CF-1.6"));
    result.addAttribute(new Attribute(CDM.HISTORY, "Read using CDM IOSP GribCollection v3"));
    result.addAttribute(new Attribute(CF.FEATURE_TYPE, FeatureType.GRID.name()));

    return result;
  }

  public abstract void addGlobalAttributes(AttributeContainer result);

  public abstract void addVariableAttributes(AttributeContainer v, GribCollectionImmutable.VariableIndex vindex);

  protected abstract String makeVariableId(VariableIndex v);

  static class Info {
    final int version; // the ncx version
    final int center, subcenter, master, local;  // GRIB 1 uses "local" for table version
    final int genProcessType, genProcessId, backProcessId;

    Info(int version, int center, int subcenter, int master, int local, int genProcessType, int genProcessId, int backProcessId) {
      this.version = version;
      this.center = center;
      this.subcenter = subcenter;
      this.master = master;
      this.local = local;
      this.genProcessType = genProcessType;
      this.genProcessId = genProcessId;
      this.backProcessId = backProcessId;
    }

    Info(GribCollectionMutable gc) {
      this.version = gc.version;
      this.center = gc.center;
      this.subcenter = gc.subcenter;
      this.master = gc.master;
      this.local = gc.local;
      this.genProcessType = gc.genProcessType;
      this.genProcessId = gc.genProcessId;
      this.backProcessId = gc.backProcessId;
    }

    @Override
    public String toString() {
      return "Info{" +
          "version=" + version +
          ", center=" + center +
          ", subcenter=" + subcenter +
          ", master=" + master +
          ", local=" + local +
          ", genProcessType=" + genProcessType +
          ", genProcessId=" + genProcessId +
          ", backProcessId=" + backProcessId +
          '}';
    }
  }

  @Immutable
  public class Dataset {
    final Type gctype;
    final List<GroupGC> groups;  // must be kept in order, because PartitionForVariable2D has index into it

    public Dataset(Type gctype, List<GribCollectionMutable.GroupGC> groups) {
      this.gctype = gctype;
      List<GroupGC> work = new ArrayList<>(groups.size());
      for (GribCollectionMutable.GroupGC gcGroup : groups) {
        work.add(new GroupGC(this, gcGroup));
      }
      this.groups = Collections.unmodifiableList(work);
    }

    public Iterable<GroupGC> getGroups() {
      return groups;
    }

    public int getGroupsSize() {
      return groups.size();
    }

    public Type getType() {
      return gctype;
    }

    public GroupGC getGroup(int index) {
      return groups.get(index);
    }

    @Nullable
    public GroupGC findGroupById(String id) {
      for (GroupGC g : getGroups()) {
        if (g.getId().equals(id))
          return g;
      }
      return null;
    }
  }

  @Immutable
  public class GroupGC {
    final Dataset ds;
    public final GribHorizCoordSystem horizCoordSys;
    final List<VariableIndex> variList;
    final List<Coordinate> coords;      // shared coordinates
    final int[] filenose;               // key for GC.fileMap
    private final Map<VariableIndex, VariableIndex> varMap;

    public GroupGC(Dataset ds, GribCollectionMutable.GroupGC gc) {
      this.ds = ds;
      this.horizCoordSys = gc.horizCoordSys;
      this.coords = gc.coords;
      this.filenose = gc.filenose;
      this.varMap = new HashMap<>(gc.variList.size() * 2);

      List<GribCollectionMutable.VariableIndex> gcVars = gc.variList;
      List<VariableIndex> work = new ArrayList<>(gcVars.size());
      for (GribCollectionMutable.VariableIndex gcVar : gcVars) {
        VariableIndex vi = makeVariableIndex(this, gcVar);
        work.add(vi);
        varMap.put(vi, vi);
      }
      this.variList = Collections.unmodifiableList(work);
    }

    public Type getType() {
      return ds.gctype;
    }

    public String getId() {
      return horizCoordSys.getId();
    }

    public Object getGdsHash() {
      return horizCoordSys.getGdsHash();
    }

    public GribCollectionImmutable getGribCollection() {
      return GribCollectionImmutable.this;
    }

    // human readable
    public String getDescription() {
      return horizCoordSys.getDescription();
    }

    public GribHorizCoordSystem getGribHorizCoordSys() {
      return horizCoordSys;
    }

    public GdsHorizCoordSys getGdsHorizCoordSys() {
      return horizCoordSys.getHcs();
    }

    VariableIndex findVariableByHash(VariableIndex vi) {
      return varMap.get(vi);
    }

    public Optional<Coordinate> findCoordinate(String name) {
      return coords.stream().filter(x -> x.getName().equals(name)).findFirst();
    }

    public List<VariableIndex> getVariables() {
      return variList;
    }

    public List<Coordinate> getCoordinates() {
      return coords;
    }

    public int getNruntimes() {
      return masterRuntime.getSize();
    }

    public int getNFiles() {
      if (filenose == null) return 0;
      return filenose.length;
    }

    public List<MFile> getFiles() {
      List<MFile> result = new ArrayList<>();
      if (filenose == null) return result;
      for (int fileno : filenose)
        result.add(fileMap.get(fileno));
      Collections.sort(result);
      return result;
    }

    public CalendarDateRange makeCalendarDateRange() {
      CalendarDateRange result = null;
      for (Coordinate coord : coords) {
        switch (coord.getType()) {
          case time:
          case timeIntv:
          case time2D:
            CoordinateTimeAbstract time = (CoordinateTimeAbstract) coord;
            CalendarDateRange range = time.makeCalendarDateRange(null);
            if (result == null) result = range;
            else result = result.extend(range);
        }
      }

      return result;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("horizCoordSys", horizCoordSys.getDescription())
          .toString();
    }

    public void show(Formatter f) {
      f.format("Group %s (%d) type=%s%n", horizCoordSys.getId(), horizCoordSys.getGdsHash().hashCode(), ds.gctype);
      f.format(" nfiles %d%n", filenose == null ? 0 : filenose.length);
      f.format(" hcs = %s%n", horizCoordSys.getHcs());
    }
  }

  @Immutable      // except for sa
  public class VariableIndex {
    final GroupGC group;     // belongs to this group
    final VariableIndex.Info info;
    final Object gribVariable;

    final List<Integer> coordIndex;  // indexes into group.coords
    final long recordsPos;    // where the records array is stored in the index. 0 means no records
    final int recordsLen;

    // stats
    final int ndups, nrecords, nmissing;

    // read in on demand
    private SparseArray<Record> sa;   // for GC only; lazily read; same array shape as variable, minus x and y

    protected VariableIndex(GroupGC g, GribCollectionMutable.VariableIndex gcVar) {
      this.group = g;
      this.info = new Info(gcVar);
      this.gribVariable = gcVar.gribVariable;

      this.coordIndex = gcVar.coordIndex;
      this.recordsPos = gcVar.recordsPos;
      this.recordsLen = gcVar.recordsLen;

      this.ndups = gcVar.ndups;
      this.nrecords = gcVar.nrecords;
      this.nmissing = gcVar.nmissing;
    }

    public synchronized void readRecords() throws IOException {
      if (this.sa != null) return;

      if (recordsLen == 0)
        return;
      byte[] b = new byte[recordsLen];

      try (RandomAccessFile indexRaf = RandomAccessFile.acquire(indexFilename)) {

        indexRaf.seek(recordsPos);
        indexRaf.readFully(b);

        /*
        message SparseArray {
          repeated uint32 size = 2 [packed=true];     // multidim sizes = shape[]
          repeated uint32 track = 3 [packed=true];    // 1-based index into record list, 0 == missing
          repeated Record records = 4;                // List<Record>
          uint32 ndups = 5;                           // duplicates found when creating
        }
       */
        GribCollectionProto.SparseArray proto = GribCollectionProto.SparseArray.parseFrom(b);

        int nsizes = proto.getSizeCount();
        int[] size = new int[nsizes];
        for (int i = 0; i < nsizes; i++)
          size[i] = proto.getSize(i);

        int ntrack = proto.getTrackCount();
        int[] track = new int[ntrack];
        for (int i = 0; i < ntrack; i++)
          track[i] = proto.getTrack(i);

        int n = proto.getRecordsCount();
        List<Record> records = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
          GribCollectionProto.Record pr = proto.getRecords(i);
          records.add(new Record(pr.getFileno(), pr.getStartPos(), pr.getBmsOffset(), pr.getDrsOffset()));
        }
        int ndups = proto.getNdups();
        this.sa = new SparseArray<>(size, track, records, ndups);

      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        logger.error(" file={} recordsLen={} recordPos={}", indexFilename, recordsLen, recordsPos);
        throw e;
      }
    }

    synchronized Record getRecordAt(int sourceIndex) {
      return sa.getContent(sourceIndex);
    }

    synchronized Record getRecordAt(int[] sourceIndex) {
      return sa.getContent(sourceIndex);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // coord based record finding. note only one record at a time
    @Nullable
    synchronized Record getRecordAt(SubsetParams coords) {
      int[] want = new int[getRank()];
      int count = 0;
      int runIdx = -1;
      for (Coordinate coord : getCoordinates()) {
        int idx = -1;
        switch (coord.getType()) {
          case runtime:
            CalendarDate runtimeCooord = coords.getRunTime();
            idx = coord.getIndex(runtimeCooord);
            runIdx = idx;
            break;

          case timeIntv:
            double[] timeIntv = coords.getTimeOffsetIntv();
            idx = coord.getIndex(new TimeCoordIntvValue((int) timeIntv[0], (int) timeIntv[1]));
            break;

          case time:
            Double timeOffset = coords.getTimeOffset(); // Double
            int coordInt = timeOffset.intValue();
            idx = coord.getIndex(coordInt);
            break;

          case time2D:
            timeIntv = coords.getTimeOffsetIntv();
            if (timeIntv != null) {
              TimeCoordIntvValue coordTinv = new TimeCoordIntvValue((int) timeIntv[0], (int) timeIntv[1]);
              idx = ((CoordinateTime2D) coord).findTimeIndexFromVal(runIdx, coordTinv); // LOOK can only use if orthogonal
              break;
            }
            Double timeCoord = coords.getTimeOffset();
            if (timeCoord != null) {
              coordInt = timeCoord.intValue();
              idx = ((CoordinateTime2D) coord).findTimeIndexFromVal(runIdx, coordInt);
              break;
            }

          // the OneTime case
          CoordinateTime2D coord2D = (CoordinateTime2D) coord;
          if (coord2D.getNtimes() == 1) {
            idx = 0;
            break;
          }
          throw new IllegalStateException("time2D must have timeOffset ot timeOffsetIntv coordinare");

          case vert:
            double[] vertIntv = coords.getVertCoordIntv();
            if (vertIntv != null) {
              VertCoordValue coordVert = new VertCoordValue(vertIntv[0], vertIntv[1]);
              idx = coord.getIndex(coordVert);
              break;
            }
            Double vertCoord = coords.getVertCoord();
            if (vertCoord != null) {
              VertCoordValue coordVert = new VertCoordValue(vertCoord);
              idx = coord.getIndex(coordVert);
            }
            break;

          case ens:
            Double ensVal = coords.getEnsCoord();
            idx = ((CoordinateEns) coord).getIndexByMember(ensVal);
            break;

          default:
            logger.warn("GribCollectionImmutable: missing CoordVal for {}%n", coord.getName());
        }

        if (idx < 0) {
          logger.debug("Cant find index for value in axis {} in variable {}", coord.getName(), name);
          return null;
        }

        want[count++] = idx;
      }
      return sa.getContent(want);
    }

    public List<Coordinate> getCoordinates() {
      List<Coordinate> result = new ArrayList<>(coordIndex.size());
      for (int idx : coordIndex)
        result.add(group.coords.get(idx));
      return result;
    }

    @Nullable
    public Coordinate getCoordinate(Coordinate.Type want) {
      for (int idx : coordIndex)
        if (group.coords.get(idx).getType() == want)
          return group.coords.get(idx);
      return null;
    }

    @Nullable
    CoordinateTimeAbstract getCoordinateTime() {
      for (int idx : coordIndex)
        if (group.coords.get(idx) instanceof CoordinateTimeAbstract)
          return (CoordinateTimeAbstract) group.coords.get(idx);
      return null;
    }

    // get the ith coordinate
    public Coordinate getCoordinate(int index) {
      int grpIndex = coordIndex.get(index);
      return group.coords.get(grpIndex);
    }

    public Iterable<Integer> getCoordinateIndex() {
      return coordIndex;
    }

    public SparseArray<Record> getSparseArray() {
      return sa;
    }

    public int getNRecords() {
      return sa == null ? -1 : sa.countNotMissing();
    }

    public int getTableVersion() {
      return info.tableVersion;
    }

    public int getDiscipline() {
      return info.discipline;
    }

    public int getCategory() {
      return info.category;
    }

    public int getParameter() {
      return info.parameter;
    }

    public int getLevelType() {
      return info.levelType;
    }

    public int getIntvType() {
      return info.intvType;
    }

    public int getSpatialStatisticalProcessType() {
      return info.spatialStatType;
    }

    public int getEnsDerivedType() {
      return info.ensDerivedType;
    }

    public int getProbType() {
      return info.probType;
    }

    public String getIntvName() {
      return info.intvName;
    }

    public String getProbabilityName() {
      return info.probabilityName;
    }

    public boolean isLayer() {
      return info.isLayer;
    }

    public boolean isEnsemble() {
      return info.isEnsemble;
    }

    public int getGenProcessType() {
      return info.genProcessType;
    }

    public int getNdups() {
      return ndups;
    }

    public GroupGC getGroup() {
      return group;
    }

    public int getNmissing() {
      return nmissing;
    }

    public int getNrecords() {
      return nrecords;
    }

    public int getSize() {
      int size = 1;
      for (int idx : coordIndex) {
        Coordinate c = group.coords.get(idx);
        int csize = (c instanceof CoordinateTime2D) ? ((CoordinateTime2D) c).getNtimes() : c.getSize();
        size *= csize;
      }
      return size;
    }


    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("info", info)
          .toString();
    }

    public int getRank() {
      return coordIndex.size();
    }

    public String toStringFrom() {
      try (Formatter sb = new Formatter()) {
        sb.format("Variable {%d-%d-%d", info.discipline, info.category, info.parameter);
        sb.format(", levelType=%d", info.levelType);
        sb.format(", intvType=%d", info.intvType);
        sb.format(", nrecords=%d", nrecords);
        sb.format(", ndups=%d", ndups);
        sb.format(", nmiss=%d}", nmissing);
        return sb.toString();
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      VariableIndex that = (VariableIndex) o;
      return gribVariable.equals(that.gribVariable);
    }

    @Override
    public int hashCode() {
      return gribVariable.hashCode();
    }

    public String makeVariableName() {
      if (isGrib1)
        return ((Grib1Variable) gribVariable).makeVariableName(config.gribConfig);
      else
        return Grib2Iosp.makeVariableNameFromTable((Grib2Tables) cust, GribCollectionImmutable.this, this, config.gribConfig.useGenType);
    }

    public String makeVariableUnits() {
      if (isGrib1)
        return Grib1Iosp.makeVariableUnits((Grib1Customizer) cust, GribCollectionImmutable.this, this);
      else
        return Grib2Iosp.makeVariableUnits((Grib2Tables) cust, this);
    }

    public String makeVariableDescription() {
      if (isGrib1)
        return Grib1Iosp.makeVariableLongName((Grib1Customizer) cust, getCenter(), getSubcenter(), getTableVersion(), getParameter(),
                getLevelType(), isLayer(), getIntvType(), getIntvName(), getProbabilityName());
      else
        return Grib2Iosp.makeVariableLongName((Grib2Tables) cust, this, config.gribConfig.useGenType);
    }

    public GribTables.Parameter getGribParameter() {
      if (isGrib1)
        return ((Grib1Customizer) cust).getParameter(getCenter(), getSubcenter(), getVersion(), getParameter());
      else
        return ((Grib2Tables) cust).getParameter(this);
    }

    public GribStatType getStatType() {
      return cust.getStatType(getIntvType());
    }

    @Immutable
    public final class Info {
      final int tableVersion;   // grib1 only : can vary by variable
      final int discipline;     // grib2 only

      // derived from pds
      final int category, parameter, levelType, intvType, ensDerivedType, probType;
      @Nullable final String intvName;  // eg "mixed intervals, 3 Hour, etc"
      final String probabilityName;
      final boolean isLayer, isEnsemble;
      final int genProcessType;
      final int spatialStatType;

      public Info(GribCollectionMutable.VariableIndex gcVar) {
        this.tableVersion = gcVar.tableVersion;
        this.discipline = gcVar.discipline;
        this.category = gcVar.category;
        this.parameter = gcVar.parameter;
        this.levelType = gcVar.levelType;
        this.intvType = gcVar.intvType;
        this.ensDerivedType = gcVar.ensDerivedType;
        this.probType = gcVar.probType;
        this.intvName = gcVar.getTimeIntvName();
        this.probabilityName = gcVar.probabilityName;
        this.isLayer = gcVar.isLayer;
        this.isEnsemble = gcVar.isEnsemble;
        this.genProcessType = gcVar.genProcessType;
        this.spatialStatType = gcVar.spatialStatType;
      }

      @Override
      public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("discipline", discipline)
            .add("category", category)
            .add("parameter", parameter)
            .toString();
      }
    }

  }

  @Immutable
  public static class Record {
    public final int fileno;    // which file
    public final long pos;      // offset on file where message starts
    public final int bmsOffset;   // if non-zero, offset where bms starts (grib2)
    public final int drsOffset;   // if non-zero, offset where drs starts  (grib2)

    public Record(int fileno, long pos, int bmsOffset, int drsOffset) { // }, int scanMode) {
      this.fileno = fileno;
      this.pos = pos;
      this.bmsOffset = bmsOffset;
      this.drsOffset = drsOffset;
    }

    @Override
    public String toString() {
      return "Record{" +
          "fileno=" + fileno +
          ", pos=" + pos +
          ", bmsOffset=" + bmsOffset +
          ", drsOffset=" + drsOffset +
          '}';
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff for FileCacheable

  public synchronized void close() throws java.io.IOException {
    if (objCache != null) {
      objCache.release(this);
    }
  }

  // release any resources like file handles
  public void release() {
  }

  // reacquire any resources like file handles
  public void reacquire() {
  }

  @Override
  public String getLocation() {
    return indexFilename;
  }

  @Override
  public long getLastModified() {
    File indexFile = new File(indexFilename);
    return indexFile.lastModified();
  }

  @Override
  public synchronized void setFileCache(FileCacheIF fileCache) {
    this.objCache = fileCache;
  }

  ///////////////

  public void showStatus(Formatter f) {
    showIndexFile(f);
    f.format("Class (%s)%n", getClass().getName());
    f.format("%s%n%n", info.toString());

    f.format("masterRuntime: size=%d%n", masterRuntime.getSize());
    if (masterRuntime.getSize() < 20)
      masterRuntime.showCoords(f);

    for (Dataset ds : datasets) {
      f.format("%nDataset %s%n", ds.getType());
      for (GroupGC g : ds.groups) {
        int nrecords = 0, ndups = 0, nmissing = 0;
        f.format(" Group %s%n", g.horizCoordSys.getId());
        for (VariableIndex v : g.variList) {
          f.format("  %s%n", v.toStringFrom());
          nrecords += v.nrecords;
          ndups += v.ndups;
          nmissing += v.nmissing;
        }
        f.format(" Group total nrecords=%d", nrecords);
        f.format(", ndups=%d", ndups);
        f.format(", nmiss=%d%n", nmissing);

        int nruntimes = 0, ntimes = 0, ntimes2D = 0, ntimeIntvs = 0;
        for (Coordinate coord : g.getCoordinates()) {
          if (coord.getType() == Coordinate.Type.runtime) nruntimes++;
          if (coord.getType() == Coordinate.Type.time) ntimes++;
          if (coord.getType() == Coordinate.Type.timeIntv) ntimeIntvs++;
          if (coord.getType() == Coordinate.Type.time2D) ntimes2D++;
        }
        f.format(" Group nruntimes=%d ntimes=%d ntimeIntvs=%d ntimes2D=%d%n", nruntimes, ntimes, ntimeIntvs, ntimes2D);
      }
    }
    if (fileMap == null) {
      f.format("%nFiles empty%n");
    } else {
      f.format("%nFiles count = %d%n", fileMap.size());
      for (int index : fileMap.keySet())
        f.format("  %d: %s%n", index, fileMap.get(index));
      f.format("%n");
    }
  }

  public void showStatusSummary(Formatter f, String type) {
    Dataset ds = getDatasetCanonical();
    if (ds == null) return;

    if (type.equalsIgnoreCase("csv")) {
      for (GroupGC g : ds.groups) {
        int nrecords = 0, ndups = 0, nmissing = 0;
        for (VariableIndex v : g.variList) {
          nrecords += v.nrecords;
          ndups += v.ndups;
          nmissing += v.nmissing;
        }
        if (nrecords == 0) nrecords = 1;
        f.format("%s, %s, %s, %s, %d, %d, %f, %d, %f%n", name, config.type, ds.getType(), g.getDescription(), nrecords, ndups, ((float) ndups / nrecords), nmissing, ((float) nmissing / nrecords));
      }
    } else {
      for (GroupGC g : ds.groups) {
        int nrecords = 0, ndups = 0, nmissing = 0;
        for (VariableIndex v : g.variList) {
          nrecords += v.nrecords;
          ndups += v.ndups;
          nmissing += v.nmissing;
        }
        f.format(" Group %s (%s) total nrecords=%d", g.getDescription(), ds.getType(), nrecords);
        if (nrecords == 0) nrecords = 1;
        f.format(", ndups=%d (%f)", ndups, ((float) ndups / nrecords));
        f.format(", nmiss=%d (%f)%n", nmissing, ((float) nmissing / nrecords));
      }
    }

  }

  private void showIndexFile(Formatter f) {
    if (indexFilename == null) return;
    f.format("indexFile=%s%n", indexFilename);
    try {
      Path indexFile = Paths.get(indexFilename);
      BasicFileAttributes attr = Files.readAttributes(indexFile, BasicFileAttributes.class);
      f.format("  size=%d lastModifiedTime=%s lastAccessTime=%s creationTime=%s%n", attr.size(), attr.lastModifiedTime(), attr.lastAccessTime(), attr.creationTime());
    } catch (IOException e) {
      e.printStackTrace();
    }
    f.format("%n");
  }

  public void showIndex(Formatter f) {
    showIndexFile(f);
    f.format("Class (%s)%n", getClass().getName());
    f.format(" version %d%n", info.version);
    f.format("%s%n%n", toString());
    f.format("%s%n%n", info.toString());

    f.format("masterRuntime: size=%d%n", masterRuntime.getSize());
    if (masterRuntime.getSize() < 200)
      masterRuntime.showCoords(f);

    for (Dataset ds : datasets) {
      f.format("%nDataset %s%n", ds.getType());
      for (GroupGC g : ds.groups) {
        f.format(" Group %s%n", g.horizCoordSys.getId());
        for (VariableIndex v : g.variList) {
          f.format("  %s%n", v.toStringFrom());
        }
      }
    }

    f.format("%n");
    if (fileMap == null) {
      f.format("Files empty%n");
    } else {
      f.format("Files count = %d%n", fileMap.size());
    }
  }

  @Override
  public String toString() {
    return "GribCollectionImmutable{" +
        "name='" + name + '\'' +
        ", directory=" + directory +
        ", config=" + config +
        ", isGrib1=" + isGrib1 +
        ", info=" + info +
        ", masterRuntime=" + masterRuntime.getName() +
        ", dateRange=" + dateRange +
        ", indexFilename='" + indexFilename + '\'' +
        '}';
  }

////////////////////////////////////////

  public long getIndexFileSize() {
    File indexFile = new File(indexFilename);
    return indexFile.length();
  }

  public MFile getFile(int fileno) {
    return fileMap.get(fileno);
  }

  public String getFilename(int fileno) {
    return fileMap.get(fileno).getPath();
  }

  public Collection<MFile> getFiles() {
    return fileMap.values();
  }

  @Nullable
  public MFile findMFileByName(String filename) {
    for (MFile file : fileMap.values())
      if (file.getName().equals(filename))
        return file;
    return null;
  }

  RandomAccessFile getDataRaf(int fileno) throws IOException {
    // absolute location
    MFile mfile = fileMap.get(fileno);
    String filename = mfile.getPath();
    File dataFile = new File(filename);

    // if data file does not exist, check reletive location - eg may be /upc/share instead of Q:
    if (!dataFile.exists()) {
      if (fileMap.size() == 1) {
        dataFile = new File(directory, name); // single file case
      } else {
        dataFile = new File(directory, dataFile.getName()); // must be in same directory as the ncx file
      }
    }

    // data file not here
    if (!dataFile.exists()) {
      throw new FileNotFoundException("data file not found = " + dataFile.getPath());
    }

    RandomAccessFile want = RandomAccessFile.acquire(dataFile.getPath());
    want.order(RandomAccessFile.BIG_ENDIAN);
    return want;
  }

  String getDataRafFilename(int fileno) {
    MFile mfile = fileMap.get(fileno);
    return mfile.getPath();
  }

  ///////////////////////

  // stuff needed by InvDatasetFcGrib
  @Nullable
  public abstract ucar.nc2.dataset.NetcdfDataset getNetcdfDataset(Dataset ds, GroupGC group, String filename,
                                                                  FeatureCollectionConfig gribConfig, Formatter errlog, org.slf4j.Logger logger) throws IOException;

  @Nullable
  public abstract ucar.nc2.dt.grid.GridDataset getGridDataset(Dataset ds, GroupGC group, String filename,
                                                              FeatureCollectionConfig gribConfig, Formatter errlog, org.slf4j.Logger logger) throws IOException;

  @Nullable
  public abstract CoverageCollection getGridCoverage(Dataset ds, GroupGC group, String filename,
                                                     FeatureCollectionConfig gribConfig, Formatter errlog, org.slf4j.Logger logger) throws IOException;


}
