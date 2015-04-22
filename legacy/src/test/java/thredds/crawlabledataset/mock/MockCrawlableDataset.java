/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package thredds.crawlabledataset.mock;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.crawlabledataset.CrawlableDatasetUtils;

import java.util.*;
import java.io.IOException;

/**
 * Basic mock for CrawlableDataset.
 *
 * Path separator is slash ("/"). Path must start with separator and must
 * have at least one path segment.
 */
public class MockCrawlableDataset implements CrawlableDataset
{
  private String path;
  private String name;

  private Date lastModified = null;
  private long length = -1;
  private boolean exists = true;

  private boolean isCollection;
  private CrawlableDataset parent;
  private List<CrawlableDataset> childrenList;
  private Map<String,CrawlableDataset> childrenMap; // Used only to ensure unique child name (all else uses childrenList)

  public MockCrawlableDataset( String path, boolean isCollection ) {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments(path);
    if ( ! CrawlableDatasetUtils.isValidAbsolutePath(pathSegments)
        && ! CrawlableDatasetUtils.isValidRelativePath( pathSegments))
      throw new IllegalArgumentException( String.format( "Ill-formed path [%s]", path));
    this.path = path;
    this.name = pathSegments[ pathSegments.length - 1 ];

    this.isCollection = isCollection;
  }

  public void setParent( MockCrawlableDataset parent ) {
    this.parent = parent;
  }

  /**
   * Construct a new MockCrawlableDataset and add it as a child of this MockCrawlableDataset.
   * (If a child with the same name already exists, it will be replaced.)
   *
   * @param childName the name of the new child.
   * @param isCollection whether the new child is a collection
   * @return the new child MockCrawlableDataset
   */
  public MockCrawlableDataset addChild( String childName, boolean isCollection ) {
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( childName);
    if ( ! CrawlableDatasetUtils.isValidRelativePath( pathSegments) || pathSegments.length > 1)
      throw new IllegalArgumentException( String.format( "Child name [%s] is not valid.", childName));

    MockCrawlableDataset childCrDs = new MockCrawlableDataset( this.path + "/" + childName, isCollection);
    childCrDs.setParent( this);
    if ( this.childrenMap == null ) {
      this.childrenMap = new HashMap<String, CrawlableDataset>();
      this.childrenList = new ArrayList<CrawlableDataset>();
    }
    CrawlableDataset prevCrDs = this.childrenMap.put(childCrDs.getName(), childCrDs);
    if ( prevCrDs != null )
      this.childrenList.remove( prevCrDs);
    this.childrenList.add( childCrDs);
    return childCrDs;
  }

  public boolean removeChild( String childName ) {
    if ( childName == null || ! this.isCollection )
      return false;
    if ( this.childrenMap == null || this.childrenMap.isEmpty() )
      return false;
    CrawlableDataset prevCrDs = this.childrenMap.remove( childName);

    return prevCrDs != null && this.childrenList.remove(prevCrDs);
  }

  public void setExists( boolean exists) {
    this.exists = exists;
  }

  public Object getConfigObject() {
    return null;
  }

  public String getPath() {
    return path;
  }

  public String getName() {
    return name;
  }

  public CrawlableDataset getParentDataset() {
    return this.parent;
  }

  public boolean exists() {
    return this.exists;
  }

  public boolean isCollection() {
    return this.isCollection;
  }

  public CrawlableDataset getDescendant( String relativePath ) {
    if ( ! this.isCollection )
      throw new IllegalStateException( "Dataset not a collection.");
    String[] pathSegments = CrawlableDatasetUtils.getPathSegments( relativePath);
    if ( ! CrawlableDatasetUtils.isValidRelativePath( pathSegments))
      throw new IllegalArgumentException( String.format( "Ill-formed relative path [%s]", path));

    boolean singleLevelPath = pathSegments.length == 1;

    CrawlableDataset curCrDs = this.childrenMap.get( pathSegments[0]);
    if ( curCrDs != null ) {
      if ( singleLevelPath )
        return curCrDs;
      return curCrDs.getDescendant( CrawlableDatasetUtils.stepDownRelativePath(pathSegments));
    } else {
      curCrDs = new MockCrawlableDataset( this.getPath() + "/" + pathSegments[0], singleLevelPath);
      ((MockCrawlableDataset)curCrDs).setExists( false);
      if ( singleLevelPath )
        return curCrDs;
      return curCrDs.getDescendant( CrawlableDatasetUtils.stepDownRelativePath(pathSegments));
    }
  }

  public List<CrawlableDataset> listDatasets() throws IOException {
    if ( ! this.isCollection )
      throw new IllegalStateException( "Dataset not a collection.");
    if ( this.childrenList == null || this.childrenList.isEmpty())
      return Collections.emptyList();
    return Collections.unmodifiableList( this.childrenList);
  }

  public List<CrawlableDataset> listDatasets( CrawlableDatasetFilter filter )
      throws IOException
  {
    if ( filter == null )
      return this.listDatasets();

    if ( ! this.isCollection )
      throw new IllegalStateException( "Dataset not a collection.");
    if ( this.childrenList == null || this.childrenList.isEmpty())
      return Collections.emptyList();

    List<CrawlableDataset> matchingChildren = new ArrayList<CrawlableDataset>();
    for ( CrawlableDataset curCrDs : this.childrenList) {
      if ( filter.accept( curCrDs))
        matchingChildren.add( curCrDs);
    }
    return matchingChildren;
  }

  public long length() {
    return this.length;
  }

  public void setLength( long length ) {
    this.length = length;
  }

  public Date lastModified() { // or long milliseconds?
    return this.lastModified;
  }

  public void setLastModified( Date lastModified) {
    this.lastModified = lastModified;
  }
}