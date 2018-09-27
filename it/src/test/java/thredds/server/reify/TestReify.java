/* Copyright 2016, University Corporation for Atmospheric Research
   See the LICENSE.txt file for more information.
*/

package thredds.server.reify;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPUtil;
import ucar.unidata.util.test.UnitTestCommon;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

abstract public class TestReify extends UnitTestCommon
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static protected final boolean DEBUG = false;
    static protected final boolean DEBUGPARTS = false;

    //////////////////////////////////////////////////
    // Constants

    static protected final String DOWNPREFIX = "/download";
    static protected final String UPPREFIX = "/upload";
    static protected String DOWNLOADDIR;
    static protected String UPLOADDIR;

    static final protected String STATUSCODEHEADER = "x-download-status";

    static {
        // Try to locate a temporary directory
        File tmp = new File("C:/Temp");
        if(!tmp.exists() || !tmp.isDirectory() || !tmp.canRead() || !tmp.canWrite())
            tmp = null;
        if(tmp != null) {
            tmp = new File("/tmp");
            if(!tmp.exists() || !tmp.isDirectory() || !tmp.canRead() || !tmp.canWrite())
                tmp = null;
        }
        if(tmp == null)
            tmp = new File(System.getProperty("user.dir"));
        File dload = new File(tmp, "download");
        dload.mkdirs();
        DOWNLOADDIR = HTTPUtil.canonicalpath(dload.getAbsolutePath());
        File uload = new File(tmp, "upload");
        uload.mkdirs();
        UPLOADDIR = HTTPUtil.canonicalpath(uload.getAbsolutePath());
    }

    //////////////////////////////////////////////////
    // Type Decls

    static enum Command
    {
        NONE, UPLOAD, DOWNLOAD, INQUIRE;

        static public Command parse(String cmd)
        {
            if(cmd == null) return null;
            for(Command x : Command.values()) {
                if(cmd.equalsIgnoreCase(x.toString())) return x;
            }
            return null;
        }
    }

    static public class NullValidator implements org.springframework.validation.Validator
    {
        public boolean supports(Class<?> clazz)
        {
            return true;
        }

        public void validate(Object target, Errors errors)
        {
            return;
        }
    }

    static abstract class AbstractTestCase
    {
        public String downloadroot = DOWNLOADDIR;
        public String uploadroot = UPLOADDIR;

        //////////////////////////////////////////////////

        protected String url;
        protected String target;

        AbstractTestCase(String url)
        {
            this.url = url;
        }

        //////////////////////////////////////////////////
        // Subclass defined

        abstract public String toString();

        //////////////////////////////////////////////////
        // Accessors

        public String getURL()
        {
            return this.url;
        }

    }

    //////////////////////////////////////////////////
    // Instance variables

    protected List<AbstractTestCase> alltestcases = new ArrayList<>();

    protected Map<String, String> serverprops = null;

    protected boolean notimplemented = true;

    //////////////////////////////////////////////////

    abstract void defineAllTestCases();

    abstract void doOneTest(AbstractTestCase tc) throws Exception;

    //////////////////////////////////////////////////    

    public void
    doAllTests()
            throws Exception
    {
        if(notimplemented) {
            System.err.println("Server up/download not implemented: tests aborted");
            return;
        }
        Assert.assertTrue("No defined testcases", this.alltestcases.size() > 0);
        for(int i = 0; i < this.alltestcases.size(); i++) {
            doOneTest(this.alltestcases.get(i));
        }
    }

    //////////////////////////////////////////////////
    // Utilities

    public void
    getServerProperties(String server)
            throws Exception
    {
        StringBuilder b = new StringBuilder();
        b.append(server);
        b.append("?request=inquire");
        int code = 0;
        String sresult = null;
        try {
            try (HTTPMethod method = HTTPFactory.Get(b.toString())) {
                code = callserver(method);
                byte[] bytes = method.getResponseAsBytes();
                if(code != 200) {
                    sresult = new String(bytes, "utf8");
                    notimplemented = true;
                    System.err.printf("Server properties call failed: status=%d msg=%s", code, sresult);
                    return;
                }
                // Convert to string
                sresult = "";
                if(bytes != null && bytes.length > 0)
                    sresult = new String(bytes, "utf8");
                sresult = urlDecode(sresult);
                System.err.printf("Getproperties: result=|%s|", sresult);
            }
        } catch (IOException e) {
            System.err.println("Server call failure: " + e.getMessage());
            notimplemented = true;
            return;
        }
        Map<String, String> result = parseMap(sresult, ';', true);
        this.serverprops = result;
        notimplemented = false;
    }

    public int
    callserver(HTTPMethod method)
            throws IOException
    {
        int code = 0;
        // Make method call
        method.execute();
        if(DEBUGPARTS) {
            RequestConfig rc = method.getDebugConfig();
            HttpRequestBase hrb = method.debugRequest();
            Assert.assertTrue("Could not get request config", rc != null);
            reportRequest(rc, hrb);
        }
        code = method.getStatusCode();
        org.apache.http.Header h = method.getResponseHeader(STATUSCODEHEADER);
        if(h != null) {
            String scode = h.getValue();
            try {
                int tmpcode = Integer.parseInt(scode);
                if(tmpcode > 0)
                    code = tmpcode;
            } catch (NumberFormatException e) {
                code = code; // ignore
            }
        }
        return code;
    }

    static public String
    replyCompare(Map<String, String> result, Map<String, String> base)
    {
        StringBuilder b = new StringBuilder();
        // do two ways to catch added plus new
        for(Map.Entry<String, String> entry : result.entrySet()) {
            String basevalue = base.get(entry.getKey());
            if(basevalue == null) {
                b.append(String.format("Added: %s%n",
                        entry.getKey()));
            } else {
                String rvalue = entry.getValue();
                if(!rvalue.equals(basevalue)) {
                    b.append(String.format("Change: %s: %s to %s%n",
                            entry.getKey(), basevalue, rvalue));
                }
            }
        }
        for(Map.Entry<String, String> entry : base.entrySet()) {
            String rvalue = result.get(entry.getKey());
            if(rvalue == null) {
                b.append(String.format("Deleted: %s%n",
                        entry.getKey()));
            }
        }
        return (b.toString().length() > 0 ? b.toString() : null);
    }

    /**
     * @param root       delete all files under this root
     * @param deleteroot true => delete root also
     * @return true if delete suceeded
     */
    static public boolean
    deleteTree(String root, boolean deleteroot)
    {
        if(root == null || root.length() == 0)
            return false;
        File rootfile = new File(root);
        if(!rootfile.exists()) return false;
        if(!deleteTree(rootfile)) return false;
        if(deleteroot && !rootfile.delete()) return false;
        return true;
    }

    static protected boolean
    deleteTree(File root)
    {
        File[] contents = root.listFiles();
        for(File f : contents) {
            if(f.isDirectory() && !deleteTree(f)) return false;
            if(!f.delete()) return false;
        }
        return true;
    }

    static File
    makedir(String name, boolean clear)
            throws IOException
    {
        File dir = new File(name);
        dir.mkdirs(); // ensure existence
        // Change permissions to allow read/write by anyone
        dir.setExecutable(true, false);
        dir.setReadable(true, false);
        dir.setWritable(true, false);
        if(!dir.canRead())
            throw new IOException(name + ": cannot read");
        if(!dir.canWrite())
            throw new IOException(name + ": cannot write");
        // optionally clear out the dir
        if(clear)
            deleteTree(name, false);
        return dir;
    }

    static void
    filereport(String path)
    {
        File tmp = new File(path);
        if(!tmp.exists())
            System.err.println(path + " does not exist");
        if(tmp.isFile())
            System.err.println(path + " is file");
        if(tmp.isDirectory())
            System.err.println(path + " is a directory");
        if(!tmp.canRead())
            System.err.println(path + " not readable");
        if(!tmp.canWrite())
            System.err.println(path + " not writeable");
        if(!tmp.canExecute())
            System.err.println(path + " not executable");
    }

    static public String
    urlDecode(String s)
    {
        try {
            s = URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
        return s;
    }

    static public String
    urlEncode(String s)
    {
        try {
            s = URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
        return s;
    }

    static public Map<String, String>
    parseMap(String params, char sep, boolean decode)
    {
        Map<String, String> map = new HashMap<>();
        if(params == null || params.length() == 0)
            return map;
        String[] pieces = params.split("[&]");
        for(int i = 0; i < pieces.length; i++) {
            String piece = pieces[i].trim();
            String[] pair = piece.split("[=]");
            String key = pair[0].trim();
            if(pair.length >= 2) {
                String v = pair[1].trim();
                if(decode) v = urlDecode(v);
                map.put(key, v);
            } else if(pair.length == 1) {
                map.put(key, "");
            } else
                assert false : "split() failed";
        }
        return map;
    }

    static public String
    mapToString(Map<String, String> map, boolean encode, String... order)
    {
        List<String> orderlist;
        if(order == null)
            orderlist = new ArrayList<String>(map.keySet());
        else
            orderlist = Arrays.asList(order);
        StringBuilder b = new StringBuilder();
        // Make two passes: one from order, and one from remainder
        boolean first = true;
        for(int i = 0; i < orderlist.size(); i++) {
            String key = orderlist.get(i);
            String value = map.get(key);
            if(value == null) continue; // ignore
            if(!first) b.append("&");
            b.append(key);
            b.append("=");
            b.append(encode ? urlEncode(value) : value);
            first = false;
        }
        for(Map.Entry<String, String> entry : map.entrySet()) {
            if(orderlist.contains(entry.getKey())) continue;
            if(!first) b.append("&");
            b.append(entry.getKey());
            b.append("=");
            b.append(encode ? urlEncode(entry.getValue()) : entry.getValue());
            first = false;
        }
        return b.toString();
    }


    static protected void
    reportRequest(RequestConfig cfg, HttpRequestBase req)
    {
        System.err.println("========= TestSide =========\n");
        System.err.println("Headers:\n");
        for(Header h : req.getAllHeaders()) {
            System.err.printf("\t%s = %s%n", h.getName(), h.getValue());
        }
        if("post".equalsIgnoreCase(req.getMethod())) {
            HttpEntityEnclosingRequestBase b = (HttpEntityEnclosingRequestBase) req;
            HttpEntity he = b.getEntity();
            List<NameValuePair> content = null;
            try {
                String s = EntityUtils.toString(he);
                System.err.println(s);
            } catch (IOException e) {
                return;
            }
        }
        System.err.println("=========\n");
        System.err.flush();
    }

    static List<org.apache.http.Header>
    iter2list(Iterator<Object> e)
    {
        List<org.apache.http.Header> result = new ArrayList<>();
        while(e.hasNext()) {
            Header h = (Header) e.next();
            result.add(h);
        }
        return result;
    }

    static List<String>
    enum2list(Enumeration<String> e)
    {
        List<String> names = new ArrayList<>();
        while(e.hasMoreElements()) {
            String name = e.nextElement();
            names.add(name);
        }
        return names;
    }


}
