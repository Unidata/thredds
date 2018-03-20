/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

import ucar.nc2.dataset.*;

import ucar.unidata.geoloc.vertical.HybridHeight;
import ucar.unidata.util.Parameter;

/**
 * Create a atmosphere_hybrid_height_coordinate Vertical Transform from
 * the information in the Coordinate Transform Variable.
 *
 * @author murray
 */
public class CFHybridHeight extends AbstractTransformBuilder implements VertTransformBuilderIF {

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
    return VerticalCT.Type.HybridHeight.name();
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
  public VerticalCT makeCoordinateTransform(NetcdfDataset ds, AttributeContainer ctv) {
    String formula_terms = getFormula(ctv);
    if (null == formula_terms)
      return null;

    // parse the formula string
    String[] values = parseFormula(formula_terms, "a b orog");
    if (values == null) return null;

    a = values[0];
    b = values[1];
    orog = values[2];

    VerticalCT rs = new VerticalCT("AtmHybridHeight_Transform_" + ctv.getName(), getTransformName(), VerticalCT.Type.HybridHeight, this);
    rs.addParameter(new Parameter("standard_name", getTransformName()));
    rs.addParameter(new Parameter("formula_terms", formula_terms));

    rs.addParameter(new Parameter("formula", "height(x,y,z) = a(z) + b(z)*orog(x,y)"));
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
  public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return new HybridHeight(ds, timeDim, vCT.getParameters());
  }
}
