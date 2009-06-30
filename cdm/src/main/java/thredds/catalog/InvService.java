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

import java.util.*;

/**
 * A Service is an abstraction for an internet service, such as a data server, FTP, etc.
 *
 * @author john caron
 */

public class InvService {
  private String name;
  private String base;
  private String suffix;
  private String desc;
  private ServiceType type = null;

  private java.net.URI uri = null;
  private List<InvService> nestedServices = new ArrayList<InvService>();
  private List<InvProperty> properties = new ArrayList<InvProperty>();
  private List<InvProperty> roots = new ArrayList<InvProperty>(); // deprecated

  private StringBuilder log = new StringBuilder();

  /**
   * Constructor, use when serviceTypeName alreaddy converted to ServiceType.
   *
   * @param name            : name to show to the user
   * @param serviceTypeName : ServiceType
   * @param base            : base for forming URL
   * @param suffix          : suffix for forming URL, may be null.
   * @param desc            : human readable description, may be null.
   */
  public InvService(String name, String serviceTypeName, String base, String suffix, String desc) {
    this.name = name;
    this.type = ServiceType.findType(serviceTypeName);
    this.base = (base == null) ? "" : base.trim();
    this.suffix = (suffix == null) ? "" : suffix.trim();
    this.desc = desc;

    // deal with strange service types
    if (type == null) {
      log.append(" ** InvService: non-standard type =(").append(serviceTypeName).append(") for service (").append(name).append(")");
      type = ServiceType.getType(serviceTypeName);
    }

    if (name == null) {
      log.append(" ** InvService has no name");
    }

  }

  /**
   * Get the service name: referenced by dataset and access elements.
   * @return the service name
   */
  public String getName() {
    return name;
  }

  /**
   * get the base URL for the service
   * @return the base URL for the service
   */
  public String getBase() {
    return base;
  }

  /**
   * get the Service Type
   * @return the Service Type
   */
  public ServiceType getServiceType() {
    return type;
  }

  /**
   * Get the suffix; may be null
   * @return the suffix; may be null
   */
  public String getSuffix() {
    return suffix;
  }

  /**
   * Get the "human readable" description; use ServiceType.toString() if not set
   * @return the "human readable" description
   */
  public String getDescription() {
    return (desc != null) ? desc : type.toString();
  }

  /**
   * Get properties for this service.
   *
   * @return List of type Property. May be empty, but not null.
   */
  public java.util.List<InvProperty> getProperties() {
    return properties;
  }

  /**
   * Get dataset roots.
   *
   * @return List of InvProperty. May be empty, may not be null.
   */
  public java.util.List<InvProperty> getDatasetRoots() {
    return roots;
  }

  /**
   * Get named property.
   *
   * @param name match this name
   * @return String value of Property or null if not exist.
   */
  public String findProperty(String name) {
    InvProperty result = null;
    for (InvProperty p : properties) {
      if (p.getName().equals(name))
        result = p;
    }
    return (result == null) ? null : result.getValue();
  }

  /**
   * string representation
   */
  public String toString() {
    return "name:(" + name + ") type:(" + type + ") base:(" + base + ") suffix:(" + suffix + ")";
  }

  /**
   * InvServices with same values are equal
   */
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof InvService))
      return false;
    return o.hashCode() == this.hashCode();
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37 * result + getName().hashCode();
      result = 37 * result + getBase().hashCode();
      result = 37 * result + getServiceType().hashCode();
      if (null != getSuffix())
        result = 37 * result + getSuffix().hashCode();
      result = 37 * result + getProperties().hashCode();
      result = 37 * result + nestedServices.hashCode();
      hashCode = result;
    }
    return hashCode;
  }

  private volatile int hashCode = 0; // Bloch, item 8

  /**
   * @return debugging info
   */
  public String dump() {
    return dump(0);
  }

  String dump(int n) {
    StringBuilder buff = new StringBuilder(200);
    buff.setLength(0);

    buff.append(InvDatasetImpl.indent(n)).append("Service ").append(this).append("\n");

      for (InvService s : getServices()) {
        buff.append(s.dump(n + 2));
    }

    List<InvProperty> props = getProperties();
    if (props.size() > 0) {
      String indent = InvDatasetImpl.indent(n + 2);
      buff.append(indent);
      buff.append("Properties:\n");
      for (InvProperty p : props) {
        buff.append(InvDatasetImpl.indent(n + 4)).append(p).append("\n");
      }
    }

    return buff.toString();
  }

  ////////////////////////////////////////////////////////////////////////////
  /**
   * Add a nested service to a service of type COMPOUND.
   * @param service add this
   */
  public void addService(InvService service) {
    nestedServices.add(service);
  }

  /**
   * Add a property
   * @param p add this
   */
  public void addProperty(InvProperty p) {
    properties.add(p);
  }


  /**
   * Add Dataset Root (1.0), key = path,  value = location.
   *
   * @param root add this
   * @deprecated use InvCatalogImpl
   */
  public void addDatasetRoot(InvProperty root) {
    roots.add(root);
  }

  /**
   * Get nested services; only if getServiceType() == ServiceType.COMPOUND.
   *
   * @return List of type InvService. May be empty, but not null.
   */
  public java.util.List<InvService> getServices() {
    return nestedServices;
  }

  /**
   * Get full name for this Service, which has all parent collection names.
   *
   * @deprecated services should always be at top level.
   * @return name
   */
  public String getFullName() {
    return name;
  }

  protected boolean check(StringBuilder out) {
    boolean isValid = true;
    if (log.length() > 0) {
      out.append(log);
    }

    // compound services
    if (getServiceType() == ServiceType.COMPOUND) {
      if (getServices().size() < 1) {
        out.append(" ** InvService (").append(getName()).append(") type COMPOUND must have a nested service\n");
        isValid = false;
      }
    } else {
      if (getServices().size() > 0) {
        out.append(" ** InvService(").append(getName()).append(") type ").append(getServiceType()).append(" may not have nested services\n");
        isValid = false;
      }
    }

    // check urlPath is ok
    try {
      uri = new java.net.URI(base);
    } catch (java.net.URISyntaxException e) {
      out.append(" ** InvService(").append(getName()).append(") invalid base URL =(").append(base).append(")");
      isValid = false;
    }

    return isValid;
  }

  /**
   * See if the service Base is reletive
   * @return true if the service Base is reletive
   */
  public boolean isRelativeBase() {
    if (getServiceType() == ServiceType.COMPOUND)
      return true;

    if (uri == null) {
      try {
        uri = new java.net.URI(base);
      } catch (java.net.URISyntaxException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }

    return !uri.isAbsolute();
  }
}