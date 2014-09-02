//package com.messaging.processors;
//
//import android.content.SharedPreferences;
//import android.graphics.Typeface;
//import android.preference.PreferenceManager;
//import android.text.Editable;
//import android.text.TextWatcher;
//import android.text.util.Linkify;
//import android.util.Log;
//import android.view.View;
//import android.widget.*;
//import com.core.InstantMessaging.R;
//import com.messaging.data.CallName;
//import com.messaging.data.Config;
//import com.messaging.data.MessageType;
//import com.messaging.data.Payload;
//
//import java.io.*;
//import java.sql.Timestamp;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//
///**
// * Created by Ruby on 8/11/2014.
// */
//public class ContractProcessor {
//
//    public void createNewContract() {
//        // set the new context
//        setContentView(R.layout.edit);    // update options done below
//
//        text = (EditText) findViewById(R.id.note);
//        title = (TextView) findViewById(R.id.notetitle);
//
//        text.setText("");
//        title.setText(R.string.newFileName);
//
//        try {
//            if(newFilename == null)
//                newFilename =  createTextFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//        Button saveBtn = (Button) findViewById(R.id.buttonSave);
//        Button.OnClickListener mCreateNewOnClickListener =
//                new Button.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        save(newFilename);
//                    }
//                };
//        saveBtn.setOnClickListener(mCreateNewOnClickListener);
//
//        Button contractBtn = (Button) findViewById(R.id.buttonSendContract);
//        Button.OnClickListener mContractOnClickListener =
//                new Button.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        try {
//                            sendContract();
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                };
//        contractBtn.setOnClickListener(mContractOnClickListener);
//
//        // clear the saved text
//        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
//        if (editor != null) {
//            editor.putInt("mode", 1);
//
//            editor.putString("text", "");
//            editor.putInt("text-quotes", 0);
//
//            editor.putString("fntext", title.getText().toString());
//            editor.putInt("fntext-quotes", countQuotes(title.getText().toString()));
//
//            editor.putString("filename", "");
//            editor.putInt("filename-quotes", 0);
//
//            editor.putInt("selection-start", -1);
//            editor.putInt("selection-end", -1);
//            editor.commit();
//        }
//
//        fileformat = FILEFORMAT_NL;
//        filename = "";
//        lastModified = 0;
//        untitled = true;
//
//        creatingFile = false;
//
//        updateOptions();
//        text.requestFocus();
//
//    } // end createNewContract();
//
//    public static int countQuotes(String t) // count " in string
//    {
//        int i = -1;
//        int count = -1;
//
//        do {
//            i = t.indexOf('"', i + 1);
//            count++;
//        } while (i != -1);
//
//        return count;
//    } // end countQuotes()
//
//    /**
//     * *************************************************************
//     * updateOptions()
//     * start options app
//     */
//    protected void updateOptions() {
//        boolean value;
//
//        // load the preferences
//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
//        //autoComplete = sharedPref.getBoolean("autocomplete", false);
//
//        /********************************
//         * Auto correct and auto case */
////        boolean autocorrect = sharedPref.getBoolean("autocorrect", false);
////        boolean autocase = sharedPref.getBoolean("autocase", false);
////
////        if (autocorrect && autocase)
////        {
////            setContentView(R.layout.edit_autotext_autocase);
////        } else if (autocorrect) {
////            setContentView(R.layout.edit_autotext);
////        } else if (autocase) {
////            setContentView(R.layout.edit_autocase);
////        } else {
////            setContentView(R.layout.edit);
////        }
//
//        text = (EditText) findViewById(R.id.note);
//        title = (TextView) findViewById(R.id.notetitle);
//
//        text.addTextChangedListener(new TextWatcher() {
//
//            public void onTextChanged(CharSequence one, int a, int b, int c) {
//
//                // put a little star in the title if the file is changed
//                if (!isTextChanged()) {
//                    CharSequence temp = title.getText();
//                    title.setText("* " + temp);
//                }
//            }
//
//            // complete the interface
//            public void afterTextChanged(Editable s) {
//            }
//
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//            }
//        });
//
//        /********************************
//         * links clickable */
//        boolean linksclickable = sharedPref.getBoolean("linksclickable", false);
//
//        if (linksclickable)
//            text.setAutoLinkMask(Linkify.ALL);
//        else
//            text.setAutoLinkMask(0);
//
//        /********************************
//         * show/hide filename */
//        value = sharedPref.getBoolean("hidefilename", false);
//        if (value)
//            title.setVisibility(View.GONE);
//        else
//            title.setVisibility(View.VISIBLE);
//
//        /********************************
//         * line wrap */
//        value = sharedPref.getBoolean("linewrap", true);
//        text.setHorizontallyScrolling(!value);
//
//        // setup the scroll view correctly
//        ScrollView scroll = (ScrollView) findViewById(R.id.scroll);
//        if (scroll != null) {
//            scroll.setFillViewport(true);
//            scroll.setHorizontalScrollBarEnabled(!value);
//        }
//
//        /********************************
//         * font face */
//        String font = sharedPref.getString("font", "Monospace");
//
//        if (font.equals("Serif"))
//            text.setTypeface(Typeface.SERIF);
//        else if (font.equals("Sans Serif"))
//            text.setTypeface(Typeface.SANS_SERIF);
//        else
//            text.setTypeface(Typeface.MONOSPACE);
//
//        /********************************
//         * font size */
//        String fontsize = sharedPref.getString("fontsize", "Medium");
//
//        if (fontsize.equals("Extra Small"))
//            text.setTextSize(12.0f);
//        else if (fontsize.equals("Small"))
//            text.setTextSize(16.0f);
//        else if (fontsize.equals("Medium"))
//            text.setTextSize(20.0f);
//        else if (fontsize.equals("Large"))
//            text.setTextSize(24.0f);
//        else if (fontsize.equals("Huge"))
//            text.setTextSize(28.0f);
//        else
//            text.setTextSize(20.0f);
//
//        /********************************
//         * Colors */
//        int bgcolor = sharedPref.getInt("bgcolor", 0xFF000000);
//        text.setBackgroundColor(bgcolor);
//
//        int fontcolor = sharedPref.getInt("fontcolor", 0xFFCCCCCC);
//        text.setTextColor(fontcolor);
//
//        title.setTextColor(bgcolor);
//        title.setBackgroundColor(fontcolor);
//
//        text.setLinksClickable(true);
//    } // updateOptions()
//
//    public static boolean isTextChanged()	// checks if the text has been changed
//    {
//        CharSequence temp = title.getText();
//
//        try {	// was getting error on the developer site, so added this to "catch" it
//
//            if (temp.charAt(0) == '*')
//            {
//                return true;
//            }
//        } catch (Exception e) {
//            return false;
//        }
//
//        return false;
//    } // end isTextChanged()
//
//    public void save(CharSequence fname)
//    {
//        errorSaving = false;
//
//        // actually save the file here
//        try {
//            File f = new File(fname.toString());
//
//            if ( (f.exists() && !f.canWrite()) || (!f.exists() && !f.getParentFile().canWrite()))
//            {
//                creatingFile = false;
//                openingFile = false;
//                errorSaving = true;
//
//                if (fname.toString().indexOf("/sdcard/") == 0)
//                    showDialog(7);
//                else
//                    showDialog(6);
//
//                text.requestFocus();
//
//                f = null;
//                return;
//            }
//            f = null; // hopefully this gets garbage collected
//
//            // Create file
//            FileWriter fstream = new FileWriter(fname.toString());
//            BufferedWriter out = new BufferedWriter(fstream);
//
//            if (fileformat == FILEFORMAT_CR)
//            {
//                out.write(text.getText().toString().replace("\n", "\r"));
//            } else if (fileformat == FILEFORMAT_CRNL) {
//                out.write(text.getText().toString().replace("\n", "\r\n"));
//            } else {
//                out.write(text.getText().toString());
//            }
//
//            out.close();
//
//            // give a nice little message
//            Toast.makeText(this, R.string.onSaveMessage, Toast.LENGTH_SHORT).show();
//
//            // the filename is the new title
//            title.setText(fname);
//            filename = fname;
//            mCurrentContractPath=fname.toString();
//            untitled = false;
//
//            lastModified = (new File(filename.toString())).lastModified();
//
//            temp_filename = "";
//
//            addRecentFile(fname);
//        } catch (Exception e) { //Catch exception if any
//            creatingFile = false;
//            openingFile = false;
//
//            if (fname.toString().indexOf("/sdcard/") == 0)
//                showDialog(5);
//            else
//                showDialog(5);
//
//            errorSaving = true;
//        }
//
//        text.requestFocus();
//    } // end saveNote()
//
//    protected void readRecentFiles()
//    {
//        int i;
//
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//
//        int numFiles = prefs.getInt("rf_numfiles", 0);
//
//        // clear the current list
//        if (recentItems == null)
//            recentItems = new ArrayList<String>();
//
//        recentItems.clear();
//
//        // start adding stuff
//        for(i = 0; i < numFiles; i++)
//        {
//            recentItems.add(prefs.getString("rf_file" + i, i + ""));
//        }
//    } // end readRecentFiles()
//
//    protected void addRecentFile(CharSequence f)
//    {
//        if (recentItems == null)
//            readRecentFiles();
//
//        // remove from list if it is already there
//        int i;
//        int length = recentItems.size();
//
//        for(i = 0; i < length; i++)
//        {
//            String t = recentItems.get(i);
//            if (t.equals(f.toString()))
//            {
//                recentItems.remove(i);
//                i--;
//                length--;
//            }
//        }
//
//        // add the new file
//        recentItems.add(0, f.toString());
//
//        // make sure there are 7 max
//        if (recentItems.size() > 7)
//            recentItems.remove(7);
//
//        // save this list in the preferences
//        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
//        //SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
//
//        for(i = 0; i < recentItems.size(); i++)
//        {
//            editor.putString("rf_file" + i, recentItems.get(i));
//        }
//
//        editor.putInt("rf_numfiles", recentItems.size());
//        editor.commit();
//    }
//
//    private CharSequence createTextFile() throws IOException {
//        // Create an image file name
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        String textFileName = Config.TXT_FILE_PREFIX + timeStamp + "_";
//        File albumF = getAlbumDir();
//        CharSequence textF = albumF + "/"+ textFileName + Config.TXT_FILE_SUFFIX;
//        return textF;
//    }
//
//    private void sendContract() {
//        String path = mCurrentContractPath;
//        Log.d("TAG", "PATH : " + path);
//        String[] splits = path.split("/");
//        byte[] msgContents = new MediaChatMessageProcessor().read(path);
//        Payload p = new Payload(MessageType.Contract, Config.TRUE, msgContents, new Timestamp(System.currentTimeMillis()), toUserName, signUpUser, splits[splits.length - 1], CallName.Chat);
//        sendChatMessage(p, Config.TRUE);
//    }
//
//    public void openFile(CharSequence fname)
//    {
//        openingFile = false;
//        StringBuffer result = new StringBuffer();
//
//        try {
//            // open file
//            FileReader f = new FileReader(fname.toString());
//            File file = new File(fname.toString());
//
//            if (f == null)
//            {
//                throw(new FileNotFoundException());
//            }
//
//            if (file.isDirectory())
//            {
//                throw(new IOException());
//            }
//
//            // if the file has nothing in it there will be an exception here
//            // that actually isn't a problem
//            if (file.length() != 0 && !file.isDirectory())
//            {
//                // using just FileReader now. Works better with weird file encoding
//                // Thanks to Ondrej Bojar <obo@cuni.cz> for finding the bug.
//
//                // read in the file
//                //        do it this way because we need that newline at
//                //        the end of the file if there is one
//                char[] buffer;
//                buffer = new char[1100];    // made it bigger just in case
//
//                int read = 0;
//
//                do {
//                    read = f.read(buffer, 0, 1000);
//
//                    if (read >= 0)
//                    {
//                        result.append(buffer, 0, read);
//                    }
//                } while (read >= 0);
//            }
//        } catch (FileNotFoundException e) {
//            // file not found
//            errorFname = fname;
//            openingError = true;
//            showDialog(5);
//        } catch (IOException e) {
//            // error reading file
//            errorFname = fname;
//            openingError = true;
//            showDialog(6);
//        } catch (Exception e) {
//            errorFname = fname;
//            openingError = true;
//            showDialog(5);
//        }
//
//        // now figure out the file format, nl, cr, crnl
//        if (!openingError)
//        {
//            openFile(fname, result);
//        }
//
//        errorSaving = false;
//        if (text != null)
//            text.requestFocus();
//    } // end openFile(CharSequence fname)
//
//    public void openFile(CharSequence fname, StringBuffer result)
//    {
//        try {
//            // have to do this first because it resets fileformat
//            createNewContract(); // to clear everything out
//
//            String newText = result.toString();
//
//            if (newText.indexOf("\r\n", 0) != -1)
//            {
//                fileformat = FILEFORMAT_CRNL;
//                newText = newText.replace("\r", "");
//            } else if(newText.indexOf("\r", 0) != -1) {
//                fileformat = FILEFORMAT_CR;
//                newText = newText.replace("\r", "\n");
//            } else {
//                fileformat = FILEFORMAT_NL;
//            }
//
//            // Okay, now we can set everything up
//            text.setText(newText);
//            title.setText(fname);
//
//            File f = new File(fname.toString());
//            lastModified = f.lastModified();
//            filename = fname;
//            untitled = false;
//
//            addRecentFile(fname);
//            openingRecent = false;
//
//            // this is just incase we get an error
//        } catch (Exception e) {
//            errorFname = fname;
//            openingError = true;
//            showDialog(5);
//        }
//
//        openingIntent = false;
//        temp_filename = "";
//    } // end openFile(CharSequence fname, StringBuffer result)
//
//
//}
