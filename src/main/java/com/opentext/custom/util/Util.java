package com.opentext.custom.util;

import com.artesia.common.exception.BaseTeamsException;
import com.artesia.security.SecuritySession;
import com.artesia.security.session.services.LocalAuthenticationServices;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Util {
    private static final Log log = LogFactory.getLog(Util.class);
    public static final String API_HOST = "http://192.168.29.27:11090/otmmapi/v6/";
    private static RestTemplate restTemplate = new RestTemplate();

    public static String getLocalSessionDigest() {
        String id = "";
        try {
            SecuritySession session = LocalAuthenticationServices.getInstance().createSession("tsuper");
            id = session.getMessageDigest();
        } catch (BaseTeamsException e) {
            throw new RuntimeException(e);
        }
        return id;
    }

    public static List<String> updateRendition(String assetId) {
        List<String> ids = new ArrayList<>();
        try {
            String sessionDigest = getLocalSessionDigest();
            log.info("update_rendition REST invocation: start");
            log.info("update_rendition session digest: " + sessionDigest);
            MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();

            requestBody.add("rendition_type", "preview");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.add("otmmauthtoken", sessionDigest);
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> entity = restTemplate.exchange(API_HOST + "assets/" + assetId + "/contents", HttpMethod.POST, request, String.class);

            log.info("update_rendition status code for preview: " + entity.getStatusCode());
            log.info("update_rendition status code value for preview: " + entity.getStatusCodeValue());
            log.info("update_rendition entity_body for preview: " + entity.getBody());

            requestBody.add("rendition_type", "thumbnail");
            request = new HttpEntity<>(requestBody, headers);
            entity = restTemplate.exchange(API_HOST + "assets/" + assetId + "/contents", HttpMethod.POST, request, String.class);

            log.info("update_rendition status code for thumbnail: " + entity.getStatusCode());
            log.info("update_rendition status code value for thumbnail: " + entity.getStatusCodeValue());
            log.info("update_rendition entity_body for thumbnail: " + entity.getBody());
        } catch (Exception e) {
            log.error("update_rendition error: ", e);
        }
        return ids;
    }

}