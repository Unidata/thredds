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
package ucar.nc2.ft.point.standard;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;

import java.util.Formatter;

/**
 * Abstract superclass for TableConfigurer implementations
 * @author caron
 * @since Jan 21, 2009
 */
public abstract class TableConfigurerImpl implements TableConfigurer {

  public String getConvName() {
    return convName;
  }

  public void setConvName(String convName) {
    this.convName = convName;
  }

  public String getConvUsed() {
    return convUsed;
  }

  public void setConvUsed(String convUsed) {
    this.convUsed = convUsed;
  }

  private String convName, convUsed;

  protected String findNameVariableWithStandardNameAndDimension(NetcdfDataset ds, String standard_name, Dimension outer, Formatter errlog) {
    Variable v = findVariableWithStandardNameAndDimension(ds, standard_name, outer, errlog);
    return (v == null) ? null : v.getShortName();
  }

  protected Variable findVariableWithStandardNameAndDimension(NetcdfDataset ds, String standard_name, Dimension outer, Formatter errlog) {
    for (Variable v : ds.getVariables()) {
      String stdName = ds.findAttValueIgnoreCase(v, CF.STANDARD_NAME, null);
      if ((stdName != null) && stdName.equals(standard_name)) {
        if (v.getRank() > 0 && v.getDimension(0).equals(outer))
          return v;
        if ((v.getRank() == 0) && (outer == null))
          return v;
      }
    }
    return null;
  }

  protected Variable findVariableWithStandardNameAndNotDimension(NetcdfDataset ds, String standard_name, Dimension outer, Formatter errlog) {
    for (Variable v : ds.getVariables()) {
      String stdName = ds.findAttValueIgnoreCase(v, CF.STANDARD_NAME, null);
      if ((stdName != null) && stdName.equals(standard_name) && v.getRank() > 0 && !v.getDimension(0).equals(outer))
        return v;
    }
    return null;
  }

  protected String matchAxisTypeAndDimension(NetcdfDataset ds, AxisType type, final Dimension outer) {
    Variable var = CoordSysEvaluator.findCoordByType(ds, type, new CoordSysEvaluator.Predicate() {
      public boolean match(CoordinateAxis axis) {
        if ((outer == null) && (axis.getRank() == 0))
          return true;
        if ((outer != null) && (axis.getRank() == 1) && (outer.equals(axis.getDimension(0))))
          return true;
        return false;
      }
    });
    if (var == null) return null;
    return var.getShortName();
  }

  protected String matchAxisTypeAndDimension(NetcdfDataset ds, AxisType type, final Dimension outer, final Dimension inner) {
    Variable var = CoordSysEvaluator.findCoordByType(ds, type, new CoordSysEvaluator.Predicate() {
      public boolean match(CoordinateAxis axis) {
        return ((axis.getRank() == 2) && outer.equals(axis.getDimension(0)) && inner.equals(axis.getDimension(1)));
      }
    });
    if (var == null) return null;
    return var.getShortName();
  }

  protected String matchAxisTypeAndDimension(NetcdfDataset ds, AxisType type, final Dimension outer, final Dimension middle, final Dimension inner) {
    Variable var = CoordSysEvaluator.findCoordByType(ds, type, new CoordSysEvaluator.Predicate() {
      public boolean match(CoordinateAxis axis) {
        return ((axis.getRank() == 3) && outer.equals(axis.getDimension(0)) && middle.equals(axis.getDimension(1)) && inner.equals(axis.getDimension(2)));
      }
    });
    if (var == null) return null;
    return var.getShortName();
  }

  protected CoordinateAxis findZAxisNotStationAlt(NetcdfDataset ds) {
    CoordinateAxis z = CoordSysEvaluator.findCoordByType(ds, AxisType.Height, new CoordSysEvaluator.Predicate() {
      public boolean match(CoordinateAxis axis) {
        Attribute stdName = axis.findAttribute(CF.STANDARD_NAME);
        return ((stdName == null) || !CF.STATION_ALTITUDE.equals(stdName.getStringValue()));
      }
    });
    if (z != null) return z;

    z = CoordSysEvaluator.findCoordByType(ds, AxisType.Pressure, new CoordSysEvaluator.Predicate() {
      public boolean match(CoordinateAxis axis) {
        Attribute stdName = axis.findAttribute(CF.STANDARD_NAME);
        return ((stdName == null) || !CF.STATION_ALTITUDE.equals(stdName.getStringValue()));
      }
    });
    return z;
  }

}
