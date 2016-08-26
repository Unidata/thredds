/* Copyright 2016, University Corporation for Atmospheric Research
   See the LICENSE.txt file for more information.
*/

package thredds.server.reify;

import org.junit.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPUtil;
import ucar.unidata.util.test.UnitTestCommon;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract public class TestReify extends UnitTestCommon
{
    static protected final boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static protected final String THREDDSPREFIX = "/thredds";
    static protected final String SERVLETPREFIX = "/download";
    static protected final String DOWNLOADDIR = "C:/Temp/download";

    static final protected String STATUSCODEHEADER = "x-download-status";

    //////////////////////////////////////////////////
    // Type Decls

    static abstract class AbstractTestCase
    {
        static public String downloadroot = DOWNLOADDIR;

        //////////////////////////////////////////////////

        protected ReifyUtils.Command cmd;
        protected String url;
        protected String target;
        protected Map<String, String> params = new HashMap<>();

        AbstractTestCase(String cmd, String url, String target, String[] params)
        {

            this.cmd = ReifyUtils.Command.parse(cmd);
            this.params.put("request", this.cmd.name().toLowerCase());

            this.url = url;
            if(this.url != null) this.params.put("url", this.url);

            this.target = HTTPUtil.canonicalpath(target);
            if(this.target != null) this.params.put("target", this.target);

            for(int i = 0; i < params.length; i++) {
                String[] pieces = params[i].trim().split("[=]");
                if(pieces.length == 1)
                    this.params.put(pieces[0].trim().toLowerCase(), "");
                else
                    this.params.put(pieces[0].trim().toLowerCase(), pieces[1].trim());
            }
        }

        //////////////////////////////////////////////////
        // Subclass defined

        abstract public Map<String, String> getReply();

        //////////////////////////////////////////////////
        // Accessors

        public ReifyUtils.Command getCommand()
        {
            return this.cmd;
        }

        public String getURL()
        {
            return this.url;
        }

        public Map<String, String> getParams()
        {
            return this.params;
        }


        public String toString()
        {
            StringBuilder buf = new StringBuilder();
            buf.append(this.getURL());
            boolean first = true;
            for(Map.Entry<String, String> entry : this.params.entrySet()) {
                buf.append(String.format("%s%s=%s", first ? "?" : "&",
                        entry.getKey(), entry.getValue()));
            }
            return buf.toString();
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected List<AbstractTestCase> alltestcases = new ArrayList<>();

    //////////////////////////////////////////////////

    abstract void defineAllTestCases();

    abstract void doOneTest(AbstractTestCase tc) throws Exception;

    //////////////////////////////////////////////////    

    public void
    doAllTests()
            throws Exception
    {
        Assert.assertTrue("No defined testcases", this.alltestcases.size() > 0);
        for(int i = 0; i < this.alltestcases.size(); i++) {
            doOneTest(this.alltestcases.get(i));
        }
    }

    //////////////////////////////////////////////////
    // Utilities

    public Map<String,String>
    getServerProperties(String server)
    {
        StringBuilder b = new StringBuilder();
        b.append(server);
        b.append("/");
        b.append("?request=inquire&inquire=downloaddir;username");
        int[] codep = new int[1];
        String sresult = null;
        try {
            sresult = callserver(b.toString(), codep);
        } catch (IOException e) {
            System.err.println("Server call failure: " + e.getMessage());
            return null;
        }
        if(codep[0] != 200) {
            System.err.println("Server call failed: status=" + codep[0]);
            return null;
        }
        Map<String, String> result = ReifyUtils.parseMap(sresult, ';', true);
        return result;
    }

    public String
    callserver(String url, int[] codep)
            throws IOException
    {
        // Make method call
        byte[] bytes = null;
        codep[0] = 0;
        try (HTTPMethod method = HTTPFactory.Get(url)) {
            method.execute();
            codep[0] = method.getStatusCode();
            org.apache.http.Header h = method.getResponseHeader(STATUSCODEHEADER);
            if(h != null) {
                String scode = h.getValue();
                int code;
                try {
                    code = Integer.parseInt(scode);
                    if(code > 0)
                        codep[0] = code;
                } catch (NumberFormatException e) {
                    code = 0;
                }
            }
            bytes = method.getResponseAsBytes();
        }
        // Convert to string
        String sbytes = "";
        if(bytes != null && bytes.length > 0)
            sbytes = new String(bytes, "utf8");
        if(codep[0] != 200)
            return sbytes;
        String result = ReifyUtils.urlDecode(sbytes);
        return result;
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
     *
     * @param root delete all files under this root
     * @param deleteroot true => delete root also
     * @return   true if delete suceeded
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

    //////////////////////////////////////////////////
    // Support classes

    static /*package*/ class NullValidator implements Validator
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

}
