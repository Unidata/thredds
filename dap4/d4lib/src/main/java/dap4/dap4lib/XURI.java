/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4lib;

import dap4.core.util.DapUtil;
import dap4.core.util.Escape;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provide an extended form of URI parser that can handle
 * multiple protocols and can parse the query and fragment parts.
 */

public class XURI
{

    //////////////////////////////////////////////////
    // Constants
    static final String QUERYSEP = "&";
    static final String FRAGMENTSEP = "&";

    // Define assembly flags

    static public enum Parts
    {
        FORMAT, // format protocol
        BASE, // base protocol
        PWD,  // including user
        HOST, // including port
        PATH,
        QUERY,
        FRAG;
    }

    // Mnemonics
    static public final EnumSet<Parts> URLONLY = EnumSet.of(Parts.BASE, Parts.PWD, Parts.HOST, Parts.PATH);
    static public final EnumSet<Parts> URLALL = EnumSet.of(Parts.FORMAT, Parts.BASE, Parts.PWD, Parts.HOST, Parts.PATH, Parts.QUERY, Parts.FRAG);
    static public final EnumSet<Parts> URLBASE = EnumSet.of(Parts.BASE, Parts.PWD, Parts.HOST, Parts.PATH, Parts.QUERY, Parts.FRAG);
    static public final EnumSet<Parts> URLPATH = EnumSet.of(Parts.PATH, Parts.QUERY, Parts.FRAG);

    //////////////////////////////////////////////////
    // Instance variables

    protected String originaluri = null;
    protected boolean isfile = false;

    protected String baseprotocol = null; // rightmost protocol
    protected String formatprotocol = null; // leftmost protocol
    protected String userinfo = null;
    protected String host = null;
    protected String path = null;
    protected String query = null;
    protected String frag = null;

    // Following are url decoded
    protected Map<String, String> queryfields // decomposed query
            = new HashMap<String, String>();
    protected Map<String, String> fragfields // decomposed fragment
            = new HashMap<String, String>();

    //////////////////////////////////////////////////
    // Constructor

    public XURI(String xurl)
            throws URISyntaxException
    {
        if(xurl == null)
            throw new URISyntaxException(xurl, "Null URI");
        // save the original uri
        this.originaluri = xurl;
        // The uri may be multi-protocol: e.g. dap4:file:...
        // Additionally, this may be a windows path, so it
        // will look like it has a single character protocol
        // that is really the drive letter.

        int[] breakpoint = new int[1];
        List<String> protocols = DapUtil.getProtocols(xurl, breakpoint); // should handle drive letters also
        String remainder = xurl.substring(breakpoint[0], xurl.length());
        switch (protocols.size()) {
        case 0: // pretend it is a file
            this.formatprotocol = "file";
            this.baseprotocol = "file";
            break;
        case 1:
            this.formatprotocol = protocols.get(0);
            if("file".equalsIgnoreCase(this.formatprotocol))
                this.baseprotocol = "file";   // default conversion
            else
                this.baseprotocol = "http";   // default conversion
            break;
        case 2:
            this.baseprotocol = protocols.get(0);
            this.formatprotocol = protocols.get(1);
            break;
        default:
            throw new URISyntaxException(xurl, "Too many protocols: at most 2 allowed");
        }
        this.isfile = "file".equals(this.baseprotocol);
        // The standard URI parser does not handle 'file:' very well,
        // so handle specially
        URI uri;
        if(this.isfile)
            parsefile(remainder);
        else
            parsenonfile(remainder); // not a file: url
        if(this.query != null)
            parseQuery(this.query);
        if(this.frag != null)
            parseFragment(this.frag);
    }

    protected void
    parsenonfile(String remainder)
            throws URISyntaxException
    {
        // Construct a usable url and parse it
        URI uri = new URI(baseprotocol + ":" + remainder);
        // Extract the parts of the uri so they can
        // be modified and later reassembled
        this.userinfo = canonical(uri.getUserInfo());
        this.host = canonical(uri.getHost());
        if(uri.getPort() > 0)
            this.host += (":" + uri.getPort());
        this.path = canonical(uri.getPath());
        // Parse the raw query (before decoding)
        this.query = uri.getRawQuery();
        // Parse the raw fragment (before decoding)
        this.frag = canonical(uri.getFragment());
    }

    protected void
    parsefile(String remainder)
    {
        // Pull off the query and fragment parts, if any.
        String query = null;
        String fragment = null;
        int qindex = remainder.indexOf("?");
        int findex = remainder.lastIndexOf("#");
        if(qindex >= 0) { // query and maybe fragment
            if(findex >= 0 && findex > qindex) {// both
                fragment = remainder.substring(findex + 1, remainder.length());
                remainder = remainder.substring(0, findex);
            }
            query = remainder.substring(qindex + 1, remainder.length());
            remainder = remainder.substring(0, qindex);
        } else if(findex >= 0) { // fragment, no query
            fragment = remainder.substring(findex + 1, remainder.length());
            remainder = remainder.substring(0, findex);
        }

        // Standardize path part to be absolute
        // => single leading '/' or windows drive letter
        StringBuilder buf = new StringBuilder(remainder);
        for(int i = 0; i < remainder.length(); i++) { // remove all leading '/'
            if(buf.charAt(i) != '/') break;
            buf.deleteCharAt(i);
        }
        // check for drive letter
        if(DapUtil.DRIVELETTERS.indexOf(buf.charAt(0)) < 0
                || buf.charAt(1) != ':') { // no drive letter, prepend '/'
            buf.insert(0, '/');
        }

        remainder = buf.toString();
        this.path = remainder;
        this.frag = fragment;
        this.query = query;
    }

    //////////////////////////////////////////////////
    // Accessors

    public String getOriginal()
    {
        return originaluri;
    }

    public String getBaseProtocol()
    {
        return baseprotocol;
    }

    public String getFormatProtocol()
    {
        return this.formatprotocol;
    }

    public void setBaseProtocol(String base)
    {
        this.baseprotocol = base;
    }

    public boolean isFile()
    {
        return this.isfile;
    }

    public String getUserinfo()
    {
        return this.userinfo;
    }

    public String getHost()
    {
        return this.host;
    }

    public String getPath()
    {
        return this.path;
    }

    public String getQuery()
    {
        return this.query;
    }

    public String getFrag()
    {
        return this.frag;
    }

    public Map<String, String> getQueryFields()
    {
        return this.queryfields;
    }

    public Map<String, String> getFragFields()
    {
        return this.fragfields;
    }

    public XURI
    parseQuery(String q)
    {
        if(q == null || q.length() == 0) return this;
        String[] params = q.split(QUERYSEP);
        this.query = q;
        for(String param : params) {
            String[] pair = param.split("[=]");
            String name = Escape.urlDecode(pair[0]);
            name = name.toLowerCase(); // for consistent lookup
            String value = "";
            if(pair.length > 1) {
                value = Escape.urlDecode(pair[1]);
                this.queryfields.put(name, value);
            }
        }
        return this;
    }

    public XURI
    parseFragment(String f)
    {
        if(f == null || f.length() == 0) return this;
        String[] params = f.split(FRAGMENTSEP);
        if(params != null && params.length > 0) {
            this.frag = f;
            for(String param : params) {
                String[] pair = param.split("=");
                String name = Escape.urlDecode(pair[0]);
                name = name.toLowerCase(); // for consistent lookup
                String value = (pair.length == 2 ? Escape.urlDecode(pair[1])
                        : "");
                this.fragfields.put(name, value);
            }
        }
        return this;
    }

    //////////////////////////////////////////////////
    // API

    /**
     * Reassemble the url using the specified parts
     *
     * @param parts to include
     * @return the assembled uri
     */

    public String
    assemble(EnumSet<Parts> parts)
    {
        StringBuilder uri = new StringBuilder();
        // Note that format and base may be same, so case it out
        int useformat = (parts.contains(Parts.FORMAT) ? 1 : 0);
        int usebase = (parts.contains(Parts.BASE) ? 2 : 0);
        switch (useformat + usebase) {
        case 0 + 0: // neither
            break;
        case 1 + 0: // FORMAT only
            uri.append(this.formatprotocol + ":");
            break;
        case 2 + 0: // BASE only
            uri.append(this.baseprotocol + ":");
            break;
        case 2 + 1: // both
            uri.append(this.formatprotocol + ":");
            if(!this.baseprotocol.equals(this.formatprotocol))
                uri.append(this.formatprotocol + ":");
            break;
        }
        uri.append(this.baseprotocol.equals("file") ? "/" : "//");

        if(userinfo != null && parts.contains(Parts.PWD))
            uri.append(this.userinfo + ":");
        if(this.host != null && parts.contains(Parts.HOST))
            uri.append(this.host);
        if(this.path != null && parts.contains(Parts.PATH))
            uri.append(this.path);
        if(this.query != null && parts.contains(Parts.QUERY))
            uri.append("?" + this.query);
        if(this.frag != null && parts.contains(Parts.FRAG))
            uri.append("#" + this.frag);
        return uri.toString();
    }


    /**
     * Canonicalize a part of a URL
     *
     * @param s part of the url
     */
    static public String
    canonical(String s)
    {
        if(s != null) {
            s = s.trim();
            if(s.length() == 0)
                s = null;
        }
        return s;
    }

    public String toString()
    {
        return originaluri;
    }
}

