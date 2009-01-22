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
package ucar.nc2.ft.point.standard;

import ucar.nc2.*;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.point.standard.plug.*;
import ucar.nc2.dataset.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.AxisType;

import java.util.*;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Analyzes the coordinate systems of a dataset to try to identify the Feature Type and the
 *   structure of the data.
 * Used by PointDatasetStandardFactory.
 *
 * @author caron
 * @since Mar 20, 2008
 */
public class TableAnalyzer {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TableAnalyzer.class);

  static private List<Configurator> conventionList = new ArrayList<Configurator>();
  static private boolean userMode = false;
  static private boolean debug = true;

  // search in the order added
  static {
    registerAnalyzer("CF-1.", CFpointObs.class, new ConventionNameOk() {
      public boolean isMatch(String convName, String wantName) {
        return convName.startsWith(wantName) && !convName.equals("CF-1.0"); // throw 1.0 to default analyser
      }
    });
    registerAnalyzer("Unidata Observation Dataset v1.0", UnidataPointObs.class, null);

    registerAnalyzer("FslWindProfiler", FslWindProfiler.class, null);
    registerAnalyzer("IRIDL", Iridl.class, null);
    registerAnalyzer("MADIS surface observations, v1.0", Madis.class, null);
    registerAnalyzer("Ndbc", Ndbc.class, null);
    registerAnalyzer("Unidata Point Feature v1.0", UnidataPointFeature.class, null);

    // further calls to registerConvention are by the user
    userMode = true;
  }

  static public void registerAnalyzer(String conventionName, Class c, ConventionNameOk match) {
    if (!(TableConfigurer.class.isAssignableFrom(c)))
      throw new IllegalArgumentException("Class " + c.getName() + " must implement TableConfigurer");

    // fail fast - check newInstance works
    TableConfigurer tc;
    try {
      tc = (TableConfigurer) c.newInstance();
    } catch (InstantiationException e) {
      throw new IllegalArgumentException("TableConfigurer Class " + c.getName() + " cannot instantiate, probably need default Constructor");
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("TableConfigurer Class " + c.getName() + " is not accessible");
    }

    Configurator anal = new Configurator(conventionName, c, tc, match);
    if (userMode) // user stuff gets put at top
      conventionList.add(0, anal);
    else
      conventionList.add(anal);
  }

  static private interface ConventionNameOk {
    boolean isMatch(String convName, String wantName);
  }

  static private class Configurator {
    String convName;
    Class confClass;
    TableConfigurer confInstance;
    ConventionNameOk match;

    Configurator(String convName, Class confClass, TableConfigurer confInstance, ConventionNameOk match) {
      this.convName = convName;
      this.confClass = confClass;
      this.confInstance = confInstance;
      this.match = match;
    }
  }

  static private Configurator matchConfigurator(String convName) {
    for (Configurator anal : conventionList) {
      if ((anal.match == null) && anal.convName.equalsIgnoreCase(convName)) return anal;
      if ((anal.match != null) && anal.match.isMatch(convName, anal.convName)) return anal;
    }
    return null;
  }

  /**
   * Find a TableConfigurer for this dataset, if there is one.
   *
   * @param wantFeatureType want this FeatureType
   * @param ds for this dataset
   * @return TableConfigurer or null if not found
   * @throws IOException on read error
   */
  static public TableConfigurer getTableConfigurer(FeatureType wantFeatureType, NetcdfDataset ds) throws IOException {
    String convUsed = null;

    // look for the Conventions attribute
    String convName = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (convName == null)
      convName = ds.findAttValueIgnoreCase(null, "Convention", null);

    // now look for TableConfigurer using that Convention
    Configurator anal = null;
    if (convName != null) {
      convName = convName.trim();

      // look for Convention parsing class
      anal = matchConfigurator(convName);
      if (anal != null) {
        convUsed = convName;
        if (debug) System.out.println("  TableConfigurer found using convName "+convName);
      }

      // now look for comma or semicolon or / delimited list
      if (anal == null) {
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

        if (names.size() > 0) {
          // search the registered conventions, in order
          for (Configurator conv : conventionList) {
            for (String name : names) {
              if (name.equalsIgnoreCase(conv.convName)) {
                anal = conv;
                convUsed = name;
                if (debug) System.out.println("  TableConfigurer found using convName "+convName);
              }
            }
            if (anal != null) break;
          }
        }
      }
    }

    // look for ones that dont use Convention attribute, in order added.
    // call method isMine() using reflection.
    if (anal == null) {
      for (Configurator conv : conventionList) {
        Class c = conv.confClass;
        Method isMineMethod;

        try {
          isMineMethod = c.getMethod("isMine", new Class[]{FeatureType.class, NetcdfDataset.class});
        } catch (NoSuchMethodException ex) {
          continue;
        }

        try {
          Boolean result = (Boolean) isMineMethod.invoke(conv.confInstance, wantFeatureType, ds);
          if (debug) System.out.println("  TableConfigurer.isMine "+c.getName()+ " result = " + result);
          if (result) {
            anal = conv;
            convUsed = conv.convName;
            break;
          }
        } catch (Exception ex) {
          System.out.println("ERROR: Class " + c.getName() + " Exception invoking isMine method\n" + ex);
        }
      }
    }

    // Instantiate a new TableConfigurer object
    TableConfigurer tc = null;
    if (anal != null) {
      try {
        tc = (TableConfigurer) anal.confClass.newInstance();
        tc.setConvName( convName);
        tc.setConvUsed( convUsed);
      } catch (InstantiationException e) {
        log.error("TableConfigurer create failed", e);
      } catch (IllegalAccessException e) {
        log.error("TableConfigurer create failed", e);
      }
    }

    return tc;
  }

  /**
   * Create a TableAnalyser for this dataset with the given TableConfigurer
   *
   * @param tc TableConfigurer, may be null.
   * @param wantFeatureType want this FeatureType
   * @param ds for this dataset
   * @return TableAnalyser
   * @throws IOException on read error
   */
  static public TableAnalyzer factory(TableConfigurer tc, FeatureType wantFeatureType, NetcdfDataset ds) throws IOException {

    // Create a TableAnalyzer with this TableConfigurer (may be null)
    TableAnalyzer analyzer = new TableAnalyzer(ds, tc);

    if (tc != null) {
      if (tc.getConvName() == null)
        analyzer.userAdvice.format(" No 'Conventions' global attribute.\n");
      else
        analyzer.userAdvice.format(" Conventions global attribute = %s %n", tc.getConvName());

      // add the convention name used
      if (tc.getConvUsed() != null) {
        analyzer.setConventionUsed(tc.getConvUsed());
        if (!tc.getConvUsed().equals(tc.getConvName()))
          analyzer.userAdvice.format(" TableConfigurer used = "+tc.getConvUsed()+".\n");
      }

    } else {
      analyzer.userAdvice.format(" No TableConfigurer found, using default analysis.\n");

    }

    // construct the nested table object
    analyzer.analyze(wantFeatureType);
    return analyzer;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  protected TableConfigurer tc;
  protected NetcdfDataset ds;
  protected Map<String, TableConfig> tableFind = new HashMap<String, TableConfig>();
  protected Set<TableConfig> tableSet = new HashSet<TableConfig>();
  protected List<TableConfig.JoinConfig> joins = new ArrayList<TableConfig.JoinConfig>();
  protected List<NestedTable> leaves = new ArrayList<NestedTable>();
  protected FeatureType ft;

  protected TableAnalyzer(NetcdfDataset ds, TableConfigurer tc) {
    this.tc = tc;
    this.ds = ds;

    if (tc == null)
      userAdvice.format("Using default TableConfigurer.\n");
  }

  public List<NestedTable> getFlatTables() {
    return leaves;
  }

  public boolean featureTypeOk(FeatureType ftype) {
    for (NestedTable nt : leaves) {
      if (nt.hasCoords() && FeatureDatasetFactoryManager.featureTypeOk(ftype, nt.getFeatureType()))
        return true;
    }
    return false;
  }

  // for debugging messages
  public FeatureType getFirstFeatureType() {
    for (NestedTable nt : leaves) {
      if (nt.hasCoords())
        return nt.getFeatureType();
    }
    return null;
  }

  public NetcdfDataset getNetcdfDataset() {
    return ds;
  }

  protected Formatter userAdvice = new Formatter();
  protected Formatter errlog = new Formatter();

  public String getUserAdvice() {
    return userAdvice.toString();
  }

  protected String conventionName;

  protected void setConventionUsed(String convName) {
    this.conventionName = convName;
  }

  /////////////////////////////////////////////////////////

  /**
   * Make a NestedTable object for the dataset.
   * @param wantFeatureType want this FeatureType
   * @throws IOException on read error
   */
  protected void analyze(FeatureType wantFeatureType) throws IOException {
    // for netcdf-3 files, convert record dimension to structure
    // LOOK may be problems when served via opendap
    boolean structAdded = (Boolean) ds.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    if (tc == null) {
      makeTablesDefault(structAdded);
      makeNestedTables();

    } else {
      TableConfig config = tc.getConfig(wantFeatureType, ds, errlog);
      if (config != null)
        addTableRecurse( config); // kinda stupid
    }

    makeLeaves();
  }

  // no TableConfig was passed in - gotta wing it
  protected void makeTablesDefault(boolean structAdded) throws IOException {

    // make Structures into a table
    List<Variable> vars = new ArrayList<Variable>(ds.getVariables());
    Iterator<Variable> iter = vars.iterator();
    while (iter.hasNext()) {
      Variable v = iter.next();
      if (v instanceof Structure) {  // handles Sequences too
        TableConfig st = new TableConfig(Table.Type.Structure, v.getShortName());
        CoordSysEvaluator.findCoords(st, ds);

        addTable(st);
        iter.remove();
        findNestedStructures((Structure) v, st); // look for nested structures

      } else if (structAdded && v.isUnlimited()) {
        iter.remove();
      }
    }

    if (tableSet.size() > 0) return;

    // look at dimensions that lat, lon, time coordinates use
    Set<Dimension> dimSet = new HashSet<Dimension>(10);
    for (CoordinateAxis axis : ds.getCoordinateAxes()) {
      if ((axis.getAxisType() == AxisType.Lat) || (axis.getAxisType() == AxisType.Lon)|| (axis.getAxisType() == AxisType.Time))
        for (Dimension dim : axis.getDimensions())
          dimSet.add(dim);
    }

    if (dimSet.size() == 1) {
      Dimension obsDim = (Dimension) dimSet.toArray()[0];
      TableConfig st = new TableConfig(Table.Type.PseudoStructure, obsDim.getName());
      st.dim = obsDim;
      CoordSysEvaluator.findCoords(st, ds);

      addTable( st);
    }

  }

  protected void findNestedStructures(Structure s, TableConfig parent) {
    for (Variable v : s.getVariables()) {
      if (v instanceof Structure) {  // handles Sequences too
        TableConfig nestedTable = new TableConfig(Table.Type.Structure, v.getShortName());
        addTable(nestedTable);
        parent.addChild(nestedTable);

        // LOOK why not add the join(parent,child) here ?
        nestedTable.join = new TableConfig.JoinConfig(Join.Type.NestedStructure);
        joins.add(nestedTable.join);

        findNestedStructures((Structure) v, nestedTable); // look for nested structures
      }
    }
  }


  protected void addTable(TableConfig t) {
    tableFind.put(t.name, t);
    if (t.dim != null)
      tableFind.put(t.dim.getName(), t);
    tableSet.add(t);
  }

  protected void addTableRecurse(TableConfig t) {
    addTable(t);
    if (t.children != null) {
      for (TableConfig child : t.children)
        addTableRecurse(child);
    }
  }

  protected List<Variable> getStructVars(List<Variable> vars, Dimension dim) {
    List<Variable> structVars = new ArrayList<Variable>();
    for (Variable v : vars) {
      if (v.isScalar()) continue;
      if (v.getDimension(0) == dim) {
        structVars.add(v);
      }
    }
    return structVars;
  }

  protected void makeNestedTables() {
    // We search among all the possible Tables in a dataset for joins, and coordinate
    // variables. Based on those, we form "interesting" sets and make them into NestedTables.

    /* link the tables together with joins
    for (TableConfig.JoinConfig join : joins) {
      NestedTable.Table parent = join.parent;
      NestedTable.Table child = join.child;

      if (child.parent != null) throw new IllegalStateException("Multiple parents");
      child.parent = parent;
      child.join = join;

      if (parent.children == null) parent.children = new ArrayList<Join>();
      parent.children.add(join);
    } */
  }

  protected void makeLeaves() {

    // find the leaves
    for (TableConfig config : tableSet) {
      if (config.children == null) { // its a leaf
        NestedTable flatTable = new NestedTable(ds, config, errlog);
        leaves.add(flatTable);
      }
    }
  }

  /////////////////////////////////////////////////////
  // utilities

  protected Variable findVariableWithAttribute(List<Variable> list, String name, String value) {
    for (Variable v : list) {
      if (ds.findAttValueIgnoreCase(v, name, "").equals(value))
        return v;
      if (v instanceof Structure) {
        Variable vv = findVariableWithAttribute(((Structure) v).getVariables(), name, value);
        if (vv != null) return vv;
      }
    }
    return null;
  }

  /////////////////////////////////////////////////////
  // track station info

  protected StationInfo stationInfo = new StationInfo();

  StationInfo getStationInfo() {
    return stationInfo;
  }

  public class StationInfo {
    public String stationId, stationDesc, stationNpts;
    public int nstations;
    public String latName, lonName, elevName;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  public void showCoordSys(java.util.Formatter sf) {
    sf.format("\nCoordinate Systems\n");
    for (CoordinateSystem cs : ds.getCoordinateSystems()) {
      sf.format(" %s\n", cs);
    }
  }

  public void showCoordAxes(java.util.Formatter sf) {
    sf.format("\nAxes\n");
    for (CoordinateAxis axis : ds.getCoordinateAxes()) {
      sf.format(" %s %s\n", axis.getAxisType(), axis.getNameAndDimensions());
    }
  }

  /* public void showTables(java.util.Formatter sf) {
    sf.format("\nTables\n");
    for (NestedTable.Table t : tableSet)
      sf.format(" %s\n", t);

    sf.format("\nJoins\n");
    for (Join j : joins)
      sf.format(" %s\n", j);
  } */

  public void showNestedTables(java.util.Formatter sf) {
    for (NestedTable nt : leaves) {
      nt.show(sf);
    }
  }

  public String getImplementationName() {
    return (tc != null) ? tc.getClass().getSimpleName() : "defaultAnalyser";
  }

  public void getDetailInfo(java.util.Formatter sf) {
    sf.format("\nTableAnalyzer on Dataset %s\n", ds.getLocation());
    if (tc != null) sf.format(" TableAnalyser = %s\n", tc.getClass().getName());
    //showCoordSys(sf);
    //showCoordAxes(sf);
    //showTables(sf);
    showNestedTables(sf);
    String errlogS = errlog.toString();
    if (errlogS.length() > 0)
      sf.format("\n Errlog=\n%s",errlogS);
    String userAdviceS = userAdvice.toString();
    if (userAdviceS.length() > 0)
      sf.format("\n userAdvice=\n%s",userAdviceS);
  }

  static void doit(String filename) throws IOException {
    System.out.println(filename);
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    TableAnalyzer csa = TableAnalyzer.factory(null, null, ncd);
    csa.getDetailInfo(new Formatter(System.out));
    System.out.println("\n-----------------");
  }

  static public void main(String args[]) throws IOException {
    /* doit("C:/data/dt2/station/Surface_METAR_20080205_0000.nc");
    doit("C:/data/bufr/edition3/idd/profiler/PROFILER_3.bufr");
    doit("C:/data/bufr/edition3/idd/profiler/PROFILER_2.bufr");
    doit("C:/data/profile/PROFILER_wind_01hr_20080410_2300.nc"); */
    //doit("C:/data/test/20070301.nc");
    //doit("C:/data/dt2/profile/PROFILER_3.bufr");

    //doit("C:/data/dt2/station/ndbc.nc");
    //doit("C:/data/dt2/station/madis2.sao");
    // doit("C:/data/metars/Surface_METAR_20070326_0000.nc");  // ok
    //doit("C:/data/dt2/station/Sean_multidim_20070301.nc"); // ok
    doit("C:/data/dt2/profile/PROFILER_3.bufr");

    //doit("C:/data/profile/PROFILER_wind_01hr_20080410_2300.nc");
    //doit("C:/data/cadis/tempting");
  }
}
