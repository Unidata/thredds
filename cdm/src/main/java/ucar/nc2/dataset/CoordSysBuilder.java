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
package ucar.nc2.dataset;

import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.CancelTask;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.dataset.conv.*;
import ucar.ma2.DataType;

import java.lang.reflect.Method;
import java.io.IOException;
import java.util.*;

/**
 * Abstract class for implementing Convention-specific parsing of netCDF files.
 * <p/>
 * You can use an NcML file alone (use registerNcML()) if file uses a convention attribute.
 * If not, you must implement a class that implements isMine() to identify your files, and
 * call wrapNcML in the augmentDataset method (see eg ATDRadarConvention class).
 * <p/>
 * <p/>
 * Subclasses Info:
 * <pre>
 * // identify which variables are coordinate axes
 * // default: 1) coordinate variables 2) variables with _coordinateAxisType attribute 3) variables listed
 * // in a coordinates attribute on another variable.
 * findCoordinateAxes( ncDataset);
 * <p/>
 * // identify which variables are used to describe coordinate system
 * findCoordinateSystems( ncDataset);
 * // identify which variables are used to describe coordinate transforms
 * findCoordinateTransforms( ncDataset);
 * // turn Variables into CoordinateAxis objects
 * makeCoordinateAxes( ncDataset);
 * // make Coordinate Systems for all Coordinate Systems Variables
 * makeCoordinateSystems( ncDataset);
 * <p/>
 * // Assign explicit CoordinateSystem objects to variables
 * assignExplicitCoordinateSystems( ncDataset);
 * makeCoordinateSystemsImplicit( ncDataset);
 * if (useMaximalCoordSys)
 * makeCoordinateSystemsMaximal( ncDataset);
 * <p/>
 * makeCoordinateTransforms( ncDataset);
 * assignCoordinateTransforms( ncDataset);
 * </pre>
 *
 * @author caron
 */

/*
  Implementation notes:

  Generally, subclasses should add the _Coordinate conventions, see
    http://www.unidata.ucar.edu/software/netcdf-java/CoordinateAttributes.html
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
  static public final String resourcesDir = "resources/nj22/coords/"; // resource path
  static protected org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordSysBuilder.class);

  // static private Map<String, Class> conventionHash = new HashMap<String, Class>();
  static private List<Convention> conventionList = new ArrayList<Convention>();
  static private Map<String, String> ncmlHash = new HashMap<String, String>();
  static private boolean useMaximalCoordSys = true;
  static private boolean userMode = false;

  /**
   * Allow plug-ins to determine if it owns a file based on the file's Convention attribute.
   */
  static public interface ConventionNameOk {

    /**
     * Do you own this file?
     *
     * @param convName name in the file
     * @param wantName name passed into registry
     * @return true if you own this file
     */
    boolean isMatch(String convName, String wantName);
  }

  // search in the order added
  static { // wont get loaded unless explicitly called
    registerConvention(_Coordinate.Convention, CoordSysBuilder.class, null);

    registerConvention("CF-1.", CF1Convention.class, new ConventionNameOk() {
      public boolean isMatch(String convName, String wantName) {
        if (convName.startsWith(wantName)) return true;
        List<String> names = breakupConventionNames(convName);
        for (String name : names)
          if (name.startsWith(wantName)) return true;
        return false;
      }
    });

    registerConvention("COARDS", COARDSConvention.class, null);
    registerConvention("NCAR-CSM", CSMConvention.class, null);
    registerConvention("Unidata Observation Dataset v1.0", UnidataObsConvention.class, null);
    registerConvention("GDV", GDVConvention.class, null);

    registerConvention("ATDRadar", ATDRadarConvention.class, null);
    registerConvention("CEDRICRadar", CEDRICRadarConvention.class, null);
    registerConvention("Zebra", ZebraConvention.class, null);
    registerConvention("GIEF/GIEF-F", GIEFConvention.class, null);
    registerConvention("IRIDL", IridlConvention.class, null);

    // the uglies
    registerConvention("NUWG", NUWGConvention.class, null);
    registerConvention("AWIPS", AWIPSConvention.class, null);
    registerConvention("AWIPS-Sat", AWIPSsatConvention.class, null);
    registerConvention("WRF", WRFConvention.class, null);

    registerConvention("M3IOVGGrid", M3IOVGGridConvention.class, null);
    registerConvention("M3IO", M3IOConvention.class, null);
    registerConvention("IFPS", IFPSConvention.class, null);
    registerConvention("ARPS/ADAS", ADASConvention.class, null);

    // point data
    registerConvention("MADIS surface observations, v1.0", MADISStation.class, null);
    registerConvention("epic-insitu-1.0", EpicInsitu.class, null);
    registerConvention("NCAR-RAF/nimbus", Nimbus.class, null);
    registerConvention("Cosmic1Convention", Cosmic1Convention.class, null);
    registerConvention("Jason2Convention", Jason2Convention.class, null);  
    registerConvention("Suomi", Suomi.class, null);

    // new
    registerConvention("NSSL National Reflectivity Mosaic", NsslRadarMosaicConvention.class, null);
    registerConvention("FslWindProfiler", FslWindProfiler.class, null);
    registerConvention("ModisSatellite", ModisSatellite.class, null);
    registerConvention("AvhrrSatellite", AvhrrConvention.class, null);
    registerConvention("NPP/NPOESS", NppConvention.class, null);

    // further calls to registerConvention are by the user
    userMode = true;
  }

  /**
   * Register an NcML file that implements a Convention by wrappping the dataset in the NcML.
   * It is then processed by CoordSysBuilder, using the _Coordinate attributes.
   *
   * @param conventionName name of Convention, must be in the "Conventions" global attribute.
   * @param ncmlLocation   location of NcML file, may be local file or URL.
   * @see ucar.nc2.ncml.NcMLReader#wrapNcML
   */
  static public void registerNcML(String conventionName, String ncmlLocation) {
    ncmlHash.put(conventionName, ncmlLocation);
  }

  /**
   * Register a class that implements a Convention. Will match (ignoring case) the COnvention name.
   *
   * @param conventionName name of Convention.
   *                       This name will be used to look in the "Conventions" global attribute.
   *                       Otherwise, you must implement the isMine() static method.
   * @param c              implementation of CoordSysBuilderIF that parses those kinds of netcdf files.
   */
  static public void registerConvention(String conventionName, Class c) {
    registerConvention(conventionName, c, null);
  }

  /**
   * Register a class that implements a Convention.
   *
   * @param conventionName name of Convention.
   *                       This name will be used to look in the "Conventions" global attribute.
   *                       Otherwise, you must implement the isMine() static method.
   * @param match          pass in your own matcher. if null, equalsIgnoreCase() will be used.
   * @param c              implementation of CoordSysBuilderIF that parses those kinds of netcdf files.
   */
  static public void registerConvention(String conventionName, Class c, ConventionNameOk match) {

    if (!(CoordSysBuilderIF.class.isAssignableFrom(c)))
      throw new IllegalArgumentException("CoordSysBuilderIF Class " + c.getName() + " must implement CoordSysBuilderIF");

    // fail fast - check newInstance works
    try {
      c.newInstance();
    } catch (InstantiationException e) {
      throw new IllegalArgumentException("CoordSysBuilderIF Class " + c.getName() + " cannot instantiate, probably need default Constructor");
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("CoordSysBuilderIF Class " + c.getName() + " is not accessible");
    }

    // user stuff gets put at top
    if (userMode)
      conventionList.add(0, new Convention(conventionName, c, match));
    else
      conventionList.add(new Convention(conventionName, c, match));

    // user stuff will override here
    // conventionHash.put(conventionName, c);
  }

  static private Class matchConvention(String convName) {
    for (Convention c : conventionList) {
      if ((c.match == null) && c.convName.equalsIgnoreCase(convName)) return c.convClass;
      if ((c.match != null) && c.match.isMatch(convName, c.convName)) return c.convClass;
    }
    return null;
  }

  /**
   * Register a class that implements a Convention.
   *
   * @param conventionName name of Convention.
   *                       This name will be used to look in the "Conventions" global attribute.
   *                       Otherwise, you must implement the isMine() static method.
   * @param className      name of class that implements CoordSysBuilderIF.
   * @throws ClassNotFoundException if class could not be loaded
   */
  static public void registerConvention(String conventionName, String className) throws ClassNotFoundException {
    Class c = Class.forName(className);
    registerConvention(conventionName, c, null);
  }

  /**
   * If true, assign implicit CoordinateSystem objects to variables that dont have one yet.
   * Default value is false.
   *
   * @param b true if if you want to guess at Coordinate Systems
   * @see #makeCoordinateSystemsMaximal
   */
  static public void setUseMaximalCoordSys(boolean b) {
    useMaximalCoordSys = b;
  }

  /**
   * Get whether to make records into Structures.
   *
   * @return whether to make records into Structures.
   */
  static public boolean getUseMaximalCoordSys() {
    return useMaximalCoordSys;
  }

  // breakup list of Conventions
  static private List<String> breakupConventionNames(String convName) {
    List<String> names = new ArrayList<String>();

    if ((convName.indexOf(',') > 0) || (convName.indexOf(';') > 0)) {
      StringTokenizer stoke = new StringTokenizer(convName, ",;");
      while (stoke.hasMoreTokens()) {
        String name = stoke.nextToken();
        names.add(name.trim());
      }
    } else if ((convName.indexOf('/') > 0)) {
      StringTokenizer stoke = new StringTokenizer(convName, "/");
      while (stoke.hasMoreTokens()) {
        String name = stoke.nextToken();
        names.add(name.trim());
      }
    }
    return names;
  }

  /**
   * Get a CoordSysBuilder whose job it is to add Coordinate information to a NetcdfDataset.
   *
   * @param ds         the NetcdfDataset to modify
   * @param cancelTask allow user to bail out.
   * @return the builder used
   * @throws java.io.IOException on io error
   * @see ucar.nc2.dataset.NetcdfDataset#enhance
   */
  static public CoordSysBuilderIF factory(NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    // look for the Conventions attribute
    String convName = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (convName == null)
      convName = ds.findAttValueIgnoreCase(null, "Convention", null);
    if (convName != null)
      convName = convName.trim();

    if (convName != null) {
      // look for ncml first
      String convNcML = ncmlHash.get(convName);
      if (convNcML != null) {
        CoordSysBuilder csb = new CoordSysBuilder();
        NcMLReader.wrapNcML(ds, convNcML, cancelTask);
        return csb;
      }
    }

    // now look for Convention parsing class
    Class convClass = null;
    if (convName != null) {
      // now look for Convention parsing class
      convClass = matchConvention(convName);

      // now look for comma or semicolon or / delimited list
      if (convClass == null) {
        List<String> names = breakupConventionNames(convName);
        if (names.size() > 0) {
          // search the registered conventions, in order
          for (Convention conv : conventionList) {
            for (String name : names) {
              if (name.equalsIgnoreCase(conv.convName)) {
                convClass = conv.convClass;
                convName = name;
              }
            }
            if (convClass != null) break;
          }
        }
      }
    }

    // look for ones that dont use Convention attribute, in order added.
    // call isMine() using reflection.
    if (convClass == null) {
      convName = null;
      for (Convention conv : conventionList) {
        Class c = conv.convClass;
        Method m;

        try {
          m = c.getMethod("isMine", new Class[]{NetcdfFile.class});
        } catch (NoSuchMethodException ex) {
          continue;
        }

        try {
          Boolean result = (Boolean) m.invoke(null, new Object[]{ds});
          if (result) {
            convClass = c;
            break;
          }
        } catch (Exception ex) {
          log.error("ERROR: Class " + c.getName() + " Exception invoking isMine method\n" + ex);
        }
      } // iterator
    } // convClass is null

    // no convention class found, use the default
    boolean usingDefault = (convClass == null);
    if (usingDefault)
      convClass = DefaultConvention.class;

    // get an instance of that class
    CoordSysBuilderIF builder;
    try {
      builder = (CoordSysBuilderIF) convClass.newInstance();
    } catch (Exception e) {
      log.error("failed on CoordSysBuilderIF for " + convClass.getName(), e);
      return null;
    }

    if (usingDefault) {
      builder.addUserAdvice("No CoordSysBuilder found - using Default Conventions.\n");
    }

    if (convName == null)
      builder.addUserAdvice("No 'Conventions' global attribute.\n");
    else
      builder.setConventionUsed(convClass.getName());

    return builder;
  }

  static private class Convention {
    String convName;
    Class convClass;
    ConventionNameOk match;

    Convention(String convName, Class convClass, ConventionNameOk match) {
      this.convName = convName;
      this.convClass = convClass;
      this.match = match;
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////

  protected String conventionName = _Coordinate.Convention; // default name of Convention, override in subclass
  protected List<VarProcess> varList = new ArrayList<VarProcess>(); // varProcess objects
  protected Map<Dimension, List<VarProcess>> coordVarMap = new HashMap<Dimension, List<VarProcess>>();
  protected Formatter parseInfo = new Formatter();
  protected Formatter userAdvice = new Formatter();

  protected boolean debug = false, showRejects = false;

  public void setConventionUsed(String convName) {
    this.conventionName = convName;
  }

  public String getConventionUsed() {
    return conventionName;
  }

  public void addUserAdvice(String advice) {
    userAdvice.format("%s", advice);
  }

  public String getParseInfo() {
    return parseInfo.toString();
  }

  public String getUserAdvice() {
    return userAdvice.toString();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // subclasses can override any of these routines

  public void augmentDataset(NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException { }

  /**
   * Identify what kind of AxisType the named variable is.
   * Only called for variables already identified as Coordinate Axes.
   * Default null - subclasses can override.
   *
   * @param ncDataset for this dataset
   * @param v         a variable alreaddy identified as a Coodinate Axis
   * @return AxisType or null if unknown.
   */
  protected AxisType getAxisType(NetcdfDataset ncDataset, VariableEnhanced v) {
    return null;
  }

  /**
   * Heres where the work is to identify coordinate axes and coordinate systems.
   *
   * @param ncDataset modify this dataset
   */
  public void buildCoordinateSystems(NetcdfDataset ncDataset) {
    // put status info into parseInfo that can be shown to someone trying to debug this process
    parseInfo.format("Parsing with Convention = %s\n", conventionName);

    // Bookeeping info for each variable is kept in the VarProcess inner class
    addVariables(ncDataset, ncDataset.getVariables(), varList);

    // identify which variables are coordinate axes
    findCoordinateAxes(ncDataset);
    // identify which variables are used to describe coordinate system
    findCoordinateSystems(ncDataset);
    // identify which variables are used to describe coordinate transforms
    findCoordinateTransforms(ncDataset);
    // turn Variables into CoordinateAxis objects
    makeCoordinateAxes(ncDataset);
    // make Coordinate Systems for all Coordinate Systems Variables
    makeCoordinateSystems(ncDataset);

    // assign explicit CoordinateSystem objects to variables
    assignCoordinateSystemsExplicit(ncDataset);

    // assign implicit CoordinateSystem objects to variables
    makeCoordinateSystemsImplicit(ncDataset);

    // optionally assign implicit CoordinateSystem objects to variables that dont have one yet
    if (useMaximalCoordSys)
      makeCoordinateSystemsMaximal(ncDataset);

    // make Coordinate Transforms
    makeCoordinateTransforms(ncDataset);

    // assign Coordinate Transforms
    assignCoordinateTransforms(ncDataset);

    if (debug) System.out.println("parseInfo = \n" + parseInfo.toString());
  }

  private void addVariables(NetcdfDataset ncDataset, List<Variable> varList, List<VarProcess> varProcessList) {
    for (Variable v : varList) {
      varProcessList.add(new VarProcess(ncDataset, v));

      if (v instanceof Structure) {
        List<Variable> nested = ((Structure) v).getVariables();
        addVariables(ncDataset, nested, varProcessList);
      }
    }
  }

  /**
   * Identify coordinate axes, set VarProcess.isCoordinateAxis = true.
   * Default is to look for those referenced by _CoordinateAxes attribute.
   * Note coordinate variables are already identified.
   *
   * @param ncDataset why
   */
  protected void findCoordinateAxes(NetcdfDataset ncDataset) {
    for (VarProcess vp : varList) {
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
      VarProcess ap = findVarProcess(vname);
      if (ap == null) {
        Group g = vp.v.getParentGroup();
        Variable v = g.findVariableOrInParent(vname);
        if (v != null)
          ap = findVarProcess(v.getName());
        else {
          parseInfo.format("***Cant find coordAxis %s referenced from var= %s\n", vname, vp.v.getName());
          userAdvice.format("***Cant find coordAxis %s referenced from var= %s\n", vname, vp.v.getName());
        }
      }

      if (ap != null) {
        if (!ap.isCoordinateAxis)
          parseInfo.format(" CoordinateAxis = %s added; referenced from var= %s\n", vname, vp.v.getName());
        ap.isCoordinateAxis = true;
      } else {
        parseInfo.format("***Cant find coordAxis %s referenced from var= %s\n", vname, vp.v.getName());
        userAdvice.format("***Cant find coordAxis %s referenced from var= %s\n", vname, vp.v.getName());
      }
    }
  }

  /**
   * Identify coordinate systems, set VarProcess.isCoordinateSystem = true.
   * Default is to look for those referenced by _CoordinateSystems attribute.
   *
   * @param ncDataset why
   */
  protected void findCoordinateSystems(NetcdfDataset ncDataset) {
    for (VarProcess vp : varList) {
      if (vp.coordSys != null) {
        StringTokenizer stoker = new StringTokenizer(vp.coordSys);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess(vname); // LOOK: full vs short name
          if (ap != null) {
            if (!ap.isCoordinateSystem)
              parseInfo.format(" CoordinateSystem = %s added; referenced from var= %s\n", vname, vp.v.getName());
            ap.isCoordinateSystem = true;
          } else {
            parseInfo.format("***Cant find coordSystem %s referenced from var= %s\n", vname, vp.v.getName());
            userAdvice.format("***Cant find coordSystem %s referenced from var= %s\n", vname, vp.v.getName());
          }
        }
      }
    }
  }

  /**
   * Identify coordinate transforms, set VarProcess.isCoordinateTransform = true.
   * Default is to look for those referenced by _CoordinateTransforms attribute
   * ( or has a _CoordinateTransformType attribute, done in VarProcess constructor)
   *
   * @param ncDataset why
   */
  protected void findCoordinateTransforms(NetcdfDataset ncDataset) {
    for (VarProcess vp : varList) {
      if (vp.coordTransforms != null) {
        StringTokenizer stoker = new StringTokenizer(vp.coordTransforms);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess(vname); // LOOK: full vs short name
          if (ap != null) {
            if (!ap.isCoordinateTransform)
              parseInfo.format(" CoordinateTransform = %s added; referenced from var= %s\n", vname, vp.v.getName());
            ap.isCoordinateTransform = true;
          } else {
            parseInfo.format("***Cant find CoordinateTransform %s referenced from var= %s\n", vname, vp.v.getName());
            userAdvice.format("***Cant find CoordinateTransform %s referenced from var= %s\n", vname, vp.v.getName());
          }
        }
      }
    }
  }

  /**
   * Take previously identified Coordinate Axis and Coordinate Variables and make them into a
   * CoordinateAxis. Uses the getAxisType() method to figure out the type, if not already set.
   *
   * @param ncDataset containing dataset
   */
  protected void makeCoordinateAxes(NetcdfDataset ncDataset) {
    for (VarProcess vp : varList) {
      if (vp.isCoordinateAxis || vp.isCoordinateVariable) {
        if (vp.axisType == null)
          vp.axisType = getAxisType(ncDataset, (VariableEnhanced) vp.v);
        if (vp.axisType == null) {
          userAdvice.format("Coordinate Axis %s does not have an assigned AxisType\n", vp.v.getName());
        }
        vp.makeIntoCoordinateAxis();
      }
    }
  }

  /**
   * Take all previously identified Coordinate Systems and create a
   * CoordinateSystem object.
   *
   * @param ncDataset why
   */
  protected void makeCoordinateSystems(NetcdfDataset ncDataset) {
    for (VarProcess vp : varList) {
      if (vp.isCoordinateSystem) {
        vp.makeCoordinateSystem();
      }
    }
  }

  /**
   * Assign explicit CoordinateSystem objects to variables.
   *
   * @param ncDataset why
   */
  protected void assignCoordinateSystemsExplicit(NetcdfDataset ncDataset) {

    // look for explicit references to coord sys variables
    for (VarProcess vp : varList) {
      if (vp.coordSys != null && !vp.isCoordinateTransform) {
        StringTokenizer stoker = new StringTokenizer(vp.coordSys);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess(vname); // LOOK: full vs short name
          if (ap == null) {
            parseInfo.format("***Cant find Coordinate System variable %s referenced from var= %s\n", vname, vp.v.getName());
            userAdvice.format("***Cant find Coordinate System variable %s referenced from var= %s\n", vname, vp.v.getName());
            continue;
          }
          if (ap.cs == null) {
            parseInfo.format("***Not a Coordinate System variable %s referenced from var= %s\n", vname, vp.v.getName());
            userAdvice.format("***Not a Coordinate System variable %s referenced from var= %s\n", vname, vp.v.getName());
            continue;
          }

          VariableEnhanced ve = (VariableEnhanced) vp.v;
          ve.addCoordinateSystem(ap.cs);
        }
      }
    }

    // look for explicit references from coord sys variables to data variables
    for (VarProcess csVar : varList) {
      if (!csVar.isCoordinateSystem || (csVar.coordSysFor == null))
        continue;

      // get list of dimensions from '_CoordinateSystemFor' attribute
      List<Dimension> dimList = new ArrayList<Dimension>(6);
      StringTokenizer stoker = new StringTokenizer(csVar.coordSysFor);
      while (stoker.hasMoreTokens()) {
        String dname = stoker.nextToken();
        Dimension dim = ncDataset.getRootGroup().findDimension(dname);
        if (dim == null) {
          parseInfo.format("***Cant find Dimension %s referenced from CoordSys var= %s%n", dname, csVar.v.getName());
          userAdvice.format("***Cant find Dimension %s referenced from CoordSys var= %s%n", dname, csVar.v.getName());
        } else
          dimList.add(dim);
      }

      // look for vars with those dimensions
      for (VarProcess vp : varList) {
        if (!vp.hasCoordinateSystem() && vp.isData() && (csVar.cs != null)) {
          VariableEnhanced ve = (VariableEnhanced) vp.v;
          if (CoordinateSystem.isSubset(dimList, vp.v.getDimensionsAll()) && CoordinateSystem.isSubset(vp.v.getDimensionsAll(), dimList))
            ve.addCoordinateSystem(csVar.cs);
        }
      }
    }

    // look for explicit listings of coordinate axes
    for (VarProcess vp : varList) {
      VariableEnhanced ve = (VariableEnhanced) vp.v;

      if (!vp.hasCoordinateSystem() && (vp.coordAxes != null) && vp.isData()) {
        List<CoordinateAxis> dataAxesList = getAxes(vp.coordAxes, vp.v.getName());
        if (dataAxesList.size() > 1) {
          String coordSysName = CoordinateSystem.makeName(dataAxesList);
          CoordinateSystem cs = ncDataset.findCoordinateSystem(coordSysName);
          if (cs != null) {
            ve.addCoordinateSystem(cs);
            parseInfo.format(" assigned explicit CoordSystem '%s' for var= %s\n", cs.getName(), vp.v.getName());
          } else {
            CoordinateSystem csnew = new CoordinateSystem(ncDataset, dataAxesList, null);
            ve.addCoordinateSystem(csnew);
            ncDataset.addCoordinateSystem(csnew);
            parseInfo.format(" created explicit CoordSystem '%s' for var= %s\n", csnew.getName(), vp.v.getName());
          }
        }
      }
    }
  }

  private List<CoordinateAxis> getAxes(String names, String varName) {
    List<CoordinateAxis> axesList = new ArrayList<CoordinateAxis>();
    StringTokenizer stoker = new StringTokenizer(names);
    while (stoker.hasMoreTokens()) {
      String vname = stoker.nextToken();
      VarProcess ap = findVarProcess(vname); // LOOK: full vs short name
      if (ap != null) {
        CoordinateAxis axis = ap.makeIntoCoordinateAxis();
        if (!axesList.contains(axis)) axesList.add(axis);
      } else {
        parseInfo.format("***Cant find Coordinate Axis %s referenced from var= %s\n", vname, varName);
        userAdvice.format("***Cant find Coordinate Axis %s referenced from var= %s\n", vname, varName);
      }
    }
    return axesList;
  }

  /**
   * Make implicit CoordinateSystem objects for variables that dont already have one, by using the
   * variables' list of coordinate axes, and any coordinateVariables for it. Must be at least 2 axes.
   *
   * @param ncDataset why
   */
  protected void makeCoordinateSystemsImplicit(NetcdfDataset ncDataset) {
    // do largest rank first
    //List<VarProcess> varsSorted = new ArrayList<VarProcess>(varList);
    //Collections.sort(varsSorted, new VarProcessSorter());

    for (VarProcess vp : varList) {
      if (!vp.hasCoordinateSystem() && vp.maybeData()) {
        List<CoordinateAxis> dataAxesList = vp.findCoordinateAxes(true);
        if (dataAxesList.size() < 2)
          continue;

        VariableEnhanced ve = (VariableEnhanced) vp.v;
        String csName = CoordinateSystem.makeName(dataAxesList);
        CoordinateSystem cs = ncDataset.findCoordinateSystem(csName);
        if ((cs != null) && cs.isComplete(vp.v)) { // DANGER WILL ROGERS!
          ve.addCoordinateSystem(cs);
          parseInfo.format(" assigned implicit CoordSystem '%s' for var= %s\n", cs.getName(), vp.v.getName());
        } else {
          CoordinateSystem csnew = new CoordinateSystem(ncDataset, dataAxesList, null);
          csnew.setImplicit(true);
          if (csnew.isComplete(vp.v)) {
            ve.addCoordinateSystem(csnew);
            ncDataset.addCoordinateSystem(csnew);
            parseInfo.format(" created implicit CoordSystem '%s' for var= %s\n", csnew.getName(), vp.v.getName());
          }
        }
      }
    }
  }

  private class VarProcessSorter implements Comparator<VarProcess> {
    public int compare(VarProcess o1, VarProcess o2) {
      return o2.v.getRank() - o1.v.getRank();
    }
  }

  /**
   * If a variable still doesnt have a coordinate system, use hueristics to try to find one that was probably
   * forgotten.
   * Look through all existing CS. create a subset of axes that fits the variable. Choose the one with highest rank.
   * It must have X,Y or lat,lon. If so, add it.
   *
   * @param ncDataset why
   */
  protected void makeCoordinateSystemsMaximal(NetcdfDataset ncDataset) {
    for (VarProcess vp : varList) {
      VariableEnhanced ve = (VariableEnhanced) vp.v;
      CoordinateSystem implicit = null;

      if (vp.hasCoordinateSystem() || !vp.isData()) continue;

      CoordinateSystem existing = null;
      if (ve.getCoordinateSystems().size() == 1) {
        existing = ve.getCoordinateSystems().get(0);
        if (!existing.isImplicit()) continue; // cant overrrride explicit
        if (existing.getRankRange() >= ve.getRank()) continue; // looks ok
        implicit = existing;
      }

      // look through all axes that fit
      List<CoordinateAxis> axisList = new ArrayList<CoordinateAxis>();
      List<CoordinateAxis> axes = ncDataset.getCoordinateAxes();
      for (CoordinateAxis axis : axes) {
        if (isCoordinateAxisForVariable(axis, ve))
          axisList.add(axis);
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
        parseInfo.format(" assigned maximal CoordSystem '%s' for var= %s\n", cs.getName(), ve.getName());
      } else {
        CoordinateSystem csnew = new CoordinateSystem(ncDataset, axisList, null);
        csnew.setImplicit(true);
        if (null != implicit) ve.removeCoordinateSystem(implicit);
        ve.addCoordinateSystem(csnew);
        ncDataset.addCoordinateSystem(csnew);
        parseInfo.format(" created maximal CoordSystem '%s' for var= %s\n", csnew.getName(), ve.getName());
      }

    }
  }

  /**
   * Does this axis "fit" this variable.
   * True if all of the dimensions in the axis also appear in the variable.
   * If char variable, last dimension is left out.
   *
   * @param axis check if this axis is ok for the given variable
   * @param v    the given variable
   * @return true if all of the dimensions in the axis also appear in the variable.
   */
  protected boolean isCoordinateAxisForVariable(Variable axis, VariableEnhanced v) {
    List<Dimension> varDims = v.getDimensionsAll();
    /* for (Dimension d : varDims) {
      if (!d.isShared())
        return false; // anon cant have coordinates
    } */ // LOOK

    // a CHAR variable must really be a STRING, so leave out the last (string length) dimension
    int checkDims = axis.getRank();
    if (axis.getDataType() == DataType.CHAR)
      checkDims--;

    for (int i = 0; i < checkDims; i++) {
      Dimension axisDim = axis.getDimension(i);

      if (!varDims.contains(axisDim)) {
        return false;
      }
    }
    return true;
  }

  protected boolean hasXY(List<CoordinateAxis> coordAxes) {
    boolean hasX = false, hasY = false, hasLat = false, hasLon = false;
    for (CoordinateAxis axis : coordAxes) {
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
   *
   * @param ncDataset why
   */
  protected void makeCoordinateTransforms(NetcdfDataset ncDataset) {
    for (VarProcess vp : varList) {
      if (vp.isCoordinateTransform && vp.ct == null) {
        vp.ct = CoordTransBuilder.makeCoordinateTransform(vp.ds, vp.v, parseInfo, userAdvice);
      }
    }
  }

  protected CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
    return CoordTransBuilder.makeCoordinateTransform(ds, ctv, parseInfo, userAdvice);
  }

  /**
   * Assign CoordinateTransform objects to Coordinate Systems.
   *
   * @param ncDataset why
   */
  protected void assignCoordinateTransforms(NetcdfDataset ncDataset) {

    // look for explicit transform assignments on the coordinate systems
    for (VarProcess vp : varList) {
      if (vp.isCoordinateSystem && vp.coordTransforms != null) {
        StringTokenizer stoker = new StringTokenizer(vp.coordTransforms);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess(vname); // LOOK: full vs short name
          if (ap != null) {
            if (ap.ct != null) {
              vp.addCoordinateTransform(ap.ct);
              parseInfo.format(" assign explicit coordTransform %s to CoordSys= %s\n", ap.ct, vp.cs);
            } else {
              parseInfo.format("***Cant find coordTransform in %s referenced from var= %s\n", vname, vp.v.getName());
              userAdvice.format("***Cant find coordTransform in %s referenced from var= %s\n", vname, vp.v.getName());
            }
          } else {
            parseInfo.format("***Cant find coordTransform variable= %s referenced from var= %s\n", vname, vp.v.getName());
            userAdvice.format("***Cant find coordTransform variable= %s referenced from var= %s\n", vname, vp.v.getName());
          }
        }
      }
    }

    // look for explicit coordSys assignments on the coordinate transforms
    for (VarProcess vp : varList) {
      if (vp.isCoordinateTransform && (vp.ct != null) && (vp.coordSys != null)) {
        StringTokenizer stoker = new StringTokenizer(vp.coordSys);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess vcs = findVarProcess(vname); // LOOK: full vs short name
          if (vcs == null) {
            parseInfo.format("***Cant find coordSystem variable= %s referenced from var= %s\n", vname, vp.v.getName());
            userAdvice.format("***Cant find coordSystem variable= %s referenced from var= %s\n", vname, vp.v.getName());
          } else {
            vcs.addCoordinateTransform(vp.ct);
            parseInfo.format("***assign explicit coordTransform %s to CoordSys=  %s\n", vp.ct, vp.cs);
          }
        }
      }
    }

    // look for coordAxes assignments on the coordinate transforms
    for (VarProcess vp : varList) {
      if (vp.isCoordinateTransform && (vp.ct != null) && (vp.coordAxes != null)) {
        List<CoordinateAxis> dataAxesList = vp.findCoordinateAxes(false);

        if (dataAxesList.size() > 0) {
          for (CoordinateSystem cs : ncDataset.getCoordinateSystems()) {
            if (cs.containsAxes(dataAxesList)) {
              cs.addCoordinateTransform(vp.ct);
              parseInfo.format("***assign (implicit coordAxes) coordTransform %s to CoordSys=  %s\n", vp.ct, cs);
            }
          }
        }
      }
    }

    // look for coordAxisType assignments on the coordinate transforms
    for (VarProcess vp : varList) {
      if (vp.isCoordinateTransform && (vp.ct != null) && (vp.coordAxisTypes != null)) {
        List<AxisType> axisTypesList = new ArrayList<AxisType>();
        StringTokenizer stoker = new StringTokenizer(vp.coordAxisTypes);
        while (stoker.hasMoreTokens()) {
          String name = stoker.nextToken();
          AxisType atype;
          if (null != (atype = AxisType.getType(name)))
            axisTypesList.add(atype);
        }
        if (axisTypesList.size() > 0) {
          for (CoordinateSystem cs : ncDataset.getCoordinateSystems()) {
            if (cs.containsAxisTypes(axisTypesList)) {
              cs.addCoordinateTransform(vp.ct);
              parseInfo.format("***assign (implicit coordAxisType) coordTransform %s to CoordSys=  %s\n", vp.ct, cs);
            }
          }
        }
      }
    }

  }

  protected VarProcess findVarProcess(String fullName) {
    if (fullName == null) return null;

    for (VarProcess vp : varList) {
      if (fullName.equals(vp.v.getName()))
        return vp;
    }
    return null;
  }

  protected VarProcess findCoordinateAxis(String name) {
    if (name == null) return null;

    for (VarProcess vp : varList) {
      if (name.equals(vp.v.getName()) && (vp.isCoordinateVariable || vp.isCoordinateAxis))
        return vp;
    }
    return null;
  }

  // track coordinate variables
  protected void addCoordinateVariable(Dimension dim, VarProcess vp) {
    List<VarProcess> list = coordVarMap.get(dim);
    if (list == null) {
      list = new ArrayList<VarProcess>();
      coordVarMap.put(dim, list);
    }
    if (!list.contains(vp))
      list.add(vp);
  }


  /**
   * Wrap each variable in the dataset with a VarProcess object.
   */
  public class VarProcess {
    public NetcdfDataset ds;
    public Variable v;

    // coord axes
    public boolean isCoordinateVariable;
    public boolean isCoordinateAxis;
    public AxisType axisType;
    public String coordAxes, coordSys, coordSysFor, coordVarAlias, positive, coordAxisTypes;
    public String coordinates;// CF : partial coordAxes
    public CoordinateAxis axis; // if its made into a Coordinate Axis, this is not null

    // coord systems
    public boolean isCoordinateSystem;
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
     *
     * @param ds dataset container
     * @param v  wrap this Variable
     */
    private VarProcess(NetcdfDataset ds, Variable v) {
      this.ds = ds;
      this.v = v;
      VariableEnhanced ve = (VariableEnhanced) v;
      isCoordinateVariable = v.isCoordinateVariable();
      if (isCoordinateVariable) {
        addCoordinateVariable(v.getDimension(0), this);
        parseInfo.format(" Coordinate Variable added = %s for dimension %s\n", v.getName(), v.getDimension(0));
      }

      Attribute att = v.findAttributeIgnoreCase(_Coordinate.AxisType);
      if (att != null) {
        String axisName = att.getStringValue();
        axisType = AxisType.getType(axisName);
        isCoordinateAxis = true;
        parseInfo.format(" Coordinate Axis added = %s type= %s\n", v.getName(), axisName);
      }

      coordVarAlias = ds.findAttValueIgnoreCase(v, _Coordinate.AliasForDimension, null);
      if (coordVarAlias != null) {
        coordVarAlias = coordVarAlias.trim();
        if (v.getRank() != 1) {
          parseInfo.format("**ERROR Coordinate Variable Alias %s has rank %d\n", v.getName(), v.getRank());
          userAdvice.format("**ERROR Coordinate Variable Alias %s has rank %d\n", v.getName(), v.getRank());
        } else {
          Dimension coordDim = ds.findDimension(coordVarAlias); // LOOK use groups
          Dimension vDim = v.getDimension(0);
          if (!coordDim.equals(vDim)) {
            parseInfo.format("**ERROR Coordinate Variable Alias %s names wrong dimension %s\n", v.getName(), coordVarAlias);
            userAdvice.format("**ERROR Coordinate Variable Alias %s names wrong dimension %s\n", v.getName(), coordVarAlias);
          } else {
            isCoordinateAxis = true;
            addCoordinateVariable(coordDim, this);
            parseInfo.format(" Coordinate Variable Alias added = %s for dimension= %s\n", v.getName(), coordVarAlias);
          }
        }
      }

      positive = ds.findAttValueIgnoreCase(v, _Coordinate.ZisPositive, null);
      if (positive == null)
        positive = ds.findAttValueIgnoreCase(v, "positive", null);
      else {
        isCoordinateAxis = true;
        positive = positive.trim();
        parseInfo.format(" Coordinate Axis added(from positive attribute ) = %s for dimension= %s\n", v.getName(), coordVarAlias);
      }

      coordAxes = ds.findAttValueIgnoreCase(v, _Coordinate.Axes, null);
      coordSys = ds.findAttValueIgnoreCase(v, _Coordinate.Systems, null);
      coordSysFor = ds.findAttValueIgnoreCase(v, _Coordinate.SystemFor, null);
      coordTransforms = ds.findAttValueIgnoreCase(v, _Coordinate.Transforms, null);
      isCoordinateSystem = (coordTransforms != null) || (coordSysFor != null);

      coordAxisTypes = ds.findAttValueIgnoreCase(v, _Coordinate.AxisTypes, null);
      coordTransformType = ds.findAttValueIgnoreCase(v, _Coordinate.TransformType, null);
      isCoordinateTransform = (coordTransformType != null) || (coordAxisTypes != null);

      /* WTF? JC 7/15/2010
      /* Old GRIB code basically has a bug in it for lat/lon coordinates
         String latLonCoordSys;
            :_CoordinateAxes = "time lat lon";
         this is a Coordinate System Variable getting misidentified as a data variable, because its doesnt qualify as a CoordinateSystem
           see http://www.unidata.ucar.edu/software/netcdf-java/reference/CoordinateAttributes.html
         but it must be a CoordinateSystem because the  _CoordinateAxes dont fit the variable.

         Problem:
         For some data, some axes are being constructed with anon dimensions, so that isCoordinateAxisForVariable is failing and
         so the data value is being tagged as a CoordinateSystem. Could insist that all variable fail ??
         */

      // this is the case of a Coordinate System with no references or coordinate transforms
      // see /testdata2/grid/grib/grib1/data/NOGAPS-Temp-Regional.grib
      if (!isCoordinateSystem && !isCoordinateTransform && !isCoordinateAxis && coordAxes != null) {
        // figure out if data or coordSys Variable
        StringTokenizer stoker = new StringTokenizer(coordAxes);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          Variable axis = ds.findVariable(vname);
          if ((axis != null) && !isCoordinateAxisForVariable(axis, ve))
            isCoordinateSystem = true;
        }
      } // */
    }

    // fakeroo
    public VarProcess(NetcdfDataset ds) {
      this.ds = ds;
    }

    public boolean isData() {
      return !isCoordinateVariable && !isCoordinateAxis && !isCoordinateSystem && !isCoordinateTransform;
    }

    public boolean maybeData() {
      return !isCoordinateVariable && !isCoordinateSystem && !isCoordinateTransform;
    }

    public boolean hasCoordinateSystem() {
      return ((VariableEnhanced) v).getCoordinateSystems().size() > 0;
    }

    public String toString() {
      return v.getShortName();
    }

    /**
     * Turn the variable into a coordinate axis, if not already. Add to the dataset, replacing variable if needed.
     *
     * @return variable as a coordinate axis
     */
    public CoordinateAxis makeIntoCoordinateAxis() {
      if (axis != null)
        return axis;

      // if not a CoordinateAxis, will turn into one
      v = axis = ds.addCoordinateAxis((VariableDS) v);

      /* if (v instanceof CoordinateAxis) {
        axis = (CoordinateAxis) v;
      } else {
        axis = ds.addCoordinateAxis((VariableDS) v);
        v = axis;
      } */

      if (axisType != null) {
        axis.setAxisType(axisType);
        axis.addAttribute(new Attribute(_Coordinate.AxisType, axisType.toString()));

        if (((axisType == AxisType.Height) || (axisType == AxisType.Pressure) || (axisType == AxisType.GeoZ)) &&
                (positive != null)) {
          axis.setPositive(positive);
          axis.addAttribute(new Attribute(_Coordinate.ZisPositive, positive));
        }
      }
      return axis;
    }

    /**
     * Create a Coordinate System object, using the list of coordinate axis names in the
     * (required) axes field.
     */
    public void makeCoordinateSystem() {

      // find referenced coordinate axes
      List<CoordinateAxis> axesList = new ArrayList<CoordinateAxis>();
      if (coordAxes != null) {
        StringTokenizer stoker = new StringTokenizer(coordAxes); // _CoordinateAxes attribute
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess(vname); // LOOK: full vs short name
          if (ap != null) {
            CoordinateAxis axis = ap.makeIntoCoordinateAxis();
            if (!axesList.contains(axis)) axesList.add(axis);
          } else {
            parseInfo.format(" Cant find axes %s for Coordinate System %s\n", vname, v.getName());
            userAdvice.format(" Cant find axes %s for Coordinate System %s\n", vname, v.getName());
          }
        }
      }

      if (axesList.size() == 0) {
        parseInfo.format(" No axes found for Coordinate System %s\n", v.getName());
        userAdvice.format(" No axes found for Coordinate System %s\n", v.getName());
        return;
      }

      // create the coordinate system
      cs = new CoordinateSystem(ds, axesList, null);
      ds.addCoordinateSystem(cs);

      parseInfo.format(" Made Coordinate System %s for ", cs.getName());
      v.getNameAndDimensions(parseInfo, true, false);
      parseInfo.format(" from %s\n", coordAxes);
    }

    /**
     * Create a list of coordinate axes for this data variable.
     * Use the list of names in axes or coordinates field.
     *
     * @param addCoordVariables if true, add any coordinate variables that are missing.
     * @return list of coordinate axes for this data variable.
     */
    public List<CoordinateAxis> findCoordinateAxes(boolean addCoordVariables) {
      List<CoordinateAxis> axesList = new ArrayList<CoordinateAxis>();

      if (coordAxes != null) { // explicit axes
        StringTokenizer stoker = new StringTokenizer(coordAxes);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess(vname); // LOOK: full vs short name
          if (ap != null) {
            CoordinateAxis axis = ap.makeIntoCoordinateAxis();
            if (!axesList.contains(axis)) axesList.add(axis);
          }
        }
      } else if (coordinates != null) { // CF partial listing of axes
        StringTokenizer stoker = new StringTokenizer(coordinates);
        while (stoker.hasMoreTokens()) {
          String vname = stoker.nextToken();
          VarProcess ap = findVarProcess(vname); // LOOK: full vs short name
          if (ap != null) {
            CoordinateAxis axis = ap.makeIntoCoordinateAxis(); // LOOK check if its legal
            if (!axesList.contains(axis)) axesList.add(axis);
          }
        }
      }

      if (addCoordVariables) {
        for (Dimension d : v.getDimensions()) {
          List<VarProcess> coordVars = coordVarMap.get(d);
          if (coordVars == null) continue;
          for (VarProcess vp : coordVars) {
            CoordinateAxis axis = vp.makeIntoCoordinateAxis();
            if (!axesList.contains(axis)) axesList.add(axis);
          }
        }
      }

      return axesList;
    }

   void addCoordinateTransform(CoordinateTransform ct) {
     if (cs == null) {
       parseInfo.format("  %s: no CoordinateSystem for CoordinateTransformVariable: %s\n", v.getName(), ct.getName());
       return;
     }
     cs.addCoordinateTransform(ct);
   }

  }

  protected VariableDS makeCoordinateTransformVariable(NetcdfDataset ds, CoordinateTransform ct) {
    VariableDS v = CoordTransBuilder.makeDummyTransformVariable(ds, ct);
    parseInfo.format("  made CoordinateTransformVariable: %s\n", ct.getName());
    return v;
  }

  /**
   * @deprecated use CoordTransBuilder.makeDummyTransformVariable
   */
  static public VariableDS makeDummyTransformVariable(NetcdfDataset ds, CoordinateTransform ct) {
    return CoordTransBuilder.makeDummyTransformVariable(ds, ct);
  }

}