package it.scarpentim.volleycourtmapping;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.File;

import it.scarpentim.volleycourtmapping.classification.ClassifierFactory;
import it.scarpentim.volleycourtmapping.image.ImageSupport;

public class MainActivity extends VolleyAbstractActivity {

    private Mat sampledImage = null;
    private ImageSupport imageSupport = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        selectedImagePath = sharedPref.getString(LAST_IMAGE, null);

        classifier = ClassifierFactory.getYolov3Instance(this);
        imageSupport = new ImageSupport(this, classifier.getLabels());
    }

    @Override
    public void onResume(){
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0,this, mLoaderCallback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.appmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.debug_mode:
                Intent myIntent = new Intent(this, DebugActivity.class);
                startActivity(myIntent);
            case R.id.action_openGallery:
                PermissionSupport.validateReadStoragePermission(this);
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(Intent.createChooser(intent,"Select image"), SELECT_PICTURE);
                return true;

        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                selectedImagePath = imageSupport.getPath(selectedImageUri);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(LAST_IMAGE, selectedImagePath);
                editor.commit();
                loadImageFromPath();
                Mat mat = imageSupport.drawSideSelector();

                Bitmap bitmap;
                bitmap = imageSupport.matToBitmap(mat);
                ImageView iv = findViewById(R.id.ivSelector);
                iv.setImageBitmap(bitmap);

            }
        }
    }

    private void loadImageFromPath() {
        if (selectedImagePath != null) {
            Log.i(TAG, "selectedImagePath: " + selectedImagePath);
            File file = new File(selectedImagePath);
            if(file.exists()) {
                sampledImage = imageSupport.loadImage(selectedImagePath);
                Bitmap bitmap = imageSupport.matToBitmap(sampledImage);
                ImageView iv = findViewById(R.id.ivPreview);
                iv.setImageBitmap(bitmap);
            }
        }
    }


    @Override
    public void showImage() {
        Bitmap bitmap = imageSupport.matToBitmap(sampledImage);
        ImageView iv = findViewById(R.id.ivPreview);
        iv.setImageBitmap(bitmap);
    }

    @Override
    public void showImage(Mat image) {

    }

    protected BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    loadImageFromPath();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };
}
