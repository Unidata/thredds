/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib.collection;

import com.google.protobuf.ByteString;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.*;
import thredds.inventory.partition.PartitionManager;
import ucar.sparr.Coordinate;
import ucar.sparr.Counter;
import ucar.sparr.SparseArray;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.stream.NcStream;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CloseableIterator;
import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.*;

/**
 * Build a GribCollection object for Grib-2 files. Manage grib collection index.
 * Covers GribCollectionProto, which serializes and deserializes.
 * Rectilyse means to turn the collection into a multidimensional variable.
 *
 * @author caron
 * @since 4/6/11
 */
public class Grib2CollectionBuilder extends GribCollectionBuilder {

  public static final String MAGIC_START = "Grib2Collectio2Index";  // was Grib2CollectionIndex
  protected static final int minVersionSingle = 1;
  protected static final int version = 1;
  private static final boolean showFiles = false;

  /* called by tdm
  static public boolean update(MCollection dcm, Formatter errlog, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm, logger);
    if (!builder.needsUpdate()) return false;
    builder.readOrCreateIndex(CollectionUpdateType.always, errlog);
    builder.gc.close();
    return true;
  }

  // from a single file, read in the index, create if it doesnt exist
  static public GribCollection readOrCreateIndexFromSingleFile(MFile file, CollectionUpdateType force,
                                                               FeatureCollectionConfig.GribConfig config, Formatter errlog, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(file, config, logger);
    builder.readOrCreateIndex(force, errlog);
    return builder.gc;
  } */

  /**
   * From a CollectionManagerRO, read in the index, create if it doesnt exist or is out of date
   *
   * @param dcm    the CollectionManager, assumed up-to-date
   * @param force  force read the index
   * @param logger log here
   * @return GribCollection
   * @throws IOException on IO error
   */
  static public GribCollection factory(MCollection dcm, CollectionUpdateType force, Formatter errlog,
                                       org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm, logger);
    builder.readOrCreateIndex(force, errlog);
    return builder.gc;
  }

  /* read in the index, index raf already open
  static public GribCollection createFromIndex(String name, File directory, RandomAccessFile raf, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(name, directory, config, logger);
    if (builder.readIndex(raf))
      return builder.gc;
    throw new IOException("Reading index failed");
  } */

  /* this writes the index always
  static public boolean writeIndexFile(File indexFile, CollectionManagerRO dcm, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm, logger);
    return builder.createIndex(indexFile);
  } */

  /// for ToolsUI
  static public boolean makeIndex(MCollection dcm, Formatter errlog, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm, logger);
    File indexFile = builder.gc.getIndexFile();
    errlog.format("Using index file %s%n", indexFile.getAbsolutePath());
    boolean ok = builder.createIndex(indexFile, errlog);
    builder.gc.close();
    return ok;
  }

  // for debugging
  static public Grib2CollectionBuilder debugOnly(MCollection dcm, org.slf4j.Logger logger) {
    return new Grib2CollectionBuilder(dcm, logger);
  }

    // from a single file, read in the index, create if it doesnt exist
  static public GribCollection readOrCreateIndexFromSingleFile(MFile file, CollectionUpdateType force,
                                                               FeatureCollectionConfig.GribConfig config, Formatter errlog, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(file, config, logger);
    builder.readOrCreateIndex(force, errlog);
    return builder.gc;
  }

  ////////////////////////////////////////////////////////////////

  protected GribCollection gc;      // make this object
  protected Grib2Customizer tables; // only gets created in makeAggGroups
  protected String name;            // collection name
  protected File directory;         // top directory

  // single file
  private Grib2CollectionBuilder(MFile file, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    super(new CollectionSingleFile(file, logger), true, logger);
    this.name = file.getName();
    this.directory = new File(dcm.getRoot());

    try {
      if (config != null) dcm.putAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG, config);
      this.gc = new Grib2Collection(this.name, this.directory, config);

    } catch (Exception e) {
      logger.error("Failed to index single file", e);
      throw new IOException(e);
    }
  }

  // for Grib2PartitionBuilder
  protected Grib2CollectionBuilder(PartitionManager tpc, org.slf4j.Logger logger) {
    super(tpc, false, logger);
  }

  // from a collection of files
  private Grib2CollectionBuilder(MCollection dcm, org.slf4j.Logger logger) {
    super(dcm, false, logger);
    this.name = dcm.getCollectionName();
    this.directory = new File(dcm.getRoot());

    FeatureCollectionConfig.GribConfig config = (FeatureCollectionConfig.GribConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    this.gc = new Grib2Collection(this.name, this.directory, config);
  }

  private void readOrCreateIndex(CollectionUpdateType ff, Formatter errlog) throws IOException {

    // force new index or test for new index needed
    boolean force = ((ff == CollectionUpdateType.always) || (ff == CollectionUpdateType.test && needsUpdate()));

    // otherwise, we're good as long as the index file exists
    File idx = gc.getIndexFile();
    if (force || !idx.exists() || !readIndex(idx.getPath())) {

      if (ff == CollectionUpdateType.never)
        throw new IOException("failed to read " + idx.getPath());

      // write out index
      idx = gc.makeNewIndexFile(logger); // make sure we have a writeable index
      logger.info("Grib2CollectionBuilder on {}: createIndex {}", gc.getName(), idx.getPath());
      if (errlog != null) errlog.format("%s: create Index at %s%n", gc.getName(), idx.getPath());
      createIndex(idx, errlog);

      // read back in index
      RandomAccessFile indexRaf = new RandomAccessFile(idx.getPath(), "r");
      gc.setIndexRaf(indexRaf);
      readIndex(indexRaf);
    }
  }

  public boolean needsUpdate() throws IOException {
    if (dcm == null) return false;
    File idx = gc.getIndexFile();
    return !idx.exists() || needsUpdate(idx.lastModified());
  }

  private boolean needsUpdate(long idxLastModified) throws IOException {
    CollectionManager.ChangeChecker cc = GribIndex.getChangeChecker();
    try (CloseableIterator<MFile> iter = dcm.getFileIterator()) {
      while (iter.hasNext()) {
        if (cc.hasChangedSince(iter.next(), idxLastModified)) return true;
      }
    }
    return false;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // reading

  private boolean readIndex(String filename) throws IOException {
    return readIndex(new RandomAccessFile(filename, "r"));
  }

  private boolean readIndex(RandomAccessFile indexRaf) throws IOException {
    FeatureCollectionConfig.GribConfig config = (FeatureCollectionConfig.GribConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    try {
      gc = Grib2CollectionBuilderFromIndex.readFromIndex(this.name, this.directory, indexRaf, config, logger);
      return true;
    } catch (IOException ioe) {
      return false;
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////
  // writing

  public class Group {
    public Grib2SectionGridDefinition gdss;
    public int gdsHash; // may have been modified
    public Grib2Rectilyser rect;
    public List<Grib2Record> records = new ArrayList<>();
    // public String nameOverride;
    public Set<Integer> fileSet; // this is so we can show just the component files that are in this group

    private Group(Grib2SectionGridDefinition gdss, int gdsHash) {
      this.gdss = gdss;
      this.gdsHash = gdsHash;
    }
  }

  private boolean createIndex(File indexFile, Formatter errlog) throws IOException {
    if (dcm == null) {
      logger.error("Grib2CollectionBuilder " + gc.getName() + " : cannot create new index ");
      throw new IllegalStateException();
    }

    long start = System.currentTimeMillis();

    List<MFile> files = new ArrayList<>();
    List<Group> groups = makeGroups(files, errlog);
    List<MFile> allFiles = Collections.unmodifiableList(files);
    writeIndex(indexFile, groups, allFiles);

    long took = System.currentTimeMillis() - start;
    logger.debug("That took {} msecs", took);
    return true;
  }

  // read all records in all files,
  // divide into groups based on GDS hash
  // each group has an arraylist of all records that belong to it.
  // for each group, run rectlizer to derive the coordinates and variables
  public List<Group> makeGroups(List<MFile> allFiles, Formatter errlog) throws IOException {
    Map<Integer, Group> gdsMap = new HashMap<>();
    Map<String, Boolean> pdsConvert = null;

    logger.debug("GribCollection {}: makeAggregatedGroups", gc.getName());
    int fileno = 0;
    Counter statsAll = new Counter(); // debugging

    logger.debug(" dcm={}", dcm);
    FeatureCollectionConfig.GribConfig config = (FeatureCollectionConfig.GribConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    Map<Integer, Integer> gdsConvert = (config != null) ? config.gdsHash : null;
    if (config != null) pdsConvert = config.pdsHash;

    // place each record into its group
    // LOOK maybe also put into unique variable Bags
    int totalRecords = 0;
    try (CloseableIterator<MFile> iter = dcm.getFileIterator()) { // not sorted
      while (iter.hasNext()) {
        MFile mfile = iter.next();
        Grib2Index index;
        try {                  // LOOK here is where gbx9 files get recreated; do not make collection index
          index = (Grib2Index) GribIndex.readOrCreateIndexFromSingleFile(false, false, mfile, config, CollectionUpdateType.test, logger);
          allFiles.add(mfile);  // add on success

        } catch (IOException ioe) {
          if (errlog != null)
            errlog.format(" ERR Grib2CollectionBuilder %s: reading/Creating gbx9 index for file %s failed err=%s%n", gc.getName(), mfile.getPath(), ioe.getMessage());
          logger.error("Grib2CollectionBuilder " + gc.getName() + " : reading/Creating gbx9 index for file " + mfile.getPath() + " failed", ioe);
          continue;
        }
        int n = index.getNRecords();
        totalRecords += n;
        if (showFiles) {
          System.out.printf("Open %d %s number of records = %d (%d) %n", fileno, mfile.getPath(), n, totalRecords);
        }

        for (Grib2Record gr : index.getRecords()) { // LOOK we are using entire Grib2Record - memory limitations
          if (this.tables == null) {
            Grib2SectionIdentification ids = gr.getId(); // so all records must use the same table (!)
            this.tables = Grib2Customizer.factory(ids.getCenter_id(), ids.getSubcenter_id(), ids.getMaster_table_version(), ids.getLocal_table_version());
            if (config != null) tables.setTimeUnitConverter(config.getTimeUnitConverter());
          }

          gr.setFile(fileno); // each record tracks which file it belongs to
          int gdsHash = gr.getGDSsection().getGDS().hashCode();  // use GDS hash code to group records
          if (gdsConvert != null && gdsConvert.get(gdsHash) != null) // allow external config to muck with gdsHash. Why? because of error in encoding
            gdsHash = gdsConvert.get(gdsHash);                       // and we need exact hash matching

          Group g = gdsMap.get(gdsHash);
          if (g == null) {
            g = new Group(gr.getGDSsection(), gdsHash);
            gdsMap.put(gdsHash, g);
          }
          g.records.add(gr);
        }
        fileno++;
        statsAll.recordsTotal += index.getRecords().size();
      }
    }

    // rectilyze each group independently
    List<Group> groups = new ArrayList<>(gdsMap.values());
    for (Group g : groups) {
      Counter stats = new Counter(); // debugging
      g.rect = new Grib2Rectilyser(tables, g.records, g.gdsHash, pdsConvert);
      g.rect.make(config, Collections.unmodifiableList(allFiles), stats, errlog);
      if (errlog != null) errlog.format(" Group hash=%d %s", g.gdsHash, stats.show());
      statsAll.add(stats);
    }

    // debugging and validation
    if (logger.isDebugEnabled()) logger.debug(statsAll.show());
    if (errlog != null && groups.size() > 1) errlog.format(" All groups=%s%n", statsAll.show());

    return groups;
  }

  // for ui debugging
  public Grib2Customizer getCustomizer() {
    return tables;
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

  private boolean writeIndex(File indexFile, List<Group> groups, List<MFile> files) throws IOException {
    Grib2Record first = null; // take global metadata from here
    boolean deleteOnClose = false;

    if (indexFile.exists()) {
      if (!indexFile.delete()) {
        logger.error("gc2 cant delete index file {}", indexFile.getPath());
      }
    }
    logger.debug(" createIndex for {}", indexFile.getPath());

    try (RandomAccessFile raf = new RandomAccessFile(indexFile.getPath(), "rw")) {
      //// header message
      raf.order(RandomAccessFile.BIG_ENDIAN);
      raf.write(MAGIC_START.getBytes(CDM.utf8Charset));
      raf.writeInt(version);
      long lenPos = raf.getFilePointer();
      raf.writeLong(0); // save space to write the length of the record section
      long countBytes = 0;
      int countRecords = 0;

      for (Group g : groups) {
        g.fileSet = new HashSet<Integer>();
        for (Grib2Rectilyser.VariableBag vb : g.rect.getGribvars()) {
          if (first == null) first = vb.first;
          GribCollectionProto.SparseArray vr = writeSparseArray(vb, g.fileSet);
          byte[] b = vr.toByteArray();
          vb.pos = raf.getFilePointer();
          vb.length = b.length;
          raf.write(b);
          countBytes += b.length;
          //countRecords += vb.recordMap.length;
        }
      }

      if (logger.isDebugEnabled()) {
        long bytesPerRecord = countBytes / ((countRecords == 0) ? 1 : countRecords);
        logger.debug("  write RecordMaps: bytes = {} record = {} bytesPerRecord={}", countBytes, countRecords, bytesPerRecord);
      }

      if (first == null) {
        deleteOnClose = true;
        logger.error("GribCollection {}: has no files", gc.getName());
        throw new IOException("GribCollection " + gc.getName() + " has no files");
      }

      long pos = raf.getFilePointer();
      raf.seek(lenPos);
      raf.writeLong(countBytes);
      raf.seek(pos); // back to the output.

      /*
      message GribCollection {
        required string name = 1;         // must be unique - index filename is name.ncx
        required string topDir = 2;       // filenames are reletive to this
        repeated MFile mfiles = 3;        // list of grib MFiles
        repeated Dataset dataset = 4;
        repeated Gds gds = 5;             // unique Gds, shared amongst datasets

        required int32 center = 6;      // these 4 fields are to get a GribTable object
        required int32 subcenter = 7;
        required int32 master = 8;
        required int32 local = 9;       // grib1 table Version

        optional int32 genProcessType = 10;
        optional int32 genProcessId = 11;
        optional int32 backProcessId = 12;

        repeated Parameter params = 20;      // not used yet

        extensions 100 to 199;
      }
      message GribCollection {
        required string name = 1;       // must be unique - index filename is name.ncx
        required string topDir = 2;   // filenames are reletive to this
        repeated MFile mfiles = 3;    // list of grib MFiles
        repeated Group groups = 4;      // separate group for each GDS

        required int32 center = 6;      // these 4 fields are to get a GribTable object
        required int32 subcenter = 7;
        required int32 master = 8;
        required int32 local = 9;       // grib1 table Version

        optional int32 genProcessType = 10;   // why ??
        optional int32 genProcessId = 11;
        optional int32 backProcessId = 12;

        extensions 100 to 199;
      }
       */
      GribCollectionProto.GribCollection.Builder indexBuilder = GribCollectionProto.GribCollection.newBuilder();
      indexBuilder.setName(gc.getName());
      indexBuilder.setTopDir(gc.getDirectory().getPath());

      // directory and mfile list
      List<GribCollectionBuilder.GcMFile> gcmfiles = GribCollectionBuilder.makeFiles(gc.getDirectory(), files);
      for (GribCollectionBuilder.GcMFile gcmfile : gcmfiles) {
        GribCollectionProto.MFile.Builder b = GribCollectionProto.MFile.newBuilder();
        b.setFilename(gcmfile.getName());
        b.setLastModified(gcmfile.getLastModified());
        indexBuilder.addMfiles(b.build());
      }

      // twoD
      indexBuilder.addDataset(writeDatasetProto(GribCollectionProto.Dataset.Type.TwoD, groups));

      // LOOK what about just storing first ??
      Grib2SectionIdentification ids = first.getId();
      indexBuilder.setCenter(ids.getCenter_id());
      indexBuilder.setSubcenter(ids.getSubcenter_id());
      indexBuilder.setMaster(ids.getMaster_table_version());
      indexBuilder.setLocal(ids.getLocal_table_version());

      Grib2Pds pds = first.getPDS();
      indexBuilder.setGenProcessType(pds.getGenProcessType());
      indexBuilder.setGenProcessId(pds.getGenProcessId());
      indexBuilder.setBackProcessId(pds.getBackProcessId());

      GribCollectionProto.GribCollection index = indexBuilder.build();
      byte[] b = index.toByteArray();
      NcStream.writeVInt(raf, b.length); // message size
      raf.write(b);  // message  - all in one gulp
      logger.debug("  write GribCollectionIndex= {} bytes", b.length);

    } finally {
      // remove it on failure
      if (deleteOnClose && !indexFile.delete())
        logger.error(" gc2 cant deleteOnClose index file {}", indexFile.getPath());
    }

    return true;
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
  private GribCollectionProto.SparseArray writeSparseArray(Grib2Rectilyser.VariableBag vb, Set<Integer> fileSet) throws IOException {
    GribCollectionProto.SparseArray.Builder b = GribCollectionProto.SparseArray.newBuilder();
    b.setCdmHash(vb.cdmHash);
    SparseArray<Grib2Record> sa = vb.coordND.getSparseArray();
    for (int size : sa.getShape())
      b.addSize(size);
    for (int track : sa.getTrack())
      b.addTrack(track);

    for (Grib2Record gr : sa.getContent()) {
      GribCollectionProto.Record.Builder br = GribCollectionProto.Record.newBuilder();

      br.setFileno(gr.getFile());
      fileSet.add(gr.getFile());
      Grib2SectionDataRepresentation drs = gr.getDataRepresentationSection();
      br.setPos(drs.getStartingPosition());
      if (gr.isBmsReplaced()) {
        Grib2SectionBitMap bms = gr.getBitmapSection();
        br.setBmsPos(bms.getStartingPosition());
      }
      b.addRecords(br);
    }
    return b.build();
  }

    /*
  message Gds {
    optional bytes gds = 1;             // all variables in the group use the same GDS
    optional sint32 gdsHash = 2 [default = 0];
    optional string nameOverride = 3;  // only when user overrides default name
  }
   */
  private GribCollectionProto.Gds writeGdsProto(GribCollection.HorizCoordSys hcs) throws IOException {
    GribCollectionProto.Gds.Builder b = GribCollectionProto.Gds.newBuilder();

    b.setGds(ByteString.copyFrom(hcs.getRawGds()));
    b.setGdsHash(hcs.getGdsHash());
    if (hcs.getNameOverride() != null)
      b.setNameOverride(hcs.getNameOverride());

    return b.build();
  }

  /*
  message Dataset {
    required Type type = 1;
    repeated Group groups = 2;
  }
   */
  private GribCollectionProto.Dataset writeDatasetProto(GribCollection.Dataset ds) throws IOException {
    GribCollectionProto.Dataset.Builder b = GribCollectionProto.Dataset.newBuilder();

    b.setType(ds.type);

    for (GribCollection.GroupHcs group : ds.groups)
      b.addGroups(writeGroupProto(group));

    return b.build();
  }

  private GribCollectionProto.Dataset writeDatasetProto(GribCollectionProto.Dataset.Type type, List<Group> groups) throws IOException {
    GribCollectionProto.Dataset.Builder b = GribCollectionProto.Dataset.newBuilder();

    b.setType(type);

    for (Group group : groups)
      b.addGroups(writeGroupProto(group));

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

message Group {
  optional bytes gds = 1;             // all variables in the group use the same GDS
  optional sint32 gdsHash = 2 [default = 0];
  optional string nameOverride = 3;         // only when user overrides default name

  repeated Variable variables = 4;    // list of variables
  repeated Coord coords = 5;          // list of coordinates
  repeated int32 fileno = 7;          // the component files that are in this group, index into gc.files

  extensions 100 to 199;
}
}
   */
  protected GribCollectionProto.Group writeGroupProto(Group g) throws IOException {
    GribCollectionProto.Group.Builder b = GribCollectionProto.Group.newBuilder();

    b.setGdsIndex(gc.findHorizCS(g.horizCoordSys));

    for (Grib2Rectilyser.VariableBag vbag : g.rect.getGribvars()) {
      b.addVariables(writeVariableProto(vbag));
    }

    int count = 0;
    for (Coordinate coord : g.rect.getCoordinates()) {
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
        case vert:
          b.addCoords(writeCoordProto((CoordinateVert) coord));
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
message Variable {
  required uint32 discipline = 1;
  required bytes pds = 2;
  required fixed32 cdmHash = 3;

  required uint64 recordsPos = 4;  // offset of SparseArray message for this Variable
  required uint32 recordsLen = 5;  // size of SparseArray message for this Variable (could be in stream instead)

  repeated uint32 coordIdx = 6;    // index into Group.coords

  // optionally keep stats
  optional float density = 7;
  optional uint32 ndups = 8;
  optional uint32 nrecords = 9;
  optional uint32 missing = 10;

  extensions 100 to 199;
}
   */
  private GribCollectionProto.Variable writeVariableProto(Grib2Rectilyser.VariableBag vb) throws IOException {
    GribCollectionProto.Variable.Builder b = GribCollectionProto.Variable.newBuilder();

    b.setDiscipline(vb.first.getDiscipline());
    b.setPds(ByteString.copyFrom(vb.first.getPDSsection().getRawBytes()));
    b.setCdmHash(vb.cdmHash);

    b.setRecordsPos(vb.pos);
    b.setRecordsLen(vb.length);

    for (int idx : vb.coordIndex)
      b.addCoordIdx(idx);

    // keep stats
    SparseArray sa = vb.coordND.getSparseArray();
    if (sa != null) {
      b.setDensity(sa.getDensity());
      b.setNdups(sa.getNduplicates());
      b.setNrecords(sa.countNotMissing());
      b.setMissing(sa.countMissing());
    }

    if (vp.twot != null) { // only for 2D
      List<Integer> invCountList = new ArrayList<>(vp.twot.getCount().length);
      for (int count : vp.twot.getCount()) invCountList.add(count);
      b.setExtension(PartitionCollectionProto.invCount, invCountList);
    }

    if (vp.time2runtime != null) { // only for 1D
      List<Integer> list = new ArrayList<>(vp.time2runtime.length);
      for (int idx : vp.time2runtime) list.add(idx);
      b.setExtension(PartitionCollectionProto.time2Runtime, list);
    }


    return b.build();
  }

  /*
  message Coord {
    required int32 type = 1;   // Coordinate.Type.oridinal
    required int32 code = 2;   // time unit; level type
    required string unit = 3;
    repeated float values = 4;
    repeated float bound = 5; // only used if interval, then = (value, bound)
    repeated int64 msecs = 6; // calendar date
   */
  protected GribCollectionProto.Coord writeCoordProto(CoordinateRuntime coord) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setType(coord.getType().ordinal());
    b.setCode(coord.getCode());
    if (coord.getUnit() != null) b.setUnit(coord.getUnit());
    for (CalendarDate cd : coord.getRuntimesSorted()) {
      b.addMsecs(cd.getMillis());
    }
    return b.build();
  }

  protected GribCollectionProto.Coord writeCoordProto(CoordinateTime coord) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setType(coord.getType().ordinal());
    b.setCode(coord.getCode());
    if (coord.getUnit() != null) b.setUnit(coord.getUnit());
    for (Integer offset : coord.getOffsetSorted()) {
      b.addValues(offset);
    }
    return b.build();
  }

  protected GribCollectionProto.Coord writeCoordProto(CoordinateTimeIntv coord) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setType(coord.getType().ordinal());
    b.setCode(coord.getCode());
    if (coord.getUnit() != null) b.setUnit(coord.getUnit());

    // LOOK old way - do we need ?
    /*     float scale = (float) tc.getTimeUnitScale(); // deal with, eg, "6 hours" by multiplying values by 6
        if (tc.isInterval()) {
          for (TimeCoord.Tinv tinv : tc.getIntervals()) {
            b.addValues(tinv.getBounds1() * scale);
            b.addBound(tinv.getBounds2() * scale);
          } */
    for (TimeCoord.Tinv tinv : coord.getTimeIntervals()) {
      b.addValues(tinv.getBounds1());
      b.addBound(tinv.getBounds2());
    }
    return b.build();
  }

  protected GribCollectionProto.Coord writeCoordProto(CoordinateVert coord) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setType(coord.getType().ordinal());
    b.setCode(coord.getCode());

    //VertCoord.VertUnit vertUnit = Grib2Utils.getLevelUnit(coord.getCode());

    if (coord.getUnit() != null) b.setUnit(coord.getUnit());
    for (VertCoord.Level level : coord.getLevelSorted()) {
      if (coord.isLayer()) {
        b.addValues((float) level.getValue1());
        b.addBound((float) level.getValue2());
      } else {
        b.addValues((float) level.getValue1());
      }
    }
    return b.build();
  }

}
