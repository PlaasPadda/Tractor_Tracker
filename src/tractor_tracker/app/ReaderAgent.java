package tractor_tracker.app;

import jade.core.Agent;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetResponder;

import com.google.gson.Gson;

public class ReaderAgent extends Agent {

    private String readerID;
    private String farmID;
    private String locationID;

    @Override
    protected void setup() {
        readerID = getLocalName();         // e.g. "Reader1"

        // Lees sy farmID and locationID deurgesit as arguments vanaf Main
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            farmID = (String) args[0];
            locationID = (String) args[1];
        } else {
            System.err.println("[" + readerID + "] Missing arguments: farmID and locationID required.");
            doDelete();
            return;
        }

        // Startup message
        System.out.println("[" + readerID + "] Started | Farm: " + farmID + " | Location: " + locationID);

        // Register met die DF sodat Tractor agents kan discover 
        registerWithDF();

        // Add die ContractNetResponder om Tractor CFPs handle  
        addBehaviour(new ContractNetResponder(this, null) {

            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) {
                Gson gson = new Gson();
                TractorInfo info = gson.fromJson(cfp.getContent(), TractorInfo.class);

                // Debug receive 
                System.out.println("[" + readerID + "] Received CFP from: "
                        + cfp.getSender().getLocalName()
                        + " | Tractor location: " + info.locationID
                        + " | My location: " + locationID);

                // Bou en stuur locationID proposal
                ACLMessage propose = cfp.createReply();
                propose.setPerformative(ACLMessage.PROPOSE);

                ReaderProposal proposal = new ReaderProposal(locationID);
                propose.setContent(gson.toJson(proposal));

                return propose;
            }

            // Stuur Inform as Proposal accepted is
            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
                Gson gson = new Gson();
                TractorInfo info = gson.fromJson(cfp.getContent(), TractorInfo.class);

                // debug successful detection
                System.out.println("[" + readerID + "] DETECTED tractor: " + info.tractorID
                        + " | Fuel: " + info.fuelLevel
                        + " | Location: " + locationID);

                // Forward detection na die Farm agent
                forwardToFarm(info);

                // Bou confirmation message en Inform die Tractor dat detection deurgesit is 
                ACLMessage inform = accept.createReply();
                inform.setPerformative(ACLMessage.INFORM);

                DetectionConfirmation confirmation = new DetectionConfirmation(info.tractorID, locationID, "SUCCESS");
                inform.setContent(gson.toJson(confirmation));

                return inform;
            }

            // Print reject as rejected
            @Override
            protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
                System.out.println("[" + readerID + "] Proposal rejected by: "
                        + reject.getSender().getLocalName());
            }
        });
    }

    // Soek vir sy Farm Agent en stuur die tractor info
    private void forwardToFarm(TractorInfo info) {
        // Soek vir Farm agent vir hierdie reader se farmID in die DF
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("farm");
        sd.setName(farmID);
        template.addServices(sd);

        try {
            DFAgentDescription[] results = DFService.search(this, template);
            if (results == null || results.length == 0) {
                System.err.println("[" + readerID + "] No Farm agent found for farmID: " + farmID);
                return;
            }

            // Bou farminfo message om na Farm agent te stuur
            ACLMessage informFarm = new ACLMessage(ACLMessage.INFORM);
            informFarm.addReceiver(results[0].getName());

            Gson gson = new Gson();
            FarmInfo farmInfo = new FarmInfo(info.tractorID, info.fuelLevel, info.locationID, farmID);
            informFarm.setContent(gson.toJson(farmInfo));

            send(informFarm);

            // Debug vir successful send
            System.out.println("[" + readerID + "] Forwarded detection to Farm: " + farmID);

        } catch (FIPAException e) {
            System.err.println("[" + readerID + "] DF search failed: " + e.getMessage());
        }
    }

    // Register met die DF met service type "reader"
    private void registerWithDF() {
    	// Bou agent description
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("reader");
        sd.setName(readerID);
        sd.addProperties(new jade.domain.FIPAAgentManagement.Property("farmID", farmID));
        sd.addProperties(new jade.domain.FIPAAgentManagement.Property("locationID", locationID));
        dfd.addServices(sd);

        // Register met DF
        try {
            DFService.register(this, dfd);
            System.out.println("[" + readerID + "] Registered with DF | Farm: " + farmID + " | Location: " + locationID);
        } catch (FIPAException e) {
            System.err.println("[" + readerID + "] DF registration failed: " + e.getMessage());
        }
    }

    @Override
    protected void takeDown() {
        // Deregister vanaf DF on shutdown
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            System.err.println("[" + readerID + "] DF deregistration failed: " + e.getMessage());
        }
        System.out.println("[" + readerID + "] Shutting down.");
    }
}