/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.test.util;

import dap4.core.util.DapException;
import junit.framework.TestCase;
import dap4.core.util.DapUtil;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.test.util.TestDir;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class DapTestCommon extends TestCase
{
    //////////////////////////////////////////////////
    // Constants

    static final boolean DEBUG = false;

    static protected final Charset UTF8 = Charset.forName("UTF-8");

    static final String DEFAULTTREEROOT = "dap4";
    // Look for these to verify we have found the thredds root
    static final String[] DEFAULTSUBDIRS = new String[]{"httpservices", "cdm", "tds", "opendap", "dap4"};

    static public final String FILESERVER = "file://localhost:8080";

    // NetcdfDataset enhancement to use: need only coord systems
    static Set<NetcdfDataset.Enhance> ENHANCEMENT = EnumSet.of(NetcdfDataset.Enhance.CoordSystems);

    static public final String CONSTRAINTTAG = "dap4.ce";

    // Equivalent to the path to the webapp/d4ts for testing purposes
    static protected final String PSEUDOWEBAPPPATH = "/d4tests/src/test/data";
    static protected final String RESOURCEPATH = "/resources";

    //////////////////////////////////////////////////
    // Static Variables

    static public org.slf4j.Logger log;

    //////////////////////////////////////////////////
    // Static methods

    // Walk around the directory structure to locate
    // the path to the thredds root (which may not
    // be names "thredds").
    // Same as code in UnitTestCommon, but for
    // some reason, Intellij will not let me import it.

    static String
    locateThreddsRoot()
    {
        // Walk up the user.dir path looking for a node that has
        // all the directories in SUBROOTS.

        String path = System.getProperty("user.dir");

        // clean up the path
        path = path.replace('\\', '/'); // only use forward slash
        assert (path != null);
        if(path.endsWith("/")) path = path.substring(0, path.length() - 1);

        File prefix = new File(path);
        for(; prefix != null; prefix = prefix.getParentFile()) {//walk up the tree
            int found = 0;
            String[] subdirs = prefix.list();
            for(String dirname : subdirs) {
                for(String want : DEFAULTSUBDIRS) {
                    if(dirname.equals(want)) {
                        found++;
                        break;
                    }
                }
            }
            if(found == DEFAULTSUBDIRS.length) try {// Assume this is it
                String root = prefix.getCanonicalPath();
                // clean up the root path
                root = root.replace('\\', '/'); // only use forward slash
                return root;
            } catch (IOException ioe) {
            }
        }
        return null;
    }

    static String
    locateDAP4Root(String threddsroot)
    {
        String root = threddsroot;
        if(root != null)
            root = root + "/" + DEFAULTTREEROOT;
        // See if it exists
        File f = new File(root);
        if(!f.exists() || !f.isDirectory())
            root = null;
        return root;
    }

    static protected String
    rebuildpath(String[] pieces, int last)
    {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i <= last; i++) {
            buf.append("/");
            buf.append(pieces[i]);
        }
        return buf.toString();
    }

    static public void
    clearDir(File dir, boolean clearsubdirs)
    {
        // wipe out the dir contents
        if(!dir.exists()) return;
        for(File f : dir.listFiles()) {
            if(f.isDirectory()) {
                if(clearsubdirs) {
                    clearDir(f, true); // clear subdirs
                    f.delete();
                }
            } else
                f.delete();
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    // System properties
    protected boolean prop_ascii = true;
    protected boolean prop_diff = true;
    protected boolean prop_baseline = false;
    protected boolean prop_visual = false;
    protected boolean prop_debug = DEBUG;
    protected boolean prop_generate = true;
    protected String prop_controls = null;

    // Define a tree pattern to recognize the root.
    protected String threddsroot = null;
    protected String dap4root = null;
    protected String d4tsServer = null;
    protected String webapproot = null;
    protected String resourcedir = null;

    protected String title = "Testing";

    public DapTestCommon()
    {
        this("dapTest");
    }

    public DapTestCommon(String name)
    {
        super(name);
        this.title = name;
        setSystemProperties();
        initPaths();
    }

    protected void
    initPaths()
    {
        // Compute the root path
        this.threddsroot = locateThreddsRoot();
        if(this.threddsroot == null)
            System.err.println("Cannot locate /thredds parent dir");
        this.dap4root = locateDAP4Root(this.threddsroot);
        if(this.dap4root == null)
            System.err.println("Cannot locate /dap4 parent dir");
        this.webapproot = this.dap4root + PSEUDOWEBAPPPATH;
        this.resourcedir = this.webapproot + RESOURCEPATH;
        // Compute the set of SOURCES
        this.d4tsServer = TestDir.dap4TestServer;
        if(DEBUG)
            System.err.println("DapTestCommon: d4tsServer=" + d4tsServer);
    }

    /**
     * Try to get the system properties
     */
    protected void setSystemProperties()
    {
        if(System.getProperty("nodiff") != null)
            prop_diff = false;
        if(System.getProperty("baseline") != null)
            prop_baseline = true;
        if(System.getProperty("nogenerate") != null)
            prop_generate = false;
        if(System.getProperty("debug") != null)
            prop_debug = true;
        if(System.getProperty("visual") != null)
            prop_visual = true;
        if(System.getProperty("ascii") != null)
            prop_ascii = true;
        if(System.getProperty("utf8") != null)
            prop_ascii = false;
        if(prop_baseline && prop_diff)
            prop_diff = false;
        prop_controls = System.getProperty("controls", "");
    }

    //////////////////////////////////////////////////
    // Accessor

    public String getDAP4Root()
    {
        return this.dap4root;
    }

    public String getResourceDir()
    {
        return this.resourcedir;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return this.title;
    }

    //////////////////////////////////////////////////
    // Instance Utilities

    public void
    visual(String header, String captured)
    {
        if(!captured.endsWith("\n"))
            captured = captured + "\n";
        // Dump the output for visual comparison
        System.out.println("Testing " + getName() + ": " + header + ":");
        System.out.println("---------------");
        System.out.print(captured);
        System.out.println("---------------");
    }

    public boolean

    compare(String baselinecontent, String testresult)
            throws Exception
    {
        StringReader baserdr = new StringReader(baselinecontent);
        StringReader resultrdr = new StringReader(testresult);
        // Diff the two files
        Diff diff = new Diff("Testing " + getTitle());
        boolean pass = !diff.doDiff(baserdr, resultrdr);
        baserdr.close();
        resultrdr.close();
        return pass;
    }

    protected void
    findServer(String path)
            throws DapException
    {
        if(d4tsServer.startsWith("file:")) {
            d4tsServer = FILESERVER + "/" + path;
        } else {
            String svc = "http://" + d4tsServer + "/d4ts";
            if(!checkServer(svc))
                throw new DapException("D4TS Server not reachable: " + svc);
            // Since we will be accessing it thru NetcdfDataset, we need to change the schema.
            d4tsServer = "dap4://" + d4tsServer + "/d4ts";
        }
    }

    protected boolean
    checkServer(String candidate)
    {
        if(candidate == null) return false;
/* requires httpclient4
        int savecount = HTTPSession.getRetryCount();
        HTTPSession.setRetryCount(1);
*/
        // See if the sourceurl is available by trying to get the DSR
        System.err.print("Checking for sourceurl: " + candidate);
        try {
            HTTPSession session = new HTTPSession(candidate);
            HTTPMethod method = HTTPFactory.Get(session);
            method.execute();
            String s = method.getResponseAsString();
            session.close();
            System.err.println(" ; found");
            return true;
        } catch (IOException ie) {
            System.err.println(" ; fail");
            return false;
        } finally {
// requires httpclient4            HTTPSession.setRetryCount(savecount);
        }
    }

    //////////////////////////////////////////////////
    // Static utilities

    // Copy result into the a specified dir
    static public void
    writefile(String path, String content)
            throws IOException
    {
        File f = new File(path);
        if(f.exists()) f.delete();
        FileWriter out = new FileWriter(f);
        out.write(content);
        out.close();
    }

    // Copy result into the a specified dir
    static public void
    writefile(String path, byte[] content)
            throws IOException
    {
        File f = new File(path);
        if(f.exists()) f.delete();
        FileOutputStream out = new FileOutputStream(f);
        out.write(content);
        out.close();
    }

    static public String
    readfile(String filename)
            throws IOException
    {
        StringBuilder buf = new StringBuilder();
        File xx = new File(filename);
        if(!xx.canRead()) {
            int x = 0;
        }
        FileReader file = new FileReader(filename);
        BufferedReader rdr = new BufferedReader(file);
        String line;
        while((line = rdr.readLine()) != null) {
            if(line.startsWith("#")) continue;
            buf.append(line + "\n");
        }
        return buf.toString();
    }

    static public byte[]
    readbinaryfile(String filename)
            throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        FileInputStream file = new FileInputStream(filename);
        return DapUtil.readbinaryfile(file);
    }

    // Properly access a dataset
    static public NetcdfDataset openDataset(String url)
            throws IOException
    {
        return NetcdfDataset.acquireDataset(null, url, ENHANCEMENT, -1, null, null);
    }

    // Fix up a filename reference in a string
    static public String shortenFileName(String text, String filename)
    {
        // In order to achieve diff consistentcy, we need to
        // modify the output to change "netcdf .../file.nc {...}"
        // to "netcdf file.nc {...}"
        String fixed = filename.replace('\\', '/');
        String shortname = filename;
        if(fixed.lastIndexOf('/') >= 0)
            shortname = filename.substring(fixed.lastIndexOf('/') + 1, filename.length());
        text = text.replaceAll(filename, shortname);
        return text;
    }

    static public void
    tag(String t)
    {
        System.err.println(t);
        System.err.flush();
    }

}

