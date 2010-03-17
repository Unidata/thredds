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

import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;

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
  static private boolean debug = false;

  // search in the order added
  static {

    registerAnalyzer("CF-1.", CFpointObs.class, new ConventionNameOk() {
      public boolean isMatch(String convName, String wantName) {
        return convName.startsWith(wantName); //  && !convName.equals("CF-1.0"); // throw 1.0 to default analyser
      }
    });
    registerAnalyzer("BUFR/CDM", BufrCdm.class, null);
    registerAnalyzer("GEMPAK/CDM", GempakCdm.class, null);
    registerAnalyzer("Unidata Observation Dataset v1.0", UnidataPointObs.class, null);

    registerAnalyzer("Cosmic", Cosmic.class, null);
    registerAnalyzer("Jason", Jason.class, null);
    registerAnalyzer("FslWindProfiler", FslWindProfiler.class, null);
    registerAnalyzer("MADIS-ACARS", MadisAcars.class, null); // must be before Madis
    registerAnalyzer("MADIS surface observations, v1.0", Madis.class, null);  // must be before FslRaob
    registerAnalyzer("FSL Raobs", FslRaob.class, null);  // must be before FslRaob

    registerAnalyzer("IRIDL", Iridl.class, null);
    registerAnalyzer("Ndbc", Ndbc.class, null);
    registerAnalyzer("Suomi-Station-CDM", Suomi.class, null);
    registerAnalyzer("BuoyShip-NetCDF", BuoyShipSynop.class, null);
    registerAnalyzer("NCAR-RAF/nimbus", RafNimbus.class, null);
    registerAnalyzer("NLDN-CDM", Nldn.class, null);

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
  private TableConfigurer tc;
  private NetcdfDataset ds;
  private Map<String, TableConfig> tableFind = new HashMap<String, TableConfig>();
  private Set<TableConfig> tableSet = new HashSet<TableConfig>();
  private List<NestedTable> leaves = new ArrayList<NestedTable>();
  private FeatureType ft;
  private TableConfig configResult;

  private TableAnalyzer(NetcdfDataset ds, TableConfigurer tc) {
    this.tc = tc;
    this.ds = ds;

    if (tc == null)
      userAdvice.format("Using default TableConfigurer.\n");
  }

  public List<NestedTable> getFlatTables() {
    return leaves;
  }

  public boolean featureTypeOk(FeatureType ftype, Formatter errlog) {
    for (NestedTable nt : leaves) {
      if (!nt.hasCoords())
        errlog.format("Table %s featureType %s: lat/lon/time coord not found%n", nt.getName(), nt.getFeatureType());

      if (!FeatureDatasetFactoryManager.featureTypeOk(ftype, nt.getFeatureType()))
        errlog.format("Table %s featureType %s doesnt match desired type %s%n", nt.getName(), nt.getFeatureType(), ftype);

      if (nt.hasCoords() && FeatureDatasetFactoryManager.featureTypeOk(ftype, nt.getFeatureType()))
        return true;
    }

    return false;
  }

  public String getName() {
    if (tc != null) return tc.getClass().getName();
    return "Default";
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

  private Formatter userAdvice = new Formatter();
  private Formatter errlog = new Formatter();

  public String getUserAdvice() {
    return userAdvice.toString();
  }

  public String getErrlog() {
    return errlog.toString();
  }

  private String conventionName;

  private void setConventionUsed(String convName) {
    this.conventionName = convName;
  }

  TableConfig getTableConfig() {
    return this.configResult;
  }

  TableConfigurer getTableConfigurer() {
    return tc;
  }

  /////////////////////////////////////////////////////////

  /**
   * Make a NestedTable object for the dataset.
   * @param wantFeatureType want this FeatureType
   * @throws IOException on read error
   */
  private void analyze(FeatureType wantFeatureType) throws IOException {
    // for netcdf-3 files, convert record dimension to structure
    // LOOK may be problems when served via opendap
    boolean structAdded = (Boolean) ds.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    if (tc == null) {
      makeTablesDefault(structAdded);
      makeNestedTables();

    } else {
      configResult = tc.getConfig(wantFeatureType, ds, errlog);
      if (configResult != null)
        addTableRecurse( configResult); // kinda stupid
      else { // use default
        makeTablesDefault(structAdded);
        makeNestedTables();
      }
    }

    // find the leaves
    for (TableConfig config : tableSet) {
      if (config.children == null) { // its a leaf
        NestedTable flatTable = new NestedTable(ds, config, errlog);
        leaves.add(flatTable);
      }
    }

    if (PointDatasetStandardFactory.showTables)
      getDetailInfo( new Formatter( System.out));
  }


  private void addTable(TableConfig t) {
    tableFind.put(t.name, t);
    if (t.dimName != null)
      tableFind.put(t.dimName, t);
    tableSet.add(t);
  }

  private void addTableRecurse(TableConfig t) {
    addTable(t);
    if (t.children != null) {
      for (TableConfig child : t.children)
        addTableRecurse(child);
    }
  }


  ///////////////////////////////////////////////////////////
  // default analasis aka guessing

  // no TableConfig was passed in - gotta wing it
  private void makeTablesDefault(boolean structAdded) throws IOException {

    // make Structures into a table
    List<Variable> vars = new ArrayList<Variable>(ds.getVariables());
    Iterator<Variable> iter = vars.iterator();
    while (iter.hasNext()) {
      Variable v = iter.next();
      if (v instanceof Structure) {  // handles Sequences too
        TableConfig st = new TableConfig(Table.Type.Structure, v.getName());
        CoordSysEvaluator.findCoords(st, ds);
        st.structName = v.getName();
        st.nestedTableName = v.getShortName();

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

    // lat, lon, time all use same dimension - use it
    if (dimSet.size() == 1) {
      Dimension obsDim = (Dimension) dimSet.toArray()[0];
      TableConfig st = new TableConfig(Table.Type.Structure, obsDim.getName());
      st.structureType = obsDim.isUnlimited() ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;
      st.structName = obsDim.isUnlimited() ? "record" : obsDim.getName();
      st.dimName = obsDim.getName();
      CoordSysEvaluator.findCoordWithDimension(st, ds, obsDim);

      CoordinateAxis time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
      if ((time != null) && (time.getRank() == 0)) {
        st.addJoin(new JoinArray(time, JoinArray.Type.scalar, 0));
        st.time = time.getShortName();
      }
      addTable( st);
    }

    if (tableSet.size() > 0) return;

    // try the time dimension
    CoordinateAxis time = null;
    for (CoordinateAxis axis : ds.getCoordinateAxes()) {
      if ((axis.getAxisType() == AxisType.Time) && axis.isCoordinateVariable()) {
        time = axis;
        break;
      }
    }
    if (time != null) {
      Dimension obsDim = (Dimension) time.getDimension(0);
      TableConfig st = new TableConfig(Table.Type.Structure, obsDim.getName());
      st.structureType = TableConfig.StructureType.PsuedoStructure;
      st.dimName = obsDim.getName();
      CoordSysEvaluator.findCoords(st, ds);

      addTable( st);
    }

  }

  private void findNestedStructures(Structure s, TableConfig parent) {
    for (Variable v : s.getVariables()) {
      if (v instanceof Structure) {  // handles Sequences too
        TableConfig nestedTable = new TableConfig(Table.Type.NestedStructure, v.getName());
        nestedTable.structName = v.getName();
        nestedTable.nestedTableName = v.getShortName();

        addTable(nestedTable);
        parent.addChild(nestedTable);

        // LOOK why not add the join(parent,child) here ?
        //nestedTable.join = new TableConfig.JoinConfig(Join.Type.NestedStructure);
        //joins.add(nestedTable.join);

        findNestedStructures((Structure) v, nestedTable); // look for nested structures
      }
    }
  }

  private void makeNestedTables() {
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


  /////////////////////////////////////////////////////
  /* track station info

  private StationInfo stationInfo = new StationInfo();

  private StationInfo getStationInfo() {
    return stationInfo;
  }

  public class StationInfo {
    public String stationId, stationDesc, stationNpts;
    public int nstations;
    public String latName, lonName, elevName;
  }  */

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
    showNestedTables(sf);
    String errlogS = errlog.toString();
    if (errlogS.length() > 0)
      sf.format("\n Errlog=\n%s",errlogS);
    String userAdviceS = userAdvice.toString();
    if (userAdviceS.length() > 0)
      sf.format("\n userAdvice=\n%s\n",userAdviceS);

    try {
      writeConfigXML(sf);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void writeConfigXML(java.util.Formatter sf) throws IOException {
    if (configResult != null) {
        PointConfigXML tcx = new PointConfigXML();
        tcx.writeConfigXML(configResult, tc.getClass().getName(), sf);
        return;
    }
    XMLOutputter fmt = new XMLOutputter( Format.getPrettyFormat());
    sf.format("%s", fmt.outputString ( makeDocument()));
  }

  /** Create an XML document from this info
   * @return netcdfDatasetInfo XML document
   */
  private Document makeDocument() {
    Element rootElem = new Element("featureDataset");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("location", ds.getLocation());
    if (tc != null)
      rootElem.addContent( new Element("analyser").setAttribute("class", tc.getClass().getName()));
    if (ft != null)
      rootElem.setAttribute("featureType", ft.toString());

    for (NestedTable nt : leaves) {
      writeTable( rootElem, nt.getLeaf());
    }

    return doc;
  }

  private Element writeTable( Element parent, Table table) {
    if (table.parent != null) {
      parent = writeTable( parent, table.parent);
    }
    Element tableElem = new Element("table");
    parent.addContent( tableElem);

    if (table.getName() != null)
      tableElem.setAttribute("name", table.getName());
    if (table.getFeatureType() != null)
      tableElem.setAttribute("featureType", table.getFeatureType().toString());
    tableElem.setAttribute("class", table.getClass().toString());

    addCoordinates(tableElem, table);
    for (VariableSimpleIF col : table.cols) {
      if (!table.nondataVars.contains(col.getShortName()))
        tableElem.addContent( new Element("variable").addContent(col.getName()));
    }

    if (table.extraJoins != null) {
      for (Join j : table.extraJoins) {
        if (j instanceof JoinArray)
          tableElem.addContent( writeJoinArray( (JoinArray)j));
        else if (j instanceof JoinMuiltdimStructure)
          tableElem.addContent( writeJoinMuiltdimStructure( (JoinMuiltdimStructure) j));
        else if (j instanceof JoinParentIndex)
          tableElem.addContent( writeJoinParentIndex( (JoinParentIndex) j));
      }
    }
    return tableElem;
  }

  private void addCoordinates( Element tableElem, Table table) {
    addCoord(tableElem, table.lat, "lat");
    addCoord(tableElem, table.lon, "lon");
    addCoord(tableElem, table.elev, "elev");
    addCoord(tableElem, table.time, "time");
    addCoord(tableElem, table.timeNominal, "timeNominal");
    addCoord(tableElem, table.stnId, "stnId");
    addCoord(tableElem, table.stnDesc, "stnDesc");
    addCoord(tableElem, table.stnNpts, "stnNpts");
    addCoord(tableElem, table.stnWmoId, "stnWmoId");
    addCoord(tableElem, table.stnAlt, "stnAlt");
    addCoord(tableElem, table.limit, "limit");
  }

  private void addCoord(Element tableElem, String name, String kind) {
    if (name != null) {
      Element elem = new Element("coordinate").setAttribute("kind", kind);
      elem.addContent(name);
      tableElem.addContent(elem);
    }
  }

  private Element writeJoinArray(JoinArray join) {
    Element joinElem = new Element("join");
    joinElem.setAttribute("class", join.getClass().toString());
    if (join.type != null)
      joinElem.setAttribute("type", join.type.toString());
    if (join.v != null)
      joinElem.addContent( new Element("variable").setAttribute("name", join.v.getName()));
    joinElem.addContent( new Element("param").setAttribute("value", Integer.toString(join.param)));
    return joinElem;
  }

  private Element writeJoinMuiltdimStructure(JoinMuiltdimStructure join) {
    Element joinElem = new Element("join");
    joinElem.setAttribute("class", join.getClass().toString());
    if (join.parentStructure != null)
      joinElem.addContent( new Element("parentStructure").setAttribute("name", join.parentStructure.getName()));
    joinElem.addContent( new Element("dimLength").setAttribute("value", Integer.toString(join.dimLength)));
    return joinElem;
  }

  private Element writeJoinParentIndex(JoinParentIndex join) {
    Element joinElem = new Element("join");
    joinElem.setAttribute("class", join.getClass().toString());
    if (join.parentStructure != null)
      joinElem.addContent( new Element("parentStructure").setAttribute("name", join.parentStructure.getName()));
    if (join.parentIndex != null)
      joinElem.addContent( new Element("parentIndex").setAttribute("name", join.parentIndex));
    return joinElem;
  }

  ////////////////////////////////////////////////////////////////////////////////

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
    //doit("Q:/cdmUnitTest/formats/gempak/surface/20090521_sao.gem");
    doit("D:/datasets/metars/Surface_METAR_20070513_0000.nc");

    //doit("C:/data/profile/PROFILER_wind_01hr_20080410_2300.nc");
    //doit("C:/data/cadis/tempting");
  }
}
