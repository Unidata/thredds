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

package ucar.nc2.dataset.conv;

import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.util.CancelTask;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.unidata.geoloc.projection.TransverseMercator;
import ucar.unidata.geoloc.projection.Stereographic;

import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Default Coordinate Conventions.
 *
 * @author caron
 * @since Dec 17, 2008
 */
public class DefaultConvention extends CSMConvention {
    protected ProjectionCT projCT = null;

    public DefaultConvention() {
      this.conventionName = "Default";
    }

    public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) {

      projCT = makeProjectionCT(ds);
      if (projCT != null) {
        VariableDS v = makeCoordinateTransformVariable(ds, projCT);
        ds.addVariable(null, v);

        String xname = findCoordinateName(ds, AxisType.GeoX);
        String yname = findCoordinateName(ds, AxisType.GeoY);
        if (xname != null && yname != null)
          v.addAttribute(new Attribute(_Coordinate.Axes, xname + " " + yname));
      }

      ds.finish();
    }

    @Override
    protected void findCoordinateAxes(NetcdfDataset ds) {

      for (VarProcess vp : varList) {
        if (vp.isCoordinateVariable) continue;

        Variable ncvar = vp.v;
        if (!(ncvar instanceof VariableDS)) continue; // cant be a structure

        String dimName = findAlias(ds, ncvar);
        if (dimName.equals("")) // none
          continue;
        Dimension dim = ds.findDimension(dimName);
        if (null != dim) {
          vp.isCoordinateAxis = true;
          parseInfo.append(" Coordinate Axis added (alias) = ").append(vp.v.getName()).append(" for dimension ").append(dimName).append("\n");
        }
      }

      super.findCoordinateAxes(ds);

      /////////////////////////
      // now we start forcing
      HashMap<AxisType, VarProcess> map = new HashMap<AxisType, VarProcess>();

      // find existing axes, so we dont duplicate
      for (VarProcess vp : varList) {
        if (vp.isCoordinateAxis) {
          AxisType atype = getAxisType(ds, (VariableEnhanced) vp.v);
          if (atype != null)
            map.put(atype, vp);
        }
      }

      // look for time axes based on units
      if (map.get(AxisType.Time) == null) {
        for (VarProcess vp : varList) {
          Variable ncvar = vp.v;
          if (!(ncvar instanceof VariableDS)) continue; // cant be a structure
          String unit = ncvar.getUnitsString();

          if (SimpleUnit.isDateUnit(unit)) {
            vp.isCoordinateAxis = true;
            map.put(AxisType.Time, vp);
            parseInfo.append(" Coordinate Axis added (unit) = ").append(vp.v.getName()).append(" for dimension ").append("\n");
            break;
          }
        }
      }

      // look for missing axes by using name hueristics
      for (VarProcess vp : varList) {
        if (vp.isCoordinateVariable) continue;
        Variable ncvar = vp.v;
        if (!(ncvar instanceof VariableDS)) continue; // cant be a structure

        AxisType atype = getAxisType(ds, (VariableEnhanced) vp.v);
        if (atype != null) {
          if (map.get(atype) == null) {
            vp.isCoordinateAxis = true;
            parseInfo.append(" Coordinate Axis added (Default forced) = ").append(vp.v.getName()).append(" for axis ").append(atype).append("\n");
            map.put(atype, vp);
          }
        }
      }
    }

    /**
     * look for aliases.
     *
     * @param ds       containing dataset
     * @param axisType look for this axis type
     * @return name of axis of that type
     */
    private String findCoordinateName(NetcdfDataset ds, AxisType axisType) {
      List<Variable> vlist = ds.getVariables();
      for (Variable aVlist : vlist) {
        VariableEnhanced ve = (VariableEnhanced) aVlist;
        if (axisType == getAxisType(ds, ve)) {
          return ve.getName();
        }
      }
      return null;
    }

    protected void makeCoordinateTransforms(NetcdfDataset ds) {
      if (projCT != null) {
        VarProcess vp = findVarProcess(projCT.getName());
        if (vp != null)
          vp.ct = projCT;
      }
      super.makeCoordinateTransforms(ds);
    }

    protected AxisType getAxisType(NetcdfDataset ds, VariableEnhanced ve) {
      AxisType result = super.getAxisType(ds, ve);
      if (result != null) return result;

      Variable v = (Variable) ve;
      String vname = v.getName();

      if (vname.equalsIgnoreCase("x") || findAlias(ds, v).equalsIgnoreCase("x"))
        return AxisType.GeoX;

      if (vname.equalsIgnoreCase("lon") || vname.equalsIgnoreCase("longitude") || findAlias(ds, v).equalsIgnoreCase("lon"))
        return AxisType.Lon;

      if (vname.equalsIgnoreCase("y") || findAlias(ds, v).equalsIgnoreCase("y"))
        return AxisType.GeoY;

      if (vname.equalsIgnoreCase("lat") || vname.equalsIgnoreCase("latitude") || findAlias(ds, v).equalsIgnoreCase("lat"))
        return AxisType.Lat;

      if (vname.equalsIgnoreCase("lev") || findAlias(ds, v).equalsIgnoreCase("lev") ||
              (vname.equalsIgnoreCase("level") || findAlias(ds, v).equalsIgnoreCase("level")))
        return AxisType.GeoZ;

      if (vname.equalsIgnoreCase("z") || findAlias(ds, v).equalsIgnoreCase("z") ||
              (vname.equalsIgnoreCase("altitude") || vname.equalsIgnoreCase("depth")))
        return AxisType.Height;

      if (vname.equalsIgnoreCase("time") || findAlias(ds, v).equalsIgnoreCase("time"))
        return AxisType.Time;

      return null;
    }

    // look for an coord_axis or coord_alias attribute
    private String findAlias(NetcdfDataset ds, Variable v) {
      String alias = ds.findAttValueIgnoreCase(v, "coord_axis", null);
      if (alias == null)
        alias = ds.findAttValueIgnoreCase(v, "coord_alias", "");
      return alias;
    }

    private ProjectionCT makeProjectionCT(NetcdfDataset ds) {
      // look for projection in global attribute
      String projection = ds.findAttValueIgnoreCase(null, "projection", null);
      if (null == projection) {
        parseInfo.append("Default Conventions error: NO projection name found \n");
        return null;
      }
      String params = ds.findAttValueIgnoreCase(null, "projection_params", null);
      if (null == params) params = ds.findAttValueIgnoreCase(null, "proj_params", null);
      if (null == params) {
        parseInfo.append("Default Conventions error: NO projection parameters found \n");
        return null;
      }

      // parse the parameters
      int count = 0;
      double[] p = new double[4];
      try {
        // new way : just the parameters
        StringTokenizer stoke = new StringTokenizer(params, " ,");
        while (stoke.hasMoreTokens() && (count < 4)) {
          p[count++] = Double.parseDouble(stoke.nextToken());
        }
      } catch (NumberFormatException e) {
        // old way : every other one
        StringTokenizer stoke = new StringTokenizer(params, " ,");
        while (stoke.hasMoreTokens() && (count < 4)) {
          stoke.nextToken(); // skip
          p[count++] = Double.parseDouble(stoke.nextToken());
        }
      }

      parseInfo.append("Default Conventions projection ").append(projection).append(" params = ").append(p[0]).append(" ").append(p[1]).append(" ").append(p[2]).append(" ").append(p[3]).append("\n");

      ProjectionImpl proj;
      if (projection.equalsIgnoreCase("LambertConformal"))
        proj = new LambertConformal(p[0], p[1], p[2], p[3]);
      else if (projection.equalsIgnoreCase("TransverseMercator"))
        proj = new TransverseMercator(p[0], p[1], p[2]);
      else if (projection.equalsIgnoreCase("Stereographic") || projection.equalsIgnoreCase("Oblique_Stereographic"))
        proj = new Stereographic(p[0], p[1], p[2]);
      else {
        parseInfo.append("Default Conventions error: Unknown projection ").append(projection).append("\n");
        return null;
      }

      return new ProjectionCT(proj.getClassName(), "FGDC", proj);
    }

  }

