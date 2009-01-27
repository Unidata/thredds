/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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