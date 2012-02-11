/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
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

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionManager;
import thredds.inventory.TimePartitionCollection;
import ucar.nc2.grib.grib1.Grib1TimePartitionBuilder;
import ucar.nc2.grib.grib2.Grib2TimePartitionBuilder;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheable;
import ucar.nc2.util.cache.FileFactory;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A collection of GribCollection objects which are Time Partitioned.
 * A TimePartition is the collection; a TimePartition.Partition represents one of the GribCollection.
 *
 * @author caron
 * @since 4/17/11
 */
public abstract class TimePartition extends GribCollection {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TimePartition.class);

  //////////////////////////////////////////////////////////
  // object cache for index files - these are opened only as GribCollection
  private static FileCache partitionCache;

  static public void initPartitionCache(int minElementsInMemory, int maxElementsInMemory, int period) {
    partitionCache = new ucar.nc2.util.cache.FileCache("GribCollectionPartitionCache ", minElementsInMemory, maxElementsInMemory, -1, period);
  }

  static public FileCache getPartitionCache() {
    return partitionCache;
  }

  static private final ucar.nc2.util.cache.FileFactory collectionFactory = new FileFactory() {
    public FileCacheable open(String location, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {
      File f = new File(location);
      RandomAccessFile raf = new RandomAccessFile(location, "r");
      Partition p = (Partition) iospMessage;
      return GribCollection.createFromIndex(false, p.getName(), f.getParentFile(), raf, p.getConfig()); // LOOK not sure what the parent directory is for
    }
  };

  static public void disableNetcdfFileCache() {
    if (null != partitionCache) partitionCache.disable();
    partitionCache = null;
  }

  ///////////////////////////////////////////////////////////////////////

  static public boolean update(boolean isGrib1, TimePartitionCollection tpc, Formatter f) throws IOException {
    if (isGrib1) return Grib1TimePartitionBuilder.update(tpc, f);
    return Grib2TimePartitionBuilder.update(tpc, f);
  }

  static public TimePartition factory(boolean isGrib1, TimePartitionCollection tpc, CollectionManager.Force force, Formatter f) throws IOException {
    if (isGrib1) return Grib1TimePartitionBuilder.factory(tpc, force, f);
    return Grib2TimePartitionBuilder.factory(tpc, force, f);
  }

  // wrapper around a GribCollection
  public class Partition implements Comparable<Partition> {
    private GribCollection gribCollection;
    private String name, indexFilename;

    // constructor from ncx
    public Partition(String name, String indexFilename) {
      this.name = name;
      this.indexFilename = indexFilename; // grib collection ncx
    }

    public String getName() {
      return name;
    }

    public String getIndexFilename() {
      return indexFilename;
    }

    public FeatureCollectionConfig.GribConfig getConfig() {
      return gribConfig;
    }

    // null if it came from the index
    public CollectionManager getDcm() {
      return dcm;
    }

    // construct GribCollection - caller must call gc.close() or tp.cleanup()
    public GribCollection getGribCollection() throws IOException {
      if (gribCollection == null) {
        if (partitionCache != null) {
          gribCollection = (GribCollection) partitionCache.acquire(collectionFactory, indexFilename, indexFilename, -1, null, this);
        } else {
          gribCollection = (GribCollection) collectionFactory.open(indexFilename, -1, null, this);
        }
      }
      return gribCollection;
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
              ", filename='" + indexFilename + '\'' +
              '}';
    }

    /////////////////////////////////////////////
    // only used during creation of index
    private CollectionManager dcm;

    // constructor from a TimePartition object
    public Partition(CollectionManager dcm) {
      this.dcm = dcm;
      this.name = dcm.getCollectionName();
    }

    public GribCollection makeGribCollection(Formatter f) throws IOException {
      if (gribCollection == null) {
        gribCollection = GribCollection.factory(isGrib1, dcm, CollectionManager.Force.test, f);  // LOOK why test ??
        indexFilename = gribCollection.getIndexFile().getPath();
      }
      return gribCollection;
    }

  }

  public class VariableIndexPartitioned extends GribCollection.VariableIndex {
    public int[] groupno, varno;

    public VariableIndexPartitioned(GribCollection.GroupHcs g, int discipline, int category, int parameter, int levelType, boolean isLayer,
                                    int intvType, String intvName, int ensDerivedType, int probType, String probabilityName,
                                    int genProcessType,
                                    int cdmHash, int timeIdx, int vertIdx, int ensIdx, long recordsPos, int recordsLen) {

      super(g, 0, discipline, category, parameter, levelType, isLayer, intvType, intvName, ensDerivedType, probType, probabilityName,
              genProcessType, cdmHash, timeIdx, vertIdx, ensIdx, recordsPos, recordsLen);
    }

    public void setPartitionIndex(int partno, int groupIdx, int varIdx) {
      groupno[partno] = groupIdx;
      varno[partno] = varIdx;
    }

    public GribCollection.VariableIndex getVindex(int partno) throws IOException {
      // at this point, we need to instantiate the Partition and the vindex.records
      Partition p = getPartitions().get(partno);
      GribCollection gc = p.getGribCollection(); // ensure that its read in

      // the group and variable index may vary across partitions
      GribCollection.GroupHcs g = gc.groups.get(groupno[partno]);
      GribCollection.VariableIndex vindex = g.varIndex.get(varno[partno]);
      vindex.readRecords();
      return vindex;
    }

    @Override
    public String toStringComplete() {
      final StringBuilder sb = new StringBuilder();
      sb.append("VariableIndexPartitioned");
      sb.append("{groupno=").append(groupno == null ? "null" : "");
      for (int i = 0; groupno != null && i < groupno.length; ++i)
        sb.append(i == 0 ? "" : ", ").append(groupno[i]);
      sb.append(", varno=").append(varno == null ? "null" : "");
      for (int i = 0; varno != null && i < varno.length; ++i)
        sb.append(i == 0 ? "" : ", ").append(varno[i]);
      sb.append('}');
      sb.append(super.toStringComplete());
      return sb.toString();
    }

    // doesnt work because not threadsafe
    public void cleanup() throws IOException {
      TimePartition.this.cleanup();
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected Map<String, Partition> partitionMap;
  protected List<Partition> partitions;

  protected TimePartition(String name, File directory, FeatureCollectionConfig.GribConfig dcm, boolean isGrib1) {
    super(name, directory, dcm, isGrib1);
  }

  @Override
  public List<String> getFilenames() {
    if (filenames == null || filenames.size() == 0) {
      List<Partition> parts = getPartitions();
      filenames = new ArrayList<String>(parts.size());
      for (Partition p : parts) filenames.add(p.indexFilename);
    }
    return filenames;
  }

  public void addPartition(String name, String filename) {
    if (partitionMap == null) partitionMap = new TreeMap<String, TimePartition.Partition>();
    partitionMap.put(name, new Partition(name, filename));
  }

  public void addPartition(CollectionManager dcm) {
    if (partitionMap == null) partitionMap = new TreeMap<String, TimePartition.Partition>();
    partitionMap.put(dcm.getCollectionName(), new Partition(dcm));
  }

  public Partition getPartitionByName(String name) {
    return partitionMap.get(name);
  }

  public void cleanup() throws IOException {
    if (partitions == null) return;
    for (TimePartition.Partition p : partitions)
      if (p.gribCollection != null)
        p.gribCollection.close();
  }

  @Override
  public GribCollection.VariableIndex makeVariableIndex(GroupHcs group, int tableVersion,
                                                        int discipline, int category, int parameter, int levelType, boolean isLayer,
                                                        int intvType, String intvName, int ensDerivedType, int probType, String probabilityName,
                                                        int genProcessType,
                                                        int cdmHash, int timeIdx, int vertIdx, int ensIdx, long recordsPos, int recordsLen) {
    throw new UnsupportedOperationException();
  }

  public GribCollection.VariableIndex makeVariableIndex(GroupHcs group, int tableVersion,
                                                 int discipline, int category, int parameter, int levelType, boolean isLayer, int intvType,
                                                 String intvName, int ensDerivedType, int probType, String probabilityName,
                                                 int genProcessType,
                                                 int cdmHash, int timeIdx, int vertIdx, int ensIdx, long recordsPos, int recordsLen,
                                                 List<Integer> groupnoList, List<Integer> varnoList) {

    VariableIndexPartitioned vip = new VariableIndexPartitioned(group, discipline, category, parameter, levelType, isLayer, intvType,
            intvName, ensDerivedType, probType, probabilityName, genProcessType, cdmHash, timeIdx, vertIdx, ensIdx, recordsPos, recordsLen);

    int nparts = varnoList.size();
    vip.groupno = new int[nparts];
    vip.varno = new int[nparts];
    for (int i = 0; i < nparts; i++) {
      vip.groupno[i] = groupnoList.get(i);
      vip.varno[i] = varnoList.get(i);
    }
    return vip;
  }

  public VariableIndexPartitioned makeVariableIndexPartitioned(GribCollection.VariableIndex vi, int nparts) {
    VariableIndexPartitioned vip = new VariableIndexPartitioned(vi.group, vi.discipline, vi.category, vi.parameter, vi.levelType,
            vi.isLayer, vi.intvType, vi.intvName, vi.ensDerivedType, vi.probType, vi.probabilityName,
            vi.genProcessType, vi.cdmHash, vi.timeIdx, vi.vertIdx, vi.ensIdx, vi.recordsPos, vi.recordsLen);

    vip.groupno = new int[nparts];
    vip.varno = new int[nparts];
    for (int i = 0; i < nparts; i++) {
      vip.groupno[i] = -1;
      vip.varno[i] = -1;
    }
    return vip;
  }

  public List<Partition> getPartitions() {
    if (partitions == null) {
      List<Partition> c = new ArrayList<Partition>(partitionMap.values());
      Collections.sort(c);
      partitions = c;
    }
    return partitions;
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

    super.showIndex(f);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff for Iosp

  public RandomAccessFile getRaf(int partno, int fileno) throws IOException {
    Partition part = getPartitions().get(partno);
    GribCollection gc =  part.getGribCollection();
    return gc.getDataRaf(fileno);
  }

  public void close() throws java.io.IOException {
    if (fileCache != null) {
      fileCache.release(this);
    } else if (indexRaf != null) {
      cleanup();
      super.close();
    }
  }

}
