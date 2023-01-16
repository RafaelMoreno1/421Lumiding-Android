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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.hidglobal.biosdk.BioDeviceStatus;
import com.hidglobal.biosdk.BioSDKAPI;
import com.hidglobal.biosdk.BioSDKFactory;
import com.hidglobal.biosdk.BioSDKVisitor;

/**
 * InitFragment initializes the biosdk and implements the BioSDKVisitor to receive updates on
 * initialization progress and when initialization completes.
 */

public class InitFragment extends Fragment implements BioSDKVisitor{
    private ProgressBar mInitProgressBar;
    private BioDeviceStatus mStatus;
    private IFragmentListener mListener;

    public InitFragment() {
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
        return inflater.inflate(R.layout.fragment_init, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initGUI(view);
        initEngine();
    }

    private void initGUI(View view){
        mInitProgressBar = view.findViewById(R.id.initProgressBar);
    }

    private boolean initEngine(){
        BioSDKAPI api = BioSDKFactory.getBioSDK();
        if(api == null){
            return false;
        }
        api.enumerateDevices(this);
        return true;
    }

    @Override
    public void onUpdateProgress(int percent){
        mInitProgressBar.setProgress(percent);
    }

    @Override
    public void onEnumerateFinished(BioDeviceStatus result) {
        mStatus = result;
        switch(result){
            case BIOSDK_OK:
            case BIOSDK_ERROR_ALREADY_INITIALIZED:
                finishFragment();
                break;
            case BIOSDK_ERROR_INTERNAL:
            case BIOSDK_ERROR_USER_DENIED_PERMISSIONS:
                mListener.popupDialog("App Needs USB Permissions", "Exit", true);
                break;
            case BIOSDK_ERROR_NO_DEVICE_PRESENT:
                mListener.popupDialog("No Supported Device Found", "Exit", true);
                break;
            case BIOSDK_ERROR_SYSTEM_PERMISSIONS:
                mListener.popupDialog("Device open in another application", "Exit", true);
                break;
            default:
                mListener.popupDialog("Device open in another application", "Exit", true);
                break;
        }
    }

    private void finishFragment(){
        mListener.onInitializationFinished(mStatus);
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
