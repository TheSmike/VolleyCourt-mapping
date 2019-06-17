package it.scarpentim.volleycourtmapping;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import it.scarpentim.volleycourtmapping.classification.Classifier;

public abstract class VolleyAbstractActivity extends AppCompatActivity {

        protected static final String TAG = "volleyCourt";
    protected static final int SELECT_PICTURE = 1;
    protected String selectedImagePath;

    protected Classifier classifier;

    public abstract void showImage();

    public abstract void showImage(Mat image);


}
