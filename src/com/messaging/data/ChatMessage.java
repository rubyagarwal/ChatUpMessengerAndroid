package com.messaging.data;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Message;
import android.widget.ImageView;

/**
 * Created by Ruby on 7/27/2014.
 */
public class ChatMessage {
    public boolean left;
    public String message;
    public Bitmap bitMap;
    public MessageType msgType;
    public Uri videoUri;

//    public ChatMessage (boolean left, String message) {
//        super();
//        this.left = left;
//        this.message = message;
//    }
    public ChatMessage (boolean left, String message, Bitmap bitMap, MessageType msgType, Uri videoUri) {
        super();
        this.left = left;
        this.message = message;
        this.bitMap=bitMap;
        this.msgType=msgType;
        this.videoUri=videoUri;
    }


}
