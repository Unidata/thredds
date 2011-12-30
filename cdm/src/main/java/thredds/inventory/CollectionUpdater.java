package thredds.inventory;

import net.jcip.annotations.ThreadSafe;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

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
  static private final long startupWait = 10 * 1000; // 10 secs
  static private boolean disabled = false;

  // could use Spring DI
  private org.quartz.Scheduler scheduler = null;
  private boolean failed = false;
  private boolean isTdm = false;

  public void setTdm(boolean tdm) {
    isTdm = tdm;
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
      System.out.printf("jobScheduled %s%n", trigger);
    }

    @Override
    public void jobUnscheduled(TriggerKey triggerKey) {
      System.out.printf("jobUnscheduled %s%n", triggerKey);
    }

    @Override
    public void triggerFinalized(Trigger trigger) {
      System.out.printf("triggerFinalized %s%n", trigger);
    }

    @Override
    public void triggerPaused(TriggerKey triggerKey) {
      System.out.printf("triggerPaused %s%n", triggerKey);
    }

    @Override
    public void triggersPaused(String s) {
      System.out.printf("triggersPaused %s%n", s);
    }

    @Override
    public void triggerResumed(TriggerKey triggerKey) {
      System.out.printf("triggerResumed %s%n", triggerKey);
    }

    @Override
    public void triggersResumed(String s) {
      System.out.printf("triggersResumed %s%n", s);
    }

    @Override
    public void jobAdded(JobDetail jobDetail) {
      System.out.printf("jobAdded %s%n", jobDetail);
    }

    @Override
    public void jobDeleted(JobKey jobKey) {
      System.out.printf("jobDeleted %s%n", jobKey);
    }

    @Override
    public void jobPaused(JobKey jobKey) {
      System.out.printf("jobPaused %s%n", jobKey);
    }

    @Override
    public void jobsPaused(String s) {
      System.out.printf("jobPaused %s%n", s);
    }

    @Override
    public void jobResumed(JobKey jobKey) {
      System.out.printf("jobsResumed %s%n", jobKey);
    }

    @Override
    public void jobsResumed(String s) {
      System.out.printf("jobsResumed %s%n", s);
    }

    @Override
    public void schedulerError(String s, SchedulerException e) {
      //To change body of implemented methods use File | Settings | File Templates.
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

    FeatureCollectionConfig.UpdateConfig useConfig = (isTdm) ? config.tdmConfig : config.updateConfig;
    //if (!useConfig.startup && (useConfig.rescan == null) && (config.protoConfig.change == null))
    //  return;

    // Job to update the collection
    org.quartz.JobDataMap map = new org.quartz.JobDataMap();
    map.put(DCM_NAME, manager);
    JobDetail updateJob = JobBuilder.newJob(UpdateCollectionJob.class)
            .withIdentity(config.name, "UpdateCollection")
            .storeDurably()
            .usingJobData(map)
            .build();

    try {
      scheduler.addJob(updateJob, false);
    } catch (SchedulerException e) {
      logger.error("cronExecutor failed to schedule startup Job for " + config, e);
      return;
    }

    if (useConfig.startup) {
      // wait 30 secs to trigger
      Date runTime = new Date(new Date().getTime() + startupWait);
      SimpleTrigger startupTrigger = (SimpleTrigger) TriggerBuilder.newTrigger()
              .withIdentity(config.name, "startup")
              .startAt(runTime)
              .forJob(updateJob)
              .build();

      try {
        scheduler.scheduleJob(startupTrigger);
        logger.info("Schedule startup scan for {} at {}\n", config, runTime);
      } catch (SchedulerException e) {
        logger.error("cronExecutor failed to schedule startup Job for " + config, e);
        return;
      }
    }

    if (useConfig.rescan != null) {
        CronTrigger rescanTrigger = TriggerBuilder.newTrigger()
                .withIdentity(config.name, "rescan")
                .withSchedule(CronScheduleBuilder.cronSchedule(useConfig.rescan))
                .forJob(updateJob)
                .build();

      try {
        scheduler.scheduleJob(rescanTrigger);
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
      map.put(DCM_NAME, manager);
      JobDetail protoJob = JobBuilder.newJob(ChangeProtoJob.class)
              .withIdentity(config.name, "UpdateProto")
              .usingJobData(pmap)
              .storeDurably()
              .build();

      try {
        CronTrigger protoTrigger = TriggerBuilder.newTrigger()
                .withIdentity(config.name, "rereadProto")
                .withSchedule(CronScheduleBuilder.cronSchedule(pconfig.change))
                .build();
        scheduler.scheduleJob(protoJob, protoTrigger);
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
      scheduler.shutdown(true);
      org.slf4j.Logger logServerStartup = org.slf4j.LoggerFactory.getLogger("serverStartup");
      logServerStartup.info("Scheduler shutdown");
    } catch (SchedulerException e) {
      logger.error("Scheduler failed to shutdown", e);
      scheduler = null;
      //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  public void triggerUpdate(String collectionName, String triggerType) {
    Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(collectionName, triggerType)
            .forJob(collectionName, "UpdateCollection") // ??
            .startNow()
            .build();

    try {
      logger.info("Trigger Update for {}", collectionName);
      scheduler.scheduleJob(trigger);
    } catch (SchedulerException e) {
      logger.error("cronExecutor failed to schedule RereadProtoJob", e);
      // e.printStackTrace();
    }
  }

  public static class UpdateCollectionJob implements org.quartz.Job {
    public UpdateCollectionJob() {
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
      try {
        CollectionManager manager = (CollectionManager) context.getJobDetail().getJobDataMap().get(DCM_NAME);
        logger.debug("Update for {} trigger = {}", manager.getCollectionName(), context.getJobDetail().getKey());
        System.out.printf("%s%n", context.getTrigger().getKey());
        String groupName = context.getTrigger().getKey().getGroup();
        if (groupName.equals("nocheck"))
          manager.updateNocheck();
        else
          manager.scan(true);
      } catch (Throwable e) {
        logger.error("InitFmrcJob failed", e);
      }
    }
  }

  public static class ChangeProtoJob implements org.quartz.Job {
    public ChangeProtoJob() {
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
      try {
        CollectionManager manager = (CollectionManager) context.getJobDetail().getJobDataMap().get(DCM_NAME);
        logger.debug("Update resetProto for {}", manager.getCollectionName());
        manager.resetProto();
      } catch (Throwable e) {
        logger.error("RereadProtoJob failed", e);
      }
    }
  }
}
