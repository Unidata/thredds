// $Id: InvService.java 48 2006-07-12 16:15:40Z caron $
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

import java.util.*;

/**
 * A Service is an abstraction for an internet service, such as a data server, FTP, etc.
 *
 * @author john caron
 * @version $Revision: 48 $ $Date: 2006-07-12 16:15:40Z $
 */

public class InvService {
  private String name;
  private String base;
  private String suffix;
  private String desc;
  private ServiceType type = null;

  private java.net.URI uri = null;
  private ArrayList nestedServices = new ArrayList(); // InvService
  private ArrayList properties = new ArrayList(); // InvProperty
  private ArrayList roots = new ArrayList(); // InvProperty

  private StringBuffer log = new StringBuffer();

  /**
   * Constructor, use when serviceTypeName alreaddy converted to ServiceType.
   * @param name : name to show to the user
   * @param serviceTypeName : ServiceType
   * @param base : base for forming URL
   * @param suffix : suffix for forming URL, may be null.
   * @param desc : human readable description, may be null.
   */
  public InvService( String name, String serviceTypeName, String base, String suffix, String desc) {
    this.name = name;
    this.type = ServiceType.getType(serviceTypeName);
    this.base = base.trim();
    this.suffix = (suffix == null) ? "" : suffix.trim();
    this.desc = desc;

    // deal with strange service types
    if (type == null) {
       log.append(" ** InvService: non-standard type =("+serviceTypeName+") for service ("+name+")");
       type = new ServiceType( serviceTypeName);
    }

  }

  /** Get the server name: referenced by dataset and access elements. */
  public String getName() { return name; }

  /** get the base URL for the server */
  public String getBase() { return base; }

  /** get the Server Type */
  public ServiceType getServiceType() { return type; }

  /** Get the suffix; may be null */
  public String getSuffix() { return suffix; }

  /** Get the "human readable" description; use ServiceType.toString() if not set */
  public String getDescription() { return (desc != null) ? desc : type.toString(); }

  /**
   * Get properties for this service.
   * @return List of type Property. May be empty, but not null.
   */
  public java.util.List getProperties() { return properties; }

  /** Get dataset roots.
   *  @return List of InvProperty. May be empty, may not be null.
   */
  public java.util.List getDatasetRoots() { return roots; }

  /**
   * Get named property.
   * @return String value of Property or null if not exist.
   */
  public String findProperty(String name) {
    InvProperty result = null;
    for (int i=0; i<properties.size(); i++) {
      InvProperty p = (InvProperty) properties.get(i);
      if (p.getName().equals( name))
        result = p;
    }
    return (result == null) ? null : result.getValue();
  }

  /** string representation */
  public String toString() {
    return "name:("+name+") type:("+type+") base:("+base+") suffix:("+suffix+")";
  }

  /** InvServices with same values are equal */
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (! (o instanceof InvService))
      return false;
    return o.hashCode() == this.hashCode();
  }

  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37*result + getName().hashCode();
      result = 37*result + getBase().hashCode();
      result = 37*result + getServiceType().hashCode();
      if (null != getSuffix())
        result = 37*result + getSuffix().hashCode();
      result = 37*result + getProperties().hashCode();
      result = 37*result + nestedServices.hashCode();
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0; // Bloch, item 8

  /** debugging info */
  public String dump() { return dump(0); }
  String dump(int n) {
    StringBuffer buff = new StringBuffer(200);
    buff.setLength(0);

    buff.append(InvDatasetImpl.indent(n)+"Service "+this+"\n");

    List svs = getServices();
    if (svs.size() > 0) {
      for (int i=0; i<svs.size(); i++) {
        InvService s = (InvService) svs.get(i);
        buff.append( s.dump(n+2));
      }
    }

    List props = getProperties();
    if (props.size() > 0) {
      String indent = InvDatasetImpl.indent(n+2);
      buff.append(indent);
      buff.append("Properties:\n");
      for (int i=0; i<props.size(); i++) {
        InvProperty p = (InvProperty) props.get(i);
        buff.append( InvDatasetImpl.indent(n+4) + p + "\n");
      }
    }

    return buff.toString();
  }

  ////////////////////////////////////////////////////////////////////////////
  /**
   * Add a nested service to a service of type COMPOUND.
   */
  public void addService( InvService service) {
    nestedServices.add( service);
  }

  /**
   * Add a property
   */
  public void addProperty(InvProperty p) {
    properties.add( p);
  }


   /** Add Dataset Root (1.0), key = path,  value = location.
    * @deprecated use InvCatalogImpl
    */
  public void addDatasetRoot( InvProperty root) { roots.add( root);}

  /**
   * Get nested services; only if getServiceType() == ServiceType.COMPOUND.
   * @return List of type InvService. May be empty, but not null.
   */
  public java.util.List getServices() {
    return nestedServices;
  }

  /** Get full name for this Service, which has all parent collection names.
   *  @deprecated services should always be at top level.
   */
  public String getFullName() {
    return name;
  }

  protected boolean check(StringBuffer out) {
    boolean isValid = true;
    if (log.length() > 0) {
      out.append( log);
    }

    // compound services
    if (getServiceType() == ServiceType.COMPOUND) {
      if (getServices().size() < 1) {
        out.append(" ** InvService ("+getName()+") type COMPOUND must have a nested service\n");
        isValid = false;
      }
    } else {
     if (getServices().size() > 0) {
        out.append(" ** InvService("+getName()+") type "+getServiceType()+" may not have nested services\n");
        isValid = false;
      }
    }

    // check urlPath is ok
    try {
      uri = new java.net.URI(base);
    } catch (java.net.URISyntaxException e) {
      out.append(" ** InvService("+getName()+") invalid base URL =("+base+")");
      isValid = false;
    }

    return isValid;
  }

  /** See if the service Base is reletive */
  public boolean isRelativeBase() {
    if (getServiceType() == ServiceType.COMPOUND) return false;

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

/**
 * $Log: InvService.java,v $
 * Revision 1.9  2005/07/22 15:56:05  caron
 * no message
 *
 * Revision 1.8  2005/07/08 18:34:59  edavis
 * Fix problem dealing with service URLs that are relative
 * to the catalog (base="") and those that are relative to
 * the collection (base URL is not empty).
 *
 * Revision 1.7  2005/06/28 15:16:28  caron
 * no message
 *
 * Revision 1.6  2004/06/09 00:27:25  caron
 * version 2.0a release; cleanup javadoc
 *
 * Revision 1.5  2004/05/21 17:06:15  edavis
 * Ensure that services added to catalog have unique names.
 *
 * Revision 1.4  2004/05/11 23:30:28  caron
 * release 2.0a
 *
 * Revision 1.3  2004/03/19 20:12:53  caron
 * trim URLs
 *
 * Revision 1.2  2004/02/20 00:49:51  caron
 * 1.3 changes
 *
 * Revision 1.3  2003/05/29 21:30:47  john
 * resolve reletive URLS differently
 *
 * Revision 1.2  2002/11/26 00:05:55  caron
 * merge2 ethan's changes
 *
 * Revision 1.4  2002/11/19 21:15:15  edavis
 * Changes for CatalogGen release 0.6:
 * Allow classes that extend InvServiceImpl access to the validate()
 * method (i.e., change validate() from default to protected).
 *
 * Revision 1.2  2002/07/01 23:35:03  caron
 * release 0.6
 *
 * Revision 1.1  2002/06/28 21:28:26  caron
 * create vresion 6 object model
 *
 * Revision 1.2  2002/03/09 01:45:53  caron
 * better javadoc
 *
 * Revision 1.1.1.1  2002/02/26 17:24:37  caron
 * import sources
 *
 */