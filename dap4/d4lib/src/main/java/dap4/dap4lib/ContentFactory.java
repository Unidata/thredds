/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.dap4lib;

import dap4.core.util.DapException;
import org.apache.http.HttpStatus;

/**
 * Content and format management for DAP4
 * <p>
 * Needed mappings.
 * <ul>
 * <li> Request/Response mode -- What are we asking for and getting in return?
 * E.g. DMR, DAP, DSR, CAPABILITIES, ERROR. Note that a client will always
 * get back what it requested or it will get back an ERROR.
 * <li> Normative response content type.
 * Used when the mode extension is used alone in the request.
 * e.g. .dmr, .dap, .dsr
 * For example: application/vnd.opendap.dap4.dataset-metadata
 * <li> Format for a normative response.
 * For example: application/vnd.opendap.dap4.dataset-metadata+xml
 * or application/vnd.opendap.dap4.dataset-metadata+json
 * As a rule, this will be the result of content negotiation.
 * <li> Forced response content types.
 * Used when the mode extension is used in combination with
 * a format extension. e.g. .dmr.xml, .dsr.txt, etc.
 * The resulting content must be coerced to the format
 * specified if possible (else return 406).
 * </ul>
 * Note that it is also necessary to track the set of URL extensions
 * separate from any "Accept:..." headers because the response
 * content type may differ depending on from where is was derived.
 * <p>
 * The key (static) method here is contentTypeFor(). It takes as input
 * a the suffix of a URL from the get header and it also takes
 * the set of request headers. As output, it returns a ContentType
 * object defining all the necessary output format information
 * for constructing a properly labeled response.
 */

abstract public class ContentFactory
{
    static public ContentType
    contentTypeFor(String urlsuffix, String acceptheader)
            throws DapException
    {
        // Parse the url suffix to extract a complete list of extensions.
        // The list is filtered to only include those defined in
        // RequestMode and those defined in RequestFormat.
        // For now. we expect to find at most two extensions:
        // a mode possibly followed by a format. Note that
        // extraneous extra extensions may be present -- like .nc

        String[] pieces = urlsuffix.split("[.]");
        RequestMode mode = null;
        ResponseFormat format = null;

        if(pieces.length >= 3) { // mode + format?
            mode = RequestMode.modeFor(pieces[pieces.length - 2]);
            format = ResponseFormat.formatFor(pieces[pieces.length - 1]);
        } else if(pieces.length == 2) { // mode only?
            mode = RequestMode.modeFor(pieces[pieces.length - 1]);
        }
        // Decode
        if(mode == null) // assume DSR
            mode = RequestMode.DSR;

        // Now look at the Accept: header, if any
        ContentType acceptor = null;
        if(acceptheader != null) {
            acceptor = Accept.parse(acceptheader,DapProtocol.legaltypes);
            if(acceptor == null)
                throw new DapException("Malformed Accept: header")
                        .setCode(HttpStatus.SC_NOT_ACCEPTABLE);
        }

        /**
         * Overly complex sigh!
         * 1. If we have no specific format extension and no Accept: header,
         *    then we need to use the mode's normative contenttype as the basis
         *    for our Content-Type: response header and the default format
         *    for that mode.
         * 2. If we have no specific format extension and have an explicit
         *    Accept: header, then we need to use the mode's normative
         *    contenttype as the basis for our Content-Type: response header
         *    and suffix it with the chosen negotiated format type.
         * 3. If we have a specific format extension, then we must use that
         *    format and a specific alternate mimetype.
         */

        StringBuilder mimetype = new StringBuilder();
        if(format == null) { // case 1 or 2
            if(acceptor == null) // case 1
                format = mode.defaultFormat();
            else // case 2
                format = acceptor.getResponseFormat();
            mimetype.append(mode.normative());
            mimetype.append("+");
            mimetype.append(format.format());
        } else // case 3 ; format != null
            mimetype.append(format.mimetype());
        if(format == null) // should not happen
            throw new DapException("Cannot determine response format")
                    .setCode(HttpStatus.SC_NOT_ACCEPTABLE);
        // Add the charset
        mimetype.append("; charset=");
        mimetype.append(format.charset());
        ContentType ct = new ContentType(mode, format, mimetype.toString());
        return ct;
    }
}
