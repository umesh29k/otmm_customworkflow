package com.opentext.custom.job.step;

import com.artesia.common.exception.BaseTeamsException;
import com.artesia.common.net.HostnameUtils;
import com.artesia.security.SecuritySession;
import com.google.gson.Gson;
import com.opentext.custom.util.OTUtils;
import com.opentext.job.JobContext;
import com.opentext.job.context.ListContextWrapper;
import com.opentext.server.job.step.AbstractBaseStep;
import com.opentext.server.job.step.JobData;
import com.opentext.server.job.step.StepStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class UpdateRendition extends AbstractBaseStep {
    private static final Log log = LogFactory.getLog(UpdateRendition.class);

    public StepStatus executeBaseStep(JobData jobData, JobContext jobContext, SecuritySession securitySession) throws BaseTeamsException {
        log.trace(">>update_rendition_step execution start");
        List<Object> ids = ((ListContextWrapper) getDataFromContext("{assetIds}", jobContext)).getListData();
        log.info(">>> update_rendition_step - IDs: " + ids);
        log.info("update_rendition_step hostname: " + HostnameUtils.getHostname());
        if (ids != null) {
            ids.forEach(id -> {
                log.info("update_rendition -> identified asset id: " + id.toString());
                OTUtils.updateRendition(id.toString());
                log.info("update_rendition_step -> asset-ids" + new Gson().toJson(id.toString()));
            });
        }
        log.trace("<<update_rendition_step execution end");
        return StepStatus.COMPLETED;
    }

}
