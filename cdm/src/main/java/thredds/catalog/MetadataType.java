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
 * Type-safe enumeration of THREDDS Metadata types.
 *
 * @author john caron
 */

public final class MetadataType {
  private static java.util.List<MetadataType> members = new java.util.ArrayList<MetadataType>(20);

  public final static MetadataType NONE = new MetadataType("");

  public final static MetadataType THREDDS = new MetadataType("THREDDS");
  public final static MetadataType ADN = new MetadataType("ADN");
  public final static MetadataType AGGREGATION = new MetadataType("Aggregation");
  public final static MetadataType CATALOG_GEN_CONFIG = new MetadataType("CatalogGenConfig");
  public final static MetadataType DUBLIN_CORE = new MetadataType("DublinCore");
  public final static MetadataType DIF = new MetadataType("DIF");
  public final static MetadataType FGDC = new MetadataType("FGDC");
  public final static MetadataType LAS = new MetadataType("LAS");
  public final static MetadataType ESG = new MetadataType("ESG");

  // not 1.0 : is anyone using?
  public final static MetadataType NETCDF = new MetadataType("NetCDF");
  public final static MetadataType NcML = new MetadataType("NcML");
  public final static MetadataType THREDDS_DLEntry = new MetadataType("THREDDS_DLEntry");
  public final static MetadataType THREDDS_DLCollection = new MetadataType("THREDDS_DLCollection");

  private String name;

  private MetadataType(String s) {
    this.name = s;
    members.add(this);
  }

  /**
   * Get all MetadataType objects
   * @return all MetadataType objects
   */
  public static java.util.Collection<MetadataType> getAllTypes() {
    return members;
  }

  /**
   * Find the MetadataType that matches this name, ignore case.
   *
   * @param name : match this name
   * @return MetadataType or null if no match.
   */
  public static MetadataType getType(String name) {
    if (name == null) return null;
    for (MetadataType m : members) {
      if (m.name.equalsIgnoreCase(name))
        return m;
    }
    return null;
  }

  /**
   * Return the MetadataType name.
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
   * MetaDataType with same name are equal.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DataType)) return false;
    return o.hashCode() == this.hashCode();
  }
}