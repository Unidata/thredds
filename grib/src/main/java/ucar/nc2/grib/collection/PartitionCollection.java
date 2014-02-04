package ucar.nc2.grib.collection;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MCollection;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Misc;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheable;
import ucar.nc2.util.cache.FileFactory;
import ucar.sparr.Coordinate;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Collection of GribCollections or other PartionCollections, partitioned by reference time.
 *
 * @author John
 * @since 12/7/13
 */
public class PartitionCollection extends GribCollection {
  //static public final byte VERT_COORDS_DIFFER = 1;
  //static public final byte ENS_COORDS_DIFFER = 2;

  //////////////////////////////////////////////////////////
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
      RandomAccessFile raf = new RandomAccessFile(location, "r");
      Partition p = (Partition) iospMessage;
      return GribCdmIndex2.makeGribCollectionFromIndexFile(raf, p.getConfig(), CollectionUpdateType.never, null, p.getLogger());
      // return ucar.nc2.grib.collection.GribCollection.readFromIndex(p.isGrib1(), p.getName(), new File(p.getDirectory()), raf, p.getConfig(), p.getLogger());
    }
  };

  /* static public boolean update(boolean isGrib1, PartitionManager tpc, org.slf4j.Logger logger) throws IOException {
    //if (isGrib1) return Grib1TimePartitionBuilder.update(tpc, logger);
    return Grib2PartitionBuilder.update(tpc, logger);
  }

  // called by InvDatasetFcGrib
  static public PartitionCollection factory(boolean isGrib1, PartitionManager tpc, CollectionUpdateType force, org.slf4j.Logger logger) throws IOException {
    //if (isGrib1) return Grib1TimePartitionBuilder.factory(tpc, force, logger);
    return Grib2PartitionBuilder.factory(tpc, force, force, null, logger);
  }   */

  //////////////////////////////////////////////////////////////////////

  class PartitionForVariable2D {
    int partno, groupno, varno, flag;
    public int ndups, nrecords, missing;
    public float density;
  }

  public class VariableIndexPartitioned extends GribCollection.VariableIndex {
    public List<PartitionForVariable2D> partList; // must not change order

    VariableIndexPartitioned(GroupHcs g, VariableIndex other, int nparts) {
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
      sb.format("  %s %4s %3s %3s %6s%n", "part", "N", "dups", "Miss", "density");
      int totalN = 0, totalDups = 0, totalMiss = 0;
      for (PartitionForVariable2D partVar : partList) {
        sb.format("   %2d: %4d %3d %3d %6.2f%n", count++, partVar.nrecords, partVar.ndups, partVar.missing, partVar.density);
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
      // find the partition
      int firstIndex = indexWanted[0];
      int partno = group.isTwod ? getPartition2D(firstIndex) : getPartition1D(firstIndex);
      if (partno < 0) return null; // missing

      // find the vi in that partition
      GribCollection.VariableIndex compVindex2D = getVindex2D(partno); // the 2D component variable in this partition
      if (compVindex2D == null) return null; // missing

      if (isPartitionOfPartitions) {
        VariableIndexPartitioned compVindex2Dp = (VariableIndexPartitioned) compVindex2D;
        return getDataRecordPofP(indexWanted, compVindex2Dp);
      }

      // translate to coordinates in vindex
      int[] sourceIndex = group.isTwod ? translateIndex2D(indexWanted, compVindex2D) : translateIndex1D(indexWanted, compVindex2D);
      GribCollection.Record record = compVindex2D.getSparseArray().getContent(sourceIndex);

      return new DataRecord(PartitionCollection.this, partno, compVindex2D.group.getGdsHorizCoordSys(), record.fileno, record.pos, record.bmsPos);
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
        return compVindex2Dp.getDataRecord(indexWantedP);
      } else {
        int[] indexWantedP = translateIndex1D(indexWanted, compVindex2Dp);
        return compVindex2Dp.getDataRecord(indexWantedP);      }
    }


    private int getPartition2D(int runtimeIdx) {
      return group.run2part[runtimeIdx];
    }

    private int getPartition1D(int timeIdx) {
      int runtimeIdx = time2runtime[timeIdx];    // LOOK maybe want time2Part ?
      if (runtimeIdx == 0) return -1;  // 0 = missing
      return group.run2part[runtimeIdx - 1];
    }

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
      if (partVar == null) return null;

      Partition p = getPartitions().get(partno);
      try (GribCollection gc = p.getGribCollection()) { // ensure that its read in  try-with
        Dataset ds = gc.getDataset2D(); // always references the twoD dataset
        // the group and variable index may vary across partitions
        GribCollection.GroupHcs g = ds.groups.get(partVar.groupno);
        GribCollection.VariableIndex vindex = g.variList.get(partVar.varno);
        vindex.readRecords();
        return vindex;
      }  // LOOK ok to close ?? or is this defensive ??
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
            resultIdx = compCoord2D.matchTimeCoordinate(runtimeIdxPart, wholeVal, wholeCoord1Dtime.getRefDate());
          }
        } else {
          resultIdx = matchCoordinate(wholeCoord1D, idx, compCoord);
        }
        if (resultIdx < 0)
          return null;
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
        CoordinateTime2D.Time2D want = time2D.getOrgValue(wholeIndex[0], wholeIndex[1]);
        compTime2D.getIndex(want, result); // sets the first 2 indices - run and time
        countDim = 2;
      }

      // the remaining dimensions, if any
      while (countDim < wholeIndex.length) {
        int idx = wholeIndex[countDim];
        int resultIdx = matchCoordinate(getCoordinate(countDim), idx, compVindex2D.getCoordinate(countDim));
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

  class DataRecord implements Comparable<DataRecord> {
    PartitionCollection usePartition;
    int partno; // partition index in usePartition
    GdsHorizCoordSys hcs;
    int fileno;
    long drsPos;
    long bmsPos;  // if non zero, use alternate bms
    int resultIndex; // where does this record go in the result array? not calculated here

    DataRecord(PartitionCollection usePartition, int partno, GdsHorizCoordSys hcs, int fileno, long drsPos, long bmsPos) {
      this.usePartition = usePartition;
      this.partno = partno;
      this.hcs = hcs;
      this.fileno = fileno;
      this.drsPos = (drsPos == 0) ? GribCollection.MISSING_RECORD : drsPos; // 0 also means missing in Grib2
      this.bmsPos = bmsPos;
    }

    @Override
    public int compareTo(DataRecord o) {
      int rp = usePartition.getName().compareTo(o.usePartition.getName());
      if (rp != 0) return rp;
      int r = Misc.compare(partno, o.partno);
      if (r != 0) return r;
      r = Misc.compare(fileno, o.fileno);
      if (r != 0) return r;
      return Misc.compare(drsPos, o.drsPos);
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
    private String indexFilename;
    private long lastModified;

    // temporary storage while building - do not use
    GribCollection gc;

    // constructor from ncx
    public Partition(String name, String indexFilename, long lastModified, String directory) {
      this.name = name;
      this.indexFilename = indexFilename; // grib collection ncx
      this.directory = directory; // grib collection directory
      this.lastModified = lastModified;
    }

    public String getName() {
      return name;
    }

    public String getIndexFilename() {
      return indexFilename;
    }

    public String getDirectory() {
      return directory;
    }

    public long getLastModified() {
      return lastModified;
    }

    public boolean isGrib1() {
      return isGrib1;         // in GribCollection
    }

    public FeatureCollectionConfig.GribConfig getConfig() {
      return gribConfig;   // in GribCollection
    }

    public org.slf4j.Logger getLogger() {
      return logger;          // in TimePartition
    }

    // null if it came from the index
    public MCollection getDcm() {
      return dcm;            // in GribCollection
    }

    // acquire or construct GribCollection - caller must call gc.close() when done
    public GribCollection getGribCollection() throws IOException {
      GribCollection result;
      if (partitionCache != null) {
        result = (GribCollection) partitionCache.acquire(collectionFactory, indexFilename, indexFilename, -1, null, this);
      } else {
        result = (GribCollection) collectionFactory.open(indexFilename, -1, null, this);
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
              ", indexFilename='" + indexFilename + '\'' +
              ", lastModified='" + CalendarDate.of(lastModified) + '\'' +
              '}';
    }

    /////////////////////////////////////////////
    // only used during creation of index
    private MCollection dcm;

    // constructor from a MCollection object
    public Partition(MCollection dcm) {
      this.dcm = dcm;
      this.name = dcm.getCollectionName();
      this.directory = dcm.getRoot();
      this.lastModified = dcm.getLastModified();
    }

    public GribCollection makeGribCollection(CollectionUpdateType force) throws IOException {
      GribCollection result = GribCdmIndex2.makeGribCollectionFromMCollection(isGrib1, dcm, force, null, logger); // LOOK caller must close
      indexFilename = result.getIndexFile().getPath();
      return result;
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected final org.slf4j.Logger logger;
  protected List<Partition> partitions;
  // protected Map<String, Partition> partitionMap = new TreeMap<>(); // LOOK not used ??
  protected boolean isPartitionOfPartitions;

  protected PartitionCollection(String name, File directory, FeatureCollectionConfig.GribConfig config, boolean isGrib1, org.slf4j.Logger logger) {
    super(name, directory, config, isGrib1);
    this.logger = logger;
    this.partitions = new ArrayList<>();
    this.datasets = new ArrayList<>();
  }

  /**
   * Use partition names as the filenames
   */
  @Override
  public List<String> getFilenames() {
    List<Partition> parts = getPartitions();
    List<String> result = new ArrayList<String>(parts.size());
    for (Partition p : parts) result.add(p.indexFilename);
    return result;
  }

  public Partition addPartition(String name, String filename, long lastModified, String directory) {
    Partition partition = new Partition(name, filename, lastModified, directory);
    //partitionMap.put(name, partition);
    partitions.add(partition);
    return partition;
  }

  public void addPartition(MCollection dcm) {
    Partition partition = new Partition(dcm);
    //partitionMap.put(dcm.getCollectionName(), new Partition(dcm));
    partitions.add(partition);
  }


  public void sortPartitions() {
    Collections.sort(partitions);
    partitions = Collections.unmodifiableList(partitions);
  }

  /* public Partition getPartitionByName(String name) {
    return partitionMap.get(name);
  }

  public Partition getPartitionLast() {
    //int last = (this.gribConfig.filesSortIncreasing) ? partitions.size() - 1 : 0;
    //return partitions.get(last);
    return partitions.get(partitions.size() - 1);
  } */

  /* public void cleanup() throws IOException {
    if (partitions == null) return;
    for (TimePartition.Partition p : partitions)
      if (p.gribCollection != null)
        p.gribCollection.close();
  } */

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
  public VariableIndexPartitioned makeVariableIndexPartitioned(GribCollection.GroupHcs group,
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

  public List<Partition> getPartitions() {
    return partitions;
  }

  public List<Partition> getPartitionsSorted() {
    List<Partition> c = new ArrayList<>(partitions);
    Collections.sort(c);
    if (this.gribConfig != null && !this.gribConfig.filesSortIncreasing) {
      Collections.reverse(c);
    }
    return c;
  }

  public void removePartition(Partition p) {
    partitions.remove(p);
    //if (null != p.getDcm())
    //  partitionMap.remove(p.getDcm().getCollectionName());
  }

  public void showIndex(Formatter f) {
    List<Partition> plist = getPartitions();
    f.format("Partitions (%d)%n", plist.size());
    for (Partition p : plist)
      f.format("  %s%n", p);
    f.format("%n");

    for (Dataset ds : datasets) {
      f.format("%nDataset = %s%n", ds.type);

      for (GroupHcs g : ds.getGroups()) {
        f.format("Hcs = %s%n", g.horizCoordSys.getHcs());

        f.format("%nVarIndex (%d)%n", g.variList.size());
        for (VariableIndex v : g.variList)
          f.format("  %s%n", v.toStringComplete());

        f.format("%nCoords (%d)%n", g.coords.size());
        for (int i = 0; i < g.coords.size(); i++) {
          Coordinate tc = g.coords.get(i);
          f.format(" %d: %s%n", i, tc);
        }
      }
    }

    super.showIndex(f);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff for Iosp

  public RandomAccessFile getRaf(int partno, int fileno) throws IOException {
    Partition part = getPartitions().get(partno);
    GribCollection gc = part.getGribCollection();
    RandomAccessFile raf = gc.getDataRaf(fileno);
    gc.close(); // LOOK ??
    return raf;
  }

  public void close() throws java.io.IOException {
    assert (objCache == null);
    super.close();
  }

  // no longer will be used
  public void delete() throws java.io.IOException {
    // remove any partitions from the cache
    if (partitionCache != null) {
      for (Partition tp : partitions) {
        partitionCache.remove(tp.indexFilename);
      }
    }
    close();
  }

  ////////////////////////////////////////////////////
  // stuff for debugging

  public void setPartitionIndexReletive() {
    File dir = new File(getLocation()); // use main index location
    for (Partition p : getPartitions()) {
      File old = new File(p.indexFilename);
      File n = new File(dir.getParent(), old.getName());
      p.indexFilename = n.getPath(); // set partition index filenames reletive to it
    }
  }

}
