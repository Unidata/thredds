/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.util.DapUtil;
import dap4.core.util.Escape;
import org.apache.http.NameValuePair;
import ucar.httpservices.HTTPUtil;

import java.net.*;
import java.nio.charset.Charset;
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
        LEAD, // lead protocol
        BASE, // base protocol
        PWD,  // including user
        HOST, // including port
        PATH,
        QUERY,
        FRAG;
    }

    // Mnemonics
    static public final EnumSet<Parts> URLONLY = EnumSet.of(Parts.BASE, Parts.PWD, Parts.HOST, Parts.PATH);
    static public final EnumSet<Parts> URLALL = EnumSet.of(Parts.LEAD, Parts.BASE, Parts.PWD, Parts.HOST, Parts.PATH, Parts.QUERY, Parts.FRAG);

    //////////////////////////////////////////////////
    // Instance variables

    protected String originaluri = null;
    protected List<String> protocols = null;
    protected String trueurl = null;  // without the query or frag and with proper single protocol
    protected URI url = null; //applied to trueurl
    protected boolean isfile = false;

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
        this.isfile = false;
        this.protocols = DapUtil.getProtocols(path); // should handle drive letters also
        if(this.protocols.size() == 0) {
            // pretend it is a file:
            this.protocols.add("file");
            path = "file://" + path;
        }
        String lastproto = this.protocols.get(this.protocols.size() - 1);

        // compute the core URI
        if(this.protocols.size() == 0) {
            this.trueurl = path;
            isfile = true;
        } else if(this.protocols.size() == 1) {
            // If the path is dap4:... then change to http:...
            String theproto = this.protocols.get(0);
            this.trueurl = path;
            this.isfile = (theproto.equals("file"));
        } else {//(this.protocols.length > 1
            int prefix = 0;
            for(int i = 0;i < this.protocols.size() - 1;i++) {
                prefix += (this.protocols.get(i) + ":").length();
            }
            this.trueurl = path.substring(prefix);
            this.isfile = (this.protocols.get(this.protocols.size() - 1).equals("file"));
        }

        // Make sure it parses
        // Note that if the path has a drive letter, this parse
        // will treat it as the host; fix below
        try {
            this.url = HTTPUtil.parseToURI(this.trueurl);
        } catch (URISyntaxException mue) {
            throw new URISyntaxException(this.trueurl, mue.getMessage());
        }

        // Extract the parts of the uri so they can
        // be modified and later reassembled

        if(!lastproto.equals(canonical(this.url.getScheme())))
            throw new URISyntaxException(this.url.toString(),
                String.format("malformed url: %s :: %s",
                    lastproto, this.url.getScheme()));
        this.baseprotocol = lastproto;
        this.userinfo = canonical(this.url.getUserInfo());
        if(this.isfile && DapUtil.hasDriveLetter(this.url.getHost() + ":")) {
            this.host = null;
            this.path = this.url.getHost() + ":";
            this.path = canonical(this.path + this.url.getPath());
        } else {
            this.host = canonical(this.url.getHost());
            if(this.url.getPort() > 0)
                this.host += (":" + this.url.getPort());
            this.path = canonical(this.url.getPath());
        }

        // Parse the raw query (before decoding)
        String query = this.url.getRawQuery();
        String[] params = null;
	    if(query != null)
	        params = this.url.getRawQuery().split(QUERYSEP);
        if(params != null && params.length > 0) {
            this.query = "";
            for(String param : params) {
                String[] pair = param.split("[=]");
                String name = Escape.urlDecode(pair[0]);
                name = name.toLowerCase(); // for consistent lookup
                String value = "";
                if(pair.length > 1) {
                    value = Escape.urlDecode(pair[1]);
                    this.fields.put(name, value);
                    this.query += name + "=" + value;
                }
            }
        }

        // Parse the raw fragment (before decoding)
        this.frag = canonical(this.url.getFragment());
        if(this.frag != null) {
            params = this.frag.split(FRAGMENTSEP);
            for(String param : params) {
                String[] pair = param.split("=");
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

    public List<String> getProtocols()
    {
        return this.protocols;
    }

    public String getLeadProtocol()
    {
        return this.protocols.get(0);
    }

    public String getBaseProtocol()
    {
        return this.baseprotocol;
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
        // Note that lead and base may be same, so case it out
        if(parts.contains(Parts.LEAD) && parts.contains(Parts.BASE) && this.protocols.size() == 1)
            uri.append(this.protocols.get(0) + ":");
        else {
            if(parts.contains(Parts.LEAD))
                uri.append(this.protocols.get(0) + ":");
            if(parts.contains(Parts.BASE))
                uri.append(this.baseprotocol + ":");
        }
        if(parts.contains(Parts.LEAD) || parts.contains(Parts.BASE) && this.protocols.size() > 0)
            uri.append("//");
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

