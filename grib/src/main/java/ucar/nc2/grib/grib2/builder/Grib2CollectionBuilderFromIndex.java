package ucar.nc2.grib.grib2.builder;

import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.catalog.parser.jdom.FeatureCollectionReader;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionManagerRO;
import thredds.inventory.MFile;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib2.Grib2Collection;
import ucar.nc2.grib.grib2.Grib2Gds;
import ucar.nc2.grib.grib2.Grib2SectionGridDefinition;
import ucar.nc2.grib.grib2.Grib2TimePartition;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.stream.NcStream;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Build a GribCollection object for Grib-2 files. Only from ncx files.
 * No updating, no nuthin.
 *
 * @author caron
 * @since 11/9/13
 */
public class Grib2CollectionBuilderFromIndex extends GribCollectionBuilder {

  protected static final int minVersionSingle = 11;
  protected static final int version = 12;
  private static final boolean showFiles = false;

  // read in the index, index raf already open
  static public GribCollection createFromIndex(String name, File directory, RandomAccessFile raf, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    Grib2CollectionBuilderFromIndex builder = new Grib2CollectionBuilderFromIndex(name, directory, config, logger);
    if (builder.readIndex(raf))
      return builder.gc;
    throw new IOException("Reading index failed"); // or return null ??
  }

  // read in the index, open raf
  static public GribCollection createFromIndex(String name, File directory, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    File idxFile = GribCollection.getIndexFile(name, directory);
    RandomAccessFile raf = new RandomAccessFile(idxFile.getPath(), "r");
    return createFromIndex(name, directory, raf, config, logger);
  }

  ////////////////////////////////////////////////////////////////

  protected GribCollection gc;
  protected Grib2Customizer tables; // only gets created in makeAggGroups

  protected Grib2CollectionBuilderFromIndex(String name, File directory, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) {
    super(null, false, logger);
    this.gc = new Grib2Collection(name, directory, config);
  }

  protected Grib2CollectionBuilderFromIndex(CollectionManagerRO dcm, boolean isSingleFile, org.slf4j.Logger logger) {
    super(dcm, isSingleFile, logger);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // reading

  protected String getMagicStart() {
    return Grib2CollectionBuilder.MAGIC_START;
  }

  protected boolean readIndex(RandomAccessFile raf) {
    System.out.printf("Grib2CollectionBuilderFromIndex readIndex %s%n", raf.getLocation());
    long start = System.currentTimeMillis();

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
      boolean versionOk = isSingleFile ? gc.version >= minVersionSingle : gc.version >= version;
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
      File protoDir = new File(proto.getDirName());
      if (dir != null && protoDir != null && !dir.getCanonicalPath().equals(protoDir.getCanonicalPath())) {
        logger.info("Grib2Collection {}: has different directory= {} than index= {} ", gc.getName(), dir.getCanonicalPath(), protoDir.getCanonicalPath());
        //return false;
      }
      gc.setDirectory(new File(proto.getDirName()));

      // see if its a partition
      boolean isPartition = proto.getPartitionsCount() > 0;

      // switch from files to mfiles in version 12
      if (!isPartition) {
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
            //if (dcm != null) dcm.setFiles(files); // LOOK !!
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
            //if (dcm != null) dcm.setFiles(files);  // LOOK !!
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

      if (!readPartitions(proto, proto.getDirName())) {
        logger.warn("Time2Partition {}: has no partitions, force recreate ", gc.getName());
        return false;
      }

      // do it
      long took = System.currentTimeMillis() - start;
      System.out.printf("  that took %s msecs%n", took);
      return true;

    } catch (Throwable t) {
      logger.error("Error reading index " + raf.getLocation(), t);
      t.printStackTrace();
      return false;
    }


  }

  protected boolean readPartitions(GribCollectionProto.GribCollectionIndex proto, String dirname) {
    return true;
  }

  protected void readTimePartitions(GribCollection.GroupHcs group, GribCollectionProto.Group proto) {
    // NOOP
  }

  int groupno = 0;

  GribCollection.GroupHcs readGroup(GribCollectionProto.Group p, GribCollection.GroupHcs group) throws IOException {
    //System.out.printf("Grib2CollectionBuilderFromIndex readGroup %d%n", groupno++);

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
      return tc.setIndex(pc.getIndex());
    } else {
      List<Integer> coords = new ArrayList<Integer>(pc.getValuesCount());
      for (float value : pc.getValuesList())
        coords.add((int) value);
      TimeCoord tc = new TimeCoord(pc.getCode(), pc.getUnit(), coords);
      return tc.setIndex(pc.getIndex());
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

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////
  static private final Logger logger = LoggerFactory.getLogger(Grib2CollectionBuilderFromIndex.class);

  private static GribCollection doOne(File dir, String filename, FeatureCollectionConfig config) throws IOException {
    long start = System.currentTimeMillis();
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    GribCollection gc = Grib2CollectionBuilderFromIndex.createFromIndex("test", dir, raf, config.gribConfig, logger);
    long took = System.currentTimeMillis() - start;
    System.out.printf("that took %s msecs%n", took);
    return gc;
  }

  private static Grib2TimePartition doOnePart(File dir, String filename, FeatureCollectionConfig config) throws IOException {
    long start = System.currentTimeMillis();
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    Grib2TimePartition tp = Grib2TimePartitionBuilderFromIndex.createFromIndex("test", dir, raf, logger);  // LOOK why no config ??
    //GribCollection gc = Grib2TimePartitionBuilderFromIndex.createFromIndex("test", dir, raf, config.gribConfig, logger);
    long took = System.currentTimeMillis() - start;
    System.out.printf("that took %s msecs%n", took);
    return tp;
  }

  public static void main(String[] args) throws IOException {

    File cat = new File("B:/ndfd/catalog.xml");
    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(cat);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    long start = System.currentTimeMillis();

    File dir = new File("B:/ndfd/200901/");
    FeatureCollectionConfig config = FeatureCollectionReader.readFeatureCollection(doc.getRootElement());

    Grib2TimePartition tp = doOnePart(dir, "B:/ndfd/200901/ncdc1Year-200901.ncx", config);

    for (TimePartition.Partition part : tp.getPartitions()) {
      long start2 = System.currentTimeMillis();
      GribCollection gc = part.getGribCollection();
      System.out.printf("%s%n", gc);
      gc.close();
      long took2 = System.currentTimeMillis() - start2;
      System.out.printf("that took %s msecs%n", took2);
    }
    tp.close();

    long took = System.currentTimeMillis() - start;
    System.out.printf("that all took %s msecs%n", took);
  }
}

