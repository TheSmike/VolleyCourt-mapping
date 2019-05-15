package it.scarpentim.volleycourtmapping;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    private static final String TAG = "TEST_volleyCourt";

    private static final int W = 3, H = 3;

    private static Context appContext = InstrumentationRegistry.getTargetContext();
    private static final Object lock = new Object();

    @BeforeClass
    public static void beforeClass() throws InterruptedException {
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, appContext, mLoaderCallback);
        synchronized (lock) {
            lock.wait();
        }
    }

    @Test
    public void testConvertImageBitmap() throws IOException {
        Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
        AssetManager assetManager = testContext.getAssets();
        InputStream testInput = assetManager.open("images/sample.jpg");
        Bitmap bitmap = BitmapFactory.decodeStream(testInput);

        float[] floatValues =  new float[bitmap.getWidth() * bitmap.getHeight() * 3];
        int[] intValues = new int[bitmap.getWidth()*bitmap.getHeight()];

        assertNotNull(bitmap);
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        //bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int i = 0; i < intValues.length; ++i) {
            floatValues[i * 3 + 0] = ((intValues[i] >> 16) & 0xFF) / 255.0f;
            floatValues[i * 3 + 1] = ((intValues[i] >> 8) & 0xFF) / 255.0f;
            floatValues[i * 3 + 2] = (intValues[i] & 0xFF) / 255.0f;
        }
    }

    @Test
    public void testMatToBitmapCode() {


        int[] intValues = new int[W*H];
        float[] floatValues =  new float[W * H * 3];

        int[] colors = {125,255,500,    255,500,120,    10,255,500};
        Bitmap bitmap = Bitmap.createBitmap( W, H, Bitmap.Config.RGB_565); //RGB

        for (int r = 0; r < H; ++r) {
            for (int c = 0; c < W; ++c) {
                bitmap.setPixel(c,r, colors[r*W+c]);
            }
        }

            bitmap.getPixels(intValues, 0, W, 0, 0, W, H);
            //bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

            for (int i = 0; i < intValues.length; ++i) {
                floatValues[i * 3 + 0] = ((intValues[i] >> 16) & 0xFF) / 255.0f;
                floatValues[i * 3 + 1] = ((intValues[i] >> 8) & 0xFF) / 255.0f;
                floatValues[i * 3 + 2] = (intValues[i] & 0xFF) / 255.0f;
            }

            Mat image = new Mat(H, W, CvType.CV_8UC1);
            for (int r = 0; r < image.rows(); ++r) {
                for (int c = 0; c < image.cols(); ++c) {
                    image.put(r, c, colors[r * image.cols() + c]);
                }
            }

            for (int r = 0; r < image.rows(); ++r) {
                for (int c = 0; c < image.cols(); ++c) {
                    assertEquals(1, image.get(r,c).length);
                    assertEquals(intValues[r*image.cols() + c], image.get(r,c)[0], 0);
                }
            }


            for (int r = 0; r < image.rows(); ++r) {
                for (int c = 0; c < image.cols(); ++c) {
                    floatValues[(r * image.cols() + c) * 3 + 0] = ((intValues[r] >> 16) & 0xFF) / 255.0f;
                    floatValues[(r * image.cols() + c) * 3 + 1] = ((intValues[r] >> 8) & 0xFF) / 255.0f;
                    floatValues[(r * image.cols() + c) * 3 + 2] = (intValues[r] & 0xFF) / 255.0f;
                }


            }

            assertEquals(4, 2 + 2);
        }


    private static BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(appContext) {
        @Override
        // Una volta che OpenCV manager è connesso viene chiamato questo metodo di
        public void onManagerConnected(int status) {
            switch (status) {
                // Una volta che OpenCV manager si è connesso con successo
                // possiamo abilitare l'interazione con la tlc
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    synchronized (lock) {
                        lock.notify();
                    }
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };
}
