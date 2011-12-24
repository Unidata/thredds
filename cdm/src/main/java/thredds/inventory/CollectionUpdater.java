package thredds.inventory;

import net.jcip.annotations.ThreadSafe;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Date;

/**
 * Handle background tasks that rescan and reset proto, for collections.
 * Singleton, thread safe.
 * Cover for quartz library.
 * Only used in tds/tdm. so why is this in the cdm??
 *
 * @author caron
 * @since Nov 21, 2010
 */
@ThreadSafe
public enum CollectionUpdater {
  INSTANCE;   // Singleton cf Bloch p 18

  static public enum FROM {tds, tdm }

  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CollectionUpdater.class);
  static private final String FC_NAME= "fc";
  static private final long startupWait = 30 * 1000; // 30 secs
  static private boolean disabled = false;

  // could use Spring DI
  private org.quartz.Scheduler scheduler = null;
  private boolean failed = false;

  // debugging only
  public org.quartz.Scheduler getScheduler() {
    return scheduler;
  }

  public void scheduleTasks(FROM from, FeatureCollectionConfig config, CollectionManager manager) {
    if (disabled || failed) return;

    FeatureCollectionConfig.UpdateConfig useConfig = null;
    if (from == FROM.tds) {
      useConfig = config.updateConfig;
    } else if (from == FROM.tdm) {
      useConfig = config.tdmConfig;
    }
    if (useConfig.startup == null && (useConfig.rescan == null) && (config.protoConfig.change == null)) return;

    // dont start a Scheduler thread unless we need it
    synchronized (this) {
      if (!failed && scheduler == null) {
        try {
          scheduler = StdSchedulerFactory.getDefaultScheduler();
          scheduler.start();
        } catch (SchedulerException e) {
          failed = true;
          throw new RuntimeException("quartz scheduler failed to initialize", e);
        }
      }
    }

    // updating the collection Job
    org.quartz.JobDataMap map = new org.quartz.JobDataMap();
    map.put(FC_NAME, manager);
    JobDetail updateJob = JobBuilder.newJob(UpdateCollectionJob.class)
            .withIdentity(config.name, "UpdateCollection")
            .usingJobData(map)
            .build();

    if (useConfig.startup != null) {
      // wait 30 secs to trigger
      Date runTime = new Date(new Date().getTime() + startupWait);
      SimpleTrigger trigger0 = (SimpleTrigger) TriggerBuilder.newTrigger()
        .withIdentity(config.name, "startup")
        .startAt(runTime)
        //.forJob(config.name, "UpdateCollection") // identify job with name, group strings
        .build();

      try {
        scheduler.scheduleJob(updateJob, trigger0);
        logger.info("Schedule startup scan for {} at {}\n" ,config, runTime);
      } catch (SchedulerException e) {
        logger.error("cronExecutor failed to schedule startup Job for "+config, e);
        return;
      }
    }

    if (useConfig.rescan != null) {
      try {
        CronTrigger trigger1 = TriggerBuilder.newTrigger()
            .withIdentity(config.name, "rescan")
            .withSchedule(CronScheduleBuilder.cronSchedule(useConfig.rescan))
            .build();

        scheduler.scheduleJob(updateJob, trigger1);

        /* WTF ?
        if (useConfig.startup != null) {
          trigger1.setJobName(updateJob.getName());
          trigger1.setJobGroup(updateJob.getGroup());
          scheduler.scheduleJob(trigger1);

        } else {
          scheduler.scheduleJob(updateJob, trigger1);
        } */

        logger.info("Schedule recurring scan for {} cronExpr={}", config.spec, useConfig.rescan);

      } catch (SchedulerException e) {
        logger.error("cronExecutor failed to schedule cron Job", e);
        // e.printStackTrace();
      }
    }

    // updating the proto dataset
    FeatureCollectionConfig.ProtoConfig pconfig = config.protoConfig;
    if (pconfig.change != null) {
      org.quartz.JobDataMap pmap = new org.quartz.JobDataMap();
      map.put(FC_NAME, manager);
      JobDetail protoJob = JobBuilder.newJob(ChangeProtoJob.class)
              .withIdentity(config.name, "UpdateProto")
              .usingJobData(pmap)
              .build();

      try {
        CronTrigger trigger2 = TriggerBuilder.newTrigger()
            .withIdentity(config.name, "rereadProto")
            .withSchedule(CronScheduleBuilder.cronSchedule(pconfig.change))
            .build();
        scheduler.scheduleJob(protoJob, trigger2);
        logger.info("Schedule Reread Proto for {}", config.name);

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
        logger.info("Update rescan for {}", manager.getCollectionName());
        manager.scan();
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
        logger.info("Update resetProto for {}", manager.getCollectionName());
        manager.resetProto();
      } catch (Throwable e) {
        logger.error("RereadProtoJob failed", e);
      }
    }
  }
}
