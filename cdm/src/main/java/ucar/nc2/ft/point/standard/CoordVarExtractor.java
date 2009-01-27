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

import ucar.ma2.StructureData;

/**
 * Class Description.
 *
 * @author caron
 * @since Jan 26, 2009
 */

// knows how to get specific coordinate data from a table or its parents
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

  public String toString() {
    return axisName + " nestingLevel= " + nestingLevel;
  }
}
