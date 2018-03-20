/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.catalog;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import ucar.nc2.units.DateType;

import java.util.List;
import java.util.Date;
import java.util.ArrayList;
import java.io.IOException;

import org.jdom2.Element;
import ucar.nc2.ncml.NcMLReader;

/**
 * A CrawlableDataset from a THREDDS catalog.
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
      Element serviceElement = configElement.getChild("serviceType", XMLEntityResolver.ncmlNS);
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
      InvAccess access = dataset.getAccess( serviceType);
      if (access != null) return access.getStandardUrlName();
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
