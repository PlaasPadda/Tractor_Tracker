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

        Object[] readerArgs11 = new Object[]{"Farm1", "farm1_p11"};
        Object[] readerArgs12 = new Object[]{"Farm1", "farm1_p12"};
        Object[] readerArgs13 = new Object[]{"Farm1", "farm1_p13"};
        Object[] readerArgs14 = new Object[]{"Farm1", "farm1_p21"};
        Object[] readerArgs15 = new Object[]{"Farm1", "farm1_p22"};
        Object[] readerArgs16 = new Object[]{"Farm1", "farm1_p23"};

        Object[] readerArgs21 = new Object[]{"Farm2", "farm2_p11"};
        Object[] readerArgs22 = new Object[]{"Farm2", "farm2_p12"};
        Object[] readerArgs23 = new Object[]{"Farm2", "farm2_p13"};
        Object[] readerArgs24 = new Object[]{"Farm2", "farm2_p21"};
        Object[] readerArgs25 = new Object[]{"Farm2", "farm2_p22"};
        Object[] readerArgs26 = new Object[]{"Farm2", "farm2_p23"};

        Object[] readerArgs31 = new Object[]{"Farm3", "farm3_p11"};
        Object[] readerArgs32 = new Object[]{"Farm3", "farm3_p12"};
        Object[] readerArgs33 = new Object[]{"Farm3", "farm3_p13"};
        Object[] readerArgs34 = new Object[]{"Farm3", "farm3_p21"};
        Object[] readerArgs35 = new Object[]{"Farm3", "farm3_p22"};
        Object[] readerArgs36 = new Object[]{"Farm3", "farm3_p23"};
        try {
            // Format: createNewAgent("agentName", "fully.qualified.ClassName", args)
        	// Creators
        	  mainContainer.createNewAgent("Creator", "tractor_tracker.app.CreatorAgent", null).start();
        	// Servers
        	  mainContainer.createNewAgent("Server", "tractor_tracker.app.ServerAgent", null).start();
        	// Managers
        	  mainContainer.createNewAgent("FarmManager", "tractor_tracker.app.FarmManagerAgent", null).start();
        	// Farms
        	  mainContainer.createNewAgent("Farm1", "tractor_tracker.app.FarmAgent", null).start();
        	  mainContainer.createNewAgent("Farm2", "tractor_tracker.app.FarmAgent", null).start();
        	  mainContainer.createNewAgent("Farm3", "tractor_tracker.app.FarmAgent", null).start();
            // Readers	
        	  mainContainer.createNewAgent("Farm1_11", "tractor_tracker.app.ReaderAgent", readerArgs11).start();
        	  mainContainer.createNewAgent("Farm1_12", "tractor_tracker.app.ReaderAgent", readerArgs12).start();
        	  mainContainer.createNewAgent("Farm1_13", "tractor_tracker.app.ReaderAgent", readerArgs13).start();
        	  mainContainer.createNewAgent("Farm1_21", "tractor_tracker.app.ReaderAgent", readerArgs14).start();
        	  mainContainer.createNewAgent("Farm1_22", "tractor_tracker.app.ReaderAgent", readerArgs15).start();
        	  mainContainer.createNewAgent("Farm1_23", "tractor_tracker.app.ReaderAgent", readerArgs16).start();

        	  mainContainer.createNewAgent("Farm2_11", "tractor_tracker.app.ReaderAgent", readerArgs21).start();
        	  mainContainer.createNewAgent("Farm2_12", "tractor_tracker.app.ReaderAgent", readerArgs22).start();
        	  mainContainer.createNewAgent("Farm2_13", "tractor_tracker.app.ReaderAgent", readerArgs23).start();
        	  mainContainer.createNewAgent("Farm2_21", "tractor_tracker.app.ReaderAgent", readerArgs24).start();
        	  mainContainer.createNewAgent("Farm2_22", "tractor_tracker.app.ReaderAgent", readerArgs25).start();
        	  mainContainer.createNewAgent("Farm2_23", "tractor_tracker.app.ReaderAgent", readerArgs26).start();

        	  mainContainer.createNewAgent("Farm3_11", "tractor_tracker.app.ReaderAgent", readerArgs31).start();
        	  mainContainer.createNewAgent("Farm3_12", "tractor_tracker.app.ReaderAgent", readerArgs32).start();
        	  mainContainer.createNewAgent("Farm3_13", "tractor_tracker.app.ReaderAgent", readerArgs33).start();
        	  mainContainer.createNewAgent("Farm3_21", "tractor_tracker.app.ReaderAgent", readerArgs34).start();
        	  mainContainer.createNewAgent("Farm3_22", "tractor_tracker.app.ReaderAgent", readerArgs35).start();
        	  mainContainer.createNewAgent("Farm3_23", "tractor_tracker.app.ReaderAgent", readerArgs36).start();
        	//Tractors
        	  mainContainer.createNewAgent("tractor1", "tractor_tracker.app.TractorAgent", null).start();
        	  mainContainer.createNewAgent("tractor2", "tractor_tracker.app.TractorAgent", null).start();
        	// Dashboards
        	  mainContainer.createNewAgent("Dashboard", "tractor_tracker.app.DashboardAgent", null).start();


        } catch (StaleProxyException e) {
            e.printStackTrace();
        }

	}

}
