package com.opentext.custom.util;

import com.artesia.asset.Asset;
import com.artesia.asset.metadata.services.AssetMetadataServices;
import com.artesia.asset.services.AssetDataLoadRequest;
import com.artesia.asset.services.AssetServices;
import com.artesia.asset.services.RetrieveAssetsCriteria;
import com.artesia.common.MetadataFieldConstants;
import com.artesia.common.exception.BaseTeamsException;
import com.artesia.common.prefs.PrefData;
import com.artesia.common.prefs.PrefDataId;
import com.artesia.entity.TeamsIdentifier;
import com.artesia.metadata.MetadataCollection;
import com.artesia.metadata.MetadataField;
import com.artesia.metadata.MetadataValue;
import com.artesia.search.Search;
import com.artesia.search.SearchConstants;
import com.artesia.search.SearchScalarCondition;
import com.artesia.security.SecuritySession;
import com.artesia.system.services.SystemServices;
import com.opentext.custom.job.model.ExpiringAssetInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.opentext.custom.constant.Constants.*;

/**
 * @author umeshkumars
 */
public class OTUtils {
    private static final Log log = LogFactory.getLog(OTUtils.class);
    public static final String MASTER_CONTENT = "masterContent";
    public static final String CUSTOM_ASSET_EXPIRY = "CustomAssetExpiry";
    public static final String CUSTOM_CONTACT_SHEET = "CUSTOM_CONTACT_SHEET -> ";
    public static SecuritySession securitySession;
    public static String hostUrl;
    public static String username;
    public static String password;
    private static final HttpHeaders requestHeaders = new HttpHeaders();
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
     * @param type
     * @param securitySession
     * @return
     * @throws BaseTeamsException
     */
    public static List<ExpiringAssetInfo> customSearch(TeamsIdentifier[] fieldIds, String searchText, SecuritySession securitySession) throws BaseTeamsException {
        TeamsIdentifier durationConstant = getDurationCons(searchText);
        AssetDataLoadRequest dataRequest = new AssetDataLoadRequest();
        dataRequest.setLoadThumbnailAndKeywords(true, true);
        dataRequest.setLoadDestinationLinks(true);
        dataRequest.setLoadMetadata(true);
        dataRequest.setMetadataFieldsToRetrieve(fieldIds);

        // Build a search to return assets imported today
        SearchScalarCondition searchScalarCondition = new SearchScalarCondition(
                MetadataFieldConstants.METADATA_FIELD_ID__ASSET_NAME,
                null, searchText);

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
                    currentNotifiedDate = notifiedDate.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    try {
                        interval = Integer.parseInt(getSystemInformation(CUSTOM_ASSET_EXPIRY, "Job", "interval", securitySession));
                    } catch (Exception e) {
                        log.error(e);
                    }
                    nextNotifiedDate = currentNotifiedDate.plusDays(interval);
                } else {
                    nextNotifiedDate = LocalDate.now();
                }
                LocalDate currentDate = new Date().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                LocalDate expiryLocalDate = expiryDate.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                boolean isAfter = false;
                if (!currentDate.isBefore(nextNotifiedDate))
                    isAfter = true;
                log.info("CUSTOM_EXPIRY_ASSETS -> Dates: currentNotifiedDate - " + currentNotifiedDate + ", nextNotifiedDate - " + nextNotifiedDate + ", currentDate - " + currentDate);
                if (((flag == null || (null != flag && (!flag.equalsIgnoreCase("true") || flag.isEmpty()))) && isAfter)
                        || expiryLocalDate.plusDays(1).isEqual(currentDate)) {
                    String assetName = assetData.getName();
                    String assetOwner = assetData.getImportUserName();

                    expiringAssetInfo(securitySession, expiringAssetsInfo, assetData, flag, frequency, expiryDateString, interval, currentDate, assetName, assetOwner);

                    //Assume variables assetIds and session have already been set
                    if (!expiryLocalDate.plusDays(1).isEqual(currentDate))
                        updateAssetMetadata(assetData, frequency, securitySession);
                }
            }
        }

        log.info("CUSTOM_ASSEST_EXPIRY -> search asset for duration: " + expiringAssetsInfo);
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
            MetadataCollection[] countCol = AssetMetadataServices.getInstance().retrieveMetadataForAssets(asset.getAssetId().asAssetIdArray(),
                    countId.asTeamsIdArray(), null, securitySession);

            MetadataField countField = (MetadataField) countCol[0].findElementById(countId);
            countField.setValue(++frequency);

            //get date-field value
            MetadataCollection[] dateCol = AssetMetadataServices.getInstance().retrieveMetadataForAssets(asset.getAssetId().asAssetIdArray(),
                    dateId.asTeamsIdArray(), null, securitySession);

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
                MetadataCollection[] flagCol = AssetMetadataServices.getInstance().retrieveMetadataForAssets(asset.getAssetId().asAssetIdArray(),
                        flagId.asTeamsIdArray(), null, securitySession);
                flagField = (MetadataField) flagCol[0].findElementById(flagId);
                flagField.setValue("TRUE");
                metadataFields.add(flagField);
            }

            MetadataField[] metadataFieldsToSave = new MetadataField[metadataFields.size()];
            for (int i = 0; i < metadataFields.size(); i++)
                metadataFieldsToSave[i] = metadataFields.get(i);

            AssetMetadataServices.getInstance().saveMetadataForAssets(asset.getAssetId().asAssetIdArray(), metadataFieldsToSave, securitySession);
            AssetServices.getInstance().unlockAsset(asset.getAssetId(), securitySession);
            log.info("CUSTOM_ASSEST_EXPIRY -> updated asset metadata: \ncount field: " + countField + "\ndate-field: " + dateField + "\nflag-field: " + flagField);
        } catch (Exception e) {
            log.error(e);
            log.info("CUSTOM_ASSET_EXPIRY -> unable to unlock");
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
     * get duration constant for the given duration property
     *
     * @param duration
     * @return
     */
    private static TeamsIdentifier getDurationCons(String duration) {
        TeamsIdentifier durationConstant;
        durationConstant = SearchConstants.OPERATOR_ID__DATE_IS_IN_LAST_7_DAYS;
        return durationConstant;
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
        return value;
    }

}