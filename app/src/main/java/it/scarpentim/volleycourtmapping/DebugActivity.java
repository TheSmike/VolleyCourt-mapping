package it.scarpentim.volleycourtmapping;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.skydoves.colorpickerpreference.ColorEnvelope;
import com.skydoves.colorpickerpreference.ColorListener;
import com.skydoves.colorpickerpreference.ColorPickerDialog;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.List;

import it.scarpentim.volleycourtmapping.classification.Classifier;
import it.scarpentim.volleycourtmapping.classification.ClassifierFactory;
import it.scarpentim.volleycourtmapping.exception.AppException;
import it.scarpentim.volleycourtmapping.image.ImageSupport;

public class DebugActivity extends VolleyAbstractActivity {

    private ImageSupport imageSupport = null;
    VolleySeekBarHandler seekBarHandler;
    OnVolleyTouchHandler onTouchListener;

    private Mat sampledImage = null;
    private Mat drawedImage;
    private boolean isPerspectiveApplied = false;

    private List<Point> corners = null;


    private enum ShowType{
        IMAGE,
        CANNY,
        HOUGH,
        COLOR_TH,
    }

    VolleyParams volleyParams;

    private ShowType show = ShowType.IMAGE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        volleyParams = new VolleyParams(this);
        seekBarHandler = new VolleySeekBarHandler(this, volleyParams);
        selectedImagePath = volleyParams.getLastImage();
        onTouchListener = new OnVolleyTouchHandler(this);
        findViewById(R.id.ivPreview).setOnTouchListener(onTouchListener);
        classifier = ClassifierFactory.getYolov3Instance(this);
        imageSupport = new ImageSupport(this, classifier.getLabels());

//        createColorPicker();
    }

    private void createColorPicker() {
        ColorPickerDialog.Builder builder = new ColorPickerDialog.Builder(this); // (this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setTitle("ColorPicker Dialog");
        builder.setPreferenceName("MyColorPickerDialog");
//        builder.setFlagView(new CustomFlag(this, R.layout.layout_flag));
        builder.setPositiveButton(getString(R.string.confirm), new ColorListener() {

            @Override
            public void onColorSelected(ColorEnvelope colorEnvelope) {
                toast("colore: #" + colorEnvelope.getColorHtml());
//                TextView textView = findViewById(R.id.textView);
//                textView.setText("#" + colorEnvelope.getColorHtml());
//
//                LinearLayout linearLayout = findViewById(R.id.linearLayout);
//                linearLayout.setBackgroundColor(colorEnvelope.getColor());
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    @Override
    public void onResume(){
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0,this, mLoaderCallback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.debugmenu, menu);
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

            case R.id.action_Color_slicing:
                if (checkImageLoaded()){
                    show = ShowType.COLOR_TH;
                    showImage();
                    return true;
                }
                case R.id.action_resetImage:
                    if (checkImageLoaded()){
                        show = ShowType.IMAGE;
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
                isPerspectiveApplied = false;
                onTouchListener.reset();
                onTouchListener.setImage(sampledImage);
                Bitmap bitmap = imageSupport.matToBitmap(imageSupport.toPhoneDim(sampledImage));
                ImageView iv = findViewById(R.id.ivPreview);
                iv.setImageBitmap(bitmap);
            }
        }
    }

    @Override
    public void showImage() {
        if (!checkImageLoaded()) return;
        Bitmap bitmap;

        switch (show) {
            case IMAGE:
                bitmap = imageSupport.matToBitmap(sampledImage);
                break;
            case COLOR_TH:
                Mat maskedSMat = imageSupport.colorMask(sampledImage);
                bitmap = imageSupport.matToBitmap(maskedSMat);
                break;

            case CANNY:
                Mat maskedCMat = imageSupport.colorMask(sampledImage);
                Mat cannyMat = imageSupport.edgeDetector(maskedCMat, volleyParams);
                bitmap = imageSupport.matToBitmap(cannyMat);
                break;
            case HOUGH:
                Mat maskedMat = imageSupport.colorMask(sampledImage);
                Mat houghMat = imageSupport.houghTransform(maskedMat, volleyParams);
                drawedImage = imageSupport.drawHoughLines(maskedMat, houghMat);
                bitmap = imageSupport.matToBitmap(drawedImage);

//                Mat houghMat = imageSupport.houghTransformWithColorFilter(sampledImage, seekBarHandler);
//                drawedImage = imageSupport.drawHoughLines(sampledImage, houghMat);
//                bitmap = imageSupport.matToBitmap(drawedImage);

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

    public void findCorners(View view) {
        if (sampledImage == null) {
            Toast.makeText(this, "Nessuna immagine caricata", Toast.LENGTH_SHORT).show();
            return;
        }

        Mat maskedMat = imageSupport.colorMask(sampledImage);
        Mat houghMat = imageSupport.houghTransform(maskedMat, volleyParams);


        List<Point> corners = null;
        try {
            corners = imageSupport.findCourtExtremesFromRigthView(houghMat);
        } catch (AppException e) {
            toast(e.getLocalizedMessage() + ". Seleziona manualmente i punti");
            onTouchListener.enable();
            return;
        }

        Mat drawedMat =  sampledImage.clone();
        for (Point corner : corners) {
            Imgproc.circle(drawedMat, corner, (int) 20, new Scalar(0,100,255),3);
        }
        showImage(drawedMat);

        this.corners = corners;

        //Mat correctedImage = imageSupport.projectOnHalfCourt(onTouchListener.getCorners(), sampledImage);
        //showImage(correctedImage);
    }

    public void transformImage(View view) {
        if (isPerspectiveApplied) {
            isPerspectiveApplied = false;
            ((TextView)view).setText(R.string.transform);
            showImage(sampledImage);

        } else {

            if (onTouchListener.isEnable()) {
                corners = onTouchListener.getCorners();
                if (corners.size() != 4) {
                    toast("Selezionare 4 vertici prima di applicare la trasformazione");
                } else {
                    isPerspectiveApplied = true;
                    ((TextView)view).setText(R.string.goback);

                    Mat correctedImage = imageSupport.projectOnHalfCourt(corners, sampledImage);
                    showImage(correctedImage);
                }
            }else {

                if (corners == null) {
                    toast("Il processo per individuare i 4 vertici non è stato eseguito, premere il bottone Corners");
                } else if (corners.size() != 4) {
                    toast("Il processo non è riuscito ad individuare i 4 vertici in modo univoco!");
                } else {
                    isPerspectiveApplied = true;
                    ((TextView) view).setText(R.string.goback);

                    Mat correctedImage = imageSupport.projectOnHalfCourt(corners, sampledImage);
                    showImage(correctedImage);
                }
            }
        }
    }

    private void toast(String msg) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, msg, duration);
        toast.show();
    }

    public void manualTransformImage(View view) {
        if (isPerspectiveApplied) {
            isPerspectiveApplied = false;
            onTouchListener.reset();
            onTouchListener.setImage(sampledImage);
            showImage(sampledImage);
        } else {
            if (sampledImage == null) {
                toast("Nessuna immagine caricata");
                return;
            }
            if (onTouchListener.getCorners().size() != 4) {
                toast("Bisogna selezionare 4 vertici!");
            } else {
                isPerspectiveApplied = true;

                Mat correctedImage = imageSupport.projectOnHalfCourt(onTouchListener.getCorners(), sampledImage);
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
            Mat mat = imageSupport.resizeForYolo(mats[0], classifier.getImageSize());
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
