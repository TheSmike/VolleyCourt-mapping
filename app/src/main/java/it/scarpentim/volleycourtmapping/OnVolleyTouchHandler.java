package it.scarpentim.volleycourtmapping;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

class OnVolleyTouchHandler implements View.OnTouchListener {

    private Mat image = null;
    List<Point> mCorners;
    private MainActivity activity;

    public OnVolleyTouchHandler(MainActivity activity) {
        this.activity = activity;
        mCorners = new ArrayList<>();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (image == null)
            throw new RuntimeException("Immagine non caricata corettamente");

        int projectedX = (int)((double)motionEvent.getX() *
                ((double) image.width()/ (double)view.getWidth()));
        int projectedY = (int)((double)motionEvent.getY() *
                ((double) image.height()/ (double)view.getHeight()));
        Point corner = new Point(projectedX, projectedY);

        mCorners.add(corner);
        Imgproc.circle(image, corner, (int) 5, new Scalar(0,0,255),2);
        activity.showImage(image);
        return false;

    }

    public void setImage(Mat image) {
        this.image = image.clone();
    }

    public  List<Point> getCorners() {
        return mCorners;
    }

    public void reset() {
        mCorners.clear();

    }
}
