/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package thredds.bufrtables;

import org.springframework.web.servlet.view.AbstractView;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.io.OutputStream;

/**
 * @author caron
 * @since Oct 4, 2008
 */
public class BtXmlView extends AbstractView {

  protected void renderMergedOutputModel(Map model, HttpServletRequest request, HttpServletResponse res) throws Exception {
    Document doc = (Document) model.get("doc");

    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    String infoString = fmt.outputString(doc);
    res.setContentLength(infoString.length());
    res.setContentType("text/xml; charset=UTF-8");
    res.setContentLength(infoString.length());

    OutputStream out = res.getOutputStream();
    out.write(infoString.getBytes());
    out.flush();
  }

}
