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
import thredds.inventory.MFile;
import ucar.coord.*;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.GribIndexCache;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib1.Grib1Gds;
import ucar.nc2.grib.grib1.Grib1ParamTime;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib1.Grib1Variable;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarTimeZone;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;
import ucar.unidata.util.StringUtil2;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * A mutable class for writing indices or building GribCollectionImmutable
 *
 * @author John
 * @since 12/1/13
 */
public class GribCollectionMutable implements Closeable {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribCollectionMutable.class);
  static public final long MISSING_RECORD = -1;

  //////////////////////////////////////////////////////////

  static MFile makeIndexMFile(String collectionName, File directory) {
    String nameNoBlanks = StringUtil2.replace(collectionName, ' ', "_");
    return new GcMFile(directory, nameNoBlanks + CollectionAbstract.NCX_SUFFIX, -1, -1, -1); // LOOK dont know lastMod, size. can it be added later?
  }

  private static CalendarDateFormatter cf = new CalendarDateFormatter("yyyyMMdd-HHmmss", new CalendarTimeZone("UTC"));

  static public String makeName(String collectionName, CalendarDate runtime) {
    String nameNoBlanks = StringUtil2.replace(collectionName, ' ', "_");
    return nameNoBlanks + "-" + cf.toString(runtime);
  }

  ////////////////////////////////////////////////////////////////
  protected String name; // collection name; index filename must be directory/name.ncx2
  protected FeatureCollectionConfig config;
  protected boolean isGrib1;
  protected File directory;
  protected String orgDirectory;

  // set by the builder
  public int version; // the ncx version
  public int center, subcenter, master, local;  // GRIB 1 uses "local" for table version
  public int genProcessType, genProcessId, backProcessId;
  public List<Parameter> params;          // not used
  protected Map<Integer, MFile> fileMap;    // all the files used in the GC; key is the index in original collection, GC has subset of them
  protected List<Dataset> datasets;
  protected List<GribHorizCoordSystem> horizCS; // one for each unique GDS
  protected CoordinateRuntime masterRuntime;
  protected GribTables cust;
  protected int indexVersion;

  // not stored in index
  protected RandomAccessFile indexRaf; // this is the raf of the index (ncx) file
  protected String indexFilename;
  protected long lastModified;
  protected long fileSize;

  public static int countGC;

  protected GribCollectionMutable(String name, File directory, FeatureCollectionConfig config, boolean isGrib1) {
    countGC++;
    this.name = name;
    this.directory = directory;
    this.config = config;
    this.isGrib1 = isGrib1;
    if (config == null)
      logger.error("HEY GribCollection {} has empty config%n", name);
    if (name == null)
      logger.error("HEY GribCollection has null name dir={}%n", directory);
  }

  // for making partition collection
  protected void copyInfo(GribCollectionMutable from) {
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

  public File getDirectory() {
    return directory;
  }

  public String getLocation() {
    if (indexRaf != null) return indexRaf.getLocation();
    return getIndexFilepathInCache();
  }

  public Collection<MFile> getFiles() {
    return fileMap.values();
  }

  public FeatureCollectionConfig getConfig() {
    return config;
  }

  /**
   * The files that comprise the collection.
   * Actual paths, including the grib cache if used.
   *
   * @return list of filename.
   */
  public List<String> getFilenames() {
    List<String> result = new ArrayList<>();
    for (MFile file : fileMap.values())
      result.add(file.getPath());
    Collections.sort(result);
    return result;
  }

  public File getIndexParentFile() {
    if (indexRaf == null) return null;
    Path index = Paths.get(indexRaf.getLocation());
    Path parent = index.getParent();
    return parent.toFile();
  }

  public String getFilename(int fileno) {
    return fileMap.get(fileno).getPath();
  }

  public List<Dataset> getDatasets() {
    return datasets;
  }

  public Dataset makeDataset(GribCollectionImmutable.Type type) {
    Dataset result = new Dataset(type);
    datasets.add(result);
    return result;
  }

  public GribCollectionMutable.Dataset getDatasetCanonical() {
    for (GribCollectionMutable.Dataset ds : datasets) {
      if (ds.gctype != GribCollectionImmutable.Type.Best) return ds;
    }
    throw new IllegalStateException("GC.getDatasetCanonical failed on=" + name);
  }

  public GribHorizCoordSystem getHorizCS(int index) {
    return horizCS.get(index);
  }

  protected void makeHorizCS() {
    Map<Object, GribHorizCoordSystem> gdsMap = new HashMap<>();   // WTF ?? unique ???
    for (Dataset ds : datasets) {
      for (GroupGC hcs : ds.groups)
        gdsMap.put(hcs.getGdsHash(), hcs.horizCoordSys);
    }

    horizCS = new ArrayList<>();
    for (GribHorizCoordSystem hcs : gdsMap.values())
      horizCS.add(hcs);
  }

  public int findHorizCS(GribHorizCoordSystem hcs) {
    return horizCS.indexOf(hcs);
  }

  public void addHorizCoordSystem(GdsHorizCoordSys hcs, byte[] rawGds, Object gdsHashObject, int predefinedGridDefinition) {

    String hcsName = makeHorizCoordSysName(hcs);

    // check for user defined group names
    String desc = null;
    if (config.gribConfig.gdsNamer != null)
      desc = config.gribConfig.gdsNamer.get(gdsHashObject.hashCode());
    if (desc == null) desc = hcs.makeDescription(); // default desc

    horizCS.add(new GribHorizCoordSystem(hcs, rawGds, gdsHashObject, hcsName, desc, predefinedGridDefinition));
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
      this.indexFilename = indexRaf.getLocation();
    }
  }

  /**
   * get index filename
   *
   * @return index filename; may not exist; may be in disk cache
   */
  private String getIndexFilepathInCache() {
    File indexFile = GribCdmIndex.makeIndexFile(name, directory);
    return GribIndexCache.getFileOrCache(indexFile.getPath()).getPath();
  }

  // set from GribCollectionBuilderFromIndex.readFromIndex()
  public File setOrgDirectory(String orgDirectory) {
    this.orgDirectory = orgDirectory;
    directory = new File(orgDirectory);
    if (!directory.exists()) {
      File indexFile = new File(indexFilename);
      File parent = indexFile.getParentFile();
      if (parent.exists())
        directory = parent;
    }
    return directory;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff for FileCacheable

  public void close() throws java.io.IOException {

    if (indexRaf != null) {
      indexRaf.close();
      indexRaf = null;
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  // these objects are created from the ncx index. lame - should only be in the builder i think
  private Set<String> hcsNames = new HashSet<>(5);

  private String makeHorizCoordSysName(GdsHorizCoordSys hcs) {
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

  public class Dataset {
    public GribCollectionImmutable.Type gctype;
    List<GroupGC> groups;  // must be kept in order, because PartitionForVariable2D has index into it

    public Dataset(GribCollectionImmutable.Type type) {
      this.gctype = type;
      groups = new ArrayList<>();
    }

    Dataset(Dataset from) {
      this.gctype = from.gctype;
      groups = new ArrayList<>(from.groups.size());
    }

    public GroupGC addGroupCopy(GroupGC from) {
      GroupGC g = new GroupGC(from);
      groups.add(g);
      return g;
    }

    public List<GroupGC> getGroups() {
      return groups;
    }
  }

  public class GroupGC implements Comparable<GroupGC> {
    GribHorizCoordSystem horizCoordSys;
    List<VariableIndex> variList;
    List<Coordinate> coords;      // shared coordinates
    int[] filenose;               // key for GC.fileMap
    HashMap<GribCollectionMutable.VariableIndex, GribCollectionMutable.VariableIndex> varMap;
    boolean isTwoD = true;        // true except for Best (?)

    GroupGC() {
      this.variList = new ArrayList<>();
      this.coords = new ArrayList<>();
    }

    // copy constructor for PartitionBuilder
    GroupGC(GroupGC from) {
      this.horizCoordSys = from.horizCoordSys;     // reference
      this.variList = new ArrayList<>(from.variList.size());  // empty list
      this.coords = new ArrayList<>(from.coords.size());      // empty list
      this.isTwoD = from.isTwoD;
    }

    public VariableIndex addVariable(VariableIndex vi) {
      variList.add(vi);
      return vi;
    }

    public GribCollectionMutable getGribCollection() {
      return GribCollectionMutable.this;
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

    public byte[] getGdsBytes() {
      return horizCoordSys.getRawGds();
    }

    public Object getGdsHash() {
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

    // get the variable in this group that has same object equality as want
    public GribCollectionMutable.VariableIndex findVariableByHash(GribCollectionMutable.VariableIndex want) {
      if (varMap == null) {
        varMap = new HashMap<>(variList.size() * 2);
        for (VariableIndex vi : variList) {
          VariableIndex old = varMap.put(vi, vi);
          if (old != null) {
            logger.error("GribCollectionMutable has duplicate variable hash {} == {}", vi, old);
          }
          //System.out.printf("%s%n", vi.hashCode());
        }
      }
      GribCollectionMutable.VariableIndex result = varMap.get(want);
      /* if (result == null) {
        System.out.printf("%s%n", want.hashCode());
        for (VariableIndex vi : variList) {
          System.out.printf("%s%n", vi.hashCode());
          System.out.printf("%s%n", vi.equals(want));
        }
      } */
      return result;
    }

    private CalendarDateRange dateRange = null;

    public CalendarDateRange getCalendarDateRange() {
      if (dateRange == null) {
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
        dateRange = result;
      }
      return dateRange;
    }

    public int getNFiles() {
      if (filenose == null) return 0;
      return filenose.length;
    }

    public void show(Formatter f) {
      f.format("Group %s (%d) isTwoD=%s%n", horizCoordSys.getId(), horizCoordSys.getGdsHash().hashCode(), isTwoD);
      f.format(" nfiles %d%n", filenose == null ? 0 : filenose.length);
      f.format(" hcs = %s%n", horizCoordSys.getHcs());
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("GroupGC{");
      sb.append(GribCollectionMutable.this.getName());
      sb.append(" isTwoD=").append(isTwoD);
      sb.append('}');
      return sb.toString();
    }
  }

  public GribCollectionMutable.VariableIndex makeVariableIndex(GroupGC g, GribTables customizer, int discipline, int center,
                                    int subcenter, byte[] rawPds, List<Integer> index, long recordsPos, int recordsLen) {
    return new VariableIndex(g, customizer, discipline, center, subcenter, rawPds, index, recordsPos, recordsLen);
  }

  public class VariableIndex implements Comparable<VariableIndex> {
    public final GroupGC group;     // belongs to this group
    public final int tableVersion;   // grib1 only : can vary by variable
    public final int discipline, center, subcenter;     // grib2 only
    public final byte[] rawPds;      // grib1 or grib2
    public final long recordsPos;    // where the records array is stored in the index. 0 means no records
    public final int recordsLen;
    public Object gribVariable;    // use this to test for object equality

    List<Integer> coordIndex;  // indexes into group.coords

    // derived from pds
    public final int category, parameter, levelType, intvType, ensDerivedType, probType;
    private String intvName;  // eg "mixed intervals, 3 Hour, etc"
    public final String probabilityName;
    public final boolean isLayer, isEnsemble;
    public final int genProcessType;

    // stats
    public int ndups, nrecords, nmissing;

    // temporary storage while building - do not use
    List<Coordinate> coords;

    private VariableIndex(GroupGC g, GribTables customizer, int discipline, int center, int subcenter, byte[] rawPds,
                          List<Integer> index, long recordsPos, int recordsLen) {
      this.group = g;
      this.discipline = discipline;
      this.rawPds = rawPds;
      this.center = center;
      this.subcenter = subcenter;
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
        Grib1ParamTime ptime = cust.getParamTime(pds);
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
        this.isEnsemble = pds.isEnsemble();

        // LOOK config vs serialized config
        gribVariable = new Grib1Variable(cust, pds, (Grib1Gds) g.getGdsHash(), config.gribConfig.useTableVersion, config.gribConfig.intvMerge, config.gribConfig.useCenter);

      } else {
        Grib2Customizer cust2 = (Grib2Customizer) customizer;

        Grib2SectionProductDefinition pdss = new Grib2SectionProductDefinition(rawPds);
        Grib2Pds pds = pdss.getPDS();
        assert pds != null;
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
        } else {
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
        this.isEnsemble = pds.isEnsemble();

        // LOOK config vs serialized config
        gribVariable = new Grib2Variable (cust2, discipline, center, subcenter, (Grib2Gds) g.getGdsHash(), pds, config.gribConfig.intvMerge, config.gribConfig.useGenType);
      }
    }

    protected VariableIndex(GroupGC g, VariableIndex other) {
      this.group = g;
      this.tableVersion = other.tableVersion;
      this.discipline = other.discipline;
      this.center = other.center;
      this.subcenter = other.subcenter;
      this.rawPds = other.rawPds;
      this.gribVariable = other.gribVariable;
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
      this.isEnsemble = other.isEnsemble;
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
      sb.append(", cdmHash=").append(gribVariable.hashCode());
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
      sb.append(", cdmHash=").append(gribVariable.hashCode());
      sb.append(", recordsPos=").append(recordsPos);
      sb.append(", recordsLen=").append(recordsLen);
      sb.append(", group=").append(group.getId());
      //sb.append(", partTimeCoordIdx=").append(partTimeCoordIdx);
      sb.append("}\n");
      /* if (time2runtime == null) sb.append("time2runtime is null");
      else {
        sb.append("time2runtime=");
        for (int idx = 0; idx < time2runtime.getN(); idx++)
          sb.append(time2runtime.get(idx)).append(",");
      } */
      return sb.toString();
    }

    public String toStringShort() {
      Formatter sb = new Formatter();
      sb.format("Variable {%d-%d-%d", discipline, category, parameter);
      sb.format(", levelType=%d", levelType);
      sb.format(", intvType=%d", intvType);
      if (intvName != null && intvName.length() > 0) sb.format(" intv=%s", intvName);
      if (probabilityName != null && probabilityName.length() > 0) sb.format(" prob=%s", probabilityName);
      sb.format(" cdmHash=%d}", gribVariable.hashCode());
      return sb.toString();
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

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || !(o instanceof VariableIndex)) return false;

      VariableIndex that = (VariableIndex) o;
      return gribVariable.equals(that.gribVariable);
    }

    @Override
    public int hashCode() {
      return gribVariable.hashCode();
    }

  }  // VariableIndex

  @Immutable
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
      sb.append(", scanMode=").append(scanMode);
      sb.append('}');
      return sb.toString();
    }
  }

  public void showIndex(Formatter f) {
    f.format("Class (%s)%n", getClass().getName());
    f.format("%s%n%n", toString());

    //f.format(" master runtime coordinate%n");
    //masterRuntime.showCoords(f);
    //f.format("%n");

    for (Dataset ds : datasets) {
      f.format("Dataset %s%n", ds.gctype);
      for (GroupGC g : ds.groups) {
        f.format(" Group %s%n", g.horizCoordSys.getId());
        for (VariableIndex v : g.variList) {
          f.format("  %s%n", v.toStringShort());
        }
      }
    }
    if (fileMap == null) {
      f.format("Files empty%n");
    } else {
      f.format("Files (%d)%n", fileMap.size());
      for (int index : fileMap.keySet()) {
        f.format("  %d: %s%n", index, fileMap.get(index));
      }
      f.format("%n");
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("GribCollectionMutable{");
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

  public String showLocation() {
    return "name="+name+" directory="+directory;
  }

  public GroupGC makeGroup() {
    return new GroupGC();
  }

}

