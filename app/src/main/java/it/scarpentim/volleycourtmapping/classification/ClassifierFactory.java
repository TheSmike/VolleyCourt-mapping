package it.scarpentim.volleycourtmapping.classification;

import android.app.Activity;
import android.util.Log;

import java.io.IOException;

public class ClassifierFactory {

    private static final int TINY_INPUT_SIZE = 416;
    private static final String TINY_INPUT_LAYER_NAME = "yolov3-tiny/net1";
    private static final String TINY_OUTPUT_LAYER_NAME = "yolov3-tiny/convolutional10/BiasAdd,yolov3-tiny/convolutional13/BiasAdd";
    private static final int[] TINY_YOLO_BLOCK_SIZE = {32, 16};

    private static final String TINY_MODEL_FILE = "ultimate_yolov3-tiny";
    private static final String TAG = "volleyCourt";

    public static Classifier getYolov3TinyInstance(Activity activity) {
            try {
                return YoloV3Classifier.create(
                        activity.getAssets(),
                        TINY_MODEL_FILE,
                        TINY_INPUT_SIZE,
                        TINY_INPUT_LAYER_NAME,
                        TINY_OUTPUT_LAYER_NAME,
                        TINY_YOLO_BLOCK_SIZE,
                        0);

            } catch (IOException e) {
                throw new RuntimeException("classifier init problem", e);
            }
    }

    private static final int INPUT_SIZE = 608;
    private static final String INPUT_LAYER_NAME = "yolov3/net1";
    private static final String OUTPUT_LAYER_NAME = "yolov3/convolutional59/BiasAdd,yolov3/convolutional67/BiasAdd,yolov3/convolutional75/BiasAdd";
    private static final int[] YOLO_BLOCK_SIZE = {32, 16, 8};

    private static final String MODEL_FILE = "yolov3";

    public static Classifier getYolov3Instance(Activity activity) {
        try {
            return YoloV3Classifier.create(
                    activity.getAssets(),
                    MODEL_FILE,
                    INPUT_SIZE,
                    INPUT_LAYER_NAME,
                    OUTPUT_LAYER_NAME,
                    YOLO_BLOCK_SIZE,
                    1);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage() + " -- " + String.valueOf(e));
            throw new RuntimeException("classifier init problem", e);
        }
    }
}
