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
import ucar.nc2.ft.point.standard.plug.UnidataPointFeatureAnalyzer;
import ucar.nc2.ft.point.standard.plug.FslWindProfiler;
import ucar.nc2.ft.point.standard.plug.UnidataPointObsAnalyzer;
import ucar.nc2.dataset.*;
import ucar.nc2.constants.FeatureType;

import java.util.*;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Analyzes the coordinate systems of a dataset to try to identify the Feature Type.
 *
 * @author caron
 * @since Mar 20, 2008
 */
public class TableAnalyzer {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TableAnalyzer.class);

  static private Map<String, Class> conventionHash = new HashMap<String, Class>();
  static private List<Analyzer> conventionList = new ArrayList<Analyzer>();
  static private boolean userMode = false;

    // search in the order added
  static {
    registerAnalyzer("Unidata Point Feature v1.0", UnidataPointFeatureAnalyzer.class);
    registerAnalyzer("FslWindProfiler", FslWindProfiler.class);
    registerAnalyzer("Unidata Observation Dataset v1.0", UnidataPointObsAnalyzer.class);

    // further calls to registerConvention are by the user
    userMode = true;
  }

  static public void registerAnalyzer(String conventionName, Class c) {
    if (!(TableAnalyzer.class.isAssignableFrom(c)))
      throw new IllegalArgumentException("Class " + c.getName() + " must extend CoordSysAnalyzer");

    // fail fast - check newInstance works
    try {
      c.newInstance();
    } catch (InstantiationException e) {
      throw new IllegalArgumentException("CoordSysAnalyzer Class " + c.getName() + " cannot instantiate, probably need default Constructor");
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("CoordSysAnalyzer Class " + c.getName() + " is not accessible");
    }

    // user stuff gets put at top
    if (userMode)
      conventionList.add(0, new Analyzer(conventionName, c));
    else
      conventionList.add(new Analyzer(conventionName, c));

    // user stuff will override here
    conventionHash.put(conventionName, c);
  }

  static private class Analyzer {
    String convName;
    Class convClass;

    Analyzer(String convName, Class convClass) {
      this.convName = convName;
      this.convClass = convClass;
    }
  }

  static public TableAnalyzer factory(FeatureType ftype, NetcdfDataset ds) throws IOException {

    // look for the Conventions attribute
    String convName = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (convName == null)
      convName = ds.findAttValueIgnoreCase(null, "Convention", null);

    // now look for Convention parsing class
    Class convClass = null;
    if (convName != null) {
      convName = convName.trim();

      // look for Convention parsing class
      convClass = conventionHash.get(convName);

      // now look for comma or semicolon or / delimited list
      if (convClass == null) {
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
          for (Analyzer conv : conventionList) {
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
    // call static method isMine() using reflection.
    if (convClass == null) {
      convName = null;
      for (Analyzer conv : conventionList) {
        Class c = conv.convClass;
        Method m;

        try {
          m = c.getMethod("isMine", new Class[]{NetcdfDataset.class});
        } catch (NoSuchMethodException ex) {
          continue;
        }

        try {
          Boolean result = (Boolean) m.invoke(null, ds);
          if (result) {
            convClass = c;
            break;
          }
        } catch (Exception ex) {
          System.out.println("ERROR: Class " + c.getName() + " Exception invoking isMine method\n" + ex);
        }
      }
    }

    // no convention class found, use CoordSysAnalyzer as the default
    boolean usingDefault = (convClass == null);
    if (usingDefault)
      convClass = TableAnalyzer.class;

    // get an instance of the class
    TableAnalyzer analyzer;
    try {
      analyzer = (TableAnalyzer) convClass.newInstance();
    } catch (InstantiationException e) {
      log.error("CoordSysAnalyzer create failed", e);
      return null;
    } catch (IllegalAccessException e) {
      log.error("CoordSysAnalyzer create failed", e);
      return null;
    }

    if (usingDefault) {
      analyzer.userAdvice.format("No CoordSysAnalyzer found - using default.\n");
    }

    // add the coord systems
    if (convName != null)
      analyzer.setConventionUsed(convName);
    else
      analyzer.userAdvice.format("No 'Convention' global attribute.\n");

    analyzer.setDataset(ds);
    analyzer.analyze();
    return analyzer;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  protected NetcdfDataset ds;
  protected Map<String, NestedTable.Table> tableFind = new HashMap<String, NestedTable.Table>();
  protected Set<NestedTable.Table> tableSet = new HashSet<NestedTable.Table>();
  protected List<NestedTable.Join> joins = new ArrayList<NestedTable.Join>();
  protected List<NestedTable> leaves = new ArrayList<NestedTable>();
  protected FeatureType ft;

  private void setDataset(NetcdfDataset ds) {
    this.ds = ds;
  }

  public List<NestedTable> getFlatTables() {
    return leaves;
  }

  public boolean featureTypeOk(FeatureType ftype) {
    for (NestedTable nt : leaves) {
      if (FeatureDatasetFactoryManager.featureTypeOk(ftype, nt.getFeatureType()))
        return true;
    }
    return false;
  }

  public NetcdfDataset getNetcdfDataset() {
    return ds;
  }

  protected Formatter userAdvice = new Formatter();
  public String getUserAdvice() {
    return userAdvice.toString();
  }

  protected String conventionName;
  protected void setConventionUsed(String convName) {
    this.conventionName = convName;
  }

  /////////////////////////////////////////////////////////

  protected void analyze() throws IOException {
    annotateDataset();
    makeTables();
    makeJoins();
    makeNestedTables();
  }

  protected void annotateDataset() { }

  protected void makeTables() throws IOException {

    // for netcdf-3 files, convert record dimension to structure
    boolean structAdded = (Boolean) ds.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    // make Structures into a table
    List<NestedTable.Table> structs = new ArrayList<NestedTable.Table>();
    List<Variable> vars = new ArrayList<Variable>(ds.getVariables());
    Iterator<Variable> iter = vars.iterator();
    while (iter.hasNext()) {
      Variable v = iter.next();
      if (v instanceof Structure) {  // handles Sequences too
        NestedTable.Table st = new NestedTable.Table((Structure) v);
        addTable(st);
        iter.remove();
        structs.add(st);

      } else if (structAdded && v.isUnlimited()) {
        iter.remove();
      }
    }

    // look for top level "psuedo structures"
    List<Dimension> dims = ds.getDimensions();
    for (Dimension dim : dims) {
      List<Variable> svars = getStructVars(vars, dim);
      if (svars.size() > 0) {
        addTable( new NestedTable.Table(ds, svars, dim)); // candidate
      }
    }

    // look for nested structures
    findNestedStructures( structs);
  }

  protected void addTable(NestedTable.Table t) {
    tableFind.put(t.getName(), t);
    if (t.dim != null)
      tableFind.put(t.dim.getName(), t);
    tableSet.add(t);
  }

  protected void findNestedStructures(List<NestedTable.Table> structs) {

    List<NestedTable.Table> nestedStructs = new ArrayList<NestedTable.Table>();
    for (NestedTable.Table structTable : structs) {
      Structure s = structTable.struct;
      for (Variable v : s.getVariables()) {
        if (v instanceof Structure) {  // handles Sequences too
          NestedTable.Table nestedTable = new NestedTable.Table((Structure) v);
          addTable(nestedTable);
          nestedStructs.add(nestedTable);

          NestedTable.Join join = new NestedTable.Join(NestedTable.JoinType.NestedStructure);
          join.setTables(structTable, nestedTable);
          joins.add(join);
        }
      }
    }

    // recurse
    if (nestedStructs.size() > 0)
      findNestedStructures(nestedStructs);
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

  protected void makeJoins() throws IOException { }

  protected void makeNestedTables() {

    // link the tables together with joins
    for (NestedTable.Join join : joins) {
      NestedTable.Table parent = join.fromTable;
      NestedTable.Table child = join.toTable;

      if (child.parent != null) throw new IllegalStateException("Multiple parents");
      child.parent = parent;
      child.join = join;

      if (parent.children == null) parent.children = new ArrayList<NestedTable.Join>();
      parent.children.add(join);
    }

    // find the leaves
    for (NestedTable.Table table : tableSet) {
      if (table.children == null) { // its a leaf
        NestedTable flatTable = new NestedTable(this, table);
        if (flatTable.isOk()) { // it has lat,lon,time coords
          leaves.add(flatTable);
        }
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

  public void showTables(java.util.Formatter sf) {
    sf.format("\nTables\n");
    for (NestedTable.Table t : tableSet)
      sf.format(" %s\n", t);

    sf.format("\nJoins\n");
    for (NestedTable.Join j : joins)
      sf.format(" %s\n", j);
  }

  public void showNestedTables(java.util.Formatter sf) {
    for (NestedTable nt : leaves) {
      nt.show(sf);
    }
  }

  public void getDetailInfo(java.util.Formatter sf) {
    sf.format("\nCoordSysAnalyzer on Dataset %s\n", ds.getLocation());
    //showCoordSys(sf);
    //showCoordAxes(sf);
    //showTables(sf);
    showNestedTables(sf);
  }

  static void doit(String filename) throws IOException {
    System.out.println(filename);
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    TableAnalyzer csa = TableAnalyzer.factory(null, ncd);
    csa.getDetailInfo(new Formatter(System.out));
    System.out.println("-----------------");
  }

  static public void main(String args[]) throws IOException {
    /* doit("C:/data/dt2/station/Surface_METAR_20080205_0000.nc");
    doit("C:/data/bufr/edition3/idd/profiler/PROFILER_3.bufr");
    doit("C:/data/bufr/edition3/idd/profiler/PROFILER_2.bufr");
    doit("C:/data/profile/PROFILER_wind_01hr_20080410_2300.nc"); */
    //doit("C:/data/test/20070301.nc");
    doit("C:/data/dt2/profile/PROFILER_3.bufr");

    //doit("C:/data/dt2/station/ndbc.nc");
    //doit("C:/data/dt2/station/UnidataMultidim.ncml");
    //doit("R:/testdata/point/bufr/data/050391800.iupt01");
    //doit("C:/data/rotatedPole/eu.mn.std.fc.d00z20070820.ncml");
    //doit("C:/data/cadis/tempting");
  }
}
