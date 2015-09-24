/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.util.net;

import org.apache.http.HttpEntity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ucar.httpservices.*;
import ucar.nc2.util.UnitTestCommon;
import ucar.unidata.test.util.ExternalServer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test HttpFormBuilder
 */

public class TestFormBuilder extends UnitTestCommon
{

    //////////////////////////////////////////////////
    // Constants

    static final boolean DEBUG = false;

    static protected final String TESTURL = "http://echo.httpkit.com";

    static protected final String FAKEBOUNDARY = "XXXXXXXXXXXXXXXXXXXX";

    // Field values to use
    static final String DESCRIPTIONENTRY = "TestFormBuilder";
    static final String NAMEENTRY = "Mr. Jones";
    static final String EMAILENTRY = "idv@ucar.edu";
    static final String ORGENTRY = "UCAR";
    static final String SUBJECTENTRY = "hello";
    static final String SOFTWAREPACKAGEENTRY = "IDV";
    static final String VERSIONENTRY = "1.0.1";
    static final String HARDWAREENTRY = "x86";
    static final String OSTEXT = System.getProperty("os.name");
    static final String EXTRATEXT = "extra";
    static final String BUNDLETEXT = "bundle";
    static final String ATTACHTEXT = "arbitrary data\n";

    static final char QUOTE = '"';
    static final char COLON = ':';

    // Regex patterns

    //////////////////////////////////////////////////
    // Instance Variables

    int passcount = 0;
    int xfailcount = 0;
    int failcount = 0;
    boolean verbose = true;
    boolean pass = false;

    protected String boundary = null;

    File attach3file = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public TestFormBuilder()
    {
        setTitle("HTTPFormBuilder test(s)");
        setSystemProperties();
        prop_visual = true; // force
    }

    @Before
    public void setUp()
    {
        ExternalServer.HTTPKIT.assumeIsAvailable();
    }

    @Test
    public void
    testSimple()
            throws Exception
    {
        pass = true;
        try {
            HTTPFormBuilder builder = buildForm(false);
            HttpEntity content = builder.build();
            try (HTTPMethod postMethod = HTTPFactory.Post(TESTURL)) {
                postMethod.setRequestContent(content);
                postMethod.execute();
                int code = postMethod.getStatusCode();
                String body = postMethod.getResponseAsString();
                if(code != 200) {
                    System.err.println("Bad return code: " + code);
                    pass = false;
                }
                if(pass) {
                    Object json = Json.parse(body);
                    json = cleanup(json, false);
                    String text = Json.toString(json);
                    text = localize(text, OSTEXT);
                    if(prop_visual)
                        visual(TESTURL, text);
                    String diffs = compare("TestSimple", expectedSimple, text);
                    if(diffs != null) {
                        System.err.println(diffs);
                        pass = false;
                    }
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            pass = false;
        }
        Assert.assertTrue("TestFormBuilder.simple: failed", pass);
    }

    @Test
    public void
    testMultiPart()
            throws Exception
    {
        // Try to create a tmp file
	attach3file = HTTPUtil.fillTempFile("attach3.txt", ATTACHTEXT);
        attach3file.deleteOnExit();

        pass = true;
        try {
            HTTPFormBuilder builder = buildForm(true);
            HttpEntity content = builder.build();
            try (HTTPMethod postMethod = HTTPFactory.Post(TESTURL)) {
                postMethod.setRequestContent(content);
                postMethod.execute();
                int code = postMethod.getStatusCode();
                String body = postMethod.getResponseAsString();
                if(code != 200) {
                    System.err.println("Bad return code: " + code);
                    pass = false;
                }
                if(pass) {
                    if(DEBUG)
                       visual("RAW",body);
                    Object json = Json.parse(body);
                    json = cleanup(json, true);
                    String text = Json.toString(json);
                    text = localize(text, OSTEXT);
                    body = text;
                    if(prop_visual)
                        visual(TESTURL, body);
                    String diffs = compare("TestMultipart", expectedMultipart, text);
                    if(diffs != null) {
                        System.err.println(diffs);
                        pass = false;
                    }
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            pass = false;
        }
        Assert.assertTrue("TestFormBuilder.multipart: failed", pass);
    }

    protected HTTPFormBuilder buildForm(boolean multipart)
            throws HTTPException
    {
        HTTPFormBuilder builder = new HTTPFormBuilder();

        /*
        StringBuffer javaInfo = new StringBuffer();
        javaInfo.append("Java: home: " + System.getProperty("java.home"));
        javaInfo.append(" version: " + System.getProperty("java.version"))
        */

        builder.add("fullName", NAMEENTRY);
        builder.add("emailAddress", EMAILENTRY);
        builder.add("organization", ORGENTRY);
        builder.add("subject", SUBJECTENTRY);
        builder.add("description", DESCRIPTIONENTRY);
        builder.add("softwarePackage", SOFTWAREPACKAGEENTRY);
        builder.add("packageVersion", VERSIONENTRY);
        builder.add("os", OSTEXT);
        builder.add("hardware", HARDWAREENTRY);

        if(multipart) {
            // Use bytes
            builder.add("attachmentOne", EXTRATEXT.getBytes(HTTPUtil.ASCII), "extra.html");
            // Use Inputstream
            byte[] bytes = BUNDLETEXT.getBytes(HTTPUtil.UTF8);
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            builder.add("attachmentTwo", bis, "bundle.xidv");
            if(attach3file != null) {
                // Use File
                builder.add("attachmentThree", attach3file);
            }
        }
        return builder;
    }

    protected String localize(String text, String os)
            throws HTTPException
    {
	// Handle case with and without blank/+
        text = text.replace(os, "<OSNAME>");
        os = os.replace(' ', '+');
        text = text.replace(os, "<OSNAME>");
        return text;
    }

    protected Map<String, Object> cleanup(Object o, boolean multipart)
            throws IOException
    {
        Map<String, Object> map = (Map<String, Object>) o;
        map = (Map<String, Object>) sort(map);
        Object oh = map.get("headers");
        String boundary = null;
        if(oh != null) {
            Map<String, Object> headers = (Map<String, Object>) oh;
            String formdata = (String) headers.get("content-type");
            if(oh != null) {
                String[] pieces = formdata.split("[ \t]*[;][ \t]*");
                for(String p : pieces) {
                    if(p.startsWith("boundary=")) {
                        boundary = p.substring("boundary=".length(), p.length());
                        break;
                    }
                }
            }
            // Remove headers
            map.remove("headers");
        }
        // Now parse and change the body
        String body = (String) map.get("body");
        if(body != null) {
            if(multipart && boundary != null) {
                Map<String, String> bodymap = parsemultipartbody(body);
                map.put("body", mapjoin(bodymap, "\n", ": "));
            } else {
                Map<String, String> bodymap = parsesimplebody(body);
                map.put("body", mapjoin(bodymap, "&", "="));
            }
        }
        return map;
    }

    protected Object sort(Object o)
    {
        if(o instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) o;
            map = new TreeMap(map); // Convert the map to sorted order
            for(Map.Entry<String, Object> entry : map.entrySet()) {
                map.put(entry.getKey(), sort(entry.getValue()));
            }
            return map;
        } else if(o instanceof List) {
            List<Object> list = (List) o;
            List<Object> out = new ArrayList<>();
            for(int i = 0; i < list.size(); i++) {
                out.add(sort(list.get(i)));
            }
            return out;
        } else
            return o;
    }

    protected Map<String, String> parsesimplebody(String body)
            throws HTTPException
    {
        Map<String, String> map = new TreeMap<>();
        String[] pieces = body.split("[&]");
        for(String piece : pieces) {
            String[] pair = piece.split("[=]");
            if(pair.length == 1) {
                pair = new String[]{pair[0], ""};
            }
            if(pair[0] == null || pair[0].length() == 0)
                throw new HTTPException("Illegal body : " + body);
            map.put(pair[0], pair[1]);
        }
        return map;
    }

    static final String patb = "--.*";
    static final Pattern blockb = Pattern.compile(patb);

    static final String patcd =
            "Content-Disposition:\\s+form-data;\\s+name=[\"]([^\"]*)[\"]";
    static final Pattern blockcd = Pattern.compile(patcd);
    static final String patcdx = patcd
            + "\\s*[;]\\s+filename=[\"]([^\"]*)[\"]";
    static final Pattern blockcdx = Pattern.compile(patcdx);

    protected Map<String, String> parsemultipartbody(String body)
            throws IOException
    {
        Map<String, String> map = new TreeMap<>();
        body = body.replace("\r\n", "\n");
        StringReader sr = new StringReader(body);
        BufferedReader rdr = new BufferedReader(sr);
        String line = rdr.readLine();
        if(line == null)
            throw new HTTPException("Empty body");
        for(; ; ) { // invariant is that the next unconsumed line is in line
            String name = null;
            String filename = null;
            StringBuilder value = new StringBuilder();
            if(!line.startsWith("--"))
                throw new HTTPException("Missing boundary marker : " + line);
            line = rdr.readLine();
            // This might have been the trailing boundary marker
            if(line == null)
                break;
            if(line.toLowerCase().startsWith("content-disposition")) {
                // Parse the content-disposition
                Matcher mcd = blockcdx.matcher(line); // try extended
                if(!mcd.lookingAt()) {
                    mcd = blockcd.matcher(line);
                    if(!mcd.lookingAt())
                        throw new HTTPException("Malformed Content-Disposition marker : " + line);
                    name = mcd.group(1);
                }  else {
                    name = mcd.group(1);
                    filename = mcd.group(2);
                }
            } else
                throw new HTTPException("Missing Content-Disposition marker : " + line);
            // Treat content-type line as optional; may or may not have charset
            line = rdr.readLine();
            if(line.toLowerCase().startsWith("content-type")) {
                line = rdr.readLine();
            }
            // treat content-transfer-encoding line as optional
            if(line.toLowerCase().startsWith("content-transfer-encoding")) {
                line = rdr.readLine();
            }
            // Skip one blank line
            line = rdr.readLine();
            // Extract the content
            value.setLength(0);
            while(!line.startsWith("--")) {
                value.append(line);
                value.append("\n");
                line = rdr.readLine();
            }
            map.put(name, value.toString());
        }
        return map;
    }

    static protected String join(String[] pieces, String sep)
    {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < pieces.length; i++) {
            if(i > 0) buf.append(sep);
            buf.append(pieces[i]);
        }
        return buf.toString();
    }

    static protected String mapjoin(Map<String, String> map, String sep1, String sep2)
    {
        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : map.entrySet()) {
            if(!first) buf.append(sep1);
            first = false;
            buf.append(entry.getKey());
            buf.append(sep2);
            buf.append(entry.getValue());
        }
        return buf.toString();
    }

    static final String expectedSimple =
            "{\n"
                    + "  \"body\" : \"description=TestFormBuilder&emailAddress=idv%40ucar.edu&fullName=Mr.+Jones&hardware=x86&organization=UCAR&os=<OSNAME>&packageVersion=1.0.1&softwarePackage=IDV&subject=hello\",\n"
                    + "  \"docs\" : \"http://httpkit.com/echo\",\n"
                    + "  \"ip\" : \"127.0.0.1\",\n"
                    + "  \"method\" : \"POST\",\n"
                    + "  \"path\" : {\n"
                    + "    \"name\" : \"/\",\n"
                    + "    \"params\" : {},\n"
                    + "    \"query\" : \"\"\n"
                    + "  },\n"
                    + "  \"powered-by\" : \"http://httpkit.com\",\n"
                    + "  \"uri\" : \"/\"\n"
                    + "}\n";

    static final String expectedMultipart =
            "{\n"
                    + "  \"body\" : \"attachmentOne: extra\n"
                    + "attachmentThree: arbitrary data\n"
                    + "attachmentTwo: bundle\n"
                    + "description: TestFormBuilder\n"
                    + "emailAddress: idv@ucar.edu\n"
                    + "fullName: Mr. Jones\n"
                    + "hardware: x86\n"
                    + "organization: UCAR\n"
                    + "os: <OSNAME>\n"
                    + "packageVersion: 1.0.1\n"
                    + "softwarePackage: IDV\n"
                    + "subject: hello\n"
                    + "\",\n"
                    + "  \"docs\" : \"http://httpkit.com/echo\",\n"
                    + "  \"ip\" : \"127.0.0.1\",\n"
                    + "  \"method\" : \"POST\",\n"
                    + "  \"path\" : {\n"
                    + "    \"name\" : \"/\",\n"
                    + "    \"params\" : {},\n"
                    + "    \"query\" : \"\"\n"
                    + "  },\n"
                    + "  \"powered-by\" : \"http://httpkit.com\",\n"
                    + "  \"uri\" : \"/\"\n"
                    + "}\n";
}
