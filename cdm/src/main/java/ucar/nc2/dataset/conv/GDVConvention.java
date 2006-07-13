// $Id:GDVConvention.java 51 2006-07-12 17:13:13Z caron $
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
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import java.util.*;

/**
 * GDV Conventions.
 * Deprecateed - use CF or _Coordinates.
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */


public class GDVConvention extends CSMConvention {
  protected ProjectionCT projCT = null;

  public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) {
    this.conventionName = "GDV";

    projCT = makeProjectionCT( ds);
    if (projCT != null) {
      VariableDS v = makeCoordinateTransformVariable(ds, projCT);
      ds.addVariable(null, v);

      String xname = findCoordinateName( ds, AxisType.GeoX);
      String yname = findCoordinateName( ds, AxisType.GeoY);
      if (xname != null && yname != null)
        v.addAttribute( new Attribute("_CoordinateAxes", xname+" "+yname));
    }

    ds.finish();
  }

 /** look for aliases. */
  protected void findCoordinateAxes( NetcdfDataset ds) {

    for (int i = 0; i < varList.size(); i++) {
      VarProcess vp = (VarProcess) varList.get(i);
      if (vp.isCoordinateVariable) continue;

      Variable ncvar = vp.v;
      if (!(ncvar instanceof VariableDS)) continue; // cant be a structure

      String dimName = findAlias( ds, ncvar);
      if (dimName.equals("")) // none
        continue;
      Dimension dim = ds.findDimension( dimName);
      if (null != dim)
        vp.isCoordinateAxis = true;
    }

    super.findCoordinateAxes( ds);
  }

   /** look for aliases. */
  private String findCoordinateName( NetcdfDataset ds, AxisType axisType) {

    List vlist = ds.getVariables();
    for (int i = 0; i < vlist.size(); i++) {
      VariableEnhanced ve = (VariableEnhanced) vlist.get(i);
      if (axisType == getAxisType( ds, ve)) {
        return ve.getName();
      }
    }
     return null;
  }

   protected void makeCoordinateTransforms( NetcdfDataset ds) {
     if (projCT != null) {
      VarProcess vp = findVarProcess(projCT.getName());
      if (vp != null)
        vp.ct = projCT;
     }
     super.makeCoordinateTransforms(  ds);
   }

  protected AxisType getAxisType( NetcdfDataset ds, VariableEnhanced ve) {
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

    return super.getAxisType( ds, ve);
  }

  // look for an coord_axis or coord_alias attribute
  private String findAlias( NetcdfDataset ds, Variable v) {
    String alias =  ds.findAttValueIgnoreCase(v, "coord_axis", null);
    if (alias == null)
      alias =  ds.findAttValueIgnoreCase(v, "coord_alias", "");
    return alias;
  }

  private ProjectionCT makeProjectionCT(NetcdfDataset ds) {
    // look for projection in global attribute
    String projection = ds.findAttValueIgnoreCase(null, "projection", null);
    if (null == projection) {
      parseInfo.append("GDV Conventions error: NO projection name found \n");
      return null;
    }
    String params = ds.findAttValueIgnoreCase(null, "projection_params", null);
    if (null == params) params = ds.findAttValueIgnoreCase(null, "proj_params", null);
    if (null == params) {
      parseInfo.append("GDV Conventions error: NO projection parameters found \n");
      return null;
    }

    // parse the parameters
    int count = 0;
    double [] p = new double[4];
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

    parseInfo.append("GDV Conventions projection "+projection+" params = "+
      p[0]+" "+ p[1]+" "+ p[2]+" "+ p[3]+"\n");

    ProjectionImpl proj = null;
    if (projection.equalsIgnoreCase("LambertConformal"))
      proj = new LambertConformal(p[0], p[1], p[2], p[3]);
    else if (projection.equalsIgnoreCase("TransverseMercator"))
      proj = new TransverseMercator(p[0], p[1], p[2]);
    else if (projection.equalsIgnoreCase("Stereographic") || projection.equalsIgnoreCase("Oblique_Stereographic"))
      proj  = new Stereographic(p[0], p[1], p[2]);
    else {
      parseInfo.append("GDV Conventions error: Unknown projection "+projection+"\n");
      return null;
    }

    return new ProjectionCT( proj.getClassName(), "FGDC", proj);
  }

}
