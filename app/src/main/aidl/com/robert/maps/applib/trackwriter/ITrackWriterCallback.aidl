// ITrackWriterCallback.aidl
package com.robert.maps.applib.trackwriter;

// Declare any non-default types here with import statements

oneway interface ITrackWriterCallback {
    void newPointWrited(double lat, double lon);
    void onTrackStatUpdate(int Cnt, double Distance, long Duration, double MaxSpeed, double AvgSpeed, long MoveTime, double AvgMoveSpeed);
}
