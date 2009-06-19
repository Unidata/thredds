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

package ucar.nc2.dataset.transform;

import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.unidata.util.Parameter;

import java.util.StringTokenizer;
import java.util.List;
import java.util.Formatter;

/**
 * Abstract superclass for implementations of CoordTransBuilderIF.
 *
 * @author caron
 */
public abstract class AbstractCoordTransBuilder implements ucar.nc2.dataset.CoordTransBuilderIF {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractCoordTransBuilder.class);
  protected Formatter errBuffer = null;

  public void setErrorBuffer(Formatter errBuffer) {
    this.errBuffer = errBuffer;
  }

  public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    throw new UnsupportedOperationException();
  }

  /**
   * Read a variable attribute as a double.
   *
   * @param v       the variable
   * @param attname name of variable
   * @param defValue default value if attribute is not found
   * @return attribute value as a double, else NaN if not found
   */
  protected double readAttributeDouble(Variable v, String attname, double defValue) {
    Attribute att = v.findAttributeIgnoreCase(attname);
    if (att == null) return defValue;
    if (att.isString())
      return Double.parseDouble(att.getStringValue());
    else
      return att.getNumericValue().doubleValue();
  }

  /**
   * Read an attribute as double[2]. If only one value, make second same as first.
   *
   * @param att the attribute. May be numeric or String.
   * @return attribute value as a double[2]
   */
  protected double[] readAttributeDouble2(Attribute att) {
    if (att == null) return null;

    double[] val = new double[2];
    if (att.isString()) {
      StringTokenizer stoke = new StringTokenizer(att.getStringValue());
      val[0] = Double.parseDouble(stoke.nextToken());
      val[1] = stoke.hasMoreTokens() ? Double.parseDouble(stoke.nextToken()) : val[0];
    } else {
      val[0] = att.getNumericValue().doubleValue();
      val[1] = (att.getLength() > 1) ? att.getNumericValue(1).doubleValue() : val[0];
    }
    return val;
  }

  /**
   * Add a Parameter to a CoordinateTransform.
   * Make sure that the variable exists. If readData is true, read the data and use it as the value of the
   * parameter, otherwise use the variable name as the value of the parameter.
   *
   * @param rs             the CoordinateTransform
   * @param paramName      the parameter name
   * @param ds             dataset
   * @param varNameEscaped escaped variable name
   * @return true if success, false is failed
   */
  protected boolean addParameter(CoordinateTransform rs, String paramName, NetcdfFile ds, String varNameEscaped) {
    if (null == (ds.findVariable(varNameEscaped))) {
      if (null != errBuffer)
        errBuffer.format("CoordTransBuilder %s: no Variable named %s\n", getTransformName(), varNameEscaped);
      return false;
    }

    rs.addParameter(new Parameter(paramName, varNameEscaped));
    return true;
  }

  protected String getFormula(NetcdfDataset ds, Variable ctv) {
    String formula = ds.findAttValueIgnoreCase(ctv, "formula_terms", null);
    if (null == formula) {
      if (null != errBuffer)
        errBuffer.format("CoordTransBuilder %s: needs attribute 'formula_terms' on Variable %s\n", getTransformName(), ctv.getName());
      return null;
    }
    return formula;
  }

  public String[] parseFormula(String formula_terms, String termString) {
    String[] formulaTerms = formula_terms.split("[\\s:]+");  // split on 1 or more whitespace or ':'
    String[] terms = termString.split("[\\s]+");             // split on 1 or more whitespace 
    String[] values = new String[terms.length];

    for (int i=0; i<terms.length; i++) {
      for (int j=0; j<formulaTerms.length; j+=2) {  // look at every other formula term
        if (terms[i].equals(formulaTerms[j])) {     // if it matches
          values[i] = formulaTerms[j+1];            // next term is the value
          break;
        }
      }
    }

    boolean ok = true;
    for (int i=0; i<values.length; i++) {
      if (values[i] == null) {
        if (null != errBuffer)
          errBuffer.format("Missing term=%s in the formula '%s' for the vertical transform= %s%n", terms[i], formula_terms, getTransformName());
        ok = false;
      }
    }

    return ok ? values : null;
  }

  /* public static void test(String s, String regexp) {
    String[] result = s.split(regexp);
    for (String r : result) System.out.println(" <"+r+">");
  }

  public static void test2(String f, String t) {
    String[] result = parseFormula(f, t);
    for (String r : result) System.out.println(" <"+r+">");
  }

  public static void main(String args[]) {
    test("ac:b blah:c dah: d dow dee","[\\s:]+");
    //test("ac:b blah:c dah d","\\s");
    //test2("ac:b blah:c dah: d dow dee","ac blah dah dow");
  }    */



  //////////////////////////////////////////
  //private UnitFormat format;
  static public double getFalseEastingScaleFactor(NetcdfDataset ds, Variable ctv) {
    String units = ds.findAttValueIgnoreCase(ctv, "units", null);
    if (units == null) {
      List<CoordinateAxis> axes = ds.getCoordinateAxes();
      for (CoordinateAxis axis : axes) {
        if (axis.getAxisType() == AxisType.GeoX) { // kludge - what if there's multiple ones?
          Variable v = axis.getOriginalVariable(); // LOOK why original variable ?
          units = v.getUnitsString();
          break;
        }
      }
    }

    if (units != null) {
      try {
        SimpleUnit unit = SimpleUnit.factoryWithExceptions(units);
        return unit.convertTo(1.0, SimpleUnit.kmUnit);
      } catch (Exception e) {
        log.error(units + " not convertible to km");
      }
    }
    return 1.0;
  }

}
