// $Id:FieldType.java 63 2006-07-12 21:50:51Z edavis $
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

package ucar.nc2.geotiff;

/**
 *
 * @author caron
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
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