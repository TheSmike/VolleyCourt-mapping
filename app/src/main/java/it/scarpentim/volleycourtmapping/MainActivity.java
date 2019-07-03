package it.scarpentim.volleycourtmapping;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.List;

import it.scarpentim.volleycourtmapping.exception.AppException;

public class MainActivity extends VolleyAbstractActivity {


    private OnSideSelectorHandler sideSelectorHandler;
    private ImageView ivSelector;

    protected TextView tvMsg;

    protected Button btnGoBack;
    protected Button btnGobackManCorners;
    protected Button btnProcessManCorners;
    private boolean leftSide;
    private boolean processing = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ivSelector = findViewById(R.id.ivSelector);
        sideSelectorHandler = new OnSideSelectorHandler(this);
        ivSelector.setOnTouchListener(sideSelectorHandler);
        findViewById(R.id.ivPreview).setOnTouchListener(onTouchListener);
        btnGoBack = findViewById(R.id.btn_goback);
        btnGobackManCorners = findViewById(R.id.btn_gobackManCorners);
        btnProcessManCorners = findViewById(R.id.btn_computeManCorners);
    }

    @Override
    public void onResume() {
        super.onResume();
        tvMsg = findViewById(R.id.tvDebugMsg);
        selectedImagePath = volleyParams.getLastImage();
        showMessage(R.string.court_side_quest);
        resetAll();
    }

    @Override
    protected void showMessage(int stringId) {
        tvMsg.setText(stringId);
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
                Intent myIntent = new Intent(MainActivity.this, DebugActivity.class);
                //myIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(myIntent);
                break;
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
        ivSelector.setAlpha(1f);
        ivSelector.setVisibility(View.VISIBLE);
        sideSelectorHandler.reset();
        btnGobackManCorners.setVisibility(View.GONE);
        btnProcessManCorners.setVisibility(View.GONE);
        btnGoBack.setVisibility(View.GONE);
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

    @Override
    protected boolean loadImageFromPath() {
        super.loadImageFromPath();
        Mat sideSelector = sideSelectorHandler.drawSideSelector();
        showSideSelector(sideSelector);
        onTouchListener.setImage(sampledImage);
        resetAll();
        return false;
    }

    @Override
    public void onFinishManuallyCornersSelection(List<Point> corners) {
        btnProcessManCorners.setEnabled(true);
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

    public void sideSelected(boolean leftSide) {
        if (processing)
            return;

        this.leftSide = leftSide;
        tvMsg.setText(R.string.processing);
        sideSelectorHandler.disableSelection();
        try {
            List<Point> corners = findCorners(sampledImage, leftSide);
            DigitalizationTask computeTask = new DigitalizationTask(leftSide, corners, true);
            computeTask.execute();
            processing = true;
        } catch (AppException e) {
            Log.w(TAG, "Corners not found automatically");
            onTouchListener.enable();
            //toast(this.getString(R.string.lines_not_detected) +  ".\n" + getString(R.string.try_manually));
            tvMsg.setText("Warning!\n" + getString(R.string.lines_not_detected )+ ". " + getString(R.string.try_manually));

            btnGobackManCorners.setVisibility(View.VISIBLE);
            btnProcessManCorners.setVisibility(View.VISIBLE);
            ivSelector.setVisibility(View.GONE);
        }

    }

    public List<Point> findCorners(Mat image, boolean isToFlip) throws AppException {
        if (image == null) {
            toast("Nessuna immagine caricata");
            return null;
        }


        Mat maskedMat = imageSupport.colorMask( flipIfNeeded(image, isToFlip) );
        Mat houghMat = imageSupport.houghTransform(maskedMat, volleyParams);

        List<Point> corners = imageSupport.findCourtExtremesFromRigthView(houghMat);
        return corners;
    }

    private Mat flipIfNeeded(Mat image, boolean isToFlip) {
        if (isToFlip)
            return imageSupport.flip(image);
        else
            return image;
    }

    @Override
    protected void onPostExecuteDigitalizationTask(boolean success) {
        processing = false;
        super.onPostExecuteDigitalizationTask(success);
        ivSelector.setVisibility(View.GONE);
        btnProcessManCorners.setVisibility(View.GONE);
        btnGobackManCorners.setVisibility(View.GONE);
        btnGoBack.setText(R.string.goback);
        btnGoBack.setVisibility(View.VISIBLE);
    }

    public void goBack(View view) {
        ivSelector.setVisibility(View.VISIBLE);
        btnGoBack.setVisibility(View.GONE);
        sideSelectorHandler.reset();
        Mat sideSelector = sideSelectorHandler.drawSideSelector();
        showSideSelector(sideSelector);
        showMessage(R.string.court_side_quest);
        showImage();
        btnGobackManCorners.setVisibility(View.GONE);
        btnProcessManCorners.setVisibility(View.GONE);
        btnProcessManCorners.setEnabled(false);
        onTouchListener.reset();
    }

    public void processManuallyCorners(View view) {
        if (processing)
            return;
        tvMsg.setText(R.string.processing);
        sideSelectorHandler.disableSelection();
        DigitalizationTask computeTask = new DigitalizationTask(leftSide, onTouchListener.getCorners(), false);
        computeTask.execute();

    }
}
