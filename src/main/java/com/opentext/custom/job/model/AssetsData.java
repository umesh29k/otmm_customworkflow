package com.opentext.custom.job.model;

import java.util.List;

public class AssetsData {
    private List<ExpiringAssetInfo> expiringAssetInfoList;

    public List<ExpiringAssetInfo> getExpiringAssetInfoList() {
        return expiringAssetInfoList;
    }

    public void setExpiringAssetInfoList(List<ExpiringAssetInfo> expiringAssetInfoList) {
        this.expiringAssetInfoList = expiringAssetInfoList;
    }
}
