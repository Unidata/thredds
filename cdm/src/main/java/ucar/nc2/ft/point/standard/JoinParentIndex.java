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

import ucar.ma2.ArrayStructure;
import ucar.ma2.StructureData;
import ucar.nc2.Structure;

import java.io.IOException;

/**
 * Class Description.
 *
 * @author caron
 * @since Jan 22, 2009
 */
public class JoinParentIndex implements Join {
  private ArrayStructure parentData;
  private String parentIndex;

  public JoinParentIndex(Structure parentStructure, String parentIndex) {
    this.parentIndex = parentIndex;

    try {
      parentData = (ArrayStructure) parentStructure.read(); // cache entire station table  LOOK
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public StructureData getJoinData(StructureData sdata) {
    int index = sdata.getScalarInt(parentIndex);
    return parentData.getStructureData(index);
  }

}