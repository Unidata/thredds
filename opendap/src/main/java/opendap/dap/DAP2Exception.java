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

import java.io.*;

import opendap.dap.parser.ErrorParser;
import opendap.dap.parser.ParseException;

/**
 * Holds an exception thrown by OPeNDAP server to a client.
 * <p/>
 * Unlike the other OPeNDAP exceptions, this one contains extra methods to
 * get the various fields sent by the server, and a <code>parse</code> method
 * to parse the <code>Error</code> sent from the server.
 *
 * <h3>
 * This class will be changing it's name to opendap.dap.DAP2Exception.
 * I expect that it will be deprecated in the next release.
 * <h2>You've been warned.</h2> Questions? Ask ndp@opendap.org.
 * </h3>
 *
 * @author jehamby
 * @version $Revision: 15901 $
 *
 */
public class DAP2Exception extends Exception {

    /**
     * Undefined error.
     */
    public static final int UNDEFINED_ERROR = -1;
    /**
     * Unknown error.
     */
    public static final int UNKNOWN_ERROR = 0;
    /**
     * The file specified by the OPeNDAP URL does not exist.
     */
    public static final int NO_SUCH_FILE = 1;
    /**
     * The variable specified in the OPeNDAP URL does not exist.
     */
    public static final int NO_SUCH_VARIABLE = 2;
    /**
     * The expression specified in the OPeNDAP URL is not valid.
     */
    public static final int MALFORMED_EXPR = 3;
    /**
     * The user has no authorization to read the OPeNDAP URL.
     */
    public static final int NO_AUTHORIZATION = 4;
    /**
     * The file specified by the OPeNDAP URL can not be read.
     */
    public static final int CANNOT_READ_FILE = 5;

    /*
    * Some Error objects may contain programs which can be used to
    * correct the reported error. These programs are run using a public
    * member function of the Error class. If an Error object does not
    * have an associated correction program, the program type is NO_PROGRAM.
    */

    /**
     * Undefined program type.
     */
    public static final int UNDEFINED_PROG_TYPE = -1;
    /**
     * This Error does not contain a program.
     */
    public static final int NO_PROGRAM = 0;
    /**
     * This Error contains Java bytecode.
     */
    public static final int JAVA_PROGRAM = 1;
    /**
     * This Error contains TCL code.
     */
    public static final int TCL_PROGRAM = 2;

    /**
     * The error code.
     *
     * @serial
     */
    private int errorCode;
    /**
     * The error message.
     *
     * @serial
     */
    private String errorMessage;
    /**
     * The program type.
     *
     * @serial
     */
    private int programType;

    /**
     * The program source.  if programType is TCL_PROGRAM, then this is ASCII
     * text.  Otherwise, undefined (this will need to become a byte[] array if
     * the server sends Java bytecodes, for example).
     *
     * @serial
     */
    private String programSource;

    /**
     * Construct an empty <code>DAP2Exception</code>.
     */
    public DAP2Exception() {
        // this should never be seen, since this class overrides getMessage()
        // to display its own error message.
        super("DAP2Exception");
    }

    /**
     * Construct a <code>DAP2Exception</code>.
     */
    public DAP2Exception(String msg) {
        this();
        errorCode = UNKNOWN_ERROR;
        errorMessage = msg;
    }


    /**
     * Construct a <code>DAP2Exception</code> with the given message.
     *
     * @param code the error core
     * @param msg  the error message
     */
    public DAP2Exception(int code, String msg) {
        this();
        errorCode = code;
        errorMessage = msg;
    }

    /**
     * Returns the error code.
     *
     * @return the error code.
     */
    public final int getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the error message.
     *
     * @return the error message.
     */
    public final String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns the program type.
     *
     * @return the program type.
     */
    public final int getProgramType() {
        return programType;
    }

    /**
     * Returns the program source.
     *
     * @return the program source.
     */
    public final String getProgramSource() {
        return programSource;
    }

    /**
     * Returns the detail message of this throwable object.
     *
     * @return the detail message of this throwable object.
     */
    public String getMessage() {
        return errorMessage;
    }

    /**
     * Sets the error code.
     *
     * @param code the error code.
     */
    public final void setErrorCode(int code) {
        errorCode = code;
    }

    /**
     * Sets the error message.
     *
     * @param msg the error message.
     */
    public final void setErrorMessage(String msg) {
        errorMessage = msg;
    }

    /**
     * Sets the program type.
     *
     * @param type the program type.
     */
    public final void setProgramType(int type) {
        programType = type;
    }

    /**
     * Sets the program source.
     *
     * @param source the program source.
     */
    public final void setProgramSource(String source) {
        programSource = source;
    }

    /**
     * Reads an Error description from the named InputStream.  This
     * method calls a generated parser to interpret an ASCII representation of an
     * <code>Error</code>, and regenerate it as a <code>DAP2Exception</code>.
     *
     * @param is the InputStream containing the <code>Error</code> to parse.
     * @see opendap.dap.parser.ErrorParser
     */
    public final void parse(InputStream is) {
        ErrorParser ep = new ErrorParser(is);
        try {
            ep.ErrorObject(this);
        } catch (ParseException e) {
            String msg = e.getMessage();
            if (msg != null)
		          msg = msg.replace('\"', '\'');
            errorMessage = "Error parsing server Error object!\n" + msg;
        }
    }

    /**
     * Print the Error message on the given <code>PrintWriter</code>.
     * This code can be used by servlets to throw DAP2Exception to client.
     *
     * @param os the <code>PrintWriter</code> to use for output.
     */
    public void print(PrintWriter os) {
        os.println("Error {");
        os.println("    code = " + errorCode + ";");

        // If the error message is wrapped in double quotes, print it, else,
        // add wrapping double quotes.
        if ((errorMessage != null) && (errorMessage.charAt(0) == '"'))
            os.println("    message = " + errorMessage + ";");
        else
            os.println("    message = \"" + errorMessage + "\";");

        os.println("};");
    }

    /**
     * Print the Error message on the given <code>OutputStream</code>.
     *
     * @param os the <code>OutputStream</code> to use for output.
     * @see DAP2Exception#print(PrintWriter)
     */
    public final void print(OutputStream os) {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
        print(pw);
        pw.flush();
    }
}


