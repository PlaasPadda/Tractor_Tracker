package tractor_tracker.app;

// AssetCreationRequest.java - Dashboard stuur na Creator
public class AssetCreationRequest {
    String assetType;
    String assetID;

    public AssetCreationRequest(String assetType, String assetID) {
        this.assetType = assetType;
        this.assetID = assetID;
    }
}
