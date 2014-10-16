/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.servlet;

import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.dap4shared.XURI;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

import dap4.servlet.URLMap.Result;

/**
 * Define a URL map that assumes that there some prefix
 * of the urlpath that can be replaced by a path prefix
 * and that everything else is the same.
 * The mapping is carried out from a properties file
 * that has lines of the form <url path prefix>=<file path prefix>
 */
public class URLMapDefault implements URLMap
{

    //////////////////////////////////////////////////
    // Instance Variables

    SortedMap<String, String> url2path = new TreeMap<String, String>();
    SortedMap<String, String> path2url = new TreeMap<String, String>();

    //////////////////////////////////////////////////
    // Constructor(s)

    public URLMapDefault()
    {
    }

    //////////////////////////////////////////////////
    // Accessors

    @Override
    public void addEntry(String urlpath, String fileprefix)
            throws DapException
    {
        // Canonicalize the urlpath
        String urlprefix = DapUtil.canonicalpath(urlpath);
        // Canonicalize the file path
        fileprefix = DapUtil.canonicalpath(fileprefix);
        url2path.put(urlprefix, fileprefix); // overwrite
        path2url.put(fileprefix, urlprefix); // overwrite
    }

    //////////////////////////////////////////////////
    // File loader

    public void load(String filepath)
            throws IOException
    {
        String[] lines;
        try (InputStream is = new FileInputStream(filepath);) {
            String content = DapUtil.readtextfile(is);
            lines = content.split("[\n]");
        }
        for(String line : lines) {
            String[] pieces = line.split("[=]");
            if(pieces.length != 2)
                throw new IOException("File: " + filepath + "; malformed line: " + line);
            addEntry(pieces[0], pieces[1]);
        }
    }

    //////////////////////////////////////////////////
    // URLMap API

    @Override
    public Result
    mapURL(String urlpath)
            throws DapException
    {
        // Canonicalize the urlpath
        urlpath = DapUtil.canonicalpath(urlpath);
        Result result = longestmatch(url2path, urlpath);
        return result;
    }

    @Override
    public Result
    mapPath(String path)
            throws DapException
    {
        // canonicalize path
        path = DapUtil.canonicalpath(path);
        Result result = longestmatch(path2url, path);
        return result;
    }

    protected Result
    longestmatch(SortedMap<String, String> map, String prefix)
    {
        Result result = new Result();
        List<String> matches = new ArrayList<>();
        for(Map.Entry<String, String> entry : map.entrySet()) {
            if(prefix.startsWith(entry.getKey()))
                matches.add(entry.getKey());
        }
        String longestmatch = null;
        if(map.get(prefix) != null)
            longestmatch = prefix;
        for(int i = 0; i < matches.size(); i++) {
            String candidate = matches.get(i);
            if(prefix.startsWith(candidate)) {
                if(longestmatch == null || candidate.length() > longestmatch.length())
                    longestmatch = candidate;
            }
        }
        if(longestmatch != null)
            result.prefix = map.get(longestmatch);
        else
            return null;

        /*
        // lastKey returns everything less than path, but
        // we need less-or-equal, so we have to do a separate
        // check for exact match
        result.prefix = map.get(prefix);
        if(result.prefix == null) {
            SortedMap<String, String> submap = map.headMap(prefix);
            if(submap.size() == 0)
                return null; // prefix is not here in any form
            longestmatch = submap.lastKey();
            result.prefix = (String) submap.get(longestmatch);
        } */
        result.suffix = prefix.substring(longestmatch.length());
        if(result.prefix.endsWith("/"))
            result.prefix = result.prefix.substring(0,result.prefix.length()-1);
        if(result.suffix.startsWith("/"))
            result.suffix = result.suffix.substring(1);
        return result;
    }


    protected StringBuilder
    canonURL(XURI xurl)
    {
        String proto = xurl.getLeadProtocol();
        String host = xurl.getHost();
        String path = DapUtil.canonicalpath(xurl.getPath());
        StringBuilder urlbuf = new StringBuilder();
        if(proto != null && proto.length() > 0)
            urlbuf.append(proto + "://");
        urlbuf.append(host);
        if(path == null)
            path = "";
        urlbuf.append("/" + path);
        return urlbuf;
    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        for(String url : url2path.keySet()) {
            buf.append(url + " <=> " + url2path.get(url));
        }
        return buf.toString();
    }
}

