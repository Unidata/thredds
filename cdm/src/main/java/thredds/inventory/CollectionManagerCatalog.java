/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.inventory;

import net.jcip.annotations.ThreadSafe;
import thredds.client.catalog.Access;
import thredds.client.catalog.CatalogRef;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.tools.CatalogCrawler;
import thredds.client.catalog.tools.DataFactory;
import ucar.nc2.units.DateType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Date;
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

    CatalogCrawler crawler = new CatalogCrawler(CatalogCrawler.Type.all_direct, 0, null, this);
    long start = System.currentTimeMillis();
    try {
      crawler.crawl(catalogUrl, null, null, null);
    } finally {
      long took = (System.currentTimeMillis() - start);
      if (debug) System.out.format("***Done " + catalogUrl + " took = " + took + " msecs%n");
    }

    lastScanned = System.currentTimeMillis();
    return true;
  }

  @Override
  public Iterable<MFile> getFilesSorted() {
    return mfiles == null ? new ArrayList<MFile>() : mfiles;
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
      return (long) access.getDataSize();
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


  @Override
  public boolean getCatalogRef(CatalogRef dd, Object context) {
    return true;
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
