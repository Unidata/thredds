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
