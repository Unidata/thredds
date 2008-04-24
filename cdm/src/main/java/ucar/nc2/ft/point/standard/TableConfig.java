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

import ucar.nc2.constants.FeatureType;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.ma2.ArrayStructure;

import java.util.List;
import java.util.ArrayList;

/**
 * @author caron
 * @since Apr 23, 2008
 */
public class TableConfig {
  public NestedTable.TableType type;
  public String name;
  public TableConfig parent;
  public JoinConfig join; // the join to its parent
  public List<TableConfig> children;

  public FeatureType featureType;

  // structure table
  public Dimension dim, outer;
  public String limit;

  // able Type ArrayStructure
  public ArrayStructure as;

  // coordinate variables
  public String lat, lon, elev, time, timeNominal;

  // station
  public String stnId, stnDesc, stnNpts, stnWmoId;
  public int nstations;

  public TableConfig(NestedTable.TableType type, String name) {
    this.type = type;
    this.name = name;
  }

  public void addChild(TableConfig t) {
    if (children == null) children = new ArrayList<TableConfig>();
    children.add(t);
    t.parent = this;
  }

  public static class JoinConfig {
    public Join.Type joinType;
    public Join override;

    // for linked and contiguous lists
    public String start, next, numRecords, parentIndex;

    public JoinConfig(Join.Type joinType) {
      this.joinType = joinType;
    }
  }

}
