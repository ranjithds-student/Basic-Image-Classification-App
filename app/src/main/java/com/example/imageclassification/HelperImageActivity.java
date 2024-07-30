package com.example.imageclassification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.IOException;
import java.util.List;

public class HelperImageActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE_CODE = 0;
    private static final int READ_STORAGE_REQUEST_CODE = 1;
    private ImageView displayIV;
    private TextView imageClassificationTV;
    private AppCompatButton openGalleryBTN;
    private ImageLabeler imageLabeler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_image);
        displayIV = findViewById(R.id.displayIV);
        imageClassificationTV = findViewById(R.id.imageClassificationTV);
        openGalleryBTN = findViewById(R.id.openGalleryBTN);

        imageLabeler = ImageLabeling.getClient(new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.7f)
                .build());

        openGalleryBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkGalleryPermission();
            }
        });
    }

    private void checkGalleryPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, READ_STORAGE_REQUEST_CODE);
            } else {
                if (ContextCompat.checkSelfPermission(HelperImageActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    onPickImage();
                }
            }
        }


    }

    public void onPickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT); //gallery
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE_CODE);
    }

    private void runClassification(Bitmap bitmap) {
        InputImage inputImage = InputImage.fromBitmap(bitmap, 0); //rotation only come when
        // camera tilts by user while clicking photo
        imageLabeler.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
            @Override
            public void onSuccess(List<ImageLabel> imageLabels) {

                if (imageLabels.size() > 0) {
                    StringBuilder builder = new StringBuilder();  //SB used to combine string

                    for (ImageLabel label : imageLabels) {
                        builder.append(label.getText())
                                .append(" : ")
                                .append(label.getConfidence())
                                .append("\n");
                    }
                    imageClassificationTV.setText(builder.toString());

                } else {
                    imageClassificationTV.setText("Could not classified");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });
    }

    //callBack comes here after the result with uri of image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_PICK_IMAGE_CODE) {
            //Process the image
            Uri uri = data.getData();

            Bitmap bitmap = loadFromURI(uri);
            displayIV.setImageBitmap(bitmap);

            runClassification(bitmap);
        }
    }

    //uri to bitmap
    private Bitmap loadFromURI(Uri uri) {
        Bitmap bitmap = null;

        try {
            //modern way of converting bitmap
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(source);
            } else {
//              old way of converting bitmap

                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);// previously
//In previous API ,we are asking Media Store tabl
// e to give us   images which at particular row at MEDIA TABLE

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                requestCode == READ_STORAGE_REQUEST_CODE) {
            onPickImage();
        }
    }
}