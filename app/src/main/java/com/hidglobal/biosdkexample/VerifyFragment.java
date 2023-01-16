/*
 * Copyright 2019 HID Global Corporation/ASSA ABLOY AB. ALL RIGHTS RESERVED.
 *
 * You are free to use this example code to generate similar functionality
 * tailored to your own specific needs.
 *
 * For a list of applicable patents and patents pending, visit www.hidglobal.com/patents/
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.hidglobal.biosdkexample;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hidglobal.biosdk.BioDeviceStatus;
import com.hidglobal.biosdk.BioSDKDevice;
import com.hidglobal.biosdk.listener.ICaptureListener;
import com.hidglobal.biosdk.ImageTools;
import com.hidglobal.biosdk.listener.IWaitForFingerClearListener;

import java.util.HashMap;
import java.util.Map;

import static com.hidglobal.biosdk.BioDeviceStatus.*;

/**
 * VerifyFragment allows user to verify the enrolled fingerprint.  Implements the ICaptureListener
 * to receive acquisition mStatus messages from the biosdk and to receive capture complete message
 * with the fingerprint image, template, spoof score, and mStatus from the capture.  Matches the
 * template against the enrolled fingerprint templates.  Displays the fingerprint image with
 * template overlaid, spoof result in a progress bar, and match result in another progress bar.
 *
 * Implements the IWaitForFingerClearListener to receive acquisition mStatus messages during wait for
 * finger clear from the biosdk and to receive wait for finger clear complete.
 */

public class VerifyFragment extends Fragment implements ICaptureListener, IWaitForFingerClearListener{
    private IFragmentListener mListener;
    private Button mVerifyButton;
    private ProgressBar mRealFingerProgressBar;
    private ProgressBar mMatchProgressBar;
    private ImageView mFingerImageView;
    private TextView mRealFingerTxtView;
    private TextView mMatchTxtView;
    private TextView mFingerFeedbackTxtView;
    BioDeviceStatus mStatus;
    int mColorGreen;
    int mColorRed;
    private int mTimeOut = 15;
    private String mMatchLevel= "MEDIUM";
    private String mPADLevel= "MEDIUM";
    private Bitmap mFingerImage;
    byte[] mProbeTemplate;
    private byte[] mTemplate;
    private int mPADResult;
    boolean mWaitForFingerClear = false;
    boolean mWaitForFingerClearRunning = false;
    private boolean mCancelCapture = false;
    boolean mCaptureInProgress = false;

    public VerifyFragment(){
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_verify, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initGUI(view);
    }

    private void initGUI(View view){
        mVerifyButton = view.findViewById(R.id.btn_verify);
        mRealFingerProgressBar = view.findViewById(R.id.realFingerProgressBarVerify);
        mMatchProgressBar = view.findViewById(R.id.matchProgressBar);
        mFingerImageView = view.findViewById(R.id.image_view_composite_ver);
        mRealFingerTxtView = view.findViewById(R.id.RealFingerLabelVer);
        mMatchTxtView = view.findViewById(R.id.MatchLabel);
        mFingerFeedbackTxtView = view.findViewById(R.id.fingerFeedbackVerLabel);
        mRealFingerTxtView.setText("");
        mMatchTxtView.setText("");
        mFingerFeedbackTxtView.setText("");
        mVerifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onVerify(v);
            }
        });
        mColorGreen = ContextCompat.getColor(view.getContext(), R.color.green);
        mColorRed = ContextCompat.getColor(view.getContext(), R.color.StopSignRed);
    }

    private void onVerify(View v) {
        BioSDKDevice fpDevice = mListener.getConnectedDevice();
        if(fpDevice == null){
            mListener.popupDialog("Fingerprint Device is null", "OK", false);
            return;
        }
        if(mCaptureInProgress == true) {
            mCancelCapture = true;
            mFingerFeedbackTxtView.setText("");
            mVerifyButton.setText("VERIFY");
            mCaptureInProgress = false;
            return;
        }
        Map<String, String> strMapSecurityLevel = new HashMap<>();
        strMapSecurityLevel.put("matching_security_level", mMatchLevel);
        strMapSecurityLevel.put("pad_security_level", mPADLevel);
        BioDeviceStatus status = fpDevice.setConfigurationState(strMapSecurityLevel);
        if(status != BioDeviceStatus.BIOSDK_OK){
            mListener.popupDialog("SetConfigState returned " + status.toString() + "(" + mMatchLevel + " " + mPADLevel + ")", "Exit",true);
            return;
        }
        // In this example we call capture_async().  Note: we could call verify_async() and pass in
        // mProbeTemplate instead of calling match() in displayResults() method.
        mStatus = fpDevice.capture_async(mTimeOut, this);
        if(mStatus != BIOSDK_OK){
            mFingerFeedbackTxtView.setText("");
            mVerifyButton.setText("VERIFY");
            mCaptureInProgress = false;
            mListener.handleBioDeviceStatus(mStatus);
        }
        resetGUI();
        mFingerFeedbackTxtView.setText("");
        mVerifyButton.setText("CANCEL");
        mCaptureInProgress = true;
    }

    public void setTimeOut(int timeOut){
        mTimeOut = timeOut;
    }

    public void setMatchLevel(String matchLevel){
        mMatchLevel = matchLevel.toUpperCase();
    }

    public void setPADLevel(String padLevel){
        mPADLevel = padLevel.toUpperCase();
    }

    public void setWaitForFingerClear(boolean waitForFingerClear){
        mWaitForFingerClear = waitForFingerClear;
    }

    @Override
    public boolean onUpdateStatus(int nAcqStatus){
        if(mCancelCapture){
            mCancelCapture = false;
            return false;
        }
        String feedback = "";

        if(mWaitForFingerClearRunning == true && nAcqStatus == AcqStatus.ACQ_FINGER_PRESENT){
            feedback = "Lift Finger";
        }
        else if(mWaitForFingerClearRunning == false && (nAcqStatus != AcqStatus.ACQ_PROCESSING || nAcqStatus != AcqStatus.ACQ_DONE)){
            feedback = "Finger Down";
        }
        class Runner implements Runnable{
            Runner(String str) {
                strFeedback = str;
            }
            @Override
            public void run(){
                mFingerFeedbackTxtView.setText(strFeedback);
            }
            final String strFeedback;
        }
        new Handler(Looper.getMainLooper()).post(new Runner(feedback));
        return true;
    }

    @Override
    public void bioSDKCaptureComplete(BioDeviceStatus result, Bitmap capImage, byte[] capTemplate, int capPADResult) {
        if(result != BIOSDK_OK){
            mListener.handleBioDeviceStatus(result);
            resetGUI();
            mVerifyButton.setText("VERIFY");
            mCaptureInProgress = false;
            return;
        }
        mFingerImage = capImage;
        mTemplate = capTemplate;
        mPADResult = capPADResult;

        if(mWaitForFingerClear){
            BioSDKDevice fpDevice = mListener.getConnectedDevice();
            if(fpDevice == null){
                return;
            }
            BioDeviceStatus status = fpDevice.waitForFingerClear_asynch(mTimeOut, this);
            if(status ==BIOSDK_OK) {
                mWaitForFingerClearRunning = true;
            }
        }
        else{
            mCaptureInProgress = false;
            displayResults();
        }
    }

    @Override
    public void bioSDKWaitForFingerClearComplete(BioDeviceStatus var1){
        mCaptureInProgress = false;
        displayResults();
    }

    private void displayResults() {
        mVerifyButton.setText("VERIFY");
        mFingerFeedbackTxtView.setText("");
        mWaitForFingerClearRunning = false;
        mFingerFeedbackTxtView.setText("");
        ImageTools.drawMinutiae(mFingerImage, mTemplate);
        mFingerImageView.setImageBitmap(mFingerImage);
        int percent = 0;

        String match = "";
        String fealFinger = "";

        int txtColorMatch = 0;
        int txtColorRealFinger = 0;

        Resources res = getResources();
        Drawable dRed = res.getDrawable(R.drawable.curved_progress_bar_red);
        Drawable dGreen = res.getDrawable(R.drawable.curved_progress_bar_green);

        if(mPADResult == 1) {
            mRealFingerProgressBar.setProgressDrawable(dGreen);
            fealFinger = "Genuine";
            txtColorRealFinger = mColorGreen;
            percent = 100;
        }
        else {
            mRealFingerProgressBar.setProgressDrawable(dRed);
            fealFinger = "Impostor";
            txtColorRealFinger = mColorRed;
            percent = 15;
        }
        mRealFingerProgressBar.setProgress(percent);
        mRealFingerTxtView.setText(fealFinger);
        mRealFingerTxtView.setTextColor(txtColorRealFinger);
        int matchScore = 0;
        if(mProbeTemplate != null){

            BioSDKDevice fpDevice = mListener.getConnectedDevice();
            if(fpDevice == null){
                return;
            }
            mStatus = fpDevice.match(mProbeTemplate, mTemplate);

            if(mStatus == BIOSDK_OK) {
                matchScore = fpDevice.getLastMatchResult();

                if(matchScore == 1) {
                    mMatchProgressBar.setProgressDrawable(dGreen);
                    mMatchProgressBar.setProgress(100);
                    match = "Match";
                    txtColorMatch = mColorGreen;
                }
                else {
                    mMatchProgressBar.setProgressDrawable(dRed);
                    mMatchProgressBar.setProgress(15);
                    match = "No Match";
                    txtColorMatch = mColorRed;
                }
            }
        }
        mMatchTxtView.setText(match);
        mMatchTxtView.setTextColor(txtColorMatch);
    }

    public void setProbeTemplate(byte[] probeTemplate){
        mProbeTemplate = probeTemplate;
    }

    public void resetGUI(){
        mRealFingerTxtView.setText("");
        mMatchTxtView.setText("");
        mFingerFeedbackTxtView.setText("");
        mFingerImageView.setImageDrawable(null);
        mMatchProgressBar.setProgress(0);
        mRealFingerProgressBar.setProgress(0);
        mRealFingerProgressBar.invalidate();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof IFragmentListener) {
            mListener = (IFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden){
        if(hidden){
            BioSDKDevice device = mListener.getConnectedDevice();
            if(device != null){
                mFingerFeedbackTxtView.setText("");
                mVerifyButton.setText("VERIFY");
                mCaptureInProgress = false;
                device.cancel_async();
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onStart(){
        super.onStart();
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    @Override
    public void onStop(){
        super.onStop();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

}
