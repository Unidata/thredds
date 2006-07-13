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
import java.io.*;

/**
 * This abstract class defines the basic data type features for the DODS data
 * access protocol (DAP) data types. All of the DAP type classes
 * (<code>DFloat64</code>, <code>DArray</code>, etc.) subclass it or one of
 * its two abstract descendents, <code>DVector</code> or
 * <code>DConstructor</code>.
 * <p>
 * These classes and their methods give a user the capacity to set up
 * sophisticated data types. They do <em>not</em> provide sophisticated ways to
 * access and use this data. On the server side, in many cases, the class
 * instances will have no data in them at all until the <code>serialize</code>
 * method is called to send data to the client. On the client side, most DODS
 * application programs will unpack the data promptly into whatever local
 * data structure the programmer deems the most useful.
 * <p>
 * Descendents of this class should implement the <code>ClientIO</code>
 * interface.  That interface defines a <code>deserialize</code> method used
 * by a DODS client to retrieve the variable's declaration and value(s) from
 * a DODS server.
 *
 * @version $Revision: 48 $
 * @author jehamby
 * @see DDS
 * @see ClientIO
 */
public abstract class BaseType implements Cloneable {
  /** The name of this variable. */
  private String _name;

  /** The parent (container class) of this object, if one exists */
  private BaseType _myParent;

  /** Constructs a new <code>BaseType</code> with no name. */
  public BaseType() {
    this("");
  }

  /**
   * Constructs a new <code>BaseType</code> with name <code>n</code>.
   * @param n the name of the variable.
   */
  public BaseType(String n) {
    _name = n;
    _myParent = null;
  }

  /**
   * Returns a clone of this <code>BaseType</code>.  A deep copy is performed
   * on all data inside the variable.
   *
   * @return a clone of this <code>BaseType</code>.
   */
  public Object clone() {
    try {
      BaseType bt = (BaseType)super.clone();
      return bt;
    } catch (CloneNotSupportedException e) {
      // this shouldn't happen, since we are Cloneable
      throw new InternalError();
    }
  }

  /**
   * Returns the name of the class instance.
   * @return the name of the class instance.
   */
  public final String getName() {
    return _name;
  }

  /**
   * Sets the name of the class instance.
   * @param n the name of the class instance.
   */
  public final void setName(String n) {
    _name = n;
  }

  /**
   * Returns the DODS type name of the class instance as a <code>String</code>.
   * @return the DODS type name of the class instance as a <code>String</code>.
   */
  abstract public String getTypeName();

  /**
   * Returns the number of variables contained in this object. For simple and
   * vector type variables, it always returns 1. To count the number
   * of simple-type variable in the variable tree rooted at this variable, set
   * <code>leaves</code> to <code>true</code>.
   *
   * @param leaves If true, count all the simple types in the `tree' of
   *    variables rooted at this variable.
   * @return the number of contained variables.
   */
  public int elementCount(boolean leaves) {
    return 1;
  }

  /**
   * Returns the number of variables contained in this object. For simple and
   * vector type variables, it always returns 1.
   *
   * @return the number of contained variables.
   */
  public final int elementCount() {
    return elementCount(false);
  }

    /**
    * Returns a string representation of the variables value. This
    * is really foreshadowing functionality for Server types, but
    * as it may come in useful for clients it is added here. Simple 
    * types (example: DFloat32) will return a single value. DConstuctor
    * and DVector types will be flattened. DStrings and DURL's will 
    * have double quotes around them.
    */
/*    abstract public void toASCII(PrintWriter pw, boolean addName, String rootName, boolean newLine);

    String toASCIIAddRootName(PrintWriter pw, boolean addName, String rootName){
    
        if(addName){
            rootName = toASCIIFlatName(rootName);
            pw.print(rootName);
        }	
	return(rootName);

    }

    String toASCIIFlatName(String rootName){
        String s;
        if(rootName != null){
            s = rootName +  "." + getName();
        }
        else {
            s = getName();
        }
	return(s);
    }
*/

  /**
   * Write the variable's declaration in a C-style syntax. This
   * function is used to create textual representation of the Data
   * Descriptor Structure (DDS).  See <em>The DODS User Manual</em> for
   * information about this structure.
   *
   * @param os The <code>PrintWriter</code> on which to print the
   *    declaration.
   * @param space Each line of the declaration will begin with the
   *    characters in this string.  Usually used for leading spaces.
   * @param print_semi a boolean value indicating whether to print a
   *    semicolon at the end of the declaration.
   * @param constrained a boolean value indicating whether to print
   *    the declartion dependent on the projection information. <b>This
   *    is only used by Server side code.</b>
   * @see DDS
   */
  public void printDecl(PrintWriter os, String space,
			boolean print_semi, boolean constrained) {
			
    //System.out.println("BaseType.printDecl()...");
    os.print(space + getTypeName() + " " + getName());
    if (print_semi)
      os.println(";");
  }

    /**
    * Write the variable's declaration in a C-style syntax. This
    * function is used to create textual representation of the Data
    * Descriptor Structure (DDS).  See <em>The DODS User Manual</em> for
    * information about this structure.
    *
    * @param os The <code>PrintWriter</code> on which to print the
    *    declaration.
    * @param space Each line of the declaration will begin with the
    *    characters in this string.  Usually used for leading spaces.
    * @param print_semi a boolean value indicating whether to print a
    *    semicolon at the end of the declaration.
    * @see DDS
    */
    public void printDecl(PrintWriter os, String space,
                                        boolean print_semi) {

        printDecl(os,space,print_semi,false);

    }

  /**
   * Print the variable's declaration.  Same as
   * <code>printDecl(os, space, true)</code>.
   *
   * @param os The <code>PrintWriter</code> on which to print the
   *    declaration.
   * @param space Each line of the declaration will begin with the
   *    characters in this string.  Usually used for leading spaces.
   * @see DDS#print(PrintWriter)
   */
  public final void printDecl(PrintWriter os, String space) {
    printDecl(os, space, true, false);
  }

  /**
   * Print the variable's declaration.  Same as
   * <code>printDecl(os, "    ", true)</code>.
   *
   * @param os The <code>PrintWriter</code> on which to print the
   *    declaration.
   * @see DDS#print(PrintWriter)
   */
  public final void printDecl(PrintWriter os) {
    printDecl(os, "    ", true, false);
  }

  /**
   * Print the variable's declaration using <code>OutputStream</code>.
   *
   * @param os The <code>OutputStream</code> on which to print the
   *    declaration.
   * @param space Each line of the declaration will begin with the
   *    characters in this string.  Usually used for leading spaces.
   * @param print_semi a boolean value indicating whether to print a
   *    semicolon at the end of the declaration.
   * @see DDS#print(PrintWriter)
   */
  public final void printDecl(OutputStream os, String space,
			      boolean print_semi, boolean constrained) {
    PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
    printDecl(pw, space, print_semi,constrained);
    pw.flush();
  }

  /**
   * Print the variable's declaration using <code>OutputStream</code>.
   *
   * @param os The <code>OutputStream</code> on which to print the
   *    declaration.
   * @param space Each line of the declaration will begin with the
   *    characters in this string.  Usually used for leading spaces.
   * @param print_semi a boolean value indicating whether to print a
   *    semicolon at the end of the declaration.
   * @see DDS#print(PrintWriter)
   */
  public final void printDecl(OutputStream os, String space,
			      boolean print_semi) {
    printDecl(os, space, print_semi,false);
  }

  /**
   * Print the variable's declaration.  Same as
   * <code>printDecl(os, space, true)</code>.
   *
   * @param os The <code>OutputStream</code> on which to print the
   *    declaration.
   * @param space Each line of the declaration will begin with the
   *    characters in this string.  Usually used for leading spaces.
   * @see DDS#print(PrintWriter)
   */
  public final void printDecl(OutputStream os, String space) {
    PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
    printDecl(pw, space);
    pw.flush();
  }

  /**
   * Print the variable's declaration.  Same as
   * <code>printDecl(os, "    ", true)</code>.
   *
   * @param os The <code>OutputStream</code> on which to print the
   *    declaration.
   * @see DDS#print(PrintWriter)
   */
  public final void printDecl(OutputStream os) {
    PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
    printDecl(pw);
    pw.flush();
  }


  /**
   * Prints the value of the variable, with its declaration.  This
   * function is primarily intended for debugging DODS applications and
   * text-based clients such as geturl.
   *
   * @param os the <code>PrintWriter</code> on which to print the value.
   * @param space this value is passed to the <code>printDecl</code> method,
   *    and controls the leading spaces of the output.
   * @param print_decl_p a boolean value controlling whether the
   *    variable declaration is printed as well as the value.
   */
  abstract public void printVal(PrintWriter os, String space,
				boolean print_decl_p);

  /**
   * Print the variable's value.  Same as
   * <code>printVal(os, space, true)</code>.
   *
   * @param os the <code>PrintWriter</code> on which to print the value.
   * @param space this value is passed to the <code>printDecl</code> method,
   *    and controls the leading spaces of the output.
   * @see DataDDS#printVal(PrintWriter)
   */
  public final void printVal(PrintWriter os, String space) {
    printVal(os, space, true);
  }

  /**
   * Print the variable's value using <code>OutputStream</code>.
   *
   * @param os the <code>OutputStream</code> on which to print the value.
   * @param space this value is passed to the <code>printDecl</code> method,
   *    and controls the leading spaces of the output.
   * @param print_decl_p a boolean value controlling whether the
   *    variable declaration is printed as well as the value.
   * @see DataDDS#printVal(PrintWriter)
   */
  public final void printVal(OutputStream os, String space,
			     boolean print_decl_p) {
    PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
    printVal(pw, space, print_decl_p);
    pw.flush();
  }

  /**
   * Print the variable's value using <code>OutputStream</code>.
   *
   * @param os the <code>OutputStream</code> on which to print the value.
   * @param space this value is passed to the <code>printDecl</code> method,
   *    and controls the leading spaces of the output.
   * @see DataDDS#printVal(PrintWriter)
   */
  public final void printVal(OutputStream os, String space) {
    PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
    printVal(pw, space);
    pw.flush();
  }

  /**
   * Checks for internal consistency.  This is important to check for complex
   * constructor classes.
   * For example, an <code>DInt32</code> instance would return false if it had
   * no name defined.  A <code>DGrid</code> instance might return false for
   * more complex reasons, such as having Map arrays of the wrong
   * size or shape.
   * <p>
   * This method is used by the <code>DDS</code> class, and will rarely, if
   * ever, be explicitly called by a DODS application program.  A
   * variable must pass this test before it is sent, but there may be
   * many other stages in a retrieve operation where it would fail.
   *
   * @param all For complex constructor types (
   *    <code>DGrid</code>, <code>DSequence</code>, <code>DStructure</code>),
   *    this flag indicates whether to check the
   *    semantics of the member variables, too.
   * @exception BadSemanticsException if semantics are bad, explains why.
   * @see DDS#checkSemantics(boolean)
   */
  public void checkSemantics(boolean all)
                             throws BadSemanticsException {
    if (_name == null)
      throw new BadSemanticsException("BaseType.checkSemantics(): Every variable must have a name");
  }

  /**
   * Check semantics.  Same as <code>checkSemantics(false)</code>.
   * @exception BadSemanticsException if semantics are bad, explains why.
   * @see BaseType#checkSemantics(boolean)
   */
  public final void checkSemantics() throws BadSemanticsException {
    checkSemantics(false);
  }

    /**
    * Constructs a new <code>PrimitiveVector</code> object optimized for the
    * particular data type of this <code>BaseType</code>.  For example, a
    * <code>DByte</code> class would create a new
    * <code>BytePrimitiveVector</code> in this call.  This allows for a very
    * optimized, yet type-safe, implementation of <code>DVector</code>
    * functionality.  For non-primitive types, such as
    * <code>DArray</code>, <code>DGrid</code>, <code>DSequence</code>, and
    * <code>DStructure</code>, the default implementation returns a
    * <code>BaseTypePrimitiveVector</code> object which can
    * deserialize an array of complex types.
    *
    * @return a new <code>PrimitiveVector</code> object for the variable type.
    */
    public PrimitiveVector newPrimitiveVector() {
        return new BaseTypePrimitiveVector(this);
    }


    public void setParent(BaseType bt) {
        _myParent = bt;
    }
    
    public BaseType getParent() {
        return(_myParent);
    }
    
    public String getLongName(){
    
        boolean done = false;
	
        BaseType parent = _myParent;
	
	String longName = _name;
	
	
        while(parent != null){
            longName = parent.getName() + "." + longName;
            parent = parent.getParent();
        }
	return(longName);
    }


  
  
  
  
  
  
  
}
