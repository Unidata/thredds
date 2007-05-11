// $Id:NCdump.java 51 2006-07-12 17:13:13Z caron $
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

package ucar.nc2;

import ucar.ma2.*;
import ucar.nc2.util.CancelTask;
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
 * @version $Id:NCdump.java 51 2006-07-12 17:13:13Z caron $
 */

public class NCdump {
  private static boolean debugSelector = false;
  private static String usage = "usage: NCdump <filename> [-cdl | -ncml] [-c | -vall] [-v varName1;varName2;..] [-v varName(0:1,:,12)]\n";

  /**
   * Print netcdf "header only" in CDL.
   * @param fileName open this file
   * @param out print to this stream
   * @return true if successful
   * @throws IOException
   */
  public static boolean printHeader(String fileName, OutputStream out) throws java.io.IOException {
    return print( fileName, out, false, false, false, false, null, null);
  }

  /**
   * print NcML representation of this netcdf file, showing coordinate variable data.
   * @param fileName open this file
   * @param out print to this stream
   * @return true if successful
   * @throws IOException
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
   * @throws IOException
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
   * @throws IOException
   */
  public static boolean print(NetcdfFile nc, OutputStream out, boolean showAll, boolean showCoords,
      boolean ncml, boolean strict, String varNames, ucar.nc2.util.CancelTask ct) throws java.io.IOException {

    boolean headerOnly = !showAll && (varNames == null);

    try {

      if (ncml)
        writeNcML(nc, out, showCoords, null); // output schema in NcML
      else if (headerOnly)
        nc.writeCDL(out, strict); // output schema in CDL form (like ncdump)
      else {
        PrintStream ps = new PrintStream( out);
        nc.toStringStart(ps, strict);
        ps.print(" data:\n");

        if (showAll) { // dump all data
          Iterator vi = nc.getVariables().iterator();
          while(vi.hasNext()) {
            Variable v = (Variable) vi.next();
            printArray( v.read(), v.getName(), ps, ct);
            if (ct != null && ct.isCancel()) return false;
          }
        }

        else if (showCoords) { // dump coordVars
          Iterator vi = nc.getVariables().iterator();
          while(vi.hasNext()) {
            Variable v = (Variable) vi.next();
            if (v.getCoordinateDimension() != null)
              printArray( v.read(), v.getName(), ps, ct);
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
              if (!showCoords || v.getCoordinateDimension() == null)
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
  }

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
   *
   * @see ucar.ma2.Range#parseSpec(String sectionSpec)
   **/
  public static CEresult parseVariableSection( NetcdfFile ncfile, String variableSection) throws InvalidRangeException {
    StringTokenizer stoke = new StringTokenizer(variableSection, ".");
    String selector = stoke.nextToken();
    if (selector == null)
      throw new IllegalArgumentException("empty sectionSpec = " + variableSection);

    // parse each selector, find the inner variable
    boolean hasInner = false;
    ArrayList fullSelection = new ArrayList();
    Variable innerV = parseVariableSelector(ncfile, selector, fullSelection);
    while (stoke.hasMoreTokens()) {
      selector = stoke.nextToken();
      innerV = parseVariableSelector(innerV, selector, fullSelection);
      hasInner = true;
    }
    return new CEresult( innerV, fullSelection, hasInner);
  }

  /** public by accident */
  static public class CEresult {
    public Variable v; // the variable
    public List ranges; // list of ranges for this variable
    public boolean hasInner;
    CEresult( Variable v, List ranges, boolean hasInner) {
      this.v = v;
      this.ranges = ranges;
      this.hasInner = hasInner;
    }
  }

  // parse variable name and index selector out of the selector String
  private static Variable parseVariableSelector( Object parent, String selector, ArrayList fullSelection)
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
      v = s.findVariable(varName);
    }
    if (v == null)
      throw new IllegalArgumentException(" cant find variable: "+varName+" in selector="+selector);

    // get the selected Ranges, or all, and add to the list
    List section;
    if (indexSelect != null)
      section = Range.parseSpec(indexSelect);
    else
      section = v.getRanges(); // all

     // Check section has no nulls, set from shape array. 
    Range.setDefaults( section, v.shape);

    fullSelection.addAll(section);

    return v;
  }


  /** Make section specification String from a range list for a Variable.
   * @param v for this Variable.
   * @param ranges list of Range. Must includes all parent structures. The list be null, meaning use all.
   *   Individual ranges may be null, meaning all for that dimension.
   * @return section specification String.
   * @throws InvalidRangeException
   */
  public static String makeSectionString(VariableIF v, List ranges) throws InvalidRangeException {
    StringBuffer sb = new StringBuffer();
    makeSpec(sb, v, ranges);
    return sb.toString();
  }

  private static List makeSpec(StringBuffer sb, VariableIF v, List orgRanges) throws InvalidRangeException {
    if (v.isMemberOfStructure()) {
      orgRanges = makeSpec(sb, v.getParentStructure(), orgRanges);
      sb.append('.');
    }
    List ranges = (orgRanges == null) ? v.getRanges() : orgRanges;

    sb.append( v.isMemberOfStructure() ? v.getShortName() : v.getName());

    if (!v.isVariableLength() && !v.isScalar()) { // sequences cant be sectioned
      sb.append('(');
      for (int count=0; count<v.getRank(); count++) {
        Range r = (Range) ranges.get(count);
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
   * @throws IOException
   */
  static public String printVariableData(VariableIF v, ucar.nc2.util.CancelTask ct) throws IOException {
    Array data = null;
    try {
      data = v.isMemberOfStructure() ? v.readAllStructures(null, true) : v.read();
    }
    catch (InvalidRangeException ex) {
      return ex.getMessage();
    }

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    printArray( data, v.getName(), new PrintStream( bos), ct);
    return bos.toString();
  }

  /**
   * Print a section the data of the given Variable.
   * @param v variable to print
   * @param ct allow task to be cancelled; may be null.
   * @return String result
   * @throws IOException
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
    for (Iterator iter = sdata.getMembers().iterator(); iter.hasNext(); ) {
      StructureMembers.Member m = (StructureMembers.Member) iter.next();
      Array sdataArray = sdata.getArray(m);
      //ilev.setIndentLevel(saveIndent);
      NCdump.printArray(sdataArray, m.getName(), m.getUnitsString(), out, indent, ct);
      if (ct != null && ct.isCancel()) return;
   }
    indent.decr();
  }

  /**
   * Maintains indentation level for printing nested structures.
   */
  private static class Indent {
    private int nspaces = 0;
    private int level = 0;
    private StringBuffer blanks;
    private String indent = "";

    // nspaces = how many spaces each level adds.
    // max 100 levels
    public Indent(int nspaces) {
      this.nspaces = nspaces;
      blanks = new StringBuffer();
      for (int i=0; i < 100*nspaces; i++)
        blanks.append(" ");
    }

    public Indent incr() {
      level++;
      setIndentLevel( level);
      return this;
    }

    public Indent decr() {
      level--;
      setIndentLevel( level);
      return this;
    }

    public String toString() { return indent; }

    public void setIndentLevel(int level) {
      this.level = level;
      indent = blanks.substring(0, level * nspaces);
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // standard NCML writing.

  /**
   * Write the NcML representation.
   * Note that ucar.nc2.dataset.NcMLWriter has a JDOM implementation, for complete NcML.
   * This method implements only the "core" NcML for plain ole netcdf files.
   *
   * @param os write to this Output Stream.
   * @param showCoords show coordinate variable values.
   * @param uri use this for the uri attribute; if null use getLocation(). // ??
   * @throws IOException
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
      out.print("    location='"+ StringUtil.quoteXmlAttribute(ncfile.getLocation())+"' >\n\n");
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

    List dimList = g.getDimensions();
    for (int i=0; i<dimList.size(); i++) {
      Dimension dim = (Dimension) dimList.get(i);
      out.print(indent);
      out.print("<dimension name='"+StringUtil.quoteXmlAttribute(dim.getName())+"' length='"+dim.getLength()+"'");
      if (dim.isUnlimited())
        out.print(" isUnlimited='true'");
      out.print(" />\n");
    }
    if (dimList.size() > 0)
      out.print("\n");

    List attList = g.getAttributes();
    Iterator attIter = attList.iterator();
    while (attIter.hasNext()) {
      Attribute att = (Attribute) attIter.next();
      writeNcMLAtt(att, out, indent);
    }
    if (attList.size() > 0)
      out.print("\n");

    List varList = g.getVariables();
    for (int i=0; i<varList.size(); i++) {
      Variable v = (Variable) varList.get(i);

      if (v instanceof Structure) {
        writeNcMLStructure( (Structure) v, out, indent);
      } else {
        writeNcMLVariable( v, out, indent, showCoords);
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

    List attList = s.getAttributes();
    Iterator attIter = attList.iterator();
    while (attIter.hasNext()) {
      Attribute att = (Attribute) attIter.next();
      writeNcMLAtt(att, out, indent);
    }
    if (attList.size() > 0)
      out.print("\n");

    List varList = s.getVariables();
    for (int i=0; i<varList.size(); i++) {
      Variable v = (Variable) varList.get(i);
      writeNcMLVariable( v, out, indent, false);
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
      java.util.List atts = v.getAttributes();
      if (atts.size() > 0) {
        if (!closed) {
           out.print(" >\n");
           closed = true;
        }
        Iterator iter = atts.iterator();
        while (iter.hasNext()) {
          Attribute att = (Attribute) iter.next();
          writeNcMLAtt(att, out, indent);
        }
      }

      // print data ?
      if ((showCoords && v.getCoordinateDimension() != null)) {
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
    java.util.List dims = v.getDimensions();
    for (int j = 0; j < dims.size(); j++) {
      Dimension dim = (Dimension) dims.get(j);
      if (j != 0)
        out.print(" ");
      if (dim.isShared())
        out.print(StringUtil.quoteXmlAttribute(dim.getName()));
      else
        out.print(dim.getLength());
    }
    out.print("'");
  }

  static private void writeNcMLAtt(Attribute att, PrintStream out, Indent indent) {
    out.print(indent);
    out.print("<attribute name='"+StringUtil.quoteXmlAttribute(att.getName())+"' value='");
    if (att.isString()) {
      for (int i=0; i<att.getLength(); i++) {
        if (i > 0) out.print("\\, "); // ??
        out.print( att.getStringValue(i));
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
   * Deafult is to dump the header info only.
   */
  public static void main( String[] args) {

    if (args.length == 0) {
      System.out.println(usage);
      return;
    }

    StringBuffer sbuff = new StringBuffer();
    for (int i=0; i<args.length; i++) {
      sbuff.append( args[i]);
      sbuff.append( " ");
    }

    try {
      NCdump.print(sbuff.toString(), System.out, null);

      // NCdump.print(args[0], System.out, false, false, false, false, null, null);

    } catch (java.io.IOException ioe) {
      ioe.printStackTrace();
    }
  }
}