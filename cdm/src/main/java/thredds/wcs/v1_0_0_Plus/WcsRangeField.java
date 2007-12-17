package thredds.wcs.v1_0_0_Plus;

import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.ma2.Range;
import ucar.ma2.InvalidRangeException;

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
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( WcsRangeField.class );

  private GridDatatype gridDatatype;

  private String name;
  private String label;
  private String description;
  private String datatypeString;
  private String unitsString;

  private double validMin;
  private double validMax;

  private List<Axis> axes;

  public WcsRangeField( GridDatatype gridDatatype )
  {
    if ( gridDatatype == null)
      throw new IllegalArgumentException( "Range field must be non-null.");
    this.gridDatatype = gridDatatype;

    this.name = this.gridDatatype.getName();
    this.label = this.gridDatatype.getInfo();
    this.description = this.gridDatatype.getDescription();
    this.datatypeString = this.gridDatatype.getDataType().toString();
    this.unitsString = this.gridDatatype.getUnitsString();

    this.validMin = this.gridDatatype.getVariable().getValidMin();
    this.validMax = this.gridDatatype.getVariable().getValidMax();

    GridCoordSystem gcs = this.gridDatatype.getCoordinateSystem();

    List<CoordinateAxis> fieldAxes = new ArrayList<CoordinateAxis>( gcs.getCoordinateAxes());
    fieldAxes.remove( gcs.getXHorizAxis() );
    fieldAxes.remove( gcs.getYHorizAxis() );
    if ( gcs.getVerticalAxis() != null )
      fieldAxes.remove( gcs.getVerticalAxis() );
    if ( gcs.getTimeAxis() != null )
      fieldAxes.remove( gcs.getTimeAxis() );

    if ( fieldAxes.isEmpty())
      axes = Collections.emptyList();
    else
    {
      axes = new ArrayList<Axis>();
      for ( CoordinateAxis curAxis : fieldAxes )
        if ( curAxis instanceof CoordinateAxis1D )
          axes.add( new Axis( (CoordinateAxis1D) curAxis ));
    }
  }

  GridDatatype getGridDatatype() { return this.gridDatatype; }

  public String getName() { return this.name; }
  public String getLabel() { return this.label; }
  public String getDescription() { return this.description; }
  public String getDatatypeString() { return this.datatypeString; }
  public String getUnitsString() { return this.unitsString; }

  public double getValidMin() { return this.validMin; }
  public double getValidMax() { return this.validMax; }

  public boolean hasMissingData() { return this.gridDatatype.hasMissingData(); }

  public List<Axis> getAxes() { return Collections.unmodifiableList( this.axes); }

  public static class Axis
  {
    private CoordinateAxis1D coordAxis;

    private String name;
    private String label;
    private String description;
    private boolean isNumeric;
    private List<String> values;

    public Axis( CoordinateAxis1D coordAxis)
    {
      this.coordAxis = coordAxis;
      this.name = this.coordAxis.getName();
      this.label = this.coordAxis.getName();
      this.description = this.coordAxis.getDescription();
      this.isNumeric = this.coordAxis.isNumeric();

      this.values = new ArrayList<String>();
      for ( int i = 0; i < this.coordAxis.getSize(); i++ )
        this.values.add( this.coordAxis.getCoordName( i ).trim() );
    }

    CoordinateAxis1D getCoordAxis() { return coordAxis; }

    public String getName() { return this.name; }
    public String getLabel() { return this.label; }
    public String getDescription() { return this.description; }
    public boolean isNumeric() { return isNumeric; }
    public List<String> getValues() { return Collections.unmodifiableList( this.values); }
  }

  public static class RangeAxisSubset
  {
    private double min, max;
    private int stride;

    public RangeAxisSubset( double minimum, double maximum, int stride )
    {
      if ( minimum > maximum )
      {
        log.error( "RangeAxisSubset(): Minimum <" + minimum + "> is greater than maximum <" + maximum + ">." );
        throw new IllegalArgumentException( "RangeAxisSubset minimum <" + minimum + "> greater than maximum <" + maximum + ">." );
      }
      if ( stride < 1 )
      {
        log.error( "RangeAxisSubset(): stride <" + stride + "> less than one (1 means all points)." );
        throw new IllegalArgumentException( "RangeAxisSubset stride <" + stride + "> less than one (1 means all points)." );
      }
      this.min = minimum;
      this.max = maximum;
      this.stride = stride;
    }

    public double getMinimum() { return min; }
    public double getMaximum() { return max; }
    public int getStride() { return stride; }

    public String toString()
    {
      return "[min=" + min + ",max=" + max + ",stride=" + stride + "]";
    }

    public Range getRange( GridCoordSystem gcs )
            throws InvalidRangeException
    {
      if ( gcs == null )
      {
        log.error( "getRange(): GridCoordSystem must be non-null." );
        throw new IllegalArgumentException( "GridCoordSystem must be non-null." );
      }
      CoordinateAxis1D vertAxis = gcs.getVerticalAxis();
      if ( vertAxis == null )
      {
        log.error( "getRange(): GridCoordSystem must have vertical axis." );
        throw new IllegalArgumentException( "GridCoordSystem must have vertical axis." );
      }
      if ( !vertAxis.isNumeric() )
      {
        log.error( "getRange(): GridCoordSystem must have numeric vertical axis to support min/max range." );
        throw new IllegalArgumentException( "GridCoordSystem must have numeric vertical axis to support min/max range." );
      }
      int minIndex = vertAxis.findCoordElement( min );
      int maxIndex = vertAxis.findCoordElement( max );
      if ( minIndex == -1 || maxIndex == -1 )
      {
        log.error( "getRange(): GridCoordSystem vertical axis does not contain min/max points." );
        throw new IllegalArgumentException( "GridCoordSystem vertical axis does not contain min/max points." );
      }

      if ( vertAxis.getPositive().equalsIgnoreCase( CoordinateAxis.POSITIVE_DOWN ) )
        return new Range( maxIndex, minIndex, stride );
      else
        return new Range( minIndex, maxIndex, stride );
    }
  }
}
