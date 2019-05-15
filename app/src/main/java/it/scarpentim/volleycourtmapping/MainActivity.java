package it.scarpentim.volleycourtmapping;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.List;

import it.scarpentim.volleycourtmapping.classification.Classifier;
import it.scarpentim.volleycourtmapping.classification.ClassifierFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "volleyCourt";

    private static final int SELECT_PICTURE = 1;
    public static final String LAST_IMAGE = "LAST_IMAGE";

    private String selectedImagePath;

    private ImageSupport imageSupport = null;
    VolleySeekBarHandler seekBarHandler;
    OnVolleyTouchHandler onTouchListener;

    private Mat sampledImage = null;
    private Mat drawedImage;
    private SharedPreferences sharedPref;
    private boolean isPerspectiveApplied = false;

    Classifier classifier;


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
        seekBarHandler = new VolleySeekBarHandler(this);
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        selectedImagePath = sharedPref.getString(LAST_IMAGE, null);
        onTouchListener = new OnVolleyTouchHandler(this);
        findViewById(R.id.ivPreview).setOnTouchListener(onTouchListener);
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
            isPerspectiveApplied = false;
            onTouchListener.reset();
            onTouchListener.setImage(sampledImage);
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


    public void showImage(Mat image) {
        if (!checkImageLoaded()) return;
        Bitmap bitmap;
        bitmap = imageSupport.matToBitmap(image);
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
        if (isPerspectiveApplied) {
            isPerspectiveApplied = false;
            onTouchListener.reset();
            onTouchListener.setImage(sampledImage);
            showImage(sampledImage);
        } else {
            if (sampledImage == null) {
                Toast.makeText(this, "Nessuna immagine caricata", Toast.LENGTH_SHORT).show();
                return;
            }
            if (onTouchListener.getCorners().size() != 4) {
                Context context = getApplicationContext();
                CharSequence text = "Bisogna selezionare 4 vertici!";
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            } else {
                isPerspectiveApplied = true;
                Mat projectiveMat = imageSupport.courtProjectiveMat(onTouchListener.getCorners());
                Mat correctedImage = new Mat(800, 800, 1);

                Imgproc.warpPerspective(sampledImage, correctedImage, projectiveMat, correctedImage.size());
                showImage(correctedImage);
            }
        }
    }

    public void classifyImage(View view){
        if (sampledImage == null) {
            Toast.makeText(this, "Nessuna immagine caricata", Toast.LENGTH_SHORT).show();
            return;
        }
        new ComputeTask().execute(sampledImage);
    }

    private class ComputeTask extends AsyncTask<Mat, String, List<Classifier.Recognition>>{

        Mat tmpMat;
        @Override
        protected List<Classifier.Recognition> doInBackground(Mat... mats) {
            Mat mat = imageSupport.resizeForYolo(mats[0]);
            List<Classifier.Recognition> recognitions = classifier.recognizeImage(imageSupport.matToBitmap(mat));
            tmpMat = imageSupport.drawBoxes(mats[0], recognitions, 0.2);
            return recognitions;
        }

        @Override
        protected void onPostExecute(List<Classifier.Recognition> mat) {
            super.onPostExecute(mat);
            showImage(tmpMat);
        }
    }
}
