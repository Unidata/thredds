/* Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
   See the LICENCE file for more information. */

package dap4.d4ts;

import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.dap4lib.DapLog;
import dap4.servlet.DapRequest;

import java.io.*;
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

    static final protected boolean NO_VLEN = true; // ignore vlen datasets for now

    static final protected String[] expatterns =
            (NO_VLEN ? new String[0]
                    : new String[]{"vlen"});

    // Define the file sources of interest
    static final protected FileSource[] SOURCES = new FileSource[]{
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

    protected DapRequest drq = null;
    protected String root = null; // root path to the displayed files

    //////////////////////////////////////////////////
    // Constructor(s)

    /**
     * @param root the file directory root
     * @throws DapException
     */
    public FrontPage(String root, DapRequest req)
            throws DapException
    {
        this.drq = req;
        this.root = DapUtil.canonicalpath(root);
        // Construct the list of usable files
        activesources = getFileList(this.root);
    }

    //////////////////////////////////////////////////

    List<FileSource>
    getFileList(String rootname)
    {

        File root = new File(rootname);
        if(!root.isDirectory()) {
            DapLog.error("FrontPage: specified root directory is not a directory: " + root);
            return null;
        }
        if(!root.canRead()) {
            DapLog.error("FrontPage: specified root directory is not readable: " + root);
            return null;
        }

        // take files from set of files immediately under root
        File[] candidates = root.listFiles();
        List<FileSource> activesources = new ArrayList<FileSource>();
        // Capture lists of files for each FileSource
        for(FileSource src : SOURCES) {
            List<File> matches = new ArrayList<File>();
            for(File candidate : candidates) {
                String name = candidate.getName();
                boolean excluded = false;
                for(String exclude : expatterns) {
                    if(name.indexOf(exclude) >= 0) {
                        excluded = true;
                        break;
                    }
                }
                if(excluded) continue;
                if(!name.endsWith(src.ext)) continue;
                if(false && !candidate.canRead()) {
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
        try {
            for(FileSource src : activesources) {
                html.append(String.format(HTML_HEADER3, src.tag));
                html.append(TABLE_HEADER);
                for(File file : src.files) {
                    String name = file.getName();
                    String absname;
                    absname = DapUtil.canonicalpath(file.getAbsolutePath());
                    String datasetname = DapUtil.denullify(absname.substring(this.root.length()));
                    String urlpath = this.drq.getControllerPath() + datasetname; // append remainder not used by mappath
                    String line = String.format(HTML_FORMAT,
                            name,
                            urlpath,
                            urlpath,
                            urlpath,
                            urlpath,
                            urlpath
                    );
                    html.append(line);
                }
                html.append(TABLE_FOOTER);
            }
        } catch (Exception e) {
            sendtrace(drq, e);
        }
        html.append(HTML_FOOTER);
        return html.toString();
    }

    static public void
    sendtrace(DapRequest drq, Exception e)
    {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.close();
            sw.close();
            sendtext(drq, sw.toString());
        } catch (IOException ioe) {
            assert false;
        }
    }

    static public void
    sendtext(DapRequest drq, String s)
    {
        try {
            //s = s.replace("<", "&lt;");
            //s = s.replace(">", "&gt;");
            //s = s + "\n";
            s = "+++++++++\n" + s;
            s = s + "+++++++++\n";
            byte[] bytes = DapUtil.extract(DapUtil.UTF8.encode(s));
            OutputStream out = drq.getOutputStream();
            out.write(bytes);
            out.flush();
        } catch (IOException ioe) {
            assert false;
        }
    }

    /**
     * Get a flattened list of all files immediately
     * under a given root with given extension(s).
     */

    /*static List<File>
    collectFiles(File dir)
    {
        List<File> result = new ArrayList<File>();
        if(!dir.isDirectory())
                DapLog.info("FrontPage: specified root is not a directory: " + dir);
        if(!dir.canRead())
            DapLog.info("FrontPage: specified root directory is not readable: " + dir);

        File[] contents = dir.listFiles();
        Arrays.sort(contents);
        for(File f : contents) {
            if(f.isDirectory()) {
                List<File> subfiles = walkDir(f);
                result.addAll(subfiles);
            } else {
                result.add(f);
            }
        }
        return result;
    }*/

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
                    + "<td halign='center'><a href='%s.dmr.xml'> DMR (XML) </a></div></td>%n"
                    + "<td halign='center'><a href='%s.dap'> DAP </a></div></td>%n"
                    + "<td halign='center'><a href='%s.dap.txt'> DAP (TEXT) </a></div></td>%n"
                    + "<td halign='center'><a href='%s.dsr'> DSR </a></div></td>%n"
                    + "</tr>%n";
}



