/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

package ucar.nc2.dataset;

/**
 * Type-safe enumeration of netCDF Dataset TransformType.
 *
 * @author john caron
 */

public class TransformType {

  private static java.util.Map<String,TransformType> hash = new java.util.HashMap<String,TransformType>(10);

  public final static TransformType Projection = new TransformType("Projection");
  public final static TransformType Vertical = new TransformType("Vertical");

    private String _TransformType;
    private TransformType(String s) {
      this._TransformType = s;
      hash.put( s, this);
    }

  /**
   * Find the AxisType that matches this name.
   * @param name find this name
   * @return AxisType or null if no match.
   */
  public static TransformType getType(String name) {
    if (name == null) return null;
    return hash.get( name);
  }

  /**
   * @return the string name.
   */
   public String toString() {
      return _TransformType;
  }

}
