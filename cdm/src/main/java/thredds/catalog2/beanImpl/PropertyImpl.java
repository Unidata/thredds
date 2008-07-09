package thredds.catalog2.beanImpl;

import thredds.catalog2.Property;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class PropertyImpl implements Property
{
  private String name;
  private String value;
  public PropertyImpl( String name, String value )
  {
    if ( name == null ) throw new IllegalArgumentException( "Name must not be null.");
    if ( value == null ) throw new IllegalArgumentException( "Value must not be null.");
    this.name = name;
    this.value = value;
  }

  @Override
  public String getName()
  {
    return this.name;
  }

  @Override
  public String getValue()
  {
    return this.value;
  }
}
