/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
// $Id: CrawlableDatasetAlias.java 63 2006-07-12 21:50:51Z edavis $
package thredds.crawlabledataset;


import thredds.crawlabledataset.filter.WildcardMatchOnNameFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// @todo Add "?" as another possible wildcard character.

/**
 * An alias for a collection of datasets (i.e., the dataset path contains
 * one or more wildcard characters ("*")).
 *
 * @author edavis
 * @since Jun 21, 2005T4:53:43 PM
 */
public class CrawlableDatasetAlias implements CrawlableDataset {
  //private static Log log = LogFactory.getLog( CrawlableDatasetAlias.class );
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CrawlableDatasetAlias.class);

  private String path;
  private String name;

  private String wildcardPattern;
  private String postWildcardPath;

  private CrawlableDataset startDs;

  private String className;
  private Object configObj;

  public static boolean isAlias(String path) {
    return (path.indexOf("*") != -1);
    //return ( path.indexOf( "?" ) != -1 || path.indexOf( "*" ) != -1 );
  }

  public CrawlableDatasetAlias(String path, String className, Object configObj) {
    if (!isAlias(path)) throw new IllegalArgumentException("No wildcard in path <" + path + ">.");

    this.path = path;

    this.className = className;
    this.configObj = configObj;

    // @todo Make sure works if "*" is in first part of dataset path
    // Determine the location of the first path section containing wildcard.
    int preWildcardIndex = this.path.lastIndexOf("/", this.path.indexOf("*"));
    int postWildcardIndex = this.path.indexOf("/", preWildcardIndex + 1);
    log.debug("[" + preWildcardIndex + "] - [" + postWildcardIndex + "]");
    String preWildcardPath = this.path.substring(0, preWildcardIndex);
    this.wildcardPattern = postWildcardIndex == -1
            ? this.path.substring(preWildcardIndex + 1)
            : this.path.substring(preWildcardIndex + 1, postWildcardIndex);
    this.postWildcardPath = postWildcardIndex == -1
            ? null : this.path.substring(postWildcardIndex + 1);
    log.debug("dirPattern <" + this.path + ">=<" + preWildcardPath + "[" + preWildcardIndex + "]" + wildcardPattern + "[" + postWildcardIndex + "]" + postWildcardPath + ">");

    // Set the name to be all segments from first wildcard on
    this.name = this.path.substring(preWildcardIndex + 1);

    // Make sure pre-wildcard path is a directory.
    try {
      startDs = CrawlableDatasetFactory.createCrawlableDataset(preWildcardPath, this.className, this.configObj);
    } catch (Exception e) {
      String tmpMsg = "Pre-wildcard path <" + preWildcardPath + "> not a CrawlableDataset of expected type <" + this.className + ">: " + e.getMessage();
      log.warn("CrawlableDatasetAlias(): " + tmpMsg);
      throw new IllegalArgumentException(tmpMsg);
    }
    if (!startDs.isCollection()) {
      String tmpMsg = "Pre-wildcard path not a directory <" + startDs.getPath() + ">";
      log.warn("CrawlableDatasetAlias(): " + tmpMsg);
      throw new IllegalArgumentException(tmpMsg);
    }
  }

  public Object getConfigObject() {
    return configObj;
  }

  /** */
  public String getPath() {
    return (this.path);
  }

  /**
   * Returns the name (unlike a CrawlableDataset, the name may not be related to the path).
   */
  public String getName() {
    return (this.name);
  }

  public boolean exists() {
    return true; // @todo ????
  }

  public boolean isCollection() {
    return true;
  }

  public CrawlableDataset getDescendant(String childPath) {
    return null; // @todo ????
  }

  public CrawlableDataset getParentDataset() {
    return null;
  }

  public List<CrawlableDataset> listDatasets() throws IOException {
    // Get list of files in pre-wildcard directory that match the wildcard pattern.
    List<CrawlableDataset> curMatchDatasets = startDs.listDatasets(new CrawlableDatasetAlias.MyFilter(wildcardPattern, postWildcardPath != null));

    // The wildcard is in the last part of the alias path, so
    // the list from startDs is what we want.
    if (postWildcardPath == null) {
      return curMatchDatasets;
    }
    //
    else {
      List<CrawlableDataset> list = new ArrayList<CrawlableDataset>();
      for (CrawlableDataset curDs : curMatchDatasets) {
        // Append the remaining path to the end of the current dataset path.
        String curMatchPathName = curDs.getPath() + "/" + postWildcardPath;

        // Create a new CrawlableDataset with the new path.
        CrawlableDataset newCrawlableDs = null;
        try {
          newCrawlableDs = CrawlableDatasetFactory.createCrawlableDataset(curMatchPathName, className, configObj);
        } catch (Exception e) {
          String tmpMsg = "Couldn't create CrawlableDataset for path <" + curMatchPathName + "> and given class name <" + className + ">: " + e.getMessage();
          log.warn("listDatasets(): " + tmpMsg);
          continue;
        }

        // If the new dataset's path contains wildcard characters, add its
        // list of datasets to the return list.
        if (isAlias(postWildcardPath)) {
          list.addAll(newCrawlableDs.listDatasets());
        }
        // If the new dataset's path does not contain any wildcard characters,
        // add the it to the return list.
        else {
          list.add(newCrawlableDs);
        }
      }
      return (list);
    }
  }

  public List<CrawlableDataset> listDatasets(CrawlableDatasetFilter filter) throws IOException {
    List<CrawlableDataset> list = this.listDatasets();
    if (filter == null) return list;

    List<CrawlableDataset> result = new ArrayList<>(list.size());
    for (CrawlableDataset curDs : list) {
      if (filter.accept(curDs)) {
        result.add(curDs);
      }
    }
    return result;
  }

  public long length() {
    return -1;
  }

  public Date lastModified() {
    return null;
  }

  private static class MyFilter implements CrawlableDatasetFilter {
    private boolean mustBeCollection;
    private WildcardMatchOnNameFilter proxyFilter;

    /**
     * A CrawlableDatasetFilter that finds CrawlableDatasets where their name matches the given
     * wildcard string and are collections if mustBeCollection is true.
     *
     * @param wildcardString   a string containing wildcard characters ("*") to match against the CrawlableDataset name
     * @param mustBeCollection if true the filter only accepts collection datasets
     */
    MyFilter(String wildcardString, boolean mustBeCollection) {
      proxyFilter = new WildcardMatchOnNameFilter(wildcardString);
      this.mustBeCollection = mustBeCollection;
    }

    public Object getConfigObject() {
      return null;
    }

    public boolean accept(CrawlableDataset dataset) {
      if (mustBeCollection && !dataset.isCollection()) return (false);
      return proxyFilter.accept(dataset);
    }

  }


}
