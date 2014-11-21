package ucar.nc2.grib.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.*;
import thredds.inventory.filter.StreamFilter;
import thredds.inventory.partition.*;
import ucar.nc2.grib.GribUtils;
import ucar.nc2.grib.grib1.Grib1Index;
import ucar.nc2.grib.grib1.Grib1RecordScanner;
import ucar.nc2.grib.grib1.Grib1SectionGridDefinition;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CloseableIterator;
import ucar.nc2.util.Counters;
import ucar.nc2.util.Indent;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Formatter;

/**
 * GribCollection Building - pass1 : gather information
 *
 * @author caron
 * @since 11/15/2014
 */
public class GCpass1 {
  static private final Logger logger = LoggerFactory.getLogger(GCpass1.class);

  public static void main(String[] args) throws IOException {
    String usage = "usage: ucar.nc2.grib.collection.GCpass1 -spec <collectionSpec> [-isGrib2] -partition [none|file|directory]";
    String def = "default: -isGrib1 partition=directory";
    if (args.length < 2) {
      System.out.printf("%s%n%s%n", usage, def);
      System.exit(0);
    }

    // String spec = "Q:/cdmUnitTest/gribCollections/cfsr/.*gbx9";
    // String spec = "B:/lead/NDFD-CONUS_5km/.*gbx9";
    // String spec = "B:/motherlode/rfc/kalr/.*gbx9";
    String spec = null; // "B:/rdavm/ds083.2/grib1/**/.*gbx9";
    FeatureCollectionType type = FeatureCollectionType.GRIB1;
    String partition = "directory";
    String useTableVersionS = null;
    for (int i = 0; i < args.length; i++) {
      String s = args[i];
      if (s.equalsIgnoreCase("-spec")) spec = args[i + 1];
      if (s.equalsIgnoreCase("-isGrib2")) type = FeatureCollectionType.GRIB2;
      if (s.equalsIgnoreCase("-partition")) partition = args[i + 1];
      if (s.equalsIgnoreCase("-useTableVersion")) useTableVersionS = args[i + 1];
    }

    Boolean useTableVersion = (useTableVersionS != null) ? Boolean.parseBoolean(useTableVersionS) : null;  // default true

    Formatter fm = new Formatter(System.out);
    GCpass1 pass1 = new GCpass1(spec, type, partition, useTableVersion, fm);
    pass1.scanAndReport();
  }

  class Accum {
    CalendarDate last = null;
    int[] count;

    Accum(int n) {
      count = new int[n];
    }

    void add(Accum a) {
      for (int i = 0; i < count.length; i++)
        count[i] += a.count[i];
    }

    void add(int idx, int val) {
      count[idx] += val;
    }
  }

  class Variable implements Comparable<Variable> {
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
      if (cdmHash != variable.cdmHash) return false;
      return true;
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
  Accum accumAll = new Accum(2);

  public GCpass1(String spec, FeatureCollectionType fcType, String timePartition, Boolean useTableVersion, Formatter fm) {
    this.config = new FeatureCollectionConfig("GCpass1", "test/GCpass1", fcType, spec, null, null, timePartition, null, null);
    this.gribConfig =  this.config.gribConfig;
    if (useTableVersion != null) this.gribConfig.useTableVersion = useTableVersion;
    this.fm = fm;

    this.config.show(fm);
    fm.format("%n");

    countersAll = new Counters();
    countersAll.add("referenceDate").setShowRange(true);
    countersAll.add("table version");
    countersAll.add("variable");
    countersAll.add("gds");

    countersAll.add("vertCoordInGDS");
    countersAll.add("predefined");
    countersAll.add("thin");
  }

  public void reportAll(Indent indent, Formatter fm) {
    fm.format("%n");
    reportOneHeader(indent, fm);
    fm.format("%s%40s", indent, "grand total");
    fm.format("%8d ", accumAll.count[0]);
    fm.format("%8d ", accumAll.count[1]);
    fm.format("%8d ", countersAll.get("variable").getUnique());
    fm.format("%8d ", countersAll.get("referenceDate").getUnique());
    fm.format("%8d ", countersAll.get("gds").getUnique());
    fm.format("%n");

    countersAll.show(fm);
  }

  public void reportOneHeader(Indent indent, Formatter fm) {
    fm.format("%s%40s #files  #records   #vars  #runtimes    #gds%n", indent, "");
  }

  public CalendarDate reportOneDir(String dir, Accum accum, Counters countersOne, Indent indent, CalendarDate last) {
    fm.format("%s%40s", indent, dir + " total");
    fm.format("%8d ", accum.count[0]);
    fm.format("%8d ", accum.count[1]);
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
    CollectionSpecParser specp = new CollectionSpecParser(config.spec, errlog);
    Path rootPath = Paths.get(specp.getRootDir());
    boolean isGrib1 = config.type == FeatureCollectionType.GRIB1;

    MCollection topCollection = DirectoryBuilder.factory(config, rootPath, null, logger);

    if (topCollection instanceof DirectoryPartition) {
      DirectoryPartition dpart = (DirectoryPartition) topCollection;
      dpart.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
      accumAll.add(scanDirectoryPartitionRecurse(isGrib1, dpart, config, countersAll, logger, indent, fm));

    } else if (topCollection instanceof DirectoryCollection) {
      // otherwise its a leaf directory
      accumAll.add(scanLeafDirectoryCollection(isGrib1, config, countersAll, logger, rootPath, indent, fm));
    }

    reportAll(indent, fm);
  }


  private Accum scanDirectoryPartitionRecurse(boolean isGrib1, DirectoryPartition dpart,
                                              FeatureCollectionConfig config,
                                              Counters countersParent,
                                              Logger logger, Indent indent, Formatter fm) throws IOException {

    fm.format("%n%sDirectory %s%n", indent, dpart.getRoot());
    indent.incr();
    Counters countersPart = countersParent.makeSubCounters();
    Accum accum = new Accum(2);

    for (MCollection part : dpart.makePartitions(CollectionUpdateType.always)) {
      part.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
      try {
        if (part.isLeaf()) {
          Path partPath = Paths.get(part.getRoot());
          accum.add(scanLeafDirectoryCollection(isGrib1, config, countersPart, logger, partPath, indent, fm));
        } else {
          accum.add(scanDirectoryPartitionRecurse(isGrib1, (DirectoryPartition) part, config, countersPart, logger, indent, fm));
        }
      } catch (Throwable t) {
        logger.warn("Error making partition " + part.getRoot(), t);
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
  private Accum scanLeafCollection(boolean isGrib1, FeatureCollectionConfig config,
                                   Counters countersParent,
                                   Logger logger, Path dirPath, Indent indent, Formatter fm) throws IOException {

    return scanLeafDirectoryCollection(isGrib1, config, countersParent, logger, dirPath, indent, fm);

  }

  /**
   * File Partition: each File is a collection of Grib records, and the collection of all files in the directory is a PartitionCollection.
   * Rewrite the PartitionCollection and optionally its children
   *
   * @param config FeatureCollectionConfig
   * @return true if partition was rewritten
   * @throws IOException
   *
  private int scanFilePartition(final boolean isGrib1, final FeatureCollectionConfig config,
                                final Logger logger, Path dirPath, Formatter fm) throws IOException {
    long start = System.currentTimeMillis();

    final Formatter errlog = new Formatter();
    CollectionSpecParser specp = new CollectionSpecParser(config.spec, errlog);
    final CollectionUpdateType updateType = CollectionUpdateType.test;

    FilePartition partition = new FilePartition(config.name, dirPath, config.olderThan, logger);
    partition.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
    if (specp.getFilter() != null)
      partition.setStreamFilter(new StreamFilter(specp.getFilter()));

    final AtomicBoolean anyChange = new AtomicBoolean(false); // just need a mutable boolean we can declare final

    // redo the child collection here; could also do inside Grib2PartitionBuilder, not sure if advantage
    if (updateType != CollectionUpdateType.never && updateType != CollectionUpdateType.testIndexOnly) {
      partition.iterateOverMFileCollection(new DirectoryCollection.Visitor() {
        public void consume(MFile mfile) {
          MCollection dcm = new CollectionSingleFile(mfile, logger);
          dcm.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);

          if (isGrib1) {
            Grib1CollectionBuilder builder = new Grib1CollectionBuilder(dcm.getCollectionName(), dcm, logger);
            try {
              boolean changed = (builder.updateNeeded(updateType) && builder.createIndex(errlog));
              if (changed) anyChange.set(true);
            } catch (IOException e) {
              e.printStackTrace();
            }

          } else {
            Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm.getCollectionName(), dcm, logger);
            try {
              boolean changed = (builder.updateNeeded(updateType) && builder.createIndex(errlog));
              if (changed) anyChange.set(true);
            } catch (IOException e) {
              e.printStackTrace();
            }

          }
        }
      });
    }

    // redo partition index if needed, will detect if children have changed
    boolean recreated = false;
    if (isGrib1) {
      Grib1PartitionBuilder builder = new Grib1PartitionBuilder(partition.getCollectionName(), new File(partition.getRoot()), partition, logger);
      if (anyChange.get() || builder.updateNeeded(updateType))
        recreated = builder.createPartitionedIndex(updateType, CollectionUpdateType.never, errlog);

    } else {
      Grib2PartitionBuilder builder = new Grib2PartitionBuilder(partition.getCollectionName(), new File(partition.getRoot()), partition, logger);
      if (anyChange.get() || builder.updateNeeded(updateType))
        recreated = builder.createPartitionedIndex(updateType, CollectionUpdateType.never, errlog);
    }

    long took = System.currentTimeMillis() - start;
    String collectionName = partition.getCollectionName();
    if (recreated) logger.info("RewriteFilePartition {} took {} msecs", collectionName, took);

    return 0;
  } */

  /**
   * Update all the grib indices in one directory, and the collection index for that directory
   *
   * @param config  FeatureCollectionConfig
   * @param dirPath directory path
   * @throws IOException
   */
  private Accum scanLeafDirectoryCollection(boolean isGrib1, FeatureCollectionConfig config,
                                            Counters parentCounters,
                                            Logger logger, Path dirPath,
                                            Indent indent, Formatter fm) throws IOException {

    if (config.ptype == FeatureCollectionConfig.PartitionType.file) {
      reportOneFileHeader(indent, fm);
      fm.format("%sDirectory %s%n", indent, dirPath);
    }
    Accum accum = new Accum(2);
    int nfiles = 0;

    Counters countersThisDir = parentCounters.makeSubCounters();

    Formatter errlog = new Formatter();
    CollectionSpecParser specp = new CollectionSpecParser(config.spec, errlog);

    DirectoryCollection dcm = new DirectoryCollection(config.name, dirPath, config.olderThan, logger);
    dcm.setUseGribFilter(false);
    dcm.setLeaf(true);
    dcm.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
    if (specp.getFilter() != null)
      dcm.setStreamFilter(new StreamFilter(specp.getFilter()));

    try (CloseableIterator<MFile> iter = dcm.getFileIterator()) {
      while (iter.hasNext()) {
        MFile mfile = iter.next();
        Counters countersOneFile = countersThisDir.makeSubCounters();
        int nrecords = 0;

        if (isGrib1) {
          Grib1Index grib1Index = readGrib1Index(mfile, true);
          if (grib1Index == null) {
            System.out.printf("%s%s: read or create failed%n", indent, mfile.getPath());
            continue;
          }
          for (ucar.nc2.grib.grib1.Grib1Record gr : grib1Index.getRecords()) {
            accumGrib1Record(gr, countersOneFile);
            nrecords++;
          }
        } else {
          Grib2Index grib2Index = readGrib2Index(mfile, true);
          if (grib2Index == null) {
            System.out.printf("%s%s: read or create failed%n", indent, mfile.getPath());
            continue;
          }
          for (ucar.nc2.grib.grib2.Grib2Record gr : grib2Index.getRecords()) {
            accumGrib2Record(gr, countersOneFile);
            nrecords++;
          }
        }

        accum.add(1, nrecords);
        countersThisDir.addTo(countersOneFile);
        if (config.ptype == FeatureCollectionConfig.PartitionType.file)
          reportOneFile(mfile, nrecords, countersOneFile, indent, fm);
        nfiles++;
      }
    }

    parentCounters.addTo(countersThisDir);
    accum.add(0, nfiles);
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

    int gdsHash = gdss.getGDS().hashCode();
    int cdmHash = Grib1Iosp.cdmVariableHash(cust1, gr, gdsHash, gribConfig.useTableVersion, gribConfig.intvMerge, gribConfig.useCenter);
    String name =  Grib1Iosp.makeVariableName(cust1, pds);
    counters.count("variable", new Variable(cdmHash, name));
    counters.count("gds", gdsHash);

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

  Grib2Customizer cust2 = null;

  private void accumGrib2Record(ucar.nc2.grib.grib2.Grib2Record gr, Counters counters) throws IOException {
    if (cust2 == null) {                              // first record LOOK test if assumption is valid
      cust2 = Grib2Customizer.factory(gr);
    }

    Grib2SectionIdentification id = gr.getId();
    Grib2SectionProductDefinition pds = gr.getPDSsection();
    Grib2Pds pdss = gr.getPDSsection().getPDS();

    String table = id.getCenter_id() + "-" + id.getSubcenter_id() + "-" + id.getMaster_table_version() + "-" + id.getLocal_table_version();
    counters.count("table version", table);
    counters.count("referenceDate", gr.getReferenceDate());

    //counters.countS("param", gr.getDiscipline() + "-" + pdss.getParameterCategory() + "-" + pdss.getParameterNumber());
    int gdsHash = gr.getGDSsection().getGDS().hashCode();
    int cdmHash = Grib2Iosp.cdmVariableHash(cust2, gr, gdsHash, gribConfig.intvMerge, gribConfig.useGenType, logger);
    String name = GribUtils.makeNameFromDescription(cust2.getVariableName(gr));
    counters.count("variable", new Variable(cdmHash, name));
    counters.count("gds", gdsHash);
  }


  private Grib1Index readGrib1Index(MFile mf, boolean readOnly) throws IOException {
    String path = mf.getPath();
    if (path.endsWith(Grib1Index.GBX9_IDX))
      path = StringUtil2.removeFromEnd(path, Grib1Index.GBX9_IDX);

    Grib1Index index = new Grib1Index();
    if (!index.readIndex(path, mf.getLastModified())) {
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
    if (!index.readIndex(path, mf.getLastModified())) {
      if (readOnly) return null;
      try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
        if (!Grib2RecordScanner.isValidFile(raf)) return null;
        index.makeIndex(path, raf);
      }
    }
    return index;
  }

}
