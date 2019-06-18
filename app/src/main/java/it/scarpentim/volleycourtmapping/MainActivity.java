package it.scarpentim.volleycourtmapping;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.List;

import it.scarpentim.volleycourtmapping.classification.Classifier;
import it.scarpentim.volleycourtmapping.classification.ClassifierFactory;
import it.scarpentim.volleycourtmapping.exception.AppException;
import it.scarpentim.volleycourtmapping.image.ImageSupport;

public class MainActivity extends VolleyAbstractActivity {

    private Mat sampledImage = null;
    private ImageSupport imageSupport = null;
    private OnSideSelectorHandler sideSelectorHandler;

    private ImageView ivSelector;
    private ImageView ivPreview;

    private TextView tvMsg;

    private State state = State.START;
    private VolleyParams volleyParams;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ivSelector = findViewById(R.id.ivSelector);
        ivPreview = findViewById(R.id.ivPreview);
        tvMsg = findViewById(R.id.tvMsg);
        volleyParams = new VolleyParams(this);

        selectedImagePath = volleyParams.getLastImage();

        classifier = ClassifierFactory.getYolov3TinyInstance(this);
        imageSupport = new ImageSupport(this, classifier.getLabels());

        sideSelectorHandler = new OnSideSelectorHandler(this);
        ivSelector.setOnTouchListener(sideSelectorHandler);


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

    private void resetAll() {
        state = State.START;
        ivSelector.setAlpha(1f);
        ivSelector.setVisibility(View.VISIBLE);
        sideSelectorHandler.reset();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                selectedImagePath = imageSupport.getPath(selectedImageUri);
                volleyParams.setLastImage(selectedImagePath);
                loadImageFromPath();
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
                ivPreview.setImageBitmap(bitmap);

                Mat sideSelector = sideSelectorHandler.drawSideSelector();
                showSideSelector(sideSelector);
                resetAll();
            }
        }
    }


    @Override
    public void showImage() {
        showImage(sampledImage);
    }

    @Override
    public void showImage(Mat image) {
        Bitmap bitmap = imageSupport.matToBitmap(image);
        ivPreview.setImageBitmap(bitmap);
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

    public int getImageHeight() {
        return imageSupport.getImageHeight();
    }

    public int getImageWidth() {
        return imageSupport.getImageWidth();
    }

    public void showSideSelector(Mat img) {
        Bitmap bitmap = imageSupport.matToBitmap(img);
        ivSelector.setImageBitmap(bitmap);
    }

    public void sideSelected() {
        tvMsg.setText(R.string.processing);
        sideSelectorHandler.disableSelection();
        ComputeTask computeTask = new ComputeTask();
        computeTask.execute();
    }

    private class ComputeTask extends AsyncTask<Mat, String, Mat>{

        Mat tmpMat;

        @Override
        protected Mat doInBackground(Mat... mats) {
            List<Point> corners = findCorners();
            if (corners != null){
                //Mat correctedImage = imageSupport.projectOnHalfCourt(corners, sampledImage);
                //return correctedImage;

//                ImageSupport is2 = new ImageSupport(MainActivity.this, classifier.getLabels());
//                //Mat originalImage = Imgcodecs.imread(selectedImagePath);
//                Rect rectCrop = new Rect(1703, 1054, 3503, 1610);
//                Mat imageRoi = is2.loadImage(selectedImagePath, rectCrop);
//                Mat resized = imageSupport.resizeForYolo(imageRoi, classifier.getImageSize());
//                List<Classifier.Recognition> recognitions = classifier.recognizeImage(imageSupport.matToBitmap(resized));
//                tmpMat = imageSupport.drawBoxes(imageRoi, recognitions, 0.05);
//                return tmpMat;

                Mat resized = imageSupport.resizeForYolo(sampledImage, classifier.getImageSize());
                List<Classifier.Recognition> recognitions = classifier.recognizeImage(imageSupport.matToBitmap(resized));
                imageSupport.digitalization(sampledImage, corners, recognitions);

                //tmpMat = imageSupport.drawBoxes(sampledImage, recognitions, 0.05);
                return tmpMat;


            } else {
                return null;
            }



        }

        @Override
        protected void onPostExecute(Mat mat) {
            super.onPostExecute(mat);
            showImage(mat);
            tvMsg.setText(R.string.finished);
        }
    }

    public List<Point> findCorners() {
        if (sampledImage == null) {
            toast("Nessuna immagine caricata");
            return null;
        }

        Mat maskedMat = imageSupport.colorMask(sampledImage);
        Mat houghMat = imageSupport.houghTransform(maskedMat, volleyParams);

        List<Point> corners;
        try {
            corners = imageSupport.findCourtExtremesFromRigthView(houghMat);
        } catch (AppException e) {
            toast(e.getLocalizedMessage() + ". Seleziona manualmente i punti");
            //onTouchListener.enable();
            return null;
        }
        return corners;
    }

    private void toast(String msg) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, msg, duration);
        toast.show();
    }

    private enum State {
        START,
        SIDE_SELECTED,
        HOUGH,
        CORNERS,
        PERSON_DETECTION,
        PROJECTION,
        DIGITALIZATION
    }



}
