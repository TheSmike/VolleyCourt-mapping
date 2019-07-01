package it.scarpentim.volleycourtmapping;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.io.File;
import java.util.List;

import it.scarpentim.volleycourtmapping.classification.Classifier;
import it.scarpentim.volleycourtmapping.classification.ClassifierFactory;
import it.scarpentim.volleycourtmapping.exception.AppException;
import it.scarpentim.volleycourtmapping.image.ImageSupport;

public abstract class VolleyAbstractActivity extends AppCompatActivity {

    protected static final String TAG = "volleyCourt";
    protected static final int SELECT_PICTURE = 1;

    protected String selectedImagePath;
    protected Mat sampledImage = null;
    protected ImageSupport imageSupport = null;
    protected VolleyParams volleyParams;
    protected OnVolleyTouchHandler onTouchListener;

    private static Classifier classifier;

    protected ImageView ivPreview;


    public abstract void showImage();
    public abstract void showImage(Mat image);


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        volleyParams = new VolleyParams(this);
        onTouchListener = new OnVolleyTouchHandler(this);
        selectedImagePath = volleyParams.getLastImage();
        initClassifier();
        imageSupport = new ImageSupport(this, classifier.getLabels());
    }

    @Override
    public void onResume(){
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0,this, mLoaderCallback);
        initClassifier();
        ivPreview = findViewById(R.id.ivPreview);
    }

    @Override
    protected void onPause() {
        super.onPause();
//        classifier.close();
//        classifier = null;
    }

    private void initClassifier() {
        if (classifier == null)
            classifier = ClassifierFactory.getYolov3Instance(this);
    }

    public List<Point> findCorners(Mat image) throws AppException {
        if (image == null) {
            toast("Nessuna immagine caricata");
            return null;
        }

        Mat maskedMat = imageSupport.colorMask(image);
        Mat houghMat = imageSupport.houghTransform(maskedMat, volleyParams);

        List<Point> corners = imageSupport.findCourtExtremesFromRigthView(houghMat);
        return corners;
    }

    protected void toast(String msg) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, msg, duration);
        toast.show();
    }

    protected void toast(int idMsg) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, idMsg, duration);
        toast.show();
    }

    protected class DigitalizationTask extends AsyncTask<Mat, String, Mat> {

        private boolean leftSide;
        private List<Point> corners;

        public DigitalizationTask(boolean leftSide, List<Point> corners) {
            this.leftSide = leftSide;
            this.corners = corners;
        }

        @Override
        protected Mat doInBackground(Mat... mats) {
            try {
                Mat image;
                    if (leftSide)
                    image = imageSupport.flip(sampledImage);
                else
                    image = sampledImage;

                if (corners != null) {
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

                    Mat resized = imageSupport.resizeForYolo(image, classifier.getImageSize());
                    List<Classifier.Recognition> recognitions = classifier.recognizeImage(imageSupport.matToBitmap(resized));
                    Mat retMat = imageSupport.digitalization(image, corners, recognitions);
                    if (leftSide)
                        retMat = imageSupport.flip(retMat);

                    //tmpMat = imageSupport.drawBoxes(sampledImage, recognitions, 0.05);
                    return retMat;


                } else {
                    return null;
                }
            }catch (Exception e){
                e.printStackTrace();
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            toast(values[0]);
        }

        @Override
        protected void onPostExecute(Mat mat) {
            if( mat == null) {
                showMessage(R.string.badFinish);
                onPostExecuteDigitalizationTask(false);
            }else{
                super.onPostExecute(mat);
                showImage(mat);
                showMessage(R.string.finished);
                onPostExecuteDigitalizationTask(true);
            }

        }
    }

    protected void onPostExecuteDigitalizationTask(boolean success){

    }

    protected abstract void showMessage(int stringId);

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

    protected boolean loadImageFromPath() {
        if (selectedImagePath != null) {
            Log.i(TAG, "selectedImagePath: " + selectedImagePath);
            File file = new File(selectedImagePath);
            if(file.exists()) {
                sampledImage = imageSupport.loadImage(selectedImagePath);
                Bitmap bitmap = imageSupport.matToBitmap(sampledImage);
                ivPreview.setImageBitmap(bitmap);
                return true;
            }
        }
        return false;
    }

    private class ClassifierTask extends AsyncTask<Mat, String, List<Classifier.Recognition>>{

        Mat tmpMat;
        private boolean isToFlip;

        public ClassifierTask(boolean isToFlip) {
            this.isToFlip = isToFlip;
        }

        @Override
        protected List<Classifier.Recognition> doInBackground(Mat... mats) {
            Mat input;
            if (isToFlip)
                input = imageSupport.flip(mats[0]);
            else
                input = mats[0];

            Mat mat = imageSupport.resizeForYolo(input, classifier.getImageSize());
            List<Classifier.Recognition> recognitions = classifier.recognizeImage(imageSupport.matToBitmap(mat));
            tmpMat = imageSupport.drawBoxes(input, recognitions);
            return recognitions;
        }

        @Override
        protected void onPreExecute() {
            TextView msgView = findViewById(R.id.tvDebugMsg);
            msgView.setText(R.string.processing);
        }

        @Override
        protected void onPostExecute(List<Classifier.Recognition> mat) {
            super.onPostExecute(mat);
            onPostExecuteClassifierTask();
            showImage(tmpMat);
            TextView msgView = findViewById(R.id.tvDebugMsg);
            msgView.setText(R.string.finished);

        }
    }

    protected void executeAsyncClassification(Mat sampledImage, boolean isToFlip) {
        new ClassifierTask(isToFlip).execute(sampledImage);
    }

    protected void onPostExecuteClassifierTask(){

    }
}
