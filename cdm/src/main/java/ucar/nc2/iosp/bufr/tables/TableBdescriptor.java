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
package ucar.nc2.iosp.bufr.tables;

import java.util.Formatter;
import java.util.Map;
import java.util.List;

/**
 * Class Description.
 *
 * @author caron
 * @since Sep 25, 2008
 */
public interface TableBdescriptor {

  /**
   * scale of descriptor.
   *
   * @return scale
   */
  public int getScale();

  /**
   * refVal of descriptor.
   *
   * @return refVal
   */
  public int getRefVal();

  /**
   * width of descriptor.
   *
   * @return width
   */
  public int getWidth();

  /**
   * units of descriptor.
   *
   * @return units
   */
  public String getUnits();

  /**
   * description of descriptor.
   *
   * @return description
   */
  public String getDescription();

  /**
   * short name of descriptor.
   *
   * @return name
   */
  public String getName();

  /**
   * fxy
   *
   * @return fxy
   */
  public short getId();
  public String getFxy();

  /**
   * is data type of descriptor a numeric.
   *
   * @return true or false
   */
  public boolean isNumeric();

  public boolean isWMO();

  String toString();

  void show( Formatter out);

}
