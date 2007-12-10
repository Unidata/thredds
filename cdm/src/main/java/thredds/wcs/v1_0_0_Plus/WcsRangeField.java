package thredds.wcs.v1_0_0_Plus;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class WcsRangeField
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( WcsRangeField.class );

  private String name;
  private String label;
  private String description;

  public WcsRangeField( String name, String label, String description)
  {
    if ( name == null )
      throw new IllegalArgumentException( "Range name must be non-null.");
    if ( label == null )
      throw new IllegalArgumentException( "Range label must be non-null.");
    this.name = name;
    this.label = label;
    this.description = description;
  }

  public String getName() { return this.name; }
  public String getLabel() { return this.label; }
  public String getDescription() { return this.description; }
}
