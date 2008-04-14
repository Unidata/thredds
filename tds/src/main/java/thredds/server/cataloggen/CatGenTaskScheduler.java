package thredds.server.cataloggen;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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
  CatGenTaskScheduler( CatGenConfig config)
  {
    this.config = config;
  }
  
  void start()
  {
    //exec.
  }

  void stop()
  {
    scheduler.shutdown();
  }
}
