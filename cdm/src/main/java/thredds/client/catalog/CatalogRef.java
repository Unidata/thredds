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
