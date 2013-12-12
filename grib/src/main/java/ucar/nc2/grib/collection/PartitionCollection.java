package ucar.nc2.grib.collection;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionManager;
import thredds.inventory.MCollection;
import thredds.inventory.partition.PartitionManager;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheable;
import ucar.nc2.util.cache.FileFactory;
import ucar.sparr.SparseArray;
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

  ///////////////////////////////////////////////////////////////////////

  static public boolean update(boolean isGrib1, PartitionManager tpc, org.slf4j.Logger logger) throws IOException {
    //if (isGrib1) return Grib1TimePartitionBuilder.update(tpc, logger);
    return Grib2PartitionBuilder.update(tpc, logger);
  }

  static public PartitionCollection factory(boolean isGrib1, PartitionManager tpc, CollectionManager.Force force, org.slf4j.Logger logger) throws IOException {
    //if (isGrib1) return Grib1TimePartitionBuilder.factory(tpc, force, logger);
    return Grib2PartitionBuilder.factory(tpc, force, logger);
  }

  public class VariableIndexPartitioned extends GribCollection.VariableIndex {
    public List<GribCollectionProto.PartVar> partList;

    public VariableIndexPartitioned(VariableIndex other) {
      super(other);
    }

    public void addPartition(int partno, int groupIdx, int varIdx, int flag, VariableIndex vi) {
      GribCollectionProto.PartVar.Builder b = GribCollectionProto.PartVar.newBuilder();
      b.setGroupno(groupIdx);
      b.setVarno(varIdx);
      b.setFlag(flag);

      // track stats in this PartVar
      b.setDensity(vi.density);
      b.setNdups(vi.ndups);
      b.setMissing(vi.missing);
      b.setNrecords(vi.nrecords);

      // keep overall stats for this variable
      density += vi.density;  // will divide by nparts when storing
      ndups += vi.ndups;
      missing += vi.missing;
      nrecords += vi.nrecords;

      if (vi.density > 1.0)
        System.out.println("HEY vi.density");

      assert partno == this.partList.size();
      this.partList.add(b.build());
    }

    public int getPartition(int runtimeIdx) {
      return group.run2part[runtimeIdx];
    }

    public GribCollection.VariableIndex getVindex(int partno) throws IOException {
      // at this point, we need to instantiate the Partition and the vindex.records
      Partition p = getPartitions().get(partno);
      GribCollectionProto.PartVar partVar = partList.get(partno);

      try (GribCollection gc = p.getGribCollection()) { // ensure that its read in
        // the group and variable index may vary across partitions
        GribCollection.GroupHcs g = gc.groups.get(partVar.getGroupno());      // WRONG LOOK  ??
        GribCollection.VariableIndex vindex = g.varIndex.get(partVar.getVarno());
        vindex.readRecords();
        return vindex;
      }  // LOOK ok to close ?? or is this defensive ??
    }

    @Override
    public String toStringComplete() {
      Formatter sb = new Formatter();
      sb.format("VariableIndexPartitioned%n");
      sb.format(" groupno=");
      for (GribCollectionProto.PartVar partVar : partList)
        sb.format("%d,", partVar.getGroupno());
      sb.format("%n varno=");
      for (GribCollectionProto.PartVar partVar : partList)
        sb.format("%d,", partVar.getVarno());
      sb.format("%n flags=");
      for (GribCollectionProto.PartVar partVar : partList)
        sb.format("%d,", partVar.getFlag());
      sb.format("%n");
      int count = 0;
      sb.format("  %s %4s %3s %3s %6s%n", "part", "N", "dups", "Miss", "density");
      for (GribCollectionProto.PartVar partVar : partList)  {
        sb.format("   %2d: %4d %3d %3d %6.2f%n", count++, partVar.getNrecords(), partVar.getNdups(),  partVar.getMissing(),  partVar.getDensity());
      }
      sb.format("%n");

      sb.format(super.toStringComplete());
      return sb.toString();
    }

    // doesnt work because not threadsafe
    public void cleanup() throws IOException {
      // TimePartition.this.cleanup(); LOOK!!
    }
  }

  // wrapper around a GribCollection
  public class Partition implements Comparable<Partition> {
    private final String name, directory;
    private String indexFilename;
    private long lastModified;
    public GribCollection gc;  // temporary storage while building - do not use

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
    }

    public GribCollection makeGribCollection(CollectionManager.Force force) throws IOException {
      GribCollection result = GribCollection.factory(isGrib1, dcm, force, logger); // LOOK caller must close
      indexFilename = result.getIndexFile().getPath();
      return result;
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected Map<String, Partition> partitionMap = new TreeMap<>();
  protected List<Partition> partitions = new ArrayList<>();
  protected final org.slf4j.Logger logger;

  protected PartitionCollection(String name, File directory, FeatureCollectionConfig.GribConfig config, boolean isGrib1, org.slf4j.Logger logger) {
    super(name, directory, config, isGrib1);
    this.logger = logger;
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

  public void addPartition(String name, String filename, long lastModified, String directory) {
    Partition partition = new Partition(name, filename, lastModified, directory);
    partitionMap.put(name, partition);
    partitions.add(partition);
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


  // read from index
  public VariableIndexPartitioned makeVariableIndexPartitioned(GribCollection.VariableIndex vi, List<GribCollectionProto.PartVar> parts) {
    VariableIndexPartitioned vip = new VariableIndexPartitioned(vi);
    vip.partList = parts;
    return vip;
  }

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

    /* for (GroupHcs g : groups) {
      for (VariableIndex v : g.varIndex) {
        VariableIndexPartitioned vp = (VariableIndexPartitioned) v;
        for (int i = 0; i < vp.flag.length; i++) {
          if (vp.flag[i] != 0) {
            f.format("  %s has missing (%d) on partition %d%n", v.id(), vp.flag[i], i);
          }
        }
      }
    } */

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
