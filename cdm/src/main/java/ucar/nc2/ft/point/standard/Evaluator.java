/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
package ucar.nc2.ft.point.standard;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.Structure;
import ucar.nc2.Attribute;

import java.util.Formatter;

/**
 * Helper routines for Nested Tables
 * @author caron
 * @since Apr 23, 2008
 */
public class Evaluator {

  static public FeatureType getFeatureType(NetcdfDataset ds, String key, Formatter errlog) {
    FeatureType ft = null;
    String fts = getLiteral(ds, key, errlog);
    if (fts != null) {
      ft = FeatureType.valueOf(fts.toUpperCase());
      if ((ft == null) && (errlog != null))
        errlog.format(" Cant find Feature type %s from %s\n", fts, key);
    }
    return ft;
  }

  /**
   * Translate key to value
   * @param ds look in this datset
   * @param key if starts with ":", look for global attribute
   * @param errlog error messages here
   * @return return global attribute value or the key itself
   */
  static public String getLiteral(NetcdfDataset ds, String key, Formatter errlog) {
    if (key.startsWith(":")) {
      String val = ds.findAttValueIgnoreCase(null, key.substring(1), null);
      if ((val == null) && (errlog != null))
        errlog.format(" Cant find global attribute %s\n", key);
      return val;
    }

    return key;
  }

  static public String getVariableName(NetcdfDataset ds, String key, Formatter errlog) {
    Variable v = null;
    String vs = getLiteral(ds, key, errlog);
    if (vs != null) {
      v = ds.findVariable(vs);
      if ((v == null) && (errlog != null))
        errlog.format(" Cant find Variable %s from %s\n", vs, key);
    }
    return v == null ? null : v.getShortName();
  }

  static public String getVariableWithAttribute(NetcdfDataset ds, String attName, String attValue) {
    for (Variable v : ds.getVariables()) {
      String stdName = ds.findAttValueIgnoreCase(v, attName, null);
      if ((stdName != null) && stdName.equals(attValue))
        return v.getName();
    }
    return null;
  }

  static public String getVariableWithAttribute(Structure struct, String attName, String attValue) {
    for (Variable v : struct.getVariables()) {
      Attribute att = v.findAttributeIgnoreCase(attName);
      if ((att != null) && att.getStringValue().equals(attValue))
        return v.getShortName();
    }
    return null;
  }

  static public Dimension getDimension(NetcdfDataset ds, String key, Formatter errlog) {
    Dimension d = null;
    String s = getLiteral(ds, key, errlog);
    if (s != null) {
      d = ds.findDimension(s); // LOOK use group
      if ((d == null) && (errlog != null))
        errlog.format(" Cant find Variable %s from %s\n", s, key);
    }
    return d;
  }


  /////////////////////////////////
  private String constant;
  private Variable v;

  Evaluator(Variable v) {
    this.v = v;
  }

  Evaluator(String constant) {
    this.constant = constant;
  }

  public String getValue() {
    if (constant != null)
      return constant;
    return null; // ??
  }

}
