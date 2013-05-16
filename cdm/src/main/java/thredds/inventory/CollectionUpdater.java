package thredds.inventory;

import net.jcip.annotations.ThreadSafe;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import thredds.featurecollection.FeatureCollectionConfig;

import java.util.Date;

/**
 * Handle background tasks for updating collections.
 * Singleton, thread safe.
 * Cover for quartz library.
 * Only used in tds/tdm.
 *
 * @author caron
 * @since Nov 21, 2010
 */
@ThreadSafe
public enum CollectionUpdater {
  INSTANCE;   // Singleton cf Bloch p 18

  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CollectionUpdater.class);
  static private final String DCM_NAME = "dcm";
  static private final String LOGGER = "logger";
  static private final long startupWait = 10 * 1000; // 10 secs
  static private boolean disabled = false;

  // could use Spring DI
  private org.quartz.Scheduler scheduler = null;
  private boolean failed = false;
  private boolean isTdm = false;

  public void setTdm(boolean tdm) {
    isTdm = tdm;
  }

  public boolean isTdm() {
    return isTdm;
  }

  private CollectionUpdater() {
    try {
      scheduler = StdSchedulerFactory.getDefaultScheduler();
      scheduler.start();
      scheduler.getListenerManager().addSchedulerListener(new MySchedListener());
    } catch (SchedulerException e) {
      failed = true;
      throw new RuntimeException("quartz scheduler failed to initialize", e);
    }
  }

  public org.quartz.Scheduler getScheduler() {
    return scheduler;
  }

  private class MySchedListener implements SchedulerListener {

    @Override
    public void jobScheduled(Trigger trigger) {
      logger.debug("jobScheduled {}", trigger);
    }

    @Override
    public void jobUnscheduled(TriggerKey triggerKey) {
      logger.debug("jobUnscheduled {}", triggerKey);
    }

    @Override
    public void triggerFinalized(Trigger trigger) {
      logger.debug("triggerFinalized {}", trigger);
    }

    @Override
    public void triggerPaused(TriggerKey triggerKey) {
      logger.debug("triggerPaused {}", triggerKey);
    }

    @Override
    public void triggersPaused(String s) {
      logger.debug("triggersPaused {}", s);
    }

    @Override
    public void triggerResumed(TriggerKey triggerKey) {
      logger.debug("triggerResumed {}", triggerKey);
    }

    @Override
    public void triggersResumed(String s) {
      logger.debug("triggersResumed {}", s);
    }

    @Override
    public void jobAdded(JobDetail jobDetail) {
      logger.debug("jobAdded {}", jobDetail);
    }

    @Override
    public void jobDeleted(JobKey jobKey) {
      logger.debug("jobDeleted {}", jobKey);
    }

    @Override
    public void jobPaused(JobKey jobKey) {
      logger.debug("jobPaused {}", jobKey);
    }

    @Override
    public void jobsPaused(String s) {
      logger.debug("jobPaused {}", s);
    }

    @Override
    public void jobResumed(JobKey jobKey) {
      logger.debug("jobsResumed {}", jobKey);
    }

    @Override
    public void jobsResumed(String s) {
      logger.debug("jobsResumed {}", s);
    }

    @Override
    public void schedulerError(String s, SchedulerException e) {
      logger.debug("schedulerError {} {}", s, e);
    }

    @Override
    public void schedulerInStandbyMode() {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void schedulerStarted() {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void schedulerShutdown() {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void schedulerShuttingdown() {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void schedulingDataCleared() {
      //To change body of implemented methods use File | Settings | File Templates.
    }
  }

  public void scheduleTasks(FeatureCollectionConfig config, CollectionManager manager) {
    if (disabled || failed) return;

    FeatureCollectionConfig.UpdateConfig updateConfig = (isTdm) ? config.tdmConfig : config.updateConfig;
    if (updateConfig == null) return;

    //String jobName = config.name + "-" + Integer.toHexString(config.hashCode());
    String jobName = manager.getCollectionName();

    // Job to update the collection
    org.quartz.JobDataMap map = new org.quartz.JobDataMap();
    map.put(DCM_NAME, manager);
    map.put(LOGGER, org.slf4j.LoggerFactory.getLogger("fc."+manager.getCollectionName()));
    JobDetail updateJob = JobBuilder.newJob(UpdateCollectionJob.class)
            .withIdentity(jobName, "UpdateCollection")
            .storeDurably()
            .usingJobData(map)
            .build();

    try {
      scheduler.addJob(updateJob, false);
    } catch (SchedulerException e) {
      logger.error("cronExecutor failed to schedule startup Job for " + config, e);
      return;
    }

    if (updateConfig.startup) {
      Date runTime = new Date(new Date().getTime() + startupWait); // wait startupWait before trigger
      SimpleTrigger startupTrigger = (SimpleTrigger) TriggerBuilder.newTrigger()
              .withIdentity(jobName, updateConfig.startupForce.toString())
              .startAt(runTime)
              .forJob(updateJob)
              .build();

      try {
        scheduler.scheduleJob(startupTrigger);
        logger.info("Schedule startup scan {} for '{}' at {}", updateConfig.startupForce.toString(), config.name, runTime);
      } catch (SchedulerException e) {
        logger.error("cronExecutor failed to schedule startup Job for " + config, e);
        return;
      }
    }

    if (updateConfig.rescan != null) {
        CronTrigger rescanTrigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName, "rescan")
                .withSchedule(CronScheduleBuilder.cronSchedule(updateConfig.rescan))
                .forJob(updateJob)
                .build();

      try {
        scheduler.scheduleJob(rescanTrigger);
        logger.info("Schedule recurring scan for '{}' cronExpr={}", config.name, updateConfig.rescan);

      } catch (SchedulerException e) {
        logger.error("cronExecutor failed to schedule cron Job", e);
        // e.printStackTrace();
      }
    }

    // updating the proto dataset
    FeatureCollectionConfig.ProtoConfig pconfig = config.protoConfig;
    if (pconfig.change != null) {
      org.quartz.JobDataMap pmap = new org.quartz.JobDataMap();
      pmap.put(DCM_NAME, manager);
      map.put(LOGGER, org.slf4j.LoggerFactory.getLogger("fc."+manager.getCollectionName()));
      JobDetail protoJob = JobBuilder.newJob(ChangeProtoJob.class)
              .withIdentity(jobName, "UpdateProto")
              .usingJobData(pmap)
              .storeDurably()
              .build();

      try {
        CronTrigger protoTrigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName, "rereadProto")
                .withSchedule(CronScheduleBuilder.cronSchedule(pconfig.change))
                .build();
        scheduler.scheduleJob(protoJob, protoTrigger);
        logger.info("Schedule proto update for '{}' cronExpr={}", config.name, pconfig.change);

      } catch (SchedulerException e) {
        logger.error("cronExecutor failed to schedule RereadProtoJob", e);
        // e.printStackTrace();
      }
    }

  }

  public void shutdown() {
    if (scheduler == null) return;
    try {
      scheduler.shutdown(true);
      org.slf4j.Logger logServerStartup = org.slf4j.LoggerFactory.getLogger("serverStartup");
      logServerStartup.info("Scheduler shutdown");
    } catch (SchedulerException e) {
      logger.error("Scheduler failed to shutdown", e);
      scheduler = null;
      //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  // Called by TDS collectionController when trigger is received externally
  public void triggerUpdate(String collectionName, String triggerType) {
    Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(collectionName+"-trigger", triggerType)
            .forJob(collectionName, "UpdateCollection") // ??
            .startNow()
            .build();

    try {
      logger.debug("Trigger Update for {} type= {}", collectionName, triggerType);
      scheduler.scheduleJob(trigger);
    } catch (SchedulerException e) {
      logger.error("triggerUpdate failed", e);
      // e.printStackTrace();
    }
  }

  public static class UpdateCollectionJob implements org.quartz.Job {
    public UpdateCollectionJob() {
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
      try {
        CollectionManager manager = (CollectionManager) context.getJobDetail().getJobDataMap().get(DCM_NAME);
        org.slf4j.Logger loggerfc = (org.slf4j.Logger) context.getJobDetail().getJobDataMap().get(LOGGER);
        String groupName = context.getTrigger().getKey().getGroup();
        if (groupName.equals("nocheck")) {
          loggerfc.info("UpdateCollection {} nocheck", manager.getCollectionName());
          manager.updateNocheck(); // update(CollectionManager.Force.nocheck)
        } else {
          loggerfc.debug("UpdateCollection {} scan(true)", manager.getCollectionName());
          manager.scan(true);
        }
      } catch (Throwable e) {
        logger.error("UpdateCollectionJob.execute failed", e);
      }
    }
  }

  public static class ChangeProtoJob implements org.quartz.Job {
    public ChangeProtoJob() {
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
      try {
        CollectionManager manager = (CollectionManager) context.getJobDetail().getJobDataMap().get(DCM_NAME);
        org.slf4j.Logger loggerfc = (org.slf4j.Logger) context.getJobDetail().getJobDataMap().get(LOGGER);
        if (manager == null) {
          loggerfc.error("Update resetProto failed: no manager object on {}", context);
          return;
        }
        logger.info("ResetProto for {}", manager.getCollectionName());
        manager.resetProto();
      } catch (Throwable e) {
        logger.error("ChangeProtoJob.execute failed", e);
      }
    }
  }
}
