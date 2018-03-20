
/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.metadata;

import com.coverity.security.Escape;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.ThreddsMetadata;
import thredds.core.TdsRequestedDataset;
import thredds.server.catalog.writer.ThreddsMetadataExtractor;
import thredds.util.ContentType;
import thredds.util.TdsPathUtils;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dt.GridDataset;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Formatter;
import java.util.List;

/**
 * This handles requests for Variable Metadata for Gridded data.
 * Factor it out because its often very big and only sometimes wanted. Put a link on the HTML page instead.
 */

@Controller
@RequestMapping("/metadata")
public class MetadataController {

  @RequestMapping(value = "**")
  public ResponseEntity<String> getMetadata(@Valid MetadataRequestParameterBean params, BindingResult result, HttpServletResponse res, HttpServletRequest req) throws Exception {

    if (result.hasErrors())
      throw new BindException(result);
    String path = TdsPathUtils.extractPath(req, "metadata");

    try (GridDataset gridDataset = TdsRequestedDataset.getGridDataset(req, res, path)) {
      if (gridDataset == null)
        return null;

      NetcdfFile ncfile = gridDataset.getNetcdfFile();   // LOOK maybe gridDataset.getFileTypeId ??
      String fileTypeS = ncfile.getFileTypeId();
      ucar.nc2.constants.DataFormatType fileFormat = ucar.nc2.constants.DataFormatType.getType(fileTypeS);
      if (fileFormat != null) fileTypeS = fileFormat.toString(); // canonicalize

      ThreddsMetadata.VariableGroup vars = new ThreddsMetadataExtractor().extractVariables(fileTypeS, gridDataset);

      boolean wantXML = (params.getAccept() != null) && params.getAccept().equalsIgnoreCase("XML");

      HttpHeaders responseHeaders = new HttpHeaders();
      String strResponse;
      if (wantXML) {
        strResponse = writeXML(vars);
        responseHeaders.set(ContentType.HEADER, ContentType.xml.getContentHeader());
       //responseHeaders.set(Constants.Content_Disposition, Constants.setContentDispositionValue(datasetPath, ".xml"));
      } else {
        strResponse = writeHTML(vars);
        responseHeaders.set(ContentType.HEADER, ContentType.html.getContentHeader());
      }
      return new ResponseEntity<>(strResponse, responseHeaders, HttpStatus.OK);
    }

  }

  private String writeHTML(ThreddsMetadata.VariableGroup vars) {
    Formatter f = new Formatter();
    f.format("<h3>Variables:</h3>%n<ul>%n");

    f.format("<em>Vocabulary</em> [");
    if (vars.getVocabUri() != null) {
      ThreddsMetadata.UriResolved uri = vars.getVocabUri();
      f.format(" %s", uri.resolved.toString());
    }
    f.format(" %s ]%n<ul>%n", Escape.html(vars.getVocabulary()));

    java.util.List<ThreddsMetadata.Variable> vlist = vars.getVariableList();
    if (vlist.size() > 0) {
      for (ThreddsMetadata.Variable v : vlist) {
        String units = (v.getUnits() == null || v.getUnits().length() == 0) ? "" : " (" + v.getUnits() + ") ";
        f.format(" <li><strong>%s</strong>", Escape.html(v.getName() + units));
        if (v.getDescription() != null)
          f.format(" = <i>%s</i>", Escape.html(v.getDescription()));
        if (v.getVocabularyName() != null && v.getVocabularyName().length() > 0)
          f.format(" = %s", Escape.html(v.getVocabularyName()));
        f.format("%n");
      }
    }
    f.format("</ul>%n");
    return f.toString();
  }

  private String writeXML(ThreddsMetadata.VariableGroup vars) {
    Document doc = new Document();
    Element elem = new Element("variables", Catalog.defNS);
    doc.setRootElement(elem);

    if (vars.getVocabulary() != null)
      elem.setAttribute("vocabulary", vars.getVocabulary());
    if (vars.getVocabUri() != null)
      elem.setAttribute("href", vars.getVocabUri().href, Catalog.xlinkNS);

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
