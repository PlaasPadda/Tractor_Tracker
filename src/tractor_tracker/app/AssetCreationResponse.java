package tractor_tracker.app;
// AssetCreationResponse.java - received from Creator by Dashboard
public class AssetCreationResponse {
    String assetType;
    String assetID;
    String status;

    public AssetCreationResponse(String assetType, String assetID, String status) {
        this.assetType = assetType;
        this.assetID = assetID;
        this.status = status;
    }
}
