/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog.builder;

import thredds.client.catalog.CatalogRef;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.DatasetNode;

/**
 * client CatalogRef Builder
 *
 * @author caron
 * @since 1/9/2015
 */
public class CatalogRefBuilder extends DatasetBuilder {

  private String title;
  private String href;

  public CatalogRefBuilder(DatasetBuilder parent) {
    super(parent);
  }

  public CatalogRefBuilder(DatasetBuilder parent, CatalogRef from) {
    super(parent, from);
    this.title = from.getName();
    this.href = from.getXlinkHref();
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setHref(String href) {
    this.href = href;
  }

  @Override
  public Dataset makeDataset(DatasetNode parent) {
    return new CatalogRef(parent, title, href, flds, accessBuilders, datasetBuilders);
  }
}
