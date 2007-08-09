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

import thredds.datatype.DateType;

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
  abstract public boolean check(StringBuffer out, boolean show);

  /**
   * Check internal data structures.
   *
   * @param out : print errors here
   * @return true if no fatal consistency errors.
   */
  public boolean check(StringBuffer out) {
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
