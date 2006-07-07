// $Id: VariableSimpleAdapter.java,v 1.1 2005/05/23 20:18:36 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
package ucar.nc2.dt;

import ucar.ma2.DataType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.VariableSimpleIF;

import java.util.List;


/**
 * Adapt a VariableSimpleIF into another VariableSimpleIF, so it can be subclassed.
 * @author caron
 * @version $Revision: 1.1 $ $Date: 2005/05/23 20:18:36 $
 */

public class VariableSimpleAdapter implements VariableSimpleIF {
  protected VariableSimpleIF v;

  public VariableSimpleAdapter( VariableSimpleIF v) {
    this.v = v;
  }

  public String getName() { return v.getName(); }
  public String getShortName() { return v.getShortName(); }
  public DataType getDataType() { return v.getDataType(); }
  public String getDescription() { return v.getDescription(); }
  public String getInfo() { return v.toString(); }
  public String getUnitsString() { return v.getUnitsString(); }

  public int getRank() {  return v.getRank(); }
  public int[] getShape() { return v.getShape(); }
  public List getAttributes() { return v.getAttributes(); }
  public ucar.nc2.Attribute findAttributeIgnoreCase(String attName){
    return v.findAttributeIgnoreCase(attName);
  }

  public double convertScaleOffsetMissing(byte value) {
    return v.convertScaleOffsetMissing( value);
  }
  public double convertScaleOffsetMissing(short value) {
    return v.convertScaleOffsetMissing( value);
  }
  public double convertScaleOffsetMissing(int value) {
    return v.convertScaleOffsetMissing( value);
  }
  public double convertScaleOffsetMissing(long value) {
    return v.convertScaleOffsetMissing( value);
  }
  public double convertScaleOffsetMissing(double value) {
    return v.convertScaleOffsetMissing( value);
  }

  public String toString() {
    return v.toString();
  }
}