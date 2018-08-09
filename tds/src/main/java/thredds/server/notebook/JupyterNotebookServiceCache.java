package thredds.server.notebook;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import thredds.client.catalog.Dataset;
import thredds.server.config.TdsContext;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class JupyterNotebookServiceCache {

  static private final Logger logger = LoggerFactory.getLogger(JupyterNotebookServiceCache.class);

  @Autowired
  TdsContext tdsContext;

  private List<NotebookMetadata> allNotebooks;

  private Cache<String, NotebookMetadata> notebookMappingCache;

  public void init(int maxAge, int maxSize) {
    this.allNotebooks = new ArrayList<>();
    this.notebookMappingCache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(maxAge))
            .maximumSize(maxSize)
            .build();
    buildNotebookList();
  }

  public String getNotebookFilename(Dataset ds) {
    try {
      NotebookMetadata nbmd = this.notebookMappingCache.get(ds.getID(), () -> {
        return getNotebookMapping(ds);
      });
      return nbmd.filename;
    } catch (Exception e) {
      logger.warn(e.getMessage());
      return null;
    }
  }

  private void buildNotebookList() {

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
          NotebookMetadata nb = new NotebookMetadata(notebookFile);
          this.allNotebooks.add(nb);
        } catch (InvalidJupyterNotebookException e) {
          logger.warn(e.getMessage());
          continue;
        } catch (FileNotFoundException e) {
          logger.warn(e.getMessage());
          continue;
        }
      }
    }
  }

  private NotebookMetadata getNotebookMapping(Dataset ds) {
    NotebookMetadata bestMatch = null;
    for (NotebookMetadata nbmd : this.allNotebooks) {
      if (nbmd.isValidForDataset(ds)) {
        if (nbmd.compareNotebookForDataset(ds, bestMatch) > 0) {
          bestMatch = nbmd;
        }
      }
    }
    return bestMatch;
  }

  public List<NotebookMetadata> getAllNotebooks() { return this.allNotebooks; }

  public Map<String, NotebookMetadata> getNotebookMapping() { return this.notebookMappingCache.asMap(); }

  private class NotebookMetadata {

    public String filename;

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

      this.filename = notebookFile.getName();
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
      if (this.accept_catalogs.contains(ds.getParentCatalog().getUriString()) || this.accept_catalogs.contains(ds.getParentCatalog().getName())) {
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
      String catUrl = ds.getParentCatalog().getUriString();
      String catName = ds.getParentCatalog().getName();
      if ((this.accept_catalogs.contains(catUrl) || this.accept_catalogs.contains(catName)) && (md.accept_catalogs.contains(catUrl) || md.accept_catalogs.contains(catName))) { return tiebreaker; }
      if (this.accept_catalogs.contains(catUrl) || this.accept_catalogs.contains(catName)) { return 1; }
      if (md.accept_catalogs.contains(catUrl) || md.accept_catalogs.contains(catName)) { return -1; }

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
