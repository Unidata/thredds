package ucar.nc2.grib.collection;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionManager;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import thredds.inventory.partition.PartitionManager;
import thredds.inventory.partition.PartitionManagerFromIndexList;
import ucar.nc2.grib.GribIndex;
import ucar.nc2.grib.grib2.Grib2Index;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2SectionIdentification;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CloseableIterator;
import ucar.sparr.Counter;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Builds indexes for collections of Grib2 files.
 * May create GC or PC
 *
 * @author John
 * @since 2/5/14
 */
public class Grib2CollectionBuilder {
  private MCollection dcm;
  private org.slf4j.Logger logger;

  private Grib2Customizer tables; // only gets created in makeAggGroups
  protected String name;            // collection name
  protected File directory;         // top directory

  // LOOK prob name could be dcm.getCollectionName()
  public Grib2CollectionBuilder(String name, MCollection dcm, org.slf4j.Logger logger) {
    this.dcm = dcm;
    this.logger = logger;

    this.name = StringUtil2.replace(name, ' ', "_");
    this.directory = new File(dcm.getRoot());
  }

  public File getIndexFile() {
    return GribCollection.getIndexFile(name, directory);
  }

  public boolean updateNeeded(CollectionUpdateType ff) throws IOException {
    if (ff == CollectionUpdateType.never) return false;
    if (ff == CollectionUpdateType.always) return true;

    File idx = GribCollection.getIndexFile(name, directory);
    if (!idx.exists()) return true;

    if (ff == CollectionUpdateType.nocheck) return false;

    return collectionWasChanged(idx.lastModified());
  }

  private boolean collectionWasChanged(long idxLastModified) throws IOException {
    CollectionManager.ChangeChecker cc = GribIndex.getChangeChecker();
    try (CloseableIterator<MFile> iter = dcm.getFileIterator()) {
      while (iter.hasNext()) {
        if (cc.hasChangedSince(iter.next(), idxLastModified)) return true;   // checks both data and gbx9 file
      }
    }
    return false;
  }

  public boolean createIndex(Formatter errlog) throws IOException {
    if (dcm == null) {
      logger.error("Grib2CollectionBuilder " + name + " : cannot create new index ");
      throw new IllegalStateException();
    }

    long start = System.currentTimeMillis();

    List<MFile> files = new ArrayList<>();
    List<Grib2CollectionWriter.Group> groups = makeGroups(files, errlog);
    List<MFile> allFiles = Collections.unmodifiableList(files);

    // gather into collections with a single runtime
    Map<Long, List<Grib2CollectionWriter.Group>> runGroups = new HashMap<>();
    for (Grib2CollectionWriter.Group g : groups) {
      List<Grib2CollectionWriter.Group> runGroup = runGroups.get(g.runtime.getMillis());
      if (runGroup == null) {
        runGroup = new ArrayList<>();
        runGroups.put(g.runtime.getMillis(), runGroup);
      }
      runGroup.add(g);
    }

    // write each rungroup separately
    boolean multipleGroups = runGroups.values().size() > 1;
    List<File> partitions = new ArrayList<>();
    Grib2CollectionWriter writer = new Grib2CollectionWriter(dcm, logger);
    for (List<Grib2CollectionWriter.Group> runGroupList : runGroups.values()) {
      Grib2CollectionWriter.Group g = runGroupList.get(0);
      // if multiple groups, we will write a partition. otherwise, we need to use the standard name (without runtime) so we know the filename from the collection
      String gcname = multipleGroups ? GribCollection.makeName(this.name, g.runtime) : this.name;
      File indexFileForRuntime = multipleGroups ? GribCollection.getIndexFile(name, directory, g.runtime) : GribCollection.getIndexFile(name, directory);
      partitions.add(indexFileForRuntime);

      writer.writeIndex(gcname, indexFileForRuntime, g.makeCoordinateRuntime(), runGroupList, allFiles);
      logger.info("Grib2CollectionBuilder write {}", indexFileForRuntime.getPath());
    }

    boolean ok = true;

    // if theres more than one runtime, create a partition collection to collect all the runtimes together
    if (multipleGroups) {
      Collections.sort(partitions);
      PartitionManager part = new PartitionManagerFromIndexList(dcm.getCollectionName(), dcm.getRoot(), partitions, logger);
      ok = Grib2PartitionBuilder.recreateIfNeeded(part, CollectionUpdateType.always, CollectionUpdateType.always, errlog, logger);
    }

    long took = System.currentTimeMillis() - start;
    logger.debug("That took {} msecs", took);
    return ok;
  }


  // read all records in all files,
  // divide into groups based on GDS hash
  // each group has an arraylist of all records that belong to it.
  // for each group, run rectlizer to derive the coordinates and variables
  public List<Grib2CollectionWriter.Group> makeGroups(List<MFile> allFiles, Formatter errlog) throws IOException {
    Map<GroupAndRuntime, Grib2CollectionWriter.Group> gdsMap = new HashMap<>();

    logger.debug("Grib2CollectionBuilder {}: makeGroups", name);
    int fileno = 0;
    Counter statsAll = new Counter(); // debugging

    logger.debug(" dcm={}", dcm);
    FeatureCollectionConfig config = (FeatureCollectionConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_CONFIG);
    Map<Integer, Integer> gdsConvert = config.gribConfig.gdsHash;
    Map<String, Boolean> pdsConvert = config.gribConfig.pdsHash;

    // place each record into its group
    int totalRecords = 0;
    try (CloseableIterator<MFile> iter = dcm.getFileIterator()) { // not sorted
      while (iter.hasNext()) {
        MFile mfile = iter.next();
        Grib2Index index;
        try {                  // LOOK here is where gbx9 files get recreated; do not make collection index
          index = (Grib2Index) GribIndex.readOrCreateIndexFromSingleFile(false, false, mfile, config.gribConfig, CollectionUpdateType.test, logger);
          allFiles.add(mfile);  // add on success

        } catch (IOException ioe) {
          logger.error("Grib2CollectionBuilder " + name + " : reading/Creating gbx9 index for file " + mfile.getPath() + " failed", ioe);
          continue;
        }
        int n = index.getNRecords();
        totalRecords += n;

        for (Grib2Record gr : index.getRecords()) { // we are using entire Grib2Record - memory limitations
          if (this.tables == null) {
            Grib2SectionIdentification ids = gr.getId(); // so all records must use the same table (!)
            this.tables = Grib2Customizer.factory(ids.getCenter_id(), ids.getSubcenter_id(), ids.getMaster_table_version(), ids.getLocal_table_version());
            if (config != null) tables.setTimeUnitConverter(config.gribConfig.getTimeUnitConverter());
          }

          gr.setFile(fileno); // each record tracks which file it belongs to
          int gdsHash = gr.getGDSsection().getGDS().hashCode();  // use GDS hash code to group records
          if (gdsConvert != null && gdsConvert.get(gdsHash) != null) // allow external config to muck with gdsHash. Why? because of error in encoding
            gdsHash = gdsConvert.get(gdsHash);                       // and we need exact hash matching

          CalendarDate runtime = gr.getReferenceDate();
          GroupAndRuntime gar = new GroupAndRuntime(gdsHash, runtime.getMillis());
          Grib2CollectionWriter.Group g = gdsMap.get(gar);
          if (g == null) {
            g = new Grib2CollectionWriter.Group(gr.getGDSsection(), gdsHash, runtime);
            gdsMap.put(gar, g);
          }
          g.records.add(gr);
        }
        fileno++;
        statsAll.recordsTotal += index.getRecords().size();
      }
    }

    // rectilyze each group independently
    List<Grib2CollectionWriter.Group> groups = new ArrayList<>(gdsMap.values());
    for (Grib2CollectionWriter.Group g : groups) {
      Counter stats = new Counter(); // debugging
      g.rect = new Grib2Rectilyser(tables, g.records, g.gdsHash, pdsConvert);
      g.rect.make(config.gribConfig, Collections.unmodifiableList(allFiles), stats, errlog);
      statsAll.add(stats);

      // look for group name overrides
      if (config.gribConfig.gdsNamer != null)
        g.nameOverride = config.gribConfig.gdsNamer.get(g.gdsHash);
    }

    // debugging and validation
    if (logger.isDebugEnabled()) logger.debug(statsAll.show());

    return groups;
  }

  private class GroupAndRuntime {
    int gdsHash;
    long runtime;

    private GroupAndRuntime(int gdsHash, long runtime) {
      this.gdsHash = gdsHash;
      this.runtime = runtime;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      GroupAndRuntime that = (GroupAndRuntime) o;

      if (gdsHash != that.gdsHash) return false;
      if (runtime != that.runtime) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = gdsHash;
      result = 31 * result + (int) (runtime ^ (runtime >>> 32));
      return result;
    }
  }

}
