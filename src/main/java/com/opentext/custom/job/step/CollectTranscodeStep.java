package com.opentext.custom.job.step;


import com.artesia.asset.AssetIdMap;
import com.artesia.asset.AssetIdentifier;
import com.artesia.common.MetadataFieldConstants;
import com.artesia.common.exception.BaseTeamsException;
import com.artesia.entity.TeamsIdentifier;
import com.artesia.metadata.MetadataCollection;
import com.artesia.metadata.MetadataField;
import com.artesia.metadata.MetadataValue;
import com.artesia.security.SecuritySession;
import com.artesia.server.asset.metadata.task.RetrieveMetadataTask;
import com.artesia.server.common.task.TaskResult;
import com.opentext.job.JobContext;
import com.opentext.job.context.ListContextWrapper;
import com.opentext.job.context.MapContextWrapper;
import com.opentext.server.job.step.AbstractBaseStep;
import com.opentext.server.job.step.JobData;
import com.opentext.server.job.step.StepStatus;
import com.opentext.server.job.step.imprt.CollectTranscodeAssetsStep;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectTranscodeStep extends AbstractBaseStep {
    private static final Log log = LogFactory.getLog(CollectTranscodeStep.class);

    public CollectTranscodeStep() {
    }

    public StepStatus executeBaseStep(JobData jobData, JobContext jobContext, SecuritySession securitySession) throws BaseTeamsException {
        log.trace(">>executeBaseStep");
        List<AssetIdentifier> assetIdentifiers = ((ListContextWrapper) this.getDataFromContext("{assetIds}", jobContext)).getListData();
        if (assetIdentifiers != null) {
            RetrieveMetadataTask retrieveMetadataTask = new RetrieveMetadataTask((AssetIdentifier[]) assetIdentifiers.toArray(new AssetIdentifier[0]), new TeamsIdentifier[]{MetadataFieldConstants.METADATA_FIELD_ID__CONTENT_TYPE});
            TaskResult<AssetIdMap<MetadataCollection>> taskResult = retrieveMetadataTask.execute(this.getTaskRequest(), securitySession);
            AssetIdMap<MetadataCollection> assetIdMap = (AssetIdMap) taskResult.getResultObject();
            Map<String, String> assetIdToContentTypeMap = new HashMap();
            List<AssetIdentifier> transcodeAssetIds = new ArrayList();
            if (assetIdMap != null && assetIdMap.getAssetIds() != null) {
                AssetIdentifier[] var10 = assetIdMap.getAssetIds();
                int var11 = var10.length;

                for (int var12 = 0; var12 < var11; ++var12) {
                    AssetIdentifier assetId = var10[var12];
                    MetadataCollection metadataCollection = (MetadataCollection) assetIdMap.getAssociatedObjectForAssetId(assetId);
                    MetadataField metadataField = (MetadataField) metadataCollection.findElementById(MetadataFieldConstants.METADATA_FIELD_ID__CONTENT_TYPE);
                    if (metadataField != null) {
                        MetadataValue metadataValue = metadataField.getValue();
                        if (metadataValue != null && metadataValue.getStringValue() != null) {
                            assetIdToContentTypeMap.put(assetId.asString(), metadataValue.getStringValue());
                            transcodeAssetIds.add(assetId);
                        }
                    }
                }
            }

            this.setDataToContext("assetIdToContentTypeMap", new MapContextWrapper(assetIdToContentTypeMap), jobContext);
            this.setDataToContext("transcodeAssetIds", new ListContextWrapper(transcodeAssetIds), jobContext);
        }

        log.trace(">>executeBaseStep");
        return StepStatus.COMPLETED;
    }
}
