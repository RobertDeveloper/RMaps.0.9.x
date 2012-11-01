package com.robert.maps.downloader;

oneway interface IDownloaderCallback {
    /**
     * Called when the service has a new value for you.
     */
    void downloadDone();
}
