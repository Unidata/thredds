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
package thredds.server.catalog.builder;

import org.jdom2.Element;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
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

  @Override
  protected DatasetBuilder readCatalogRef(DatasetBuilder parent, Element catRefElem) {
    DatasetBuilder catref = super.readCatalogRef( parent, catRefElem);

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
    if (roots != null) flds.put(Dataset.DatasetRoots, roots);
    return new ConfigCatalog(baseURI, name, flds, datasetBuilders);
  }

}
