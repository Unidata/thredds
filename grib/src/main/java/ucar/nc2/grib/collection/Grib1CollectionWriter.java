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

import com.google.protobuf.ByteString;
import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import ucar.coord.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.grib1.*;
import ucar.nc2.stream.NcStream;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Grib1 specific ncx writer
 *
 * @author caron
 * @since 2/20/14
 */
class Grib1CollectionWriter extends GribCollectionWriter {

  public static final String MAGIC_START = "Grib1Collectio2Index";  // was Grib1CollectionIndex
  protected static final int minVersion = 1;
  protected static final int version = 2;

  protected final MCollection dcm; // may be null, when read in from index
  protected final org.slf4j.Logger logger;

  protected Grib1CollectionWriter(MCollection dcm, org.slf4j.Logger logger) {
    this.dcm = dcm;
    this.logger = logger;
  }

  static class Group implements GribCollectionBuilder.Group {
    final Grib1SectionGridDefinition gdss;
    final Object gdsHashObject;       // may have been modified
    final CalendarDate runtime;

    public List<Grib1CollectionBuilder.VariableBag> gribVars;
    public List<Coordinate> coords;

    public Set<Long> runtimes = new HashSet<>();
    public List<Grib1Record> records = new ArrayList<>();
    public Set<Integer> fileSet; // this is so we can show just the component files that are in this group

    Group(Grib1SectionGridDefinition gdss, Object gdsHashObject, CalendarDate runtime) {
      this.gdss = gdss;
      this.gdsHashObject = gdsHashObject;
      this.runtime = runtime;
    }

    @Override
    public CalendarDate getRuntime() {
      return runtime;
    }

    @Override
    public List<Coordinate> getCoordinates() {
      return coords;
    }

    @Override
    public Set<Long> getCoordinateRuntimes() {
      return runtimes;
    }
  }

    /*
   MAGIC_START
   version
   sizeRecords
   VariableRecords (sizeRecords bytes)
   sizeIndex
   GribCollectionIndex (sizeIndex bytes)
   */

  // indexFile is in the cache
  boolean writeIndex(String name, File idxFile, CoordinateRuntime masterRuntime, List<Group> groups, List<MFile> files,
                     GribCollectionImmutable.Type type) throws IOException {
    Grib1Record first = null; // take global metadata from here
    boolean deleteOnClose = false;

    if (idxFile.exists()) {
      RandomAccessFile.eject(idxFile.getPath());
      if (!idxFile.delete())
        logger.warn(" gc1 cant delete index file {}", idxFile.getPath());
    }
    logger.debug(" createIndex for {}", idxFile.getPath());

    try (RandomAccessFile raf = new RandomAccessFile(idxFile.getPath(), "rw")) {
      raf.order(RandomAccessFile.BIG_ENDIAN);

      //// header message
      raf.write(MAGIC_START.getBytes(CDM.utf8Charset));
      raf.writeInt(version);
      long lenPos = raf.getFilePointer();
      raf.writeLong(0); // save space to write the length of the record section
      long countBytes = 0;
      int countRecords = 0;

      Set<Integer> allFileSet = new HashSet<>();
      for (Group g : groups) {
        g.fileSet = new HashSet<>();
        for (Grib1CollectionBuilder.VariableBag vb : g.gribVars) {
          if (first == null) first = vb.first;
          GribCollectionProto.SparseArray vr = writeSparseArray(vb, g.fileSet);
          byte[] b = vr.toByteArray();
          vb.pos = raf.getFilePointer();
          vb.length = b.length;
          raf.write(b);
          countBytes += b.length;
          countRecords += vb.coordND.getSparseArray().countNotMissing();
        }
        for (int index : g.fileSet) allFileSet.add(index);
      }
      long bytesPerRecord = countBytes / ((countRecords == 0) ? 1 : countRecords);
      if (logger.isDebugEnabled())
        logger.debug("  write RecordMaps: bytes = {} records = {} bytesPerRecord={}", countBytes, countRecords, bytesPerRecord);

      if (first == null) {
        deleteOnClose = true;
        throw new IOException("GribCollection " + name + " has no records");
      }

      long pos = raf.getFilePointer();
      raf.seek(lenPos);
      raf.writeLong(countBytes);
      raf.seek(pos); // back to the output.

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
      for (Group g : groups)
        indexBuilder.addGds(writeGdsProto(g.gdss.getRawBytes(), g.gdss.getPredefinedGridDefinition()));

      // the GC dataset
      indexBuilder.addDataset( writeDatasetProto(type, groups));

      /* int count = 0;
      for (DatasetCollectionManager dcm : collections) {
        indexBuilder.addParams(makeParamProto(new Parameter("spec" + count, dcm.())));
        count++;
      } */

      // what about just storing first ??
      Grib1SectionProductDefinition pds = first.getPDSsection();
      indexBuilder.setCenter(pds.getCenter());
      indexBuilder.setSubcenter(pds.getSubCenter());
      indexBuilder.setLocal(pds.getTableVersion());
      indexBuilder.setMaster(0);
      indexBuilder.setGenProcessId(pds.getGenProcess());

      GribCollectionProto.GribCollection index = indexBuilder.build();
      byte[] b = index.toByteArray();
      NcStream.writeVInt(raf, b.length); // message size
      raf.write(b);  // message  - all in one gulp

      logger.debug("  write GribCollectionIndex= {} bytes", b.length);
      logger.debug("  file size =  %d bytes", raf.length());
      return true;

    } finally {

      // remove it on failure
      if (deleteOnClose && !idxFile.delete())
        logger.error(" gc1 cant deleteOnClose index file {}", idxFile.getPath());
    }
  }

    /*
  message Record {
    required uint32 fileno = 1;  // index into GribCollectionIndex.files
    required uint64 pos = 2;     // offset in Grib file of the start of drs (grib2) or entire message (grib1)
    optional uint64 bmsPos = 3 [default = 0]; // use alternate bms
  }

  // dont need SparseArray unless someone wants to read from the variable
  message SparseArray {
    required fixed32 cdmHash = 1; // which variable
    repeated uint32 size = 2;     // multidim sizes
    repeated uint32 track = 3;    // 1-based index into record list, 0 == missing
    repeated Record records = 4;  // List<Record>
  }
   */
  private GribCollectionProto.SparseArray writeSparseArray(Grib1CollectionBuilder.VariableBag vb, Set<Integer> fileSet) throws IOException {
    GribCollectionProto.SparseArray.Builder b = GribCollectionProto.SparseArray.newBuilder();
    b.setCdmHash(vb.gv.hashCode());
    SparseArray<Grib1Record> sa = vb.coordND.getSparseArray();
    for (int size : sa.getShape())
      b.addSize(size);
    for (int track : sa.getTrack())
      b.addTrack(track);

    for (Grib1Record gr : sa.getContent()) {
      GribCollectionProto.Record.Builder br = GribCollectionProto.Record.newBuilder();

      br.setFileno(gr.getFile());
      fileSet.add(gr.getFile());
      Grib1SectionIndicator is = gr.getIs();
      br.setPos(is.getStartPos()); // start of entire message

      // br.setScanMode(gr.getScanMode()); // added 2/6/2014  LOOK dont need scan for GRIB1 I think ??
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
  private GribCollectionProto.Dataset writeDatasetProto(GribCollectionImmutable.Type type, List<Group> groups) throws IOException {
    GribCollectionProto.Dataset.Builder b = GribCollectionProto.Dataset.newBuilder();

    GribCollectionProto.Dataset.Type ptype = GribCollectionProto.Dataset.Type.valueOf(type.toString());
    b.setType(ptype);

    int count = 0 ;
    for (Group group : groups)
      b.addGroups(writeGroupProto(group, count++));

    return b.build();
  }

    /*
  message Group {
    required uint32 gdsIndex = 1;       // index into GribCollection.gds array
    repeated Variable variables = 2;    // list of variables
    repeated Coord coords = 3;          // list of coordinates
    repeated int32 fileno = 4;          // the component files that are in this group, index into gc.mfiles

    repeated Parameter params = 20;      // not used yet
    extensions 100 to 199;
  }
   */
  protected GribCollectionProto.Group writeGroupProto(Group g, int groupIndex) throws IOException {
    GribCollectionProto.Group.Builder b = GribCollectionProto.Group.newBuilder();

    b.setGdsIndex(groupIndex); // index into gds list
    b.setIsTwod(true);         // LOOK

    for (Grib1CollectionBuilder.VariableBag vbag : g.gribVars) {
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
     required uint32 discipline = 1;
     required bytes pds = 2;          // raw pds
     required fixed32 cdmHash = 3;

     required uint64 recordsPos = 4;  // offset of SparseArray message for this Variable
     required uint32 recordsLen = 5;  // size of SparseArray message for this Variable

     repeated uint32 coordIdx = 6;    // indexes into Group.coords

     // optionally keep stats
     optional float density = 7;
     optional uint32 ndups = 8;
     optional uint32 nrecords = 9;
     optional uint32 missing = 10;

     repeated uint32 invCount = 15;      // for Coordinate TwoTimer, only 2D vars
     repeated uint32 time2runtime = 16;  // time index to runtime index, only 1D vars
     repeated Parameter params = 20;    // not used yet

     extensions 100 to 199;
   }
   */
  private GribCollectionProto.Variable writeVariableProto(Grib1CollectionBuilder.VariableBag vb) throws IOException {
    GribCollectionProto.Variable.Builder b = GribCollectionProto.Variable.newBuilder();

    b.setDiscipline(0);
    b.setPds(ByteString.copyFrom(vb.first.getPDSsection().getRawBytes()));

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
