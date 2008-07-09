package thredds.catalog2;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class PropertyImpl implements Property
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( PropertyImpl.class );

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
    return null;
  }

  @Override
  public String getValue()
  {
    return null;
  }
}
