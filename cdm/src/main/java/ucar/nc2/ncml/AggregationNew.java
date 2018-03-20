/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
    super(ncd, dimName, Aggregation.Type.joinNew, recheckS);
  }

  protected void buildNetcdfDataset(CancelTask cancelTask) throws IOException {
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

    List<String> aggVarNames = getAggVariableNames();

    // Look for a variable matching the new aggregation dimension
    Variable joinAggCoord = ncDataset.findVariable(dimName);

    // Not found, create the aggregation coordinate variable
    if (joinAggCoord == null) {
      DataType coordType = getCoordinateType();
      joinAggCoord = new VariableDS(ncDataset, null, null, dimName, coordType, dimName, null, null);
      ncDataset.addVariable(null, joinAggCoord);
      joinAggCoord.setProxyReader(this);
      if (isDate)
        joinAggCoord.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, "Time"));

      // if speced externally, this variable will get replaced
      CacheVar cv = new CoordValueVar(joinAggCoord.getFullName(), joinAggCoord.getDataType(), joinAggCoord.getUnitsString());
      joinAggCoord.setSPobject(cv);
      cacheList.add(cv);
    } else if (joinAggCoord.isScalar()) {
      // For an existing variable matching the aggregated dim name, if it's a scalar
      // variable, we can just use it and its values for the aggregation coordinate variable
      // Need to ensure it's included in the list of variables to aggregate
      if (!aggVarNames.contains(joinAggCoord.getShortName())) {
        aggVarNames.add(joinAggCoord.getShortName());
      }
    } else {
      throw new IllegalArgumentException("Variable " + dimName + " already exists, but is not a scalar (suitable for aggregating as a coordinate).");
    }

    // if no names specified, add all "non-coordinate" variables.
    // Note that we haven't identified coordinate systems with CoordSysBuilder, so that info is not available.
    // So this isn't that general of a solution. But probably better than nothing
    if (aggVarNames.size() == 0) {
      for (Variable v : typical.getVariables()) {
        if (!(v instanceof CoordinateAxis))
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
      vagg.setProxyReader( this);
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

    if (isDate && timeUnitsChange) {
      readTimeCoordinates(ncDataset.findVariable(dimName), cancelTask);
    }

    ncDataset.finish();
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
