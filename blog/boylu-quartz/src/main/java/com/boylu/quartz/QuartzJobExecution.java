package com.boylu.quartz;

import com.boylu.entity.SysJob;
import com.boylu.utils.JobInvokeUtils;
import org.quartz.JobExecutionContext;

/**
 * @author: boylu
 * @date 2021/12/8
 * @apiNote 瀹氭椂浠诲姟澶勭悊锛堝厑璁稿苟鍙戞墽琛岋級
 */
public class QuartzJobExecution extends AbstractQuartzJob {
    @Override
    protected void doExecute(JobExecutionContext context, SysJob job) throws Exception {
        JobInvokeUtils.invokeMethod(job);
    }
}
