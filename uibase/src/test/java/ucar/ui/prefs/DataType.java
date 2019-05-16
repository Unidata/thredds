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

  public static final DataType NONE = new DataType("");
  public static final DataType BOOLEAN = new DataType("boolean");
  public static final DataType BYTE = new DataType("byte");
  public static final DataType SHORT = new DataType("short");
  public static final DataType INT = new DataType("int");
  public static final DataType LONG = new DataType("long");
  public static final DataType FLOAT = new DataType("float");
  public static final DataType DOUBLE = new DataType("double");
  public static final DataType STRING = new DataType("string");

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

  public static java.util.Collection getTypes() {
    return hash.values();
  }

  public static java.util.Collection getTypeNames() {
    return names;
  }

}
