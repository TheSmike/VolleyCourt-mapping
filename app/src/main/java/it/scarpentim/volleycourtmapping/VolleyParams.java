package it.scarpentim.volleycourtmapping;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;


public class VolleyParams {

    public static final String CANNY_MIN_THRES = "cannyMinThres";
    public static final String CANNY_MAX_THRES = "cannyMaxThres";
    public static final String HOUGH_MAX_DISTANCE = "houghMaxDistance";
    public static final String HOUGH_MIN_LENGTH = "houghMinlength";
    public static final String HOUGH_VOTES = "houghVotes";
    private static final String DEBUG_SIDE_LEFT = "debugSide";

    protected static final String LAST_IMAGE = "LAST_IMAGE";

    private static final int DEF_CANNY_MIN = 80;
    private static final int DEF_CANNY_MAX = 150;
    private static final int DEF_HOUGH_MAX_DISTANCE = 25;
    private static final int DEF_HOUGH_MIN_LENGTH = 125;
    private static final int DEF_HOUGH_VOTES = 20;


    private final SharedPreferences sharedPref;
    private final SharedPreferences.Editor editor;

    public VolleyParams(Activity activity) {
        sharedPref = activity.getSharedPreferences("VOLLEY_APP_PREF", Context.MODE_PRIVATE);
        editor = sharedPref.edit();

    }



    //getter

    public int getCannyMinThres(int defaultValue) {
        return sharedPref.getInt(CANNY_MIN_THRES, defaultValue);
    }

    public  int getCannyMaxThres(int defaultValue) {
        return sharedPref.getInt(CANNY_MAX_THRES, defaultValue);
    }

    public int getHoughMaxDistance(int defaultValue) {
        return sharedPref.getInt(HOUGH_MAX_DISTANCE, defaultValue);
    }

    public  int getHoughMinlength(int defaultValue) {
        return sharedPref.getInt(HOUGH_MIN_LENGTH, defaultValue);
    }

    public int getHoughVotes(int defaultValue) {
        return sharedPref.getInt(HOUGH_VOTES, defaultValue);
    }
    public String getLastImage(String defaultValue) {
        return sharedPref.getString(LAST_IMAGE, defaultValue);
    }
    public String getLastImage() {
        return sharedPref.getString(LAST_IMAGE, null);
    }

    public boolean isDebugSideLeft() {
        return sharedPref.getBoolean(DEBUG_SIDE_LEFT, false);
    }


    // setter

    public void setCannyMinThres(int val) {
        editor.putInt(CANNY_MIN_THRES, val);
        editor.commit();
    }

    public void setCannyMaxThres(int val) {
        editor.putInt(CANNY_MAX_THRES, val);
        editor.commit();
    }

    public void setHoughMaxDistance(int val) {
        editor.putInt(HOUGH_MAX_DISTANCE, val);
        editor.commit();
    }

    public void setHoughMinlength(int val) {
        editor.putInt(HOUGH_MIN_LENGTH, val);
        editor.commit();
    }

    public void setHoughVotes(int val) {
        editor.putInt(HOUGH_VOTES, val);
        editor.commit();
    }

    public void setLastImage(String val) {
        editor.putString(LAST_IMAGE, val);
        editor.commit();
    }
    public void setDebugSideLeft(boolean val) {
        editor.putBoolean(DEBUG_SIDE_LEFT, val);
        editor.commit();
    }


    //getter without default param

    public double getCannyMinThres() {
        return getCannyMinThres(DEF_CANNY_MIN);
    }

    public double getCannyMaxThres() {
        return getCannyMaxThres(DEF_CANNY_MAX);
    }

    public int getHoughVotes() {
        return getHoughVotes(DEF_HOUGH_VOTES);
    }

    public double getHoughMinlength() {
        return getHoughMinlength(DEF_HOUGH_MIN_LENGTH);
    }

    public double getHoughMaxDistance() {
        return getHoughMaxDistance(DEF_HOUGH_MAX_DISTANCE);
    }
}
