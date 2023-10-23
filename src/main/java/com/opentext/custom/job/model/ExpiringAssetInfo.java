package com.opentext.custom.job.model;

public class ExpiringAssetInfo{
    private String flag;
    private String assetName;
    private String assetOwner;
    private String expiryDate;
    private String assetId;
    private String folderPath;
    private String hostName;
    private String nextNotifyDate;
    private String currentAttempt;
    private String totalAttempt;

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetOwner() {
        return assetOwner;
    }

    public void setAssetOwner(String assetOwner) {
        this.assetOwner = assetOwner;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getNextNotifyDate() {
        return nextNotifyDate;
    }

    public void setNextNotifyDate(String nextNotifyDate) {
        this.nextNotifyDate = nextNotifyDate;
    }

    public String getCurrentAttempt() {
        return currentAttempt;
    }

    public void setCurrentAttempt(String currentAttempt) {
        this.currentAttempt = currentAttempt;
    }

    public String getTotalAttempt() {
        return totalAttempt;
    }

    public void setTotalAttempt(String totalAttempt) {
        this.totalAttempt = totalAttempt;
    }

    @Override
    public String toString() {
        return "ExpiringAssetInfo{" +
                "flag='" + flag + '\'' +
                ", assetName='" + assetName + '\'' +
                ", assetOwner='" + assetOwner + '\'' +
                ", expiryDate='" + expiryDate + '\'' +
                ", assetId='" + assetId + '\'' +
                ", folderPath='" + folderPath + '\'' +
                ", hostName='" + hostName + '\'' +
                ", nextNotifyDate='" + nextNotifyDate + '\'' +
                ", currentAttempt='" + currentAttempt + '\'' +
                ", totalAttempt='" + totalAttempt + '\'' +
                '}';
    }
}