package tractor_tracker.app;
// AssetCreationResponse.java - Creator stuur na Dashboard
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
