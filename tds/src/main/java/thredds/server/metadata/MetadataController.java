
/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.metadata;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import thredds.catalog.ThreddsMetadata;
import thredds.client.catalog.Catalog;
import thredds.servlet.DatasetHandler;
import thredds.servlet.ServletUtil;
import thredds.util.ContentType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.thredds.MetadataExtractor;
import ucar.unidata.util.StringUtil2;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Formatter;
import java.util.List;

/**
 * This handles requests for Variable Metadata for Gridded data.
 * Factor it out because its often very big and only sometimes wanted. Put a link on the HTML page instead.
 */

@Controller
@RequestMapping("/metadata")
public class MetadataController {
  private static Logger log = LoggerFactory.getLogger(MetadataController.class);

  @RequestMapping(value = "**")
  public void getMetadata(@Valid MetadataRequestParameterBean params, BindingResult result, HttpServletResponse res, HttpServletRequest req) throws IOException {

    if (result.hasErrors()) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    String path = ServletUtil.getRequestPath(req);
    path = path.substring(10);
    //String info = ServletUtil.showRequestDetail(null, req);
    //System.out.printf("%s%n", info);

    GridDataset gridDataset = null;
    try {
      gridDataset = DatasetHandler.openGridDataset(req, res, path);
      if (gridDataset == null) {
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        log.debug("DatasetHandler.FAIL path={}", path);
        return;
      }

      NetcdfFile ncfile = gridDataset.getNetcdfFile();
      String fileType = ncfile.getFileTypeId();
      thredds.catalog.DataFormatType fileFormat = thredds.catalog.DataFormatType.findType(fileType);
      ThreddsMetadata.Variables vars = MetadataExtractor.extractVariables(fileFormat, gridDataset);

      boolean wantXML = (params.getAccept() != null) && params.getAccept().equalsIgnoreCase("XML");

      String strResponse;
      if (wantXML) {
        strResponse = writeXML(vars);
        res.setContentType(ContentType.xml.getContentHeader());
      } else {
        strResponse = writeHTML(vars);
        res.setContentType(ContentType.html.getContentHeader());
      }
      thredds.servlet.ServletUtil.setResponseContentLength(res, strResponse);

      PrintWriter pw = res.getWriter();
      pw.write(strResponse);
      pw.flush();
      res.flushBuffer();

    } catch (FileNotFoundException e) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());

    } catch (Throwable t) {
      log.error("Error", t);
      res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

    } finally {
      if (gridDataset != null)
        gridDataset.close();
    }

  }

  public String writeHTML(ThreddsMetadata.Variables vars) {
    Formatter f = new Formatter();
    f.format("<h3>Variables:</h3>%n<ul>%n");

    f.format("<em>Vocabulary</em> [");
    if (vars.getVocabUri() != null) {
      URI uri = vars.getVocabUri();
      f.format(" %s", uri.toString());
    }
    f.format(" %s ]%n<ul>%n", StringUtil2.quoteHtmlContent(vars.getVocabulary()));

    java.util.List<ThreddsMetadata.Variable> vlist = vars.getVariableList();
    if (vlist.size() > 0) {
      for (ThreddsMetadata.Variable v : vlist) {
        String units = (v.getUnits() == null || v.getUnits().length() == 0) ? "" : " (" + v.getUnits() + ") ";
        f.format(" <li><strong>%s</strong>", StringUtil2.quoteHtmlContent(v.getName() + units));
        if (v.getDescription() != null)
          f.format(" = <i>%s</i>", StringUtil2.quoteHtmlContent(v.getDescription()));
        if (v.getVocabularyName() != null && v.getVocabularyName().length() > 0)
          f.format(" = %s", StringUtil2.quoteHtmlContent(v.getVocabularyName()));
        f.format("%n");
      }
    }
    f.format("</ul>%n");
    return f.toString();
  }

  private String writeXML(ThreddsMetadata.Variables vars) {
    Document doc = new Document();
    Element elem = new Element("variables", Catalog.defNS);
    doc.setRootElement(elem);

    if (vars.getVocabulary() != null)
      elem.setAttribute("vocabulary", vars.getVocabulary());
    if (vars.getVocabHref() != null)
      elem.setAttribute("href", vars.getVocabHref(), Catalog.xlinkNS);

    List<ThreddsMetadata.Variable> varList = vars.getVariableList();
    for (ThreddsMetadata.Variable v : varList) {
      elem.addContent(writeVariable(v));
    }

    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    return fmt.outputString(doc);
  }

  private Element writeVariable(ThreddsMetadata.Variable v) {
    Element elem = new Element("variable", Catalog.defNS);
    if (v.getName() != null)
      elem.setAttribute("name", v.getName());
    if (v.getDescription() != null) {
      String desc = v.getDescription().trim();
      if (desc.length() > 0)
        elem.setText(v.getDescription());
    }
    if (v.getVocabularyName() != null)
      elem.setAttribute("vocabulary_name", v.getVocabularyName());
    if (v.getUnits() != null)
      elem.setAttribute("units", v.getUnits());
    String id = v.getVocabularyId();
    if (id != null)
      elem.setAttribute("vocabulary_id", id);

    return elem;
  }

}
