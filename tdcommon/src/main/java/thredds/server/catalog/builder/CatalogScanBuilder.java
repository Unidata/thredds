/* Copyright */
package thredds.server.catalog.builder;

import thredds.client.catalog.DatasetNode;
import thredds.client.catalog.builder.DatasetBuilder;
import thredds.server.catalog.CatalogScan;

/**
 * CatalogScan Builder
 *
 * @author caron
 * @since 6/17/2015
 */
public class CatalogScanBuilder extends DatasetBuilder {
  String path, location, watch;

  public CatalogScanBuilder(DatasetBuilder parent, String path, String location, String watch) {
    super(parent);
    this.path = path;
    this.location = location;
    this.watch = watch;
  }

  public CatalogScan makeDataset(DatasetNode parent) {
    String xlink = "/thredds/catalog/"+path+"/catalogScan.xml";   // LOOK hardcoded thredds, need context, or make it reletive ??
    return new CatalogScan(parent, xlink, path, location, watch, flds);
  }
}
