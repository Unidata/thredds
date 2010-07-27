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

/**
 * Holds a OPeNDAP <code>Byte</code> value.
 *
 * @author jehamby
 * @version $Revision: 15901 $
 * @see BaseType
 */
public class DByte extends BaseType implements ClientIO {
    /**
     * Constructs a new <code>DByte</code>.
     */
    public DByte() {
        super();
    }

    /**
     * Constructs a new <code>DByte</code> with name <code>n</code>.
     *
     * @param n the name of the variable.
     */
    public DByte(String n) {
        super(n);
    }

    /**
     * The value of this <code>DByte</code>.
     */
    private byte val;

    /**
     * Get the current value as a byte.
     *
     * @return the current value.
     */
    public final byte getValue() {
        return val;
    }

    /**
     * Set the current value.
     *
     * @param newVal the new value.
     */
    public final void setValue(byte newVal) {
        val = newVal;
    }

    /**
     * Constructs a new <code>BytePrimitiveVector</code>.
     *
     * @return a new <code>BytePrimitiveVector</code>.
     */
    public PrimitiveVector newPrimitiveVector() {
        return new BytePrimitiveVector(this);
    }

    /**
     * Returns the OPeNDAP type name of the class instance as a <code>String</code>.
     *
     * @return the OPeNDAP type name of the class instance as a <code>String</code>.
     */
    public String getTypeName() {
        return "Byte";
    }


    /**
     * Prints the value of the variable, with its declaration.  This
     * function is primarily intended for debugging OPeNDAP applications and
     * text-based clients such as geturl.
     *
     * @param os           the <code>PrintWriter</code> on which to print the value.
     * @param space        this value is passed to the <code>printDecl</code> method,
     *                     and controls the leading spaces of the output.
     * @param print_decl_p a boolean value controlling whether the
     *                     variable declaration is printed as well as the value.
     * @see BaseType#printVal(PrintWriter, String, boolean)
     */
    public void printVal(PrintWriter os, String space, boolean print_decl_p) {
        if (print_decl_p) {
            printDecl(os, space, false);
            os.println(" = " + (((int) val) & 0xFF) + ";");
        } else
            // convert to unsigned and print
            os.print(((int) val) & 0xFF);
    }

    /**
     * Reads data from a <code>DataInputStream</code>. This method is only used
     * on the client side of the OPeNDAP client/server connection.
     *
     * @param source   a <code>DataInputStream</code> to read from.
     * @param sv       the <code>ServerVersion</code> returned by the server.
     * @param statusUI the <code>StatusUI</code> object to use for GUI updates
     *                 and user cancellation notification (may be null).
     * @throws EOFException if EOF is found before the variable is completely
     *                      deserialized.
     * @throws IOException  thrown on any other InputStream exception.
     * @see ClientIO#deserialize(DataInputStream, ServerVersion, StatusUI)
     */
    public synchronized void deserialize(DataInputStream source,
                                         ServerVersion sv,
                                         StatusUI statusUI)
            throws IOException, EOFException {
        // throw away first three bytes (padding)
        byte unused;
        for (int i = 0; i < 3; i++) {
            unused = source.readByte();
        }
        // read fourth byte
        val = source.readByte();

        if (statusUI != null)
            statusUI.incrementByteCount(4);
    }


    /**
     * Writes data to a <code>DataOutputStream</code>. This method is used
     * primarily by GUI clients which need to download OPeNDAP data, manipulate
     * it, and then re-save it as a binary file.
     *
     * @param sink a <code>DataOutputStream</code> to write to.
     * @throws IOException thrown on any <code>OutputStream</code>
     *                     exception.
     */
    public void externalize(DataOutputStream sink) throws IOException {
        // print three null bytes (padding)
        for (int i = 0; i < 3; i++) {
            sink.writeByte(0);
        }
        sink.writeByte(val);
    }
}


