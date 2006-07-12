// $Id$
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

/*
 * $Log: SelectFromRange.java,v $
 * Revision 1.3  2006/01/20 20:42:05  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.2  2005/04/05 22:37:04  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.1  2004/03/05 06:33:40  edavis
 * Classes to handle DQC and user query information.
 *
 */