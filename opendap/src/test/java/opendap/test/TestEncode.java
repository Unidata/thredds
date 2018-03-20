/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package opendap.test;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.httpservices.HTTPUtil;
import ucar.nc2.util.EscapeStrings;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;

public class TestEncode
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static final String URLILLEGAL = " \"%<>[\\]^`{|}"; // expected
    static final String QUERYILLEGAL = " \"%<>\\^`{|}"; // expected
    static final String allnonalphanum = " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";


    @Test
    public void testURLEncode() throws Exception
    {
        String legal = "";
        String illegal = "";
        // Check for url legal/illegal characters
        for(char c : allnonalphanum.toCharArray()) {
            String url = "http://localhost:8080/thredds/" + c;
            try {
                URI rui = new URI( url);
                legal += c;
            } catch (URISyntaxException e) {
                //System.err.printf("fail: c=|%c|\t%s\n", c, e.toString());
                illegal += c;
            }
        }
        System.out.println("legal url characters = |" + legal + "|");
        System.out.println("illegal url characters = |" + illegal + "|");
        Assert.assertTrue("URI Illegals mismatch", URLILLEGAL.equals(illegal));
    }

    @Test
    public void testQueryEncode() throws Exception
    {
        String legal = "";
        String illegal = "";
        // Check for url legal/illegal characters
        for(char c : allnonalphanum.toCharArray()) {
            String url = "http://localhost:8080/thredds/?" + c;
            try {
                URI rui = new URI(url);
                legal += c;
            } catch (URISyntaxException e) {
                //System.err.printf("fail: c=|%c|\t%s\n", c, e.toString());
                illegal += c;
            }
        }
        System.out.println("legal query characters = |" + legal + "|");
        System.out.println("illegal query characters = |" + illegal + "|");
        Assert.assertTrue("Query Illegals mismatch", QUERYILLEGAL.equals(illegal));
    }

    @Test
    public void testOGC()
    {
        EscapeStrings.testOGC();
    }

    public static void testB(String x)
    {
        System.out.printf("org ==   %s%n", x);
        System.out.printf("esc ==   %s%n", EscapeStrings.backslashEscape(x, ".\\"));
        System.out.printf("unesc == %s%n%n", EscapeStrings.backslashUnescape(EscapeStrings.backslashEscape(x, ".\\")));
        assert x.equals(EscapeStrings.backslashUnescape(EscapeStrings.backslashEscape(x, ".\\")));
    }

    @Test
    public void testBackslashEscape()
    {
        testB("var.name");
        testB("var..name");
        testB("var..na\\me");
        testB(".var..na\\me");
        testB(".var.\\.na\\me");
    }
}
