// $Id: MetadataType.java,v 1.10 2006/02/13 19:52:21 caron Exp $
/*
 * Copyright 2002 Unidata Program Center/University Corporation for
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
 * @version $Revision: 1.10 $ $Date: 2006/02/13 19:52:21 $
 */

public final class MetadataType {
    private static java.util.ArrayList members = new java.util.ArrayList(20);

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
      members.add( this);
    }

      /** Return all MetadataType objects */
    public static java.util.Collection getAllTypes() { return members; }

    /**
     * Find the MetadataType that matches this name, ignore case.
     * @param name : match this name
     * @return MetadataType or null if no match.
     */
    public static MetadataType getType(String name) {
      if (name == null) return null;
      for (int i = 0; i < members.size(); i++) {
        MetadataType m = (MetadataType) members.get(i);
        if (m.name.equalsIgnoreCase( name))
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

     /** Override Object.hashCode() to be consistent with this equals. */
     public int hashCode() { return name.hashCode(); }

     /** MetaDataType with same name are equal. */
     public boolean equals(Object o) {
       if (this == o) return true;
       if (!(o instanceof DataType)) return false;
       return o.hashCode() == this.hashCode();
    }
}

/**
 * $Log: MetadataType.java,v $
 * Revision 1.10  2006/02/13 19:52:21  caron
 * javadoc
 *
 * Revision 1.9  2005/07/21 18:50:48  caron
 * no message
 *
 * Revision 1.8  2004/06/09 00:27:25  caron
 * version 2.0a release; cleanup javadoc
 *
 * Revision 1.7  2004/05/11 23:30:28  caron
 * release 2.0a
 *
 * Revision 1.6  2004/02/20 00:49:51  caron
 * 1.3 changes
 *
 * Revision 1.5  2003/12/04 22:27:44  caron
 * *** empty log message ***
 *
 * Revision 1.4  2003/05/29 22:53:17  john
 * reletive URL parsing
 *
 * Revision 1.3  2003/05/29 21:30:47  john
 * resolve reletive URLS differently
 *
 * Revision 1.2  2002/11/26 00:05:55  caron
 * merge2 ethan's changes
 *
 * Revision 1.5  2002/11/19 21:17:05  edavis
 * hanges for CatalogGen release 0.6:
 * Add the CATALOG_GEN_CONFIG metadata type.
 *
 * Revision 1.3  2002/07/02 20:46:04  caron
 * add GridFTP service, netcdf metadata
 *
 * Revision 1.2  2002/07/01 23:35:03  caron
 * release 0.6
 *
 * Revision 1.1  2002/06/28 21:28:27  caron
 * create vresion 6 object model
 *n
 *
 */
