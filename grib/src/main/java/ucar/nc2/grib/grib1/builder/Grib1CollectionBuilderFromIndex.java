package ucar.nc2.grib.grib1.builder;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib1.*;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.stream.NcStream;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Build a GribCollection object for Grib-1 files by reading an already made ncx files.
 *
 * @author caron
 * @since 11/29/13
 */
public class Grib1CollectionBuilderFromIndex extends GribCollectionBuilder {

    // read in the index, open raf
  static public GribCollection createFromIndex(String name, File directory, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    File idxFile = GribCollection.getIndexFile(name, directory);
    RandomAccessFile raf = new RandomAccessFile(idxFile.getPath(), "r");
    return createFromIndex(name, directory, raf, config, logger);
  }

  // read in the index, index raf already open
  static public GribCollection createFromIndex(String name, File directory, RandomAccessFile raf, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) throws IOException {
    Grib1CollectionBuilderFromIndex builder = new Grib1CollectionBuilderFromIndex(name, directory, config, logger);
    if (builder.readIndex(raf))
      return builder.gc;
    throw new IOException("Reading index failed"); // or return null ??
  }

  ////////////////////////////////////////////////////////////////

  //protected final List<CollectionManager> collections = new ArrayList<CollectionManager>();
  protected GribCollection gc;
  protected Grib1Customizer cust;

  protected Grib1CollectionBuilderFromIndex(String name, File directory, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) {
    super(null, false, logger);
    this.gc = new Grib1Collection(name, directory, config);
  }

  protected Grib1CollectionBuilderFromIndex(MCollection dcm, boolean isSingleFile, org.slf4j.Logger logger) {
    super(dcm, isSingleFile, logger);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // reading

  public boolean readIndex(String filename) throws IOException {
    return readIndex( new RandomAccessFile(filename, "r") );
  }

  protected String getMagicStart() {
    return Grib1CollectionBuilder.MAGIC_START;
  }

  /**
   * Read the index file
   * @param raf the index file
   * @return true on success
   */
  protected boolean readIndex(RandomAccessFile raf) {
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
      boolean versionOk = isSingleFile ? gc.version >= Grib1CollectionBuilder.minVersionSingle : gc.version >= Grib1CollectionBuilder.version;
      if (!versionOk) {
        logger.warn("Grib1Collection {}: index found version={}, want version= {} on file {}", gc.getName(), gc.version, Grib1CollectionBuilder.version, raf.getLocation());
        return false;
      }

      long skip = raf.readLong();
      raf.skipBytes(skip);

      int size = NcStream.readVInt(raf);
      if ((size < 0) || (size > 100 * 1000 * 1000)) {
        logger.warn("Grib1Collection {}: invalid or empty index ", gc.getName());
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

      File dir = gc.getDirectory();
      String dirname = proto.getDirName();
      if (dir != null && !dir.getPath().equals(dirname)) {
        logger.debug("Grib1Collection {}: has different directory= {} than index= {} ", gc.getName(), dir.getPath(), dirname);
        //return false;
      }

      boolean isPartition = proto.getPartitionsCount() > 0;

      // switch from files to mfiles in version 12
      if (!isPartition) {
        if (gc.version < 10) {
          int n = proto.getFilesCount();
          if (n == 0) {
            logger.warn("Grib1Collection {}: has no files, force recreate ", gc.getName());
            return false;
          } else {
            List<MFile> files = new ArrayList<MFile>(proto.getFilesCount());
            for (int i = 0; i < n; i++)
              files.add(new GribCollectionBuilder.GcMFile(dir, proto.getFiles(i), -1));
            gc.setFiles(files);
            // if (dcm != null) dcm.setFiles(files);  // LOOK !!
          }

        } else {
          int n = proto.getMfilesCount();
          if (n == 0) {
            logger.warn("Grib1Collection {}: has no files, force recreate ", gc.getName());
            return false;
          } else {
            List<MFile> files = new ArrayList<MFile>(n);
            for (int i = 0; i < n; i++)
              files.add(new GribCollectionBuilder.GcMFile(dir, proto.getMfiles(i)));
            gc.setFiles(files);
            // if (dcm != null) dcm.setFiles(files);  // LOOK !!
          }
        }
      }

      gc.groups = new ArrayList<GribCollection.GroupHcs>(proto.getGroupsCount());
      for (int i = 0; i < proto.getGroupsCount(); i++)
        gc.groups.add(readGroup(proto.getGroups(i), gc.makeGroup(), gc.center));
      gc.groups = Collections.unmodifiableList(gc.groups);

      gc.params = new ArrayList<Parameter>(proto.getParamsCount());
      for (int i = 0; i < proto.getParamsCount(); i++)
        gc.params.add(readParam(proto.getParams(i)));

      if (!readPartitions(proto, dirname)) {
        logger.warn("Time1Partition {}: has no partitions, force recreate ", gc.getName());
        return false;
      }

      return true;

    } catch (Throwable t) {
      logger.error("Error reading index " + raf.getLocation(), t);
      return false;
    }
  }

  protected boolean readPartitions(GribCollectionProto.GribCollectionIndex proto, String directory) {
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

}
