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

import ucar.unidata.util.EscapeStrings;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;

import java.util.List;

/**
 * Parsed Section Expression, allows nesting for subsetting of Structure member variables.
 *
 * @author caron
 * @since May 8, 2008
 * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/reference/SectionSpecification.html">SectionSpecification</a>
 */
public class ParsedSectionSpec {
  private static boolean debugSelector = false;

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
    List<String> tokes = EscapeStrings.tokenizeEscapedName(variableSection);
    if (tokes.size() == 0)
      throw new IllegalArgumentException("empty sectionSpec = " + variableSection);

    String selector = tokes.get(0);
    ParsedSectionSpec outerV = parseVariableSelector(ncfile, selector);

    // parse each selector, find the inner variable
    ParsedSectionSpec current = outerV;
    for (int i=1; i<tokes.size(); i++) {
      selector = tokes.get(i);
      current.child = parseVariableSelector( current.v,  selector);
      current = current.child;
    }

    return outerV;
  }

  // selector := varFullNameEsc(indexSelect) or memberNameEsc(indexSelect)
  // parse variable name and index selector out of the selector String. variable name must be escaped
  private static ParsedSectionSpec parseVariableSelector(Object parent, String selector) throws InvalidRangeException {
    String varNameEsc, indexSelect = null;

    int pos1 = EscapeStrings.indexOf(selector, '(');
    if (pos1 < 0) { // no index
      varNameEsc = selector;
    } else {
      varNameEsc = selector.substring(0, pos1);
      int pos2 = selector.indexOf(')', pos1+1);
      indexSelect = selector.substring(pos1, pos2);
    }
    if (debugSelector)
      System.out.println(" parseVariableSection <" + selector + "> = <" + varNameEsc + ">, <" + indexSelect + ">");

    Variable v = null;
    if (parent instanceof NetcdfFile) { // then varNameEsc = varFullNameEsc (i.e. includes groups)
      NetcdfFile ncfile = (NetcdfFile) parent;
      v = ncfile.findVariable(varNameEsc);

    } else if (parent instanceof Structure) { // then varNameEsc = memberNameEsc (i.e. includes groups)
      Structure s = (Structure) parent;
      v = s.findVariable( NetcdfFile.unescapeName(varNameEsc) ); // s.findVariable wants unescaped version
    }
    if (v == null)
      throw new IllegalArgumentException(" cant find variable: " + varNameEsc + " in selector=" + selector);
    
    if (v.getDataType() == DataType.SEQUENCE)
      indexSelect = null; // ignore whatever was sent

    // get the selected Ranges, or all, and add to the list
    Section section;
    if (indexSelect != null) {
      section = new Section(indexSelect);
      section = Section.fill(section, v.getShape()); // Check section has no nulls, set from shape array.
    } else {
      section = v.getShapeAsSection(); // all
    }

    return new ParsedSectionSpec(v, section);
  }

  /**
   * Make section specification String from a range list for a Variable.
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

    sb.append(v.isMemberOfStructure() ? NetcdfFile.escapeNameSectionSpec(v.getShortName()) : NetcdfFile.makeFullNameEscapedSectionSpec(v));

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

  ///////////////////////////////////////////////////////////////////////////
  public final Variable v;        // the variable
  public final Section section;   // section for this variable, filled in from variable if needed
  public ParsedSectionSpec child; // if not null, v is a Structure, and this is one of its members

  private ParsedSectionSpec(Variable v, Section section) {
    this.v = v;
    this.section = section;
    this.child = null;
  }

  @Override
  public String toString() {
    return "ParsedSectionSpec{" +
            "v=" + v.getFullName() +
            ", section=" + section +
            ", child=" + child +
            '}';
  }
}
