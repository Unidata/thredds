// $Id:CoordSysBuilder.java 51 2006-07-12 17:13:13Z caron $
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
import ucar.ma2.DataType;

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
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
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
    registerConvention(_Coordinate.Convention, CoordSysBuilder.class);
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
    registerConvention("M3IO", M3IOConvention.class);
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
    * @param className name of class that implements CoordSysBuilderIF.
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

  protected String conventionName = _Coordinate.Convention; // name of Convention
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
      if (vp.coordAxes != null)
        findCoordinateAxes(vp, vp.coordAxes);
      if (vp.coordinates != null)
        findCoordinateAxes(vp, vp.coordinates);
    }
  }

  private void findCoordinateAxes(VarProcess vp, String coordinates) {
    StringTokenizer stoker = new StringTokenizer(coordinates);
    while (stoker.hasMoreTokens()) {
      String vname = stoker.nextToken();
      VarProcess ap = findVarProcess(vname); // LOOK: full vs short name
      if (ap != null) {
        if (!ap.isCoordinateAxis)
          parseInfo.append(" CoordinateAxis = " + vname + " added; referenced from var= " + vp.v.getName() + "\n");
        ap.isCoordinateAxis = true;
      } else {
        parseInfo.append("***Cant find coordAxis " + vname + " referenced from var= " + vp.v.getName() + "\n");
        userAdvice.append("***Cant find coordAxis " + vname + " referenced from var= " + vp.v.getName() + "\n");
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

    // look for explicit references to coord sys variables
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

    // look for explicit listings of coordinate axes
    for (int i = 0; i < varList.size(); i++) {
      VarProcess vp = (VarProcess) varList.get(i);
      VariableEnhanced ve = (VariableEnhanced) vp.v;

      if (!vp.hasCoordinateSystem() && (vp.coordAxes != null) && vp.isData()) {
        List dataAxesList = getAxes(vp.coordAxes, vp.v.getName());
        if (dataAxesList.size() > 1) {
          String coordSysName = CoordinateSystem.makeName(dataAxesList);
          CoordinateSystem cs = ncDataset.findCoordinateSystem(coordSysName);
          if (cs != null) {
            ve.addCoordinateSystem(cs);
            parseInfo.append(" assigned explicit CoordSystem '" + cs.getName() + "' for var= " + vp.v.getName() + "\n");
          } else {
            CoordinateSystem csnew = new CoordinateSystem(ncDataset, dataAxesList, null);
            ve.addCoordinateSystem(csnew);
            ncDataset.addCoordinateSystem(csnew);
            parseInfo.append(" created explicit CoordSystem '" + csnew.getName() + "' for var= " + vp.v.getName() + "\n");
          }
        }
      }
    }
  }

  private List getAxes( String names, String varName) {
    ArrayList axesList = new ArrayList();
    StringTokenizer stoker = new StringTokenizer( names);
    while (stoker.hasMoreTokens()) {
      String vname = stoker.nextToken();
      VarProcess ap = findVarProcess( vname); // LOOK: full vs short name
      if (ap != null)
        axesList.add( ap.v);
      else {
        parseInfo.append("***Cant find Coordinate Axis "+vname+" referenced from var= "+varName+"\n");
        userAdvice.append("***Cant find Coordinate Axis "+vname+" referenced from var= "+varName+"\n");
      }
    }
    return axesList;
  }

  /**
   * Make implicit CoordinateSystem objects for variables that dont already have one, by using the
   * variables' list of coordinate axes, and any coordinateVariables for it. Must be at least 2 axes.
   * @param ncDataset
   */
  protected void makeCoordinateSystemsImplicit(NetcdfDataset ncDataset) {
    for (int i = 0; i < varList.size(); i++) {
      VarProcess vp = (VarProcess) varList.get(i);

      if (!vp.hasCoordinateSystem() && vp.isData()) {
        List dataAxesList = vp.findCoordinateAxes( true);
        if (dataAxesList.size() < 2)
          continue;

        VariableEnhanced ve = (VariableEnhanced) vp.v;
        String csName = CoordinateSystem.makeName(dataAxesList);
        CoordinateSystem cs = ncDataset.findCoordinateSystem(csName);
        if (cs != null) {
          ve.addCoordinateSystem(cs);
          parseInfo.append(" assigned implicit coord System '" + cs.getName() + "' for var= " + vp.v.getName() + "\n");
        } else {
          CoordinateSystem csnew = new CoordinateSystem(ncDataset, dataAxesList, null);
          csnew.setImplicit( true);

          ve.addCoordinateSystem(csnew);
          ncDataset.addCoordinateSystem(csnew);
          parseInfo.append(" created implicit coord System '" + csnew.getName() + "' for var= " + vp.v.getName() + "\n");
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

      if (vp.hasCoordinateSystem() || !vp.isData()) continue;

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
          parseInfo.append(" assigned maximal coord System '" + cs.getName() + "' for var= " + ve.getName() + "\n");
        } else {
          CoordinateSystem csnew = new CoordinateSystem(ncDataset, axisList, null);
          csnew.setImplicit( true);
          if (null != implicit) ve.removeCoordinateSystem(implicit);
          ve.addCoordinateSystem(csnew);
          ncDataset.addCoordinateSystem(csnew);
          parseInfo.append(" created maximal coord System '" + csnew.getName() + "' for var= " + ve.getName() + "\n");
        }

    }
  }

  /**
   * Does this axis "fit" this variable.
   * True if all of the dimensions in the axis also appear in the variable.
   * If char variable, last dimension is left out.
   */
  protected boolean isCoordinateAxisForVariable(Variable axis, VariableEnhanced v) {
    List varDims = v.getDimensionsAll();

    // a CHAR variable must really be a STRING, so leave out the last (string length) dimension
    int checkDims = axis.getRank();
    if (axis.getDataType() == DataType.CHAR)
      checkDims--;

    for (int i=0; i<checkDims; i++) {
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
    public String coordinates;// CF : partial coordAxes

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

      Attribute att = v.findAttributeIgnoreCase(_Coordinate.AxisType);
      if (att != null) {
        String axisName = att.getStringValue();
        axisType = AxisType.getType( axisName);
        isCoordinateAxis = true;
        parseInfo.append(" Coordinate Axis added = "+v.getName()+" type= "+axisName+"\n");
      }

      coordVarAlias = ds.findAttValueIgnoreCase(v, _Coordinate.AliasForDimension, null);
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

      positive = ds.findAttValueIgnoreCase(v, _Coordinate.ZisPositive, null);
      if (positive == null)
        positive = ds.findAttValueIgnoreCase(v, "positive", null);
      else {
        isCoordinateAxis = true;
        positive = positive.trim();
      }

      coordAxes = ds.findAttValueIgnoreCase(v, _Coordinate.Axes, null);
      coordSys = ds.findAttValueIgnoreCase(v, _Coordinate.Systems, null);
      coordTransforms = ds.findAttValueIgnoreCase(v, _Coordinate.Transforms, null);
      isCoordinateSystem = (coordTransforms != null);

      coordAxisTypes = ds.findAttValueIgnoreCase(v, _Coordinate.AxisTypes, null);
      coordTransformType = ds.findAttValueIgnoreCase(v, _Coordinate.TransformType, null);
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

    public boolean hasCoordinateSystem() {
      return ((VariableEnhanced) v).getCoordinateSystems().size() > 0;
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
        ca.addAttribute( new Attribute(_Coordinate.AxisType, axisType.toString()));

        if (((axisType == AxisType.Height) || (axisType == AxisType.Pressure) || (axisType == AxisType.GeoZ)) &&
            (positive != null)) {
          ca.setPositive( positive);
          ca.addAttribute( new Attribute(_Coordinate.ZisPositive, positive));
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
      } else if (coordinates != null) { // CF partial listing of axes
        StringTokenizer stoker = new StringTokenizer( coordinates);
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

}