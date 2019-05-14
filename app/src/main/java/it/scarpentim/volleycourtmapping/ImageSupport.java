package it.scarpentim.volleycourtmapping;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ImageSupport {

    private static final String TAG = "volleyCourt";

    private static final double MARGIN = 0.1;
    private static final double MAX_DISTANCE = 5;
    private MainActivity activity;

    public ImageSupport(MainActivity mainActivity) {
        this.activity = mainActivity;
    }

    public Mat loadImage(String path) {
        Mat originalImage = Imgcodecs.imread(path);
        Mat rgbImage = new Mat();
        Imgproc.cvtColor(originalImage, rgbImage, Imgproc.COLOR_BGR2RGB);
        Display display = activity.getWindowManager().getDefaultDisplay();
        // Qui va selezionato l'import della classe "android graphics Point" !
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        Mat sampledImage = new Mat();
        double downSampleRatio = calculateSubSampleSize(rgbImage, width, height);
        Imgproc.resize(rgbImage, sampledImage, new Size(), downSampleRatio,
                downSampleRatio, Imgproc.INTER_AREA);
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

        for (int i = 0; i < newLines.rows(); i++) {
            double[] line = newLines.get(i, 0);
            double xStart = line[0],
                    yStart = line[1],
                    xEnd = line[2],
                    yEnd = line[3];
            org.opencv.core.Point lineStart = new org.opencv.core.Point(xStart, yStart);
            org.opencv.core.Point lineEnd = new org.opencv.core.Point(xEnd, yEnd);
            Imgproc.line(resultMat, lineStart, lineEnd, new Scalar(255, 0, 0), 1);
        }
        return resultMat;
    }

    private Mat mergeNearParallelLines(Mat lines) {

        List<LineFunction> fzs = new ArrayList<>();

        for (int i = 0; i < lines.rows(); i++) {
            double[] line = lines.get(i, 0);
            double  xStart = line[0],
                    yStart = line[1],
                    xEnd = line[2],
                    yEnd = line[3];
            LineFunction f = new LineFunction(xStart, yStart, xEnd, yEnd);
            fzs.add(f);
        }

        for (int i = 0; i < fzs.size(); i++) {

            for (int j = 0; j < fzs.size(); j++) {
                if (i != j){
                    if (similarSlope(fzs.get(i), fzs.get(j)) && somePointsNear(fzs.get(i), fzs.get(j))) {
                        Log.d(TAG, "linee parallele e vicine, equazioni:");
                        Log.d(TAG, String.format("y%d = %fx+%f", j, fzs.get(j).m,fzs.get(j).b));
                        setUnionLine(fzs.get(i), fzs.get(j));
                        fzs.remove(j);
                    }
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
//            int c = 0;
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

    private boolean somePointsNear(LineFunction f1, LineFunction f2) {

        double y;
        y = f1.compute(f2.xStart);
        if (y > f2.yStart - MAX_DISTANCE && y < f2.yStart + MAX_DISTANCE)
            return true;

        y = f1.compute(f2.xEnd);
        if (y > f2.yStart - MAX_DISTANCE && y < f2.yStart + MAX_DISTANCE)
            return true;

        return false;
    }

    private boolean areNear(double x1, double y1, double x2, double y2) {
        return x1 > x2 - MAX_DISTANCE && x1 < x2 + MAX_DISTANCE
                && y1 > y2 - MAX_DISTANCE && y1 < y2 + MAX_DISTANCE;
    }

    private boolean similarSlope(LineFunction f1, LineFunction f2) {
        return f1.m > f2.m - MARGIN && f1.m < f2.m + MARGIN;
    }
}
