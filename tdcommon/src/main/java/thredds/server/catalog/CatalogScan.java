/* Copyright */
package thredds.server.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.*;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.builder.CatalogRefBuilder;
import thredds.client.catalog.builder.DatasetBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Describe
 *
 * @author caron
 * @since 6/9/2015
 */
public class CatalogScan extends CatalogRef {
  static private final Logger logger = LoggerFactory.getLogger(CatalogScan.class);
  static private final String CATSCAN = "catalogScan.xml";

  private final String path, location, watch;

  public String getPath() {
    return path;
  }

  public String getLocation() {
    return location;
  } // reletive to ${tds.content.root.path}

  public String getWatch() {
    return watch;
  }

  public CatalogScan(DatasetNode parent, String xlink, String path, String location, String watch, Map<String, Object> flds) {
    super(parent, path, xlink, flds, null, null);

    this.path = path;           // could use flds
    this.location = location;
    this.watch = watch;
  }

  public Catalog makeCatalog(File baseDir, String matchRemaining, String filename, URI baseURI, CatalogReader reader) throws IOException {

    /* Get the dataset reletive location.
    String reletiveLocation = translatePathToReletiveLocation(workPath, path);
    if (reletiveLocation == null) {
      String tmpMsg = "makeCatalogForDirectory(): Requesting path <" + workPath + "> must start with \"" + path + "\".";
      logger.error(tmpMsg);
      return null;
    } */
    //String parentPath = (reletiveLocation.length() > 1) ? path + "/" + reletiveLocation : path + "/";
    //String parentId = (reletiveLocation.length() > 1) ? this.getId() + "/" + reletiveLocation : this.getId() + "/";
    String relLocation = (matchRemaining.length() > 1) ? location + "/" + matchRemaining : location;
    String name = (matchRemaining.length() > 1) ? getName() + "/" + matchRemaining : getName();
    File absLocation = new File( baseDir, relLocation);
    // it must be an actual catalog
    if (!filename.equalsIgnoreCase(CATSCAN)) {
      return reader.getFromAbsolutePath(absLocation + "/" + filename);  // LOOK key is wrong; ccc wants reletive path (!)
    }

    // it must be a directory
    Path wantDir = absLocation.toPath();
    if (!Files.exists(wantDir)) throw new FileNotFoundException("Requested catalog does not exist =" + absLocation);
    if (!Files.isDirectory(wantDir)) throw new FileNotFoundException("Not a directory =" + absLocation);

    // Setup and create catalog builder.
    CatalogBuilder catBuilder = new CatalogBuilder();
    catBuilder.setBaseURI(baseURI);
    assert this.getParentCatalog() != null;
    //for (Service s : this.getParentCatalog().getServices())
    //  catBuilder.addService(s);

    DatasetBuilder top = new DatasetBuilder(null);
    top.transferMetadata(this, true);
    top.setName(name);
    top.put(Dataset.Id, null); // no id for top
    catBuilder.addDataset(top);

    // first look for catalogs
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(wantDir, "*.xml")) {
      for (Path p : ds) {
        if (!Files.isDirectory(p)) {
          String pfilename = p.getFileName().toString();
          String urlPath = pfilename;

          //String path = dataDirComplete.length() == 0 ? filename : dataDirComplete + "/" + filename;  // reletive starting from current directory
          CatalogRefBuilder catref = new CatalogRefBuilder(top);
          catref.setTitle(urlPath);
          catref.setHref(urlPath);
          top.addDataset(catref);
        }
      }
    }

    // now look for directories
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(wantDir)) {
      for (Path dir : ds) {
        if (Files.isDirectory(dir)) {
          String dfilename = dir.getFileName().toString();
          String urlPath = (matchRemaining.length() > 1) ? dfilename + "/" + matchRemaining : dfilename;

          CatalogRefBuilder catref = new CatalogRefBuilder(top);
          catref.setTitle(urlPath);
          catref.setHref(urlPath + "/"+ CATSCAN);
          catref.addToList(Dataset.Properties, new Property("CatalogScan", "true"));
          top.addDataset(catref);
        }
      }
    }

    // make the catalog
    return catBuilder.makeCatalog();
  }

}
