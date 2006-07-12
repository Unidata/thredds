// $Id$
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
package ucar.nc2.dataset;

import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.dataset.conv.*;

import java.lang.reflect.Method;
import java.io.IOException;
import java.util.*;

/**
 * Abstract class for implementing Convention-specific parsing of netCDF files.
 *
 * You can use an NcML file alone (use registerNcML()) if file uses a convention attribute.
 * If not, you must implement a class that implements isMine() to identify your files, and
 *  call wrapNcML in the augmentDataset method (see eg ATDRadarConvention class).
 *
 * <p>
 * Subclasses Info:
 * <pre>
    // identify which variables are coordinate axes
    // default: 1) coordinate variables 2) variables with _coordinateAxisType attribute 3) variables listed
    // in a coordinates attribute on another variable.
    findCoordinateAxes( ncDataset);

    // identify which variables are used to describe coordinate system
    findCoordinateSystems( ncDataset);
    // identify which variables are used to describe coordinate transforms
    findCoordinateTransforms( ncDataset);
    // turn Variables into CoordinateAxis objects
    makeCoordinateAxes( ncDataset);
    // make Coordinate Systems for all Coordinate Systems Variables
    makeCoordinateSystems( ncDataset);

    // Assign explicit CoordinateSystem objects to variables
    assignExplicitCoordinateSystems( ncDataset);
    makeCoordinateSystemsImplicit( ncDataset);
    if (useMaximalCoordSys)
      makeCoordinateSystemsMaximal( ncDataset);

    makeCoordinateTransforms( ncDataset);
    assignCoordinateTransforms( ncDataset);
 </pre>
 *
 *
 * @author caron
 * @version $Revision$ $Date$
 */

/*
  Implementation notes:

  Generally, subclasses should add the _Coordinate conventions, see
    http://www.unidata.ucar.edu/content/software/netcdf-java/CoordinateAttributes.html
  Then let this class do the rest of the work.

  How to add Coordinate Transforms:
    A.
    1) create a dummy Variable called the Coordinate Transform Variable.
      This Coordinate Transform variable always has a name that identifies the transform,
      and any attributes needed for the transformation.
    2) explicitly point to it by adding a _CoordinateTransform attribute to a Coordinate System Variable
       _CoordinateTransforms = "LambertProjection HybridSigmaVerticalTransform"

    B. You could explicitly add it by overriding assignCoordinateTransforms()
 */

public class CoordSysBuilder implements CoordSysBuilderIF {
  /** resource path */
  static public String resourcesDir = "resources/nj22/coords/";
  static private HashMap conventionHash = new HashMap();
  static private ArrayList conventionList = new ArrayList();
  static private HashMap ncmlHash = new HashMap();
  static private boolean useMaximalCoordSys = true;
  static private boolean userMode = false;

  // search in the order added
  static { // wont get loaded unless explicitly called
    registerConvention("_Coordinates", CoordSysBuilder.class);
    registerConvention("Unidata Observation Dataset v1.0", UnidataObsConvention.class);

    registerConvention("COARDS", COARDSConvention.class);
    registerConvention("NCAR-CSM", CSMConvention.class);
    registerConvention("CF-1.0", CF1Convention.class);
    registerConvention("GDV", GDVConvention.class);

    registerConvention("ATDRadar", ATDRadarConvention.class);
    registerConvention("Zebra", ZebraConvention.class);
    registerConvention("GIEF/GIEF-F", GIEFConvention.class);
    registerConvention("IRIDL", IridlConvention.class);

    // the uglies
    registerConvention("NUWG", NUWGConvention.class);
    registerConvention("AWIPS", AWIPSConvention.class);
    registerConvention("AWIPS-Sat", AWIPSsatConvention.class);
    registerConvention("WRF", WRFConvention.class);

    registerConvention("M3IOVGGrid", M3IOVGGridConvention.class);
    registerConvention("IFPS", IFPSConvention.class);
    registerConvention("ARPS/ADAS", ADASConvention.class);

    //station data
    registerConvention("MADIS surface observations, v1.0", MADISStation.class);
    registerConvention("epic-insitu-1.0", EpicInsitu.class);

    // further calls to registerConvention are by the user
    userMode = true;
  }

  /**
   * Register an NcML file that implements a Convention by wrappping the dataset in the NcML.
   * It is then processed by CoordSysBuilder, using the _Coordinate attributes.
   * @param conventionName name of Convention, must be in the "Conventions" global attribute.
   * @param ncmlLocation location of NcML file, may be local file or URL.
   * @see ucar.nc2.ncml.NcMLReader#wrapNcML
   */
  static public void registerNcML( String conventionName, String ncmlLocation) {
    ncmlHash.put( conventionName, ncmlLocation);
  }

  /**
    * Register a class that implements a Convention.
    * @param conventionName name of Convention.
    *   This name will be used to look in the "Conventions" global attribute.
    *   Otherwise, you must implement the isMine() static method.
    * @param c implementation of CoordSysBuilderIF that parses those kinds of netcdf files.
    */
  static public void registerConvention( String conventionName, Class c) {
    if (!(CoordSysBuilderIF.class.isAssignableFrom( c)))
      throw new IllegalArgumentException("CoordSysBuilderIF Class "+c.getName()+" must implement CoordSysBuilderIF");

    // fail fast - check newInstance works
    try {
      c.newInstance();
    } catch (InstantiationException e) {
      throw new IllegalArgumentException("CoordSysBuilderIF Class "+c.getName()+" cannot instantiate, probably need default Constructor");
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("CoordSysBuilderIF Class "+c.getName()+" is not accessible");
    }

    // user stuff gets put at top
    if (userMode)
      conventionList.add( 0, new Convention( conventionName, c));
    else
      conventionList.add( new Convention( conventionName, c));

    // user stuff will override here
    conventionHash.put( conventionName, c);
  }

  /**
    * Register a class that implements a Convention.
    * @param conventionName name of Convention.
    *   This name will be used to look in the "Conventions" global attribute.
    *   Otherwise, you must implement the isMine() static method.
    * @param className name of subclass of CoordSysBuilder that parses those kinds of netcdf files.
    */
   static public void registerConvention( String conventionName, String className) throws ClassNotFoundException {
     Class c = Class.forName( className);
     registerConvention( conventionName, c);
   }

  /** If true, assign implicit CoordinateSystem objects to variables that dont have one yet.
   * Default value is false.
   * @see #makeCoordinateSystemsMaximal
   **/
  static public void setUseMaximalCoordSys( boolean b) { useMaximalCoordSys = b; }

  /** Get whether to make records into Structures. */
  static public boolean getUseMaximalCoordSys( ) { return useMaximalCoordSys; }

  /**
   * Add Coordinate information to a NetcdfDataset using a registered Convention parsing class.
   * @param ds the NetcdfDataset to modify
   * @param cancelTask allow user to bail out.
   */
  static public void addCoordinateSystems( NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    if (ds.getCoordSysWereAdded()) return; // ??

    // look for the Conventions attribute
    String convName = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (convName == null)
      convName = ds.findAttValueIgnoreCase(null, "Convention", null);
    if (convName != null)
      convName = convName.trim();

    if (convName != null) {
      // look for ncml first
      String convNcML;
      convNcML = (String) ncmlHash.get( convName);
      if (convNcML != null) {
        CoordSysBuilder csb = new CoordSysBuilder();
        NcMLReader.wrapNcML( ds, convNcML, cancelTask);
        csb.buildCoordinateSystems( ds);
        return;
      }
    }

    // now look for Convention parsing class
    Class convClass = null;
    if (convName != null) {
      // now look for Convention parsing class
      convClass = (Class) conventionHash.get( convName);

      // now look for comma or semicolon delimited list
      if ((convClass == null) && ((convName.indexOf(',') > 0) || (convName.indexOf(';') > 0)))  {
        ArrayList names = new ArrayList();
        StringTokenizer stoke = new StringTokenizer(convName, ",;");
        while (stoke.hasMoreTokens()) {
          String name = stoke.nextToken();
          names.add(name.trim());
        }

        // search the registered conventions, in order
        for (int i = 0; i < conventionList.size(); i++) {
          Convention conv = (Convention) conventionList.get(i);
          for (int j = 0; j < names.size(); j++) {
            convName = (String) names.get(j);
            if (convName.equalsIgnoreCase(conv.convName))
              convClass = conv.convClass;
          }
          if (convClass != null) break;
        }
      }
    }

    // look for ones that dont use Convention attribute, in order added.
    // call isMine() using reflection.
    if (convClass == null) {
      convName = null;
      for (int i = 0; i < conventionList.size(); i++) {
        Convention conv = (Convention) conventionList.get(i);
        Class c = conv.convClass;
        Method m;

        try {
          m = c.getMethod("isMine", new Class[] { NetcdfFile.class });
        } catch (NoSuchMethodException ex) {
          continue;
        }

        try {
          Boolean result = (Boolean) m.invoke(null, new Object[] {ds});
          if (result.booleanValue()) {
            convClass = c;
            break;
          }
        } catch (Exception ex) {
          System.out.println("ERROR: Class "+c.getName()+" Exception invoking isMine method\n"+ex);
        }
      } // iterator
    } // convClass is null

    // no convention class found, GDV is the default
    boolean usingDefault = (convClass == null);
    if (usingDefault)
      convClass = GDVConvention.class;

    // get an instance of that class
    CoordSysBuilderIF builder;
    try {
      builder = (CoordSysBuilderIF) convClass.newInstance();
      if (builder == null) return;
    } catch (InstantiationException e) {
      return;
    } catch (IllegalAccessException e) {
      return;
    }

    if (usingDefault) {
      builder.addUserAdvice("No CoordSysBuilder found - using default (GDV).\n");
    }

    // add the coord systems
    if (convName != null)
      builder.setConventionUsed( convName);
    else
      builder.addUserAdvice("No 'Convention' global attribute.\n");

    builder.augmentDataset( ds, cancelTask);
    builder.buildCoordinateSystems( ds);
  }

  static private class Convention {
    String convName;
    Class convClass;
    Convention(String convName,  Class convClass) {
      this.convName = convName;
      this.convClass = convClass;
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////

  protected String conventionName = "_Coordinates"; // name of Convention
  protected ArrayList varList = new ArrayList(); // varProcess objects
  protected StringBuffer parseInfo = new StringBuffer();
  protected StringBuffer userAdvice = new StringBuffer();

  protected boolean debug = false, showRejects = false;

  public void setConventionUsed( String convName) {
    this.conventionName = convName;
  }

  public void addUserAdvice( String advice) {
    userAdvice.append(advice);
  }
  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // subclasses can override any of these routines

  /**
   * This is where subclasses make changes to the dataset, like adding new variables, attribuites, etc.
   *
   * @param ncDataset modify this dataset
   * @param cancelTask give user a chance to bail out
   * @throws IOException
   */
  public void augmentDataset( NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException { }

  /**
   * Identify what kind of AxisType the named variable is.
   * Only called for variables already identified as Coodinate Axes.
   * @param ncDataset for this dataset
   * @param v a variable alreaddy identified as a Coodinate Axis
   * @return AxisType or null if unknown.
   */
  protected AxisType getAxisType( NetcdfDataset ncDataset, VariableEnhanced v) {
    return null;
  }

  /**
   * Heres where the work is to identify coordinate axes and coordinate systems.
   * @param ncDataset modify this dataset
   */
  public void buildCoordinateSystems( NetcdfDataset ncDataset) {
    // put status info into parseInfo that can be shown to someone trying to debug this process
    parseInfo.append("Parsing with Convention = "+conventionName+"\n");

    // Bookeeping info for each variable is kept in the VarProcess inner class
    addVariables( ncDataset, ncDataset.getVariables(), varList);

    // identify which variables are coordinate axes
    findCoordinateAxes( ncDataset);
    // identify which variables are used to describe coordinate system
    findCoordinateSystems( ncDataset);
    // identify which variables are used to describe coordinate transforms
    findCoordinateTransforms( ncDataset);
    // turn Variables into CoordinateAxis objects
    makeCoordinateAxes( ncDataset);
    // make Coordinate Systems for all Coordinate Systems Variables
    makeCoordinateSystems( ncDataset);

    // assign explicit CoordinateSystem objects to variables
    assignExplicitCoordinateSystems( ncDataset);

    // assign implicit CoordinateSystem objects to variables
    makeCoordinateSystemsImplicit( ncDataset);

    // optionally assign implicit CoordinateSystem objects to variables that dont have one yet
    if (useMaximalCoordSys)
      makeCoordinateSystemsMaximal( ncDataset);

    // make Coordinate Transforms
    makeCoordinateTransforms( ncDataset);

    // assign Coordinate Transforms
    assignCoordinateTransforms( ncDataset);

    NetcdfDatasetInfo info = ncDataset.getInfo();
    info.setCoordSysBuilderName( conventionName);
    info.addParseInfo(parseInfo.toString());
    info.addUserAdvice(userAdvice.toString());

    ncDataset.setCoordSysWereAdded (true);
    if (debug) System.out.println("parseInfo = \n"+parseInfo.toString());
  }

  private void addVariables( NetcdfDataset ncDataset, List varList, List varProcessList) {
    for (int i = 0; i < varList.size(); i++) {
      VariableEnhanced v = (VariableEnhanced) varList.get(i);
      varProcessList.add(new VarProcess(ncDataset, v));

      if (v instanceof Structure) {
        java.util.List nested = ((Structure) v).getVariables();
        addVariables( ncDataset, nested, varProcessList);
      }
    }
  }

  /**
   * Identify coordinate axes, set VarProcess.isCoordinateAxis = true.
   * Default is to look for those referenced by _CoordinateAxes attribute.
   * Note coordinate variables are already identified.
   */
  protected void findCoordinateAxes( NetcdfDataset ncDataset) {
    for (int i = 0; i < varList.size(); i++) {
      VarProcess vp = (VarProcess) varList.get(i);
      if (vp.coordAxes != null) {
        StringTokenizer stoker = new StringTokenizer( vp.coordAxes);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess( vname); // LOOK: full vs short name
          if (ap != null) {
            if (!ap.isCoordinateAxis)
              parseInfo.append(" CoordinateAxis = "+vname+" added; referenced from var= "+vp.v.getName()+"\n");
            ap.isCoordinateAxis = true;
          } else {
            parseInfo.append("***Cant find coordAxis "+vname+" referenced from var= "+vp.v.getName()+"\n");
            userAdvice.append("***Cant find coordAxis "+vname+" referenced from var= "+vp.v.getName()+"\n");
          }
        }
      }
    }
  }

  /**
   * Identify coordinate systems, set VarProcess.isCoordinateSystem = true.
   * Default is to look for those referenced by _CoordinateSystems attribute.
   */
  protected void findCoordinateSystems( NetcdfDataset ncDataset) {
    for (int i = 0; i < varList.size(); i++) {
      VarProcess vp = (VarProcess) varList.get(i);
      if (vp.coordSys != null) {
        StringTokenizer stoker = new StringTokenizer( vp.coordSys);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess( vname); // LOOK: full vs short name
          if (ap != null) {
            if (!ap.isCoordinateSystem )
              parseInfo.append(" CoordinateSystem = "+vname+" added; referenced from var= "+vp.v.getName()+"\n");
            ap.isCoordinateSystem = true;
          } else {
            parseInfo.append("***Cant find coordSystem "+vname+" referenced from var= "+vp.v.getName()+"\n");
            userAdvice.append("***Cant find coordSystem "+vname+" referenced from var= "+vp.v.getName()+"\n");
          }
        }
      }
    }
  }

  /**
   * Identify coordinate transforms, set VarProcess.isCoordinateTransform = true.
   * Default is to look for those referenced by _CoordinateTransforms attribute
   * ( or has a _CoordinateTransformType attribute, done in VarProcess constructor)
   */
  protected void findCoordinateTransforms( NetcdfDataset ncDataset) {
    for (int i = 0; i < varList.size(); i++) {
      VarProcess vp = (VarProcess) varList.get(i);
      if (vp.coordTransforms != null) {
        StringTokenizer stoker = new StringTokenizer( vp.coordTransforms);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess( vname); // LOOK: full vs short name
          if (ap != null) {
            if (!ap.isCoordinateTransform)
              parseInfo.append(" Coordinate Transform = "+vname+" added; referenced from var= "+vp.v.getName()+"\n");
            ap.isCoordinateTransform = true;
          } else {
            parseInfo.append("***Cant find coord Transform "+vname+" referenced from var= "+vp.v.getName()+"\n");
            userAdvice.append("***Cant find coord Transform "+vname+" referenced from var= "+vp.v.getName()+"\n");
          }
        }
      }
    }
  }

  /**
   * Take previously identified Coordinate Axis and Coordinate Variables and make them into a
   * CoordinateAxis. Uses the getAxisType() method to figure out the type, if not already set.
   */
  protected void makeCoordinateAxes( NetcdfDataset ncDataset) {
    for (int i = 0; i < varList.size(); i++) {
      VarProcess vp = (VarProcess) varList.get(i);
      if (vp.isCoordinateAxis || vp.isCoordinateVariable) {
        if (vp.axisType == null)
          vp.axisType = getAxisType( ncDataset, (VariableEnhanced) vp.v);
        if (vp.axisType == null)
          userAdvice.append("Coordinate Axis "+vp.v.getName()+" does not have an assigned AxisType\n");
        vp.makeIntoCoordinateAxis();
      }
    }
  }

   /**
   * Take all previously identified Coordinate Systems and create a
   * CoordinateSystem object.
   */
  protected void makeCoordinateSystems( NetcdfDataset ncDataset) {
    for (int i = 0; i < varList.size(); i++) {
      VarProcess vp = (VarProcess) varList.get(i);
      if (vp.isCoordinateSystem) {
        vp.makeCoordinateSystem();
      }
    }
  }

  /**
   * Assign explicit CoordinateSystem objects to variables.
   */
  protected void assignExplicitCoordinateSystems( NetcdfDataset ncDataset) {
    for (int i = 0; i < varList.size(); i++) {
      VarProcess vp = (VarProcess) varList.get(i);
      if (vp.coordSys != null && !vp.isCoordinateTransform) {
        StringTokenizer stoker = new StringTokenizer( vp.coordSys);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess( vname); // LOOK: full vs short name
          if (ap == null) {
            parseInfo.append("***Cant find Coordinate System variable "+vname+" referenced from var= "+vp.v.getName()+"\n");
            userAdvice.append("***Cant find Coordinate System variable "+vname+" referenced from var= "+vp.v.getName()+"\n");
            continue;
          }
          if (ap.cs == null) {
            parseInfo.append("***Not a Coordinate System variable ="+vname+" referenced from var= "+vp.v.getName()+"\n");
            userAdvice.append("***Not a Coordinate System variable ="+vname+" referenced from var= "+vp.v.getName()+"\n");
            continue;
          }

          VariableEnhanced ve = (VariableEnhanced) vp.v;
          ve.addCoordinateSystem( ap.cs);
        }
      }
    }
  }

  /**
   * Make implicit CoordinateSystem objects for variables that dont already have one, by using the
   * variables' list of coordinate axes, and any coordinateVariables for it. Must be at least 2 axes.
   * @param ncDataset
   */
  protected void makeCoordinateSystemsImplicit(NetcdfDataset ncDataset) {
    for (int i = 0; i < varList.size(); i++) {
      VarProcess vp = (VarProcess) varList.get(i);

      if (vp.coordSys == null && vp.isData()) {
        List dataAxesList = vp.findCoordinateAxes( true);
        if (dataAxesList.size() < 2)
          continue;

        VariableEnhanced ve = (VariableEnhanced) vp.v;
        String csName = CoordinateSystem.makeName(dataAxesList);
        CoordinateSystem cs = ncDataset.findCoordinateSystem(csName);
        if (cs != null) {
          ve.addCoordinateSystem(cs);
          parseInfo.append(" assigned implicit coord System " + cs.getName() + " for var= " + vp.v.getName() + "\n");
        } else {
          CoordinateSystem csnew = new CoordinateSystem(ncDataset, dataAxesList, null);
          csnew.setImplicit( true);

          ve.addCoordinateSystem(csnew);
          ncDataset.addCoordinateSystem(csnew);
          parseInfo.append(" created implicit coord System " + csnew.getName() + " for var= " + vp.v.getName() + "\n");
        }
      }
    }
  }

  /**
   * If a variable still doesnt have a coordinate system, use hueristics to try to find one that was probably
   * forgotten.
   * Look through all existing CS. create a subset of axes that fits the variable. Choose the one with highest rank.
   * It must have X,Y or lat,lon. If so, add it.
   * @param ncDataset
   */
  protected void makeCoordinateSystemsMaximal(NetcdfDataset ncDataset) {
    for (int i = 0; i < varList.size(); i++) {
      VarProcess vp = (VarProcess) varList.get(i);
      VariableEnhanced ve = (VariableEnhanced) vp.v;
      CoordinateSystem implicit = null;

      if ((ve.getCoordinateSystems().size() > 1) || !vp.isData()) continue;

      CoordinateSystem existing = null;
      if (ve.getCoordinateSystems().size() == 1) {
        existing = (CoordinateSystem) ve.getCoordinateSystems().get(0);
        if (!existing.isImplicit()) continue; // cant overrrride explicit
        if (existing.getRankRange() >= ve.getRank()) continue; // looks ok
        implicit = existing;
      }

      // look through all axes that fit
      ArrayList axisList = new ArrayList();
      List axes = ncDataset.getCoordinateAxes();
      for (int j = 0; j < axes.size(); j++) {
        CoordinateAxis axis = (CoordinateAxis) axes.get(j);
        if ( isCoordinateAxisForVariable( axis, ve))
          axisList.add( axis);
      }

      if ((existing != null) && (axisList.size() <= existing.getRankRange())) continue;
      if (axisList.size() < 2) continue;

      /* ArrayList bestAxisList = null;
      List csys = ncDataset.getCoordinateSystems();
      for (int j = 0; j < csys.size(); j++) {
        CoordinateSystem cs = (CoordinateSystem) csys.get(j);
        List axes = cs.getCoordinateAxes();
        ArrayList axisList = new ArrayList();
        for (int k = 0; k < axes.size(); k++) {
          CoordinateAxis axis = (CoordinateAxis) axes.get(k);
          if ( isCoordinateAxisForVariable( axis, ve))
            axisList.add( axis);
        }

        if (hasXY( axisList)) {
          if ((bestAxisList == null) || (axisList.size() > bestAxisList.size()))
            bestAxisList = axisList;
        }

      }
      // make or get a coord sys for it
      if (bestAxisList != null) { */

        String csName = CoordinateSystem.makeName(axisList);
        CoordinateSystem cs = ncDataset.findCoordinateSystem(csName);
        if (cs != null) {
          if (null != implicit) ve.removeCoordinateSystem(implicit);
          ve.addCoordinateSystem(cs);
          parseInfo.append(" assigned maximal coord System " + cs.getName() + " for var= " + ve.getName() + "\n");
        } else {
          CoordinateSystem csnew = new CoordinateSystem(ncDataset, axisList, null);
          csnew.setImplicit( true);
          if (null != implicit) ve.removeCoordinateSystem(implicit);
          ve.addCoordinateSystem(csnew);
          ncDataset.addCoordinateSystem(csnew);
          parseInfo.append(" created maximal coord System " + csnew.getName() + " for var= " + ve.getName() + "\n");
        }

    }
  }

  /**
   * Does this axis "fit" this variable.
   * True if all of the dimensions in the axis also appear in the variable.
   */
  protected boolean isCoordinateAxisForVariable(Variable axis, VariableEnhanced v) {
    List varDims = v.getDimensionsAll();
    for (int i=0; i<axis.getRank(); i++) {
      Dimension axisDim = axis.getDimension(i);
      if (!varDims.contains( axisDim)) {
        return false;
      }
    }
    return true;
  }

   protected boolean hasXY(List coordAxes) {
     boolean hasX = false, hasY = false, hasLat = false, hasLon = false;
     for (int i=0; i<coordAxes.size(); i++) {
       CoordinateAxis axis = (CoordinateAxis) coordAxes.get(i);
       AxisType axisType = axis.getAxisType();
       if (axisType == AxisType.GeoX) hasX = true;
       if (axisType == AxisType.GeoY) hasY = true;
       if (axisType == AxisType.Lat) hasLat = true;
       if (axisType == AxisType.Lon) hasLon = true;
     }
     return (hasLat && hasLon) || (hasX && hasY);
   }


  //protected void assignImplicitCoordinateTransforms( NetcdfDataset ncDataset, CoordinateSystem csnew) {
  //}

  /**
    * Take all previously identified Coordinate Transforms and create a
    * CoordinateTransform object by calling CoordTransBuilder.makeCoordinateTransform().
    */
   protected void makeCoordinateTransforms( NetcdfDataset ncDataset) {
     for (int i = 0; i < varList.size(); i++) {
       VarProcess vp = (VarProcess) varList.get(i);
       if (vp.isCoordinateTransform && vp.ct == null) {
         vp.ct = CoordTransBuilder.makeCoordinateTransform( vp.ds, vp.v, parseInfo, userAdvice);
       }
     }
   }

  protected CoordinateTransform makeCoordinateTransform (NetcdfDataset ds, Variable ctv) {
    return CoordTransBuilder.makeCoordinateTransform( ds, ctv, parseInfo, userAdvice);
  }

   /**
    * Assign CoordinateTransform objects to Coordinate Systems.
    */
   protected void assignCoordinateTransforms( NetcdfDataset ncDataset) {
     // look for explicit transform assignments on the coordinate systems
     for (int i = 0; i < varList.size(); i++) {
       VarProcess vp = (VarProcess) varList.get(i);
       if (vp.isCoordinateSystem && vp.coordTransforms != null) {
         StringTokenizer stoker = new StringTokenizer( vp.coordTransforms);
         while (stoker.hasMoreTokens()) {
           String vname = stoker.nextToken();
           VarProcess ap = findVarProcess( vname); // LOOK: full vs short name
           if (ap != null) {
             if (ap.ct != null) {
               vp.cs.addCoordinateTransform( ap.ct);
               parseInfo.append(" assign explicit coordTransform "+ap.ct+" to CoordSys= "+vp.cs+"\n");
             } else {
               parseInfo.append("***Cant find coordTransform in "+vname+" referenced from var= "+vp.v.getName()+"\n");
               userAdvice.append("***Cant find coordTransform in "+vname+" referenced from var= "+vp.v.getName()+"\n");
             }
           } else {
             parseInfo.append("***Cant find coordTransform variable="+vname+" referenced from var= "+vp.v.getName()+"\n");
             userAdvice.append("***Cant find coordTransform variable="+vname+" referenced from var= "+vp.v.getName()+"\n");
           }
         }
       }
     }

     // look for explicit coordSys assignments on the coordinate transforms
     for (int i = 0; i < varList.size(); i++) {
       VarProcess vp = (VarProcess) varList.get(i);
       if (vp.isCoordinateTransform && (vp.ct != null) && (vp.coordSys != null)) {
         StringTokenizer stoker = new StringTokenizer( vp.coordSys);
         while (stoker.hasMoreTokens()) {
           String vname = stoker.nextToken();
           VarProcess vcs = findVarProcess( vname); // LOOK: full vs short name
           if (vcs == null) {
             parseInfo.append("***Cant find coordSystem variable "+vname+" referenced from var= "+vp.v.getName()+"\n");
             userAdvice.append("***Cant find coordSystem variable "+vname+" referenced from var= "+vp.v.getName()+"\n");
           }
           else {
             vcs.cs.addCoordinateTransform( vp.ct);
             parseInfo.append(" assign explicit coordTransform "+vp.ct+" to CoordSys= "+vp.cs+"\n");
           }
         }
       }
     }

     // look for coordAxes assignments on the coordinate transforms
     for (int i = 0; i < varList.size(); i++) {
       VarProcess vp = (VarProcess) varList.get(i);
       if (vp.isCoordinateTransform && (vp.ct != null) && (vp.coordAxes != null)) {
         List dataAxesList = vp.findCoordinateAxes( false);

         if (dataAxesList.size() > 0) {
           List csList = ncDataset.getCoordinateSystems();
           for (int j = 0; j < csList.size(); j++) {
             CoordinateSystem cs = (CoordinateSystem) csList.get(j);
             if (cs.containsAxes( dataAxesList)) {
               cs.addCoordinateTransform(vp.ct);
               parseInfo.append(" assign (implicit coordAxes) coordTransform "+vp.ct+" to CoordSys= "+cs+"\n");
             }
           }
         }
       }
     }

     // look for coordAxisType assignments on the coordinate transforms
     for (int i = 0; i < varList.size(); i++) {
       VarProcess vp = (VarProcess) varList.get(i);
       if (vp.isCoordinateTransform && (vp.ct != null) && (vp.coordAxisTypes != null)) {
         List axisTypesList = new ArrayList();
         StringTokenizer stoker = new StringTokenizer(vp.coordAxisTypes);
         while (stoker.hasMoreTokens()) {
           String name = stoker.nextToken();
           AxisType atype;
           if (null != (atype = AxisType.getType(name)))
             axisTypesList.add(atype);
         }
         if (axisTypesList.size() > 0) {
           List csList = ncDataset.getCoordinateSystems();
           for (int j = 0; j < csList.size(); j++) {
             CoordinateSystem cs = (CoordinateSystem) csList.get(j);
             if (cs.containsAxisTypes(axisTypesList)) {
               cs.addCoordinateTransform(vp.ct);
               parseInfo.append(" assign (implicit coordAxisType) coordTransform "+vp.ct+" to CoordSys= "+cs+"\n");
             }
           }
         }
       }
     }

   }

  protected VarProcess findVarProcess(String name) {
    if (name == null) return null;

    for (int i=0; i<varList.size(); i++) {
      VarProcess vp = (VarProcess) varList.get(i);
      if (name.equals(vp.v.getName()))
        return vp;
    }
    return null;
  }

  protected VarProcess findCoordinateAxis(String name) {
    if (name == null) return null;

    for (int i=0; i<varList.size(); i++) {
      VarProcess vp = (VarProcess) varList.get(i);
      if (name.equals(vp.v.getName()) && (vp.isCoordinateVariable || vp.isCoordinateAxis))
        return vp;
    }
    return null;
  }

  /**
   * Wrap each variable in the dataset with a VarProcess object.
   */
  public class VarProcess {
    public NetcdfDataset ds;
    public Variable v;

    // data variable
    // public ArrayList dataAxesList;

    // coord axes
    public boolean isCoordinateVariable;
    public boolean isCoordinateAxis;
    public AxisType axisType;
    public String coordAxes, coordSys, coordVarAlias, positive, coordAxisTypes;

    // coord systems
    public boolean isCoordinateSystem;
    public ArrayList axesList;
    public String coordTransforms;
    public CoordinateSystem cs;

    // coord transform
    public boolean isCoordinateTransform;
    public String coordTransformType;
    public CoordinateTransform ct;

    /**
     * Wrap the given variable.
     * Identify Coordinate Variables.
     * Process all _Coordinate attributes.
     */
    private VarProcess( NetcdfDataset ds, VariableEnhanced ve) {
      this.ds = ds;
      this.v = (Variable) ve;
      isCoordinateVariable = v.getCoordinateDimension() != null;
      if (isCoordinateVariable)
        parseInfo.append(" Coordinate Variable added = "+v.getName()+"\n");

      Attribute att = v.findAttributeIgnoreCase("_CoordinateAxisType");
      if (att != null) {
        String axisName = att.getStringValue();
        axisType = AxisType.getType( axisName);
        isCoordinateAxis = true;
        parseInfo.append(" Coordinate Axis added = "+v.getName()+" type= "+axisName+"\n");
      }

      coordVarAlias = ds.findAttValueIgnoreCase(v, "_CoordinateVariableAlias", null);
      if (coordVarAlias != null) {
        coordVarAlias = coordVarAlias.trim();
        if (v.getRank() != 1) {
          parseInfo.append("**ERROR Coordinate Variable Alias "+v.getName()+" has rank "+v.getRank()+"\n");
          userAdvice.append("**ERROR Coordinate Variable Alias "+v.getName()+" has rank "+v.getRank()+"\n");
        } else {
          Dimension coordDim = ds.findDimension( coordVarAlias);
          Dimension vDim = v.getDimension( 0);
          if (!coordDim.equals(vDim)) {
            parseInfo.append("**ERROR Coordinate Variable Alias "+v.getName()+" names wrong dimension "+coordVarAlias+"\n");
            userAdvice.append("**ERROR Coordinate Variable Alias "+v.getName()+" names wrong dimension "+coordVarAlias+"\n");
          } else {
            isCoordinateAxis = true;
            v.setIsCoordinateAxis( vDim);
            parseInfo.append(" found Coordinate Variable Alias "+v.getName()+" for dimension "+coordVarAlias+"\n");
          }
        }
      }

      positive = ds.findAttValueIgnoreCase(v, "_CoordinateZIsPositive", null);
      if (positive == null)
        positive = ds.findAttValueIgnoreCase(v, "positive", null);
      else {
        isCoordinateAxis = true;
        positive = positive.trim();
      }

      coordAxes = ds.findAttValueIgnoreCase(v, "_CoordinateAxes", null);
      coordSys = ds.findAttValueIgnoreCase(v, "_CoordinateSystems", null);
      coordTransforms = ds.findAttValueIgnoreCase(v, "_CoordinateTransforms", null);
      isCoordinateSystem = (coordTransforms != null);

      coordAxisTypes = ds.findAttValueIgnoreCase(v, "_CoordinateAxisTypes", null);
      coordTransformType = ds.findAttValueIgnoreCase(v, "_CoordinateTransformType", null);
      isCoordinateTransform = (coordTransformType != null) || (coordAxisTypes != null);

      // this is the case of a Coordinate System with no references or coordinate transforms
      // see /testdata/grid/grib/grib1/data/NOGAPS-Temp-Regional.grib
      if (!isCoordinateSystem && !isCoordinateTransform && !isCoordinateAxis && coordAxes != null) {
        // figure out if data or coordSys Variable
        StringTokenizer stoker = new StringTokenizer( coordAxes);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          Variable axis = ds.findVariable( vname);
          if ((axis != null) && !isCoordinateAxisForVariable(axis, ve))
              isCoordinateSystem = true;
        }
      }


    }

    // fakeroo
    public VarProcess( NetcdfDataset ds) {
      this.ds = ds;
    }

    public boolean isData() {
      return !isCoordinateVariable && !isCoordinateAxis && !isCoordinateSystem && !isCoordinateTransform;
    }

    /**
     * Turn the variable into a coordinate axis.
     */
    public void makeIntoCoordinateAxis() {
      CoordinateAxis ca;
      if (v instanceof CoordinateAxis) {
        ca = (CoordinateAxis) v;
      } else {
        ca = CoordinateAxis.factory(ds, (VariableDS) v); // LOOK StructureDS cant be coord axis, though a member can
        ds.addCoordinateAxis( ca);
        v = ca;
      }

      Dimension dim = ca.getCoordinateDimension();
      if (dim != null)
        dim.addCoordinateVariable(  ca); // remove old one

      if (axisType != null) {
        ca.setAxisType( axisType);
        ca.addAttribute( new Attribute("_CoordinateAxisType", axisType.toString()));

        if (((axisType == AxisType.Height) || (axisType == AxisType.Pressure) || (axisType == AxisType.GeoZ)) &&
            (positive != null)) {
          ca.setPositive( positive);
          ca.addAttribute( new Attribute("_CoordinateZisPositive", positive));
        }
      }

    }

    /**
     * Create a Coordinate System object, using list of coordinate axis names in the
     * (required) axes field. */
    public void makeCoordinateSystem () {

      // find referenced coordinate axes
      axesList = new ArrayList();
      StringTokenizer stoker = new StringTokenizer( coordAxes);
      while (stoker.hasMoreTokens()) {
        String vname = stoker.nextToken();
        VarProcess ap = findVarProcess( vname); // LOOK: full vs short name
        if (ap != null)
          axesList.add( ap.v);
        else {
          parseInfo.append(" Cant find axes "+vname+ " for Coordinate System "+v.getName()+"\n");
          userAdvice.append("Cant find axes "+vname+ " for Coordinate System "+v.getName()+"\n");
        }
      }

      if (axesList.size() == 0) {
        parseInfo.append(" No axes found for Coordinate System "+v.getName()+"\n");
        userAdvice.append("No axes found for Coordinate System "+v.getName()+"\n");
        return;
      }

      // create the coordinate system
      cs = new CoordinateSystem(ds, axesList, null);
      ds.addCoordinateSystem( cs);

      parseInfo.append(" Made Coordinate System "+cs.getName()+"\n");
    }

    /**
     * Create a list of coordinate axes for this data variable.
     * Use the list of names in axes field.
     * Add any coordinate variables that are missing.
     */
    public ArrayList findCoordinateAxes(boolean addCoordVariables) {
      ArrayList axesList = new ArrayList();

      if (coordAxes != null) { // explicit axes
        StringTokenizer stoker = new StringTokenizer( coordAxes);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess( vname); // LOOK: full vs short name
          if (ap != null)
            axesList.add( ap.v);
        }
      }

      if (addCoordVariables) {
        List dims = v.getDimensions();
        for (int i = 0; i < dims.size(); i++) {
          Dimension d = (Dimension) dims.get(i);
          List coordVars = d.getCoordinateVariables();
          for (int j = 0; j < coordVars.size(); j++) {
            Variable cv =  (Variable) coordVars.get(j);
            if (!axesList.contains(cv))
              axesList.add( cv);
          }
        }
      }

      return axesList;
    }

  }

  protected VariableDS makeCoordinateTransformVariable(NetcdfDataset ds, CoordinateTransform ct) {
    VariableDS v = CoordTransBuilder.makeDummyTransformVariable( ds, ct);
    parseInfo.append("  made CoordinateTransformVariable:"+ct.getName()+"\n");
    return v;
  }

  /**
   * @deprecated  use CoordTransBuilder.makeDummyTransformVariable
   */
  static public VariableDS makeDummyTransformVariable(NetcdfDataset ds, CoordinateTransform ct) {
    return CoordTransBuilder.makeDummyTransformVariable(ds, ct);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////
  // Coordinate Transformations.

  /*
   * Create a CoordinateTransform if possible out of the information in this Variable.
   * Generally, this just passes the attrinutes of the variables as parameters of the CoordinateTransform.
   * Specifically, this routine currently handles
   * <ul>
   *   <li>CF lambert_conformal_conic
   * </ul>
   * @param ds for this dataset
   * @param ctv for this variable
   * @return CoordinateTransform
   *
  protected CoordinateTransform makeCoordinateTransform (NetcdfDataset ds, Variable ctv) {
    CoordinateTransform ct = null;

    // standard name
    String transform_name = ds.findAttValueIgnoreCase(ctv, "transform_name", null);
    if (null == transform_name)
      transform_name = ds.findAttValueIgnoreCase(ctv, "Projection_Name", null);

    // these names are from CF - dont want to have to duplicate
    if (null == transform_name)
      transform_name = ds.findAttValueIgnoreCase(ctv, "grid_mapping_name", null);
    if (null == transform_name)
      transform_name = ds.findAttValueIgnoreCase(ctv, "standard_name", null);

    if (null != transform_name) {
      transform_name = transform_name.trim();

      // check projections
      if (transform_name.equalsIgnoreCase("albers_conical_equal_area"))
        ct = makeProjectionAlbers(ds, ctv);
      else if (transform_name.equalsIgnoreCase("lambert_azimuthal_equal_area"))
        ct = makeProjectionLambertAzimuthal(ds, ctv);
      else if (transform_name.equalsIgnoreCase("lambert_conformal_conic"))
        ct = makeProjectionLC(ds, ctv);
      else if (transform_name.equalsIgnoreCase("mercator"))
        ct = makeProjectionMercator(ds, ctv);
      else if (transform_name.equalsIgnoreCase("orthographic"))
        ct = makeProjectionOrthographic(ds, ctv);
      else if (transform_name.equalsIgnoreCase("polar_stereographic"))
        ct = makeProjectionStereographic(ds, ctv, true);
      else if (transform_name.equalsIgnoreCase("stereographic"))
        ct = makeProjectionStereographic(ds, ctv, false);
      else if (transform_name.equalsIgnoreCase("transverse_mercator"))
        ct = makeProjectionTransverseMercator(ds, ctv);
      else if (transform_name.equalsIgnoreCase("UTM"))
        ct = makeProjectionUTM(ds, ctv);
      else
        // otherwise it should be a vertical transformation
        ct = makeVerticalCoordinateTransform(ds, ctv, transform_name);

      if (ct == null) {
        parseInfo.append("**Failed to make Projection transform "+transform_name+" from "+ctv+": missing parameters\n");
        userAdvice.append("**Failed to make Projection transform "+transform_name+" from "+ctv+": missing parameters\n");
      } else {
        parseInfo.append(" Made Projection transform "+transform_name+" from "+ctv.getName()+"\n");
        return ct;
      }

      return ct;
    }

    // otherwise
    ct = new CoordinateTransform(ctv.getName(), conventionName, null);
    List atts = ctv.getAttributes();
    for (int i = 0; i < atts.size(); i++) {
      Attribute att = (Attribute) atts.get(i);
      ct.addParameter( new Parameter(att.getName(), att.getStringValue())); // LOOK what about numeric
    }
    parseInfo.append(" Made unknown transform from: "+ctv+"\n");
    return ct;
  }

  protected CoordinateTransform makeProjectionAlbers(NetcdfDataset ds, Variable ctv) {
     double[] pars = readAttributeDouble2(ctv.findAttribute( "standard_parallel"));
     if (pars == null) return null;

     double lon0 = readAttributeDouble( ctv, "longitude_of_central_meridian");
     double lat0 = readAttributeDouble( ctv, "latitude_of_projection_origin");
     double false_easting = readAttributeDouble( ctv, "false_easting");
     double false_northing = readAttributeDouble( ctv, "false_northing");

     AlbersEqualArea proj = new AlbersEqualArea(lat0, lon0, pars[0], pars[1]);
     CoordinateTransform rs = new ProjectionCT(ctv.getShortName(), "FGDC", proj);

     parseInfo.append(" Made Albers projection\n");
     return rs;
   }

  protected CoordinateTransform makeProjectionLambertAzimuthal(NetcdfDataset ds, Variable ctv) {
     double[] pars = readAttributeDouble2(ctv.findAttribute( "standard_parallel"));
     if (pars == null) return null;

     double lon0 = readAttributeDouble( ctv, "longitude_of_projection_origin");
     double lat0 = readAttributeDouble( ctv, "latitude_of_projection_origin");
     double false_easting = readAttributeDouble( ctv, "false_easting");
     double false_northing = readAttributeDouble( ctv, "false_northing");
     String units = ds.findAttValueIgnoreCase( ctv, "units", null);

     LambertAzimuthalEqualArea proj = new LambertAzimuthalEqualArea(lat0, lon0, false_easting, false_northing, units,
             ProjectionImpl.EARTH_RADIUS);
     CoordinateTransform rs = new ProjectionCT(ctv.getShortName(), "FGDC", proj);

     parseInfo.append(" Made Lambert Azimuthal projection\n");
     return rs;
   }

  protected CoordinateTransform makeProjectionLC(NetcdfDataset ds, Variable ctv) {
    double[] pars = readAttributeDouble2(ctv.findAttribute( "standard_parallel"));
    if (pars == null) return null;

    double lon0 = readAttributeDouble( ctv, "longitude_of_central_meridian");
    double lat0 = readAttributeDouble( ctv, "latitude_of_projection_origin");
    double false_easting = readAttributeDouble( ctv, "false_easting");
    double false_northing = readAttributeDouble( ctv, "false_northing");
    String units = ds.findAttValueIgnoreCase( ctv, "units", null);

    LambertConformal lc = new LambertConformal(lat0, lon0, pars[0], pars[1], false_easting, false_northing, units);
    CoordinateTransform rs = new ProjectionCT(ctv.getShortName(), "FGDC", lc);

    parseInfo.append(" Made Lambert Conformal projection\n");
    return rs;
  }

  protected CoordinateTransform makeProjectionMercator(NetcdfDataset ds, Variable ctv) {

    double par = readAttributeDouble( ctv, "standard_parallel");
    double lon0 = readAttributeDouble( ctv, "longitude_of_projection_origin");
    double lat0 = readAttributeDouble( ctv, "latitude_of_projection_origin");

    Mercator proj = new Mercator(lat0, lon0, par);
    CoordinateTransform rs = new ProjectionCT(ctv.getShortName(), "FGDC", proj);

    parseInfo.append(" Made Mercator projection\n");
    return rs;
  }

  protected CoordinateTransform makeProjectionOrthographic(NetcdfDataset ds, Variable ctv) {

    double lon0 = readAttributeDouble( ctv, "longitude_of_projection_origin");
    double lat0 = readAttributeDouble( ctv, "latitude_of_projection_origin");

    Orthographic proj = new Orthographic(lat0, lon0);
    CoordinateTransform rs = new ProjectionCT(ctv.getShortName(), "FGDC", proj);

    parseInfo.append(" Made Orthographic projection\n");
    return rs;
  }

  protected CoordinateTransform makeProjectionStereographic(NetcdfDataset ds, Variable ctv, boolean polar) {
    double scale = readAttributeDouble( ctv, "scale_factor_at_projection_origin");
    double lon0 = readAttributeDouble( ctv, "longitude_of_projection_origin");

    double lat0 = 90.0;
    if (!polar) {
      lat0 = readAttributeDouble( ctv, "latitude_of_projection_origin");
    }

    Stereographic proj = new Stereographic( lat0, lon0, scale);
    CoordinateTransform rs = new ProjectionCT(ctv.getShortName(), "FGDC", proj);

    parseInfo.append(" Made Stereographic projection\n");
    return rs;
  }

  protected CoordinateTransform makeProjectionTransverseMercator(NetcdfDataset ds, Variable ctv) {

    double scale = readAttributeDouble( ctv, "scale_factor_at_central_meridian");
    double lon0 = readAttributeDouble( ctv, "longitude_of_central_meridian");
    double lat0 = readAttributeDouble( ctv, "latitude_of_projection_origin");
    double east = readAttributeDouble( ctv, "false_easting");  // LOOK deal with false_easting, could modify coordinates ??
    double north = readAttributeDouble( ctv, "lase_northing");

    TransverseMercator proj = new TransverseMercator(lat0, lon0, scale);
    CoordinateTransform rs = new ProjectionCT(ctv.getShortName(), "FGDC", proj);

    parseInfo.append(" Made Transverse Mercator projection\n");
    return rs;
  }

  protected CoordinateTransform makeProjectionUTM(NetcdfDataset ds, Variable ctv) {
    int zone = (int) readAttributeDouble( ctv, "utm_zone_number");
    boolean isNorth = zone > 0;
    zone = Math.abs(zone);

    Attribute a;
    double axis = 0.0, f = 0.0;
    if (null != (a = ctv.findAttribute( "semimajor_axis")))
      axis = a.getNumericValue().doubleValue();
    if (null != (a = ctv.findAttribute( "inverse_flattening ")))
      f = a.getNumericValue().doubleValue();

    // double a, double f, int zone, boolean isNorth
    UtmProjection proj = (axis != 0.0) ? new UtmProjection(axis, f, zone, isNorth) : new UtmProjection(zone, isNorth);
    CoordinateTransform rs = new ProjectionCT(ctv.getShortName(), "FGDC", proj);

    parseInfo.append(" Made UTM projection\n");
    return rs;
  } */

  //////////////////////////////////////////////////////////////////////////////////

/*  protected CoordinateTransform makeVerticalCoordinateTransform(NetcdfDataset ds, Variable v,
      String transform_name) {

    if (transform_name.equalsIgnoreCase("existing3DField")) {
      VerticalCT ct = new VerticalCT (v.getName(), conventionName, VerticalCT.Type.Existing3DField);
      ct.addParameter(new Parameter(VTfromExistingData.existingDataField, v.getName()));
      return ct;
    }

    // these are the CF vertical transforms
    String formula = ds.findAttValueIgnoreCase(v, "formula_terms", null);
    if (null == formula) return null;

    CoordinateTransform ct = null;

    if (transform_name.equalsIgnoreCase("atmosphere_sigma_coordinate")) {
      try {
        SigmaFormula f = new SigmaFormula(formula);
        ct = f.makeCT( v.getName(), ds);
      } catch (IOException ioe) {
        parseInfo.append("  failed to make Sigma CoordinateTransform for "+v.getName()+"\n");
        userAdvice.append("failed to make Sigma CoordinateTransform for "+v.getName()+"\n");
        return null;
      }
    }

    if (transform_name.equalsIgnoreCase("atmosphere_hybrid_sigma_pressure_coordinate")) {
      HybridSigmaPressureFormula f = new HybridSigmaPressureFormula(formula);
      try {
        ct = f.makeCT( v.getName(), ds);
      } catch (IOException ioe) {
        parseInfo.append("  failed to make HybridSigmaPressure CoordinateTransform for "+v.getName()+"\n");
        userAdvice.append("failed to make HybridSigmaPressure CoordinateTransform for "+v.getName()+"\n");
        return null;
      }
    }

   if (transform_name.equalsIgnoreCase("ocean_s_coordinate")) {
      try {
        OceanSFormula f = new OceanSFormula(formula);
        ct = f.makeCT( v.getName(), ds);
      } catch (IOException ioe) {
        parseInfo.append("  failed to make OceanS CoordinateTransform for "+v.getName()+"\n");
        userAdvice.append("failed to make OceanS CoordinateTransform for "+v.getName()+"\n");
        return null;
      }
    }

   if (transform_name.equalsIgnoreCase("ocean_sigma_coordinate")) {
      try {
        OceanSigmaFormula f = new OceanSigmaFormula(formula);
        ct = f.makeCT( v.getName(), ds);
      } catch (IOException ioe) {
        parseInfo.append("  failed to make OceanSigma CoordinateTransform for "+v.getName()+"\n");
        userAdvice.append("failed to make OceanSigma CoordinateTransform for "+v.getName()+"\n");
        return null;
      }
    }
    
    if (ct == null) {
      parseInfo.append("  **Cant find "+transform_name+" CoordinateTransform for "+v.getName()+"\n");
      userAdvice.append("**Cant find "+transform_name+" CoordinateTransform for "+v.getName()+"\n");
    }

    showFormula( ds, formula);
    return ct;
  }

  private void showFormula( NetcdfDataset ds, String formula) {
    StringTokenizer stoke = new StringTokenizer( formula);

    while (stoke.hasMoreTokens()) {
      String name = stoke.nextToken();
      String vname = stoke.nextToken();
      Variable v = ds.findVariable( vname);

      if (v == null) {
        parseInfo.append(" ***ERROR Cant find   "+vname+" in formula"+formula+"\n");
        userAdvice.append("***ERROR Cant find   "+vname+" in formula"+formula+"\n");
        return;
      }

      parseInfo.append("    "+name+" = ");
      v.getNameAndDimensions(parseInfo, false, false);
      parseInfo.append("\n");
    }
  }

  private class SigmaFormula {
    String sigma="", ps="", ptop="";

    SigmaFormula( String formula) {
       // parse the formula string
      StringTokenizer stoke = new StringTokenizer(formula);
      while (stoke.hasMoreTokens()) {
        String toke = stoke.nextToken();
        if (toke.equalsIgnoreCase("sigma:"))
          sigma = stoke.nextToken();
        else if (toke.equalsIgnoreCase("ps:"))
          ps = stoke.nextToken();
        else if (toke.equalsIgnoreCase("ptop:"))
          ptop = stoke.nextToken();
      }
    }

    CoordinateTransform makeCT( String varName, NetcdfFile ds) throws IOException {
      CoordinateTransform rs = new VerticalCT(varName, conventionName, VerticalCT.Type.Sigma);
      rs.addParameter(new Parameter("formula", "pressure(x,y,z) = ptop + sigma(z)*(surfacePressure(x,y)-ptop)"));

      if (!addParameter( rs, AtmosSigma.PS, ds, ps, false)) return null;
      if (!addParameter( rs, AtmosSigma.SIGMA, ds, sigma, false)) return null;
      if (!addParameter( rs, AtmosSigma.PTOP, ds, ptop, true)) return null;
      return rs;
    }

    public String toString() { return "Sigma "+sigma + ps + ptop; }
  }

 private class HybridSigmaPressureFormula {
    String a="", b="", ps="", p0="";

    HybridSigmaPressureFormula( String formula) {
       // parse the formula string
      StringTokenizer stoke = new StringTokenizer(formula);
      while (stoke.hasMoreTokens()) {
        String toke = stoke.nextToken();
        if (toke.equalsIgnoreCase("a:"))
          a = stoke.nextToken();
        else if (toke.equalsIgnoreCase("b:"))
          b = stoke.nextToken();
        else if (toke.equalsIgnoreCase("ps:"))
          ps = stoke.nextToken();
        else if (toke.equalsIgnoreCase("p0:"))
          p0 = stoke.nextToken();
      }
    }

    CoordinateTransform makeCT( String varName, NetcdfFile ds)  throws IOException {
      CoordinateTransform rs = new VerticalCT(varName, conventionName, VerticalCT.Type.HybridSigmaPressure);
      rs.addParameter(new Parameter("formula", "pressure(x,y,z) = a(z)*p0 + b(z)*surfacePressure(x,y)"));

      if (!addParameter( rs, HybridSigmaPressure.PS, ds, ps, false)) return null;
      if (!addParameter( rs, HybridSigmaPressure.A, ds, a, false)) return null;
      if (!addParameter( rs, HybridSigmaPressure.B, ds, b, false)) return null;
      if (!addParameter( rs, HybridSigmaPressure.P0, ds, p0, true)) return null;
      return rs;
    }

    public String toString() { return "HybridSigma "+a + b + ps + p0; }
  }

  // :formula_terms = "s: s_rho eta: zeta depth: h a: theta_s b: theta_b depth_c: hc";
  private class OceanSFormula {
    String s="", eta="", depth="", a="", b="", depth_c="";

    OceanSFormula( String formula) {
       // parse the formula string
      StringTokenizer stoke = new StringTokenizer(formula);
      while (stoke.hasMoreTokens()) {
        String toke = stoke.nextToken();
        if (toke.equalsIgnoreCase("s:"))
          s = stoke.nextToken();
        else if (toke.equalsIgnoreCase("eta:"))
          eta = stoke.nextToken();
        else if (toke.equalsIgnoreCase("depth:"))
          depth = stoke.nextToken();
        else if (toke.equalsIgnoreCase("a:"))
          a = stoke.nextToken();
        else if (toke.equalsIgnoreCase("b:"))
          b = stoke.nextToken();
        else if (toke.equalsIgnoreCase("depth_c:"))
          depth_c = stoke.nextToken();
      }
    }

    CoordinateTransform makeCT( String varName, NetcdfFile ds)  throws IOException {

      CoordinateTransform rs = new VerticalCT("oceanS-"+varName, conventionName, VerticalCT.Type.OceanS);
      rs.addParameter((new Parameter("height formula", "height(x,y,z) = eta(x,y)*(1+s(z)) + depth_c*s(z) + (depth(x,y)-depth_c)*C(z)")));
      rs.addParameter((new Parameter("C formula", "C(z) = (1-b)*sinh(a*s(z))/sinh(a) + b*(tanh(a*(s(z)+0.5))/(2*tanh(0.5*a))-0.5)")));

      if (!addParameter( rs, OceanS.ETA, ds, eta, false)) return null;
      if (!addParameter( rs, OceanS.S, ds, s, false)) return null;
      if (!addParameter( rs, OceanS.DEPTH, ds, depth, false)) return null;

      if (!addParameter( rs, OceanS.DEPTH_C, ds, depth_c, true)) return null;
      if (!addParameter( rs, OceanS.A, ds, a, true)) return null;
      if (!addParameter( rs, OceanS.B, ds, b, true)) return null;

      return rs;
    }


    public String toString() { return "OceanS "+s + eta + depth + a + b + depth_c; }
  }

  private class OceanSigmaFormula {
    String s="", eta="", depth="";

    OceanSigmaFormula( String formula) {
       // parse the formula string
      StringTokenizer stoke = new StringTokenizer(formula);
      while (stoke.hasMoreTokens()) {
        String toke = stoke.nextToken();
        if (toke.equalsIgnoreCase("sigma:"))
          s = stoke.nextToken();
        else if (toke.equalsIgnoreCase("eta:"))
          eta = stoke.nextToken();
        else if (toke.equalsIgnoreCase("depth:"))
          depth = stoke.nextToken();
      }
    }

    CoordinateTransform makeCT( String varName, NetcdfFile ds)  throws IOException {

      CoordinateTransform rs = new VerticalCT("oceanSigma-"+varName, conventionName, VerticalCT.Type.OceanSigma);
      rs.addParameter((new Parameter("height formula", "height(x,y,z) = eta(x,y) + sigma(k)*(depth(x,y) + eta(x,y))")));

      if (!addParameter( rs, OceanSigma.ETA, ds, eta, false)) return null;
      if (!addParameter( rs, OceanSigma.SIGMA, ds, s, false)) return null;
      if (!addParameter( rs, OceanSigma.DEPTH, ds, depth, false)) return null;

      return rs;
    }


    public String toString() { return "OceanS "+s + eta + depth; }
  }

  protected double readAttributeDouble(Variable v, String attname) {
    Attribute att = v.findAttributeIgnoreCase(attname);
    if (att == null) return Double.NaN;
    if (att.isString())
      return Double.parseDouble( att.getStringValue());
    else
      return att.getNumericValue().doubleValue();
  }

  private double[] readAttributeDouble2(Attribute a) {
    if (a == null) return null;

    double[] val= new double[2];
    if (a.isString()) {
      StringTokenizer stoke = new StringTokenizer(a.getStringValue());
      val[0] = Double.parseDouble( stoke.nextToken());
      val[1] = stoke.hasMoreTokens() ? Double.parseDouble( stoke.nextToken()) : val[0];
    } else {
      val[0] = a.getNumericValue().doubleValue();
      val[1] = (a.getLength() > 1) ? a.getNumericValue(1).doubleValue() : val[0];
    }
    return val;
  }

    /*
   * Add a Parameter to a CoordinateTransform.
   * Make sure that variable exist. If readData is true, read the data and use it as the value of the
   *  parameter, otherwise use the variable name as the value of the parameter.
   *
   * @param rs the CoordinateTransform
   * @param paramName the parameter name
   * @param ds dataset
   * @param varName variable name
   * @param readData if true, read data and use a  s parameter value
   * @return true if success, false is failed
   *
  protected boolean addParameter( CoordinateTransform rs, String paramName, NetcdfFile ds, String varName, boolean readData) {
    Variable dataVar = null;
    if (null == (dataVar = ds.findVariable(varName))){
      parseInfo.append("CoordSysBuilder No Variable named "+varName);
      userAdvice.append("CoordSysBuilder No Variable named "+varName);
      return false;
    }

    if (readData) {
      Array data = null;
      try {
        data = dataVar.read();
      } catch (IOException e) {
        parseInfo.append("CoordSysBuilder failed on read of "+varName+" err="+e+"\n");
        userAdvice .append("CoordSysBuilder failed on read of "+varName+" err="+e+"\n");
        return false;
      }
      double[] vals = (double [] ) data.get1DJavaArray( double.class);
      rs.addParameter(new Parameter(paramName, vals));

    } else
      rs.addParameter(new Parameter(paramName, varName));

    return true;
  }   */


}

  //////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////
  /** subclasses must implement these
  public abstract void augmentDataset( NetcdfDataset ncDataset);
  public abstract boolean isXAxis( CoordinateAxis dc);
  public abstract boolean isYAxis( CoordinateAxis dc);
  public abstract boolean isZAxis( CoordinateAxis dc);
  public abstract boolean isTimeAxis( CoordinateAxis dc);
  public abstract boolean isLatAxis( CoordinateAxis dc);
  public abstract boolean isLonAxis( CoordinateAxis dc);
  public abstract boolean isHeightAxis( CoordinateAxis dc);
  public abstract boolean isPressureAxis( CoordinateAxis dc);
  public abstract String getZisPositive( CoordinateAxis dc); // up = coords get bigger as you go up in altitude, else down

  /** construct CoordinateTransforms in preparation of calling getCoordinateTransforms()
  protected void constructCoordinateTransforms(NetcdfDataset ncDataset) { }

  /** Get a list of CoordinateTransforms for the given Coordinate System
  protected abstract java.util.List getCoordinateTransforms(CoordinateSystem cs);

  // coordinate axes

  /**
   * Find all Coordinate axes, replace in dataset.
   * Default just finds coordinate variables, subclasses usually override.
   * @param ds: dataset
   *
  protected void constructCoordAxes(NetcdfDataset ds) {
    findCoordinateVariables(ds);
  }

  /**
   * Find the netCDF coordinate variables,
   * make them into CoordinateAxis, replace in dataset.
   *
  protected void findCoordinateVariables(NetcdfDataset ds) {
    Iterator vars = ds.getVariables().iterator(); // uses copy
    while (vars.hasNext()) {
      Variable ncvar = (Variable) vars.next();
      if (debug) System.out.println(" findCoordinateVariables "+ncvar.getName()+" = "+ncvar.getClass().getName());
      if (ncvar.isCoordinateVariable() && !(ncvar instanceof CoordinateAxis)) {
        ds.addCoordinateAxis( (VariableDS) ncvar);
        parseInfo.append("add Coordinate Variable= "+ncvar.getName()+"\n");
      }
    }
  }

  /**
   * Run through all coordinate axes and assign a AxisType based on calling
   *  the isXXXAxis() methods.
   *
  protected void assignAxisType(NetcdfDataset ds) {
    Iterator cas = ds.getCoordinateAxes().iterator();
    while (cas.hasNext()) {
      CoordinateAxis ca = (CoordinateAxis) cas.next();
      if (!(ca instanceof CoordinateAxis1D))
        parseInfo.append( "CoordinateAxis "+ca.getName()+ " not 1D\n");

      //if (ca.isAuxilary()) // skip aux
      //  continue;

      if (isLatAxis(ca))
        ca.setAxisType(AxisType.Lat);
      else if (isLonAxis(ca))
        ca.setAxisType(AxisType.Lon);
      else if (isXAxis(ca))
        ca.setAxisType(AxisType.GeoX);
      else if (isYAxis(ca))
        ca.setAxisType(AxisType.GeoY);
      else if (isTimeAxis(ca))
        ca.setAxisType(AxisType.Time);
      else if (isPressureAxis(ca)) {
        ca.setAxisType(AxisType.Pressure);
        ca.setPositive("down");
      }
      else if (isHeightAxis(ca)) {
        ca.setAxisType(AxisType.Height);
        ca.setPositive(getZisPositive(ca));
      }
      else if (isZAxis(ca)) {
        ca.setAxisType(AxisType.GeoZ);
        ca.setPositive(getZisPositive(ca));
      }
      else {
        parseInfo.append("** " + ca.getName() + " not georeferencing Axis\n");
      }

    }
  }

  //////////////////////////////////////////////////////////////////////////////
  // coordinate systems

  /**
   * Default construction of Coordinate Systems.
   * This version constructs coordinate systems based on the list of coordinate axes that fit.
   * If multiple z axes, make coordinate system for each.
   *
  protected void constructCoordinateSystems(NetcdfDataset ds) {

    if (ds.getCoordinateAxes().size() == 0) {
      parseInfo.append("No coordinate axes were found\n");
      return;
    }
    parseInfo.append("Looking for coordinate systems:\n");

    // run through all top variables
    Iterator vars = ds.getVariables().iterator();
    while (vars.hasNext()) {
      VariableEnhanced v = (VariableEnhanced) vars.next();
      constructCoordinateSystems( ds, v);
    }
  }

  protected void constructCoordinateSystems(NetcdfDataset ds, VariableEnhanced v) {

      if (v instanceof StructureDS) {
        StructureDS s = (StructureDS) v;
        List members = s.getVariables();
        for (int i = 0; i < members.size(); i++) {
          VariableEnhanced nested =  (VariableEnhanced) members.get(i);
          constructCoordinateSystems( ds, nested);
        }
      } else {
        parseInfo.append("  check ");
        v.getNameAndDimensions(parseInfo, true, false);
        parseInfo.append(": ");

        if (wantCoordSystemsForVariable( ds, v))
          makeCoordSystemsForVariable(ds, v);
        parseInfo.append("\n");
      }

   }


  /**
   * Do we want a coordinate system for this variable?
   * only if rank > 1 and not a coordinate variable.
   *
  protected boolean wantCoordSystemsForVariable(NetcdfDataset ds, VariableEnhanced v) {
    int fullRank = v.getDimensionsAll().size();

    if (fullRank == 0) {
      parseInfo.append("is scalar\n");
      return false;
    }

    if (fullRank == 1) {  // why ??
      parseInfo.append("is rank 1\n");
      return false;
    }

    if (v.isCoordinateVariable()) {
      parseInfo.append("isCoordinateVariable\n");
      return false;
    }

    /* if ((v instanceof CoordinateAxis) && (v.getRank() == 1)) {
      parseInfo.append("isCoordinateAxis rank 1\n");
      return false;
    } *


    return true;
  }

  /**
   * Find the coordinate systems for this variable and add them to the variable.
   * If the coordinate system doesnt exist, make it.
   * Algorithm:
   *  1. Find all coordinate axes that fit the variable
   *  2. If both lat/lon and x/y axes, make seperate coordinate system for each.
   *  3. If multiple Z axes, make seperate coordinate system for each z axis.
   *
  protected void makeCoordSystemsForVariable(NetcdfDataset ds, VariableEnhanced v) {

    // find all axes that fit
    ArrayList axes = findAllCoordinateAxisForVariable(ds, v);
    if (axes.size() == 0) {
      parseInfo.append("none");
      return;
    }
    parseInfo.append("has coordSys ");

    // check for lat/lon and x/y, and seperate out the Z axis
    ArrayList axesXY = new ArrayList();
    ArrayList axesLL = new ArrayList();
    ArrayList axesZ = new ArrayList();
    ArrayList axesOther = new ArrayList();
    for (int i=0; i<axes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) axes.get(i);
      if ((axis.getAxisType() == AxisType.GeoZ) || (axis.getAxisType() == AxisType.Pressure) ||
          (axis.getAxisType() == AxisType.Height))
        axesZ.add( axis);
      else if ((axis.getAxisType() == AxisType.GeoX) || (axis.getAxisType() == AxisType.GeoY))
        axesXY.add( axis);
      else if ((axis.getAxisType() == AxisType.Lat) || (axis.getAxisType() == AxisType.Lon))
        axesLL.add( axis);
      else // hmm, could only be time, right ?
        axesOther.add(axis);
    }

    if (axesXY.size() > 0) {
      ArrayList axesO2 = new ArrayList( axesXY);
      axesO2.addAll( axesOther);
      seperateZaxes( ds, v, axesO2, axesZ);
    }

    if (axesLL.size() > 0) {
      ArrayList axesO2 = new ArrayList( axesLL);
      axesO2.addAll( axesOther);
      seperateZaxes( ds, v, axesO2, axesZ);
    }

    if ((axesXY.size() == 0) && (axesLL.size() == 0)) {
      seperateZaxes( ds, v, axesOther, axesZ);
    }

  }

  protected void seperateZaxes(NetcdfDataset ds, VariableEnhanced v, ArrayList axesOther, ArrayList axesZ) {

    // 0 or 1
    if (axesZ.size() < 2) {
      ArrayList axes = new ArrayList( axesOther);
      axes.addAll( axesZ);
      addCoordinateSystem( ds, v, axes);
      return;
    }

    // more than 1: make seperate coord sys for each
    for (int i=0; i<axesZ.size(); i++) {
      CoordinateAxis axisZ = (CoordinateAxis) axesZ.get(i);
      ArrayList axes = new ArrayList( axesOther);
      axes.add(axisZ);
      addCoordinateSystem( ds, v, axes);
    }

  }

  /**
   * Find all coordinate axes that fit the variable.
   *
  protected ArrayList findAllCoordinateAxisForVariable(NetcdfDataset ds, VariableEnhanced v) {
     // find all axes that fit
    ArrayList axes = new ArrayList();
    List coordAxes = ds.getCoordinateAxes();
    for (int i=0; i<coordAxes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) coordAxes.get(i);
      //if (axis.isAuxilary()) // skip aux
      //  continue;
      if ((axis != v) && isCoordinateAxisForVariable(axis, v))
        axes.add(axis);
    }
    return axes;
  }

  /**
   * Does this axis "fit" this variable?
   * True if all of the dimensions in the axis also appear in the variable
   *
  protected boolean isCoordinateAxisForVariable(Variable axis, VariableEnhanced v) {
    List varDims = v.getDimensionsAll();
    for (int i=0; i<axis.getRank(); i++) {
      Dimension axisDim = axis.getDimension(i);
      if (!varDims.contains( axisDim)) {
        if (showRejects) System.out.println(v.getName()+" missing dim "+axisDim.getName()+" in axis "+axis.getName());
        return false;
      }
    }
    return true;
  }

  /**
   * Get the coordinate system that consists of the given axes, and add it to the variable
   * If it doesnt exist, create it and add it to the dataset.
   * SIDE EFFECT: when you create it, call addCoordinateTransforms().
   *
  protected void addCoordinateSystem (NetcdfDataset ds, VariableEnhanced v, ArrayList axes) {
    CoordinateSystem cs = new CoordinateSystem(axes, null);
    if (!coordsys.containsKey( cs.getName())) {
      coordsys.put( cs.getName(), cs);
      ds.addCoordinateSystem( cs);
      cs.addCoordinateTransforms( getCoordinateTransforms( cs));
    }
    cs = (CoordinateSystem) coordsys.get( cs.getName());
    ds.addCoordinateSystem( v, cs);
    parseInfo.append(cs.getName()+" ");
  }

  //////////////////////////////////////////////////////////////////////////////
  // coordinate transforms

  protected void assignCoordinateTransforms(NetcdfDataset ds) {
    parseInfo.append("Looking for coordinate transforms:\n");

    // run through each coordinate system
    Iterator css = ds.getCoordinateSystems().iterator();
    while (css.hasNext()) {
      CoordinateSystem cs = (CoordinateSystem) css.next();
      List cts = getCoordinateTransforms( cs);
      if (cts.size() > 0) {
        cs.addCoordinateTransforms(cts);
        parseInfo.append("  "+cs.getName()+"  has transforms:");
        Iterator iter = cts.iterator();
        while (iter.hasNext()) {
          CoordinateTransform ct = (CoordinateTransform) iter.next();
          parseInfo.append(" "+ct.getName());
        }
        parseInfo.append("\n");
      }
    }
  }

  // not currently used
  private ProjectionImpl makeProjection(CoordinateTransform ct) {
    Parameter att = ct.findParameterIgnoreCase("Projection_Name");
    if (att != null) {
      String projName = att.getStringValue();
      if (projName.equalsIgnoreCase( "Lambert_Conformal_Conic"))
        return makeLambertProjection( ct);
    }
    return null;
  }

  private ProjectionImpl makeLambertProjection(CoordinateTransform ct) {

    double par1 = 0.0;
    Parameter att = ct.findParameterIgnoreCase("Standard_Parallel_1");
    if (att != null) par1 = att.getNumericValue();

    double par2 = 0.0;
    att = ct.findParameterIgnoreCase("Standard_Parallel_2");
    if (att != null) par2 = att.getNumericValue();

    double lon0 = 0.0;
    att = ct.findParameterIgnoreCase("Longitude_of_Central_Meridian");
    if (att != null) lon0 = att.getNumericValue();

    double lat0 = 0.0;
    att = ct.findParameterIgnoreCase("Latitude_of_Projection_Origin");
    if (att != null) lat0 = att.getNumericValue();

    return new LambertConformal(lat0, lon0, par1, par2);
  }


  /////////////////////////////////////
  // utilities

 /** Search for a coord axis with the given name in the collection of CoordAxis.
  * @param ds look in this dataset
  *  @param fullName name to search for.
  *  @return CoordAxisImpl with that name, or null if none.
  *
  protected CoordinateAxis findCoordAxis( NetcdfDataset ds, String fullName) {
    List coordAxes = ds.getCoordinateAxes();
    for (int i=0; i<coordAxes.size(); i++) {
      CoordinateAxis dc = (CoordinateAxis) coordAxes.get(i);
      if (fullName.equals( dc.getName()))
        return dc;
    }
    return null;
  }



}

  //////////////////////////////////////////////////////////////////////////
  // these are convenience routines for subclasses to use.

  /**
   * Add a CoordAxis for the named dimension from the variable ncvar.
   * Decide if its an edge or midpoint based on its size.
   * Add it to the coordAxes collection.
   *
   * @param Dimension dim: add CoordAxis for this dimension.
   * @param Variable ncvar: get values from this variable.
   *
  protected void addCoordAxisFromVariable( Dimension dim, Variable ncvar) {
    Array arr;
    try {
      arr = ncvar.read();
    } catch (java.io.IOException ioe) {
      System.out.println("DAMAGED NETCDF FILE: ParseStrategy failed to create CoordAxis for "+
        ncvar.getName()+" in file "+ netcdf.getPathName()+" "+ ioe);
      return;
     }

     Array mid = null, edge = null;
     if (arr.getSize() == dim.getLength())
       mid = arr;
     else
       edge = arr;

    DimCoordAxis dc = findDimCoordAxis( dim.getName());
    if (dc == null) {
      CoordAxisImpl ca =  new CoordAxisImpl(dim.getName(),
        netcdf.findAttValueIgnoreCase(ncvar, "long_name", ncvar.getName()),
        netcdf.findAttValueIgnoreCase(ncvar, "units", "none"),
        mid, edge);

      dc = new DimCoordAxis( dim, ca);
      coordAxes.add(dc);

    } else {
      // already got a CoordAxis, must supplement
      if (arr.getSize() == dim.getLength()) {
        dc.ca.setCoords( arr);
        dc.mid = ncvar;
      } else if (arr.getSize() == dim.getLength()+1) {
        dc.ca.setEdges( arr);
        dc.edge = ncvar;
      } else {
        System.out.println("Illegal cardinality on Variable "+ ncvar.getName()+
          " as coord axis for dimension "+ dim.getName());
       return;
      }
    }
  }

  /**
   * Add a CoordAxis for the named dimension from the variables top and bot,
   * interpreted as the top and bottom edge.
   * Add it to the coordAxes collection.
   *
   * @param Dimension dim: add CoordAxis for this dimension.
   * @param Variable top, bot: get values from these variables.
   *
  protected DimCoordAxis addCoordAxisFromTopBotVars( Dimension dim, Variable top, Variable bot) {
      //read data
    Array arrTop, arrBot;
    try {
      arrTop = top.read();
    } catch (java.io.IOException ioe) {
      System.out.println("DAMAGED NETCDF FILE: ParseStrategy failed to create CoordAxis for "+
        top.getName()+" in file "+ netcdf.getPathName()+" "+ ioe);
      return null;
     }
    try {
      arrBot = bot.read();
    } catch (java.io.IOException ioe) {
      System.out.println("DAMAGED NETCDF FILE: ParseStrategy failed to create CoordAxis for "+
        bot.getName()+" in file "+ netcdf.getPathName()+" "+ ioe);
      return null;
    }

      // check sizes
    if (arrTop.getSize() != arrBot.getSize()) {
      System.out.println("ParseStrategy addCoordAxisFromTopBotVars: illegal aarray sizes for dim "
        + dim.getName()+ " top ("+ top.getName() + ") = "+ arrTop.getSize()+
        " bot ("+ bot.getName() + ") = "+ arrBot.getSize());
      return null;
     }
    int size = (int) arrTop.getSize();
    ArrayDouble.D1 edge = new ArrayDouble.D1( size+1);

      // create edges
    Index imaTop = arrTop.getIndex();
    Index imaBot = arrBot.getIndex();
    double valTop = arrTop.getDouble(imaTop.set0(0));
    double valBot = arrBot.getDouble(imaBot.set0(0));
    IndexIterator iterHi, iterLo;
    if (valTop > valBot) {
      iterHi = arrTop.getIndexIterator();
      iterLo = arrBot.getIndexIterator();
    } else {
      iterHi = arrBot.getIndexIterator();
      iterLo = arrTop.getIndexIterator();
    }

    double lastHi = 0.0;
    int count = 0;
    while (iterHi.hasNext()) {
      double valHi = iterHi.getDoubleNext();
      double valLo = iterLo.getDoubleNext();
      edge.set(count, valLo);
      edge.set(count+1, valHi);
      if (count > 0) {
        if (valLo != lastHi) {
          System.out.println("ParseStrategy addCoordAxisFromTopBotVars: non continuous dim= "
            + dim.getName()+ "index = "+count+": top ("+ top.getName() + ") = "+ lastHi+
            " bot ("+ bot.getName() + ") = "+ valLo);
          return null;
        }
      }
      count++;
      lastHi = valHi;
    }

    if (debug) {
      System.out.print("addCoordAxisFromTopBotVars edges = ");
      for (int i=0; i<size-1; i++)
        System.out.print(" "+edge.get(i));
      System.out.println();
    }

    String long_name = netcdf.findAttValueIgnoreCase(top, "long_name", top.getName());
    String units = netcdf.findAttValueIgnoreCase(top, "units", top.getName());

    DimCoordAxis dc = findDimCoordAxis( dim.getName());
    if (dc == null) {
      CoordAxisImpl ca =  new CoordAxisImpl(dim.getName(), long_name, units, null, edge);
      dc = new DimCoordAxis( dim, ca);
      coordAxes.add(dc);
      return dc;
    } else {
      System.out.println("ERROR already have a coordinate axis for dimension "+ dim.getName()+
        " trying to add top = "+ top.getName() + " bot = "+ bot.getName());
    }
    return null;
  }

  /**
   * Make a dummy CoordAxis for the named dimension.
   * add it to the coordAxes collecion.
   * @param Dimension dim: add CoordAxis for this dimension.
   *
  protected void coordAxisDummy( Dimension dim) {
    double[] mid = new double[dim.getLength()];
    for (int i=0; i<dim.getLength(); i++)
      mid[i] = 1.0 * i;

    CoordAxisImpl ca =  new CoordAxisImpl(dim.getName(), "dummy", "none",
       mid, null);

    coordAxes.add(ca);
  } */

 /** Search for a coord axis with the given name in the collection of CoordAxis.
  *  @param String name: name to search for.
  *  @return CoordAxisImpl with that name, or null if none.
  *
  protected DimCoordAxis findDimCoordAxis( String name) {
    for (int i=0; i<coordAxes.size(); i++) {
      DimCoordAxis dc = (DimCoordAxis) coordAxes.get(i);
      if (name.equals( dc.dim.getName()))
        return dc;
    }
    return null;
  }

  /**
   * Make a GeoCoordSysImpl for the Variable v, by finding a coord axis with the same name as
   * each dimension, except for one dimension which can be skipped.
   * All other dimensions must have a coord axis, or return null.
   * @param String name: name of coord sys
   * @param String desc: description of the coords sys.
   * @param Variable: make coord sys for this variable.
   * @param int skipDim: skip this dimension
   * @return GeoCoordSysImpl for this variable, or null if failure.
   *
  protected GeoCoordSysImpl coordSysFromVar(String name, String desc, Variable v, int skipDim) {
    int rank = v.getRank()-1;
    int count = 0;
    CoordAxisImpl[] caxes = new CoordAxisImpl[ rank];
    for (int i=0; i< rank+1; i++) {
      if (i == skipDim)
        continue;
      caxes[count] = coordAxisFind( v.getDimension(i).getName());
      if (caxes[count] == null) {
        System.out.println("No coord var for dimension= <"+ v.getDimension(i).getName()+">");
        return null;
      }
      count++;
    }
    return coordSysFromAxes(name, desc, caxes);
  }

  /** Construct coord sys name from variable's dimension's names.
   *  @param Variable v
   *  @return String coordinate system name
   *
  private StringBuffer gb_hashkey = new StringBuffer(200);
  protected String coordSysNameForVariable( Variable v) {
    boolean first = true;
    gb_hashkey.setLength(0);
    for (int i=0; i<v.getRank(); i++) {
      if (!first)
        gb_hashkey.append(",");
      first = false;
      gb_hashkey.append( v.getDimension(i).getName());
    }
    return gb_hashkey.toString();
  }

  /** Construct coord sys name from variable's dimension's names, but
   *  skip the given dimension.
   *  @param Variable v
   *  @param in skipDim, the dimension to skip, as an index into the Variables
   *  @return String coordinate system name
   *
  private StringBuffer gb_hashkey = new StringBuffer(200);
  protected String coordSysNameForVariable( Variable v, int skipDim) {
    boolean first = true;
    gb_hashkey.setLength(0);
    for (int i=0; i<v.getRank(); i++) {
      if (i == skipDim)
        continue;
      if (!first)
        gb_hashkey.append(",");
      first = false;
      gb_hashkey.append( v.getDimension(i).getName());
    }
    return gb_hashkey.toString();
  } */

  /** Find a coord axis with the given name in the given list of CoordAxisimpl.
   *  @param List axes: list of axes to search through
   *  @param String name: name for coord axis of this name
   *  @return index in the list, or -1 if not found.
   *
  protected int findCoordAxis(List axes, String name) {
    for (int i=0; i<axes.size(); i++) {
      CoordAxisImpl cv = (CoordAxisImpl) axes.get(i);
      if (name.equals( cv.getName()))
        return i;
    }
    return -1;
  }

   /* look through the axes, whose coord var has the given attribute
  protected int findCoordAxisByAttribute( List axes, String att) {
    for (int i=0; i<axes.size(); i++) {
      CoordAxisImpl cv = (CoordAxisImpl) axes.get(i);
      DimCoordAxis dc = findDimCoordAxis( cv.getName()); // track down the original ncvars
      if (dc.mid != null) {
        if (null != netcdf.findAttValueIgnoreCase(dc.mid, att, null))
          return i;
      }
      if (dc.edge != null) {
        if (null != netcdf.findAttValueIgnoreCase(dc.edge, att, null))
          return i;
      }
    }
    return -1;
  }

   // look through the axes, whose coord var has the given attribute and value
  protected int findCoordAxisByAttributeValue( List axes, String att, String val) {
    for (int i=0; i<axes.size(); i++) {
      CoordAxisImpl cv = (CoordAxisImpl) axes.get(i);
      DimCoordAxis dc = findDimCoordAxis( cv.getName()); // track down the original ncvars
      if (dc.mid != null) {
        String attVal = netcdf.findAttValueIgnoreCase(dc.mid, att, null);
        if ((null != attVal) && attVal.equals( val))
          return i;
      }
      if (dc.edge != null) {
        String attVal = netcdf.findAttValueIgnoreCase(dc.edge, att, null);
        if ((null != attVal) && attVal.equals( val))
          return i;
      }
    }

    return -1;
  }

   /* look through the axes, which has units equal to "units"
  protected int findCoordAxisByUnit( List axes, String units) {
    for (int i=0; i<axes.size(); i++) {
      CoordAxisImpl cv = (CoordAxisImpl) axes.get(i);
      String axis_unit = cv.getUnitString();
      if (null == axis_unit) continue;
      if (units.equalsIgnoreCase(axis_unit))
        return i;
    }
    return -1;
  }

   // look through the axes, for one which can be convertible to "units"
  protected int findCoordAxisByUnitConvertible( List axes, String units) {
    for (int i=0; i<axes.size(); i++) {
      CoordAxisImpl cv = (CoordAxisImpl) axes.get(i);
      String axis_unit = cv.getUnitString();
      if (null == axis_unit) continue;
      if (units.equalsIgnoreCase("time")) {
        if (SimpleUnit.isDateUnit(axis_unit))
          return i;
      } else {
        if (SimpleUnit.isCompatible(units, axis_unit))
          return i;
      }
    }
    return -1;
  }

  /** Find a coord axis that starts with the given name in a given list of CoordAxisimpl.
   *  @param List axes: list of axes to search through
   *  @param String startingWith: look for coord axis whose name starts with this.
   *  @return index in the list, or -1 if not found.
   *
  protected int findCoordAxisStartsWith(List axes, String startingWith) {
    for (int i=0; i<axes.size(); i++) {
      CoordAxisImpl cv = (CoordAxisImpl) axes.get(i);
      if ((cv != null) && cv.getName().startsWith( startingWith))
        return i;
    }
    return -1;
  } */

  // can we pass in dimension, and combine next 2,3?


  /* See if this dimension has an aliased coordinate variable:
   *  A global attribute of that dimension name, whose value is the name of a coord var, eg:
   *  <pre>
   *    :dimName = alias;
   *    Variable alias(alias);
   *   </pre>
   *
   *  @param Dimension dim: look for this dimension name
   *  @return: aliased coordinate variable, or null if not exist.
   *
  protected Variable searchAliasedDimension( Dimension dim) {
    String dimName = dim.getName();
    String alias = netcdf.findAttValueIgnoreCase(null, dimName, null);
    if (alias == null)
      return null;

    Dimension dim2 = netcdf.findDimension( alias);
    if (dim2 == null)
      return null;
    return dim2.getCoordinateVariable();
  }

  /** Search for an aliased coord in the netcdf file
   * same as findCoordVarFromAlias, but doesnt have to be a coord var.
   * Has to be 1 dimensional, with dimension = dimName.
   * <pre>
   *    :dimName = alias;
   *    Variable alias(dimName);
   *   </pre>
   *
   *  @param Dimension dim: look for this dimension name
   *  @return: aliased variable, or null if not exist.
   *

  protected Variable searchAliasedDimension2( Dimension dim) {
    String dimName = dim.getName();
    String alias = netcdf.findAttValueIgnoreCase(null, dimName, null);
    if (alias == null)
      return null;

    Variable ncvar = netcdf.findVariable( alias);
    if (ncvar == null)
      return null;
    if (ncvar.getRank() != 1)
        return null;

    Iterator dimIter = ncvar.getDimensions().iterator();
    Dimension dim2 = (Dimension) dimIter.next();
    if (dimName.equals(dim2.getName()))
      return ncvar;

    return null;
  }





    // a mapping from dim -> CoordAxis, and the ncvars that define the CoordAxis
  protected class DimCoordAxis {
    Dimension dim;
    CoordAxisImpl ca;
    Variable mid = null, edge = null;
    boolean isReferential = false;
    boolean isGeoCoord = false;
    int type = 0; // GeoCoordSysImpl.?_DIM

    DimCoordAxis(Dimension dim, CoordAxisImpl ca) {
      this.dim = dim;
      this.ca = ca;
    }
  }

    private ProjectionImpl makeLambertProjection(CoordinateTransform ct) {

    double par1 = 0.0;
    Parameter att = ct.findParameterIgnoreCase("Standard_Parallel_1");
    if (att != null) par1 = att.getNumericValue();

    double par2 = 0.0;
    att = ct.findParameterIgnoreCase("Standard_Parallel_2");
    if (att != null) par2 = att.getNumericValue();

    double lon0 = 0.0;
    att = ct.findParameterIgnoreCase("Longitude_of_Central_Meridian");
    if (att != null) lon0 = att.getNumericValue();

    double lat0 = 0.0;
    att = ct.findParameterIgnoreCase("Latitude_of_Projection_Origin");
    if (att != null) lat0 = att.getNumericValue();

    return new LambertConformal(lat0, lon0, par1, par2);
  }
} */