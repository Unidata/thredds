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

package ucar.nc2;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;

import java.util.StringTokenizer;
import java.util.List;

/**
 * Nested Section Expression, allows subsetting of Structure member variables.
 *
 * @author caron
 * @since May 8, 2008
 * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/reference/SectionSpecification.html">SectionSpecification</a>
 */
public class ParsedSectionSpec {
  public Variable v; // the variable
  public Section section; // section for this variable
  public ParsedSectionSpec child;

  private ParsedSectionSpec(Variable v, Section section) {
    this.v = v;
    this.section = section;
    this.child = null;
  }

  /**
   * Parse a section specification String. These have the form:
   * <pre>
   *  section specification := selector | selector '.' selector
   *  selector := varName ['(' dims ')']
   *  varName := ESCAPED_STRING
   * <p/>
   *   dims := dim | dim, dims
   *   dim := ':' | slice | start ':' end | start ':' end ':' stride
   *   slice := INTEGER
   *   start := INTEGER
   *   stride := INTEGER
   *   end := INTEGER
   *   ESCAPED_STRING : must escape characters = ".("
   * </pre>
   * <p/>
   * Nonterminals are in lower case, terminals are in upper case, literals are in single quotes.
   * Optional components are enclosed between square braces '[' and ']'.
   *
   * @param ncfile          look for variable in here
   * @param variableSection the string to parse, eg "record(12).wind(1:20,:,3)"
   * @return return ParsedSectionSpec, aprsed representation of the variableSection String
   * @throws IllegalArgumentException       when token is misformed, or variable name doesnt exist in ncfile
   * @throws ucar.ma2.InvalidRangeException if section does not match variable shape
   * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/reference/SectionSpecification.html">SectionSpecification</a>
   */
  public static ParsedSectionSpec parseVariableSection(NetcdfFile ncfile, String variableSection) throws InvalidRangeException {
    StringTokenizer stoke = new StringTokenizer(variableSection, ".");
    String selector = stoke.nextToken();
    if (selector == null)
      throw new IllegalArgumentException("empty sectionSpec = " + variableSection);

    // parse each selector, find the inner variable
    ParsedSectionSpec outerV = parseVariableSelector(ncfile, selector);
    ParsedSectionSpec current = outerV;
    while (stoke.hasMoreTokens()) {
      selector = stoke.nextToken();
      current.child = parseVariableSelector( current.v,  selector);
      current = current.child;
    }

    return outerV;
  }

  private static boolean debugSelector = false;

  // parse variable name and index selector out of the selector String. variable name must be escaped
  private static ParsedSectionSpec parseVariableSelector(Object parent, String selector) throws InvalidRangeException {
    String varName, indexSelect = null;

    int pos1 = selector.indexOf('(');
    if (pos1 < 0) { // no selector
      varName = selector;
    } else {
      varName = selector.substring(0, pos1);
      int pos2 = selector.indexOf(')');
      indexSelect = selector.substring(pos1, pos2);
    }
    if (debugSelector)
      System.out.println(" parseVariableSection <" + selector + "> = <" + varName + ">, <" + indexSelect + ">");

    Variable v = null;
    if (parent instanceof NetcdfFile) {
      NetcdfFile ncfile = (NetcdfFile) parent;
      v = ncfile.findVariable(varName);

    } else if (parent instanceof Structure) {
      Structure s = (Structure) parent;
      v = s.findVariable(varName); // LOOK
    }
    if (v == null)
      throw new IllegalArgumentException(" cant find variable: " + varName + " in selector=" + selector);

    // get the selected Ranges, or all, and add to the list
    Section section;
    if (indexSelect != null) {
      section = new Section(indexSelect);
      section.setDefaults(v.getShape()); // Check section has no nulls, set from shape array.
    } else {
      section = new Section(v.getShape()); // all
    }

    return new ParsedSectionSpec(v, section);
  }

  /** Make section specification String from a range list for a Variable.
   * @param v for this Variable.
   * @param ranges list of Range. Must includes all parent structures. The list be null, meaning use all.
   *   Individual ranges may be null, meaning all for that dimension.
   * @return section specification String.
   * @throws InvalidRangeException is specified section doesnt match variable shape
   */
  public static String makeSectionSpecString(Variable v, List<Range> ranges) throws InvalidRangeException {
    StringBuilder sb = new StringBuilder();
    makeSpec(sb, v, ranges);
    return sb.toString();
  }

  private static List<Range> makeSpec(StringBuilder sb, Variable v, List<Range> orgRanges) throws InvalidRangeException {
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

}
