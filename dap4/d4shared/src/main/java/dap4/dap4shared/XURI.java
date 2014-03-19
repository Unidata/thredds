/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.util.DapUtil;
import dap4.core.util.Escape;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
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

    //////////////////////////////////////////////////
    // Instance variables

    protected String originaluri = null;
    protected String[] protocols = null;
    protected String coreuri = null;
    protected URI uri = null; //applied to coreurl

    protected String[] allprotocols = null;
    protected String baseprotocol = null;
    protected String userinfo = null;
    protected String host = null;
    protected String path = null;
    protected String query = null;
    protected String frag = null;

    // Following are url decoded
    Map<String, String> fields // decomposed query
        = new HashMap<String, String>();
    Map<String, String> parameters // decomposed fragment
        = new HashMap<String, String>();

    //////////////////////////////////////////////////
    // Constructor

    public XURI(String path)
        throws URISyntaxException
    {
        if(path == null)
            throw new URISyntaxException(path, "Null URI");
        // save the original uri
        this.originaluri = path;
        // The uri may be multi-protocol: e.g. dap4:file:...
        // Additionally, this may be a windows path, so it
        // will look like it has a single character protocol
        // that is really the drive letter.
        this.protocols = DapUtil.getProtocols(path);
        if(this.protocols.length == 0) {
            // pretend it is a file:
            this.protocols = new String[]{"file:"};
            this.path = "file://" + this.path;
        } else if(this.protocols.length == 1) {
            // See if this looks like a drive character
            if(this.protocols[0].length() == 1) {
                char c = this.protocols[0].charAt(0);
                if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                    // looks like a drive letter
                    this.protocols = new String[]{"file"};
                    path = "file://" + path;
                }
            }
        }
        // compute the core URI
        if(this.protocols.length <= 1) {
            this.coreuri = path;
        } else {//(this.protocols > 1)
            int prefix = 0;
            for(int i = 0;i < this.protocols.length - 1;i++)
                prefix += (this.protocols[i] + ":").length();
            this.coreuri = path.substring(prefix);
        }

        // Make sure it parses
        this.uri = new URI(this.coreuri);

        // Extract the parts of the uri so they can
        // be modified and later reassembled
        assert this.protocols[this.protocols.length - 1].equals(canonical(this.uri.getScheme()));
        this.baseprotocol = this.protocols[this.protocols.length - 1];
        this.userinfo = canonical(this.uri.getRawUserInfo());
        this.host = canonical(this.uri.getRawAuthority()); // including port
        this.path = canonical(this.uri.getRawPath());
        this.query = canonical(this.uri.getRawQuery());
        this.frag = canonical(this.uri.getRawFragment());

        // Parse the raw query (before decoding)
        if(this.query != null) {
            String[] pieces = this.query.split(QUERYSEP);
            for(String piece : pieces) {
                String[] pair = piece.split("=");
                String name = Escape.urlDecode(pair[0]);
                name = name.toLowerCase(); // for consistent lookup
                String value = (pair.length == 2 ? Escape.urlDecode(pair[1]) : "");
                this.fields.put(name, value);
            }
        }

        // Parse the raw fragment (before decoding)
        if(this.frag != null) {
            String[] pieces = this.frag.split(FRAGMENTSEP);
            for(String piece : pieces) {
                String[] pair = piece.split("=");
                String name = Escape.urlDecode(pair[0]);
                name = name.toLowerCase(); // for consistent lookup
                String value = (pair.length == 2 ? Escape.urlDecode(pair[1])
                    : "");
                this.parameters.put(name, value);
            }
        }
    }

    //////////////////////////////////////////////////
    // Accessors

    public String getOriginal()
    {
        return originaluri;
    }

    public String[] getProtocols()
    {
        return this.protocols;
    }

    public String getLeadProtocol()
    {
        return this.protocols[0];
    }

    public String getBaseProtocol()
    {
        return this.baseprotocol;
    }

    public void setBaseProtocol(String base)
    {
        this.baseprotocol = base;
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

    public Map<String, String> getFields()
    {
        return fields;
    }

    public Map<String, String> getParameters()
    {
        return parameters;
    }

    //////////////////////////////////////////////////
    // API 

    public String
    pureURI()
    {
        // reassemble as modified and without the query and fragment parts
        try {
            String auth = this.host;
            if(this.userinfo != null) auth = this.userinfo + "@" + auth;
            URI urx = new URI(getBaseProtocol(), auth, this.path, null, null);
            return urx.toString();
        } catch (URISyntaxException e) {
            assert (false) : "Internal Error";
        }
        return null;
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
}


