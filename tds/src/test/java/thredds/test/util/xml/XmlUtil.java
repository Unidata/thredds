package thredds.test.util.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPath;
import org.springframework.mock.web.MockHttpServletResponse;

public class XmlUtil {
	
	private XmlUtil(){}
	
	public static Document getStringResponseAsDoc( MockHttpServletResponse response ) throws UnsupportedEncodingException, JDOMException, IOException{
		
        SAXBuilder sb = new SAXBuilder();
        String strResponse = response.getContentAsString();        
        return sb.build( new ByteArrayInputStream(strResponse.getBytes(response.getCharacterEncoding())) );
        
	}
	
	public static List containsXPath(String strXpath, Document doc){
		
        XPath xpath;
		try {
			
			xpath = XPath.newInstance( strXpath );
			int tmp = xpath.selectNodes(doc).size();
			return xpath.selectNodes(doc);
			
		} catch (JDOMException e) {

			e.printStackTrace();
		}		
		
		return null;
	}

}
