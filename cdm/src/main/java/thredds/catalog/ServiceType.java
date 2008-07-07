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

/**
 * Type-safe enumeration of THREDDS Service types.
 *
 * @author john caron
 */

public final class ServiceType {
  private static java.util.List<ServiceType> members = new java.util.ArrayList<ServiceType>(20);

  public final static ServiceType NONE = new ServiceType("");

  public final static ServiceType ADDE = new ServiceType("ADDE");
  public final static ServiceType DODS = new ServiceType("DODS");
  public final static ServiceType OPENDAP = new ServiceType("OPENDAP");
  public final static ServiceType OPENDAPG = new ServiceType( "OPENDAP-G" );

  public final static ServiceType HTTPServer = new ServiceType( "HTTPServer" );
  public final static ServiceType FTP = new ServiceType( "FTP" );
  public final static ServiceType GRIDFTP = new ServiceType( "GridFTP" );
  public final static ServiceType FILE = new ServiceType( "File" );
  public final static ServiceType NetcdfServer = new ServiceType( "NetcdfServer" ); // deprecated
  public final static ServiceType NetcdfSubset = new ServiceType( "NetcdfSubset" );

  public final static ServiceType LAS = new ServiceType( "LAS" );
  public final static ServiceType WMS = new ServiceType( "WMS" );
  public final static ServiceType WFS = new ServiceType( "WFS" );
  public final static ServiceType WCS = new ServiceType( "WCS" );
  public final static ServiceType WSDL = new ServiceType( "WSDL" );

  public final static ServiceType WebForm = new ServiceType( "WebForm" );

  public final static ServiceType CATALOG = new ServiceType( "Catalog" );
  public final static ServiceType QC = new ServiceType( "QueryCapability" );
  public final static ServiceType RESOLVER = new ServiceType( "Resolver" );
  public final static ServiceType COMPOUND = new ServiceType( "Compound" );
  public final static ServiceType THREDDS = new ServiceType( "THREDDS" );

  public final static ServiceType NETCDF = new ServiceType( "NetCDF" ); // deprecated - use dataFormatType = NetCDF
  public final static ServiceType HTTP = new ServiceType( "HTTP" ); // deprecated - use HTTPServer

  private String name;

  private ServiceType(String s) {
    this.name = s;
    members.add(this);
  }
  
  private ServiceType( String name, boolean fake)
  {
    this.name = name;
  }

  /**
   * Get all ServiceType objects
   *
   * @return all ServiceType objects
   */
  public static java.util.Collection<ServiceType> getAllTypes() {
    return members;
  }

  /**
   * Return the known ServiceType that matches the given name (ignoring case)
   * or null if the name is unknown.
   *
   * @param name name of the desired ServiceType.
   * @return ServiceType or null if no match.
   */
  public static ServiceType findType(String name) {
    if (name == null) return null;
    for (ServiceType serviceType : members) {
      if (serviceType.name.equalsIgnoreCase(name))
        return serviceType;
    }
    return null;
  }

  /**
   * Return a ServiceType that matches the given name by either matching
   * a known type (ignoring case) or creating an unknown type.
   *
   * @param name name of the desired ServiceType
   * @return the named ServiceType or null if given name is null.
   */
  public static ServiceType getType(String name) {
    if (name == null) return null;
    ServiceType type = findType( name );
    return type != null ? type : new ServiceType( name, false );
  }

  /**
   * Return the ServiceType name.
   */
  public String toString() {
    return name;
  }

  /**
   * Override Object.hashCode() to be consistent with this equals.
   */
  public int hashCode() {
    return name.hashCode();
  }

  /**
   * ServiceType with same name are equal.
   */
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ServiceType))
      return false;
    return o.hashCode() == this.hashCode();
  }
}