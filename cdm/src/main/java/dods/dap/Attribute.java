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
//
// -- 7/14/99 Modified by: Nathan Potter (ndp@oce.orst.edu)
// Added Support For DInt16, DUInt16, DFloat32.
// Added (and commented out) support for DBoolean.
// -- 7/14/99 ndp 
//  
/////////////////////////////////////////////////////////////////////////////

package dods.dap;
import java.util.Enumeration;
import java.util.Vector;

/**
 * An <code>Attribute</code> holds information about a single attribute in an
 * <code>AttributeTable</code>.  It has a type, and contains either a
 * <code>Vector</code> of <code>String</code>s containing the attribute's
 * values, or a reference to an <code>AttributeTable</code>, if the
 * <code>Attribute</code> is a container.  An <code>Attribute</code> may also
 * be created as an alias pointing to another <code>Attribute</code> of any
 * type, including container.
 *
 * @version $Revision: 1.1 $
 * @author jehamby
 * @see AttributeTable
 */
public class Attribute implements Cloneable {

  /** Unknown attribute type.  This is currently unused. */
  public static final int UNKNOWN = 1;

  /** Container attribute type.  This Attribute holds an AttributeTable. */
  public static final int CONTAINER = 2;

  /** Byte attribute type. Holds an unsigned Byte.  */
  public static final int BYTE = 3;

  /** Int16 attribute type.  Holds a signed Short. */
  public static final int INT16 = 4;

  /** UInt16 attribute type.  Holds an unsigned Short. */
  public static final int UINT16 = 5;

  /** Int32 attribute type.  Holds a signed Integer. */
  public static final int INT32 = 6;

  /** UInt32 attribute type.  Holds an unsigned Integer. */
  public static final int UINT32 = 7;

  /** Float32 attribute type.  Holds a Float. */
  public static final int FLOAT32 = 8;

  /** Float64 attribute type.  Holds a Double. */
   public static final int FLOAT64 = 9;

  /** String attribute type.  Holds a String. */
  public static final int STRING = 10;

  /** URL attribute type.  Holds a String representing a URL. */
  public static final int URL = 11;

  /** The type of the attribute. */
  private int type;

  /** The name of the attribute. */
  private String name;

  /** True if this <code>Attribute</code> is an alias. */
  private boolean is_alias;

  /**
   * If <code>is_alias</code> is true, the name of the <code>Attribute</code>
   * we are aliased to.
   */
  private String aliased_to;

  /** Either an AttributeTable or a Vector of String. */
  private Object attr;

    /**
     * Construct a container attribute.
     *
     * @param container the <code>AttributeTable</code> container.
     * @deprecated Use the ctor with the name.
     */
    public Attribute(AttributeTable container) {
	type = CONTAINER;
	is_alias = false;
	attr = container;
    }

    /**
     * Construct an <code>Attribute</code> with the given type and initial
     * value.
     *
     * @param type the type of attribute to create.  Use one of the type
     *    constants defined by this class.
     * @param name the name of the attribute.
     * @param value the initial value of this attribute.  Use the
     *    <code>appendValue</code> method to create a vector of values.
     * @param check if true, check the value and throw
     * AttributeBadValueException if it's not valid; if false do not check its
     * validity. 
     * @exception AttributeBadValueException thrown if the value is not a legal
     * member of type */
    public Attribute(int type, String name, String value, boolean check) 
	throws AttributeBadValueException {

	if (check)
	    dispatchCheckValue(type, value);

	this.type = type;
	this.name = name;
	is_alias = false;
	attr = new Vector();
	((Vector)attr).addElement(value);
    }

    /**
     * Construct an <code>Attribute</code> with the given type and initial
     * value. Checks the value of the attribute and throws an exception if
     * it's not valid.
     *
     * @param type the type of attribute to create.  Use one of the type
     *    constants defined by this class.
     * @param name the name of the attribute.
     * @param value the initial value of this attribute.  Use the
     *    <code>appendValue</code> method to create a vector of values.
     * @exception AttributeBadValueException thrown if the value is not a legal
     * member of type */
    public Attribute(int type, String name, String value) 
	throws AttributeBadValueException {

  	dispatchCheckValue(type, value);

	this.type = type;
	this.name = name;
	is_alias = false;
	attr = new Vector();
	((Vector)attr).addElement(value);
    }

  /**
   * Construct a container attribute.
   *
   * @param container the <code>AttributeTable</code> container.
   */
  public Attribute(String name, AttributeTable container) {
    type = CONTAINER;
    this.name = name;
    is_alias = false;
    attr = container;
  }

  /**
   * Construct an attribute aliased to the given name and contents.
   *
   * @param aliasedTo the name of the target <code>Attribute</code>.  This is
   *   necessary because <code>Attribute</code> doesn't know its own name
   *   (this is handled by the <code>DAS</code> or
   *   <code>AttributeTable</code> holding it.
   * @param attr the <code>Attribute</code> to point to.
   */
  public Attribute(String aliasedTo, Attribute attr) {
    this.type = attr.type;
    this.is_alias = true;
    this.aliased_to = aliasedTo;
    this.attr = attr.attr;  // share a reference to the other attribute's data
  }

  /**
   * Construct an empty attribute with the given type.
   *
   * @param type the type of attribute to create.  Use one of the type
   *    constants defined by this class, other than <code>CONTAINER</code>.
   * @exception IllegalArgumentException thrown if 
   *    <code>type</code> is <code>CONTAINER</code>. To construct an empty
   *    container attribute, first construct and empty AttributeTable and then
   *    use that to construct the Attribute.
   */
  public Attribute(String name, int type) throws IllegalArgumentException {
    this.type = type;
    this.name = name;
    if(type == CONTAINER)
      throw new IllegalArgumentException("can't construct Attribute(CONTAINER)");
    else
      attr = new Vector();
  }

  /**
   * Returns a clone of this <code>Attribute</code>.  A deep copy is performed
   * on all attribute values.
   *
   * @return a clone of this <code>Attribute</code>.
   */
  public Object clone() {
    try {
      Attribute a = (Attribute)super.clone();
      // assume type, is_alias, and aliased_to have been cloned already
      if(type == CONTAINER)
	a.attr = ((AttributeTable)attr).clone();
      else
	a.attr = ((Vector)attr).clone();
      return a;
    } catch (CloneNotSupportedException e) {
      // this shouldn't happen, since we are Cloneable
      throw new InternalError();
    }
  }

  /**
   * Returns the attribute type as a <code>String</code>.
   *
   * @return the attribute type <code>String</code>.
   */
  public final String getTypeString() {
    switch(type) {
    case CONTAINER: return "Container";
    case BYTE: return "Byte";
    case INT16: return "Int16";
    case UINT16: return "UInt16";
    case INT32: return "Int32";
    case UINT32: return "UInt32";
    case FLOAT32: return "Float32";
    case FLOAT64: return "Float64";
    case STRING: return "String";
    case URL: return "Url";
//    case BOOLEAN: return "Boolean";
    default: return "";
    }
  }

  /**
   * Returns the attribute type constant.
   *
   * @return the attribute type constant.
   */
  public final int getType() {
    return type;
  }
  
  /**
   * Returns the attribute's name.
   *
   * @return the attribute name.
   */
  public final String getName() {
    return name;
  }

  /**
   * Returns true if the attribute is a container.
   * @return true if the attribute is a container.
   */
  public final boolean isContainer() {
    return (type == CONTAINER);
  }

  /**
   * Returns true if the attribute is an alias.
   * @return true if the attribute is an alias.
   */
  public final boolean isAlias() {
    return is_alias;
  }

  /**
   * Returns the name of the attribute aliased to.
   * @return the name of the attribute aliased to.
   */
  public final String getAliasedTo() {
    return aliased_to;
  }

  /**
   * Returns the <code>AttributeTable</code> container.
   * @return the <code>AttributeTable</code> container.
   */
  public final AttributeTable getContainer() {
    return (AttributeTable)attr;
  }

  /**
   * Returns the values of this attribute as an <code>Enumeration</code>
   * of <code>String</code>.
   *
   * @return an <code>Enumeration</code> of <code>String</code>.
   */
  public final Enumeration getValues() {
    return ((Vector)attr).elements();
  }

  /**
   * Returns the attribute value at <code>index</code>.
   *
   * @param index the index of the attribute value to return.
   * @return the attribute <code>String</code> at <code>index</code>.
   */
  public final String getValueAt(int index) {
    return (String)((Vector)attr).elementAt(index);
  }

  /**
   * Append a value to this attribute. Always checks the validity of the
   * attribute's value.
   *
   * @param value the attribute <code>String</code> to add.
   * @exception AttributeBadValueException thrown if the value is not a legal
   *     member of type
   */
  public final void appendValue(String value) 
      throws AttributeBadValueException {
      appendValue(value, true);
  }

  /**
   * Append a value to this attribute.
   *
   * @param value the attribute <code>String</code> to add.
   * @param check if true, check the validity of he attribute's value, if
   * false don't.
   * @exception AttributeBadValueException thrown if the value is not a legal
   *     member of type
   */
  public final void appendValue(String value, boolean check) 
      throws AttributeBadValueException {

      if (check)
	  dispatchCheckValue(type, value);

      ((Vector)attr).addElement(value);
  }

  /**
   * Remove the <code>i</code>'th <code>String</code> from this attribute.
   *
   * @param index the index of the value to remove.
   */
  public final void deleteValueAt(int index) {
    ((Vector)attr).removeElementAt(index);
  }

    /**
     * Check if the value is legal for a given type.
     *
     * @param type the type of the value.
     * @param value the value <code>String</code>.
     * @exception AttributeBadValueException if the value is not a legal
     *    member of type
     */
    private static void dispatchCheckValue(int type, String value)
	throws AttributeBadValueException {
 
	switch (type) {

	  case BYTE:
	    if(!checkByte(value))
		throw new AttributeBadValueException("`" + value + "' is not a Byte value.");
	    break;

	  case INT16:
	    if(!checkShort(value))
		throw new AttributeBadValueException("`" + value + "' is not an Int16 value.");
	    break;

	  case UINT16:
	    if(!checkUShort(value))
		throw new AttributeBadValueException("`" + value + "' is not an UInt16 value.");
	    break;

	  case INT32:
	    if(!checkInt(value))
		throw new AttributeBadValueException("`" + value + "' is not an Int32 value.");
	    break;

	  case UINT32:
	    if(!checkUInt(value))
		throw new AttributeBadValueException("`" + value + "' is not an UInt32 value.");
	    break;

	  case FLOAT32:
	    if(!checkFloat(value))
		throw new AttributeBadValueException("`" + value + "' is not a Float32 value.");
	    break;

	  case FLOAT64:
	    if(!checkDouble(value))
		throw new AttributeBadValueException("`" + value + "' is not a Float64 value.");
	    break;

	    //    case BOOLEAN:
	    //      if(!checkBoolean(value))
	    //	throw new AttributeBadValueException("`" + value + "' is not a Boolean value.");
	    //      break;

	  default:
	    // Assume UNKNOWN, CONTAINER, STRING, and URL are okay.
	}
    }


    /**
     * Check if string is a valid Byte.
     * @param s the <code>String</code> to check.
     * @return true if the value is legal.
     */
    private static final boolean checkByte(String s) 
	throws AttributeBadValueException {
	try {
	    // Byte.parseByte() can't be used because values > 127 are allowed
	    short val = Short.parseShort(s);
	    if (val > 0xFF)
		return false;
	    else
		return true;
	}
	catch (NumberFormatException e) {
	    throw new AttributeBadValueException("`" + s + "' is not a Byte value.");
	}
    }

  /**
   * Check if string is a valid Int16.
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkShort(String s) {
    try {
	Short.parseShort(s);
	return true;
    }
    catch (NumberFormatException e) {
	return false;
    }
  }

  /**
   * Check if string is a valid UInt16.
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkUShort(String s) {
    // Note: Because there is no Unsigned class in Java, use Long instead.
    try {
	long val = Long.parseLong(s);
	if (val > 0xFFFFL)
	  return false;
	else
	  return true;
    }
    catch (NumberFormatException e) {
	return false;
    }
  }

  /**
   * Check if string is a valid Int32.
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkInt(String s) {
    try {
	Integer.parseInt(s);
	return true;
    }
    catch (NumberFormatException e) {
	return false;
    }
  }

  /**
   * Check if string is a valid UInt32.
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkUInt(String s) {
    // Note: Because there is no Unsigned class in Java, use Long instead.
    try {
	long val = Long.parseLong(s);
	if (val > 0xFFFFFFFFL)
	  return false;
	else
	  return true;
    }
    catch (NumberFormatException e) {
	return false;
    }
  }

  /**
   * Check if string is a valid Float32.
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkFloat(String s) {
    try {
	Float.valueOf(s);
	return true;
    }
    catch (NumberFormatException e) {
	if (s.equalsIgnoreCase("nan") || s.equalsIgnoreCase("inf"))
	    return true;

	return false;
    }
  }

  /**
   * Check if string is a valid Float64.
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkDouble(String s) {
    try {
	Double.valueOf(s);
	return true;
    }
    catch (NumberFormatException e) {
	if (s.equalsIgnoreCase("nan") || s.equalsIgnoreCase("inf"))
	    return true;

	return false;
    }
  }

/* NOTE: THis method is flawed think of a better way to do this 
	BEFORE you uncomment it!! The valueOf() method for 
	Boolean returns true if and only if the string passed 
	is equal to (ignoring case) to "true"
	Otherwise it returns false... A lousy test., UNLESS
	the JAVA implementation of a Boolean string works for
	you.

   * Check if string is a valid Boolean.
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   
  private static final boolean checkBoolean(String s) {
    try {
	Boolean.valueOf(s);
	return true;
    }
    catch (NumberFormatException e) {
	return false;
    }
  }

*/
}

// $Log: Attribute.java,v $
// Revision 1.1  2005/12/16 22:07:04  caron
// dods src under our CVS
//
// Revision 1.5  2002/05/30 23:24:58  jimg
// I added methods that provide a way to add attribute values without checking
// their type/value validity. This provides a way to add bad values in a
// *_dods_error attribute container so that attributes that are screwed up won't
// be lost (because they might make sense to a person, for example) or cause the
// whole DAS to break. The DAS parser (DASParser.jj) uses this new code.
//
