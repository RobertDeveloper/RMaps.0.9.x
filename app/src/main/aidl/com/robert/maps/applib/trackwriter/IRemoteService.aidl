// IRemoteService.aidl
package com.robert.maps.applib.trackwriter;

// Declare any non-default types here with import statements
import com.robert.maps.applib.trackwriter.ITrackWriterCallback;

interface IRemoteService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void registerCallback(ITrackWriterCallback cb);
    void unregisterCallback(ITrackWriterCallback cb);
}
