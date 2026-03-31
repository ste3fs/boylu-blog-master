package com.mojian.utils;

import com.mojian.entity.SysJob;
import com.mojian.quartz.QuartzDisallowConcurrentExecution;
import com.mojian.quartz.QuartzJobExecution;
import com.mojian.quartz.ScheduleConstants;
import com.mojian.quartz.TaskException;
import org.quartz.*;

/**
 * @author: boylu
 * @date 2021/12/8
 * @apiNote
 */
public class ScheduleUtils {
    /**
     * 瀵版鍩宷uartz娴犺濮熺猾?     *
     * @param job 閹笛嗩攽鐠佲€冲灊
     * @return 閸忚渹缍嬮幍褑顢戞禒璇插缁?     */
    private static Class<? extends Job> getQuartzJobClass(SysJob job) {
        boolean isConcurrent = "0".equals(job.getConcurrent());
        return isConcurrent ? QuartzJobExecution.class : QuartzDisallowConcurrentExecution.class;
    }

    /**
     * 閺嬪嫬缂撴禒璇插鐟欙箑褰傜€电钖?     */
    public static TriggerKey getTriggerKey(Long jobId, String jobGroup) {
        return TriggerKey.triggerKey(ScheduleConstants.TASK_CLASS_NAME + jobId, jobGroup);
    }

    /**
     * 閺嬪嫬缂撴禒璇插闁款喖顕挒?     */
    public static JobKey getJobKey(Long jobId, String jobGroup) {
        return JobKey.jobKey(ScheduleConstants.TASK_CLASS_NAME + jobId, jobGroup);
    }

    /**
     * 閸掓稑缂撶€规碍妞傛禒璇插
     */
    public static void createScheduleJob(Scheduler scheduler, SysJob job) throws SchedulerException, TaskException {
        Class<? extends Job> jobClass = getQuartzJobClass(job);
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(getJobKey(jobId, jobGroup)).build();

        // 鐞涖劏鎻蹇氱殶鎼达附鐎鍝勬珤
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
        cronScheduleBuilder = handleCronScheduleMisfirePolicy(job, cronScheduleBuilder);

        // 閹稿鏌婇惃鍒onExpression鐞涖劏鎻蹇旂€杞扮娑擃亝鏌婇惃鍓噐igger
        CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(getTriggerKey(jobId, jobGroup))
                .withSchedule(cronScheduleBuilder).build();

        jobDetail.getJobDataMap().put(ScheduleConstants.TASK_PROPERTIES, job);

        if (scheduler.checkExists(getJobKey(jobId, jobGroup))) {
            scheduler.deleteJob(getJobKey(jobId, jobGroup));
        }

        scheduler.scheduleJob(jobDetail, trigger);

        // 閺嗗倸浠犳禒璇插
        if (job.getStatus().equals(ScheduleConstants.Status.PAUSE.getValue())) {
            scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
    }

    /**
     * 鐠佸墽鐤嗙€规碍妞傛禒璇插缁涙牜鏆?     */
    public static CronScheduleBuilder handleCronScheduleMisfirePolicy(SysJob job, CronScheduleBuilder cb)
            throws TaskException {
        switch (job.getMisfirePolicy()) {
            case ScheduleConstants.MISFIRE_DEFAULT:
                return cb;
            case ScheduleConstants.MISFIRE_IGNORE_MISFIRES:
                return cb.withMisfireHandlingInstructionIgnoreMisfires();
            case ScheduleConstants.MISFIRE_FIRE_AND_PROCEED:
                return cb.withMisfireHandlingInstructionFireAndProceed();
            case ScheduleConstants.MISFIRE_DO_NOTHING:
                return cb.withMisfireHandlingInstructionDoNothing();
            default:
                throw new TaskException("The task misfire policy '" + job.getMisfirePolicy()
                        + "' cannot be used in cron schedule tasks", TaskException.Code.CONFIG_ERROR);
        }
    }
}
