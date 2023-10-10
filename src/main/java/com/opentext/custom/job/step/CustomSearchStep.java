package com.opentext.custom.job.step;

import com.artesia.common.MetadataFieldConstants;
import com.artesia.common.exception.BaseTeamsException;
import com.artesia.entity.TeamsIdentifier;
import com.artesia.security.SecuritySession;
import com.google.gson.Gson;
import com.opentext.activity.ActionProperty;
import com.opentext.activity.PropertyDataType;
import com.opentext.activity.PropertyType;
import com.opentext.custom.job.model.AssetsData;
import com.opentext.custom.job.model.ExpiringAssetInfo;
import com.opentext.custom.util.OTUtils;
import com.opentext.job.JobContext;
import com.opentext.server.job.step.BaseStep;
import com.opentext.server.job.step.JobData;
import com.opentext.server.job.step.StepStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

import static com.opentext.custom.constant.Constants.*;
import static com.opentext.custom.constant.Constants.TEAMS_HOME;
import static com.opentext.custom.util.OTUtils.getSystemInformation;

/**
 * It moves an asset from source folder to destination folder
 */
public class CustomSearchStep extends BaseStep {
    private static final Log log = LogFactory.getLog(CustomSearchStep.class);

    public StepStatus executeStep(JobData jobData, JobContext jobContext, SecuritySession securitySession) {
        if (null == System.getProperty(TEAMS_HOME)) {
            Properties props = System.getProperties();
            props.setProperty(TEAMS_HOME, System.getenv(TEAMS_HOME));
        }
        String searchText = getBeanProperties().get("searchString");
        String fieldsId = getBeanProperties().get("fieldsIDCommaSeparated");

        List<ExpiringAssetInfo> assetIdentifiers = null;
        try {
            List<TeamsIdentifier> fieldsIdList = new ArrayList<>();
            String []ids = fieldsId.split(",");
            if(ids.length > 0){
                Arrays.stream(ids).forEach(id->fieldsIdList.add(new TeamsIdentifier(id)));
            }
            fieldsIdList.add(MetadataFieldConstants.METADATA_FIELD_ID__ASSET_DESCRIPTION);
            fieldsIdList.add(MetadataFieldConstants.METADATA_FIELD_ID__SOURCE_REFERENCE);
            fieldsIdList.add(MetadataFieldConstants.METADATA_FIELD_ID__EXPIRATION_DATE);
            TeamsIdentifier[] fieldIds = (TeamsIdentifier[]) fieldsIdList.toArray();
            assetIdentifiers = OTUtils.customSearch(fieldIds, searchText, securitySession);
        } catch (BaseTeamsException e) {
            log.error("CUSTOM_ASSET_EXPIRY -> ", e);
        }

        try {
            Map<String, Object> data = new HashMap<>();
            AssetsData assetsData = new AssetsData();
            if (null != assetIdentifiers && !assetIdentifiers.isEmpty()) {
                assetsData.setExpiringAssetInfoList(assetIdentifiers);
                data.put(ASSETS_CONTEXT, new Gson().toJson(assetsData));
                jobContext.setData(data);
            } else {
                data.put(ASSETS_CONTEXT, new Gson().toJson(assetsData));
                jobContext.setData(data);
            }
        } catch (Exception e) {
            log.error("CUSTOM_ASSET_EXPIRY -> exception: " + e.getMessage());
        }
        return StepStatus.COMPLETED;
    }

    @Override
    protected void initializeProperties(Set<ActionProperty> actionProperties) {
        ActionProperty fieldsIDCommaSeparated = new ActionProperty("fieldsIDCommaSeparated", "Enter comma separated field IDs",
                "Fields ID", true, PropertyDataType.STRING, "{fieldsIDCommaSeparated}", PropertyType.INPUT);
        actionProperties.add(fieldsIDCommaSeparated);

        ActionProperty searchString = new ActionProperty("searchString", "The search text input",
                "Search Text", true, PropertyDataType.STRING, "{searchString}", PropertyType.INPUT);
        actionProperties.add(searchString);
    }
}