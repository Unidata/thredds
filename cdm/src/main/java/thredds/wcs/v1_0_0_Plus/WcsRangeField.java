package thredds.wcs.v1_0_0_Plus;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

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

  private List<Axis> axes;

  public WcsRangeField( String name, String label, String description )
  {
    if ( name == null )
      throw new IllegalArgumentException( "Range name must be non-null." );
    if ( label == null )
      throw new IllegalArgumentException( "Range label must be non-null." );
    this.name = name;
    this.label = label;
    this.description = description;
  }

  public String getName() { return this.name; }
  public String getLabel() { return this.label; }
  public String getDescription() { return this.description; }

  public static class Axis
  {
    private String name;
    private String label;
    private String description;
    private List<String> values;

    public Axis( String name, String label, String description, List<String> values)
    {
      this.name = name;
      this.label = label;
      this.description = description;
      this.values = new ArrayList<String>();
      Collections.copy( this.values, values);
    }

    public String getName() { return this.name; }
    public String getLabel() { return this.label; }
    public String getDescription() { return this.description; }
    public List<String> getValues() { return Collections.unmodifiableList( this.values); }
  }
}
