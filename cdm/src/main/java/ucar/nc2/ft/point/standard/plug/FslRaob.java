/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.ft.point.standard.TableConfigurerImpl;
import ucar.nc2.ft.point.standard.TableConfig;
import ucar.nc2.ft.point.standard.PointConfigXML;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.ma2.DataType;

import java.util.Formatter;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * RAOBs from FSL
 *
 * @author caron
 * @since Nov 24, 2009
 */
public class FslRaob extends TableConfigurerImpl {

  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    String title = ds.findAttValueIgnoreCase(null, "version", null);
    return title != null && (title.startsWith("Forecast Systems Lab 1.3") || title.startsWith("Forecast Systems Lab 1.4"));
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {
    String title = ds.findAttValueIgnoreCase(null, "version", null);
    assert title != null;
    boolean v4 = title.startsWith("Forecast Systems Lab 1.4");
    String xml = v4 ? "resources/nj22/pointConfig/FslRaob14.xml" : "resources/nj22/pointConfig/FslRaob13.xml";

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

}
