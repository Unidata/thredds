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
import ucar.nc2.Dimension;
import ucar.ma2.ArrayStructure;

import java.util.List;
import java.util.ArrayList;

/**
 * This encapsolates the info needed by NestedTable to handle point feature "nested table" datasets.
 * A TableAnalyzer creates these from a specific dataset convention.
 * <p> a TableConfig has a tree of TableConfigs, representing the join of parent and children tables.
 *
 * @author caron
 * @since Apr 23, 2008
 */
public class TableConfig {
  public TableType type;
  public String name;
  public TableConfig parent;
  public JoinConfig join; // the join to its parent
  public List<TableConfig> children;

  public FeatureType featureType;

  // multidim: outer and inner dimensions
  public Dimension dim, outer;
  
  public String limit;

  // TableType ArrayStructure
  public ArrayStructure as;

  // coordinate variable names
  public String lat, lon, elev, time, timeNominal;

  // station info
  public String stnId, stnDesc, stnNpts, stnWmoId;
  public int nstations;

  /**
   * Constructor
   * @param type  type of join
   * @param name  name of table
   */
  public TableConfig(TableType type, String name) {
    this.type = type;
    this.name = name;
  }

  public void addChild(TableConfig t) {
    if (children == null) children = new ArrayList<TableConfig>();
    children.add(t);
    t.parent = this;
  }

  public static class JoinConfig {
    public JoinType joinType;
    public Join override;

    // variable names for linked and contiguous lists
    public String start, next, numRecords, parentIndex;

    public JoinConfig(JoinType joinType) {
      this.joinType = joinType;
    }
  }

}
