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
 * EnrollFragment allows user to enroll a fingerprint.  Implements the ICaptureListener to receive
 * acquisition status messages from the biosdk and to receive capture complete message with the
 * fingerprint image, template, spoof result, and status from the capture.  Displays the fingerprint
 * image with template overlaid and spoof result in a progress bar.
 *
 * Implements the IWaitForFingerClearListener to receive acquisition status messages during wait for
 * finger clear from the biosdk and to receive wait for finger clear complete.
 */

public class EnrollFragment extends Fragment implements ICaptureListener, IWaitForFingerClearListener {
    private IFragmentListener mListener;
    private Button mEnrollButton;
    private ProgressBar mRealFingerProgressBar;
    private ImageView mFingerImageView;
    private TextView mRealFingerTxtView;
    private TextView mFingerFeedbackTxtView;
    BioDeviceStatus mStatus;
    int mColorGreen;
    int mColorRed;
    private int mTimeOut;
    private String mMatchLevel = "MEDIUM";
    private String mPADLevel = "MEDIUM";
    private Bitmap mFingerImage;
    private byte[] mTemplate;
    private int mPADResult;
    boolean mIsWaitForFingerClearRunning = false;
    private boolean mCancelCapture = false;
    boolean mWaitForFingerClear = false;
    boolean mCaptureInProgress = false;

    public EnrollFragment() {
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
        return inflater.inflate(R.layout.fragment_enroll, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initGUI(view);
    }

    private void initGUI(View view){
        mEnrollButton = view.findViewById(R.id.btn_enroll);
        mRealFingerProgressBar = view.findViewById(R.id.realFingerProgressBar);
        mFingerImageView = view.findViewById(R.id.image_view_composite);
        mRealFingerTxtView = view.findViewById(R.id.realFingerLabel);
        mRealFingerTxtView.setText("");
        mFingerFeedbackTxtView = view.findViewById(R.id.fingerFeedbackEnLabel);
        mFingerFeedbackTxtView.setText("");
        mEnrollButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEnroll(v);
            }
        });
        mColorGreen = ContextCompat.getColor(view.getContext(), R.color.green);
        mColorRed = ContextCompat.getColor(view.getContext(), R.color.StopSignRed);
    }

    private void onEnroll(View v) {
        BioSDKDevice fpDevice = mListener.getConnectedDevice();
        if(fpDevice == null){
            mListener.popupDialog("Fingerprint Device is null", "OK", false);
            return;
        }
        if(mCaptureInProgress == true){
            mCancelCapture = true;
            mFingerFeedbackTxtView.setText("");
            mEnrollButton.setText("ENROLL");
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
        mStatus = fpDevice.capture_async(mTimeOut, this);
        if(mStatus != BIOSDK_OK){
            mFingerFeedbackTxtView.setText("");
            mEnrollButton.setText("ENROLL");
            mCaptureInProgress = false;
            mListener.handleBioDeviceStatus(mStatus);
        }
        resetGUI();
        mFingerFeedbackTxtView.setText("");
        mEnrollButton.setText("CANCEL");
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
    public boolean onUpdateStatus(int acqStatus){
        if(mCancelCapture){
            mCancelCapture = false;
            return false;
        }

        String feedback = "";

        if(mIsWaitForFingerClearRunning == true && acqStatus == AcqStatus.ACQ_FINGER_PRESENT){
            feedback = "Lift Finger";
        }
        else if(mIsWaitForFingerClearRunning == false && (acqStatus != AcqStatus.ACQ_PROCESSING || acqStatus != AcqStatus.ACQ_DONE)){
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
    public void bioSDKCaptureComplete(BioDeviceStatus result, Bitmap capImage, byte[] capTemplate, int capPADResult){
        if(result != BIOSDK_OK){
            mListener.onEnrollmentFinished(null);
            mListener.handleBioDeviceStatus(result);
            resetGUI();
            mEnrollButton.setText("ENROLL");
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
            if(status == BIOSDK_OK) {
                mIsWaitForFingerClearRunning = true;
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

    private void displayResults(){
        mEnrollButton.setText("ENROLL");
        mFingerFeedbackTxtView.setText("");
        mIsWaitForFingerClearRunning = false;
        ImageTools.drawMinutiae(mFingerImage, mTemplate);
        mFingerImageView.setImageBitmap(mFingerImage);
        int percent = 0;
        String realFinger = "";
        int textColor = 0;
        Resources res = getResources();
        Drawable dRed = res.getDrawable(R.drawable.curved_progress_bar_red);
        Drawable dGreen = res.getDrawable(R.drawable.curved_progress_bar_green);
        if(mPADResult == 1 ) {
            mRealFingerProgressBar.setProgressDrawable(dGreen);
            textColor = mColorGreen;
            realFinger = "Genuine";
            percent = 100;
        }
        else {
            mRealFingerProgressBar.setProgressDrawable(dRed);
            textColor = mColorRed;
            realFinger = "Impostor";
            percent = 15;
        }
        mRealFingerProgressBar.setProgress(percent);
        mRealFingerTxtView.setText(realFinger);
        mRealFingerTxtView.setTextColor(textColor);
        mListener.onEnrollmentFinished(mTemplate);
    }

    public void resetGUI(){
        mFingerImageView.setImageDrawable(null);
        mRealFingerProgressBar.setProgress(0);
        mRealFingerTxtView.setText("");
        mFingerFeedbackTxtView.setText("");
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
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onStart(){
        super.onStart();

    }

    @Override
    public void onHiddenChanged(boolean hidden){
        if(hidden){
            BioSDKDevice device = mListener.getConnectedDevice();
            if(device != null){
                mFingerFeedbackTxtView.setText("");
                mEnrollButton.setText("ENROLL");
                mCaptureInProgress = false;
                device.cancel_async();
            }
        }
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
