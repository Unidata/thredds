/*
 * Copyright (c) 1998 - 2014. University Corporation for Atmospheric Research/Unidata
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

package thredds.server.catalog;

import net.jcip.annotations.Immutable;
import thredds.client.catalog.*;
import thredds.client.catalog.builder.AccessBuilder;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.builder.CatalogRefBuilder;
import thredds.client.catalog.builder.DatasetBuilder;
import thredds.filesystem.MFileOS7;
import thredds.inventory.MFile;
import thredds.inventory.MFileFilter;
import thredds.inventory.filter.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;
import ucar.nc2.util.CloseableIterator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * DatasetScan
 *
 * @author John
 * @since 1/12/2015
 */
@Immutable
public class DatasetScan extends CatalogRef {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatasetScan.class);

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private final DatasetScanConfig config;
  private final AddTimeCoverageEnhancer addTimeCoverage;
  private final List<RegExpNamer> namers;
  private final CompositeMFileFilter fileFilters;
  private final CompositeMFileFilter dirFilters;

  public DatasetScan(DatasetNode parent, String name, String xlink, Map<String, Object> flds, List<AccessBuilder> accessBuilders, List<DatasetBuilder> datasetBuilders,
                     DatasetScanConfig config) {
    super(parent, name, xlink, flds, accessBuilders, datasetBuilders);
    this.config = config;

    addTimeCoverage = (config.addTimeCoverage != null) ? new AddTimeCoverageEnhancer(config.addTimeCoverage) : null;

    // namers
    if (config.namers != null && config.namers.size() > 0) {
      namers = new ArrayList<>();
      for (DatasetScanConfig.Namer cname : config.namers)
        namers.add(new RegExpNamer(cname));
    } else {
      namers = null;
    }

    // filters
    if (config.filters != null && config.filters.size() > 0) {
      fileFilters = new CompositeMFileFilter();
      dirFilters = new CompositeMFileFilter();
      for (DatasetScanConfig.Filter cfilter : config.filters) {
        makeFilter(cfilter);
      }
    } else {
      fileFilters = null;
      dirFilters = null;
    }
  }

  private void makeFilter(DatasetScanConfig.Filter cfilter) {
    MFileFilter filter;
    if (cfilter.wildcardAttVal != null) {
      filter = new WildcardMatchOnName(cfilter.wildcardAttVal);   // always on name, not path
    } else if (cfilter.regExpAttVal != null) {
      filter = new RegExpMatchOnName(cfilter.regExpAttVal);
    } else if (cfilter.lastModLimitAttVal > 0) {
      filter = new LastModifiedLimit(cfilter.lastModLimitAttVal);
    } else {
      log.error("Unimplemented DatasetScan filter "+cfilter);
      return;
    }

    if (cfilter.collection)
      dirFilters.addFilter(filter, cfilter.includer);
    if (cfilter.atomic)
      fileFilters.addFilter(filter, cfilter.includer);
  }

  public String getPath() { return config.path; }

  public String getScanLocation() { return config.scanDir; }

  DatasetScanConfig getConfig() {
    return config;
  }

  /////////////////////////////////////////////////////////

  /**
   * Called from DataRootManager.makeDynamicCatalog(), called from LocalCatalogServiceController ...
   * <p/>
   * Build a catalog for the given path by scanning the location
   * associated with this DatasetScan. The given path must start with the path of this DatasetScan.
   *
   * @param orgPath the part of the baseURI that is the path
   * @param baseURI  the base URL for the catalog, used to resolve relative URLs.
   * @return the catalog for this path or null if build unsuccessful.
   */
  public Catalog makeCatalogForDirectory(String orgPath, URI baseURI) throws IOException {

    // Get the dataset location.
    String dataDirReletive = translatePathToLocation(orgPath);
    if (dataDirReletive == null) {
      String tmpMsg = "makeCatalogForDirectory(): Requesting path <" + orgPath + "> must start with \"" + config.path + "\".";
      log.error(tmpMsg);
      return null;
    }
    String parentPath = (dataDirReletive.length() > 1) ? config.path + "/" + dataDirReletive : config.path + "/";
    String parentId = (dataDirReletive.length() > 1) ? this.getId() + "/" + dataDirReletive : this.getId() + "/";

    // translate any properties         LOOK this should be done at configure time
    //String scanDir = ConfigCatalog.translateAlias(config.scanDir);
    String dataDirComplete = (dataDirReletive.length() > 1) ? config.scanDir + "/" + dataDirReletive : config.scanDir;

    // Setup and create catalog builder.
    CatalogBuilder catBuilder = new CatalogBuilder();
    catBuilder.setBaseURI(baseURI);
    assert this.getParentCatalog() != null;
    for (Service s : this.getParentCatalog().getServices())
      catBuilder.addService(s);

    DatasetBuilder top = new DatasetBuilder(null);
    String name = (dataDirReletive.length() > 1) ? dataDirReletive : getName();
    top.transferMetadata(this, true);
    top.setName(name);
    top.put(Dataset.Id, null); // no id for top
    catBuilder.addDataset(top);

    Path p = Paths.get(dataDirComplete);
    if (!Files.exists(p)) throw new FileNotFoundException("Directory does not exist =" + dataDirComplete);
    if (!Files.isDirectory(p)) throw new FileNotFoundException("Not a directory =" + dataDirComplete);

    // scan and sort the directory
    List<MFile> mfiles = getSortedFiles(p, config.isSortIncreasing);

    if (config.addLatest != null && config.addLatest.latestOnTop)
      top.addDataset(makeLatestProxy(top, parentId));

    // create Datasets
    for (MFile mfile : mfiles) {
      DatasetBuilder ds;

      if (mfile.isDirectory()) {
        CatalogRefBuilder catref = new CatalogRefBuilder(top);
        catref.setTitle(makeName(mfile));
        catref.setHref(mfile.getName() + "/catalog.xml");
        top.addDataset(catref);
        ds = catref;

      } else {
        ds = new DatasetBuilder(top);
        ds.setName( makeName(mfile));
        String urlPath = parentPath + mfile.getName();
        ds.put(Dataset.UrlPath, urlPath);
        ds.put(Dataset.DataSize, mfile.getLength());   // <dataSize units="Kbytes">54.73</dataSize>
        CalendarDate date = CalendarDate.of(mfile.getLastModified());
        ds.put(Dataset.Dates, new DateType(date).setType("modified"));   // <date type="modified">2011-09-02T20:50:58.288Z</date>

        if (addTimeCoverage != null)
          addTimeCoverage.addMetadata(ds, mfile);

        top.addDataset(ds);
      }

      ds.put(Dataset.Id, parentId + mfile.getName());
    }

    if (config.addLatest != null && !config.addLatest.latestOnTop)
      top.addDataset(makeLatestProxy(top, parentId));

    // make the catalog
    return catBuilder.makeCatalog();
  }

  private String translatePathToLocation(String dsPath) {
    if (dsPath == null) return null;
    if (dsPath.length() == 0) return null;

    if (dsPath.startsWith("/"))
      dsPath = dsPath.substring(1);

    if (!dsPath.startsWith(config.path))
      return null;

    // remove the matching part, the rest is the "data directory"
    String dataDir = dsPath.substring(config.path.length());
    if (dataDir.startsWith("/"))
      dataDir = dataDir.substring(1);

    if (!dataDir.endsWith("/"))
      dataDir = dataDir + "/";

    return dataDir;
  }

  ///////////////////////
  // Scan and sort

  private List<MFile> getSortedFiles(Path p, final boolean isSortIncreasing) throws IOException {

    // scan the directory
    List<MFile> mfiles = new ArrayList<>();
    try (DatasetScanMFileIterator iter = new DatasetScanMFileIterator(p)) {
      while (iter.hasNext())
        mfiles.add(iter.next());
    }

    // sort them
    Collections.sort(mfiles, new Comparator<MFile>() {
      public int compare(MFile o1, MFile o2) {
        if (o1.isDirectory() != o2.isDirectory())
          return o1.isDirectory() ? 1 : -1;

        if (isSortIncreasing)
          return o1.getName().compareTo(o2.getName());
        else
          return o2.getName().compareTo(o1.getName());
      }
    });

    return mfiles;
  }

  private class DatasetScanMFileIterator implements CloseableIterator<MFile> {
    DirectoryStream<Path> dirStream;
    Iterator<Path> dirStreamIterator;
    MFile nextMFile;
    long now;

    DatasetScanMFileIterator(Path p) throws IOException {
      dirStream = Files.newDirectoryStream(p);
      dirStreamIterator = dirStream.iterator();
      now = System.currentTimeMillis();
    }

    public boolean hasNext() {

      while (true) {
        if (!dirStreamIterator.hasNext()) {
          nextMFile = null;
          return false;
        }

        try {
          Path nextPath = dirStreamIterator.next();
          BasicFileAttributes attr = Files.readAttributes(nextPath, BasicFileAttributes.class);
          nextMFile = new MFileOS7(nextPath, attr);
          if (accept(nextMFile))
            return true;

        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }

    private boolean accept(MFile mfile) {
      if (mfile.isDirectory())
        return dirFilters == null || dirFilters.accept(mfile);
      return fileFilters == null || fileFilters.accept(mfile);
    }

    public MFile next() {
      if (nextMFile == null) throw new NoSuchElementException();
      return nextMFile;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    // better alternative is for caller to send in callback (Visitor pattern)
    // then we could use the try-with-resource
    public void close() throws IOException {
      dirStream.close();
    }
  }

  ////////////////////////////////////////////////
  // Naming

  private String makeName(MFile mfile) {
    if (namers == null) return mfile.getName();
    for (RegExpNamer namer : namers) {
      String result = namer.rename(mfile);
      if (result != null) return result;
    }
    return mfile.getName();
  }

  private static class RegExpNamer {
    private java.util.regex.Pattern pattern;
    DatasetScanConfig.Namer namer;

    RegExpNamer(DatasetScanConfig.Namer namer) {
      this.pattern = java.util.regex.Pattern.compile(namer.regExp);
      this.namer = namer;
    }

    public String rename(MFile mfile) {
      String name = namer.onName ? mfile.getName() : mfile.getPath();
      java.util.regex.Matcher matcher = this.pattern.matcher(name);
      if (!matcher.find()) return null;

      StringBuffer startTime = new StringBuffer();
      matcher.appendReplacement(startTime, namer.replaceString);
      startTime.delete(0, matcher.start());

      if (startTime.length() == 0) return null;
      return startTime.toString();
    }
  }

  //////////////////////////////////////////////////////////
  // add TimeCovergae
  private static class AddTimeCoverageEnhancer {
    private DatasetScanConfig.AddTimeCoverage atc;
    private boolean matchOnName;
    private String matchPattern;
    private java.util.regex.Pattern pattern;

    AddTimeCoverageEnhancer(DatasetScanConfig.AddTimeCoverage atc) {
      this.atc = atc;
      this.matchOnName = (atc.matchName != null);
      this.matchPattern = (atc.matchName != null) ? atc.matchName : atc.matchPath;
      try {
        this.pattern = java.util.regex.Pattern.compile(this.matchPattern);
      } catch (java.util.regex.PatternSyntaxException e) {
        log.error("ctor(): bad match pattern <" + this.matchPattern + ">, failed to compile: " + e.getMessage());
        this.pattern = null;
      }
    }

    boolean addMetadata(DatasetBuilder dataset, MFile crDataset) {
      if (this.pattern == null) return false;

      String matchTargetString = (this.matchOnName) ? crDataset.getName() : crDataset.getPath();

      java.util.regex.Matcher matcher = this.pattern.matcher(matchTargetString);
      if (!matcher.find()) {
        return (false); // Pattern not found.
      }
      StringBuffer startTime = new StringBuffer();
      try {
        matcher.appendReplacement(startTime, atc.subst);
      } catch (IndexOutOfBoundsException e) {
        log.error("addMetadata(): capture group mismatch between match pattern <" + this.matchPattern + "> and substitution pattern <" + atc.subst + ">: " + e.getMessage());
        return (false);
      }
      startTime.delete(0, matcher.start());

      try {
        DateRange dateRange = new DateRange(new DateType(startTime.toString(), null, null), null, new TimeDuration(atc.duration), null);
        dataset.put(Dataset.TimeCoverage, dateRange);

      } catch (Exception e) {
        log.warn("addMetadata(): Start time <" + startTime.toString() + "> or duration <" + atc.duration + "> not parsable" +
                " (crDataset.getName() <" + crDataset.getName() + ">, this.matchPattern() <" + this.matchPattern + ">, this.substitutionPattern() <" + atc.subst + ">): " + e.getMessage());
        return (false);
      }

      return (true);
    }
  }

  //////////////////
  /*
    // specialized filter handles olderThan and/or filename pattern matching
  // for DatasetScan
  static class ScanFilter implements CrawlableDatasetFilter {
    private final Pattern p;
    private final long olderThan;

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
   */

  /* <dataset name="latest.xml" ID="testGridScan/latest.xml" urlPath="latest.xml">
     <serviceName>latest</serviceName>
   </dataset> */
  private DatasetBuilder makeLatestProxy(DatasetBuilder parent, String parentId) {
    DatasetBuilder proxy = new DatasetBuilder(parent);
    proxy.setName(config.addLatest.latestName);
    proxy.put(Dataset.UrlPath, config.addLatest.latestName);
    proxy.put(Dataset.Id, parentId + config.addLatest.latestName);
    proxy.put(Dataset.ServiceName, config.addLatest.latestServiceName);
    return proxy;
  }

  /**
   * Build a catalog for the given resolver path by scanning the
   * location associated with this InvDatasetScan. The given path must start
   * with the path of this DatasetScan and refer to a resolver
   * ProxyDatasetHandler that is part of this InvDatasetScan.
   *
   * @param orgPath    the part of the baseURI that is the path
   * @param baseURI the base URL for the catalog, used to resolve relative URLs.
   * @return the resolver catalog for this path (uses version 1.1) or null if build unsuccessful.
   */
  public Catalog makeLatestResolvedCatalog(String orgPath, URI baseURI) throws IOException {

    // Get the dataset path.
    String dataDirReletive = translatePathToLocation(orgPath);
    if (dataDirReletive == null) {
      String tmpMsg = "makeCatalogForDirectory(): Requesting path <" + orgPath + "> must start with \"" + config.path + "\".";
      log.error(tmpMsg);
      return null;
    }
    String parentPath = (dataDirReletive.length() > 1) ? config.path + "/" + dataDirReletive : config.path + "/";
    String parentId = (dataDirReletive.length() > 1) ? this.getId() + "/" + dataDirReletive : this.getId() + "/";

    // translate any properties
    // String scanDir = ConfigCatalog.translateAlias(config.scanDir);
    String dataDirComplete = (dataDirReletive.length() > 1) ? config.scanDir + "/" + dataDirReletive : config.scanDir;

    // Setup and create catalog builder.
    CatalogBuilder catBuilder = new CatalogBuilder();
    catBuilder.setBaseURI(baseURI);
    for (Service s : this.getParentCatalog().getServices())
      catBuilder.addService(s);

    Path p = Paths.get(dataDirComplete);
    if (!Files.exists(p)) throw new FileNotFoundException("Directory does not exist =" + dataDirComplete);
    if (!Files.isDirectory(p)) throw new FileNotFoundException("Not a directory =" + dataDirComplete);

    // scan and sort the directory
    List<MFile> mfiles = getSortedFiles(p, false); // latest on top

    long now = System.currentTimeMillis();

    for (MFile mfile : mfiles) {
      if (mfile.isDirectory()) continue;

      if (config.addLatest.lastModLimit > 0) {
        if (now - mfile.getLastModified() < config.addLatest.lastModLimit)
          continue;
      }

      // this is the one we want
      DatasetBuilder ds = new DatasetBuilder(null);
      ds.transferMetadata(this, true);

      ds.setName( makeName(mfile));
      String urlPath = parentPath + mfile.getName();
      ds.put(Dataset.UrlPath, urlPath);
      ds.put(Dataset.DataSize, mfile.getLength());   // <dataSize units="Kbytes">54.73</dataSize>
      CalendarDate date = CalendarDate.of(mfile.getLastModified());
      ds.put(Dataset.Dates, new DateType(date).setType("modified"));   // <date type="modified">2011-09-02T20:50:58.288Z</date>
      ds.put(Dataset.Id, parentId + mfile.getName());

      if (addTimeCoverage != null)
        addTimeCoverage.addMetadata(ds, mfile);

      catBuilder.addDataset(ds);
      break; // only the one
    }

    // make the catalog
    return catBuilder.makeCatalog();
  }


}
