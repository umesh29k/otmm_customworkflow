package com.opentext.custom.job.step;

import com.artesia.asset.selection.AssetIdsSelectionContext;
import com.artesia.asset.services.AssetServices;
import com.artesia.common.exception.BaseTeamsException;
import com.artesia.entity.TeamsIdentifier;
import com.artesia.entity.TeamsNumberIdentifier;
import com.artesia.security.SecuritySession;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opentext.activity.ActionProperty;
import com.opentext.activity.PropertyDataType;
import com.opentext.activity.PropertyType;
import com.opentext.job.JobContext;
import com.opentext.server.job.step.BaseStep;
import com.opentext.server.job.step.JobData;
import com.opentext.server.job.step.StepStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.opentext.custom.constant.Constants.*;

/**
 * This class is used to send notification after completion of custom activity
 *
 * @author umeshkumars
 */
public class AddWatermarkStep extends BaseStep {
    private static final Log log = LogFactory.getLog(AddWatermarkStep.class);

    public StepStatus executeStep(JobData jobData, JobContext jobContext, SecuritySession securitySession)
            throws BaseTeamsException {
        final List<TeamsIdentifier> assetIds = new ArrayList<>();
        String templateId = getBeanProperties().get("templateId");

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
        log.error("custom_watermarking_job_action -> asset-id: " + assetIds);
        log.error("custom_watermarking_job_action -> watermarking is initiated");
        try {
            //String templateId = (String) jobContext.getData().get("CustomWatermark.template.id");
            log.info("custom_watermarking_job_action -> templateId: " + templateId);
            AssetServices.getInstance().addorUpdateWaterMarkonAssets(assetIdsSelectionContext, new TeamsNumberIdentifier(Long.parseLong(templateId)), securitySession);
        } catch (Exception e) {
            log.error("custom_watermarking_job_action -> Unable to add watermark: ", e);
        }
        log.error("custom_watermarking_job_action -> watermarking is added");
        return StepStatus.COMPLETED;
    }


    @Override
    protected void initializeProperties(Set<ActionProperty> actionProperties) {
        ActionProperty assetIdsProperty = new ActionProperty("templateId", "The watermark template ID [ numeric long ID ] generated for the watermark template",
                "Template ID", true, PropertyDataType.STRING, "{templateId}", PropertyType.INPUT);
        actionProperties.add(assetIdsProperty);
    }
}