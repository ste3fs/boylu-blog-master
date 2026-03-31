package com.mojian.quartz;

import com.mojian.entity.SysJob;
import com.mojian.utils.JobInvokeUtils;
import org.quartz.JobExecutionContext;

/**
 * @author: boylu
 * @date 2021/12/8
 * @apiNote 瀹氭椂浠诲姟澶勭悊锛堢姝㈠苟鍙戞墽琛岋級
 */
public class QuartzDisallowConcurrentExecution extends AbstractQuartzJob {
    @Override
    protected void doExecute(JobExecutionContext context, SysJob job) throws Exception {
        JobInvokeUtils.invokeMethod(job);
    }
}
