package com.opentext.custom.util;

import com.artesia.common.exception.BaseTeamsException;
import com.artesia.common.prefs.PrefData;
import com.artesia.common.prefs.PrefDataId;
import com.artesia.security.SecuritySession;
import com.artesia.system.services.SystemServices;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

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