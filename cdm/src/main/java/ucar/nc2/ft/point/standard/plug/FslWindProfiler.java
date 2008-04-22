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
package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Variable;
import ucar.nc2.ft.point.standard.NestedTable;
import ucar.nc2.ft.point.standard.TableAnalyzer;
import ucar.ma2.StructureMembers;
import ucar.ma2.ArrayStructureMA;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * @author caron
 * @since Apr 18, 2008
 */
public class FslWindProfiler extends TableAnalyzer {

  // :title = "WPDN data : selected by ob time : time range from 1207951200 to 1207954800";
  static public boolean isMine(NetcdfDataset ds) {
    String title = ds.findAttValueIgnoreCase(null, "title", null);
    return title != null && (title.startsWith("WPDN data"));
  }

  @Override
  public void makeJoins() throws IOException {
    super.makeJoins();

    List<Variable> vars = new ArrayList<Variable>();
    vars.add(ds.findVariable("staName"));
    vars.add(ds.findVariable("staLat"));
    vars.add(ds.findVariable("staLon"));
    vars.add(ds.findVariable("staElev"));

    StructureMembers members = new StructureMembers("station");
    for (Variable v : vars) {
      StructureMembers.Member m = members.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), new int[0]);
      m.setDataArray(v.read());
    }

    int n = ds.getUnlimitedDimension().getLength();
    ArrayStructureMA as = new ArrayStructureMA(members, new int[]{n});
    NestedTable.Table stnTable = new NestedTable.Table("station", as);
    addTable(stnTable);
    stnTable.getDataVariables().addAll(vars);

    NestedTable.Table obsTable = tableFind.get("recNum");
    NestedTable.Join join = new NestedTable.Join(NestedTable.JoinType.Identity);
    join.setTables(stnTable, obsTable);
    joins.add(join);
  }

}
