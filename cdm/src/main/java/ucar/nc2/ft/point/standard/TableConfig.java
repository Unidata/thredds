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

import ucar.nc2.constants.FeatureType;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.ma2.ArrayStructure;
import ucar.ma2.StructureData;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * This encapsolates the info needed by NestedTable to handle point feature "nested table" datasets.
 * A TableAnalyzer creates these from a specific dataset convention.
 * <p> a TableConfig has a tree of TableConfigs, representing the join of parent and children tables.
 *
 * @author caron
 * @since Apr 23, 2008
 */
public class TableConfig {
  public Table.Type type;
  public String name;
  public TableConfig parent;
  public List<Join> extraJoin;
  public List<TableConfig> children;

  public String structName; // full name of structure
  public String  nestedTableName; // short name of structure
  public boolean isPsuedoStructure = false;

  // linked, contiguous  list
  public String start;  // name of variable - starting child index (in parent)
  public String next;  // name of variable - next child index (in child)
  public String numRecords;  // name of variable - number of children (in parent)

  // top only
  public FeatureType featureType;

  // TablePsuedoStructureList, Structure
  public List<String> vars;

  // multidim: outer and inner dimensions
  public Dimension dim, outer;
  
  // Table.Type ArrayStructure
  public ArrayStructure as;

  // Table.Type Singleton
  public StructureData sdata;

  // Table.Type ParentIndex
  public Map<Integer, List<Integer>> indexMap;
  public String parentIndex;  // name of variable - parent index (in parent)

  // coordinate variable names
  public String lat, lon, elev, time, timeNominal, limit;

  // station info
  public String stnId, stnDesc, stnNpts, stnWmoId, stnAlt;

  /**
   * Constructor
   * @param type  type of join
   * @param name  name of table
   */
  public TableConfig(Table.Type type, String name) {
    this.type = type;
    this.name = name;
    this.structName = name;
  }

  public void addChild(TableConfig t) {
    if (children == null) children = new ArrayList<TableConfig>();
    children.add(t);
    t.parent = this;
  }

  public void addJoin(Join extra) {
    if (extraJoin == null) extraJoin = new ArrayList<Join>(3);
    extraJoin.add(extra);
  }

}
