package it.scarpentim.volleycourtmapping;

import android.content.SharedPreferences;
import android.widget.SeekBar;
import android.widget.Toast;

public class VolleySeekBarHandler implements SeekBar.OnSeekBarChangeListener {

    private int cannyMinThres;
    private int cannyMaxThres;
    private int houghVotes;
    private int houghMinlength;
    private int houghMaxDistance;

    private VolleyAbstractActivity activity;
    private VolleyParams volleyParams;


    public VolleySeekBarHandler(VolleyAbstractActivity activity, VolleyParams volleyParams) {
        this.activity = activity;
        this.volleyParams = volleyParams;
        SeekBar minSeek, maxSeek, houghVotesSeek, houghMinLengthSeek, houghMaxDistanceSeek;

        minSeek = activity.findViewById(R.id.seekBar_MinThreshCanny);
        cannyMinThres = volleyParams.getCannyMinThres(minSeek.getProgress());
        minSeek.setProgress(cannyMinThres);
        minSeek.setOnSeekBarChangeListener(this);

        maxSeek = activity.findViewById(R.id.seekBar_MaxThreshCanny);
        cannyMaxThres = volleyParams.getCannyMaxThres(maxSeek.getProgress());
        maxSeek.setProgress(cannyMaxThres);
        maxSeek.setOnSeekBarChangeListener(this);

        houghVotesSeek = activity.findViewById(R.id.seekBar_HoughVotes);
        houghVotes = volleyParams.getHoughVotes(houghVotesSeek.getProgress());
        houghVotesSeek.setProgress(houghVotes);
        houghVotesSeek.setOnSeekBarChangeListener(this);

        houghMinLengthSeek = activity.findViewById(R.id.seekBar_HoughMinLength);
        houghMinlength = volleyParams.getHoughMinlength(houghMinLengthSeek.getProgress());
        houghMinLengthSeek.setProgress(houghMinlength);
        houghMinLengthSeek.setOnSeekBarChangeListener(this);

        houghMaxDistanceSeek = activity.findViewById(R.id.seekBar_HoughMaxDistance);
        houghMaxDistance = volleyParams.getHoughMaxDistance(houghMaxDistanceSeek.getProgress());
        houghMaxDistanceSeek.setProgress(houghMaxDistance);
        houghMaxDistanceSeek.setOnSeekBarChangeListener(this);

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.seekBar_MinThreshCanny:
                cannyMinThres = progress;
                volleyParams.setCannyMinThres(cannyMinThres);
                break;
            case R.id.seekBar_MaxThreshCanny:
                cannyMaxThres = progress;
                volleyParams.setCannyMaxThres(cannyMaxThres);
                break;
            case R.id.seekBar_HoughMaxDistance:
                houghMaxDistance = progress;
                volleyParams.setHoughMaxDistance(houghMaxDistance);
                break;
            case R.id.seekBar_HoughMinLength:
                houghMinlength = progress;
                volleyParams.setHoughMinlength(houghMinlength);
                break;
            case R.id.seekBar_HoughVotes:
                houghVotes = progress;
                volleyParams.setHoughVotes(houghVotes);
                break;
        }
        Toast.makeText(activity, String.valueOf(progress), Toast.LENGTH_SHORT ).show();
        activity.showImage();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
