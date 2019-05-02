/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import com.google.protobuf.ByteString;
import thredds.inventory.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.coord.Coordinate;
import ucar.nc2.grib.coord.CoordinateEns;
import ucar.nc2.grib.coord.CoordinateRuntime;
import ucar.nc2.grib.coord.CoordinateTime;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.nc2.grib.coord.CoordinateTimeIntv;
import ucar.nc2.grib.coord.CoordinateVert;
import ucar.nc2.grib.coord.SparseArray;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.stream.NcStream;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.*;

/**
 * Build a GribCollection object for Grib-2 files. Manage grib collection index.
 * Covers GribCollectionProto, which serializes and deserializes.
 *
 * @author caron
 * @since 4/6/11
 */
class Grib2CollectionWriter extends GribCollectionWriter {
  public static final String MAGIC_START = "Grib2Collectio2Index";  // was Grib2CollectionIndex
  static final int minVersion = 1;  // increment this when you want to force index rebuild
  protected static final int version = 3;     // increment this as needed, must be backwards compatible through minVersion

  Grib2CollectionWriter(MCollection dcm, org.slf4j.Logger logger) {
    super(dcm, logger);
  }

  public static class Group implements GribCollectionBuilder.Group {
    public final Grib2SectionGridDefinition gdss;
    public final int hashCode;       // may have been modified
    public CalendarDate runtime;

    List<Grib2CollectionBuilder.VariableBag> gribVars;
    public List<Coordinate> coords;
    public final List<Grib2Record> records = new ArrayList<>();
    public final Set<Long> runtimes = new HashSet<>();
    Set<Integer> fileSet; // this is so we can show just the component files that are in this group

    Group(Grib2SectionGridDefinition gdss, int hashCode) {
      this.gdss = gdss;
      this.hashCode = hashCode;
    }

    Group(Grib2SectionGridDefinition gdss, int hashCode, CalendarDate runtime) {
      this.gdss = gdss;
      this.hashCode = hashCode;
      this.runtime = runtime;
    }

    @Override
    public CalendarDate getRuntime() {
      return runtime;
    }

    @Override
    public Set<Long> getCoordinateRuntimes() {
      return runtimes;
    }

    @Override
    public List<Coordinate> getCoordinates() {
      return coords;
    }
  }

  ///////////////////////////////////////////////////
  // heres where the actual writing is

  /*
   MAGIC_START
   version
   sizeRecords
   SparseArray's (sizeRecords bytes)
   sizeIndex
   GribCollectionIndex (sizeIndex bytes)
   */

  boolean writeIndex(String name, File idxFile, CoordinateRuntime masterRuntime, List<Group> groups, List<MFile> files,
                            GribCollectionImmutable.Type type, CalendarDateRange dateRange) throws IOException {
    Grib2Record first = null; // take global metadata from here
    boolean deleteOnClose = false;

    if (idxFile.exists()) {
      RandomAccessFile.eject(idxFile.getPath());
      if (!idxFile.delete()) {
        logger.error("gc2 cant delete index file {}", idxFile.getPath());
      }
    }
    logger.debug(" createIndex for {}", idxFile.getPath());

    try (RandomAccessFile raf = new RandomAccessFile(idxFile.getPath(), "rw")) {
      //// header message
      raf.order(RandomAccessFile.BIG_ENDIAN);
      raf.write(MAGIC_START.getBytes(CDM.utf8Charset));
      raf.writeInt(version);
      long lenPos = raf.getFilePointer();
      raf.writeLong(0); // save space to write the length of the record section
      long countBytes = 0;
      int countRecords = 0;

      Set<Integer> allFileSet = new HashSet<>();
      for (Group g : groups) {
        g.fileSet = new HashSet<>();
        for (Grib2CollectionBuilder.VariableBag vb : g.gribVars) {
          if (first == null) first = vb.first;
          GribCollectionProto.SparseArray vr = writeSparseArray(vb, g.fileSet);
          byte[] b = vr.toByteArray();
          vb.pos = raf.getFilePointer();
          vb.length = b.length;
          raf.write(b);
          countBytes += b.length;
          countRecords += vb.coordND.getSparseArray().countNotMissing();
        }
        allFileSet.addAll(g.fileSet);
      }

      if (logger.isDebugEnabled()) {
        long bytesPerRecord = countBytes / ((countRecords == 0) ? 1 : countRecords);
        logger.debug("  write RecordMaps: bytes = {} record = {} bytesPerRecord={}", countBytes, countRecords, bytesPerRecord);
      }

      if (first == null) {
        deleteOnClose = true;
        throw new IOException("GribCollection " + name + " has no records");
      }

      long pos = raf.getFilePointer();
      raf.seek(lenPos);
      raf.writeLong(countBytes);
      raf.seek(pos); // back to the output.

      /*
      message GribCollection {
        string name = 1;         // must be unique - index filename is name.ncx
        string topDir = 2;       // MFile, Partition filenames are reletive to this
        repeated MFile mfiles = 3;        // list of grib MFiles
        repeated Dataset dataset = 4;
        repeated Gds gds = 5;             // unique Gds, shared amongst datasets
        Coord masterRuntime = 6;  // list of runtimes in this GC

        int32 center = 7;      // these 4 fields are to get a GribCustomizer
        int32 subcenter = 8;
        int32 master = 9;
        int32 local = 10;       // grib1 table Version

        int32 genProcessType = 11;
        int32 genProcessId = 12;
        int32 backProcessId = 13;
        int32 version = 14;     // >= 3 for proto3 (5.0+)

        // repeated Parameter params = 20;      // not used
        FcConfig config = 21;

        // extensions
        repeated Partition partitions = 100;
        bool isPartitionOfPartitions = 101;
        repeated uint32 run2part = 102 [packed=true];  // masterRuntime index to partition index
      }
       */
      GribCollectionProto.GribCollection.Builder indexBuilder = GribCollectionProto.GribCollection.newBuilder();
      indexBuilder.setName(name);
      indexBuilder.setTopDir(dcm.getRoot());
      indexBuilder.setVersion(currentVersion);

      // directory and mfile list
      File directory = new File(dcm.getRoot());
      List<GcMFile> gcmfiles = GcMFile.makeFiles(directory, files, allFileSet);
      for (GcMFile gcmfile : gcmfiles) {
        GribCollectionProto.MFile.Builder b = GribCollectionProto.MFile.newBuilder();
        b.setFilename(gcmfile.getName());
        b.setLastModified(gcmfile.getLastModified());
        b.setLength(gcmfile.getLength());
        b.setIndex(gcmfile.index);
        indexBuilder.addMfiles(b.build());
      }

      indexBuilder.setMasterRuntime(writeCoordProto(masterRuntime));

       //gds
      for (Object go : groups) {
        Group g = (Group) go;
        indexBuilder.addGds(writeGdsProto(g.gdss.getRawBytes(), -1));
      }

      // the GC dataset
      indexBuilder.addDataset(writeDatasetProto(type, groups));

      // what about just storing first ??
      Grib2SectionIdentification ids = first.getId();
      indexBuilder.setCenter(ids.getCenter_id());
      indexBuilder.setSubcenter(ids.getSubcenter_id());
      indexBuilder.setMaster(ids.getMaster_table_version());
      indexBuilder.setLocal(ids.getLocal_table_version());

      Grib2Pds pds = first.getPDS();
      indexBuilder.setGenProcessType(pds.getGenProcessType());
      indexBuilder.setGenProcessId(pds.getGenProcessId());
      indexBuilder.setBackProcessId(pds.getBackProcessId());

      indexBuilder.setStartTime(dateRange.getStart().getMillis());
      indexBuilder.setEndTime(dateRange.getEnd().getMillis());

      GribCollectionProto.GribCollection index = indexBuilder.build();
      byte[] b = index.toByteArray();
      NcStream.writeVInt(raf, b.length); // message size
      raf.write(b);  // message  - all in one gulp
      logger.debug("  write GribCollectionIndex= {} bytes", b.length);

    } finally {
      // remove it on failure
      if (deleteOnClose && !idxFile.delete())
        logger.error(" gc2 cant deleteOnClose index file {}", idxFile.getPath());
    }

    return true;
  }

  /*
  message Record {
    uint32 fileno = 1;               // which GRIB file ? key into GC.fileMap
    uint64 pos = 2;                  // offset in GRIB file of the start of entire message
    uint64 bmsPos = 3;               // use alternate bms if non-zero
    uint32 drsOffset = 4;            // offset of drs from pos (grib2 only)
  }

  // SparseArray only at the GCs (MRC and SRC) not at the Partitions
  // dont need SparseArray in memory until someone wants to read from the variable
  message SparseArray {
    repeated uint32 size = 2 [packed=true];     // multidim sizes = shape[]
    repeated uint32 track = 3 [packed=true];    // 1-based index into record list, 0 == missing
    repeated Record records = 4;                // List<Record>
    uint32 ndups = 5;                           // duplicates found when creating
  }
   */
  private GribCollectionProto.SparseArray writeSparseArray(Grib2CollectionBuilder.VariableBag vb, Set<Integer> fileSet) {
    GribCollectionProto.SparseArray.Builder b = GribCollectionProto.SparseArray.newBuilder();
    SparseArray<Grib2Record> sa = vb.coordND.getSparseArray();
    for (int size : sa.getShape())
      b.addSize(size);
    for (int track : sa.getTrack())
      b.addTrack(track);

    for (Grib2Record gr : sa.getContent()) {
      GribCollectionProto.Record.Builder br = GribCollectionProto.Record.newBuilder();

      br.setFileno(gr.getFile());
      fileSet.add(gr.getFile());
      long startPos = gr.getIs().getStartPos();
      br.setStartPos(startPos);

      if (gr.isBmsReplaced()) {
        Grib2SectionBitMap bms = gr.getBitmapSection();
        br.setBmsOffset((int) (bms.getStartingPosition() - startPos));
      }

      Grib2SectionDataRepresentation drs = gr.getDataRepresentationSection();
      br.setDrsOffset((int)(drs.getStartingPosition() - startPos));
      b.addRecords(br);
    }

    b.setNdups(sa.getNdups());
    return b.build();
  }

    /*
  message Dataset {
    required Type type = 1;
    repeated Group groups = 2;
   */
  private GribCollectionProto.Dataset writeDatasetProto(GribCollectionImmutable.Type type, List<Group> groups) {
    GribCollectionProto.Dataset.Builder b = GribCollectionProto.Dataset.newBuilder();

    GribCollectionProto.Dataset.Type ptype = GribCollectionProto.Dataset.Type.valueOf(type.toString());
    b.setType(ptype);

    for (Group group : groups)
      b.addGroups(writeGroupProto(group));

    return b.build();
  }

  /*
  message Group {
    Gds gds = 1;                             // use this to build the HorizCoordSys
    repeated Variable variables = 2;         // list of variables
    repeated Coord coords = 3;               // list of coordinates
    repeated uint32 fileno = 4 [packed=true]; // the component files that are in this group, key into gc.mfiles
  }
   */
  private GribCollectionProto.Group writeGroupProto(Group g) {
    GribCollectionProto.Group.Builder b = GribCollectionProto.Group.newBuilder();

    b.setGds( writeGdsProto(g.gdss.getRawBytes(), -1));

    for (Grib2CollectionBuilder.VariableBag vbag : g.gribVars) {
      b.addVariables(writeVariableProto(vbag));
    }

    for (Coordinate coord : g.coords) {
      switch (coord.getType()) {
        case runtime:
          b.addCoords(writeCoordProto((CoordinateRuntime) coord));
          break;
        case time:
          b.addCoords(writeCoordProto((CoordinateTime) coord));
          break;
        case timeIntv:
          b.addCoords(writeCoordProto((CoordinateTimeIntv) coord));
          break;
        case time2D:
          b.addCoords(writeCoordProto((CoordinateTime2D) coord));
          break;
        case vert:
          b.addCoords(writeCoordProto((CoordinateVert) coord));
          break;
        case ens:
          b.addCoords(writeCoordProto((CoordinateEns) coord));
          break;
      }
    }

    for (Integer aFileSet : g.fileSet)
      b.addFileno(aFileSet);

    return b.build();
  }


  /*
  message Variable {
   uint32 discipline = 1;
   bytes pds = 2;                   // raw pds
   repeated uint32 ids = 3 [packed=true];         // extra info not in pds; grib2 id section

   uint64 recordsPos = 4;  // offset of SparseArray message for this Variable
   uint32 recordsLen = 5;  // size of SparseArray message for this Variable

   repeated uint32 coordIdx = 6 [packed=true];    // indexes into Group.coords

   // optionally keep stats
   uint32 ndups = 8;
   uint32 nrecords = 9;
   uint32 missing = 10;

   // partition only
   repeated PartitionVariable partVariable = 100;
   }
   */
  private GribCollectionProto.Variable writeVariableProto(Grib2CollectionBuilder.VariableBag vb) {
    GribCollectionProto.Variable.Builder b = GribCollectionProto.Variable.newBuilder();

    b.setDiscipline(vb.first.getDiscipline());
    b.setPds(ByteString.copyFrom(vb.first.getPDSsection().getRawBytes()));

    // extra id info
    b.addIds(vb.first.getId().getCenter_id());
    b.addIds(vb.first.getId().getSubcenter_id());

    b.setRecordsPos(vb.pos);
    b.setRecordsLen(vb.length);

    for (int idx : vb.coordIndex)
      b.addCoordIdx(idx);

    // keep stats
    SparseArray sa = vb.coordND.getSparseArray();
    if (sa != null) {
      b.setNdups(sa.getNdups());
      b.setNrecords(sa.countNotMissing());
      b.setMissing(sa.countMissing());
    }

    return b.build();
  }

}
