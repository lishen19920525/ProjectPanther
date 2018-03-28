package io.panther.bundle;

/**
 * Created by LiShen on 2018/3/28.
 * ProjectPanther
 */

public class DataJsonBundle {
    private String key;
    private String data;
    private Long updateTime;

    public DataJsonBundle() {
        key = "";
        data = "";
        updateTime = 0L;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }
}