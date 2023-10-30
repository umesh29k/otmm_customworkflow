package com.opentext.custom.job.step;

import com.artesia.asset.Asset;
import com.artesia.asset.imprt.ImportAsset;
import com.artesia.common.exception.BaseTeamsException;
import com.artesia.content.ContentData;
import com.artesia.content.ContentInfo;
import com.artesia.content.MimeType;
import com.artesia.content.MimeTypes;
import com.artesia.security.SecuritySession;
import com.artesia.server.asset.entity.AssetEntity;
import com.artesia.server.content.task.RetrieveAllMimeTypesTask;
import com.artesia.transformer.TransformationJobData;
import com.opentext.job.JobContext;
import com.opentext.job.context.ListContextWrapper;
import com.opentext.server.job.step.JobData;
import com.opentext.server.job.step.StepStatus;
import com.opentext.server.job.step.transform.BaseTransformerStep;
import com.opentext.server.job.util.JobUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CustomTransImprt extends BaseTransformerStep {
    private static final Log log = LogFactory.getLog(CustomTransImprt.class);

    private String transformerType;

    protected StepStatus execute(JobData jobData, JobContext jobContext, SecuritySession securitySession) throws BaseTeamsException {
        log.trace(">>execute()");
        List<TransformationJobData> transformationDataList = new ArrayList<>();
        MimeTypes mimeTypes = (MimeTypes)(new RetrieveAllMimeTypesTask()).execute(getTaskRequest(), securitySession).getResultObject();
        AssetEntity assetEntity = retrieveAsset(this.assetId, securitySession);
        Asset asset = assetEntity.getAsset();
        ContentInfo transformContent = asset.getAssetContentInfo().getMasterContent();
        String masterFileLocation = this.assetTransformationElement.getMasterContentFilePath();
        if (transformContent != null && masterFileLocation != null) {
            asset.getAssetContentInfo().getMasterContent().getContentData().setFile(new File(masterFileLocation));
            MimeType mt = mimeTypes.getMimeTypeByName(transformContent.getMimeType());
            createTransformationChain(transformContent.getContentData(), mt, true, this.workingDir, transformationDataList, this.transformerType, jobContext);
            ImportAsset impAsset = (ImportAsset)JobUtils.getDataFromContext("{importAsset}", jobContext);
            if (impAsset != null && StringUtils.isNotBlank(impAsset.getProxyContentFilePath())) {
                File proxyFile = new File(impAsset.getProxyContentFilePath());
                ContentData contentData = new ContentData();
                contentData.setFile(proxyFile);
                createTransformationChain(contentData, mimeTypes.getMimeTypeByName(impAsset.getProxyContentMimeType()), false, this.workingDir, transformationDataList, this.transformerType, jobContext);
            }
            setDataToContext("transformationJobData", new ListContextWrapper(transformationDataList), jobContext);
        }
        log.trace("<<execute()");
        return StepStatus.COMPLETED;
    }

    public String getTransformerType() {
        return this.transformerType;
    }

    public void setTransformerType(String transformerType) {
        this.transformerType = transformerType;
    }
}
