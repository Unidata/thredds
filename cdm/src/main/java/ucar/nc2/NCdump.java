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
import java.util.*;

/**
 * Print contents of an existing netCDF file of unknown structure, like ncdump.
 *
 * A difference with ncdump is that the nesting of multidimensional array data is represented by nested brackets,
 * so the output is not legal CDL that can be used as input for ncgen. Also, the default is header only (-h)
 *
 * @author Russ Rew, John Caron
 * @deprecated use NCdumpW, to handle Unicode correctly
 */

public class NCdump {
  private static boolean debugSelector = false;
  private static String usage = "usage: NCdump <filename> [-cdl | -ncml] [-c | -vall] [-v varName1;varName2;..] [-v varName(0:1,:,12)]\n";

  /**
   * Print netcdf "header only" in CDL.
   * @param fileName open this file
   * @param out print to this stream
   * @return true if successful
   * @throws IOException on write error
   */
  public static boolean printHeader(String fileName, OutputStream out) throws java.io.IOException {
    return print( fileName, out, false, false, false, false, null, null);
  }

  /**
   * print NcML representation of this netcdf file, showing coordinate variable data.
   * @param fileName open this file
   * @param out print to this stream
   * @return true if successful
   * @throws IOException on write error
   */
  public static boolean printNcML(String fileName, OutputStream out) throws java.io.IOException {
    return print( fileName, out, false, true, true, false, null, null);
  }

   /**
   * NCdump that parses a command string, using default options.
   * Usage:
   * <pre>NCdump filename [-ncml] [-c | -vall] [-v varName;...]</pre>
   * @param command command string
   * @param out send output here
   * @return true if successful
   * @throws IOException on write error
   */
  public static boolean print(String command, OutputStream out) throws java.io.IOException {
    return print( command, out, null);
  }

  /**
   * ncdump that parses a command string.
   * Usage:
   * <pre>NCdump filename [-ncml] [-c | -vall] [-v varName;...]</pre>
   * @param command command string
   * @param out send output here
   * @param ct allow task to be cancelled; may be null.
   * @return true if successful
   * @throws IOException on write error
   */
  public static boolean print(String command, OutputStream out, ucar.nc2.util.CancelTask ct) throws java.io.IOException {

    // pull out the filename from the command
    String filename;
    StringTokenizer stoke = new StringTokenizer( command);
    if (stoke.hasMoreTokens())
      filename = stoke.nextToken();
    else {
      out.write( usage.getBytes());
      return false;
    }

    NetcdfFile nc = null;
    try {
      nc = NetcdfFile.open(filename, ct);

      // the rest of the command
      int pos = command.indexOf(filename);
      command = command.substring(pos + filename.length());
      return NCdump.print(nc, command, out, ct);

    } catch (java.io.FileNotFoundException e) {
      String mess = "file not found= "+filename;
      out.write( mess.getBytes());
      return false;

    } finally {
      if (nc != null) nc.close();
    }

  }

  /**
   * ncdump, parsing command string, file already open.
   * @param nc apply command to this file
   * @param command : command string
   * @param out send output here
   * @param ct allow task to be cancelled; may be null.
   * @return true if successful
   * @throws IOException on write error
   */
  public static boolean print(NetcdfFile nc, String command, OutputStream out, ucar.nc2.util.CancelTask ct) throws java.io.IOException {

    boolean showAll = false;
    boolean showCoords = false;
    boolean ncml = false;
    boolean strict = false;
    String varNames = null;

    if (command != null) {
      StringTokenizer stoke = new StringTokenizer( command);

      while (stoke.hasMoreTokens()) {
        String toke = stoke.nextToken();
         if (toke.equalsIgnoreCase("-help")) {
          out.write( usage.getBytes());
          out.write( '\n');
          return true;
        }
        if (toke.equalsIgnoreCase("-vall"))
          showAll = true;
        if (toke.equalsIgnoreCase("-c"))
          showCoords = true;
        if (toke.equalsIgnoreCase("-ncml"))
          ncml = true;
        if (toke.equalsIgnoreCase("-cdl"))
          strict = true;
        if (toke.equalsIgnoreCase("-v") && stoke.hasMoreTokens())
          varNames = stoke.nextToken();
      }
    }

    return NCdump.print( nc, out, showAll, showCoords, ncml, strict, varNames, ct);
  }

  /**
   *  ncdump-like print of netcdf file.
   * @param fileName NetcdfFile to open
   * @param out print to this stream
   * @param showAll dump all variable data
   * @param showCoords only print header and coordinate variables
   * @param ncml print NcML representation (other arguments are ignored)
   * @param strict print strict CDL representation
   * @param varNames semicolon delimited list of variables whose data should be printed
   * @param ct allow task to be cancelled; may be null.
   * @return true if successful
   * @throws IOException on write error

   */
  public static boolean print(String fileName, OutputStream out, boolean showAll, boolean showCoords,
    boolean ncml, boolean strict, String varNames, ucar.nc2.util.CancelTask ct) throws java.io.IOException {

    NetcdfFile nc = null;
    try {
      //nc = NetcdfFileCache.acquire(fileName, ct);
      nc = NetcdfFile.open(fileName, ct);
      return print(nc, out, showAll, showCoords, ncml, strict, varNames, ct);

    } catch (java.io.FileNotFoundException e) {
      String mess = "file not found= "+fileName;
      out.write( mess.getBytes());
      return false;

    } finally {
      //NetcdfFileCache.release(nc);
      if (nc != null) nc.close();
    }

  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////
  // heres where the work is done

  /**
   *  ncdump-like print of netcdf file.
   * @param nc already opened NetcdfFile
   * @param out print to this stream
   * @param showAll dump all variable data
   * @param showCoords only print header and coordinate variables
   * @param ncml print NcML representation (other arguments are ignored)
   * @param strict print strict CDL representation
   * @param varNames semicolon delimited list of variables whose data should be printed. May have
   *  Fortran90 like selector: eg varName(1:2,*,2)
   * @param ct allow task to be cancelled; may be null.
   * @return true if successful
   * @throws IOException on write error
   */
  public static boolean print(NetcdfFile nc, OutputStream out, boolean showAll, boolean showCoords,
      boolean ncml, boolean strict, String varNames, ucar.nc2.util.CancelTask ct) throws java.io.IOException {

    PrintWriter pw = new PrintWriter( new OutputStreamWriter(out));
    return NCdumpW.print(nc, pw, showAll, showCoords, ncml, strict, varNames, ct);
  }
  /*
    boolean headerOnly = !showAll && (varNames == null);

    try {

      if (ncml)
        writeNcML(nc, out, showCoords, null); // output schema in NcML
      else if (headerOnly)
        nc.writeCDL(out, strict); // output schema in CDL form (like ncdump)
      else {
        PrintWriter pw = new PrintWriter( new OutputStreamWriter(out));
        nc.toStringStart(pw, strict);
        pw.print(" data:\n");

        if (showAll) { // dump all data
          for (Variable v : nc.getVariables()) {
            printArray(v.read(), v.getName(), ps, ct);
            if (ct != null && ct.isCancel()) return false;
          }
        }

        else if (showCoords) { // dump coordVars
          for (Variable v : nc.getVariables()) {
            if (v.isCoordinateVariable())
              printArray(v.read(), v.getName(), ps, ct);
            if (ct != null && ct.isCancel()) return false;
          }
        }

        if (!showAll && (varNames != null)) { // dump the list of variables
          StringTokenizer stoke = new StringTokenizer(varNames,";");
          while (stoke.hasMoreTokens()) {
            String varSubset = stoke.nextToken(); // variable name and optionally a subset

            if (varSubset.indexOf('(') >= 0) { // has a selector
              Array data = nc.read(varSubset, false);
              printArray(data, varSubset, ps, ct);

            } else {   // do entire variable
              Variable v = nc.findVariable(varSubset);
              if (v == null) {
                ps.print(" cant find variable: "+varSubset+"\n   "+usage);
                continue;
              }
              // dont print coord vars if they are already printed
              if (!showCoords || v.isCoordinateVariable())
                printArray( v.read(), v.getName(), ps, ct);
            }
            if (ct != null && ct.isCancel()) return false;
          }
        }

        nc.toStringEnd( ps);
        ps.flush();
      }

    } catch (Exception e) {
      e.printStackTrace();
      out.write(e.getMessage().getBytes());
      return false;
    }

    return true;
  }    */

  /**
   * Parse a section specification String. These have the form:
   * <pre>
   *  section specification := selector | selector '.' selector
   *  selector := varName ['(' dims ')']
   *  varName := STRING
   *
   *   dims := dim | dim, dims
   *   dim := ':' | slice | start ':' end | start ':' end ':' stride
   *   slice := INTEGER
   *   start := INTEGER
   *   stride := INTEGER
   *   end := INTEGER
   * </pre>
   *
   * Nonterminals are in lower case, terminals are in upper case, literals are in single quotes.
   * Optional components are enclosed between square braces '[' and ']'.
   *
   * @param ncfile look for variable in here
   * @param variableSection the string to parse, eg "record(12).wind(1:20,:,3)"
   * @return return CEresult which has the equivilent Variable
   * @throws IllegalArgumentException when token is misformed, or variable name doesnt exist in ncfile
   * @throws ucar.ma2.InvalidRangeException if section does not match variable shape
   *
   * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/reference/SectionSpecification.html">SectionSpecification</a>
   */
  static private CEresult parseVariableSection( NetcdfFile ncfile, String variableSection) throws InvalidRangeException {
    StringTokenizer stoke = new StringTokenizer(variableSection, ".");
    String selector = stoke.nextToken();
    if (selector == null)
      throw new IllegalArgumentException("empty sectionSpec = " + variableSection);

    // parse each selector, find the inner variable
    boolean hasInner = false;
    List<Range> fullSelection = new ArrayList<Range>();
    Variable innerV = parseVariableSelector(ncfile, selector, fullSelection);
    while (stoke.hasMoreTokens()) {
      selector = stoke.nextToken();
      innerV = parseVariableSelector(innerV, selector, fullSelection);
      hasInner = true;
    }
    return new CEresult( innerV, fullSelection, hasInner);
  }

  /** public by accident */
  static private class CEresult {
    public Variable v; // the variable
    public List<Range> ranges; // list of ranges for this variable
    public boolean hasInner;
    CEresult( Variable v, List<Range> ranges, boolean hasInner) {
      this.v = v;
      this.ranges = ranges;
      this.hasInner = hasInner;
    }
  }

  // parse variable name and index selector out of the selector String. variable name must be escaped
  private static Variable parseVariableSelector( Object parent, String selector, List<Range> fullSelection)
          throws InvalidRangeException {
    String varName, indexSelect = null;
    int pos1 = selector.indexOf('(');

    if (pos1 < 0) { // no selector
      varName = selector;
    } else {
      varName = selector.substring(0, pos1);
      indexSelect = selector.substring(pos1);
    }
    if (debugSelector)
      System.out.println(" parseVariableSection <"+selector+"> = <"+varName+">, <"+indexSelect+">");

    Variable v = null;
    if (parent instanceof NetcdfFile) {
      NetcdfFile ncfile = (NetcdfFile) parent;
      v = ncfile.findVariable(varName);

    } else if (parent instanceof Structure) {
      Structure s = (Structure) parent;
      v = s.findVariable(varName); // LOOK
    }
    if (v == null)
      throw new IllegalArgumentException(" cant find variable: "+varName+" in selector="+selector);

    // get the selected Ranges, or all, and add to the list
    Section section;
    if (indexSelect != null) {
      section = new Section(indexSelect);
      section.setDefaults(v.getShape()); // Check section has no nulls, set from shape array.
    } else
      section = new Section(v.getShape()); // all

    fullSelection.addAll(section.getRanges());

    return v;
  }


  /** Make section specification String from a range list for a Variable.
   * @param v for this Variable.
   * @param ranges list of Range. Must includes all parent structures. The list be null, meaning use all.
   *   Individual ranges may be null, meaning all for that dimension.
   * @return section specification String.
   * @throws InvalidRangeException is specified section doesnt match variable shape
   */
  public static String makeSectionString(VariableIF v, List<Range> ranges) throws InvalidRangeException {
    StringBuilder sb = new StringBuilder();
    makeSpec(sb, v, ranges);
    return sb.toString();
  }

  private static List<Range> makeSpec(StringBuilder sb, VariableIF v, List<Range> orgRanges) throws InvalidRangeException {
    if (v.isMemberOfStructure()) {
      orgRanges = makeSpec(sb, v.getParentStructure(), orgRanges);
      sb.append('.');
    }
    List<Range> ranges = (orgRanges == null) ? v.getRanges() : orgRanges;

    sb.append( v.isMemberOfStructure() ? v.getShortName() : v.getNameEscaped());

    if (!v.isVariableLength() && !v.isScalar()) { // sequences cant be sectioned
      sb.append('(');
      for (int count=0; count<v.getRank(); count++) {
        Range r = ranges.get(count);
        if (r == null)
          r = new Range( 0, v.getDimension(count).getLength());
        if (count>0) sb.append(", ");
        sb.append(r.first());
        sb.append(':');
        sb.append(r.last());
        sb.append(':');
        sb.append(r.stride());
      }
      sb.append(')');
    }

    return (orgRanges == null) ? null : ranges.subList(v.getRank(), ranges.size());
  }


  /**
   * Print all the data of the given Variable.
   * @param v variable to print
   * @param ct allow task to be cancelled; may be null.
   * @return String result
   * @throws IOException on write error
   */
  static public String printVariableData(VariableIF v, ucar.nc2.util.CancelTask ct) throws IOException {
    Array data = v.read();
    /* try {
      data = v.isMemberOfStructure() ? v.readAllStructures(null, true) : v.read();
    }
    catch (InvalidRangeException ex) {
      return ex.getMessage();
    } */

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    printArray( data, v.getName(), new PrintStream( bos), ct);
    return bos.toString();
  }

  /**
   * Print a section of the data of the given Variable.
   * @param v variable to print
   * @param sectionSpec string specification
   * @param ct allow task to be cancelled; may be null.
   * @return String result formatted data ouptut
   * @throws IOException on write error
   * @throws InvalidRangeException is specified section doesnt match variable shape
   */
  static public String printVariableDataSection(VariableIF v, String sectionSpec, ucar.nc2.util.CancelTask ct) throws IOException, InvalidRangeException {
    Array data = v.read(sectionSpec);

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    printArray( data, v.getName(), new PrintStream( bos), ct);
    return bos.toString();
  }

  /**
   * Print the data array.
   * @param array data to print.
   * @param name title the output.
   * @param out send output here.
   * @param ct allow task to be cancelled; may be null.
   */
  static public void printArray(Array array, String name, PrintStream out, CancelTask ct) {
    printArray( array, name, null, out, new Indent(2), ct);
  }

  static private void printArray(Array array, String name, String units, PrintStream out, Indent ilev, CancelTask ct) {
    if (ct != null && ct.isCancel()) return;

    if (name != null) out.print(ilev+name + " =");
    ilev.incr();

    if (array == null)
      throw new IllegalArgumentException("null array");

    if ((array instanceof ArrayChar) && (array.getRank() > 0) ) {
      printStringArray(out, (ArrayChar) array, ilev, ct);

    } else if (array.getElementType() == String.class) {
      printStringArray(out, (ArrayObject) array, ilev, ct);

    } else if (array.getElementType() == StructureData.class) {
      if (array.getSize() == 1)
        printStructureData( out, (StructureData) array.getObject( array.getIndex()), ilev, ct);
      else
        printStructureDataArray( out, array, ilev, ct);
    } else {
      printArray(array, out, ilev, ct);
    }

    if (units != null)
      out.print(" "+units);
    out.print("\n");
    ilev.decr();
  }

  static private void printArray(Array ma, PrintStream out, Indent indent, CancelTask ct) {
     if (ct != null && ct.isCancel()) return;

    int rank = ma.getRank();
    Index ima = ma.getIndex();

    // scalar
    if (rank == 0) {
      out.print( ma.getObject(ima).toString());
      return;
    }

    int [] dims = ma.getShape();
    int last = dims[0];

    out.print("\n" + indent + "{");

    if ((rank == 1) && (ma.getElementType() != StructureData.class)) {
      for(int ii = 0; ii < last; ii++) {
        out.print( ma.getObject(ima.set(ii)).toString());
        if (ii != last-1) out.print(", ");
        if (ct != null && ct.isCancel()) return;
      }
      out.print("}");
      return;
    }

    indent.incr();
    for(int ii = 0; ii < last; ii++) {
      Array slice = ma.slice(0, ii);
      printArray(slice, out, indent, ct);
      if(ii != last-1) out.print(",");
      if (ct != null && ct.isCancel()) return;
    }
    indent.decr();

    out.print("\n"+indent + "}");
  }

  static void printStringArray(PrintStream out, ArrayChar ma, Indent indent, ucar.nc2.util.CancelTask ct) {
    if (ct != null && ct.isCancel()) return;

    int rank = ma.getRank();

    if (rank == 1) {
      out.print( "  \""+ma.getString()+"\"");
      return;
    }

    if (rank == 2) {
      boolean first = true;
      for (ArrayChar.StringIterator iter = ma.getStringIterator(); iter.hasNext(); ) {
        if (!first) out.print(", ");
        out.print( "\""+iter.next()+"\"");
        first = false;
        if (ct != null && ct.isCancel()) return;
      }
      return;
    }

    int [] dims = ma.getShape();
    int last = dims[0];

    out.print("\n" + indent + "{");
    indent.incr();
    for(int ii = 0; ii < last; ii++) {
      ArrayChar slice = (ArrayChar) ma.slice(0, ii);
      printStringArray(out, slice, indent, ct);
      if(ii != last-1) out.print(",");
      if (ct != null && ct.isCancel()) return;
    }
    indent.decr();

    out.print("\n"+indent + "}");
  }

  static void printStringArray(PrintStream out, ArrayObject ma, Indent indent, ucar.nc2.util.CancelTask ct) {
    if (ct != null && ct.isCancel()) return;

    int rank = ma.getRank();
    Index ima = ma.getIndex();

    if (rank == 0) {
      out.print( "  \""+ma.getObject(ima)+"\"");
      return;
    }

    if (rank == 1) {
      boolean first = true;
      for (int i=0; i<ma.getSize(); i++) {
        if (!first) out.print(", ");
        out.print( "  \""+ma.getObject(ima.set(i))+"\"");
        first = false;
      }
      return;
    }

    int [] dims = ma.getShape();
    int last = dims[0];

    out.print("\n" + indent + "{");
    indent.incr();
    for(int ii = 0; ii < last; ii++) {
      ArrayObject slice = (ArrayObject) ma.slice(0, ii);
      printStringArray(out, slice, indent, ct);
      if(ii != last-1) out.print(",");
      //out.print("\n");
    }
    indent.decr();

    out.print("\n"+indent+ "}");
  }

  static private void printStructureDataArray(PrintStream out, Array array, Indent indent,
                                              ucar.nc2.util.CancelTask ct) {
    //int saveIndent = ilev.getIndentLevel();
    for (IndexIterator ii = array.getIndexIterator(); ii.hasNext(); ) {
      StructureData sdata = (StructureData) ii.next();
      out.println("\n" + indent + "{");
      printStructureData( out, sdata, indent, ct);
      //ilev.setIndentLevel(saveIndent);
      out.print(indent+ "} "+sdata.getName()+"("+ii+")");
      if (ct != null && ct.isCancel()) return;
    }
  }

  /**
   * Print contents of a StructureData.
   * @param out send output here.
   * @param  sdata StructureData to print.
   */
  static public void printStructureData(PrintStream out, StructureData sdata) {
     printStructureData(out, sdata, new Indent(2), null);
  }

  static private void printStructureData(PrintStream out, StructureData sdata, Indent indent, CancelTask ct) {
    indent.incr();
    //int saveIndent = ilev.getIndentLevel();
    for (StructureMembers.Member m : sdata.getMembers()) {
      Array sdataArray = sdata.getArray(m);
      //ilev.setIndentLevel(saveIndent);
      NCdump.printArray(sdataArray, m.getName(), m.getUnitsString(), out, indent, ct);
      if (ct != null && ct.isCancel()) return;
    }
    indent.decr();
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // standard NCML writing.

  /**
   * Write the NcML representation for a file.
   * Note that ucar.nc2.dataset.NcMLWriter has a JDOM implementation, for complete NcML.
   * This method implements only the "core" NcML for plain ole netcdf files.
   *
   * @param ncfile write NcML for this file
   * @param os write to this Output Stream.
   * @param showCoords show coordinate variable values.
   * @param uri use this for the uri attribute; if null use getLocation(). // ??
   * @throws IOException on write error
   */
  static public void writeNcML( NetcdfFile ncfile, java.io.OutputStream os, boolean showCoords, String uri) throws IOException {
     PrintStream out = new PrintStream( os);
     out.print("<?xml version='1.0' encoding='UTF-8'?>\n");
     out.print("<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'\n");

    // out.print("    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n");
    // out.print("    xsi:schemaLocation='http://www.unidata.edu/schemas/ncml-2.2 http://www.unidata.ucar.edu/schemas/ncml-2.2.xsd'\n");

    if (uri != null)
      out.print("    location='"+ StringUtil.quoteXmlAttribute(uri)+"' >\n\n");
    else
      out.print("    location='"+ StringUtil.quoteXmlAttribute( URLnaming.canonicalizeWrite(ncfile.getLocation()))+"' >\n\n");

    if (ncfile.getId() != null)
      out.print("    id='"+ StringUtil.quoteXmlAttribute(ncfile.getId())+"' >\n");
    if (ncfile.getTitle() != null)
      out.print("    title='"+ StringUtil.quoteXmlAttribute(ncfile.getTitle())+"' >\n");

    writeNcMLGroup( ncfile, ncfile.getRootGroup(), out, new Indent(2), showCoords);

    out.print("</netcdf>\n");
    out.flush();
  }

  static private void writeNcMLGroup( NetcdfFile ncfile, Group g, PrintStream out, Indent indent, boolean showCoords) throws IOException {
    if (g != ncfile.getRootGroup()) {
      out.print(indent);
      out.print("<group name='" + StringUtil.quoteXmlAttribute(g.getShortName()) + "' >\n");
    }
    indent.incr();

    List<Dimension> dimList = g.getDimensions();
    for (Dimension dim : dimList) {
      out.print(indent);
      out.print("<dimension name='" + StringUtil.quoteXmlAttribute(dim.getName()) + "' length='" + dim.getLength() + "'");
      if (dim.isUnlimited())
        out.print(" isUnlimited='true'");
      out.print(" />\n");
    }
    if (dimList.size() > 0)
      out.print("\n");

    List<Attribute> attList = g.getAttributes();
    for (Attribute att : attList) {
      writeNcMLAtt(att, out, indent);
    }
    if (attList.size() > 0)
      out.print("\n");

    for (Variable v : g.getVariables()) {
      if (v instanceof Structure) {
        writeNcMLStructure((Structure) v, out, indent);
      } else {
        writeNcMLVariable(v, out, indent, showCoords);
      }
    }

    // nested groups
    List groupList = g.getGroups();
    for (int i=0; i<groupList.size(); i++) {
      if (i > 0) out.print("\n");
      Group nested = (Group) groupList.get(i);
      writeNcMLGroup( ncfile, nested, out, indent, showCoords);
    }

    indent.decr();

    if (g != ncfile.getRootGroup()) {
      out.print(indent);
      out.print("</group>\n");
    }
  }

  static private void writeNcMLStructure( Structure s, PrintStream out, Indent indent) throws IOException {
    out.print(indent);
    out.print("<structure name='"+StringUtil.quoteXmlAttribute(s.getShortName()));

    // any dimensions?
    if (s.getRank() > 0) {
      writeNcMLDimension( s, out);
    }
    out.print(">\n");

    indent.incr();

    List<Attribute> attList = s.getAttributes();
    for (Attribute att : attList) {
      writeNcMLAtt(att, out, indent);
    }
    if (attList.size() > 0)
      out.print("\n");

    List<Variable> varList = s.getVariables();
    for (Variable v : varList) {
      writeNcMLVariable(v, out, indent, false);
    }

    indent.decr();

    out.print(indent);
    out.print("</structure>\n");
  }

  static private void writeNcMLVariable( Variable v, PrintStream out, Indent indent, boolean showCoords) throws IOException {
      out.print(indent);
      out.print("<variable name='"+StringUtil.quoteXmlAttribute(v.getShortName())+"' type='"+ v.getDataType()+"'");

      // any dimensions (scalers must skip this attribute) ?
      if (v.getRank() > 0) {
        writeNcMLDimension( v, out);
      }

      indent.incr();

      boolean closed = false;

      // any attributes ?
      java.util.List<Attribute> atts = v.getAttributes();
      if (atts.size() > 0) {
        out.print(" >\n");
        closed = true;
        for (Attribute att : atts) {
          writeNcMLAtt(att, out, indent);
        }
      }

      // print data ?
      if ((showCoords && v.isCoordinateVariable())) {
        if (!closed) {
           out.print(" >\n");
           closed = true;
        }
        writeNcMLValues(v, out, indent);
      }

      indent.decr();

      // close variable element
      if (!closed)
        out.print(" />\n");
      else {
        out.print(indent);
        out.print("</variable>\n");
      }

  }

  // LOOK anon dimensions
  static private void writeNcMLDimension( Variable v, PrintStream out) {
    out.print(" shape='");
    java.util.List<Dimension> dims = v.getDimensions();
    for (int j = 0; j < dims.size(); j++) {
      Dimension dim = dims.get(j);
      if (j != 0)
        out.print(" ");
      if (dim.isShared())
        out.print(StringUtil.quoteXmlAttribute(dim.getName()));
      else
        out.print(dim.getLength());
    }
    out.print("'");
  }

  @SuppressWarnings({"ObjectToString"})
  static private void writeNcMLAtt(Attribute att, PrintStream out, Indent indent) {
    out.print(indent);
    out.print("<attribute name='"+StringUtil.quoteXmlAttribute(att.getName())+"' value='");
    if (att.isString()) {
      for (int i=0; i<att.getLength(); i++) {
        if (i > 0) out.print("\\, "); // ??
        out.print( StringUtil.quoteXmlAttribute(att.getStringValue(i)));
      }
    } else {
     for (int i=0; i<att.getLength(); i++) {
        if (i > 0) out.print(" ");
        out.print(att.getNumericValue(i) + " ");
     }
     out.print("' type='"+att.getDataType());
    }
    out.print("' />\n");
  }

  static private int totalWidth = 80;
  static private void writeNcMLValues(Variable v, PrintStream out, Indent indent) throws IOException {
    Array data = v.read();
    int width = formatValues(indent+"<values>", out, 0, indent);

    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext())
      width = formatValues(ii.next()+" ", out, width, indent);
    formatValues("</values>\n", out, width, indent);
  }

  static private int formatValues(String s, PrintStream out, int width, Indent indent) {
    int len = s.length();
    if (len + width > totalWidth) {
      out.print("\n");
      out.print(indent);
      width = indent.toString().length();
    }
    out.print(s);
    width += len;
    return width;
  }

  private static char[] org = { '\b', '\f', '\n', '\r', '\t', '\\', '\'', '\"' };
  private static String[] replace = {"\\b", "\\f", "\\n", "\\r", "\\t", "\\\\", "\\\'", "\\\""};

  /**
   * Replace special characters '\t', '\n', '\f', '\r'.
   * @param s string to quote
   * @return equivilent string replacing special chars
   */
  static public String encodeString(String s) {
    return StringUtil.replace(s, org, replace);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  /**
     Main program.
    <p><strong>ucar.nc2.NCdump filename [-cdl | -ncml] [-c | -vall] [-v varName1;varName2;..] [-v varName(0:1,:,12)] </strong>
     <p>where: <ul>
   * <li> filename : path of any CDM readable file
   * <li> cdl or ncml: output format is CDL or NcML
   * <li> -vall : dump all variable data
   * <li> -c : dump coordinate variable data
   * <li> -v varName1;varName2; : dump specified variable(s)
   * <li> -v varName(0:1,:,12) : dump specified variable section
   * </ul>
   * Default is to dump the header info only.
   * @param args arguments
   */
  public static void main( String[] args) {

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
      NCdump.print(sbuff.toString(), System.out, null);

      // NCdump.print(args[0], System.out, false, false, false, false, null, null);

    } catch (java.io.IOException ioe) {
      ioe.printStackTrace();
    }
  }
}