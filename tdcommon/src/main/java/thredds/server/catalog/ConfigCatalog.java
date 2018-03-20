/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.catalog;

import thredds.client.catalog.Access;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.CatalogRef;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.builder.AccessBuilder;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.builder.CatalogRefBuilder;
import thredds.client.catalog.builder.DatasetBuilder;
import thredds.server.catalog.builder.CatalogScanBuilder;
import thredds.server.catalog.builder.FeatureCollectionRefBuilder;

import javax.annotation.concurrent.Immutable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TDS Configuration Catalog
 *
 * @author caron
 * @since 1/15/2015
 */
@Immutable
public class ConfigCatalog extends Catalog {

  public ConfigCatalog(URI baseURI, String name, Map<String, Object> flds, List<DatasetBuilder> datasets) {
    super(baseURI, name, flds, datasets);
  }

  public List<DatasetRootConfig> getDatasetRoots() {
    return (List<DatasetRootConfig>) getLocalFieldAsList(Catalog.DatasetRoots);
  }

  public List<CatalogScan> getCatalogScans() {
    List<CatalogScan> result = new ArrayList<>();
    for (Dataset ds : getDatasetsLocal())
      if (ds instanceof CatalogScan)
        result.add((CatalogScan) ds);
    return result;
  }

  // turn ConfigCatalog into a mutable CatalogBuilder so we can mutate
  public CatalogBuilder makeCatalogBuilder() {
    CatalogBuilder builder = new CatalogBuilder(this);
    for (Dataset ds : getDatasetsLocal()) {
      builder.addDataset(makeDatasetBuilder(null, ds));
    }
    return builder;
  }

  private DatasetBuilder makeDatasetBuilder(DatasetBuilder parent, Dataset ds) {

    DatasetBuilder builder;
    if (ds instanceof CatalogScan)
      builder = new CatalogScanBuilder(parent, (CatalogScan) ds);
    else if (ds instanceof FeatureCollectionRef)
      builder = new FeatureCollectionRefBuilder(parent, (FeatureCollectionRef) ds);
    else if (ds instanceof CatalogRef)
      builder = new CatalogRefBuilder(parent, (CatalogRef) ds);
    else
      builder = new DatasetBuilder(parent, ds);

    List<Access> accesses = (List<Access>) ds.getLocalFieldAsList(Dataset.Access);
    for (Access access : accesses)
      builder.addAccess(new AccessBuilder(builder, access));

    if (!(ds instanceof CatalogRef)) {
      for (Dataset nested : ds.getDatasetsLocal())
        builder.addDataset(makeDatasetBuilder(builder, nested));
    }

    return builder;
  }


    /* static public ConfigCatalog makeCatalogWithServices(ConfigCatalog cc, List<Service> services) {
    Map<String, Object> flds = new HashMap<>();

    for (Map.Entry<String, Object> entry : cc.getFldIterator()) {
      flds.put(entry.getKey(), entry.getValue());
    }
    flds.put(Catalog.Services, services);

    //   public ConfigCatalog(URI baseURI, String name, Map<String, Object> flds, List<DatasetBuilder> datasets) {
    return new ConfigCatalog(cc.getBaseURI(), cc.getName(), flds, null);
  }  */


}
