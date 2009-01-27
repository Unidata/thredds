// $Id: DataType.java,v 1.2 2003/06/03 20:06:05 caron Exp $
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
