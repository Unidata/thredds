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

package thredds.catalog;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.inventory.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ft.fmrc.Fmrc;
import ucar.nc2.thredds.MetadataExtractor;
import ucar.nc2.units.DateRange;
import ucar.unidata.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Feature Collection (experimental).
 * Like InvDatasetFmrc, this is a InvCatalogRef subclass. So the reference is placed in the parent, but
 * the catalog itself isnt constructed until it is dereferenced by DataRootHandler.makeDynamicCatalog().
 *
 * @author caron
 * @since Mar 3, 2010
 */
@ThreadSafe
public class InvDatasetFeatureCollection extends InvCatalogRef {
  static private final Logger logger = org.slf4j.LoggerFactory.getLogger(InvDatasetFeatureCollection.class);
  static private final String FMRC = "fmrc.ncd";
  static private final String BEST = "best.ncd";
  static private final String SCAN = "files";

  private final String path;
  private final FeatureType featureType;

  private final CollectionSpecParser sp;
  private final DatasetCollectionManager manager;
  private final Fmrc fmrc;
  private final Set<FeatureCollection.FmrcDatasetType> wantDatasets;

  private Object lock = new Object();

  @GuardedBy("lock")
  private volatile InvCatalogImpl topCatalog;
  private volatile InvDatasetScan scan;
  private volatile boolean madeDatasets = false;

  public InvDatasetFeatureCollection(InvDatasetImpl parent, String name, String path, String featureType, FeatureCollection.Config config) {
    super(parent, name, "/thredds/catalog/" + path + "/catalog.xml");
    this.path = path;
    this.featureType = FeatureType.getType(featureType);
    this.wantDatasets = config.fmrcConfig.datasets;

    // one could just pass the config into Fmrc and let it do the management
    Formatter errlog = new Formatter();
    try {
      sp = new CollectionSpecParser(config.spec, errlog);
      manager = new DatasetCollectionManager(sp, config.olderThan, errlog);
      manager.setRecheck(config.recheckEvery);
    } catch (Exception e) {
      throw new RuntimeException(errlog.toString());
    }

    // optional date extraction is used to get rundates when not embedded in the file
    DateExtractor dateExtractor = (sp.getDateFormatMark() == null) ? new DateExtractorNone() : new DateExtractorFromName(sp.getDateFormatMark(), true);
    fmrc = new Fmrc(manager, dateExtractor, config);
  }

  public String getPath() {
    return path;
  }

  public String getTopDirLocation() {
    return sp.getTopDir();
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////

  // called by DataRootHandler.makeDynamicCatalog() when the catref is requested

  public InvCatalogImpl makeCatalog(String match, String orgPath, URI baseURI) {
    logger.debug("FMRC make catalog for " + match + " " + baseURI);
    try {
      if ((match == null) || (match.length() == 0))
        return makeTopCatalog(baseURI);
        /* else if (match.equals(RUNS))
      return makeCatalogRuns(baseURI);
    else if (match.equals(OFFSET))
      return makeCatalogOffsets(baseURI);
    else if (match.equals(FORECAST))
      return makeCatalogForecasts(baseURI); */
      else if (match.startsWith(SCAN) && wantDatasets.contains(FeatureCollection.FmrcDatasetType.Files)) {
        getDatasets(); // ensures scan has been created
        return scan.makeCatalogForDirectory(orgPath, baseURI);
      }

    } catch (Exception e) {
      logger.error("Error making catalog for " + path, e);
    }

    return null;
  }

  /**
   * Make the top catalog of this catref.
   *
   * @param baseURI base URI of the request
   * @return the top FMRC catalog
   * @throws java.io.IOException         on I/O error
   * @throws java.net.URISyntaxException if path is misformed
   */
  private InvCatalogImpl makeTopCatalog(URI baseURI) throws IOException, URISyntaxException {

    // LOOK when does the topcatalog need to be recreated ??  Perhaps when proto changes ?? ALso could affect LastModified

    if (topCatalog == null) {
      synchronized (lock) {
        if (topCatalog == null) {
          InvCatalogImpl parentCatalog = (InvCatalogImpl) getParentCatalog();
          URI myURI = baseURI.resolve(getXlinkHref());
          InvCatalogImpl mainCatalog = new InvCatalogImpl(getName(), parentCatalog.getVersion(), myURI);

          InvDatasetImpl top = new InvDatasetImpl(this); // LOOK clone correct ??
          top.setParent(null);
          InvDatasetImpl parent = (InvDatasetImpl) this.getParent();
          if (parent != null)
            top.transferMetadata(parent); // make all inherited metadata local

          String id = getID();
          if (id == null)
            id = getPath();
          top.setID(id);

          // add Variables, GeospatialCoverage, TimeCoverage
          GridDataset gds = getGridDataset(FMRC); // LOOK may take a long time here. one could cache this info

          ThreddsMetadata tmi = top.getLocalMetadataInheritable();
          if (tmi.getVariables().size() == 0) {
            ThreddsMetadata.Variables vars = MetadataExtractor.extractVariables(this, gds);
            if (vars != null)
              tmi.addVariables(vars);
          }
          if (tmi.getGeospatialCoverage() == null) {
            ThreddsMetadata.GeospatialCoverage gc = MetadataExtractor.extractGeospatial(gds);
            if (gc != null)
              tmi.setGeospatialCoverage(gc);
          }
          if (tmi.getTimeCoverage() == null) {
            DateRange dateRange = MetadataExtractor.extractDateRange(gds);
            if (dateRange != null)
              tmi.setTimeCoverage(dateRange);
          }

          mainCatalog.addDataset(top);

          // any referenced services need to be local
          List serviceLocal = getServicesLocal();
          for (InvService service : parentCatalog.getServices()) {
            if (!serviceLocal.contains(service))
              mainCatalog.addService(service);
          }
          findDODSService(parentCatalog.getServices()); // LOOK kludge

          for (InvDataset ds : getDatasets()) {
            top.addDataset((InvDatasetImpl) ds);
          }
          mainCatalog.finish();

          // wait till completely constructed before switching pointer, for concurrency
          topCatalog = mainCatalog;
        }
      }
    }

    return topCatalog;
  }

  private String dodsService; // LOOK why ??

  private void findDODSService(List<InvService> services) {
    for (InvService service : services) {
      if ((dodsService == null) && (service.getServiceType() == ServiceType.OPENDAP)) {
        dodsService = service.getName();
        return;
      }
      if (service.getServiceType() == ServiceType.COMPOUND)
        findDODSService(service.getServices());
    }
  }

  // these are the child datasets of this catalog
  // the names here are passed back into getNetcdfDataset(), getGridDataset()

  @Override
  public java.util.List<InvDataset> getDatasets() {
    // LOOK do we need to make sure topcatalog has been constructed ??
    // LOOK need thread safety
    if (!madeDatasets) {
      synchronized (lock) {
        if (!madeDatasets) {

          List<InvDataset> datasets = new ArrayList<InvDataset>();

          String id = getID();
          if (id == null) id = getPath();

          if (wantDatasets.contains(FeatureCollection.FmrcDatasetType.TwoD)) {

            InvDatasetImpl ds = new InvDatasetImpl(this, "Forecast Model Run Collection (2D time coordinates)");
            String name = getName() + "_" + FMRC;
            name = StringUtil.replace(name, ' ', "_");
            ds.setUrlPath(this.path + "/" + name);
            ds.setID(id + "/" + name);
            ThreddsMetadata tm = ds.getLocalMetadata();
            tm.addDocumentation("summary", "Forecast Model Run Collection (2D time coordinates).");
            ds.getLocalMetadataInheritable().setServiceName(this.dodsService); // LOOK why ??
            ds.finish();
            datasets.add(ds);
          }

          if (wantDatasets.contains(FeatureCollection.FmrcDatasetType.Best)) {

            InvDatasetImpl ds = new InvDatasetImpl(this, "Best Time Series");
            String name = getName() + "_" + BEST;
            name = StringUtil.replace(name, ' ', "_");
            ds.setUrlPath(this.path + "/" + name);
            ds.setID(id + "/" + name);
            ThreddsMetadata tm = ds.getLocalMetadata();
            tm.addDocumentation("summary", "Best time series, taking the data from the most recent run available.");
            ds.getLocalMetadataInheritable().setServiceName(this.dodsService); // LOOK why ??
            ds.finish();
            datasets.add(ds);
          }

          if (wantDatasets.contains(FeatureCollection.FmrcDatasetType.Files)) {

            // LOOK - replace this with InvDatasetScan( collectionManager) or something
            long olderThan = (long) (1000 * manager.getOlderThanFilterInSecs());
            ScanFilter filter = new ScanFilter(sp.getFilter(), olderThan);
            InvDatasetScan scanDataset = new InvDatasetScan((InvCatalogImpl) this.getParentCatalog(), this, "File_Access", path + "/" + SCAN,
                    sp.getTopDir(), filter, true, "true", false, null, null, null);

            ThreddsMetadata tmi = scanDataset.getLocalMetadataInheritable();
            tmi.setServiceName("all");
            tmi.addDocumentation("summary", "Individual data file, which comprise the Forecast Model Run Collection.");
            tmi.setGeospatialCoverage(null);
            tmi.setTimeCoverage(null);

            scanDataset.finish();
            datasets.add(scanDataset);

            // replace all at once
            this.scan = scanDataset;
          }

          // replace all at once
          this.datasets = datasets;
          finish();
          madeDatasets = true;
        }
      }
    }

    return datasets;
  }

  // specialized filter handles olderThan and/or filename pattern matching

  public static class ScanFilter implements CrawlableDatasetFilter {
    private Pattern p;
    private long olderThan;

    public ScanFilter(Pattern p, long olderThan) {
      this.p = p;
      this.olderThan = olderThan;
    }

    @Override
    public boolean accept(CrawlableDataset dataset) {
      if (dataset.isCollection()) return true;

      if (p != null) {
        java.util.regex.Matcher matcher = p.matcher(dataset.getName());
        if (!matcher.matches()) return false;
      }

      if (olderThan > 0) {
        Date lastModDate = dataset.lastModified();
        if (lastModDate != null) {
          long now = System.currentTimeMillis();
          if (now - lastModDate.getTime() <= olderThan)
            return false;
        }
      }

      return true;
    }

    @Override
    public Object getConfigObject() {
      return null;
    }
  }

  // called by DatasetHandler.getNetcdfFile()

  /**
   * Get the dataset named by the path
   *
   * @param path URL path
   * @return requested dataset
   * @throws IOException if read error
   */
  public NetcdfDataset getNetcdfDataset(String path) throws IOException {
    int pos = path.indexOf("/");
    String type = (pos > -1) ? path.substring(0, pos) : path;
    String name = (pos > -1) ? path.substring(pos + 1) : "";

    // this assumes that these are files. also might be remote datasets from a catalog
    if (type.equals(SCAN)) {
      String filename = new StringBuilder(getTopDirLocation())
              .append(getTopDirLocation().endsWith("/") ? "" : "/")
              .append(name).toString();
      return NetcdfDataset.acquireDataset(null, filename, null, -1, null, null); // no enhancement
    }

    // makeFmrc();
    NetcdfDataset result = null;
    // String location = path;

    if (path.endsWith(FMRC) && wantDatasets.contains(FeatureCollection.FmrcDatasetType.TwoD)) {
      GridDataset gds = getGridDataset(FMRC);
      result = (NetcdfDataset) gds.getNetcdfFile();

    } else if (path.endsWith(BEST) && wantDatasets.contains(FeatureCollection.FmrcDatasetType.Best)) {
      GridDataset gds = getGridDataset(BEST);
      result = (NetcdfDataset) gds.getNetcdfFile();

    } /* else {
      location = name;

      if (type.equals(OFFSET)) {
        int pos1 = name.indexOf(OFFSET_NAME);
        int pos2 = name.indexOf("hr");
        if ((pos1<0) || (pos2<0)) return null;
        String id = name.substring(pos1+OFFSET_NAME.length(), pos2);
        double hour = Double.parseDouble(id);
        result = fmrc.getForecastOffsetDataset( hour);

      } else if (type.equals(RUNS)) {
        int pos1 = name.indexOf(RUN_NAME);
        if (pos1<0) return null;
        String id = name.substring(pos1+RUN_NAME.length());

        DateFormatter formatter = new DateFormatter();
        Date date = formatter.getISODate(id);
        result = fmrc.getRunTimeDataset(date);

      } else if (type.equals(FORECAST)) {
        int pos1 = name.indexOf(FORECAST_NAME);
        if (pos1<0) return null;
        String id = name.substring(pos1+FORECAST_NAME.length());

        DateFormatter formatter = new DateFormatter();
        Date date = formatter.getISODate(id);
        result = fmrc.getForecastTimeDataset(date);
      }
    }    */

    if (null != result) result.setLocation(path); // LOOK seems fishy, probably not thread-safe ??
    return result;
  }


  // called by DatasetHandler.openGridDataset()

  public GridDataset getGridDataset(String name) throws IOException {
    if (name.endsWith(FMRC) && wantDatasets.contains(FeatureCollection.FmrcDatasetType.TwoD)) {
      return fmrc.getDataset2D(null);

    } else if (name.endsWith(BEST) && wantDatasets.contains(FeatureCollection.FmrcDatasetType.Best)) {
      return fmrc.getDatasetBest();
    }

    return null;
  }

  // called by DataRoorHandler.getCrawlableDatasetAsFile()
  // have to remove the extra "files" from the path
  public File getFile(String remaining) {
    if( null == getTopDirLocation()) return null;
    int pos = remaining.indexOf(SCAN);
    StringBuilder fname = new StringBuilder( getTopDirLocation());
    if ( ! getTopDirLocation().endsWith( "/"))
      fname.append( "/");
    fname.append( ( pos > -1 ) ? remaining.substring( pos + SCAN.length() + 1 ) : remaining);
    return new File( fname.toString() );
  }

}
