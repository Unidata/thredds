package thredds.server.notebook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thymeleaf.context.WebContext;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.core.CatalogManager;
import thredds.core.StandardService;
import thredds.core.TdsRequestedDataset;
import thredds.server.config.TdsContext;
import thredds.util.Constants;
import thredds.util.TdsPathUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Controller
@RequestMapping("/notebook")
public class NotebookController {

  private final String DS_REPLACE_TEXT = "{{datasetName}}";
  private final String CAT_REPLACE_TEXT = "{{catUrl}}";

  @Autowired
  TdsContext tdsContext;

  @Autowired
  CatalogManager catalogManager;

  // TODO: clean, refactor, use standard service bases
  @RequestMapping("**")
  public void getNotebookForDataset(HttpServletRequest req, HttpServletResponse res, @Valid NotebookParamsBean params,
                                    BindingResult validationResult) throws Exception {

    // Get Catalog and Dataset
    TdsRequestedDataset reqD = new TdsRequestedDataset(req, getBase());
    String datasetId = reqD.getPath();

    String catalogName = params.catalog;
    if (catalogName == null) {
      throw new IllegalArgumentException("Argument 'catalog' cannot be null.");
    }

    Catalog catalog = getCatalog(catalogName, req);
    Dataset dataset = catalog.findDatasetByID(datasetId);

    // Get notebook
    File responseFile = new File(tdsContext.getThreddsDirectory(), getNotebookFilename(dataset));
    if (!responseFile.exists()) {
      throw new FileNotFoundException(responseFile.getName());
    }

    // Transform notebook with dataset id;
    String fileContents = new String(Files.readAllBytes(Paths.get(responseFile.getAbsolutePath())));
    String host = req.getRequestURL().toString();
    host = host.substring(0, host.indexOf(req.getRequestURI()));
    fileContents = fileContents.replace(DS_REPLACE_TEXT, dataset.getName()).replace(CAT_REPLACE_TEXT, host + catalog.getBaseURI());

    // Set headers...
    res.setHeader(Constants.Content_Disposition, Constants.setContentDispositionValue(responseFile.getName()));
    res.setHeader(Constants.Content_Length, Integer.toString(fileContents.length()));
    // Set content...
    res.getOutputStream().write(fileContents.getBytes());
    res.flushBuffer();
    res.getOutputStream().close();
    res.setStatus(HttpServletResponse.SC_OK);
  }

  protected String getBase() {
    return StandardService.jupyterNotebook.getBase();
  }

  private String getNotebookFilename(Dataset ds) {
    return "notebooks/jupyter_viewer.ipynb";
  }

  private Catalog getCatalog(String catalogName, HttpServletRequest req) throws URISyntaxException, IOException {

    String catalogReqBase = StandardService.catalogRemote.getBase();

    // replace /notebook/ with /catalog/ and remove datasetId from path
    String baseUriString = req.getRequestURL().toString().replace(getBase(), catalogReqBase);
    baseUriString = baseUriString.substring(0, baseUriString.indexOf(catalogReqBase) + catalogReqBase.length());

    Catalog catalog;
    URI baseUri;
    try {
      baseUri = new URI(baseUriString);
      catalog = catalogManager.getCatalog(catalogName, baseUri);
    } catch (URISyntaxException e) {
      String msg = "Bad URI syntax [" + baseUriString + "]: " + e.getMessage();
      throw new URISyntaxException(msg, e.getReason());
    }

    if (catalog == null)
      throw new FileNotFoundException(baseUriString + catalogName);

    return catalog;
  }
}
