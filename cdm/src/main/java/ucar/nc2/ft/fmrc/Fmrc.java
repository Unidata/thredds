/*
 * Copyright (c) 1998-2017 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE.txt for license information.
 */

package ucar.nc2.ft.fmrc;

import org.jdom2.Element;
import org.jdom2.Namespace;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.*;
import ucar.nc2.Attribute;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ncml.NcMLWriter;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;

import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;

/**
 * Forecast Model Run Collection, manages dynamic collections of GridDatasets.
 * Fmrc represents a virtual dataset.
 * To instantiate, you obtain an FmrcInv "snapshot" from which you can call getDatatset().
 * <p/>
 * Assumes that we dont have multiple runtimes in the same file.
 * Can handle different time steps in different files.
 * Can handle different grids in different files. However this creates problems for the "typical dataset".
 * Cannot handle different ensembles in different files.  (LOOK fix)
 * Cannot handle different levels in different files. ok
 *
 * @author caron
 * @since Jan 11, 2010
 */
@ThreadSafe
public class Fmrc implements Closeable {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Fmrc.class);
  static private final Namespace ncNSHttps = thredds.client.catalog.Catalog.ncmlNSHttps;
  static private NcMLWriter ncmlWriter = new NcMLWriter();

  /**
   * Factory method
   *
   * @param collection describes the collection. May be one of:
   *  <ol>
   *  <li>collection specification string
   *  <li>catalog:catalogURL
   *  <li>filename.ncml
   *  <li>
   *  </ol>
   *  collectionSpec date extraction is used to get rundates
   * @param errlog     place error messages here
   * @return Fmrc or null on error
   * @throws IOException on read error
   * @see "http://www.unidata.ucar.edu/software/netcdf-java/reference/collections/CollectionSpecification.html"
   */
  public static Fmrc open(String collection, Formatter errlog) throws IOException {
    if (collection.startsWith(MFileCollectionManager.CATALOG)) {
      CollectionManagerCatalog manager = new CollectionManagerCatalog(collection, collection, null, errlog);
      return new Fmrc(manager, new FeatureCollectionConfig());

    } else if (collection.endsWith(".ncml")) {
      NcmlCollectionReader ncmlCollection = NcmlCollectionReader.open(collection, errlog);
      if (ncmlCollection == null) return null;
      Fmrc fmrc = new Fmrc(ncmlCollection.getCollectionManager(), new FeatureCollectionConfig());
      fmrc.setNcml(ncmlCollection.getNcmlOuter(), ncmlCollection.getNcmlInner());
      return fmrc;
    }

    return new Fmrc(collection, errlog);
  }
  
  public static Fmrc readNcML(String ncmlString, Formatter errlog) throws IOException {
      NcmlCollectionReader ncmlCollection = NcmlCollectionReader.readNcML(ncmlString, errlog);
      if (ncmlCollection == null) return null;
      Fmrc fmrc = new Fmrc(ncmlCollection.getCollectionManager(), new FeatureCollectionConfig());
      fmrc.setNcml(ncmlCollection.getNcmlOuter(), ncmlCollection.getNcmlInner());
      return fmrc;
  }

  public static Fmrc open(FeatureCollectionConfig config, Formatter errlog) throws IOException {
    if (config.spec.startsWith(MFileCollectionManager.CATALOG)) {
      String name = config.collectionName != null ? config.collectionName : config.spec;
      CollectionManagerCatalog manager = new CollectionManagerCatalog(name, config.spec, null, errlog);
      return new Fmrc(manager, config);
    }

    return new Fmrc(config, errlog);
  }

  ////////////////////////////////////////////////////////////////////////
  private final MCollection manager;
  private final FeatureCollectionConfig config;

  // should be final
  // private Element ncmlOuter, ncmlInner;

  // the current state - changing must be thread safe
  private final Object lock = new Object();
  private FmrcDataset fmrcDataset;
  private volatile boolean forceProto = false;
  private volatile long lastInvChanged;
  private volatile long lastProtoChanged;

  private Fmrc(String collectionSpec, Formatter errlog) throws IOException {
    this.manager = MFileCollectionManager.open(collectionSpec, collectionSpec, null, errlog);  // LOOK no name
    this.config = new FeatureCollectionConfig();
    this.config.spec = collectionSpec;
  }

  private Fmrc(FeatureCollectionConfig config, Formatter errlog) {
    this.manager = new MFileCollectionManager(config, errlog, null);
    this.config = config;
  }

  // from AggregationFmrc
  public Fmrc(MCollection manager, FeatureCollectionConfig config) {
    this.manager = manager;
    this.config = config;
  }

  public void setNcml(Element outerNcml, Element innerNcml) {
    config.protoConfig.outerNcml = outerNcml;
    config.innerNcml = innerNcml;
  }

  public void close() {
    if (manager != null)
      manager.close();
  }

  // exposed for debugging

  public MCollection getManager() {
    return manager;
  }

  public FmrcInv getFmrcInv(Formatter debug) throws IOException {
    return makeFmrcInv( debug);
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  public CalendarDateRange getDateRangeForRun(CalendarDate run) {
    return fmrcDataset.getDateRangeForRun( run);
  }

  public CalendarDateRange getDateRangeForOffset(double offset) {
    return fmrcDataset.getDateRangeForOffset( offset);
  }

  public List<CalendarDate> getRunDates() throws IOException {
    checkNeeded( false); // ??
    return fmrcDataset.getRunDates();
  }

  public List<CalendarDate> getForecastDates() throws IOException {
    checkNeeded( false); // ??
    return fmrcDataset.getForecastDates();
  }

  // for making offset datasets
  public double[] getForecastOffsets() throws IOException {
    checkNeeded( false); // ??
    return fmrcDataset.getForecastOffsets();
  }

  public GridDataset getDataset2D(NetcdfDataset result) throws IOException {
    checkNeeded( false);
    return fmrcDataset.getNetcdfDataset2D(result);
  }

  public GridDataset getDatasetBest() throws IOException {
    checkNeeded( false);
    return fmrcDataset.getBest();
  }

  public GridDataset getDatasetBest(FeatureCollectionConfig.BestDataset bd) throws IOException {
    checkNeeded( false);
    return fmrcDataset.getBest(bd);
  }

  public GridDataset getRunTimeDataset(CalendarDate run) throws IOException {
    checkNeeded( false);
    return fmrcDataset.getRunTimeDataset(run);
  }

  public GridDataset getConstantForecastDataset(CalendarDate time) throws IOException {
    checkNeeded( false);
    return fmrcDataset.getConstantForecastDataset(time);
  }

  public GridDataset getConstantOffsetDataset(double hour) throws IOException {
    checkNeeded( false);
    return fmrcDataset.getConstantOffsetDataset(hour);
  }

  /////////////////////////////////////////

  public void updateProto() {
    forceProto = true;
  }

  public void update() {
     synchronized (lock) {
      boolean forceProtoLocal = forceProto;

      if (fmrcDataset == null) {
        try {
          fmrcDataset = new FmrcDataset(config);
        } catch (Throwable t) {
          logger.error(config.name+": initial fmrcDataset creation failed", t);
          //throw new RuntimeException(t);
        }
      }

      try {
        FmrcInv fmrcInv = makeFmrcInv(null);
        fmrcDataset.setInventory(fmrcInv, forceProtoLocal);
        logger.debug("{}: make new Dataset, new proto = {}", config.name, forceProtoLocal);
        if (forceProtoLocal) forceProto = false;
        this.lastInvChanged = System.currentTimeMillis();
        if (forceProtoLocal) this.lastProtoChanged = this.lastInvChanged;

      } catch (Throwable t) {
        logger.error(config.name+": makeFmrcInv failed", t);
        //throw new RuntimeException(t);
      }
    }

  }

  // true if things have changed since given time
  public boolean checkInvState(long lastInvChange) throws IOException {
    return this.lastInvChanged > lastInvChange;
  }
  // true if things have changed since given time
  public boolean checkProtoState(long lastProtoChanged) throws IOException {
    return this.lastProtoChanged > lastProtoChanged;
  }

  private void checkNeeded(boolean force) {
    if (fmrcDataset == null) {
      try {
        update();
      } catch (Throwable t) {
        logger.error(config.name+": rescan failed");
        throw new RuntimeException(t);
      }
    }
  }

  // scan has been done, create FmrcInv
  private FmrcInv makeFmrcInv(Formatter debug) throws IOException {
    try {
      Map<CalendarDate, FmrInv> fmrMap = new HashMap<>(); // all files are grouped by run date in an FmrInv
      List<FmrInv> fmrList = new ArrayList<>(); // an fmrc is a collection of fmr

      // get the inventory, sorted by path
      for (MFile f : manager.getFilesSorted()) {
        Map<String, String> filesRunDateMap = ((MFileCollectionManager) manager).getFilesRunDateMap();
        CalendarDate runDate;

        if (!filesRunDateMap.isEmpty()) {
          // run time has been defined in NcML FMRC agg by the coord attribute,
          // so explicitly set it in the dataset using the _Coordinate.ModelBaseDate
          // global attribute, otherwise the run time offsets might be incorrectly
          // computed if the incorrect run date is found in GridDatasetInv.java (line
          // 177 with comment // Look: not really right )
          runDate = CalendarDate.parseISOformat(null, filesRunDateMap.get(f.getPath()));
          Element element = new Element("netcdf", ncNSHttps);
          Element runDateAttr = ncmlWriter.makeAttributeElement(new Attribute(_Coordinate.ModelRunDate, runDate.toString()));
          config.innerNcml = element.addContent(runDateAttr);
        }

        GridDatasetInv inv;
        try {
          inv = GridDatasetInv.open(manager, f, config.innerNcml); // inventory is discovered for each GDS
        } catch (IOException ioe) {
          logger.warn("Error opening " + f.getPath() + "(skipped)", ioe);
          continue; // skip
        }

        runDate = inv.getRunDate();
        if (debug != null) debug.format("  opened %s rundate = %s%n", f.getPath(), inv.getRunDateString());

        // add to fmr for that rundate
        FmrInv fmr = fmrMap.get(runDate);
        if (fmr == null) {
          fmr = new FmrInv(runDate);
          fmrMap.put(runDate, fmr);
          fmrList.add(fmr);
        }
        fmr.addDataset(inv, debug);
      }
      if (debug != null) debug.format("%n");

      // finish the FmrInv
      Collections.sort(fmrList);
      for (FmrInv fmr : fmrList) {
        fmr.finish();
        if (logger.isDebugEnabled())
          logger.debug("Fmrc:"+config.name+": made fmr with rundate="+fmr.getRunDate()+" nfiles= "+fmr.getFiles().size());
      }

      return new FmrcInv("fmrc:"+manager.getCollectionName(), fmrList, config.fmrcConfig.regularize);

    } catch (Throwable t) {
      logger.error("makeFmrcInv", t);
      throw new RuntimeException(t);
    }
  }

  public void showDetails(Formatter out) throws IOException {
    checkNeeded(false);
    fmrcDataset.showDetails(out);
  }
}
