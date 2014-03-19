/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.util.DapException;
import dap4.core.util.ResponseFormat;

import java.util.HashMap;
import java.util.Map;

/**
 * Store protocol related constants
 */

abstract public class DapProtocol implements DapCodes
{
    static public final String X_DAP_SERVER = "TDS-4";
    static public final String X_DAP_VERSION = "4.0";

    static public final String CONSTRAINTTAG = "dap4.ce";

    static public Map<RequestMode, ContentType> contenttypes;

    static {
        contenttypes = new HashMap<RequestMode, ContentType>();
        contenttypes.put(RequestMode.DMR,
            new ContentType(RequestMode.DMR, "application/vnd.opendap.dap4.dataset-metadata+xml",
                "text/xml",
                "text/plain",
                "text/html"));
        contenttypes.put(RequestMode.DAP,
            new ContentType(RequestMode.DAP, "application/vnd.opendap.dap4.data",
                "text/xml",
                "text/plain",
                "text/html"));
        contenttypes.put(RequestMode.DSR,
            new ContentType(RequestMode.DMR, "application/vnd.opendap.dap4.dataset-services+xml",
                "text/xml",
                "text/plain",
                "text/html"));
        contenttypes.put(RequestMode.CAPABILITIES,
            new ContentType(RequestMode.CAPABILITIES, "text/xml",
                "text/xml",
                "text/plain",
                "text/html"));
        contenttypes.put(RequestMode.ERROR,
            new ContentType(RequestMode.ERROR, "application/vnd.opendap.dap4.error+xml",
                "text/xml",
                "text/plain",
                "text/html"));
    }

    //////////////////////////////////////////////////

    static public class ContentType
    {
        public RequestMode mode;
        public String contenttype;
        public String xmltype;
        public String texttype;
        public String htmltype;

        public ContentType(RequestMode mode,
                           String contenttype,
                           String xmltype,
                           String texttype,
                           String htmltype)
        {
            this.mode = mode;
            this.contenttype = contenttype;
            this.xmltype = xmltype;
            this.texttype = texttype;
            this.htmltype = htmltype;
        }

        public String getFormat(ResponseFormat format)
            throws DapException
        {
            if(format == null) format = ResponseFormat.NONE;
            switch (format) {
            case XML:
                return xmltype;
            case TEXT:
                return texttype;
            case HTML:
                return htmltype;
            case NONE:
                return contenttype;
            }
            throw new DapException(String.format("Unsupported format request: %s for Mode %s",
                format, this.mode))
                .setCode(org.apache.http.HttpStatus.SC_BAD_REQUEST);
        }
    }

}
