package tractor_tracker.app;

//TractorInfo.java - Tractor CFP na Readers
public class TractorInfo {
 String tractorID;
 float fuelLevel;
 String locationID;

 public TractorInfo(String tractorID, float fuelLevel, String locationID) {
     this.tractorID = tractorID;
     this.fuelLevel = fuelLevel;
     this.locationID = locationID;
 }
}