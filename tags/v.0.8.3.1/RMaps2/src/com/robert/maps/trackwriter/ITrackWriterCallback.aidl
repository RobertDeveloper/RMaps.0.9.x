package com.robert.maps.trackwriter;

oneway interface ITrackWriterCallback {
    /**
     * Called when the service has a new value for you.
     */
    void newPointWrited(double lat, double lon);
}
