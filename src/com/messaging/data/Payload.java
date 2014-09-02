package com.messaging.data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Arrays;

public class Payload implements Serializable {

    private static final long serialVersionUID = 1L;
    private CallName callName;
    private MessageType msgType;
    private String isMedia;
    private byte[] message;
    private Timestamp timeStamp;
    private String toUser;
    private String fromUser;
    private String mediaFileName;

    public Payload() {
        // TODO Auto-generated constructor stub
    }

    public CallName getCallName() {
        return callName;
    }

    public MessageType getMsgType() {
        return msgType;
    }

    public String getMediaFileName() {
        return mediaFileName;
    }

    public void setCallName(CallName callName) {
        this.callName = callName;
    }

    public void setMsgType(MessageType msgType) {
        this.msgType = msgType;
    }

    public void setMediaFileName(String mediaFileName) {
        this.mediaFileName = mediaFileName;
    }

    public String getIsMedia() {
        return isMedia;
    }

    public byte[] getMessage() {
        return message;
    }

    public Timestamp getTimeStamp() {
        return timeStamp;
    }

    public String getToUser() {
        return toUser;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setIsMedia(String isMedia) {
        this.isMedia = isMedia;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }

    public void setTimeStamp(Timestamp timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    // public String toString() {
    // return "Payload [callName="
    // + callName
    // + ", msgType="
    // + msgType
    // + ", isMedia="
    // + isMedia
    // + ", message="
    // + (message != null ? arrayToString(message, message.length)
    // : null) + ", timeStamp=" + timeStamp + ", toUser="
    // + toUser + ", fromUser=" + fromUser + "]";
    // }

    @Override
    public String toString() {
        return "Payload [callName=" + callName + ", msgType=" + msgType
                + ", isMedia=" + isMedia + ", message="
                + Arrays.toString(message) + ", timeStamp=" + timeStamp
                + ", toUser=" + toUser + ", fromUser=" + fromUser
                + ", mediaFileName=" + mediaFileName + "]";
    }

    private String arrayToString(Object array, int len) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        for (int i = 0; i < len; i++) {
            if (i > 0)
                buffer.append(", ");
            if (array instanceof byte[])
                buffer.append(((byte[]) array)[i]);
        }
        buffer.append("]");
        return buffer.toString();
    }

    public Payload(MessageType msgType, String isMedia, byte[] message, Timestamp timeStamp, String toUser, String fromUser, String mediaFileName, CallName callName) {
        this.msgType = msgType;
        this.isMedia = isMedia;
        this.message = message;
        this.timeStamp = timeStamp;
        this.toUser = toUser;
        this.fromUser = fromUser;
        this.mediaFileName = mediaFileName;
        this.callName = callName;
    }
}
