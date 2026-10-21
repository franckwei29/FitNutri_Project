package com.fitnutri.backend.records;
public enum RecordSource {
    MANUAL, PHOTO, IMAGE, VOICE;
    /** JSON normalization only; the preserved legacy PHOTO column is never changed by a read. */
    public RecordSource canonical(){return this==PHOTO?IMAGE:this;}
}
