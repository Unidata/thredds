/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4lib;

import java.util.ArrayList;
import java.util.List;

/**
 * Store protocol related constants
 */

abstract public class DapProtocol implements DapCodes
{
    static public final String X_DAP_SERVER = "TDS-5";
    static public final String X_DAP_VERSION = "4.0";

    static public final String CONSTRAINTTAG = "dap4.ce";

    // Define all legal ContentType objects
    static public List<ContentType> legaltypes;

    static {
        legaltypes = new ArrayList<>();
        legaltypes.add(new ContentType(RequestMode.DMR, ResponseFormat.XML));
        legaltypes.add(new ContentType(RequestMode.DMR, ResponseFormat.TEXT));
        legaltypes.add(new ContentType(RequestMode.DMR, ResponseFormat.HTML));
        legaltypes.add(new ContentType(RequestMode.DMR, ResponseFormat.PROTOBUF));
        legaltypes.add(new ContentType(RequestMode.DAP, ResponseFormat.XML));
        legaltypes.add(new ContentType(RequestMode.DAP, ResponseFormat.TEXT));
        legaltypes.add(new ContentType(RequestMode.DAP, ResponseFormat.HTML));
        legaltypes.add(new ContentType(RequestMode.DAP, ResponseFormat.PROTOBUF));
        legaltypes.add(new ContentType(RequestMode.DSR, ResponseFormat.XML));
        //legaltypes.add(new ContentType(RequestMode.CAPABILITIES,ResponseFormat.XML, "text/xml"));
        //legaltypes.add(new ContentType(RequestMode.ERROR,ResponseFormat.XML, "text/xml"));
    }

    static public String defaultmimetype(RequestMode mode)
    {
        StringBuilder mt = new StringBuilder();
        mt.append(mode.normative());
        switch (mode) {
        case DAP:
            break;
        default:
            mt.append("+xml");
            break;
        }
        return mt.toString();
    }

}
