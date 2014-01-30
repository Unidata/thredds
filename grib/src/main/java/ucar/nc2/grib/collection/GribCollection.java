package ucar.nc2.grib.collection;

import net.jcip.annotations.Immutable;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.sparr.Coordinate;
import ucar.sparr.CoordinateTwoTimer;
import ucar.sparr.SparseArray;
import ucar.nc2.NetcdfFile;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheable;
import ucar.nc2.util.cache.FileFactory;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * A collection of grib files as a single logical dataset.
 * Concrete classes are for Grib1 and Grib2.
 * Note that there is no dependence on GRIB tables here.
 * Handles .ncx2 files.
 * Data files are opened and managed externally.
 *
 * @author John
 * @since 12/1/13
 */
public class GribCollection implements FileCacheable, AutoCloseable {
  public static final String NCX_IDX2 = ".ncx2";
  public static final long MISSING_RECORD = -1;

  //////////////////////////////////////////////////////////
  // object cache for data files - these are opened only as raf, not netcdfFile
  private static FileCache dataRafCache;
  private static DiskCache2 diskCache;

  static public void initDataRafCache(int minElementsInMemory, int maxElementsInMemory, int period) {
    dataRafCache = new ucar.nc2.util.cache.FileCache("GribCollectionDataRafCache ", minElementsInMemory, maxElementsInMemory, -1, period);
  }

  static public FileCache getDataRafCache() {
    return dataRafCache;
  }

  static private final ucar.nc2.util.cache.FileFactory dataRafFactory = new FileFactory() {
    public FileCacheable open(String location, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {
      return new RandomAccessFile(location, "r");
    }
  };

  static public void disableDataRafCache() {
    if (null != dataRafCache) dataRafCache.disable();
    dataRafCache = null;
  }

  static public File getIndexFile(String path) {
    return getDiskCache2().getFile(path);
  }

  static public File getIndexFile(MCollection dcm) {
    File idxFile = new File(new File(dcm.getRoot()), dcm.getCollectionName() + NCX_IDX2);
    return getIndexFile(idxFile.getPath());
  }

  static public void setDiskCache2(DiskCache2 dc) {
    diskCache = dc;
  }

  static public DiskCache2 getDiskCache2() {
    if (diskCache == null)
      diskCache = DiskCache2.getDefault();

    return diskCache;
  }

  //////////////////////////////////////////////////////////

  /* canonical order (time, ens, vert)
  static public int calcIndex(int timeIdx, int ensIdx, int vertIdx, int nens, int nverts) {
    if (nens == 0) nens = 1;
    if (nverts == 0) nverts = 1;
    return vertIdx + ensIdx * nverts + timeIdx * nverts * nens;
  }

  /**
   * Find index in partition dataset when vert and/or ens coordinates dont match with proto
   *
   * @param timeIdx time index in this partition
   * @param ensIdx  ensemble index in proto dataset
   * @param vertIdx vert index in proto dataset
   * @param flag    TimePartition.VERT_COORDS_DIFFER and/or TimePartition.ENS_COORDS_DIFFER
   * @param ec      ensemble coord in partition dataset
   * @param vc      vert coord in partition dataset
   * @param ecp     ensemble coord in proto dataset
   * @param vcp     vert coord in proto dataset
   * @return index in partition dataset
   *
  static public int calcIndex(int timeIdx, int ensIdx, int vertIdx, int flag, EnsCoord ec, VertCoord vc, EnsCoord ecp, VertCoord vcp) {
    int want_ensIdx = ensIdx;
    if ((flag & TimePartition.ENS_COORDS_DIFFER) != 0) {
      want_ensIdx = findEnsIndex(ensIdx, ec.getCoords(), ecp.getCoords());
      if (want_ensIdx == -1) return -1;
    }
    int want_vertIdx = vertIdx;
    if ((flag & TimePartition.VERT_COORDS_DIFFER) != 0) {
      want_vertIdx = findVertIndex(vertIdx, vc.getCoords(), vcp.getCoords());
      if (want_vertIdx == -1) return -1;
    }
    return calcIndex(timeIdx, want_ensIdx, want_vertIdx, (ec == null) ? 0 : ec.getSize(), (vc == null) ? 0 : vc.getSize());
  }

  static private int findEnsIndex(int indexp, List<EnsCoord.Coord> coords, List<EnsCoord.Coord> coordsp) {
    EnsCoord.Coord want = coordsp.get(indexp);
    for (int i = 0; i < coords.size(); i++) {
      EnsCoord.Coord have = coords.get(i);
      if (have.equals(want)) return i;
    }
    return -1;
  }

  static private int findVertIndex(int indexp, List<VertCoord.Level> coords, List<VertCoord.Level> coordsp) {
    VertCoord.Level want = coordsp.get(indexp);
    for (int i = 0; i < coords.size(); i++) {
      VertCoord.Level have = coords.get(i);
      if (have.equals(want)) return i;
    }
    return -1;
  } */

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  ////////////////////////////////////////////////////////////////
  protected String name; // collection name; index filename must be directory/name.ncx2
  protected File directory;
  protected final FeatureCollectionConfig.GribConfig gribConfig;
  protected final boolean isGrib1;

  protected FileCache objCache = null;  // optional object cache - used in the TDS
  public int version; // the ncx version

  // set by the builder
  public int center, subcenter, master, local;  // GRIB 1 uses "local" for table version
  public int genProcessType, genProcessId, backProcessId;
  public List<Parameter> params;  // not used
  private List<MFile> files;  // must be kept in order
  private Map<String, MFile> fileMap;
  protected List<Dataset> datasets;
  protected List<HorizCoordSys> horizCS; // one for each gds

  // synchronize any access to indexRaf                                     ```
  protected RandomAccessFile indexRaf; // this is the raf of the index (ncx) file
  protected File indexFile;

  // for making partition collection
  protected void copyInfo(GribCollection from) {
    this.center = from.center;
    this.subcenter = from.subcenter;
    this.master = from.master;
    this.local = from.local;
    this.genProcessType = from.genProcessType;
    this.genProcessId = from.genProcessId;
    this.backProcessId = from.backProcessId;
  }

  public String getName() {
    return name;
  }

  public List<MFile> getFiles() {
    return files;
  }

  public FeatureCollectionConfig.GribConfig getGribConfig() {
    return gribConfig;
  }

  public List<String> getFilenames() {
    List<String> result = new ArrayList<>();
    for (MFile file : files)
      result.add(file.getPath());
    Collections.sort(result);
    return result;
  }

  public List<Dataset> getDatasets() {
    return datasets;
  }

  public GribCollection.Dataset getDataset2D() {
    for (GribCollection.Dataset ds : datasets)
      if (ds.type == GribCollectionProto.Dataset.Type.TwoD) return ds;
    return null;
  }

  public GribCollection.Dataset findDataset(String name) {
    for (GribCollection.Dataset ds : datasets)
      if (ds.type.toString().equals(name)) return ds;
    return null;
  }

  public Dataset makeDataset(GribCollectionProto.Dataset.Type type) {
    Dataset result = new Dataset(type);
    datasets.add(result);
    return result;
  }

  public HorizCoordSys getHorizCS(int index) {
    return horizCS.get(index);
  }

  protected void makeHorizCS() {
    Map<Integer, HorizCoordSys> gdsMap = new HashMap<>();
    for (Dataset ds : datasets) {
      for (GroupHcs hcs : ds.getGroups())
        gdsMap.put(hcs.getGdsHash(), hcs.horizCoordSys);
    }

    horizCS = new ArrayList<>();
    for (HorizCoordSys hcs : gdsMap.values())
      horizCS.add(hcs);
  }

  public int findHorizCS(HorizCoordSys hcs) {
    return horizCS.indexOf(hcs);
  }

  public void addHorizCoordSystem(GdsHorizCoordSys hcs, byte[] rawGds, int gdsHash, String nameOverride) {
    horizCS.add(new HorizCoordSys(hcs, rawGds, gdsHash, nameOverride));
  }

  public MFile findMFileByName(String filename) {
    if (fileMap == null) {
      fileMap = new HashMap<String, MFile>(files.size() * 2);
      for (MFile file : files)
        fileMap.put(file.getName(), file);
    }
    return fileMap.get(filename);
  }

  public void setFiles(List<MFile> files) {
    this.files = Collections.unmodifiableList(files);
    this.fileMap = null;
  }

  /**
   * public by accident, do not use
   *
   * @param indexRaf the open raf of the index file
   */
  void setIndexRaf(RandomAccessFile indexRaf) {
    this.indexRaf = indexRaf;
    if (indexRaf != null) {
      if (indexFile == null) {
        indexFile = new File(indexRaf.getLocation());
      } /* else if (debug) {
        File f = new File(indexRaf.getLocation());
        if (!f.getCanonicalPath().equals().indexFile.getCanonicalPath())
          logger.warn("indexRaf {} not equal indexFile {}", f.getCanonicalPath(), indexFile.getCanonicalPath());
      }  */
    }
  }

  /**
   * get index file; may not exist
   *
   * @return File, but may not exist
   */
  public File getIndexFile() {
    if (indexFile == null) {
      String nameNoBlanks = StringUtil2.replace(name, ' ', "_");
      File f = new File(directory, nameNoBlanks + NCX_IDX2);
      indexFile = getDiskCache2().getFile(f.getPath()); // diskCcahe manages where the index file lives
    }
    return indexFile;
  }

  static public File getIndexFile(String collectionName, File directory) {
    String nameNoBlanks = StringUtil2.replace(collectionName, ' ', "_");
    File f = new File(directory, nameNoBlanks + NCX_IDX2);
    return getDiskCache2().getFile(f.getPath()); // diskCache manages where the index file lives
  }

  public File makeNewIndexFile(org.slf4j.Logger logger) {
    if (indexFile != null && indexFile.exists()) {
      if (!indexFile.delete())
        logger.warn("Failed to delete {}", indexFile.getPath());
    }
    indexFile = null;
    return getIndexFile();
  }

  public List<Parameter> getParams() {
    return params;
  }

  public int getCenter() {
    return center;
  }

  public int getSubcenter() {
    return subcenter;
  }

  public int getMaster() {
    return master;
  }

  public int getLocal() {
    return local;
  }

  public int getGenProcessType() {
    return genProcessType;
  }

  public int getGenProcessId() {
    return genProcessId;
  }

  public int getBackProcessId() {
    return backProcessId;
  }

  public boolean isGrib1() {
    return isGrib1;
  }

  public File getDirectory() {
    return directory;
  }

  public void setDirectory(File directory) {
    this.directory = directory;
  }

  protected GribCollection(String name, File directory, FeatureCollectionConfig.GribConfig config, boolean isGrib1) {
    this.name = name;
    this.directory = directory;
    this.gribConfig = config;
    this.isGrib1 = isGrib1;
  }

  /////////////////////////////////////////////

  // stuff for InvDatasetFcGrib
  public ucar.nc2.dataset.NetcdfDataset getNetcdfDataset(String datasetName, String groupName, String filename,
                                                         FeatureCollectionConfig.GribConfig gribConfig, Formatter errlog, org.slf4j.Logger logger) throws IOException {
    return null;
  }

  public ucar.nc2.dt.grid.GridDataset getGridDataset(String datasetName, String groupName, String filename,
                                                     FeatureCollectionConfig.GribConfig gribConfig, Formatter errlog, org.slf4j.Logger logger) throws IOException {
    return null;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff for Iosp

  public RandomAccessFile getDataRaf(int fileno) throws IOException {
    // absolute location
    MFile mfile = files.get(fileno);
    String filename = mfile.getPath();
    File dataFile = new File(directory, filename);

    // check reletive location - eg may be /upc/share instead of Q:
    if (!dataFile.exists()) {
      if (files.size() == 1) {
        dataFile = new File(directory, name); // single file case
      } else {
        dataFile = new File(directory, dataFile.getName()); // must be in same directory as the ncx file
      }
    }


    // data file not here
    if (!dataFile.exists()) {
      throw new FileNotFoundException("data file not found = " + dataFile.getPath());
    }

    RandomAccessFile want = getDataRaf(dataFile.getPath());
    want.order(RandomAccessFile.BIG_ENDIAN);
    return want;
  }

  private RandomAccessFile getDataRaf(String location) throws IOException {
    if (dataRafCache != null) {
      return (RandomAccessFile) dataRafCache.acquire(dataRafFactory, location, null);
    } else {
      return new RandomAccessFile(location, "r");
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff for FileCacheable

  public void close() throws java.io.IOException {
    if (objCache != null) {
      objCache.release(this);
    } else if (indexRaf != null) {
      indexRaf.close();
      indexRaf = null;
    }
  }

  @Override
  public String getLocation() {
    if (indexRaf != null) return indexRaf.getLocation();
    return getIndexFile().getPath();
  }

  @Override
  public long getLastModified() {
    if (indexFile != null) {
      return indexFile.lastModified();
    }
    return 0;
  }

  @Override
  public void setFileCache(FileCache fileCache) {
    this.objCache = fileCache;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  // these objects are created from the ncx index.
  private Set<String> groupNames = new HashSet<>(5);

  public class Dataset {
    final GribCollectionProto.Dataset.Type type;
    List<GroupHcs> groups;  // must be kept in order; unmodifiableList

    public Dataset(GribCollectionProto.Dataset.Type type) {
      this.type = type;
      groups = new ArrayList<>();
    }

    Dataset(Dataset from) {
      this.type = from.type;
      groups = new ArrayList<>(from.groups.size());
    }

    public GroupHcs addGroupCopy(GroupHcs from) {
      GroupHcs g = new GroupHcs(from);
      groups.add(g);
      return g;
    }

    public List<GroupHcs> getGroups() {
      return groups;
    }

    public GribCollectionProto.Dataset.Type getType() {
      return type;
    }

    public boolean isTwoD() {
      return type == GribCollectionProto.Dataset.Type.TwoD;
    }

    public GroupHcs getGroup(int index) {
      return groups.get(index);
    }

    public GroupHcs findGroupById(String id) {
      for (GroupHcs g : getGroups()) {
        if (g.getId().equals(id))
          return g;
      }
      return null;
    }

    public int findGroupIdxById(String id) {
      for (int i = 0; i < groups.size(); i++) {
        GroupHcs g = groups.get(i);
        if (g.getId().equals(id)) return i;
      }
      return -1;
    }

  }

  @Immutable
  public class HorizCoordSys {
    private final GdsHorizCoordSys hcs;
    private final byte[] rawGds;
    private final int gdsHash;
    private final String id, description;
    private final String nameOverride;

    public HorizCoordSys(GdsHorizCoordSys hcs, byte[] rawGds, int gdsHash, String nameOverride) {
      this.hcs = hcs;
      this.rawGds = rawGds;
      this.gdsHash = gdsHash;
      this.nameOverride = nameOverride;
      this.id = makeId();
      this.description = makeDescription();
    }

    public GdsHorizCoordSys getHcs() {
      return hcs;
    }

    public byte[] getRawGds() {
      return rawGds;
    }

    public int getGdsHash() {
      return gdsHash;
    }

    // unique name for Group
    public String getId() {
      return id;
    }

    // human readable
    public String getDescription() {
      return description;
    }

    public String getNameOverride() {
      return nameOverride;
    }

    private String makeId() {
      if (nameOverride != null) return nameOverride;

      // default id
      String base = hcs.makeId();
      // ensure uniqueness
      String tryit = base;
      int count = 1;
      while (groupNames.contains(tryit)) {
        count++;
        tryit = base + "-" + count;
      }
      groupNames.add(tryit);
      return tryit;
    }

    private String makeDescription() {
      // check for user defined group names
      String result = null;
      if (gribConfig != null && gribConfig.gdsNamer != null)
        result = gribConfig.gdsNamer.get(gdsHash);
      if (result != null) return result;

      return hcs.makeDescription(); // default desc
    }
  }

  // this class should be immutable, because it escapes
  public class GroupHcs implements Comparable<GroupHcs> {
    HorizCoordSys horizCoordSys;
    public List<VariableIndex> variList;
    public List<Coordinate> coords;
    public int[] filenose;
    private Map<Integer, GribCollection.VariableIndex> varMap;

    // partitions only
    public int[] run2part;   // runtimeCoord.length; which partition to use for runtime i
    public int[] offsets;   // runtimeCoord.length; offset of each runtime from firstDate

    GroupHcs() {
      this.variList = new ArrayList<>();
      this.coords = new ArrayList<>();
    }

    // copy constructor for PartitionBuilder
    GroupHcs(GroupHcs from) {
      this.horizCoordSys = from.horizCoordSys;     // reference
      this.variList = new ArrayList<>(from.variList.size());
      this.coords = new ArrayList<>(from.coords.size());
    }

    public void setHorizCoordSystem(GdsHorizCoordSys hcs, byte[] rawGds, int gdsHash, String nameOverride) {
      horizCoordSys = new HorizCoordSys(hcs, rawGds, gdsHash, nameOverride);
    }

    public VariableIndex addVariable(VariableIndex vi) {
      variList.add(vi);
      return vi;
    }

    public GribCollection getGribCollection() {
      return GribCollection.this;
    }

    // unique name for Group
    public String getId() {
      return horizCoordSys.getId();
    }

    // human readable
    public String getDescription() {
      return horizCoordSys.getDescription();
    }

    public GdsHorizCoordSys getGdsHorizCoordSys() {
      return horizCoordSys.getHcs();
    }

    public int getGdsHash() {
      return horizCoordSys.getGdsHash();
    }

    @Override
    public int compareTo(GroupHcs o) {
      return getDescription().compareTo(o.getDescription());
    }

    public List<MFile> getFiles() {
      List<MFile> result = new ArrayList<>();
      if (filenose == null) return result;
      for (int fileno : filenose)
        result.add(files.get(fileno));
      Collections.sort(result);
      return result;
    }

    public List<String> getFilenames() {
      List<String> result = new ArrayList<>();
      if (filenose == null) return result;
      for (int fileno : filenose)
        result.add(files.get(fileno).getPath());
      Collections.sort(result);
      return result;
    }

    public GribCollection.VariableIndex findVariableByHash(int cdmHash) {
      if (varMap == null) {
        varMap = new HashMap<>(variList.size() * 2);
        for (VariableIndex vi : variList)
          varMap.put(vi.cdmHash, vi);
      }
      return varMap.get(cdmHash);
    }

    private CalendarDateRange dateRange = null;

    public CalendarDateRange getCalendarDateRange() {
      if (dateRange == null) {
        CalendarDate start = null;
        CalendarDate lastRuntime = null;
        CalendarDate end = null;
        for (Coordinate coord : coords) {
          Coordinate.Type type = coord.getType();
          switch (type) {
            case runtime:
              CoordinateRuntime runtime = (CoordinateRuntime) coord;
              start = runtime.getFirstDate();
              lastRuntime = runtime.getLastDate();
              end = lastRuntime;
              break;
            case time:
              CoordinateTime time = (CoordinateTime) coord;
              CalendarDateRange range = time.makeCalendarDateRange(null, lastRuntime);
              if (end.isBefore(range.getEnd())) end = range.getEnd();
              break;
            case timeIntv:
              CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) coord;
              range = timeIntv.makeCalendarDateRange(null, lastRuntime);
              if (end.isBefore(range.getEnd())) end = range.getEnd();
              break;
          }
        }
        dateRange = CalendarDateRange.of(start, end);
      }
      return dateRange;
    }
  }

  public GribCollection.VariableIndex makeVariableIndex(GroupHcs g, int cdmHash, int discipline,
                                                        byte[] rawPds, Grib2Pds pds, List<Integer> index, long recordsPos, int recordsLen) {

    return new VariableIndex(g, -1, discipline, rawPds, pds, cdmHash, index, recordsPos, recordsLen);
  }

  public GribCollection.VariableIndex makeVariableIndex(GroupHcs g, VariableIndex other) {

    return new VariableIndex(g, other);
  }

  public class VariableIndex implements Comparable<VariableIndex> {
    public final GroupHcs group;     // belongs to this group
    public final int tableVersion;   // grib1 : can vary by variable
    public final int discipline;     // grib2
    public final byte[] rawPds;      // grib1 or grib2
    public final int cdmHash;
    public final long recordsPos;    // where the records array is stored in the index. 0 means no records
    public final int recordsLen;

    public List<Integer> coordIndex;  // indexes into group.coords

    private SparseArray<Record> sa;   // lazy read for GC and PGC only

    public boolean isTwod;
    public CoordinateTwoTimer twot;  // twoD only
    public int[] time2runtime;       // oneD only: for each timeIndex, which runtime coordinate does it use? 1-based so 0 = missing

    // derived from pds
    public int category, parameter, levelType, intvType, ensDerivedType, probType;
    private String intvName;
    public String probabilityName;
    public boolean isLayer;
    public int genProcessType;

    // stats
    public int ndups, nrecords, missing, totalSize;
    public float density;

    // temporary storage while building - do not use
    List<Coordinate> coords;

    public VariableIndex(GroupHcs g, int tableVersion, int discipline, byte[] rawPds, Grib2Pds pds,
                         int cdmHash, List<Integer> index, long recordsPos, int recordsLen) {
      this.group = g;
      this.tableVersion = tableVersion;
      this.discipline = discipline;
      this.rawPds = rawPds;
      this.cdmHash = cdmHash;
      this.coordIndex = index;
      this.recordsPos = recordsPos;
      this.recordsLen = recordsLen;

      // quantities that are stored in the pds
      this.category = pds.getParameterCategory();
      this.parameter = pds.getParameterNumber();
      this.levelType = pds.getLevelType1();
      this.intvType = pds.getStatisticalProcessType();
      this.isLayer = Grib2Utils.isLayer(pds);

      this.ensDerivedType = -1;
      if (pds.isEnsembleDerived()) {
        Grib2Pds.PdsEnsembleDerived pdsDerived = (Grib2Pds.PdsEnsembleDerived) pds;
        ensDerivedType = pdsDerived.getDerivedForecastType(); // derived type (table 4.7)
      }

      this.probType = -1;
      this.probabilityName = null;
      if (pds.isProbability()) {
        Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds;
        probabilityName = pdsProb.getProbabilityName();
        probType = pdsProb.getProbabilityType();
      }

      this.genProcessType = pds.getGenProcessType();
    }

    protected VariableIndex(GroupHcs g, VariableIndex other) {
      this.group = g;
      this.tableVersion = other.tableVersion;
      this.discipline = other.discipline;
      this.rawPds = other.rawPds;
      this.cdmHash = other.cdmHash;
      this.coordIndex = new ArrayList<>(other.coordIndex);
      this.recordsPos = 0;
      this.recordsLen = 0;

      this.category = other.category;
      this.parameter = other.parameter;
      this.levelType = other.levelType;
      this.intvType = other.intvType;
      this.isLayer = other.isLayer;
      this.ensDerivedType = other.ensDerivedType;
      this.probabilityName = other.probabilityName;
      this.probType = other.probType;
      this.genProcessType = other.genProcessType;

      this.time2runtime = other.time2runtime;
      this.twot = other.twot;   // LOOK why did i delete this before ??
      this.isTwod = other.isTwod;
    }

    public List<Coordinate> getCoordinates() {
      List<Coordinate> result = new ArrayList<>(coordIndex.size());
      for (int idx : coordIndex)
        result.add(group.coords.get(idx));
      return result;
    }

    public Coordinate getCoordinate(Coordinate.Type want) {
      for (int idx : coordIndex)
        if (group.coords.get(idx).getType() == want)
          return group.coords.get(idx);
      return null;
    }

    public Coordinate getCoordinate(int index) {
      int grpIndex = coordIndex.get(index);
      return group.coords.get(grpIndex);
    }

    public int getCoordinateIndex(Coordinate.Type want) {
      for (int idx : coordIndex)
        if (group.coords.get(idx).getType() == want)
          return idx;
      return -1;
    }

    public String getTimeIntvName() {
      if (intvName != null) return intvName;
      CoordinateTimeIntv timeiCoord = (CoordinateTimeIntv) getCoordinate(Coordinate.Type.timeIntv);
      if (timeiCoord == null) return null;
      intvName = timeiCoord.getTimeIntervalName();
      return intvName;
    }

    public SparseArray<Record> getSparseArray() {
      return sa;
    }

    /////////////////////////////
    public String id() {
      return discipline + "-" + category + "-" + parameter;
    }

    public int getVarid() {
      return (discipline << 16) + (category << 8) + parameter;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append("VariableIndex");
      sb.append("{tableVersion=").append(tableVersion);
      sb.append(", discipline=").append(discipline);
      sb.append(", category=").append(category);
      sb.append(", parameter=").append(parameter);
      sb.append(", levelType=").append(levelType);
      sb.append(", intvType=").append(intvType);
      sb.append(", ensDerivedType=").append(ensDerivedType);
      sb.append(", probType=").append(probType);
      sb.append(", intvName='").append(intvName).append('\'');
      sb.append(", probabilityName='").append(probabilityName).append('\'');
      sb.append(", isLayer=").append(isLayer);
      sb.append(", genProcessType=").append(genProcessType);
      sb.append(", cdmHash=").append(cdmHash);
      //sb.append(", partTimeCoordIdx=").append(partTimeCoordIdx);
      sb.append('}');
      return sb.toString();
    }

    public String toStringComplete() {
      final StringBuilder sb = new StringBuilder();
      sb.append("VariableIndex");
      sb.append("{tableVersion=").append(tableVersion);
      sb.append(", discipline=").append(discipline);
      sb.append(", category=").append(category);
      sb.append(", parameter=").append(parameter);
      sb.append(", levelType=").append(levelType);
      sb.append(", intvType=").append(intvType);
      sb.append(", ensDerivedType=").append(ensDerivedType);
      sb.append(", probType=").append(probType);
      sb.append(", intvName='").append(intvName).append('\'');
      sb.append(", probabilityName='").append(probabilityName).append('\'');
      sb.append(", isLayer=").append(isLayer);
      sb.append(", cdmHash=").append(cdmHash);
      sb.append(", recordsPos=").append(recordsPos);
      sb.append(", recordsLen=").append(recordsLen);
      sb.append(", group=").append(group.getId());
      //sb.append(", partTimeCoordIdx=").append(partTimeCoordIdx);
      sb.append("}\n");
      if (time2runtime == null) sb.append("time2runtime is null");
      else {
        sb.append("time2runtime=");
        for (int idx : time2runtime) sb.append(idx).append(",");
      }
      return sb.toString();
    }

    public String toStringShort() {
      Formatter sb = new Formatter();
      sb.format("Variable {%d-%d-%d", discipline, category, parameter);
      sb.format(", levelType=%d", levelType);
      sb.format(", intvType=%d", intvType);
      if (intvName != null && intvName.length() > 0) sb.format(" intv=%s", intvName);
      if (probabilityName != null && probabilityName.length() > 0) sb.format(" prob=%s", probabilityName);
      sb.format(" cdmHash=%d}", cdmHash);
      return sb.toString();
    }

    public void readRecords() throws IOException {
      if (this.sa != null) return;

      if (recordsLen == 0) return;
      byte[] b = new byte[recordsLen];

      // synchronize to protect the raf, and records[]
      synchronized (indexRaf) {
        indexRaf.seek(recordsPos);
        indexRaf.readFully(b);
      }

      /*
      message SparseArray {
        required fixed32 cdmHash = 1; // which variable
        repeated uint32 size = 2;     // multidim sizes
        repeated uint32 track = 3;    // 1-based index into record list, 0 == missing
        repeated Record records = 4;  // List<Record>
      }
     */
      // synchronize to protect records[]
      synchronized (this) {
        GribCollectionProto.SparseArray proto = GribCollectionProto.SparseArray.parseFrom(b);
        int cdmHash = proto.getCdmHash();
        if (cdmHash != this.cdmHash)
          throw new IllegalStateException("Corrupted index");

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
          records.add(new Record(pr.getFileno(), pr.getPos(), pr.getBmsPos()));
        }

        this.sa = new SparseArray<>(size, track, records);
      }
    }

    @Override
    public int compareTo(VariableIndex o) {
      int r = discipline - o.discipline;  // LOOK add center, subcenter, version?
      if (r != 0) return r;
      r = category - o.category;
      if (r != 0) return r;
      r = parameter - o.parameter;
      if (r != 0) return r;
      r = levelType - o.levelType;
      if (r != 0) return r;
      r = intvType - o.intvType;
      return r;
    }

    public void calcTotalSize() {
      this.totalSize = 1;
      for (int idx : this.coordIndex) {
        Coordinate coord = this.group.coords.get(idx);
        if (coord instanceof CoordinateTime2D)
          this.totalSize *= ((CoordinateTime2D) coord).getNtimes();
        else
          this.totalSize *= coord.getSize();
      }
      this.density = ((float) this.nrecords) / this.totalSize;
    }
  }

  public static class Record {
    public int fileno; // which file
    public long pos;   // offset on file where data starts
    public long bmsPos;   // if non-zero, offset where bms starts

    public Record(int fileno, long pos, long bmsPos) {
      this.fileno = fileno;
      this.pos = pos;
      this.bmsPos = bmsPos;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("GribCollection.Record{");
      sb.append("fileno=").append(fileno);
      sb.append(", pos=").append(pos);
      sb.append(", bmsPos=").append(bmsPos);
      sb.append('}');
      return sb.toString();
    }
  }

  public void showIndex(Formatter f) {
    f.format("%s%n%n", toString());
    f.format("Class (%s)%n", getClass().getName());
    if (files == null) {
      f.format("Files empty%n");
    } else {
      f.format("Files (%d)%n", files.size());
      for (MFile file : files)
        f.format("  %s%n", file);
      f.format("%n");
    }

    for (Dataset ds : datasets) {
      for (GroupHcs g : ds.groups) {
        for (VariableIndex v : g.variList) {
          f.format("  %s%n", v.toStringComplete());
        }
      }
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("GribCollection");
    sb.append("{\n name='").append(name).append('\'');
    sb.append("\n directory=").append(directory);
    sb.append("\n isGrib1=").append(isGrib1);
    sb.append("\n center=").append(center);
    sb.append("\n subcenter=").append(subcenter);
    sb.append("\n master=").append(master);
    sb.append("\n local=").append(local);
    sb.append("\n genProcessType=").append(genProcessType);
    sb.append("\n genProcessId=").append(genProcessId);
    sb.append("\n backProcessId=").append(backProcessId);
    sb.append("\n indexFile=").append(indexFile);
    sb.append("\n version=").append(version);
    sb.append('}');
    return sb.toString();
  }

  public GribCollection.GroupHcs makeGroup() {
    return new GribCollection.GroupHcs();
  }

  // must override NetcdfFile to pass in the iosp
  // used by the subclasses of GribCollection
  static protected class GcNetcdfFile extends NetcdfFile {
    public GcNetcdfFile(IOServiceProvider spi, RandomAccessFile raf, String location, CancelTask cancelTask) throws IOException {
      super(spi, raf, location, cancelTask);
    }
  }

}

