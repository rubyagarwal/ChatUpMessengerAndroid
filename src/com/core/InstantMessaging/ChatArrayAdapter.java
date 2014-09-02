package com.core.InstantMessaging;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.messaging.data.ChatMessage;
import com.messaging.data.Config;
import com.messaging.data.MessageType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ChatArrayAdapter extends ArrayAdapter<ChatMessage> {

    private TextView chatText;
    private ImageView mImageView;
    private VideoView mVideoView;
    private List<ChatMessage> chatMessageList = new ArrayList<ChatMessage>();
    private LinearLayout singleMessageContainer;

    private static final String TAG = "ChatArrayAdapter";

    @Override
    public void add(ChatMessage object) {
        chatMessageList.add(object);
        super.add(object);
    }

    public ChatArrayAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public int getCount() {
        return this.chatMessageList.size();
    }

    public ChatMessage getItem(int index) {
        return this.chatMessageList.get(index);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.activity_chat_singlemessage, parent, false);
        }
        singleMessageContainer = (LinearLayout) row.findViewById(R.id.singleMessageContainer);
        ChatMessage chatMessageObj = getItem(position);
        chatText = (TextView) row.findViewById(R.id.singleMessage);
        chatText.setVisibility(View.VISIBLE);
        chatText.setText(chatMessageObj.message);
        mImageView = (ImageView) row.findViewById(R.id.imageView1);
        mImageView.setVisibility(View.GONE);

        mVideoView = (VideoView) row.findViewById(R.id.videoView1);
        mVideoView.setVisibility(View.GONE);

        if(!MessageType.Text.equals(chatMessageObj.msgType)) {
            if(MessageType.Image.equals(chatMessageObj.msgType) && chatMessageObj.bitMap != null) {
                chatText.setVisibility(View.GONE);
                setPic(chatMessageObj.bitMap);
            } else if(MessageType.Video.equals(chatMessageObj.msgType)) {
                chatText.setVisibility(View.GONE);
                mVideoView.setVideoURI(chatMessageObj.videoUri);
                mVideoView.start();
            } else if(MessageType.Contract.equals(chatMessageObj.msgType)) {
                Log.d(TAG,"Text file has come it seems");

            }
        }
        chatText.setBackgroundResource(chatMessageObj.left ? R.drawable.bubble_a : R.drawable.bubble_b);
        singleMessageContainer.setGravity(chatMessageObj.left ? Gravity.LEFT : Gravity.RIGHT);
        return row;
    }

    public Bitmap decodeToBitmap(byte[] decodedByte) {
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }

    private void setPic(Bitmap bitmap) {
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        }

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        mImageView.setImageBitmap(bitmap);
        //mVideoUri = null;
        mImageView.setVisibility(View.VISIBLE);
        //mVideoView.setVisibility(View.INVISIBLE);
    }
}