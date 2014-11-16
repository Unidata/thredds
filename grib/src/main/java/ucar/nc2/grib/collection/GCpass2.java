package ucar.nc2.grib.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.*;
import thredds.inventory.filter.StreamFilter;
import thredds.inventory.partition.*;
import ucar.nc2.grib.grib1.Grib1Index;
import ucar.nc2.grib.grib1.Grib1RecordScanner;
import ucar.nc2.grib.grib1.Grib1SectionGridDefinition;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.util.CloseableIterator;
import ucar.nc2.util.Counters;
import ucar.nc2.util.Indent;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Formatter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Describe
 *
 * @author caron
 * @since 11/15/2014
 */
public class GCpass2 {
  static private final Logger logger = LoggerFactory.getLogger(GCpass2.class);

  public static void main(String[] args) throws IOException {
    Formatter fm = new Formatter(System.out);
    // GCpass2 pass1 = new GCpass2("B:/rdavm/ds083.2/grib1/**/.*gbx9", FeatureCollectionType.GRIB1);
    // GCpass2 pass1 = new GCpass2("B:/rdavm/ds627.0/ei.oper.an.pv/**/.*gbx9", FeatureCollectionType.GRIB1);
    // GCpass2 pass1 = new GCpass2("B:/motherlode/rfc/kalr/.*gbx9", FeatureCollectionType.GRIB1, "file");
    GCpass2 pass1 = new GCpass2("B:/lead/NDFD-CONUS_5km/.*gbx9", FeatureCollectionType.GRIB2, "file");
    pass1.scanAndReport(fm);
  }

  ///////////////////////////////////
  FeatureCollectionConfig config;
  Counters countersAll;
  int nrecordsAll = 0;

  public GCpass2(String spec, FeatureCollectionType type, String timePartition) {

    this.config = new FeatureCollectionConfig("testPass1", "grib/testPass1", type, spec, null, null, timePartition, null, null);

    countersAll = new Counters();
    countersAll.add(new Counters.CounterOfString("referenceDate").setShowRange(true));
    countersAll.add(new Counters.CounterOfString("table version"));
    countersAll.add(new Counters.CounterOfString("param"));
    countersAll.add(new Counters.CounterOfInt("vertCoordInGDS"));
    countersAll.add(new Counters.CounterOfInt("predefined"));
    countersAll.add(new Counters.CounterOfInt("thin"));
  }

  public void reportAll(Indent indent, Formatter fm) {
    fm.format("%n");
    reportOneHeader(indent, fm);
    fm.format("%s%40s", indent, "grand total");
    fm.format("%8d ", nrecordsAll);
    fm.format("%8d ", countersAll.get("param").getUnique());
    fm.format("%8d ", countersAll.get("referenceDate").getUnique());
    fm.format("%n");

    countersAll.show(fm);
  }

  public void reportOneHeader(Indent indent, Formatter fm) {
    fm.format("%s%40s #records   #vars  #runtimes%n", indent, "");
  }

  public void reportOneDir(String dir, int nrecords, Counters countersOne, Indent indent, Formatter fm) {
    fm.format("%s%40s", indent, dir+" total");
    fm.format("%8d ", nrecords);
    fm.format("%8d ", countersOne.get("param").getUnique());
    fm.format("%8d ", countersOne.get("referenceDate").getUnique());
    fm.format("%s ", ((Counters.CounterOfString)countersOne.get("referenceDate")).showRange());
    fm.format("%n");
  }

  public void reportOneFile(MFile mfile, int nrecords, Counters countersOne, Indent indent, Formatter fm) {
    fm.format("%s%40s", indent, mfile.getName());
    fm.format("%8d ", nrecords);
    fm.format("%8d ", countersOne.get("param").getUnique());
    fm.format("%8d ", countersOne.get("referenceDate").getUnique());

    int vertCoordInGDS = countersOne.get("vertCoordInGDS").getUnique();
    int predefined = countersOne.get("predefined").getUnique();
    int thin = countersOne.get("thin").getUnique();
    if (vertCoordInGDS != 0) fm.format("vertCoordInGDS=%d ", vertCoordInGDS);
    if (predefined != 0) fm.format("predefined=%d ", vertCoordInGDS);
    if (thin != 0) fm.format("thin=%d ", vertCoordInGDS);
    fm.format("%n");
  }

  public void scanAndReport(Formatter fm) throws IOException {
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
      nrecordsAll = scanDirectoryPartitionRecurse(isGrib1, dpart, config, countersAll, logger, indent, fm);

    } else if (topCollection instanceof DirectoryCollection) {
      // otherwise its a leaf directory
      nrecordsAll = scanLeafCollection(isGrib1, config, countersAll, logger, rootPath, indent, fm);
    }

    reportAll(indent, fm);
  }


  private int scanDirectoryPartitionRecurse(boolean isGrib1, DirectoryPartition dpart,
                                                          FeatureCollectionConfig config,
                                                          Counters countersParent,
                                                          Logger logger, Indent indent, Formatter fm) throws IOException {

    fm.format("%n%sDirectory %s%n", indent, dpart.getRoot());
    indent.incr();
    Counters countersPart = countersParent.makeSubCounters();
    int nrecordsPart = 0;

    for (MCollection part : dpart.makePartitions(CollectionUpdateType.always)) {
      part.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
      try {
        if (part.isLeaf()) {
          Path partPath = Paths.get(part.getRoot());
          nrecordsPart += scanLeafCollection(isGrib1, config, countersPart, logger, partPath, indent, fm);
        } else {
          nrecordsPart += scanDirectoryPartitionRecurse(isGrib1, (DirectoryPartition) part, config, countersPart, logger, indent, fm);
        }
      } catch (Throwable t) {
        logger.warn("Error making partition "+part.getRoot(), t);
        continue; // keep on truckin; can happen if directory is empty
      }
    }   // loop over partitions

    // do this partition; we just did children so never update them

    countersParent.addTo( countersPart);
    reportOneDir(dpart.getRoot(), nrecordsPart, countersPart, indent, fm);
    indent.decr();
    return nrecordsPart;
  }

  /**
   * Update all the grib indices in one directory, and the collection index for that directory
   *
   * @param config  FeatureCollectionConfig
   * @param dirPath directory path
   * @throws IOException
   */
  private int scanLeafCollection(boolean isGrib1, FeatureCollectionConfig config,
                                     Counters countersParent,
                                     Logger logger, Path dirPath, Indent indent, Formatter fm) throws IOException {

    return scanLeafDirectoryCollection(isGrib1, config, countersParent, logger, dirPath, indent, fm);

  }

  /**
   * File Partition: each File is a collection of Grib records, and the collection of all files in the directory is a PartitionCollection.
   * Rewrite the PartitionCollection and optionally its children
   *
   * @param config              FeatureCollectionConfig
   * @return true if partition was rewritten
   * @throws IOException
   */
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
            } catch (IOException e) { e.printStackTrace(); }

          } else {
            Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm.getCollectionName(), dcm, logger);
            try {
              boolean changed = (builder.updateNeeded(updateType) && builder.createIndex(errlog));
              if (changed) anyChange.set(true);
            } catch (IOException e) { e.printStackTrace(); }

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
  }

  /**
   * Update all the grib indices in one directory, and the collection index for that directory
   *
   * @param config  FeatureCollectionConfig
   * @param dirPath directory path
   * @throws IOException
   */
  private int scanLeafDirectoryCollection(boolean isGrib1, FeatureCollectionConfig config,
                                              Counters parentCounters,
                                              Logger logger, Path dirPath,
                                              Indent indent, Formatter fm) throws IOException {

    if (config.ptype == FeatureCollectionConfig.PartitionType.file) {
      reportOneHeader(indent, fm);
      fm.format("%sDirectory %s%n", indent, dirPath);
    }
    int nrecordsDir = 0;
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
          Grib1Index grib1Index = createGrib1Index(mfile, true);
          if (grib1Index == null) {
            System.out.printf("%s%s: read or create failed%n", indent, mfile.getPath());
            continue;
          }
          for (ucar.nc2.grib.grib1.Grib1Record gr : grib1Index.getRecords()) {
            accumGrib1Record(gr, countersOneFile);
            nrecords++;
          }
        } else {
          Grib2Index grib2Index = createGrib2Index(mfile, true);
          if (grib2Index == null) {
            System.out.printf("%s%s: read or create failed%n", indent, mfile.getPath());
            continue;
          }
          for (ucar.nc2.grib.grib2.Grib2Record gr : grib2Index.getRecords()) {
            accumGrib2Record(gr, countersOneFile);
            nrecords++;
          }
        }

        parentCounters.addTo(countersThisDir);
        nrecordsDir += nrecords;
        countersThisDir.addTo(countersOneFile);
        if (config.ptype == FeatureCollectionConfig.PartitionType.file)
          reportOneFile(mfile, nrecords, countersOneFile, indent, fm);
      }
    }

    reportOneDir(dirPath.toString(), nrecordsDir, countersThisDir, indent, fm);
    return nrecordsDir;
  }

  private void accumGrib1Record(ucar.nc2.grib.grib1.Grib1Record gr, Counters counters) throws IOException {

     Grib1SectionGridDefinition gdss = gr.getGDSsection();  // LOOK for multiple gcs
     Grib1SectionProductDefinition pds = gr.getPDSsection();
     String table = pds.getCenter() + "-" + pds.getSubCenter() + "-" + pds.getTableVersion();
     counters.countS("table version", table);
     counters.countS("param", Integer.toString(pds.getParameterNumber()));
     counters.countS("referenceDate", pds.getReferenceDate().toString());

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


  private void accumGrib2Record(ucar.nc2.grib.grib2.Grib2Record gr, Counters counters) throws IOException {

    Grib2SectionIdentification id = gr.getId();
    Grib2SectionProductDefinition pds = gr.getPDSsection();
    Grib2Pds pdss = gr.getPDSsection().getPDS();

     String table = id.getCenter_id() + "-" + id.getSubcenter_id() +"-"+ id.getMaster_table_version() + "-" + id.getLocal_table_version();
     counters.countS("table version", table);
     counters.countS("param", gr.getDiscipline() + "-" + pdss.getParameterCategory() + "-" + pdss.getParameterNumber());
     counters.countS("referenceDate", gr.getReferenceDate().toString());
   }


  private Grib1Index createGrib1Index(MFile mf, boolean readOnly) throws IOException {
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

  private Grib2Index createGrib2Index(MFile mf, boolean readOnly) throws IOException {
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
