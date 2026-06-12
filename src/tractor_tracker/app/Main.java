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
        Object[] readerArgs = new Object[]{"Farm0", "location_A"};
        try {
//            // Format: createNewAgent("agentName", "fully.qualified.ClassName", args)
        	  mainContainer.createNewAgent("Reader1", "tractor_tracker.app.ReaderAgent", readerArgs).start();
        	  mainContainer.createNewAgent("Tractor1", "tractor_tracker.app.TractorAgent", null).start();
//            mainContainer.createNewAgent("Server", "tractor.agents.ServerAgent", null).start();
//            mainContainer.createNewAgent("Creator", "tractor.agents.CreatorAgent", null).start();
//            mainContainer.createNewAgent("Dashboard", "tractor.agents.DashboardAgent", null).start();

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }

	}

}
