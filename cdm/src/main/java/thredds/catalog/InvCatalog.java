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

import ucar.nc2.units.DateType;

import java.util.*;
import java.net.*;

/**
 * Public interface to a thredds catalog, a virtual directory of datasets.
 * A catalog consists of nested collections of InvDatasets.
 *
 * @author john caron
 */

public abstract class InvCatalog {
  protected String name, version;
  protected URI baseURI = null;
  protected InvDatasetImpl topDataset;
  protected Map<String, InvDataset> dsHash = new HashMap<String, InvDataset>(); // for datasets with ids
  protected Map<String, InvService> serviceHash = new HashMap<String, InvService>(); // for services (unique by name)

  protected List<InvService> services = new ArrayList<InvService>(); // InvService
  protected List<InvProperty> properties = new ArrayList<InvProperty>(); // InvProperty
  protected List<InvDataset> datasets = new ArrayList<InvDataset>(); // InvDataset
  protected DateType expires;

  /**
   * Protected constructor.
   */
  protected InvCatalog() {
  }

  /**
   * Get the name of the catalog
   *
   * @return name of catalog
   */
  public String getName() {
    return name;
  }

  /**
   * Get the version of the catalog
   *
   * @return catalog version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Get top-level dataset.
   *
   * @return top-level InvDataset. May not be null.
   * @deprecated use getDatasets()
   */
  public InvDataset getDataset() {
    return topDataset;
  }

  /**
   * Find a contained dataset by its ID.
   *
   * @param id : dataset ID
   * @return InvDataset or null if not found.
   */
  public InvDataset findDatasetByID(String id) {
    return dsHash.get(id);
  }

  /**
   * Get top-level datasets.
   *
   * @return List of InvDataset. May be empty, may not be null.
   */
  public java.util.List<InvDataset> getDatasets() {
    return datasets;
  }

  /**
   * Get top-level services.
   *
   * @return List of InvService. May be empty, may not be null.
   */
  public java.util.List<InvService> getServices() {
    return services;
  }

  /**
   * Get catalog properties.
   *
   * @return List of InvProperty. May be empty, may not be null.
   */
  public java.util.List<InvProperty> getProperties() {
    return properties;
  }

  /**
   * Find named property.
   *
   * @param name match this name
   * @return string value of property or null if not exist.
   */
  public String findProperty(String name) {
    InvProperty result = null;
    for (InvProperty p : getProperties()) {
      if (p.getName().equals(name))
        result = p;
    }
    return (result == null) ? null : result.getValue();
  }


  /**
   * Get date catalog expires, or null if none given.
   *
   * @return expiration date, or null
   */
  public DateType getExpires() {
    return expires;
  }

  /**
   * Find the named service declared in the top level of this catalog.
   *
   * @param name match this name
   * @return service that matches the given name, or null if none found.
   */
  public InvService findService(String name) {
    if (name == null) return null;

    for (InvService s : services) {
      if (name.equals(s.getName())) return s;

      // look for nested servers
      for (InvService nested : s.getServices()) {
        if (name.equals(nested.getName())) return nested;
      }
    }

    return null;
  }

  /**
   * Resolve reletive URIs, using the catalog's base URI. If the uriString is not reletive, then
   * no resolution is done. This also allows baseURI to be a file: scheme.
   *
   * @param uriString any uri, reletive or absolute
   * @return resolved uri string, or null on error
   * @throws URISyntaxException if uriString violates RFC 2396
   * @see java.net.URI#resolve
   */
  public URI resolveUri(String uriString) throws URISyntaxException {
    URI want = new URI(uriString);

    if ((baseURI == null) || want.isAbsolute())
      return want;

    // gotta deal with file ourself
    String scheme = baseURI.getScheme();
    if ((scheme != null) && scheme.equals("file")) { // LOOK at ucar.nc2.util.NetworkUtils.resolve
      String baseString = baseURI.toString();
      if ((uriString.length() > 0) && (uriString.charAt(0) == '#'))
        return new URI(baseString + uriString);
      int pos = baseString.lastIndexOf('/');
      if (pos > 0) {
        String r = baseString.substring(0, pos + 1) + uriString;
        return new URI(r);
      }
    }

    //otherwise let the URI class resolve it
    return baseURI.resolve(want);
  }

  /**
   * Get the base URI as a String
   *
   * @return baseURI as a String
   */
  public String getUriString() {
    return baseURI.toString();
  }

  /**
   * Check internal data structures.
   *
   * @param out  : print errors here
   * @param show : print messages for each object (debug)
   * @return true if no fatal consistency errors.
   */
  abstract public boolean check(StringBuilder out, boolean show);

  /**
   * Check internal data structures.
   *
   * @param out : print errors here
   * @return true if no fatal consistency errors.
   */
  public boolean check(StringBuilder out) {
    return check(out, false);
  }

  /**
   * Munge this catalog so the given dataset is the top catalog.
   *
   * @param dataset make this top; must be existing dataset in this catalog.
   */
  abstract public void subset(InvDataset dataset);

  /**
   * Munge this catalog to remove any dataset that doesnt pass through the filter.
   *
   * @param filter remove datasets that dont pass this filter.
   */
  abstract public void filter(DatasetFilter filter);

}
