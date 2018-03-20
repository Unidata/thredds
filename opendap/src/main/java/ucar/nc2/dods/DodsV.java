/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import opendap.dap.*;
import opendap.dap.Attribute;
import opendap.dap.parsers.ParseException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.List;
import java.io.IOException;
import java.io.PrintStream;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;

/**
 * DodsV: a container for dods basetypes, so we can track stuff as we process it.
 * We make the DodsV tree follow the name heirarchy, by putting the darray object to the side.
 * The root is just a container, but the other nodes each represent a BaseType/Variable.
 * <p/>
 * DDS has a vector of BaseTypes, which may be:
 * scalar primitive (byte, int, float, String, etc)
 * array of primitive: DArray with (empty) PrimitiveVector with "template" as the BaseType.
 * DConstructor is a container for other BaseTypes, can call getVariables() to get them
 * - DStructure
 * - DGrid first Variable is the Array, rest are the maps
 * - DSequence
 * array of DStructure: DArray with BaseTypePrimitiveVector whose template is a DStructure
 * <p/>
 * DataDDS also contains the data:
 * scalar primitive (byte, int, float, String, etc): getValue()
 * array of primitive: DArray with PrimitiveVector (BytePrimitiveVector, FloatPrimitiveVector, etc): getValue(i)
 * Get internal java array with getInternalStorage().
 * array of String: DArray with BaseTypePrimitiveVector containing String objects
 * scalar DStructure, DGrid: same as DDS
 * array of DStructure: DArray with BaseTypePrimitiveVector, whose values are DStructure
 * array of DGrid, DSequence: (not sure how to interpret)
 * DSequence: values = Vector (rows) containing Vector (fields)
 *
 * @author caron
 */

class DodsV implements Comparable {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DodsV.class);
  static private boolean debugAttributes = false;

  /**
   * Parse the DDS, creating a tree of DodsV objects. The root node is only a container, ie it has no BaseType.
   * The Darray object (which has the dimension info) becomes a field of the DodsV, rather than being in the tree.
   *
   * @param dds the DDS to parse
   * @return the root dodsV object
   */
  static DodsV parseDDS(DDS dds) {
    DodsV root = new DodsV(null, null);

    // recursively get the Variables from the DDS
    Enumeration variables = dds.getVariables();
    parseVariables(root, variables);

    // assign depth first sequence number
    root.assignSequence(root);
    return root;
  }

  /**
   * Recursively build the dodsV tree.
   * 1) put all Variables into a DodsV
   * 2) unravel DConstructors (DSequence, DStructure, DGrid)
   * 3) for Darray, we put Variable = elemType, and store the darray seperately, not in the heirarchy.
   *
   * @param parent   of the tree
   * @param children list of BaseType
   */
  static private void parseVariables(DodsV parent, Enumeration children) {
    while (children.hasMoreElements()) {
      opendap.dap.BaseType bt = (opendap.dap.BaseType) children.nextElement();

      if (bt instanceof DList) {
        String mess = "Variables of type " + bt.getClass().getName() + " are not supported.";
        logger.warn(mess);
        continue;
      }

      DodsV dodsV = new DodsV(parent, bt);

      if (bt instanceof DConstructor) {
        DConstructor dcon = (DConstructor) bt;
        java.util.Enumeration enumerate2 = dcon.getVariables();
        parseVariables(dodsV, enumerate2);

      } else if (bt instanceof DArray) {
        DArray da = (DArray) bt;

        // Check to see if the array has any zero-length dimension; if so then ignore
        for (Enumeration e = da.getDimensions(); e.hasMoreElements(); ) {
          DArrayDimension dim = (DArrayDimension) e.nextElement();
          if (dim.getSize() <= 0) return;
        }

        BaseType elemType = da.getPrimitiveVector().getTemplate();
        dodsV.bt = elemType;
        dodsV.darray = da;

        if ((elemType instanceof DGrid) || (elemType instanceof DSequence) || (elemType instanceof DList)) {
          String mess = "Arrays of type " + elemType.getClass().getName() + " are not supported.";
          logger.warn(mess);
          continue;
        }

        if (elemType instanceof DStructure) { // note that for DataDDS, cant traverse this further to find the data.
          DConstructor dcon = (DConstructor) elemType;
          java.util.Enumeration nestedVariables = dcon.getVariables();
          parseVariables(dodsV, nestedVariables);
        }
      }

      parent.children.add(dodsV);
    }
  }

  /**
   * Parse the DDS, creating a tree of DodsV objects. The root node is only a container, ie it has no BaseType.
   * The Darray object (which has the dimension info) becomes a field of the DodsV, rather than being in the tree.
   *
   * @param dds the DataDDS to parse
   * @return the root DodsV object
   * @throws opendap.dap.NoSuchVariableException if DataDDS contains a Variable not in the DDS
   */
  static DodsV parseDataDDS(DataDDS dds) throws NoSuchVariableException {
    DodsV root = new DodsV(null, null);

    // recursively get the Variables from the DDS
    Enumeration variables = dds.getVariables();
    parseDataVariables(root, variables);

    // assign depth first sequence number
    root.assignSequence(root);
    return root;
  }

  /**
   * Recursively build the dodsV tree.
   * 1) put all Variables into a DodsV
   * 2) unravel DConstructors (DSequence, DStructure, DGrid)
   * 3) for Darray, we put Variable = elemType, and store the darray seperately, not in the heirarchy.
   *
   * @param parent   of the tree
   * @param children list of BaseType
   * @throws opendap.dap.NoSuchVariableException if children and parent are inconsistent
   */
  static private void parseDataVariables(DodsV parent, Enumeration children) throws NoSuchVariableException {
    while (children.hasMoreElements()) {
      opendap.dap.BaseType bt = (opendap.dap.BaseType) children.nextElement();
      DodsV dodsV = new DodsV(parent, bt);
      parent.children.add(dodsV);

      if (bt instanceof DGrid) {
        DGrid dgrid = (DGrid) bt;
        if (dodsV.parent.bt == null) { // is top level
          // top level grids are replaced by their "data array"
          dodsV.darray = (DArray) dgrid.getVar(0);
          processDArray(dodsV);
        } else {
          // nested grids are made into Structures
          dodsV.makeAllDimensions();
        }

        java.util.Enumeration enumerate2 = dgrid.getVariables();
        parseDataVariables(dodsV, enumerate2);

      } else if (bt instanceof DSequence) {
        DSequence dseq = (DSequence) bt;
        int seqlen = dseq.getRowCount();
        if (seqlen > 0) {
          DArrayDimension ddim = new DArrayDimension(seqlen, null);
          dodsV.dimensions.add(ddim);
        }
        dodsV.makeAllDimensions();

        java.util.Enumeration enumerate2 = dseq.getVariables();
        parseDataVariables(dodsV, enumerate2);

      } else if (bt instanceof DConstructor) {
        DStructure dcon = (DStructure) bt; // LOOK DConstructor != DStructure?
        dodsV.makeAllDimensions();
        java.util.Enumeration enumerate2 = dcon.getVariables();
        parseDataVariables(dodsV, enumerate2);

      } else if (bt instanceof DArray) {
        dodsV.darray = (DArray) bt;
        processDArray(dodsV);

        dodsV.bt = dodsV.elemType;
        if (dodsV.elemType instanceof DStructure) {
          DStructure dcon = (DStructure) dodsV.elemType;
          java.util.Enumeration nestedVariables = dcon.getVariables();
          parseDataVariables(dodsV, nestedVariables);
        }
      } else {
        dodsV.makeAllDimensions();
      }

    }
  }

  static private void processDArray(DodsV dodsV) {
    DArray da = dodsV.darray;
    Enumeration dims = da.getDimensions();
    while (dims.hasMoreElements()) {
      DArrayDimension dim = (DArrayDimension) dims.nextElement();
      dodsV.dimensions.add(dim);
    }
    dodsV.makeAllDimensions(); // redo

    BaseType elemType;
    PrimitiveVector pv = da.getPrimitiveVector();
    if (pv instanceof BaseTypePrimitiveVector) {
      BaseTypePrimitiveVector bpv = (BaseTypePrimitiveVector) pv;
      elemType = bpv.getValue(0);
    } else {
      elemType = da.getPrimitiveVector().getTemplate();
    }

    dodsV.elemType = elemType;

    if ((dodsV.elemType instanceof DGrid) || (dodsV.elemType instanceof DSequence) || (dodsV.elemType instanceof DList)) {
      String mess = "Arrays of type " + dodsV.bt.getClass().getName() + " are not supported.";
      logger.error(mess);
      throw new IllegalArgumentException(mess);
    }

  }

  //////////////////////////////////////////////////////////////////

  DodsV parent;
  BaseType bt;
  BaseType elemType; // different for DGrid
  List<DodsV> children = new ArrayList<>(); // DodsV objects
  DArray darray; // if its an array
  List<DArrayDimension> dimensions = new ArrayList<>();
  List<DArrayDimension> dimensionsAll = new ArrayList<>();
  List<DODSAttribute> attributes = new ArrayList<>();

  Array data; // preload
  boolean isDone; // nc var has been made
  int seq; // "depth first" order

  DodsV(DodsV parent, BaseType bt) {
    this.parent = parent;
    this.bt = bt;
    this.elemType = bt;
  }

  public int compareTo(Object o) {
    if (o instanceof DodsV)
      return seq - ((DodsV) o).seq;
    return -1;
  }

  void show(PrintStream out, String space) {
    out.print(space + "DodsV.show " + getEncodedName() + " " + getType());
    out.print("(");
    int count = 0;
    for (DArrayDimension dim : dimensionsAll) {
      String name = dim.getEncodedName() == null ? "" : dim.getEncodedName() + "=";
      if (count > 0) out.print(",");
      out.print(name + dim.getSize());
      count++;
    }
    out.println(")");

    for (DodsV dodsV : children) {
      dodsV.show(out, space + "  ");
    }
  }

  @Override
  public String toString() {
    return bt.getLongName();
  }

  //String getName() { return bt == null ? " root" : bt.getEncodedName(); }

  String getClearName() {
    return bt == null ? "root" : bt.getClearName();
  }

  String getEncodedName() {
    return bt == null ? "root" : bt.getEncodedName();
  }

  String getFullName() {
    if (parent != null && parent.bt != null)
      return (parent.getFullName() + "." + bt.getEncodedName());
    return (bt == null) ? "root" : bt.getEncodedName();
  }

  String getNetcdfShortName() {
    return DODSNetcdfFile.makeShortName(getClearName());
  }

  String getType() {
    return bt == null ? "" : bt.getTypeName();
  }

  DataType getDataType() {
    if (bt == null)
      throw new IllegalStateException("DodsV.bt  == null");
    if (bt instanceof DGrid)
      return DODSNetcdfFile.convertToNCType(elemType, isUnsigned());
    else
      return DODSNetcdfFile.convertToNCType(bt, isUnsigned());
  }

  public boolean isUnsigned() {
    ucar.nc2.Attribute att = findAttributeIgnoreCase(CDM.UNSIGNED);
    return (att == null) || att.getStringValue().equalsIgnoreCase("true");
  }

  public ucar.nc2.Attribute findAttributeIgnoreCase(String name) {
    for (ucar.nc2.Attribute a : attributes) {
      if (name.equalsIgnoreCase(a.getShortName()))
        return a;
    }
    return null;
  }

  int[] getShape() {
    int[] shape = new int[dimensions.size()];
    for (int i = 0; i < dimensions.size(); i++) {
      DArrayDimension dim = dimensions.get(i);
      shape[i] = dim.getSize();
    }
    return shape;
  }

  int[] getShapeAll() {
    if (bt instanceof DSequence) {
      DSequence dseq = (DSequence) bt;
      int seqlen = dseq.getRowCount();
      return new int[]{seqlen};
    }

    int[] shape = new int[dimensionsAll.size()];
    for (int i = 0; i < dimensionsAll.size(); i++) {
      DArrayDimension dim = dimensionsAll.get(i);
      shape[i] = dim.getSize();
    }
    return shape;
  }

  void addAttribute(DODSAttribute att) {
    attributes.add(att);
  }

  void makeAllDimensions() {
    dimensionsAll = new ArrayList<>();
    if (parent != null)
      dimensionsAll.addAll(parent.dimensionsAll);
    dimensionsAll.addAll(dimensions);
  }


  // assign depth first sequence number
  private int nextInSequence = 0;

  private void assignSequence(DodsV root) {
    for (DodsV nested : children) {
      nested.assignSequence(root);
      nested.seq = root.nextInSequence;
      nextInSequence++;
    }
  }

  /**
   * Parse the DAS, assign attribute tables to the DodsV objects.
   * Nested attribute tables are supposed to follow the tree we construct with dodsV, so its easy to assign to correct dodsV.
   *
   * @param das parse this DAS
   * @throws IOException on io error
   */
  void parseDAS(DAS das) throws IOException {
    Enumeration tableNames = das.getNames();
    while (tableNames.hasMoreElements()) {
      String tableName = (String) tableNames.nextElement();
      AttributeTable attTable = das.getAttributeTableN(tableName);

      if (tableName.equals("NC_GLOBAL") || tableName.equals("HDF_GLOBAL")) {
        addAttributeTable(this, attTable, tableName, true);

      } else if (tableName.equals("DODS_EXTRA") || tableName.equals("EXTRA_DIMENSION")) {
        // handled seperately in DODSNetcdfFile
        continue;

      } else {
        DodsV dodsV = findDodsV(tableName, false); // short name matches the table name
        if (dodsV != null) {
          addAttributeTable(dodsV, attTable, tableName, true);
        } else {
          dodsV = findTableDotDelimited(tableName);
          if (dodsV != null) {
            addAttributeTable(dodsV, attTable, tableName, true);
          } else {
            if (debugAttributes) System.out.println("DODSNetcdf getAttributes CANT find <" + tableName + "> add to globals");
            addAttributeTable(this, attTable, tableName, false);
          }
        }
      }
    }
  }

  private void addAttributeTable(DodsV dodsV, AttributeTable attTable, String fullName, boolean match) {
    if (attTable == null) return;

    java.util.Enumeration attNames = attTable.getNames();
    while (attNames.hasMoreElements()) {
      String attName = (String) attNames.nextElement();
      opendap.dap.Attribute att = attTable.getAttribute(attName);
      if (att == null) {
        logger.error("Attribute not found=" + attName + " in table=" + attTable.getEncodedName());
        continue;
      }
      addAttribute(dodsV, att, fullName, match);
    }
  }

  private void addAttribute(DodsV dodsV, opendap.dap.Attribute att, String fullName, boolean match) {
    if (att == null) return;
    fullName = fullName + "." + att.getEncodedName();

    if (!att.isContainer()) {
      DODSAttribute ncatt = new DODSAttribute(match ? att.getEncodedName() : fullName, att);
      dodsV.addAttribute(ncatt);
      if (debugAttributes) System.out.println(" addAttribute " + ncatt.getShortName() + " to " + dodsV.getFullName());

    } else if (att.getEncodedName() == null) {
      logger.info("DODS attribute name is null = " + att);
    } else {
      DodsV child = dodsV.findDodsV(att.getEncodedName(), false);
      if (child != null) {
        addAttributeTable(child, att.getContainerN(), fullName, match);
      } else {
        if (att.getEncodedName().equals("DODS")) return; // special case - DODS info
        if (debugAttributes) System.out.println(" Cant find nested Variable " + att.getEncodedName() + " in " + dodsV.getFullName());
        addAttributeTable(this, att.getContainerN(), fullName, false);
      }
    }
  }

  /**
   * Search the immediate children for a BaseType with given name.
   *
   * @param name    look for this name
   * @param useDone
   * @return child that matches if found, else null
   */
  DodsV findDodsV(String name, boolean useDone) {
    for (DodsV dodsV : children) {
      if (useDone && dodsV.isDone) continue; // LOOK useDone ??
      if ((name == null) || (dodsV == null) || (dodsV.bt == null)) {
        logger.warn("Corrupted structure");
        continue;
      }
      if (name.equals(dodsV.bt.getEncodedName()))
        return dodsV;
    }
    return null;
  }

  DodsV findByNetcdfShortName(String ncname) {
    for (DodsV child : children) {
      if (ncname.equals(child.getNetcdfShortName()))
        return child;
    }
    return null;
  }

  DodsV findByDodsShortName(String dodsname) {
    for (DodsV child : children) {
      if (dodsname.equals(child.getClearName()))
        return child;
    }

    for (DodsV child : children) {
      DodsV d = child.findByDodsShortName(dodsname);
      if (null != d) return d;
    }

    return null;
  }

  // From a ddsV tree, we need to find the corresponding dodsV in the DataDDS tree.
  // find the DodsV object in the dataVlist corresponding to the ddsV
  DodsV findDataV(DodsV ddsV) {
    if (ddsV.parent.bt != null) {
      DodsV parentV = findDataV(ddsV.parent);
      if (parentV == null) // dataDDS may not have the structure wrapper
        return findDodsV(ddsV.bt.getEncodedName(), true);
      return parentV.findDodsV(ddsV.bt.getEncodedName(), true);
    }

    DodsV dataV = findDodsV(ddsV.bt.getEncodedName(), true);
    /* if ((dataV == null) && (ddsV.bt instanceof DGrid)) { // when asking for the Grid array
      DodsV gridArray = (DodsV) ddsV.children.get(0);
      return findDodsV( gridArray.bt.getName(), dataVlist, true);
    } */
    return dataV;
  }

  DodsV findTableDotDelimited(String tableName) {
    DodsV dodsV = this;
    StringTokenizer toker = new StringTokenizer(tableName, ".");
    while (toker.hasMoreTokens()) {
      String name = toker.nextToken();
      dodsV = dodsV.findDodsV(name, false);
      if (dodsV == null) return null;
    }
    return dodsV;
  }

  // Additions to help support RC.getUseGroups

  /**
   * Return number of children
   *
   * @return |children|
   */
  int getChildCount() {
    return children.size();
  }

  /**
   * Return a child by index
   *
   * @param index return index'th child
   * @return index'th child or null if index is too large
   */
  DodsV findByIndex(int index) {
    if (children.size() <= index) return null;
    return children.get(index);
  }

  public BaseType getBaseType() {
    return bt;
  }


  ////////////////////////////////////////////////////////////////////////////////
  private static void doit(String urlName) throws IOException, DAP2Exception {
    System.out.println("DODSV read =" + urlName);
    try (DConnect2 dodsConnection = new DConnect2(urlName, true)) {

      // get the DDS
      DDS dds = dodsConnection.getDDS();
      dds.print(System.out);
      DodsV root = DodsV.parseDDS(dds);

      // get the DAS
      DAS das = dodsConnection.getDAS();
      das.print(System.out);
      root.parseDAS(das);

      // show the dodsV tree
      root.show(System.out, "");
    }
  }

  public static void main(String args[]) throws IOException, DAP2Exception {
    // doit("http://localhost:8080/thredds/dodsC/ncdodsTest/conventions/zebra/SPOL_3Volumes.nc");
    doit("http://iridl.ldeo.columbia.edu/SOURCES/.CAYAN/dods");
  }


}
