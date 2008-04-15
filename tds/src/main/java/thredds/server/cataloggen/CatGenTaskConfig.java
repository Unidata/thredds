package thredds.server.cataloggen;

import java.io.File;
import java.net.URL;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatGenTaskConfig
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( CatGenTaskConfig.class );

  private final String name;
  private final String configDocName;
  private final String resultFileName;
  private final int periodInMinutes;
  private final int delayInMinutes;

  private File configDoc = null;
  private URL configDocURL = null;
  private File resultFile = null;

  /**
   * Constructor
   *
   * @param name            - the name of the task.
   * @param configDocName   - the name of the config doc for the task.
   * @param resultFileName  - name of the resulting file
   * @param periodInMinutes - the time in minutes between runs of the task
   * @param delayInMinutes  - the time to wait before the first run of the task
   */
  CatGenTaskConfig( String name,
                   String configDocName,
                   String resultFileName,
                   int periodInMinutes,
                   int delayInMinutes )
  {
    if ( name == null || name.equals( "" ) )
    {
      log.error( "ctor(): The name cannot be null or empty string." );
      throw new IllegalArgumentException( "The name cannot be null or empty string." );
    }
    if ( configDocName == null || configDocName.equals( "" ) )
    {
      log.error( "ctor(): The config doc name cannot be null or empty string." );
      throw new IllegalArgumentException( "The config doc name cannot be null or empty string." );
    }
    if ( resultFileName == null || resultFileName.equals( "" ) )
    {
      log.error( "ctor(): The result file name cannot be null or empty string." );
      throw new IllegalArgumentException( "The result file name cannot be null or empty string." );
    }
    this.name = name;
    this.configDocName = configDocName;
    this.resultFileName = resultFileName;
    this.periodInMinutes = periodInMinutes;
    this.delayInMinutes = delayInMinutes;
  }

  CatGenTaskConfig( CatGenTaskConfig taskConfig )
  {
    this.name = taskConfig.name;
    this.configDocName = taskConfig.configDocName;
    this.resultFileName = taskConfig.resultFileName;
    this.periodInMinutes = taskConfig.periodInMinutes;
    this.delayInMinutes = taskConfig.delayInMinutes;
  }

  public String getName()
  {
    return name;
  }

  public String getConfigDocName()
  {
    return configDocName;
  }

  public String getResultFileName()
  {
    return resultFileName;
  }

  public int getPeriodInMinutes()
  {
    return periodInMinutes;
  }

  public int getDelayInMinutes()
  {
    return delayInMinutes;
  }
}
