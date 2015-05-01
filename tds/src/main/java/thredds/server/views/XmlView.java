/* Copyright */
package thredds.server.views;

import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.web.servlet.view.AbstractView;
import thredds.util.ContentType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Take a jdom document and render it
 *
 * @author caron
 * @since 4/30/2015
 */
public class XmlView extends AbstractView {

  protected void renderMergedOutputModel(Map model, HttpServletRequest req, HttpServletResponse res) throws Exception {
    res.setContentType(ContentType.xml.getContentHeader());

    Document doc = (Document) model.get("Document");

    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(doc, res.getOutputStream());
  }

  public String getContentType() {
    return ContentType.xml.getContentHeader(); // doesnt seem to get called, at least in mock framework
  }
}
