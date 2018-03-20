/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import opendap.dap.*;
import opendap.dap.parsers.ParseException;
import opendap.test.TestSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayStructure;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureMembers;
import ucar.nc2.NCdumpW;
import ucar.nc2.Variable;
import ucar.nc2.util.IO;
import ucar.unidata.util.test.UtilsMa2Test;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

/**
 *
 */
public class TestConvertD2N {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // debugging
  static DataDDS testDataDDSfromServer(String urlName, String CE) throws IOException,
          opendap.dap.DAP2Exception, InvalidRangeException {

    System.out.println("--DConnect ="+urlName);
    DConnect2 dodsConnection = new DConnect2(urlName, true);

    // get the DDS
    DDS dds =  dodsConnection.getDDS();
    dds.print(System.out);
    //DodsV root = DodsV.parseDDS( dds);

    // get the DAS
    DAS das =  dodsConnection.getDAS();
    das.print(System.out);
    System.out.println();

    //root.parseDAS(das);

    // get the DataDDS
    System.out.println("--DConnect.getData CE= "+CE);
    DataDDS dataDDS = dodsConnection.getData("?"+CE, null);
    dataDDS.print(System.out);
    System.out.println();

    System.out.println("--show DataDDS");
    PrintWriter pw = new PrintWriter(System.out);
    showDDS( dataDDS, pw);
    pw.flush();
    System.out.println();

    System.out.println("--parseDataDDS DodsV.show");
    DodsV dataRoot = DodsV.parseDataDDS( dataDDS);
    dataRoot.show(System.out, "");
    System.out.println();

    // try to parse with ConvertD2N
    System.out.println("--testConvertDDS");
    testConvertDDS( urlName, dataDDS, System.out);
    System.out.println();

    // show the original contents
    System.out.println("--"+urlName+".asc?"+CE);
    System.out.println( IO.readURLcontents( urlName+".asc?"+CE));

    System.out.println("============");

    return dataDDS;
  }

  static void testArray(String urlName) throws IOException, opendap.dap.DAP2Exception {

    System.out.println("checkArray ="+urlName);
    DConnect2 dodsConnection = new DConnect2(urlName, true);

    // get the DataDDS
    DataDDS dataDDS = dodsConnection.getData("?", null);
    dataDDS.print(System.out);
    System.out.println();
    DodsV root = DodsV.parseDataDDS( dataDDS);

    ConvertD2N converter = new ConvertD2N();
    DODSNetcdfFile dodsfile = new DODSNetcdfFile(urlName);
    List vars = dodsfile.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      Variable v = (Variable) vars.get(i);
      String name = DODSNetcdfFile.getDODSConstraintName(v);
      DodsV dodsV = root.findByDodsShortName( name);
      if (dodsV == null) {
        System.out.println("Cant find "+name);
        continue;
      }
      Array data = converter.convertTopVariable(v, null, dodsV);
      showArray( v.getFullName(), data, System.out, "");
    }

    /* for (int i = 0; i < root.children.size(); i++) {
      DodsV dodsV = (DodsV) root.children.get(i);
      Variable v = dodsfile.findVariable( dodsV.getNetcdfShortName());
      Array data = converter.convertTopVariable(v, null, dodsV);
      showArray( data, System.out, "");
    } */
    System.out.println("============");
  }

  static void showDDS( DataDDS dds, PrintWriter out) {
    out.println("DDS="+dds.getEncodedName());
    Enumeration e = dds.getVariables();
    while (e.hasMoreElements()) {
      BaseType bt  =  (BaseType) e.nextElement();
      showBT( bt, out, " ");
    }
  }

  static boolean showData = false;
  static boolean useNC = false;
  static void testConvertDDS( String urlName, DataDDS dataDDS, PrintStream out) throws IOException, DAP2Exception {
    DODSNetcdfFile dodsfile = new DODSNetcdfFile(urlName);
    System.out.println(dodsfile.toString());

    if (useNC) {
      List vars = dodsfile.getVariables();
      for (int i = 0; i < vars.size(); i++) {
        Variable v = (Variable) vars.get(i);
        Array data = v.read();
        if (showData)
          logger.debug(NCdumpW.toString(data, v.getFullName() + data.shapeToString(), null));
      }
    }

    ConvertD2N converter = new ConvertD2N();
    DodsV root = DodsV.parseDataDDS( dataDDS);
    for (int i = 0; i < root.children.size(); i++) {
      DodsV dodsV = root.children.get(i);
      Variable v = dodsfile.findVariable( dodsV.getFullName());
      Array data = converter.convertTopVariable(v, null, dodsV);
      showArray( v.getFullName(), data, out, "");

      if (useNC) {
        Array data2 = v.read();
        UtilsMa2Test.testEquals(data, data2);
      }

      if (showData)
        logger.debug(NCdumpW.toString(data, v.getFullName()+data.shapeToString(), null));
    }

  }

  static void showBT( BaseType bt, PrintWriter out, String space) {

    if (bt instanceof DSequence) {
      showSequence( (DSequence) bt, out, space);
      return;
    }

    if (bt instanceof DArray) {
      showArray( (DArray) bt, out, space);
      return;
    }

    out.println(space + bt.getEncodedName() + " ("+bt.getClass().getName()+")");

    if (bt instanceof DConstructor) {
      Enumeration e = ((DConstructor)bt).getVariables();
      String nspace = space + " ";
      while (e.hasMoreElements()) {
        BaseType nbt  =  (BaseType) e.nextElement();
        showBT( nbt, out, nspace);
      }
      out.println(space+"-----"+ bt.getEncodedName());
    }

  }

  static void showSequence( DSequence seq, PrintWriter out, String space) {
    int nrows = seq.getRowCount();
    out.println(space + seq.getEncodedName() + " ("+seq.getClass().getName()+")");

    String nspace = space + " ";

    // for sequences, gotta look at the _rows_ (!)
    if (nrows > 0) {
      out.println(nspace + "Vector["+nrows+"] allvalues; show first:");
      Vector v = seq.getRow(0);
      for (int i = 0; i < v.size(); i++) {
        BaseType bt = (BaseType) v.elementAt(i);
        showBT( bt, out, nspace+" ");
      }
    }
  }

  static void showArray( DArray a, PrintWriter out, String space) {
    int nrows = a.getLength();
    out.print(space + a.getEncodedName() + " ("+a.getClass().getName()+") ");

    out.print(" (");
    int count = 0;
    Enumeration dims = a.getDimensions();
    while (dims.hasMoreElements()) {
      DArrayDimension dim = (DArrayDimension) dims.nextElement();
      String name = dim.getEncodedName() == null ? "" : dim.getEncodedName()+"=";
      if (count > 0) out.print(",");
      out.print( name+dim.getSize());
      count++;
    }
    out.println(")");

    String nspace = space + " ";
    PrimitiveVector pv = a.getPrimitiveVector();
    BaseType template = pv.getTemplate();
    out.println( nspace + pv.getClass().getName()+"["+nrows+"] template="+ template.getClass().getName());

    if ((pv instanceof BaseTypePrimitiveVector) && !(template instanceof DString)) {
      if (nrows > 0) {
        BaseType vbt = ((BaseTypePrimitiveVector)pv).getValue(0);
        showBT( vbt, out, nspace+" ");
     }
    }

  }

  static void showArray( String name, Array a, PrintStream out, String space) {
    out.print(space + "Array "+name+" ("+a.getClass().getName()+") ");
    showShape( a.getShape(), out);
    out.println();

    if (a instanceof ArrayStructure) {
      ArrayStructure sa = (ArrayStructure) a;
      StructureMembers sm = sa.getStructureMembers();
      List memlist = sm.getMembers();
      for (int i = 0; i < memlist.size(); i++) {
        StructureMembers.Member member = (StructureMembers.Member) memlist.get(i);
        out.print(space+" "+member.getDataType()+" "+member.getName());
        showShape( member.getShape(), out);
        out.println();
        Object data = member.getDataArray();
        if (data != null)  {
          Array array = (Array) data;
          showArray( member.getName(), array, out, space+"  ");
        }
      }
      out.println();
    }

  }

  static void showShape( int[] shape, PrintStream out) {
    out.print(" (");
    for (int i = 0; i < shape.length; i++) {
      if (i > 0) out.print(",");
      out.print( shape[i]);
    }
    out.print(")");
  }

  static private void test(String url) throws IOException, ParseException, DAP2Exception, InvalidRangeException {
    testDataDDSfromServer(url, "");
    testArray(url);
  }

  static public void main( String[] args) throws IOException, ParseException, DAP2Exception, InvalidRangeException {

    /* test(server+"test.01"); // scalars
    test(server+"test.02");  // 1D arrays
    test(server+"test.03");  // 3D arrays

    test(server+"test.04");  // Structure with scalars
    test(server+"test.05");  // nested Structures with scalars
    test(server+"test.07a"); // Structure
    test(server+"test.21");  // Structure with multidim fields
    test(server+"test.50");  // array of structures

    test(server+"test.53");  // array of structures with nested scalar structure

    test(server+"test.06");  // Grids
    test(server+"test.06a");  // Grids


    test(server+"b31");  // top Sequence
    test(server+"test.07"); // top Sequence
    test(server+"test.56"); // top Sequence with multidim field
    test(server+"test.31"); // top Sequence with nested Structure, Grid  // */

    //test(server+"NestedSeq"); // nested Seq
    //test(server+"NestedSeq2"); // nested Seq   */

    testDataDDSfromServer(TestSources.XURL1+"/NestedSeq2", "person1.age,person1.stuff&person1.age=3"); // nested Seq
   // testDataDDSfromServer("http://dapper.pmel.noaa.gov/dapper/epic/woce_sl_time_monthly.cdp","location.profile&location._id=3"); // nested Seq

    // testDataDDSfromServer("http://dapper.pmel.noaa.gov/dapper/argo/argo_all.cdp", ""); // Sequence

    //testDataDDSfromServer(server+"test.22", "");  // Structure with nested Structure, Grid
    //testDataDDSfromServer(server+"test.23", "");  // Structure with nested Sequence, Grid
    //testDataDDSfromServer(server+"test.31", "");  // Sequence with nested Structure, Grid
    //testDataDDSfromServer(server+"NestedSeq2", "");
  }
}
