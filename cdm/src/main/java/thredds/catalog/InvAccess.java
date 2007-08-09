/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package thredds.catalog;

import java.net.URI;

/**
 * Public interface to an access element, defining how to access a specific web resource.
 *
 * @author john caron
 */

abstract public class InvAccess {

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
      System.err.println("Error parsing URL= " + getUnresolvedUrlName());
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
      return "thredds:" + url;
    return url;
  }
}