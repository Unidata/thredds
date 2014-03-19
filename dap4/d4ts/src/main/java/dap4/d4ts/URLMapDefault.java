/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.d4ts;

import dap4.core.util.DapException;
import dap4.core.util.DapUtil;

/**
 * Define a simple URL map that assumes that there some prefix
 * of the url is replaced by a path prefix and that everything
 * else is the same.
 */
public class URLMapDefault extends URLMap
{
    //////////////////////////////////////////////////
    // Instance Variables

    String urlprefix;
    String pathprefix;

    //////////////////////////////////////////////////
    // Constructor(s)

    public URLMapDefault(String urlprefix, String pathprefix)
    {
        this.urlprefix = urlprefix;
        pathprefix = pathprefix.replace('\\', '/');
        pathprefix = DapUtil.canonicalpath(pathprefix, false);
        this.pathprefix = pathprefix;
    }

    //////////////////////////////////////////////////
    // URLMap API

    public String
    mapURL(String url)
        throws DapException
    {
        if(!url.startsWith(urlprefix))
            throw new DapException("URL not mappable: " + url);
        String suffix = url.substring(urlprefix.length(), url.length());
        while(suffix.charAt(0) == '/')
            suffix = suffix.substring(1);
        return pathprefix + "/" + suffix;
    }

    public String
    mapPath(String path)
        throws DapException
    {
        path = DapUtil.canonicalpath(path, false);
        if(!path.startsWith(pathprefix))
            throw new DapException("Path not mappable: " + path);
        String suffix = path.substring(pathprefix.length(), path.length());
        while(suffix.charAt(0) == '/')
            suffix = suffix.substring(1);
        return urlprefix + "/" + suffix;
    }

}

