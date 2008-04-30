package thredds.server.cataloggen;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.List;
import java.util.ArrayList;
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
  private final List<ScheduledFuture> scheduledTasks = new ArrayList<ScheduledFuture>();

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
      if ( curTask.getPeriodInMinutes() > 0
           && ! scheduler.isShutdown() )
      {
        CatGenTaskRunner catGenTaskRunner = new CatGenTaskRunner( curTask, configDir, resultDir );
        scheduledTasks.add(
                scheduler.scheduleAtFixedRate( catGenTaskRunner,
                                               curTask.getDelayInMinutes() * 60,
                                               curTask.getPeriodInMinutes() * 60,
                                               TimeUnit.SECONDS ) );
      }
    }
  }

  void stop()
  {
    scheduler.shutdown();
  }
}
