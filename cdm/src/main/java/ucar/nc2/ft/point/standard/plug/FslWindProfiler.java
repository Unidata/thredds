/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.point.standard.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.nc2.Dimension;
import ucar.ma2.DataType;

import java.util.Formatter;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * FLS Wind profile data
 * @author caron
 * @since Apr 23, 2008
 */
public class FslWindProfiler extends TableConfigurerImpl  {

    // :title = "WPDN data : selected by ob time : time range from 1207951200 to 1207954800";
  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    String title = ds.findAttValueIgnoreCase(null, "title", null);
    if (title == null) {
        title = ds.findAttValueIgnoreCase(null, "DD_reference", null);
        if(title != null) {
            title = ds.findVariable("staLat") != null ? title : null;
        }
    }
    return title != null && (title.startsWith("WPDN data") || title.startsWith("RASS data") || title.contains("88-21-R2"));
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {
    String title = ds.findAttValueIgnoreCase(null, "title", null);
    if (title == null)
        title = ds.findAttValueIgnoreCase(null, "DD_reference", null);
    assert (title != null);

    boolean isRass = title.startsWith("RASS data");
    String xml = isRass ? "resources/nj22/pointConfig/FslRassProfiler.xml" : "resources/nj22/pointConfig/FslWindProfiler.xml";

    PointConfigXML reader = new PointConfigXML();
    TableConfig tc = reader.readConfigXMLfromResource(xml, wantFeatureType, ds, errlog);

    for (TableConfig inner : tc.children.get(0).children)
      makeMultidimInner(ds, tc, inner, inner.outerName, inner.innerName);
    return tc;
  }

  private void makeMultidimInner(NetcdfDataset ds, TableConfig parentTable, TableConfig childTable, String outerDin, String innerDim) {
    Dimension parentDim = ds.findDimension(outerDin);
    Dimension childDim = ds.findDimension(innerDim);

    // divide up the variables between the parent and the child
    List<String> obsVars;
    List<Variable> vars = ds.getVariables();
    List<String> parentVars = new ArrayList<>(vars.size());
    obsVars = new ArrayList<>(vars.size());
    for (Variable orgV : vars) {
      if (orgV instanceof Structure) continue;

      Dimension dim0 = orgV.getDimension(0);
      if ((dim0 != null) && dim0.equals(parentDim)) {
        if ((orgV.getRank() == 1) || ((orgV.getRank() == 2) && orgV.getDataType() == DataType.CHAR)) {
          parentVars.add(orgV.getShortName());
        } else {
          Dimension dim1 = orgV.getDimension(1);
          if ((dim1 != null) && dim1.equals(childDim))
            obsVars.add(orgV.getShortName());
        }
      }
    }
    parentTable.vars = parentVars;
    childTable.vars = obsVars;
  }

  /* public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) {
    //TableConfig nt = new TableConfig(Table.Type.Singleton, "station");

    TableConfig profile = new TableConfig(Table.Type.Structure, "record");
    profile.structName = "record";
    profile.dimName = Evaluator.getDimensionName(ds, "recNum", errlog);
    profile.time = Evaluator.getVariableName(ds, "timeObs", errlog);

    profile.stnId = Evaluator.getVariableName(ds, "staName", errlog);
    profile.stnWmoId = Evaluator.getVariableName(ds, "wmoStaNum", errlog);
    profile.lat = Evaluator.getVariableName(ds, "staLat", errlog);
    profile.lon = Evaluator.getVariableName(ds, "staLon", errlog);
    // profile.elev = Evaluator.getVariableName(ds, "staElev", errlog);

    //profile.join = new TableConfig.JoinConfig(Join.Type.Identity);
    profile.featureType = FeatureType.PROFILE;
    // nt.addChild(profile);

    TableConfig levels = new TableConfig(Table.Type.MultidimInner, "levels");
    levels.outerName = profile.dimName;
    levels.innerName = Evaluator.getDimensionName(ds, "level", errlog);
    levels.elev = Evaluator.getVariableName(ds, "level", errlog);

    profile.addChild(levels);

    Dimension outer = Evaluator.getDimension(ds, "recNum", errlog);
    Dimension inner = Evaluator.getDimension(ds, "level", errlog);

    // divide up the variables between the parent and the obs
    List<String> obsVars = null;
    List<Variable> vars = ds.getVariables();
    List<String> parentVars = new ArrayList<String>(vars.size());
    obsVars = new ArrayList<String>(vars.size());
    for (Variable orgV : vars) {
      if (orgV instanceof Structure) continue;

      Dimension dim0 = orgV.getDimension(0);
      if ((dim0 != null) && dim0.equals(outer)) {
        if ((orgV.getRank() == 1) || ((orgV.getRank() == 2) && orgV.getDataType() == DataType.CHAR)) {
          parentVars.add(orgV.getShortName());
        } else {
          Dimension dim1 = orgV.getDimension(1);
          if ((dim1 != null) && dim1.equals(inner))
            obsVars.add(orgV.getShortName());
        }
      }
    }
    profile.vars = parentVars;
    levels.vars = obsVars;

    
    return profile;
  }   */

}
