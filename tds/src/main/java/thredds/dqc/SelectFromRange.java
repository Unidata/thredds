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
// $Id: SelectFromRange.java 63 2006-07-12 21:50:51Z edavis $
package thredds.dqc;

/**
 * A description
 *
 * User: edavis
 * Date: Jan 22, 2004
 * Time: 8:40:05 PM
 */
public class SelectFromRange extends Selector
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( SelectFromRange.class );

  /** Public constructor. */
  public SelectFromRange() {}

  protected double allowedRangeMin = 0.0;
  protected double allowedRangeMax = 0.0;
  protected String units = null;
  protected String template = null;
  protected boolean modulo = false;
  protected double moduloValue = 0.0;

  public double getAllowedRangeMin() { return allowedRangeMin; }
  public double getAllowedRangeMax() { return allowedRangeMax; }

  /**
   * Set the allowed range.
   *
   * @param min - minimum point in the allowed range.
   * @param max - maximum point in the allowed range.
   * @throws IllegalArgumentException if min is greater than max.
   */
  public void setAllowedRange( double min, double max )
  {
    if ( min > max)
    {
      String tmp = "setAllowedRange(): the low end of the allowed range <" + min +
              "> is greater than the high end of the allowed range <" + max + ">.";
      log.debug( tmp);
      throw( new IllegalArgumentException( tmp));
    }
    this.allowedRangeMin = min;
    this.allowedRangeMax = max;
    this.moduloValue = this.allowedRangeMax - this.allowedRangeMin;
    log.debug( "setAllowedRange(): set min <" + min + ">, max <" + max + ">, and modulo value <" + this.moduloValue + ">.");
  }


  public String getUnits() { return units; }
  public void setUnits( String units ) { this.units = units; }

  public String getTemplate() { return template; }
  public void setTemplate( String template ) { this.template = template; }

  public boolean isModulo() { return modulo; }
  public void setModulo( boolean modulo ) { this.modulo = modulo; }

  public Selection validateSelection( Selection selection )
  {
    return( null);
//    // Check that the given Selection is a valid RequestedRange.
//    //if ( ! ( selection instanceof RequestedRange ) )
//    {
//
//    }
//
//    // Check that the given Selection is valid acording to this SelectFromRange.
//
//    // Deal with modulo stuff.
//    if (this.isModulo())
//    {
//      //
//    }
//    else
//    {
//
//    }
//    return null;
  }
}
