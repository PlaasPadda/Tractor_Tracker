package tractor_tracker.app;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import com.google.gson.Gson;

public class FarmAgent extends Agent {

    private String farmID;

    @Override
    protected void setup() {
    	//startup
        farmID = getLocalName(); // e.g. "Farm0"

        System.out.println("[" + farmID + "] Started.");

        // Register met die DF sodat Farm Manager dit kan kry 
        registerWithDF();

        // CyclicBehaviour: luister vir INFORM messages van Reader agents
        addBehaviour(new CyclicBehaviour(this) {

            @Override
            public void action() {
            	// Lees net inform messages
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    Gson gson = new Gson();
                    FarmInfo info = gson.fromJson(msg.getContent(), FarmInfo.class);

                    // Debug print message
//                    System.out.println("[" + farmID + "] Received detection from Reader: "
//                            + msg.getSender().getLocalName()
//                            + " | Tractor: " + info.tractorID
//                            + " | Fuel: " + info.fuelLevel
//                            + " | Location: " + info.locationID);

                    // Forward info na die Farm Manager
                    forwardToFarmManager(info);

                } else {
                    // No messages, block until one arrives
                    block();
                }
            }
        });
    }

    // Soek vir Farm Manager agent in die DF en forward tractor info
    private void forwardToFarmManager(FarmInfo info) {
    	// Description waarvoor Farm soek
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("farm-manager");
        template.addServices(sd);

        try {
        	// Soek FarmManager en sit in results
            DFAgentDescription[] results = DFService.search(this, template);
            if (results == null || results.length == 0) {
                System.err.println("[" + farmID + "] No Farm Manager agent found in DF.");
                return;
            }

            // Bou inform met manager as receiver om info te forward
            ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
            inform.addReceiver(results[0].getName());

            Gson gson = new Gson();
            inform.setContent(gson.toJson(info));

            send(inform);

            // Debug sent message
            System.out.println("[" + farmID + "] Forwarded detection to Farm Manager.");

        } catch (FIPAException e) {
            System.err.println("[" + farmID + "] DF search failed: " + e.getMessage());
        }
    }

    // Register met die DF sodat Farm Manager kan sien 
    private void registerWithDF() {
    	// Gee description van homself
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("farm");
        sd.setName(farmID);
        dfd.addServices(sd);

        try {
        // Register met daai description
            DFService.register(this, dfd);
            System.out.println("[" + farmID + "] Registered with DF.");
        } catch (FIPAException e) {
            System.err.println("[" + farmID + "] DF registration failed: " + e.getMessage());
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            System.err.println("[" + farmID + "] DF deregistration failed: " + e.getMessage());
        }
        System.out.println("[" + farmID + "] Shutting down.");
    }
}