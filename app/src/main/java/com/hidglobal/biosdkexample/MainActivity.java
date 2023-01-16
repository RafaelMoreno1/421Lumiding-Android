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

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.hidglobal.biosdk.*;
import com.hidglobal.biosdk.BioSDKAPI;
import com.hidglobal.biosdk.listener.*;
import static com.hidglobal.biosdk.BioDeviceStatus.BIOSDK_OK;

/**
 * MainActivity for the app.  Implements IBioSDKDeviceListener to receive device connection events.
 * Creates the fragments responsible for initialization, enrollment, verification, and settings.
 * Serves as the hub coordinating access to the device.
 */

public class MainActivity extends AppCompatActivity implements IFragmentListener, IBioSDKDeviceListener{
    private final String TAG = "MainActivity";
    final FragmentManager mFragmentMgr = getSupportFragmentManager();
    final InitFragment mInitFragment = new InitFragment();
    final EnrollFragment mEnrollFragment = new EnrollFragment();
    final VerifyFragment mVerifyFragment = new VerifyFragment();
    final SettingsFragment mSettingsFragment = new SettingsFragment();
    private static final int INITIAL_TIMEOUT = 15;
    private BottomNavigationView mNavView;
    BioSDKDevice mFPDevice = null;
    Fragment mActiveFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adjustFontScale(getResources().getConfiguration());
        setContentView(R.layout.activity_main);
        mNavView = findViewById(R.id.nav_view);
        createFragments();
        setupNavigationView(mNavView);
        initializeSDK();
    }

    public void adjustFontScale(Configuration configuration) {
        if (configuration.fontScale > 1.20) {
            Log.w(TAG, "fontScale=" + configuration.fontScale); //Custom Log class, you can use Log.w
            Log.w(TAG, "font too big. scale down..."); //Custom Log class, you can use Log.w
            configuration.fontScale = (float) 1.20;
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(metrics);
            metrics.scaledDensity = configuration.fontScale * metrics.density;
            getBaseContext().getResources().updateConfiguration(configuration, metrics);
        }
    }

    public boolean initializeSDK(){
        // One time init
        BioSDKFactory.initializeBioSDKAPI(getApplicationContext());
        mFragmentMgr.beginTransaction().show(mInitFragment).commit();
        mActiveFragment = mInitFragment;
        return true;
    }

    private boolean initializeDevice(){
        BioSDKAPI bAPI = BioSDKFactory.getBioSDK();
        if(bAPI == null){
            popupDialog("No device connected.", "Exit", false);
            return false;
        }
        //The one-time open of the device
        mFPDevice = bAPI.openDevice(0);
        if(mFPDevice == null){
            popupDialog("No device connected.", "Exit", false);
            return false;
        }
        mFPDevice.setBioSDKDeviceListener(this);
        return true;
    }

    private void createFragments(){
        // Set timeout and wait for finger clear to false (default values for this example)
        mEnrollFragment.setTimeOut(INITIAL_TIMEOUT);
        mEnrollFragment.setWaitForFingerClear(false);
        mVerifyFragment.setTimeOut(INITIAL_TIMEOUT);
        mVerifyFragment.setWaitForFingerClear(false);
        // Create the fragments
        mFragmentMgr.beginTransaction().add(R.id.fragment_container, mInitFragment, "1").hide(mInitFragment).commit();
        mFragmentMgr.beginTransaction().add(R.id.fragment_container, mEnrollFragment, "2").hide(mEnrollFragment).commit();
        mFragmentMgr.beginTransaction().add(R.id.fragment_container, mVerifyFragment, "3").hide(mVerifyFragment).commit();
        mFragmentMgr.beginTransaction().add(R.id.fragment_container, mSettingsFragment, "4").hide(mSettingsFragment).commit();
    }

    private void setupNavigationView(BottomNavigationView navigationView) {
        navigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectNavigationItem(menuItem);
                        return true;
                    }
                });
    }

    public void selectNavigationItem(MenuItem menuItem) {
        // Create a new fragment and specify the fragment to show based on nav item clicked
        switch(menuItem.getItemId()) {
            case R.id.navigation_enroll:
                mEnrollFragment.resetGUI();
                mEnrollFragment.setTimeOut(mSettingsFragment.getTimeOut());
                mEnrollFragment.setMatchLevel(mSettingsFragment.getMatchLevel());
                mEnrollFragment.setPADLevel(mSettingsFragment.getPADLevel());
                mEnrollFragment.setWaitForFingerClear(mSettingsFragment.getWaitForFingerClear());
                mFragmentMgr.beginTransaction().hide(mActiveFragment).show(mEnrollFragment).commitAllowingStateLoss();
                mActiveFragment = mEnrollFragment;
                break;
            case R.id.navigation_verify:
                mVerifyFragment.resetGUI();
                mVerifyFragment.setTimeOut(mSettingsFragment.getTimeOut());
                mVerifyFragment.setMatchLevel(mSettingsFragment.getMatchLevel());
                mVerifyFragment.setPADLevel(mSettingsFragment.getPADLevel());
                mVerifyFragment.setWaitForFingerClear(mSettingsFragment.getWaitForFingerClear());
                mFragmentMgr.beginTransaction().hide(mActiveFragment).show(mVerifyFragment).commitAllowingStateLoss();
                mActiveFragment = mVerifyFragment;
                break;
            case R.id.navigation_settings:
                mFragmentMgr.beginTransaction().hide(mActiveFragment).show(mSettingsFragment).commitAllowingStateLoss();
                mActiveFragment = mSettingsFragment;
                break;
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        BioSDKFactory.releaseBioSDKAPI();
    }

    public void handleBioDeviceStatus(BioDeviceStatus status){
        switch(status){
            case BIOSDK_OK:
            {
                // Do nothing
            }break;
            case BIOSDK_TIMEOUT:
            {
                popupDialog("Capture Timed Out", "OK",false);
            } break;
            case BIOSDK_CANCELLED:
            {
                popupDialog("Capture Cancelled", "OK",false);
            } break;
            case BIOSDK_ERROR_ALREADY_INITIALIZED:
            {
                popupDialog("SDK Already Initialized", "OK", false);
            } break;
            case BIOSDK_ERROR_NOT_INITIALIZED:
            {
                popupDialog("SDK Not InitializedExiting: Exiting", "Exit", true);
            } break;
            case BIOSDK_ERROR_NO_DATA:
            {
                popupDialog("No Data", "OK", false);
            } break;
            case BIOSDK_ERROR_PARAMETER:
            {
                popupDialog("Bad Parameter", "OK", false);
            } break;
            case BIOSDK_ERROR_THREAD:
            {
                popupDialog("Thread Error", "OK", false);
            } break;
            case BIOSDK_ERROR_PROCESSING:
            {
                popupDialog("Processing Error", "OK", false);
            } break;
            case BIOSDK_ERROR_ASYNC_TASK_RUNNING:
            {
                popupDialog("Async Task Running", "OK", false);
            } break;
            case BIOSDK_ERROR_INTERNAL:
            {
                popupDialog("Internal Error: Exiting", "Exit", true);
            } break;
            case BIOSDK_ERROR_USER_DENIED_PERMISSIONS:
            {
                popupDialog("App Needs USB Permissions: Exiting", "Exit", true);
            } break;
            case BIOSDK_ERROR_SYSTEM_PERMISSIONS:
            {
                popupDialog("System Permission Error: Exiting", "Exit", true);
            } break;
            case BIOSDK_ERROR_NO_DEVICE_PRESENT:
            {
                popupDialog("No Supported Device Found: Exiting", "Exit", true);
            } break;
            case BIOSDK_ERROR_ENROLLMENTS_DO_NOT_MATCH:
            {
                popupDialog("Enrollments Do Not Match", "OK", false);
            } break;
            case BIOSDK_FINGER_PRESENT:
            {
                popupDialog("Finger is Present", "OK", false);
            } break;
            default:
            {
                popupDialog("Unknown Error", "OK", false);
            } break;
        }
    }

    public void popupDialog(String msg, String btn, final boolean terminal){

        // custom dialog
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.alert_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
        dialogButton.setText(btn);
        TextView tv = dialog.findViewById(R.id.text);
        tv.setText(msg);
        // if button is clicked, close the custom dialog
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if(terminal) terminate();
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                if(terminal) terminate();
            }
        });
        dialog.show();
    }


    private void terminate(){
        BioSDKFactory.releaseBioSDKAPI();
        finish();
        return;
    }

    @Override
    public void onInitializationFinished(BioDeviceStatus status){
        if(status != BIOSDK_OK){
            BioSDKFactory.releaseBioSDKAPI();
            finish();
            return;
        }
        if(false == initializeDevice()){
            return;
        }
        // Default to the Enroll Fragment
        mFragmentMgr.beginTransaction().hide(mActiveFragment).show(mEnrollFragment).commitAllowingStateLoss();
        mActiveFragment = mEnrollFragment;
        return;
    }

    @Override
    public void onEnrollmentFinished(byte[] template){
        // In a real integration, this enrollment template would be persisted in a database.  For
        // this source code example, we just send it to the Verify Fragment.
        mVerifyFragment.setProbeTemplate(template);
    }

    @Override
    public void onTerminate(String msg){
        popupDialog(msg, "Exit", true);
    }

    @Override
    public BioSDKDevice getConnectedDevice(){
        return mFPDevice;
    }

    @Override
    public Void device_connected() {
        return null;
    }

    @Override
    public Void device_disconnected() {
        mFPDevice = null;
        popupDialog("Device disconnected!", "Exit", true);
        return null;
    }

}
