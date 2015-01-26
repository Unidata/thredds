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

import thredds.catalog2.Property;
import thredds.catalog2.builder.BuilderIssues;

import java.util.*;

/**
 * Helper class for those classes that contain properties: ServiceImpl, DatasetNodeImpl, and CatalogImpl.
 *
 * @author edavis
 * @since 4.0
 */
class PropertyContainer
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private Map<String, Property> propertiesMap;

  private boolean isBuilt;

  PropertyContainer()
  {
    this.isBuilt = false;
    this.propertiesMap = null;
  }

  boolean isEmpty()
  {
    if ( this.propertiesMap == null )
      return true;
    return this.propertiesMap.isEmpty();
  }

  int size()
  {
    if ( this.propertiesMap == null )
      return 0;
    return this.propertiesMap.size();
  }

  /**
   * Add a Property with the given name and value to this container.
   *
   * @param name the name of the Property to add.
   * @param value the value of the Property to add.
   * @throws IllegalArgumentException if name or value are null.
   * @throws IllegalStateException if build() has been called on this PropertyContainer.
   */
  void addProperty( String name, String value )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This PropertyContainer has been built.");

    if ( this.propertiesMap == null )
      this.propertiesMap = new LinkedHashMap<String, Property>();

    PropertyImpl property = new PropertyImpl( name, value );
    if ( null != this.propertiesMap.put( name, property ))
      if ( log.isDebugEnabled())
        log.debug( "addProperty(): reseting property [" + name + "]." );
    
    return;
  }

  /**
   * Remove the named Property from this container if it was present.
   *
   * @param name the name of the Property to remove.
   * @return true if a Property with the given name was present and has been removed, otherwise false.
   * @throws IllegalArgumentException if name is null.
   * @throws IllegalStateException if build() has been called on this PropertyContainer.
   */
  boolean removeProperty( String name )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This PropertyContainer has been built." );

    if ( name == null )
      throw new IllegalArgumentException( "Given name may not be null." );

    if ( this.propertiesMap == null )
      return false;

    Property property = this.propertiesMap.remove( name );
    if ( property == null )
      return false;

    return true;
  }

  List<String> getPropertyNames()
  {
    if ( this.propertiesMap == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<String>( this.propertiesMap.keySet() ) );
  }

  boolean containsPropertyName( String name )
  {
    if ( name == null )
      return false;

    if ( this.propertiesMap == null )
      return false;

    if ( this.propertiesMap.get( name ) == null )
      return false;
    return true;
  }

  String getPropertyValue( String name )
  {
    if ( name == null )
      return null;

    if ( this.propertiesMap == null )
      return null;

    Property property = this.propertiesMap.get( name );
    if ( property == null )
      return null;
    return property.getValue();
  }

  List<Property> getProperties()
  {
    if ( this.propertiesMap == null )
      return Collections.emptyList();

    return Collections.unmodifiableList( new ArrayList<Property>( this.propertiesMap.values() ) );
  }

  Property getPropertyByName( String name )
  {
    if ( name == null )
      return null;

    if ( this.propertiesMap == null )
      return null;

    return this.propertiesMap.get( name );
  }

  boolean isBuilt()
  {
    return this.isBuilt;
  }

  /**
   * This method always returns an empty BuilderIssues object because no action is required to
   * finish any contained Property classes.
   *
   * The reasons for this are:
   * <ol>
   * <li>The Property class is immutable and doesn't allow null names or values.</li>
   * <li>This container stores the properties in a Map by property name (so there are no duplicate names).</li>
   * </ol>
   *
   * @return an empty BuilderIssues object.
   */
  BuilderIssues getIssues()
  {
    return null;
  }

  /**
   * Mark this PropertyContainer as unmodifiable and return "true".
   *
   * No validation is required
   * (see {@link #getIssues()})
   */
  void build()
  {
    this.isBuilt = true;
    return;
  }
}
