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
package ucar.nc2;

import ucar.ma2.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.URLnaming;
import ucar.nc2.util.Indent;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Formatter;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;

/**
 * Print contents of an existing netCDF file, using a Writer.
 * <p/>
 * A difference with ncdump is that the nesting of multidimensional array data is represented by nested brackets,
 * so the output is not legal CDL that can be used as input for ncgen. Also, the default is header only (-h)
 *
 * @author caron
 * @since Nov 4, 2007
 */

public class NCdumpW {
  private static String usage = "usage: NCdumpW <filename> [-cdl | -ncml] [-c | -vall] [-v varName1;varName2;..] [-v varName(0:1,:,12)]\n";

  public enum WantValues {
    none, coordsOnly, all
  }

  ;

  /**
   * Print netcdf "header only" in CDL.
   *
   * @param fileName open this file
   * @param out      print to this Writer
   * @return true if successful
   * @throws java.io.IOException on write error
   */
  public static boolean printHeader(String fileName, Writer out) throws java.io.IOException {
    return print(fileName, out, false, false, false, false, null, null);
  }

  /**
   * print NcML representation of this netcdf file, showing coordinate variable data.
   *
   * @param fileName open this file
   * @param out      print to this Writer
   * @return true if successful
   * @throws java.io.IOException on write error
   */
  public static boolean printNcML(String fileName, Writer out) throws java.io.IOException {
    return print(fileName, out, false, true, true, false, null, null);
  }

  /**
   * NCdump that parses a command string, using default options.
   * Usage:
   * <pre>NCdump filename [-ncml] [-c | -vall] [-v varName;...]</pre>
   *
   * @param command command string
   * @param out     send output here
   * @return true if successful
   * @throws java.io.IOException on write error
   */
  public static boolean print(String command, Writer out) throws java.io.IOException {
    return print(command, out, null);
  }

  /**
   * ncdump that parses a command string.
   * Usage:
   * <pre>NCdump filename [-ncml] [-c | -vall] [-v varName;...]</pre>
   *
   * @param command command string
   * @param out     send output here
   * @param ct      allow task to be cancelled; may be null.
   * @return true if successful
   * @throws java.io.IOException on write error
   */
  public static boolean print(String command, Writer out, ucar.nc2.util.CancelTask ct) throws java.io.IOException {

    // pull out the filename from the command
    String filename;
    StringTokenizer stoke = new StringTokenizer(command);
    if (stoke.hasMoreTokens())
      filename = stoke.nextToken();
    else {
      out.write(usage);
      return false;
    }

    NetcdfFile nc = null;
    try {
      nc = NetcdfFile.open(filename, ct);

      // the rest of the command
      int pos = command.indexOf(filename);
      command = command.substring(pos + filename.length());
      return print(nc, command, out, ct);

    } catch (java.io.FileNotFoundException e) {
      out.write("file not found= ");
      out.write(filename);
      return false;

    } finally {
      if (nc != null) nc.close();
      out.flush();
    }

  }

  /**
   * ncdump, parsing command string, file already open.
   *
   * @param nc      apply command to this file
   * @param command : command string
   * @param out     send output here
   * @param ct      allow task to be cancelled; may be null.
   * @return true if successful
   * @throws java.io.IOException on write error
   */
  public static boolean print(NetcdfFile nc, String command, Writer out, ucar.nc2.util.CancelTask ct) throws java.io.IOException {

    WantValues showValues = WantValues.none;
    boolean ncml = false;
    boolean strict = false;
    String varNames = null;

    if (command != null) {
      StringTokenizer stoke = new StringTokenizer(command);

      while (stoke.hasMoreTokens()) {
        String toke = stoke.nextToken();
        if (toke.equalsIgnoreCase("-help")) {
          out.write(usage);
          out.write('\n');
          return true;
        }
        if (toke.equalsIgnoreCase("-vall"))
          showValues = WantValues.all;
        if (toke.equalsIgnoreCase("-c") && (showValues == WantValues.none))
          showValues = WantValues.coordsOnly;
        if (toke.equalsIgnoreCase("-ncml"))
          ncml = true;
        if (toke.equalsIgnoreCase("-cdl"))
          strict = true;
        if (toke.equalsIgnoreCase("-v") && stoke.hasMoreTokens())
          varNames = stoke.nextToken();
      }
    }

    return print(nc, out, showValues, ncml, strict, varNames, ct);
  }

  /**
   * ncdump-like print of netcdf file.
   *
   * @param filename   NetcdfFile to open
   * @param out        print to this stream
   * @param showAll    dump all variable data
   * @param showCoords only print header and coordinate variables
   * @param ncml       print NcML representation (other arguments are ignored)
   * @param strict     print strict CDL representation
   * @param varNames   semicolon delimited list of variables whose data should be printed
   * @param ct         allow task to be cancelled; may be null.
   * @return true if successful
   * @throws java.io.IOException on write error
   */
  public static boolean print(String filename, Writer out, boolean showAll, boolean showCoords,
                              boolean ncml, boolean strict, String varNames, ucar.nc2.util.CancelTask ct) throws java.io.IOException {

    NetcdfFile nc = null;
    try {
      //nc = NetcdfFileCache.acquire(fileName, ct);
      nc = NetcdfFile.open(filename, ct);
      return print(nc, out, showAll, showCoords, ncml, strict, varNames, ct);

    } catch (java.io.FileNotFoundException e) {
      out.write("file not found= ");
      out.write(filename);
      out.flush();
      return false;

    } finally {
      if (nc != null) nc.close();
    }

  }

  /**
   * ncdump-like print of netcdf file.
   *
   * @param nc         already opened NetcdfFile
   * @param out        print to this stream
   * @param showAll    dump all variable data
   * @param showCoords only print header and coordinate variables
   * @param ncml       print NcML representation (other arguments are ignored)
   * @param strict     print strict CDL representation
   * @param varNames   semicolon delimited list of variables whose data should be printed. May have
   *                   Fortran90 like selector: eg varName(1:2,*,2)
   * @param ct         allow task to be cancelled; may be null.
   * @return true if successful
   * @throws java.io.IOException on write error
   */
  public static boolean print(NetcdfFile nc, Writer out, boolean showAll, boolean showCoords,
                              boolean ncml, boolean strict, String varNames, ucar.nc2.util.CancelTask ct) throws java.io.IOException {

    WantValues showValues = WantValues.none;
    if (showAll)
      showValues = WantValues.all;
    else if (showCoords)
      showValues = WantValues.coordsOnly;

    return print(nc, out, showValues, ncml, strict, varNames, ct);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////
  // heres where the work is done

  /**
   * ncdump-like print of netcdf file.
   *
   * @param nc         already opened NetcdfFile
   * @param out        print to this stream
   * @param showValues do you want the variable values printed?
   * @param ncml       print NcML representation (other arguments are ignored)
   * @param strict     print strict CDL representation
   * @param varNames   semicolon delimited list of variables whose data should be printed. May have
   *                   Fortran90 like selector: eg varName(1:2,*,2)
   * @param ct         allow task to be cancelled; may be null.
   * @return true if successful
   * @throws java.io.IOException on write error
   */
  public static boolean print(NetcdfFile nc, Writer out, WantValues showValues,
                              boolean ncml, boolean strict, String varNames, ucar.nc2.util.CancelTask ct) throws java.io.IOException {

    boolean headerOnly = (showValues == WantValues.none) && (varNames == null);

    try {

      if (ncml)
        writeNcML(nc, out, showValues, null); // output schema in NcML
      else if (headerOnly)
        nc.writeCDL(new PrintWriter(out), strict); // output schema in CDL form (like ncdump)
      else {
        PrintWriter ps = new PrintWriter(out);
        nc.toStringStart(ps, strict);
        ps.print(" data:\n");

        if (showValues == WantValues.all) { // dump all data
          for (Variable v : nc.getVariables()) {
            printArray(v.read(), v.getName(), ps, ct);
            if (ct != null && ct.isCancel()) return false;
          }
        } else if (showValues == WantValues.coordsOnly) { // dump coordVars
          for (Variable v : nc.getVariables()) {
            if (v.isCoordinateVariable())
              printArray(v.read(), v.getName(), ps, ct);
            if (ct != null && ct.isCancel()) return false;
          }
        }

        if ((showValues != WantValues.all) && (varNames != null)) { // dump the list of variables
          StringTokenizer stoke = new StringTokenizer(varNames, ";");
          while (stoke.hasMoreTokens()) {
            String varSubset = stoke.nextToken(); // variable name and optionally a subset

            if (varSubset.indexOf('(') >= 0) { // has a selector
              Array data = nc.readSection(varSubset);
              printArray(data, varSubset, ps, ct);

            } else {   // do entire variable
              Variable v = nc.findVariable(varSubset);
              if (v == null) {
                ps.print(" cant find variable: " + varSubset + "\n   " + usage);
                continue;
              }
              // dont print coord vars if they are already printed
              if ((showValues != WantValues.coordsOnly) || v.isCoordinateVariable())
                printArray(v.read(), v.getName(), ps, ct);
            }
            if (ct != null && ct.isCancel()) return false;
          }
        }

        nc.toStringEnd(ps);
      }

    } catch (Exception e) {
      e.printStackTrace();
      out.write(e.getMessage());
      out.flush();
      return false;
    }

    out.flush();
    return true;
  }


  /**
   * Print all the data of the given Variable.
   *
   * @param v  variable to print
   * @param ct allow task to be cancelled; may be null.
   * @return String result
   * @throws java.io.IOException on write error
   */
  static public String printVariableData(VariableIF v, ucar.nc2.util.CancelTask ct) throws IOException {
    Array data = v.read();
    /* try {
      data = v.isMemberOfStructure() ? v.readAllStructures(null, true) : v.read();
    }
    catch (InvalidRangeException ex) {
      return ex.getMessage();
    } */

    StringWriter writer = new StringWriter(10000);
    printArray(data, v.getName(), new PrintWriter(writer), ct);
    return writer.toString();
  }

  /**
   * Print a section of the data of the given Variable.
   *
   * @param v           variable to print
   * @param sectionSpec string specification
   * @param ct          allow task to be cancelled; may be null.
   * @return String result formatted data ouptut
   * @throws IOException           on write error
   * @throws InvalidRangeException is specified section doesnt match variable shape
   */
  static public String printVariableDataSection(Variable v, String sectionSpec, ucar.nc2.util.CancelTask ct) throws IOException, InvalidRangeException {
    Array data = v.read(sectionSpec);

    StringWriter writer = new StringWriter(20000);
    printArray(data, v.getName(), new PrintWriter(writer), ct);
    return writer.toString();
  }

  /**
   * Print the data array.
   *
   * @param array data to print.
   * @param name  title the output.
   * @param out   send output here.
   * @param ct    allow task to be cancelled; may be null.
   * @throws java.io.IOException on read error
   */
  static public void printArray(Array array, String name, PrintWriter out, CancelTask ct) throws IOException {
    printArray(array, name, null, out, new Indent(2), ct);
    out.flush();
  }

  static public String printArray(Array array, String name, CancelTask ct) throws IOException {
    CharArrayWriter carray = new CharArrayWriter(100000);
    PrintWriter pw = new PrintWriter(carray);
    printArray(array, name, null, pw, new Indent(2), ct);
    return carray.toString();
  }

  static private void printArray(Array array, String name, String units, PrintWriter out, Indent ilev, CancelTask ct) throws IOException {
    if (ct != null && ct.isCancel()) return;

    if (name != null) out.print(ilev + name + " =");
    ilev.incr();

    if (array == null)
      throw new IllegalArgumentException("null array for " + name);

    if ((array instanceof ArrayChar) && (array.getRank() > 0)) {
      printStringArray(out, (ArrayChar) array, ilev, ct);

    } else if (array.getElementType() == String.class) {
      printStringArray(out, (ArrayObject) array, ilev, ct);

    } else if (array instanceof ArrayStructure) {
      if (array.getSize() == 1)
        printStructureData(out, (StructureData) array.getObject(array.getIndex()), ilev, ct);
      else
        printStructureDataArray(out, (ArrayStructure) array, ilev, ct);

    } else if (array instanceof ArraySequence) {
      printSequence(out, (ArraySequence) array, ilev, ct);

    } else if (array.getElementType() == ByteBuffer.class) {
      array.resetLocalIterator();
      while (array.hasNext()) {
        printByteBuffer(out, (ByteBuffer) array.next(), ilev);
        out.println(",");
        if (ct != null && ct.isCancel()) return;
      }
    } else {
      printArray(array, out, ilev, ct);
    }

    if (units != null)
      out.print(" " + units);
    out.print("\n");
    ilev.decr();
    out.flush();
  }

  static private void printArray(Array ma, PrintWriter out, Indent indent, CancelTask ct) {
    if (ct != null && ct.isCancel()) return;

    int rank = ma.getRank();
    Index ima = ma.getIndex();

    // scalar
    if (rank == 0) {
      out.print(ma.getObject(ima).toString());
      return;
    }

    int[] dims = ma.getShape();
    int last = dims[0];

    out.print("\n" + indent + "{");

    if ((rank == 1) && (ma.getElementType() != StructureData.class)) {
      for (int ii = 0; ii < last; ii++) {
        out.print(ma.getObject(ima.set(ii)).toString());
        if (ii != last - 1) out.print(", ");
        if (ct != null && ct.isCancel()) return;
      }
      out.print("}");
      return;
    }

    indent.incr();
    for (int ii = 0; ii < last; ii++) {
      Array slice = ma.slice(0, ii);
      printArray(slice, out, indent, ct);
      if (ii != last - 1) out.print(",");
      if (ct != null && ct.isCancel()) return;
    }
    indent.decr();

    out.print("\n" + indent + "}");
  }

  static void printStringArray(PrintWriter out, ArrayChar ma, Indent indent, ucar.nc2.util.CancelTask ct) {
    if (ct != null && ct.isCancel()) return;

    int rank = ma.getRank();

    if (rank == 1) {
      out.print("  \"" + ma.getString() + "\"");
      return;
    }

    if (rank == 2) {
      boolean first = true;
      for (ArrayChar.StringIterator iter = ma.getStringIterator(); iter.hasNext();) {
        if (!first) out.print(", ");
        out.print("\"" + iter.next() + "\"");
        first = false;
        if (ct != null && ct.isCancel()) return;
      }
      return;
    }

    int[] dims = ma.getShape();
    int last = dims[0];

    out.print("\n" + indent + "{");
    indent.incr();
    for (int ii = 0; ii < last; ii++) {
      ArrayChar slice = (ArrayChar) ma.slice(0, ii);
      printStringArray(out, slice, indent, ct);
      if (ii != last - 1) out.print(",");
      if (ct != null && ct.isCancel()) return;
    }
    indent.decr();

    out.print("\n" + indent + "}");
  }

  private static void printByteBuffer(PrintWriter out, ByteBuffer bb, Indent indent) {
    out.print(indent + "0x");
    int last = bb.limit() - 1;
    for (int i = 0; i <= last; i++) {
      out.printf("%02x", bb.get(i));
    }
  }

  static void printStringArray(PrintWriter out, ArrayObject ma, Indent indent, ucar.nc2.util.CancelTask ct) {
    if (ct != null && ct.isCancel()) return;

    int rank = ma.getRank();
    Index ima = ma.getIndex();

    if (rank == 0) {
      out.print("  \"" + ma.getObject(ima) + "\"");
      return;
    }

    if (rank == 1) {
      boolean first = true;
      for (int i = 0; i < ma.getSize(); i++) {
        if (!first) out.print(", ");
        out.print("  \"" + ma.getObject(ima.set(i)) + "\"");
        first = false;
      }
      return;
    }

    int[] dims = ma.getShape();
    int last = dims[0];

    out.print("\n" + indent + "{");
    indent.incr();
    for (int ii = 0; ii < last; ii++) {
      ArrayObject slice = (ArrayObject) ma.slice(0, ii);
      printStringArray(out, slice, indent, ct);
      if (ii != last - 1) out.print(",");
      //out.print("\n");
    }
    indent.decr();

    out.print("\n" + indent + "}");
  }

  static private void printStructureDataArray(PrintWriter out, ArrayStructure array, Indent indent,
                                              ucar.nc2.util.CancelTask ct) throws IOException {
    StructureDataIterator sdataIter = array.getStructureDataIterator();
    int count = 0;
    while (sdataIter.hasNext()) {
      StructureData sdata = sdataIter.next();
      out.println("\n" + indent + "{");
      printStructureData(out, sdata, indent, ct);
      //ilev.setIndentLevel(saveIndent);
      out.print(indent + "} " + sdata.getName() + "(" + count + ")");
      if (ct != null && ct.isCancel()) return;
      count++;
    }
  }

  static private void printVariableArray(PrintWriter out, ArrayObject array, Indent indent, CancelTask ct) throws IOException {
    out.println("\n" + indent + "{");
    indent.incr();
    IndexIterator iter = array.getIndexIterator();
    while (iter.hasNext()) {
      Array data = (Array) iter.next();
      printArray(data, out, indent, ct);
    }
    indent.decr();
    out.print(indent + "}");
  }


  static private void printSequence(PrintWriter out, ArraySequence seq, Indent indent, CancelTask ct) throws IOException {
    StructureDataIterator iter = seq.getStructureDataIterator();
    while (iter.hasNext()) {
      StructureData sdata = iter.next();
      out.println("\n" + indent + "{");
      printStructureData(out, sdata, indent, ct);
      out.print(indent + "} " + sdata.getName());
      if (ct != null && ct.isCancel()) return;
    }
  }

  /**
   * Print contents of a StructureData.
   *
   * @param out   send output here.
   * @param sdata StructureData to print.
   * @throws java.io.IOException on read error
   */
  static public void printStructureData(PrintWriter out, StructureData sdata) throws IOException {
    printStructureData(out, sdata, new Indent(2), null);
    out.flush();
  }

  static private void printStructureData(PrintWriter out, StructureData sdata, Indent indent, CancelTask ct) throws IOException {
    indent.incr();
    for (StructureMembers.Member m : sdata.getMembers()) {
      Array sdataArray = sdata.getArray(m);
      printArray(sdataArray, m.getName(), m.getUnitsString(), out, indent, ct);
      if (ct != null && ct.isCancel()) return;
    }
    indent.decr();
  }

  /**
   * Print array as undifferentiated sequence of values.
   *
   * @param ma  any Array except ArrayStructure
   * @param out print to here
   */
  static public void printArray(Array ma, PrintWriter out) {
    ma.resetLocalIterator();
    while (ma.hasNext()) {
      out.print(ma.next());
      out.print(' ');
    }
  }

  static public void printArray(Array ma) {
    PrintWriter out = new PrintWriter(System.out);
    printArray(ma, out);
    out.flush();
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // standard NCML writing.

  /**
   * Write the NcML representation for a file.
   * Note that ucar.nc2.dataset.NcMLWriter has a JDOM implementation, for complete NcML.
   * This method implements only the "core" NcML for plain ole netcdf files.
   *
   * @param ncfile     write NcML for this file
   * @param os         write to this Writer. Must be using UTF-8 encoding (where applicable)
   * @param showCoords show coordinate variable values.
   * @param uri        use this for the uri attribute; if null use getLocation(). // ??
   * @throws IOException on write error
   */
  static public void writeNcML(NetcdfFile ncfile, java.io.Writer os, boolean showCoords, String uri) throws IOException {
    writeNcML(ncfile, os, showCoords ? WantValues.coordsOnly : WantValues.none, uri);
  }

  static public void writeNcML(NetcdfFile ncfile, java.io.Writer os, WantValues showValues, String uri) throws IOException {
    writeNcML(ncfile, new Formatter(os), showValues, uri);
  }

  static public void writeNcML(NetcdfFile ncfile, Formatter out, WantValues showValues, String uri) throws IOException {
    out.format("<?xml version='1.0' encoding='UTF-8'?>%n");
    out.format("<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'%n");

    if (uri != null)
      out.format("    location='%s' >%n%n", StringUtil.quoteXmlAttribute(uri));
    else
      out.format("    location='%s' >%n%n", StringUtil.quoteXmlAttribute(URLnaming.canonicalizeWrite(ncfile.getLocation())));

    if (ncfile.getId() != null)
      out.format("    id='%s'%n", StringUtil.quoteXmlAttribute(ncfile.getId()));
    if (ncfile.getTitle() != null)
      out.format("    title='%s'%n", StringUtil.quoteXmlAttribute(ncfile.getTitle()));

    writeNcMLGroup(ncfile, ncfile.getRootGroup(), out, new Indent(2), showValues);

    out.format("</netcdf>%n");
    out.flush();
  }

  static public void writeNcMLVariable(Variable v, Formatter out) throws IOException {
    if (v instanceof Structure) {
      writeNcMLStructure((Structure) v, out, new Indent(2), WantValues.none);
    } else {
      writeNcMLVariable(v, out, new Indent(2), WantValues.none);
    }
  }


  static private void writeNcMLGroup(NetcdfFile ncfile, Group g, Formatter out, Indent indent, WantValues showValues) throws IOException {
    if (g != ncfile.getRootGroup())
      out.format("%s<group name='%s' >%n", indent, StringUtil.quoteXmlAttribute(g.getShortName()));
    indent.incr();

    List<Dimension> dimList = g.getDimensions();
    for (Dimension dim : dimList) {
      out.format("%s<dimension name='%s' length='%s'", indent, StringUtil.quoteXmlAttribute(dim.getName()), dim.getLength());
      if (dim.isUnlimited())
        out.format(" isUnlimited='true'");
      out.format(" />%n");
    }
    if (dimList.size() > 0)
      out.format("%n");

    List<Attribute> attList = g.getAttributes();
    for (Attribute att : attList) {
      writeNcMLAtt(att, out, indent);
    }
    if (attList.size() > 0)
      out.format("%n");

    for (Variable v : g.getVariables()) {
      if (v instanceof Structure) {
        writeNcMLStructure((Structure) v, out, indent, showValues);
      } else {
        writeNcMLVariable(v, out, indent, showValues);
      }
    }

    // nested groups
    List groupList = g.getGroups();
    for (int i = 0; i < groupList.size(); i++) {
      if (i > 0) out.format("%n");
      Group nested = (Group) groupList.get(i);
      writeNcMLGroup(ncfile, nested, out, indent, showValues);
    }

    indent.decr();

    if (g != ncfile.getRootGroup()) {
      out.format("%s</group>%n", indent);
    }
  }

  static private void writeNcMLStructure(Structure s, Formatter out, Indent indent, WantValues showValues) throws IOException {
    out.format("%s<structure name='%s", indent, StringUtil.quoteXmlAttribute(s.getShortName()));

    // any dimensions?
    if (s.getRank() > 0) {
      writeNcMLDimension(s, out);
    }
    out.format(">%n");

    indent.incr();

    List<Attribute> attList = s.getAttributes();
    for (Attribute att : attList) {
      writeNcMLAtt(att, out, indent);
    }
    if (attList.size() > 0)
      out.format("%n");

    List<Variable> varList = s.getVariables();
    for (Variable v : varList) {
      writeNcMLVariable(v, out, indent, showValues);
    }

    indent.decr();

    out.format("%s</structure>%n", indent);
  }

  static private void writeNcMLVariable(Variable v, Formatter out, Indent indent, WantValues showValues) throws IOException {
    out.format("%s<variable name='%s' type='%s'", indent, StringUtil.quoteXmlAttribute(v.getShortName()), v.getDataType());

    // any dimensions (scalers must skip this attribute) ?
    if (v.getRank() > 0) {
      writeNcMLDimension(v, out);
    }

    indent.incr();

    boolean closed = false;

    // any attributes ?
    java.util.List<Attribute> atts = v.getAttributes();
    if (atts.size() > 0) {
      out.format(" >\n");
      closed = true;
      for (Attribute att : atts) {
        writeNcMLAtt(att, out, indent);
      }
    }

    // print data ?
    if ((showValues == WantValues.all) ||
            ((showValues == WantValues.coordsOnly) && v.isCoordinateVariable())) {
      if (!closed) {
        out.format(" >\n");
        closed = true;
      }
      writeNcMLValues(v, out, indent);
    }

    indent.decr();

    // close variable element
    if (!closed)
      out.format(" />\n");
    else {
      out.format("%s</variable>%n", indent);
    }

  }

  // LOOK anon dimensions
  static private void writeNcMLDimension(Variable v, Formatter out) {
    out.format(" shape='");
    java.util.List<Dimension> dims = v.getDimensions();
    for (int j = 0; j < dims.size(); j++) {
      Dimension dim = dims.get(j);
      if (j != 0)
        out.format(" ");
      if (dim.isShared())
        out.format("%s", StringUtil.quoteXmlAttribute(dim.getName()));
      else
        out.format("%d", dim.getLength());
    }
    out.format("'");
  }

  @SuppressWarnings({"ObjectToString"})
  static private void writeNcMLAtt(Attribute att, Formatter out, Indent indent) {
    out.format("%s<attribute name='%s' value='", indent, StringUtil.quoteXmlAttribute(att.getName()));
    if (att.isString()) {
      for (int i = 0; i < att.getLength(); i++) {
        if (i > 0) out.format("\\, "); // ??
        out.format("%s", StringUtil.quoteXmlAttribute(att.getStringValue(i)));
      }
    } else {
      for (int i = 0; i < att.getLength(); i++) {
        if (i > 0) out.format(" ");
        out.format("%s ", att.getNumericValue(i));
      }
      out.format("' type='%s", att.getDataType());
    }
    out.format("' />\n");
  }

  static private int totalWidth = 80;

  static private void writeNcMLValues(Variable v, Formatter out, Indent indent) throws IOException {
    Array data = v.read();
    int width = formatValues(indent + "<values>", out, 0, indent);

    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext())
      width = formatValues(ii.next() + " ", out, width, indent);
    formatValues("</values>\n", out, width, indent);
  }

  static private int formatValues(String s, Formatter out, int width, Indent indent) {
    int len = s.length();
    if (len + width > totalWidth) {
      out.format("%n%s", indent);
      width = indent.toString().length();
    }
    out.format("%s", s);
    width += len;
    return width;
  }

  private static char[] org = {'\b', '\f', '\n', '\r', '\t', '\\', '\'', '\"'};
  private static String[] replace = {"\\b", "\\f", "\\n", "\\r", "\\t", "\\\\", "\\\'", "\\\""};

  /**
   * Replace special characters '\t', '\n', '\f', '\r'.
   *
   * @param s string to quote
   * @return equivilent string replacing special chars
   */
  static public String encodeString(String s) {
    return StringUtil.replace(s, org, replace);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  /**
   * Main program.
   * <p><strong>ucar.nc2.NCdump filename [-cdl | -ncml] [-c | -vall] [-v varName1;varName2;..] [-v varName(0:1,:,12)] </strong>
   * <p>where: <ul>
   * <li> filename : path of any CDM readable file
   * <li> cdl or ncml: output format is CDL or NcML
   * <li> -vall : dump all variable data
   * <li> -c : dump coordinate variable data
   * <li> -v varName1;varName2; : dump specified variable(s)
   * <li> -v varName(0:1,:,12) : dump specified variable section
   * </ul>
   * Default is to dump the header info only.
   *
   * @param args arguments
   */
  public static void main(String[] args) {

    if (args.length == 0) {
      System.out.println(usage);
      return;
    }

    StringBuilder sbuff = new StringBuilder();
    for (String arg : args) {
      sbuff.append(arg);
      sbuff.append(" ");
    }

    try {
      Writer writer = new BufferedWriter(new OutputStreamWriter(System.out, Charset.forName("UTF-8")));
      NCdumpW.print(sbuff.toString(), writer, null);

    } catch (java.io.IOException ioe) {
      ioe.printStackTrace();
    }
  }
}