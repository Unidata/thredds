/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.geotiff;

/**
 *
 * @author caron
 */

class FieldType {
  static private FieldType[] types = new FieldType[20];

  static public final FieldType BYTE = new FieldType("BYTE", 1, 1);
  static public final FieldType ASCII = new FieldType("ASCII", 2, 1);
  static public final FieldType SHORT = new FieldType("SHORT", 3, 2);
  static public final FieldType LONG = new FieldType("LONG", 4, 4);
  static public final FieldType RATIONAL = new FieldType("RATIONAL", 5, 8);

  static public final FieldType SBYTE = new FieldType("SBYTE", 6, 1);
  static public final FieldType UNDEFINED = new FieldType("UNDEFINED", 7, 1);
  static public final FieldType SSHORT = new FieldType("SSHORT", 8, 2);
  static public final FieldType SLONG = new FieldType("SLONG", 9, 4);
  static public final FieldType SRATIONAL = new FieldType("SRATIONAL", 10, 8);
  static public final FieldType FLOAT = new FieldType("FLOAT", 11, 4);
  static public final FieldType DOUBLE = new FieldType("DOUBLE", 12, 8);


  static FieldType get( int code) {
    return types[code];
  }

  String name;
  int code;
  int size;

  private FieldType( String  name, int code, int size) {
    this.name = name;
    this.code = code;
    this.size = size;
    types[code] = this;
  }

  public String toString() { return name; }
}