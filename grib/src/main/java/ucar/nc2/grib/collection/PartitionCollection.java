package ucar.nc2.grib.collection;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MCollection;
import thredds.inventory.partition.PartitionManager;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheable;
import ucar.nc2.util.cache.FileFactory;
import ucar.sparr.Coordinate;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Collection of GribCollections, partitioned by time
 *
 * @author John
 * @since 12/7/13
 */
public class PartitionCollection extends GribCollection {
  static public final byte VERT_COORDS_DIFFER = 1;
  static public final byte ENS_COORDS_DIFFER = 2;

  //////////////////////////////////////////////////////////
  // object cache for index files - these are opened only as GribCollection
  private static FileCache partitionCache;

  static public void initPartitionCache(int minElementsInMemory, int maxElementsInMemory, int period) {
    partitionCache = new ucar.nc2.util.cache.FileCache("TimePartitionCache", minElementsInMemory, maxElementsInMemory, -1, period);
  }

  static public FileCache getPartitionCache() {
    return partitionCache;
  }

  static private final ucar.nc2.util.cache.FileFactory collectionFactory = new FileFactory() {
    public FileCacheable open(String location, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {
      RandomAccessFile raf = new RandomAccessFile(location, "r");
      Partition p = (Partition) iospMessage;
      return ucar.nc2.grib.collection.GribCollection.readFromIndex(p.isGrib1(), p.getName(), new File(p.getDirectory()), raf, p.getConfig(), p.getLogger());
    }
  };

  static public void disableNetcdfFileCache() {
    if (null != partitionCache) partitionCache.disable();
    partitionCache = null;
  }


  static public boolean update(boolean isGrib1, PartitionManager tpc, org.slf4j.Logger logger) throws IOException {
    //if (isGrib1) return Grib1TimePartitionBuilder.update(tpc, logger);
    return Grib2PartitionBuilder.update(tpc, logger);
  }

  // called by InvDatasetFcGrib
  static public PartitionCollection factory(boolean isGrib1, PartitionManager tpc, CollectionUpdateType force, org.slf4j.Logger logger) throws IOException {
    //if (isGrib1) return Grib1TimePartitionBuilder.factory(tpc, force, logger);
    return Grib2PartitionBuilder.factory(tpc, force, force, null, logger);
  }

  //////////////////////////////////////////////////////////////////////

  public class Dataset {
    List<GroupHcs> groups;
    // List<GribCollection.HorizCoordSys> gdsList;
    final PartitionCollectionProto.Dataset.Type type;

    public Dataset(PartitionCollectionProto.Dataset.Type type) {
      this.type = type;
      groups = new ArrayList<>();
    }

    Dataset(Dataset from) {
      this.type = from.type;
      groups = new ArrayList<>(from.groups.size());
    }

    public GroupHcs addGroup(GroupHcs from) {
      GroupHcs g = new GroupHcs(from);
      groups.add(g);
      return g;
    }

    public List<GroupHcs> getGroups() {
      return groups;
    }

    public String getType() {
      return type.toString();
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

  class PartitionVariable {
    int partno, groupno, varno, flag;
    public int ndups, nrecords, missing;
    public float density;
  }

  public class VariableIndexPartitioned extends GribCollection.VariableIndex {
    public List<PartitionVariable> partList; // must not change order

    public VariableIndexPartitioned(VariableIndex other) {
      super(other);
    }

    public void addPartition(int partno, int groupno, int varno, int flag, int ndups, int nrecords,
                             int missing, float density) {
      PartitionVariable partVar = new PartitionVariable();
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

    public int getPartition(int runtimeIdx) {
      return group.run2part.get(runtimeIdx);
    }

    /**
     * Get VariableIndex for this partition
     * @param partno partition number
     * @return VariableIndex or null if not exists
     * @throws IOException
     */
    public GribCollection.VariableIndex getVindex(int partno) throws IOException {
      // at this point, we need to instantiate the Partition and the vindex.records

      PartitionVariable partVar = null;
      for (PartitionVariable pvar : partList)
        if (pvar.partno == partno) partVar = pvar;
      if (partVar == null) return null;

      Partition p = getPartitions().get(partno);
      try (GribCollection gc = p.getGribCollection()) { // ensure that its read in
        // the group and variable index may vary across partitions
        GribCollection.GroupHcs g = gc.groups.get(partVar.groupno);
        GribCollection.VariableIndex vindex = g.variList.get(partVar.varno);
        vindex.readRecords();
        return vindex;
      }  // LOOK ok to close ?? or is this defensive ??
    }

    @Override
    public String toStringComplete() {
      Formatter sb = new Formatter();
      sb.format("VariableIndexPartitioned%n");
      sb.format(" partno=");
      for (PartitionVariable partVar : partList)
        sb.format("%d,", partVar.partno);
      sb.format("%n groupno=");
      for (PartitionVariable partVar : partList)
         sb.format("%d,", partVar.groupno);
      sb.format("%n varno=");
      for (PartitionVariable partVar : partList)
        sb.format("%d,", partVar.varno);
      sb.format("%n flags=");
      for (PartitionVariable partVar : partList)
        sb.format("%d,", partVar.flag);
      sb.format("%n");
      int count = 0;
      sb.format("  %s %4s %3s %3s %6s%n", "part", "N", "dups", "Miss", "density");
      for (PartitionVariable partVar : partList)  {
        sb.format("   %2d: %4d %3d %3d %6.2f%n", count++, partVar.nrecords, partVar.ndups,  partVar.missing,  partVar.density);
      }
      sb.format("%n");

      sb.format(super.toStringComplete());
      return sb.toString();
    }

    // doesnt work because not threadsafe
    public void cleanup() throws IOException {
      // TimePartition.this.cleanup(); LOOK!!
    }

    @Override
    void setTotalSize(int totalSize, int sizePerRun) {
      super.setTotalSize(totalSize, sizePerRun);
      for (PartitionVariable pvar : partList)
        pvar.density = ((float) pvar.nrecords) / sizePerRun;
    }
  }

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

    // constructor from a TimePartition object
    public Partition(MCollection dcm) {
      this.dcm = dcm;
      this.name = dcm.getCollectionName();
      this.directory = dcm.getRoot();
      this.lastModified = dcm.getLastModified();
    }

    public GribCollection makeGribCollection(CollectionUpdateType force) throws IOException {
      GribCollection result = GribCollection.factory(isGrib1, dcm, force, null, logger); // LOOK caller must close
      indexFilename = result.getIndexFile().getPath();
      return result;
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected final org.slf4j.Logger logger;
  protected Map<String, Partition> partitionMap = new TreeMap<>();
  protected List<Partition> partitions;
  public List<Dataset> datasets;   // 2D, best, 0hour etc
  public List<HorizCoordSys> gdsList;  // factored out of the groups
  public List<HorizCoordSys> horizCS; // one for each gds

  public HorizCoordSys getHorizCS(int index) {
    return horizCS.get(index);
  }

  public void addHorizCoordSystem(GdsHorizCoordSys hcs, byte[] rawGds, int gdsHash, String nameOverride) {
    horizCS.add(new HorizCoordSys(hcs, rawGds, gdsHash, nameOverride));
  }

  protected PartitionCollection(String name, File directory, FeatureCollectionConfig.GribConfig config, boolean isGrib1, org.slf4j.Logger logger) {
    super(name, directory, config, isGrib1);
    this.logger = logger;
    this.partitions = new ArrayList<>();
    this.datasets = new ArrayList<>();
    this.horizCS = new ArrayList<>();
  }

  protected void set(PartitionCollection from) {
    super.set(from);

    datasets = new ArrayList<>(from.datasets.size());
    for (Dataset fromDset : from.datasets) {
      datasets.add (new Dataset(fromDset));
    }
  }

  protected void finish() {
    Set<HorizCoordSys> gdsSet = new HashSet<>();
    for (Dataset ds : datasets) {
      for (GroupHcs hcs : ds.getGroups())
        gdsSet.add(hcs.horizCoordSys);
    }

    gdsList = new ArrayList<>();
    for (HorizCoordSys hcs : gdsSet)
      gdsList.add(hcs);
  }

  public int findIndex(HorizCoordSys hcs) {
    return gdsList.indexOf(hcs);
  }

  public List<Dataset> getDatasets() {
    return datasets;
  }

  public Dataset makeDataset(PartitionCollectionProto.Dataset.Type type) {
    Dataset result = new Dataset(type);
    datasets.add(result);
    return result;
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
    partitionMap.put(name, partition);
    partitions.add(partition);
    return partition;
  }

  public void addPartition(MCollection dcm) {
    Partition partition = new Partition(dcm);
    partitionMap.put(dcm.getCollectionName(), new Partition(dcm));
    partitions.add(partition);
  }

  public Partition getPartitionByName(String name) {
    return partitionMap.get(name);
  }

  public Partition getPartitionLast() {
    //int last = (this.gribConfig.filesSortIncreasing) ? partitions.size() - 1 : 0;
    //return partitions.get(last);
    return partitions.get(partitions.size() - 1);
  }

  /* public void cleanup() throws IOException {
    if (partitions == null) return;
    for (TimePartition.Partition p : partitions)
      if (p.gribCollection != null)
        p.gribCollection.close();
  } */

  ///////////////////////////////////////////////////////////////////////////////////////////////////

  // construct - going to write index
  public VariableIndexPartitioned makeVariableIndexPartitioned(GribCollection.VariableIndex vi, int nparts) {
    VariableIndexPartitioned vip = new VariableIndexPartitioned(vi);
    vip.partList = new ArrayList<>(nparts);
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
    if (null != p.getDcm())
      partitionMap.remove(p.getDcm().getCollectionName());
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
    gc.close();
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
