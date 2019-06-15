package it.scarpentim.volleycourtmapping;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.SeekBar;
import android.widget.Toast;

public class VolleySeekBarHandler implements SeekBar.OnSeekBarChangeListener {


    public static final String CANNY_MIN_THRES = "cannyMinThres";
    public static final String CANNY_MAX_THRES = "cannyMaxThres";
    public static final String HOUGH_MAX_DISTANCE = "houghMaxDistance";
    public static final String HOUGH_MINLENGTH = "houghMinlength";
    public static final String HOUGH_VOTES = "houghVotes";

    private int cannyMinThres;
    private int cannyMaxThres;
    private int houghVotes;
    private int houghMinlength;
    private int houghMaxDistance;

    private VolleyAbstractActivity activity;
    private SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    public VolleySeekBarHandler(VolleyAbstractActivity activity) {
        this.activity = activity;
        SeekBar minSeek, maxSeek, houghVotesSeek, houghMinLengthSeek, houghMaxDistanceSeek;

        sharedPref = activity.getPreferences(Context.MODE_PRIVATE);

        minSeek = activity.findViewById(R.id.seekBar_MinThreshCanny);
        cannyMinThres = sharedPref.getInt(CANNY_MIN_THRES, minSeek.getProgress());
        minSeek.setProgress(cannyMinThres);
        minSeek.setOnSeekBarChangeListener(this);

        maxSeek = activity.findViewById(R.id.seekBar_MaxThreshCanny);
        cannyMaxThres = sharedPref.getInt(CANNY_MAX_THRES, maxSeek.getProgress());
        maxSeek.setProgress(cannyMaxThres);
        maxSeek.setOnSeekBarChangeListener(this);

        houghVotesSeek = activity.findViewById(R.id.seekBar_HoughVotes);
        houghVotes = sharedPref.getInt(HOUGH_VOTES, houghVotesSeek.getProgress());
        houghVotesSeek.setProgress(houghVotes);
        houghVotesSeek.setOnSeekBarChangeListener(this);

        houghMinLengthSeek = activity.findViewById(R.id.seekBar_HoughMinLength);
        houghMinlength = sharedPref.getInt(HOUGH_MINLENGTH, houghMinLengthSeek.getProgress());
        houghMinLengthSeek.setProgress(houghMinlength);
        houghMinLengthSeek.setOnSeekBarChangeListener(this);

        houghMaxDistanceSeek = activity.findViewById(R.id.seekBar_HoughMaxDistance);
        houghMaxDistance = sharedPref.getInt(HOUGH_MAX_DISTANCE, houghMaxDistanceSeek.getProgress());
        houghMaxDistanceSeek.setProgress(houghMaxDistance);
        houghMaxDistanceSeek.setOnSeekBarChangeListener(this);


        editor = sharedPref.edit();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.seekBar_MinThreshCanny:
                cannyMinThres = progress;
                editor.putInt(CANNY_MIN_THRES, cannyMinThres);
                break;
            case R.id.seekBar_MaxThreshCanny:
                cannyMaxThres = progress;
                editor.putInt(CANNY_MAX_THRES, cannyMaxThres);
                break;
            case R.id.seekBar_HoughMaxDistance:
                houghMaxDistance = progress;
                editor.putInt(HOUGH_MAX_DISTANCE, houghMaxDistance);
                break;
            case R.id.seekBar_HoughMinLength:
                houghMinlength = progress;
                editor.putInt(HOUGH_MINLENGTH, houghMinlength);
                break;
            case R.id.seekBar_HoughVotes:
                houghVotes = progress;
                editor.putInt(HOUGH_VOTES, houghVotes);
                break;
        }
        Toast.makeText(activity, String.valueOf(progress), Toast.LENGTH_SHORT ).show();
        activity.showImage();


        editor.commit();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public int getCannyMinThres() {
        return cannyMinThres;
    }

    public int getCannyMaxThres() {
        return cannyMaxThres;
    }

    public int getHoughMaxDistance() {
        return houghMaxDistance;
    }

    public int getHoughMinlength() {
        return houghMinlength;
    }

    public int getHoughVotes() {
        return houghVotes;
    }
}
