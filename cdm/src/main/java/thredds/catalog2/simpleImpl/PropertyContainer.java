package thredds.catalog2.simpleImpl;

import thredds.catalog2.Property;
import thredds.catalog2.builder.BuilderFinishIssue;

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

  private boolean canModify;

  PropertyContainer()
  {
    this.canModify = true;
    this.propertiesMap = null;
  }

  /**
   * Add a Property with the given name and value to this container.
   *
   * @param name the name of the Property to add.
   * @param value the value of the Property to add.
   * @throws IllegalArgumentException if name or value are null.
   * @throws IllegalStateException if build() has been called on this PropertyContainer.
   */
  public void addProperty( String name, String value )
  {
    if ( ! this.canModify )
      throw new IllegalStateException( "This PropertyContainer has been built.");

    if ( this.propertiesMap == null )
      this.propertiesMap = new HashMap<String, Property>();

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
  public boolean removeProperty( String name )
  {
    if ( ! this.canModify )
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

  public List<String> getPropertyNames()
  {
    if ( this.propertiesMap == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<String>( this.propertiesMap.keySet() ) );
  }

  public boolean containsPropertyName( String name )
  {
    if ( name == null )
      return false;

    if ( this.propertiesMap == null )
      return false;

    if ( this.propertiesMap.get( name ) == null )
      return false;
    return true;
  }

  public String getPropertyValue( String name )
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

  public List<Property> getProperties()
  {
    if ( this.propertiesMap == null )
      return Collections.emptyList();

    return Collections.unmodifiableList( new ArrayList<Property>( this.propertiesMap.values() ) );
  }

  public Property getPropertyByName( String name )
  {
    if ( name == null )
      return null;

    if ( this.propertiesMap == null )
      return null;

    return this.propertiesMap.get( name );
  }

  /**
   * This method always returns "true" because no action is required to
   * finish any contained Property classes.
   *
   * The reasons for this are:
   * <ol>
   * <li>The Property class is immutable and doesn't allow null names or values.</li>
   * <li>This container stores the properties in a Map by property name (so there are no duplicate names).</li>
   * </ol>
   *
   * @param issues a list in which to add any issues that come up during isFinished()
   * @return true if this PropertyContainer is in a state where finish() will succeed.
   */
  public boolean isBuildable( List<BuilderFinishIssue> issues )
  {
    return true;
  }

  /**
   * Mark this PropertyContainer as unmodifiable and return "true".
   *
   * No validation is required
   * (see {@link #isBuildable(List<BuilderFinishIssue>) isBuildable(issues)})
   */
  public void build()
  {
    this.canModify = false;
    return;
  }
}
