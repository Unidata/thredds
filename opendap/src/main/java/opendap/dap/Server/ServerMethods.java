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




package opendap.dap.Server;

import java.io.DataOutputStream;
import java.io.IOException;

import opendap.dap.BaseType;
import opendap.dap.DDS;
import opendap.dap.NoSuchVariableException;

/**
 *
 *
 * @version $Revision: 16122 $
 * @author ndp
 * @see BaseType
 * @see DDS
 */


/**
 * This interface defines the additional behaviors that Server side types
 * need to support. These include:
 * <p/>
 * <p>The file I/O operations of which each variable must be capable.
 * <p/>
 * <p>The projection information. A projection defines the variables
 * specified by a constraint to be returned by the server. These methods
 * store projection information for a non-vector variable. Each variable
 * type used on the server-side of OPeNDAP must implement this interface or
 * one of its descendents.
 * <p/>
 * <p>The methods that define how each type responds to relational
 * operators. Most (all?) types will not have sensible responses to all of
 * the relational operators (e.g. SDByte won't know how to match a regular
 * expression but SDString will). For those operators that are nonsensical a
 * class should throw InvalidOperator.
 *
 * @author jhrg & ndp
 * @version $Revision: 16122 $
 * @see ServerArrayMethods
 * @see Operator
 */


public interface ServerMethods {

    //    FileIO interface

    /**
     * Set the Synthesized property.
     *
     * @param state If <code>true</code> then the variable is considered a
     *              synthetic variable and no part of OPeNDAP will ever try to read it from a
     *              file, otherwise if <code>false</code> the variable is considered a
     *              normal variable whose value should be read using the
     *              <code>read()</code> method. By default this property is false.
     * @see #isSynthesized()
     * @see #read(String, Object)
     */
    public void setSynthesized(boolean state);

    /**
     * Get the value of the Synthesized property.
     *
     * @return <code>true</code> if this is a synthetic variable,
     *         <code>false</code> otherwise.
     */
    public boolean isSynthesized();

    /**
     * Set the Read property. A normal variable is read using the
     * <code>read()</code> method. Once read the <em>Read</em> property is
     * <code>true</code>. Use this function to manually set the property
     * value. By default this property is false.
     *
     * @param state <code>true</code> if the variable has been read,
     *              <code>false</code> otherwise.
     * @see #isRead()
     * @see #read(String, Object)
     */
    public void setRead(boolean state);

    /**
     * Get the value of the Read property.
     *
     * @return <code>true</code> if the variable has been read,
     *         <code>false</code> otherwise.
     * @see #read(String, Object)
     * @see #setRead(boolean)
     */
    public boolean isRead();

    /**
     * Read a value from the named dataset for this variable.
     *
     * @param datasetName String identifying the file or other data store
     *                    from which to read a vaue for this variable.
     * @param specialO    This <code>Object</code> is a goody that is used by a
     *                    Server implementations to deliver important, and as
     *                    yet unknown, stuff to the read method. If you don't
     *                    need it, make it a <code>null</code>.
     * @return <code>true</code> if more data remains to be read, otehrwise
     *         <code>false</code>.
     * @throws NoSuchVariableException When a variable can't be found.
     * @throws IOException When there is a problem reading data.
     */
    public boolean read(String datasetName, Object specialO)
            throws NoSuchVariableException, IOException;

    // Projection Interface

    /**
     * Set the state of this variable's projection. <code>true</code> means
     * that this variable is part of the current projection as defined by
     * the current constraint expression, otherwise the current projection
     * for this variable should be <code>false</code>.<p>
     * <p/>
     * For simple variables and for children of DVector, the variable either
     * is or is not projected. For children of DConstructor, it may be that
     * the request is for only part of the constructor type (e.g., only oe
     * field of a structure). However, the structure variable itself must be
     * marked as projected given the implementation of serialize. The
     * serialize() method does not search the entire tree of variables; it
     * relies on the fact that for a particular variable to be sent, the
     * <em>path</em> from the top of the DDS to that variable must be marked
     * as `projected', not just the variable itself. This keeps the
     * CEEvaluator.send() method from having to search the entire tree for
     * the variables to be sent.
     *
     * @param state <code>true</code> if the variable is part of the current
     *              projection, <code>false</code> otherwise.
     * @param all   set (or clear) the Project property of any children.
     * @see CEEvaluator
     */
    public void setProject(boolean state, boolean all);

    /**
     * Set the Project property of this variable. This is equivalent to
     * calling setProject(<state>, true).
     *
     * @param state <code>true</code> if the variable is part of the current
     *              projection, <code>false</code> otherwise.
     * @see #setProject(boolean)
     * @see CEEvaluator
     */
    public void setProject(boolean state);

    /**
     * Check the projection state of this variable.
     * Is the this variable marked as projected? If the variable is listed
     * in the projection part of a constraint expression, then the CE parser
     * should mark it as <em>projected</em>. When this method is called on
     * such a variable it should return <code>true</code>, otherwise it
     * should return <code>false</code>.
     *
     * @return <code>true</code> if the variable is part of the current
     *         projections, <code>false</code> otherwise.
     * @see #setProject(boolean,boolean)
     * @see #setProject(boolean)
     * @see CEEvaluator
     */
    public boolean isProject();

    /**
     * The <code>Operator</code> class contains a generalized implementation
     * of this method. It should be used unless a localized
     * architecture/implementation requires otherwise.
     *
     * @param bt The variable to which to compare 'this' value.
     * @return True when they are equal, false otherwise.
     * @throws InvalidOperatorException When the operator cannot be applied to
     * the two data types.
     * @throws RegExpException When the regular expression is badly formed.
     * @throws SBHException When Something Bad Happens.
     *
     * @see Operator
     */
    public boolean equal(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;

    /**
     * The <code>Operator</code> class contains a generalized implementation
     * of this method. It should be used unless a localized
     * architecture/implementation requires otherwise.
     *
     * @param bt The variable to which to compare 'this' value.
     * @return True when they are not equal, false otherwise.
     * @throws InvalidOperatorException When the operator cannot be applied to
     * the two data types.
     * @throws RegExpException When the regular expression is badly formed.
     * @throws SBHException When Something Bad Happens.

     * @see Operator
     */
    public boolean not_equal(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;

    /**
     * The <code>Operator</code> class contains a generalized implementation
     * of this method. It should be used unless a localized
     * architecture/implementation requires otherwise.
     *
     * @param bt The variable to which to compare 'this' value.
     * @return True when this value is greater than the based 'bt' value,
     * false otherwise.
     * @throws InvalidOperatorException When the operator cannot be applied to
     * the two data types.
     * @throws RegExpException When the regular expression is badly formed.
     * @throws SBHException When Something Bad Happens.
     *
     * @see Operator
     */
    public boolean greater(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;

    /**
     * The <code>Operator</code> class contains a generalized implementation
     * of this method. It should be used unless a localized
     * architecture/implementation requires otherwise.
     *
     * @param bt The variable to which to compare 'this' value.
     * @return True when this value is greater or equal than the based 'bt' value,
     * false otherwise.
     * @throws InvalidOperatorException When the operator cannot be applied to
     * the two data types.
     * @throws RegExpException When the regular expression is badly formed.
     * @throws SBHException When Something Bad Happens.
     *
     * @see Operator
     */
    public boolean greater_eql(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;

    /**
     * The <code>Operator</code> class contains a generalized implementation
     * of this method. It should be used unless a localized
     * architecture/implementation requires otherwise.
     *
     * @param bt The variable to which to compare 'this' value.
     * @return True when this value is less than the based 'bt' value,
     * false otherwise.
     * @throws InvalidOperatorException When the operator cannot be applied to
     * the two data types.
     * @throws RegExpException When the regular expression is badly formed.
     * @throws SBHException When Something Bad Happens.
     *
     * @see Operator
     */
    public boolean less(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;

    /**
     * The <code>Operator</code> class contains a generalized implementation
     * of this method. It should be used unless a localized
     * architecture/implementation requires otherwise.
     *
     * @param bt The variable to which to compare 'this' value.
     * @return True when this value is less than or equal to the based 'bt' value,
     * false otherwise.
     * @throws InvalidOperatorException When the operator cannot be applied to
     * the two data types.
     * @throws RegExpException When the regular expression is badly formed.
     * @throws SBHException When Something Bad Happens.
     *
     * @see Operator
     */
    public boolean less_eql(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;

    /**
     * The <code>Operator</code> class contains a generalized implementation
     * of this method. It should be used unless a localized
     * architecture/implementation requires otherwise.
     *
     * @param bt The variable to which to compare 'this' value.
     * @return True when regular expression evaluates to true.,
     * false otherwise.
     * @throws InvalidOperatorException When the operator cannot be applied to
     * the two data types.
     * @throws RegExpException When the regular expression is badly formed.
     * @throws SBHException When Something Bad Happens.
     *
     * @see Operator
     */
    public boolean regexp(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException;


    /**
     * Server-side serialization for OPeNDAP variables (sub-classes of
     * <code>BaseType</code>). This does not send the entire class as the
     * Java <code>Serializable</code> interface does, rather it sends only
     * the binary data values. Other software is responsible for sending
     * variable type information (see <code>DDS</code>).<p>
     * <p/>
     * Writes data to a <code>DataOutputStream</code>. This method is used
     * on the server side of the OPeNDAP client/server connection, and possibly
     * by GUI clients which need to download OPeNDAP data, manipulate it, and
     * then re-save it as a binary file.<p>
     * <p/>
     * For children of DConstructor, this method should call itself on each
     * of the components. For other types this method should call
     * externalize().
     *
     * @param dataset a <code>String</code> indicated which dataset to read
     *                from (Or something else if you so desire).
     * @param sink    a <code>DataOutputStream</code> to write to.
     * @param ce      the <code>CEEvaluator</code> to use in the parse process.
     * @param specialO    This <code>Object</code> is a goody that is used by a
     *                    Server implementations to deliver important, and as
     *                    yet unknown, stuff to the read method. If you don't
     *                    need it, make it a <code>null</code>.
     * @throws IOException thrown on any <code>OutputStream</code> exception.
     * @throws opendap.dap.NoSuchVariableException When a variable cannot be found.
     * @throws DAP2ServerSideException When there is a server error.
     * @see BaseType
     * @see DDS
     * @see ServerDDS
     */
    public void serialize(String dataset, DataOutputStream sink,
                          CEEvaluator ce, Object specialO)
            throws NoSuchVariableException, DAP2ServerSideException, IOException;
}


