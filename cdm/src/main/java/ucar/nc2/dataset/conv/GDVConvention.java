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
package ucar.nc2.dataset.conv;

import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import java.util.*;

/**
 * GDV Conventions.
 * Deprecated - use CF or _Coordinates.
 * DefaultConvention is now the default Convention
 * @author caron
 */


public class GDVConvention extends CSMConvention {
  protected ProjectionCT projCT = null;

  public GDVConvention() {
    this.conventionName = "GDV";
    checkForMeter = false;
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

  /**
   * look for aliases.
   */
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
        parseInfo.format(" Coordinate Axis added (GDV alias) = %s for dimension %s\n", vp.v.getName(), dimName);
      }
    }

    super.findCoordinateAxes(ds);

    // desperado
    findCoordinateAxesForce(ds);
  }

  private void findCoordinateAxesForce(NetcdfDataset ds) {

    HashMap<AxisType, VarProcess> map = new HashMap<AxisType, VarProcess>();

    // find existing axes, so we dont duplicate
    for (VarProcess vp : varList) {
      if (vp.isCoordinateAxis) {
        AxisType atype = getAxisType(ds, (VariableEnhanced) vp.v);
        if (atype != null)
          map.put(atype, vp);
      }
    }

    // look for variables to turn into axes
    for (VarProcess vp : varList) {
      if (vp.isCoordinateVariable) continue;
      Variable ncvar = vp.v;
      if (!(ncvar instanceof VariableDS)) continue; // cant be a structure

      AxisType atype = getAxisType(ds, (VariableEnhanced) vp.v);
      if (atype != null) {
        if (map.get(atype) == null) {
          vp.isCoordinateAxis = true;
          parseInfo.format(" Coordinate Axis added (GDV forced) = %s  for axis %s\n", vp.v.getName(), atype);
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

     return super.getAxisType(ds, ve);
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
      parseInfo.format("GDV Conventions error: NO projection name found \n");
      return null;
    }
    String params = ds.findAttValueIgnoreCase(null, "projection_params", null);
    if (null == params) params = ds.findAttValueIgnoreCase(null, "proj_params", null);
    if (null == params) {
      parseInfo.format("GDV Conventions error: NO projection parameters found \n");
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

    parseInfo.format("GDV Conventions projection %s params = %f %f %f %f\n", projection, p[0],p[1],p[2],p[3]);

    ProjectionImpl proj;
    if (projection.equalsIgnoreCase("LambertConformal"))
      proj = new LambertConformal(p[0], p[1], p[2], p[3]);
    else if (projection.equalsIgnoreCase("TransverseMercator"))
      proj = new TransverseMercator(p[0], p[1], p[2]);
    else if (projection.equalsIgnoreCase("Stereographic") || projection.equalsIgnoreCase("Oblique_Stereographic"))
      proj = new Stereographic(p[0], p[1], p[2]);
    else {
      parseInfo.format("GDV Conventions error: Unknown projection %s\n", projection);
      return null;
    }

    return new ProjectionCT(proj.getClassName(), "FGDC", proj);
  }

}
