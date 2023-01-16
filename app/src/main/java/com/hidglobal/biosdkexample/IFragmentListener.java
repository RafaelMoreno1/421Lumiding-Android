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

import com.hidglobal.biosdk.BioDeviceStatus;
import com.hidglobal.biosdk.BioSDKDevice;

public interface IFragmentListener {
    void onInitializationFinished(BioDeviceStatus status);
    void onTerminate(String msg);
    BioSDKDevice getConnectedDevice();
    void onEnrollmentFinished(byte[] template);
    void handleBioDeviceStatus(BioDeviceStatus status);
    void popupDialog(String msg, String btn, final boolean terminal);
}
