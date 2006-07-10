// $Id: CF1Convention.java,v 1.18 2006/05/31 20:51:11 caron Exp $
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
package ucar.nc2.dataset.conv;

import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.DataType;

import java.util.*;
import java.io.IOException;

/**
 * CF-1 Convention.
 * see http://www.cgd.ucar.edu/cms/eaton/cf-metadata/index.html
 * <p/>
 * <i>
 * "The CF conventions for climate and forecast metadata are designed to promote the
 * processing and sharing of files created with the netCDF API. The conventions define
 * metadata that provide a definitive description of what the data in each variable
 * represents, and of the spatial and temporal properties of the data.
 * This enables users of data from different sources to decide which quantities are
 * comparable, and facilitates building applications with powerful extraction, regridding,
 * and display capabilities."
 * </i>
 *
 * @author caron
 * @version $Revision: 1.18 $ $Date: 2006/05/31 20:51:11 $
 */

public class CF1Convention extends CSMConvention {


  private static String[] vertical_coords = {"atmosphere_sigma_coordinate",
          "atmosphere_hybrid_sigma_pressure_coordinate",
          "atmosphere_hybrid_height_coordinate",
          "atmosphere_sleve_coordinate",
          "ocean_sigma_coordinate",
          "ocean_s_coordinate",
          "ocean_sigma_z_coordinate",
          "ocean_double_sigma_coordinate"};

  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) {
    this.conventionName = "CF-1.0";

    // look for transforms
    List vars = ds.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      Variable v = (Variable) vars.get(i);

      // look for vertical transforms
      String sname = ds.findAttValueIgnoreCase(v, "standard_name", null);
      if (sname != null) {
        sname = sname.trim();

        if (sname.equalsIgnoreCase("atmosphere_ln_pressure_coordinate")) {
          makeAtmLnCoordinate( ds, v);
          continue;
        }

        for (int j = 0; j < vertical_coords.length; j++)
          if (sname.equalsIgnoreCase(vertical_coords[j])) {
            v.addAttribute(new Attribute("_CoordinateTransformType", TransformType.Vertical.toString()));
            // v.addAttribute( new Attribute("_CoordinateAxes", v.getName())); LOOK: may also be time dependent
          }
      }

      // look for horiz transforms
      String grid_mapping_name = ds.findAttValueIgnoreCase(v, "grid_mapping_name", null);
      if (grid_mapping_name != null) {
        grid_mapping_name = grid_mapping_name.trim();
        v.addAttribute(new Attribute("_CoordinateTransformType", TransformType.Projection.toString()));
        v.addAttribute(new Attribute("_CoordinateAxisTypes", "GeoX GeoY"));
      }

    }

    ds.finish();
  }

  private void makeAtmLnCoordinate(NetcdfDataset ds, Variable v) {
    // get the formula attribute
    String formula = ds.findAttValueIgnoreCase(v, "formula_terms", null);
    if (null == formula) {
      String msg = " Need attribute 'formula_terms' on Variable " + v.getName() + "\n";
      parseInfo.append(msg);
      userAdvice.append(msg);
      return;
    }

    // parse the formula string
    Variable p0Var = null, levelVar = null;
    StringTokenizer stoke = new StringTokenizer(formula, " :");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken();
      if (toke.equalsIgnoreCase("p0")) {
        String name = stoke.nextToken();
        p0Var = ds.findVariable(name);
      } else if (toke.equalsIgnoreCase("lev")) {
        String name = stoke.nextToken();
        levelVar = ds.findVariable(name);
      }
    }

    if (null == p0Var) {
      String msg = " Need p0:varName on Variable " + v.getName() + " formula_terms\n";
      parseInfo.append(msg);
      userAdvice.append(msg);
      return;
    }

    if (null == levelVar) {
      String msg = " Need lev:varName on Variable " + v.getName() + " formula_terms\n";
      parseInfo.append(msg);
      userAdvice.append(msg);
      return;
    }

    String units = ds.findAttValueIgnoreCase(p0Var, "units", "hPa");

    // create the data and the variable
    try { // p(k) = p0 * exp(-lev(k))
      double p0 = p0Var.readScalarDouble();
      Array levelData = levelVar.read();
      Array pressureData = Array.factory(double.class, levelData.getShape() );
      IndexIterator ii = levelData.getIndexIterator();
      IndexIterator iip = pressureData.getIndexIterator();
      while (ii.hasNext())  {
        double val = p0 * Math.exp( -1.0 * ii.getDoubleNext());
        iip.setDoubleNext(val);
      }

      CoordinateAxis1D p = new CoordinateAxis1D( ds, null, v.getShortName()+"_pressure", DataType.DOUBLE,
              levelVar.getDimensionsString(), units,
              "Vertical Pressure coordinate synthesized from atmosphere_ln_pressure_coordinate formula");
      p.setCachedData(pressureData, false);
      p.addAttribute(new Attribute("_CoordinateAxisType", "Pressure"));
      p.addAttribute(new Attribute("_CoordinateAliasForDimension", p.getDimensionsString()));
      ds.addVariable(null, p);
      Dimension d = p.getDimension(0);
      d.addCoordinateVariable(p);
      parseInfo.append(" added Vertical Pressure coordinate "+p.getName()+"\n");

    } catch (IOException e) {
      String msg = " Unable to read variables from " + v.getName() + " formula_terms\n";
      parseInfo.append(msg);
      userAdvice.append(msg);
    }

  }

  // we assume that coordinate axes get identified by
  //  1) being coordinate variables or
  //  2) being listed in coordinates attribute.

  /**
   * Augment CSM axis type identification with "projection_x_coordinate", "projection_y_coordinate"
   * and  the various dimensionless vertical coordinates
   */
  protected AxisType getAxisType(NetcdfDataset ncDataset, VariableEnhanced v) {

    String sname = ncDataset.findAttValueIgnoreCase((Variable) v, "standard_name", null);
    if (sname != null) {
      sname = sname.trim();

      if (sname.equalsIgnoreCase("projection_x_coordinate"))
        return AxisType.GeoX;

      if (sname.equalsIgnoreCase("projection_y_coordinate"))
        return AxisType.GeoY;

      for (int i = 0; i < vertical_coords.length; i++)
        if (sname.equalsIgnoreCase(vertical_coords[i]))
          return AxisType.GeoZ;
    }

    // dont use axis attribute - not clear enough

    return super.getAxisType(ncDataset, v);
  }

  /**
   * Assign CoordinateTransform objects to Coordinate Systems.
   */
  protected void assignCoordinateTransforms(NetcdfDataset ncDataset) {
    super.assignCoordinateTransforms(ncDataset);

    // need to explicitly assign vertical transforms
    for (int i = 0; i < varList.size(); i++) {
      VarProcess vp = (VarProcess) varList.get(i);
      if (vp.isCoordinateTransform && (vp.ct != null) && (vp.ct.getTransformType() == TransformType.Vertical)) {
        List domain = getFormulaDomain(ncDataset, vp.v);
        if (null == domain) continue;

        List csList = ncDataset.getCoordinateSystems();
        for (int j = 0; j < csList.size(); j++) {
          CoordinateSystem cs = (CoordinateSystem) csList.get(j);
          if (!cs.containsAxis(vp.v.getShortName())) continue; // cs must contain the vertical axis
          if (cs.containsDomain(domain)) { // cs must contain the formula domain
            cs.addCoordinateTransform(vp.ct);
            parseInfo.append(" assign (CF) coordTransform " + vp.ct + " to CoordSys= " + cs + "\n");
          }
        }
      }
    }
  }

  // run through all the variables in the formula, and get their domain (list of dimensions)
  private List getFormulaDomain(NetcdfDataset ds, Variable v) {
    String formula = ds.findAttValueIgnoreCase(v, "formula_terms", null);
    if (null == formula) {
      parseInfo.append("*** Cant find formula_terms attribute ");
      return null;
    }

    ArrayList domain = new ArrayList();
    StringTokenizer stoke = new StringTokenizer(formula);
    while (stoke.hasMoreTokens()) {
      String what = stoke.nextToken();
      String varName = stoke.nextToken();
      Variable formulaV = ds.findVariable(varName);
      if (null == formulaV) {
        parseInfo.append("*** Cant find formula variable "+varName);
        continue;
      }
      domain.addAll(formulaV.getDimensions());
    }

    return domain;
  }

}

