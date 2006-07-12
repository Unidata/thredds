// $Id$
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

package ucar.nc2.geotiff;

/**
 *
 * @author caron
 * @version $Revision$ $Date$
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