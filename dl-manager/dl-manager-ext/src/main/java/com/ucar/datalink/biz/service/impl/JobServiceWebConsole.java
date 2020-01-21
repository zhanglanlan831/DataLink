package com.ucar.datalink.biz.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ucar.datalink.biz.service.JobControlService;
import com.ucar.datalink.biz.service.JobService;
import com.ucar.datalink.biz.utils.URLConnectionUtil;
import com.ucar.datalink.domain.job.DataxCommand;
import com.ucar.datalink.domain.job.JobExecutionInfo;
import com.ucar.datalink.domain.job.JobExecutionState;
import com.ucar.datalink.util.DataxUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by user on 2017/9/21.
 */

@Service("webconsole")
public class JobServiceWebConsole implements JobControlService {

    private static final Logger logger = LoggerFactory.getLogger(JobServiceWebConsole.class);

    private static final long DEFAULT_SLEEP_TIME = 1000;

    @Autowired
    JobService jobService;


    @Override
    public String start(DataxCommand command, Object additional) {
        String result = "failure";
        try {
            String worker = (String)additional;
            String json = JSONObject.toJSONString(command);
            if("-1".equals(worker)) {
                worker = DataxUtil.dynamicChoosenDataxMacheine();
            }

            //发送一个HTTP请求到 DataX服务器
            String address = DataxUtil.startURL(worker);
            result = URLConnectionUtil.retryPOST(address, json);
            if (result != null && result.contains("msg")) {
                return "failure";
            }
            if (!command.isDebug()) {
                int count = 0;
                JobExecutionInfo executionInfo = null;
                while (true) {      //轮询检查数据库，如果状态不等于 UNEXECUTE 则返回
                    if (count > 10) {
                        throw new RuntimeException("job start failure(wait 10s timeout)");
                    }
                    try {
                        Thread.sleep(1000);
                        count++;
                    } catch (InterruptedException e) {
                        result = e.getMessage();
                        logger.error(e.getMessage(),e);
                        break;
                    }
                    executionInfo = jobService.getJobExecutionById( Long.parseLong(result) );
                    //如果获取到的job状态不是 UNEXECUTE 未初始化则返回
                    if( executionInfo!=null && !JobExecutionState.UNEXECUTE.equals(executionInfo.getState()) ) {
                        result = "success";
                        break;
                    }
                }
            }
        } catch(Exception e) {
            result = e.getMessage();
            logger.error(e.getMessage(),e);
        }
        return result;
    }

    @Override
    public List<JobExecutionInfo> history(long id) {
        List<JobExecutionInfo> info = jobService.getJobExecutionByJobId(id);
        return info;
    }


    @Override
    public List<JobExecutionInfo> history(long jobId,long startPage,long count) {
        return jobService.getJobExecutionByJobId(jobId,startPage,count);
    }

    @Override
    public JobExecutionInfo state(long id) {
        return jobService.getJobExecutionById(id);
    }
}