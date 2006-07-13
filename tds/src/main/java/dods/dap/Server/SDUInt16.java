/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1999, COAS, Oregon State University  
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged. 
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Nathan Potter (ndp@oce.orst.edu)
//
//                        College of Oceanic and Atmospheric Scieneces
//                        Oregon State University
//                        104 Ocean. Admin. Bldg.
//                        Corvallis, OR 97331-5503
//         
/////////////////////////////////////////////////////////////////////////////
//
// Based on source code and instructions from the work of:
//
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

package dods.dap.Server;
import dods.dap.*;
import java.io.*;
import dods.dap.parser.ExprParserConstants;

/**
 * Holds a DODS Server <code>Unsigned Int16</code> value.
 *
 * @version $Revision: 51 $
 * @author ndp
 * @see BaseType
 */
public abstract class SDUInt16 extends DUInt16 implements ServerMethods, RelOps, ExprParserConstants  {
    private boolean Project;
    private boolean Synthesized;
    private boolean ReadMe;
    
  /** Constructs a new <code>SDUInt16</code>. */
  public SDUInt16() { 
    super(); 
    Project = false;
    Synthesized = false;
    ReadMe = false;
  }

  /**
   * Constructs a new <code>SDUInt16</code> with name <code>n</code>.
   * @param n the name of the variable.
   */
  public SDUInt16(String n) { 
    super(n); 
    Project = false;
    Synthesized = false;
    ReadMe = false;
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
    * @param constrained a boolean value indicating whether to print
    *    the declartion dependent on the projection information. <b>This
    *    is only used by Server side code.</b>
    * @see DDS
    */
    public void printDecl(PrintWriter os, String space,
                                    boolean print_semi, boolean constrained) {
        if(constrained && !Project)
	        return;
		
		// BEWARE! Since printDecl()is (multiple) overloaded in BaseType
		// and all of the different signatures of printDecl() in BaseType
		// lead to one signature, we must be careful to override that
	    // SAME signature here. That way all calls to printDecl() for 
	    // this object lead to this implementation.

		// Also, since printDecl()is (multiple) overloaded in BaseType
		// and all of the different signatures of printDecl() in BaseType
		// lead to the signature we are overriding here, we MUST call
		// the printDecl with the SAME signature THROUGH the super class
		// reference (assuming we want the super class functionality). If
		// we do otherwise, we will create an infinte call loop. OOPS!
		
        super.printDecl(os,space,print_semi,constrained);
    }




    /**
    * Prints the value of the variable, with its declaration.  This
    * function is primarily intended for debugging DODS applications and
    * text-based clients such as geturl.
    *
    * <h2> Important Note</h2>
    * This method overrides the BaseType method of the same name and 
    * type signature and it significantly changes the behavior for all versions
    * of <code>printVal()</code> for this type: 
    * <b><i> All the various versions of printVal() will only
    * print a value, or a value with declaration, if the variable is
    * in the projection.</i></b>
    * <br>
    * <br>In other words, if a call to 
    * <code>isProject()</code> for a particular variable returns 
    * <code>true</code> then <code>printVal()</code> will print a value 
    * (or a declaration and a value). 
    * <br>
    * <br>If <code>isProject()</code> for a particular variable returns 
    * <code>false</code> then <code>printVal()</code> is basically a No-Op.
    * <br>
    * <br>
    *
    * @param os the <code>PrintWriter</code> on which to print the value.
    * @param space this value is passed to the <code>printDecl</code> method,
    *    and controls the leading spaces of the output.
    * @param print_decl_p a boolean value controlling whether the
    *    variable declaration is printed as well as the value.
    * @see BaseType#printVal(PrintWriter, String, boolean)
    * @see ServerMethods#isProject()
    */
    public void printVal(PrintWriter os, String space, boolean print_decl_p) {
        if(!Project)
	        return;
        super.printVal(os,space,print_decl_p);
     }


    // --------------- Projection Interface
    /** Set the state of this variable's projection. <code>true</code> means
	that this variable is part of the current projection as defined by
	the current constraint expression, otherwise the current projection
	for this variable should be <code>false</code>.
	@param state <code>true</code> if the variable is part of the current
	projection, <code>false</code> otherwise.
	@param all This parameter has no effect for this type of variable.
	@see CEEvaluator */
    public void setProject(boolean state, boolean all) {
        Project = state;        
    }

    /** Set the state of this variable's projection. <code>true</code> means
	that this variable is part of the current projection as defined by
	the current constraint expression, otherwise the current projection
	for this variable should be <code>false</code>. 
	@param state <code>true</code> if the variable is part of the current
	projection, <code>false</code> otherwise.
	@see CEEvaluator */
    public void setProject(boolean state) {
	    setProject(state, true);
    }

    /** Is the given variable marked as projected? If the variable is listed
	in the projection part of a constraint expression, then the CE parser
	should mark it as <em>projected</em>. When this method is called on
	such a variable it should return <code>true</code>, otherwise it
	should return <code>false</code>.
	@return <code>true</code> if the variable is part of the current
	projections, <code>false</code> otherwise.
	@see CEEvaluator 
        @see #setProject(boolean)
    */
    public boolean isProject(){
        return(Project);
    }
    
    
    
// --------------- RelOps Interface
/** The RelOps interface defines how each type responds to relational
    operators. Most (all?) types will not have sensible responses to all of
    the relational operators (e.g. DBoolean won't know how to match a regular
    expression but DString will). For those operators that are nonsensical a
    class should throw InvalidOperatorException.*/
    
    
    public boolean equal(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException{
            return(Operator.op(EQUAL,this,bt));
    }    
    public boolean not_equal(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException{
            return(Operator.op(NOT_EQUAL,this,bt));
    }
    public boolean greater(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException{
             return(Operator.op(GREATER,this,bt));
    }
    public boolean greater_eql(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException{
            return(Operator.op(GREATER_EQL,this,bt));
    }
    public boolean less(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException{
            return(Operator.op(LESS,this,bt));
    }
    public boolean less_eql(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException{
            return(Operator.op(LESS_EQL,this,bt));
    }
    public boolean regexp(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException{
            return(Operator.op(REGEXP,this,bt));
    }
        
    

// --------------- FileIO Interface

   /** Set the Synthesized property.
	@param state If <code>true</code> then the variable is considered a
	synthetic variable and no part of DODS will ever try to read it from a
	file, otherwise if <code>false</code> the variable is considered a
	normal variable whose value should be read using the
	<code>read()</code> method. By default this property is false.
	@see #isSynthesized()
	@see #read(String,Object)
    */
    public void setSynthesized(boolean state){
        Synthesized = state;
    }

    /** Get the value of the Synthesized property.
	@return <code>true</code> if this is a synthetic variable,
	<code>false</code> otherwise. */
    public boolean isSynthesized(){
        return(Synthesized);
    }

    /** Set the Read property. A normal variable is read using the
	<code>read()</code> method. Once read the <em>Read</em> property is
	<code>true</code>. Use this function to manually set the property
	value. By default this property is false.
	@param state <code>true</code> if the variable has been read,
	<code>false</code> otherwise.
	@see #isRead()
	@see #read(String,Object)
    */
    public void setRead(boolean state){
        ReadMe = state;
    }

    /** Get the value of the Read property.
	@return <code>true</code> if the variable has been read,
	<code>false</code> otherwise.
	@see #read(String,Object)
	@see #setRead(boolean)
    */
    public boolean isRead(){
        return(ReadMe);
    }

     /** Read a value from the named dataset for this variable. 
	@param datasetName String identifying the file or other data store
	from which to read a vaue for this variable.
	@param specialO This <code>Object</code> is a goody that is used by Server implementations
	to deliver important, and as yet unknown, stuff to the read method. If you
	don't need it, make it a <code>null</code>.
	@return <code>true</code> if more data remains to be read, otherwise
	<code>false</code>. This is an abtsract method that must be implemented
	as part of the installation/localization of a DODS server.
	@exception IOException
	@exception EOFException */
    public abstract boolean read(String datasetName, Object specialO) throws NoSuchVariableException, IOException, EOFException;
    
    
    /** 
    * Server-side serialization for DODS variables (sub-classes of
    * <code>BaseType</code>).
    * This does not send the entire class as the Java <code>Serializable</code>
    * interface does, rather it sends only the binary data values. Other software
    * is responsible for sending variable type information (see <code>DDS</code>).
    *
    * Writes data to a <code>DataOutputStream</code>. This method is used
    * on the server side of the DODS client/server connection, and possibly
    * by GUI clients which need to download DODS data, manipulate it, and
    * then re-save it as a binary file.
    *
    * @param sink a <code>DataOutputStream</code> to write to.
    * @exception IOException thrown on any <code>OutputStream</code> exception. 
    * @see BaseType
    * @see DDS
    * @see ServerDDS */
    public void serialize(String dataset,DataOutputStream sink,CEEvaluator ce, Object specialO)
                                                throws NoSuchVariableException, SDODSException, IOException {
	
        if(!isRead())
	    read(dataset, specialO);
	    
        if(ce.evalClauses(specialO))
            externalize(sink);
	    		
    }
    
}
