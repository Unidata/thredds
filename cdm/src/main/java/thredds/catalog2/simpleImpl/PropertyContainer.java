package thredds.catalog2.simpleImpl;

import thredds.catalog2.Property;
import thredds.catalog2.builder.BuilderFinishIssue;
import thredds.catalog2.builder.BuildException;

import java.util.*;

/**
 * Helper class for those classes that contain properties: ServiceImpl, DatasetNodeImpl, and CatalogImpl.
 *
 * @author edavis
 * @since 4.0
 */
class PropertyContainer
{
  private final List<Property> properties;
  private final Map<String, Property> propertiesMap;

  PropertyContainer()
  {
    this.properties = new ArrayList<Property>();
    this.propertiesMap = new HashMap<String, Property>();
  }

  public void addProperty( String name, String value )
  {
    PropertyImpl property = new PropertyImpl( name, value );
    Property curProp = this.propertiesMap.get( name );
    if ( curProp != null )
    {
      int index = this.properties.indexOf( curProp );
      this.properties.remove( index );
      this.propertiesMap.remove( name );
      this.properties.add( index, property );
    }
    else
    {
      this.properties.add( property );
    }

    this.propertiesMap.put( name, property );
    return;
  }

  public List<String> getPropertyNames()
  {
    return Collections.unmodifiableList( new ArrayList<String>( this.propertiesMap.keySet() ) );
  }

  public String getPropertyValue( String name )
  {
    return this.propertiesMap.get( name ).getValue();
  }

  public List<Property> getProperties()
  {
    return Collections.unmodifiableList( this.properties );
  }

  public Property getPropertyByName( String name )
  {
    return this.propertiesMap.get( name );
  }

  /**
   * This class always returns "true" because no action is required to
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
  public boolean isFinished( List<BuilderFinishIssue> issues )
  {
    return true;
  }

  public void finish() throws BuildException
  {
    return;
  }
}
