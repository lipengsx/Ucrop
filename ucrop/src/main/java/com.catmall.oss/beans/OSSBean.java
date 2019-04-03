package com.catmall.oss.beans;

import java.io.Serializable;

public class OSSBean implements Serializable {
    private String expiration;
    private String accessKeyId;
    private String accessKeySecrect;
    private String securityToken;
    private String outEndPoint;
    private String bucket;

    public String getExpiration() {
        return expiration;
    }

    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecrect() {
        return accessKeySecrect;
    }

    public void setAccessKeySecrect(String accessKeySecrect) {
        this.accessKeySecrect = accessKeySecrect;
    }

    public String getSecurityToken() {
        return securityToken;
    }

    public void setSecurityToken(String securityToken) {
        this.securityToken = securityToken;
    }

    public String getOutEndPoint() {
        return outEndPoint;
    }

    public void setOutEndPoint(String outEndPoint) {
        this.outEndPoint = outEndPoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }
}
