package com.clouder.watch.common.sync.message;

import com.clouder.watch.common.sync.SyncMessage;

/**
 * Created by yang_shoulai on 9/6/2015.
 */
public class SearchPhoneSyncMessage extends SyncMessage {

    public SearchPhoneSyncMessage(String packageName, String path) {
        super(packageName, path);
    }

    public SearchPhoneSyncMessage(String path) {
        this(null, path);
    }

    private boolean startSearch;

    public boolean isStartSearch() {
        return startSearch;
    }

    public void setStartSearch(boolean startSearch) {
        this.startSearch = startSearch;
    }
}
