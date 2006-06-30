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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;
import dods.dap.BaseType;
import dods.dap.DDS;
import dods.dap.NoSuchVariableException;
import dods.dap.Server.SBHException;

/** 
 *
 *
 * @version $Revision: 1.1 $
 * @author ndp
 * @see BaseType
 * @see DDS
 */
 
 
/** This interface defines the additional behaviors that Server side types
    need to support. These include:
    
    <p>The file I/O operations of which each variable must be capable.
    
    <p>The projection information. A projection defines the variables
    specified by a constraint to be returned by the server. These methods 
    store projection information for a non-vector variable. Each variable 
    type used on the server-side of DODS must implement this interface or
    one of its descendents.

    <p>The methods that define how each type responds to relational
    operators. Most (all?) types will not have sensible responses to all of
    the relational operators (e.g. SDByte won't know how to match a regular
    expression but SDString will). For those operators that are nonsensical a
    class should throw InvalidOperator.

    @see ServerArrayMethods
    @see Operator
    
    @version $Revision: 1.1 $
    @author jhrg & ndp */


public interface ServerMethods  {

    //    FileIO interface
    
    /** Set the Synthesized property.
	@param state If <code>true</code> then the variable is considered a
	synthetic variable and no part of DODS will ever try to read it from a
	file, otherwise if <code>false</code> the variable is considered a
	normal variable whose value should be read using the
	<code>read()</code> method. By default this property is false.
	@see #isSynthesized()
	@see #read(String, Object) 
    */
    public void setSynthesized(boolean state);

    /** Get the value of the Synthesized property.
	@return <code>true</code> if this is a synthetic variable,
	<code>false</code> otherwise.
    */
    public boolean isSynthesized();

    /** Set the Read property. A normal variable is read using the
	<code>read()</code> method. Once read the <em>Read</em> property is
	<code>true</code>. Use this function to manually set the property
	value. By default this property is false.
	@param state <code>true</code> if the variable has been read,
	<code>false</code> otherwise.
	@see #isRead()
	@see #read(String, Object) 
    */
    public void setRead(boolean state);

    /** Get the value of the Read property.
	@return <code>true</code> if the variable has been read,
	<code>false</code> otherwise.
	@see #read(String, Object) 
	@see #setRead(boolean) 
    */
    public boolean isRead();
    
    /** Read a value from the named dataset for this variable. 
	@param datasetName String identifying the file or other data store
	from which to read a vaue for this variable.
	@param o Object this is a goody that is used by Server implementations
	to deliver important, and as yet unknown, stuff to the read method. If you
	don't need it, make it a <code>null</code>.
	@return <code>true</code> if more data remains to be read, otehrwise
	<code>false</code>. 
	@exception NoSuchVariableException
	@exception IOException
	@exception EOFException 
    */
    public boolean read(String datasetName, Object specialO) 
	            throws NoSuchVariableException, IOException, EOFException;
    
    // Projection Interface
    
    /** 
	Set the state of this variable's projection. <code>true</code> means
	that this variable is part of the current projection as defined by
	the current constraint expression, otherwise the current projection
	for this variable should be <code>false</code>.<p> 

	For simple variables and for children of DVector, the variable either
	is or is not projected. For children of DConstructor, it may be that
	the request is for only part of the constructor type (e.g., only oe
	field of a structure). However, the structure variable itself must be
	marked as projected given the implementation of serialize. The
	serialize() method does not search the entire tree of variables; it
	relies on the fact that for a particular variable to be sent, the
	<em>path</em> from the top of the DDS to that variable must be marked
	as `projected', not just the variable itself. This keeps the
	CEEvaluator.send() method from having to search the entire tree for
	the variables to be sent.
	
	@param state <code>true</code> if the variable is part of the current
	projection, <code>false</code> otherwise.
	@param all set (or clear) the Project property of any children.
	@see CEEvaluator 
    */
    public void setProject(boolean state, boolean all);

    /** Set the Project property of this variable. This is equivalent to
     * calling setProject(<state>, true).
     * @param state <code>true</code> if the variable is part of the current
     * projection, <code>false</code> otherwise.
     * @see #setProject(boolean)
     * @see CEEvaluator 
     */
    public void setProject(boolean state);

    /** Is the given variable marked as projected? If the variable is listed
	in the projection part of a constraint expression, then the CE parser
	should mark it as <em>projected</em>. When this method is called on
	such a variable it should return <code>true</code>, otherwise it
	should return <code>false</code>.
	@return <code>true</code> if the variable is part of the current
	projections, <code>false</code> otherwise.
        @see #setProject(boolean,boolean)
        @see #setProject(boolean)
	@see CEEvaluator 
    */
    public boolean isProject();
    
    /** 
	The <code>Operator</code> class contains a generalized implementation
	of this method. It should be used unless a localized
	architecture/implementation requires otherwise.
	@see Operator 
   */
    public boolean equal(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;
    /** The <code>Operator</code> class contains a generalized implementation
      * of this method. It should be used unless a localized
      * architecture/implementation requires otherwise.
      @see Operator */
    public boolean not_equal(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;
    /** The <code>Operator</code> class contains a generalized implementation
      * of this method. It should be used unless a localized
      * architecture/implementation requires otherwise.
      @see Operator */
    public boolean greater(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;
    /** The <code>Operator</code> class contains a generalized implementation
      * of this method. It should be used unless a localized
      * architecture/implementation requires otherwise.
      @see Operator */
    public boolean greater_eql(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;
    /** The <code>Operator</code> class contains a generalized implementation
      * of this method. It should be used unless a localized
      * architecture/implementation requires otherwise.
      @see Operator */
    public boolean less(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;
    /** The <code>Operator</code> class contains a generalized implementation
      * of this method. It should be used unless a localized
      * architecture/implementation requires otherwise.
      @see Operator */
    public boolean less_eql(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;
    /** The <code>Operator</code> class contains a generalized implementation
      * of this method. It should be used unless a localized
      * architecture/implementation requires otherwise.
      @see Operator */
    public boolean regexp(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;


    /** 
     * Server-side serialization for DODS variables (sub-classes of
     * <code>BaseType</code>). This does not send the entire class as the
     * Java <code>Serializable</code> interface does, rather it sends only
     * the binary data values. Other software is responsible for sending
     * variable type information (see <code>DDS</code>).<p>
     *
     * Writes data to a <code>DataOutputStream</code>. This method is used
     * on the server side of the DODS client/server connection, and possibly
     * by GUI clients which need to download DODS data, manipulate it, and
     * then re-save it as a binary file.<p>
     *
     * For children of DConstructor, this method should call itself on each
     * of the components. For other types this method should call
     * externalize(). 
     *
     * @param dataset a <code>String</code> indicated which dataset to read
     * from (Or something else if you so desire).
     * @param sink a <code>DataOutputStream</code> to write to.
     * @param ce the <code>CEEvaluator</code> to use in the parse process.
     * @exception IOException thrown on any <code>OutputStream</code> exception. 
     * @see BaseType
     * @see DDS
     * @see ServerDDS */
    public void serialize(String dataset, DataOutputStream sink, 
			  CEEvaluator ce, Object specialO) 
	throws NoSuchVariableException, SDODSException,  IOException;
}
