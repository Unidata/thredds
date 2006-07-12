// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
 * @version $Revision$ $Date$
 */

public abstract class InvCatalog {
  protected String name, version;
  protected URI baseURI = null;
  protected InvDatasetImpl topDataset;
  protected HashMap dsHash = new HashMap(); // for datasets with ids
  protected HashMap serviceHash = new HashMap(); // for services (unique by name)

  protected ArrayList services = new ArrayList(); // InvService
  protected ArrayList properties = new ArrayList(); // InvProperty
  protected ArrayList datasets = new ArrayList(); // InvDataset
  protected DateType expires;

    /** Protected constructor. */
  protected InvCatalog() {}

    /** Get the name of the catalog */
  public String getName() { return name; }

  /** Get the version of the catalog */
  public String getVersion() { return version; }

  /** Get top-level dataset.
   *  @return top-level InvDataset. May not be null.
   *  @deprecated use getDatasets()
   */
  public InvDataset getDataset() { return topDataset; }

  /** Find a contained dataset by its ID.
   *  @param id : dataset ID
   *  @return InvDataset or null if not found.
   */
  public InvDataset findDatasetByID( String id) {
    return (InvDataset) dsHash.get( id);
  }

  /** Get top-level datasets.
   *  @return List of Invdataset. May be empty, may not be null.
   */
  public java.util.List getDatasets() { return datasets; }

  /** Get top-level services.
   *  @return List of InvService. May be empty, may not be null.
   */
  public java.util.List getServices() { return services; }

  /** Get catalog properties.
   *  @return List of InvProperty. May be empty, may not be null.
   */
  public java.util.List getProperties() { return properties; }

  /**
   * Find named property.
   * @return string value of property or null if not exist.
   */
  public String findProperty(String name) {
    InvProperty result = null;
    java.util.List plist = getProperties();
    for (int i=0; i<plist.size(); i++) {
      InvProperty p = (InvProperty) plist.get(i);
      if (p.getName().equals( name))
        result = p;
    }
    return (result == null) ? null : result.getValue();
  }


  /** Get date catalog expires, or null if none given.*/
  public DateType getExpires() { return expires; }

  /**
   * Find the named service declared in the top level of this catalog.
   * @return  service that matches the given name, or null if none found.
   */
  public InvService findService(String name) {
    if (name == null)  return null;

    for (int k=0; k< services.size(); k++) {
      InvService s = (InvService) services.get(k);
      if (name.equals( s.getName())) return s;

      // look for nested servers
      java.util.List serviceList = s.getServices();
      for (int i = 0; i < serviceList.size(); i++) {
        InvService nested = (InvService) serviceList.get(i);
        if (name.equals( nested.getName())) return nested;
      }
    }

    return null;
  }

  /**
   * Resolve reletive URIs, using the catalog's base URI. If the uriString is not reletive, then
   *  no resolution is done. This also allows baseURI to be a file: scheme.
   * @param uriString any uri, reletive or absolute
   * @return resolved uri string, or null on error
   * @see java.net.URI#resolve
   * @throws URISyntaxException if uriString violates RFC 2396
   */
  public URI resolveUri( String uriString) throws URISyntaxException {
    URI want = new URI(uriString);

    if ((baseURI == null) || want.isAbsolute())
      return want;

    // gotta deal with file ourself
    if (baseURI.getScheme().equals("file")) {
      String baseString = baseURI.toString();
      if ((uriString.length() > 0) && (uriString.charAt(0) == '#'))
        return new URI( baseString+uriString);
      int pos = baseString.lastIndexOf('/');
      if (pos > 0) {
        String r = baseString.substring(0,pos+1) + uriString;
        return new URI(r);
      }
    }

    //otherwise let the URI class resolve it
    return baseURI.resolve( want);
  }

  /** Get the base URI as a String */
  public String getUriString() { return baseURI.toString(); }

  /**
   * Check internal data structures.
   * @param out : print errors here
   * @param show : print messages for each object (debug)
   * @return true if no fatal consistency errors.
   */
  abstract public boolean check(StringBuffer out, boolean show);

  /**
   * Check internal data structures.
   * @param out : print errors here
   * @return true if no fatal consistency errors.
   */
  public boolean check(StringBuffer out) { return check( out, false); }

  /**
   * Munge this catalog so the given dataset is the top catalog.
   * @param dataset make this top; must be existing dataset in this catalog.
   */
  abstract public void subset( InvDataset dataset);

  /**
   * Munge this catalog to remove any dataset that doesnt pass through the filter.
   * @param filter remove datasets that dont pass this filter.
   */
  abstract public void filter( DatasetFilter filter);

}
