package it.scarpentim.volleycourtmapping.classification;

import android.app.Activity;

import java.io.IOException;

public class ClassifierFactory {

    private static final int INPUT_SIZE = 416;
    private static final String INPUT_LAYER_NAME = "yolov3-tiny/net1";
    private static final String OUTPUT_LAYER_NAME = "yolov3-tiny/convolutional10/BiasAdd,yolov3-tiny/convolutional13/BiasAdd";
    private static final int[] TINY_YOLO_BLOCK_SIZE = {32, 16};

    private static final String MODEL_FILE = "ultimate_yolov3-tiny";

    public static Classifier getYolov3Instance(Activity activity) {
            try {
                return YoloV3Classifier.create(
                        activity.getAssets(),
                        MODEL_FILE,
                        INPUT_SIZE,
                        INPUT_LAYER_NAME,
                        OUTPUT_LAYER_NAME,
                        TINY_YOLO_BLOCK_SIZE,
                        0);

            } catch (IOException e) {
                throw new RuntimeException("classifier init problem", e);
            }
    }
}
