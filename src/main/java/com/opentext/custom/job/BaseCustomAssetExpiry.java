package com.opentext.custom.job;

import com.artesia.common.exception.BaseTeamsException;
import com.artesia.common.prefs.PrefData;
import com.artesia.common.prefs.PrefDataId;
import com.artesia.security.SecuritySession;
import com.artesia.system.services.SystemServices;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;

import static com.opentext.custom.constant.Constants.TEAMS_HOME;

/**
 * BaseExample is a super class to all the sample activity related
 * example classes. It just reads the data from the property file.
 */
public class BaseCustomAssetExpiry {
    private static final Log log = LogFactory.getLog(BaseCustomAssetExpiry.class);

    private BaseCustomAssetExpiry() {
    }

    static {
        if (null == System.getProperty(TEAMS_HOME)) {
            Properties props = System.getProperties();
            props.setProperty(TEAMS_HOME, System.getenv(TEAMS_HOME));
        }
    }

    public static String getSystemInformation(final String comp, final String key, final String name, final SecuritySession session)
            throws BaseTeamsException {
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
