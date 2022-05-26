// IRemoteService.aidl
package com.google.cloud.android.speech;

import com.google.cloud.android.speech.IREmoteServiceCallback;

interface IRemoteService {
	void registerCallback(IRemoteServiceCallback callback);
	void unregisterCallback(IRemoteServiceCallback callback);
	void startRecognizing(int sampleRate);
	void finishRecognizing();
	void recognize(in byte[] data, int size);
}