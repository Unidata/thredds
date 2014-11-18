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
import ucar.coord.*;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.GribIndex;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Misc;
import ucar.nc2.util.cache.FileCacheable;
import ucar.nc2.util.cache.FileFactory;
import ucar.nc2.util.cache.SmartArrayInt;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

/**
 * An Immutable PartitionCollection
 *
 * @author caron
 * @since 11/10/2014
 */
public abstract class PartitionCollectionImmutable extends GribCollectionImmutable {
  static private final Logger logger = LoggerFactory.getLogger(PartitionCollectionImmutable.class);
  static public int countPC;   // debug

  static final ucar.nc2.util.cache.FileFactory partitionCollectionFactory = new FileFactory() {
    public FileCacheable open(String location, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {

      try (RandomAccessFile raf = RandomAccessFile.acquire(location)) {
        Partition p = (Partition) iospMessage;
        return GribCdmIndex.openGribCollectionFromIndexFile(raf, p.getConfig(), true, p.getLogger()); // do we know its a partition ?

      } catch (Throwable t) {
        RandomAccessFile.eject(location);
        throw t;
      }
    }
  };

  ///////////////////////////////////////////////
  private final List<Partition> partitions;
  private final boolean isPartitionOfPartitions;
  private final int[] run2part;   // masterRuntime.length; which partition to use for masterRuntime i

  PartitionCollectionImmutable( PartitionCollectionMutable pc) {
    super(pc);

    List<PartitionCollectionMutable.Partition> pcParts = pc.partitions;
    List<Partition> work = new ArrayList<>(pcParts.size());
    for (PartitionCollectionMutable.Partition pcPart : pcParts) {
      work.add( new Partition(pcPart));
    }

    this.partitions = Collections.unmodifiableList(work);
    this.isPartitionOfPartitions = pc.isPartitionOfPartitions;
    this.run2part = pc.run2part;
  }

    // return open GC
  public GribCollectionImmutable getLatestGribCollection(List<String> paths) throws IOException {
    Partition last = partitions.get(partitions.size()-1);
    paths.add(last.getName());

    GribCollectionImmutable gc = last.getGribCollection();
    if (gc instanceof PartitionCollectionImmutable) {
      try {
        PartitionCollectionImmutable pc = (PartitionCollectionImmutable) gc;
        return pc.getLatestGribCollection(paths);
      } finally {
        gc.close();  // make sure its closed even on exception
      }
    } else {
      return gc;
    }

  }

  protected VariableIndex makeVariableIndex(GroupGC group, GribCollectionMutable.VariableIndex mutableVar) {
    return new VariableIndexPartitioned(group, mutableVar);
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

  public boolean isPartitionOfPartitions() {
    return isPartitionOfPartitions;
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

  public VariableIndexPartitioned getVariable2DByHash(GribHorizCoordSystem hcs, int cdmHash) {
    Dataset ds2d = getDataset2D();
    if (ds2d == null) return null;
    for (GroupGC groupHcs : ds2d.getGroups())
      if (groupHcs.horizCoordSys == hcs)
        return (VariableIndexPartitioned) groupHcs.findVariableByHash(cdmHash);
    return null;
  }

    //////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff for Iosp

  public RandomAccessFile getRaf(int partno, int fileno) throws IOException {
    Partition part = getPartition(partno);
    try (GribCollectionImmutable gc = part.getGribCollection()) {  // LOOK this closes the GC.ncx2
      return gc.getDataRaf(fileno);
    }
  }

  // debugging
  public String getFilename(int partno, int fileno) throws IOException {
    Partition part = getPartition(partno);
    try (GribCollectionImmutable gc = part.getGribCollection()) {  // LOOK this closes the GC.ncx2
      return gc.getFilename(fileno);
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

    // wrapper around a GribCollection
  @Immutable
  public class Partition implements Comparable<Partition> {
      private final String name, partDirectory;
      private final String filename;
      private final long lastModified;

      // constructor from ncx
      public Partition(PartitionCollectionMutable.Partition pcPart) {
        this.name = pcPart.name;
        this.filename = pcPart.filename; // grib collection ncx
        this.lastModified = pcPart.lastModified;
        this.partDirectory = pcPart.directory;
      }

      public boolean isPartitionOfPartitions() {
        return isPartitionOfPartitions;
      }

      public String getName() {
        return name;
      }

      public String getFilename() {
        return filename;
      }

      public String getDirectory() {
        return partDirectory;
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

      private String getIndexFilenameInCache() throws FileNotFoundException {
        File file = new File(partDirectory, filename);
        File existingFile = GribIndex.getExistingFileOrCache(file.getPath());

        if (existingFile == null) {
          existingFile = new File(directory, filename); // try the Collection directory
          if (!existingFile.exists()) {
            throw new FileNotFoundException("No index filename for partition= " + this.toString());
          }
        }

        /* if (existingFile == null) {
          if (GribIosp.debugIndexOnly) {  // we are running in debug mode where we only have the indices, not the data files
            // tricky: substitute the current root
            File orgParentDir = new File(directory);
            File currentFile = new File(PartitionCollectionImmutable.this.indexFilename);
            File currentParent = currentFile.getParentFile();
            File currentParentWithDir = new File(currentParent, orgParentDir.getName());
            File nestedIndex = isPartitionOfPartitions ? new File(currentParentWithDir, filename) : new File(currentParent, filename); // JMJ
            path = nestedIndex.getPath();

          } else {
            throw new FileNotFoundException("No index filename for partition= " + this.toString());
          } */

        return existingFile.getPath();
      }

      // acquire or construct GribCollection - caller must call gc.close() when done
      public GribCollectionImmutable getGribCollection() throws IOException {
        GribCollectionImmutable result;
        String path = getIndexFilenameInCache();

        if (GribCdmIndex.gribCollectionCache != null) {
                    // FileFactory factory, Object hashKey, String location, int buffer_size, CancelTask cancelTask, Object spiObject
          result = (GribCollectionImmutable) GribCdmIndex.gribCollectionCache.acquire(partitionCollectionFactory, path, path, -1, null, this);
        } else {
                    // String location, int buffer_size, CancelTask cancelTask, Object iospMessage
          result = (GribCollectionImmutable) partitionCollectionFactory.open(path, -1, null, this);
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
                ", name='" + name + '\'' +
                ", directory='" + directory + '\'' +
                ", filename='" + filename + '\'' +
                ", lastModified='" + CalendarDate.of(lastModified) + '\'' +
                '}';
      }
    }

  @Immutable
  public class VariableIndexPartitioned extends GribCollectionImmutable.VariableIndex {
    final int nparts;
    final SmartArrayInt partnoSA;
    final SmartArrayInt groupnoSA;
    final SmartArrayInt varnoSA;

        // partition only
    final SmartArrayInt time2runtime; // oneD only: for each timeIndex, which runtime coordinate does it use? 1-based so 0 = missing;
                             // index into the corresponding 2D variable's runtime coordinate

    VariableIndexPartitioned(GribCollectionImmutable.GroupGC g, GribCollectionMutable.VariableIndex other) {
      super(g, other);

      PartitionCollectionMutable.VariableIndexPartitioned pother  = (PartitionCollectionMutable.VariableIndexPartitioned) other;
      this.nparts = pother.nparts;
      this.time2runtime =  pother.time2runtime;

      this.partnoSA =  pother.partnoSA;
      this.groupnoSA =  pother.groupnoSA;
      this.varnoSA =  pother.varnoSA;
    }

    public int getNparts() {
      return nparts;
    }

    /* @Override
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
    }  */


    public void show(Formatter f) {

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

      if (GribIosp.debugRead) System.out.printf("%nPartitionCollection.getDataRecord index wanted = (%s) on %s isTwod=%s%n", Misc.showInts(indexWanted), indexFilename, group.isCanonicalGroup);

      // find the runtime index
      int firstIndex = indexWanted[0];
      int runIdx = group.isCanonicalGroup ? firstIndex : time2runtime.get(firstIndex) - 1; // time2runtime is for oneD
      if (GribIosp.debugRead && !group.isCanonicalGroup) System.out.printf("  firstIndex = %d runIdx=%d %n", firstIndex, runIdx);
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

      // find the 2D vi in that partition
      GribCollectionImmutable.VariableIndex compVindex2D = getVindex2D(partno); // the 2D component variable in the partno partition
      if (compVindex2D == null) return null; // missing
      if (GribIosp.debugRead) System.out.printf("  compVindex2D = %s%n", compVindex2D.toStringFrom());

      if (isPartitionOfPartitions) {
        VariableIndexPartitioned compVindex2Dp = (VariableIndexPartitioned) compVindex2D;
        return getDataRecordPofP(indexWanted, compVindex2Dp);
      }

      // translate to coordinates in vindex
      int[] sourceIndex = group.isCanonicalGroup ? translateIndex2D(indexWanted, compVindex2D) : translateIndex1D(indexWanted, compVindex2D);
      if (sourceIndex == null) return null; // missing
      GribCollectionImmutable.Record record = compVindex2D.getRecordAt(sourceIndex);
      if (record == null) {
        return null;
        // compVindex2D.getSparseArray().getContent(sourceIndex); // debug
      }

      if (GribIosp.debugRead) System.out.printf("  result success: partno=%d fileno=%d %n", partno, record.fileno);
      return new DataRecord(PartitionCollectionImmutable.this, partno, compVindex2D.group.getGdsHorizCoordSys(), record.fileno, record.pos, record.bmsPos, record.scanMode);
    }

    /**
     * find DataRecord in a PofP
     * @param indexWanted index into this PoP
     * @param compVindex2Dp 2D variable from the desired partition; may be PofP or PofGC
     * @return desired record to be read, from the GC
     * @throws IOException
     */
    private DataRecord getDataRecordPofP(int[] indexWanted, VariableIndexPartitioned compVindex2Dp) throws IOException {
      if (group.isCanonicalGroup) {
        // corresponding index into compVindex2Dp
        int[] indexWantedP = translateIndex2D(indexWanted, compVindex2Dp);
        if (GribIosp.debugRead) System.out.printf("  (2D) getDataRecordPofP= %s %n", Misc.showInts(indexWantedP));
        return compVindex2Dp.getDataRecord(indexWantedP);
      } else {
        int[] indexWantedP = translateIndex1D(indexWanted, compVindex2Dp);
        if (GribIosp.debugRead) System.out.printf("  (1D) getDataRecordPofP= %s %n", Misc.showInts(indexWantedP));
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
    private GribCollectionImmutable.VariableIndex getVindex2D(int partno) throws IOException {
      // at this point, we need to instantiate the Partition and the vindex.records
      // the 2D vip for this variable
      VariableIndexPartitioned vip =  isPartitionOfPartitions ?
        (VariableIndexPartitioned) getVariable2DByHash(group.horizCoordSys, info.cdmHash) :
        this;

      int idx = vip.partnoSA.findIdx(partno);
      if (idx < 0 ||  idx >= vip.nparts) {
        if (GribIosp.debugRead) System.out.printf("  cant find partition=%d in vip=%s%n", partno, vip);
        return null;
      }

      Partition p = getPartition(partno);
      try (GribCollectionImmutable gc = p.getGribCollection()) { // ensure that its read in try-with
        GribCollectionImmutable.Dataset ds = gc.getDatasetCanonical(); // always references the twoD or GC dataset
        // the group and variable index may vary across partitions
        GribCollectionImmutable.GroupGC g = ds.groups.get(vip.groupnoSA.get(idx));
        GribCollectionImmutable.VariableIndex vindex = g.variList.get(vip.varnoSA.get(idx));
        vindex.readRecords();
        return vindex;
      }  // LOOK opening the file here, and then again to read the data. partition cache helps i guess but we could do better i think.
    }

    /*

     */


    /**
     * translate index in VariableIndexPartitioned to corresponding index in one of its component VariableIndex
     * by matching coordinate values. The 1D (Best) case.
     *
     * @param wholeIndex index in VariableIndexPartitioned
     * @param compVindex2D     component 2D VariableIndex
     * @return corresponding index in compVindex2D, or null if missing
     */
    private int[] translateIndex1D(int[] wholeIndex, GribCollectionImmutable.VariableIndex compVindex2D) {
      int[] result = new int[wholeIndex.length + 1];

      // figure out the runtime
      int timeIdx = wholeIndex[0];
      int runtimeIdxWhole = time2runtime.get(timeIdx) - 1;  // 1-based; runtime Index into master runtime
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
          if (resultIdx < 0)
            resultIdx = compCoord2D.matchTimeCoordinate(runtimeIdxPart, wholeVal, wholeCoord1Dtime.getRefDate()); // debug

        } else {
          resultIdx = matchCoordinate(wholeCoord1D, idx, compCoord);
          if (resultIdx < 0)
            resultIdx = matchCoordinate(wholeCoord1D, idx, compCoord); // debug
        }
        if (resultIdx < 0) {
          logger.info("Couldnt match coordinates ({}) for variable {}", Misc.showInts(wholeIndex), compVindex2D.toStringFrom());
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
    private int[] translateIndex2D(int[] wholeIndex, GribCollectionImmutable.VariableIndex compVindex2D) {
      int[] result = new int[wholeIndex.length];
      int countDim = 0;

      // special case for 2D time
      CoordinateTime2D compTime2D = (CoordinateTime2D) compVindex2D.getCoordinate(Coordinate.Type.time2D);
      if (compTime2D != null) {
        CoordinateTime2D time2D = (CoordinateTime2D) getCoordinate(Coordinate.Type.time2D);
        if (wholeIndex.length < 2)
          System.out.println("HEY");
        CoordinateTime2D.Time2D want = time2D.getOrgValue(wholeIndex[0], wholeIndex[1], GribIosp.debugRead);
        if (GribIosp.debugRead) System.out.printf("  translateIndex2D[runIdx=%d, timeIdx=%d] in componentVar coords = (%s,%s) %n",
                wholeIndex[0], wholeIndex[1], (want == null) ? "null" : want.getRun(), want);
        if (want == null) return null;
        if (!compTime2D.getIndex(want, result)) // sets the first 2 indices - run and time
          return null; // missing data
        countDim = 2;
      }

      // the remaining dimensions, if any
      while (countDim < wholeIndex.length) {
        int idx = wholeIndex[countDim];
        int resultIdx = matchCoordinate(getCoordinate(countDim), idx, compVindex2D.getCoordinate(countDim));
        if (GribIosp.debugRead) System.out.printf("  translateIndex2D[idx=%d] resultIdx= %d %n", idx, resultIdx);
        if (resultIdx < 0) {
          // matchCoordinate(getCoordinate(countDim), idx, compVindex2D.getCoordinate(countDim)); // debug
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

  @Immutable
  class DataRecord extends GribIosp.DataRecord {
    final PartitionCollectionImmutable usePartition;
    final int partno; // partition index in usePartition

    DataRecord(PartitionCollectionImmutable usePartition, int partno, GdsHorizCoordSys hcs, int fileno, long drsPos, long bmsPos, int scanMode) {
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

        //debugging
    public void show() throws IOException {
      String dataFilename = usePartition.getFilename(partno, fileno);
      System.out.printf(" **DataReader partno=%d fileno=%d filename=%s datapos=%d%n", partno, fileno, dataFilename, dataPos);
    }

  }

}
