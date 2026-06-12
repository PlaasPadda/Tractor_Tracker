package tractor_tracker.app;
// DetectionConfirmation.java - Confirmation vanaf Reader na Tractor
public class DetectionConfirmation {
    String tractorID;
    String locationID;
    String status;

    public DetectionConfirmation(String tractorID, String locationID, String status) {
        this.tractorID = tractorID;
        this.locationID = locationID;
        this.status = status;
    }
}
