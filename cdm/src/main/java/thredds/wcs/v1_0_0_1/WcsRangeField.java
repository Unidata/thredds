package thredds.wcs.v1_0_0_1;

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
//  private org.slf4j.Logger logger =
//          org.slf4j.LoggerFactory.getLogger( WcsRangeField.class );

  private String name;
  private String label;
  private String description;

  private Axis axis;

  public WcsRangeField( String name, String label, String description, Axis axis )
  {
    if ( name == null )
      throw new IllegalArgumentException( "Range name must be non-null." );
    if ( label == null )
      throw new IllegalArgumentException( "Range label must be non-null." );
    this.name = name;
    this.label = label;
    this.description = description;
    this.axis = axis;
  }

  public String getName() { return this.name; }
  public String getLabel() { return this.label; }
  public String getDescription() { return this.description; }
  public Axis getAxis() { return axis; }

  public static class Axis
  {
    private String name;
    private String label;
    private String description;
    private boolean isNumeric;
    private List<String> values;

    public Axis( String name, String label, String description, boolean isNumeric, List<String> values)
    {
      this.name = name;
      this.label = label;
      this.description = description;
      this.isNumeric = isNumeric;
      this.values = new ArrayList<String>(values);
    }

    public String getName() { return this.name; }
    public String getLabel() { return this.label; }
    public String getDescription() { return this.description; }
    public boolean isNumeric() { return isNumeric; }
    public List<String> getValues() { return Collections.unmodifiableList( this.values); }
  }
}
