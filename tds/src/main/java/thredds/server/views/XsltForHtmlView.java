/* Copyright */
package thredds.server.views;

import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.view.AbstractView;
import thredds.server.config.TdsContext;
import thredds.util.ContentType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.Map;

/**
 * Run a jdom2 document through XSLT to get an HTML page.
 *
 * @author caron
 * @since 5/1/2015
 */
@Component
public class XsltForHtmlView extends AbstractView {

  protected void renderMergedOutputModel(Map model, HttpServletRequest req, HttpServletResponse res) throws Exception {
    res.setContentType(ContentType.xml.getContentHeader());

    Document doc = (Document) model.get("Document");
    String transform = (String) model.get("Transform");
    String resourceName = "/WEB-INF/xsl/"+transform+".xsl";
    Resource resource = new ClassPathResource(resourceName);
    try (InputStream is = resource.getInputStream()) {      // could cache the xslt
      Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(is));
      transformer.setParameter("tdsContext", req.getContextPath());

      JDOMSource in = new JDOMSource(doc);
      JDOMResult out = new JDOMResult();
      transformer.transform(in, out);
      Document html = out.getDocument();

      XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      fmt.output(html, res.getOutputStream());
    }
  }

  public String getContentType() {
    return ContentType.html.getContentHeader();  // doesnt seem to get called, at least in mock framework
  }
}
