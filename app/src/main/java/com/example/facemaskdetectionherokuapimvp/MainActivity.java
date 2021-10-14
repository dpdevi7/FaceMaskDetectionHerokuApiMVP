package com.example.facemaskdetectionherokuapimvp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.BitmapCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.facemaskdetectionherokuapimvp.MVP.Contract;
import com.example.facemaskdetectionherokuapimvp.MVP.DetectionPresenter;
import com.example.facemaskdetectionherokuapimvp.Retrofit.APIClient;
import com.example.facemaskdetectionherokuapimvp.Retrofit.APIInterface;
import com.example.facemaskdetectionherokuapimvp.Utils.Constants;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements Contract.View {

    private DetectionPresenter presenter;
    private ImageView imageView;
    private MaterialButton materialButtonCaptureImage;
    private MaterialButton materialButtoneDetectMask;
    private TextView textView;
    private File photoFile;
    private Bitmap mBitmap;
    private String currentPhotoPath;
    private static final String TAG = MainActivity.class.getSimpleName();


    private APIInterface apiInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 0. ask for read and write storage permission
        askForPermissions();

        setContentView(R.layout.activity_main);

        // 1. init Preseter class
        presenter = new DetectionPresenter(this);

        // 2. retrofit client init
        apiInterface = APIClient.getClient().create(APIInterface.class);

        // 2. import views
        initView();

        // 3. capture image and show in imageview
        materialButtonCaptureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                dispatchTakePictureIntent();

            }
        });

        materialButtoneDetectMask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                presenter.callFaceDetectionService(apiInterface, photoFile, mBitmap);

            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go

            try {
                // 1. give file to API
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(MainActivity.this, "Camera Error!!!", Toast.LENGTH_SHORT).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);


                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, Constants.REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = imageFile.getAbsolutePath();
        return imageFile;
    }

    private void initView(){
        imageView = findViewById(R.id.iv);
        materialButtonCaptureImage = findViewById(R.id.mb_capture);
        materialButtoneDetectMask = findViewById(R.id.mb_predict);
        textView = findViewById(R.id.tv);
    }

    private void askForPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
    }

    @Override
    public void showProgress() {

        materialButtoneDetectMask.setEnabled(false);

    }

    @Override
    public void hideProgress(boolean success) {

        materialButtoneDetectMask.setEnabled(true);

    }

    @Override
    public void setDetectionBitmap(Bitmap bitmap) {

        materialButtoneDetectMask.setEnabled(true);
        imageView.setImageBitmap(bitmap);

    }

    private String getImageOrientationFromCamera(File file){
        String orientation;
        try {
            ExifInterface exif = new ExifInterface(Uri.fromFile(file).getEncodedPath());
            orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);

            return orientation;


        } catch (IOException e) {
            e.printStackTrace();
            orientation = "";

            return orientation;
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==Constants.REQUEST_IMAGE_CAPTURE && resultCode==RESULT_OK){

            try {
                // 1. get absolute path of file
                String photoPath = photoFile.getAbsolutePath();
                Log.d(TAG, "onActivityResult: before compression file size: "+photoFile.length()/1024);

                // 2. get file orientation
                // front facing camera = 8
                // back facing camera = 6
                String orientation = getImageOrientationFromCamera(photoFile);
                Log.d(TAG, "onActivityResult: before compression orientation: "+orientation);


                // 3. create Bitmap from file
                if (orientation.equals("6")){

                    mBitmap = BitmapFactory.decodeFile(photoPath);
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90.0f);
                    mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
                    Log.d(TAG, "onActivityResult: before compression bitmap size: "+ BitmapCompat.getAllocationByteCount(mBitmap));

                    // 4. compress bitmap
                    mBitmap.compress(Bitmap.CompressFormat.JPEG, 30, new FileOutputStream(photoFile));


                }else {

                    mBitmap = BitmapFactory.decodeFile(photoPath);
                    Matrix matrix = new Matrix();
                    matrix.postRotate(270.0f);
                    mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
                    Log.d(TAG, "onActivityResult: before compression bitmap size: "+ BitmapCompat.getAllocationByteCount(mBitmap));

                    // 4. compress bitmap
                    mBitmap.compress(Bitmap.CompressFormat.JPEG, 50, new FileOutputStream(photoFile));
                }



                Log.d(TAG, "onActivityResult: after compression file size: "+photoFile.length()/1024);

                // 5. set compressed bitmap to imageview
                Log.d(TAG, "onActivityResult: after compression bitmap size: "+ BitmapCompat.getAllocationByteCount(mBitmap));
                imageView.setImageBitmap(mBitmap);
                Log.d(TAG, "onActivityResult: after comprssion orientation: "+getImageOrientationFromCamera(photoFile));




            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }



        }
    }

    }