package it.scarpentim.volleycourtmapping;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import it.scarpentim.volleycourtmapping.classification.Classifier;

import static org.opencv.core.Core.FILLED;

public class ImageSupport {

    private static final String TAG = "volleyCourt";

    private static final double MARGIN = 0.01745 * 10;
    private static final double MAX_DISTANCE = 5;
    public static final int YOLO_SIZE = 416;
    private MainActivity activity;

    private static final Scalar WHITE = new Scalar(255,255,255);
    private static final Scalar BLACK = new Scalar(0,0,0);
    private static final Map<String, Scalar> colors = new HashMap<>();

    private float widthRatio;
    private float heightRatio;
    private int screenWidth;
    private int screenHeight;

    public ImageSupport(MainActivity mainActivity, String[] labels) {
        this.activity = mainActivity;
        initColors(labels);
        initDisplaySize();
    }

    private void initDisplaySize() {
        Display display = activity.getWindowManager().getDefaultDisplay();
        // Qui va selezionato l'import della classe "android graphics Point" !
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
    }

    private void initColors(String[] labels) {
        int r = 200;
        int g = 150;
        int b = 100;

        for (int i = 0; i < labels.length; i++) {
            r = (r + ((i+0) % 3 == 0 ? 0 : 103)) % 256;
            g = (g + ((i+1) % 3 == 0 ? 0 : 111)) % 256;
            b = (b + ((i+2) % 3 == 0 ? 0 : 117)) % 256;
            colors.put(labels[i], new Scalar(r,g,b));
        }
    }

    public Mat loadImage(String path) {
        Mat originalImage = Imgcodecs.imread(path);
        Mat rgbImage = new Mat();
        Imgproc.cvtColor(originalImage, rgbImage, Imgproc.COLOR_BGR2RGB);
        Mat sampledImage = new Mat();
        double downSampleRatio = calculateSubSampleSize(rgbImage, screenWidth, screenHeight);
        Imgproc.resize(rgbImage, sampledImage, new Size(), downSampleRatio,
                downSampleRatio, Imgproc.INTER_AREA);

        this.widthRatio = (float)sampledImage.cols() / YOLO_SIZE;
        this.heightRatio = (float)sampledImage.rows() / YOLO_SIZE;

        return sampledImage;
    }

    public double calculateSubSampleSize(Mat srcImage, int reqWidth, int reqHeight) {
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

    public String getPath(Uri uri) {
        if (uri == null) {
            return null;
        }
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = activity.getContentResolver().query(uri, projection,null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        return uri.getPath();
    }

    public Bitmap matToBitmap(Mat image)
    {
        // Creiamo una Bitmap
        Bitmap bitMap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.RGB_565);
        // Convertiamo l'immagine di tipo Mat in una Bitmap
        Utils.matToBitmap(image, bitMap);
        // Collego la ImageView e gli assegno la BitMap
        return bitMap;
    }


    public Mat houghTransform(Mat image, VolleySeekBarHandler sbh) {
        Mat mEdgeImage = cannyFilter(image, sbh);

        Mat lines = new Mat();
        Imgproc.HoughLinesP(mEdgeImage, lines, 1, Math.PI / 180, sbh.getHoughVotes(), sbh.getHoughMinlength(), sbh.getHoughMaxDistance());
                
//
        

        return lines;
    }

    public Mat cannyFilter(Mat image, VolleySeekBarHandler sbh) {
        Mat mGray = new Mat(image.height(),image.width(), CvType.CV_8UC1);
        Mat mEdgeImage = new Mat(image.height(),image.width(), CvType.CV_8UC1);
        Imgproc.cvtColor(image, mGray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(mGray, mEdgeImage, sbh.getCannyMinThres(), sbh.getCannyMaxThres());
        return mEdgeImage;
    }

    public Mat drawHoughLines(Mat sampledImage, Mat lines) {
        Mat resultMat = sampledImage.clone();

        Mat newLines = mergeNearParallelLines(lines);
//        Mat newLines = lines;

        for (int i = 0; i < newLines.rows(); i++) {
            double[] line = newLines.get(i, 0);
            double xStart = line[0],
                    yStart = line[1],
                    xEnd = line[2],
                    yEnd = line[3];
            org.opencv.core.Point lineStart = new org.opencv.core.Point(xStart, yStart);
            org.opencv.core.Point lineEnd = new org.opencv.core.Point(xEnd, yEnd);
            Imgproc.line(resultMat, lineStart, lineEnd, new Scalar(255, 0, 0), 2);
        }
        return resultMat;
    }

    private Mat mergeNearParallelLines(Mat lines) {

        List<LineFunction> fzs = matToLineFunction(lines);

        ListIterator<LineFunction> iterator1 = fzs.listIterator();
        while(iterator1.hasNext()){
            LineFunction fz1 = iterator1.next();

            ListIterator<LineFunction> iterator2 = fzs.listIterator();
            while(iterator2.hasNext()) {
                LineFunction fz2 = iterator2.next();

                if(fz1.equals(fz2))
                    continue;

                if (similarNearSegment(fz1, fz2)) {
                    Log.d(TAG, "linee parallele e vicine, equazioni:");
                    Log.d(TAG, String.format("y = %fx+%f", fz2.m, fz2.b));
                    setUnionLine(fz2, fz1);
                    iterator1.remove();
                    break;
                }
            }
        }

        Mat newLines = new Mat(fzs.size(),1, CvType.CV_32SC4);
        for (int i = 0; i < fzs.size(); i++) {
            double[] line = new double[4];
            line[0] = fzs.get(i).xStart;
            line[1] = fzs.get(i).yStart;
            line[2] = fzs.get(i).xEnd;
            line[3] = fzs.get(i).yEnd;
            newLines.put(i,0, line);
        }

        return newLines;


//        for (int i = 0; i < lines.rows(); i++) {
//            double[] line = lines.get(i, 0);
//            double  xStart = line[0],
//                    yStart = line[1],
//                    xEnd = line[2],
//                    yEnd = line[3];
//            org.opencv.core.Point lineStart = new org.opencv.core.Point(xStart, yStart);
//            org.opencv.core.Point lineEnd = new org.opencv.core.Point(xEnd, yEnd);
//
//            double m = (yEnd - yStart) / (xEnd - xStart);
//            double b = yStart - m * xStart;
//
//            Log.d(TAG, String.format("Verifica per y = %fx+%f", m,b));
//            int classifier = 0;
//            for (int j = 0; j < fzs.size(); j++) {
//                if (i != j){
//                    if (m > fzs.get(j).m - MARGIN && m < fzs.get(j).m + MARGIN)
//                    {
//                        Log.d(TAG, "linee parallele, equazioni:");
//
//                        Log.d(TAG, String.format("y%d = %fx+%f", j, fzs.get(j).m,fzs.get(j).b));
//                        fzs.remove(j);
//                        ;
//                    }
//                }
//            }
//        }
//
//        Mat newLines = new Mat(fzs.size(),1, CvType.CV_32SC4);
//        for (int i = 0; i < fzs.size(); i++) {
//            double[] line = newLines.get(i, 0);
//            line[0] = fzs.get(i).xStart;
//            line[1] = fzs.get(i).yStart;
//            line[2] = fzs.get(i).xEnd;
//            line[3] = fzs.get(i).yEnd;
//        }

    }

    private boolean similarNearSegment(LineFunction fz1, LineFunction fz2) {
        return similarSlopeLines(fz1, fz2)
                && nearSegments(fz1, fz2);
    }

    private List<LineFunction> matToLineFunction(Mat lines) {
        List<LineFunction> fzs = new ArrayList<>();

        for (int i = 0; i < lines.rows(); i++) {
            double[] line = lines.get(i, 0);
            LineFunction f = new LineFunction(lines.get(i, 0));
            fzs.add(f);
        }
        return fzs;
    }

    private void setUnionLine(LineFunction f1, LineFunction f2) {
        double xSx, ySx;
        if (f1.xStart < f1.xEnd && f1.xStart < f2.xStart && f1.xStart < f2.xEnd){
            xSx = f1.xStart;
            ySx = f1.yStart;
        }else if (f1.xEnd < f2.xStart && f1.xEnd < f2.xEnd){
            xSx = f1.xEnd;
            ySx = f1.yEnd;
        }else if (f2.xStart < f2.xEnd){
            xSx = f2.xStart;
            ySx = f2.yStart;
        }else{
            xSx = f2.xEnd;
            ySx = f2.yEnd;
        }
        double xDx, yDx;
        if (f1.xStart > f1.xEnd && f1.xStart > f2.xStart && f1.xStart > f2.xEnd){
            xDx = f1.xStart;
            yDx = f1.yStart;
        }else if (f1.xEnd > f2.xStart && f1.xEnd > f2.xEnd){
            xDx = f1.xEnd;
            yDx = f1.yEnd;
        }else if (f2.xStart > f2.xEnd){
            xDx = f2.xStart;
            yDx = f2.yStart;
        }else{
            xDx = f2.xEnd;
            yDx = f2.yEnd;
        }

        f1.xStart = xSx;
        f1.yStart = ySx;
        f1.xEnd = xDx;
        f1.yEnd = yDx;

    }

    /**
     * Verifica se due rette sono vicine l'una all'altra
     * @param f1
     * @param f2
     * @return
     */
    private boolean nearSegments(LineFunction f1, LineFunction f2) {
//        Log.v(TAG, "distanza tra rette " + f1 + " " + f2 + " --> " + d);
        return (f1.distanceSegmentToPoint(f2.getStartPoint()) < MAX_DISTANCE
                || f1.distanceSegmentToPoint(f2.getEndPoint()) < MAX_DISTANCE
                || f2.distanceSegmentToPoint(f1.getStartPoint()) < MAX_DISTANCE
                || f2.distanceSegmentToPoint(f1.getEndPoint()) < MAX_DISTANCE);
    }

    private boolean similarSlopeLines(LineFunction f1, LineFunction f2) {
        double atanF1 = Math.atan(f1.m);
        double atanF2 = Math.atan(f2.m);
        return atanF1 > atanF2 - MARGIN && atanF1 < atanF2 + MARGIN;
    }

    public Mat drawBoxes(Mat image, List<Classifier.Recognition> boxes, double confidenceThreshold) {
        Mat boxesImage = new Mat();
        image.copyTo(boxesImage);
        Scalar color;


        for (Classifier.Recognition box : boxes) {
            Log.i(TAG, String.valueOf(box));
            if (box.getConfidence() > confidenceThreshold) {
                color = colors.get(box.getTitle());

                org.opencv.core.Point pt1 = new org.opencv.core.Point(box.getLocation().left * widthRatio, box.getLocation().top * heightRatio);
                org.opencv.core.Point pt2 = new org.opencv.core.Point(box.getLocation().right * widthRatio, box.getLocation().bottom * heightRatio);
                Imgproc.rectangle(boxesImage, pt1, pt2, color, 3);
                org.opencv.core.Point pt3 = new org.opencv.core.Point(box.getLocation().left * widthRatio, box.getLocation().top * heightRatio);
                org.opencv.core.Point pt4 = new org.opencv.core.Point(Math.min(box.getLocation().right, box.getLocation().left + (box.getTitle().length() * 13)) * widthRatio, (box.getLocation().top + 11) * heightRatio);
                Imgproc.rectangle(boxesImage, pt3, pt4, color, FILLED);

                pt1.set(new double[] {pt1.x + 2*heightRatio, (pt1.y + 10*heightRatio)});
                Imgproc.putText(boxesImage, box.getTitle(), pt1, Core.FONT_HERSHEY_SIMPLEX, 0.4 * heightRatio, (isLight(color)?BLACK:WHITE), (int) (1 * heightRatio));
            }
        }

        return boxesImage;
    }

    private boolean isLight(Scalar color) {
        double r = color.val[0];
        double g = color.val[1];
        double b = color.val[2];
        double sum = r+g+b;
        return r + g > 210*2 ||  sum > (225*3);
    }

    public Mat resizeForYolo(Mat mat) {
        Mat returnMat = new Mat(YOLO_SIZE, YOLO_SIZE, mat.type());
        Imgproc.resize(mat, returnMat, returnMat.size());
        return returnMat;
    }

    public Mat projectOnHalfCourt(List<org.opencv.core.Point> corners, Mat sampledImage) {

        int maxSize = Math.min(screenHeight, screenWidth);
        Mat projectiveMat = courtProjectiveMat(corners, maxSize);
        Mat correctedImage = new Mat(maxSize, maxSize, 1);

        Imgproc.warpPerspective(sampledImage, correctedImage, projectiveMat, correctedImage.size());

        return correctedImage;
    }

    public Mat courtProjectiveMat(List<org.opencv.core.Point> corners, int maxSize) {

        Mat srcPoints = Converters.vector_Point2f_to_Mat(corners);
        Mat destPoints = Converters.vector_Point2f_to_Mat(
                Arrays.asList(
                        new org.opencv.core.Point[]{
                                new org.opencv.core.Point(maxSize, maxSize),
                                new org.opencv.core.Point(0, maxSize),
                                new org.opencv.core.Point(0, 0),
                                new org.opencv.core.Point(maxSize, 0)
                        }
                )
        );
        Mat transformation = Imgproc.getPerspectiveTransform(srcPoints, destPoints);
        return transformation;
    }

    public Mat houghTransformWithColorFilter(Mat image, VolleySeekBarHandler seekBarHandler) {
        Mat mask = new Mat();
        Scalar lowerB, upperB;
        Log.d(TAG, "tipo immagine: " + image.type());
        lowerB = new Scalar(0, 0, 60); //rgb(34, 86, 107)
        upperB = new Scalar(150, 255, 120); //rgb(30, 119, 161)
        Core.inRange(image, lowerB, upperB, mask);
        Mat dest = new Mat();
        Core.bitwise_and(image, image, dest, mask);
        //Mat mEdgeImage = cannyFilter(dest, seekBarHandler);

        Mat lines = new Mat();
        Imgproc.HoughLinesP(mask, lines, 1, Math.PI / 180, seekBarHandler.getHoughVotes(), seekBarHandler.getHoughMinlength(), seekBarHandler.getHoughMaxDistance());
        return lines;
    }

    public Mat colorMask(Mat image) {
        Mat mask = new Mat();
        Scalar lowerB, upperB;
        Log.d(TAG, "tipo immagine: " + image.type());
        lowerB = new Scalar(0, 0, 70); //rgb(34, 86, 107)
        upperB = new Scalar(150, 255, 120); //rgb(30, 119, 161)
        Core.inRange(image, lowerB, upperB, mask);
        Mat dest = new Mat();
        Core.bitwise_and(image, image, dest, mask);
        return dest;
    }

    public List<org.opencv.core.Point> findCourtExtremesFromRigthView(Mat lines) {

        lines = mergeNearParallelLines(lines);
        List<LineFunction> fzs = matToLineFunction(lines);
        List<org.opencv.core.Point> corners = new ArrayList<>();

        Log.d(TAG, "number of lines : "  + fzs.size());
        int idx = -1;
        double maxDist = Double.MIN_VALUE;
        for (int i = 0; i < fzs.size(); i++) {
            Log.d(TAG, "line " + i + " = " + fzs.get(i));
            double dist1 = hypotenuseSquare(fzs.get(i).xStart, fzs.get(i).yStart);
            double dist2 = hypotenuseSquare(fzs.get(i).xEnd, fzs.get(i).yEnd);
            double dist = Math.max(dist1, dist2);
            if (dist > maxDist) {
                idx = i;
                maxDist = dist;
            }
        }
        Log.d(TAG, String.valueOf(idx));
        List<org.opencv.core.Point> intersections = new ArrayList<>();
        maxDist = Double.MIN_VALUE;
        int maxInterscetionIdx = -1;
        int maxInterscetionLineIdx = -1;
        for (int i = 0; i < fzs.size(); i++) {
            if (i != idx){
                LineFunction f1 = fzs.get(idx);
                LineFunction f2 = fzs.get(i);
                org.opencv.core.Point intersection = f1.intersection(f2);
                if (intersection != null){
                    if (f1.segmentContainPoint(intersection) && f2.segmentContainPoint(intersection)){
                        intersections.add(intersection);
                        Log.d(TAG, "Intersezione in " + intersection);
                        double dist = hypotenuseSquare(intersection.x, intersection.y);
                        if (dist > maxDist){
                            maxInterscetionLineIdx = i;
                            maxInterscetionIdx = intersections.size() - 1;
                            maxDist = dist;
                        }
                    }
                }
            }
        }
        org.opencv.core.Point maxIntersection = intersections.get(maxInterscetionIdx);
        Log.d(TAG, "maxIntersection: " + maxIntersection);
        corners.add(maxIntersection);
        return intersections;
    }

    private double hypotenuseSquare(double c1, double c2) {
        return c1 * c1 + c2 * c2;
    }
}
