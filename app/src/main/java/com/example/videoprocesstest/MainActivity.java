package com.example.videoprocesstest;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import pyxis.uzuki.live.mediaresizer.MediaResizer;
import pyxis.uzuki.live.mediaresizer.MediaResizerGlobal;
import pyxis.uzuki.live.mediaresizer.data.ResizeOption;
import pyxis.uzuki.live.mediaresizer.data.VideoResizeOption;
import pyxis.uzuki.live.mediaresizer.model.MediaType;
import pyxis.uzuki.live.mediaresizer.model.ScanRequest;
import pyxis.uzuki.live.mediaresizer.model.VideoResolutionType;

public class MainActivity extends AppCompatActivity {

    private Button btn;
    private VideoView videoView;
    private static final String VIDEO_DIRECTORY = "/demonuts";
    private int GALLERY = 1;
    private String [] permissions = new String []{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
    };
    private MediaController mediaController;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btn = findViewById(R.id.btn);
        videoView =  findViewById(R.id.vv);
        mediaController = new MediaController(this, false){
            @Override
            public void hide(){

            }
        };
        mediaController.show();
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseVideoFromGallary();
            }
        });
    }

//

    public void chooseVideoFromGallary() {

        checkPermissions();
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(galleryIntent, GALLERY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("result",""+resultCode);
        Log.d("result ",""+ data.getDataString() + "tío chaval");
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == this.RESULT_CANCELED) {
            Log.d("what","cancle");
            return;
        }
        if (requestCode == GALLERY) {
            Log.d("what","gale");
            if (data != null) {
                Uri contentURI = data.getData();
                Log.d("contentURI ",""+contentURI);
                String selectedVideoPath = getPath(contentURI);
                Log.d("selectedVideoPath ",""+selectedVideoPath);
                String outputPath =  getOutputFilePath(selectedVideoPath);
                Log.d("outputPath ",""+outputPath);

                // use this statement in 1.0.0
                MediaResizerGlobal.INSTANCE.initializeApplication(this);
                VideoResizeOption resizeOption = new VideoResizeOption.Builder()
                        .setVideoResolutionType(VideoResolutionType.AS720)
                        .setVideoBitrate(1000 * 1000)
                        .setAudioBitrate(128 * 1000)
                        .setAudioChannel(1)
                        .setScanRequest(ScanRequest.TRUE)
                        .build();

                ResizeOption option  = new ResizeOption.Builder()
                        .setMediaType(MediaType.VIDEO)
                        .setVideoResizeOption(resizeOption)
                        .setTargetPath(selectedVideoPath)
                        .setOutputPath(outputPath)
                        .setCallback((code, output) -> {
                            Log.d("code ",""+code);
                            Log.d("output ",""+output);
                        }).build();

                MediaResizer.process(option);

                Log.d("path",selectedVideoPath);
                saveVideoToInternalStorage(selectedVideoPath);
                videoView.setVideoURI(contentURI);
                videoView.requestFocus();
                videoView.start();

            }

        }
    }

    private void callback(int Code, String output){
        Log.d("Code: ",""+Code);
        Log.d("output: ",""+output);
    }

    private  String getOutputFilePath(String inputFile){
        String splitInputPath [] = inputFile.split("/");
        String OutputPath = "";

        for(int i=0; i < splitInputPath.length-1; i++){
            OutputPath +=   splitInputPath[i]+ "/";
        }
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddhhmmss");
        String dateTime = dateFormat.format(date);
        OutputPath += "vp_" +dateTime + ".mp4";
        return OutputPath;
    }

//    taked from https://stackoverflow.com/questions/33162152/storage-permission-error-in-marshmallow
    private boolean checkPermissions(){
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions){
            result = ContextCompat.checkSelfPermission(this,p);
            if(result != PackageManager.PERMISSION_GRANTED){
                listPermissionsNeeded.add(p);
            }
        }
        if(!listPermissionsNeeded.isEmpty()){
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),100);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // do something
            }
            return;
        }
    }

    private void saveVideoToInternalStorage (String filePath) {

        File newfile;

        try {

            File currentFile = new File(filePath);
            File wallpaperDirectory = new File(Environment.getExternalStorageDirectory() + VIDEO_DIRECTORY);
            newfile = new File(wallpaperDirectory, Calendar.getInstance().getTimeInMillis() + ".mp4");

            if (!wallpaperDirectory.exists()) {
                wallpaperDirectory.mkdirs();
            }

            if(currentFile.exists()){

                InputStream in = new FileInputStream(currentFile);
                OutputStream out = new FileOutputStream(newfile);

                // Copy the bits from instream to outstream
                byte[] buf = new byte[1024];
                int len;

                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                Log.v("vii", "Video file saved successfully.");
            }else{
                Log.v("vii", "Video saving failed. Source file missing.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Video.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            // HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
            // THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else
            return null;
    }

}
