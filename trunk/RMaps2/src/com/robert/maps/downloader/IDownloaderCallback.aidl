package com.robert.maps.downloader;

oneway interface IDownloaderCallback {
    /**
     * Called when the service has a new value for you.
     */
    void downloadDone();
    void downloadStart(int tileCnt, long startTime, String fileName, int lat0, int lon0, int lat1, int lon1);
    void downloadTileDone(int tileCnt, int errorCnt, int x, int y, int z);
}
