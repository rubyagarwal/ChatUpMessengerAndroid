package com.core.InstantMessaging;

import android.os.Handler;
import android.util.Log;
import com.messaging.data.CallName;
import com.messaging.data.Config;
import com.messaging.data.Payload;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
*Consumes messages from a RabbitMQ broker
*
*/
public class MessageConsumer{

    //The Queue name for this consumer
    private QueueingConsumer MySubscription;
    public String mExchange;
    protected boolean Running ;
    protected String signUpUser;
  //last message to post back
    private byte[] mLastMessage;
    private String TAG = "MessageConsumer";
    Channel channel;

    public MessageConsumer(String exchange, String signUser) {
        mExchange = exchange;
        signUpUser = signUser;
    }

  // An interface to be implemented by an object that is interested in messages(listener)
  public interface OnReceiveMessageHandler{
	  
      public void onReceiveMessage(byte[] message);
  };

    public interface OnReceiveChatMessageHandler{

        public void onReceiveChatMessage(byte[] message);
    };

  //A reference to the listener, we can only have one at a time(for now)
  private OnReceiveMessageHandler mOnReceiveMessageHandler;
    private OnReceiveChatMessageHandler mOnReceiveChatMessageHandler;

  /**
   *
   * Set the callback for received messages
   * @param handler The callback
   */
  public void setOnReceiveMessageHandler(OnReceiveMessageHandler handler){
      mOnReceiveMessageHandler = handler;
  };
    public void setOnReceiveChatMessageHandler(OnReceiveChatMessageHandler cHandler){
        mOnReceiveChatMessageHandler = cHandler;
    };

  private Handler mMessageHandler = new Handler();
  private Handler mConsumeHandler = new Handler();
    private Handler chatHandler = new Handler();

  // Create runnable for posting back to main thread
  final Runnable mReturnMessage = new Runnable() {
      public void run() {
          mOnReceiveMessageHandler.onReceiveMessage(mLastMessage);
      }
  };


    final Runnable chatMessage = new Runnable() {
        public void run() {
            mOnReceiveChatMessageHandler.onReceiveChatMessage(mLastMessage);
        }
    };


  final Runnable mConsumeRunner = new Runnable() {
      public void run() {
          Consume();
      }
  };

  /**
   * Create Exchange and then start consuming. A binding needs to be added before any messages will be delivered
   */
  public boolean connectToRabbitMQ()
  {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(Config.IP);
            factory.setUsername(Config.USERNAME);
            factory.setPassword(Config.PASSWORD);
            Connection connection = factory.newConnection();
            channel = connection.createChannel();

            channel.exchangeDeclare(mExchange, "direct", true);
            //String queueName = channel.queueDeclare().getQueue();

            String binding = signUpUser.toLowerCase().replace(" ",""); // say sign up user.
            String queueName = binding;
            channel.queueDeclare(queueName,true,false, false, null);
            channel.queueBind(queueName, mExchange, binding);
            Log.d(TAG, "creating consumer exchange " + mExchange +
                    " queue " + queueName + " binding " + binding);
            MySubscription = new QueueingConsumer(channel);
             channel.basicConsume(queueName, false, MySubscription);
            Running = true;
            mConsumeHandler.post(mConsumeRunner);
        } catch (IOException ioe){
            Log.e(TAG,"IOE");
            ioe.printStackTrace();
        }

        return true;
  }

  private void Consume()
  {
      Thread thread = new Thread()
      {

           @Override
              public void run() {
               while(Running){
                  QueueingConsumer.Delivery delivery;
                  try {
                      delivery = MySubscription.nextDelivery();
                      mLastMessage = delivery.getBody();
                      ObjectMapper objectMapper = new ObjectMapper();
                      Payload p = (Payload) objectMapper.readValue(mLastMessage, Payload.class);
                      Log.d(TAG,"{RECEIVED}" + p.getCallName());
                      if(CallName.Chat.equals(p.getCallName())) {
                          Log.d(TAG,"Chat response");
                          chatHandler.post(chatMessage);
                      } else if(CallName.DownloadMedia.equals(p.getCallName())) {
                          Log.d(TAG, "Download media response");
                          chatHandler.post(chatMessage);
                      } else if(CallName.GetConversationHistory.equals(p.getCallName())) {
                          Log.d(TAG, "Get conv history response");
                          chatHandler.post(chatMessage);
                      }
                      channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                  } catch (InterruptedException ie) {
                      ie.printStackTrace();
                  } catch (IOException ioe) {
                      ioe.printStackTrace();
                  }
               }
           }
      };
      thread.start();

  }

  public void dispose(){
      Running = false;
  }
}
