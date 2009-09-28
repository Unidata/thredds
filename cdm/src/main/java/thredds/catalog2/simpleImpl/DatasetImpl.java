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

import thredds.catalog2.Dataset;
import thredds.catalog2.Access;
import thredds.catalog2.builder.*;
import thredds.catalog.ServiceType;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
class DatasetImpl
        extends DatasetNodeImpl
        implements Dataset, DatasetBuilder
{
  private List<AccessImpl> accessImplList;

  private boolean isBuilt = false;

  DatasetImpl( String name, CatalogImpl parentCatalog, DatasetNodeImpl parent )
  {
    super( name, parentCatalog, parent);
  }

  public AccessBuilder addAccessBuilder()
  {
    if ( isBuilt )
      throw new IllegalStateException( "This DatasetBuilder has been built.");
    AccessImpl a = new AccessImpl( this);
    if ( this.accessImplList == null )
      this.accessImplList = new ArrayList<AccessImpl>();
    this.accessImplList.add( a );
    return a;
  }

  public boolean removeAccessBuilder( AccessBuilder accessBuilder )
  {
    if ( isBuilt )
      throw new IllegalStateException( "This DatasetBuilder has been built." );

    if ( this.accessImplList == null )
      return false;
    return this.accessImplList.remove( (AccessImpl) accessBuilder );
  }

  public boolean isAccessible()
  {
    if ( this.accessImplList == null )
      return false;
    return ! this.accessImplList.isEmpty();
  }

  public List<Access> getAccesses()
  {
    if ( !isBuilt )
      throw new IllegalStateException( "This Dataset has escaped its DatasetBuilder before build() was called." );
    if ( this.accessImplList == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<Access>(this.accessImplList ));
  }

  public List<Access> getAccessesByType( ServiceType type )
  {
    if ( !isBuilt )
      throw new IllegalStateException( "This Dataset has escaped its DatasetBuilder before build() was called." );
    List<Access> list = new ArrayList<Access>();
    if ( this.accessImplList != null )
      for ( Access a : this.accessImplList )
        if ( a.getService().getType().equals( type ))
          list.add( a );
    return list;
  }

  public List<AccessBuilder> getAccessBuilders()
  {
    if ( isBuilt )
      throw new IllegalStateException( "This DatasetBuilder has been built." );
    if ( this.accessImplList == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<AccessBuilder>( this.accessImplList ));
  }

  public List<AccessBuilder> getAccessBuildersByType( ServiceType type )
  {
    if ( isBuilt )
      throw new IllegalStateException( "This DatasetBuilder has been built." );
    List<AccessBuilder> list = new ArrayList<AccessBuilder>();
    if ( this.accessImplList != null )
      for ( AccessBuilder a : this.accessImplList )
        if ( a.getServiceBuilder().getType().equals( type ) )
          list.add( a );
    return list;
  }

  public boolean isBuilt()
  {
    return this.isBuilt;
  }

  @Override
  public BuilderIssues getIssues()
  {
    BuilderIssues issues = super.getIssues();

    // Check subordinates.
    if ( this.accessImplList != null )
      for ( AccessBuilder ab : this.accessImplList )
        issues.addAllIssues( ab.getIssues());

    return issues;
  }

  public Dataset build() throws BuilderException
  {
    if ( this.isBuilt )
      return this;

    super.build();

    // Check subordinates.
    if ( this.accessImplList != null )
      for ( AccessBuilder ab : this.accessImplList )
        ab.build();

    this.isBuilt = true;
    return this;
  }
}
