/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE.txt for license information.
 */

package thredds.server.wms;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import thredds.TestOnLocalServer;
import thredds.util.ContentType;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import static org.junit.Assert.assertEquals;

@Category(NeedsCdmUnitTest.class)
public class TestWmsServer {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Namespace NS_WMS = Namespace.getNamespace("wms", "http://www.opengis.net/wms");

    @Test
    public void testCapabilites() throws IOException, JDOMException {
        String endpoint = TestOnLocalServer.withHttpPath("/wms/scanCdmUnitTests/conventions/coards/sst.mnmean.nc?service=WMS&version=1.3.0&request=GetCapabilities");
        byte[] result = TestOnLocalServer.getContent(endpoint, 200, ContentType.xmlwms);
        Reader in = new StringReader(new String(result, CDM.utf8Charset));
        SAXBuilder sb = new SAXBuilder();
        Document doc = sb.build(in);

        XPathExpression<Element> xpath = XPathFactory.instance().compile("//wms:Capability/wms:Layer/wms:Layer/wms:Layer", Filters.element(), null, NS_WMS);
        List<Element> elements = xpath.evaluate(doc);
        assertEquals(1, elements.size());

        XPathExpression<Element> xpath2 = XPathFactory.instance().compile("//wms:Capability/wms:Layer/wms:Layer/wms:Layer/wms:Name", Filters.element(), null, NS_WMS);
        Element emt = xpath2.evaluateFirst(doc);
        assertEquals("sst", emt.getTextTrim());
    }

    @Test
    public void testGetPng() {
        String endpoint = TestOnLocalServer.withHttpPath("/wms/scanCdmUnitTests/conventions/cf/ipcc/tas_A1.nc?service=WMS&version=1.3.0&request=GetMap&CRS=CRS:84&WIDTH=512&HEIGHT=512&LAYERS=%20tas&BBOX=0,-90,360,90&format=image/png;mode=32bit&time=1850-01-16T12:00:00Z");
        byte[] result = TestOnLocalServer.getContent(endpoint, 200, null);
        // make sure we get a png back
        // first byte (unsigned) should equal 137 (decimal)
        assertEquals(result[0] & 0xFF, 137);
        // bytes 1, 2, and 3, when interperted as ASCII, should be P N G
        assertEquals(new String(((byte[]) Arrays.copyOfRange(result, 1, 4)), Charset.forName("US-ASCII")), "PNG");
    }
}
