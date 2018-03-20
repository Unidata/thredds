/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: DataTypeBean.java,v 1.2 2003/06/03 20:06:05 caron Exp $


package ucar.util.prefs.ui;

/**
 * Make enum into a bean for testing ComboBox.
 *
 * @author john caron
 * @version $Revision: 1.2 $ $Date: 2003/06/03 20:06:05 $
 */

public class DataTypeBean {

  private static java.util.HashMap hash = new java.util.HashMap(10);
  private static java.util.ArrayList names = new java.util.ArrayList(10);

  public final static DataTypeBean BOOLEAN = new DataTypeBean("boolean");
  public final static DataTypeBean BYTE = new DataTypeBean("byte");
  public final static DataTypeBean SHORT = new DataTypeBean("short");
  public final static DataTypeBean INT = new DataTypeBean("int");
  public final static DataTypeBean LONG = new DataTypeBean("long");
  public final static DataTypeBean FLOAT = new DataTypeBean("float");
  public final static DataTypeBean DOUBLE = new DataTypeBean("double");
  public final static DataTypeBean STRING = new DataTypeBean("string");

  private String _DataTypeBean;
  public DataTypeBean() { }
  public void setName(String s) {
      this._DataTypeBean = s;
      hash.put( s, this);
      names.add( s);
  }
  public String getName() { return _DataTypeBean; }

  private DataTypeBean(String s) {
      this._DataTypeBean = s;
      hash.put( s, this);
      names.add( s);
  }

  /**
   * The string name.
   */
   public String toString() {
      return _DataTypeBean;
  }

  static public java.util.Collection getTypes() {
    return hash.values();
  }

  static public java.util.Collection getTypeNames() {
    return names;
  }

}


/**
 * $Log: DataTypeBean.java,v $
 * Revision 1.2  2003/06/03 20:06:05  caron
 * fix javadocs
 *
 * Revision 1.1  2003/04/08 15:06:21  caron
 * nc2 version 2.1
 *
 */
