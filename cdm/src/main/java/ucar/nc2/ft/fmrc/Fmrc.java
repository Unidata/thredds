/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.ft.fmrc;

import net.jcip.annotations.ThreadSafe;
import org.jdom.Element;
import thredds.inventory.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.units.DateRange;

import java.util.*;
import java.io.IOException;

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
public class Fmrc {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Fmrc.class);

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
    if (collection.startsWith(DatasetCollectionManager.CATALOG)) {
      DatasetCollectionFromCatalog manager = new DatasetCollectionFromCatalog(collection);
      return new Fmrc(manager, new FeatureCollectionConfig());

    } else if (collection.endsWith(".ncml")) {
      NcmlCollectionReader ncmlCollection = NcmlCollectionReader.open(collection, errlog);
      if (ncmlCollection == null) return null;
      Fmrc fmrc = new Fmrc(ncmlCollection.getDatasetManager(), new FeatureCollectionConfig());
      fmrc.setNcml(ncmlCollection.getNcmlOuter(), ncmlCollection.getNcmlInner());
      return fmrc;
    }

    return new Fmrc(collection, errlog);
  }

  public static Fmrc open(FeatureCollectionConfig config, Formatter errlog) throws IOException {
    if (config.spec.startsWith(DatasetCollectionManager.CATALOG)) {
      DatasetCollectionFromCatalog manager = new DatasetCollectionFromCatalog(config.spec);
      return new Fmrc(manager, config);
    }

    return new Fmrc(config, errlog);
  }

  ////////////////////////////////////////////////////////////////////////
  private final DatasetCollectionManager manager;
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
    this.manager = DatasetCollectionManager.open(collectionSpec, null, errlog);
    this.config = new FeatureCollectionConfig();
    this.config.spec = collectionSpec;
  }

  private Fmrc(FeatureCollectionConfig config, Formatter errlog) {
    this.manager = new DatasetCollectionManager(config, errlog);
    this.config = config;
  }

  // from AggregationFmrc
  public Fmrc(DatasetCollectionManager manager, FeatureCollectionConfig config) {
    this.manager = manager;
    this.config = config;
  }

  public void setNcml(Element outerNcml, Element innerNcml) {
    config.protoConfig.outerNcml = outerNcml;
    config.innerNcml = innerNcml;
  }

  public void close() {
    manager.close();
  }

  // exposed for debugging

  public CollectionManager getManager() {
    return manager;
  }

  public FmrcInv getFmrcInv(Formatter debug) throws IOException {
    return makeFmrcInv( debug);
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  public DateRange getDateRangeForRun(Date run) {
    return fmrcDataset.getDateRangeForRun( run);
  }

  public DateRange getDateRangeForOffset(double offset) {
    return fmrcDataset.getDateRangeForOffset( offset);
  }

  public List<Date> getRunDates() throws IOException {
    checkNeeded( false); // ??
    return fmrcDataset.getRunDates();
  }

  public List<Date> getForecastDates() throws IOException {
    checkNeeded( false); // ??
    return fmrcDataset.getForecastDates();
  }

  // for making offset datasets
  public double[] getForecastOffsets() throws IOException {
    checkNeeded( false); // ??
    return fmrcDataset.getForecastOffsets();
  }

  // LOOK : all of these guys could use ehcache
  public GridDataset getDataset2D(NetcdfDataset result) throws IOException {
    checkNeeded( false);
    GridDataset gds = fmrcDataset.getNetcdfDataset2D(result);
    return gds;
  }

  public GridDataset getDatasetBest() throws IOException {
    checkNeeded( false);
    GridDataset gds =  fmrcDataset.getBest();
    return gds;
  }

  public GridDataset getDatasetBest(FeatureCollectionConfig.BestDataset bd) throws IOException {
    checkNeeded( false);
    GridDataset gds =  fmrcDataset.getBest(bd);
    return gds;
  }

  public GridDataset getRunTimeDataset(Date run) throws IOException {
    checkNeeded( false);
    GridDataset gds =  fmrcDataset.getRunTimeDataset(run);
    return gds;
  }

  public GridDataset getConstantForecastDataset(Date time) throws IOException {
    checkNeeded( false);
    GridDataset gds =  fmrcDataset.getConstantForecastDataset(time);
    return gds;
  }

  public GridDataset getConstantOffsetDataset(double hour) throws IOException {
    checkNeeded( false);
    GridDataset gds =  fmrcDataset.getConstantOffsetDataset(hour);
    return gds;
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
          logger.error(config.spec+": initial fmrcDataset creation failed", t);
          throw new RuntimeException(t);
        }
      }

      try {
        FmrcInv fmrcInv = makeFmrcInv(null);
        fmrcDataset.setInventory(fmrcInv, forceProtoLocal);
        if (logger.isInfoEnabled()) logger.info(config.spec+": make new Dataset, new proto = "+forceProtoLocal);
        if (forceProtoLocal) forceProto = false;
        this.lastInvChanged = System.currentTimeMillis();
        if (forceProtoLocal) this.lastProtoChanged = this.lastInvChanged;

      } catch (Throwable t) {
        logger.error(config.spec+": makeFmrcInv failed");
        throw new RuntimeException(t);
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

  public void checkNeeded(boolean force) {
    synchronized (lock) {
      if (fmrcDataset == null) {
        try {
          manager.rescan();
          update();
          return;
        } catch (Throwable t) {
          logger.error(config.spec+": rescan failed");
          throw new RuntimeException(t);
        }
      }

      if (!force && !manager.isRescanNeeded()) return;
      try {
        if (!manager.rescan()) return;
        update();
      } catch (Throwable t) {
        logger.error(config.spec+": rescan failed");
        throw new RuntimeException(t);
      }
      // needs updating

    }
  }

  // scan has been done, create FmrcInv
  private FmrcInv makeFmrcInv(Formatter debug) throws IOException {
    try {
      Map<Date, FmrInv> fmrMap = new HashMap<Date, FmrInv>(); // all files are grouped by run date in an FmrInv
      List<FmrInv> fmrList = new ArrayList<FmrInv>(); // an fmrc is a collection of fmr

      // get the inventory, sorted by path
      List<MFile> fileList = manager.getFiles();
      for (MFile f : fileList) {
        if (logger.isDebugEnabled())
          logger.debug("Fmrc: "+config.spec+": file="+f.getPath());

        GridDatasetInv inv = null;
        try {
          inv = GridDatasetInv.open(manager, f, config.innerNcml); // inventory is discovered for each GDS
        } catch (IOException ioe) {
          logger.warn("Error opening " + f.getPath() + "(skipped)", ioe.getMessage());
          continue; // skip
        }

        Date runDate = inv.getRunDate();
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
          logger.debug("Fmrc: spec="+config.spec+": fmr rundate="+fmr.getRunDate()+" nfiles= "+fmr.getFiles().size());
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

  public static void main(String[] args) throws IOException {
    Formatter errlog = new Formatter();

    String spec1 = "/data/testdata/ncml/nc/nam_c20s/NAM_CONUS_20km_surface_#yyyyMMdd_HHmm#.grib1";
    String spec2 = "/data/testdata/grid/grib/grib1/data/agg/.*grb";
    String spec3 = "/data/testdata/ncml/nc/ruc_conus40/RUC_CONUS_40km_#yyyyMMdd_HHmm#.grib1";
    String spec4 = "/data/testdata/cdmUnitTest/rtmodels/.*_nmm\\.GrbF[0-9]{5}$";

    String cat1 = "catalog:http://motherlode.ucar.edu:8080/thredds/catalog/fmrc/NCEP/RUC2/CONUS_40km/files/catalog.xml";
    String cat2 = "catalog:http://motherlode.ucar.edu:8080/thredds/catalog/fmrc/NCEP/NDFD/CONUS_5km/files/catalog.xml";

    String specH = "C:/data/datasets/nogaps/US058GMET-GR1mdl.*air_temp";
    String specH2 = "C:/data/ft/grid/cg/.*nc$";
    String specH3 = "C:/data/ft/grid/namExtract/#yyyyMMdd_HHmm#.*nc$";
    Fmrc fmrc = new Fmrc(specH3, errlog);
    System.out.printf("errlog = %s%n", errlog);
  }
}
