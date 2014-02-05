package ucar.nc2.grib.collection;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionSingleFile;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import ucar.nc2.grib.GribIndex;
import ucar.nc2.grib.grib2.Grib2Index;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2SectionGridDefinition;
import ucar.nc2.grib.grib2.Grib2SectionIdentification;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CloseableIterator;
import ucar.sparr.Counter;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Description
 *
 * @author John
 * @since 2/5/14
 */
public class SingleRuntimeBuilder {

  protected GribCollection gc;      // make this object
  protected Grib2Customizer tables; // only gets created in makeAggGroups
  protected String name;            // collection name
  protected File directory;         // top directory

  MCollection dcm;
  org.slf4j.Logger logger;

  private SingleRuntimeBuilder(MCollection dcm, org.slf4j.Logger logger) {
    this.dcm = dcm;
    this.logger = logger;
  }

  private boolean createIndex(Formatter errlog) throws IOException {
    if (dcm == null) {
      logger.error("Grib2CollectionBuilder " + gc.getName() + " : cannot create new index ");
      throw new IllegalStateException();
    }

    long start = System.currentTimeMillis();

    List<MFile> files = new ArrayList<>();
    List<Grib2CollectionBuilder.Group> groups = makeGroups(files, errlog);
    List<MFile> allFiles = Collections.unmodifiableList(files);

    // gather into collections with a single runtime
    Map<Long, List<Grib2CollectionBuilder.Group>> runGroups = new HashMap<>();
    for (Grib2CollectionBuilder.Group g : groups) {
      List<Grib2CollectionBuilder.Group> runGroup = runGroups.get(g.runtime.getMillis());
      if (runGroup == null) {
        runGroup = new ArrayList<>();
        runGroups.put(g.runtime.getMillis(), runGroup);
      }
      runGroup.add(g);
    }

    // write each rungroup separately
    Grib2CollectionBuilder builder = new Grib2CollectionBuilder("test", dcm, logger);
    for (List<Grib2CollectionBuilder.Group> runGroup : runGroups.values()) {
      Grib2CollectionBuilder.Group g = runGroup.get(0);
      File indexFileForRuntime = GribCollection.getIndexFile(gc.getName(), directory, g.runtime);

      builder.writeIndex(indexFileForRuntime, runGroup, allFiles);
    }

    //List<MFile> allFiles = Collections.unmodifiableList(files);
    //writeIndex(indexFile, groups, allFiles);

    long took = System.currentTimeMillis() - start;
    logger.debug("That took {} msecs", took);
    return true;
  }


  // read all records in all files,
  // divide into groups based on GDS hash
  // each group has an arraylist of all records that belong to it.
  // for each group, run rectlizer to derive the coordinates and variables
  public List<Grib2CollectionBuilder.Group> makeGroups(List<MFile> allFiles, Formatter errlog) throws IOException {
    Map<GroupAndRuntime, Grib2CollectionBuilder.Group> gdsMap = new HashMap<>();
    Map<String, Boolean> pdsConvert = null;

    logger.debug("Grib2CollectionBuilder {}: makeGroups", gc.getName());
    int fileno = 0;
    Counter statsAll = new Counter(); // debugging

    logger.debug(" dcm={}", dcm);
    FeatureCollectionConfig.GribConfig config = (FeatureCollectionConfig.GribConfig) dcm.getAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG);
    Map<Integer, Integer> gdsConvert = (config != null) ? config.gdsHash : null;
    if (config != null) pdsConvert = config.pdsHash;

    // place each record into its group
    int totalRecords = 0;
    try (CloseableIterator<MFile> iter = dcm.getFileIterator()) { // not sorted
      while (iter.hasNext()) {
        MFile mfile = iter.next();
        Grib2Index index;
        try {                  // LOOK here is where gbx9 files get recreated; do not make collection index
          index = (Grib2Index) GribIndex.readOrCreateIndexFromSingleFile(false, false, mfile, config, CollectionUpdateType.test, logger);
          allFiles.add(mfile);  // add on success

        } catch (IOException ioe) {
          logger.error("Grib2CollectionBuilder " + gc.getName() + " : reading/Creating gbx9 index for file " + mfile.getPath() + " failed", ioe);
          continue;
        }
        int n = index.getNRecords();
        totalRecords += n;

        for (Grib2Record gr : index.getRecords()) { // we are using entire Grib2Record - memory limitations
          if (this.tables == null) {
            Grib2SectionIdentification ids = gr.getId(); // so all records must use the same table (!)
            this.tables = Grib2Customizer.factory(ids.getCenter_id(), ids.getSubcenter_id(), ids.getMaster_table_version(), ids.getLocal_table_version());
            if (config != null) tables.setTimeUnitConverter(config.getTimeUnitConverter());
          }

          gr.setFile(fileno); // each record tracks which file it belongs to
          int gdsHash = gr.getGDSsection().getGDS().hashCode();  // use GDS hash code to group records
          if (gdsConvert != null && gdsConvert.get(gdsHash) != null) // allow external config to muck with gdsHash. Why? because of error in encoding
            gdsHash = gdsConvert.get(gdsHash);                       // and we need exact hash matching

          CalendarDate runtime = gr.getReferenceDate();
          GroupAndRuntime gar = new GroupAndRuntime(gdsHash, runtime.getMillis());
          Grib2CollectionBuilder.Group g = gdsMap.get(gar);
          if (g == null) {
            g = new Grib2CollectionBuilder.Group(gr.getGDSsection(), gdsHash, runtime);
            gdsMap.put(gar, g);
          }
          g.records.add(gr);
        }
        fileno++;
        statsAll.recordsTotal += index.getRecords().size();
      }
    }

    // rectilyze each group independently
    List<Grib2CollectionBuilder.Group> groups = new ArrayList<>(gdsMap.values());
    for (Grib2CollectionBuilder.Group g : groups) {
      Counter stats = new Counter(); // debugging
      g.rect = new Grib2Rectilyser(tables, g.records, g.gdsHash, pdsConvert);
      g.rect.make(config, Collections.unmodifiableList(allFiles), stats, errlog);
      statsAll.add(stats);

      // look for group name overrides
      if (config != null && config.gdsNamer != null)
        g.nameOverride = config.gdsNamer.get(g.gdsHash);
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

  /* public class Group implements Comparable<Group> {
    public final Grib2SectionGridDefinition gdss;
    public final int gdsHash; // may have been modified
    public final CalendarDate runtime;

    public Grib2Rectilyser rect;
    public List<Grib2Record> records = new ArrayList<>();
    public String nameOverride;
    public Set<Integer> fileSet; // this is so we can show just the component files that are in this group

    private Group(Grib2SectionGridDefinition gdss, int gdsHash, CalendarDate runtime) {
      this.gdss = gdss;
      this.gdsHash = gdsHash;
      this.runtime = runtime;
    }

    @Override
    public int compareTo(Group o) {
      int ret = runtime.compareTo(o.runtime);
      if (ret != 0) return ret;
      return Integer.compare(gdsHash, o.gdsHash);
    }
  } */
}
