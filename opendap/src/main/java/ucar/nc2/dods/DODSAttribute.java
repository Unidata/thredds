/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import ucar.ma2.*;

import java.util.*;

import ucar.unidata.util.StringUtil2;

/**
 * Adapter for dods.dap.Attribute into a ucar.nc2.Attribute.
 * Byte attributes are widened to short because DODS has Bytes as unsigned,
 * but in Java they are signed.
 *
 * @author caron
 * @see ucar.nc2.Attribute
 */

//Coverity[FB.EQ_DOESNT_OVERRIDE_EQUALS]
public class DODSAttribute extends ucar.nc2.Attribute {

  /**
   * constructor: adapter around dods.dap.Attribute
   *
   * @param dodsName the attribute name
   * @param att      the dods attribute
   */
  public DODSAttribute(String dodsName, opendap.dap.Attribute att) {
    super(DODSNetcdfFile.makeShortName(dodsName));
    setDODSName(DODSNetcdfFile.makeDODSName(dodsName));

    DataType ncType = DODSNetcdfFile.convertToNCType(att.getType(), false); // LOOK dont know if attribute is unsigned byte

    // count number
    int nvals = 0;
    Iterator iter = att.getValuesIterator();
    while (iter.hasNext()) {
      iter.next();
      nvals++;
    }

    // DAS parser is now assumed to handle escaping and remove  "" from strings
    /*
    String[] vals = new String[nvals];
    iter = att.getValuesIterator();
    int count = 0;
    while(iter.hasNext()) {
      String val = (String) iter.next();
      if (val.charAt(0) == '"')
        val = val.substring(1);
      int n = val.length();
      if ((n > 0) && (val.charAt(n-1) == '"'))
        val = val.substring(0, n-1);

      vals[count++] = unescapeAttributeStringValues( val);
    } */

    // need String[]
    String[] vals = new String[nvals];
    iter = att.getValuesIterator();
    int count = 0;
    while (iter.hasNext()) {
      vals[count++] = (String) iter.next();
    }

    Array data;
    if (ncType == DataType.STRING)
      data = Array.factory(ncType, new int[]{nvals}, vals);
    else {
      try {
        // create an Array of the correct type
        data = Array.factory(ncType, new int[]{nvals});
        Index ima = data.getIndex();
        for (int i = 0; i < nvals; i++) {
          double dval = Double.parseDouble(vals[i]);
          data.setDouble(ima.set(i), dval);
        }
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Illegal Numeric Value for Attribute Value for " + dodsName);
      }
    }
    setValues(data);
    setImmutable();
  }

  protected DODSAttribute(String dodsName, String val) {
    super(DODSNetcdfFile.makeShortName(dodsName), val);
    setDODSName(DODSNetcdfFile.makeDODSName(dodsName));
  }

  static private String[] escapeAttributeStrings = {"\\", "\""};
  static private String[] substAttributeStrings = {"\\\\", "\\\""};

  private String unescapeAttributeStringValues(String value) {
    return StringUtil2.substitute(value, substAttributeStrings, escapeAttributeStrings);
  }

  //////////////////////////////////////////////////
  // DODSNode Interface
  String dodsName = null;

  public String getDODSName() {
    return dodsName;
  }

  public void setDODSName(String name) {
    this.dodsName = name;
  }

  public void resetShortName(String name) {
    setShortName(name);
  }

}
