// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package ucar.nc2.ncml;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.nc2.util.CancelTask;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.ma2.DataType;

import java.io.IOException;
import java.util.List;

/**
 * JoinNew Aggregation.
 *
 * @author caron
 */
public class AggregationNew extends Aggregation {
  public AggregationNew(NetcdfDataset ncd, String dimName, String recheckS) {
    super(ncd, dimName, Aggregation.Type.JOIN_NEW, recheckS);
  }

  protected void buildDataset(boolean isNew, CancelTask cancelTask) throws IOException {
    buildCoords(cancelTask);

    // open a "typical"  nested dataset and copy it to newds
    Dataset typicalDataset = getTypicalDataset();
    NetcdfFile typical = typicalDataset.acquireFile(null);
    NcMLReader.transferDataset(typical, ncDataset, isNew ? null : new MyReplaceVariableCheck());

    // create aggregation dimension
    String dimName = getDimensionName();
    Dimension aggDim = new Dimension(dimName, getTotalCoords(), true);
    ncDataset.removeDimension(null, dimName); // remove previous declaration, if any
    ncDataset.addDimension(null, aggDim);

    // create aggregation coordinate variable
    DataType coordType;
    VariableDS joinAggCoord = (VariableDS) ncDataset.getRootGroup().findVariable(dimName);
    if (joinAggCoord == null) {
      coordType = getCoordinateType();
      joinAggCoord = new VariableDS(ncDataset, null, null, dimName, coordType, dimName, null, null);
      ncDataset.addVariable(null, joinAggCoord);
    } else {
      coordType = joinAggCoord.getDataType();
      joinAggCoord.setDimensions(dimName); // reset its dimension
      if (!isNew) joinAggCoord.setCachedData(null, false); // get rid of any cached data, since its now wrong
    }
    joinAggCoord.setProxyReader(this);

    if (isDate()) {
      joinAggCoord.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, "Time"));
    }

    // now we can create all the aggNew variables
    // use only named variables
    List vars = getVariables();
    for (int i = 0; i < vars.size(); i++) {
      String varname = (String) vars.get(i);
      Variable v = ncDataset.getRootGroup().findVariable(varname);
      if (v == null) {
        logger.error(ncDataset.getLocation() + " aggNewDimension cant find variable " + varname);
        continue;
      }

      // construct new variable, replace old one
      VariableDS vagg = new VariableDS(ncDataset, null, null, v.getShortName(), v.getDataType(),
              dimName + " " + v.getDimensionsString(), null, null);
      vagg.setProxyReader(this);
      NcMLReader.transferVariableAttributes(v, vagg);

      ncDataset.removeVariable(null, v.getShortName());
      ncDataset.addVariable(null, vagg);

      if (cancelTask != null && cancelTask.isCancel()) return;
    }

    ncDataset.finish();
    makeProxies(typicalDataset, ncDataset);
    typical.close();
  }

}
