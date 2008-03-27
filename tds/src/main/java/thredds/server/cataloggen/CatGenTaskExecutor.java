package thredds.server.cataloggen;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatGenTaskExecutor
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( CatGenTaskExecutor.class );

  private final ScheduledExecutorService exec = Executors.newScheduledThreadPool( 1 );

  void start()
  {
    //exec.
  }
}
