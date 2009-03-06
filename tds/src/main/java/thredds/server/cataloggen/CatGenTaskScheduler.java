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
  private final File altResultsDir;

  CatGenTaskScheduler( CatGenConfig config, File configDir, File resultDir, File altResultsDir )
  {
    this.config = config;
    this.configDir = configDir;
    this.resultDir = resultDir;
    this.altResultsDir = altResultsDir;
  }
  
  void start()
  {
    for ( CatGenTaskConfig curTask : config.getTaskInfoList())
    {
      if ( curTask.getPeriodInMinutes() > 0
           && ! scheduler.isShutdown() )
      {
        CatGenTaskRunner catGenTaskRunner = new CatGenTaskRunner( curTask, configDir, resultDir, altResultsDir );
        scheduledTasks.add(
                scheduler.scheduleAtFixedRate( catGenTaskRunner,
                                               curTask.getDelayInMinutes(),
                                               curTask.getPeriodInMinutes(),
                                               TimeUnit.MINUTES ) );
      }
    }
  }

  void stop()
  {
    scheduler.shutdown();
  }
}
