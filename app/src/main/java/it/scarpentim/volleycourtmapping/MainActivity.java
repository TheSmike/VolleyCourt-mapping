package it.scarpentim.volleycourtmapping;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "volleyCourt";

    private static final int SELECT_PICTURE = 1;
    public static final String LAST_IMAGE = "LAST_IMAGE";

    private String selectedImagePath;

    private ImageSupport imageSupport = null;
    VolleySeekBarHandler seekBarHandler;

    private Mat sampledImage = null;
    private Mat drawedImage;
    private SharedPreferences sharedPref;

    private enum ShowType{
        IMAGE,
        CANNY,
        HOUGH
    }

    private ShowType show = ShowType.IMAGE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageSupport = new ImageSupport(this);
        seekBarHandler = new VolleySeekBarHandler(this);
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        selectedImagePath = sharedPref.getString(LAST_IMAGE, null);
    }

    @Override
    public void onResume(){
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0,this, mLoaderCallback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mymenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.action_openGallery:
                show = ShowType.IMAGE;
                PermissionSupport.validateReadStoragePermission(this);
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(Intent.createChooser(intent,"Select image"), SELECT_PICTURE);
                return true;

            case R.id.action_Canny:
                if (checkImageLoaded()){
                    show = ShowType.CANNY;
                    showImage();
                    return true;
                }

            case R.id.action_Hough:
                if (checkImageLoaded()){
                    show = ShowType.HOUGH;
                    showImage();
                    return true;
                }

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
            }
        }
    }

    private void loadImageFromPath() {
        if (selectedImagePath != null) {
            Log.i(TAG, "selectedImagePath: " + selectedImagePath);
            sampledImage = imageSupport.loadImage(selectedImagePath);
            Bitmap bitmap = imageSupport.matToBitmap(sampledImage);
            ImageView iv = findViewById(R.id.ivPreview);
            iv.setImageBitmap(bitmap);
        }
    }

    public void showImage() {
        if (!checkImageLoaded()) return;
        Bitmap bitmap;

        switch (show) {
            case IMAGE:
                bitmap = imageSupport.matToBitmap(sampledImage);
                break;
            case CANNY:
                Mat cannyMat = imageSupport.cannyFilter(sampledImage, seekBarHandler);
                bitmap = imageSupport.matToBitmap(cannyMat);
                break;
            case HOUGH:
                Mat houghMat = imageSupport.houghTransform(sampledImage, seekBarHandler);
                drawedImage = imageSupport.drawHoughLines(sampledImage, houghMat);
                bitmap = imageSupport.matToBitmap(drawedImage);
                break;
            default:
                return;
        }

        ImageView iv = findViewById(R.id.ivPreview);
        iv.setImageBitmap(bitmap);
    }


    private boolean checkImageLoaded() {
        if (sampledImage == null){
            Toast.makeText(this, "Nessuna immagine caricata", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
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

    public void transformImage(View view) {
        if (drawedImage == null){
            Toast.makeText(this, "Nessuna immagine caricata", Toast.LENGTH_SHORT).show();
            return;
        }

    }
}
