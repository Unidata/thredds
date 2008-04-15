package thredds.server.cataloggen;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.io.File;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatGenTaskScheduler
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( CatGenTaskScheduler.class );

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool( 1 );

  private final CatGenConfig config;
  private final File configDir;
  private final File resultDir;

  CatGenTaskScheduler( CatGenConfig config, File configDir, File resultDir)
  {
    this.config = config;
    this.configDir = configDir;
    this.resultDir = resultDir;
  }
  
  void start()
  {
    for ( CatGenTaskConfig curTask : config.getTaskInfoList())
    {
      if ( ! scheduler.isShutdown() )
      {
        CatGenTaskRunner catGenTaskRunner = new CatGenTaskRunner( curTask, configDir, resultDir );
        scheduler.execute( catGenTaskRunner );
      }
    }
  }

  void stop()
  {
    scheduler.shutdown();
  }
}
