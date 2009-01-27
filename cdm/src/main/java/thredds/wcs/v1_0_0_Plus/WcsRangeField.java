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
package thredds.wcs.v1_0_0_Plus;

import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dataset.CoordinateAxis1D;

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
//  private static org.slf4j.Logger log =
//          org.slf4j.LoggerFactory.getLogger( WcsRangeField.class );

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

    // ToDo GeoGrids only handle scalar range fields. (???)
    axes = Collections.emptyList();
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

  public List<Axis> getAxes() { return this.axes; }
  //public List<Axis> getAxes() { return Collections.unmodifiableList( this.axes); }

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
}
