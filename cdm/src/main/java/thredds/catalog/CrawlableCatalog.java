/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import ucar.nc2.units.DateType;

import java.util.List;
import java.util.Date;
import java.util.ArrayList;
import java.io.IOException;

import org.jdom.Element;
import ucar.nc2.ncml.NcMLReader;

/**
 * @author caron
 * @since Aug 10, 2007
 */
public class CrawlableCatalog implements CrawlableDataset {
  private String catalogURL;
  private Object configObj;
  private ServiceType serviceType;

  private InvCatalogImpl catalog;
  private InvDatasetImpl dataset;
  private CrawlableCatalog parent;
  private boolean isCollection;

  /**
   * Constructor.
   * @param catalogURL the catalog URL
   * @param configObj a JDOM Element, example:
   *  <pre>
   *    <any>
   *      <serviceType>OPENDAP</serviceType>
   *    </any>
   *  </pre>
   */
  public CrawlableCatalog(String catalogURL, Object configObj) {
    this.catalogURL = catalogURL;
    
    this.configObj = configObj;
    if (configObj instanceof Element) {
      Element configElement = (Element) configObj;
      Element serviceElement = configElement.getChild("serviceType", NcMLReader.ncNS);
      if (null != serviceElement) {
        String service = serviceElement.getTextTrim();
        serviceType = ServiceType.getType(service);
      }
    }

    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);
    catalog = catFactory.readXML(catalogURL);
    dataset = (InvDatasetImpl) catalog.getDataset();
    isCollection = true;
  }

  CrawlableCatalog(CrawlableCatalog parent, InvDatasetImpl dataset) {
    this.parent = parent;
    this.dataset = dataset;
    this.serviceType = parent.serviceType;

    if (dataset instanceof InvCatalogRef) {
      isCollection = true;
    } else {
      isCollection = dataset.hasNestedDatasets();
    }
  }

  public Object getConfigObject() {
    return configObj;
  }

  public String getPath() {
    if (serviceType != null) {
      return dataset.getAccess( serviceType).getStandardUrlName();
    }
    return dataset.getCatalogUrl();
  }

  public String getName() {
    return dataset.getName();
  }

  public CrawlableDataset getParentDataset() {
    return parent;
  }

  public boolean exists() {
    return catalog != null;
  }

  public boolean isCollection() {
    return isCollection;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public CrawlableDataset getDescendant(String relativePath) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List<CrawlableDataset> listDatasets() throws IOException {
    java.util.List<InvDataset> datasets = dataset.getDatasets();

    List<CrawlableDataset> result = new ArrayList<CrawlableDataset>();
    for (InvDataset d : datasets) {
      if (filter(d))
        result.add( new CrawlableCatalog(this, (InvDatasetImpl) d));
    }
    return result;
  }

  public List<CrawlableDataset> listDatasets(CrawlableDatasetFilter filter) throws IOException {
    java.util.List<InvDataset> datasets = dataset.getDatasets();

    List<CrawlableDataset> result = new ArrayList<CrawlableDataset>();
    for (InvDataset d : datasets) {
      if (!filter(d)) continue;

      CrawlableCatalog cc = new CrawlableCatalog(this, (InvDatasetImpl) d);
      if (filter.accept(cc))  result.add(cc);
    }
    return result;
  }

  private boolean filter( InvDataset d) {
    if (serviceType == null) return true;
    return d.getAccess( serviceType) != null;
  }

  public long length() {
    double size = dataset.getDataSize();
    if ((size == 0.0) || Double.isNaN(size))
      return 0;
    else
      return (long) size;
  }

  public Date lastModified() {
    DateType dt = dataset.getLastModifiedDate();
    return (dt == null) ? null : dt.getDate();
  }
}
