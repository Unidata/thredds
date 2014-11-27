/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.collection;

import net.jcip.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.MFile;
import ucar.coord.*;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.GribTables;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.nc2.util.cache.FileCacheable;
import ucar.unidata.io.RandomAccessFile;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * An Immutable GribCollection, corresponds to one index (ncx) file.
 * The index file is opened on demand.
 *
 * @author caron
 * @since 11/10/2014
 */
@Immutable
public abstract class GribCollectionImmutable implements Closeable, FileCacheable {
  static private final Logger logger = LoggerFactory.getLogger(GribCollectionImmutable.class);
  public static int countGC; // debug

  public enum Type {GC, SRC, MRC, TwoD, Best, Analysis} // must match with GribCollectionProto.Dataset.Type

  ////////////////////////////////////////////////////////////////
  protected final String name; // collection name; index filename must be directory/name.ncx2
  protected final File directory;
  protected final FeatureCollectionConfig config;
  protected final boolean isGrib1;
  protected final Info info;

  protected final List<Dataset> datasets;
  protected final List<GribHorizCoordSystem> horizCS; // one for each unique GDS
  protected final CoordinateRuntime masterRuntime;

  protected final Map<Integer, MFile> fileMap;    // all the files used in the GC; key is the index in original collection, GC has subset of them
  protected final GribTables cust;
  //protected Map<String, MFile> filenameMap;
  protected final String indexFilename;

  protected FileCacheIF objCache = null;  // optional object cache - used in the TDS

  // possible we could use the Proto equivalents, and eliminate GribCollectionMutable ?
  GribCollectionImmutable(GribCollectionMutable gc) {
    countGC++;

    this.config = gc.config;
    this.name = gc.name;
    this.directory = gc.directory;
    this.isGrib1 = gc.isGrib1;
    this.info = new Info(gc);

    List<Dataset> work = new ArrayList<>(gc.datasets.size());
    for (GribCollectionMutable.Dataset gcDataset : gc.datasets) {
      work.add( new Dataset(gcDataset.type, gcDataset.groups));
    }
    this.datasets = Collections.unmodifiableList( work);

    this.horizCS = Collections.unmodifiableList( gc.horizCS);
    this.masterRuntime = gc.masterRuntime;
    this.fileMap = gc.fileMap;
    this.cust = gc.cust;

    File indexFile = GribCdmIndex.makeIndexFile(name, directory);
    indexFilename = indexFile.getPath();
  }

  // overridden in PartitionCollection
  protected VariableIndex makeVariableIndex(GroupGC group, GribCollectionMutable.VariableIndex mutableVar) {
    return new VariableIndex(group, mutableVar);
  }

  public List<Dataset> getDatasets() {
    return datasets;
  }

  public Dataset getDataset(String name) {
    for (Dataset ds : datasets)
      if (ds.type.toString().equalsIgnoreCase(name)) return ds;
    return null;
  }

  public Dataset getDataset2D() {
    for (Dataset ds : datasets)
      if (ds.isTwoD()) return ds;
    return null;
  }

  public Dataset getDatasetCanonical() {
    for (Dataset ds : datasets) {
      if (ds.type != GribCollectionImmutable.Type.Best) return ds;
    }
    throw new IllegalStateException("GC.getDatasetCanonical failed on="+name);
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

  public static class Info {
    final int version; // the ncx version
    final int center, subcenter, master, local;  // GRIB 1 uses "local" for table version
    final int genProcessType, genProcessId, backProcessId;

    public Info(int version, int center, int subcenter, int master, int local, int genProcessType, int genProcessId, int backProcessId) {
      this.version = version;
      this.center = center;
      this.subcenter = subcenter;
      this.master = master;
      this.local = local;
      this.genProcessType = genProcessType;
      this.genProcessId = genProcessId;
      this.backProcessId = backProcessId;
    }

    public Info(GribCollectionMutable gc) {
      this.version = gc.version;
      this.center = gc.center;
      this.subcenter = gc.subcenter;
      this.master = gc.master;
      this.local = gc.local;
      this.genProcessType = gc.genProcessType;
      this.genProcessId = gc.genProcessId;
      this.backProcessId = gc.backProcessId;
    }


  }

  @Immutable
  public class Dataset {
    final Type type;
    final List<GroupGC> groups;  // must be kept in order, because PartitionForVariable2D has index into it

    public Dataset(Type type, List<GribCollectionMutable.GroupGC> groups) {
      this.type = type;
      List<GroupGC> work = new ArrayList<>(groups.size());
      for (GribCollectionMutable.GroupGC gcGroup : groups) {
        work.add( new GroupGC(gcGroup));
      }
      this.groups = Collections.unmodifiableList( work);
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

  // this class should be immutable, because it escapes
  @Immutable
  public class GroupGC {
    final GribHorizCoordSystem horizCoordSys;
    final List<VariableIndex> variList;
    final List<Coordinate> coords;      // shared coordinates
    final int[] filenose;               // key for GC.fileMap
    final private Map<Integer, VariableIndex> varMap;
    final boolean isTwoD;        // true if you should use time2runtime

    public GroupGC(GribCollectionMutable.GroupGC gc) {
      this.horizCoordSys = gc.horizCoordSys;
      this.coords = gc.coords;
      this.filenose = gc.filenose;
      this.varMap = new HashMap<>(gc.variList.size() * 2);
      this.isTwoD = gc.isTwoD;

      List<GribCollectionMutable.VariableIndex> gcVars = gc.variList;
      List<VariableIndex> work = new ArrayList<>(gcVars.size());
      for (GribCollectionMutable.VariableIndex gcVar : gcVars) {
        VariableIndex vi = makeVariableIndex(this, gcVar);
        work.add( vi);
        varMap.put(vi.getCdmHash(), vi);
      }
      this.variList = Collections.unmodifiableList( work);
    }

    public String getId() {
      return horizCoordSys.getId();
    }

    public int getGdsHash() {
      return horizCoordSys.getGdsHash();
    }

    public GribCollectionImmutable getGribCollection() {
      return GribCollectionImmutable.this;
    }

        // human readable
    public String getDescription() {
      return horizCoordSys.getDescription();
    }

    public GdsHorizCoordSys getGdsHorizCoordSys() {
      return horizCoordSys.getHcs();
    }

    public VariableIndex findVariableByHash(int cdmHash) {
      return varMap.get(cdmHash);
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
      final StringBuilder sb = new StringBuilder("GroupGC{");
      sb.append(GribCollectionImmutable.this.getName());
      sb.append(" isTwoD=").append(isTwoD);
      sb.append('}');
      return sb.toString();
    }

    public void show(Formatter f) {
      f.format("Group %s (%d) isTwoD=%s%n", horizCoordSys.getId(), horizCoordSys.getGdsHash(), isTwoD);
      f.format(" nfiles %d%n", filenose == null ? 0 : filenose.length);
      f.format(" hcs = %s%n", horizCoordSys.getHcs());
    }
  }

  @Immutable      // except for sa
  public class VariableIndex {
    final GroupGC group;     // belongs to this group
    final VariableIndex.Info info;

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
      this.coordIndex = gcVar.coordIndex;
      this.recordsPos = gcVar.recordsPos;
      this.recordsLen = gcVar.recordsLen;

      this.ndups = gcVar.ndups;
      this.nrecords = gcVar.nrecords;
      this.nmissing = gcVar.nmissing;
    }

    public int calcTotalSize() {
      int totalSize = 1;
      for (int idx : this.coordIndex) {
        Coordinate coord = this.group.coords.get(idx);
        if (coord instanceof CoordinateTime2D)
          totalSize *= ((CoordinateTime2D) coord).getNtimes();
        else
          totalSize *= coord.getSize();
      }
      return totalSize;
      //this.density = ((float) this.nrecords) / this.totalSize;
    }

    public synchronized void readRecords() throws IOException {
      if (this.sa != null) return;

      if (recordsLen == 0) return;
      byte[] b = new byte[recordsLen];

      try (RandomAccessFile indexRaf = RandomAccessFile.acquire(indexFilename)) {

        indexRaf.seek(recordsPos);
        indexRaf.readFully(b);

        /*
        message SparseArray {
          required fixed32 cdmHash = 1; // which variable
          repeated uint32 size = 2;     // multidim sizes
          repeated uint32 track = 3;    // 1-based index into record list, 0 == missing
          repeated Record records = 4;  // List<Record>
        }
       */
        GribCollectionProto.SparseArray proto = GribCollectionProto.SparseArray.parseFrom(b);
        int cdmHash = proto.getCdmHash();
        if (cdmHash != info.cdmHash)
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
        int ndups = proto.hasNdups() ? proto.getNdups() : -1;
        this.sa = new SparseArray<>(size, track, records, ndups);
      }
    }

    public synchronized Record getRecordAt(int sourceIndex) {
      return sa.getContent(sourceIndex);
    }

    public synchronized Record getRecordAt(int[] sourceIndex) {
      return sa.getContent(sourceIndex);
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

    public CoordinateTimeAbstract getCoordinateTime() {
      for (int idx : coordIndex)
        if (group.coords.get(idx) instanceof CoordinateTimeAbstract)
          return (CoordinateTimeAbstract) group.coords.get(idx);
      return null;
    }

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

    public byte[] getRawPds() {
      return info.rawPds;
    }

    public int getCdmHash() {
      return info.cdmHash;
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

    public int getNrecords() {
      return nrecords;
    }

    public int getNmissing() {
      return nmissing;
    }

    public String toStringFrom() {
      Formatter sb = new Formatter();
      sb.format("Variable {%d-%d-%d", info.discipline, info.category, info.parameter);
      sb.format(", levelType=%d", info.levelType);
      sb.format(", intvType=%d", info.intvType);
      sb.format(", group=%s}", group);
      return sb.toString();
    }

    @Immutable
    public final class Info {
      final int tableVersion;   // grib1 only : can vary by variable
      final int discipline;     // grib2 only
      final byte[] rawPds;      // grib1 or grib2
      final int cdmHash;

      // derived from pds
      final int category, parameter, levelType, intvType, ensDerivedType, probType;
      final String intvName;  // eg "mixed intervals, 3 Hour, etc"
      final String probabilityName;
      final boolean isLayer, isEnsemble;
      final int genProcessType;

      public Info(GribCollectionMutable.VariableIndex gcVar) {
        this.tableVersion = gcVar.tableVersion;
        this.discipline = gcVar.discipline;
        this.rawPds = gcVar.rawPds;
        this.cdmHash = gcVar.cdmHash;
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
      }

    }
  }

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

    //////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff for FileCacheable

  public synchronized void close() throws java.io.IOException {
    if (objCache != null) {
      if (objCache.release(this)) return;
    }
  }

    // release any resources like file handles
  public void release() throws IOException {
  }

  // reacquire any resources like file handles
  public void reacquire() throws IOException {
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

  public void showIndex(Formatter f) {
    f.format("Class (%s)%n", getClass().getName());
    f.format("%s%n%n", toString());

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
    if (fileMap == null) {
      f.format("Files empty%n");
    } else {
      f.format("Files count = %d%n", fileMap.size());
      /* for (int index : fileMap.keySet())
        f.format("  %d: %s%n", index, fileMap.get(index));
      f.format("%n");  */
    }

  }

  ////////////////////////////////////////

  public long getIndexFileSize() {
    File indexFile = new File(indexFilename);
    return indexFile.length();
  }

  public String getFilename(int fileno) {
    return fileMap.get(fileno).getPath();
  }

  public String getFirstFilename() {
    return null; // fileMap.get(fileno).getPath(); LOOK
  }

  public Collection<MFile> getFiles() {
    return fileMap.values();
  }

  public MFile findMFileByName(String filename) {
    for (MFile file : fileMap.values())
      if (file.getName().equals(filename))
        return file;
    return null;
  }

  public RandomAccessFile getDataRaf(int fileno) throws IOException {
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

  ///////////////////////

    // LOOK could use this in iosp
  public abstract String makeVariableName(VariableIndex vindex);

  // stuff for InvDatasetFcGrib
  public abstract ucar.nc2.dataset.NetcdfDataset getNetcdfDataset(Dataset ds, GroupGC group, String filename,
                                                                  FeatureCollectionConfig gribConfig, Formatter errlog, org.slf4j.Logger logger) throws IOException;

  public abstract ucar.nc2.dt.grid.GridDataset getGridDataset(Dataset ds, GroupGC group, String filename,
                                                              FeatureCollectionConfig gribConfig, Formatter errlog, org.slf4j.Logger logger) throws IOException;


}
