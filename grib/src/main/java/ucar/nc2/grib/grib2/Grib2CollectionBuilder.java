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

package ucar.nc2.grib.grib2;

import com.google.protobuf.ByteString;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionManager;
import thredds.inventory.CollectionManagerSingleFile;
import thredds.inventory.MFile;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.stream.NcStream;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;

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

  public static final String MAGIC_START = "Grib2CollectionIndex";
  protected static final int minVersionSingle = 11;
  protected static final int version = 12;
  private static final boolean showFiles = false;

    // called by tdm
  static public boolean update(CollectionManager dcm, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm, logger);
    if (!builder.needsUpdate()) return false;
    builder.readOrCreateIndex(CollectionManager.Force.always);
    builder.gc.close();
    return true;
  }

  // from a single file, read in the index, create if it doesnt exist
  static public GribCollection readOrCreateIndexFromSingleFile(MFile file, CollectionManager.Force force, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(file, config, logger);
    builder.readOrCreateIndex(force);
    return builder.gc;
  }

  // from a collection, read in the index, create if it doesnt exist or is out of date
  // assume that the CollectionManager is up to date, eg doesnt need to be scanned
  static public GribCollection factory(CollectionManager dcm, CollectionManager.Force force, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm, logger);
    builder.readOrCreateIndex(force);
    return builder.gc;
  }

  // read in the index, index raf already open
  static public GribCollection createFromIndex(String name, File directory, RandomAccessFile raf, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(name, directory, config, logger);
    if (builder.readIndex(raf))
      return builder.gc;
    throw new IOException("Reading index failed");
  }

  // this writes the index always
  static public boolean writeIndexFile(File indexFile, CollectionManager dcm, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm, logger);
    return builder.createIndex(indexFile);
  }

  ////////////////////////////////////////////////////////////////

  protected GribCollection gc;
  protected Grib2Customizer tables; // only gets created in makeAggGroups

  // single file
  private Grib2CollectionBuilder(MFile file, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    super(new CollectionManagerSingleFile(file, logger), true, logger);

    try {
      if (config != null) dcm.putAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG, config);
      this.gc = new Grib2Collection(file.getName(), new File(dcm.getRoot()), config);

    } catch (Exception e) {
      logger.error("Failed to index single file", e);
      throw new IOException(e);
    }
  }

  private Grib2CollectionBuilder(CollectionManager dcm, org.slf4j.Logger logger) {
    super(dcm, false, logger);
    FeatureCollectionConfig.GribConfig config = (FeatureCollectionConfig.GribConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    this.gc = new Grib2Collection(dcm.getCollectionName(), new File(dcm.getRoot()), config);
  }

  private Grib2CollectionBuilder(String name, File directory, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) {
    super(null, false, logger);
    this.gc = new Grib2Collection(name, directory, config);
  }

  protected Grib2CollectionBuilder(CollectionManager dcm, boolean isSingleFile, org.slf4j.Logger logger) {
    super(dcm, isSingleFile, logger);
  }

  // read or create index
  private void readOrCreateIndex(CollectionManager.Force ff) throws IOException {

    // force new index or test for new index needed
    boolean force = ((ff == CollectionManager.Force.always) || (ff == CollectionManager.Force.test && needsUpdate()));

    // otherwise, we're good as long as the index file exists
    File idx = gc.getIndexFile();
    if (force || !idx.exists() || !readIndex(idx.getPath()) )  {
       // write out index
       idx = gc.makeNewIndexFile(logger); // make sure we have a writeable index
       logger.info("{}: createIndex {}", gc.getName(), idx.getPath());
       createIndex(idx);

       // read back in index
       RandomAccessFile indexRaf = new RandomAccessFile(idx.getPath(), "r");
       gc.setIndexRaf(indexRaf);
       readIndex(indexRaf);
     }
  }

  public boolean needsUpdate() {
    if (dcm == null) return false;
    File idx = gc.getIndexFile();
    return !idx.exists() || needsUpdate(idx.lastModified());
  }

  private boolean needsUpdate(long idxLastModified) {
    CollectionManager.ChangeChecker cc = GribIndex.getChangeChecker();
    for (MFile mfile : dcm.getFiles()) {
      if (cc.hasChangedSince(mfile, idxLastModified)) return true;
    }
    return false;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // reading

  public String getMagicStart() {
    return MAGIC_START;
  }

  public boolean readIndex(String filename) throws IOException {
    return readIndex( new RandomAccessFile(filename, "r") );
  }

  protected boolean readIndex(RandomAccessFile raf) {
    gc.setIndexRaf(raf); // LOOK leaving the raf open in the GribCollection
    try {
      raf.order(RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);

     //// header message
      if (!NcStream.readAndTest(raf, getMagicStart().getBytes())) {
        logger.error("Grib2Collection {}: invalid index", gc.getName());
        return false;
      }

      gc.version = raf.readInt();
      boolean versionOk = isSingleFile ? gc.version >= minVersionSingle : gc.version == version;
      if (!versionOk) {
        logger.warn("Grib2Collection {}: index found version={}, want version= {} on file {}", gc.getName(), gc.version, version, raf.getLocation());
        return false;
      }

      long skip = raf.readLong();
      raf.skipBytes(skip);

      int size = NcStream.readVInt(raf);
      if ((size < 0) || (size > 100 * 1000 * 1000)) {
        logger.warn("Grib2Collection {}: invalid index ", gc.getName());
        return false;
      }

      byte[] m = new byte[size];
      raf.readFully(m);

      GribCollectionProto.GribCollectionIndex proto = GribCollectionProto.GribCollectionIndex.parseFrom(m);

      gc.center = proto.getCenter();
      gc.subcenter = proto.getSubcenter();
      gc.master = proto.getMaster();
      gc.local = proto.getLocal();
      gc.genProcessType = proto.getGenProcessType();
      gc.genProcessId = proto.getGenProcessId();
      gc.backProcessId = proto.getBackProcessId();
      gc.local = proto.getLocal();
      // gc.tables = Grib2Tables.factory(gc.center, gc.subcenter, gc.master, gc.local);

      File dir = gc.getDirectory();
      String dirname = proto.getDirName();
      if (dir != null && !dir.getPath().equals(dirname)) {
        logger.debug("Grib2Collection {}: has different directory= {} than index= {} ", gc.getName(), dir.getPath(), dirname);
        //return false;
      }

      // switch from files to mfiles in version 12
      if (!(this instanceof Grib2TimePartitionBuilder)) {
        if (gc.version < 12) {
          int n = proto.getFilesCount();
          if (n == 0) {
            logger.warn("Grib2Collection {}: has no files, force recreate ", gc.getName());
            return false;
          } else {
            List<MFile> files = new ArrayList<MFile>(proto.getFilesCount());
            for (int i = 0; i < n; i++)
              files.add(new GribCollectionBuilder.GcMFile(dir, proto.getFiles(i), -1));
            gc.setFiles(files);
            if (dcm != null) dcm.setFiles(files);
          }

        } else {
          int n = proto.getMfilesCount();
          if (n == 0) {
            logger.warn("Grib2Collection {}: has no files, force recreate ", gc.getName());
            return false;
          } else {
            List<MFile> files = new ArrayList<MFile>(n);
            for (int i = 0; i < n; i++)
              files.add(new GribCollectionBuilder.GcMFile(dir, proto.getMfiles(i)));
            gc.setFiles(files);
            if (dcm != null) dcm.setFiles(files);
          }
        }
      }

      gc.groups = new ArrayList<GribCollection.GroupHcs>(proto.getGroupsCount());
      for (int i = 0; i < proto.getGroupsCount(); i++)
        gc.groups.add(readGroup(proto.getGroups(i), gc.makeGroup()));
      gc.groups = Collections.unmodifiableList(gc.groups);

      gc.params = new ArrayList<Parameter>(proto.getParamsCount());
      for (int i = 0; i < proto.getParamsCount(); i++)
        gc.params.add(readParam(proto.getParams(i)));

      if (!readPartitions(proto, dirname)) {
        logger.warn("Time2Partition {}: has no partitions, force recreate ", gc.getName());
        return false;
      }

      return true;

    } catch (Throwable t) {
      logger.error("Error reading index " + raf.getLocation(), t);
      return false;
    }
  }

  protected boolean readPartitions(GribCollectionProto.GribCollectionIndex proto, String dirname) {
    return true;
  }

  protected void readTimePartitions(GribCollection.GroupHcs group, GribCollectionProto.Group proto) {
    // NOOP
  }

  GribCollection.GroupHcs readGroup(GribCollectionProto.Group p, GribCollection.GroupHcs group) throws IOException {

    byte[] rawGds = p.getGds().toByteArray();
    Grib2SectionGridDefinition gdss = new Grib2SectionGridDefinition(rawGds);
    Grib2Gds gds = gdss.getGDS();
    int gdsHash = (p.getGdsHash() != 0) ? p.getGdsHash() : gds.hashCode();
    group.setHorizCoordSystem(gds.makeHorizCoordSys(), rawGds, gdsHash);

    group.varIndex = new ArrayList<GribCollection.VariableIndex>();
    for (int i = 0; i < p.getVariablesCount(); i++)
      group.varIndex.add(readVariable(p.getVariables(i), group));
    Collections.sort(group.varIndex);

    group.timeCoords = new ArrayList<TimeCoord>(p.getTimeCoordsCount());
    for (int i = 0; i < p.getTimeCoordsCount(); i++)
      group.timeCoords.add(readTimeCoord(p.getTimeCoords(i)));

    group.vertCoords = new ArrayList<VertCoord>(p.getVertCoordsCount());
    for (int i = 0; i < p.getVertCoordsCount(); i++)
      group.vertCoords.add(readVertCoord(p.getVertCoords(i)));

    group.ensCoords = new ArrayList<EnsCoord>(p.getEnsCoordsCount());
    for (int i = 0; i < p.getEnsCoordsCount(); i++)
      group.ensCoords.add(readEnsCoord(p.getEnsCoords(i)));

    group.filenose = new int[p.getFilenoCount()];
    for (int i = 0; i < p.getFilenoCount(); i++)
      group.filenose[i] = p.getFileno(i);

    readTimePartitions(group, p);

    // finish
    for (GribCollection.VariableIndex vi : group.varIndex) {
      TimeCoord tc = group.timeCoords.get(vi.timeIdx);
      vi.ntimes = tc.getSize();
      VertCoord vc = (vi.vertIdx < 0) ? null : group.vertCoords.get(vi.vertIdx);
      vi.nverts = (vc == null) ? 0 : vc.getSize();
      EnsCoord ec = (vi.ensIdx < 0) ? null : group.ensCoords.get(vi.ensIdx);
      vi.nens = (ec == null) ? 0 : ec.getSize();
    }

    // group.assignVertNames();

    return group;
  }

  private Parameter readParam(GribCollectionProto.Parameter pp) throws IOException {
    if (pp.hasSdata())
      return new Parameter(pp.getName(), pp.getSdata());

    int count = 0;
    double[] vals = new double[pp.getDataCount()];
    for (double val : pp.getDataList())
      vals[count++] = val;

    return new Parameter(pp.getName(), vals);
  }

  private TimeCoord readTimeCoord(GribCollectionProto.Coord pc) throws IOException {
    if (pc.getBoundCount() > 0) {  // its an interval
      List<TimeCoord.Tinv> coords = new ArrayList<TimeCoord.Tinv>(pc.getValuesCount());
      for (int i = 0; i < pc.getValuesCount(); i++)
        coords.add(new TimeCoord.Tinv((int) pc.getValues(i), (int) pc.getBound(i)));
      TimeCoord tc =  new TimeCoord(pc.getCode(), pc.getUnit(), coords);
      return tc.setIndex( pc.getIndex());
    } else {
      List<Integer> coords = new ArrayList<Integer>(pc.getValuesCount());
      for (float value : pc.getValuesList())
        coords.add((int) value);
      TimeCoord tc =  new TimeCoord(pc.getCode(), pc.getUnit(), coords);
      return tc.setIndex( pc.getIndex());
    }
  }

  private VertCoord readVertCoord(GribCollectionProto.Coord pc) throws IOException {
    boolean isLayer = (pc.getBoundCount() > 0);
    List<VertCoord.Level> coords = new ArrayList<VertCoord.Level>(pc.getValuesCount());
    for (int i = 0; i < pc.getValuesCount(); i++)
      coords.add(new VertCoord.Level(pc.getValues(i), isLayer ? pc.getBound(i) : 0));
    return new VertCoord(pc.getCode(), coords, isLayer);
  }

  private EnsCoord readEnsCoord(GribCollectionProto.Coord pc) throws IOException {
    List<EnsCoord.Coord> coords = new ArrayList<EnsCoord.Coord>(pc.getValuesCount());
    for (int i = 0; i < pc.getValuesCount(); i += 2)
      coords.add(new EnsCoord.Coord((int) pc.getValues(i), (int) pc.getValues(i + 1)));
    return new EnsCoord(coords);
  }

  protected GribCollection.VariableIndex readVariable(GribCollectionProto.Variable pv, GribCollection.GroupHcs group) {
    int discipline = pv.getDiscipline();
    int category = pv.getCategory();
    int param = pv.getParameter();
    int levelType = pv.getLevelType();
    int intvType = pv.getIntervalType();
    String intvName = pv.getIntvName();
    boolean isLayer = pv.getIsLayer();
    int ensDerivedType = pv.getEnsDerivedType();
    int probType = pv.getProbabilityType();
    String probabilityName = pv.getProbabilityName();
    int cdmHash = pv.getCdmHash();
    long recordsPos = pv.getRecordsPos();
    int recordsLen = pv.getRecordsLen();
    int timeIdx = pv.getTimeIdx();
    int vertIdx = pv.getVertIdx();
    int ensIdx = pv.getEnsIdx();
    int tableVersion = pv.getTableVersion();
    int genProcessType = pv.getGenProcessType();

    return gc.makeVariableIndex(group, tableVersion, discipline, category, param, levelType, isLayer, intvType, intvName,
            ensDerivedType, probType, probabilityName, genProcessType, cdmHash, timeIdx, vertIdx, ensIdx, recordsPos, recordsLen);
  }

  ///////////////////////////////////////////////////////////////////////////////////
  // writing

  private class Group {
    public Grib2SectionGridDefinition gdss;
    public int gdsHash; // may have been modified
    public Grib2Rectilyser rect;
    public List<Grib2Record> records = new ArrayList<Grib2Record>();
    public String nameOverride;
    public Set<Integer> fileSet; // this is so we can show just the component files that are in this group

    private Group(Grib2SectionGridDefinition gdss, int gdsHash) {
      this.gdss = gdss;
      this.gdsHash = gdsHash;
    }
  }

  ///////////////////////////////////////////////////
  // create the index

  private boolean createIndex(File indexFile) throws IOException {
    if (dcm == null) {
      logger.error("Grib2CollectionBuilder "+gc.getName()+" : cannot create new index ");
      throw new IllegalStateException();
    }

    long start = System.currentTimeMillis();

    ArrayList<MFile> files = new ArrayList<MFile>();
    List<Group> groups = makeAggregatedGroups(files);
    createIndex(indexFile, groups, files);

    long took = System.currentTimeMillis() - start;
    logger.debug("That took {} msecs", took);
    return true;
  }

  // read all records in all files,
  // divide into groups based on GDS hash
  // each group has an arraylist of all records that belong to it.
  // for each group, run rectlizer to derive the coordinates and variables
  public List<Group> makeAggregatedGroups(List<MFile> files) throws IOException {
    Map<Integer, Group> gdsMap = new HashMap<Integer, Group>();
    Map<String, Boolean> pdsConvert = null;

    //boolean intvMerge = intvMergeDefault;
    //boolean useGenType = false;

    logger.debug("GribCollection {}: makeAggregatedGroups", gc.getName());
    int fileno = 0;
    Grib2Rectilyser.Counter stats = new Grib2Rectilyser.Counter(); // debugging

    logger.debug(" dcm={}", dcm);
    FeatureCollectionConfig.GribConfig config = (FeatureCollectionConfig.GribConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    Map<Integer, Integer> gdsConvert = (config != null) ?  config.gdsHash : null;
    FeatureCollectionConfig.GribIntvFilter intvMap = (config != null) ?  config.intvFilter : null;
    if (config != null) pdsConvert = config.pdsHash;
    //intvMerge = (config == null) || (config.intvMerge == null) ? intvMergeDefault : config.intvMerge;
    //useGenType = (config == null) || (config.useGenType == null) ? false : config.useGenType;

    for (MFile mfile : dcm.getFiles()) {
      if (showFiles) logger.debug("{}: {}", fileno, mfile.getPath());

      Grib2Index index = null;
      try {
        index = (Grib2Index) GribIndex.readOrCreateIndexFromSingleFile(false, !isSingleFile, mfile, config, CollectionManager.Force.test, logger);
        files.add(mfile);  // add on success

      } catch (IOException ioe) {
        logger.error("Grib2CollectionBuilder "+gc.getName()+" : reading/Creating gbx9 index for file "+ mfile.getPath()+" failed", ioe);
        continue;
      }

      for (Grib2Record gr : index.getRecords()) {
        if (this.tables == null) {
          Grib2SectionIdentification ids = gr.getId(); // so all records must use the same table (!)
          this.tables = Grib2Customizer.factory(ids.getCenter_id(), ids.getSubcenter_id(), ids.getMaster_table_version(), ids.getLocal_table_version());
          if (config != null) tables.setTimeUnitConverter(config.getTimeUnitConverter());
        }
        if (intvMap != null && filterOut(gr, intvMap)) {
          stats.filter++;
          continue; // skip
        }

        gr.setFile(fileno); // each record tracks which file it belongs to
        int gdsHash = gr.getGDSsection().getGDS().hashCode();  // use GDS hash code to group records
        if (gdsConvert != null && gdsConvert.get(gdsHash) != null) // allow external config to muck with gdsHash. Why? because of error in encoding
          gdsHash = gdsConvert.get(gdsHash);             // and we need exact hash matching

        Group g = gdsMap.get(gdsHash);
        if (g == null) {
          g = new Group(gr.getGDSsection(), gdsHash);
          gdsMap.put(gdsHash, g);
        }
        g.records.add(gr);
      }
      fileno++;
        stats.recordsTotal += index.getRecords().size();
    }

    List<Group> result = new ArrayList<Group>(gdsMap.values());
    for (Group g : result) {
      g.rect = new Grib2Rectilyser(tables, g.records, g.gdsHash, pdsConvert);
      g.rect.make(stats, files);
    }

    // debugging and validation
    if (logger.isDebugEnabled()) logger.debug(stats.show());

    return result;
  }

  // true means remove
  private boolean filterOut(Grib2Record gr, FeatureCollectionConfig.GribIntvFilter intvFilter) {
    int[] intv = tables.getForecastTimeIntervalOffset(gr);
    if (intv == null) return false;
    int haveLength = intv[1] - intv[0];

    // HACK
    if (haveLength == 0 && intvFilter.isZeroExcluded()) {  // discard 0,0
      if ((intv[0] == 0) && (intv[1] == 0)) {
        //f.format(" FILTER INTV [0, 0] %s%n", gr);
        return true;
      }
      return false;

    } else if (intvFilter.hasFilter()) {
      int discipline = gr.getIs().getDiscipline();
      Grib2Pds pds = gr.getPDS();
      int category = pds.getParameterCategory();
      int number = pds.getParameterNumber();
      int id = (discipline << 16) + (category << 8) + number;

      int prob = Integer.MIN_VALUE;
      if (pds.isProbability()) {
        prob = (int) (1000 * pds.getProbabilityUpperLimit());
      }
      return intvFilter.filterOut(id, haveLength, prob);
    }
    return false;
  }

  /*
   MAGIC_START
   version
   sizeRecords
   VariableRecords (sizeRecords bytes)
   sizeIndex
   GribCollectionIndex (sizeIndex bytes)
   */

  private void createIndex(File indexFile, List<Group> groups, List<MFile> files) throws IOException {
    Grib2Record first = null; // take global metadata from here
    boolean deleteOnClose = false;

    if (indexFile.exists()) {
      if (!indexFile.delete()) {
        logger.error("gc2 cant delete index file {}", indexFile.getPath());
      }
    }
    logger.debug(" createIndex for {}", indexFile.getPath());

    RandomAccessFile raf = new RandomAccessFile(indexFile.getPath(), "rw");
    raf.order(RandomAccessFile.BIG_ENDIAN);
    try {
      //// header message
      raf.write(MAGIC_START.getBytes("UTF-8"));
      raf.writeInt(version);
      long lenPos = raf.getFilePointer();
      raf.writeLong(0); // save space to write the length of the record section
      long countBytes = 0;
      int countRecords = 0;
      for (Group g : groups) {
        g.fileSet = new HashSet<Integer>();
        for (Grib2Rectilyser.VariableBag vb : g.rect.getGribvars()) {
          if (first == null) first = vb.first;
          GribCollectionProto.VariableRecords vr = writeRecordsProto(vb, g.fileSet);
          byte[] b = vr.toByteArray();
          vb.pos = raf.getFilePointer();
          vb.length = b.length;
          raf.write(b);
          countBytes += b.length;
          countRecords += vb.recordMap.length;
        }
      }
      long bytesPerRecord = countBytes / ((countRecords == 0) ? 1 : countRecords);
      if (logger.isDebugEnabled()) logger.debug("  write RecordMaps: bytes = {} record = {} bytesPerRecord={}", new Object[] {countBytes, countRecords, bytesPerRecord});

      if (first == null) {
        deleteOnClose = true;
        logger.error("GribCollection {}: has no files", gc.getName());
        throw new IOException("GribCollection " + gc.getName() + " has no files");
      }

      long pos = raf.getFilePointer();
      raf.seek(lenPos);
      raf.writeLong(countBytes);
      raf.seek(pos); // back to the output.

      GribCollectionProto.GribCollectionIndex.Builder indexBuilder = GribCollectionProto.GribCollectionIndex.newBuilder();
      indexBuilder.setName(gc.getName());
      indexBuilder.setDirName(gc.getDirectory().getPath());

      // directory and mfile list
      indexBuilder.setDirName(gc.getDirectory().getPath());
      List<GribCollectionBuilder.GcMFile> gcmfiles = GribCollectionBuilder.makeFiles(gc.getDirectory(), files);
      for (GribCollectionBuilder.GcMFile gcmfile : gcmfiles) {
        indexBuilder.addMfiles(gcmfile.makeProto());
      }

      for (Group g : groups)
        indexBuilder.addGroups(writeGroupProto(g));

      /* int count = 0;
      for (DatasetCollectionManager dcm : collections) {
        indexBuilder.addParams(makeParamProto(new Parameter("spec" + count, dcm.())));
        count++;
      } */

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

      GribCollectionProto.GribCollectionIndex index = indexBuilder.build();
      byte[] b = index.toByteArray();
      NcStream.writeVInt(raf, b.length); // message size
      raf.write(b);  // message  - all in one gulp
      logger.debug("  write GribCollectionIndex= {} bytes", b.length);

    } finally {
      logger.debug("  file size =  {} bytes", raf.length());
      if (raf != null) raf.close();

            // remove it on failure
      if (deleteOnClose && !indexFile.delete())
        logger.error(" gc2 cant deleteOnClose index file {}", indexFile.getPath());
    }
  }

  /* private void createIndexForGroup(Group group, ArrayList<String> filenames) throws IOException {
    Grib2Record first = null; // take global metadata from here

    File file = new File(gc.getDirectory(), group.name + GribCollection.IDX_EXT);
    if (file.exists()) file.delete(); // replace it

    RandomAccessFile raf = new RandomAccessFile(file.getPath(), "rw");
    raf.order(RandomAccessFile.BIG_ENDIAN);
    try {
      //// header message
      String magic = gc.getMagicBytes();
      raf.write(magic.getBytes("UTF-8"));
      raf.writeInt(version);
      long lenPos = raf.getFilePointer();
      raf.writeLong(0); // save space to write the length of the record section
      long countBytes = 0;
      int countRecords = 0;
      group.fileSet = new HashSet<Integer>();

      for (Rectilyser.VariableBag vb : group.rect.getGribvars()) {
        if (first == null) first = vb.first;
        GribCollectionProto.VariableRecords vr = makeRecordsProto(vb, group.fileSet);
        byte[] b = vr.toByteArray();
        vb.pos = raf.getFilePointer();
        vb.length = b.length;
        raf.write(b);
        countBytes += b.length;
      }
      countRecords += group.records.size();
      if (countRecords == 0) countRecords = 1;
      long bytesPerRecord = countBytes / countRecords;
      logger.debug("VariableRecords: bytes = {} record = {} bytesPerRecord={}", new Object[] {countBytes, countRecords, bytesPerRecord});

      long pos = raf.getFilePointer();
      raf.seek(lenPos);
      raf.writeLong(countBytes);
      raf.seek(pos); // back to the output.

      GribCollectionProto.GribCollectionIndex.Builder indexBuilder = GribCollectionProto.GribCollectionIndex.newBuilder();
      indexBuilder.setName(group.name);

      for (String fn : filenames)
        indexBuilder.addFiles(fn);

      indexBuilder.addGroups(makeGroupProto(group));

      int count = 0;
      for (CollectionManager dcm : collections) {
        indexBuilder.addParams(makeParamProto(new Parameter("spec" + count, dcm.toString())));
        count++;
      }

      Grib2SectionIdentification ids = first.getId();
      indexBuilder.setCenter(ids.getCenter_id());
      indexBuilder.setSubcenter(ids.getSubcenter_id());
      indexBuilder.setMaster(ids.getMaster_table_version());
      indexBuilder.setLocal(ids.getLocal_table_version());

      GribCollectionProto.GribCollectionIndex index = indexBuilder.build();
      byte[] b = index.toByteArray();
      NcStream.writeVInt(raf, b.length); // message size
      raf.write(b);  // message  - all in one gulp
      logger.debug("GribCollectionIndex= {} bytes%n", b.length);

    } finally {
      logger.debug("file size =  {} bytes%n", raf.length());
      raf.close();
      if (raf != null) raf.close();
    }
  } */

  private GribCollectionProto.VariableRecords writeRecordsProto(Grib2Rectilyser.VariableBag vb, Set<Integer> fileSet) throws IOException {
    GribCollectionProto.VariableRecords.Builder b = GribCollectionProto.VariableRecords.newBuilder();
    b.setCdmHash(vb.cdmHash);

    for (Grib2Rectilyser.Record ar : vb.recordMap) {
      GribCollectionProto.Record.Builder br = GribCollectionProto.Record.newBuilder();

      if (ar == null || ar.gr == null) {
        br.setFileno(0);
        br.setPos(0); // missing : ok to use 0 since drsPos > 0

      } else {
        br.setFileno(ar.gr.getFile());
        fileSet.add(ar.gr.getFile());
        Grib2SectionDataRepresentation drs = ar.gr.getDataRepresentationSection();
        br.setPos(drs.getStartingPosition());
        if (ar.gr.isBmsReplaced()) {
          Grib2SectionBitMap bms = ar.gr.getBitmapSection();
          br.setBmsPos(bms.getStartingPosition());
        }
      }
      b.addRecords(br);
    }
    return b.build();
  }

  private GribCollectionProto.Group writeGroupProto(Group g) throws IOException {
    GribCollectionProto.Group.Builder b = GribCollectionProto.Group.newBuilder();

    b.setGds(ByteString.copyFrom(g.gdss.getRawBytes()));
    b.setGdsHash(g.gdsHash);

    for (Grib2Rectilyser.VariableBag vb : g.rect.getGribvars())
      b.addVariables(writeVariableProto(g.rect, vb));

    List<TimeCoord> timeCoords = g.rect.getTimeCoords();
    for (int i = 0; i < timeCoords.size(); i++)
      b.addTimeCoords(writeCoordProto(timeCoords.get(i), i));

    List<VertCoord> vertCoords = g.rect.getVertCoords();
    for (int i = 0; i < vertCoords.size(); i++)
      b.addVertCoords(writeCoordProto(vertCoords.get(i), i));

    List<EnsCoord> ensCoords = g.rect.getEnsCoords();
    for (int i = 0; i < ensCoords.size(); i++)
      b.addEnsCoords(writeCoordProto(ensCoords.get(i), i));

    for (Integer aFileSet : g.fileSet)
      b.addFileno(aFileSet);

    if (g.nameOverride != null)
      b.setName(g.nameOverride);

    return b.build();
  }

  private GribCollectionProto.Variable writeVariableProto(Grib2Rectilyser rect, Grib2Rectilyser.VariableBag vb) throws IOException {
    GribCollectionProto.Variable.Builder b = GribCollectionProto.Variable.newBuilder();

    b.setDiscipline(vb.first.getDiscipline());
    Grib2Pds pds = vb.first.getPDS();
    b.setCategory(pds.getParameterCategory());
    b.setParameter(pds.getParameterNumber());
    b.setLevelType(pds.getLevelType1());
    b.setIsLayer(Grib2Utils.isLayer(vb.first));
    b.setIntervalType(pds.getStatisticalProcessType());
    b.setCdmHash(vb.cdmHash);

    b.setRecordsPos(vb.pos);
    b.setRecordsLen(vb.length);
    b.setTimeIdx(vb.timeCoordIndex);
    if (vb.vertCoordIndex >= 0)
      b.setVertIdx(vb.vertCoordIndex);
    if (vb.ensCoordIndex >= 0)
      b.setEnsIdx(vb.ensCoordIndex);

    if (pds.isEnsembleDerived()) {
      Grib2Pds.PdsEnsembleDerived pdsDerived = (Grib2Pds.PdsEnsembleDerived) pds;
      b.setEnsDerivedType(pdsDerived.getDerivedForecastType()); // derived type (table 4.7)
    }

    if (pds.isProbability()) {
      Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds;
      b.setProbabilityName(pdsProb.getProbabilityName());
      b.setProbabilityType(pdsProb.getProbabilityType());
    }

    if (pds.isTimeInterval())
      b.setIntvName(rect.getTimeIntervalName(vb.timeCoordIndex));

    int genType = pds.getGenProcessType();
    if (genType != GribNumbers.UNDEFINED)
      b.setGenProcessType(pds.getGenProcessType());

    return b.build();
  }

  protected GribCollectionProto.Parameter writeParamProto(Parameter param) throws IOException {
    GribCollectionProto.Parameter.Builder b = GribCollectionProto.Parameter.newBuilder();

    b.setName(param.getName());
    if (param.isString())
      b.setSdata(param.getStringValue());
    else {
      for (int i = 0; i < param.getLength(); i++)
        b.addData(param.getNumericValue(i));
    }

    return b.build();
  }

  protected GribCollectionProto.Coord writeCoordProto(TimeCoord tc, int index) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setIndex(index);
    b.setCode(tc.getCode());
    b.setUnit(tc.getUnits());
    float scale = (float) tc.getTimeUnitScale(); // deal with, eg, "6 hours" by multiplying values by 6
    if (tc.isInterval()) {
      for (TimeCoord.Tinv tinv : tc.getIntervals()) {
        b.addValues(tinv.getBounds1() * scale);
        b.addBound(tinv.getBounds2() * scale);
      }
    } else {
      for (int value : tc.getCoords())
        b.addValues(value * scale);
    }
    return b.build();
  }

  protected GribCollectionProto.Coord writeCoordProto(VertCoord vc, int index) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setIndex(index);
    b.setCode(vc.getCode());
    String units = vc.getUnits();
    if (units == null) units = "";
    b.setUnit(units);
    for (VertCoord.Level coord : vc.getCoords()) {
      if (vc.isLayer()) {
        b.addValues((float) coord.getValue1());
        b.addBound((float) coord.getValue2());
      } else {
        b.addValues((float) coord.getValue1());
      }
    }
    return b.build();
  }

  protected GribCollectionProto.Coord writeCoordProto(EnsCoord ec, int index) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setIndex(index);
    b.setCode(0);
    b.setUnit("");
    for (EnsCoord.Coord coord : ec.getCoords()) {
      b.addValues((float) coord.getCode());
      b.addValues((float) coord.getEnsMember());
    }
    return b.build();
  }

}
