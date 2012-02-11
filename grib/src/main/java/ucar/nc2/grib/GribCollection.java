/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib;

import net.jcip.annotations.ThreadSafe;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionManager;
import ucar.nc2.NetcdfFile;
import ucar.nc2.grib.grib1.Grib1CollectionBuilder;
import ucar.nc2.grib.grib2.Grib2CollectionBuilder;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.DiskCache;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheable;
import ucar.nc2.util.cache.FileFactory;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;
import ucar.unidata.util.StringUtil2;

import java.io.*;
import java.util.*;

/**
 * A collection of grib files as a single logical dataset.
 * Concrete classes are for Grib1 and Grib2.
 * Note that there is no dependence on GRIB tables here.
 *
 * @author caron
 * @since 4/6/11
 */
@ThreadSafe
public abstract class GribCollection implements FileCacheable {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribCollection.class);
  public static final String IDX_EXT = ".ncx";
  public static final long MISSING_RECORD = -1;

  //////////////////////////////////////////////////////////
  // object cache for data files - these are opened only as raf, not netcdfFile
  private static FileCache dataRafCache;

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

  static public void disableNetcdfFileCache() {
    if (null != dataRafCache) dataRafCache.disable();
    dataRafCache = null;
  }

  //////////////////////////////////////////////////////////

  // canonical order (time, ens, vert)
  public static int calcIndex(int timeIdx, int ensIdx, int vertIdx, int nens, int nverts) {
    if (nens == 0) nens = 1;
    if (nverts == 0) nverts = 1;
    return vertIdx + ensIdx * nverts + timeIdx * nverts * nens;
  }

  static public File getIndexFile(CollectionManager dcm) {
    return new File(new File(dcm.getRoot()), dcm.getCollectionName() + IDX_EXT);
  }

  /**
   * Create a GribCollection from a collection of grib files
   * @param isGrib1 true if files are grib1, else grib2
   * @param dcm the file collection : assume its been scanned
   * @param force should index file be used or remade?
   * @param f  error messages here
   * @return  GribCollection
   * @throws IOException  on io error
   */
  static public GribCollection factory(boolean isGrib1, CollectionManager dcm, CollectionManager.Force force, Formatter f) throws IOException {
    if (isGrib1) return Grib1CollectionBuilder.factory(dcm, force, f);
    return Grib2CollectionBuilder.factory(dcm, force, f);
  }

  static public GribCollection createFromIndex(boolean isGrib1, String name, File directory, RandomAccessFile raf, FeatureCollectionConfig.GribConfig config) throws IOException {
    if (isGrib1) return Grib1CollectionBuilder.createFromIndex(name, directory, raf, config);
    return Grib2CollectionBuilder.createFromIndex(name, directory, raf, config);
  }

  static public boolean update(boolean isGrib1, CollectionManager dcm, Formatter f) throws IOException {
    if (isGrib1) return Grib1CollectionBuilder.update(dcm, f);
    return Grib2CollectionBuilder.update(dcm, f);
  }

  ////////////////////////////////////////////////////////////////

  protected String name;
  protected File directory;
  protected FeatureCollectionConfig.GribConfig gribConfig;
  protected boolean isGrib1;
  public int version;

  // set by the builder
  public int center, subcenter, master, local;  // GRIB 1 uses "local" for table version
  public int genProcessType, genProcessId, backProcessId;
  public List<String> filenames;
  public List<GroupHcs> groups;
  public List<Parameter> params;

  // LOOK need thread safety
  protected RandomAccessFile indexRaf; // this is the raf of the index (ncx) file

  public String getName() {
    return name;
  }

  public List<String> getFilenames() {
    return filenames;
  }

  public RandomAccessFile getIndexRaf() {
    return indexRaf;
  }

  public void setIndexRaf(RandomAccessFile indexRaf) {
    this.indexRaf = indexRaf;
  }

  private File indexFile;

  public File getIndexFile() {
    if (indexFile == null) {
      File f = new File(directory, name + IDX_EXT);
      indexFile = DiskCache.getFile(f.getPath(), false);
    }
    return indexFile;
  }

  public File makeNewIndexFile() {
    if (indexFile != null && indexFile.exists())
      indexFile.delete();
    indexFile = null;
    return getIndexFile();
  }

  public List<GroupHcs> getGroups() {
    return groups;
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

  protected GribCollection(String name, File directory, FeatureCollectionConfig.GribConfig dcm, boolean isGrib1) {
    this.name = name;
    this.directory = directory;
    // this.config = (dcm == null) ? null : (FeatureCollectionConfig.GribConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    this.gribConfig = dcm;
    this.isGrib1 = isGrib1;
  }

  /////////////////////////////////////////////

  // stuff for InvDatasetFcGrib
  public abstract ucar.nc2.dataset.NetcdfDataset getNetcdfDataset(String groupName, String filename, FeatureCollectionConfig.GribConfig gribConfig) throws IOException;

  public abstract ucar.nc2.dt.GridDataset getGridDataset(String groupName, String filename, FeatureCollectionConfig.GribConfig gribConfig) throws IOException;

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

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff for Iosp

  public RandomAccessFile getDataRaf(int fileno) throws IOException {
    String filename = filenames.get(fileno);
    RandomAccessFile want = getDataRaf(filename);
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

  public void close() throws java.io.IOException {
    if (fileCache != null) {
      fileCache.release(this);
    } else if (indexRaf != null) {
      indexRaf.close();
      indexRaf = null;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff for FileCacheable

  @Override
  public String getLocation() {
    if (indexRaf != null) return indexRaf.getLocation();
    return getIndexFile().getPath();
  }

  @Override
  public boolean sync() throws IOException {
    return false;
  }

  @Override
  public void setFileCache(FileCache fileCache) {
    this.fileCache = fileCache;
  }

  protected FileCache fileCache = null;
  ////////////////////////////////////////////////////////////////////////////////////////////////////

  // these objects are created from the ncx index.
  private Set<String> groupNames = new HashSet<String>(5);
  public class GroupHcs implements Comparable<GroupHcs> {
    public GdsHorizCoordSys hcs;
    public byte[] rawGds;
    public int gdsHash;

    private String id, description;
    public List<VariableIndex> varIndex;
    public List<TimeCoord> timeCoords;
    public List<VertCoord> vertCoords;
    public List<EnsCoord> ensCoords;
    public int[] filenose;
    public List<TimeCoordUnion> timeCoordPartitions; // used only for time partitions - DO NOT USE

    public void setHorizCoordSystem(GdsHorizCoordSys hcs, byte[] rawGds, int gdsHash) {
      this.hcs = hcs;
      this.rawGds = rawGds;
      this.gdsHash = gdsHash;
    }

    private String makeId() {
      // check for user defined group names
      String result = null;
      if (gribConfig != null && gribConfig.gdsNamer != null)
        result = gribConfig.gdsNamer.get(gdsHash);
      if (result != null) {
        StringBuilder sb = new  StringBuilder(result);
        StringUtil2.replace(sb, ". :", "p--");
        return sb.toString();
      }
      if (gribConfig != null && gribConfig.groupNamer != null) {
        File firstFile = new File(filenames.get(filenose[0])); //  NAM_Firewxnest_20111215_0600.grib2
        LatLonPoint centerPoint = hcs.getCenterLatLon();
        StringBuilder sb = new  StringBuilder(firstFile.getName().substring(15, 26) + "-" + centerPoint.toString());
        StringUtil2.replace(sb, ". :", "p--");
        return sb.toString();
      }

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
      if (gribConfig != null && gribConfig.groupNamer != null) {
        File firstFile = new File(filenames.get(filenose[0])); //  NAM_Firewxnest_20111215_0600.grib2
        LatLonPoint centerPoint = hcs.getCenterLatLon();
        return "First Run " + firstFile.getName().substring(15, 26) + ", Center " + centerPoint;
      }

      return hcs.makeDescription(); // default desc
    }

    public GribCollection getGribCollection() {
      return GribCollection.this;
    }

    // unique name for Group
    public String getId() {
      if (id == null) id = makeId();
      return id;
    }

    // human readable
    public String getDescription() {
      if (description == null)
        description = makeDescription();

      return description;
    }

    @Override
    public int compareTo(GroupHcs o) {
      return getDescription().compareTo(o.getDescription());
    }

    public List<String> getFilenames() {
      List<String> result = new ArrayList<String>();
      for (int fileno : filenose)
        result.add(filenames.get(fileno));
      Collections.sort(result);
      return result;
    }

    public GribCollection.VariableIndex findAnyVariableWithTime(int usesTimeIndex) {
      for (VariableIndex vi : varIndex)
        if (vi.timeIdx == usesTimeIndex) return vi;
      return null;
    }

    public GribCollection.VariableIndex findVariableByHash(int cdmHash) {
      for (VariableIndex vi : varIndex)
        if (vi.cdmHash == cdmHash) return vi;
      return null;
    }

    /* public ThreddsMetadata.Variables getVariables() {
      ThreddsMetadata.Variables vars = new ThreddsMetadata.Variables("GRIB-2");
      for (VariableIndex vi : varIndex) {
        String name = Grib2Iosp.makeVariableName(GribCollection.this, vi);
        String desc = Grib2Iosp.makeVariableLongName(tables, vi);
        String units = Grib2Iosp.makeVariableUnits(tables, vi);
        vars.addVariable(new ThreddsMetadata.Variable(name, desc, name, units, null));
      }
      return vars;
    } */

    public CalendarDateRange getTimeCoverage() {
      TimeCoord useTc = null;
      for (TimeCoord tc : timeCoords) {
        if (useTc == null || useTc.getSize() < tc.getSize())  // use time coordinate with most values
          useTc = tc;
      }
      return (useTc == null) ? null : useTc.getCalendarRange();
    }

  }

      //Map<Integer,String> gdsNamer = (Map<Integer,String>) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GDS_NAMER);
      //String groupNamer = (String) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GROUP_NAMER);

  /* kludge
  private String setGroupNameOverride(int gdsHash, Map<Integer,String> gdsNamer, String groupNamer, MFile mfile) {
    String result = null;
    if (gdsNamer != null)
      result = gdsNamer.get(gdsHash);
    if (result != null) return result;
    if (groupNamer == null) return null;
    return mfile.getName();
  }   */



  /* public class HorizCoordSys {
    public Grib2Gds gds;
    public int template; // GDS Template number (code table 3.1)
    public int nx, ny, nPoints, scanMode;

    HorizCoordSys(Grib2SectionGridDefinition gdss) {
      this.template = gdss.getGDSTemplateNumber();
      this.nPoints = gdss.getNumberPoints();
      this.gds = gdss.getGDS();
      this.nx = gds.nx;
      this.ny = gds.ny;
      this.scanMode = gds.scanMode;
    }

    public String getName() {
      return gds.getNameShort() + "-" + ny + "X" + nx;
    }

    @Override
    public String toString() {
      Formatter f = new Formatter();
      f.format("name='%s' nc=%d ny=%d", getName(), nx, ny);
      //for (Parameter p : params)
      //  f.format("  %s%n", p);
      return f.toString();
    }

    /* public ThreddsMetadata.GeospatialCoverage getGeospatialCoverage() {
      ThreddsMetadata.Range eastwest = new ThreddsMetadata.Range(start, size, )
      return new ThreddsMetadata.GeospatialCoverage();
    }
  }   */

  public GribCollection.VariableIndex makeVariableIndex(GroupHcs g, int tableVersion,
                                                        int discipline, int category, int parameter, int levelType, boolean isLayer,
                                                        int intvType, String intvName, int ensDerivedType, int probType, String probabilityName,
                                                        int genProcessType,
                                                        int cdmHash, int timeIdx, int vertIdx, int ensIdx, long recordsPos, int recordsLen) {

    return new VariableIndex(g, tableVersion, discipline, category, parameter, levelType, isLayer,
            intvType, intvName, ensDerivedType, probType, probabilityName, genProcessType, cdmHash, timeIdx, vertIdx, ensIdx, recordsPos, recordsLen);
  }

  public class VariableIndex implements Comparable<VariableIndex> {
    public final int tableVersion; // grib1 : can vary by variable
    public final int discipline, category, parameter, levelType, intvType, ensDerivedType, probType;  // uniquely identifies the variable
    public final String intvName;                                                                     // uniquely identifies the variable
    public final String probabilityName;                                                              // uniquely identifies the variable
    public final boolean isLayer;                                                                     // uniquely identifies the variable
    public final int genProcessType;
    public final int cdmHash;                  // unique hashCode - from Grib2Record, but works here also
    public final int timeIdx, vertIdx, ensIdx; // which time, vert and ens coordinates to use (in group)
    public final long recordsPos;              // where the records array is stored in the index
    public final int recordsLen;
    public final GroupHcs group;               // belongs to this group

    public int ntimes, nverts, nens;           // time, vert and ens coordinate lengths
    public Record[] records;                   // Record[ntimes*nverts*nens] - lazy init

    public int partTimeCoordIdx; // partition time coordinate index

    public VariableIndex(GroupHcs g, int tableVersion,
                         int discipline, int category, int parameter, int levelType, boolean isLayer,
                         int intvType, String intvName, int ensDerivedType, int probType, String probabilityName,
                         int genProcessType,
                         int cdmHash, int timeIdx, int vertIdx, int ensIdx, long recordsPos, int recordsLen) {
      this.group = g;
      this.tableVersion = tableVersion;
      this.discipline = discipline;
      this.category = category;
      this.parameter = parameter;
      this.levelType = levelType;
      this.isLayer = isLayer;
      this.intvType = intvType;
      this.intvName = intvName;
      this.ensDerivedType = ensDerivedType;
      this.probabilityName = probabilityName;
      this.probType = probType;
      this.genProcessType = genProcessType;
      this.cdmHash = cdmHash;
      this.timeIdx = timeIdx;
      this.vertIdx = vertIdx;
      this.ensIdx = ensIdx;
      this.recordsPos = recordsPos;
      this.recordsLen = recordsLen;
    }

    public TimeCoord getTimeCoord() {
      return timeIdx < 0 ? null : group.timeCoords.get(timeIdx);
    }

    public VertCoord getVertCoord() {
      return vertIdx < 0 ? null : group.vertCoords.get(vertIdx);
    }

    public EnsCoord getEnsCoord() {
      return ensIdx < 0 ? null : group.ensCoords.get(ensIdx);
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
      sb.append(", timeIdx=").append(timeIdx);
      sb.append(", vertIdx=").append(vertIdx);
      sb.append(", ensIdx=").append(ensIdx);
      sb.append(", ntimes=").append(ntimes);
      sb.append(", nverts=").append(nverts);
      sb.append(", nens=").append(nens);
      sb.append(", partTimeCoordIdx=").append(partTimeCoordIdx);
      sb.append('}');
      return sb.toString();
    }

    public String toStringComplete() {
      final StringBuilder sb = new StringBuilder();
      sb.append("VariableIndex");
      sb.append("{tableVersion=").append(tableVersion);
      sb.append(", discipline=").append(discipline);
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
      sb.append(", timeIdx=").append(timeIdx);
      sb.append(", vertIdx=").append(vertIdx);
      sb.append(", ensIdx=").append(ensIdx);
      sb.append(", recordsPos=").append(recordsPos);
      sb.append(", recordsLen=").append(recordsLen);
      sb.append(", group=").append(group);
      sb.append(", ntimes=").append(ntimes);
      sb.append(", nverts=").append(nverts);
      sb.append(", nens=").append(nens);
      sb.append(", records=").append(records == null ? "null" : Arrays.asList(records).toString());
      sb.append(", partTimeCoordIdx=").append(partTimeCoordIdx);
      sb.append('}');
      return sb.toString();
    }

    public Record[] getRecords() throws IOException {
      readRecords();
      return records;
    }

    // LOOK : use ehcache here ??
    public void readRecords() throws IOException {
      if (records != null) return;
      byte[] b = new byte[recordsLen];

      // synchronize to protect the raf, and records[]
      synchronized (indexRaf) {
        indexRaf.seek(recordsPos);
        indexRaf.readFully(b);
      }

      // synchronize to protect records[]
      synchronized (this) {
        GribCollectionProto.VariableRecords proto = GribCollectionProto.VariableRecords.parseFrom(b);
        int cdmHash = proto.getCdmHash();
        if (cdmHash != this.cdmHash)
          throw new IllegalStateException("Corrupted index");
        int n = proto.getRecordsCount();
        Record[] recordsTemp = new Record[n];
        for (int i = 0; i < n; i++) {
          GribCollectionProto.Record pr = proto.getRecords(i);
          recordsTemp[i] = new Record(pr.getFileno(), pr.getPos(), pr.getMissing());
        }
        records = recordsTemp; // switch all at once - worse case is it gets read more than once
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

  }

  public static class Record {
    public int fileno; // which file
    public long pos;   // offset on file where data starts

    public Record(int fileno, long pos, boolean missing) {
      this.fileno = fileno;
      this.pos = pos;
      if (missing) this.pos = MISSING_RECORD;
    }
  }

  public void showIndex(Formatter f) {
    f.format("%s%n%n", toString());
    f.format("Class (%s)%n", getClass().getName());
    f.format("Files (%d)%n", filenames.size());
    for (String file : filenames)
      f.format("  %s%n", file);
    f.format("%n");

    for (GroupHcs g : groups) {
      f.format("Hcs = %s%n", g.hcs);

      f.format("%nVarIndex (%d)%n", g.varIndex.size());
      for (VariableIndex v : g.varIndex)
        f.format("  %s%n", v.toStringComplete());

      f.format("%nTimeCoords (%d)%n", g.timeCoords.size());
      for (int i = 0; i < g.timeCoords.size(); i++) {
        TimeCoord tc = g.timeCoords.get(i);
        f.format(" %d: %s%n", i, tc);
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
    sb.append("\n version=").append(version);
    sb.append("\n center=").append(center);
    sb.append("\n subcenter=").append(subcenter);
    sb.append("\n master=").append(master);
    sb.append("\n local=").append(local);
    sb.append("\n genProcessType=").append(genProcessType);
    sb.append("\n genProcessId=").append(genProcessId);
    sb.append("\n backProcessId=").append(backProcessId);
    sb.append("\n indexFile=").append(indexFile);
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
