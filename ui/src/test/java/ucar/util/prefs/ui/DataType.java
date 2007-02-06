// $Id: DataType.java,v 1.2 2003/06/03 20:06:05 caron Exp $
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

package ucar.util.prefs.ui;

/**
 * Type-safe enumeration of netCDF data types.
 * This is for testing ComboBox.
 *
 * @author john caron
 * @version $Revision: 1.2 $ $Date: 2003/06/03 20:06:05 $
 */

public class DataType {

  private static java.util.LinkedHashMap hash = new java.util.LinkedHashMap(10);
  private static java.util.ArrayList names = new java.util.ArrayList(10);

  public final static DataType NONE = new DataType("");
  public final static DataType BOOLEAN = new DataType("boolean");
  public final static DataType BYTE = new DataType("byte");
  public final static DataType SHORT = new DataType("short");
  public final static DataType INT = new DataType("int");
  public final static DataType LONG = new DataType("long");
  public final static DataType FLOAT = new DataType("float");
  public final static DataType DOUBLE = new DataType("double");
  public final static DataType STRING = new DataType("string");

  public DataType() { }

  private String _DataType;
  public DataType(String s) {
      this._DataType = s;
      hash.put( s, this);
      names.add( s);
  }

  /**
   * The string name.
   */
   public String toString() {
      return _DataType;
  }

  static public java.util.Collection getTypes() {
    return hash.values();
  }

  static public java.util.Collection getTypeNames() {
    return names;
  }

}


/**
 * $Log: DataType.java,v $
 * Revision 1.2  2003/06/03 20:06:05  caron
 * fix javadocs
 *
 * Revision 1.1  2003/04/08 15:06:21  caron
 * nc2 version 2.1
 *
 */
