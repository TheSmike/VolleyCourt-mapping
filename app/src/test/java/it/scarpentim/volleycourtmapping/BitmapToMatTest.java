package it.scarpentim.volleycourtmapping;

import android.graphics.Bitmap;
import android.util.Log;

import org.junit.Test;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class BitmapToMatTest extends OpenCVTestInitializer {

    private static final int W = 3, H = 3;

    @Test
    public void compareMatricesTest() {


        int[] intValues = new int[W*H];
        float[] floatValues =  new float[W * H * 3];

        int[] colors = {125,255,500,    255,500,120,    10,255,500};
        Bitmap bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.RGB_565); //RGB

        bitmap.getPixels(intValues, 0, W, 0, 0, W, H);
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
                assertEquals(bitmap.getPixel(c,r), image.get(r,c));
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


    @Test
    public void testMultiplication(){

        Mat m = new Mat(3,3, CvType.CV_32F);
        m.put(0,0, 10.82740443884044);
        m.put(0,1, -35.056559366336954);
        m.put(0,2, 10396.351575348615);
        m.put(1,0, 13.087245088033004);
        m.put(1,1, 37.01285409511175);
        m.put(1,2, -21334.85303208853);
        m.put(2,0, -0.0017975185888991375);
        m.put(2,1, 0.02438386677330884);
        m.put(2,2, 1.0);

        Mat p = new Mat(3,1, CvType.CV_32F);

        Mat mul = m.mul(p);

        for (int r = 0; r < mul.rows(); r++) {
            for (int c = 0; c < mul.rows(); c++) {
                System.out.println("mul.get(r,c) = " + mul.get(r,c));
            }
        }
    }

}