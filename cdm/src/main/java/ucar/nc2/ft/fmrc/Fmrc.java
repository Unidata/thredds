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
      String catUrl = collection.substring(DatasetCollectionManager.CATALOG.length());
      DatasetCollectionFromCatalog manager = new DatasetCollectionFromCatalog(catUrl, null);
      return new Fmrc(manager);

    } else if (collection.endsWith(".ncml")) {
      NcmlCollectionReader ncmlCollection = NcmlCollectionReader.open(collection, errlog);
      if (ncmlCollection == null) return null;
      Fmrc fmrc = new Fmrc(ncmlCollection.getDatasetManager());
      fmrc.setNcml(ncmlCollection.getNcmlOuter(), ncmlCollection.getNcmlInner());
      return fmrc;
    }

    return new Fmrc(collection, errlog);
  }

  public static Fmrc open(FeatureCollectionConfig.Config config, Formatter errlog) throws IOException {
    if (config.spec.startsWith(DatasetCollectionManager.CATALOG)) {
      String catUrl = config.spec.substring(DatasetCollectionManager.CATALOG.length());
      DatasetCollectionFromCatalog manager = new DatasetCollectionFromCatalog(catUrl, null);
      return new Fmrc(manager);
    }

    return new Fmrc(config, errlog);
  }

  ////////////////////////////////////////////////////////////////////////
  private final CollectionManager manager;
  private final FeatureCollectionConfig.Config config;

  // should be final
  private Element ncmlOuter, ncmlInner;

  // the current state - changing must be thread safe
  private Object lock = new Object();
  private FmrcDataset fmrcDataset;
  private volatile boolean forceProto = false;
  private volatile Date lastInvChanged;
  private volatile Date lastProtoChanged;

  private Fmrc(String collectionSpec, Formatter errlog) {
    manager = new DatasetCollectionManager(collectionSpec, errlog);
    config = new FeatureCollectionConfig.Config();
  }

  private Fmrc(FeatureCollectionConfig.Config config, Formatter errlog) {
    DatasetCollectionManager dcm = new DatasetCollectionManager(config, errlog);
    dcm.setRecheck(config.recheckAfter);
 
    this.manager = dcm;
    this.config = config;
  }

  // from AggregationFmrc
  public Fmrc(CollectionManager manager) {
    this.manager = manager;
    this.config = new FeatureCollectionConfig.Config();
  }

  public void setNcml(Element ncmlOuter, Element ncmlInner) {
    this.ncmlOuter = ncmlOuter;
    this.ncmlInner = ncmlInner;
  }

  public double getOlderThanFilterInSecs() {
    if (manager instanceof DatasetCollectionManager)
      return ((DatasetCollectionManager)manager).getOlderThanFilterInSecs();
    return -1;
  }

  // exposed for debugging

  public CollectionManager getManager() {
    return manager;
  }

  public FmrcInv getFmrcInv(Formatter debug) throws IOException {
    return makeFmrcInv( debug);
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  public void triggerProto() {
    forceProto = true;
  }

  public void triggerRescan() throws IOException {
    checkNeeded( true);
  }

  public List<Date> getRunDates() throws IOException {
    checkNeeded( false); // ??
    return fmrcDataset.getRunDates();
  }

  public List<Date> getForecastDates() throws IOException {
    checkNeeded( false); // ??
    return fmrcDataset.getForecastDates();
  }

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

  // true if things have changed since given time
  public boolean checkInvState(Date lastInvChange) throws IOException {
    checkNeeded(false);
    return !this.lastInvChanged.before(lastInvChange);
  }
  // true if things have changed since given time
  public boolean checkProtoState(Date lastProtoChanged) throws IOException {
    checkNeeded(false);
    return !this.lastProtoChanged.before(lastProtoChanged);
  }

  private void checkNeeded(boolean force) throws IOException {
    synchronized (lock) {
      boolean forceProtoLocal = forceProto;

      if (fmrcDataset == null) {
        try {
          fmrcDataset = new FmrcDataset(config, ncmlInner, ncmlOuter);
          manager.scan(null);
          FmrcInv fmrcInv = makeFmrcInv(null);
          fmrcDataset.setInventory(fmrcInv, forceProtoLocal);
          if (forceProtoLocal) forceProto = false;
          this.lastInvChanged = new Date();
          this.lastProtoChanged = new Date();
          return;
        } catch (Throwable t) {
          logger.error(config.spec+": initial fmrcDataset creation failed", t);
          throw new RuntimeException(t);
        }
      }

      if (!force && !manager.isRescanNeeded()) return;
      
      if (!manager.rescan()) return;

      try {
        FmrcInv fmrcInv = makeFmrcInv(null);
        fmrcDataset.setInventory(fmrcInv, forceProtoLocal);
        if (logger.isInfoEnabled()) logger.info(config.spec+": make new Dataset, new proto = "+forceProtoLocal);
        if (forceProtoLocal) forceProto = false;
        this.lastInvChanged = new Date();
        if (forceProtoLocal) this.lastProtoChanged = new Date();

      } catch (Throwable t) {
        logger.error(config.spec+": rescan failed");
        throw new RuntimeException(t);
      }
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

        GridDatasetInv inv = GridDatasetInv.open(manager, f, ncmlInner); // inventory is discovered for each GDS
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
          logger.debug("Fmrc: "+config.spec+": fmr "+fmr.getRunDate()+" nfiles= "+fmr.getFiles().size());
      }

      return new FmrcInv(manager.getCollectionName(), fmrList, config.fmrcConfig.regularize);

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
