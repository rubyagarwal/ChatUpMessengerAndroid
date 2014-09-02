package com.messaging.processors;

import android.os.AsyncTask;
import android.util.Log;
import com.messaging.data.CallName;
import com.messaging.data.Config;
import com.messaging.data.MessageType;
import com.messaging.data.Payload;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.utils.RabbitMqHelper;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ruby on 8/11/2014.
 */
public class ConversationHistoryProcessor {

    private static final String TAG = "ConversationHistoryProcessor";

    public void getConversationHistory(String to, String from) {
        Log.d(TAG, "Get conv history for " + to + " and " + from);
        List<String> history = new ArrayList<String>();

        Payload p = new Payload();
        p.setMessage("".getBytes());
        p.setCallName(CallName.GetConversationHistory);
        p.setFromUser(from);
        p.setIsMedia("false");
        p.setMediaFileName("");
        p.setMsgType(MessageType.Text);
        p.setTimeStamp(new Timestamp(System.currentTimeMillis()));
        p.setToUser(to);

        try {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(p);
            Log.d(TAG,"Sending message to get conv history");
            new PublishMessage().execute(json);
        } catch (IOException ioe) {
            Log.e(TAG, "IOE");
            ioe.printStackTrace();
        }
    }
    private class PublishMessage extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {

                Connection connection = new RabbitMqHelper().createConnection();
                Channel channel = connection.createChannel();
                // sender
                channel.exchangeDeclare(Config.ALL_MSGS_EXCHANGE, "fanout", true);
                channel.queueDeclare(Config.ALL_MSGS_QUEUE, false, false, false, null);

                channel.basicPublish(Config.ALL_MSGS_EXCHANGE, Config.ALL_MSGS_QUEUE, null, params[0].getBytes("utf-8"));
                channel.close();
                connection.close();
            } catch (Exception e) {
                Log.e(TAG, "Exception");
                e.printStackTrace();
            }
            return null;
        }
    }
}
