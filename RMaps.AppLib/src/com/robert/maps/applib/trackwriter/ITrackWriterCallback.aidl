package com.robert.maps.applib.trackwriter;

oneway interface ITrackWriterCallback {
    /**
     * Called when the service has a new value for you.
     */
    void newPointWrited(double lat, double lon);
}
