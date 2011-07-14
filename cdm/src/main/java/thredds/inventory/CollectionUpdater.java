package thredds.inventory;

import net.jcip.annotations.ThreadSafe;
import org.quartz.*;

import java.text.ParseException;
import java.util.Date;

/**
 * Handle background tasks that rescan and reset proto, for collections.
 * Singleton, thread safe.
 *
 * @author caron
 * @since Nov 21, 2010
 */
@ThreadSafe
public enum CollectionUpdater {
  INSTANCE;   // cf Bloch p 18

  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CollectionUpdater.class);
  static private final String FC_NAME= "fc";
  static private final long startupWait = 30 * 1000; // 30 secs

  // could use Spring DI
  private org.quartz.Scheduler scheduler = null;
  private boolean failed = false;

  // debugging only
  public org.quartz.Scheduler getScheduler() {
    return scheduler;
  }

  public void scheduleTasks(FeatureCollectionConfig config, CollectionManager manager) {
    if (failed) return;

    FeatureCollectionConfig.UpdateConfig update = config.updateConfig;
    if (!update.startup && (update.rescan == null) && (config.protoConfig.change == null)) return;

    // dont start a Scheduler thread unless we need it
    synchronized (this) {
      if (!failed && scheduler == null) {
        SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
        try {
          scheduler = schedFact.getScheduler();
          scheduler.start();
        } catch (SchedulerException e) {
          failed = true;
          throw new RuntimeException("quartz scheduler failed to initialize", e);
        }
      }
    }

    // updating the collection
    JobDetail updateJob = new JobDetail(config.name, "UpdateCollection", UpdateCollectionJob.class);
    org.quartz.JobDataMap map = new org.quartz.JobDataMap();
    map.put(FC_NAME, manager);
    updateJob.setJobDataMap(map);

    if (update.startup) {
      // wait 30 secs to trigger
      Date runTime = new Date(new Date().getTime() + startupWait);
      Trigger trigger0 = new SimpleTrigger(config.name, "startup", runTime);
      try {
        scheduler.scheduleJob(updateJob, trigger0);
        logger.info("Schedule startup scan for {} at {}\n" ,config, runTime);
      } catch (SchedulerException e) {
        logger.error("cronExecutor failed to schedule startup Job for "+config, e);
        return;
      }
    }

    if (update.rescan != null) {
      try {
        Trigger trigger1 = new CronTrigger(config.name, "rescan", update.rescan);
        if (update.startup) {
          trigger1.setJobName(updateJob.getName());
          trigger1.setJobGroup(updateJob.getGroup());
          scheduler.scheduleJob(trigger1);
        } else {
          scheduler.scheduleJob(updateJob, trigger1);
        }
        logger.info("Schedule recurring scan for {} cronExpr={}\n", config.name, update.rescan);
      } catch (ParseException e) {
        logger.error("cronExecutor failed: bad cron expression= "+ update.rescan, e);
      } catch (SchedulerException e) {
        logger.error("cronExecutor failed to schedule cron Job", e);
        // e.printStackTrace();
      }
    }

    // updating the proto dataset
    FeatureCollectionConfig.ProtoConfig pconfig = config.protoConfig;
    if (pconfig.change != null) {
      JobDetail protoJob = new JobDetail(config.name, "UpdateProto", ChangeProtoJob.class);
      org.quartz.JobDataMap pmap = new org.quartz.JobDataMap();
      pmap.put(FC_NAME, manager);
      protoJob.setJobDataMap(pmap);

      try {
        Trigger trigger2 = new CronTrigger(config.name, "rereadProto", pconfig.change);
        scheduler.scheduleJob(protoJob, trigger2);
        logger.info("Schedule Reread Proto for {}", config.name);
      } catch (ParseException e) {
        logger.error("cronExecutor failed: RereadProto has bad cron expression= "+ pconfig.change, e);
      } catch (SchedulerException e) {
        logger.error("cronExecutor failed to schedule RereadProtoJob", e);
        // e.printStackTrace();
      }
    }

  }

  public void shutdown() {
    if (scheduler == null) return;
    try {
      scheduler.shutdown( true);
      org.slf4j.Logger logServerStartup = org.slf4j.LoggerFactory.getLogger( "serverStartup" );
      logServerStartup.info( "Scheduler shutdown" );
    } catch (SchedulerException e) {
      logger.error("Scheduler failed to shutdown", e);
      scheduler = null;
      //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  public static class UpdateCollectionJob implements org.quartz.Job {
    public UpdateCollectionJob() {}
    public void execute(JobExecutionContext context) throws JobExecutionException {
      try {
        CollectionManager manager = (CollectionManager) context.getJobDetail().getJobDataMap().get(FC_NAME);
        logger.info("Trigger rescan for "+manager.getCollectionName());
        manager.rescan();
      } catch (Throwable e) {
        logger.error("InitFmrcJob failed", e);
      }
    }
  }

  public static class ChangeProtoJob implements org.quartz.Job {
    public ChangeProtoJob() {}
    public void execute(JobExecutionContext context) throws JobExecutionException {
      try {
        CollectionManager manager = (CollectionManager) context.getJobDetail().getJobDataMap().get(FC_NAME);
        logger.info("Trigger resetProto for "+manager.getCollectionName());
        manager.resetProto();
      } catch (Throwable e) {
        logger.error("RereadProtoJob failed", e);
      }
    }
  }
}
