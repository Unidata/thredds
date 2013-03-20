package thredds.test.util.xml;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.xpath.XPath;

public class NcmlParserUtil {
	
	private NcmlParserUtil(){}
	
	private static final String NS_PREFIX ="nc";
	private static final String NS_PREFIX_ON_TAG ="nc:";
	
	public static List getNcMLElements(String path, Document doc){
		
		//XPath doesn't support default namespaces, so we add nc as a prefix for the tags within the namespace!!!		
		if( ! path.startsWith(NS_PREFIX_ON_TAG) && !path.startsWith("/")  ) path = NS_PREFIX_ON_TAG+path;
		
		Pattern pattern = Pattern.compile("/\\w");
		Matcher matcher = pattern.matcher(path);
		
		StringBuilder sb = new StringBuilder();
		int currentChar=0;
		while( matcher.find() ){
			
			sb.append(path.substring(currentChar, matcher.start()- currentChar+1 ));
			if( !sb.toString().endsWith("/") ) sb.append("/");
			sb.append(NS_PREFIX_ON_TAG );
			currentChar = matcher.start() + 1;
		}
		
		sb.append(path.substring(currentChar, path.length()));
		
        XPath xpath;
		try {
			
			xpath = XPath.newInstance( sb.toString() );
			xpath.addNamespace(NS_PREFIX , doc.getRootElement().getNamespaceURI() );
			return xpath.selectNodes(doc);
			
		} catch (JDOMException e) {

			e.printStackTrace();
		}
		
		return null; 
        			
	}

}
