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
import thredds.server.catalog.ConfigCatalog;
import thredds.server.catalog.DatasetRoot;
import thredds.server.catalog.DatasetScanConfig;

import java.util.*;

/**
 * Builder of ConfigCatalog
 *
 * @author caron
 * @since 1/15/2015
 */
public class ConfigCatalogBuilder extends CatalogBuilder {
  protected List<DatasetRoot> roots;

  protected DatasetBuilder buildOtherDataset(DatasetBuilder parent, Element elem) {
    if (elem.getName().equals("datasetRoot")) {
      DatasetRoot root = readDatasetRoot(elem);
      if (roots == null) roots = new ArrayList<>();
      roots.add(root);
      return null;
    }

    else if (elem.getName().equals("datasetScan")) {
      return readDatasetScan(parent, elem);
    }

    else if (elem.getName().equals("netcdf") && elem.getNamespace().equals(Catalog.ncmlNS)) {
      if (parent instanceof DatasetScanBuilder) {
        ((DatasetScanBuilder) parent).ncml = elem.detach();
      }
      return null;
    }

    return null;
  }

  protected DatasetBuilder readDatasetScan(DatasetBuilder parent, Element dsElem) {
    DatasetScanConfigBuilder configBuilder = new DatasetScanConfigBuilder(errlog);
    DatasetScanConfig config = configBuilder.readDatasetScanConfig(dsElem);

    DatasetScanBuilder dataset = new DatasetScanBuilder(parent, config);
    readDatasetInfo(dataset, dsElem);

    return dataset;
  }

  protected DatasetRoot readDatasetRoot(Element s) {
    String name = s.getAttributeValue("path");
    String value = s.getAttributeValue("location");
    return new DatasetRoot(name, value);
  }


  public ConfigCatalog makeCatalog() {
    Map<String, Object> flds = setFields();
    if (roots != null) flds.put(Dataset.DatasetRoots, roots);
    return new ConfigCatalog(baseURI, name, flds, datasetBuilders);
  }

}
