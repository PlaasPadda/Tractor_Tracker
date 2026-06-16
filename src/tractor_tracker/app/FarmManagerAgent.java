package tractor_tracker.app;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class FarmManagerAgent extends Agent {

    private String farmManagerID;
    //lys van farms wat available is
    private List<AID> activeFarms = new ArrayList<>();

    @Override
    protected void setup() {
        farmManagerID = getLocalName(); // e.g. "FarmManager"

        // Debug startup
        System.out.println("[" + farmManagerID + "] Started.");

        // Register met die DF sodat Farm agents kan sien 
        registerWithDF();

        // Behaviour 1: CyclicBehaviour om tractor info te kry van farms en forward na server
        addBehaviour(new CyclicBehaviour(this) {

            @Override
            public void action() {
            	// Aanvaar net informs
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    Gson gson = new Gson();
                    FarmInfo info = gson.fromJson(msg.getContent(), FarmInfo.class);

                    // Debug receive
                    System.out.println("[" + farmManagerID + "] Received detection from Farm: "
                            + msg.getSender().getLocalName()
                            + " | Tractor: " + info.tractorID
                            + " | Fuel: " + info.fuelLevel
                            + " | Location: " + info.locationID
                            + " | Farm: " + info.farmID);

                    // Forward na Server agent
                    forwardToServer(info);

                } else {
                    block();
                    // Sodat nie infinite loop
                }
            }
        });

        // Behaviour 2: TickerBehaviour om farm list te refresh 
        addBehaviour(new TickerBehaviour(this, 10000) { // refreshes every 10 seconds

            @Override
            protected void onTick() {
                refreshFarmList();
            }
        });
    }

    // Soek vir Server agent vanaf DF en forward tractor info
    private void forwardToServer(FarmInfo info) {
    	// Set description waarvoor jy soek
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("server");
        template.addServices(sd);

        try {
        	// Soek in DF vir daai description
            DFAgentDescription[] results = DFService.search(this, template);
            if (results == null || results.length == 0) {
                System.err.println("[" + farmManagerID + "] No Server agent found in DF.");
                return;
            }

            // Bou inform message en add server as receiver
            ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
            inform.addReceiver(results[0].getName());

            Gson gson = new Gson();
            inform.setContent(gson.toJson(info));

            send(inform);

            // DEbug send
            System.out.println("[" + farmManagerID + "] Forwarded detection to Server.");

        } catch (FIPAException e) {
            System.err.println("[" + farmManagerID + "] DF search failed: " + e.getMessage());
        }
    }

    // Refresh die Farm agent list 
    private void refreshFarmList() {
    	// Set description waarvoor jy soek
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("farm");
        template.addServices(sd);

        try {
        	// SOek vir farms
            DFAgentDescription[] results = DFService.search(this, template);

            // Clear current farm list en add die farms in result buffer met loop
            activeFarms.clear();
            if (results != null) {
                for (DFAgentDescription result : results) {
                    activeFarms.add(result.getName());
                }
            }

            // Debug Refresh
            System.out.println("[" + farmManagerID + "] Active farms refreshed: " + activeFarms.size() + " farm(s) found.");

        } catch (FIPAException e) {
            System.err.println("[" + farmManagerID + "] Failed to refresh farm list: " + e.getMessage());
        }
    }

    // Register die Farm Manager met DF 
    private void registerWithDF() {
    	// Set description
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("farm-manager");
        sd.setName(farmManagerID);
        dfd.addServices(sd);

        try {
        	// Register met daai descriptionn
            DFService.register(this, dfd);
            System.out.println("[" + farmManagerID + "] Registered with DF.");
        } catch (FIPAException e) {
            System.err.println("[" + farmManagerID + "] DF registration failed: " + e.getMessage());
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            System.err.println("[" + farmManagerID + "] DF deregistration failed: " + e.getMessage());
        }
        System.out.println("[" + farmManagerID + "] Shutting down.");
    }
}
