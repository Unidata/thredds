package thredds.server.notebook;

import org.json.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.core.CatalogManager;
import thredds.core.StandardService;
import thredds.core.TdsRequestedDataset;
import thredds.server.config.TdsContext;
import thredds.util.Constants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
    File responseFile = getNotebookFile(dataset, catalog);
    if (responseFile == null) {
      // TODO: handle
    }

    // Transform notebook with dataset id;
    String fileContents = new String(Files.readAllBytes(Paths.get(responseFile.getAbsolutePath())));
    URI baseURI = catalog.getBaseURI();
    String catUrlString = baseURI.toString();
    if (!baseURI.isAbsolute()) {
      String host = req.getRequestURL().toString();
      catUrlString = host.substring(0, host.indexOf(req.getRequestURI())) + catUrlString;
    }
    if (!catUrlString.endsWith(catalogName)) { catUrlString += catalogName; }

    fileContents = fileContents.replace(DS_REPLACE_TEXT, dataset.getName()).replace(CAT_REPLACE_TEXT, catUrlString);

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
    return "/notebook/";
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
    File returnFile = null;
    NotebookMetadata returnFileMetadata = null;

    File notebooksDir = new File(tdsContext.getThreddsDirectory(), "notebooks");
    if (notebooksDir.exists() && notebooksDir.isDirectory()) {

      File[] files = notebooksDir.listFiles(new FileFilter() {
        @Override
        public boolean accept(File pathname) {
          return pathname.getName().endsWith(".ipynb");
        }
      });

      for (File notebookFile : files) {
        NotebookMetadata nbMeta;
        try {
          nbMeta = new NotebookMetadata(notebookFile);
        } catch (InvalidJupyterNotebookException e) {
          // TODO: warn/log
          continue;
        } catch (FileNotFoundException e) {
          // TODO: warn/log
          continue;
        }

        if (nbMeta != null && nbMeta.isValidForDataset(ds)) {
          if (nbMeta.compareNotebookForDataset(ds, returnFileMetadata) > 0) {
            returnFile = notebookFile;
            returnFileMetadata = nbMeta;
          }
        }
      }
    }

    return returnFile;
  }


  private class NotebookMetadata {

    public boolean accept_all;

    public List<String> accept_datasetIDs;

    public List<String> accept_catalogs;

    public List<String> accept_dataset_types;

    public int order;

    public NotebookMetadata(File notebookFile) throws InvalidJupyterNotebookException, FileNotFoundException {
      if (!notebookFile.exists()) {
        throw new FileNotFoundException(notebookFile.getName());
      }

      JSONObject jobj = parseFile(notebookFile);
      if (jobj == null) {
        throw new InvalidJupyterNotebookException(String.format("Notebook %s could not be parsed", notebookFile.getName()));
      }

      this.accept_all = tryGetBoolJSON(NotebookMetadataKeys.acceptAll.key, jobj);
      this.accept_datasetIDs = tryGetListFromJSON(NotebookMetadataKeys.acceptDatasetIDs.key, jobj);
      this.accept_catalogs = tryGetListFromJSON(NotebookMetadataKeys.acceptCatalogs.key, jobj);
      this.accept_dataset_types = tryGetListFromJSON(NotebookMetadataKeys.acceptDatasetTypes.key, jobj);
      this.order = tryGetIntFromJSON(NotebookMetadataKeys.order.key, jobj);
    }

    public boolean isValidForDataset(Dataset ds) {

      if (this.accept_all) {
        return true;
      }
      if (this.accept_datasetIDs.contains(ds.getID())) {
        return true;
      }
      if (this.accept_catalogs.contains(ds.getCatalogUrl())) {
        return true;
      }
      if (this.accept_dataset_types.contains(ds.getFeatureTypeName())) {
        return true;
      }

      return false;
    }

    public int compareNotebookForDataset(Dataset ds, NotebookMetadata md) {
      if (md == null) {
        return 1;
      }

      // order
      int tiebreaker = md.order - this.order;

      // dataset id
      String id = ds.getID();
      if (this.accept_datasetIDs.contains(id) && md.accept_datasetIDs.contains(id)) { return tiebreaker; }
      if (this.accept_datasetIDs.contains(id)) { return 1; }
      if (md.accept_datasetIDs.contains(id)) { return -1; }

      // catalogs
      String catalog = ds.getCatalogUrl();
      if (this.accept_catalogs.contains(catalog) && md.accept_catalogs.contains(catalog)) { return tiebreaker; }
      if (this.accept_catalogs.contains(catalog)) { return 1; }
      if (md.accept_catalogs.contains(catalog)) { return -1; }

      // data type
      String dataType = ds.getFeatureTypeName();
      if (this.accept_dataset_types.contains(dataType) && md.accept_dataset_types.contains(dataType)) { return tiebreaker; }
      if (this.accept_dataset_types.contains(dataType)) { return 1; }
      if (md.accept_dataset_types.contains(dataType)) { return -1; }

      // accept all
      if (this.accept_all && md.accept_all) { return tiebreaker; }
      if (this.accept_all) { return 1; }
      if (md.accept_all) { return -1; }

      return 0;
    }

    private JSONObject parseFile(File notebookFile) {
      InputStream is;
      try {
        is = new FileInputStream(notebookFile);
      } catch (FileNotFoundException e) {
        return null;
      }

      JSONTokener tokener = new JSONTokener(is);
      JSONObject jobj;
      try {
        jobj = new JSONObject(tokener);
        JSONObject metadata = jobj.getJSONObject("metadata");

        try {
          JSONObject viewerInfo = metadata.getJSONObject("viewer_info");
          return viewerInfo;
        } catch (JSONException e) {
          return getDefaultViewerInfo();
        }
      } catch (JSONException e) {
        return null;
      }
    }

    private JSONObject getDefaultViewerInfo() {
      JSONObject defaultInfo = new JSONObject();
      defaultInfo.put(NotebookMetadataKeys.acceptAll.key, true);
      return defaultInfo;
    }

    private boolean tryGetBoolJSON(String key, JSONObject jobj) {
      try {
        return jobj.getBoolean(key);
      } catch (JSONException e) {
        return false;
      }
    }

    private List<String> tryGetListFromJSON(String key, JSONObject jobj) {
      List<String> list = new ArrayList<>();
      try {
        JSONArray jArray = jobj.getJSONArray(key);
        if (jArray != null)
          for (int i = 0; i < jArray.length(); i++) {
            list.add(jArray.getString(i));
          }
        return list;
      } catch (JSONException e) {
        return list;
      }
    }

    private int tryGetIntFromJSON(String key, JSONObject jobj) {
      try {
        return jobj.getInt(key);
      } catch (JSONException e) {
        return Integer.MAX_VALUE;
      }
    }
  }

  private enum NotebookMetadataKeys {
    acceptAll("accept_all"),
    acceptDatasetIDs("accept_datasetIDs"),
    acceptCatalogs("accept_catalogs"),
    acceptDatasetTypes("accept_dataset_types"),
    order("order");

    final String key;

    NotebookMetadataKeys(String key) {
      this.key = key;
    }
  }

  private class InvalidJupyterNotebookException extends Exception {
    public InvalidJupyterNotebookException(String message) {
      super(message);
    }
  }
}
