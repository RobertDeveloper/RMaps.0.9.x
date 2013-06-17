package com.robert.maps.applib.trackwriter;

oneway interface ITrackWriterCallback {
    /**
     * Called when the service has a new value for you.
     */
    void newPointWrited(double lat, double lon);
    void onTrackStatUpdate(int Cnt, double Distance, double Duration, double MaxSpeed, double AvgSpeed, int MoveTime, double AvgMoveSpeed);
}
