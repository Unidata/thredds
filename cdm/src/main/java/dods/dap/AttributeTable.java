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
import java.io.*;

/**
 * An <code>AttributeTable</code> stores a set of names and, for each name,
 * an <code>Attribute</code> object.  For more information on the types of
 * data which can be stored in an attribute, including aliases and other
 * <code>AttributeTable</code> objects, see the documentation for
 * <code>Attribute</code>.
 * <p>
 * The attribute tables have a standard printed representation.  There is a
 * <code>print</code> method for writing this form and a <code>parse</code>
 * method for reading the printed form.
 * <p>
 * An <code>AttributeTable</code>'s print representation might look like:
 * <pre>
 *   String long_name "Weekly Means of Sea Surface Temperature";
 * </pre>
 * or
 * <pre>
 *   actual_range {
 *       Float64 min -1.8;
 *       Float64 max 35.09;
 *   }
 * </pre>
 * or
 * <pre>
 *   String Investigators "Cornillon", "Fleirl", "Watts";
 * </pre>
 * or
 * <pre>
 *   Alias New_Attribute Old_Attribute;
 * </pre>
 * Here, <em>long_name</em> and <em>Investigators</em> are
 * simple attributes, <em>actual_range</em> is a container attribute, and
 * <em>New_Attribute</em> is an alias pointing to <em>Old_Attribute</em>.
 *
 * @version $Revision: 1.1 $
 * @author jehamby
 * @see DAS
 * @see Attribute
 */
public class AttributeTable implements Cloneable {
    private static final boolean _Debug = false;

    /** A table of Attributes with their names as a key */
    private SortedTable attr;

    /** What's the name of this table? */
    private String name;

    /** Create a new empty <code>AttributeTable</code>. 
	@deprecated */
    public AttributeTable() {
	attr = new SortedTable();
    }

    /** Create a new empty <code>AttributeTable</code>. */
    public AttributeTable(String name) {
	this.name = name;
	attr = new SortedTable();
    }

    /**
     * Returns a clone of this <code>AttributeTable</code>.  A deep copy is
     * performed on all <code>Attribute</code> and <code>AttributeTable</code>
     * objects inside the <code>AttributeTable</code>.
     *
     * @return a clone of this <code>AttributeTable</code>.
     */
    public Object clone() {
	try {
	    AttributeTable at = (AttributeTable)super.clone();
	    at.name = name;
	    at.attr = new SortedTable();
	    for(int i=0; i<attr.size(); i++) {
		String key = (String)attr.getKey(i);
		Attribute element = (Attribute)attr.elementAt(i);
		// clone element (don't clone key because it's a read-only String)
		at.attr.put(key, element.clone());
	    }
	    return at;
	} catch (CloneNotSupportedException e) {
	    // this shouldn't happen, since we are Cloneable
	    throw new InternalError();
	}
    }

  /** Returns the name of this AttributeTable. */
  public final String getName() {
    return name;
  }

  /**
   * Returns an <code>Enumeration</code> of the attribute names in this
   * <code>AttributeTable</code>.
   * Use the <code>getAttribute</code> method to get the
   * <code>Attribute</code> for a given name.
   *
   * @return an <code>Enumeration</code> of <code>String</code>.
   * @see AttributeTable#getAttribute(String)
   */
  public final Enumeration getNames() {
    return attr.keys();
  }

  /**
   * Returns the <code>Attribute</code> which matches name.
   *
   * @param name the name of the <code>Attribute</code> to return.
   * @return the <code>Attribute</code> with the specified name, or null
   * if there is no matching <code>Attribute</code>.
   * @see Attribute
   */
  public final Attribute getAttribute(String name) {
    return (Attribute)attr.get(name);
  }

    /**
    * Adds an attribute to the table.  If the given name already
    * refers to an attribute, and the attribute has a vector value,
    * the given value is appended to the attribute vector.  Calling
    * this function repeatedly is the way to create an attribute
    * vector.
    * <p>
    * The function throws an exception if the attribute is a
    * container, or if the type of the input value does not match the
    * existing attribute's type and the <code>check</code> parameter
    * is true.  Use the <code>appendContainer</code> method to add container
    * attributes. 
    *
    * @param name The name of the attribute to add or modify.
    * @param type The type code of the attribute to add or modify.
    * @param value The value to add to the attribute table.
    * @param check Check the validity of the attribute's value? 
    * @exception AttributeExistsException thrown if an Attribute with the same
    *    name, but a different type was previously defined.
    * @exception AttributeBadValueException thrown if the value is not a legal
    *    member of type
    * @see AttributeTable#appendContainer(String)
    */
    public final void appendAttribute(String name, int type, String value,
				      boolean check)
	throws AttributeExistsException, AttributeBadValueException {

        Attribute a = (Attribute)attr.get(name);

        if (a != null && (type != a.getType())) {

            // type mismatch error
            throw new AttributeExistsException("`" + name 
			   + "' previously defined with a different type.");

        } else if (a != null) {

            a.appendValue(value, check);

        } else {

            a = new Attribute(type, name, value, check);
            attr.put(name, a);
        }
    }

    /**
    * Adds an attribute to the table.  If the given name already
    * refers to an attribute, and the attribute has a vector value,
    * the given value is appended to the attribute vector.  Calling
    * this function repeatedly is the way to create an attribute
    * vector.
    * <p>
    * The function throws an exception if the attribute is a
    * container, or if the type of the input value does not match the
    * existing attribute's type.  Use the <code>appendContainer</code>
    * method to add container attributes.
    *
    * @param name The name of the attribute to add or modify.
    * @param type The type code of the attribute to add or modify.
    * @param value The value to add to the attribute table.
    * @exception AttributeExistsException thrown if an Attribute with the same
    *    name, but a different type was previously defined.
    * @exception AttributeBadValueException thrown if the value is not a legal
    *    member of type
    * @see AttributeTable#appendContainer(String)
    */
    public final void appendAttribute(String name, int type, String value)
	throws AttributeExistsException, AttributeBadValueException {
	appendAttribute(name, type, value, true);
    }

    /**
    * Create and append an attribute container to the table.
    * A container is another <code>AttributeTable</code> object.
    *
    * @param name the name of the container to add.
    * @return A pointer to the new <code>AttributeTable</code> object, or null
    *    if a container by that name already exists.
    */
    public final AttributeTable appendContainer(String name) {
        // return null if name already exists
        if (attr.get(name) != null)
            return null;

        AttributeTable at = new AttributeTable(name);
        Attribute a = new Attribute(name, at);
        attr.put(name, a);
        return at;
    }

    /**
    * Add an alias to the current table.
    *
    * @param alias The alias to insert into the attribute table.
    * @param name The name of the already-existing attribute to which
    *    the alias will refer.
    * @exception NoSuchAttributeException thrown if the existing attribute
    *    could not be found.
    * @exception AttributeExistsException thrown if the new alias has the same
    *    name as an existing attribute.
    */
    public final void addAlias(String alias, String name)
       throws NoSuchAttributeException, AttributeExistsException {
        // return false if alias already exists.
        if (attr.get(alias) != null)
            throw new AttributeExistsException("Could not alias `" + name +
                                                 "' and `" + alias + "'.");

        // Make sure name exists.
        Attribute a = (Attribute)attr.get(name);
        if (a == null)
            throw new NoSuchAttributeException("Could not alias `" + name +
					       "' and `" + alias + "'.");

        Attribute newAttr = new Attribute(name, a);
        attr.put(alias, newAttr);
    }

    /**
    * Delete the attribute named <code>name</code>.
    *
    * @param name The name of the attribute to delete.  This can be an
    *    attribute of any type, including containers.
    */
    public final void delAttribute(String name) {
        attr.remove(name);
    }

    /**
    * Delete the attribute named <code>name</code>.  If the attribute has a
    * vector value, delete the <code>i</code>'th element of the vector.
    *
    * @param name The name of the attribute to delete.  This can be an
    *    attribute of any type, including containers.
    * @param i If the named attribute is a vector, and <code>i</code> is
    *   non-negative, the <code>i</code>'th entry in the vector is deleted.
    *   If <code>i</code> equals -1, the entire attribute is deleted.
    * @see AttributeTable#delAttribute(String)
    */
    public final void delAttribute(String name, int i) {

        if (i == -1) {  // delete the whole attribute

            attr.remove(name);

        }
        else {

            Attribute a = (Attribute)attr.get(name);

            if (a != null) {

                if (a.isContainer()) {

                    attr.remove(name);  // delete the entire container

                }
                else {

                    a.deleteValueAt(i);

                }
            }
        }
    }

    /**
    * Print the attribute table on the given <code>PrintWriter</code>.
    *
    * @param os the <code>PrintWriter</code> to use for output.
    * @param pad the number of spaces to indent each line.
    */
    public void print(PrintWriter os, String pad) {

        if (_Debug) System.out.println("Entered AttributeTable.print()");

        for (Enumeration e = getNames(); e.hasMoreElements() ;) {

            String name = (String)e.nextElement();
            Attribute a = getAttribute(name);

            if (a.isAlias()) {
                if (_Debug) System.out.println("  Attribute \"" + name + "\" is an Alias.");

                os.println(pad + "Alias " + name + " " + a.getAliasedTo() + ";");

            }
            else {

                if (a.isContainer()) {
                    if (_Debug) System.out.println("  Attribute \"" + name + "\" is a Container.");
                    os.println(pad + name + " {");
                    ((AttributeTable)a.getContainer()).print(os, pad + "    ");
                    os.println(pad + "}");
                }
                else {
                    if (_Debug) System.out.println("    Printing Attribute \"" + name + "\".");

                    os.print(pad + a.getTypeString() + " " + name + " ");
                    Enumeration es = a.getValues();
                    String val = (String)es.nextElement();  // get first element

                    while(es.hasMoreElements()) {  // lookahead one element
                        os.print(val + ", ");
                        val = (String)es.nextElement();
                    }
                    os.println(val + ";");  // print last element
                }
            }
        }
        os.flush();
        if (_Debug) System.out.println("Leaving AttributeTable.print()");
    }

    /**
    * Print the attribute table on the given <code>OutputStream</code>.
    *
    * @param os the <code>OutputStream</code> to use for output.
    * @param pad the number of spaces to indent each line.
    */
    public final void print(OutputStream os, String pad) {
        print(new PrintWriter(new BufferedWriter(new OutputStreamWriter(os))), pad);
    }

    /**
    * Print the attribute table on the given <code>PrintWriter</code> with
    * four spaces of indentation.
    *
    * @param os the <code>PrintWriter</code> to use for output.
    */
    public final void print(PrintWriter os) {
        print(os, "    ");
    }

    /**
    * Print the attribute table on the given <code>OutputStream</code> with
    * four spaces of indentation.
    *
    * @param os the <code>OutputStream</code> to use for output.
    */
    public final void print(OutputStream os) {
        print(os, "    ");
    }
}

// $Log: AttributeTable.java,v $
// Revision 1.1  2005/12/16 22:07:04  caron
// dods src under our CVS
//
// Revision 1.7  2002/05/30 23:25:57  jimg
// I added methods that provide a way to add attribues without the usual
// type/value checking. See today's log in Attribute.java for more info.
//
