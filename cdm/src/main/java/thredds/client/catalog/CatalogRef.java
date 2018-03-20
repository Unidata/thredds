/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import thredds.client.catalog.builder.AccessBuilder;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.builder.DatasetBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * A Client CatalogRef
 *
 * @author caron
 * @since 1/7/2015
 */
public class CatalogRef extends Dataset {
  private final String xlink;
  private boolean isRead;

  public CatalogRef(DatasetNode parent, String name, String xlink, Map<String, Object> flds, List<AccessBuilder> accessBuilders, List<DatasetBuilder> datasetBuilders) {
    super(parent, name, flds, accessBuilders, datasetBuilders);
    this.xlink = xlink;
  }

  public String getXlinkHref() {
    return xlink;
  }

  // LOOK not so sure about this, prevents immutable
  public boolean isRead() {
    return isRead;
  }

  public void setRead(boolean isRead) {
    this.isRead = isRead;
  }

  // only present for server catalogs, put here as convenience
  // return Boolean, so can tell if its been set or not.
  public Boolean useRemoteCatalogService() {
    return (Boolean) flds.get(UseRemoteCatalogService);
  }

  /**
   * @return Xlink reference as a URI, resolved
   */
  public URI getURI() {
    try {
      Catalog parent = getParentCatalog();
      if (parent != null)
        return parent.resolveUri(xlink);
    } catch (java.net.URISyntaxException e) {
      return null;
    }
    return null;
  }

  /////////////////////////////////////////////////

  protected String translatePathToReletiveLocation(String dsPath, String configPath) {
    if (dsPath == null) return null;
    if (dsPath.length() == 0) return null;

    if (dsPath.startsWith("/"))
      dsPath = dsPath.substring(1);

    if (!dsPath.startsWith(configPath))
      return null;

    // remove the matching part, the rest is the "reletive location"
    String dataDir = dsPath.substring(configPath.length());
    if (dataDir.startsWith("/"))
      dataDir = dataDir.substring(1);

    return dataDir;
  }

  //////////////////////////////////////////////////////////////////
  private Catalog proxy = null;

  @Override
  public boolean hasNestedDatasets() {
    return true;
  }

  @Override
  public List<Dataset> getDatasets() {
    if (proxy != null)
      return proxy.getDatasets();
    return super.getDatasets();
  }

  @Override
  public List<Dataset> getDatasetsLogical() {
    try {
      ucar.nc2.util.Optional<DatasetNode> opt = readCatref();
      if (!opt.isPresent())
        throw new RuntimeException(opt.getErrorMessage());

      DatasetNode proxy = opt.get();
      return proxy.getDatasets();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public synchronized ucar.nc2.util.Optional<DatasetNode> readCatref() throws IOException {
    if (proxy != null)
      return ucar.nc2.util.Optional.of(proxy);

    CatalogBuilder builder = new CatalogBuilder();
    Catalog cat = builder.buildFromCatref(this);
    if (builder.hasFatalError() || cat == null) {
      return ucar.nc2.util.Optional.empty("Error reading catref " + getURI() + " err=" + builder.getErrorMessage());
    }
    this.proxy = cat;
    return ucar.nc2.util.Optional.of(proxy);
  }


}
