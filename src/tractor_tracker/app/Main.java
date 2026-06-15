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
        Object[] readerArgs01 = new Object[]{"Farm0", "location_01"};
        Object[] readerArgs02 = new Object[]{"Farm0", "location_02"};
        Object[] readerArgs11 = new Object[]{"Farm1", "location_11"};
        Object[] readerArgs12 = new Object[]{"Farm1", "location_12"};
        try {
            // Format: createNewAgent("agentName", "fully.qualified.ClassName", args)
        	// Servers
        	  mainContainer.createNewAgent("Server", "tractor_tracker.app.ServerAgent", null).start();
        	// Managers
        	  mainContainer.createNewAgent("FarmManager", "tractor_tracker.app.FarmManagerAgent", null).start();
        	// Farms
        	  mainContainer.createNewAgent("Farm0", "tractor_tracker.app.FarmAgent", null).start();
        	  mainContainer.createNewAgent("Farm1", "tractor_tracker.app.FarmAgent", null).start();
            // Readers	
        	  mainContainer.createNewAgent("Reader01", "tractor_tracker.app.ReaderAgent", readerArgs01).start();
        	  mainContainer.createNewAgent("Reader02", "tractor_tracker.app.ReaderAgent", readerArgs02).start();
        	  mainContainer.createNewAgent("Reader11", "tractor_tracker.app.ReaderAgent", readerArgs11).start();
        	  mainContainer.createNewAgent("Reader12", "tractor_tracker.app.ReaderAgent", readerArgs12).start();
        	//Tractors
        	  mainContainer.createNewAgent("Tractor1", "tractor_tracker.app.TractorAgent", null).start();
        	  mainContainer.createNewAgent("Tractor2", "tractor_tracker.app.TractorAgent", null).start();
        	// Dashboards
        	  mainContainer.createNewAgent("Dashboard", "tractor_tracker.app.DashboardAgent", null).start();


        } catch (StaleProxyException e) {
            e.printStackTrace();
        }

	}

}
