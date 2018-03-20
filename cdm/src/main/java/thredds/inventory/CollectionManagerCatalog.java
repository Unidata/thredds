/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import thredds.client.catalog.Access;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.tools.CatalogCrawler;
import thredds.client.catalog.tools.DataFactory;
import ucar.nc2.units.DateType;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

/**
 * CollectionManager of datasets from a catalog.
 *
 * @author caron
 * @since Jan 14, 2010
 */
@ThreadSafe
public class CollectionManagerCatalog extends CollectionManagerAbstract implements CatalogCrawler.Listener {
  private final String catalogUrl;
  private long lastScanned;
  private boolean debug = false;
  private List<MFile> mfiles;

  public CollectionManagerCatalog(String collectionName, String collectionSpec, String olderThan, Formatter errlog) {
    super(collectionName, null);

    if (collectionSpec.startsWith(MFileCollectionManager.CATALOG))
      collectionSpec = collectionSpec.substring(MFileCollectionManager.CATALOG.length());

    int pos = collectionSpec.indexOf('?');
    if (pos > 0) {
      this.dateExtractor = new DateExtractorFromName(collectionSpec.substring(pos + 1), true);  // WTF ?
      collectionSpec = collectionSpec.substring(0, pos);
    }

    this.catalogUrl = collectionSpec;
    this.root = System.getProperty("user.dir");
  }

  @Override
  public String getRoot() {
    return null;
  }

  @Override
  public long getLastScanned() {
    return lastScanned;
  }

  @Override
  public long getLastChanged() {
    return 0;
  }

  @Override
  public boolean isScanNeeded() {
    return (mfiles == null); // LOOK
  }

  @Override
  public boolean scan(boolean sendEvent) throws IOException {
    mfiles = new ArrayList<>(100);

    CatalogCrawler crawler = new CatalogCrawler(CatalogCrawler.Type.all_direct, 0, null, this, null, null, null);
    long start = System.currentTimeMillis();
    try {
      crawler.crawl(catalogUrl);
    } finally {
      long took = (System.currentTimeMillis() - start);
      if (debug) System.out.format("***Done " + catalogUrl + " took = " + took + " msecs%n");
    }

    lastScanned = System.currentTimeMillis();
    return true;
  }

  @Override
  public Iterable<MFile> getFilesSorted() {
    return mfiles == null ? new ArrayList<>() : mfiles;
  }

  private static class MFileRemote implements MFile {
    private Object info;
    private final Access access;
    private Date lastModified;

    MFileRemote(Access access) {
      this.access = access;
      for (DateType dateType : access.getDataset().getDates()) {
        if (dateType.getType().equals("modified"))
          lastModified = dateType.getDate();
      }
    }

    @Override
    public long getLastModified() {
      return lastModified == null ? -1 : lastModified.getTime();
    }

    @Override
    public long getLength() {
      return access.getDataSize();
    }

    @Override
    public boolean isDirectory() {
      return false;
    }

    @Override
    public String getPath() {
      return access.getWrappedUrlName();
    }

    @Override
    public String getName() {
      return access.getDataset().getName();
    }

    @Override
    public MFile getParent() throws IOException {
      return null;
    }

    @Override
    public int compareTo(MFile o) {
      return getPath().compareTo(o.getPath());
    }

    @Override
    public Object getAuxInfo() {
      return info;
    }

    @Override
    public void setAuxInfo(Object info) {
      this.info = info;
    }
  }

  ///////////////////////////////
  // CatalogCrawler.Listener

  @Override
  public void getDataset(Dataset ds, Object context) {
    if (ds.hasAccess()) {
      DataFactory tdataFactory = new DataFactory();
      Access access = tdataFactory.chooseDatasetAccess(ds.getAccess());
      if (access == null) throw new IllegalStateException();
      MFileRemote mfile = new MFileRemote(access);
      if (mfile.getPath().endsWith(".xml")) return; // eliminate latest.xml  LOOK kludge-o-rama
      mfiles.add(mfile);
      if (debug) System.out.format("add %s %n", mfile.getPath());
    }
  }


  /* public static void main(String arg[]) throws IOException {
    Formatter errlog = new Formatter();
    String catUrl = "http://thredds.ucar.edu/thredds/catalog/fmrc/NCEP/NDFD/CONUS_5km/files/catalog.xml";
    CollectionManagerCatalog man = new CollectionManagerCatalog("test", catUrl, null, errlog);
    man.debug = true;
    man.scan(true);
    Fmrc fmrc = Fmrc.open(MFileCollectionManager.CATALOG+catUrl, errlog);
    System.out.printf("errlog = %s %n", errlog);
  }  */


}
