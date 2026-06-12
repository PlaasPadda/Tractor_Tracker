package tractor_tracker.app;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;

public class Main {

	public static void main(String[] args) {
		// Maak JADE runtime instance 
        Runtime runtime = Runtime.instance();

        // Maak profile vir main container
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.GUI, "true");        

        // Maak main container
        AgentContainer mainContainer = runtime.createMainContainer(profile);

        // Optional: spawn your first agents here at startup
        Object[] readerArgs1 = new Object[]{"Farm0", "location_A"};
        Object[] readerArgs2 = new Object[]{"Farm0", "location_B"};
        try {
            // Format: createNewAgent("agentName", "fully.qualified.ClassName", args)
        	// Managers
        	  mainContainer.createNewAgent("FarmManager", "tractor_tracker.app.FarmManagerAgent", null).start();
        	// Farms
        	  mainContainer.createNewAgent("Farm0", "tractor_tracker.app.FarmAgent", null).start();
            // Readers	
        	  mainContainer.createNewAgent("Reader1", "tractor_tracker.app.ReaderAgent", readerArgs1).start();
        	  mainContainer.createNewAgent("Reader2", "tractor_tracker.app.ReaderAgent", readerArgs2).start();
        	//Tractors
        	  mainContainer.createNewAgent("Tractor1", "tractor_tracker.app.TractorAgent", null).start();

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }

	}

}
