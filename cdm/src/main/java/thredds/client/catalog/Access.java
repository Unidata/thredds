/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import thredds.client.catalog.tools.DataFactory;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.ft.remote.CdmrFeatureDataset;
import ucar.nc2.stream.CdmRemote;

import javax.annotation.concurrent.Immutable;
import java.net.URI;

/**
 *  A Dataset Access element
 *
 * @author caron
 * @since 1/7/2015
 */
@Immutable
public class Access {                 // (5)
  private final Dataset dataset;
  private final String urlPath;
  private final Service service;
  private final String dataFormatS;
  private final long dataSize;

  public Access(Dataset dataset, String urlPath, Service service, String dataFormatS, long dataSize) {
    this.dataset = dataset;
    this.urlPath = urlPath; // urlPath.startsWith("/") ? urlPath.substring(1) : urlPath;
    this.service = service;
    this.dataFormatS = dataFormatS;
    this.dataSize = dataSize;
  }

  public Dataset getDataset() {
    return dataset;
  }

  public String getUrlPath() {
    return urlPath;
  }

  public Service getService() {
    return service;
  }

  public DataFormatType getDataFormatType() {
    if (dataFormatS == null) return null;
    try {
      return DataFormatType.getType(dataFormatS);
    } catch (Exception e) {
      return null;
    }
  }

  public String getDataFormatName() {
    return dataFormatS;
  }

  public long getDataSize() {
    return dataSize;
  }                   // optional

  /**
   * Get the standard URL, with resolution if the URL is reletive.
   * catalog.resolveURI( getUnresolvedUrlName())
   *
   * @return URL string, or null if error.
   */
  public String getStandardUrlName() {
    URI uri = getStandardUri();
    if (uri == null) return null;
    return uri.toString();
  }

  /**
   * Construct the standard THREDDS access URI for this dataset access method,
   * resolved agaisnt the parent catalog if the URI is relative.
   *
   * @return the standard fully resolved THREDDS access URI for this dataset access method, or null if error.
   */
  public URI getStandardUri() {
    try {
      Catalog cat = dataset.getParentCatalog();
      if (cat == null)
        return new URI(getUnresolvedUrlName());
      return cat.resolveUri(getUnresolvedUrlName());

    } catch (java.net.URISyntaxException e) {
      throw new RuntimeException("Error parsing URL= " + getUnresolvedUrlName());
    }
  }

  /**
   * Construct "unresolved" URL: service.getBase() + getUrlPath() + service.getSuffix().
   * It is not resolved, so it may be a reletive URL.
   * @return unresolved Url as a String
   */
  public String getUnresolvedUrlName() {
    return service.getBase() + getUrlPath() + service.getSuffix();
  }

  public String getWrappedUrlName() {
    URI uri = getStandardUri();
    if (uri == null) return null;

    if (service.getType() == ServiceType.THREDDS)
      return DataFactory.SCHEME + uri;
    if (service.getType() == ServiceType.CdmRemote)
      return CdmRemote.SCHEME + uri;
    if (service.getType() == ServiceType.CdmrFeature)
      return CdmrFeatureDataset.SCHEME + uri;
    return uri.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Access access = (Access) o;

    if (dataSize != access.dataSize) return false;
    if (dataFormatS != null ? !dataFormatS.equals(access.dataFormatS) : access.dataFormatS != null) return false;
    if (service != null ? !service.equals(access.service) : access.service != null) return false;
    if (urlPath != null ? !urlPath.equals(access.urlPath) : access.urlPath != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = urlPath != null ? urlPath.hashCode() : 0;
    result = 31 * result + (service != null ? service.hashCode() : 0);
    result = 31 * result + (dataFormatS != null ? dataFormatS.hashCode() : 0);
    result = 31 * result + (int) (dataSize ^ (dataSize >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "Access{" +
            "service=" + service +
            ", urlPath='" + urlPath + '\'' +
            ", dataFormatS='" + dataFormatS + '\'' +
            ", dataSize=" + dataSize +
            '}';
  }
}
