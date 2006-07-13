// $Id: InvProperty.java 48 2006-07-12 16:15:40Z caron $
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


/**
 * A Property is a name/value pair.
 *
 * @author john caron
 * @version $Revision: 48 $ $Date: 2006-07-12 16:15:40Z $
 */

public class InvProperty {

  private String name, value;

  public InvProperty() { }

  /** Constructor */
  public InvProperty( String name, String value) {
    this.name = name;
    this.value = value;
  }

  /**
   * Get the name of the property.
   */
  public String getName() { return name; }

  /**
   * Get the value of the property.
   */
  public String getValue() { return value; }

    /** string representation */
  public String toString() {
    return "<"+name+"> <"+value+">";
  }

  /** InvProperty elements with same name are equal. */
   public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof InvProperty)) return false;
     return o.hashCode() == this.hashCode();
  }
  /**
   * Override Object.hashCode() to be consistent with equals.
   */
  public int hashCode() {
    return getName().hashCode();
  }

}

/**
 * $Log: InvProperty.java,v $
 * Revision 1.4  2004/06/09 00:27:25  caron
 * version 2.0a release; cleanup javadoc
 *
 * Revision 1.3  2004/05/11 23:30:28  caron
 * release 2.0a
 *
 * Revision 1.2  2004/02/20 00:49:51  caron
 * 1.3 changes
 *
 * Revision 1.1.1.1  2002/11/23 17:49:45  caron
 * thredds reorg
 *
 * Revision 1.1  2002/06/28 21:28:24  caron
 * create vresion 6 object model
 *
 *
 */