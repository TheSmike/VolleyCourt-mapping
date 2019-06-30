package it.scarpentim.volleycourtmapping;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.core.Mat;

public class MainActivity extends VolleyAbstractActivity {


    private OnSideSelectorHandler sideSelectorHandler;
    private ImageView ivSelector;

    protected TextView tvMsg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ivSelector = findViewById(R.id.ivSelector);
        sideSelectorHandler = new OnSideSelectorHandler(this);
        ivSelector.setOnTouchListener(sideSelectorHandler);
    }

    @Override
    public void onResume() {
        super.onResume();
        tvMsg = findViewById(R.id.tvDebugMsg);
        showMessage(R.string.court_side_quest);
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
        resetAll();
        return false;
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
        tvMsg.setText(R.string.processing);
        sideSelectorHandler.disableSelection();
        DigitalizationTask computeTask = new DigitalizationTask(leftSide);
        computeTask.execute();
    }

    protected void hideSelector() {
        ivSelector.setVisibility(View.GONE);
    }

    @Override
    protected void onPostExecuteDigitalizationTask() {
        super.onPostExecuteDigitalizationTask();
        hideSelector();
    }
}
