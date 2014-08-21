/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point.standard;

import ucar.nc2.constants.CF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.Structure;
import ucar.nc2.Attribute;
import ucar.ma2.DataType;

import java.util.Formatter;

/**
 * Helper routines for Nested Tables
 *
 * @author caron
 * @since Apr 23, 2008
 */
public abstract class Evaluator {
  public static class VarAtt {
    public Variable var;
    public Attribute att;

    public VarAtt(Variable var, Attribute att) {
      this.var = var;
      this.att = att;
    }
  }

  static public String findNameVariableWithStandardNameAndDimension(NetcdfDataset ds, String standard_name, Dimension outer, Formatter errlog) {
    Variable v = findVariableWithAttributeAndDimension(ds, CF.STANDARD_NAME, standard_name, outer, errlog);
    return (v == null) ? null : v.getShortName();
  }

  static public Variable findVariableWithAttributeAndDimension(NetcdfDataset ds, String att_name, String att_value, Dimension outer, Formatter errlog) {
    for (Variable v : ds.getVariables()) {
      String attValue = ds.findAttValueIgnoreCase(v, att_name, null);
      if ((attValue != null) && attValue.equalsIgnoreCase(att_value)) {
        if (v.getRank() > 0 && v.getDimension(0).equals(outer))
          return v;
        if (isEffectivelyScaler(v) && (outer == null))
          return v;
      }
    }

     // descend into structures
    for (Variable v : ds.getVariables()) {
      if (v instanceof Structure) {
        Structure s = (Structure) v;
        if (s.getRank() > 0 && s.getDimension(0).equals(outer) || (s.getRank() == 0 && outer == null)) {
          for (Variable vs : s.getVariables()) {
            Attribute att = vs.findAttributeIgnoreCase(att_name);
            if ((att != null) && att.isString() && att.getStringValue().equalsIgnoreCase(att_value))
              return vs;
          }
        }
      }
    }

    // failed
    return null;
  }

  static public boolean isEffectivelyScaler(Variable v) {
    return (v.getRank() == 0) || (v.getRank() == 1 && v.getDataType() == DataType.CHAR);
  }

  /**
   * Find first variable with given attribute name
   *
   * @param ds      in this dataset
   * @param attName attribute name, case insensitive
   * @return first variable with given attribute name, or null
   */
  static public VarAtt findVariableWithAttribute(NetcdfDataset ds, String attName) {
    for (Variable v : ds.getVariables()) {
      Attribute att = v.findAttributeIgnoreCase(attName);
      if (att != null) return new VarAtt(v, att);
    }

    // descend into structures
    for (Variable v : ds.getVariables()) {
      if (v instanceof Structure) {
        Structure s = (Structure) v;
        for (Variable vs : s.getVariables()) {
          Attribute att = vs.findAttributeIgnoreCase(attName);
          if (att != null) return new VarAtt(vs, att);
        }
      }
    }
    return null;
  }

  /**
   * Find first variable with given attribute name and value.
   * If not found, look one level into structures.
   *
   * @param ds       in this dataset
   * @param attName  attribute name, case insensitive
   * @param attValue attribute value, case sensitive
   * @return first variable with given attribute name and value, or null
   */
  static public Variable findVariableWithAttributeValue(NetcdfDataset ds, String attName, String attValue) {
    for (Variable v : ds.getVariables()) {
      String haveValue = ds.findAttValueIgnoreCase(v, attName, null);
      if ((haveValue != null) && haveValue.equals(attValue))
        return v;
    }

    // descend into structures
    for (Variable v : ds.getVariables()) {
      if (v instanceof Structure) {
        Variable vn = findVariableWithAttributeValue((Structure) v, attName, attValue);
        if (null != vn) return vn;
      }
    }
    return null;
  }

  /**
   * Find first variable with given attribute name and value
   *
   * @param ds       in this dataset
   * @param attName  attribute name, case insensitive
   * @param attValue attribute value, case sensitive
   * @return name of first variable with given attribute name and value, or null
   */
  static public String findNameOfVariableWithAttributeValue(NetcdfDataset ds, String attName, String attValue) {
    Variable v = findVariableWithAttributeValue(ds, attName, attValue);
    return (v == null) ? null : v.getShortName();
  }

  /**
   * Find first member variable in this struct with given attribute name and value
   *
   * @param struct   in this structure
   * @param attName  attribute name, case insensitive
   * @param attValue attribute value, case sensitive
   * @return first member variable with given attribute name and value, or null
   */
  static public Variable findVariableWithAttributeValue(Structure struct, String attName, String attValue) {
    for (Variable v : struct.getVariables()) {
      Attribute att = v.findAttributeIgnoreCase(attName);
      if ((att != null) && att.getStringValue().equals(attValue))
        return v;
    }
    return null;
  }

  /**
   * Find structure variable of rank 2 with the 2 given dimensions
   * (or) Find structure variable of rank 1 with the 1 given dimension
   * @param ds  in this dataset
   * @param dim0 first dimension
   * @param dim1 second dimension  (ok to be null)
   * @return structure variable or null
   */
  static public Structure findStructureWithDimensions(NetcdfDataset ds, Dimension dim0, Dimension dim1) {
    for (Variable v : ds.getVariables()) {
      if (!(v instanceof Structure)) continue;

      if (dim1 != null && v.getRank() == 2 && v.getDimension(0).equals(dim0) && v.getDimension(1).equals(dim1))
        return (Structure) v;

      if (dim1 == null && v.getRank() == 1 && v.getDimension(0).equals(dim0))
         return (Structure) v;
      }
  return null;
  }

  /**
   * Find first nested structure
   * @param s in this structure
   * @return first nested structure or null
   */
  static public Structure findNestedStructure(Structure s) {
    for (Variable v : s.getVariables()) {
      if ((v instanceof Structure))
        return (Structure) v;
    }
    return null;
  }

  /**
   * Does this dataset have a record structure? netcdf-3 specific
   * @param ds in this dataset
   * @return true if record structure exists
   */
  static public boolean hasNetcdf3RecordStructure(NetcdfDataset ds) {
    Variable v = ds.findVariable("record");
    return (v != null) && (v.getDataType() == DataType.STRUCTURE);
  }

  ////////////////////////////////////////////////////////////////////////////
  // literals support ":gatt"

  /**
   * Translate key to value
   *
   * @param ds     look in this dataset
   * @param key    if starts with ":", look for global attribute
   * @param errlog error messages here
   * @return return global attribute value or the key itself
   */
  static public String getLiteral(NetcdfDataset ds, String key, Formatter errlog) {
    if (key.startsWith(":")) {
      String val = ds.findAttValueIgnoreCase(null, key.substring(1), null);
      if ((val == null) && (errlog != null))
        errlog.format(" Cant find global attribute %s%n", key);
      return val;
    }

    return key;
  }

  /**
   * Turn the key into a String and return the corresponding featureType, if any.
   *
   * @param ds     look in this datset
   * @param key    if starts with ":", replace with value of global attribute
   * @param errlog error messages here
   * @return featureType, or null
   */
  static public FeatureType getFeatureType(NetcdfDataset ds, String key, Formatter errlog) {
    FeatureType ft = null;
    String fts = getLiteral(ds, key, errlog);
    if (fts != null) {
      ft = FeatureType.valueOf(fts.toUpperCase());
      if ((ft == null) && (errlog != null))
        errlog.format(" Cant find Feature type %s from %s%n", fts, key);
    }
    return ft;
  }

  /**
   * Find the variable pointed to by key
   *
   * @param ds     in this dataset
   * @param key    may be variable name or ":gatt" where gatt is local attribute whose value is the variable name
   * @param errlog error messages here
   * @return name of variable or null if not exist
   */
  static public String getVariableName(NetcdfDataset ds, String key, Formatter errlog) {
    Variable v = null;
    String vs = getLiteral(ds, key, errlog);
    if (vs != null) {
      v = ds.findVariable(vs);
      if ((v == null) && (errlog != null))
        errlog.format(" Cant find Variable %s from %s%n", vs, key);
    }
    return v == null ? null : v.getShortName();
  }

  /**
   * Find the dimension pointed to by key
   *
   * @param ds     in this dataset
   * @param key    may be dimension name or ":gatt" where gatt is local attribute whose value is the dimension name
   * @param errlog error messages here
   * @return dimension or null if not exist
   */
  static public Dimension getDimension(NetcdfDataset ds, String key, Formatter errlog) {
    Dimension d = null;
    String s = getLiteral(ds, key, errlog);
    if (s != null) {
      d = ds.findDimension(s); // LOOK use group
      if ((d == null) && (errlog != null))
        errlog.format(" Cant find Variable %s from %s%n", s, key);
    }
    return d;
  }

  /**
   * Find the dimension pointed to by key
   *
   * @param ds     in this dataset
   * @param key    may be dimension name or ":gatt" where gatt is local attribute whose value is the dimension name
   * @param errlog error messages here
   * @return name of dimension or null if not exist
   */
  static public String getDimensionName(NetcdfDataset ds, String key, Formatter errlog) {
    Dimension d = getDimension(ds, key, errlog);
    return (d == null) ? null : d.getShortName();
  }


  private Evaluator() { }  // Private ctor to ensure non-instantiability.
}
