/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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
package thredds.catalog2.simpleImpl;

import thredds.catalog2.DatasetNode;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.builder.DatasetNodeBuilder;
import thredds.catalog2.builder.BuilderIssues;

import java.util.*;

/**
 * Helper class for those classes that contain dataset nodes: CatalogImpl and DatasetNodeImpl.
 *
 * @author edavis
 * @since 4.0
 */
class DatasetNodeContainer
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

//  /**
//   * Map for contained DatasetNodeImpls keyed by dataset ID.
//   */
//  private Map<String, DatasetNodeImpl> datasetNodeImplMapById;

  /**
   * List of contained DatasetNodeImpl objects.
   */
  private List<DatasetNodeImpl> datasetNodeImplList;
  private List<String> localIdList;

  /**
   * The root container used for tracking DatasetNodeImpl objects by globally unique ID.
   */
  private final DatasetNodeContainer rootContainer;

  /**
   * Map for tracking DatasetNodeImpl objects by globally unique ID if this is root container.
   */
  private Map<String, DatasetNodeImpl> datasetNodeImplMapByGloballyUniqueId;

  private boolean isBuilt;

  DatasetNodeContainer( DatasetNodeContainer rootContainer )
  {
    this.isBuilt = false;
    this.datasetNodeImplList = null;

    this.rootContainer = rootContainer;
  }

  DatasetNodeContainer getRootContainer()
  {
    if ( this.rootContainer != null )
      return this.rootContainer;
    return this;
  }

  boolean isDatasetNodeIdInUseGlobally( String id )
  {
    if ( this.getDatasetNodeByGloballyUniqueId( id ) == null )
      return false;
    return true;
  }

  boolean addDatasetNodeByGloballyUniqueId( DatasetNodeImpl datasetNode )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeContainer has been built." );
    if ( datasetNode == null )
      return false;
    if ( datasetNode.getId() == null )
      return false;

    if ( this.rootContainer != null )
      return this.rootContainer.addDatasetNodeByGloballyUniqueId( datasetNode );
    else
    {
      if ( this.datasetNodeImplMapByGloballyUniqueId == null )
        this.datasetNodeImplMapByGloballyUniqueId = new HashMap<String,DatasetNodeImpl>();

      if ( this.datasetNodeImplMapByGloballyUniqueId.containsKey( datasetNode.getId() ))
        return false;
      DatasetNodeImpl replacedDatasetNode = this.datasetNodeImplMapByGloballyUniqueId.put( datasetNode.getId(), datasetNode );
      if ( replacedDatasetNode == null )
        return true;
      else
      {
        String msg = "DatasetNodeContainer in bad state [MapByGloballyUniqueId: containsKey(" + datasetNode.getId() + ")==false then put()!=null].";
        log.error( "addDatasetNodeByGloballyUniqueId(): " + msg );
        throw new IllegalStateException( msg);
      }
    }
  }

  boolean removeDatasetNodeByGloballyUniqueId( String id )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeContainer has been built." );
    if ( id == null )
      return false;

    if ( this.rootContainer != null )
      return this.rootContainer.removeDatasetNodeByGloballyUniqueId( id );
    else
    {
      if ( this.datasetNodeImplMapByGloballyUniqueId == null )
        return false;

      DatasetNodeImpl removedDatasetNode = this.datasetNodeImplMapByGloballyUniqueId.remove( id );
      if ( removedDatasetNode == null )
        return false;
      return true;
    }
  }

  boolean addDatasetNodeToLocalById( DatasetNodeImpl datasetNode )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeContainer has been built." );
    if ( datasetNode == null )
      return false;
    if ( datasetNode.getId() == null )
      return false;

    if ( this.localIdList == null )
      this.localIdList = new ArrayList<String>();

    return this.localIdList.add( datasetNode.getId() );
  }

  boolean removeDatasetNodeFromLocalById( String id )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeContainer has been built." );
    if ( id == null )
      return false;

    if ( this.localIdList == null )
      return false;

    return this.localIdList.remove( id );
  }

  DatasetNodeImpl getDatasetNodeByGloballyUniqueId( String id )
  {
    if ( id == null )
      return null;

    if ( this.rootContainer != null )
      return this.rootContainer.getDatasetNodeByGloballyUniqueId( id );
    else
    {
      if ( this.datasetNodeImplMapByGloballyUniqueId == null )
        return null;

      return this.datasetNodeImplMapByGloballyUniqueId.get( id );
    }
  }

  boolean isEmpty()
  {
    if ( this.datasetNodeImplList == null )
      return true;
    return this.datasetNodeImplList.isEmpty();
  }

  int size()
  {
    if ( this.datasetNodeImplList == null )
      return 0;
    return this.datasetNodeImplList.size();
  }

  /**
   * Add a DatasetNodeImpl to this container.
   *
   * @param datasetNode the DatasetNodeImpl to add.
   * @throws IllegalArgumentException if datasetNode is null.
   * @throws IllegalStateException if build() has been called on this DatasetNodeContainer or the id of the DatasetNode is not unique in the root container.
   */
  void addDatasetNode( DatasetNodeImpl datasetNode )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeContainer has been built.");

    // If datasetNode has an ID, make sure it is globally unique (i.e., not in use).
    // If it is unique, track it both globally and as part of this collection.
    if ( datasetNode.getId() != null )
    {
      if ( ! this.addDatasetNodeByGloballyUniqueId( datasetNode ) )
        throw new IllegalStateException( "Globally unique DatasetNode ID is already being used." );
      if ( this.localIdList == null )
        this.localIdList = new ArrayList<String>();
      this.localIdList.add( datasetNode.getId() );
    }

    if ( this.datasetNodeImplList == null )
      this.datasetNodeImplList = new ArrayList<DatasetNodeImpl>();

    if ( ! this.datasetNodeImplList.add( datasetNode ))
      log.error( "addDatasetNode(): failed to add datasetNode name [" + datasetNode.getName() + "]." );

    return;
  }

  /**
   * Remove the given DatasetNode from this container if it is present.
   *
   * @param datasetNode the DatasetNode to remove.
   * @return true if the DatasetNode was present and has been removed, otherwise false.
   * @throws IllegalArgumentException if datasetNode is null.
   * @throws IllegalStateException if build() has been called on this DatasetNodeContainer.
   */
  boolean removeDatasetNode( DatasetNodeImpl datasetNode )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeContainer has been built." );

    if ( datasetNode == null )
      return false;

    if ( this.datasetNodeImplList == null )
      return false;

    // Remove from container.
    if ( ! this.datasetNodeImplList.remove( datasetNode ))
      return false;

    // Check if has global ID and remove from Map tracking DataseNodes by global ID.
    String id = datasetNode.getId();
    if ( id != null )
    {
      if ( this.localIdList != null && this.localIdList.remove( id ) )
      {
        if ( ! this.removeDatasetNodeByGloballyUniqueId( id ) )
        {
          String msg = "Removal from DatasetNode by global ID inconsistent with DatasetNode removal [" + datasetNode.getName() + "].";
          log.error( "removeDatasetNode(): " + msg );
          throw new IllegalStateException( msg );
        }
      }
    }
    return true;
  }

  List<DatasetNode> getDatasets()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeCollection has escaped its Builder before being built." );

    if ( this.datasetNodeImplList == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<DatasetNode>( this.datasetNodeImplList ));
  }

  DatasetNode getDatasetById( String id )
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeCollection has escaped its Builder before being built." );
    if ( id == null )
      return null;
    if ( this.datasetNodeImplList == null )
      return null;

    if ( this.localIdList != null && this.localIdList.contains( id ) )
      return this.getDatasetNodeByGloballyUniqueId( id );
    return null;
  }

  List<DatasetNodeBuilder> getDatasetNodeBuilders()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeContainer has been built." );
    if ( this.datasetNodeImplList == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<DatasetNodeBuilder>( this.datasetNodeImplList ) );
  }

  DatasetNodeBuilder getDatasetNodeBuilderById( String id )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeContainer has been built." );
    if ( id == null )
      return null;
    if ( this.datasetNodeImplList == null )
      return null;

    if ( this.localIdList != null && this.localIdList.contains( id ) )
      return this.getDatasetNodeByGloballyUniqueId( id );
    return null;
  }

  /**
   * Check whether contained DatasetNodeBuilders are all in a state such that
   * calling their build() will succeed.
   *
   * @return true if this DatasetNodeContainer is in a state where build() will succeed.
   */
  BuilderIssues getIssues()
  {
    BuilderIssues issues = new BuilderIssues();

    // Check on contained DatasetNodeImpl objects.
    if ( this.datasetNodeImplList != null )
      for ( DatasetNodeBuilder dnb : this.datasetNodeImplList )
        issues.addAllIssues( dnb.getIssues());

    return issues;
  }

  /**
   * Call build() on all contained datasets.
   *
   * @throws thredds.catalog2.builder.BuilderException if any of the contained datasets are not in a valid state.
   */
  void build()
          throws BuilderException
  {
    if ( this.isBuilt )
      return;

    // Build contained DatasetNodeImpl objects.
    if ( this.datasetNodeImplList != null )
      for ( DatasetNodeBuilder dnb : this.datasetNodeImplList )
        dnb.build();

    this.isBuilt = true;
    return;
  }
}