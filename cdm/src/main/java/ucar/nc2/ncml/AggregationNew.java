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

    // create aggregation coordinate variable
    DataType coordType = getCoordinateType();
    VariableDS joinAggCoord = new VariableDS(ncDataset, null, null, dimName, coordType, dimName, null, null);
    ncDataset.addVariable(null, joinAggCoord);
    joinAggCoord.setProxyReader( this);
    if (isDate)
      joinAggCoord.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, "Time"));

    // if speced externally, this variable will get replaced
    CacheVar cv = new CoordValueVar(joinAggCoord.getFullName(), joinAggCoord.getDataType(), joinAggCoord.getUnitsString());
    joinAggCoord.setSPobject( cv);
    cacheList.add(cv);

    List<String> aggVarNames = getAggVariableNames();

    // if no names specified, add all "non-coordinate" variables.
    // Note that we havent identified coordinate systems with CoordSysBuilder, so that info ius not available.
    // So this isnt that general of a solution. But probably better than nothing
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
