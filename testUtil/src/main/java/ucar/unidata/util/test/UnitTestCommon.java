/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package ucar.unidata.util.test;

import org.junit.rules.TemporaryFolder;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract public class UnitTestCommon
{
    //////////////////////////////////////////////////
    // Static Constants

    static public boolean LOGSTDIO = System.getProperty("intellij") == null;

    static public final boolean DEBUG = false;

    static public final Charset UTF8 = Charset.forName("UTF-8");

    static protected final int[] OKCODES = new int[]{200, 404};

    protected static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UnitTestCommon.class);

    // Look for these to verify we have found the thredds root
    static final String[] DEFAULTSUBDIRS = new String[]{"httpservices", "cdm", "tds", "opendap", "dap4"};

    // NetcdfDataset enhancement to use: need only coord systems
    static final Set<NetcdfDataset.Enhance> ENHANCEMENT = EnumSet.of(NetcdfDataset.Enhance.CoordSystems);

    static protected String threddsroot = null;
    static protected String threddsServer = null;

    static {
        // Compute the root path
        threddsroot = locateThreddsRoot();
        assert threddsroot != null : "Cannot locate /thredds parent dir";
        threddsServer = TestDir.remoteTestServer;
        if(DEBUG)
            System.err.println("UnitTestCommon: threddsServer=" + threddsServer);
    }

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

        // It appears that under Jenkins, the Java property "user.dir" is
        // set incorrectly for our purposes. In this case, we want
        // to use the WORKSPACE environment variable set by Jenkins.
        String workspace = System.getenv("WORKSPACE");
        System.err.println("WORKSPACE=" + (workspace == null ? "null" : workspace));
        System.err.flush();

        String userdir = System.getProperty("user.dir");

        String path = (workspace != null ? workspace : userdir); // Pick one

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
    // Static classes

    /**
     * Provide an interface that allows for arbitrary modification
     * of text before is is passed to compare().
     */
    static public interface Modifier
    {
        public String modify(String text);
    }

    /**
     * Instance of Modifier specialized to delete lines matching
     * a given Java regular expression
     * of text before is is passed to compare().
     * A Line is defined by text.split("[\n]").
     */
    static public class ModDelete implements Modifier
    {
        protected Pattern pattern = null;

        public ModDelete(String regexp)
        {
            this.pattern = Pattern.compile(regexp);

        }

        public String modify(String text)
        {
            String[] lines = text.split("[\n]");
            StringBuilder result = new StringBuilder();
            for(int i = 0; i < lines.length; i++) {
                String line = lines[i];
                Matcher m = this.pattern.matcher(line);
                if(m.matches()) {
                    result.append(line);
                    result.append("\n");
                }
            }
            return result.toString();
        }
    }

    /**
     * Instance of Modifier specialized to delete named attributes.
     */
    static public class ModSuppress implements Modifier
    {
        protected List<Pattern> patterns = new ArrayList<>();

        public ModSuppress()
        {
        }

        public void
        suppress(String attributename)
        {
            String re = String.format("<Attribute[ ]+name=\"%s\".*</Attribute>[^\n]\n",
                    attributename);
            Pattern pattern = Pattern.compile(re);
            patterns.add(pattern);
        }

        public String modify(String text)
        {
            StringBuilder result = new StringBuilder(text);
            for(Pattern p : patterns) {
                for(; ; ) {
                    Matcher m = p.matcher(result.toString());
                    if(!m.matches()) break;
                    int pos0 = m.start();
                    int pos1 = m.end();
                    result.delete(pos0, pos1);
                }
            }
            return result.toString();
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
    protected boolean prop_display = false;

    protected String title = "Testing";
    protected String name = "testcommon";

    //////////////////////////////////////////////////
    // Constructor(s)

    public UnitTestCommon()
    {
        this("Testing");
    }

    public UnitTestCommon(String name)
    {
        this.title = name;
        setSystemProperties();
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
        if(System.getProperty("hasdisplay") != null)
            prop_display = true;
        if(prop_baseline && prop_diff)
            prop_diff = false;
        prop_controls = System.getProperty("controls", "");
    }

    //////////////////////////////////////////////////
    // Accessor

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return this.title;
    }

    public String getThreddsroot()
    {
        return this.threddsroot;
    }

    public String getName()
    {
        return this.name;
    }

    public String
    getResourceDir()
    {
        throw new UnsupportedOperationException();
    }

    //////////////////////////////////////////////////
    // Instance Utilities

    public void
    visual(String header, String captured)
    {
        visual(header, captured, '-');
    }

    public void
    visual(String header, String captured, char marker)
    {
        if(!captured.endsWith("\n"))
            captured = captured + "\n";
        // Dump the output for visual comparison
        System.err.println("Testing " + getName() + ": " + header + ":");
        StringBuilder sep = new StringBuilder();
        for(int i = 0; i < 10; i++) {
            sep.append(marker);
        }
        System.err.println(sep.toString());
        System.err.println("Testing " + title + ": " + header + ":");
        System.err.println("===============");
        System.err.println(captured);
        System.err.println(sep.toString());
        System.err.println("===============");
    }

    static public String
    compare(String tag, String baseline, String testresult)
    {
        // Check for empty testresult
        if(testresult.trim().length() == 0)
            return ">>>> EMPTY TEST RESULT";
        try {
            // Diff the two print results
            Diff diff = new Diff(tag);
            StringWriter sw = new StringWriter();
            boolean pass = !diff.doDiff(baseline, testresult, sw);
            return (pass ? null : sw.toString());
        } catch (Exception e) {
            System.err.println("UnitTest: Diff failure: " + e);
            return null;
        }
    }

    static public boolean
    same(String tag, String baseline, String testresult)
    {
        String result = compare(tag, baseline, testresult);
        if(result == null) {
            System.err.println("Files are Identical");
            return true;
        } else {
            System.err.println(result);
            return false;
        }
    }

    static public boolean
    similar(String tag, String baseline, String testresult, Modifier mbaseline, Modifier mtest)
    {
        String baselinemod = mbaseline.modify(baseline);
        String testresultmod = mtest.modify(testresult);
        String result = compare(tag, baselinemod, testresultmod);
        if(result == null) {
            System.err.println("Files are Similar");
            return true;
        } else {
            System.err.println(result);
            return false;
        }
    }

    protected boolean
    checkServer(String candidate)
    {
        if(candidate == null) return false;
        // ping to see if we get a response
        System.err.print("Checking for sourceurl: " + candidate);
        try {
            try (HTTPMethod method = HTTPFactory.Get(candidate)) {
                method.execute();
                String s = method.getResponseAsString();
                System.err.println(" ; found");
                return true;
            }
        } catch (IOException ie) {
            System.err.println(" ; fail");
            return false;
        }
    }

    protected void
    bindstd()
    {
        if(LOGSTDIO)
        SysStreamLogger.bindSystemStreams();
    }

    protected void
    unbindstd()
    {
        if(LOGSTDIO)
        SysStreamLogger.unbindSystemStreams();
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
        FileInputStream stream = new FileInputStream(filename);
        byte[] result = readbinaryfile(stream);
        stream.close();
        return result;
    }

    static public byte[]
    readbinaryfile(InputStream stream)
            throws IOException
    {
        // Extract the stream into a bytebuffer
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] tmp = new byte[1 << 16];
        for(; ; ) {
            int cnt;
            cnt = stream.read(tmp);
            if(cnt <= 0) break;
            bytes.write(tmp, 0, cnt);
        }
        return bytes.toByteArray();
    }

    // Properly access a dataset
    static public NetcdfDataset openDataset(String url)
            throws IOException
    {
        DatasetUrl durl = DatasetUrl.findDatasetUrl(url);
        return NetcdfDataset.acquireDataset(null, durl, ENHANCEMENT, -1, null, null);
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

    static public String canonjoin2(String prefix, String suffix)
    {
        if(prefix == null) prefix = "";
        if(suffix == null) suffix = "";
        StringBuilder result = new StringBuilder(prefix);
        if(!prefix.endsWith("/"))
            result.append("/");
        result.append(suffix.startsWith("/") ? suffix.substring(1) : suffix);
        return result.toString();
    }

    static public String canonjoin(String... pieces)
    {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < pieces.length; i++) {
            // invariant buf does not end with ('/')
            String piece = pieces[i];
            if(piece == null) continue;
            piece = canonicalpath(piece);
            if(i == 0)
                buf.append(piece);
            else {//i>=0
                if(!piece.startsWith("/"))
                    buf.append("/");
                buf.append(piece);
            }
        }
        return buf.toString();
    }

    /**
     * Convert path to:
     * 1. use '/' consistently
     * 2. remove any trailing '/'
     * 3. trim blanks
     *
     * @param path convert this path
     * @return canonicalized version
     */
    static public String
    canonicalpath(String path)
    {
        if(path == null) return null;
        path = path.trim();
        path = path.replace('\\', '/');
        if(path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        // As a last step, lowercase the drive letter, if any
        if(hasDriveLetter(path))
            path = path.substring(0, 1).toLowerCase() + path.substring(1);
        return path;
    }

    /**
     * return true if this path appears to start with a windows drive letter
     *
     * @param path
     * @return
     */

    static public boolean
    hasDriveLetter(String path)
    {
        if(path != null && path.length() >= 2) {
            return (DRIVELETTERS.indexOf(path.charAt(0)) >= 0 && path.charAt(1) == ':');
        }
        return false;
    }

    static final public String DRIVELETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toLowerCase();


    static public String
    extractDatasetname(String urlorpath, String suffix)
    {
        try {
            URI x = new URI(urlorpath);
            StringBuilder path = new StringBuilder(x.getPath());
            int index = path.lastIndexOf("/");
            if(index < 0) index = 0;
            if(index > 0) path.delete(0, index + 1);
            if(suffix != null) {
                path.append('.');
                path.append(suffix);
            }
            return path.toString();
        } catch (URISyntaxException e) {
            assert (false);
        }
        return null;
    }

    static protected String
    ncdumpmetadata(NetcdfFile ncfile, String datasetname)
            throws Exception
    {
        StringWriter sw = new StringWriter();
        StringBuilder args = new StringBuilder("-strict");
        if(datasetname != null) {
            args.append(" -datasetname ");
            args.append(datasetname);
        }
        // Print the meta-databuffer using these args to NcdumpW
        try {
            if(!ucar.nc2.NCdumpW.print(ncfile, args.toString(), sw, null))
                throw new Exception("NcdumpW failed");
        } catch (IOException ioe) {
            throw new Exception("NcdumpW failed", ioe);
        }
        sw.close();
        return sw.toString();
    }

    static protected String
    ncdumpdata(NetcdfFile ncfile, String datasetname)
            throws Exception
    {
        StringBuilder args = new StringBuilder("-strict -vall");
        if(datasetname != null) {
            args.append(" -datasetname ");
            args.append(datasetname);
        }
        // Dump the databuffer
        StringWriter sw = new StringWriter();
        try {
            if(!ucar.nc2.NCdumpW.print(ncfile, args.toString(), sw, null))
                throw new Exception("NCdumpW failed");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new Exception("NCdumpW failed", ioe);
        }
        sw.close();
        return sw.toString();
    }

    /**
     * @param prefix  - string to prefix all command line options: typically "--"
     * @param options - list of option names of interest
     * @return specified properties converted to command line form
     */
    static public String[]
    propertiesToArgs(String prefix, String... options)
    {
        if(options == null || options.length == 0)
            throw new IllegalArgumentException("No options specified");
        if(prefix == null) prefix = "--";
        List<String> args = new ArrayList<>();
        Set<String> defined = System.getProperties().stringPropertyNames();
        for(String key : options) {
            if(!defined.contains(key)) continue; //not defined
            String value = System.getProperty(key);
            args.add(prefix + key);
            if(value != null)
                args.add(value);
        }
        return args.toArray(new String[args.size()]);
    }

    static protected boolean
    check(int code)
    {
        return check(code, OKCODES);
    }

    static protected boolean
    check(int code, int[] ok)
    {
        for(int okcode : ok) {
            if(okcode == code) return true;
        }
        return false;
    }

    /*
    // Replacement for stderr & stdout
    static public class STDIO
    {
        public STDIO(String name)
        {
        }

        public void
        printf(String format, Object... args)
        {
            System.err.println(String.format(format, args));
        }

        public void
        println(String msg)
        {
            printf("%s%n", msg);
        }

        public void
        print(String msg)
        {
            printf("%s", msg);
        }

        public void
        flush()
        {
        }
    }

    static public STDIO stderr = new STDIO("test");
    static public STDIO stdout = new STDIO("test");

    static TemporaryFolder temporaryfolder = null;

    static public File
    makeTemporaryDir(String name)
            throws IOException
    {
        if(temporaryfolder == null)
            temporaryfolder = new TemporaryFolder();
        return temporaryfolder.newFolder(name);
    }

    static public File
    makeTemporaryFile(String name)
            throws IOException
    {
        if(temporaryfolder == null)
            temporaryfolder = new TemporaryFolder();
        return temporaryfolder.newFile(name);
    }
    static public void
    logify(String s)
    {
        StringReader rs = new StringReader(s);
        BufferedReader r = new BufferedReader(rs);
        String line = null;
        for(; ; ) {
            try {
                line = r.readLine();
            } catch (IOException ioe) {
                break;
            }
            if(line == null) break;
            System.err.println(line);
        }
    }
    */
}

