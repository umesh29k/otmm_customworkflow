package com.opentext.custom.constant;

public class Constants {
    /**
     * <p><b>private constructor</b></p>
     */
    private Constants() {
    }

    public static final String BASE_URL = "http://localhost:11090/otmmapi/v6";
    public static final String UPDATE_RENDITION_URL = BASE_URL + "/assets/{0}/contents";
    public static final String DOWNLOAD_ORIGINAL_CONTENT_URL = BASE_URL + "/assets/{0}/contents?disposition=attachment";
    public static final String SESSION_URL = BASE_URL + "/sessions";
    public static final String FREQUENCY_PROP = "frequency";
    public static final String ASSETS_CONTEXT = "assets";
    public static final String DATA = "data";
    public static final String ASSET_IDS = "assetIds";
    public static final String TEAMS_HOME = "TEAMS_HOME";
    public static final String TSUPER = "tsuper";
    public static final String PREVIEW = "preview";
    public static final String RENDITION_TYPE = "rendition_type";
    public static final String OTMMAUTHTOKEN = "otmmauthtoken";
    public static final String THUMBNAIL = "thumbnail";
    public static final String ATTACH_PREVIEW = "Attach Preview";
    public static final String JOB_NAME = "job_name";
    public static final String ATTACH_THUMBNAIL = "Attach Thumbnail";
    public static final String X_REQUESTED_BY = "X-Requested-By";
    public static final String REQUEST_BY = "requestBy";
    public static final String ID = "id";
    public static final String ASSET = "ASSET";
    public static final String CONFIG = "CONFIG";
    public static final String REPOSITORY_LOCATION = "REPOSITORY_LOCATION";
    public static final String CUSTOM_ASSET_EXPIRY = "CustomAssetExpiry";
    public static final String TEMP_CONVERT = "temp/convert/";
    public static final String TEMP_BINARY = "temp/binary/";
    public static final String ARTESIA_FIELD_MASTER_FILE_PATH = "ARTESIA.FIELD.MASTER FILE PATH";
}
