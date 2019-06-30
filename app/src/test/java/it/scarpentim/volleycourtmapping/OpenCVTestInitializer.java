package it.scarpentim.volleycourtmapping;

import org.junit.Before;
import org.opencv.core.Core;

public class OpenCVTestInitializer {

        @Before
        public void initOpenCV() {
            String opencvpath = "D:\\Universita\\PASM\\OpenCV-3.1.0-android-sdk\\OpenCV-android-sdk\\sdk\\native\\jni\\";
            System.load(opencvpath + Core.NATIVE_LIBRARY_NAME + ".dll");
        }

}
