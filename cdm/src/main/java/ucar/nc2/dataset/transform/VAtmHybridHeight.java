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

import ucar.nc2.Dimension;
import ucar.nc2.Variable;

import ucar.nc2.dataset.*;

import ucar.unidata.geoloc.vertical.HybridHeight;
import ucar.unidata.util.Parameter;

import java.util.StringTokenizer;

/**
 * Create a atmosphere_hybrid_height_coordinate Vertical Transform from
 * the information in the Coordinate Transform Variable.
 *
 * @author murray
 */
public class VAtmHybridHeight extends AbstractCoordTransBuilder {

  /**
   * The name of the a term
   */
  private String a;

  /**
   * The name of the a term
   */
  private String b;

  /**
   * The name of the orog term
   */
  private String orog;

  /**
   * Get the standard name of this transform
   *
   * @return the name
   */
  public String getTransformName() {
    return "atmosphere_hybrid_height_coordinate";
  }

  /**
   * Get the type of the transform
   *
   * @return the type
   */
  public TransformType getTransformType() {
    return TransformType.Vertical;
  }

  /**
   * Make the <code>CoordinateTransform</code> from the dataset
   *
   * @param ds  the dataset
   * @param ctv the variable with the formula
   * @return the <code>CoordinateTransform</code>
   */
  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
    String formula_terms = getFormula(ds, ctv);
    if (null == formula_terms)
      return null;

    // parse the formula string
    String[] values = parseFormula(formula_terms, "a b orog");
    if (values == null) return null;

    a = values[0];
    b = values[1];
    orog = values[2];

    CoordinateTransform rs = new VerticalCT("AtmHybridHeight_Transform_"
            + ctv.getShortName(), getTransformName(),
            VerticalCT.Type.HybridHeight, this);
    rs.addParameter(new Parameter("standard_name", getTransformName()));
    rs.addParameter(new Parameter("formula_terms", formula_terms));

    rs.addParameter(
            new Parameter(
                    "formula", "height(x,y,z) = a(z) + b(z)*orog(x,y)"));
    if (!addParameter(rs, HybridHeight.A, ds, a)) {
      return null;
    }
    if (!addParameter(rs, HybridHeight.B, ds, b)) {
      return null;
    }
    if (!addParameter(rs, HybridHeight.OROG, ds, orog)) {
      return null;
    }
    return rs;
  }

  /**
   * Get a String representation of this object
   *
   * @return a String representation of this object
   */
  public String toString() {
    return "HybridHeight:" + "orog:" + orog + " a:" + a + " b:" + b;
  }

  /**
   * Make the vertical transform transform
   *
   * @param ds      the dataset
   * @param timeDim the time dimention
   * @param vCT     the vertical coordinate transform
   * @return the VerticalTransform
   */
  public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(
          NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return new HybridHeight(ds, timeDim, vCT.getParameters());
  }
}
