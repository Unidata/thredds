/*
 *
 *  * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package ucar.nc2.grib.collection;

import net.jcip.annotations.Immutable;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionAbstract;
import thredds.inventory.CollectionSpecParser;
import thredds.inventory.MFile;
import thredds.inventory.partition.DirectoryCollection;
import ucar.coord.*;
import ucar.nc2.grib.grib1.Grib1ParamTime;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2SectionProductDefinition;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarTimeZone;
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
import java.nio.file.Paths;
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
public abstract class GribCollection implements FileCacheable, AutoCloseable {
  public static final long MISSING_RECORD = -1;

  public enum Type {GC, TwoD, Best, Analysis} // must match with GribCollectionProto.Dataset.Type

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

  static public void setDiskCache2(DiskCache2 dc) {
    diskCache = dc;
  }

  static public DiskCache2 getDiskCache2() {
    if (diskCache == null)
      diskCache = DiskCache2.getDefault();

    return diskCache;
  }

  ///////////////////////////////////////////////////////////////////

   /*
  index = gcname + ".ncx"
  A partition divides the files into a tree of collections

  partition = none

    if multiple runtimes: make seperate GC for each one, make a PC that puts them together. GC name= collectionName + runtime, PC = collectionName.
    if single runtime:   GC name = collectionName
    in both cases, the index for the collection = collectionName

  partition = directory

    use the directory tree as the partition
    gcname = collectionName + directory

  partition = file

    use the directory tree and the individual files as the partition
    gcname = collectionName = filename

   */
  static public File getIndexFileFromConfig(FeatureCollectionConfig config) {
    Formatter errlog = new Formatter();
    CollectionSpecParser specp = new CollectionSpecParser(config.spec, errlog);

    String name = StringUtil2.replace(config.name, '\\', "/");

    String cname = null;
    switch (config.ptype) {
      case file:
      case directory:
        cname = DirectoryCollection.makeCollectionName(name, Paths.get(specp.getRootDir()));
        break;
      case none:
        cname = !specp.wantSubdirs() ? name : DirectoryCollection.makeCollectionName(name, Paths.get(specp.getRootDir()));  // LOOK ??
    }

    File f = getIndexFile(cname, new File(specp.getRootDir()));
    return getIndexFileInCache(f.getPath());
  }

  static File getIndexFile(String collectionName, File directory) {
    String nameNoBlanks = StringUtil2.replace(collectionName, ' ', "_");
    File f = new File(directory, nameNoBlanks + CollectionAbstract.NCX_SUFFIX);
    return getIndexFileInCache(f.getPath());
  }

  private static  CalendarDateFormatter cf = new CalendarDateFormatter("yyyyMMdd-HHmmss", new CalendarTimeZone("UTC"));
  static public File getIndexFile(String collectionName, File directory, CalendarDate runtime) {
    File f = new File(directory, makeName(collectionName, runtime)  + CollectionAbstract.NCX_SUFFIX);
    return getIndexFileInCache(f.getPath());
  }

  /**
   * Get index file, may be in cache directory, may not exist
   * @param path full path of index file
   * @return File, possibly in cache
   */
  static public File getIndexFileInCache(String path) {
    return getDiskCache2().getFile(path); // diskCache manages where the index file lives
  }  

  static public String makeName(String collectionName, CalendarDate runtime) {
    String nameNoBlanks = StringUtil2.replace(collectionName, ' ', "_");
    return nameNoBlanks+"-"+cf.toString(runtime);
  }

  static public String makeNameFromIndexFilename(String idxPathname) {
    idxPathname = StringUtil2.replace(idxPathname, '\\', "/");
    int pos = idxPathname.lastIndexOf('/');
    String idxFilename = (pos < 0) ? idxPathname : idxPathname.substring(pos+1);
    assert idxFilename.endsWith(CollectionAbstract.NCX_SUFFIX);
    String result =  idxFilename.substring(0, idxFilename.length() - CollectionAbstract.NCX_SUFFIX.length());
    return result;
  }

  ////////////////////////////////////////////////////////////////
  protected final String name; // collection name; index filename must be directory/name.ncx2
  protected /* final */ File directory;
  protected final FeatureCollectionConfig config;
  protected final boolean isGrib1;
  protected String indexFilename;  // not in the cache - so can derive name, directory <--> indexFilename

  // set by the builder
  public int version; // the ncx version
  public int center, subcenter, master, local;  // GRIB 1 uses "local" for table version
  public int genProcessType, genProcessId, backProcessId;
  public List<Parameter> params;          // not used
  private Map<Integer, MFile> fileMap;    // all the files used in the GC; key in index in original collection, GC has subset of them
  protected List<Dataset> datasets;
  protected List<HorizCoordSys> horizCS; // one for each unique GDS
  protected CoordinateRuntime masterRuntime;

  // not stored
  private Map<String, MFile> filenameMap;
  protected RandomAccessFile indexRaf; // this is the raf of the index (ncx) file, synchronize any access to it
  protected FileCache objCache = null;  // optional object cache - used in the TDS

  protected GribCollection(String name, File directory, String indexFilename, FeatureCollectionConfig config, boolean isGrib1) {
    this.name = name;
    this.directory = directory;
    this.config = config;
    this.isGrib1 = isGrib1;
    this.indexFilename = indexFilename;
    if (config == null)
      System.out.println("HEY GribCollection");
  }

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

  public Collection<MFile> getFiles() {
    return fileMap.values();
  }

  public FeatureCollectionConfig getConfig() {
    return config;
  }

  public List<String> getFilenames() {
    List<String> result = new ArrayList<>();
    for (MFile file : fileMap.values())
      result.add(file.getPath());
    Collections.sort(result);
    return result;
  }

  public List<Dataset> getDatasets() {
    return datasets;
  }

  public GribCollection.Dataset findDataset(String name) {
    for (GribCollection.Dataset ds : datasets)
      if (ds.type.toString().equalsIgnoreCase(name)) return ds;
    return null;
  }

  public Dataset makeDataset(Type type) {
    Dataset result = new Dataset(type);
    datasets.add(result);
    return result;
  }

  public GribCollection.Dataset getDatasetCanonical() {
    for (GribCollection.Dataset ds : datasets) {
      if (ds.getType() == Type.GC) return ds;
      if (ds.getType() == Type.TwoD) return ds;
    }
    return null;
  }

  public HorizCoordSys getHorizCS(int index) {
    return horizCS.get(index);
  }

  public CoordinateRuntime getMasterRuntime() {
    return masterRuntime;
  }

  protected void makeHorizCS() {
    Map<Integer, HorizCoordSys> gdsMap = new HashMap<>();
    for (Dataset ds : datasets) {
      for (GroupGC hcs : ds.getGroups())
        gdsMap.put(hcs.getGdsHash(), hcs.horizCoordSys);
    }

    horizCS = new ArrayList<>();
    for (HorizCoordSys hcs : gdsMap.values())
      horizCS.add(hcs);
  }

  public int findHorizCS(HorizCoordSys hcs) {
    return horizCS.indexOf(hcs);
  }

  public void addHorizCoordSystem(GdsHorizCoordSys hcs, byte[] rawGds, int gdsHash, String nameOverride, int predefinedGridDefinition) {
    horizCS.add(new HorizCoordSys(hcs, rawGds, gdsHash, nameOverride, predefinedGridDefinition));
  }

  public MFile findMFileByName(String filename) {
    if (filenameMap == null) {
      filenameMap = new HashMap<>(fileMap.size() * 2);
      for (MFile file : fileMap.values())
        filenameMap.put(file.getName(), file);
    }
    return filenameMap.get(filename);
  }

  public void setFileMap(Map<Integer, MFile> fileMap) {
    this.fileMap = fileMap;
  }

  /**
   * public by accident, do not use
   *
   * @param indexRaf the open raf of the index file
   */
  void setIndexRaf(RandomAccessFile indexRaf) {
    this.indexRaf = indexRaf;
    if (indexRaf != null) {
      indexFilename = indexRaf.getLocation(); // LOOK may be in cache ??
    }
  }

  /**
   * get index filename
   *
   * @return index filename; may not exist; may be in disk cache
   */
  public File getIndexFile() {
    return getIndexFileInCache(indexFilename);
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


  /////////////////////////////////////////////

  // stuff for InvDatasetFcGrib
  public abstract ucar.nc2.dataset.NetcdfDataset getNetcdfDataset(Dataset ds, GroupGC group, String filename,
                                                         FeatureCollectionConfig gribConfig, Formatter errlog, org.slf4j.Logger logger) throws IOException;
  public abstract ucar.nc2.dt.grid.GridDataset getGridDataset(Dataset ds, GroupGC group, String filename,
                                                     FeatureCollectionConfig gribConfig, Formatter errlog, org.slf4j.Logger logger) throws IOException;

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff for Iosp

  public RandomAccessFile getDataRaf(int fileno) throws IOException {
    // absolute location
    MFile mfile = fileMap.get(fileno);
    String filename = mfile.getPath();
    File dataFile = new File(filename);

    /* check reletive location - eg may be /upc/share instead of Q:  LOOK WTF ?
    if (!dataFile.exists()) {
      if (fileMap.size() == 1) {
        dataFile = new File(directory, name); // single file case
      } else {
        dataFile = new File(directory, dataFile.getName()); // must be in same directory as the ncx file
      }
    } */


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
    File indexFile = getIndexFile();
    if (indexFile.exists()) {
      return indexFile.lastModified();
    }
    return 0;
  }

  @Override
  public void setFileCache(FileCache fileCache) {
    this.objCache = fileCache;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  // these objects are created from the ncx index. lame - should only be in the builder i think
  private Set<String> groupNames = new HashSet<>(5);

  public class Dataset {
    final Type type;
    List<GroupGC> groups;  // must be kept in order, because PartitionForVariable2D has index into it

    public Dataset(Type type) {
      this.type = type;
      groups = new ArrayList<>();
    }

    Dataset(Dataset from) {
      this.type = from.type;
      groups = new ArrayList<>(from.groups.size());
    }

    public GroupGC addGroupCopy(GroupGC from) {
      GroupGC g = new GroupGC(from);
      groups.add(g);
      return g;
    }

    public Iterable<GroupGC> getGroups() {
      return groups;
    }

    public int getGroupsSize() {
       return groups.size();
     }

     public Type getType() {
      return type;
    }

    public boolean isTwoD() {
      return type == Type.TwoD;
    }

    public GroupGC getGroup(int index) {
      return groups.get(index);
    }

    public GroupGC findGroupById(String id) {
      for (GroupGC g : getGroups()) {
        if (g.getId().equals(id))
          return g;
      }
      return null;
    }
  }

  @Immutable
  public class HorizCoordSys { // encapsolates the gds; shared by the GroupHcs
    private final GdsHorizCoordSys hcs;
    private final byte[] rawGds;
    private final int gdsHash;
    private final String id, description;
    private final String nameOverride;
    private final int predefinedGridDefinition;

    public HorizCoordSys(GdsHorizCoordSys hcs, byte[] rawGds, int gdsHash, String nameOverride, int predefinedGridDefinition) {
      this.hcs = hcs;
      this.rawGds = rawGds;
      this.gdsHash = gdsHash;
      this.nameOverride = nameOverride;
      this.predefinedGridDefinition = predefinedGridDefinition;

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

    public int getPredefinedGridDefinition() {
      return predefinedGridDefinition;
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
      if (config.gribConfig.gdsNamer != null)
        result = config.gribConfig.gdsNamer.get(gdsHash);
      if (result != null) return result;

      return hcs.makeDescription(); // default desc
    }
  }

  // this class should be immutable, because it escapes
  public class GroupGC implements Comparable<GroupGC> {
    HorizCoordSys horizCoordSys;
    List<VariableIndex> variList;
    List<Coordinate> coords;      // shared coordinates
    int[] filenose;               // key for GC.fileMap
    Map<Integer, GribCollection.VariableIndex> varMap;
    boolean isTwod = true;

    GroupGC() {
      this.variList = new ArrayList<>();
      this.coords = new ArrayList<>();
    }

    // copy constructor for PartitionBuilder
    GroupGC(GroupGC from) {
      this.horizCoordSys = from.horizCoordSys;     // reference
      this.variList = new ArrayList<>(from.variList.size());
      this.coords = new ArrayList<>(from.coords.size());
      this.isTwod = from.isTwod;
    }

    public void setHorizCoordSystem(GdsHorizCoordSys hcs, byte[] rawGds, int gdsHash, String nameOverride, int predefinedGridDefinition) {
      horizCoordSys = new HorizCoordSys(hcs, rawGds, gdsHash, nameOverride, predefinedGridDefinition);
    }

    public VariableIndex addVariable(VariableIndex vi) {
      variList.add(vi);
      return vi;
    }

    public GribCollection getGribCollection() {
      return GribCollection.this;
    }

    public Iterable<VariableIndex> getVariables() {
      return variList;
    }

    public Iterable<Coordinate> getCoordinates() {
      return coords;
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
    public int compareTo(GroupGC o) {
      return getDescription().compareTo(o.getDescription());
    }

    public List<MFile> getFiles() {
      List<MFile> result = new ArrayList<>();
      if (filenose == null) return result;
      for (int fileno : filenose)
        result.add(fileMap.get(fileno));
      Collections.sort(result);
      return result;
    }

    public List<String> getFilenames() {
      List<String> result = new ArrayList<>();
      if (filenose == null) return result;
      for (int fileno : filenose)
        result.add(fileMap.get(fileno).getPath());
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
              range = timeIntv.makeCalendarDateRange(null);
              if (end.isBefore(range.getEnd())) end = range.getEnd();
              break;
          }
        }
        dateRange = CalendarDateRange.of(start, end);
      }
      return dateRange;
    }

    public int getNFiles() {
      if (filenose == null) return 0;
      return filenose.length;
    }

    public int getNCoords() {
      return coords.size();
    }

    public int getNVariables() {
      return variList.size();
    }

    public void show(Formatter f) {
       f.format("Group %s isTwoD=%s%n", horizCoordSys.getId(), isTwod);
       f.format(" nfiles %d%n", filenose == null ? 0 : filenose.length);
     }
  }

  public GribCollection.VariableIndex makeVariableIndex(GroupGC g, int cdmHash, int discipline, GribTables customizer,
           byte[] rawPds, List<Integer> index, long recordsPos, int recordsLen) {
    return new VariableIndex(g, discipline, customizer, rawPds, cdmHash, index, recordsPos, recordsLen);
  }

  public GribCollection.VariableIndex makeVariableIndex(GroupGC g, VariableIndex other) {
    return new VariableIndex(g, other);
  }

  public class VariableIndex implements Comparable<VariableIndex> {
    public final GroupGC group;     // belongs to this group
    public final int tableVersion;   // grib1 only : can vary by variable
    public final int discipline;     // grib2 only
    public final byte[] rawPds;      // grib1 or grib2
    public final int cdmHash;
    public final long recordsPos;    // where the records array is stored in the index. 0 means no records
    public final int recordsLen;

    List<Integer> coordIndex;  // indexes into group.coords

    private SparseArray<Record> sa;   // lazy read, for GC only

    // partition only
    CoordinateTwoTimer twot;  // twoD only
    int[] time2runtime;       // oneD only: for each timeIndex, which runtime coordinate does it use? 1-based so 0 = missing

    // derived from pds
    public final int category, parameter, levelType, intvType, ensDerivedType, probType;
    private String intvName;  // eg "mixed intervals, 3 Hour, etc"
    public final String probabilityName;
    public final boolean isLayer;
    public final int genProcessType;

    // stats
    public int ndups, nrecords, missing, totalSize;
    public float density;

    // temporary storage while building - do not use
    List<Coordinate> coords;

    private VariableIndex(GroupGC g, int discipline, GribTables customizer, byte[] rawPds,
                         int cdmHash, List<Integer> index, long recordsPos, int recordsLen) {
      this.group = g;
      this.discipline = discipline;
      this.rawPds = rawPds;
      this.cdmHash = cdmHash;
      this.coordIndex = index;
      this.recordsPos = recordsPos;
      this.recordsLen = recordsLen;

      if (isGrib1) {
        Grib1Customizer cust = (Grib1Customizer) customizer;
        Grib1SectionProductDefinition pds = new Grib1SectionProductDefinition(rawPds);

        // quantities that are stored in the pds
        this.category = 0;
        this.tableVersion = pds.getTableVersion();
        this.parameter = pds.getParameterNumber();
        this.levelType = pds.getLevelType();
        Grib1ParamTime ptime = pds.getParamTime(cust);
        if (ptime.isInterval()) {
          this.intvType = pds.getTimeRangeIndicator();
        } else {
          this.intvType = -1;
        }
        this.isLayer = cust.isLayer(pds.getLevelType());

        this.ensDerivedType = -1;
        this.probType = -1;
        this.probabilityName = null;

        this.genProcessType = pds.getGenProcess(); // LOOK process vs process type ??

      } else {
        Grib2SectionProductDefinition pdss = new Grib2SectionProductDefinition(rawPds);
         Grib2Pds pds = null;
         try {
           pds = pdss.getPDS();
         } catch (IOException e) {
           throw new RuntimeException(e);
         }
        this.tableVersion = -1;

        // quantities that are stored in the pds
        this.category = pds.getParameterCategory();
        this.parameter = pds.getParameterNumber();
        this.levelType = pds.getLevelType1();
        this.intvType = pds.getStatisticalProcessType();
        this.isLayer = Grib2Utils.isLayer(pds);

        if (pds.isEnsembleDerived()) {
          Grib2Pds.PdsEnsembleDerived pdsDerived = (Grib2Pds.PdsEnsembleDerived) pds;
          ensDerivedType = pdsDerived.getDerivedForecastType(); // derived type (table 4.7)
        }  else {
          this.ensDerivedType = -1;
        }

        if (pds.isProbability()) {
          Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds;
          probabilityName = pdsProb.getProbabilityName();
          probType = pdsProb.getProbabilityType();
        } else {
          this.probType = -1;
          this.probabilityName = null;
        }

        this.genProcessType = pds.getGenProcessType();
      }
    }

    protected VariableIndex(GroupGC g, VariableIndex other) {
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

    public int getCoordinateIdx(Coordinate.Type want) {
      for (int idx : coordIndex)
        if (group.coords.get(idx).getType() == want)
          return idx;
      return -1;
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

    public Iterable<Integer> getCoordinateIndex() {
      return coordIndex;
    }

    public String getTimeIntvName() {
      if (intvName != null) return intvName;
      CoordinateTimeIntv timeiCoord = (CoordinateTimeIntv) getCoordinate(Coordinate.Type.timeIntv);
      if (timeiCoord != null) {
        intvName = timeiCoord.getTimeIntervalName();
        return intvName;
      }

      CoordinateTime2D time2DCoord = (CoordinateTime2D) getCoordinate(Coordinate.Type.time2D);
      if (time2DCoord == null || !time2DCoord.isTimeInterval()) return null;
      intvName = time2DCoord.getTimeIntervalName();
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

      if (indexRaf != null) {
        // synchronize to protect the raf, and records[]
        synchronized (indexRaf) {
          indexRaf.seek(recordsPos);
          indexRaf.readFully(b);
        }
      } else {
        try (RandomAccessFile raf = new RandomAccessFile(getIndexFile().getPath(), "r")) {  // try-with-close
          raf.seek(recordsPos);
          raf.readFully(b);
        }
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
          records.add(new Record(pr.getFileno(), pr.getPos(), pr.getBmsPos(), pr.getScanMode()));
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
    public final int fileno;    // which file
    public final long pos;      // offset on file where data starts
    public final long bmsPos;   // if non-zero, offset where bms starts
    public final int scanMode;  // from gds

    public Record(int fileno, long pos, long bmsPos, int scanMode) {
      this.fileno = fileno;
      this.pos = pos;
      this.bmsPos = bmsPos;
      this.scanMode = scanMode;
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
    f.format("Class (%s)%n", getClass().getName());
    f.format("%s%n%n", toString());


    f.format(" master runtime coordinate%n");
    masterRuntime.showCoords(f);
    f.format("%n");

    for (Dataset ds : datasets) {
      f.format("Dataset %s%n", ds.getType());
      for (GroupGC g : ds.groups) {
        f.format("Group %s%n", g.horizCoordSys.getId());
        for (VariableIndex v : g.variList) {
          f.format("  %s%n", v.toStringShort());
        }
      }
    }
    if (fileMap == null) {
      f.format("Files empty%n");
    } else {
      f.format("Files (%d)%n", fileMap.size());
      for (int index : fileMap.keySet())  {
        f.format("  %d: %s%n", index, fileMap.get(index));
      }
      f.format("%n");
    }

  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("GribCollection{");
    sb.append("\nname='").append(name).append('\'');
    sb.append("\n directory=").append(directory);
    sb.append("\n config=").append(config);
    sb.append("\n isGrib1=").append(isGrib1);
    sb.append("\n version=").append(version);
    sb.append("\n center=").append(center);
    sb.append("\n subcenter=").append(subcenter);
    sb.append("\n master=").append(master);
    sb.append("\n local=").append(local);
    sb.append("\n genProcessType=").append(genProcessType);
    sb.append("\n genProcessId=").append(genProcessId);
    sb.append("\n backProcessId=").append(backProcessId);
    sb.append("\n}");
    return sb.toString();
  }

  public GroupGC makeGroup() {
    return new GroupGC();
  }

  // must override NetcdfFile to pass in the iosp
  // used by the subclasses of GribCollection
  static protected class NetcdfFileGC extends NetcdfFile {
    public NetcdfFileGC(IOServiceProvider spi, RandomAccessFile raf, String location, CancelTask cancelTask) throws IOException {
      super(spi, raf, location, cancelTask);
    }
  }

}

