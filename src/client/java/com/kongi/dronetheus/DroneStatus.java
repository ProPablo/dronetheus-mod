package com.kongi.dronetheus;

//TODO: This should be a record instead 
public class DroneStatus {
    public boolean streaming;
    public int queueSize;
    public boolean wasdEnabled;
    public String state;
    public DroneStatus(boolean streaming, int queueSize, boolean wasdEnabled, String state) {
        this.streaming = streaming;
        this.queueSize = queueSize;
        this.wasdEnabled = wasdEnabled;
        this.state = state;
    }
}
