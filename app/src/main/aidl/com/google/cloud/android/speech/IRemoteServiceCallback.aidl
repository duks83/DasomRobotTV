// IRemoteServiceCallback.aidl
package com.google.cloud.android.speech;

interface IRemoteServiceCallback {
    void onSpeechRecognized(String text);
}
