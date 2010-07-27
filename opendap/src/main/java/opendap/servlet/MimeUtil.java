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

package opendap.servlet;

import java.io.*;

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
        PrintStream ps = new PrintStream(os);
        setMimeText(ps, desc, version, enc);
    }

    public static void setMimeText(PrintStream ps, int desc, String version,
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
        PrintStream ps = new PrintStream(os);
        setMimeBinary(ps, desc, version, enc);
    }

    public static void setMimeBinary(PrintStream ps, int desc, String version,
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
        PrintStream ps = new PrintStream(os);
        setMimeError(ps, code, reason, version);
    }

    public static void setMimeError(PrintStream ps, int code, String reason,
                                    String version) {
        ps.println("HTTP/1.0 " + code + " " + reason);
        ps.println("XDODS-Server: " + version);
        ps.println("");
    }

}


