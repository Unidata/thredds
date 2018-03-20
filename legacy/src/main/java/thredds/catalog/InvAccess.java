/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.stream.CdmRemote;

import java.net.URI;

/**
 * Public interface to a catalog access element, defining how to access a specific web resource.
 *
 * @author john caron
 */

abstract public class InvAccess {
  static private final Logger logger = LoggerFactory.getLogger(InvAccess.class);

  protected InvDataset dataset;
  protected ServiceType type;
  protected DataFormatType dataFormat;
  protected InvService service;
  protected String urlPath;
  protected double dataSize = Double.NaN;

  /**
   * @return the parent dataset. Should not be null.
   */
  public thredds.catalog.InvDataset getDataset() {
    return dataset;
  }

  /**
   * @return the service. Should not be null.
   */
  public thredds.catalog.InvService getService() {
    return service;
  }

  /**
   * @return the urlPath. Should not be null.
   */
  public String getUrlPath() {
    return urlPath;
  }

  /**
   * @return the dataFormatType; may be null, or inherited from dataset.
   */
  public DataFormatType getDataFormatType() {
    return (dataFormat != null) ? dataFormat : dataset.getDataFormatType();
  }

  /**
   * @return the size in bytes. A value of 0.0 or Double.NaN means unknown.
   */
  public double getDataSize() {
    return dataSize;
  }

  /**
   * @return true if it has valid data size info
   */
  public boolean hasDataSize() {
    return dataSize != 0.0 && !Double.isNaN(dataSize);
  }

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

  public String getWrappedUrlName() {
    URI uri = getStandardUri();
    if (uri == null) return null;
    return wrap(uri.toString());
  }

  /**
   * Construct the standard THREDDS access URI for this dataset access method,
   * resolve if the URI is relative.
   *
   * @return the standard fully resolved THREDDS access URI for this dataset access method, or null if error.
   */
  public URI getStandardUri() {
    try {
      InvCatalog cat = dataset.getParentCatalog();
      if (cat == null)
        return new URI(getUnresolvedUrlName());

      return cat.resolveUri(getUnresolvedUrlName());

    } catch (java.net.URISyntaxException e) {
      logger.warn("Error parsing URL= " + getUnresolvedUrlName());
      return null;
    }
  }

  /**
   * Construct "unresolved" URL: service.getBase() + getUrlPath() + service.getSuffix().
   * It is not resolved, so it may be a reletive URL.
   * @return Unresolved Url as a String
   */
  public String getUnresolvedUrlName() {
    return service.getBase() + getUrlPath() + service.getSuffix();
  }

  private String wrap(String url) {
    if (service.getServiceType() == ServiceType.THREDDS)
      return ucar.nc2.thredds.ThreddsDataFactory.SCHEME + url;
    if (service.getServiceType() == ServiceType.CdmRemote)
      return CdmRemote.SCHEME + url;
    return url;
  }
}