package com.opentext.custom.job.step;

import com.artesia.asset.Asset;
import com.artesia.asset.AssetIdentifier;
import com.artesia.asset.services.AssetDataLoadRequest;
import com.artesia.asset.services.AssetServices;
import com.artesia.common.exception.BaseTeamsException;
import com.artesia.security.SecuritySession;
import com.artesia.security.session.services.LocalAuthenticationServices;
import com.google.gson.Gson;
import com.opentext.job.JobContext;
import com.opentext.job.context.ListContextWrapper;
import com.opentext.job.subjob.SubJobData;
import com.opentext.server.job.common.SubJobDecider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.opentext.server.job.util.JobUtils.getDataFromContext;

public class CustomImprtSubJobDecider implements SubJobDecider {
    private static final Log log = LogFactory.getLog(CustomImprtSubJobDecider.class);

    public SubJobData getSubJobData(Object currentItem, JobContext jobContext) {
        log.trace(">>getJobName()");
        List<Object> ids = ((ListContextWrapper) getDataFromContext("{assetIds}", jobContext)).getListData();
        SecuritySession session = null;
        AtomicReference<Asset> asset = new AtomicReference<>();
        try {
            session = LocalAuthenticationServices.getInstance().createSession("tsuper");
        } catch (BaseTeamsException e) {
            log.error("update_rendition - unable to create local session for the username tsuper: " + e);
        }
        if (ids != null) {
            SecuritySession finalSession = session;
            ids.forEach(id -> {
                log.info("update_rendition -> identified asset id: " + id.toString());
                try {
                    asset.set(AssetServices.getInstance().retrieveAsset(new AssetIdentifier(id.toString()), new AssetDataLoadRequest(), finalSession));
                } catch (BaseTeamsException e) {
                    log.error("update_rendition - unable to retrieve asset details: " + e);
                }
                log.info("update_rendition_step -> asset-ids" + new Gson().toJson(id.toString()));
            });
        }
        SubJobData subJobData = new SubJobData();
        String jobName = null;
        AssetIdentifier assetId = asset.get().getAssetId();
        String contentType = asset.get().getMimeType();
        if (contentType.toLowerCase().contains("video")) {
            jobName = "VideoJob";
        } else if (contentType.toLowerCase().contains("audio")) {
            jobName = "AudioJob";
        }
        log.debug("Sub Job Name[" + jobName + "] for the asset[" + assetId.asString() + "]");

            subJobData.setJobName(jobName);
        JobContext newJobContext = new JobContext();
        newJobContext.getData().put("assetId", assetId.asString());
        subJobData.setJobContext(newJobContext);
        log.trace("<<getJobName()");
        return subJobData;
    }
}
