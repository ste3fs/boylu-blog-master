package com.mojian.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mojian.entity.SysJob;
import com.mojian.quartz.TaskException;
import org.quartz.SchedulerException;

import java.util.List;

/**
 * <p>
 * 瀹氭椂浠诲姟璋冨害琛?鏈嶅姟绫? * </p>
 *
 * @author: boylu
 * @since 2021-12-08
 */
public interface SysJobService extends IService<SysJob> {

    /**
     * 鍒嗛〉
     *
     * @param jobName
     * @param jobGroup
     * @param status
     * @return
     */
    Page<SysJob> selectJobPage(String jobName, String jobGroup, String status);

    /**
     * 璇︽儏
     *
     * @param jobId
     * @return
     */
    SysJob selectJobById(Long jobId);

    /**
     * 娣诲姞
     * @param job
     * @return
     * @throws SchedulerException
     * @throws TaskException
     */
    SysJob addJob(SysJob job) throws SchedulerException, TaskException, TaskException;

    /**
     * 淇敼
     * @param job
     * @return
     * @throws SchedulerException
     * @throws TaskException
     */
    SysJob updateJob(SysJob job) throws SchedulerException, TaskException;

    /**
     * 鍒犻櫎
     * @param ids
     * @return
     */
    void deleteJob(List<Long> ids);


    /**
     * 绔嬪嵆鎵ц
     * @param job
     * @return
     */
    void runJob(SysJob job);

    /**
     * 淇敼鐘舵€?     * @param job
     * @return
     * @throws SchedulerException
     */
    void changeStatus(SysJob job) throws SchedulerException;


    /**
     * 鏆傚仠浠诲姟
     * @param job
     * @return
     */
    void pauseJob(SysJob job) throws SchedulerException;

}
