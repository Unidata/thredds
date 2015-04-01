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

import net.jcip.annotations.Immutable;
import thredds.client.catalog.tools.DataFactory;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.stream.CdmRemote;

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
    this.urlPath = urlPath;
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
            "urlPath='" + urlPath + '\'' +
            ", service=" + service +
            ", dataFormatS='" + dataFormatS + '\'' +
            ", dataSize=" + dataSize +
            '}';
  }
}
