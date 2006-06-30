/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1998, California Institute of Technology.  
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged. 
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Jake Hamby, NASA/Jet Propulsion Laboratory
//         Jake.Hamby@jpl.nasa.gov
/////////////////////////////////////////////////////////////////////////////

package dods.dap;
import java.util.Enumeration;
import dods.util.SortedTable;
import dods.dap.parser.DASParser;
import dods.dap.parser.ParseException;
import java.io.*;

/**
 * The Data Attribute Structure is a set of name-value pairs used to
 * describe the data in a particular dataset.  The name-value pairs
 * are called the "attributes."  The values may be of any of the
 * DODS simple data types (DByte, DInt32, DUInt32, DFloat64, DString and
 * DURL), and may be scalar or vector.  (Note that all values are
 * actually stored as string data.)
 * <p>
 * A value may also consist of a set of other name-value pairs.  This
 * makes it possible to nest collections of attributes, giving rise
 * to a hierarchy of attributes.  DODS uses this structure to provide
 * information about variables in a dataset.
 * <p>
 * In the following example of a DAS, several of the attribute
 * collections have names corresponding to the names of variables in
 * a hypothetical dataset.  The attributes in that collection are said to
 * belong to that variable.  For example, the <code>lat</code> variable has an
 * attribute <code>units</code> of <code>degrees_north</code>.
 *
 * <blockquote><pre>
 *  Attributes {
 *      GLOBAL {
 *          String title "Reynolds Optimum Interpolation (OI) SST";
 *      }
 *      lat {
 *          String units "degrees_north";
 *          String long_name "Latitude";
 *          Float64 actual_range 89.5, -89.5;
 *      }
 *      lon {
 *          String units "degrees_east";
 *          String long_name "Longitude";
 *          Float64 actual_range 0.5, 359.5;
 *      }
 *      time {
 *          String units "days since 1-1-1 00:00:00";
 *          String long_name "Time";
 *          Float64 actual_range 726468., 729289.;
 *          String delta_t "0000-00-07 00:00:00";
 *      }
 *      sst {
 *          String long_name "Weekly Means of Sea Surface Temperature";
 *          Float64 actual_range -1.8, 35.09;
 *          String units "degC";
 *          Float64 add_offset 0.;
 *          Float64 scale_factor 0.0099999998;
 *          Int32 missing_value 32767;
 *      }
 *  }
 * </pre></blockquote>
 *
 * Attributes may have arbitrary names, although in most datasets it
 * is important to choose these names so a reader will know what they
 * describe.  In the above example, the <code>GLOBAL</code> attribute provides
 * information about the entire dataset.
 * <p>
 * Data attribute information is an important part of the the data
 * provided to a DODS client by a server, and the DAS is how this
 * data is packaged for sending (and how it is received). 
 *
 * @version $Revision: 1.1 $
 * @author jehamby
 * @see DDS 
 * @see AttributeTable
 * @see Attribute
 */
public class DAS implements Cloneable {

  /** A table containing AttributeTables with their names as a key */
  private SortedTable attr;

  /** Create a new empty <code>DAS</code>.  */
  public DAS() {
    attr = new SortedTable();
  }

  /**
   * Returns a clone of this <code>DAS</code>.  A deep copy is performed on
   * all <code>AttributeTable</code>s inside the <code>DAS</code>.
   *
   * @return a clone of this <code>DAS</code>.
   */
  public Object clone() {
    try {
      DAS d = (DAS)super.clone();
      d.attr = new SortedTable();
      for(int i=0; i<attr.size(); i++) {
	String key = (String)attr.getKey(i);
	AttributeTable element = (AttributeTable)attr.elementAt(i);
	// clone element (don't clone key because it's a read-only String)
	d.attr.put(key, element.clone());
      }
      return d;
    } catch (CloneNotSupportedException e) {
      // this shouldn't happen, since we are Cloneable
      throw new InternalError();
    }
  }

  /**
   * Returns an <code>Enumeration</code> of the attribute names in this
   * <code>DAS</code>.
   * Use the <code>getAttributeTable</code> method to get the
   * <code>AttributeTable</code> for a given name.
   *
   * @return an <code>Enumeration</code> of <code>String</code>.
   * @see DAS#getAttributeTable(String)
   */
  public final Enumeration getNames() {
    return attr.keys();
  }

  /**
   * Returns the <code>AttributeTable</code> with the given name.
   *
   * @param name the name of the <code>AttributeTable</code> to return.
   * @return the <code>AttributeTable</code> with the specified name, or null
   * if there is no matching <code>AttributeTable</code>.
   * @see AttributeTable
   */
  public final AttributeTable getAttributeTable(String name) {
    return (AttributeTable)attr.get(name);
  }

  /**
   * Adds an <code>AttributeTable</code> to the DAS.
   *
   * @param name the name of the <code>AttributeTable</code> to add.
   * @param a the <code>AttributeTable</code> to add.
   * @see AttributeTable
   */
  public void addAttributeTable(String name, AttributeTable a) {
    attr.put(name, a);
  }

  /**
   * Reads a <code>DAS</code> from the named <code>InputStream</code>.  This
   * method calls a generated parser to interpret an ASCII representation of a
   * <code>DAS</code>, and regenerate that <code>DAS</code> in memory.
   *
   * @param is the <code>InputStream</code> containing the <code>DAS</code> to
   *    parse.
   * @exception ParseException error in parser.
   * @exception DASException error in constructing <code>DAS</code>.
   * @exception dods.dap.parser.TokenMgrError error in token manager
   *    (unterminated quote).
   * @see dods.dap.parser.DASParser
   */
  public void parse(InputStream is) throws ParseException, DASException {
    DASParser dp = new DASParser(is);
    dp.Attributes(this);
  }

  /**
   * Print the <code>DAS</code> on the given <code>PrintWriter</code>.
   *
   * @param os the <code>PrintWriter</code> to use for output.
   */
  public void print(PrintWriter os) {
    os.println("Attributes {");
    for (Enumeration e = getNames(); e.hasMoreElements() ;) {
      String name = (String)e.nextElement();
      os.println("    " + name + " {");
      getAttributeTable(name).print(os, "        ");
      os.println("    }");
    }
    os.println("}");
    os.flush();
  }

  /**
   * Print the <code>DAS</code> on the given <code>OutputStream</code>.
   *
   * @param os the <code>OutputStream</code> to use for output.
   * @see DAS#print(PrintWriter)
   */
  public final void print(OutputStream os) {
    print(new PrintWriter(new BufferedWriter(new OutputStreamWriter(os))));
  }

  
}
