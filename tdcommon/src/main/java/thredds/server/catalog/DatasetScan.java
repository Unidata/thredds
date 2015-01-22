/*
 * Copyright (c) 1998 - 2014. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.catalog;

import net.jcip.annotations.Immutable;
import org.jdom2.Element;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.CatalogRef;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.DatasetNode;
import thredds.client.catalog.builder.AccessBuilder;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.builder.CatalogRefBuilder;
import thredds.client.catalog.builder.DatasetBuilder;
import thredds.filesystem.MFileOS7;
import thredds.inventory.CollectionAbstract;
import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import ucar.nc2.util.CloseableIterator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * DatasetScan
 *
 * @author John
 * @since 1/12/2015
 */
@Immutable
public class DatasetScan extends CatalogRef {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatasetScan.class);
  private final DatasetScanConfig config;

  public DatasetScan(DatasetNode parent, String name, String xlink, Map<String, Object> flds, List<AccessBuilder> accessBuilders, List<DatasetBuilder> datasetBuilders,
                     DatasetScanConfig config) {
    super(parent, name, xlink, flds, accessBuilders, datasetBuilders);
    this.config = config;
  }

  DatasetScanConfig getConfig() {
    return config;
  }

  org.jdom2.Element getNcmlElement() {
    return (org.jdom2.Element) getLocalField(Dataset.Ncml);
  }

  MFile requestCrawlableDataset(String path) {
    return null;
  }

  private class RegExpNamer {
    private java.util.regex.Pattern pattern;
    private String replaceString;
    private boolean usePath;

    RegExpNamer(String regExp, String replaceString, boolean usePath) {
      this.pattern = java.util.regex.Pattern.compile(regExp);
      this.replaceString = replaceString;
    }

    public String getLabel(MFile mfile) {
      String name = usePath ? mfile.getPath() : mfile.getName();
      java.util.regex.Matcher matcher = this.pattern.matcher(name);
      if (!matcher.find()) return null;

      StringBuffer startTime = new StringBuffer();
      matcher.appendReplacement(startTime, this.replaceString);
      startTime.delete(0, matcher.start());

      if (startTime.length() == 0) return null;

      return startTime.toString();
    }
  }

  /**
   * Called from DataRootHandler.makeDynamicCatalog(), called from LocalCatalogServiceController ...
   *
   * Try to build a catalog for the given path by scanning the location
   * associated with this InvDatasetScan. The given path must start with
   * the path of this InvDatasetScan.
   *
   * @param orgPath the part of the baseURI that is the path
   * @param catURI  the base URL for the catalog, used to resolve relative URLs.
   * @return the catalog for this path or null if build unsuccessful.
   */
  public Catalog makeCatalogForDirectory(String orgPath, URI catURI) throws IOException {

    // Get the dataset path.
    String dsDirPath = translatePathToLocation(orgPath);
    if (dsDirPath == null) {
      String tmpMsg = "makeCatalogForDirectory(): Requesting path <" + orgPath + "> must start with \"" + config.path + "\".";
      log.error(tmpMsg);
      return null;
    }

    // Setup and create catalog builder.
    CatalogBuilder catBuilder = new CatalogBuilder();
    catBuilder.setBaseURI(catURI);
    DatasetBuilder top = new DatasetBuilder(null);
    top.setName(getName());
    catBuilder.addDataset(top);

    Path p = Paths.get(dsDirPath);
    if (!Files.exists(p)) throw new FileNotFoundException("Does not exist "+ dsDirPath);
    if (!Files.isDirectory(p)) throw new FileNotFoundException("Not a directory "+ dsDirPath);

    // scan the directory
    try (MFileIterator iter = new MFileIterator(p)) {
      while (iter.hasNext()) {
        MFile mfile = iter.next();
        if (mfile.isDirectory()) {
          CatalogRefBuilder catref = new CatalogRefBuilder(top);
          catref.setTitle(mfile.getName());
          catref.setHref(config.path+"/"+mfile.getName());
          top.addDataset(catref);
        } else {
          DatasetBuilder ds = new DatasetBuilder(top);
          ds.setName(mfile.getName());
          top.addDataset(ds);
        }
      }
    }

    // make the catalog
    return catBuilder.makeCatalog();
  }

  public String translatePathToLocation( String dsPath ) {
    if ( dsPath == null ) return null;
    if ( dsPath.length() == 0 ) return null;

    if ( dsPath.startsWith( "/" ) )
      dsPath = dsPath.substring( 1 );

    if ( !dsPath.startsWith( config.path))
      return null;

    // remove the matching part, the rest is the "data directory"
    String dataDir = dsPath.substring( config.path.length() );
    if ( dataDir.startsWith( "/" ) )
      dataDir = dataDir.substring( 1 );

    // heres the location
    return config.scanDir +"/"+ dataDir;
  }

  ///////////////////////
  private long olderThanMillis = -1;


    // returns everything defined by specp, checking olderThanMillis
  private class MFileIterator implements CloseableIterator<MFile> {
    DirectoryStream<Path> dirStream;
    Iterator<Path> dirStreamIterator;
    MFile nextMFile;
    long now;

    MFileIterator(Path p) throws IOException {
      dirStream = Files.newDirectoryStream(p);
      dirStreamIterator = dirStream.iterator();
      now = System.currentTimeMillis();
    }

    public boolean hasNext() {

      while (true) {
        if (!dirStreamIterator.hasNext()) {
          nextMFile = null;
          return false;
        }

        try {
          Path nextPath = dirStreamIterator.next();
          BasicFileAttributes attr =  Files.readAttributes(nextPath, BasicFileAttributes.class);

          FileTime last = attr.lastModifiedTime();
          long millisSinceModified = now - last.toMillis();
          if (millisSinceModified < olderThanMillis)
            continue;
          nextMFile = new MFileOS7(nextPath, attr);
          return true;

       } catch (IOException e) {
         throw new RuntimeException(e);
       }
      }
    }


    public MFile next() {
      if (nextMFile == null) throw new NoSuchElementException();
      return nextMFile;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    // better alternative is for caller to send in callback (Visitor pattern)
    // then we could use the try-with-resource
    public void close() throws IOException {
      dirStream.close();
    }
  }

}
