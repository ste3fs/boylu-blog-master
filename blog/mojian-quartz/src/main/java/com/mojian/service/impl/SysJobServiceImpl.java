package com.mojian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mojian.entity.SysJob;
import com.mojian.exception.ServiceException;
import com.mojian.mapper.SysJobMapper;
import com.mojian.quartz.ScheduleConstants;
import com.mojian.quartz.TaskException;
import com.mojian.service.SysJobService;
import com.mojian.utils.CronUtils;
import com.mojian.utils.PageUtil;
import com.mojian.utils.ScheduleUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;

/**
 * Quartz job service implementation.
 *
 * @author: boylu
 * @since 2021-12-08
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SysJobServiceImpl extends ServiceImpl<SysJobMapper, SysJob> implements SysJobService {

    private final Scheduler scheduler;

    @PostConstruct
    public void init() throws SchedulerException, TaskException {
        scheduler.clear();
        List<SysJob> jobList = baseMapper.selectList(null);
        for (SysJob job : jobList) {
            ScheduleUtils.createScheduleJob(scheduler, job);
        }
    }

    @Override
    public Page<SysJob> selectJobPage(String jobName, String jobGroup, String status) {
        LambdaQueryWrapper<SysJob> queryWrapper = new LambdaQueryWrapper<SysJob>()
                .like(StringUtils.isNotBlank(jobName), SysJob::getJobName, jobName)
                .eq(StringUtils.isNotBlank(jobGroup), SysJob::getJobGroup, jobGroup)
                .eq(StringUtils.isNotBlank(status), SysJob::getStatus, status);

        return page(PageUtil.getPage(), queryWrapper);
    }

    @Override
    public SysJob selectJobById(Long jobId) {
        SysJob job = baseMapper.selectById(jobId);
        Date nextExecution = CronUtils.getNextExecution(job.getCronExpression());
        job.setNextValidTime(nextExecution);
        return job;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysJob addJob(SysJob job) throws SchedulerException, TaskException {
        checkCronIsValid(job);

        baseMapper.insert(job);
        ScheduleUtils.createScheduleJob(scheduler, job);
        return job;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysJob updateJob(SysJob job) throws SchedulerException, TaskException {
        checkCronIsValid(job);

        SysJob properties = baseMapper.selectById(job.getJobId());
        baseMapper.updateById(job);
        updateSchedulerJob(job, properties.getJobGroup());

        return job;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteJob(List<Long> ids) {
        for (Long jobId : ids) {
            SysJob job = baseMapper.selectById(jobId);
            if (job == null) {
                continue;
            }
            try {
                scheduler.deleteJob(ScheduleUtils.getJobKey(job.getJobId(), job.getJobGroup()));
            } catch (SchedulerException e) {
                throw new ServiceException("Failed to delete scheduled job");
            }
        }
        baseMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pauseJob(SysJob job) throws SchedulerException {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        job.setStatus(ScheduleConstants.Status.PAUSE.getValue());
        baseMapper.updateById(job);
        scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void runJob(SysJob job) {
        try {
            Long jobId = job.getJobId();
            String jobGroup = job.getJobGroup();
            JobDataMap dataMap = new JobDataMap();
            dataMap.put(ScheduleConstants.TASK_PROPERTIES, job);
            scheduler.triggerJob(ScheduleUtils.getJobKey(jobId, jobGroup), dataMap);
        } catch (Exception e) {
            throw new ServiceException("Failed to execute scheduled job: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(SysJob job) throws SchedulerException {
        String status = job.getStatus();
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        int row = baseMapper.updateById(job);
        if (row > 0) {
            if (ScheduleConstants.Status.NORMAL.getValue().equals(status)) {
                scheduler.resumeJob(ScheduleUtils.getJobKey(jobId, jobGroup));
            } else if (ScheduleConstants.Status.PAUSE.getValue().equals(status)) {
                scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
            }
        }
    }

    private void checkCronIsValid(SysJob job) {
        boolean valid = CronUtils.isValid(job.getCronExpression());
        if (!valid) {
            throw new ServiceException("Invalid cron expression");
        }
    }

    public void updateSchedulerJob(SysJob job, String jobGroup) throws SchedulerException, TaskException {
        Long jobId = job.getJobId();
        JobKey jobKey = ScheduleUtils.getJobKey(jobId, jobGroup);
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
        ScheduleUtils.createScheduleJob(scheduler, job);
    }
}
