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



package opendap.dap.Server;

import opendap.dap.*;

import java.io.*;

/**
 * Holds a OPeNDAP Server <code>List</code> value.
 *
 * @author ndp
 * @version $Revision: 15901 $
 * @see BaseType
 */
public abstract class SDList extends DList implements ServerMethods, RelOps {

    private static final boolean _Debug = false;

    private boolean Synthesized;
    private boolean ReadMe;

    /**
     * Constructs a new <code>SDList</code>.
     */
    public SDList() {
        super();
        Synthesized = false;
        ReadMe = false;
    }

    /**
     * Constructs a new <code>SDList</code> with name <code>n</code>.
     *
     * @param n the name of the variable.
     */
    public SDList(String n) {
        super(n);
        Synthesized = false;
        ReadMe = false;
    }

    /**
     * Write the variable's declaration in a C-style syntax. This
     * function is used to create textual representation of the Data
     * Descriptor Structure (DDS).  See <em>The OPeNDAP User Manual</em> for
     * information about this structure.
     *
     * @param os          The <code>PrintWriter</code> on which to print the
     *                    declaration.
     * @param space       Each line of the declaration will begin with the
     *                    characters in this string.  Usually used for leading spaces.
     * @param print_semi  a boolean value indicating whether to print a
     *                    semicolon at the end of the declaration.
     * @param constrained a boolean value indicating whether to print
     *                    the declartion dependent on the projection information. <b>This
     *                    is only used by Server side code.</b>
     * @see BaseType#printDecl(PrintWriter, String, boolean, boolean)
     */
    public void printDecl(PrintWriter os, String space,
                          boolean print_semi, boolean constrained) {
        if (constrained && !isProject())
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

        super.printDecl(os, space, print_semi, constrained);
    }


    /**
     * Prints the value of the variable, with its declaration.  This
     * function is primarily intended for debugging OPeNDAP applications and
     * text-based clients such as geturl.
     * <p/>
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
     * @param os           the <code>PrintWriter</code> on which to print the value.
     * @param space        this value is passed to the <code>printDecl</code> method,
     *                     and controls the leading spaces of the output.
     * @param print_decl_p a boolean value controlling whether the
     *                     variable declaration is printed as well as the value.
     * @see BaseType#printVal(PrintWriter, String, boolean)
     * @see ServerMethods#isProject()
     */
    public void printVal(PrintWriter os, String space, boolean print_decl_p) {
        if (!isProject())
            return;

        PrimitiveVector pv = getPrimitiveVector();

        if (pv instanceof BaseTypePrimitiveVector) {

            BaseTypePrimitiveVector vals = (BaseTypePrimitiveVector) pv;

            if (print_decl_p) {
                printDecl(os, space, false, true);
                os.print(" = ");
            }

            os.print("{ ");
            //vals.printVal(os, "");

            ServerMethods sm;
            int len = vals.getLength();
            for (int i = 0; i < len - 1; i++) {
                sm = (ServerMethods) vals.getValue(i);
                if (sm.isProject()) {
                    ((BaseType) sm).printVal(os, "", false);
                    os.print(", ");
                }
            }
            // print last value, if any, without trailing comma
            if (len > 0) {
                sm = (ServerMethods) vals.getValue(len - 1);
                if (sm.isProject()) {
                    ((BaseType) sm).printVal(os, "", false);
                }
            }


            os.print("}");

            if (print_decl_p)
                os.println(";");
        } else {
            super.printVal(os, space, print_decl_p);
        }
    }

    // --------------- Projection Interface

    /**
     * Set the state of this variable's projection. <code>true</code> means
     * that this variable is part of the current projection as defined by
     * the current constraint expression, otherwise the current projection
     * for this variable should be <code>false</code>.
     *
     * @param state <code>true</code> if the variable is part of the current
     *              projection, <code>false</code> otherwise.
     * @param all   This parameter has no effect for this type of variable.
     * @see CEEvaluator
     */
    @Override
    public void setProject(boolean state, boolean all) {
        setProjected(state);
        PrimitiveVector vals = getPrimitiveVector();
        ((ServerMethods) (vals.getTemplate())).setProject(state, all);

//		if(vals instanceof BaseTypePrimitiveVector){
//			System.out.println("Setting Projecttion for List member: "+
//			((BaseType)((BaseTypePrimitiveVector)vals).getTemplate()).getTypeName() + " " +
//			((BaseType)((BaseTypePrimitiveVector)vals).getTemplate()).getName());
//			((ServerMethods)((BaseTypePrimitiveVector)vals).getTemplate()).setProject(state);
//		}

    }


// --------------- RelOps Interface

    /**
     * The RelOps interface defines how each type responds to relational
     * operators. Most (all?) types will not have sensible responses to all of
     * the relational operators (e.g. DList won't know how to match a regular
     * expression but DString will). For those operators that are nonsensical a
     * class should throw InvalidOperatorException.
     */

    public boolean equal(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
        throw new InvalidOperatorException("Equals (=) operator does not work with the type SDList!");
    }

    public boolean not_equal(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
        throw new InvalidOperatorException("Not Equals (!=) operator does not work with the type SDList!");
    }

    public boolean greater(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
        throw new InvalidOperatorException("Greater Than (>)operator does not work with the type SDList!");
    }

    public boolean greater_eql(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
        throw new InvalidOperatorException("GreaterThan or equals (<=) operator does not work with the type SDList!");
    }

    public boolean less(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
        throw new InvalidOperatorException("LessThan (<) operator does not work with the type SDList!");
    }

    public boolean less_eql(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
        throw new InvalidOperatorException("LessThan oe equals (<=) operator does not work with the type SDList!");
    }

    public boolean regexp(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
        throw new InvalidOperatorException("Regular Expression's don't work with the type SDList!");
    }

// --------------- FileIO Interface

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
    public void setSynthesized(boolean state) {
        Synthesized = state;
    }

    /**
     * Get the value of the Synthesized property.
     *
     * @return <code>true</code> if this is a synthetic variable,
     *         <code>false</code> otherwise.
     */
    public boolean isSynthesized() {
        return (Synthesized);
    }

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
    public void setRead(boolean state) {
        ReadMe = state;
    }

    /**
     * Get the value of the Read property.
     *
     * @return <code>true</code> if the variable has been read,
     *         <code>false</code> otherwise.
     * @see #read(String, Object)
     * @see #setRead (boolean)
     */
    public boolean isRead() {
        return (ReadMe);
    }

    /**
     * Read a value from the named dataset for this variable.
     * <h2>Caution:</h2>
     * When reading Lists of sequences (children of DSequence) it is crucial
     * that it be handled with great care. Sequences have been implemented so that
     * only one instance (or row if you will) is retained in memory at a given time.
     * In order to correctly read a List of Sequences the read() method for the
     * List must create an instance of the Sequence for each member of the List, typically
     * by repeatedly cloning the template variable in the PrimitiveVector. The important next
     * step is: DO NOT attempt to read any data into the sequences from within the read()
     * method of the List. The sequence's data will get read, and constraint expressions
     * applied when the serialze() method of the List calls the serialize() method
     * of the Sequence, which in turn calls the read method of the Sequence. Good Luck!
     *
     * @param datasetName String identifying the file or other data store
     *                    from which to read a vaue for this variable.
     * @param specialO    This <code>Object</code> is a goody that is used by Server implementations
     *                    to deliver important, and as yet unknown, stuff to the read method. If you
     *                    don't need it, make it a <code>null</code>.
     * @return <code>true</code> if more data remains to be read, otherwise
     *         <code>false</code>. This is an abtsract method that must be implemented
     *         as part of the installation/localization of a OPeNDAP server.
     * @throws IOException
     * @throws EOFException
     */

    public abstract boolean read(String datasetName, Object specialO) throws NoSuchVariableException, IOException, EOFException;


    /**
     * <p/>
     * Server-side serialization for OPeNDAP variables (sub-classes of
     * <code>BaseType</code>).
     * This does not send the entire class as the Java <code>Serializable</code>
     * interface does, rather it sends only the binary data values. Other software
     * is responsible for sending variable type information (see <code>DDS</code>).
     * </p><p>
     * Writes data to a <code>DataOutputStream</code>. This method is used
     * on the server side of the OPeNDAP client/server connection, and possibly
     * by GUI clients which need to download OPeNDAP data, manipulate it, and
     * then re-save it as a binary file.
     * </p>
     * <h2>Caution:</h2>
     * When serializing lists of sequences (children of DSequence) it is crucial
     * that it be handled with great care. Sequences have been implemented so that
     * only one instance (or row if you will) is retained in memory at a given time.
     * In order to correctly serialize a list of sequences the read() method for the
     * List must create an instance of the sequence for each member of the list, typically
     * by repeatedly cloning the template variable in the PrimitiveVector. The important next
     * step is to NOT attempt to read any data into the sequences from within the read()
     * method of the List. The sequence's data will get read, and constraint expressions
     * applied when the serialze() method of the List calls the serialize() method
     * of the sequence. Good Luck!
     *
     * @param sink a <code>DataOutputStream</code> to write to.
     * @throws IOException thrown on any <code>OutputStream</code> exception.
     * @see BaseType
     * @see DDS
     * @see ServerDDS
     */
    public void serialize(String dataset, DataOutputStream sink, CEEvaluator ce, Object specialO)
            throws NoSuchVariableException, DAP2ServerSideException, IOException {

        PrimitiveVector vals = getPrimitiveVector();

        if (_Debug) System.out.println("\nSTARTED.............SDList.serialize(" + dataset + ")");

        if (!isRead())
            read(dataset, specialO);

        if (vals.getTemplate() instanceof DSequence || ce.evalClauses(specialO)) {

            // Because arrays of primitive types (ie int32, float32, byte, etc) are
            // handled in the C++ core using the XDR package we must write the
            // length twice for those types. For BaseType vectors, we should write
            // it only once. This is in effect a work around for a bug in the C++
            // core as the C++ core does not consume 2 length values for thge
            // BaseType vectors. Bummer...
            int length = vals.getLength();
            sink.writeInt(length);

            // Gotta check for this to make sure that DConstructor types
            // (Especially SDSequence) get handled correctly!!!
            if (vals instanceof BaseTypePrimitiveVector) {
                for (int i = 0; i < length; i++) {
                    ServerMethods sm = (ServerMethods) ((BaseTypePrimitiveVector) vals).getValue(i);
                    sm.serialize(dataset, sink, ce, specialO);
                }
            } else {

                // because both XDR and OPeNDAP read the length, we must write it twice
                sink.writeInt(length);
                vals.externalize(sink);
            }


        }
        if (_Debug) System.out.println("\nFINISHED............SDList.serialize(" + dataset + ") ");

    }


}


