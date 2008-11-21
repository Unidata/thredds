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

package ucar.nc2.ncml;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.DatasetConstructor;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.util.CancelTask;
import ucar.nc2.*;
import ucar.ma2.DataType;

import java.io.IOException;
import java.util.List;

/**
 * JoinNew Aggregation.
 *
 * @author caron
 */
public class AggregationNew extends AggregationOuterDimension {

  public AggregationNew(NetcdfDataset ncd, String dimName, String recheckS) {
    super(ncd, dimName, Aggregation.Type.JOIN_NEW, recheckS);
  }

  protected void buildDataset(CancelTask cancelTask) throws IOException {
    buildCoords(cancelTask);

    // open a "typical"  nested dataset and copy it to newds
    Dataset typicalDataset = getTypicalDataset();
    NetcdfFile typical = typicalDataset.acquireFile(null);
    DatasetConstructor.transferDataset(typical, ncDataset, null);

    // create aggregation dimension
    String dimName = getDimensionName();
    Dimension aggDim = new Dimension(dimName, getTotalCoords());
    ncDataset.removeDimension(null, dimName); // remove previous declaration, if any
    ncDataset.addDimension(null, aggDim);

    promoteGlobalAttributes( (DatasetOuterDimension) typicalDataset);

    // create aggregation coordinate variable
    DataType coordType = getCoordinateType();
    VariableDS joinAggCoord = new VariableDS(ncDataset, null, null, dimName, coordType, dimName, null, null);
    ncDataset.addVariable(null, joinAggCoord);
    joinAggCoord.setProxyReader(this);
    if (isDate)
      joinAggCoord.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, "Time"));
    CacheVar cv = new CoordValueVar(joinAggCoord.getName(), joinAggCoord.getDataType(), joinAggCoord.getUnitsString());
    joinAggCoord.setSPobject( cv);
    cacheList.add(cv);

    List<String> aggVarNames = getAggVariableNames();

    // if no names specified, add all "non-coordinate" variables.
    // Note that we havent identified coordinate systems with CoordSysBuilder, so that info ius not available.
    // So this isnt that general of a solution. But probably better than nothing
    if (aggVarNames.size() == 0) {
      for (Variable v : typical.getVariables()) {
        if (!v.isCoordinateVariable())
          aggVarNames.add(v.getShortName());
      }
    }

    // now we can create all the aggNew variables
    // use only named variables
    for (String varname : aggVarNames) {
      Variable aggVar = ncDataset.findVariable(varname);
      if (aggVar == null) {
        logger.error(ncDataset.getLocation() + " aggNewDimension cant find variable " + varname);
        continue;
      }

      // construct new variable, replace old one LOOK what about Structures?
      Group newGroup =  DatasetConstructor.findGroup(ncDataset, aggVar.getParentGroup());
      VariableDS vagg = new VariableDS(ncDataset, newGroup, null, aggVar.getShortName(), aggVar.getDataType(),
          dimName + " " + aggVar.getDimensionsString(), null, null);
      vagg.setProxyReader(this);
      DatasetConstructor.transferVariableAttributes(aggVar, vagg);

      // _CoordinateAxes if it exists must be modified
      Attribute att = vagg.findAttribute(_Coordinate.Axes);
      if (att != null) {
        String axes = dimName + " " + att.getStringValue();
        vagg.addAttribute(new Attribute(_Coordinate.Axes, axes));
      }

      newGroup.removeVariable( aggVar.getShortName());
      newGroup.addVariable( vagg);
      aggVars.add(vagg);

      if (cancelTask != null && cancelTask.isCancel()) return;
    }

    setDatasetAcquireProxy(typicalDataset, ncDataset);
    typicalDataset.close( typical); // close it because we use DatasetProxyReader to acquire
  }

  /**
   * What is the data type of the aggregation coordinate ?
   *
   * @return the data type of the aggregation coordinate
   */
  private DataType getCoordinateType() {
    List<Dataset> nestedDatasets = getDatasets();
    DatasetOuterDimension first = (DatasetOuterDimension) nestedDatasets.get(0);
    return first.isStringValued ? DataType.STRING : DataType.DOUBLE;
  }

}
