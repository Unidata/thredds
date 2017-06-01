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

    static public class DatasetParse
    {
        public String prefix = null; // Prefix of the datasetpath with content type stripped
        public RequestMode mode = null;
        public ResponseFormat format = null;

        public String toString()
        {
            StringBuilder buf = new StringBuilder();
            buf.append("{");
            if(this.prefix != null) buf.append(this.prefix);
            buf.append(",");
            if(this.mode != null) buf.append(this.mode);
            buf.append(",");
            if(this.format != null) buf.append(this.format);
            buf.append("}");
            return buf.toString();
        }
    }

    /**
     * Figure out the DAP4 ContentType defined by a datasetpath.
     * It looks at the trailing sequence of extensions to figure
     * out the mode (DMR, etc) and format (XML, etc).
     *
     * @param parse        The parse datasetpath
     * @param acceptheader The value of any "Accept:" header for negotiation
     * @return
     * @throws DapException
     */
    static public ContentType
    contentTypeFor(DatasetParse parse, String acceptheader)
            throws DapException
    {
        // Decode
        if(parse.mode == null) // assume DSR
            parse.mode = RequestMode.DSR;

        // Now look at the Accept: header, if any
        ContentType acceptor = null;
        if(acceptheader != null) {
            acceptor = Accept.parse(acceptheader, DapProtocol.legaltypes);
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

        boolean coerced = (parse.format != null); // specific format specified as extension
        if(parse.format == null) { // case 1 or 2
            if(acceptor == null) // case 1
                parse.format = parse.mode.defaultFormat();
            else // case 2
                parse.format = acceptor.getResponseFormat();
        } // else case 3
        if( parse.format == null)
            assert parse.format != null;
        StringBuilder mimetype = new StringBuilder();
        if(coerced)
            mimetype.append(parse.format.mimetype()); // Case 3
        else
            mimetype.append(parse.mode.normative());  // case 1 and 2
        switch(parse.format) {
        case SERIAL:
            break;
        default:
            mimetype.append("+");
            mimetype.append(parse.format.format());
        }
        // Add the charset
        mimetype.append("; charset=");
        mimetype.append(parse.format.charset());
        ContentType ct = new ContentType(parse.mode, parse.format).setMimeType(mimetype.toString());
        return ct;
    }

    /**
     * Parse the url suffix to extract a complete list of extensions.
     * The list is filtered to only include those defined in
     * RequestMode and those defined in RequestFormat.
     * For now. we expect to find at most two extensions:
     * a mode possibly followed by a format. Note that
     * extraneous extra extensions may be present -- like .nc
     *
     * @param datasetpath
     * @return The parsed datasetpath
     * @throws DapException
     */
    static public DatasetParse
    datasetParse(String datasetpath)
            throws DapException
    {

        String[] pieces = datasetpath.split("[.]");
        DatasetParse parse = new DatasetParse();

        int pos = pieces.length - 1;
        // Start by trying to convert last element to a format
        parse.format = ResponseFormat.formatFor(pieces[pos]);
        if(parse.format == null) // Apparently no format, so see if last element is a mode
            parse.mode = RequestMode.modeFor(pieces[pos]);
        if(parse.mode == null && pos > 1) { // see if next to last is mode
            pos--;
            parse.mode = RequestMode.modeFor(pieces[pos]);
        }
        // Compute the prefix
        StringBuilder prefix = new StringBuilder();
        for(int i = 0; i < pos; i++) {
            if(i > 0) prefix.append('.');
            prefix.append(pieces[i]);
        }
        parse.prefix = prefix.toString();
        return parse;
    }
}
