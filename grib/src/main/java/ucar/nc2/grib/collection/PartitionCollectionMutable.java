/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.DateExtractor;
import thredds.inventory.MCollection;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.grib.GribIndexCache;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.cache.SmartArrayInt;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Collection of GribCollections or other PartitionCollections, partitioned by reference time.
 *
 * @author John
 * @since 12/7/13
 */
public class PartitionCollectionMutable extends GribCollectionMutable {

  static class PartitionForVariable2D {
    final int partno;
    final int groupno;
    final int varno; // , flag;     // what the hell is the flag used for ?
    GribCollectionMutable.VariableIndex vi;

    PartitionForVariable2D(int partno, int groupno, int varno) {
      this.partno = partno;
      this.groupno = groupno;
      this.varno = varno;
    }
  }

  public class VariableIndexPartitioned extends GribCollectionMutable.VariableIndex {
    final int nparts;
    SmartArrayInt partnoSA;
    SmartArrayInt groupnoSA;
    SmartArrayInt varnoSA;

    List<PartitionForVariable2D> partList; // used only when creating, then discarded in finish

    VariableIndexPartitioned(GroupGC g, VariableIndex other, int nparts) {
      super(g, other);
      this.nparts = nparts;
    }

    public void setPartitions(List<GribCollectionProto.PartitionVariable> pvList) {
      int[] partno = new int[nparts];
      int[] groupno = new int[nparts];
      int[] varno = new int[nparts];
      int count = 0;
      for (GribCollectionProto.PartitionVariable part : pvList) {
        partno[count] = part.getPartno();
        groupno[count] = part.getGroupno();
        varno[count] = part.getVarno();
        count++;
      }
      this.partnoSA =  new SmartArrayInt(partno);
      this.groupnoSA =  new SmartArrayInt(groupno);
      this.varnoSA =  new SmartArrayInt(varno);

      partList = null; // GC
    }

    public void finish() {
      if (partList == null) return;  // nothing to do
      if (partList.size() > nparts)  // might be smaller due to failed partition
        logger.warn("PartitionCollectionMutable partList.size() > nparts");

      int[] partno = new int[nparts];
      int[] groupno = new int[nparts];
      int[] varno = new int[nparts];
      int count = 0;
      for (PartitionForVariable2D part : partList) {
        partno[count] = part.partno;
        groupno[count] = part.groupno;
        varno[count] = part.varno;
        count++;
      }
      this.partnoSA =  new SmartArrayInt(partno);
      this.groupnoSA =  new SmartArrayInt(groupno);
      this.varnoSA =  new SmartArrayInt(varno);

      partList = null; // GC
    }

    // only used by PartitionBuilder, not PartitionBuilderFromIndex
    void addPartition(int partno, int groupno, int varno, int ndups, int nrecords, int nmissing,
        GribCollectionMutable.VariableIndex vi) {
      if (partList == null) partList = new ArrayList<>(nparts);
      partList.add(new PartitionForVariable2D(partno, groupno, varno));
      this.ndups += ndups;
      this.nrecords += nrecords;
      this.nmissing += nmissing;
    }

    @Override
    public String toStringComplete() {
      Formatter sb = new Formatter();
      sb.format("VariableIndexPartitioned%n");
      sb.format(" partno=");
      this.partnoSA.show(sb);
      sb.format("%n groupno=");
      this.groupnoSA.show(sb);
      sb.format("%n varno=");
      this.varnoSA.show(sb);
      sb.format("%n");
      int count = 0;
      sb.format("     %7s %3s %3s %6s %3s%n", "N", "dups", "Miss", "density", "partition");
      for (int i=0; i<nparts; i++) {
        int partWant = this.partnoSA.get(i);
        Partition part = partitions.get(partWant);
        sb.format("   %2d: %7d %s%n", count++, partWant, part.getFilename());
      }
      sb.format("%n");

      sb.format(super.toStringComplete());
      return sb.toString();
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////

  // wrapper around a GribCollection
  public class Partition implements Comparable<Partition> {
    final String name, directory;
    final String filename;
    long lastModified, fileSize;
    CalendarDate partitionDate;

    // temporary storage while building - do not use - must call getGribCollection()()
    // GribCollection gc;

    // constructor from ncx
    public Partition(String name, String filename, long lastModified, long fileSize, String directory, CalendarDate partitionDate) {
      this.name = name;
      this.filename = filename; // grib collection ncx
      this.lastModified = lastModified;
      this.fileSize = fileSize;
      this.directory = directory;
      this.partitionDate = partitionDate;
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

    private DateExtractor extractor;
    private DateExtractor getDateExtractor() {
      if (extractor == null)
         extractor = config.getDateExtractor();
      return extractor;
    }

    @Nullable
    String getIndexFilenameInCache() {
      File file = new File(directory, filename);
      File existingFile = GribIndexCache.getExistingFileOrCache(file.getPath());
      if (existingFile == null) {
        // try reletive to index file
        File parent = getIndexParentFile();
        if (parent == null) return null;
        existingFile = new File(parent, filename);
        if (!existingFile.exists()) return null;
      }
      return existingFile.getPath();
    }

    // acquire or construct GribCollection - caller must call gc.close() when done
    public GribCollectionImmutable getGribCollection() throws IOException {
      String path = getIndexFilenameInCache();
      if (path == null) {
        if (Grib.debugIndexOnly) {  // we are running in debug mode where we only have the indices, not the data files
          // tricky: substitute the current root
          File orgParentDir = new File(directory);
          File currentFile = new File(PartitionCollectionMutable.this.indexFilename);
          File currentParent = currentFile.getParentFile();
          File currentParentWithDir = new File(currentParent, orgParentDir.getName());
          File nestedIndex = isPartitionOfPartitions ? new File(currentParentWithDir, filename) : new File(currentParent, filename); // JMJ
          path = nestedIndex.getPath();
        } else {
          throw new FileNotFoundException("No index filename for partition= "+this.toString());
        }
      }

      // LOOK not cached
      return (GribCollectionImmutable) PartitionCollectionImmutable.partitionCollectionFactory.open(new DatasetUrl(null, path), -1, null, this);
    }

    // the children must already exist
    @Nullable
    public GribCollectionMutable makeGribCollection() {
      GribCollectionMutable result = GribCdmIndex.openMutableGCFromIndex(dcm.getIndexFilename(GribCdmIndex.NCX_SUFFIX), config, false, true, logger);
      if (result == null) {
        logger.error("Failed on openMutableGCFromIndex {}", dcm.getIndexFilename(GribCdmIndex.NCX_SUFFIX));
        return null;
      }
      lastModified = result.lastModified;
      fileSize = result.fileSize;
      if (result.masterRuntime != null)
        partitionDate = result.masterRuntime.getFirstDate();
      return result;
    }

    @Override
    public int compareTo(@Nonnull Partition o) {
      if (partitionDate != null && o.partitionDate != null)
        return partitionDate.compareTo(o.partitionDate);
      return name.compareTo(o.name);
    }

    @Override
    public String toString() {
      return "Partition{" +
              "dcm=" + dcm +
              ", name='" + name + '\'' +
              ", directory='" + directory + '\'' +
              ", filename='" + filename + '\'' +
              ", partitionDate='" + partitionDate + '\'' +
              ", lastModified='" + CalendarDate.of(lastModified) + '\'' +
              '}';
    }

    /////////////////////////////////////////////
    // only used during creation of index
    private MCollection dcm;

    // constructor from a MCollection object
    public Partition(MCollection dcm) {
      FeatureCollectionConfig config = (FeatureCollectionConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_CONFIG);
      if (config == null)
        logger.warn("Partition missing FeatureCollectionConfig {}", dcm);

      this.dcm = dcm;
      this.name = dcm.getCollectionName();
      this.lastModified = dcm.getLastModified();
      this.directory = StringUtil2.replace(dcm.getRoot(), '\\', "/");
      this.partitionDate = dcm.getPartitionDate();

      String indexFilename = StringUtil2.replace(dcm.getIndexFilename(GribCdmIndex.NCX_SUFFIX), '\\', "/");
      if (partitionDate == null) {
        partitionDate = getDateExtractor().getCalendarDateFromPath(indexFilename);  // LOOK dicey
      }

      // now remove the directory
      if (indexFilename.startsWith(directory)) {
        indexFilename = indexFilename.substring(directory.length());
        if (indexFilename.startsWith("/")) indexFilename = indexFilename.substring(1);
      }
      filename = indexFilename;
    }

  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected final org.slf4j.Logger logger;
  protected List<Partition> partitions;
  protected boolean isPartitionOfPartitions;
  int[] run2part;   // masterRuntime.length; which partition to use for masterRuntime i

  protected PartitionCollectionMutable(String name, File directory, FeatureCollectionConfig config, boolean isGrib1, org.slf4j.Logger logger) {
    super(name, directory, config, isGrib1);
    this.logger = logger;
    this.partitions = new ArrayList<>();
    this.datasets = new ArrayList<>();
  }

  /**
   * Use partition names as the file names
   */
  @Override
  public List<String> getFilenames() {
    List<String> result = new ArrayList<>();
    for (Partition p : getPartitions()) {
      result.add(p.getIndexFilenameInCache());
    }
    Collections.sort(result);
    return result;
  }

  Partition addPartition(String name, String filename, long lastModified, long length,
      String directory, CalendarDate partitionDate) {
    Partition partition = new Partition(name, filename, lastModified, length, directory, partitionDate);
    partitions.add(partition);
    return partition;
  }

  public void addPartition(MCollection dcm) {
    Partition partition = new Partition(dcm);
    try (GribCollectionMutable gc = partition.makeGribCollection()) {  // make sure we can open the collection
      if (gc == null)
        logger.warn("failed to open partition {} =skipping", dcm.getCollectionName());
      else
        partitions.add(partition);
    } catch (Exception e) {
      logger.warn("failed to open partition {} -skipping", dcm.getCollectionName(), e);
    }
  }

  void sortPartitions() {
    Collections.sort(partitions);
    partitions = Collections.unmodifiableList(partitions);
  }


  ///////////////////////////////////////////////////////////////////////////////////////////////////

  // construct - going to write index

  /**
   * Create a VariableIndexPartitioned, add it to the given group
   *
   * @param group  the new VariableIndexPartitioned is in this group
   * @param from   copy info from here
   * @param nparts size of partition list
   * @return a new VariableIndexPartitioned
   */
  VariableIndexPartitioned makeVariableIndexPartitioned(GroupGC group,
      GribCollectionMutable.VariableIndex from, int nparts) {
    VariableIndexPartitioned vip = new VariableIndexPartitioned(group, from, nparts);
    group.addVariable(vip);

    if (from instanceof VariableIndexPartitioned && !isPartitionOfPartitions) {    // LOOK dont really understand this
      VariableIndexPartitioned vipFrom = (VariableIndexPartitioned) from;
      assert vipFrom.partList == null; // // check if vipFrom has been finished
      for (int i=0; i<vipFrom.nparts; i++)
        vip.addPartition(vipFrom.partnoSA.get(i), vipFrom.groupnoSA.get(i), vipFrom.varnoSA.get(i), 0, 0, 0, vipFrom);
    }

    return vip;
  }

  public Iterable<Partition> getPartitions() {
    return partitions;
  }

  Partition getPartition(int idx) {
    return partitions.get(idx);
  }

  int getPartitionSize() {
     return partitions.size();
   }

  public void showIndex(Formatter f) {
    super.showIndex(f);

    int count = 0;
    f.format("isPartitionOfPartitions=%s%n", isPartitionOfPartitions);
    f.format("Partitions%n");
    for (Partition p :  getPartitions())
      f.format("%d:  %s%n", count++, p);
    f.format("%n");

    if (run2part == null) f.format("run2part null%n");
    else {
      f.format(" master runtime -> partition %n");
      for (int idx=0; idx<masterRuntime.getSize(); idx++) {
        int partno =  run2part[idx];
        Partition part = getPartition(partno);
        f.format(" %d:  %s -> part %3d %s%n", count, masterRuntime.getRuntimeDate(idx), partno,  part);
        count++;
      }
      f.format("%n");
    }
  }

}
