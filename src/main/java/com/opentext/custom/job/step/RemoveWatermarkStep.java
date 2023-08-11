package com.opentext.custom.job.step;

import com.artesia.asset.selection.AssetIdsSelectionContext;
import com.artesia.asset.services.AssetServices;
import com.artesia.common.exception.BaseTeamsException;
import com.artesia.entity.TeamsIdentifier;
import com.artesia.security.SecuritySession;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opentext.job.JobContext;
import com.opentext.server.job.step.BaseStep;
import com.opentext.server.job.step.JobData;
import com.opentext.server.job.step.StepStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

import static com.opentext.custom.constant.Constants.ASSET_IDS;
import static com.opentext.custom.constant.Constants.DATA;

/**
 * This class is used to send notification after completion of custom activity
 *
 * @author umeshkumars
 */
public class RemoveWatermarkStep extends BaseStep {
    private static final Log log = LogFactory.getLog(RemoveWatermarkStep.class);

    public StepStatus executeStep(JobData jobData, JobContext jobContext, SecuritySession securitySession)
            throws BaseTeamsException {
        final List<TeamsIdentifier> assetIds = new ArrayList<>();
        if (null != jobContext) {
            final JsonObject jsonObject = new JsonParser().parse(new Gson().toJson(jobContext)).getAsJsonObject();

            log.info("custom_watermarking_job_action asset ids " + new Gson().toJson(jobContext));
            if (jsonObject.getAsJsonObject(DATA) != null && jsonObject.getAsJsonObject(DATA).getAsJsonObject(ASSET_IDS) != null)
                jsonObject.getAsJsonObject(DATA).getAsJsonObject(ASSET_IDS).getAsJsonArray("listData").forEach(jsonElement ->
                        assetIds.add(new TeamsIdentifier(jsonElement.getAsJsonObject().get("_id").getAsString())));
            log.info("custom_watermarking_job_action -> asset-ids" + new Gson().toJson(assetIds));
        }

        AssetIdsSelectionContext assetIdsSelectionContext = new AssetIdsSelectionContext();
        assetIdsSelectionContext.setAssetIds(assetIds);

        AssetServices.getInstance().deleteWaterMarkonAssets(assetIdsSelectionContext, securitySession);

        return StepStatus.COMPLETED;
    }

}