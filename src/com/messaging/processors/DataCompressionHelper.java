package com.messaging.processors;

/**
 * Created by Ruby on 8/5/2014.
 */
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import android.util.Log;
import com.messaging.data.Config;

public class DataCompressionHelper {
    private static final String TAG = "DataCompressionHelper";

    public byte[] decompressData(byte[] comBs) {
        Log.d(TAG, "Before decompression " + comBs.length);
        byte[] decomBs = null;
        try {
            Inflater inflater = new Inflater();
            inflater.setInput(comBs);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(
                    comBs.length);
            byte[] buffer = new byte[Config.BUFFER_SIZE];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();
            decomBs = outputStream.toByteArray();
        } catch (IOException ioe) {
            Log.e(TAG, "IOE");
            ioe.printStackTrace();
        } catch (DataFormatException dfe) {
            Log.e(TAG, "DFE");
            dfe.printStackTrace();
        }
        Log.d(TAG, "After decompression " + decomBs.length);
        return decomBs;
    }

    public byte[] compressData(byte[] decomBs) {
        Log.d(TAG, "Before compression " + decomBs.length);
        byte[] comBs = null;

        try {
            Deflater deflater = new Deflater();
            deflater.setInput(decomBs);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(
                    decomBs.length);
            deflater.finish();
            byte[] buffer = new byte[Config.BUFFER_SIZE];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();
            comBs = outputStream.toByteArray();
        } catch (IOException ioe) {
            Log.e(TAG, "IOE");
            ioe.printStackTrace();
        }
        Log.d(TAG, "After compression " + comBs.length);
        return comBs;
    }

}
