/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.grib.coord.Coordinate;
import ucar.nc2.grib.coord.CoordinateRuntime;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.nc2.grib.coord.CoordinateTimeAbstract;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.GribIndexCache;
import ucar.nc2.grib.coord.TimeCoordIntvValue;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Misc;
import ucar.nc2.util.cache.FileCacheable;
import ucar.nc2.util.cache.FileFactory;
import ucar.nc2.util.cache.SmartArrayInt;
import ucar.unidata.io.RandomAccessFile;

import javax.annotation.concurrent.Immutable;
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
  private static final Logger logger = LoggerFactory.getLogger(PartitionCollectionImmutable.class);
  public static int countPC;   // debug

  static final ucar.nc2.util.cache.FileFactory partitionCollectionFactory = new FileFactory() {
    public FileCacheable open(DatasetUrl durl, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {
      try (RandomAccessFile raf = RandomAccessFile.acquire(durl.trueurl)) {
        Partition p = (Partition) iospMessage;
        return GribCdmIndex.openGribCollectionFromIndexFile(raf, p.getConfig(), p.getLogger()); // do we know its a partition ?

      } catch (Throwable t) {
        RandomAccessFile.eject(durl.trueurl);
        throw t;
      }
    }
  };

  ///////////////////////////////////////////////
  private final List<Partition> partitions;
  private final boolean isPartitionOfPartitions;
  private final int[] run2part;   // masterRuntime.length; which partition to use for masterRuntime i

  PartitionCollectionImmutable(PartitionCollectionMutable pc) {
    super(pc);

    List<PartitionCollectionMutable.Partition> pcParts = pc.partitions;
    List<Partition> work = new ArrayList<>(pcParts.size());
    for (PartitionCollectionMutable.Partition pcPart : pcParts) {
      work.add(new Partition(pcPart));
    }

    this.partitions = Collections.unmodifiableList(work);
    this.isPartitionOfPartitions = pc.isPartitionOfPartitions;
    this.run2part = pc.run2part;
  }

  // return open GC
  public GribCollectionImmutable getLatestGribCollection(List<String> paths) throws IOException {
    Partition last = partitions.get(partitions.size() - 1);
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

  private Partition getPartition(int idx) {
    return partitions.get(idx);
  }

  @Nullable
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
    if (!this.config.getSortFilesAscending()) {
      Collections.reverse(c);
    }
    return c;
  }

  @Nullable
  private VariableIndexPartitioned getVariable2DByHash(GribHorizCoordSystem hcs, VariableIndex vi) {
    Dataset ds2d = getDatasetCanonical();
    if (ds2d == null) return null;
    for (GroupGC groupHcs : ds2d.getGroups())
      if (groupHcs.getGdsHash().equals(hcs.getGdsHash()))
        return (VariableIndexPartitioned) groupHcs.findVariableByHash(vi);
    return null;
  }

  @Override
  public void showIndex(Formatter f) {
    super.showIndex(f);
    f.format("%nPartition isPartitionOfPartitions = %s%n", isPartitionOfPartitions);
    for (Partition p : partitions) {
      f.format("  %s%n", p);
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff for Iosp

  RandomAccessFile getRaf(int partno, int fileno) throws IOException {
    Partition part = getPartition(partno);
    try (GribCollectionImmutable gc = part.getGribCollection()) {
      return gc.getDataRaf(fileno);
    }
  }

  // debugging
  public String getFilename(int partno, int fileno) throws IOException {
    Partition part = getPartition(partno);
    try (GribCollectionImmutable gc = part.getGribCollection()) {
      return gc.getFilename(fileno);
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  // wrapper around a GribCollection
  @Immutable
  public class Partition implements Comparable<Partition> {
    private final String name; // partDirectory;
    private final String filename;
    private final long lastModified, fileSize;
    private final CalendarDate partitionDate;

    // constructor from ncx
    public Partition(PartitionCollectionMutable.Partition pcPart) {
      this.name = pcPart.name;
      this.filename = pcPart.filename; // grib collection ncx
      this.lastModified = pcPart.lastModified;
      this.fileSize = pcPart.fileSize;
      //this.partDirectory = pcPart.directory;
      this.partitionDate = pcPart.partitionDate;
    }

    public String getName() {
      return name;
    }

    public String getFilename() {
      return filename;
    }

      /* public String getDirectory() {
        return partDirectory;
      } */

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

    public String getIndexFilenameInCache() throws FileNotFoundException {
      File file = new File(directory, filename);
      File existingFile = GribIndexCache.getExistingFileOrCache(file.getPath());

      if (existingFile == null) {
        throw new FileNotFoundException("No index filename for partition= " + this.toString() + " looking for " + file.getPath());
      }

        /* if (existingFile == null) {
          if (Grib.debugIndexOnly) {  // we are running in debug mode where we only have the indices, not the data files
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
      String path = getIndexFilenameInCache();
      return GribCdmIndex.acquireGribCollection(partitionCollectionFactory, path, path, -1, null, this);
    }

    @Override
    public int compareTo(@Nonnull Partition o) {
      return name.compareTo(o.name);
    }

    @Override
    public String toString() {
      return "Partition{ " + name + '\'' +
              //        ", directory='" + directory + '\'' +
              ", filename='" + filename + '\'' +
              ", partitionDate='" + partitionDate + '\'' +
              ", lastModified='" + CalendarDate.of(lastModified) + '\'' +
              ", fileSize='" + fileSize + '\'' +
              '}';
    }

  }

  @Immutable
  public class VariableIndexPartitioned extends GribCollectionImmutable.VariableIndex {
    final int nparts;
    final SmartArrayInt partnoSA;  // conceptually int[nparts] : index into PartitionCollectionImmutable.partitions[] -> Partition
    final SmartArrayInt groupnoSA; // once you have the partition, which group in that partition's dataset? Partition.Dataset.Group[] -> Group
    final SmartArrayInt varnoSA;   // once you have the group, which variable? Group[] -> Variable

    // partition only
    // final SmartArrayInt time2runtime; // oneD only: for each timeIndex, which runtime coordinate does it use? 1-based so 0 = missing;
    // index into the corresponding 2D variable's runtime coordinate

    VariableIndexPartitioned(GribCollectionImmutable.GroupGC g, GribCollectionMutable.VariableIndex other) {
      super(g, other);

      PartitionCollectionMutable.VariableIndexPartitioned pother = (PartitionCollectionMutable.VariableIndexPartitioned) other;
      this.nparts = pother.nparts;
      // this.time2runtime =  pother.time2runtime;

      this.partnoSA = pother.partnoSA;
      this.groupnoSA = pother.groupnoSA;
      this.varnoSA = pother.varnoSA;
    }

    public int getNparts() {
      return nparts;
    }

    public GribCollectionImmutable.Type getType() {
      return group.ds.gctype;
    }

    public void show(Formatter sb) {
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
      for (int i = 0; i < nparts; i++) {
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

      // sb.format(super.toStringComplete());
      // return sb.toString();
    }

    ///////////////////////////////////////////////////////////////////

    /**
     * find the data record for a request
     *
     * @param indexWanted the source index request, excluding x and y
     * @return DataRecord pointing to where the data is, or null if missing
     */
    @Nullable
    DataRecord getDataRecord(int[] indexWanted) throws IOException {

      if (Grib.debugRead)
        logger.debug("%nPartitionCollection.getDataRecord index wanted = (%s) on %s type=%s%n",
                Misc.showInts(indexWanted), indexFilename, group.ds.gctype);

      // find the runtime index
      int firstIndex = indexWanted[0];
      int masterIdx;
      if (group.ds.gctype == Type.TwoD) {
        // find the partition by matching run coordinate with master runtime
        CoordinateRuntime runtime = (CoordinateRuntime) getCoordinate(Coordinate.Type.runtime);
        if (runtime == null) {
          throw new IllegalStateException("Type.TwoD must have runtime coordinate");
        }
        Object val = runtime.getValue(firstIndex);
        masterIdx = masterRuntime.getIndex(val);
        if (Grib.debugRead)
          logger.debug("  TwoD firstIndex = %d val=%s masterIdx=%d %n", firstIndex, val, masterIdx);

      } else if (group.ds.gctype == Type.Best) {
        // find the partition from the "time2runtime" array in the time coordinate
        CoordinateTimeAbstract time = getCoordinateTime();
        if (time == null) {
          throw new IllegalStateException("Type.Best must have time coordinate");
        }
        masterIdx = time.getMasterRuntimeIndex(firstIndex) - 1;
        if (Grib.debugRead) logger.debug("  Best firstIndex = %d masterIdx=%d %n", firstIndex, masterIdx);

      } else if (group.ds.gctype == Type.MRUTP) {
        CoordinateTime2D time2D = (CoordinateTime2D) getCoordinateTime();
        if (time2D == null) {
          throw new IllegalStateException("Type.MRUTP must have time coordinate");
        }
        Object val = time2D.getRefDate(firstIndex);
        masterIdx = masterRuntime.getIndex(val);

        if (Grib.debugRead) logger.debug("  MRUTP firstIndex = %d masterIdx=%d %n", firstIndex, masterIdx);

      } else {
        throw new IllegalStateException("Unknown gctype= " + group.ds.gctype + " on " + indexFilename);
      }

      int partno = run2part[masterIdx];
      if (partno < 0) {
        return null; // LOOK is this possible?
      }

      // find the 2D vi in that partition
      GribCollectionImmutable.VariableIndex vindex2Dpart = getVindex2D(partno); // the 2D component variable in the partno partition
      if (vindex2Dpart == null) return null; // missing
      if (Grib.debugRead) logger.debug("  compVindex2D = %s%n", vindex2Dpart.toStringFrom());

      if (isPartitionOfPartitions) {
        VariableIndexPartitioned compVindex2Dp = (VariableIndexPartitioned) vindex2Dpart;
        return getDataRecordPofP(indexWanted, compVindex2Dp);
      }

      // translate to coordinates in vindex
      int[] sourceIndex;
      if (group.getType() == Type.Best)
        sourceIndex = translateIndexBest(indexWanted, vindex2Dpart);
      else
        sourceIndex = translateIndex2D(indexWanted, vindex2Dpart);

      if (sourceIndex == null) return null; // missing
      GribCollectionImmutable.Record record = vindex2Dpart.getRecordAt(sourceIndex);
      if (record == null) {
        return null;
      }

      if (Grib.debugRead) logger.debug("  result success: partno=%d fileno=%d %n", partno, record.fileno);
      return new DataRecord(PartitionCollectionImmutable.this, partno, vindex2Dpart.group.getGdsHorizCoordSys(), record);
    }

    /**
     * find DataRecord in a PofP
     *
     * @param indexWanted   index into this PoP
     * @param compVindex2Dp 2D variable from the desired partition; may be PofP or PofGC
     * @return desired record to be read, from the GC, or null if missing
     */
    @Nullable
    private DataRecord getDataRecordPofP(int[] indexWanted, VariableIndexPartitioned compVindex2Dp) throws IOException {
      if (group.getType() == Type.Best) {
        int[] indexWantedP = translateIndexBest(indexWanted, compVindex2Dp);
        if (Grib.debugRead) logger.debug("  (Best) getDataRecordPofP= %s %n", Misc.showInts(indexWantedP));
        if (indexWantedP == null) return null;
        return compVindex2Dp.getDataRecord(indexWantedP);

      } else {
        // corresponding index into compVindex2Dp
        int[] indexWantedP = translateIndex2D(indexWanted, compVindex2Dp);
        if (Grib.debugRead) logger.debug("  (2D) getDataRecordPofP= %s %n", Misc.showInts(indexWantedP));
        if (indexWantedP == null) return null;
        return compVindex2Dp.getDataRecord(indexWantedP);

      } /* else if (group.getType() == Type.Best) {
        int[] indexWantedP = translateIndexBest(indexWanted, compVindex2Dp);
        if (Grib.debugRead) logger.debug("  (Best) getDataRecordPofP= %s %n", Misc.showInts(indexWantedP));
        if (indexWantedP == null) return null;
        return compVindex2Dp.getDataRecord(indexWantedP);

      } /* else if (group.getType() == Type.MRUTP) {
        int[] indexWantedP = translateIndex1D(masterIdx, indexWanted, compVindex2Dp);
        if (Grib.debugRead) logger.debug("  (1D) getDataRecordPofP= %s %n", Misc.showInts(indexWantedP));
        if (indexWantedP == null) return null;
        return compVindex2Dp.getDataRecord(indexWantedP);
      } */

      // throw new IllegalStateException("Not handling Type "+group.getType());
    }

    /**
     * Get VariableIndex (2D) for this partition
     *
     * @param partno master partition number
     * @return VariableIndex or null if not exists
     */
    @Nullable
    private GribCollectionImmutable.VariableIndex getVindex2D(int partno) throws IOException {
      // at this point, we need to instantiate the Partition and the vindex.records

      // the 2D vip for this variable
      VariableIndexPartitioned vip = isPartitionOfPartitions ? getVariable2DByHash(group.horizCoordSys, this) : this;

      if (vip == null)
        throw new IllegalStateException();

      int partWant = vip.partnoSA.findIdx(partno); // which partition ? index into PartitionCollectionImmutable.partitions[]. variable doesnt have to exist in all partitions
      if (partWant < 0 || partWant >= vip.nparts) {
        if (Grib.debugRead) logger.debug("  cant find partition=%d in vip=%s%n", partno, vip);
        return null;
      }
      // LOOK was partVar = new PartitionForVariable2D(partnoSA.get(idx), groupnoSA.get(idx), varnoSA.get(idx));
      //        GroupGC g = ds.groups.get(partVar.groupno);
      //        GribCollection.VariableIndex vindex = g.variList.get(partVar.varno);
      // now

      Partition p = getPartition(partno);
      try (GribCollectionImmutable gc = p.getGribCollection()) { // ensure that its read in try-with
        GribCollectionImmutable.Dataset ds = gc.getDatasetCanonical(); // always references the twoD or GC dataset
        // the group and variable index may vary across partitions
        GribCollectionImmutable.GroupGC g = ds.groups.get(vip.groupnoSA.get(partWant));         // LOOK partWant vs partno ??
        GribCollectionImmutable.VariableIndex vindex = g.variList.get(vip.varnoSA.get(partWant));
        vindex.readRecords();
        return vindex;
      }  // LOOK opening the file here, and then again to read the data. partition cache helps i guess but we could do better i think.
    }

    /*
     * MRUTP
     * translate index in VariableIndexPartitioned to corresponding index in one of its component VariableIndex (which will be 2D)
     * by matching coordinate values.
     *
     * @param wholeIndex   index in VariableIndexPartitioned, runtime has been added
     * @param vindex2Dpart component 2D VariableIndex
     * @return corresponding index in compVindex2D, or null if missing
     *
    private int[] translateIndex1D(int[] wholeIndex, GribCollectionImmutable.VariableIndex vindex2Dpart) {
      int[] result = new int[wholeIndex.length];

      // figure out the runtime index in the partition
      Coordinate runtime = vindex2Dpart.getCoordinate(0);
      int runtimeIdxPart = matchCoordinate(masterRuntime, masterIdx, runtime);
      if (runtimeIdxPart < 0)
        return null;         // LOOK is this possible ?? should throw exception??
      result[0] = runtimeIdxPart;

      // figure out the other indexes in the partition
      // assumes gc coordinates in same order as iosp
      int countDim = 0;
      while (countDim < wholeIndex.length) {
        Coordinate wholeCoord = getCoordinate(countDim + 1);
        int idx = wholeIndex[countDim];

        Coordinate coordPart = vindex2Dpart.getCoordinate(countDim + 1); // wholeIndex(time, vert) -> vip(runtime, time, vert)
        int resultIdx;
        if (coordPart instanceof CoordinateTime2D) {
          CoordinateTime2D coordPart2D = (CoordinateTime2D) coordPart; // partition coordinate
          CoordinateTime2D coordWhole2D = (CoordinateTime2D) wholeCoord; // whole coordinate
          CoordinateTime2D.Time2D wholeVal = coordWhole2D.getOrgValue(masterIdx, idx);
          if (wholeVal == null)  // is this possible?
            return null;

          resultIdx = coordPart2D.matchTimeCoordinate(runtimeIdxPart, wholeVal);
          // if (resultIdx < 0) resultIdx = compCoord2D.matchTimeCoordinate(runtimeIdxPart, wholeVal, wholeCoord1Dtime.getRefDate()); // debug

        } else {
          resultIdx = matchCoordinate(wholeCoord, idx, coordPart);
          // if (resultIdx < 0) resultIdx = matchCoordinate(wholeCoord1D, idx, compCoord); // debug
        }
        if (resultIdx < 0) {
          // logger.info("Couldnt match coordinates ({}) for variable {}", Misc.showInts(wholeIndex), compVindex2D.toStringFrom());
          return null;
        }
        result[countDim + 1] = resultIdx;
        countDim++;
      }

      return result;
    } */

    /**
     * Best
     * translate index in VariableIndexPartitioned to corresponding index in one of its component VariableIndex (which will be 2D)
     * by matching coordinate values.
     *
     * @param wholeIndex   index in VariableIndexPartitioned
     * @param compVindex2D component 2D VariableIndex
     * @return corresponding index in compVindex2D, or null if missing
     */
    @Nullable
    private int[] translateIndexBest(int[] wholeIndex, GribCollectionImmutable.VariableIndex compVindex2D) {
      int[] result = new int[wholeIndex.length + 1];

      // figure out the runtime
      int timeIdx = wholeIndex[0];
      CoordinateTimeAbstract time = getCoordinateTime();
      assert time != null;
      int masterIdx = time.getMasterRuntimeIndex(timeIdx) - 1;
      int runtimeIdxPart = matchCoordinate(masterRuntime, masterIdx, compVindex2D.getCoordinate(0));
      if (runtimeIdxPart < 0)
        return null;         // LOOK is this possible ??
      result[0] = runtimeIdxPart;

      // figure out the time and any other dimensions
      int countDim = 0;
      while (countDim < wholeIndex.length) {
        Coordinate wholeCoord1D = getCoordinate(countDim);
        int idx = wholeIndex[countDim];

        Coordinate compCoord = compVindex2D.getCoordinate(countDim + 1);
        int resultIdx;
        if (compCoord.getType() == Coordinate.Type.time2D) {
          CoordinateTime2D compCoord2D = (CoordinateTime2D) compCoord; // of the component
          CoordinateTimeAbstract wholeCoord1Dtime = (CoordinateTimeAbstract) wholeCoord1D;// CoordinateTime or CoordinateTimeIntv
          Object wholeVal1D = wholeCoord1D.getValue(idx);
          if (wholeVal1D == null)  // is this possible?
            return null;

          CoordinateTime2D.Time2D wholeVal2D = compCoord2D.isTimeInterval() ?
                  new CoordinateTime2D.Time2D(wholeCoord1Dtime.getRefDate(), null, (TimeCoordIntvValue) wholeVal1D) :
                  new CoordinateTime2D.Time2D(wholeCoord1Dtime.getRefDate(), (Integer) wholeVal1D, null);

          resultIdx = compCoord2D.matchTimeCoordinate(runtimeIdxPart, wholeVal2D);
          // if (resultIdx < 0) resultIdx = compCoord2D.matchTimeCoordinate(runtimeIdxPart, wholeVal, wholeCoord1Dtime.getRefDate()); // debug

        } else {
          resultIdx = matchCoordinate(wholeCoord1D, idx, compCoord);
          // if (resultIdx < 0) resultIdx = matchCoordinate(wholeCoord1D, idx, compCoord); // debug
        }
        if (resultIdx < 0) {
          // logger.info("Couldnt match coordinates ({}) for variable {}", Misc.showInts(wholeIndex), compVindex2D.toStringFrom());
          return null;
        }
        result[countDim + 1] = resultIdx;
        countDim++;
      }

      return result;
    }

    /**
     * TwoD
     * Given the index in the whole (wholeIndex), translate to index in component (compVindex2D) by matching the coordinate values
     *
     * @param wholeIndex   index in the whole
     * @param compVindex2D want index in here
     * @return index into  compVindex2D, or null if missing
     */
    @Nullable
    private int[] translateIndex2D(int[] wholeIndex, GribCollectionImmutable.VariableIndex compVindex2D) {
      int[] result = new int[wholeIndex.length];
      int countDim = 0;

      // special case for 2D time
      CoordinateTime2D compTime2D = (CoordinateTime2D) compVindex2D.getCoordinate(Coordinate.Type.time2D);
      if (compTime2D != null) {
        CoordinateTime2D time2D = (CoordinateTime2D) getCoordinate(Coordinate.Type.time2D);
        if (time2D == null) throw new IllegalStateException("CoordinateTime2D has no time2D");
        CoordinateTime2D.Time2D want = time2D.getOrgValue(wholeIndex[0], wholeIndex[1]);
        if (Grib.debugRead)
          logger.debug("  translateIndex2D[runIdx=%d, timeIdx=%d] in componentVar coords = (%s,%s) %n",
                  wholeIndex[0], wholeIndex[1], (want == null) ? "null" : want.getRefDate(), want);
        if (want == null) {
          // time2D.getOrgValue(wholeIndex[0], wholeIndex[1], Grib.debugRead); // debug
          return null;
        }
        if (!compTime2D.getIndex(want, result)) {// sets the first 2 indices - run and time
          // compTime2D.getIndex(want, result); // debug
          return null; // missing data
        }
        countDim = 2;
      }

      // the remaining dimensions, if any
      while (countDim < wholeIndex.length) {
        int idx = wholeIndex[countDim];
        int resultIdx = matchCoordinate(getCoordinate(countDim), idx, compVindex2D.getCoordinate(countDim));
        if (Grib.debugRead) logger.debug("  translateIndex2D[idx=%d] resultIdx= %d %n", idx, resultIdx);
        if (resultIdx < 0) {   // partition variable doesnt have a coordinate value that is in the "whole" variable
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

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // experimental coord based
    @Nullable
    DataRecord getDataRecord(SubsetParams coords) throws IOException {

      // identify the master index for this runtime
      CalendarDate runtime = coords.getRunTime();
      int masterIdx = masterRuntime.getIndex(runtime.getMillis());
      // LOOK ok to use Best like this (see other getDataRecord) ?

      if (masterIdx < 0) { // means that the runtie is not in the masterRuntime list
        throw new RuntimeException("masterRuntime does not contain runtime " + runtime);
      }

      // each runtime is mapped to a partition
      int partno = run2part[masterIdx];
      if (partno < 0)
        return null; // LOOK is this possible?

      // find the 2D vi in that partition
      GribCollectionImmutable.VariableIndex compVindex2D = getVindex2D(partno); // the 2D component variable in the partno partition
      if (compVindex2D == null) return null; // missing
      if (Grib.debugRead) logger.debug("  compVindex2D = %s%n", compVindex2D.toStringFrom());

      if (isPartitionOfPartitions) {
        VariableIndexPartitioned compVindex2Dp = (VariableIndexPartitioned) compVindex2D;
        return compVindex2Dp.getDataRecord(coords);
      }

      // otherwise its a GribCollection
      GribCollectionImmutable.Record record = compVindex2D.getRecordAt(coords);
      if (record == null)
        return null;

      if (Grib.debugRead) logger.debug("  result success: partno=%d fileno=%d %n", partno, record.fileno);
      DataRecord dr = new DataRecord(PartitionCollectionImmutable.this, partno, compVindex2D.group.getGdsHorizCoordSys(), record);
      if (GribDataReader.validator != null) dr.validation = coords;
      return dr;
    }

  }

  @Immutable
  class DataRecord extends GribDataReader.DataRecord {
    final PartitionCollectionImmutable usePartition;
    final int partno; // partition index in usePartition

    DataRecord(PartitionCollectionImmutable usePartition, int partno, GdsHorizCoordSys hcs, GribCollectionImmutable.Record record) {
      super(-1, record, hcs);
      this.usePartition = usePartition;
      this.partno = partno;
    }

    @Override
    public int compareTo(@Nonnull GribDataReader.DataRecord o) {
      DataRecord op = (DataRecord) o;
      int rp = usePartition.getName().compareTo(op.usePartition.getName());
      if (rp != 0) return rp;
      int r = Misc.compare(partno, op.partno);
      if (r != 0) return r;
      r = Misc.compare(record.fileno, o.record.fileno);
      if (r != 0) return r;
      return Misc.compare(record.pos, o.record.pos);
    }

    boolean usesSameFile(DataRecord o) {
      if (o == null) return false;
      int rp = usePartition.getName().compareTo(o.usePartition.getName());
      if (rp != 0) return false;
      int r = Misc.compare(partno, o.partno);
      if (r != 0) return false;
      r = Misc.compare(record.fileno, o.record.fileno);
      return r == 0;
    }

    //debugging
    public void show() throws IOException {
      String dataFilename = usePartition.getFilename(partno, record.fileno);
      System.out.printf(" **DataReader partno=%d fileno=%d filename=%s startPos=%d%n", partno, record.fileno, dataFilename, record.pos);
    }
  }
}
