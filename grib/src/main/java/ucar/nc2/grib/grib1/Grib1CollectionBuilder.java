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

package ucar.nc2.grib.grib1;

import com.google.protobuf.ByteString;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionManager;
import thredds.inventory.CollectionManagerSingleFile;
import thredds.inventory.MFile;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.stream.NcStream;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;

import java.io.*;
import java.util.*;

/**
 * Build a GribCollection object for Grib-1 files. Manage grib collection index.
 * Covers GribCollectionProto, which serializes and deserializes.
 * Rectilyse means to turn the collection into a multidimensional variable.
 *
 * @author caron
 * @since 4/6/11
 */
public class Grib1CollectionBuilder {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribCollection.class);
  protected static final int version = 8;
  private static final boolean intvMergeDefault = true;

  public static final String MAGIC_START = "Grib1CollectionIndex";

  // from a single file, read in the index, create if it doesnt exist or is out of date
  static public GribCollection readOrCreateIndexFromSingleFile(MFile file, CollectionManager.Force force,
                                                               FeatureCollectionConfig.GribConfig config, Formatter f) throws IOException {
    Grib1CollectionBuilder builder = new Grib1CollectionBuilder(file, config, f);
    builder.readOrCreateIndex(force, f);
    return builder.gc;
  }

  // called by tdm
  static public boolean update(CollectionManager dcm, Formatter f) throws IOException {
    Grib1CollectionBuilder builder = new Grib1CollectionBuilder(dcm);
    if (!builder.needsUpdate()) return false;
    builder.readOrCreateIndex(CollectionManager.Force.always, f);
    builder.gc.close();
    return true;
  }

  // from a collection, read in the index, create if it doesnt exist or is out of date
  // assume that the CollectionManager is up to date, eg doesnt need to be scanned
  static public GribCollection factory(CollectionManager dcm, CollectionManager.Force force, Formatter f) throws IOException {
    Grib1CollectionBuilder builder = new Grib1CollectionBuilder(dcm);
    builder.readOrCreateIndex(force, f);
    return builder.gc;
  }

  // read in the index, index raf already open
  static public GribCollection createFromIndex(String name, File directory, RandomAccessFile indexRaf, FeatureCollectionConfig.GribConfig config) throws IOException {
    Grib1CollectionBuilder builder = new Grib1CollectionBuilder(name, directory, config);
    if (builder.readIndex(indexRaf))
      return builder.gc;
    throw new IOException("Reading index failed");
  }

  // this writes the index always
  static public boolean writeIndexFile(File indexFile, CollectionManager dcm, Formatter f) throws IOException {
    Grib1CollectionBuilder builder = new Grib1CollectionBuilder(dcm);
    return builder.createIndex(indexFile, f);
  }

  ////////////////////////////////////////////////////////////////

  private final List<CollectionManager> collections = new ArrayList<CollectionManager>();
  protected GribCollection gc;
  protected Grib1Customizer cust;
  protected boolean isSingleFile;

  // single file
  private Grib1CollectionBuilder(MFile file, FeatureCollectionConfig.GribConfig config, Formatter f) throws IOException {
    this.isSingleFile = true;
    try {
      //String spec = StringUtil2.substitute(file.getPath(), "\\", "/");
      CollectionManager dcm = new CollectionManagerSingleFile(file);
      this.collections.add(dcm);
      if (config != null) dcm.putAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG, config);
      this.gc = new Grib1Collection(file.getName(), new File(dcm.getRoot()), config);

    } catch (Exception e) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      e.printStackTrace(new PrintStream(bos));
      f.format("%s", bos.toString());
      throw new IOException(e);
    }
  }

  private Grib1CollectionBuilder(CollectionManager dcm) {
    this.collections.add(dcm);
    FeatureCollectionConfig.GribConfig config = (FeatureCollectionConfig.GribConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    this.gc = new Grib1Collection(dcm.getCollectionName(), new File(dcm.getRoot()), config);
  }

  private Grib1CollectionBuilder(String name, File directory, FeatureCollectionConfig.GribConfig config) {
    this.gc = new Grib1Collection(name, directory, config);
  }

  protected Grib1CollectionBuilder() {
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
    File idx = gc.getIndexFile(); // LOOK problem - index exists but its out of date - trigger rewrite, but not writeable.
    if (force || !idx.exists() || !readIndex(idx.getPath()) )  {
      idx = gc.makeNewIndexFile(); // make sure we have a writeable index
      logger.debug("{}: createIndex {}", gc.getName(), idx.getPath());
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
        if (cc.hasChangedSince(mfile, idxLastModified))
          return true;
      }
    }
    return false;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // reading

  public boolean readIndex(String filename) throws IOException {
    return readIndex( new RandomAccessFile(filename, "r") );
  }

  /**
   * Read the index file
   * @param raf the index file
   * @return true on success
   */
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

      int v = raf.readInt();
      if (v != getVersion()) {
        logger.warn("GribCollection {}: index found version={}, want version= {} on file {}", new Object[]{gc.getName(), v, version, raf.getLocation()});
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
      if (cust == null) {
        cust = Grib1Customizer.factory(gc.center, gc.subcenter, gc.local, null); // we need this in readVertCoord()
      }

      gc.filenames = new ArrayList<String>(proto.getFilesCount());
      for (int i = 0; i < proto.getFilesCount(); i++)
        gc.filenames.add(proto.getFiles(i));

      // error condition on a GribCollection Index
      if (proto.getFilesCount() == 0) {
        logger.warn("GribCollection {}: has no files, force recreate ", gc.getName());
        return false;
      }

      gc.groups = new ArrayList<GribCollection.GroupHcs>(proto.getGroupsCount());
      for (int i = 0; i < proto.getGroupsCount(); i++)
        gc.groups.add(readGroup(proto.getGroups(i), gc.makeGroup(), gc.center));
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

  GribCollection.GroupHcs readGroup(GribCollectionProto.Group p, GribCollection.GroupHcs group, int center) throws IOException {

    byte[] rawGds = null;
    Grib1Gds gds;
    if (p.hasPredefinedGds()) {
      gds = ucar.nc2.grib.grib1.Grib1GdsPredefined.factory(center, p.getPredefinedGds());
    } else {
      rawGds = p.getGds().toByteArray();
      Grib1SectionGridDefinition gdss = new Grib1SectionGridDefinition(rawGds);
      gds = gdss.getGDS();
    }
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
      TimeCoord tc = new TimeCoord(pc.getCode(), pc.getUnit(), coords);
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
    return new VertCoord(coords, cust.getVertUnit(pc.getCode()),  isLayer);
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
    int tableVersion = pv.getTableVersion();
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

    return gc.makeVariableIndex(group, tableVersion, discipline, category, param, levelType, isLayer, intvType, intvName,
            ensDerivedType, probType, probabilityName, -1, cdmHash, timeIdx, vertIdx, ensIdx, recordsPos, recordsLen);
  }

  ///////////////////////////////////////////////////////////////////////////////////
  // writing

  private class Group {
    public Grib1SectionGridDefinition gdss;
    public int gdsHash; // may have been modified
    public Grib1Rectilyser rect;
    public List<Grib1Record> records = new ArrayList<Grib1Record>();
    public String nameOverride;
    public Set<Integer> fileSet; // this is so we can show just the component files that are in this group

    private Group(Grib1SectionGridDefinition gdss, int gdsHash) {
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
  public List<Group> makeAggregatedGroups(ArrayList<String> filenames, Formatter f) throws IOException {
    Map<Integer, Group> gdsMap = new HashMap<Integer, Group>();
    Map<Integer, Integer> gdsConvert = null;
    Grib1Rectilyser.Counter c = new Grib1Rectilyser.Counter();
    boolean intvMerge = intvMergeDefault;

    f.format("GribCollection %s: makeAggregatedGroups%n", gc.getName());
    int total = 0;
    int fileno = 0;
    for (CollectionManager dcm : collections) {
      f.format(" dcm= %s%n", dcm);
      FeatureCollectionConfig.GribConfig config = (FeatureCollectionConfig.GribConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
      if (config != null) gdsConvert = config.gdsHash;
      intvMerge = (config == null) || (config.intvMerge == null) ? intvMergeDefault : config.intvMerge;

      for (MFile mfile : dcm.getFiles()) {
        // f.format("%3d: %s%n", fileno, mfile.getPath());
        filenames.add(mfile.getPath());

        Grib1Index index = null;
        try {
          index = (Grib1Index) GribIndex.readOrCreateIndexFromSingleFile(true, !isSingleFile, mfile, config, CollectionManager.Force.test, f);

        } catch (IOException ioe) {
          logger.warn("GribCollectionBuilder {}: reading/Creating gbx9 index failed err={}", gc.getName(), ioe.getMessage());
          f.format("GribCollectionBuilder: reading/Creating gbx9 index failed err=%s%n  skipping %s%n", ioe.getMessage(), mfile.getPath() + GribIndex.IDX_EXT);
          continue;
        }

        for (Grib1Record gr : index.getRecords()) {
          gr.setFile(fileno); // each record tracks which file it belongs to
          int gdsHash = gr.getGDSsection().getGDS().hashCode();  // use GDS hash code to group records
          if (gdsConvert != null && gdsConvert.get(gdsHash) != null) // allow external config to muck with gdsHash. Why? because of error in encoding
            gdsHash = (Integer) gdsConvert.get(gdsHash);               // and we need exact hash matching
          if (cust == null)
            cust = Grib1Customizer.factory(gr, null);
          if (config != null)
            cust.setTimeUnitConverter(config.getTimeUnitConverter()); // LOOK doesnt work with multiple collections

          Group g = gdsMap.get(gdsHash);
          if (g == null) {
            g = new Group(gr.getGDSsection(), gdsHash);
            gdsMap.put(gdsHash, g);
            //g.nameOverride = setGroupNameOverride(gdsHash, gdsNamer, groupNamer, mfile);
          }
          g.records.add(gr);
          total++;
        }
        fileno++;
        c.recordsTotal += index.getRecords().size();
      }
    }
    List<Group> result = new ArrayList<Group>(gdsMap.values());
    for (Group g : result) {
      g.rect = new Grib1Rectilyser(cust, g.records, g.gdsHash, intvMerge);
      g.rect.make(c);
    }

    c.show(f);
    return result;
  }
  public String getMagicStart() {
    return MAGIC_START;
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
    Grib1Record first = null; // take global metadata from here

    if (indexFile.exists()) indexFile.delete(); // replace it
    f.format(" createIndex for %s%n", indexFile.getPath());

    RandomAccessFile raf = new RandomAccessFile(indexFile.getPath(), "rw");
    raf.order(RandomAccessFile.BIG_ENDIAN);
    try {
      //// header message
      raf.write(getMagicStart().getBytes("UTF-8"));
      raf.writeInt(version);
      long lenPos = raf.getFilePointer();
      raf.writeLong(0); // save space to write the length of the record section
      long countBytes = 0;
      int countRecords = 0;
      for (Group g : groups) {
        g.fileSet = new HashSet<Integer>();
        for (Grib1Rectilyser.VariableBag vb : g.rect.getGribvars()) {
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
      Grib1SectionProductDefinition pds = first.getPDSsection();
      indexBuilder.setCenter(pds.getCenter());
      indexBuilder.setSubcenter(pds.getSubCenter());
      indexBuilder.setLocal(pds.getTableVersion());
      indexBuilder.setMaster(0);
      indexBuilder.setGenProcessId(pds.getGenProcess());

      GribCollectionProto.GribCollectionIndex index = indexBuilder.build();
      byte[] b = index.toByteArray();
      NcStream.writeVInt(raf, b.length); // message size
      raf.write(b);  // message  - all in one gulp
      f.format("  write GribCollectionIndex= %d bytes%n", b.length);

    } finally {
      f.format("  file size =  %d bytes%n", raf.length());
      raf.close();
      if (raf != null) raf.close();
    }
  }

  private GribCollectionProto.VariableRecords writeRecordsProto(Grib1Rectilyser.VariableBag vb, Set<Integer> fileSet) throws IOException {
    GribCollectionProto.VariableRecords.Builder b = GribCollectionProto.VariableRecords.newBuilder();
    b.setCdmHash(vb.cdmHash);
    for (Grib1Rectilyser.Record ar : vb.recordMap) {
      GribCollectionProto.Record.Builder br = GribCollectionProto.Record.newBuilder();

      if (ar == null || ar.gr == null) {
        br.setFileno(0);
        br.setPos(0);
        br.setMissing(true); // missing : cant use 0 since that may be a valid value

      } else {
        br.setFileno(ar.gr.getFile());
        fileSet.add(ar.gr.getFile());
        Grib1SectionIndicator is = ar.gr.getIs();
        br.setPos(is.getStartPos()); // start of entire message
      }
      b.addRecords(br);
    }
    return b.build();
  }

  private GribCollectionProto.Group writeGroupProto(Group g) throws IOException {
    GribCollectionProto.Group.Builder b = GribCollectionProto.Group.newBuilder();

    if (g.gdss.getPredefinedGridDefinition() >= 0)
      b.setPredefinedGds(g.gdss.getPredefinedGridDefinition());
    else {
      b.setGds(ByteString.copyFrom(g.gdss.getRawBytes()));
      b.setGdsHash(g.gdsHash);
  }

    for (Grib1Rectilyser.VariableBag vb : g.rect.getGribvars())
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

  private GribCollectionProto.Variable writeVariableProto(Grib1Rectilyser rect, Grib1Rectilyser.VariableBag vb) throws IOException {
    GribCollectionProto.Variable.Builder b = GribCollectionProto.Variable.newBuilder();
    Grib1SectionProductDefinition pds = vb.first.getPDSsection();

    b.setDiscipline(0);
    b.setCategory(0);
    b.setParameter(pds.getParameterNumber());
    b.setTableVersion(pds.getTableVersion()); // can differ for variables in the same file
    b.setLevelType(pds.getLevelType());
    b.setIsLayer(cust.isLayer(pds.getLevelType())); // LOOK alternatively could store an entire PDS (one for each variable)
    b.setCdmHash(vb.cdmHash);
    b.setRecordsPos(vb.pos);
    b.setRecordsLen(vb.length);
    b.setTimeIdx(vb.timeCoordIndex);
    if (vb.vertCoordIndex >= 0)
      b.setVertIdx(vb.vertCoordIndex);
    if (vb.ensCoordIndex >= 0)
      b.setEnsIdx(vb.ensCoordIndex);

    Grib1ParamTime ptime = pds.getParamTime(cust); // LOOK could use  cust.getParamTime(pds) to not retain object
    if (ptime.isInterval()) {
      b.setIntervalType(pds.getTimeRangeIndicator());
      b.setIntvName(rect.getTimeIntervalName(vb.timeCoordIndex));
    }

    /* if (pds.isEnsembleDerived()) {
      Grib1Pds.PdsEnsembleDerived pdsDerived = (Grib1Pds.PdsEnsembleDerived) pds;
      b.setEnsDerivedType(pdsDerived.getDerivedForecastType()); // derived type (table 4.7)
    }

    if (pds.isProbability()) {
      Grib1Pds.PdsProbability pdsProb = (Grib1Pds.PdsProbability) pds;
      b.setProbabilityName(pdsProb.getProbabilityName());
      b.setProbabilityType(pdsProb.getProbabilityType());
    } */

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
    String units = (vc.getUnits() != null) ? vc.getUnits() : "";
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
