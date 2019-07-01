package it.scarpentim.volleycourtmapping;

import android.view.MotionEvent;
import android.view.View;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

class OnSideSelectorHandler implements View.OnTouchListener {

    private static final int MARGIN = 50;
    private static final int EXTRA = 25;

    private static final Scalar WHITE = new Scalar(255,255,255);
    private static final Scalar BLACK = new Scalar(0,0,0);

    private MainActivity activity;
    private Mat img;

    private int imageWidth;
    private int imageHeight;
    private int courtSide;
    private boolean enable = true;

    public OnSideSelectorHandler(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (enable) {
            int projectedX = (int) ((double) motionEvent.getX() *
                    ((double) img.width() / (double) view.getWidth()));
            int projectedY = (int) ((double) motionEvent.getY() *
                    ((double) img.height() / (double) view.getHeight()));
            Point corner = new Point(projectedX, projectedY);

            Point pt1, pt2, pt3, pt4;
            boolean leftSide;
            if (corner.x < imageWidth / 2) {
                leftSide = true;
                pt1 = new Point(MARGIN + 2, MARGIN + 2);
                pt2 = new Point(MARGIN + 2, MARGIN + courtSide - 2);
                pt3 = new Point(MARGIN + courtSide - 2, MARGIN + courtSide - 2);
                pt4 = new Point(MARGIN + courtSide - 2, MARGIN + 2);

            } else {
                leftSide = false;
                pt1 = new Point(MARGIN + courtSide + 2, MARGIN + 2);
                pt2 = new Point(MARGIN + courtSide + 2, MARGIN + courtSide - 2);
                pt3 = new Point(MARGIN + courtSide + courtSide - 2, MARGIN + courtSide - 2);
                pt4 = new Point(MARGIN + courtSide + courtSide - 2, MARGIN + 2);
            }

            MatOfPoint matOfPoints = new MatOfPoint();
            matOfPoints.fromArray(new Point[]{pt1, pt2, pt3, pt4});
            Imgproc.fillConvexPoly(img, matOfPoints, new Scalar(164, 200, 255));
            //;rectangle(img, pt1, pt2, new Scalar(0, 100, 255, 0.5), 3);

            if (corner.x < imageWidth / 2)
                writeSx(70);
            else
                writeDx(-70);

            activity.showSideSelector(img);
            activity.sideSelected(leftSide);
        }
        return false;
    }


    public Mat drawSideSelector() {
        imageWidth = activity.getImageWidth();
        imageHeight = activity.getImageHeight();
        img = new Mat(imageHeight, imageWidth, CvType.CV_8UC3);
        img.setTo(new Scalar(220,220,220,0));

        courtSide = (activity.getImageWidth() - MARGIN *2) / 2;
        int threeMeters = (int) (courtSide * (3.0/9));

        drawBlueLine(img, MARGIN, MARGIN, MARGIN, MARGIN + courtSide);
        drawBlueLine(img, MARGIN, MARGIN + courtSide, MARGIN + courtSide * 2, MARGIN + courtSide);
        drawBlueLine(img, MARGIN + courtSide * 2, MARGIN + courtSide, MARGIN + courtSide * 2, MARGIN);
        drawBlueLine(img, MARGIN + courtSide * 2, MARGIN, MARGIN, MARGIN);


        drawBlueLine(img, MARGIN + courtSide, MARGIN - EXTRA, MARGIN + courtSide, MARGIN + courtSide + EXTRA);
        drawBlueLine(img, MARGIN + courtSide - threeMeters, MARGIN, MARGIN + courtSide - threeMeters, MARGIN + courtSide);
        drawBlueLine(img, MARGIN + courtSide + threeMeters, MARGIN, MARGIN + courtSide + threeMeters, MARGIN + courtSide);


        writeSx(0);
        writeDx(0);

        return img;
    }

    private void writeDx(int xOffset) {
        Point ptTxt2 = new Point(courtSide + 250 + xOffset, imageWidth / 2 - 200);
        Imgproc.putText(img, "Dx", ptTxt2, Core.FONT_HERSHEY_SIMPLEX, 7, BLACK, 3);
    }

    private void writeSx(int xOffset) {
        Point ptTxt1 = new Point(courtSide / 2 + xOffset - 150, imageWidth / 2 - 200);
        Imgproc.putText(img, "Sx", ptTxt1, Core.FONT_HERSHEY_SIMPLEX, 7, BLACK, 3);
    }

    private void drawBlueLine(Mat img, int xStart, int yStart, int xEnd, int yEnd) {
        Scalar color = new Scalar(51, 102, 255);
        org.opencv.core.Point lineStart = new org.opencv.core.Point(xStart, yStart);
        org.opencv.core.Point lineEnd = new org.opencv.core.Point(xEnd, yEnd);
        Imgproc.line(img, lineStart, lineEnd, color, 4);
    }

    public void disableSelection() {
        enable = false;
    }

    public void reset() {
        enable = true;
    }
}
