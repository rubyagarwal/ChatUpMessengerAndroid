package com.utils;

import com.messaging.data.Config;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;

/**
 * Created by Ruby on 8/5/2014.
 */
public class RabbitMqHelper {

    private static final String TAG = "CreatePublisherChannel";
     public Connection createConnection() throws IOException{
         ConnectionFactory factory = new ConnectionFactory();
         factory.setHost(Config.IP);
         factory.setUsername(Config.USERNAME);
         factory.setPassword(Config.PASSWORD);
         Connection connection = factory.newConnection();
         return connection;
     }
}
