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
import thredds.inventory.DateExtractor;
import thredds.inventory.MCollection;
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

  //////////////////////////////////////////////////////////////////////

  static class PartitionForVariable2D {
    int partno, groupno, varno; // , flag;     // what the hell is the flag used for ?
    //public int ndups, nrecords, missing;  // optional debugging - remove ? or factor out ??
    //public float density;                 // optional


    PartitionForVariable2D(int partno, int groupno, int varno) {
      this.partno = partno;
      this.groupno = groupno;
      this.varno = varno;
    }
  }

  public class VariableIndexPartitioned extends GribCollectionMutable.VariableIndex {
    int nparts;
    SmartArrayInt partnoSA;
    SmartArrayInt groupnoSA;
    SmartArrayInt varnoSA;

    List<PartitionForVariable2D> partList; // used only when creating, then discarded in finish

    VariableIndexPartitioned(GroupGC g, VariableIndex other, int nparts) {
      super(g, other);
      this.nparts = nparts;
    }

    public void setPartitions(List<PartitionCollectionProto.PartitionVariable> pvList) {
      int[] partno = new int[nparts];
      int[] groupno = new int[nparts];
      int[] varno = new int[nparts];
      int count = 0;
      for (PartitionCollectionProto.PartitionVariable part : pvList) {
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
      if (partList.size() > nparts)
        System.out.println("PartitionCollectionMutable partList.size() > nparts");   // might be smaller due to failed partition

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
    public void addPartition(int partno, int groupno, int varno, int ndups, int nrecords, int nmissing, GribCollectionMutable.VariableIndex vi) {
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
      //sb.format("%n flags=");
      //for (PartitionForVariable2D partVar : partList)
      //  sb.format("%d,", partVar.flag);
      sb.format("%n");
      int count = 0;
      sb.format("     %7s %3s %3s %6s %3s%n", "N", "dups", "Miss", "density", "partition");
      // int totalN = 0, totalDups = 0, totalMiss = 0;
      for (int i=0; i<nparts; i++) {
        int partWant = this.partnoSA.get(i);
        Partition part = partitions.get(partWant);
        sb.format("   %2d: %7d %s%n", count++, partWant, part.getFilename());
        //sb.format("   %2d: %7d %3d %3d   %6.2f   %d %s%n", count++, partVar.nrecords, partVar.ndups, partVar.missing, partVar.density, partVar.partno, part.getFilename());
        //totalN += partVar.nrecords;
        //totalDups += partVar.ndups;
        //totalMiss += partVar.missing;
      }
      //sb.format("total: %4d %3d %3d %n", totalN, totalDups, totalMiss);
      sb.format("%n");

      sb.format(super.toStringComplete());
      return sb.toString();
    }


    /* public void show(Formatter f) {
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
        for (int i=0; i<time2runtime.getN(); i++) {
          int idx = time2runtime.get(i);
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
    } */

  }

  //////////////////////////////////////////////////////////////////////////////////////////

  // wrapper around a GribCollection
  public class Partition implements Comparable<Partition> {
    final String name, directory;
    final String filename;
    long lastModified, fileSize;
    CalendarDate partitionDate;
    private boolean isBad;

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

    private DateExtractor extractor;
    private DateExtractor getDateExtractor() {
      if (extractor == null)
         extractor = config.getDateExtractor();
      return extractor;
    }


    public String getIndexFilenameInCache() {
      File file = new File(directory, filename);
      File existingFile = GribIndexCache.getExistingFileOrCache(file.getPath());
      if (existingFile == null) {
        // try reletive to index file
        File parent = getIndexParentFile();
        if (parent == null) return null;
        existingFile = new File(parent, filename);
        //System.out.printf("try reletive file = %s%n", existingFile);
        if (!existingFile.exists()) return null;
      }
      return existingFile.getPath();
    }

    // acquire or construct GribCollection - caller must call gc.close() when done
    public GribCollectionImmutable getGribCollection() throws IOException {
      String path = getIndexFilenameInCache();
      if (path == null) {
        if (GribIosp.debugIndexOnly) {  // we are running in debug mode where we only have the indices, not the data files
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
      return (GribCollectionImmutable) PartitionCollectionImmutable.partitionCollectionFactory.open(path, -1, null, this);
    }

    // the children must already exist
    public GribCollectionMutable makeGribCollection() throws IOException {
      GribCollectionMutable result = GribCdmIndex.openMutableGCFromIndex(dcm.getIndexFilename(), config, false, true, logger);
      if (result == null) {
        logger.error("Failed on openMutableGCFromIndex {}", dcm.getIndexFilename());
        return null;
      }
      lastModified = result.lastModified;
      fileSize = result.fileSize;
      if (result.masterRuntime != null)
        partitionDate = result.masterRuntime.getFirstDate();
      return result;
    }

    @Override
    public int compareTo(Partition o) {
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
              ", isBad=" + isBad +
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

      String indexFilename = StringUtil2.replace(dcm.getIndexFilename(), '\\', "/");
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
      if (p.isBad()) continue;
      result.add(p.getIndexFilenameInCache());
    }
    return result;
  }

  public Partition addPartition(String name, String filename, long lastModified, long length, String directory, CalendarDate partitionDate) {
    Partition partition = new Partition(name, filename, lastModified, length, directory, partitionDate);
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
   * Create a VariableIndexPartitioned, add it to the given group
   *
   * @param group  the new VariableIndexPartitioned is in this group
   * @param from   copy info from here
   * @param nparts size of partition list
   * @return a new VariableIndexPartitioned
   */
  public VariableIndexPartitioned makeVariableIndexPartitioned(GroupGC group, GribCollectionMutable.VariableIndex from, int nparts) {
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

  public Partition getPartition(int idx) {
    return partitions.get(idx);
  }

  public int getPartitionSize() {
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
