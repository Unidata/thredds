// $Id:AbstractCoordTransBuilder.java 51 2006-07-12 17:13:13Z caron $
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

package ucar.nc2.dataset.transform;

import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.*;
import ucar.ma2.Array;
import ucar.unidata.util.Parameter;

import java.util.StringTokenizer;
import java.util.List;
import java.io.IOException;

/**
 * Abstract superclass for implementations of CoordTransBuilderIF.
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
public abstract class AbstractCoordTransBuilder implements ucar.nc2.dataset.CoordTransBuilderIF {
  protected StringBuffer errBuffer = null;

  public void setErrorBuffer(StringBuffer errBuffer) {
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
   * @return attribute value as a double, else NaN if not found
   */
  protected double readAttributeDouble(Variable v, String attname) {
    Attribute att = v.findAttributeIgnoreCase(attname);
    if (att == null) return Double.NaN;
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
   * Make sure that variable exist. If readData is true, read the data and use it as the value of the
   * parameter, otherwise use the variable name as the value of the parameter.
   *
   * @param rs        the CoordinateTransform
   * @param paramName the parameter name
   * @param ds        dataset
   * @param varName   variable name
   * @return true if success, false is failed
   */
  protected boolean addParameter(CoordinateTransform rs, String paramName, NetcdfFile ds, String varName) {
    Variable dataVar;
    if (null == (dataVar = ds.findVariable(varName))) {
      if (null != errBuffer)
        errBuffer.append("CoordTransBuilder " + getTransformName() + ": no Variable named " + varName);
      return false;
    }

    /* if (readData) {
      Array data;
      try {
        data = dataVar.read();
      } catch (IOException e) {
        if (null != errBuffer)
          errBuffer.append("CoordTransBuilder " + getTransformName() + ": failed on read of " + varName + " err=" + e + "\n");
        return false;
      }
      double[] vals = (double []) data.get1DJavaArray(double.class);
      rs.addParameter(new Parameter(paramName, vals));

    } else */
      rs.addParameter(new Parameter(paramName, varName));

    return true;
  }

  protected String getFormula(NetcdfDataset ds, Variable ctv) {
    String formula = ds.findAttValueIgnoreCase(ctv, "formula_terms", null);
    if (null == formula) {
      if (null != errBuffer)
        errBuffer.append("CoordTransBuilder " + getTransformName() + ": needs attribute 'formula_terms' on Variable "
                + ctv.getName() + "\n");
      return null;
    }
    return formula;
  }

  protected String getUnits(NetcdfDataset ds) {
        // kind o' kludge
    List axes = ds.getCoordinateAxes();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis =  (CoordinateAxis) axes.get(i);
      if (axis.getAxisType() == AxisType.GeoX) {
        return axis.getUnitsString();
      }
    }
    return null;
  }
}
