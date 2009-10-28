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

import ucar.ma2.StructureData;

/**
 * Abstract superclass for extracting coordinat values from nested tables.
 *
 * @author caron
 * @since Jan 26, 2009
 */

public abstract class CoordVarExtractor {
  protected String axisName;
  protected int nestingLevel;

  protected CoordVarExtractor(String axisName, int nestingLevel) {
    this.axisName = axisName;
    this.nestingLevel = nestingLevel;
  }

  public abstract double getCoordValue(StructureData sdata);

  public abstract String getCoordValueString(StructureData sdata);

  public abstract String getUnitsString();

  public abstract boolean isString();



  public double getCoordValue(StructureData[] tableData) {
    return getCoordValue(tableData[nestingLevel]);
  }

  public String getCoordValueString(StructureData[] tableData) {
    return getCoordValueString(tableData[nestingLevel]);
  }

  public String getCoordValueAsString(StructureData sdata) {
    if (isString()) return getCoordValueString(sdata);
    double dval = getCoordValue( sdata);
    return Double.toString(dval);
  }

  protected abstract boolean isMissing(StructureData tableData);

  public boolean isMissing(StructureData[] tableData) {
    return isMissing(tableData[nestingLevel]);
  }

  public String toString() {
    return axisName + " nestingLevel= " + nestingLevel;
  }
}
