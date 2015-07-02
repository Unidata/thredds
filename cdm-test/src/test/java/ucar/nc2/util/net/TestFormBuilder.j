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
import org.junit.Test;
import ucar.httpservices.HTTPException;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPFormBuilder;
import ucar.httpservices.HTTPMethod;
import ucar.nc2.util.UnitTestCommon;

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
    static final String EXTRATEXT = "extra";
    static final String BUNDLETEXT = "bundle";
    static final String OSTEXT = System.getProperty("os.name");

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

    //////////////////////////////////////////////////
    // Constructor(s)

    public TestFormBuilder()
    {
        setTitle("HTTPFormBuilder test(s)");
        setSystemProperties();
        prop_visual = true; // force
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
        assertTrue("TestFormBuilder.simple: failed", pass);
    }

    @Test
    public void
    testMultiPart()
            throws Exception
    {
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
        assertTrue("TestFormBuilder.multipart: failed", pass);
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
            builder.add("attachmentOne", EXTRATEXT.getBytes(HTTPFormBuilder.ASCII), "extra.html");
            builder.add("attachmentTwo", BUNDLETEXT.getBytes(HTTPFormBuilder.ASCII), "bundle.xidv");
            builder.add("attachmentThree", new byte[]{1, 2, 0}, "arbfile");
        }
        return builder;
    }

    protected String localize(String text, String os)
            throws HTTPException
    {
	String osplus = os.replace(' ', '+');
        text = text.replace(osplus, "<OS+NAME>");
        text = text.replace(os, "<OS NAME>");
        return text;
    }

    protected Map<String, Object> cleanup(Object o, boolean multipart)
            throws HTTPException
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
                map.put("body", mapjoin(bodymap, "&","="));
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

    static final Pattern blockb = Pattern.compile(
            "--[^\n]*+[\n]", Pattern.DOTALL);

    static final Pattern blockc = Pattern.compile(
            "Content-Disposition:\\s+form-data;\\s+name="
                    + "[\"]([^\"]*)[\"]"
            , Pattern.DOTALL);

    static final Pattern blockf = Pattern.compile(
            "[;]\\s+filename="
                    + "[\"]([^\"]*)[\"]"
                    + "[\n][Cc]ontent-[Tt]ype[:]\\s*([^\n]*)"
            , Pattern.DOTALL);

    static final Pattern blockx = Pattern.compile(
            "([^\n]*)[\n]"
            , Pattern.DOTALL);


    protected Map<String, String> parsemultipartbody(String body)
            throws HTTPException
    {
        Map<String, String> map = new TreeMap<>();
        body = body.replace("\r\n", "\n");
        while(body.length() > 0) {
            Matcher mb = blockb.matcher(body);
            if(!mb.lookingAt()) {
                throw new HTTPException("Missing boundary marker : " + body);
            }
            int len = mb.group(0).length();
            body = body.substring(len);
            if(body.length() == 0) break; // trailing boundary marker
            Matcher mc = blockc.matcher(body);
            if(!mc.lookingAt())
                throw new HTTPException("Missing Content-type marker : " + body);
            len = mc.group(0).length();
            String name = mc.group(1);
            body = body.substring(len);
            Matcher mf = blockf.matcher(body);
            String filename = null;
            String contenttype = null;
            if(mf.lookingAt()) {
                filename = mf.group(1);
                contenttype = mf.group(2);
                len = mf.group(0).length();
                body = body.substring(len);
            }
            // Skip the two newlines
            if(!body.startsWith("\n\n"))
                throw new HTTPException("Missing newlines");
            body = body.substring(2);
            // Extract the pieces
            String value = null;
            Matcher mx = blockx.matcher(body);
            if(mx.lookingAt()) {
                value = mx.group(1);
                len = mx.group(0).length();
                body = body.substring(len);
            } else {
                throw new HTTPException("Match failure at : " + body);
            }
            map.put(name, value);
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
                    + "  \"body\" : \"description=TestFormBuilder&emailAddress=idv%40ucar.edu&fullName=Mr.+Jones&hardware=x86&organization=UCAR&os=<OS+NAME>&packageVersion=1.0.1&softwarePackage=IDV&subject=hello\",\n"
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
                    + "attachmentThree: u0001u0002u0000\n"
                    + "attachmentTwo: bundle\n"
                    + "description: TestFormBuilder\n"
                    + "emailAddress: idv@ucar.edu\n"
                    + "fullName: Mr. Jones\n"
                    + "hardware: x86\n"
                    + "organization: UCAR\n"
                    + "os: <OS NAME>\n"
                    + "packageVersion: 1.0.1\n"
                    + "softwarePackage: IDV\n"
                    + "subject: hello\",\n"
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



