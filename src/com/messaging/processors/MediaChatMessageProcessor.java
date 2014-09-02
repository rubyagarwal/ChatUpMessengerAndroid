package com.messaging.processors;

import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;
import com.messaging.data.CallName;
import com.messaging.data.Config;
import com.messaging.data.MessageType;
import com.messaging.data.Payload;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.examples.LogTail;
import com.utils.RabbitMqHelper;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import java.io.*;
import java.sql.Timestamp;

/**
 * Created by Ruby on 8/5/2014.
 */
public class MediaChatMessageProcessor {

    private static final String TAG = "MediaChatMessageProcessor";

    public void processMediaChatMessageClick(String fileName, String toUserName) {
        // since the signed up user is asking for download, s/he becomes the toUserName for RabbitMq response.
        Log.d(TAG, "Media chat message clicked "+fileName);
        try {
            MessageType msgType = MessageType.Text;
            if (fileName.contains(Config.JPEG_FILE_SUFFIX) && !fileName.contains(" ")) {
                // received image. send message to server for downloading.
                msgType = MessageType.Image;
                Payload p = new Payload(msgType, Config.TRUE, fileName.getBytes(), new Timestamp(System.currentTimeMillis()), toUserName, toUserName, fileName, CallName.DownloadMedia);
                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                String json = ow.writeValueAsString(p);
                new sendDownloadReqMessage().execute(json);
            } else if (fileName.contains(Config.MP4_FILE_SUFFIX) && !fileName.contains(" ")) {
                msgType = MessageType.Video;
                Payload p = new Payload(msgType, Config.TRUE, fileName.getBytes(), new Timestamp(System.currentTimeMillis()), toUserName, toUserName, fileName, CallName.DownloadMedia);
                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                String json = ow.writeValueAsString(p);
                new sendDownloadReqMessage().execute(json);
            }
            else if (fileName.contains(Config.XLS_FILE_SUFFIX) && !fileName.contains(" ")) { //agarub
                msgType = MessageType.Contract;
                Payload p = new Payload(msgType, Config.TRUE, fileName.getBytes(), new Timestamp(System.currentTimeMillis()), toUserName, toUserName, fileName, CallName.DownloadMedia);
                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                String json = ow.writeValueAsString(p);
                new sendDownloadReqMessage().execute(json);
            }
        } catch (IOException ioe) {
            Log.e(TAG, "IOE");
            ioe.printStackTrace();
        }
    }

    private class sendDownloadReqMessage extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
            Connection connection = new RabbitMqHelper().createConnection();
            Channel channel = connection.createChannel();
            // sender
            channel.exchangeDeclare(Config.ALL_MSGS_EXCHANGE, "fanout", true);
            channel.queueDeclare(Config.ALL_MSGS_QUEUE, false, false, false, null);
            Log.d(TAG,"Send download request message " +params[0]);
            channel.basicPublish(Config.ALL_MSGS_EXCHANGE, Config.ALL_MSGS_QUEUE, null, params[0].getBytes("utf-8"));
            channel.close();
            connection.close();
            return null;
            } catch (IOException ioe){
                Log.e(TAG, "IOE");
                ioe.printStackTrace();
            }
            return null;
        }

    }

    public byte[] read(String aInputFileName) {
        Log.d("ChatActivity", "Reading in binary file named : " + aInputFileName);
        File file = new File(aInputFileName);
        System.out.println("File size: " + file.length());
        byte[] result = new byte[(int) file.length()];
        try {
            InputStream input = null;
            input = new FileInputStream(aInputFileName);
            input.read(result);
            System.out.println(input.toString());
            input.close();

        } catch (FileNotFoundException ex) {
            // log("File not found.");
        } catch (IOException ex) {
            // log(ex);
        }
        return result;
    }
}
