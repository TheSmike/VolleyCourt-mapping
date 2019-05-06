package it.scarpentim.volleycourtmapping;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "volleyCourt";


    private static final int SELECT_PICTURE = 1;

    private String selectedImagePath;
    Mat sampledImage=null;
    Mat originalImage=null;
    Mat greyImage=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mymenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_openGallery) {
            PermissionSupport.validateReadStoragePermission();
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
                selectedImagePath = getPath(selectedImageUri);
                Log.i(TAG, "selectedImagePath: " + selectedImagePath);
                //loadImage(selectedImagePath);
                loadImage(selectedImagePath);
                displayImage(sampledImage);
            }
        }
    }

    private void loadImage(String path) {
        originalImage = Imgcodecs.imread(path);
        Mat rgbImage = new Mat();
        Imgproc.cvtColor(originalImage, rgbImage, Imgproc.COLOR_BGR2RGB);
        Display display = getWindowManager().getDefaultDisplay();
// Qui va selezionato l'import della classe "android graphics Point" !
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        sampledImage = new Mat();
        double downSampleRatio = calculateSubSampleSize(rgbImage, width, height);
        Imgproc.resize(rgbImage, sampledImage, new Size(), downSampleRatio,
                downSampleRatio, Imgproc.INTER_AREA);
    }

    private double calculateSubSampleSize(Mat srcImage, int reqWidth, int reqHeight) {
        // Recuperiamo l'altezza e larghezza dell'immagine sorgente
        int height = srcImage.height();
        int width = srcImage.width();
        double inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            // Calcoliamo i rapporti tra altezza e larghezza richiesti e quelli dell'immagine sorgente
            double heightRatio = (double) reqHeight / (double) height;
            double widthRatio = (double) reqWidth / (double) width;
            // Scegliamo tra i due rapporti il minore
            inSampleSize = heightRatio<widthRatio ? heightRatio :widthRatio;
        }
        return inSampleSize;
    }

    private String getPath(Uri uri) {
        if (uri == null) {
            return null;
        }
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection,null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        return uri.getPath();
    }

    private void displayImage(Mat image)
    {
    // Creiamo una Bitmap
        Bitmap bitMap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.RGB_565);
        // Convertiamo l'immagine di tipo Mat in una Bitmap
        Utils.matToBitmap(image, bitMap);
        // Collego la ImageView e gli assegno la BitMap
        ImageView iv = findViewById(R.id.ivPreview);
        iv.setImageBitmap(bitMap);
    }
}
