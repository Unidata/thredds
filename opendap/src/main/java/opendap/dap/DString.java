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


package opendap.dap;

import java.io.*;

/**
 * Holds a OPeNDAP <code>String</code> value.
 *
 * @author jehamby
 * @version $Revision: 21071 $
 * @see BaseType
 */
public class DString extends BaseType implements ClientIO {
    /**
     * Constructs a new <code>DString</code>.
     */
    public DString() {
        super();
        val = "String value has not been set.";
    }

    /**
     * Constructs a new <code>DString</code> with name <code>n</code>.
     *
     * @param n the name of the variable.
     */
    public DString(String n) {
        super(n);
        val = "String value has not been set.";
    }

    /**
     * The value of this <code>DString</code>.
     */
    private String val;

    /**
     * Get the current value as a <code>String</code>.
     *
     * @return the current value.
     */
    public final String getValue() {
        return val;
    }

    /**
     * Set the current value.
     *
     * @param newVal the new value.
     */
    public final void setValue(String newVal) {
        val = newVal;
    }

    /**
     * Returns the OPeNDAP type name of the class instance as a <code>String</code>.
     *
     * @return the OPeNDAP type name of the class instance as a <code>String</code>.
     */
    public String getTypeName() {
        return "String";
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
            os.println(" = \"" + Util.escattr(val) + "\";");
        } else
            os.print("\"" + Util.escattr(val) + "\"");
    }

    /**
     * Reads data from a <code>DataInputStream</code>. This method is only used
     * on the client side of the OPeNDAP client/server connection.
     *
     * @param source   a <code>DataInputStream</code> to read from.
     * @param sv       the <code>ServerVersion</code> returned by the server.
     * @param statusUI the <code>StatusUI</code> object to use for GUI updates
     *                 and user cancellation notification (may be null).
     * @throws EOFException      if EOF is found before the variable is completely
     *                           deserialized.
     * @throws IOException       thrown on any other InputStream exception.
     * @throws DataReadException if a negative string length was read.
     * @see ClientIO#deserialize(DataInputStream, ServerVersion, StatusUI)
     */
    public synchronized void deserialize(DataInputStream source,
                                         ServerVersion sv,
                                         StatusUI statusUI)
            throws IOException, EOFException, DataReadException {
        int dap_len = source.readInt();

        //System.out.println("DString deserialize string dap_length: "+ dap_len);

        if (dap_len < 0)
            throw new DataReadException("Negative string length (dap_length: "+ dap_len +") read.");
        if (dap_len > Short.MAX_VALUE)
            throw new DataReadException("DString deserialize string length (dap_length: "+ dap_len +") too large.");



        int modFour = dap_len % 4;
        // number of bytes to pad
        int pad = (modFour != 0) ? (4 - modFour) : 0;

        byte byteArray[] = new byte[dap_len];

        // With blackdown JDK1.1.8v3 (comes with matlab 6) read() didn't always
        // finish reading a string.  readFully() insures that it gets all <dap_len>
        // characters it requested.  rph 08/20/01.

        //source.read(byteArray, 0, dap_len);
        source.readFully(byteArray, 0, dap_len);

        // pad out to a multiple of four bytes
        byte unused;
        for (int i = 0; i < pad; i++)
            unused = source.readByte();

        if (statusUI != null)
            statusUI.incrementByteCount(4 + dap_len + pad);

        // convert bytes to a new String using ISO8859_1 (Latin 1) encoding.
        // This was chosen because it converts each byte to its Unicode value
        // with no translation (the first 256 glyphs in Unicode are ISO8859_1)
        try {
            val = new String(byteArray, 0, dap_len, "ISO8859_1");
        }
        catch (UnsupportedEncodingException e) {
            // this should never happen
            throw new UnsupportedEncodingException("ISO8859_1 encoding not supported by this VM!");
        }
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
        // convert String to a byte array using ISO8859_1 (Latin 1) encoding.
        // This was chosen because it converts each byte to its Unicode value
        // with no translation (the first 256 glyphs in Unicode are ISO8859_1)

        try {
            byte byteArray[] = val.getBytes("ISO8859_1");
            sink.writeInt(byteArray.length);
            int modFour = byteArray.length % 4;
            // number of bytes to pad
            int pad = (modFour != 0) ? (4 - modFour) : 0;
            sink.write(byteArray, 0, byteArray.length);
            for (int i = 0; i < pad; i++) {
                sink.writeByte(0);
            }
        }
        catch (UnsupportedEncodingException e) {
            // this should never happen
            throw new UnsupportedEncodingException("ISO8859_1 encoding not supported by this VM!");
        }
    }
}


