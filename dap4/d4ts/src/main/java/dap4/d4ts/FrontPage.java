/* Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
   See the LICENCE file for more information. */

package dap4.d4ts;

import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.servlet.DapLog;
import dap4.servlet.ServletInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Given a directory, return a front page of HTML
 * that lists all of the files in that page.
 *
 * @author Dennis Heimbigner
 */

public class FrontPage
{

    //////////////////////////////////////////////////
    // Constants

    static final boolean NO_VLEN = true; // ignore vlen datasets for now

    // Define the file sources of interest
    static final FileSource[] SOURCES = new FileSource[]{
            new FileSource(".nc", "netCDF"),
            new FileSource(".hdf5", "HDF5"),
            new FileSource(".syn", "Synthetic")
    };

    // Remote Test server: should match values in TestDir.java
    private static String dap4TestServerPropName = "d4ts";
    static public String dap4TestServer = "remotetest.unidata.ucar.edu"; //mutable

    static {
        String d4ts = System.getProperty(dap4TestServerPropName);
        if(d4ts != null && d4ts.length() > 0)
            dap4TestServer = d4ts;
    }

    //////////////////////////////////////////////////

    static class FileSource
    {
        public String ext = null;
        public String tag = null;
        public List<File> files = null;

        public FileSource(String ext, String tag)
        {
            this.ext = ext;
            this.tag = tag;
        }
    }

    //////////////////////////////////////////////////
    // Instance Variables

    protected List<FileSource> activesources;

    protected ServletInfo svcinfo = null;
    protected String root = null; // root path to the displayed files

    //////////////////////////////////////////////////
    // Constructor(s)

    /**
     * @param root the file directory root
     * @throws DapException
     */
    public FrontPage(String root, ServletInfo svcinfo)
            throws DapException
    {
        this.svcinfo = svcinfo;
        this.root = DapUtil.canonicalpath(root);
        // Construct the list of usable files
        activesources = getFileList(root);
    }

    //////////////////////////////////////////////////

    List<FileSource>
    getFileList(String root)
    {
        File dir = new File(root);
        if(!dir.isDirectory()) {
            DapLog.error("FrontPage: specified root directory is not a directory: " + root);
            return null;
        }
        if(!dir.canRead()) {
            DapLog.error("FrontPage: specified root directory is not readable: " + root);
            return null;
        }

        File[] candidates = dir.listFiles();
        Arrays.sort(candidates);
        List<FileSource> activesources = new ArrayList<FileSource>();
        // Capture lists of files for each FileSource
        for(FileSource src : SOURCES) {
            List<File> matches = new ArrayList<File>();
            for(File candidate : candidates) {
                if(!candidate.isFile()) continue;
                String name = candidate.getName();
                if(name == null) continue;
                if(NO_VLEN && name.indexOf("vlen") >= 0)  // temporary
                    continue;
                int dotpos = name.lastIndexOf(".");
                if(!name.endsWith(src.ext))
                    continue;
                if(!candidate.canRead()) {
                    DapLog.info("FrontPage: file not readable: " + candidate);
                    continue;
                }
                matches.add(candidate);
            }
            if(matches.size() > 0) {
                FileSource clone = new FileSource(src.ext, src.tag);
                clone.files = matches;
                activesources.add(clone);
            }
        }
        return activesources;
    }

    String
    buildPage()
            throws DapException
    {
        StringBuilder html = new StringBuilder();
        html.append(HTML_PREFIX);
        html.append(HTML_HEADER1);
        html.append(HTML_HEADER2);

        for(FileSource src : activesources) {
            html.append(String.format(HTML_HEADER3, src.tag));
            html.append(TABLE_HEADER);
            for(File file : src.files) {
                String name = file.getName();
                String absname;
                try {
                    absname = DapUtil.canonicalpath(file.getCanonicalPath());
                } catch (IOException ioe) {
                    throw new DapException(ioe);
                }
                absname = DapUtil.canonicalpath(file.getAbsolutePath());
                if(!absname.startsWith(this.root))
                    throw new DapException("Malformed file name: " + absname);
                String datasetname = DapUtil.denullify(absname.substring(this.root.length()));
                String urlpath = this.svcinfo.getServer() + "/" + this.svcinfo.getServletname() + datasetname; // append remainder not used by mappath
                String line = String.format(HTML_FORMAT, name, urlpath, urlpath, urlpath, urlpath);
                html.append(line);
            }
            html.append(TABLE_FOOTER);
        }
        html.append(HTML_FOOTER);
        return html.toString();
    }

    //////////////////////////////////////////////////
    // HTML prefix and suffix
    // (Remember that java does not allow Strings to cross lines)
    static final String HTML_PREFIX =
            "<html>\n<head>\n<title>DAP4 Test Files</title>\n<meta http-equiv=\"Content-Type\" content=\"text/html\">\n</meta>\n<body bgcolor=\"#FFFFFF\">\n";

    static final String HTML_HEADER1 = "<h1>DAP4 Test Files</h1>\n";
    static final String HTML_HEADER2 = "<h2>http://" + dap4TestServer + "/d4ts/</h2>\n<hr>\n";
    static final String HTML_HEADER3 = "<h3>%s Based Test Files</h3>%n";

    static final String TABLE_HEADER = "<table>\n";
    static final String TABLE_FOOTER = "</table>\n";

    static final String HTML_FOOTER = "<hr>\n</html>\n";

    static final String HTML_FORMAT =
            "<tr>%n"
                    + "<td halign='right'><b>%s:</b></td>%n"
                    + "<td halign='center'><a href='%s.dmr.txt'> DMR (TEXT) </a></div></td>%n"
                    + "<td halign='center'><a href='%s.dmr'> DMR (XML) </a></div></td>%n"
                    + "<td halign='center'><a href='%s.dap'> DAP </a></div></td>%n"
                    + "<td halign='center'><a href='%s.dsr'> DSR </a></div></td>%n"
                    + "</tr>%n";
}



