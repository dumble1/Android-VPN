// IRemoteService.aidl
//from http://darphin.tistory.com/29
package com.example.anonymous;

import com.example.anonymous.IRemoteServiceCallback;
// Declare any non-default types here with import statements

interface IRemoteService {
   boolean registerCallback(IRemoteServiceCallback callback);
   	boolean unregisterCallback(IRemoteServiceCallback callback);
}
