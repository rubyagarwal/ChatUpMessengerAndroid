package com.core.InstantMessaging;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import com.messaging.data.*;
import com.messaging.processors.ConversationHistoryProcessor;
import com.messaging.processors.DataCompressionHelper;
import com.messaging.processors.MediaChatMessageProcessor;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.utils.RabbitMqHelper;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import java.io.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//contract import
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.util.Linkify;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


/**
 * Created by Ruby on 7/27/2014.
 */
public class ChatActivity extends Activity {

    private ChatArrayAdapter chatArrayAdapter;
    private ListView listView;
    private EditText chatText;
    private Button buttonSend;
    private Button picBtn;
    private Button vidBtn;
    private Button contractBtn;

    private Button docBtn;
    private Button xlBtn;
    private Button onsBtn;

    private String toUserName;
    private String chatMsg;
    private String signUpUser;
    private MessageConsumer mConsumer;
    private List<String> convHistory = new ArrayList<String>();
    private String TAG = "ChatActivity";
    protected AlbumStorageDirFactory mAlbumStorageDirFactory = null;
    private String mCurrentFilePath;
    private ImageView mImageView;

    private String mlastclickedmediapath;

    //all contract var
    protected static EditText text = null;
    protected static TextView title = null;

    private final static int FILEFORMAT_NL = 1;
    private final static int FILEFORMAT_CR = 2;
    private final static int FILEFORMAT_CRNL = 3;
    private int fileformat;
    protected CharSequence filename = "";
    protected long lastModified = 0;
    protected boolean untitled = true;
    private boolean creatingFile = false;
    private boolean savingFile = false;
    private boolean errorSaving = false;
    private boolean openingFile = false;

    static private List<String> recentItems = null;

    private String mCurrentContractPath;
    private static CharSequence temp_filename = "";
    private CharSequence newFilename = null;
    private CharSequence errorFname = "File";
    private boolean openingError = false;
    private boolean openingRecent = false;
    private boolean openingIntent = false;




    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,">>>>>>>>>>>>Starting this activity<<<<<<<<<<<<<<");
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        toUserName = i.getStringExtra("toUser");
        signUpUser = i.getStringExtra("signUpUser");
        setTitle(toUserName);
        setContentView(R.layout.chat_activity);

        // Create the consumer
        mConsumer = new MessageConsumer(Config.CHAT_ONLY_EXCHANGE, signUpUser);
        new consumerConnect().execute();

        // here send a request to our server to get the previous conv between the two users.
       new ConversationHistoryProcessor().getConversationHistory(toUserName, signUpUser);
        if (i.hasExtra("msg")) {
            Log.d(TAG, "MSG exists : Notification clicked / sending contract.");
            convHistory.add(i.getStringExtra("msg"));
        }

        // fetch items in view
        buttonSend = (Button) findViewById(R.id.buttonSend);
        listView = (ListView) findViewById(R.id.listView1);
        mImageView = (ImageView) findViewById(R.id.imageView1);

        chatArrayAdapter = new ChatArrayAdapter(getApplicationContext(), R.layout.activity_chat_singlemessage);
        listView.setAdapter(chatArrayAdapter);
        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatArrayAdapter);
        chatArrayAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatArrayAdapter.getCount() - 1);
            }
        });

        // chat message click
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChatMessage msg = (ChatMessage)parent.getItemAtPosition(position);
                new MediaChatMessageProcessor().processMediaChatMessageClick(msg.message, signUpUser);
            }
        });

        // register for messages
        mConsumer.setOnReceiveChatMessageHandler(new MessageConsumer.OnReceiveChatMessageHandler() {

            public void onReceiveChatMessage(byte[] message) {
                String text = "";
                Bitmap bitmap = null;
                Payload p = new Payload();
                Uri videoUri = null;
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    p = (Payload) objectMapper.readValue(message, Payload.class);
                    Log.d(TAG, "Received response " + p.toString());
                    if(CallName.Chat.equals(p.getCallName())) {
                        if (Config.TRUE.equalsIgnoreCase(p.getIsMedia())) {
                            text = p.getMediaFileName();
                        } else {
                            text = new String(p.getMessage(), "UTF8");
                        }
                    } else if(CallName.DownloadMedia.equals(p.getCallName())) {
                        // here save media on the receiving end
                        text = "Received " + p.getMediaFileName();
                        byte [] decompressed = new DataCompressionHelper().decompressData(p.getMessage());
                        if(MessageType.Image.equals(p.getMsgType())) {
                            ByteArrayInputStream inputStream = new ByteArrayInputStream(decompressed);
                            bitmap = BitmapFactory.decodeStream(inputStream);
                            saveReceivedImageOnDevice(p.getMediaFileName());
                        } else if(MessageType.Video.equals(p.getMsgType())) {
                            String videoLocation=saveReceivedVideoOnDevice(p.getMediaFileName(), decompressed);
                            videoUri = Uri.parse(videoLocation);
                        } else if(MessageType.Contract.equals(p.getMsgType())) {
                            saveReceivedContractOnDevice(decompressed, p.getMediaFileName());
                        }
                    } else if(CallName.GetConversationHistory.equals(p.getCallName())){
                        String msg = new String(p.getMessage(), "UTF-8");
                        //Log.d(TAG,"MSG "+msg);
                        if(!msg.isEmpty()) {
                            String [] chatArr = msg.split("&");
                            for (String chat : chatArr) {
                                if(! chat.isEmpty()) {
                                    convHistory.add(chat);
                                }
                            }
                        }
                        // set previous conversation.
                        for (String chat : convHistory) {
                            String[] splits = chat.split(":");
                            if(splits.length==2 && !splits[1].isEmpty()) {
                                if (signUpUser.equalsIgnoreCase(splits[0])) {
                                    // I sent the message
                                    chatArrayAdapter.add(new ChatMessage(false, splits[1], null, MessageType.Text, null));
                                } else {
                                    // I received the message,
                                    chatArrayAdapter.add(new ChatMessage(true, splits[1], null, MessageType.Text, null));
                                }
                            }
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    Log.d(TAG, "UEE");
                    e.printStackTrace();
                } catch (IOException ioe) {
                    Log.d(TAG,"IOE");
                    ioe.printStackTrace();
                }

                if(!text.isEmpty()) {
                    createNotificationForDevice(text);
                    chatArrayAdapter.add(new ChatMessage(true, text, bitmap, p.getMsgType(), videoUri));
                }

            }
        });

        chatText = (EditText) findViewById(R.id.chatText);
        chatText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    Payload p = new Payload(MessageType.Text, Config.FALSE,chatText.getText().toString().getBytes(), new Timestamp(System.currentTimeMillis()), toUserName, signUpUser,"",CallName.Chat);
                    return sendChatMessage(p, Config.FALSE);
                }
                return false;
            }
        });

        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Payload p = new Payload(MessageType.Text, Config.FALSE,chatText.getText().toString().getBytes(), new Timestamp(System.currentTimeMillis()), toUserName, signUpUser,"",CallName.Chat);
                sendChatMessage(p, Config.FALSE);
            }
        });

        // capture image
        picBtn = (Button) findViewById(R.id.buttonPicture);
        setBtnListenerOrDisable(picBtn, mTakePicOnClickListener, MediaStore.ACTION_IMAGE_CAPTURE);
        vidBtn = (Button) findViewById(R.id.buttonVideo);
        setBtnListenerOrDisable(vidBtn, mTakeVidOnClickListener, MediaStore.ACTION_VIDEO_CAPTURE);

        contractBtn = (Button) findViewById(R.id.buttonContract);
        contractBtn.setOnClickListener(mCreateContractOnClickListener);

        // agarub
        docBtn = (Button) findViewById(R.id.docBtn);
        xlBtn = (Button) findViewById(R.id.xlBtn);
        onsBtn = (Button) findViewById(R.id.openBtn);

        docBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                documentButtonListener();
            }
        });

        xlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                excelButtonListener();
            }
        });

        onsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAndSendListener();
            }
        });
        initializeStorageDirFactory();

    }

    private void documentButtonListener(){
//        initializeStorageDirFactory();
//        CharSequence fileName = createDocFile();
//        FileInputStream in = null;
//        HWPFDocument hw = null;
//        FileOutputStream out = null;
//        File file = new File(fileName.toString());
//        try {
//            in = new FileInputStream("/sdcard/doc/dumy.doc");
//            hw = new HWPFDocument(in);
//            out = new FileOutputStream(file);
//            hw.write(out);
//        } catch (FileNotFoundException fnfe){
//            Log.e(TAG,"FileNotFoundException");
//            fnfe.printStackTrace();
//        } catch (IOException ioe){
//            Log.e(TAG,"IOException");
//            ioe.printStackTrace();
//        }
//        openAnyFile(file.getAbsolutePath());
    }

    private void excelButtonListener(){
        initializeStorageDirFactory();
        CharSequence fileName = createExcelFile();
        Workbook wb = new HSSFWorkbook();
        File file = new File( fileName.toString());
        FileOutputStream os = null;
        try{
            os = new FileOutputStream(file);
            wb.write(os);
            Log.d(TAG,"Created excel at location "+fileName);
        } catch (IOException ioe) {
            Log.e(TAG, "IOE");
            ioe.printStackTrace();
        } catch (Exception ioe) {
            Log.e(TAG, "Exception");
            ioe.printStackTrace();
        } finally {
            try {
                if(os!=null)
                    os.close();
            } catch (Exception e){
                Log.e(TAG, "Exception");
                e.printStackTrace();
            }

        }
        Log.d(TAG,"Superstition");
        openAnyFile(file.getAbsolutePath());
    }

    private void openAndSendListener(){
        Intent i = new Intent(getApplicationContext(),FileExplore.class);
        i.putExtra("to",toUserName);
        i.putExtra("from", signUpUser);
        startActivity(i);
        finish();
    }

    private String saveReceivedVideoOnDevice(String mediaFileName, byte[] decompressed) {
        initializeStorageDirFactory();
        File albumF = getAlbumDir();
        String videoLocation=albumF.getAbsolutePath().concat("/").concat(mediaFileName);
        Log.d(TAG,"Saving video on device at location : " + videoLocation);
        try{
            FileOutputStream out = new FileOutputStream(videoLocation);
            out.write(decompressed);
            out.close();
        } catch (FileNotFoundException fnf){
            Log.e(TAG, "FNF");
            fnf.printStackTrace();
        } catch (IOException ioe) {
            Log.e(TAG, "IOE");
            ioe.printStackTrace();
        }
        galleryAddMedia(videoLocation);
        Log.d(TAG, "Saved video");
        return videoLocation;
    }

    private void saveReceivedImageOnDevice(String imageFileName){
        initializeStorageDirFactory();
        File albumF = getAlbumDir();
        String imgLocation=albumF.getAbsolutePath().concat("/").concat(imageFileName);
        Log.d(TAG, " Saving image on device at location" + imgLocation);
        galleryAddMedia(imgLocation);
        Log.d(TAG,"Saved image");
    }

    private void saveReceivedContractOnDevice(byte [] decompressed, String mediaFileName) {
        initializeStorageDirFactory();
        File albumF = getAlbumDir();
        String textFileLoc=albumF.getAbsolutePath().concat("/").concat(mediaFileName);
        Log.d(TAG, "Saving contract on device at location "+mediaFileName);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(textFileLoc);
            System.out.println(decompressed.length);
            out.write(decompressed);
            out.close();
            openAnyFile(textFileLoc); // agarub
            //openContractFile(textFileLoc);
        } catch (IOException e) {
            Log.d(TAG,"IOE");
            e.printStackTrace();
        }
        Log.d(TAG,"Saved contract");
    }

    private void openAnyFile(String fileN){
        Log.d(TAG,"Opening file  " + fileN);
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
        File file = new File(fileN);
        String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
        String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (extension.equalsIgnoreCase("") || mimetype == null)
        {

// if there is no extension or there is no definite mimetype, still try to open the file
            intent.setDataAndType(Uri.fromFile(file), "text/*");
        }
        else
        {
            intent.setDataAndType(Uri.fromFile(file), mimetype);
        }
// custom message for the intent
        startActivity(Intent.createChooser(intent, "Choose an Application:"));
    }

    private void initializeStorageDirFactory(){
        if(null==mAlbumStorageDirFactory){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
            } else {
                mAlbumStorageDirFactory = new BaseAlbumDirFactory();
            }
        }

    }
    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    // TODO
    private void createNotificationForDevice(String msg) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notify)
                .setContentTitle("Message from " + toUserName)
                .setContentText(msg);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, ChatActivity.class);
        resultIntent.putExtra("toUser", toUserName);
        resultIntent.putExtra("signUpUser", signUpUser);
        resultIntent.putExtra("msg", toUserName + ":" + msg);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(ChatActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int mId = 1;
        mBuilder.setAutoCancel(true);
        mNotificationManager.notify(mId, mBuilder.build());
    }


    private class consumerConnect extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... Message) {
            try {
                // Connect to broker
                mConsumer.connectToRabbitMQ();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    public boolean sendChatMessage(Payload p, String isMedia) {
        Log.d(TAG, "Sending chat message");
        try {
            byte [] compressed=null;
            if(Config.TRUE.equalsIgnoreCase(p.getIsMedia())){
                compressed = new DataCompressionHelper().compressData(p.getMessage());
                p.setMessage(compressed);
            }

            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(p);

            new sendMsg().execute(json, isMedia);
            //updating the current device
            if(Config.TRUE.equalsIgnoreCase(isMedia)) {
                chatArrayAdapter.add(new ChatMessage(false, "Sent media ".concat(p.getMediaFileName()),null,p.getMsgType(), null));
            } else {
                chatArrayAdapter.add(new ChatMessage(false, chatText.getText().toString(),null,p.getMsgType(), null));
            }
            chatMsg = chatText.getText().toString();
            chatText.setText("");
        } catch (Exception e) {
            Log.e(TAG, "UEE");
            e.printStackTrace();
        }
        return true;
    }


    private class sendMsg extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
                Log.d(TAG, "IS MEDIA : " + params[1]);
                Connection connection = new RabbitMqHelper().createConnection();
                Channel channel = connection.createChannel();
                // sender
                channel.exchangeDeclare(Config.ALL_MSGS_EXCHANGE, "fanout", true);
                channel.queueDeclare(Config.ALL_MSGS_QUEUE, false, false, false, null);
                // message

                if (Config.TRUE.equalsIgnoreCase(params[1])) {
                    // media message
                    AMQP.BasicProperties.Builder bob = new AMQP.BasicProperties.Builder();
                    Integer prio = new Integer(0);
                    AMQP.BasicProperties persistentBasic = bob.priority(prio).contentType("application/octet-stream").contentEncoding("UTF-8").build();
                    channel.basicPublish(Config.ALL_MSGS_EXCHANGE, Config.ALL_MSGS_QUEUE, persistentBasic, params[0].getBytes("utf-8"));
                } else {
                    // text only
                    channel.basicPublish(Config.ALL_MSGS_EXCHANGE, Config.ALL_MSGS_QUEUE, null, params[0].getBytes("utf-8"));
                }
                Log.d(TAG, "Sending message exchange " + Config.ALL_MSGS_EXCHANGE + " queue " + Config.ALL_MSGS_QUEUE);
                channel.close();
                connection.close();

            } catch (IOException ioe) {
                Log.d(TAG, "sendMsg IOE");
                ioe.printStackTrace();
            }
            return null;
        }
    }



    private String getAlbumName() {

        return getString(R.string.album_name);
    }

    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            //Log.d("Storage Directory",mAlbumStorageDirFactory.toString());
            storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());
            if (storageDir != null) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
                        Log.d(TAG, "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(TAG + ":" + getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    private File createMediaFile(int mediaType) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File albumF = getAlbumDir();
        File mediaF = null;
        switch(mediaType) {
            case Config.PHOTO:
                String imageFileName = Config.JPEG_FILE_PREFIX + timeStamp + "_";
                mediaF = File.createTempFile(imageFileName, Config.JPEG_FILE_SUFFIX, albumF);
                break;
            case Config.VIDEO:
                String videoFileName = Config.MP4_FILE_PREFIX + timeStamp + "_";
                mediaF = File.createTempFile(videoFileName, Config.MP4_FILE_SUFFIX, albumF);
                break;

        }
        return mediaF;
    }

    private File setUpMediaFile(int mediaType) throws IOException {

        File f = createMediaFile(mediaType);
        mCurrentFilePath = f.getAbsolutePath();

        return f;
    }


    private void galleryAddMedia(String filePath) {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        File f = new File(filePath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void dispatchTakeMediaIntent(int mediaType) {

        Intent takeMediaIntent = null;
        switch(mediaType) {
            case Config.PHOTO:
                takeMediaIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                break;
            case Config.VIDEO:
                takeMediaIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                break;
            case Config.AUDIO:

                break;
            case Config.CONTRACT:

                break;


        }
        File f = null;

        try {
            f = setUpMediaFile(mediaType);
            mCurrentFilePath = f.getAbsolutePath();
            if(takeMediaIntent != null) {
                takeMediaIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                setResult(RESULT_OK, takeMediaIntent);
            }
        } catch (IOException e) {
            e.printStackTrace();
            f = null;
            mCurrentFilePath = null;
        }
        switch(mediaType) {
            case Config.PHOTO: {
                startActivityForResult(takeMediaIntent, Config.PHOTO);
                break;
            }
            case Config.VIDEO: {
                startActivityForResult(takeMediaIntent, Config.VIDEO);
                break;
            }
        }


    }

    private void handleCameraPhoto() {

        if (mCurrentFilePath != null) {
            Log.d(TAG,"handle path nt null " + mCurrentFilePath);
            //setPic();
            galleryAddMedia(mCurrentFilePath);
            mlastclickedmediapath = mCurrentFilePath;
            mCurrentFilePath = null;
        }

    }

    private void handleCameraVideo(Intent intent) {
        if (mCurrentFilePath != null) {
            //setPic();
            galleryAddMedia(mCurrentFilePath);
            mlastclickedmediapath = mCurrentFilePath;
            mCurrentFilePath = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        try {
            switch (requestCode) {
                case Config.PHOTO: {
                    if (resultCode == RESULT_OK) {
                            handleCameraPhoto();
                            Thread.sleep(Config.SLEEP_TIME);
                            String path = mlastclickedmediapath;
                            Log.d("TAG", "PATH : " + path);
                            String[] splits = path.split("/");
                            byte[] msgContents = new MediaChatMessageProcessor().read(path);
                            Payload p = new Payload(MessageType.Image, Config.TRUE, msgContents, new Timestamp(System.currentTimeMillis()), toUserName, signUpUser, splits[splits.length - 1], CallName.Chat);
                            sendChatMessage(p, Config.TRUE);
                    }

                    break;
                } // PHOTO

                case Config.VIDEO: {
                    if (resultCode == RESULT_OK) {
                        handleCameraVideo(data);
                        Thread.sleep(Config.SLEEP_TIME);
                        String path = mlastclickedmediapath;
                        Log.d("TAG", "PATH : " + path);
                        String[] splits = path.split("/");
                        byte[] msgContents = new MediaChatMessageProcessor().read(path);
                        Payload p = new Payload(MessageType.Video, Config.TRUE, msgContents, new Timestamp(System.currentTimeMillis()), toUserName, signUpUser, splits[splits.length - 1], CallName.Chat);
                        sendChatMessage(p, Config.TRUE);
                    }
                    break;
                } // ACTION_TAKE_VIDEO
            } // switch
        } catch (InterruptedException ie) {
            Log.e(TAG,"IE");
            ie.printStackTrace();
        }
    }

    // This will save the media on the device where it was created.
    Button.OnClickListener mTakePicOnClickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispatchTakeMediaIntent(Config.PHOTO);
                    Log.d(TAG, "Photo capturing path" + mCurrentFilePath);
                }



            };

    Button.OnClickListener mTakeVidOnClickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG,"Video capturing path" + mCurrentFilePath);
                    dispatchTakeMediaIntent(Config.VIDEO);
                }
            };

//    Button.OnClickListener mTakeAudOnClickListener =
//            new Button.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    dispatchTakeMediaIntent(Config.AUDIO);
//                }
//            };

    Button.OnClickListener mCreateContractOnClickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //dispatchTakeMediaIntent(Config.CONTRACT);
                    createNewContract();
                }
            };

    public void setBtnListenerOrDisable(
            Button btn,
            Button.OnClickListener onClickListener,
            String intentName
    ) {
        if (isIntentAvailable(this, intentName)) {
            btn.setOnClickListener(onClickListener);
        } else {
            btn.setText(
                    "Cannot " + btn.getText());
            btn.setClickable(false);
        }
    }

//all contract member


    public void createNewContract() {
        // set the new context
        setContentView(R.layout.edit);    // update options done below

        text = (EditText) findViewById(R.id.note);
        title = (TextView) findViewById(R.id.notetitle);

        try {
            if(newFilename == null)
                newFilename =  createTextFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        text.setText("");
        title.setText(R.string.newFileName);

        Button saveBtn = (Button) findViewById(R.id.buttonSave);
        Button.OnClickListener mCreateNewOnClickListener =
                new Button.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    //saveDocument(newFilename);agarub
                         saveContract(newFilename);
                    }
                };
        saveBtn.setOnClickListener(mCreateNewOnClickListener);

        Button contractBtn = (Button) findViewById(R.id.buttonSendContract);
        Button.OnClickListener mContractOnClickListener =
                new Button.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            sendContract();
//                            Log.d(TAG, "Creating intent for chat activity");
//                            Intent i = new Intent(getApplicationContext(),ChatActivity.class);
//                            i.putExtra("toUser", toUserName);
//                            i.putExtra("signUpUser",signUpUser);
//                            i.putExtra("msg",signUpUser+":Sending contract "+newFilename);
//                            startActivity(i);
//                            //finish();
                        } catch (Exception e) {
                            Log.e(TAG, "exception");
                            e.printStackTrace();
                        }
                    }
                };
        contractBtn.setOnClickListener(mContractOnClickListener);

        // clear the saved text
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        if (editor != null) {
            editor.putInt("mode", 1);

            editor.putString("text", "");
            editor.putInt("text-quotes", 0);

            editor.putString("fntext", title.getText().toString());
            editor.putInt("fntext-quotes", countQuotes(title.getText().toString()));

            editor.putString("filename", "");
            editor.putInt("filename-quotes", 0);

            editor.putInt("selection-start", -1);
            editor.putInt("selection-end", -1);
            editor.commit();
        }

        fileformat = FILEFORMAT_NL;
        filename = "";
        lastModified = 0;
        untitled = true;

        creatingFile = false;

        updateOptions();
        text.requestFocus();

    } // end createNewContract();

    public static int countQuotes(String t) // count " in string
    {
        int i = -1;
        int count = -1;

        do {
            i = t.indexOf('"', i + 1);
            count++;
        } while (i != -1);

        return count;
    } // end countQuotes()

    /**
     * *************************************************************
     * updateOptions()
     * start options app
     */
    protected void updateOptions() {
        boolean value;

        // load the preferences
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        //autoComplete = sharedPref.getBoolean("autocomplete", false);

        /********************************
         * Auto correct and auto case */
//        boolean autocorrect = sharedPref.getBoolean("autocorrect", false);
//        boolean autocase = sharedPref.getBoolean("autocase", false);
//
//        if (autocorrect && autocase)
//        {
//            setContentView(R.layout.edit_autotext_autocase);
//        } else if (autocorrect) {
//            setContentView(R.layout.edit_autotext);
//        } else if (autocase) {
//            setContentView(R.layout.edit_autocase);
//        } else {
//            setContentView(R.layout.edit);
//        }

        text = (EditText) findViewById(R.id.note);
        title = (TextView) findViewById(R.id.notetitle);

        text.addTextChangedListener(new TextWatcher() {

            public void onTextChanged(CharSequence one, int a, int b, int c) {

                // put a little star in the title if the file is changed
                if (!isTextChanged()) {
                    CharSequence temp = title.getText();
                    title.setText("* " + temp);
                }
            }

            // complete the interface
            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });

        /********************************
         * links clickable */
        boolean linksclickable = sharedPref.getBoolean("linksclickable", false);

        if (linksclickable)
            text.setAutoLinkMask(Linkify.ALL);
        else
            text.setAutoLinkMask(0);

        /********************************
         * show/hide filename */
        value = sharedPref.getBoolean("hidefilename", false);
        if (value)
            title.setVisibility(View.GONE);
        else
            title.setVisibility(View.VISIBLE);

        /********************************
         * line wrap */
        value = sharedPref.getBoolean("linewrap", true);
        text.setHorizontallyScrolling(!value);

        // setup the scroll view correctly
        ScrollView scroll = (ScrollView) findViewById(R.id.scroll);
        if (scroll != null) {
            scroll.setFillViewport(true);
            scroll.setHorizontalScrollBarEnabled(!value);
        }

        /********************************
         * font face */
        String font = sharedPref.getString("font", "Monospace");

        if (font.equals("Serif"))
            text.setTypeface(Typeface.SERIF);
        else if (font.equals("Sans Serif"))
            text.setTypeface(Typeface.SANS_SERIF);
        else
            text.setTypeface(Typeface.MONOSPACE);

        /********************************
         * font size */
        String fontsize = sharedPref.getString("fontsize", "Medium");

        if (fontsize.equals("Extra Small"))
            text.setTextSize(12.0f);
        else if (fontsize.equals("Small"))
            text.setTextSize(16.0f);
        else if (fontsize.equals("Medium"))
            text.setTextSize(20.0f);
        else if (fontsize.equals("Large"))
            text.setTextSize(24.0f);
        else if (fontsize.equals("Huge"))
            text.setTextSize(28.0f);
        else
            text.setTextSize(20.0f);

        /********************************
         * Colors */
        int bgcolor = sharedPref.getInt("bgcolor", 0xFF000000);
        text.setBackgroundColor(bgcolor);

        int fontcolor = sharedPref.getInt("fontcolor", 0xFFCCCCCC);
        text.setTextColor(fontcolor);

        title.setTextColor(bgcolor);
        title.setBackgroundColor(fontcolor);

        text.setLinksClickable(true);
    } // updateOptions()

    public static boolean isTextChanged()	// checks if the text has been changed
    {
        CharSequence temp = title.getText();

        try {	// was getting error on the developer site, so added this to "catch" it

            if (temp.charAt(0) == '*')
            {
                return true;
            }
        } catch (Exception e) {
            return false;
        }

        return false;
    } // end isTextChanged()

//    public void saveDocument(CharSequence fname){
//        try {
//            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
//            wordMLPackage.getMainDocumentPart().addParagraphOfText(text.getText().toString());
//            wordMLPackage.save(new java.io.File(fname.toString()));
//            addRecentFile(fname);
//        } catch (InvalidFormatException ife){
//            Log.e(TAG,"InvalidFormatException");
//            ife.printStackTrace();
//        } catch (Docx4JException d4e){
//            Log.e(TAG,"Docx4JException");
//            d4e.printStackTrace();
//        }
//    }

    public void saveContract(CharSequence fname)
    {
        Log.d(TAG,"Saving the contract " + fname);
        errorSaving = false;

        // actually save the file here
        try {
            File f = new File(fname.toString());

            if ( (f.exists() && !f.canWrite()) || (!f.exists() && !f.getParentFile().canWrite()))
            {
                creatingFile = false;
                openingFile = false;
                errorSaving = true;

                if (fname.toString().indexOf("/sdcard/") == 0)
                    showDialog(7);
                else
                    showDialog(6);

                text.requestFocus();

                f = null;
                return;
            }
            f = null; // hopefully this gets garbage collected

            // Create file
            FileWriter fstream = new FileWriter(fname.toString());
            BufferedWriter out = new BufferedWriter(fstream);

            Log.d(TAG,"File format " + fileformat);
            if (fileformat == FILEFORMAT_CR)
            {
                out.write(text.getText().toString().replace("\n", "\r"));
            } else if (fileformat == FILEFORMAT_CRNL) {
                out.write(text.getText().toString().replace("\n", "\r\n"));
            } else {
                out.write(text.getText().toString());
            }

            out.close();

            // give a nice little message
            Toast.makeText(this, R.string.onSaveMessage, Toast.LENGTH_SHORT).show();

            // the filename is the new title
            title.setText(fname);
            filename = fname;
            mCurrentContractPath=fname.toString();
            untitled = false;

            lastModified = (new File(filename.toString())).lastModified();

            temp_filename = "";

            addRecentFile(fname);
        } catch (Exception e) { //Catch exception if any
            creatingFile = false;
            openingFile = false;

            if (fname.toString().indexOf("/sdcard/") == 0)
                showDialog(5);
            else
                showDialog(5);

            errorSaving = true;
        }
        if(text != null)
        text.requestFocus();
    } // end saveNote()

    protected void readRecentFiles()
    {
        int i;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        int numFiles = prefs.getInt("rf_numfiles", 0);

        // clear the current list
        if (recentItems == null)
            recentItems = new ArrayList<String>();

        recentItems.clear();

        // start adding stuff
        for(i = 0; i < numFiles; i++)
        {
            recentItems.add(prefs.getString("rf_file" + i, i + ""));
        }
    } // end readRecentFiles()

    protected void addRecentFile(CharSequence f)
    {
        if (recentItems == null)
            readRecentFiles();

        // remove from list if it is already there
        int i;
        int length = recentItems.size();

        for(i = 0; i < length; i++)
        {
            String t = recentItems.get(i);
            if (t.equals(f.toString()))
            {
                recentItems.remove(i);
                i--;
                length--;
            }
        }

        // add the new file
        recentItems.add(0, f.toString());

        // make sure there are 7 max
        if (recentItems.size() > 7)
            recentItems.remove(7);

        // save this list in the preferences
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        //SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();

        for(i = 0; i < recentItems.size(); i++)
        {
            editor.putString("rf_file" + i, recentItems.get(i));
        }

        editor.putInt("rf_numfiles", recentItems.size());
        editor.commit();
    }

    private CharSequence createTextFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String textFileName = Config.TXT_FILE_PREFIX + timeStamp + "_"; // agarub
        File albumF = getAlbumDir();
        CharSequence textF = albumF + "/"+ textFileName + Config.TXT_FILE_SUFFIX; // agarub
        return textF;
    }

    private CharSequence createDocFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String textFileName = Config.DOCX_FILE_PREFIX + timeStamp + "_"; // agarub
        File albumF = getAlbumDir();
        CharSequence textF = albumF + "/"+ textFileName + Config.DOCX_FILE_SUFFIX; // agarub
        return textF;
    }

    private CharSequence createExcelFile(){
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String textFileName = Config.XLS_FILE_PREFIX + timeStamp + "_"; // agarub
        File albumF = getAlbumDir();
        CharSequence textF = albumF + "/"+ textFileName + Config.XLS_FILE_SUFFIX; // agarub
        return textF;
    }

    private void sendContract() {
        Log.d(TAG, "Sending contract");
        if(null!=mCurrentContractPath) {
            String path = mCurrentContractPath;
            //String path = "/sdcard/Doc/1.docx"; // agarub
            Log.d("TAG", "PATH : " + path);
            String[] splits = path.split("/");
            byte[] msgContents = new MediaChatMessageProcessor().read(path);
            Payload p = new Payload(MessageType.Contract, Config.TRUE, msgContents, new Timestamp(System.currentTimeMillis()), toUserName, signUpUser, splits[splits.length - 1], CallName.Chat);
            sendChatMessage(p, Config.TRUE);

            Log.d(TAG, "Creating intent for chat activity");
            Intent i = new Intent(getApplicationContext(),ChatActivity.class);
            i.putExtra("toUser", toUserName);
            i.putExtra("signUpUser",signUpUser);
            i.putExtra("msg",signUpUser+":Sending contract "+newFilename);
            startActivity(i);
            finish();
        }
    }

    public void openContractFile(CharSequence fname)
    {
        openingFile = false;
        StringBuffer result = new StringBuffer();

        try {
            // open file
            FileReader f = new FileReader(fname.toString());
            File file = new File(fname.toString());

            if (f == null)
            {
                throw(new FileNotFoundException());
            }

            if (file.isDirectory())
            {
                throw(new IOException());
            }
            if (file.length() != 0 && !file.isDirectory())
            {
                char[] buffer;
                buffer = new char[1100];    // made it bigger just in case

                int read = 0;

                do {
                    read = f.read(buffer, 0, 1000);

                    if (read >= 0)
                    {
                        result.append(buffer, 0, read);
                    }
                } while (read >= 0);
            }
        } catch (FileNotFoundException e) {
            // file not found
            errorFname = fname;
            openingError = true;
            showDialog(5);
        } catch (IOException e) {
            // error reading file
            errorFname = fname;
            openingError = true;
            showDialog(6);
        } catch (Exception e) {
            errorFname = fname;
            openingError = true;
            showDialog(5);
        }

        // now figure out the file format, nl, cr, crnl
        if (!openingError)
        {
            openFile(fname, result);
        }

        errorSaving = false;
        if (text != null)
            text.requestFocus();
    } // end openFile(CharSequence fname)

    public void openFile(CharSequence fname, StringBuffer result)
    {
        try {
            // have to do this first because it resets fileformat
            createNewContract(); // to clear everything out

            String newText = result.toString();

            if (newText.indexOf("\r\n", 0) != -1)
            {
                fileformat = FILEFORMAT_CRNL;
                newText = newText.replace("\r", "");
            } else if(newText.indexOf("\r", 0) != -1) {
                fileformat = FILEFORMAT_CR;
                newText = newText.replace("\r", "\n");
            } else {
                fileformat = FILEFORMAT_NL;
            }

            // Okay, now we can set everything up
            text.setText(newText);
            title.setText(fname);

            File f = new File(fname.toString());
            lastModified = f.lastModified();
            filename = fname;
            untitled = false;

            addRecentFile(fname);
            openingRecent = false;

            // this is just incase we get an error
        } catch (Exception e) {
            errorFname = fname;
            openingError = true;
            showDialog(5);
        }

        openingIntent = false;
        temp_filename = "";
    } // end openContractFile


}
