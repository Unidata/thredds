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

  private MetadataType(String name, boolean fake) {
    this.name = name;
  }

  /**
   * Get all MetadataType objects
   * @return all MetadataType objects
   */
  public static java.util.Collection<MetadataType> getAllTypes() {
    return members;
  }

  /**
   * Return the known MetadataType that matches the given name (ignoring case)
   * or null if the name is unknown.
   *
   * @param name the name of the desired DataFormatType.
   * @return MetadataType or null if no match.
   */
  public static MetadataType findType(String name) {
    if (name == null) return null;
    for (MetadataType m : members) {
      if (m.name.equalsIgnoreCase(name))
        return m;
    }
    return null;
  }

  /**
   * Return a MetadataType that matches the given name by either matching
   * a known type (ignoring case) or creating an unknown type.
   *
   * @param name name of the desired MetadataType
   * @return the named MetadataType or null if given name is null.
   */
  public static MetadataType getType(String name) {
    if (name == null) return null;
    MetadataType type = findType( name );
    return type != null ? type : new MetadataType( name, false );
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
    if (!(o instanceof MetadataType)) return false;
    return o.hashCode() == this.hashCode();
  }
}