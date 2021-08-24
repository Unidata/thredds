package thredds.util.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPath;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.mock.web.MockHttpServletResponse;

public class XmlUtil {

  private XmlUtil() {
  }

  public static Document getStringResponseAsDoc(MockHttpServletResponse response) throws UnsupportedEncodingException, JDOMException, IOException {

    SAXBuilder sb = new SAXBuilder();
    sb.setExpandEntities(false);
    String strResponse = response.getContentAsString();
    return sb.build(new ByteArrayInputStream(strResponse.getBytes(response.getCharacterEncoding())));

  }

  public static List<Element> evaluateXPath(Document doc, String strXpath) {

    try {

      XPathExpression<Element> xpath = XPathFactory.instance().compile(strXpath, Filters.element());
      return xpath.evaluate(doc);


    } catch (IllegalStateException e) {

      e.printStackTrace();
    }

    return null;
  }

}
