package com.opentext.custom.util;

import com.artesia.asset.Asset;
import com.artesia.asset.AssetIdentifier;
import com.artesia.asset.metadata.services.AssetMetadataServices;
import com.artesia.asset.services.AssetDataLoadRequest;
import com.artesia.asset.services.AssetServices;
import com.artesia.asset.services.RetrieveAssetsCriteria;
import com.artesia.common.MetadataFieldConstants;
import com.artesia.common.exception.BaseTeamsException;
import com.artesia.common.prefs.PrefData;
import com.artesia.common.prefs.PrefDataId;
import com.artesia.entity.TeamsIdentifier;
import com.artesia.metadata.*;
import com.artesia.search.Search;
import com.artesia.search.SearchConstants;
import com.artesia.search.SearchScalarCondition;
import com.artesia.security.SecuritySession;
import com.artesia.security.session.services.LocalAuthenticationServices;
import com.artesia.system.services.SystemServices;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opentext.custom.job.model.ExpiringAssetInfo;
import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Format;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static com.opentext.custom.constant.Constants.*;

/**
 * @author umeshkumars
 */
public class OTUtils {
    private static final Log log = LogFactory.getLog(OTUtils.class);
    private static final RestTemplate restTemplate = new RestTemplate();

    /**
     * private constructor
     */
    private OTUtils() {
    }

    /**
     * search expiring assets for the given duration
     *
     * @param fieldIds
     * @param searchText
     * @param securitySession
     * @return
     * @throws BaseTeamsException
     */
    public static List<ExpiringAssetInfo> customSearch(TeamsIdentifier[] fieldIds, String searchText, SecuritySession securitySession) throws BaseTeamsException {
        AssetDataLoadRequest dataRequest = new AssetDataLoadRequest();
        dataRequest.setLoadThumbnailAndKeywords(true, true);
        dataRequest.setLoadDestinationLinks(true);
        dataRequest.setLoadMetadata(true);
        dataRequest.setMetadataFieldsToRetrieve(fieldIds);

        // Build a search to return assets imported today
        SearchScalarCondition searchScalarCondition = new SearchScalarCondition(MetadataFieldConstants.METADATA_FIELD_ID__ASSET_NAME, null, searchText);

        Search search = new Search(searchScalarCondition);

        // Build the criteria object
        RetrieveAssetsCriteria criteria = new RetrieveAssetsCriteria();
        criteria.setSearchInfo(search, 5);
        List<Asset> searchAssets = new ArrayList<>();
        searchAssets.addAll(List.of(AssetServices.getInstance().retrieveAssets(criteria, dataRequest, securitySession)));
        return getExpiringAssetList(searchAssets, securitySession);
    }

    /**
     * get expiring asset list
     *
     * @param searchAssets
     * @param securitySession
     * @return
     * @throws BaseTeamsException
     */
    private static List<ExpiringAssetInfo> getExpiringAssetList(List<Asset> searchAssets, SecuritySession securitySession) throws BaseTeamsException {
        List<ExpiringAssetInfo> expiringAssetsInfo = new ArrayList<>();
        for (Asset assetData : searchAssets) {
            // Load a single asset
            if (assetData != null) {
                String flag = assetData.getMetadata().getValueForField(new TeamsIdentifier("COM.DBS.METADATA.CUSTOM.EXPIRY.NOTIFICATION.FLAG")).getStringValue();
                MetadataValue countFieldValue = assetData.getMetadata().getValueForField(new TeamsIdentifier("COM.DBS.METADATA.CUSTOM.EXPIRY.NOTIFICATION.COUNT"));
                Integer frequency = null != countFieldValue.getValue() ? countFieldValue.getIntValue() : 0;
                MetadataValue dateFieldValue = assetData.getMetadata().getValueForField(new TeamsIdentifier("COM.DBS.METADATA.CUSTOM.EXPIRY.NOTIFICATION.DATE"));
                MetadataValue expiryDateFieldValue = assetData.getMetadata().getValueForField(MetadataFieldConstants.METADATA_FIELD_ID__EXPIRATION_DATE);
                Date expiryDate = (Date) (null != expiryDateFieldValue ? expiryDateFieldValue.getValue() : new Date());
                Date notifiedDate = (Date) (null != dateFieldValue ? dateFieldValue.getValue() : new Date());
                String expiryDateString = assetData.getMetadata().getValueForField(MetadataFieldConstants.METADATA_FIELD_ID__EXPIRATION_DATE).getStringValue();
                LocalDate nextNotifiedDate = null;
                LocalDate currentNotifiedDate = null;
                Integer interval = 0;
                if (null != notifiedDate) {
                    currentNotifiedDate = notifiedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    interval = Integer.parseInt(getSystemInformation(CUSTOM_ASSET_EXPIRY, "Job", "interval", securitySession));
                    nextNotifiedDate = currentNotifiedDate.plusDays(interval);
                } else {
                    nextNotifiedDate = LocalDate.now();
                }
                LocalDate currentDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate expiryLocalDate = expiryDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                boolean isAfter = false;
                if (!currentDate.isBefore(nextNotifiedDate)) isAfter = true;
                log.info("CUSTOM_EXPIRY_ASSETS -> Dates: currentNotifiedDate - " + currentNotifiedDate + ", nextNotifiedDate - " + nextNotifiedDate + ", currentDate - " + currentDate);
                if (((flag == null || (null != flag && (!flag.equalsIgnoreCase("true") || flag.isEmpty()))) && isAfter) || expiryLocalDate.plusDays(1).isEqual(currentDate)) {
                    String assetName = assetData.getName();
                    String assetOwner = assetData.getImportUserName();

                    expiringAssetInfo(securitySession, expiringAssetsInfo, assetData, flag, frequency, expiryDateString, interval, currentDate, assetName, assetOwner);

                    //Assume variables assetIds and session have already been set
                    if (!expiryLocalDate.plusDays(1).isEqual(currentDate))
                        updateAssetMetadata(assetData, frequency, securitySession);
                }
            }
        }

        log.info("GET_ASSET_LIST -> search asset for duration: " + expiringAssetsInfo);
        return expiringAssetsInfo;
    }

    /**
     * update asset metadata
     *
     * @param asset
     * @param frequency
     * @param securitySession
     */
    private static void updateAssetMetadata(Asset asset, Integer frequency, SecuritySession securitySession) {
        TeamsIdentifier flagId = new TeamsIdentifier("COM.DBS.METADATA.CUSTOM.EXPIRY.NOTIFICATION.FLAG");
        TeamsIdentifier countId = new TeamsIdentifier("COM.DBS.METADATA.CUSTOM.EXPIRY.NOTIFICATION.COUNT");
        TeamsIdentifier dateId = new TeamsIdentifier("COM.DBS.METADATA.CUSTOM.EXPIRY.NOTIFICATION.DATE");

        //save the new value
        try {
            AssetServices.getInstance().lockAsset(asset.getAssetId(), securitySession);

            //set flag-field value
            MetadataField flagField = new MetadataField(flagId);

            //get count-field value
            MetadataCollection[] countCol = AssetMetadataServices.getInstance().retrieveMetadataForAssets(asset.getAssetId().asAssetIdArray(), countId.asTeamsIdArray(), null, securitySession);

            MetadataField countField = (MetadataField) countCol[0].findElementById(countId);
            countField.setValue(++frequency);

            //get date-field value
            MetadataCollection[] dateCol = AssetMetadataServices.getInstance().retrieveMetadataForAssets(asset.getAssetId().asAssetIdArray(), dateId.asTeamsIdArray(), null, securitySession);

            MetadataField dateField = (MetadataField) dateCol[0].findElementById(dateId);
            Format f = new SimpleDateFormat("MM/dd/yyyy");
            String currentDate = f.format(new Date());
            log.info("CUSTOM_ASSET_EXPIRY -> Current Date : " + currentDate);
            dateField.setValue(currentDate);

            List<MetadataField> metadataFields = new ArrayList<>();
            metadataFields.add(dateField);
            metadataFields.add(countField);
            Integer systemFrequency = null != getSystemInformation(CUSTOM_ASSET_EXPIRY, "Job", FREQUENCY_PROP, securitySession) ? Integer.parseInt(getSystemInformation(CUSTOM_ASSET_EXPIRY, "Job", FREQUENCY_PROP, securitySession)) : 0;
            if (frequency >= systemFrequency) {
                MetadataCollection[] flagCol = AssetMetadataServices.getInstance().retrieveMetadataForAssets(asset.getAssetId().asAssetIdArray(), flagId.asTeamsIdArray(), null, securitySession);
                flagField = (MetadataField) flagCol[0].findElementById(flagId);
                flagField.setValue("TRUE");
                metadataFields.add(flagField);
            }

            MetadataField[] metadataFieldsToSave = new MetadataField[metadataFields.size()];
            for (int i = 0; i < metadataFields.size(); i++)
                metadataFieldsToSave[i] = metadataFields.get(i);

            AssetMetadataServices.getInstance().saveMetadataForAssets(asset.getAssetId().asAssetIdArray(), metadataFieldsToSave, securitySession);
            AssetServices.getInstance().unlockAsset(asset.getAssetId(), securitySession);
            log.info("GET_ASSET_LIST -> updated asset metadata: \ncount field: " + countField + "\ndate-field: " + dateField + "\nflag-field: " + flagField);
        } catch (BaseTeamsException e) {
            throw new RuntimeException(e);
        }
    }

    private static void expiringAssetInfo(SecuritySession securitySession, List<ExpiringAssetInfo> expiringAssetsInfo, Asset assetData, String flag, Integer frequency, String expiryDateString, Integer interval, LocalDate currentDate, String assetName, String assetOwner) throws BaseTeamsException {
        ExpiringAssetInfo expiringAssetInfo = new ExpiringAssetInfo();
        expiringAssetInfo.setAssetName(assetName);
        expiringAssetInfo.setAssetOwner(assetOwner);
        expiringAssetInfo.setFlag(flag);
        expiringAssetInfo.setExpiryDate(expiryDateString);
        expiringAssetInfo.setAssetId(assetData.getAssetId().getId());
        expiringAssetInfo.setHostName(getSystemInformation(CUSTOM_ASSET_EXPIRY, "Job", "hostName", securitySession));
        expiringAssetInfo.setNextNotifyDate(currentDate.plusDays(interval).toString());
        expiringAssetInfo.setCurrentAttempt(frequency.toString());
        Integer systemFrequency = null != getSystemInformation(CUSTOM_ASSET_EXPIRY, "Job", FREQUENCY_PROP, securitySession) ? Integer.parseInt(getSystemInformation(CUSTOM_ASSET_EXPIRY, "Job", FREQUENCY_PROP, securitySession)) : 0;
        expiringAssetInfo.setTotalAttempt(systemFrequency.toString());
        expiringAssetsInfo.add(expiringAssetInfo);
    }

    /**
     * @param comp
     * @param key
     * @param name
     * @param session
     * @return
     * @throws BaseTeamsException
     */
    public static String getSystemInformation(final String comp, final String key, final String name, final SecuritySession session) throws BaseTeamsException {
        SystemServices systemServices = SystemServices.getInstance();
        PrefDataId prefDataId = new PrefDataId(comp, key, name);
        PrefData systemSettings = systemServices.retrieveSystemSettingsByPrefDataId(prefDataId, session);
        String value = null;
        if (systemSettings != null) {
            value = systemSettings.getValue();
        }
        log.info("update_rendition_step [comp:key:name:value]" + comp + ":" + key + ":" + name + ":" + value);
        return value;
    }

    public static Map<String, String> getLocalSessionDigest() {
        Map<String, String> map = new HashMap<>();
        try {
            SecuritySession session = LocalAuthenticationServices.getInstance().createSession(TSUPER);
            map.put("id", session.getMessageDigest());
            map.put("requestBy", Integer.toString(session.getId()));
        } catch (BaseTeamsException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    public static List<String> updateRendition(String assetId, SecuritySession securitySession) {
        List<String> ids = new ArrayList<>();
        Map<String, String> map = getLocalSessionDigest();
        HttpHeaders headers = new HttpHeaders();
        headers.add(OTMMAUTHTOKEN, map.get(ID));
        headers.add(X_REQUESTED_BY, map.get(REQUEST_BY));
        log.info("update_rendition_step sessing map: " + map);
        ResponseEntity<String> entity = null;
        String convertedFilePath = getConvertedFilePath(assetId, securitySession);

        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("file", new FileSystemResource(new File(convertedFilePath)), MediaType.IMAGE_PNG);
        multipartBodyBuilder.part(RENDITION_TYPE, PREVIEW);
        multipartBodyBuilder.part(JOB_NAME, ATTACH_PREVIEW);
        MultiValueMap<String, HttpEntity<?>> multipartBody = multipartBodyBuilder.build();
        HttpEntity<MultiValueMap<String, HttpEntity<?>>> request = new HttpEntity<>(multipartBody, headers);

        try {
            log.info("update_rendition_step claling: " + MessageFormat.format(UPDATE_RENDITION_URL, assetId) + " for preview");
            entity = restTemplate.exchange(MessageFormat.format(UPDATE_RENDITION_URL, assetId), HttpMethod.POST, request, String.class);
            log.info("update_rendition_step status code for preview: " + entity.getStatusCode());
            log.info("update_rendition_step status code value for preview: " + entity.getStatusCodeValue());
            log.info("update_rendition_step entity_body for preview: " + entity.getBody());
        } catch (java.lang.Exception e) {
            log.error("update_rendition_step :" + e);
        }

        multipartBodyBuilder.part(RENDITION_TYPE, THUMBNAIL);
        multipartBodyBuilder.part(JOB_NAME, ATTACH_THUMBNAIL);
        multipartBody = multipartBodyBuilder.build();
        request = new HttpEntity<>(multipartBody, headers);

        try {
            log.info("update_rendition_step claling: " + MessageFormat.format(UPDATE_RENDITION_URL, assetId) + " for thumbnail");
            entity = restTemplate.exchange(MessageFormat.format(UPDATE_RENDITION_URL, assetId), HttpMethod.POST, request, String.class);
            log.info("update_rendition_step status code for thumbnail: " + entity.getStatusCode());
            log.info("update_rendition_step status code value for thumbnail: " + entity.getStatusCodeValue());
            log.info("update_rendition_step entity_body for thumbnail: " + entity.getBody());
        } catch (Exception e) {
            log.error("update_rendition_step :" + e);
        }
        return ids;
    }

    private static String getConvertedFilePath(String assetId, SecuritySession securitySession) {
        Asset asset = null;
        String convertedFilePath = null;
        try {
            String repositoryBaseLoc = (getSystemInformation(ASSET, CONFIG, REPOSITORY_LOCATION, securitySession) + "/").replace("//", "/");
            AssetDataLoadRequest dlr = new AssetDataLoadRequest();
            dlr.setLoadMetadata(false);
            asset = AssetServices.getInstance().retrieveAsset(new AssetIdentifier(assetId), dlr, securitySession);
            asset.getImportJobId();

            log.info("update_rendition_step asset repository base location: " + repositoryBaseLoc);
            if (!Files.exists(new File(repositoryBaseLoc + TEMP_CONVERT).toPath())) {
                Files.createDirectories(new File(repositoryBaseLoc + TEMP_CONVERT).toPath());
                log.info("update_rendition_step " + TEMP_CONVERT + " folder path created");
            } else log.info("update_rendition_step " + TEMP_CONVERT + " folder path already exist");
            if (!Files.exists(new File(repositoryBaseLoc + TEMP_BINARY).toPath())) {
                Files.createDirectories(new File(repositoryBaseLoc + TEMP_BINARY).toPath());
                log.info("update_rendition_step " + TEMP_BINARY + " folder path created");
            } else log.info("update_rendition_step " + TEMP_BINARY + " folder path already exist");

            boolean status = downloadFile(assetId, asset.getName(), repositoryBaseLoc + TEMP_BINARY);
            convertedFilePath = repositoryBaseLoc + TEMP_CONVERT + asset.getName().substring(0, asset.getName().lastIndexOf(".")) + ".png";
            if (!status) {
                log.info("update_rendition_step unable to download file");
            }
            log.info("update_rendition_step converted file: " + convertedFilePath);
            log.info("update_rendition_step downloaded asset binary location: " + repositoryBaseLoc + TEMP_BINARY + asset.getName());
            if (!Files.exists(Path.of(repositoryBaseLoc + TEMP_BINARY + asset.getName()))) {
                convertedFilePath = repositoryBaseLoc + TEMP_CONVERT + "default.png";
            } else {
                if (convert(repositoryBaseLoc + TEMP_CONVERT, repositoryBaseLoc + TEMP_BINARY + asset.getName())) {
                    log.info("update_rendition_step converted MS doc to png thumbnail to location: " + repositoryBaseLoc);
                } else
                    log.error("update_rendition_step failed to convert MS Doc to png thumbnail to location: " + repositoryBaseLoc);
            }
            //renaming converted file for correct extension, generated extension being as .'png, changed it to .png
            new File(repositoryBaseLoc + TEMP_CONVERT + asset.getName().substring(0, asset.getName().lastIndexOf(".")) + ".'png").renameTo(new File(repositoryBaseLoc + TEMP_CONVERT + asset.getName().substring(0, asset.getName().lastIndexOf(".")) + ".png"));
        } catch (BaseTeamsException e) {
            log.error("update_rendition_step asset load metadata exception: " + e.getMessage());
        } catch (IOException e) {
            log.error("update_rendition_step unable to create directory: " + e);
        }
        log.info("update_rendition_step ConvertedFilePath : " + convertedFilePath);
        return convertedFilePath;
    }

    public static boolean convert(String outDirLoc, String filePath) {
        boolean status = true;
        log.info("update_rendition_step file conversion command: " + "soffice --convert-to 'png:writer_png_Export:{\"PageRange\":{\"type\":\"string\",\"value\":\"2-\"}}' " + filePath + " --outdir " + outDirLoc);
        String s;
        Process p;
        try {
            log.info("update_rendition_step office document conversion process started");
            p = Runtime.getRuntime().exec("soffice --convert-to 'png:writer_png_Export:{\"PageRange\":{\"type\":\"string\",\"value\":\"2-\"}}' " + filePath + " --outdir " + outDirLoc);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((s = br.readLine()) != null) log.info("update_rendition_step line: " + s);
            p.waitFor();
            log.info("update_rendition_step office document conversion process completed: " + p.info());
            log.info("update_rendition_step exit: " + p.exitValue());
            p.destroy();
        } catch (Exception e) {
            log.error("update_rendition_step office document conversion exception. - IO exception: " + e);
            log.error("update_rendition_step run time exception, while executing office document conversion process: " + e);
            status = false;
        }
        return status;
    }

    public static boolean downloadFile(String assetId, String fileName, String tempDir) {
        boolean status = true;
        try {
            RequestCallback requestCallback = request -> {
                request.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
                Map<String, String> map = getLocalSessionDigest();
                //Map<String, String> map = initializeSession("tsuper", "Password1!");
                request.getHeaders().add(OTMMAUTHTOKEN, map.get(ID));
                request.getHeaders().add(X_REQUESTED_BY, map.get(REQUEST_BY));
            };
            ResponseExtractor<Void> responseExtractor = response -> {
                try {
                    Path path = Paths.get(tempDir + fileName);
                    if (Files.exists(path))
                        Files.delete(path);
                    Files.copy(response.getBody(), path);
                    log.info("update_rendition_step file downloaded operation completed");
                } catch (Exception e) {
                    log.error("update_rendition_step file download operation exception: " + e);
                }
                return null;
            };
            restTemplate.execute(URI.create(MessageFormat.format(DOWNLOAD_ORIGINAL_CONTENT_URL, assetId)), HttpMethod.GET, requestCallback, responseExtractor);
        } catch (Exception e) {
            status = false;
        }
        return status;
    }

    public static Map<String, String> initializeSession(String unm, String pwd) {
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("username", unm);
        requestBody.add("password", pwd);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        Map<String, String> map = new HashMap<>();
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(requestBody, headers);
        try {
            ResponseEntity<String> entity = restTemplate.exchange(SESSION_URL, HttpMethod.POST, request, String.class);
            JsonObject jsonObject = new JsonParser().parse(entity.getBody()).getAsJsonObject();
            if (jsonObject.getAsJsonObject("session_resource") != null) {
                if (jsonObject.getAsJsonObject("session_resource").getAsJsonObject("session") != null) {
                    if (jsonObject.getAsJsonObject("session_resource").getAsJsonObject("session").get("message_digest") != null) {
                        map.put("id", jsonObject.getAsJsonObject("session_resource").getAsJsonObject("session").get("message_digest").getAsString());
                    }
                    log.info("update_rendition_step " + jsonObject.getAsJsonObject("session_resource").getAsJsonObject("session"));
                    if (jsonObject.getAsJsonObject("session_resource").getAsJsonObject("session").get("id") != null) {
                        map.put(REQUEST_BY, jsonObject.getAsJsonObject("session_resource").getAsJsonObject("session").get("id").getAsString());
                    }
                }
            }
        } catch (Exception e) {
            log.error("update_rendition_step " + e);
        }
        log.info("update_rendition_step " + map);
        return map;
    }

    /* public static void main(String[] a) {
        //downloadFile("3f6d000b2156255ccbe142c41702928fc06eda4d", "test.png", "C:\\Users\\585606\\Documents\\codebase\\jar\\");
    }*/
}