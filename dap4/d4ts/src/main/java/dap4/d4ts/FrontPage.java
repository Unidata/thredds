/* Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
   See the LICENCE file for more information. */

package dap4.d4ts;

import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.dap4lib.DapLog;
import dap4.servlet.DapRequest;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Given a directory, return a front page of HTML
 * that lists all of the files in that page.
 *
 * @author Dennis Heimbigner
 */

public class FrontPage
{

    static final boolean DUMPFILELIST = false;

    //////////////////////////////////////////////////
    // Constants

    static final protected boolean NO_VLEN = false; // ignore vlen datasets for now

    static final protected String[] expatterns = new String[0];

    // Define the file sources of interest
    static final protected FileSource[] SOURCES = new FileSource[]{
            new FileSource(".nc", "netCDF"),
            new FileSource(".hdf5", "HDF5"),
            new FileSource(".dap", "Raw Protocol Output"),
            new FileSource(".syn", "Synthetic")
    };

    static public class Root
    {
        public String prefix;
        public String dir;
        public List<FileSource> files;

        public String toString()
        {
            return String.format("{'%s/%s'}", this.prefix,this.dir);
        }

        public Root(String dir, String prefix)
        {
            this.dir = dir;
            this.prefix = DapUtil.canonicalpath(prefix);
        }

        public String getFullPath() {return DapUtil.canonjoin(this.prefix,this.dir);}

        public void setFiles(List<FileSource> files)
        {
            this.files = files;
        }
    }

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

    protected DapRequest drq = null;
    protected List<Root> roots = null; // root paths to the displayed files

    //////////////////////////////////////////////////
    // Constructor(s)

    /**
     * @param rootinfo the file directory roots
     * @throws DapException
     */
    public FrontPage(List<Root> rootinfo, DapRequest req)
            throws DapException
    {
        this.drq = req;
        this.roots = rootinfo;
        for(Root root : this.roots) {
            // Construct the list of usable files
            buildFileList(root);
        }

    }
    //////////////////////////////////////////////////

    protected void
    buildFileList(Root rootinfo)
            throws DapException
    {
        File root = new File(rootinfo.getFullPath());
        if(!root.isDirectory())
            throw new DapException("FrontPage: specified root directory is not a directory: " + rootinfo.getFullPath());
        if(!root.canRead())
            throw new DapException("FrontPage: specified root directory is not readable: " + rootinfo.getFullPath());

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
                if(!candidate.canRead()) {
                    DapLog.info("FrontPage: file not readable: " + candidate);
                    continue;
                }
                matches.add(candidate);
            }
            if(matches.size() > 0) {
                // Sort the set of files
                matches.sort(new Comparator<File>() {
                    public int compare(File f1, File f2) {
                         return f1.getName().compareTo(f2.getName());
                    }});
                if(DUMPFILELIST) {
                    for(File x : matches) {
                        System.err.printf("file: %s/%s%n", rootinfo.prefix, x.getName());
                    }
                }
                FileSource clone = new FileSource(src.ext, src.tag);
                clone.files = matches;
                activesources.add(clone);
            }
        }
        rootinfo.setFiles(activesources);
    }

    protected String
    buildPage()
            throws DapException
    {
        StringBuilder html = new StringBuilder();
        html.append(HTML_PREFIX);
        html.append(HTML_HEADER1);

        StringBuilder rootnames = new StringBuilder();
        for(Root root : this.roots) {
            if(rootnames.length() > 0)
                rootnames.append(",");
           rootnames.append(root.dir);
        }
        html.append(String.format(HTML_HEADER2,rootnames));

        for(Root root : this.roots) {
            try {
                for(FileSource src : root.files) {
                    html.append(String.format(HTML_HEADER3, src.tag));
                    html.append(TABLE_HEADER);
                    for(File file : src.files) {
                        String name = file.getName();
                        StringBuilder buf = new StringBuilder();
                        buf.append(this.drq.getControllerPath());
                        buf.append('/');
                        buf.append(DapUtil.canonicalpath(root.dir));
                        buf.append('/');
                        buf.append(DapUtil.canonicalpath(name));
                        String urlpath = buf.toString();
                        String line = String.format(HTML_FORMAT,
                                name,
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
    static final String HTML_HEADER2 = "<h2>http://" + dap4TestServer + "/d4ts/{%s}</h2>%n<hr>%n";
    static final String HTML_HEADER3 = "<h3>%s Based Test Files</h3>%n";

    static final String TABLE_HEADER = "<table>\n";
    static final String TABLE_FOOTER = "</table>\n";

    static final String HTML_FOOTER = "<hr>\n</html>\n";

    static final String HTML_FORMAT =
            "<tr>%n"
                    + "<td halign='right'><b>%s:</b></td>%n"
                    + "<td halign='center'><a href='%s.dmr.xml'> DMR.XML </a></div></td>%n"
                    + "<td halign='center'><a href='%s.dap'> DAP </a></div></td>%n"
                    + "<td halign='center'><a href='%s.dsr.xml'> DSR.XML </a></div></td>%n"
                    + "</tr>%n";
}



