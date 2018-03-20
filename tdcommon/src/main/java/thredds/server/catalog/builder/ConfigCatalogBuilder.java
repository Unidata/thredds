/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.catalog.builder;

import org.jdom2.Element;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.Service;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.builder.DatasetBuilder;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.server.catalog.ConfigCatalog;
import thredds.server.catalog.DatasetRootConfig;
import thredds.server.catalog.DatasetScanConfig;

import java.util.*;

/**
 * Builder of ConfigCatalog
 *
 * @author caron
 * @since 1/15/2015
 */
public class ConfigCatalogBuilder extends CatalogBuilder {
  protected List<DatasetRootConfig> roots;

  protected DatasetBuilder buildOtherDataset(DatasetBuilder parent, Element elem) {
    // this finds datasetRoot in catalogs (unwanted side effect in regular dataset elements)
    if (elem.getName().equals("datasetRoot")) {
      DatasetRootConfig root = readDatasetRoot(elem);
      if (roots == null) roots = new ArrayList<>();
      roots.add(root);
      return null;
    }

    else if (elem.getName().equals("catalogScan")) {
      return readCatalogScan(parent, elem);
    }

    else if (elem.getName().equals("datasetScan")) {
      return readDatasetScan(parent, elem);
    }

    else if (elem.getName().equals("featureCollection")) {
      return readFeatureCollection(parent, elem);
    }

    // this finds ncml in regular dataset elements
    else if (elem.getName().equals("netcdf") && elem.getNamespace().equals(Catalog.ncmlNS)) {
      if (parent != null)
        parent.put(Dataset.Ncml, elem.detach());
      return null;
    }

    return null;
  }

  private DatasetRootConfig readDatasetRoot(Element s) {
    String name = s.getAttributeValue("path");
    String value = s.getAttributeValue("location");
    return new DatasetRootConfig(name, value);
  }

  private CatalogScanBuilder readCatalogScan(DatasetBuilder parent, Element s) {
    String name = s.getAttributeValue("name");
    String path = s.getAttributeValue("path");
    String location = s.getAttributeValue("location");
    String watch = s.getAttributeValue("watch");
    return new CatalogScanBuilder(parent, name, path, location, watch);
  }

  @Override
  protected DatasetBuilder readCatalogRef(DatasetBuilder parent, Element catRefElem) {
    DatasetBuilder catref = super.readCatalogRef(parent, catRefElem);

    String useRemoteCatalogService = catRefElem.getAttributeValue("useRemoteCatalogService");
    if (useRemoteCatalogService != null) {
      if (useRemoteCatalogService.equalsIgnoreCase("true"))
        catref.put(Dataset.UseRemoteCatalogService, Boolean.TRUE);
      else if (useRemoteCatalogService.equalsIgnoreCase("false"))
        catref.put(Dataset.UseRemoteCatalogService, Boolean.FALSE);
    }

    return catref;
  }

  private DatasetBuilder readDatasetScan(DatasetBuilder parent, Element dsElem) {
    DatasetScanConfigBuilder configBuilder = new DatasetScanConfigBuilder(errlog);
    DatasetScanConfig config = configBuilder.readDatasetScanConfig(dsElem);
    if (configBuilder.fatalError) {
       // this.fatalError = true;
       return null;

     } else {
      DatasetScanBuilder dataset = new DatasetScanBuilder(parent, config);
      readDatasetInfo(dataset, dsElem);
      for (Element elem : dsElem.getChildren("netcdf", Catalog.ncmlNS)) {
        dataset.put(Dataset.Ncml, elem.detach());
      }
      return dataset;
    }
  }

  private DatasetBuilder readFeatureCollection(DatasetBuilder parent, Element fcElem) {
    thredds.featurecollection.FeatureCollectionConfigBuilder configBuilder = new thredds.featurecollection.FeatureCollectionConfigBuilder(errlog);
    FeatureCollectionConfig config = configBuilder.readConfig(fcElem);
    if (configBuilder.fatalError) {
      // this.fatalError = true;
      return null;

    } else {
      FeatureCollectionRefBuilder dataset = new FeatureCollectionRefBuilder(parent, config);
      readDatasetInfo(dataset, fcElem);
      for (Element elem : fcElem.getChildren("netcdf", Catalog.ncmlNS)) {   // ??
        dataset.put(Dataset.Ncml, elem.detach());
      }
      return dataset;
    }
  }


  public ConfigCatalog makeCatalog() {
    Map<String, Object> flds = setFields();
    if (roots != null) flds.put(Catalog.DatasetRoots, roots);
    // if (catScans != null) flds.put(Catalog.CatalogScan, catScans);
    return new ConfigCatalog(baseURI, name, flds, datasetBuilders);
  }

}
