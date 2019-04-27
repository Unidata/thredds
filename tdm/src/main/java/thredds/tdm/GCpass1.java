package thredds.tdm;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.*;
import thredds.inventory.filter.StreamFilter;
import thredds.inventory.partition.*;
import ucar.nc2.grib.GribIndex;
import ucar.nc2.grib.GribIndexCache;
import ucar.nc2.grib.GribUtils;
import ucar.nc2.grib.collection.Grib1Iosp;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.grib1.*;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.grib2.table.Grib2Tables;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CloseableIterator;
import ucar.nc2.util.Counters;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.Indent;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * GribCollection Building - pass1 : gather information, optionally make gbx9 files
 *
 * @author caron
 * @since 11/15/2014
 */
public class GCpass1 {
  static private final Logger logger = LoggerFactory.getLogger(GCpass1.class);

  /*
String usage = "usage: thredds.tdm.GCpass1 -spec <collectionSpec> [-isGrib2] -partition [none|file|directory] -useTableVersion [true|false] "+
      "-useCacheDir <dir>"; */
  private static class CommandLine {
    @Parameter(names = {"-spec"}, description = "Collection specification string, exactly as in the <featureCollection>.", required = false)
    public String spec;

    @Parameter(names = {"-rootDir"}, description = "Collection rootDir, exactly as in the <featureCollection>.", required = false)
    public String rootDir;

    @Parameter(names = {"-regexp"}, description = "Collection regexp string, exactly as in the <featureCollection>.", required = false)
    public String regexp;

    @Parameter(names = {"-isGrib2"}, description = "Is Grib2 collection.", required = false)
    public boolean isGrib2 = false;

    @Parameter(names = {"-partition"}, description = "Partition type: none, directory, file", required = false)
    public FeatureCollectionConfig.PartitionType partitionType = FeatureCollectionConfig.PartitionType.directory;

    @Parameter(names = {"-useTableVersion"}, description = "Use Table version to make seperate variables.", required = false)
    public boolean useTableVersion = false;

    @Parameter(names = {"-useCacheDir"}, description = "Set the Grib index cache directory.", required = false)
    public String cacheDir;

    @Parameter(names = {"-h", "--help"}, description = "Display this help and exit", help = true)
    public boolean help = false;

    private final JCommander jc;

    public CommandLine(String progName, String[] args) throws ParameterException {
      this.jc = new JCommander(this, args);  // Parses args and uses them to initialize *this*.
      jc.setProgramName(progName);           // Displayed in the usage information.
    }

    public void printUsage() {
      jc.usage();
    }

    FeatureCollectionType getFeatureCollectionType() {
      return isGrib2 ? FeatureCollectionType.GRIB2 : FeatureCollectionType.GRIB1;
    }
  }

  public static void main(String[] args) throws IOException {
    CommandLine cmdLine = new CommandLine("thredds.tdm.GCpass1", args);

    if (cmdLine.help) {
      cmdLine.printUsage();
      return;
    }

    if (cmdLine.cacheDir != null) {
      DiskCache2 gribCache = DiskCache2.getDefault();
      gribCache.setRootDirectory(cmdLine.cacheDir);
      gribCache.setAlwaysUseCache(true);
      GribIndexCache.setDiskCache2(gribCache);
    }

    /*
      public FeatureCollectionConfig(String name, String path, FeatureCollectionType fcType, String spec, String collectionName,
                                 String dateFormatMark, String olderThan, String timePartition, Element innerNcml)
     */
    FeatureCollectionConfig config = new FeatureCollectionConfig("GCpass1", "GCpass1", cmdLine.getFeatureCollectionType(),
            cmdLine.spec, null, null, null, cmdLine.partitionType.toString(), null);
    FeatureCollectionConfig.GribConfig gribConfig = config.gribConfig;
    gribConfig.useTableVersion = cmdLine.useTableVersion;

    if (cmdLine.rootDir != null && cmdLine.regexp != null)
      config.setFilter(cmdLine.rootDir,cmdLine.regexp);

    Formatter fm = new Formatter(System.out);
    GCpass1 pass1 = new GCpass1(config, fm);
    pass1.scanAndReport();
  }

  public static class Accum {
    CalendarDate last = null;
    int nfiles;
    int nrecords;
    float fileSize;
    float indexSize;

    void add(Accum a) {
      this.nfiles += a.nfiles;
      this.nrecords += a.nrecords;
      this.fileSize += a.fileSize;
      this.indexSize += a.indexSize;
    }
  }

  static class Variable implements Comparable<Variable> {
    int cdmHash;
    String name;

    Variable(int cdmHash, String name) {
      this.cdmHash = cdmHash;
      this.name = name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Variable variable = (Variable) o;
      return cdmHash == variable.cdmHash;
    }

    @Override
    public int hashCode() {
      return cdmHash;
    }

    @Override
    public int compareTo(Variable o) {
      return name.compareTo(o.name);
    }

    @Override
    public String toString() {
      return name;
    }
  }

  ///////////////////////////////////
  FeatureCollectionConfig config;
  FeatureCollectionConfig.GribConfig gribConfig;
  Formatter fm;
  Counters countersAll;
  Accum accumAll = new Accum();

  public GCpass1(FeatureCollectionConfig config, Formatter fm) {
    this.config = config;
    this.gribConfig = config.gribConfig;
    this.fm = fm;

    this.config.show(fm);
    fm.format("%n");

    countersAll = new Counters();
    countersAll.add("referenceDate").setShowRange(true);
    countersAll.add("table version");
    countersAll.add("variable");
    countersAll.add("gds");

    countersAll.add("gdsTemplate");
    countersAll.add("vertCoordInGDS");
    countersAll.add("predefined");
    countersAll.add("thin");
  }

  public void reportAll(Indent indent, Formatter fm) {
    fm.format("%n");
    reportOneHeader(indent, fm);
    fm.format("%s%60s", indent, "grand total");
    fm.format("%8d ", accumAll.nfiles);
    fm.format("%8d ", accumAll.nrecords);
    fm.format("%8.0f ", accumAll.indexSize);
    fm.format("%8.0f ", accumAll.fileSize);
    fm.format("%8d ", countersAll.get("variable").getUnique());
    fm.format("%8d ", countersAll.get("referenceDate").getUnique());
    fm.format("%8d ", countersAll.get("gds").getUnique());
    fm.format("%n");

    countersAll.show(fm);

    if (gds1set.size() > 1) {
      fm.format("gds1%n");
      for (Grib1Record gr1 : gds1set.values()) {
        Grib1Gds gds = gr1.getGDS();
        fm.format(" hash %s == %s%n", gds.hashCode(), gds);
      }
    }

    if (gds2set.size() > 1) {
      fm.format("gds2%n");
      for (Integer key : gds2set.keySet()) {
        Grib2Record gr2 = gds2set.get(key);
        Grib2Gds gds = gr2.getGDS();
        fm.format(" key = %d hash = %s%n", key, gds.hashCode());
        Grib2Show.showGdsTemplate(gr2.getGDSsection(), fm, cust2);
        fm.format("%n");
      }
    }
  }

  public void reportOneHeader(Indent indent, Formatter fm) {
    fm.format("%s%60s #files   #records #idxSize #dataSize #vars  #runtimes    #gds%n", indent, "");
  }

  public CalendarDate reportOneDir(String dir, Accum accum, Counters countersOne, Indent indent, CalendarDate last) {
    fm.format("%s%60s", indent, dir + " total");
    fm.format("%8d ", accum.nfiles);
    fm.format("%8d ", accum.nrecords);
    fm.format("%8.3f ", accum.indexSize);
    fm.format("%8.3f ", accum.fileSize);
    fm.format("%8d ", countersOne.get("variable").getUnique());
    fm.format("%8d ", countersOne.get("referenceDate").getUnique());
    fm.format("%8d ", countersOne.get("gds").getUnique());
    fm.format("%s ", countersOne.get("referenceDate").showRange());

    CalendarDate first = (CalendarDate) countersOne.get("referenceDate").getFirst();
    if (last != null && first.isBefore(last))
      fm.format(" ***");
    fm.format("%n");

    return (CalendarDate) countersOne.get("referenceDate").getLast();
  }

  public void reportOneFileHeader(Indent indent, Formatter fm) {
    fm.format("%s%40s #records   #vars  #runtimes #gds%n", indent, "");
  }

  public void reportOneFile(MFile mfile, int nrecords, Counters countersOne, Indent indent, Formatter fm) {
    fm.format("%s%40s", indent, mfile.getName());
    fm.format("%8d ", nrecords);
    fm.format("%8d ", countersOne.get("variable").getUnique());
    fm.format("%8d ", countersOne.get("referenceDate").getUnique());
    fm.format("%8d ", countersOne.get("gds").getUnique());

    int vertCoordInGDS = countersOne.get("vertCoordInGDS").getUnique();
    int predefined = countersOne.get("predefined").getUnique();
    int thin = countersOne.get("thin").getUnique();
    if (vertCoordInGDS != 0) fm.format("vertCoordInGDS=%d ", vertCoordInGDS);
    if (predefined != 0) fm.format("predefined=%d ", vertCoordInGDS);
    if (thin != 0) fm.format("thin=%d ", vertCoordInGDS);
    fm.format("%n");
  }

  public void scanAndReport() throws IOException {
    Indent indent = new Indent(2);
    if (config.ptype != FeatureCollectionConfig.PartitionType.file)
      reportOneHeader(indent, fm);

    Formatter errlog = new Formatter();
    CollectionSpecParser specp = config.getCollectionSpecParser(errlog);
    Path rootPath = Paths.get(specp.getRootDir());
    boolean isGrib1 = config.type == FeatureCollectionType.GRIB1;

    try (MCollection topCollection = DirectoryBuilder.factory(config, rootPath, true, null, GribCdmIndex.NCX_SUFFIX, logger)) {

      if (topCollection instanceof DirectoryPartition) {
        DirectoryPartition dpart = (DirectoryPartition) topCollection;
        dpart.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
        accumAll.add(scanDirectoryPartitionRecurse(isGrib1, dpart, config, countersAll, logger, indent, fm));

      } else if (topCollection instanceof DirectoryCollection) {
        // otherwise its a leaf directory
        accumAll.add(scanLeafDirectoryCollection(isGrib1, config, countersAll, logger, rootPath, true, indent, fm));
      }

      reportAll(indent, fm);
    }
  }

  private Accum scanDirectoryPartitionRecurse(boolean isGrib1, DirectoryPartition dpart,
                                              FeatureCollectionConfig config,
                                              Counters countersParent,
                                              Logger logger, Indent indent, Formatter fm) throws IOException {

    fm.format("%n%sDirectory %s%n", indent, dpart.getRoot());
    indent.incr();
    Counters countersPart = countersParent.makeSubCounters();
    Accum accum = new Accum();

    for (MCollection part : dpart.makePartitions(CollectionUpdateType.always)) {
      part.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
      try {
        if (part instanceof DirectoryPartition) {
          accum.add(scanDirectoryPartitionRecurse(isGrib1, (DirectoryPartition) part, config, countersPart, logger, indent, fm));
        } else {
          Path partPath = Paths.get(part.getRoot());
          accum.add(scanLeafDirectoryCollection(isGrib1, config, countersPart, logger, partPath, false, indent, fm));
        }
      } catch (Throwable t) {
        logger.warn("Error making partition " + part.getRoot(), t);
        dpart.removePartition(part);
      }
    }   // loop over partitions

    countersParent.addTo(countersPart);
    accum.last = reportOneDir(dpart.getRoot(), accum, countersPart, indent, accum.last);
    indent.decr();
    return accum;
  }

  /**
   * Update all the grib indices in one directory, and the collection index for that directory
   *
   * @param config  FeatureCollectionConfig
   * @param dirPath directory path
   * @throws IOException
   */
  private Accum scanLeafDirectoryCollection(boolean isGrib1, FeatureCollectionConfig config,
                                            Counters parentCounters,
                                            Logger logger, Path dirPath, boolean isTop,
                                            Indent indent, Formatter fm) throws IOException {

    if (config.ptype == FeatureCollectionConfig.PartitionType.file) {
      reportOneFileHeader(indent, fm);
      fm.format("%sDirectory %s%n", indent, dirPath);
    }
    Accum accum = new Accum();
    int nfiles = 0;

    Counters countersThisDir = parentCounters.makeSubCounters();

    Formatter errlog = new Formatter();
    CollectionSpecParser specp = config.getCollectionSpecParser(errlog);

    DirectoryCollection dcm = new DirectoryCollection(config.collectionName, dirPath, isTop, config.olderThan, logger);
    // dcm.setUseGribFilter(false);
    dcm.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
    if (specp.getFilter() != null)
      dcm.setStreamFilter(new StreamFilter(specp.getFilter(), specp.getFilterOnName()));

    try (CloseableIterator<MFile> iter = dcm.getFileIterator()) {
      while (iter.hasNext()) {
        MFile mfile = iter.next();
        Counters countersOneFile = countersThisDir.makeSubCounters();
        int nrecords = 0;

        if (isGrib1) {
          Grib1Index grib1Index = readGrib1Index(mfile, false);
          if (grib1Index == null) {
            System.out.printf("%s%s: read or create failed%n", indent, mfile.getPath());
            continue;
          }
          for (ucar.nc2.grib.grib1.Grib1Record gr : grib1Index.getRecords()) {
            accumGrib1Record(gr, countersOneFile);
            nrecords++;
          }
        } else {
          Grib2Index grib2Index = readGrib2Index(mfile, false);
          if (grib2Index == null) {
            System.out.printf("%s%s: read or create failed%n", indent, mfile.getPath());
            continue;
          }
          for (ucar.nc2.grib.grib2.Grib2Record gr : grib2Index.getRecords()) {
            accumGrib2Record(gr, countersOneFile);
            nrecords++;
          }
        }

        accum.nrecords += nrecords;
        countersThisDir.addTo(countersOneFile);
        if (config.ptype == FeatureCollectionConfig.PartitionType.file)
          reportOneFile(mfile, nrecords, countersOneFile, indent, fm);
        nfiles++;

        // get file sizes
        String path = mfile.getPath();
        if (path.endsWith(GribIndex.GBX9_IDX)) {
          accum.indexSize += ((float) mfile.getLength() / (1000 * 1000)); // mb
        } else {
          accum.fileSize += ((float) mfile.getLength() / (1000 * 1000)); // mb
          File idxFile = GribIndexCache.getExistingFileOrCache(path + GribIndex.GBX9_IDX);
          if (idxFile.exists())
            accum.indexSize += ((float) idxFile.length() / (1000 * 1000)); // mb
        }
      }
    }

    parentCounters.addTo(countersThisDir);
    accum.nfiles += nfiles;
    accum.last = reportOneDir(dirPath.toString(), accum, countersThisDir, indent, accum.last);
    return accum;
  }

  Grib1Customizer cust1 = null;

  private void accumGrib1Record(ucar.nc2.grib.grib1.Grib1Record gr, Counters counters) throws IOException {

    if (cust1 == null) { // first record
      cust1 = Grib1Customizer.factory(gr, null);
    }

    Grib1SectionGridDefinition gdss = gr.getGDSsection();
    Grib1SectionProductDefinition pds = gr.getPDSsection();
    String table = pds.getCenter() + "-" + pds.getSubCenter() + "-" + pds.getTableVersion();
    counters.count("table version", table);
    counters.count("referenceDate", pds.getReferenceDate());

    int gdsHash = gr.getGDS().hashCode();
    int cdmHash = Grib1Variable.cdmVariableHash(cust1, gr, gdsHash, gribConfig.useTableVersion, gribConfig.intvMerge, gribConfig.useCenter);
    String name = Grib1Iosp.makeVariableName(cust1, gribConfig, pds);
    counters.count("variable", new Variable(cdmHash, name));
    if (counters.count("gds", gdsHash)) storeGrib1Record(gdsHash, gr);
    counters.count("gdsTemplate", gdss.getGridTemplate());

    if (gdss.isThin()) {
      //fm.format("  THIN= (gds=%d)%n", gdss.getGridTemplate());
      counters.count("thin", gdss.getGridTemplate());
    }

    if (!pds.gdsExists()) {
      //fm.format("   PREDEFINED GDS%n");
      counters.count("predefined", gdss.getPredefinedGridDefinition());
    }

    if (gdss.hasVerticalCoordinateParameters()) {
      //fm.format("   Has vertical coordinates in GDS%n");
      counters.count("vertCoordInGDS", pds.getLevelType());
    }

  }

  Grib2Tables cust2 = null;

  private void accumGrib2Record(ucar.nc2.grib.grib2.Grib2Record gr, Counters counters) throws IOException {
    if (cust2 == null) {                              // first record LOOK test if assumption is valid
      cust2 = Grib2Tables.factory(gr);
    }

    Grib2SectionIdentification id = gr.getId();
    //Grib2SectionProductDefinition pds = gr.getPDSsection();
    //Grib2Pds pdss = gr.getPDSsection().getPDS();

    String table = id.getCenter_id() + "-" + id.getSubcenter_id() + "-" + id.getMaster_table_version() + "-" + id.getLocal_table_version();
    counters.count("table version", table);
    counters.count("referenceDate", gr.getReferenceDate());

    //counters.countS("param", gr.getDiscipline() + "-" + pdss.getParameterCategory() + "-" + pdss.getParameterNumber());
    int cdmHash = Grib2Variable.cdmVariableHash(cust2, gr, 0, gribConfig.intvMerge, gribConfig.useGenType); // LOOK needs gdsHashOverride
    String name = GribUtils.makeNameFromDescription(cust2.getVariableName(gr));
    counters.count("variable", new Variable(cdmHash, name));
    int gdsHash = gr.getGDS().hashCode();
    if (counters.count("gds", gdsHash)) storeGrib2Record(gdsHash, gr);
    counters.count("gdsTemplate", gr.getGDSsection().getGDSTemplateNumber());
  }


  private Grib1Index readGrib1Index(MFile mf, boolean readOnly) throws IOException {
    String path = mf.getPath();
    if (path.endsWith(Grib1Index.GBX9_IDX))
      path = StringUtil2.removeFromEnd(path, Grib1Index.GBX9_IDX);

    Grib1Index index = new Grib1Index();
    if (!index.readIndex(path, mf.getLastModified(), CollectionUpdateType.test)) {
      if (readOnly) return null;
      try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
        if (!Grib1RecordScanner.isValidFile(raf)) return null;
        index.makeIndex(path, raf);
      }
    }
    return index;
  }

  private Grib2Index readGrib2Index(MFile mf, boolean readOnly) throws IOException {
    String path = mf.getPath();
    if (path.endsWith(Grib1Index.GBX9_IDX))
      path = StringUtil2.removeFromEnd(path, Grib1Index.GBX9_IDX);

    Grib2Index index = new Grib2Index();
    if (!index.readIndex(path, mf.getLastModified(), CollectionUpdateType.test)) {
      if (readOnly) return null;
      try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
        if (!Grib2RecordScanner.isValidFile(raf)) return null;
        index.makeIndex(path, raf);
      }
    }
    return index;
  }

  Map<Integer, Grib1Record> gds1set = new HashMap<>();
  private void storeGrib1Record( int hash, Grib1Record gr1) {
    gds1set.put(hash, gr1);
  }


  Map<Integer, Grib2Record> gds2set = new HashMap<>();
  private void storeGrib2Record( int hash, Grib2Record gr2) {
    gds2set.put(hash, gr2);
  }

}
