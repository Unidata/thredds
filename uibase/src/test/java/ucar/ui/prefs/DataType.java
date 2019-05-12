/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.prefs;

/**
 * Type-safe enumeration of netCDF data types.
 * This is for testing ComboBox.
 *
 * @author john caron
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
