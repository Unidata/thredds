/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.conv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayObject;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CancelTask;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.unidata.geoloc.projection.TransverseMercator;
import ucar.unidata.geoloc.projection.Stereographic;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Default Coordinate Conventions.
 * Split from GDV.
 * @author caron
 * @since Dec 17, 2008
 */
public class DefaultConvention extends CoordSysBuilder {
  static private final Logger logger = LoggerFactory.getLogger(DefaultConvention.class);

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
        if (dimName.length() == 0) // none
          continue;
        Dimension dim = ds.findDimension(dimName);
        if (null != dim) {
          vp.isCoordinateAxis = true;
          parseInfo.format(" Coordinate Axis added (alias) = %s for dimension %s%n", vp.v.getFullName(), dimName);
        }
      }

      // coordinates is an alias for _CoordinateAxes
      for (VarProcess vp : varList) {
        if (vp.coordAxes == null) { // dont override if already set
          String coordsString = ds.findAttValueIgnoreCase(vp.v, CF.COORDINATES, null);
          if (coordsString != null) {
            vp.coordinates = coordsString;
          }
        }
      }

      super.findCoordinateAxes(ds);

      /////////////////////////
      // now we start forcing
      HashMap<AxisType, VarProcess> map = new HashMap<>();

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

          if (unit != null && SimpleUnit.isDateUnit(unit)) {
            vp.isCoordinateAxis = true;
            map.put(AxisType.Time, vp);
            parseInfo.format(" Time Coordinate Axis added (unit) = %s from unit %s%n", vp.v.getFullName(), unit);
            //break; // allow multiple time coords
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
            parseInfo.format(" Coordinate Axis added (Default forced) = %s for axis %s%n", vp.v.getFullName(), atype);
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
          return ve.getFullName();
        }
      }
      return null;
    }

    protected void makeCoordinateTransforms(NetcdfDataset ds) {
      if (projCT != null) {
        VarProcess vp = findVarProcess(projCT.getName(), null);
        if (vp != null)
          vp.ct = projCT;
      }
      super.makeCoordinateTransforms(ds);
    }

    protected AxisType getAxisType(NetcdfDataset ds, VariableEnhanced ve) {
      AxisType result = getAxisTypeCoards(ds, ve);
      if (result != null) return result;

      Variable v = (Variable) ve;
      String vname = v.getShortName();
      if (vname == null)
        return null;
      String unit = v.getUnitsString();
      if (unit == null) unit = "";
      String desc = v.getDescription();
      if (desc == null) desc = "";

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
          vname.equalsIgnoreCase("altitude") || desc.contains("altitude") || vname.equalsIgnoreCase("depth") ||
          vname.equalsIgnoreCase("elev") || vname.equalsIgnoreCase("elevation")) {
        if (SimpleUnit.isCompatible("m", unit)) // units of meters
          return AxisType.Height;
    }

      if (vname.equalsIgnoreCase("time") || findAlias(ds, v).equalsIgnoreCase("time"))  {
        if (SimpleUnit.isDateUnit(unit))
          return AxisType.Time;
      }

      if (vname.equalsIgnoreCase("time") && v.getDataType() == DataType.STRING) {
        try {
          Array firstValue = v.read("0");
          // System.out.printf("%s%n", NCdumpW.printArray(firstValue, "firstValue", null));
          if (firstValue instanceof ArrayObject.D1) {
            ArrayObject.D1 sarry = (ArrayObject.D1) firstValue;
            String firstStringValue = (String) sarry.get(0);
            if (CalendarDate.parseISOformat(null, firstStringValue) != null) // valid iso date string
              return AxisType.Time;
          }
        } catch (IOException | InvalidRangeException e) {
          logger.warn("time string error", e);
        }
      }

      return null;
    }

    // look for an coord_axis or coord_alias attribute
    private String findAlias(NetcdfDataset ds, Variable v) {
      String alias = ds.findAttValueIgnoreCase(v, "coord_axis", null);
      if (alias == null)
        alias = ds.findAttValueIgnoreCase(v, "coord_alias", "");
      if (alias == null) alias = "";
      return alias;
    }

  // replicated from COARDS, but we need to diverge from COARDS
    // we assume that coordinate axes get identified by being coordinate variables
   private AxisType getAxisTypeCoards( NetcdfDataset ncDataset, VariableEnhanced v) {

     String unit = v.getUnitsString();
     if (unit == null)
       return null;

     if( unit.equalsIgnoreCase("degrees_east") ||
             unit.equalsIgnoreCase("degrees_E") ||
             unit.equalsIgnoreCase("degreesE") ||
             unit.equalsIgnoreCase("degree_east") ||
             unit.equalsIgnoreCase("degree_E") ||
             unit.equalsIgnoreCase("degreeE"))
       return AxisType.Lon;

     if ( unit.equalsIgnoreCase("degrees_north") ||
             unit.equalsIgnoreCase("degrees_N") ||
             unit.equalsIgnoreCase("degreesN") ||
             unit.equalsIgnoreCase("degree_north") ||
             unit.equalsIgnoreCase("degree_N") ||
             unit.equalsIgnoreCase("degreeN"))
       return AxisType.Lat;

     if (SimpleUnit.isDateUnit(unit)) // || SimpleUnit.isTimeUnit(unit)) removed dec 18, 2008
       return AxisType.Time;

     // look for other z coordinate
     //if (SimpleUnit.isCompatible("m", unit))
     //  return AxisType.Height;
     if (SimpleUnit.isCompatible("mbar", unit))
       return AxisType.Pressure;
     if (unit.equalsIgnoreCase("level") || unit.equalsIgnoreCase("layer") || unit.equalsIgnoreCase("sigma_level"))
       return AxisType.GeoZ;

     String positive = ncDataset.findAttValueIgnoreCase((Variable) v, "positive", null);
     if (positive != null) {
       if (SimpleUnit.isCompatible("m", unit))
         return AxisType.Height;
       else
         return AxisType.GeoZ;
     }
     return null;
   }

    private ProjectionCT makeProjectionCT(NetcdfDataset ds) {
      // look for projection in global attribute
      String projection = ds.findAttValueIgnoreCase(null, "projection", null);
      if (null == projection) {
        parseInfo.format("Default Conventions error: NO projection name found %n");
        return null;
      }
      String params = ds.findAttValueIgnoreCase(null, "projection_params", null);
      if (null == params) params = ds.findAttValueIgnoreCase(null, "proj_params", null);
      if (null == params) {
        parseInfo.format("Default Conventions error: NO projection parameters found %n");
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

      parseInfo.format("Default Conventions projection %s params = %f %f %f %f%n", projection, p[0],p[1],p[2],p[3]);

      ProjectionImpl proj;
      if (projection.equalsIgnoreCase("LambertConformal"))
        proj = new LambertConformal(p[0], p[1], p[2], p[3]);
      else if (projection.equalsIgnoreCase("TransverseMercator"))
        proj = new TransverseMercator(p[0], p[1], p[2]);
      else if (projection.equalsIgnoreCase("Stereographic") || projection.equalsIgnoreCase("Oblique_Stereographic"))
        proj = new Stereographic(p[0], p[1], p[2]);
      else {
        parseInfo.format("Default Conventions error: Unknown projection %s%n",projection);
        return null;
      }

      return new ProjectionCT(proj.getClassName(), "FGDC", proj);
    }

}

