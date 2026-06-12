package tractor_tracker.app;
// FarmInfo.java - Tractor info wat geforward word deur pipeline
public class FarmInfo {
    String tractorID;
    float fuelLevel;
    String locationID;
    String farmID;

    public FarmInfo(String tractorID, float fuelLevel, String locationID, String farmID) {
        this.tractorID = tractorID;
        this.fuelLevel = fuelLevel;
        this.locationID = locationID;
        this.farmID = farmID;
    }
}