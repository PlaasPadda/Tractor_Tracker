package tractor_tracker.app;

//ReaderProposal.java - Reader stuur proposal aan Tractor 
public class ReaderProposal {
    String locationID;
    long detectionTime;

    public ReaderProposal(String locationID, long detectionTime) {
        this.locationID = locationID;
        this.detectionTime = detectionTime;
    }
}