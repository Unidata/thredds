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

package opendap.servlet;

import opendap.dap.Util;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Writes  MIME type headers to the passed streams.
 *
 * @author ndp
 * @version $Revision: 15901 $
 * @see opendap.dap.BaseType
 */

public class MimeUtil {

    // Send string to set the transfer (mime) type and server version
    // Note that the content description field is used to indicate whether valid
    // information of an error message is contained in the document and the
    // content-encoding field is used to indicate whether the data is compressed.


    public static final int unknown = 0;
    public static final int dods_das = 1;
    public static final int dods_dds = 2;
    public static final int dods_data = 3;
    public static final int dods_error = 4;
    public static final int web_error = 5;

    public static final int deflate = 1;
    public static final int x_plain = 2;

    static String contentDescription[] = {"unknown", "dods_das", "dods_dds", "dods_data",
            "dods_error", "web_error"};

    static String encoding[] = {"unknown", "deflate", "x-plain"};


    public static void setMimeText(OutputStream os, int desc, String version,
                                   int enc) {
        PrintWriter ps = new PrintWriter(new OutputStreamWriter(os,Charset.forName("UTF-8")));

        setMimeText(ps, desc, version, enc);
    }

    public static void setMimeText(PrintWriter ps, int desc, String version,
                                   int enc) {
        ps.println("HTTP/1.0 200 OK");
        ps.println("XDODS-Server: " + version);
        ps.println("Content-type: text/plain");
        ps.println("Content-Description: " + contentDescription[desc]);
        // Don't write a Content-Encoding header for x-plain since that breaks
        // Netscape on NT. jhrg 3/23/97
        if (enc != x_plain)
            ps.println("Content-Encoding: " + encoding[enc]);
        ps.println("");
    }

    public static void setMimeBinary(OutputStream os, int desc, String version,
                                     int enc) {
        PrintWriter ps = new PrintWriter(new OutputStreamWriter(os, Util.UTF8));
        setMimeBinary(ps, desc, version, enc);
    }

    public static void setMimeBinary(PrintWriter ps, int desc, String version,
                                     int enc) {
        ps.println("HTTP/1.0 200 OK");
        ps.println("XDODS-Server: " + version);
        ps.println("Content-type: application/octet-stream");
        ps.println("Content-Description: " + contentDescription[desc]);
        // Don't write a Content-Encoding header for x-plain since that breaks
        // Netscape on NT. jhrg 3/23/97
        if (enc != x_plain)
            ps.println("Content-Encoding: " + encoding[enc]);
        ps.println("");
    }

    public static void setMimeError(OutputStream os, int code, String reason,
                                    String version) {
        PrintWriter ps = new PrintWriter(new OutputStreamWriter(os,Charset.forName("UTF-8")));
        setMimeError(ps, code, reason, version);
    }

    public static void setMimeError(PrintWriter ps, int code, String reason,
                                    String version) {
        ps.println("HTTP/1.0 " + code + " " + reason);
        ps.println("XDODS-Server: " + version);
        ps.println("");
    }

}


