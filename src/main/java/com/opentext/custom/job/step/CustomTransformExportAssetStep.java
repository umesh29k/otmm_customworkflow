package com.opentext.custom.job.step;

import com.artesia.asset.Asset;
import com.artesia.asset.AssetIdentifier;
import com.artesia.asset.selection.AttestedContext;
import com.artesia.asset.services.AssetDataLoadRequest;
import com.artesia.asset.services.AssetServices;
import com.artesia.common.exception.BaseTeamsException;
import com.artesia.content.ContentData;
import com.artesia.content.ContentDataRequest;
import com.artesia.entity.TeamsIdentifier;
import com.artesia.export.AssetExportElement;
import com.artesia.export.ExportAsset;
import com.artesia.export.ExportContentRequest;
import com.artesia.export.ExportElement;
import com.artesia.security.SecuritySession;
import com.artesia.security.session.services.LocalAuthenticationServices;
import com.artesia.server.common.task.TaskRequest;
import com.artesia.server.export.task.PerformAssetExportTransformationTask;
import com.artesia.server.system.task.SystemSettingTaskHelper;
import com.artesia.transformer.TransformationJobData;
import com.google.gson.Gson;
import com.opentext.job.JobClass;
import com.opentext.job.JobContext;
import com.opentext.job.context.ListContextWrapper;
import com.opentext.job.context.MapContextWrapper;
import com.opentext.server.job.step.JobData;
import com.opentext.server.job.step.StepStatus;
import com.opentext.server.job.step.transform.BaseTransformerStep;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class CustomTransformExportAssetStep extends BaseTransformerStep {
    private static final Log log = LogFactory.getLog(CustomTransformExportAssetStep.class);

    public StepStatus execute(JobData jobData, JobContext jobContext, SecuritySession session) throws BaseTeamsException {
        log.trace(">>transformAssets()");
        TaskRequest request = getTaskRequest();
        Boolean transformAssetStepInitated = false;//(Boolean)getDataFromContext("{transformAssetStepInitated}", jobContext);
        String exportJobId = (String) getDataFromContext("{exportJobId}", jobContext);
        if (transformAssetStepInitated != null && transformAssetStepInitated.booleanValue() == true)
            return StepStatus.COMPLETED;
        List<Object> ids = ((ListContextWrapper) getDataFromContext("{assetIds}", jobContext)).getListData();
        AtomicReference<Asset> asset = new AtomicReference<>();
        try {
            session = LocalAuthenticationServices.getInstance().createSession("tsuper");
        } catch (BaseTeamsException e) {
            log.error("update_rendition - unable to create local session for the username tsuper: " + e);
        }
        log.info("CustomTransformerExportAssetStep: test020");
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

        log.info("CustomTransformerExportAssetStep: test010");

        setDataToContext("transformAssetStepInitated", new Boolean(true), jobContext);
        ExportElement exportElement = new ExportElement() {
            @Override
            public TeamsIdentifier getElementId() {
                return asset.get().getAssetId();
            }
        };
        log.info("CustomTransformerExportAssetStep: test0");
        ExportAsset exportAsset = new ExportAsset(asset.get().getAssetId(), exportElement);// getDataFromContext("{exportAsset}", jobContext);
        exportAsset.setAssetId(asset.getOpaque().getAssetId());
        exportAsset.setAsset(asset.get());

        AttestedContext attestedContext = new AttestedContext();
        attestedContext.setContextType(AttestedContext.CONTEXT_TYPE.COLLECTION);

        ContentDataRequest contentDataRequest = new ContentDataRequest();
        contentDataRequest.setAttestedContext(attestedContext);
        contentDataRequest.setLoadStorageAttributes(true);

        ExportContentRequest exportContentRequest = new ExportContentRequest();
        exportContentRequest.setExportPreviewContent(true);
        exportContentRequest.setExportThumbnailContent(true);
        //exportContentRequest.setExportSupportingContent(true);
        exportContentRequest.setDefaultContentDataRequest(contentDataRequest);

        exportAsset.setExportContentRequest(exportContentRequest);
        log.info("CustomTransformerExportAssetStep: test1");
        exportAsset.setAsset(asset.get());

        exportAsset.setMasterFile(new File("/opt/otmm_repository/data/repository/original/vol0/0/rival-battle_a78cc5402163d1b463e1eca0e5f88b12764e79f7.png"));
        exportAsset.setOriginalWorkingAreaMasterFile(new File("/opt/otmm_repository/temp"));
        exportAsset.setOriginalWorkingAreaPreviewFile(new File("/opt/otmm_repository/temp"));
        exportAsset.setOriginalWorkingAreaThumbnailFile(new File("/opt/otmm_repository/temp"));
        //exportAsset.setOriginalWorkingAreaSupportingFiles(new File(""));
        log.info("CustomTransformerExportAssetStep: test2");
        MapContextWrapper<String> originalToRenamedFiles = (MapContextWrapper<String>) jobContext.getData().get("originalToRenamedFiles");
        TransformationJobData data = new TransformationJobData();
        data.setContinueTransformationChain(false);
        data.setJobType((jobData.getJobParameters().get("jobClass") != null) ? (String) jobData
                .getJobParameters().get("jobClass") : JobClass.EXPORT.getPrompt());
        data.setParentJobName((jobData.getJobParameters().get("Import") != null) ? (String) jobData
                .getJobParameters().get("Import") : (String) jobData.getJobParameters().get("jobName"));
        data.setSubJobId((Long) jobData.getJobParameters().get("subJobId"));
        data.setJobInstanceId(jobData.getJobInstanceId().longValue());
        data.setWorkingDir(this.workingDir);
        data.setTransformerId(new TeamsIdentifier("ARTESIA.TRANSFORMER.CUSTOM.IMAGE"));
        ContentData transformContent = new ContentData();
        transformContent.setFile(exportAsset.getMasterFile());
        transformContent.setDataSource(ContentData.ContentDataSource.FILE);
        data.setTransformContent(transformContent);
        data.setOtmmJobId(Long.valueOf(exportJobId).longValue());
        log.info("CustomTransformerExportAssetStep: test3");
        data.setTransformationId(exportAsset.getuId());
        if (originalToRenamedFiles != null && originalToRenamedFiles.getMapData() != null &&
                !originalToRenamedFiles.getMapData().isEmpty())
            data.setOriginalToRenamedFiles(originalToRenamedFiles.getMapData());
        String eventEnabled = SystemSettingTaskHelper.getSystemSetting("TRANSFORMER", "CONFIG", "TRANSFORMATION_EVENT_ENABLED", "FALSE");
        boolean isTransformationEventEnabled = false;
        if (eventEnabled.equalsIgnoreCase("TRUE"))
            isTransformationEventEnabled = true;
        PerformAssetExportTransformationTask transformTask = new PerformAssetExportTransformationTask(null, null, this.workingDir);
        transformTask.setExportAsset(exportAsset);
        transformTask.setTransformerArguments(exportAsset.getExportElement().getExportTransformer());
        transformTask.setJobId(exportJobId);
        transformTask.setTranformationJobData(data);
        transformTask.setTransformationEventEnabled(isTransformationEventEnabled);
        if (((AssetExportElement) exportAsset.getExportElement()).isIncludePreview() && exportAsset
                .getMasterFile() != null)
            transformTask.setOutputFileName(exportAsset.getMasterFile().getName());
        log.info("CustomTransformerExportAssetStep: test4");
        transformTask.execute(request, session);
        log.trace("<<execute()");
        return data.isContinueTransformationChain() ? StepStatus.COMPLETED : StepStatus.STOP;
    }
}
