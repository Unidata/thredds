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

// package dods.dap.Server.servlet; // JC-CHANGED
package dods.servlet;

import java.io.*;

/**
 * Holds a DODS Server <code>Sequence</code> value.
 *
 * @version $Revision: 1.1 $
 * @author ndp
 * @see BaseType
 */

public class MimeUtil {

        // Send string to set the transfer (mime) type and server version
        // Note that the content description field is used to indicate whether valid
        // information of an error message is contained in the document and the
        // content-encoding field is used to indicate whether the data is compressed.


        public static final int unknown    = 0;
        public static final int dods_das   = 1;
        public static final int dods_dds   = 2;
        public static final int dods_data  = 3;
        public static final int dods_error = 4;
        public static final int web_error  = 5;

        public static final int deflate = 1;
        public static final int x_plain = 2;

        static String contentDescription[]={"unknown", "dods_das", "dods_dds", "dods_data",
                                "dods_error", "web_error"};

        static String encoding[]={"unknown", "deflate", "x-plain"};


        public static void setMimeText(OutputStream os, int desc, String version,
                        int enc )
        {
        PrintStream ps = new PrintStream(os);
                setMimeText(ps,desc,version,enc);
        }

        public static void setMimeText(PrintStream ps, int desc, String version,
                        int enc )
        {
                ps.println("HTTP/1.0 200 OK" );
                ps.println("XDODS-Server: " + version );
                ps.println("Content-type: text/plain");
                ps.println("Content-Description: " + contentDescription[desc]);
                // Don't write a Content-Encoding header for x-plain since that breaks
                // Netscape on NT. jhrg 3/23/97
                if (enc != x_plain)
                        ps.println("Content-Encoding: " + encoding[enc]);
                ps.println("");
        }

        public static void setMimeBinary(OutputStream os, int desc, String version,
                        int enc )
        {
        PrintStream ps = new PrintStream(os);
                setMimeBinary(ps,desc,version,enc);
        }

        public static void setMimeBinary(PrintStream ps, int desc, String version,
                        int enc )
        {
                ps.println("HTTP/1.0 200 OK" );
                ps.println("XDODS-Server: " + version );
                ps.println("Content-type: application/octet-stream");
                ps.println("Content-Description: " + contentDescription[desc]);
                // Don't write a Content-Encoding header for x-plain since that breaks
                // Netscape on NT. jhrg 3/23/97
                if (enc != x_plain)
                        ps.println("Content-Encoding: " + encoding[enc]);
                ps.println("");
        }

        public static void setMimeError(OutputStream os, int code, String reason,
                        String version)
        {
        PrintStream ps = new PrintStream(os);
                setMimeError(ps,code,reason,version);
        }

        public static void setMimeError(PrintStream ps, int code, String reason,
                        String version)
        {
                ps.println("HTTP/1.0 " + code + " " + reason );
                ps.println("XDODS-Server: " + version );
                ps.println("");
        }

}
