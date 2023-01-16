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
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

/**
 * SettingsFragment allows user turn on or off wait for finger clear and to change the capture
 * timeout value.
 */

public class SettingsFragment extends Fragment {
    private IFragmentListener mListener;
    private Switch mFingerClearSwitch;
    private Spinner mTimeOutSpinner;
    final static int DEFAULT_TIMOUT_SPINNER_INDEX = 2;
    private int mTimeOutIndex = DEFAULT_TIMOUT_SPINNER_INDEX;
    ArrayList<String> mTimeOutArrayList;
    int mTimeOut;
    boolean mWaitForFingerClear = false;
    String mMatchLevel = "Medium";
    String mPADLevel = "Medium";


    public SettingsFragment(){
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
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initGUI(view);
    }

    int getTimeOut(int index){
        switch(index){
            case 0:
                return 0;
            case 1:
                return 5;
            case 2:
                return 15;
            case 3:
                return 30;
            case 4:
                return 45;
            case 5:
                return 60;
            default:
                return 15;
        }
    }

    private void initGUI(View view){
        mFingerClearSwitch = view.findViewById(R.id.switchFingerClear);
        // For the time out spinner control, we will add items dynamically
        mTimeOutSpinner = view.findViewById(R.id.spinnerTimeOut);
        mTimeOutArrayList = new ArrayList<>();
        mTimeOutArrayList.add("Infinite");
        mTimeOutArrayList.add("5 Seconds");
        mTimeOutArrayList.add("15 Seconds");
        mTimeOutArrayList.add("30 Seconds");
        mTimeOutArrayList.add("45 Seconds");
        mTimeOutArrayList.add("60 Seconds");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_dropdown_item, mTimeOutArrayList);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        mTimeOutSpinner.setAdapter(adapter);
        mTimeOutSpinner.setSelection(DEFAULT_TIMOUT_SPINNER_INDEX);
        mTimeOut = getTimeOut(DEFAULT_TIMOUT_SPINNER_INDEX);
        mTimeOutSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mTimeOutIndex = position;
                mTimeOut = getTimeOut(mTimeOutIndex);
            }
            @Override
            public void onNothingSelected(AdapterView <?> parent) {
            }
        });

        mFingerClearSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mWaitForFingerClear = isChecked;
            }
        });

        Spinner spMatchLevel = view.findViewById(R.id.spinner_matching_level);
        // For the match level spinner control, we will use the match_level_array from strings.xml
        ArrayAdapter<CharSequence> adapterMatchLevel = ArrayAdapter.createFromResource(spMatchLevel.getContext(),R.array.match_level_array, R.layout.spinner_dropdown_item);
        adapterMatchLevel.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spMatchLevel.setAdapter(adapterMatchLevel);
        spMatchLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mMatchLevel = parent.getItemAtPosition(position).toString().equals("Conv.")? "Convenience" : parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        int nPosIndex2 = getIndex(spMatchLevel, mMatchLevel);
        if(nPosIndex2 != -1){
            spMatchLevel.setSelection(nPosIndex2);
        }

        Spinner spPADLevel = view.findViewById(R.id.spinner_pad_level);
        // For the PAD level spinner control, we will use the pad_level_array from strings.xml
        ArrayAdapter<CharSequence> adapterPadLevel = ArrayAdapter.createFromResource(spPADLevel.getContext(),R.array.pad_level_array, R.layout.spinner_dropdown_item);
        adapterPadLevel.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spPADLevel.setAdapter(adapterPadLevel);

        spPADLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPADLevel = parent.getItemAtPosition(position).toString().equals("Conv.")? "Convenience" : parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        int nPosIndex = getIndex(spPADLevel, mPADLevel);
        if(nPosIndex != -1){
            spPADLevel.setSelection(nPosIndex);
        }
    }

    public int getTimeOut(){
        return mTimeOut;
    }

    public String getMatchLevel() {
        return mMatchLevel.toUpperCase();
    }

    public String getPADLevel(){
        return mPADLevel.toUpperCase();
    }

    public boolean getWaitForFingerClear(){
        return mWaitForFingerClear;
    }

    private int getIndex(Spinner spinner, String myString){
        if(myString.equals("0")){
            return 0;
        }
        for (int i=0;i<spinner.getCount();i++){
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(myString)){
                return i;
            }
        }
        return -1;
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
