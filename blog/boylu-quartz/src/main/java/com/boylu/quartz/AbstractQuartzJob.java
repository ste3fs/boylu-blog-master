package com.boylu.quartz;

import com.boylu.common.Constants;
import com.boylu.entity.SysJob;
import com.boylu.entity.SysJobLog;
import com.boylu.mapper.SysJobLogMapper;
import com.boylu.utils.SpringUtil;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Base quartz job implementation.
 *
 * @author: boylu
 * @date 2021/12/8
 */
public abstract class AbstractQuartzJob implements org.quartz.Job {
    private static final Logger log = LoggerFactory.getLogger(AbstractQuartzJob.class);

    private static final ThreadLocal<LocalDateTime> THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public void execute(JobExecutionContext context) {
        SysJob job = new SysJob();
        BeanUtils.copyProperties(context.getMergedJobDataMap().get(ScheduleConstants.TASK_PROPERTIES), job);
        try {
            before(context, job);
            doExecute(context, job);
            after(job, null);
        } catch (Exception e) {
            log.error("Task execution failed", e);
            after(job, e);
        }
    }

    protected void before(JobExecutionContext context, SysJob job) {
        THREAD_LOCAL.set(LocalDateTime.now());
    }

    protected void after(SysJob job, Exception e) {
        LocalDateTime startTime = THREAD_LOCAL.get();
        THREAD_LOCAL.remove();
        if (startTime == null || job == null || StringUtils.contains(job.getInvokeTarget(), "redisTimer")) {
            return;
        }

        SysJobLog jobLog = new SysJobLog();
        jobLog.setJobId(job.getJobId());
        jobLog.setJobName(job.getJobName());
        jobLog.setJobGroup(job.getJobGroup());
        jobLog.setInvokeTarget(job.getInvokeTarget());
        jobLog.setStartTime(startTime);
        jobLog.setStopTime(LocalDateTime.now());

        long runMs = Duration.between(startTime, jobLog.getStopTime()).toMillis();
        if (e == null) {
            jobLog.setStatus(Constants.NO);
            jobLog.setJobMessage(jobLog.getJobName() + " completed in " + runMs + " ms");
        } else {
            jobLog.setStatus(Constants.YES);
            jobLog.setJobMessage(jobLog.getJobName() + " failed after " + runMs + " ms");

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw, true));
            jobLog.setExceptionInfo(StringUtils.substring(sw.toString(), 0, 2000));
        }

        SpringUtil.getBean(SysJobLogMapper.class).insert(jobLog);
    }

    protected abstract void doExecute(JobExecutionContext context, SysJob job) throws Exception;
}
