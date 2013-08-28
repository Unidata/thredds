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
