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

package thredds.inventory;

import net.jcip.annotations.ThreadSafe;
import ucar.nc2.units.DateType;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.ft.fmrc.Fmrc;

import java.io.IOException;
import java.util.Formatter;
import java.util.Date;

import thredds.catalog.*;
import thredds.catalog.crawl.CatalogCrawler;

/**
 * CollectionManager of datasets from a catalog.
 *
 * @author caron
 * @since Jan 14, 2010
 */
@ThreadSafe
public class DatasetCollectionFromCatalog extends DatasetCollectionMFiles implements CatalogCrawler.Listener {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatasetCollectionFromCatalog.class);
  private final String catalogUrl;
  private boolean debug = false;

  public DatasetCollectionFromCatalog(String collection) {
    super(collection);

    if (collection.startsWith(DatasetCollectionMFiles.CATALOG))
      collection = collection.substring(DatasetCollectionMFiles.CATALOG.length());

    int pos = collection.indexOf('?');
    if (pos > 0) {
      this.dateExtractor = new DateExtractorFromName(collection.substring(pos+1), true);
      collection = collection.substring(0,pos);
    }

    this.catalogUrl = collection;
  }

  @Override
  protected boolean hasScans() {
    return true;
  }

  @Override
  protected void reallyScan(java.util.Map<String, MFile> map) throws IOException {
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);
    InvCatalogImpl cat = catFactory.readXML(catalogUrl);
    StringBuilder buff = new StringBuilder();
    boolean isValid = cat.check(buff, false);
    if (!isValid) {
      log.warn("Catalog invalid= "+catalogUrl+" validation output= "+ buff);
      return;
    }

    CatalogCrawler crawler = new CatalogCrawler(CatalogCrawler.USE_ALL_DIRECT, false, this);
    long start = System.currentTimeMillis();
    try {
      crawler.crawl(cat, null, null, map);
    } finally {
      long took = (System.currentTimeMillis() - start);
      if (debug) System.out.format("***Done " + catalogUrl + " took = " + took + " msecs\n");
    }
  }

  @Override
  public void getDataset(InvDataset ds, Object context) {

    if (ds.hasAccess()) {
      ThreddsDataFactory tdataFactory = new ThreddsDataFactory();
      InvAccess access = tdataFactory.chooseDatasetAccess(ds.getAccess());
      MFileRemote mfile = new MFileRemote(access);
      if (mfile.getPath().endsWith(".xml")) return; // eliminate latest.xml  LOOK kludge-o-rama
      java.util.Map<String, MFile> map = (java.util.Map<String, MFile>) context;
      map.put(mfile.getPath(), mfile);
      if (debug) System.out.format("add %s %n", mfile.getPath());
    }

  }

  @Override
  public boolean getCatalogRef(InvCatalogRef dd, Object context) {
    return true;
  }

  private class MFileRemote implements MFile {
    private Object info;
    private final InvAccess access;
    private Date lastModified;

    MFileRemote(InvAccess access) {
      this.access = access;
      for (DateType dateType : access.getDataset().getDates()) {
        if (dateType.getType().equals("modified"))
          lastModified =  dateType.getDate();
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

  public static void main(String arg[]) throws IOException {
    String catUrl = "http://motherlode.ucar.edu:8080/thredds/catalog/fmrc/NCEP/NDFD/CONUS_5km/files/catalog.xml";
    DatasetCollectionFromCatalog man = new DatasetCollectionFromCatalog(catUrl);
    man.debug = true;
    man.scan(true);
    Formatter errlog = new Formatter();
    Fmrc fmrc = Fmrc.open(DatasetCollectionMFiles.CATALOG+catUrl, errlog);
    System.out.printf("errlog = %s %n", errlog);
  }


}
