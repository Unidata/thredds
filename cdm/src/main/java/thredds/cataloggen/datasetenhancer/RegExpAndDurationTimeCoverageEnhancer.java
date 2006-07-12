// $Id$
package thredds.cataloggen.datasetenhancer;

import thredds.cataloggen.DatasetEnhancer;
import thredds.catalog.InvDataset;
import thredds.catalog.InvDatasetImpl;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.datatype.DateRange;
import thredds.datatype.DateType;
import thredds.datatype.TimeDuration;

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

  private String matchPattern;
  private String substitutionPattern;
  private String duration;

  private java.util.regex.Pattern pattern;

  /**
   * Constructor
   *
   * @param matchPattern a regular expression used to match against the CrawlableDataset name.
   * @param substitutionPattern used, after substitution with the regular expression capturing groups, as the start time for a time coverage.
   * @param duration used as the duration string for a time coverage.
   */
  public RegExpAndDurationTimeCoverageEnhancer( String matchPattern, String substitutionPattern, String duration )
  {
    this.matchPattern = matchPattern;
    this.substitutionPattern = substitutionPattern;
    this.duration = duration;

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

    java.util.regex.Matcher matcher = this.pattern.matcher( crDataset.getName() );
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