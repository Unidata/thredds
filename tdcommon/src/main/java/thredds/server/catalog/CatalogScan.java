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
  static public final String CATSCAN = "catalogScan.xml";

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

  public CatalogScan(DatasetNode parent, String name, String xlink, Map<String, Object> flds, String path, String location, String watch) {
    super(parent, name, xlink, flds, null, null);

    this.path = path;           // could use flds
    this.location = location;
    this.watch = watch;
  }

  // when we have a real catalog  (filename != CATSCAN)
  public ConfigCatalog getCatalog(File baseDir, String matchRemaining, String filename, CatalogReader reader) throws IOException {
    String relLocation = (matchRemaining.length() >= 1) ? location + "/" + matchRemaining : location;
    File absLocation = new File(baseDir, relLocation);
    ConfigCatalog cc = reader.getFromAbsolutePath(absLocation + "/" + filename);
    if (cc == null)
      logger.warn("Cant find catalog from scan: " + absLocation + "/" + filename);
    return cc;
  }

  // when we have a catalog built from a directory (filename == CATSCAN)
  public CatalogBuilder makeCatalogFromDirectory(File baseDir, String matchRemaining, URI baseURI) throws IOException {
    String relLocation = (matchRemaining.length() >= 1) ? location + "/" + matchRemaining : location;
    String name = (matchRemaining.length() >= 1) ? getName() + "/" + matchRemaining : getName();
    File absLocation = new File( baseDir, relLocation);

    // it must be a directory
    Path wantDir = absLocation.toPath();
    if (!Files.exists(wantDir)) throw new FileNotFoundException("Requested catalog does not exist =" + absLocation);
    if (!Files.isDirectory(wantDir)) throw new FileNotFoundException("Not a directory =" + absLocation);

    // Setup and create catalog builder.
    CatalogBuilder catBuilder = new CatalogBuilder();
    catBuilder.setBaseURI(baseURI);
    assert this.getParentCatalog() != null;

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
          String urlPath = (matchRemaining.length() >= 1) ? dfilename + "/" + matchRemaining : dfilename;

          CatalogRefBuilder catref = new CatalogRefBuilder(top);
          catref.setTitle(urlPath);
          catref.setHref(urlPath + "/"+ CATSCAN);
          catref.addToList(Dataset.Properties, new Property("CatalogScan", "true"));
          top.addDataset(catref);
        }
      }
    }

    return catBuilder;
  }

}
