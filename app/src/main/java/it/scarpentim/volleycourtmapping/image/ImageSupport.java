package it.scarpentim.volleycourtmapping.image;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import it.scarpentim.volleycourtmapping.R;
import it.scarpentim.volleycourtmapping.VolleyAbstractActivity;
import it.scarpentim.volleycourtmapping.VolleyParams;
import it.scarpentim.volleycourtmapping.classification.Classifier;
import it.scarpentim.volleycourtmapping.exception.AppException;
import it.scarpentim.volleycourtmapping.geometry.GeoUtils;
import it.scarpentim.volleycourtmapping.geometry.LineFunction;
import it.scarpentim.volleycourtmapping.geometry.LineIntersection;

import static org.opencv.core.Core.FILLED;

public class ImageSupport {

    private static final String TAG = "volleyCourt";

    private static final double SLOPE_MARGIN = 0.01745 * 2;
    private static final double MAX_DISTANCE = 10;

    public static final int YOLO_SIZE = 608;
    //public static final int YOLO_SIZE = 416;
    public static final Float CONFIDENCE_THRESHOLD = 0.0075f;

    private static final double POINT_MARGIN = 5;
    private static final double SQUARE_POINT_MARGIN = POINT_MARGIN * POINT_MARGIN;
    private static final double ROUND = 1;
    private static final org.opencv.core.Point MID_POINT = new org.opencv.core.Point(287, 124);


    private VolleyAbstractActivity activity;

    private static final Scalar WHITE = new Scalar(255, 255, 255);
    private static final Scalar BLACK = new Scalar(0, 0, 0);
    private static final Map<String, Scalar> colors = new HashMap<>();

    private float widthRatio;
    private float heightRatio;
    private int screenWidth;
    private int screenHeight;
    private int imageWidth;
    private int imageHeight;

    public ImageSupport(VolleyAbstractActivity mainActivity, String[] labels) {
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
            r = (r + ((i + 0) % 3 == 0 ? 0 : 103)) % 256;
            g = (g + ((i + 1) % 3 == 0 ? 0 : 111)) % 256;
            b = (b + ((i + 2) % 3 == 0 ? 0 : 117)) % 256;
            colors.put(labels[i], new Scalar(r, g, b));
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

        this.widthRatio = (float) sampledImage.cols() / YOLO_SIZE;
        this.heightRatio = (float) sampledImage.rows() / YOLO_SIZE;

        this.imageWidth = sampledImage.cols();
        this.imageHeight = sampledImage.rows();

        return sampledImage;
    }

    public Mat loadImage(String path, Rect rectCrop) {
        Mat originalImage = Imgcodecs.imread(path);
        Mat imageRoi = new Mat(originalImage, rectCrop);

        Mat rgbImage = new Mat();
        Imgproc.cvtColor(imageRoi, rgbImage, Imgproc.COLOR_BGR2RGB);
        Mat sampledImage = new Mat();
        double downSampleRatio = calculateSubSampleSize(rgbImage, screenWidth, screenHeight);
        Imgproc.resize(rgbImage, sampledImage, new Size(), downSampleRatio,
                downSampleRatio, Imgproc.INTER_AREA);

        this.widthRatio = (float) sampledImage.cols() / YOLO_SIZE;
        this.heightRatio = (float) sampledImage.rows() / YOLO_SIZE;

        this.imageWidth = sampledImage.cols();
        this.imageHeight = sampledImage.rows();

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
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    public String getPath(Uri uri) {
        if (uri == null) {
            return null;
        }
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = activity.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        return uri.getPath();
    }

    public Bitmap matToBitmap(Mat image) {
        // Creiamo una Bitmap
        Bitmap bitMap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.RGB_565);
        // Convertiamo l'immagine di tipo Mat in una Bitmap
        Utils.matToBitmap(image, bitMap);
        // Collego la ImageView e gli assegno la BitMap
        return bitMap;
    }


    public Mat houghTransform(Mat image, VolleyParams sbh) {
        Mat mEdgeImage = edgeDetector(image, sbh);

        Mat lines = new Mat();
        Imgproc.HoughLinesP(mEdgeImage, lines, 1, Math.PI / 180, sbh.getHoughVotes(), sbh.getHoughMinlength(), sbh.getHoughMaxDistance());

        return lines;
    }

    public Mat edgeDetector(Mat image, VolleyParams params) {
//        return sobelDetector(image);
        return cannyDetector(image, params);
    }

    private Mat sobelDetector(Mat image) {
        Mat blurredImage = new Mat();
        Size size = new Size(7, 7);
        Imgproc.GaussianBlur(image, blurredImage, size, 0, 0);
        Mat gray = new Mat();
        Imgproc.cvtColor(blurredImage, gray, Imgproc.COLOR_RGB2GRAY);
        Mat xFirstDervative = new Mat(), yFirstDervative = new Mat();
        int ddepth = CvType.CV_16S;
        Imgproc.Sobel(gray, xFirstDervative, ddepth, 1, 0);
        Imgproc.Sobel(gray, yFirstDervative, ddepth, 0, 1);
        Mat absXD = new Mat(), absYD = new Mat();
        Core.convertScaleAbs(xFirstDervative, absXD);
        Core.convertScaleAbs(yFirstDervative, absYD);
        Mat edgeImage = new Mat();
        Core.addWeighted(absXD, 0.5, absYD, 0.5, 0, edgeImage);
        return edgeImage;
    }

    private Mat cannyDetector(Mat image, VolleyParams sbh) {
        Mat mGray = new Mat(image.height(), image.width(), CvType.CV_8UC1);
        Mat mEdgeImage = new Mat(image.height(), image.width(), CvType.CV_8UC1);
        Imgproc.cvtColor(image, mGray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(mGray, mEdgeImage, sbh.getCannyMinThres(), sbh.getCannyMaxThres());
        return mEdgeImage;
    }

    public Mat drawHoughLines(Mat sampledImage, Mat lines) {
        Mat resultMat = sampledImage.clone();

//        Mat onewLines = mergeNearParallelLines(lines);
//        Mat newLines = mergeNearParallelLines(onewLines);

//        Mat newLines = lines;

        Mat newLines = mergeNearParallelLines(lines);


        for (int i = 0; i < newLines.rows(); i++) {
            double[] line = newLines.get(i, 0);
            double xStart = line[0],
                    yStart = line[1],
                    xEnd = line[2],
                    yEnd = line[3];
            org.opencv.core.Point lineStart = new org.opencv.core.Point(xStart, yStart);
            org.opencv.core.Point lineEnd = new org.opencv.core.Point(xEnd, yEnd);
//            if (fzs.get(i).m > (0.30 - 0.05) && fzs.get(i).m < (0.30 + 0.05)) {

            int r = (255 + i * 10) % 256;
            int g = (0 + i * 20) % 256;
            int b = (0 + i * 70) % 256;
            if (r + g + b < 300)
                g = 250;
            Scalar color = new Scalar(r, g, b);
            Imgproc.line(resultMat, lineStart, lineEnd, color, 3);
        }
//        }
        return resultMat;
    }

    private Mat mergeNearParallelLines(Mat lines) {

        List<LineFunction> fzs = matToLineFunction(lines);

        //tmp
//        List<LineFunction> newFzs = new ArrayList<>();
//        for (int i = 0; i < fzs.size(); i++) {
//            if (fzs.get(i).m > (-9.84 - 1.05) && fzs.get(i).m < (-9.84 + 1.05)) {
//                newFzs.add(fzs.get(i));
//            }
//        }
//        fzs = newFzs;
        //end tmp

        ListIterator<LineFunction> iterator1 = fzs.listIterator();
        while (iterator1.hasNext()) {
            LineFunction fz1 = iterator1.next();

            ListIterator<LineFunction> iterator2 = fzs.listIterator();
            while (iterator2.hasNext()) {
                LineFunction fz2 = iterator2.next();

                if (fz1.equals(fz2))
                    continue;

                if (similarNearSegment(fz1, fz2)) {
                    Log.d(TAG, "linee parallele e vicine, equazioni:");
                    Log.d(TAG, fz2.toString());
                    setUnionLine(fz2, fz1);
                    iterator1.remove();
                    break;
                }
            }
        }

        Mat newLines = new Mat(fzs.size(), 1, CvType.CV_32SC4);
        for (int i = 0; i < fzs.size(); i++) {
            double[] line = new double[4];
            line[0] = fzs.get(i).xStart;
            line[1] = fzs.get(i).yStart;
            line[2] = fzs.get(i).xEnd;
            line[3] = fzs.get(i).yEnd;
            newLines.put(i, 0, line);
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
//                    if (m > fzs.get(j).m - SLOPE_MARGIN && m < fzs.get(j).m + SLOPE_MARGIN)
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
        if (f1.topTo(MID_POINT)) {
            LineFunction topSegment = topSegment(f1, f2);
            double ySx = topSegment.computeY(xMin(f1, f2));
            double yDx = topSegment.computeY(xMax(f1, f2));
            double xSx = topSegment.computeX(ySx);
            double xDx = topSegment.computeX(yDx);
            if (Double.isNaN(xSx) || Double.isNaN(xDx)) {
                xSx = xMin(f1, f2);
                xDx = xMax(f1, f2);
            }
            f1.reload(xSx, ySx, xDx, yDx);
        } else if (f1.leftTo(MID_POINT)) {
            LineFunction leftSegment = leftSegment(f1, f2);
            double xTop = leftSegment.computeX(yMin(f1, f2));
            double xBottom = leftSegment.computeX(yMax(f1, f2));
            double yTop = leftSegment.computeY(xTop);
            double yBottom = leftSegment.computeY(xBottom);
            if (Double.isNaN(yTop) || Double.isNaN(yBottom)) {
                yTop = yMin(f1, f2);
                yBottom = yMax(f1, f2);
            }
            f1.reload(xTop, yTop, xBottom, yBottom);
        } else if (f1.bottomTo(MID_POINT)) {
            LineFunction bottomSegment = bottomSegment(f1, f2);
            double ySx = bottomSegment.computeY(xMin(f1, f2));
            double yDx = bottomSegment.computeY(xMax(f1, f2));
            double xSx = bottomSegment.computeX(ySx);
            double xDx = bottomSegment.computeX(yDx);
            if (Double.isNaN(xSx) || Double.isNaN(xDx)) {
                xSx = xMin(f1, f2);
                xDx = xMax(f1, f2);
            }
            f1.reload(xSx, ySx, xDx, yDx);
        } else if (f1.rightTo(MID_POINT)) {
            LineFunction rightSegment = rightSegment(f1, f2);
            double xTop = rightSegment.computeX(yMin(f1, f2));
            double xBottom = rightSegment.computeX(yMax(f1, f2));
            double yTop = rightSegment.computeY(xTop);
            double yBottom = rightSegment.computeY(xBottom);
            if (Double.isNaN(yTop) || Double.isNaN(yBottom)) {
                yTop = yMin(f1, f2);
                yBottom = yMax(f1, f2);
            }
            f1.reload(xTop, yTop, xBottom, yBottom);
        }
//        leftTopPoint(f1,f2);
//        leftBottomPoint(f1,f2);


//        double xSx, ySx;
//        if (f1.xStart < f1.xEnd && f1.xStart < f2.xStart && f1.xStart < f2.xEnd){
//            xSx = f1.xStart;
//            ySx = f1.yStart;
//        }else if (f1.xEnd < f2.xStart && f1.xEnd < f2.xEnd){
//            xSx = f1.xEnd;
//            ySx = f1.yEnd;
//        }else if (f2.xStart < f2.xEnd){
//            xSx = f2.xStart;
//            ySx = f2.yStart;
//        }else{
//            xSx = f2.xEnd;
//            ySx = f2.yEnd;
//        }
//        double xDx, yDx;
//        if (f1.xStart > f1.xEnd && f1.xStart > f2.xStart && f1.xStart > f2.xEnd){
//            xDx = f1.xStart;
//            yDx = f1.yStart;
//        }else if (f1.xEnd > f2.xStart && f1.xEnd > f2.xEnd){
//            xDx = f1.xEnd;
//            yDx = f1.yEnd;
//        }else if (f2.xStart > f2.xEnd){
//            xDx = f2.xStart;
//            yDx = f2.yStart;
//        }else{
//            xDx = f2.xEnd;
//            yDx = f2.yEnd;
//        }
//
//        f1.reload(xSx, ySx, xDx, yDx);
//        f1.xStart = xSx;
//        f1.yStart = ySx;
//        f1.xEnd = xDx;
//        f1.yEnd = yDx;

    }


    private LineFunction leftSegment(LineFunction f1, LineFunction f2) {
        if ((f1.xStart + f1.xEnd) < (f2.xStart + f2.xEnd))
            return f1;
        else
            return f2;
    }

    private LineFunction rightSegment(LineFunction f1, LineFunction f2) {
        if ((f1.xStart + f1.xEnd) > (f2.xStart + f2.xEnd))
            return f1;
        else
            return f2;
    }

    private double xMax(LineFunction f1, LineFunction f2) {
        return max(f1.xStart, f1.xEnd, f2.xStart, f2.xEnd);
    }

    private double max(double d1, double d2, double d3, double d4) {
        return Math.max(Math.max(d1, d2), Math.max(d3, d4));
    }

    private double xMin(LineFunction f1, LineFunction f2) {
        return min(f1.xStart, f1.xEnd, f2.xStart, f2.xEnd);
    }

    private double min(double d1, double d2, double d3, double d4) {
        return Math.min(Math.min(d1, d2), Math.min(d3, d4));
    }

    private double yMin(LineFunction f1, LineFunction f2) {
        return min(f1.yStart, f1.yEnd, f2.yStart, f2.yEnd);
    }

    private double yMax(LineFunction f1, LineFunction f2) {
        return max(f1.yStart, f1.yEnd, f2.yStart, f2.yEnd);
    }

    private LineFunction topSegment(LineFunction f1, LineFunction f2) {
        if ((f1.yStart + f1.yEnd) < (f2.yStart + f2.yEnd))
            return f1;
        else
            return f2;
    }

    private LineFunction bottomSegment(LineFunction f1, LineFunction f2) {
        if ((f1.yStart + f1.yEnd) > (f2.yStart + f2.yEnd))
            return f1;
        else
            return f2;
    }


    /**
     * Verifica se due rette sono vicine l'una all'altra
     *
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
        double atanF1 = f1.getSlopeInRadians();
        double atanF2 = f2.getSlopeInRadians();
        return atanF1 > atanF2 - SLOPE_MARGIN && atanF1 < atanF2 + SLOPE_MARGIN;
    }

    public Mat drawBoxes(Mat image, List<Classifier.Recognition> boxes) {
        Mat boxesImage = new Mat();
        image.copyTo(boxesImage);
        Scalar color;


        for (Classifier.Recognition box : boxes) {
            Log.i(TAG, String.valueOf(box));
            if (box.getTitle().equals("person") && box.getConfidence() > CONFIDENCE_THRESHOLD) {
                color = colors.get(box.getTitle());

                org.opencv.core.Point pt1 = new org.opencv.core.Point(box.getLocation().left * widthRatio, box.getLocation().top * heightRatio);
                org.opencv.core.Point pt2 = new org.opencv.core.Point(box.getLocation().right * widthRatio, box.getLocation().bottom * heightRatio);
                Imgproc.rectangle(boxesImage, pt1, pt2, color, 3);
                org.opencv.core.Point pt3 = new org.opencv.core.Point(box.getLocation().left * widthRatio, box.getLocation().top * heightRatio);
                org.opencv.core.Point pt4 = new org.opencv.core.Point(Math.min(box.getLocation().right, box.getLocation().left + (box.getTitle().length() * 13)) * widthRatio, (box.getLocation().top + 11) * heightRatio);
                Imgproc.rectangle(boxesImage, pt3, pt4, color, FILLED);

                pt1.set(new double[]{pt1.x + 2 * heightRatio, (pt1.y + 10 * heightRatio)});
                Imgproc.putText(boxesImage, box.getTitle(), pt1, Core.FONT_HERSHEY_SIMPLEX, 0.4 * heightRatio, (isLight(color) ? BLACK : WHITE), (int) (1 * heightRatio));
            }
        }

        return boxesImage;
    }

    private boolean isLight(Scalar color) {
        double r = color.val[0];
        double g = color.val[1];
        double b = color.val[2];
        double sum = r + g + b;
        return r + g > 210 * 2 || sum > (225 * 3);
    }

    public Mat resizeForYolo(Mat mat, int yoloSize) {
        Mat returnMat = new Mat(yoloSize, yoloSize, mat.type());
        Imgproc.resize(mat, returnMat, returnMat.size());
        return returnMat;
    }

    public Mat projectOnHalfCourt(List<org.opencv.core.Point> corners, Mat sampledImage) {

        int maxSize = Math.min(screenHeight, screenWidth);

        corners = reorderCorner(corners);

        Mat projectiveMat = courtProjectiveMat(corners, maxSize);
        Mat correctedImage = new Mat(maxSize, maxSize, 1);
        Imgproc.warpPerspective(sampledImage, correctedImage, projectiveMat, correctedImage.size());

        return correctedImage;
    }

    private List<org.opencv.core.Point> reorderCorner(List<org.opencv.core.Point> corners) {
        List<org.opencv.core.Point> newCorners = new ArrayList<>();

        org.opencv.core.Point rightBottom = new org.opencv.core.Point(screenWidth - 1, screenHeight - 1);
        double min = Double.MAX_VALUE;
        int idxBottomRight = -1;
        for (int i = 0; i < corners.size(); i++) {
            double d = GeoUtils.squarePointsDistance(corners.get(i), rightBottom);
            if (d < min) {
                min = d;
                idxBottomRight = i;
            }
        }
        org.opencv.core.Point first = corners.get(idxBottomRight);
        newCorners.add(first);

        double bestRight = Double.MIN_VALUE;
        int idxBestRight = -1;
        for (int i = 0; i < corners.size(); i++) {
            if (i != idxBottomRight) {
                double x = corners.get(i).x;
                if (x > bestRight) {
                    bestRight = x;
                    idxBestRight = i;
                }
            }
        }
        org.opencv.core.Point second = corners.get(idxBestRight);
        newCorners.add(second);

        double bestTop = Double.MAX_VALUE;
        int idxBestTop = -1;
        for (int i = 0; i < corners.size(); i++) {
            if (i != idxBottomRight && i != idxBestRight) {
                double y = corners.get(i).y;
                if (y < bestTop) {
                    bestTop = y;
                    idxBestTop = i;
                }
            }
        }
        org.opencv.core.Point third = corners.get(idxBestTop);
        newCorners.add(third);

        int idxBestLeftBottom = -1;
        for (int i = 0; i < corners.size(); i++) {
            if (i != idxBottomRight && i != idxBestRight && i != idxBestTop) {
                idxBestLeftBottom = i;
                break;
            }
        }
        org.opencv.core.Point fourth = corners.get(idxBestLeftBottom);
        newCorners.add(fourth);

        return newCorners;


//        for (int i = 0; i < corners.size(); i++) {
//
//        }


//        List<Point> screenCorners = new ArrayList<>();
//        Point rightBottom = new Point(screenWidth -1, screenHeight -1);
//        Point rightTop = new Point(screenWidth -1, 0);
//        Point leftTop = new Point(0, 0);
//        Point leftBottom = new Point(0, screenHeight -1);
//        screenCorners.add(rightBottom);
//        screenCorners.add(rightTop);
//        screenCorners.add(leftTop);
//        screenCorners.add(leftBottom);
//
//        for (Point screenCorner : screenCorners) {
//            double min = Double.MAX_VALUE;
//            int idxBottomRight = -1;
//            for (int i = 0; i < corners.size(); i++) {
//                double d = squarePointsDistance(corners.get(i), screenCorner);
//                if (d < min){
//                    min = d;
//                    idxBottomRight = i;
//                }
//            }
//            newCorners.add(corners.get(idxBottomRight));
//        }
//        return newCorners;

    }

    public Mat courtProjectiveMat(List<org.opencv.core.Point> corners, int maxSize) {

        double size = (float) maxSize * (3.0 / 9.0);

        Mat srcPoints = Converters.vector_Point2f_to_Mat(corners);
        Mat destPoints = Converters.vector_Point2f_to_Mat(
                Arrays.asList(
                        new org.opencv.core.Point[]{
                                new org.opencv.core.Point(0, maxSize),
                                new org.opencv.core.Point(maxSize, maxSize),
                                new org.opencv.core.Point(maxSize, size),
                                new org.opencv.core.Point(0, size),
                        }
                )
        );
        Mat transformation = Imgproc.getPerspectiveTransform(srcPoints, destPoints);
        return transformation;
    }

    public Mat houghTransformWithColorFilter(Mat image, VolleyParams volleyParams) {
        Mat mask = new Mat();
        Scalar lowerB, upperB;
        Log.d(TAG, "tipo immagine: " + image.type());
        lowerB = new Scalar(0, 0, 60); //rgb(34, 86, 107)
        upperB = new Scalar(150, 255, 120); //rgb(30, 119, 161)
        Core.inRange(image, lowerB, upperB, mask);
        Mat dest = new Mat();
        Core.bitwise_and(image, image, dest, mask);
        //Mat mEdgeImage = edgeDetector(dest, volleyParams);

        Mat lines = new Mat();
        Imgproc.HoughLinesP(mask, lines, 1, Math.PI / 180, volleyParams.getHoughVotes(), volleyParams.getHoughMinlength(), volleyParams.getHoughMaxDistance());
        return lines;
    }

    public Mat colorMask(Mat image) {
        Mat mask = new Mat();
        Mat dest = new Mat();

        Scalar lowerB, upperB;
        Log.d(TAG, "tipo immagine: " + image.type());
//        lowerB = new Scalar(50, 50, 70); //rgb(34, 86, 107)
//        upperB = new Scalar(165, 210, 130); //rgb(30, 119, 161)

        lowerB = new Scalar(28, 100, 62);
        upperB = new Scalar(153, 128, 161);

//        lowerB = new Scalar(28, 100, 34);
//        upperB = new Scalar(164, 128, 161);

        Core.inRange(image, lowerB, upperB, mask);
        Core.bitwise_or(image, image, dest, mask);

//        Mat result = blurImage(dest);

        return dest;
    }

    private Mat blurImage(Mat image) {

        Mat blurredImage = new Mat();
        Size size = new Size(7, 7);

        //action_average
//
        Imgproc.blur(image, blurredImage, size);
        return blurredImage;

        //action_gaussian
//        Imgproc.GaussianBlur(image, blurredImage, size, 0, 0);
//        return blurredImage;
        // median

//        int kernelDim = 7;
//        Imgproc.medianBlur(image, blurredImage, kernelDim);
//        return blurredImage;
    }

    private Scalar majorLimit(Scalar color) {
        return new Scalar(color.val[0] + ROUND, color.val[1] + ROUND, color.val[2] + ROUND);
    }

    private Scalar minorLimit(Scalar color) {
        return new Scalar(color.val[0] - ROUND, color.val[1] - ROUND, color.val[2] - ROUND);
    }

    public List<org.opencv.core.Point> findCourtExtremesFromRigthView(Mat lines) throws AppException {

        lines = mergeNearParallelLines(lines);
        List<LineFunction> fzs = matToLineFunction(lines);

        Log.d(TAG, "number of lines : " + fzs.size());

        LineFunction topBottomRightLine = getTopBottomRightLine(fzs);

        List<org.opencv.core.Point> intersections = new ArrayList<>();


        LineIntersection[] bottomLinesIntersection = getBottomRightLineIntersections(fzs, topBottomRightLine, topBottomRightLine.getExtremePointFarFromOrigin());
        for (int i = 0; i < bottomLinesIntersection.length; i++) {
            intersections.add(bottomLinesIntersection[i].getIntersection());
        }


//        List<org.opencv.core.Point> intersections = new ArrayList<>();
//        double maxDist = Double.MIN_VALUE;
//        int maxInterscetionIdx = -1;
//        int maxInterscetionLineIdx = -1;
//
//        List<LineFunction> newLines = new ArrayList<>();
//        for (int i = 0; i < fzs.size(); i++) {
//            LineFunction f1 = topBottomRightLine;
//            LineFunction f2 = fzs.get(i);
//            if (f1 != f2){
//                org.opencv.core.Point intersection = f1.intersection(f2);
//                if (intersection != null){
//                    if (f1.segmentContainPoint(intersection) && f2.segmentContainPoint(intersection)){
//                        intersections.add(intersection);
//                        newLines.add(f2);
//                        Log.d(TAG, "Intersezione in " + intersection);
//                        double dist = hypotenuseSquare(intersection.x, intersection.y);
//                        if (dist > maxDist){
//                            maxInterscetionLineIdx = i;
//                            maxInterscetionIdx = intersections.size() - 1;
//                            maxDist = dist;
//                        }
//                    }
//                }
//            }
//        }
//
//        for (int j = 0; j < newLines.size(); j++) {
//            for (int i = 0; i < fzs.size(); i++) {
//
//                    LineFunction f1 = newLines.get(j);
//                    LineFunction f2 = fzs.get(i);
//                    if (f1.equals(f2))
//                        continue;
//
//                    org.opencv.core.Point intersection = f1.intersection(f2);
//                    if (intersection != null){
//                        if (f1.segmentContainPoint(intersection) && f2.segmentContainPoint(intersection)){
//                            intersections.add(intersection);
//                            Log.d(TAG, "Intersezione in " + intersection);
//                            //double dist = hypotenuseSquare(intersection.x, intersection.y);
//                        }
//                    }
//
//            }
//
//        }
//
//
//        org.opencv.core.Point maxIntersection = intersections.get(maxInterscetionIdx);
//        Log.d(TAG, "maxIntersection: " + maxIntersection);
//        corners.add(maxIntersection);
//
//        ListIterator<org.opencv.core.Point> iterator = intersections.listIterator();
//        while(iterator.hasNext()){
//            org.opencv.core.Point next = iterator.next();
//            for (org.opencv.core.Point point : intersections) {
//                if (point != next) {
//                    if (arePointsNear(next, point)){
//                        iterator.remove();
//                        break;
//                    }
//                }
//            }
//        }

        return intersections;
    }

    private LineIntersection[] getBottomRightLineIntersections(List<LineFunction> fzs, LineFunction fBase, org.opencv.core.Point endPoint) throws AppException {

        double minDistance = Double.MAX_VALUE;
        int minIntersectionLineIdx = -1;
        org.opencv.core.Point minIntersection = null;

        double secondMinDistance = Double.MAX_VALUE;
        int secondMinIntersectionLineIdx = -1;
        org.opencv.core.Point secondMinIntersection = null;


        for (int i = 0; i < fzs.size(); i++) {
            LineFunction fi = fzs.get(i);
            if (fBase != fi) {
                org.opencv.core.Point intersection = fBase.intersection(fi);
                if (intersection != null) {
                    if (fBase.segmentContainPoint(intersection) && fi.segmentContainPoint(intersection)) {
                        double dist = distanceSquare(intersection, endPoint);
                        if (dist < minDistance) {
                            secondMinDistance = minDistance;
                            secondMinIntersectionLineIdx = minIntersectionLineIdx;
                            secondMinIntersection = minIntersection;

                            minDistance = dist;
                            minIntersectionLineIdx = i;
                            minIntersection = intersection;

                        } else if (dist < secondMinDistance) {
                            secondMinDistance = dist;
                            secondMinIntersectionLineIdx = i;
                            secondMinIntersection = intersection;
                        }
                    }
                }
            }
        }

        if (minIntersectionLineIdx == -1 || secondMinIntersectionLineIdx == -1)
            throw new AppException(activity.getString(R.string.lines_not_detected));

        LineIntersection lineIntersection1 = new LineIntersection(fzs.get(minIntersectionLineIdx), minIntersection);
        LineIntersection lineIntersection2 = new LineIntersection(fzs.get(secondMinIntersectionLineIdx), secondMinIntersection);

        List<LineFunction> conjunctionLines = new ArrayList<>();
        LineFunction f1 = lineIntersection1.getLineFunction();
        LineFunction f2 = lineIntersection2.getLineFunction();
        for (int i = 0; i < fzs.size(); i++) {
            LineFunction fi = fzs.get(i);
            if (f1 != fi && f2 != fi) {
                org.opencv.core.Point intersection1 = f1.intersection(fi);
                org.opencv.core.Point intersection2 = f2.intersection(fi);
                if (intersection1 != null && intersection2 != null) {
                    if (f1.segmentContainPoint(intersection1) && fi.segmentContainPoint(intersection1)
                            && f2.segmentContainPoint(intersection2) && fi.segmentContainPoint(intersection2)) {
                        conjunctionLines.add(fi);
                    }
                }
            }
        }

        //se c'è piu di una linea scegliamo quella più in basso (non dovrebbero esserci altri linee in mezzo)
        float max = Float.MIN_VALUE;
        int minIdx = -1;
        for (int i = 0; i < conjunctionLines.size(); i++) {
            if (conjunctionLines.get(i) == fBase)
                continue;
            org.opencv.core.Point intersection = conjunctionLines.get(i).intersection(f1);
            if (intersection.y > max)
                minIdx = i;
        }
        if (minIdx == -1)
            throw new AppException(activity.getString(R.string.lines_not_detected));

        LineFunction lineFunction3 = conjunctionLines.get(minIdx);
        org.opencv.core.Point intersection3 = lineFunction3.intersection(f1);
        org.opencv.core.Point intersection4 = lineFunction3.intersection(f2);

        LineIntersection lineIntersection3 = new LineIntersection(lineFunction3, intersection3);
        LineIntersection lineIntersection4 = new LineIntersection(lineFunction3, intersection4);

        return new LineIntersection[]{
                lineIntersection1,
                lineIntersection2,
                lineIntersection3,
                lineIntersection4
        };
    }

    private double distanceSquare(org.opencv.core.Point p1, org.opencv.core.Point p2) {
        return Math.pow(p1.y - p2.y, 2) + Math.pow(p1.x - p2.x, 2);
    }

    private LineFunction getTopBottomRightLine(List<LineFunction> fzs) {
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
        LineFunction bestBottomRightLine = fzs.get(idx);
        Log.d(TAG, String.format("line near bottom right corner fz%d ==> %s", idx, bestBottomRightLine));
        return bestBottomRightLine;
    }

    private boolean arePointsNear(org.opencv.core.Point p1, org.opencv.core.Point p2) {
        return Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2) < SQUARE_POINT_MARGIN;
    }

    private double hypotenuseSquare(double c1, double c2) {
        return c1 * c1 + c2 * c2;
    }

    public Mat toPhoneDim(Mat image) {
        return image;
//        Mat sampledImage = new Mat();
//        Imgproc.resize(image, sampledImage, new Size(), 0.5, 0.5, Imgproc.INTER_AREA);
//        return sampledImage;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public int getImageWidth() {
        return imageWidth;
    }


    public Mat digitalization(Mat sampledImage, List<org.opencv.core.Point> corners, List<Classifier.Recognition> recognitions, boolean digitalVersion) {

        Mat correctedImage;
        if (digitalVersion) {
            correctedImage = resizeForYolo(loadDigitalCourt(), sampledImage.cols());
        } else {
            correctedImage = projectOnHalfCourt(corners, sampledImage);
        }

        int maxSize = Math.min(screenHeight, screenWidth);
        corners = reorderCorner(corners);
        Mat projectiveMat = courtProjectiveMat(corners, maxSize);


        int counter = 0;
        Mat retMat = sampledImage.clone();
        for (Classifier.Recognition box : recognitions) {
            Log.i(TAG, "orderd box --> " + String.valueOf(box));
            if (box.getTitle().equals("person") && box.getConfidence() > CONFIDENCE_THRESHOLD) {

                org.opencv.core.Point pt1 = new org.opencv.core.Point(box.getLocation().left * widthRatio, box.getLocation().top * heightRatio);
                org.opencv.core.Point pt2 = new org.opencv.core.Point(box.getLocation().right * widthRatio, box.getLocation().bottom * heightRatio);
                Imgproc.rectangle(retMat, pt1, pt2, WHITE, 3);


                org.opencv.core.Point pt3 = new org.opencv.core.Point(box.getLocation().left * widthRatio, box.getLocation().top * heightRatio);
                org.opencv.core.Point pt4 = new org.opencv.core.Point(Math.min(box.getLocation().right, box.getLocation().left + (box.getTitle().length() * 13)) * widthRatio, (box.getLocation().top + 11) * heightRatio);
                Imgproc.rectangle(retMat, pt3, pt4, WHITE, FILLED);


                pt1.set(new double[] {pt1.x + 2*heightRatio, (pt1.y + 10*heightRatio)});
                Imgproc.putText(retMat, box.getTitle(), pt1, Core.FONT_HERSHEY_SIMPLEX, 0.4 * heightRatio, (BLACK), (int) (1 * heightRatio));


                org.opencv.core.Point bottomLeft = new org.opencv.core.Point(box.getLocation().left * widthRatio, box.getLocation().bottom * heightRatio);
                org.opencv.core.Point bottomRight = new org.opencv.core.Point(box.getLocation().right * widthRatio, box.getLocation().bottom * heightRatio);
                LineFunction baseLine = new LineFunction(new double[]{bottomLeft.x, bottomLeft.y, bottomRight.x, bottomRight.y});

                Imgproc.line(retMat, bottomLeft, bottomRight, BLACK, 3);

                Mat p = baseLine.midPointInHomogeneousCoord();
                Mat mul = new Mat(3,1,CvType.CV_64FC1);
                Core.gemm(projectiveMat, p, 1.0f, Mat.zeros(3, 1, CvType.CV_64FC1 ), 1.0f, mul, 0);
                //Core.multiply(projectiveMat, p, mul);


                double x = mul.get(0, 0)[0] / mul.get(2, 0)[0];
                double y = mul.get(1, 0)[0] / mul.get(2, 0)[0];
                double z = mul.get(2, 0)[0] / mul.get(2, 0)[0];


                //Core.gemm(mul, , 1 / p.get(2,0)[0], null, 0, mul, 0);
                //Core.multiply(mul, new Scalar(1 / p.get(2,0)[0]), dest);
                Log.d(TAG, "x => " + x);
                Log.d(TAG, "y => " + y);
                Log.d(TAG, "z => " + z);


                double mid = sampledImage.cols() / 2;
                if( x > 0 && x < correctedImage.cols() && y > 0 && y < correctedImage.rows()) {
                    double newx = ((x - mid) * 0.9) + mid;
                    double newy = ((y - mid) * 0.9) + mid;
                    Imgproc.circle(correctedImage, new org.opencv.core.Point(newx, newy), (int) (40*0.9), new Scalar(255, 153, 0), 5);
                    counter++;
                    if (counter == 6)
                        break;
                }



                //Imgproc.getPerspectiveTransform()
//                Mat correctedImage = new Mat(maxSize, maxSize, 1);
                //Imgproc.warpPerspective(sampledImage, correctedImage, projectiveMat, correctedImage.size());
            }
        }
        return correctedImage;
    }

    private Mat loadDigitalCourt() {
//        InputStream stream = null;
//
//        Uri uri = Uri.parse("file:///android_asset/image/digital_court.png");
//        try {
//            File file = new File("");
//            stream = activity.getContentResolver().openInputStream(uri);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
//        bmpFactoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
//
//        Bitmap bmp = BitmapFactory.decodeStream(stream, null, bmpFactoryOptions);
//        Mat ImageMat = new Mat();
        Mat mat = new Mat();
        Bitmap bmp = getBitmapFromAsset(activity, "digital_court.png");
        Utils.bitmapToMat(bmp, mat);
        return mat;
    }

    public static Bitmap getBitmapFromAsset(Context context, String filePath) {
        AssetManager assetManager = context.getAssets();

        InputStream istr;
        Bitmap bitmap = null;
        try {
            istr = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            // handle exception
        }

        return bitmap;
    }

    public Mat flip(Mat image) {
        Mat retMat = new Mat();
        Core.flip(image, retMat, 1 );
        return retMat;
    }

    public List<org.opencv.core.Point> flipCorners(List<org.opencv.core.Point> corners) {

        Log.d(TAG, "corners before flip: " + String.valueOf(corners));
        List<org.opencv.core.Point> flippedCorners = new ArrayList<>();
        int xAxisTmp = imageWidth / 2;
        for (org.opencv.core.Point corner : corners) {
            double newX = corner.x - xAxisTmp;
            newX = -newX;
            newX = newX + xAxisTmp - 1;
            flippedCorners.add(new org.opencv.core.Point(newX, corner.y));
        }
        Log.d(TAG, "corners after flip: " + String.valueOf(flippedCorners));
        return flippedCorners;

    }
}
