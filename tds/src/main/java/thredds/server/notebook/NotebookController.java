package thredds.server.notebook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.core.AllowedServices;
import thredds.core.CatalogManager;
import thredds.core.StandardService;
import thredds.core.TdsRequestedDataset;
import thredds.server.config.TdsContext;
import thredds.server.exception.ServiceNotAllowed;
import thredds.util.Constants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.*;
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

  @Autowired
  AllowedServices allowedServices;

  @Autowired
  JupyterNotebookServiceCache jupyterNotbooks;

  @RequestMapping("**")
  public void getNotebookForDataset(HttpServletRequest req, HttpServletResponse res, @Valid NotebookParamsBean params,
                                    BindingResult validationResult) throws Exception {

    if (!allowedServices.isAllowed(StandardService.jupyterNotebook))
      throw new ServiceNotAllowed(StandardService.jupyterNotebook.toString());

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
    File responseFile = getNotebookFile(dataset, catalog);
    if (responseFile == null) {
      throw new ServiceNotAllowed(StandardService.jupyterNotebook.toString());
    }

    // Transform notebook with dataset id;
    String fileContents = new String(Files.readAllBytes(Paths.get(responseFile.getAbsolutePath())));

    // Build catalog URL
    String catUrlString = req.getRequestURL().toString();
    catUrlString = catUrlString.substring(0, catUrlString.indexOf(getBase())) + StandardService.catalogRemote.getBase() + catalogName;

    fileContents = fileContents.replace(DS_REPLACE_TEXT, dataset.getName()).replace(CAT_REPLACE_TEXT, catUrlString);

    // Set headers...
    res.setHeader(Constants.Content_Disposition, Constants.setContentDispositionValue(responseFile.getName()));
    res.setHeader(Constants.Content_Length, Integer.toString(fileContents.length()));

    // Set content...
    String mimeType =  "application/x-ipynb+json";
    res.setContentType(mimeType);
    res.getOutputStream().write(fileContents.getBytes());
    res.flushBuffer();
    res.getOutputStream().close();
    res.setStatus(HttpServletResponse.SC_OK);
  }

  protected String getBase() {
    return StandardService.jupyterNotebook.getBase();
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

  private File getNotebookFile(Dataset ds, Catalog cat) {

    String filename = jupyterNotbooks.getNotebookFilename(ds);
    if (filename == null) return null;

    File notebooksDir = new File(tdsContext.getThreddsDirectory(), "notebooks");

    if (notebooksDir.exists() && notebooksDir.isDirectory()) {
      File jupyterViewer = new File(notebooksDir, filename);
      return jupyterViewer.exists() ? jupyterViewer : null;
    }
    return null;
  }
}
