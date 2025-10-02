package com.hanserwei.hannote.auth.alarm;

public interface AlarmInterface {

    /**
     * 发送告警信息
     *
     * @param message 告警信息
     * @return 发送结果
     */
    boolean send(String message);
}