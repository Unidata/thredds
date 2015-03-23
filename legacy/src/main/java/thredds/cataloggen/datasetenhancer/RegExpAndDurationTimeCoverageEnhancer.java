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
// $Id:RegExpAndDurationTimeCoverageEnhancer.java 63 2006-07-12 21:50:51Z edavis $
package thredds.cataloggen.datasetenhancer;

import thredds.cataloggen.DatasetEnhancer;
import thredds.catalog.InvDataset;
import thredds.catalog.InvDatasetImpl;
import thredds.crawlabledataset.CrawlableDataset;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;

/**
 * Try to add timeCoverage to the InvDataset using a regular expression match,
 * capturing group replacement, and a duration string.
 * </p><ol>
 * <li> Look for a match to the given matchPattern in the CrawlableDataset name.</li>
 * <li> Substitute all replacment patterns ("$n") in the substitionPattern with the capturing groups from the regular expression match.</li>
 * <li> Use the string obtained from the substitution as the start time and the given duration string to form a timeCoverage element for the InvDataset.</li>
 * </ol>
 *
 * @author edavis
 * @since Dec 6, 2005 10:59:21 AM
 */
public class RegExpAndDurationTimeCoverageEnhancer implements DatasetEnhancer
{
  static private org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( RegExpAndDurationTimeCoverageEnhancer.class );

  private final String matchPattern;
  private final String substitutionPattern;
  private final String duration;

  private final MatchTarget matchTarget;
  private enum MatchTarget
  {
    DATASET_NAME,
    DATASET_PATH
  }

  private java.util.regex.Pattern pattern;

  /**
   * Factory method that returns a RegExpAndDurationTimeCoverageEnhancer instance
   * that will apply the match pattern to the dataset name.
   *
   * @param matchPattern a regular expression used to match against the CrawlableDataset name.
   * @param substitutionPattern the time coverage start time (which may contain regular expression capturing group substitution strings).
   * @param duration the time coverage duration string.
   *
   * @return a RegExpAndDurationTimeCoverageEnhancer that will apply the match pattern to the dataset name.
   */
  public static
  RegExpAndDurationTimeCoverageEnhancer
  getInstanceToMatchOnDatasetName( String matchPattern,
                                   String substitutionPattern,
                                   String duration )
  {
    return new RegExpAndDurationTimeCoverageEnhancer(
            matchPattern, substitutionPattern,
            duration, MatchTarget.DATASET_NAME );
  }

  /**
   * Factory method that returns a RegExpAndDurationTimeCoverageEnhancer instance
   * that will apply the match pattern to the dataset path.
   *
   * @param matchPattern a regular expression used to match against the CrawlableDataset path.
   * @param substitutionPattern the time coverage start time (which may contain regular expression capturing group substitution strings).
   * @param duration the time coverage duration string.
   *
   * @return a RegExpAndDurationTimeCoverageEnhancer that will apply the match pattern to the dataset path.
   */
  public static RegExpAndDurationTimeCoverageEnhancer
  getInstanceToMatchOnDatasetPath( String matchPattern,
                                   String substitutionPattern,
                                   String duration )
  {
    return new RegExpAndDurationTimeCoverageEnhancer(
            matchPattern, substitutionPattern,
            duration, MatchTarget.DATASET_PATH );
  }

  /**
   * Constructor
   *
   * @param matchPattern a regular expression used to match against the CrawlableDataset name.
   * @param substitutionPattern used, after substitution with the regular expression capturing groups, as the start time for a time coverage.
   * @param duration used as the duration string for a time coverage.
   * @param matchTarget indicates what information to match on (e.g., the dataset path).
   */
  private RegExpAndDurationTimeCoverageEnhancer( String matchPattern,
                                                String substitutionPattern,
                                                String duration,
                                                MatchTarget matchTarget )
  {
    if ( matchPattern == null ) throw new IllegalArgumentException( "Null match pattern not allowed.");
    if ( substitutionPattern == null ) throw new IllegalArgumentException( "Null substitution pattern not allowed.");
    if ( duration == null ) throw new IllegalArgumentException( "Null duration not allowed.");
    if ( matchTarget == null ) throw new IllegalArgumentException( "Null match target not allowed.");

    this.matchPattern = matchPattern;
    this.substitutionPattern = substitutionPattern;
    this.duration = duration;
    this.matchTarget = matchTarget;

    try
    {
      this.pattern = java.util.regex.Pattern.compile( matchPattern );
    }
    catch ( java.util.regex.PatternSyntaxException e )
    {
      log.error( "ctor(): bad match pattern <" + this.matchPattern + ">, failed to compile: " +e.getMessage() );
      this.pattern = null;
    }
  }

  public MatchTarget getMatchTarget() { return this.matchTarget; }
  public String getMatchPattern() { return matchPattern; }
  public String getSubstitutionPattern() { return substitutionPattern; }
  public String getDuration() { return duration; }

  public Object getConfigObject() { return null; }

  public boolean addMetadata( InvDataset dataset, CrawlableDataset crDataset )
  {
    if ( this.pattern == null )
    {
      log.error( "addMetadata(): bad match pattern <" + this.matchPattern + ">." );
      return false;
    }

    String matchTargetString;
    if ( this.matchTarget.equals( MatchTarget.DATASET_NAME ))
      matchTargetString = crDataset.getName();
    else if ( this.matchTarget.equals( MatchTarget.DATASET_PATH ))
      matchTargetString = crDataset.getPath();
    else
      throw new IllegalStateException( "Unknown match target [" + this.matchTarget.toString() + "].");

    java.util.regex.Matcher matcher = this.pattern.matcher( matchTargetString );
    if ( ! matcher.find() )
    {
      return ( false ); // Pattern not found.
    }
    StringBuffer startTime = new StringBuffer();
    try
    {
      matcher.appendReplacement( startTime, this.substitutionPattern );
    }
    catch ( IndexOutOfBoundsException e )
    {
      log.error( "addMetadata(): capture group mismatch between match pattern <" + this.matchPattern + "> and substitution pattern <" + this.substitutionPattern + ">: " + e.getMessage());
      return( false);
    }
    startTime.delete( 0, matcher.start());

    try
    {
      ( (InvDatasetImpl) dataset ).setTimeCoverage(
              new DateRange( new DateType( startTime.toString(), null, null ), null,
                             new TimeDuration( this.duration ), null ) );
    }
    catch ( Exception e )
    {
      log.warn( "addMetadata(): Start time <" + startTime.toString() + "> or duration <" + this.duration + "> not parsable" +
                " (crDataset.getName() <" + crDataset.getName() + ">, this.matchPattern() <" + this.matchPattern + ">, this.substitutionPattern() <" + this.substitutionPattern + ">): " + e.getMessage() );
      return ( false );
    }

    return ( true );
  }
}