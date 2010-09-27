/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

package opendap.dap;

import opendap.util.EscapeStrings;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Serializable;

/**
 * An <code>Attribute</code> holds information about a single attribute in an
 * <code>AttributeTable</code>.  It has a type, and contains either a
 * <code>Vector</code> of <code>String</code>s containing the attribute's
 * values, or a reference to an <code>AttributeTable</code>, if the
 * <code>Attribute</code> is a container.  An <code>Attribute</code> may also
 * be created as an alias pointing to another <code>Attribute</code> of any
 * type, including container.
 *
 * @author jehamby
 * @version $Revision: 22638 $
 * @see AttributeTable
 */
public class Attribute implements Cloneable, Serializable {

  static final long serialVersionUID = 1;

  private static final boolean _Debug = false;
  private static final boolean DebugValueChecking = false;

  /**
   * Unknown attribute type.  This is currently unused.
   */
  public static final int UNKNOWN = 0;

  /**
   * Alias attribute type.  This is an attribute that works like a
   * UNIX style soft link to another attribute.
   */
  public static final int ALIAS = 1;

  /**
   * Container attribute type.  This Attribute holds an AttributeTable.
   */
  public static final int CONTAINER = 2;

  /**
   * Byte attribute type. Holds an unsigned Byte.
   */
  public static final int BYTE = 3;

  /**
   * Int16 attribute type.  Holds a signed Short.
   */
  public static final int INT16 = 4;

  /**
   * UInt16 attribute type.  Holds an unsigned Short.
   */
  public static final int UINT16 = 5;

  /**
   * Int32 attribute type.  Holds a signed Integer.
   */
  public static final int INT32 = 6;

  /**
   * UInt32 attribute type.  Holds an unsigned Integer.
   */
  public static final int UINT32 = 7;

  /**
   * Float32 attribute type.  Holds a Float.
   */
  public static final int FLOAT32 = 8;

  /**
   * Float64 attribute type.  Holds a Double.
   */
  public static final int FLOAT64 = 9;

  /**
   * String attribute type.  Holds a String.
   */
  public static final int STRING = 10;

  /**
   * URL attribute type.  Holds a String representing a URL.
   */
  public static final int URL = 11;

  /**
   * The type of the attribute.
   */
  private int type;

  /**
   * The name of the attribute.
   */
  private String name;
  private String nameEncoded;

  /**
   * Either an AttributeTable or a Vector of String.
   */
  private Object attr;

  /**
   * Construct a container attribute.
   *
   * @param container the <code>AttributeTable</code> container.
   * @deprecated Use the ctor with the name.
   */
  public Attribute(AttributeTable container) {
    type = CONTAINER;
    attr = container;
  }

  /**
   * Construct an <code>Attribute</code> with the given type and initial
   * value.
   *
   * @param type  the type of attribute to create.  Use one of the type
   *              constants defined by this class.
   * @param name  the name of the attribute.
   * @param value the initial value of this attribute.  Use the
   *              <code>appendValue</code> method to create a vector of values.
   * @param check if true, check the value and throw
   *              AttributeBadValueException if it's not valid; if false do not check its
   *              validity.
   * @throws AttributeBadValueException thrown if the value is not a legal
   *                                    member of type
   */
  public Attribute(int type, String name, String value, boolean check)
          throws AttributeBadValueException {

    value = fixnan(value);
    if (check)
      dispatchCheckValue(type, value);

    this.type = type;
    setName(name);
    attr = new Vector();
    ((Vector) attr).addElement(value);
  }

  /**
   * Construct an <code>Attribute</code> with the given type and initial
   * value. Checks the value of the attribute and throws an exception if
   * it's not valid.
   *
   * @param type  the type of attribute to create.  Use one of the type
   *              constants defined by this class.
   * @param name  the name of the attribute.
   * @param value the initial value of this attribute.  Use the
   *              <code>appendValue</code> method to create a vector of values.
   * @throws AttributeBadValueException thrown if the value is not a legal
   *                                    member of type
   */
  public Attribute(int type, String name, String value)
          throws AttributeBadValueException {

    value = fixnan(value);
    dispatchCheckValue(type, value);

    this.type = type;
    setName(name);
    attr = new Vector();
    ((Vector) attr).addElement(value);
  }

  /**
   * Construct a container attribute.
   *
   * @param container the <code>AttributeTable</code> container.
   */
  public Attribute(String name, AttributeTable container) {

      type = CONTAINER;
    setName(name);
    attr = container;
  }


  /**
   * Construct an empty attribute with the given type.
   *
   * @param type the type of attribute to create.  Use one of the type
   *             constants defined by this class, other than <code>CONTAINER</code>.
   * @throws IllegalArgumentException thrown if
   *                                  <code>type</code> is <code>CONTAINER</code>. To construct an empty
   *                                  container attribute, first construct and empty AttributeTable and then
   *                                  use that to construct the Attribute.
   */
  public Attribute(String name, int type) throws IllegalArgumentException {
    this.type = type;
    setName(name);
    if (type == CONTAINER)
      throw new IllegalArgumentException("Can't construct an Attribute(CONTAINER)");
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
      Attribute a = (Attribute) super.clone();
      a.name = name;
      // assume type, is_alias, and aliased_to have been cloned already
      if (type == CONTAINER)
        a.attr = ((AttributeTable) attr).clone();
      else
        a.attr = ((Vector) attr).clone();
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
    switch (type) {
      case CONTAINER:
        return "Container";
      case ALIAS:
        return "Alias";
      case BYTE:
        return "Byte";
      case INT16:
        return "Int16";
      case UINT16:
        return "UInt16";
      case INT32:
        return "Int32";
      case UINT32:
        return "UInt32";
      case FLOAT32:
        return "Float32";
      case FLOAT64:
        return "Float64";
      case STRING:
        return "String";
      case URL:
        return "Url";
        //    case BOOLEAN: return "Boolean";
      default:
        return "";
    }
  }

  /**
   * Returns the attribute type as a <code>String</code>.
   *
   * @return the attribute type <code>String</code>.
   */
  public static final int getTypeVal(String s) {

    if (s.equalsIgnoreCase("Container"))
      return CONTAINER;
    else if (s.equalsIgnoreCase("Byte"))
      return BYTE;
    else if (s.equalsIgnoreCase("Int16"))
      return INT16;
    else if (s.equalsIgnoreCase("UInt16"))
      return UINT16;
    else if (s.equalsIgnoreCase("Int32"))
      return INT32;
    else if (s.equalsIgnoreCase("UInt32"))
      return UINT32;
    else if (s.equalsIgnoreCase("Float32"))
      return FLOAT32;
    else if (s.equalsIgnoreCase("Float64"))
      return FLOAT64;
    else if (s.equalsIgnoreCase("String"))
      return STRING;
    else if (s.equalsIgnoreCase("URL"))
      return URL;
    else
      return UNKNOWN;
  }

  /**
   * Returns the attribute type constant.
   *
   * @return the attribute type constant.
   */
  public int getType() {
    return type;
  }

  /**
   * Returns the attribute's name.
   *
   * @return the attribute name.
   */
  public String getName() {
    return nameEncoded;
  }

  /**
   * Sets the attribute's name.
   */
  public void setName(String n) {
    nameEncoded = n;
    setClearName(EscapeStrings.www2id(n));
  }

  /**
   * Returns the attribute's name.
   *
   * @return the attribute name.
   */
  public String getClearName() {
    return name;
  }

  /**
   * Sets the attribute's name.
   */
  public void setClearName(String n) {
    name = n;
    nameEncoded = EscapeStrings.id2www(n);
  }

  /**
   * Returns true if the attribute is a container.
   *
   * @return true if the attribute is a container.
   */
  public boolean isContainer() {
    return (type == CONTAINER);
  }

  /**
   * Returns true if the attribute is an alias.
   *
   * @return true if the attribute is an alias.
   */
  public boolean isAlias() {
    return (false);
  }


  /**
   * Returns the <code>AttributeTable</code> container.
   *
   * @return the <code>AttributeTable</code> container.
   * @throws NoSuchAttributeException If
   * the instance of Attribute on which it is called is not a container.
   */
  public AttributeTable getContainer() throws NoSuchAttributeException {
    checkContainerUsage();
    return (AttributeTable) attr;
  }

  /**
   * Returns the <code>AttributeTable</code> container.
   *
   * @return the <code>AttributeTable</code> container, or null if not a container.
   */
  public AttributeTable getContainerN() {
    return (attr instanceof AttributeTable) ? (AttributeTable) attr : null;
  }

  /**
   * Returns the values of this attribute as an <code>Enumeration</code>
   * of <code>String</code>.
   *
   * @return an <code>Enumeration</code> of <code>String</code>.
   */
  public Enumeration getValues() throws NoSuchAttributeException {
    checkVectorUsage();
    return ((Vector) attr).elements();
  }

  /**
   * Returns the values of this attribute as an <code>Enumeration</code> of <code>String</code>.
   *
   * @return an <code>Iterator<String></code> of String </code>, or null if a container..
   */
  public Iterator getValuesIterator() {
    return (attr instanceof Vector) ? ((Vector) attr).iterator() : null;
  }

    /**
     * Returns the nummber of values held in this attribute.
     *
     * @return the attribute <code>String</code> at <code>index</code>.
     */
    public int getNumVal() throws NoSuchAttributeException {
      checkVectorUsage();
      return ((Vector) attr).size();
    }

    /**
     * Returns the attribute value at <code>index</code>.
     *
     * @param index the index of the attribute value to return.
     * @return the attribute <code>String</code> at <code>index</code>.
     */
    public String getValueAt(int index) throws NoSuchAttributeException {
      checkVectorUsage();
      return (String) ((Vector) attr).elementAt(index);
    }

  /**
   * Returns the attribute value at <code>index</code>.
   * @param index the index of the attribute value to return.
   * @return the attribute <code>String</code> at <code>index</code>, or null if a container..
   */
  public String getValueAtN(int index) {
    if (!(attr instanceof Vector)) return null;
    return (String) ((Vector) attr).elementAt(index);
  }


  /**
   * Append a value to this attribute. Always checks the validity of the
   * attribute's value.
   *
   * @param value the attribute <code>String</code> to add.
   * @throws AttributeBadValueException thrown if the value is not a legal
   *                                    member of type
   */
  public void appendValue(String value)
          throws NoSuchAttributeException, AttributeBadValueException {
    checkVectorUsage();
    appendValue(value, true);
  }

  /**
   * Append a value to this attribute.
   *
   * @param value the attribute <code>String</code> to add.
   * @param check if true, check the validity of he attribute's value, if
   *              false don't.
   * @throws AttributeBadValueException thrown if the value is not a legal
   *                                    member of type
   */
  public void appendValue(String value, boolean check)
          throws NoSuchAttributeException, AttributeBadValueException {

    checkVectorUsage();
    value = fixnan(value);
    if(check)
      dispatchCheckValue(type, value);

    ((Vector) attr).addElement(value);
  }

  /**
   * Remove the <code>i</code>'th <code>String</code> from this attribute.
   *
   * @param index the index of the value to remove.
   */
  public void deleteValueAt(int index)
          throws AttributeBadValueException, NoSuchAttributeException {
    checkVectorUsage();
    ((Vector) attr).removeElementAt(index);
  }

  /**
   * Check if the value is legal for a given type.
   *
   * @param type  the type of the value.
   * @param value the value <code>String</code>.
   * @throws AttributeBadValueException if the value is not a legal
   *                                    member of type
   */
  private static void dispatchCheckValue(int type, String value)
          throws AttributeBadValueException {

    switch (type) {

      case BYTE:
        if (!checkByte(value))
          throw new AttributeBadValueException("`" + value + "' is not a Byte value.");
        break;

      case INT16:
        if (!checkShort(value))
          throw new AttributeBadValueException("`" + value + "' is not an Int16 value.");
        break;

      case UINT16:
        if (!checkUShort(value))
          throw new AttributeBadValueException("`" + value + "' is not an UInt16 value.");
        break;

      case INT32:
        if (!checkInt(value))
          throw new AttributeBadValueException("`" + value + "' is not an Int32 value.");
        break;

      case UINT32:
        if (!checkUInt(value))
          throw new AttributeBadValueException("`" + value + "' is not an UInt32 value.");
        break;

      case FLOAT32:
        if (!checkFloat(value))
          throw new AttributeBadValueException("`" + value + "' is not a Float32 value.");
        break;

      case FLOAT64:
        if (!checkDouble(value))
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
   *
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkByte(String s)
          throws AttributeBadValueException {
    try {
      // Byte.parseByte() can't be used because values > 127 are allowed
      short val = Short.parseShort(s);
      if (DebugValueChecking) System.out.println("Attribute.checkByte() - string: '" + s + "'   value: " + val);
      if (val > 0xFF || val < 0)
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
   *
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkShort(String s) {
    try {
      short val = Short.parseShort(s);
      if (DebugValueChecking) System.out.println("Attribute.checkShort() - string: '" + s + "'   value: " + val);
      return true;
    }
    catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Check if string is a valid UInt16.
   *
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkUShort(String s) {
    // Note: Because there is no Unsigned class in Java, use Long instead.
    try {
      long val = Long.parseLong(s);
      if (DebugValueChecking) System.out.println("Attribute.checkUShort() - string: '" + s + "'   value: " + val);
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
   *
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkInt(String s) {
    try {
      int val = Integer.parseInt(s);
      if (DebugValueChecking) System.out.println("Attribute.checkInt() - string: '" + s + "'   value: " + val);
      return true;
    }
    catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Check if string is a valid UInt32.
   *
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkUInt(String s) {
    // Note: Because there is no Unsigned class in Java, use Long instead.
    try {
      long val = Long.parseLong(s);
      if (DebugValueChecking) System.out.println("Attribute.checkUInt() - string: '" + s + "'   value: " + val);
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
   *
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkFloat(String s) {
    try {
      float val = Float.parseFloat(s);
      if (DebugValueChecking) System.out.println("Attribute.checkFloat() - string: '" + s + "'   value: " + val);
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
   *
   * @param s the <code>String</code> to check.
   * @return true if the value is legal.
   */
  private static final boolean checkDouble(String s) {
    try {
      double val = Double.parseDouble(s);
      if (DebugValueChecking) System.out.println("Attribute.checkDouble() - string: '" + s + "'   value: " + val);
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


  private void checkVectorUsage() throws NoSuchAttributeException {

    if (!(attr instanceof Vector)) {
      throw new NoSuchAttributeException(
              "The Attribute '" + getName() + "' is a container. " +
                      "It's contents are Attribues, not values.");
    }
  }

  private void checkContainerUsage() throws NoSuchAttributeException {

    if (_Debug) System.out.print("Attribute.checkContainerUsage(): ");

    if (!(attr instanceof AttributeTable)) {
      throw new NoSuchAttributeException(
              "The Attribute '" + getName() + "' is not a container (AttributeTable)." +
                      "It's content is made up of values, not other Attributes.");
    }
    if (_Debug) System.out.println("The Attribute is a container");
  }


  public void print(PrintWriter os, String pad) {

    if (_Debug) os.println("Entered Attribute.print()");

    if (this.attr instanceof AttributeTable) {

      if (_Debug) os.println("  Attribute \"" + name + "\" is a Container.");

      ((AttributeTable) this.attr).print(os, pad);

    } else {
      if (_Debug) os.println("    Printing Attribute \"" + name + "\".");

      os.print(pad + getTypeString() + " " + getName() + " ");

      Enumeration es = ((Vector) this.attr).elements();


      while (es.hasMoreElements()) {
        String val = (String) es.nextElement();
        boolean useQuotes = false;

        if (val.indexOf(' ') >= 0 ||
                val.indexOf('\t') >= 0 ||
                val.indexOf('\n') >= 0 ||
                val.indexOf('\r') >= 0
                ) {

          if (val.indexOf('\"') != 0)
            useQuotes = true;
        }

        if (useQuotes)
          os.print("\"" + val + "\"");
        else
          os.print(val);
        if (es.hasMoreElements())
          os.print(", ");
      }
      os.println(";");
    }
    if (_Debug) os.println("Leaving Attribute.print()");
    os.flush();
  }

  /**
   * Print the attribute on the given <code>OutputStream</code>.
   *
   * @param os  the <code>OutputStream</code> to use for output.
   * @param pad the number of spaces to indent each line.
   */
  public final void print(OutputStream os, String pad) {
    print(new PrintWriter(new BufferedWriter(new OutputStreamWriter(os))), pad);
  }

  /**
   * Print the attribute on the given <code>PrintWriter</code> with
   * four spaces of indentation.
   *
   * @param os the <code>PrintWriter</code> to use for output.
   */
  public final void print(PrintWriter os) {
    print(os, "");
  }

  /**
   * Print the attribute on the given <code>OutputStream</code> with
   * four spaces of indentation.
   *
   * @param os the <code>OutputStream</code> to use for output.
   */
  public final void print(OutputStream os) {
    print(os, "");
  }


  /**
   * @param os
   * @opendap.ddx.experimental
   */
  public void printXML(OutputStream os) {
    printXML(os, "");
  }


  /**
   * @param os
   * @param pad
   * @opendap.ddx.experimental
   */
  public void printXML(OutputStream os, String pad) {
    PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
    printXML(pw, pad);
    pw.flush();
  }


  /**
   * @param pw
   * @opendap.ddx.experimental
   */
  public void printXML(PrintWriter pw) {
    printXML(pw, "");
  }


  /**
   * @param pw
   * @param pad
   * @opendap.ddx.experimental
   */
  public void printXML(PrintWriter pw, String pad) {
    printXML(pw, pad, false);
  }


  /**
   * @param pw
   * @param pad
   * @param constrained
   * @opendap.ddx.experimental
   */
  public void printXML(PrintWriter pw, String pad, boolean constrained) {


    if (_Debug) pw.println("Entered Attribute.printXML()");

    if (this.attr instanceof AttributeTable) {

      if (_Debug) pw.println("  Attribute \"" + name + "\" is a Container.");

      ((AttributeTable) this.attr).printXML(pw, pad, constrained);

    } else {
      if (_Debug) pw.println("    Printing Attribute \"" + name + "\".");

      pw.println(pad + "<Attribute name=\"" +
              opendap.dap.XMLparser.DDSXMLParser.normalizeToXML(getName()) +
              "\" type=\"" + getTypeString() + "\">");

      Enumeration es = ((Vector) this.attr).elements();
      while (es.hasMoreElements()) {
        String val = (String) es.nextElement();
        pw.println(pad + "\t" +
                "<value>" +
                opendap.dap.XMLparser.DDSXMLParser.normalizeToXML(val) +
                "</value>");
      }
      pw.println(pad + "</Attribute>");
    }
    if (_Debug) pw.println("Leaving Attribute.print()");
    pw.flush();

  }


static String
fixnan(String value)
{
    /* Check for hyrax error */
    if(value.equalsIgnoreCase("nan."))
        value = "nan";
    else if(value.equalsIgnoreCase("inf."))
	value = "inf";
    return value;
}


}

// $Log: Attribute.java,v $
// Revision 1.2  2003/09/02 17:49:34  ndp
// *** empty log message ***
//
// Revision 1.1  2003/08/12 23:51:25  ndp
// Mass check in to begin Java-OPeNDAP development work
//
// Revision 1.10  2003/04/16 21:50:53  caron
// turn off debug flag
//
// Revision 1.9  2003/04/07 22:12:32  jchamber
// added serialization
//
// Revision 1.8  2003/02/12 16:41:15  ndp
// *** empty log message ***
//
// Revision 1.7  2002/10/08 21:59:18  ndp
// Added XML functionality to the core. This includes the new DDS code (aka DDX)
// for parsing XML representations of the dataset description ( that's a DDX)
// Also BaseType has been modified to hold Attributes and methods added to DDS
// to ingest DAS's (inorder to add Attributes to variables) and to get the DAS
// object from the DDS. Geturl and DConnect hav been modified to provide client
// access to this new set of functionalites. ndp 10/8/2002
//
// Revision 1.6  2002/08/27 04:30:11  ndp
// AttributeTable added to BaseType
// Interfaces for AttributeTable implmented in BaseType, DConstructor, and DDS
// Methods added to DDS to print DAS and to return a DAS object.
// XMLParser updated to populate BaseType AttributeTables.
//
// Revision 1.5  2002/05/30 23:24:58  jimg
// I added methods that provide a way to add attribute values without checking
// their type/value validity. This provides a way to add bad values in a
// *_dods_error attribute container so that attributes that are screwed up won't
// be lost (because they might make sense to a person, for example) or cause the
// whole DAS to break. The DAS parser (DASParser.jj) uses this new code.
//


