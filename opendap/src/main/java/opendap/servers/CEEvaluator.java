/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
// 
// Author: James Gallagher <jgallagher@opendap.org>
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
// 
// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// - Neither the name of the OPeNDAP nor the names of its contributors may
//   be used to endorse or promote products derived from this software
//   without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/////////////////////////////////////////////////////////////////////////////


package opendap.servers;

import java.util.*;
import java.io.*;

import opendap.dap.*;
import opendap.dap.parsers.*;
import opendap.servers.parsers.CeParser;
import opendap.servlet.ReqState;
import opendap.util.Debug;

/**
 * This class is used to parse and evaluate a constraint expression. When
 * constructed it must be passed a valid DDS along with the expression. This
 * DDS will be used as the environment (collection of variables and
 * functions) during the parse and evaluation of the constraint expression.
 * <p/>
 * A server (servlet, CGI, ...) must first instantiate the DDS (possibly
 * reading it from a cache) and then create and instance of this class. Once
 * created, the constraint may be parsed and then evaluated. The class
 * supports sending data based on the results of CE evaluation. That is, the
 * send() method of the class combines both the evaluation of the constraint
 * and the output of data values so that the server can return data using a
 * single method call.<p>
 * <p/>
 * <b>Custom parsing</b>
 * The CEEvaluator parses constraint expressions into Clause objects
 * using a ClauseFactory. Customized behavior during parsing can be
 * achieved by passing a customized ClauseFactory into the CEEvaluator.<p>
 * <p/>
 * <b>Support for server side functions</b>
 * Server side functions are supported via the FunctionLibrary class.
 * Custom server side function support is achieved by using
 * a customized ClauseFactory which in turn contains a customized
 * FunctionLibrary.<p>
 * <p/>
 * More details are found in the documentation for the respective classes.
 *
 * @author jhrg
 * @author ndp
 * @author Joe Wielgosz (joew@cola.iges.org)
 * @version $Revision: 21071 $
 * @see ServerDDS
 * @see ServerMethods
 * @see ClauseFactory
 * @see FunctionLibrary
 * @see Clause
 */

public class CEEvaluator {

    private static boolean _Debug;

    /**
     * This contains the DDS to be used during parse and evaluation of the
     * CE.
     */
    private ServerDDS _dds;

    /**
     * The Clause objects which hold the parsed selection information.
     */
    private Vector _cv;

    /**
     * The factory which will be used by the parser to construct the clause
     * tree. This allows servers to pass in a factory which creates
     * custom clause objects.
     */
    private ClauseFactory clauseFactory;

    /**
     * Construct a new <code>CEEvaluator</code> with <code>dds</code> as the
     * DDS object with which to resolve all variable and function names.
     *
     * @param dds DDS object describing the dataset targeted by this
     *            constraint.
     */
    public CEEvaluator(ServerDDS dds) {
        _dds = dds;
        _cv = new Vector();
        _Debug = Debug.isSet("CE");
        if (_Debug)
            System.out.println("CE debugging enabled.");

    }

    /**
     * Construct a new <code>CEEvaluator</code> with <code>dds</code> as the
     * DDS object with which to resolve all variable and function names, and
     * <code>clauseFactory</code> as a source of Clause objects .
     *
     * @param clauseFactory The factory which will be used by the parser to construct the clause
     *                      tree. This allows servers to pass in a factory which creates
     *                      custom clause objects.
     * @param dds           DDS object describing the dataset targeted by this
     *                      constraint.
     */
    public CEEvaluator(ServerDDS dds, ClauseFactory clauseFactory) {
        _dds = dds;
        _cv = new Vector();
        _Debug = Debug.isSet("CE");
        if (_Debug)
            System.out.println("CE debugging enabled.");

        this.clauseFactory = clauseFactory;

        _Debug = Debug.isSet("CEEvaluator");
    }


    /**
     * Return a reference to the CEEvaluator's DDS object.
     */
    public ServerDDS getDDS() {
        return _dds;
    }

    /**
     * Parse a constraint expression. Variables in the projection are marked
     * as such in the CEEvaluator's ServerDDS instance. The selection
     * subexpression is then parsed and a list of Clause objects is built.
     * <p/>
     * The parser is located in opendap.servers.parsers.CeParser.
     *
     * @param constraint The constraint expression to parse.
     * @throws ParseException
     * @throws NoSuchVariableException
     * @throws NoSuchFunctionException
     * @throws InvalidOperatorException
     * @throws InvalidParameterException
     * @throws SBHException
     */
    public void parseConstraint(String constraint, String urlencoded)
            throws ParseException, opendap.dap.DAP2Exception, NoSuchVariableException,
            NoSuchFunctionException, InvalidOperatorException,
            InvalidParameterException, SBHException, WrongTypeException {

        if (clauseFactory == null) {
            clauseFactory = new ClauseFactory();
        }

        // Parses constraint expression (duh...) and sets the
        // projection flag for each member of the CE's ServerDDS
        // instance. This also builds the list of clauses.
	
	try {
        CeParser.constraint_expression(this,
            _dds.getFactory(),
            clauseFactory,
            constraint, urlencoded);
	} catch (ConstraintException ce) {
	    // convert to a DAP2Exception
        ce.printStackTrace();
	    throw new DAP2Exception(ce);
	}

        if (_Debug) {
            int it = 0;
            Enumeration ec = getClauses();

            System.out.println("Results of clause parsing:");
            if (!ec.hasMoreElements())
                System.out.println("    No Clauses Found.");

            while (ec.hasMoreElements()) {
                it++;
                System.out.println("    Clause " + it + ": " + ec.nextElement());
            }
        }

    }

    /**
     * Convenience wrapper for parseConstraint.
     *
     * @param rs
     * @throws ParseException
     * @throws opendap.dap.DAP2Exception
     * @throws NoSuchVariableException
     * @throws NoSuchFunctionException
     * @throws InvalidOperatorException
     * @throws InvalidParameterException
     * @throws SBHException
     * @throws WrongTypeException
     */
    public void parseConstraint(ReqState rs)
            throws ParseException, opendap.dap.DAP2Exception, NoSuchVariableException,
            NoSuchFunctionException, InvalidOperatorException,
            InvalidParameterException, SBHException, WrongTypeException
    {
           parseConstraint(rs.getConstraintExpression(),rs.getRequestURL().toString());
    }

    /**
     * Add a clause to the constraint expression.
     *
     * @param c The Clause to append.
     */
    public void appendClause(Clause c) {
        if (c != null) {
            _cv.add(c);
        }
    }

    /**
     * Remove a clause from the constraint expression. This will
     * will remove the first occurence of the passed clause from
     * the constraint expression. This is done be reference, so if
     * the passed Clause object is NOT already in the constraint
     * expression then nothing happens. And, if it should appear
     * more than once (which I <b>don't</b> think is possible) only
     * the first occurence will be removed.
     *
     * @param c The Clause to append.
     * @return True if constraint expression contained the passed Clause
     *         object and it was successfully removed.
     */
    public boolean removeClause(Clause c) {
        if (c != null) {
            return (_cv.remove(c));
        }
        return (false);
    }

    /**
     * Get access to the list of clauses built by parsing the selection part
     * of the constraint expression.
     * <p/>
     * NB: This is not valid until the CE has been parsed!
     */
    public final Enumeration getClauses() {
        return _cv.elements();
    }


    /**
     * This function sends the variables described in the constrained DDS to
     * the output described by <code>sink</code>. This function calls
     * <code>parse_constraint()</code>, <code>BaseType::read()</code>, and
     * <code>ServerIO::serialize()</code>.
     *
     * @param dataset  The name of the dataset to send.
     * @param sink     A pointer to the output buffer for the data.
     * @param specialO An <code>Object</code> passed by the server. This is typically used by server implementations
     *                 to deliver needed functionaliy or information to the read methods of each <code>BaseType</code>.
     * @see #parseConstraint
     * @see ServerMethods#serialize(String, DataOutputStream,
     *      CEEvaluator, Object) ServerMethods.serialize()
     */
    public void send(String dataset, OutputStream sink, Object specialO)
            throws NoSuchVariableException, DAP2ServerSideException, IOException {

        Enumeration e = _dds.getVariables();
        while (e.hasMoreElements()) {

            ServerMethods s = (ServerMethods) e.nextElement();

            if (_Debug)
                System.out.println("Sending variable: " +
                        ((BaseType) s).getEncodedName());

            if (s.isProject()) {

                if (_Debug)
                    System.out.println("Calling "
                            + ((BaseType) s).getTypeName()
                            + ".serialize() (Name: "
                            + ((BaseType) s).getEncodedName()
                            + ")");

              //System.out.printf("serialize %s (%s) start=%d%n ", ((BaseType) s).getName(), ((BaseType) s).getTypeName(),
              //    ((DataOutputStream) sink).size());

                s.serialize(dataset, (DataOutputStream) sink, this, specialO);
            }
        }
      //System.out.printf("serialize total size=%d%n ", ((DataOutputStream) sink).size());

    }


    /**
     * Evaluate all of the Clauses in the Clause vector.
     *
     * @param specialO That special Object that can be passed down
     *                 through the <code>DDS.send()</code> method.
     * @return True if all the Clauses evaluate to true, false otherwise.
     */
    public boolean evalClauses(Object specialO) throws NoSuchVariableException, DAP2ServerSideException, IOException {

        boolean result = true;
        Enumeration ec = getClauses();

        while (ec.hasMoreElements() && result == true) {
            Object o = ec.nextElement();
            if (_Debug) {
                System.out.println("Evaluating clause: " + ec.nextElement());
            }

            result = ((TopLevelClause) o).evaluate();

        }

        // Hack: pop the projections of all DArrayDimensions that
        // have been pushed.


        return (result);
    }


    /**
     * Mark all the variables in the DDS either as part of the current
     * projection (when <code>state</code> is true) or not
     * (<code>state</code> is false). This is a convenience function that
     * provides a way to clear or set an entire dataset described by a DDS
     * with respect to its projection.
     *
     * @param state true if the variables should all be projected, false is
     *              no variable should be projected.
     */
    public void markAll(boolean state)
            throws DAP2Exception, NoSuchVariableException, SBHException
    {
        // For all the Variables in the DDS
        Enumeration e = _dds.getVariables();
        while (e.hasMoreElements()) {
            // Get the thing
            Object o = e.nextElement();
            //- Clip this to stop marking all dimensions of Grids and Arrays
            // If we are marking all for true, then we need to make sure
            // we get all the parts of each array and grid

            // This code should probably be moved into SDArray and SDGrid.
            // There we should add a resetProjections() method that changes
            // the current projection to be the entire array. 11/18/99 jhrg
            if (state) {
                if (o instanceof SDArray) { // Is this thing a SDArray?
                    SDArray SDA = (SDArray) o;

                    // Get it's DArrayDimensions
                    Enumeration eSDA = SDA.getDimensions();
                    while (eSDA.hasMoreElements()) {
                        DArrayDimension dad = (DArrayDimension) eSDA.nextElement();
                        // Tweak it's projection state
                        dad.setProjection(0, 1, dad.getSize() - 1);
                    }
                } else if (o instanceof SDGrid) {  // Is this thing a SDGrid?
                    SDGrid SDG = (SDGrid) o;
                    SDArray sdgA = (SDArray) SDG.getVar(0); // Get it's internal SDArray.

                    // Get it's DArrayDimensions
                    Enumeration eSDA = sdgA.getDimensions();
                    while (eSDA.hasMoreElements()) {
                        DArrayDimension dad = (DArrayDimension) eSDA.nextElement();
                        // Tweak it's projection state
                        dad.setProjection(0, 1, dad.getSize() - 1);

                    }
                }
            }
            //-------------------------- End Clip ---------------------------

            ServerMethods s = (ServerMethods) o;
            s.setProject(state);
        }
    }

    /**
         * Print all of the Clauses in the Clause vector.
         *
         */
        public void printConstraint(PrintWriter pw)
        {
            Enumeration ec = getClauses();
            boolean first = true;
            while (ec.hasMoreElements()) {
                Clause cl = (Clause)ec.nextElement();
                if(!first) pw.print(" & ");
                cl.printConstraint(pw);
                first = false;
            }
            pw.flush();
        }
}
