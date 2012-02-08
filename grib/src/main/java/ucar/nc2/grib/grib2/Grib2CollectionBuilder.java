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
public class Grib2CollectionBuilder {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2CollectionBuilder.class);
  public static final String MAGIC_START = "Grib2CollectionIndex";
  protected static final int version = 10;
  private static final boolean intvMergeDefault = true;

    // called by tdm
  static public boolean update(CollectionManager dcm, Formatter f) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm);
    if (!builder.needsUpdate()) return false;
    builder.readOrCreateIndex(CollectionManager.Force.always, f);
    builder.gc.close();
    return true;
  }

  // from a single file, read in the index, create if it doesnt exist
  static public GribCollection readOrCreateIndexFromSingleFile(MFile file, CollectionManager.Force force, FeatureCollectionConfig.GribConfig config,
                                                               Formatter f) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(file, config, f);
    builder.readOrCreateIndex(force, f);
    return builder.gc;
  }

  // from a collection, read in the index, create if it doesnt exist or is out of date
  // assume that the CollectionManager is up to date, eg doesnt need to be scanned
  static public GribCollection factory(CollectionManager dcm, CollectionManager.Force force, Formatter f) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm);
    builder.readOrCreateIndex(force, f);
    return builder.gc;
  }

  // read in the index, index raf already open
  static public GribCollection createFromIndex(String name, File directory, RandomAccessFile raf, FeatureCollectionConfig.GribConfig config) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(name, directory, config);
    if (builder.readIndex(raf))
      return builder.gc;
    throw new IOException("Reading index failed");
  }

  // this writes the index always
  static public boolean writeIndexFile(File indexFile, CollectionManager dcm, Formatter f) throws IOException {
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm);
    return builder.createIndex(indexFile, f);
  }

  ////////////////////////////////////////////////////////////////

  private final List<CollectionManager> collections = new ArrayList<CollectionManager>(); // are there every more than one ?
  protected GribCollection gc;
  protected Grib2Customizer tables; // only gets created in makeAggGroups
  protected boolean isSingleFile;

  // single file
  private Grib2CollectionBuilder(MFile file, FeatureCollectionConfig.GribConfig config, Formatter f) throws IOException {
    this.isSingleFile = true;
    try {
      //String spec = StringUtil2.substitute(file.getPath(), "\\", "/");
      CollectionManager dcm = new CollectionManagerSingleFile(file);
      if (config != null) dcm.putAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG, config);
      this.collections.add(dcm);
      this.gc = new Grib2Collection(file.getName(), new File(dcm.getRoot()), config);

    } catch (Exception e) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      e.printStackTrace(new PrintStream(bos));
      f.format("%s", bos.toString());
      throw new IOException(e);
    }
  }

  private Grib2CollectionBuilder(CollectionManager dcm) {
    this.collections.add(dcm);
    FeatureCollectionConfig.GribConfig config = (FeatureCollectionConfig.GribConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    this.gc = new Grib2Collection(dcm.getCollectionName(), new File(dcm.getRoot()), config);
  }

  private Grib2CollectionBuilder(String name, File directory, FeatureCollectionConfig.GribConfig config) {
    this.gc = new Grib2Collection(name, directory, config);
  }

  protected Grib2CollectionBuilder() {
    this.gc = null;
  }

  protected int getVersion() {
    return version;
  }

  // read or create index
  private void readOrCreateIndex(CollectionManager.Force ff, Formatter f) throws IOException {

    // force new index or test for new index needed
    boolean force = ((ff == CollectionManager.Force.always) || (ff == CollectionManager.Force.test && needsUpdate()));

    // otherwise, we're good as long as the index file exists
    File idx = gc.getIndexFile();
    if (force || !idx.exists() || !readIndex(idx.getPath()) )  {
      idx = gc.makeNewIndexFile(); // make sure we have a writeable index
      logger.info("GribCollection {}: createIndex {}", gc.getName(), idx.getPath());
      createIndex(idx, f);        // write out index
      gc.setIndexRaf(new RandomAccessFile(idx.getPath(), "r"));
      readIndex(gc.getIndexRaf()); // read back in index
    }
  }

  public boolean needsUpdate() {
    File idx = gc.getIndexFile();
    return !idx.exists() || needsUpdate(idx.lastModified());
  }

  private boolean needsUpdate(long idxLastModified) {
    CollectionManager.ChangeChecker cc = GribIndex.getChangeChecker();
    for (CollectionManager dcm : collections) {
      for (MFile mfile : dcm.getFiles()) {
        if (cc.hasChangedSince(mfile, idxLastModified)) return true;
      }
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

  public boolean readIndex(RandomAccessFile raf) {
    gc.setIndexRaf(raf); // LOOK leaving the raf open in the GribCollection
    try {
      raf.order(RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);

     //// header message
      if (!NcStream.readAndTest(raf, getMagicStart().getBytes())) {
        logger.error("GribCollection {}: invalid index", gc.getName());
        return false;
      }

      gc.version = raf.readInt();
      if (gc.version != getVersion()) {
        logger.warn("GribCollection {}: index found version={}, want version= {} on file {}", new Object[]{gc.getName(), gc.version, version, raf.getLocation()});
        return false;
      }

      long skip = raf.readLong();
      raf.skipBytes(skip);

      int size = NcStream.readVInt(raf);
      if ((size < 0) || (size > 100 * 1000 * 1000)) {
        logger.warn("GribCollection {}: invalid index ", gc.getName());
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

      gc.filenames = new ArrayList<String>(proto.getFilesCount());
      for (int i = 0; i < proto.getFilesCount(); i++)
        gc.filenames.add(proto.getFiles(i));

      // error condition on a GribCollection Index
      if ((proto.getFilesCount() == 0) && !(this instanceof Grib2TimePartitionBuilder)) {
        logger.warn("GribCollection {}: has no files, force recreate ", gc.getName());
        return false;
      }

      gc.groups = new ArrayList<GribCollection.GroupHcs>(proto.getGroupsCount());
      for (int i = 0; i < proto.getGroupsCount(); i++)
        gc.groups.add(readGroup(proto.getGroups(i), gc.makeGroup()));
      Collections.sort(gc.groups);
      //int count = 0;
      //for (GribCollection.GroupHcs gh : gc.groups)
      //  gh.setId("group"+(count++));

      gc.params = new ArrayList<Parameter>(proto.getParamsCount());
      for (int i = 0; i < proto.getParamsCount(); i++)
        gc.params.add(readParam(proto.getParams(i)));

      if (!readPartitions(proto)) {
        logger.warn("TimePartition {}: has no partitions, force recreate ", gc.getName());
        return false;
      }

      return true;

    } catch (Throwable t) {
      logger.error("Error reading index " + raf.getLocation(), t);
      return false;
    }
  }

  protected boolean readPartitions(GribCollectionProto.GribCollectionIndex proto) {
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

  private boolean createIndex(File indexFile, Formatter f) throws IOException {
    long start = System.currentTimeMillis();

    ArrayList<String> filenames = new ArrayList<String>();
    List<Group> groups = makeAggregatedGroups(filenames, f);
    createIndex(indexFile, groups, filenames, f);

    long took = System.currentTimeMillis() - start;
    f.format("That took %d msecs%n", took);
    return true;
  }

  // read all records in all files,
  // divide into groups based on GDS hash
  // each group has an arraylist of all records that belong to it.
  // for each group, run rectlizer to derive the coordinates and variables
  public List<Group> makeAggregatedGroups(List<String> filenames, Formatter f) throws IOException {
    Map<Integer, Group> gdsMap = new HashMap<Integer, Group>();
    boolean intvMerge = intvMergeDefault;
    //boolean useGenType = false;

    f.format("GribCollection %s: makeAggregatedGroups%n", gc.getName());
    int fileno = 0;
    Grib2Rectilyser.Counter stats = new Grib2Rectilyser.Counter(); // debugging

    for (CollectionManager dcm : collections) {
      f.format(" dcm= %s%n", dcm);
      FeatureCollectionConfig.GribConfig config = (FeatureCollectionConfig.GribConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
      Map<Integer, Integer> gdsConvert = (config != null) ?  config.gdsHash : null;
      FeatureCollectionConfig.GribIntvFilter intvMap = (config != null) ?  config.intvFilter : null;
      intvMerge = (config == null) || (config.intvMerge == null) ? intvMergeDefault : config.intvMerge;
      //useGenType = (config == null) || (config.useGenType == null) ? false : config.useGenType;

      for (MFile mfile : dcm.getFiles()) {
        // f.format("%3d: %s%n", fileno, mfile.getPath());
        filenames.add(mfile.getPath());

        Grib2Index index = null;
        try {
          index = (Grib2Index) GribIndex.readOrCreateIndexFromSingleFile(false, !isSingleFile, mfile, config, CollectionManager.Force.test, f);

        } catch (IOException ioe) {
          logger.warn("GribCollectionBuilder {}: reading/Creating gbx9 index failed err={}", gc.getName(), ioe.getMessage());
          f.format("GribCollectionBuilder: reading/Creating gbx9 index failed err=%s%n  skipping %s%n", ioe.getMessage(), mfile.getPath() + GribIndex.IDX_EXT);
          continue;
        }

        for (Grib2Record gr : index.getRecords()) {
          if (this.tables == null) {
            Grib2SectionIdentification ids = gr.getId(); // so all records must use the same table (!)
            this.tables = Grib2Customizer.factory(ids.getCenter_id(), ids.getSubcenter_id(), ids.getMaster_table_version(), ids.getLocal_table_version());
            if (config != null) tables.setTimeUnitConverter(config.getTimeUnitConverter()); // LOOK doesnt really work with multiple collections
          }
          if (intvMap != null && filterTinv(gr, intvMap, f)) {
            stats.filter++;
            continue; // skip
          }

          gr.setFile(fileno); // each record tracks which file it belongs to
          int gdsHash = gr.getGDSsection().getGDS().hashCode();  // use GDS hash code to group records
          if (gdsConvert != null && gdsConvert.get(gdsHash) != null) // allow external config to muck with gdsHash. Why? because of error in encoding
            gdsHash = (Integer) gdsConvert.get(gdsHash);             // and we need exact hash matching

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
    }

    List<Group> result = new ArrayList<Group>(gdsMap.values());
    for (Group g : result) {
      g.rect = new Grib2Rectilyser(tables, g.records, g.gdsHash, intvMerge);
      g.rect.make(stats, filenames);
    }

    // debugging and validation
    stats.show(f);

    return result;
  }

  private boolean filterTinv(Grib2Record gr, FeatureCollectionConfig.GribIntvFilter intvFilter, Formatter f) {
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

    } else if (intvFilter.hasMap()) {
      int discipline = gr.getIs().getDiscipline();
      int category = gr.getPDS().getParameterCategory();
      int number = gr.getPDS().getParameterNumber();
      int id = (discipline << 16) + (category << 8) + number;
      Integer needLength = intvFilter.getLengthById(id);

      if (needLength != null && needLength != haveLength) {
        //f.format(" FILTER INTV [%d != %d] %s%n", haveLength, needLength, gr);
        return true;
      }
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

  private void createIndex(File indexFile, List<Group> groups, ArrayList<String> filenames, Formatter f) throws IOException {
    Grib2Record first = null; // take global metadata from here

    if (indexFile.exists()) indexFile.delete(); // replace it
    f.format(" createIndex for %s%n", indexFile.getPath());

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
      f.format("  write RecordMaps: bytes = %d record = %d bytesPerRecord=%d%n", countBytes, countRecords, bytesPerRecord);

      if (first == null) {
        logger.error("GribCollection {}: has no files\n{}", gc.getName(), f.toString());
        throw new IOException("GribCollection " + gc.getName() + " has no files");
      }

      long pos = raf.getFilePointer();
      raf.seek(lenPos);
      raf.writeLong(countBytes);
      raf.seek(pos); // back to the output.

      GribCollectionProto.GribCollectionIndex.Builder indexBuilder = GribCollectionProto.GribCollectionIndex.newBuilder();
      indexBuilder.setName(gc.getName());

      for (String fn : filenames)
        indexBuilder.addFiles(fn);

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
      f.format("  write GribCollectionIndex= %d bytes%n", b.length);

    } finally {
      f.format("  file size =  %d bytes%n", raf.length());
      if (raf != null) raf.close();
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

    if (pds.isInterval())
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
