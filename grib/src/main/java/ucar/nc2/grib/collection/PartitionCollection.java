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

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MCollection;
import ucar.coord.CoordinateRuntime;
import ucar.coord.CoordinateTime2D;
import ucar.coord.CoordinateTimeAbstract;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Misc;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheable;
import ucar.nc2.util.cache.FileFactory;
import ucar.coord.Coordinate;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Collection of GribCollections or other PartionCollections, partitioned by reference time.
 *
 * @author John
 * @since 12/7/13
 */
public abstract class PartitionCollection extends GribCollection {
  // object cache for index files - these are opened only as GribCollection
  private static FileCache partitionCache;

  static public void initPartitionCache(int minElementsInMemory, int maxElementsInMemory, int period) {
    partitionCache = new ucar.nc2.util.cache.FileCache("TimePartitionCache", minElementsInMemory, maxElementsInMemory, -1, period);
  }

  static public FileCache getPartitionCache() {
    return partitionCache;
  }

  static public void disableNetcdfFileCache() {
    if (null != partitionCache) partitionCache.disable();
    partitionCache = null;
  }

  static private final ucar.nc2.util.cache.FileFactory collectionFactory = new FileFactory() {
    public FileCacheable open(String location, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {
      RandomAccessFile raf = null;
      try {
        raf = new RandomAccessFile(location, "r");
        Partition p = (Partition) iospMessage;
        return GribCdmIndex.openGribCollectionFromIndexFile(raf, p.getConfig(), true, p.getLogger()); // dataOnly
      } catch (Throwable t) {
        if (raf != null)
          raf.close();
        throw t;
      }
    }
  };

  //////////////////////////////////////////////////////////////////////

  class PartitionForVariable2D {
    int partno, groupno, varno, flag;
    public int ndups, nrecords, missing;
    public float density;
  }

  public class VariableIndexPartitioned extends GribCollection.VariableIndex {
    private List<PartitionForVariable2D> partList; // must not change order - really ??

    VariableIndexPartitioned(GroupGC g, VariableIndex other, int nparts) {
      super(g, other);
      partList = new ArrayList<>(nparts);
    }

    public void addPartition(int partno, int groupno, int varno, int flag, int ndups, int nrecords,
                             int missing, float density) {

      PartitionForVariable2D partVar = new PartitionForVariable2D();
      partVar.partno = partno;
      partVar.groupno = groupno;
      partVar.varno = varno;
      partVar.flag = flag;

      // track stats in this PartVar
      partVar.density = density;
      partVar.ndups = ndups;
      partVar.missing = missing;
      partVar.nrecords = nrecords;

      // keep overall stats for this variable
      this.ndups += partVar.ndups;
      this.missing += partVar.missing;
      this.nrecords += partVar.nrecords;

      this.partList.add(partVar);
    }

    public void addPartition(int partno, int groupno, int varno, int flag, VariableIndex vi) {
      addPartition(partno, groupno, varno, flag, vi.ndups, vi.nrecords, vi.missing, vi.density);
    }

    public void addPartition(PartitionForVariable2D pv) {
      addPartition(pv.partno, pv.groupno, pv.varno, pv.flag, pv.ndups, pv.nrecords, pv.missing, pv.density);
    }

    public Iterable<PartitionForVariable2D> getPartitionsForVariable() {
      return partList;
    }

    public PartitionForVariable2D getPartitionsForVariable(int idx) {
      return partList.get(idx);
    }

    @Override
    public String toStringComplete() {
      Formatter sb = new Formatter();
      sb.format("VariableIndexPartitioned%n");
      sb.format(" partno=");
      for (PartitionForVariable2D partVar : partList)
        sb.format("%d,", partVar.partno);
      sb.format("%n groupno=");
      for (PartitionForVariable2D partVar : partList)
        sb.format("%d,", partVar.groupno);
      sb.format("%n varno=");
      for (PartitionForVariable2D partVar : partList)
        sb.format("%d,", partVar.varno);
      sb.format("%n flags=");
      for (PartitionForVariable2D partVar : partList)
        sb.format("%d,", partVar.flag);
      sb.format("%n");
      int count = 0;
      sb.format("     %7s %3s %3s %6s %3s%n", "N", "dups", "Miss", "density", "partition");
      int totalN = 0, totalDups = 0, totalMiss = 0;
      for (PartitionForVariable2D partVar : partList) {
        Partition part = partitions.get(partVar.partno);
        sb.format("   %2d: %7d %3d %3d   %6.2f   %d %s%n", count++, partVar.nrecords, partVar.ndups, partVar.missing, partVar.density, partVar.partno, part.getFilename());
        totalN += partVar.nrecords;
        totalDups += partVar.ndups;
        totalMiss += partVar.missing;
      }
      sb.format("total: %4d %3d %3d %n", totalN, totalDups, totalMiss);
      sb.format("%n");
      sb.format("totalSize = %4d density=%6.2f%n", this.totalSize, this.density);

      sb.format(super.toStringComplete());
      return sb.toString();
    }


    public void show(Formatter f) {
      if (twot != null)
        twot.showMissing(f);

      if (time2runtime != null) {
        Coordinate run = getCoordinate(Coordinate.Type.runtime);
        Coordinate tcoord = getCoordinate(Coordinate.Type.time);
        if (tcoord == null) tcoord = getCoordinate(Coordinate.Type.timeIntv);
        CoordinateTimeAbstract time = (CoordinateTimeAbstract) tcoord;
        CalendarDate ref = time.getRefDate();
        CalendarPeriod.Field unit = time.getTimeUnit().getField();

        f.format("time2runtime: %n");
        int count = 0;
        for (int idx : time2runtime) {
          if (idx == 0) {
            f.format(" %2d: MISSING%n", count);
            count++;
            continue;
          }
          Object val = time.getValue(count);
          f.format(" %2d: %s -> %2d (%s)", count, val, idx-1, run.getValue(idx-1));
          if (val instanceof Integer) {
            int valI = (Integer) val;
            f.format(" == %s ", ref.add((double) valI, unit));
          }
          f.format(" %n");
          count++;
        }
        f.format("%n");
      }
    }

    // doesnt work because not threadsafe
    public void cleanup() throws IOException {
      // TimePartition.this.cleanup(); LOOK!!
    }

    ///////////////////////////////////////////////////////////////////

    /**
     * find the data record for a request
     *
     * @param indexWanted the source index request, excluding x and y
     * @return DataRecord pointing to where the data is
     * @throws IOException
     */
    DataRecord getDataRecord(int[] indexWanted) throws IOException {

      if (GribIosp.debugRead) System.out.printf("%nPartitionCollection debugRead index wanted = (%s) on %s isTwod=%s%n", Misc.showInts(indexWanted), indexFilename, group.isTwod);

      // find the runtime index
      int firstIndex = indexWanted[0];
      int runIdx = group.isTwod ? firstIndex : time2runtime[firstIndex] - 1; // time2runtime is oneD
      if (GribIosp.debugRead && !group.isTwod) System.out.printf("  firstIndex = %d time2runtime[firstIndex]=%d %n", firstIndex, runIdx);
      if (runIdx < 0) {
        return null; // LOOK why is this possible?
      }

       // find the partition by matching run coordinate with master runtime
      CoordinateRuntime runtime = (CoordinateRuntime) getCoordinate(Coordinate.Type.runtime);
      Object val = runtime.getValue(runIdx);
      int masterIdx = masterRuntime.getIndex(val);
      int partno = run2part[masterIdx];
      if (GribIosp.debugRead) System.out.printf("  runCoord = %s masterRuntime.getIndex(runCoord)=%d partition=%d %n", val, masterIdx, partno);
      if (partno < 0) {
        return null; // missing
      }

      // find the vi in that partition
      GribCollection.VariableIndex compVindex2D = getVindex2D(partno); // the 2D component variable in the partno partition
      if (compVindex2D == null) return null; // missing

      if (isPartitionOfPartitions) {
        VariableIndexPartitioned compVindex2Dp = (VariableIndexPartitioned) compVindex2D;
        return getDataRecordPofP(indexWanted, compVindex2Dp);
      }

      // translate to coordinates in vindex
      int[] sourceIndex = group.isTwod ? translateIndex2D(indexWanted, compVindex2D) : translateIndex1D(indexWanted, compVindex2D);
      if (sourceIndex == null) return null; // missing
      GribCollection.Record record = compVindex2D.getSparseArray().getContent(sourceIndex);

      if (GribIosp.debugRead) System.out.printf("  result success: partno=%d fileno=%d %n", partno, record.fileno);
      return new DataRecord(PartitionCollection.this, partno, compVindex2D.group.getGdsHorizCoordSys(), record.fileno, record.pos, record.bmsPos, record.scanMode);
    }

    /**
     * find DataRecord in a PofP
     * @param indexWanted index into this PoP
     * @param compVindex2Dp 2D variable from the desired partition; may be PofP or PofGC
     * @return desired record to be read, from the GC
     * @throws IOException
     */
    private DataRecord getDataRecordPofP(int[] indexWanted, VariableIndexPartitioned compVindex2Dp) throws IOException {
      if (group.isTwod) {
        // corresponding index into compVindex2Dp
        int[] indexWantedP = translateIndex2D(indexWanted, compVindex2Dp);
        if (GribIosp.debugRead) System.out.printf("  getDataRecordPofP= %s %n", Misc.showInts(indexWantedP));
        return compVindex2Dp.getDataRecord(indexWantedP);
      } else {
        int[] indexWantedP = translateIndex1D(indexWanted, compVindex2Dp);
        if (GribIosp.debugRead) System.out.printf("  getDataRecordPofP= %s %n", Misc.showInts(indexWantedP));
        if (indexWantedP == null) return null;
        return compVindex2Dp.getDataRecord(indexWantedP);
      }
    }


    /* private int getPartition2D(int runtimeIdx) {
      return group.run2part[runtimeIdx];
    }

    private int getPartition1D(int timeIdx) {
      int runtimeIdx = time2runtime[timeIdx];
      if (runtimeIdx == 0) return -1;  // 0 = missing
      return group.run2part[runtimeIdx - 1];
    } */

    /**
     * Get VariableIndex (2D) for this partition
     *
     * @param partno partition number
     * @return VariableIndex or null if not exists
     * @throws IOException
     */
    private GribCollection.VariableIndex getVindex2D(int partno) throws IOException {
      // at this point, we need to instantiate the Partition and the vindex.records

      VariableIndexPartitioned vip =  isPartitionOfPartitions ?
        (PartitionCollection.VariableIndexPartitioned) getVariable2DByHash(group.horizCoordSys, cdmHash) :
        this;

      PartitionForVariable2D partVar = null;
      for (PartitionForVariable2D pvar : vip.partList)  { // LOOK linear search
        if (pvar.partno == partno) {
          partVar = pvar;
          break;
        }
      }
      if (partVar == null) {
        if (GribIosp.debugRead) System.out.printf("  cant find partition=%d in vip=%s%n", partno, vip);
        return null;
      }

      Partition p = getPartition(partno);
      try (GribCollection gc = p.getGribCollection()) { // ensure that its read in try-with
        Dataset ds = gc.getDatasetCanonical(); // always references the twoD or GC dataset
        // the group and variable index may vary across partitions
        GroupGC g = ds.groups.get(partVar.groupno);
        GribCollection.VariableIndex vindex = g.variList.get(partVar.varno);
        vindex.readRecords();
        return vindex;
      }  // LOOK opening the file here, and then again to read the data. partition cache helps i guess but we could do better i think.
    }


    /**
     * translate index in VariableIndexPartitioned to corresponding index in one of its component VariableIndex
     * by matching coordinate values
     *
     * @param wholeIndex index in VariableIndexPartitioned
     * @param compVindex2D     component 2D VariableIndex
     * @return corresponding index in compVindex2D, or null if missing
     */
    private int[] translateIndex1D(int[] wholeIndex, GribCollection.VariableIndex compVindex2D) {
      int[] result = new int[wholeIndex.length + 1];

      // figure out the runtime
      int timeIdx = wholeIndex[0];
      int runtimeIdxWhole = time2runtime[timeIdx] - 1;  // 1-based
      int runtimeIdxPart = matchCoordinate(getCoordinate(0), runtimeIdxWhole, compVindex2D.getCoordinate(0));
      if (runtimeIdxPart < 0)
        return null;
      result[0] = runtimeIdxPart;

      // figure out the time and any other dimensions
      int countDim = 0;
      while (countDim < wholeIndex.length) {
        int idx = wholeIndex[countDim];
        Coordinate compCoord = compVindex2D.getCoordinate(countDim + 1);
        Coordinate wholeCoord1D = getCoordinate(countDim + 1);
        int resultIdx;
        if (compCoord.getType() == Coordinate.Type.time2D) {
          CoordinateTime2D compCoord2D = (CoordinateTime2D) compCoord; // of the component
          CoordinateTimeAbstract wholeCoord1Dtime = (CoordinateTimeAbstract) wholeCoord1D;
          Object wholeVal = wholeCoord1D.getValue(idx);
          resultIdx = compCoord2D.matchTimeCoordinate(runtimeIdxPart, wholeVal, wholeCoord1Dtime.getRefDate());
          if (resultIdx < 0) {
            resultIdx = compCoord2D.matchTimeCoordinate(runtimeIdxPart, wholeVal, wholeCoord1Dtime.getRefDate()); // debug
          }
        } else {
          resultIdx = matchCoordinate(wholeCoord1D, idx, compCoord);
        }
        if (resultIdx < 0) {
          logger.info("Couldnt match coordinates for variable {}", compVindex2D);
          return null;
        }
        result[countDim + 1] = resultIdx;
        countDim++;
      }

      return result;
    }

    /**
     * Given the index in the whole (indexWhole), translate to index in component (compVindex2D) by matching the coordinate values
     * @param wholeIndex    index in the whole
     * @param compVindex2D  want index in here
     * @return  index into  compVindex2D
     */
    private int[] translateIndex2D(int[] wholeIndex, GribCollection.VariableIndex compVindex2D) {
      int[] result = new int[wholeIndex.length];
      int countDim = 0;

      // special case for 2D time
      CoordinateTime2D compTime2D = (CoordinateTime2D) compVindex2D.getCoordinate(Coordinate.Type.time2D);
      if (compTime2D != null) {
        CoordinateTime2D time2D = (CoordinateTime2D) getCoordinate(Coordinate.Type.time2D);
        CoordinateTime2D.Time2D want = time2D.getOrgValue(wholeIndex[0], wholeIndex[1], GribIosp.debugRead);
        if (GribIosp.debugRead) System.out.printf("  translateIndex2D[runIdx=%d, timeIdx=%d] in componentVar coords = (%s,%s) %n",
                wholeIndex[0], wholeIndex[1], (want == null) ? "null" : want.getRun(), want);
        if (want == null) return null;
        compTime2D.getIndex(want, result); // sets the first 2 indices - run and time
        countDim = 2;
      }

      // the remaining dimensions, if any
      while (countDim < wholeIndex.length) {
        int idx = wholeIndex[countDim];
        int resultIdx = matchCoordinate(getCoordinate(countDim), idx, compVindex2D.getCoordinate(countDim));
        if (GribIosp.debugRead) System.out.printf("  translateIndex2D[idx=%d] resultIdx= %d %n", idx, resultIdx);
        if (resultIdx < 0) {
          matchCoordinate(getCoordinate(countDim), idx, compVindex2D.getCoordinate(countDim)); // debug
          return null;
        }
        result[countDim] = resultIdx;
        countDim++;
      }

      return result;
    }

    private int matchCoordinate(Coordinate whole, int wholeIdx, Coordinate part) {
      Object val = whole.getValue(wholeIdx);
      if (val == null)
        return -1;
      return part.getIndex(val);
    }

  }

  class DataRecord extends GribIosp.DataRecord {
    PartitionCollection usePartition;
    int partno; // partition index in usePartition

    DataRecord(PartitionCollection usePartition, int partno, GdsHorizCoordSys hcs, int fileno, long drsPos, long bmsPos, int scanMode) {
      super(-1, fileno, drsPos, bmsPos, scanMode, hcs);
      this.usePartition = usePartition;
      this.partno = partno;
    }

    @Override
    public int compareTo(GribIosp.DataRecord o) {
      DataRecord op = (DataRecord) o;
      int rp = usePartition.getName().compareTo(op.usePartition.getName());
      if (rp != 0) return rp;
      int r = Misc.compare(partno, op.partno);
      if (r != 0) return r;
      r = Misc.compare(fileno, o.fileno);
      if (r != 0) return r;
      return Misc.compare(dataPos, o.dataPos);
    }

    public boolean usesSameFile(DataRecord o) {
      if (o == null) return false;
      int rp = usePartition.getName().compareTo(o.usePartition.getName());
      if (rp != 0) return false;
      int r = Misc.compare(partno, o.partno);
      if (r != 0) return false;
      r = Misc.compare(fileno, o.fileno);
      if (r != 0) return false;
      return true;
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////

  // wrapper around a GribCollection
  public class Partition implements Comparable<Partition> {
    private final String name, directory;
    private final String filename;
    private long lastModified;
    private boolean isBad;

    // temporary storage while building - do not use - must call getGribCollection()()
    // GribCollection gc;

    // constructor from ncx
    public Partition(String name, String filename, long lastModified, String directory) {
      this.name = name;
      this.filename = filename; // grib collection ncx
      this.lastModified = lastModified;
      this.directory = directory;
    }

    public String getName() {
      return name;
    }

    public String getFilename() {
      return filename;
    }

    public String getDirectory() {
      return directory;
    }

    public long getLastModified() {
      return lastModified;
    }

    public boolean isBad() {
      return isBad;
    }

    public void setBad(boolean isBad) {
      this.isBad = isBad;
    }

    public boolean isGrib1() {
      return isGrib1;         // in GribCollection
    }

    public FeatureCollectionConfig getConfig() {
      return config;   // in GribCollection
    }

    public org.slf4j.Logger getLogger() {
      return logger;          // in TimePartition
    }

    // null if it came from the index
    public MCollection getDcm() {
      return dcm;            // in GribCollection
    }

    public String getIndexFilenameInCache() {
      File file = new File(directory, filename);
      File existingFile = GribCollection.getExistingFileOrCache( file.getPath());
      if (existingFile == null) {
        GribCollection.getExistingFileOrCache( file.getPath());  // debug
        return null;
      }
      return existingFile.getPath();
    }

    // acquire or construct GribCollection - caller must call gc.close() when done
    public GribCollection getGribCollection() throws IOException {
      GribCollection result;
      String path = getIndexFilenameInCache();
      if (path == null) {
        if (GribIosp.debugIndexOnly) {  // we are running in debug mode where we only have the indices, not the data files
          // tricky: substitute the current root
          File orgParentDir = new File(directory);
          File currentFile = new File(PartitionCollection.this.indexFilename);
          File currentParent = currentFile.getParentFile();
          File currentParentWithDir = new File(currentParent, orgParentDir.getName());
          File nestedIndex = isPartitionOfPartitions ? new File(currentParentWithDir, filename) : new File(currentParent, filename); // JMJ
          path = nestedIndex.getPath();
        } else {
          throw new FileNotFoundException("No index filename for partition= "+this.toString());
        }
      }

      if (partitionCache != null) {                     // FileFactory factory, Object hashKey, String location, int buffer_size, CancelTask cancelTask, Object spiObject
        result = (GribCollection) partitionCache.acquire(collectionFactory, path, path, -1, null, this);
      } else {
        result = (GribCollection) collectionFactory.open(path, -1, null, this);
      }
      return result;
    }

    @Override
    public int compareTo(Partition o) {
      return name.compareTo(o.name);
    }

    @Override
    public String toString() {
      return "Partition{" +
              "dcm=" + dcm +
              ", name='" + name + '\'' +
              ", directory='" + directory + '\'' +
              ", filename='" + filename + '\'' +
              ", lastModified='" + CalendarDate.of(lastModified) + '\'' +
              ", isBad=" + isBad +
              '}';
    }

    /////////////////////////////////////////////
    // only used during creation of index
    private MCollection dcm;

    // constructor from a MCollection object
    public Partition(MCollection dcm) {
      this.dcm = dcm;
      this.name = dcm.getCollectionName();
      this.lastModified = dcm.getLastModified();
      this.directory = StringUtil2.replace(dcm.getRoot(), '\\', "/");

      String indexFilename = StringUtil2.replace(dcm.getIndexFilename(), '\\', "/");
      if (indexFilename.startsWith(directory)) {
        indexFilename = indexFilename.substring(directory.length());
        if (indexFilename.startsWith("/")) indexFilename = indexFilename.substring(1);
      }
      filename = indexFilename;

      FeatureCollectionConfig config = (FeatureCollectionConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_CONFIG);
      if (config == null)
        logger.warn("HEY Partition missing a FeatureCollectionConfig {}", dcm);
    }

    public GribCollection makeGribCollection(CollectionUpdateType force) throws IOException {
      // LOOK CLOSE if (gc != null) return gc;
      return GribCdmIndex.openGribCollectionFromMCollection(isGrib1, dcm, force, null, logger); // caller must close
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected final org.slf4j.Logger logger;
  protected List<Partition> partitions;

  public boolean isPartitionOfPartitions() {
    return isPartitionOfPartitions;
  }

  protected boolean isPartitionOfPartitions;

  int[] run2part;   // masterRuntime.length; which partition to use for masterRuntime i

  protected PartitionCollection(String name, File directory, FeatureCollectionConfig config, boolean isGrib1, org.slf4j.Logger logger) {
    super(name, directory, config, isGrib1);
    this.logger = logger;
    this.partitions = new ArrayList<>();
    this.datasets = new ArrayList<>();
  }

  public VariableIndex getVariable2DByHash(HorizCoordSys hcs, int cdmHash) {
    GribCollection.Dataset ds2d = getDataset2D();
    if (ds2d == null) return null;
    for (GroupGC groupHcs : ds2d.getGroups())
      if (groupHcs.horizCoordSys == hcs)
        return groupHcs.findVariableByHash(cdmHash);
    return null;
  }

  private GribCollection.Dataset getDataset2D() {
    for (GribCollection.Dataset ds : datasets)
      if (ds.isTwoD()) return ds;
    return null;
  }

  /**
   * Use partition names as the file names
   */
  @Override
  public List<String> getFilenames() {
    List<String> result = new ArrayList<>();
    for (Partition p : getPartitions()) {
      if (p.isBad()) continue;
      result.add(p.getIndexFilenameInCache());
    }
    return result;
  }

  public Partition addPartition(String name, String filename, long lastModified, String directory) {
    Partition partition = new Partition(name, filename, lastModified, directory);
    partitions.add(partition);
    return partition;
  }

  public void addPartition(MCollection dcm) {
    Partition partition = new Partition(dcm);
    partitions.add(partition);
  }


  public void sortPartitions() {
    Collections.sort(partitions);
    partitions = Collections.unmodifiableList(partitions);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////

  // construct - going to write index

  /**
   * Create a VariableIndexPartitioned
   *
   * @param group  new  VariableIndexPartitioned is in this group
   * @param from   copy info from here
   * @param nparts size of partition list
   * @return a new VariableIndexPartitioned
   */
  public VariableIndexPartitioned makeVariableIndexPartitioned(GroupGC group,
                                                               GribCollection.VariableIndex from, int nparts) {
    VariableIndexPartitioned vip = new VariableIndexPartitioned(group, from, nparts);
    group.addVariable(vip);

    if (from instanceof VariableIndexPartitioned && !isPartitionOfPartitions) {
      VariableIndexPartitioned fromp = (VariableIndexPartitioned) from;
      for (PartitionForVariable2D pv : fromp.partList)
        vip.addPartition(pv);
    }
    return vip;
  }

  public Iterable<Partition> getPartitions() {
    return partitions;
  }

  public Partition getPartition(int idx) {
    return partitions.get(idx);
  }

  public Partition getPartitionByName(String name) {
    for (Partition p : partitions)
      if (p.name.equalsIgnoreCase(name)) return p;
    return null;
  }

  public int getPartitionSize() {
     return partitions.size();
   }

   public List<Partition> getPartitionsSorted() {
    List<Partition> c = new ArrayList<>(partitions);
    Collections.sort(c);
    if (!this.config.gribConfig.filesSortIncreasing) {
      Collections.reverse(c);
    }
    return c;
  }

  public void removePartition(Partition p) {
    partitions.remove(p);
  }

  // return open GC
  public GribCollection getLatestGribCollection(List<String> paths) throws IOException {
    Partition last = partitions.get(partitions.size()-1);
    paths.add(last.getName());

    GribCollection gc = last.getGribCollection();
    if (gc instanceof PartitionCollection) {
      try {
        PartitionCollection pc = (PartitionCollection) gc;
        GribCollection result = pc.getLatestGribCollection(paths);
        return result;
      } finally {
        gc.close();  // make sure its closed even on exception
      }
    } else {
      return gc;
    }

  }

  public void showIndex(Formatter f) {
    super.showIndex(f);

    int count = 0;
    f.format("Partitions%n");
    for (Partition p :  getPartitions())
      f.format("%d:  %s%n", count++, p);
    f.format("%n");

    if (run2part == null) f.format("run2part null%n");
    else {
      f.format(" master runtime -> partition %n");
      count = 0;
      for (CalendarDate cd : masterRuntime.getRuntimesSorted()) {
        int partno =  run2part[count];
        Partition part = getPartition(partno);
        f.format(" %d:  %s -> part %3d %s%n", count, cd, run2part[count],  part);
        count++;
      }
      f.format("%n");
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff for Iosp

  public RandomAccessFile getRaf(int partno, int fileno) throws IOException {
    Partition part = getPartition(partno);
    try (GribCollection gc = part.getGribCollection()) {  // LOOK this closes the GC.ncx2
      return gc.getDataRaf(fileno);
    }
  }

  /* public void close() throws java.io.IOException {
    assert (objCache == null); LOOK WHY ??
    super.close();
  }  */

  /* no longer will be used
  public void delete() throws java.io.IOException {
    // remove any partitions from the cache
    if (partitionCache != null) {
      for (Partition tp : partitions) {
        partitionCache.remove(tp.indexFilename);
      }
    }
    close();
  }  */

}
