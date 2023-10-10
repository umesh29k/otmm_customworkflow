package com.opentext.custom.job.step;

import com.artesia.common.exception.BaseTeamsException;
import com.artesia.common.net.HostnameUtils;
import com.artesia.security.SecuritySession;
import com.artesia.transformer.AssetTransformationElement;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opentext.custom.util.Util;
import com.opentext.job.JobContext;
import com.opentext.job.context.ListContextWrapper;
import com.opentext.server.job.step.AbstractBaseStep;
import com.opentext.server.job.step.JobData;
import com.opentext.server.job.step.StepStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.opentext.custom.constant.Constants.ASSET_IDS;
import static com.opentext.custom.constant.Constants.DATA;

public class UpdateRendition extends AbstractBaseStep {
    private static final Log log = LogFactory.getLog(UpdateRendition.class);

    public StepStatus executeBaseStep(JobData jobData, JobContext jobContext, SecuritySession securitySession) throws BaseTeamsException {
        log.trace(">>update_rendition_step execution start");
        List<Object> ids = ((ListContextWrapper) getDataFromContext("{assetIds}", jobContext)).getListData();
        log.info(">>> update_rendition_step - IDs: " + ids);
        log.info(">>> update_rendition_step - IDs JSON: " + new Gson().toJson(ids));
        log.info("update_rendition_step hostname: " + HostnameUtils.getHostname());
        if (null != jobContext) {
            final JsonObject jsonObject = new JsonParser().parse(new Gson().toJson(jobContext)).getAsJsonObject();
            log.info("update_rendition_step asset ids " + new Gson().toJson(jobContext));
            if (jsonObject.getAsJsonObject(DATA) != null && jsonObject.getAsJsonObject(DATA).getAsJsonObject(ASSET_IDS) != null)
                jsonObject.getAsJsonObject(DATA).getAsJsonObject(ASSET_IDS).getAsJsonArray("listData").forEach(jsonElement ->
                {
                    String assetId = jsonElement.getAsJsonObject().get("_id").getAsString();
                    log.info("update_rendition -> identified asset id: " + assetId);
                    Util.updateRendition(assetId);
                    log.info("update_rendition_step -> asset-ids" + new Gson().toJson(assetId));
                });
        }
        log.trace("<<update_rendition_step execution end");
        return StepStatus.COMPLETED;
    }

}
