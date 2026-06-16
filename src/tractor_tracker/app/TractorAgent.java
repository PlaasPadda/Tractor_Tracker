package tractor_tracker.app;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import com.google.gson.Gson;

public class TractorAgent extends Agent {

    private String tractorID;
    private float fuelConsumed = 0.0f;
    private String currentLocationID;

    // Simulated location pool — tractor randomly "moves" between these
    private static final String[] SIMULATED_LOCATIONS = {
        "farm1p11", "farm1p12", "farm2p11", "farm2p12"
    };

    private final Random random = new Random();

    @Override
    protected void setup() {
        tractorID = getLocalName(); // e.g. "Tractor1"
        currentLocationID = SIMULATED_LOCATIONS[random.nextInt(SIMULATED_LOCATIONS.length)];

        System.out.println("[" + tractorID + "] Started at location: " + currentLocationID);

        // Behaviour 1: Ticker to simulate fuel consumption over time
        addBehaviour(new TickerBehaviour(this, 5000) { // fires every 5 seconds
            @Override
            protected void onTick() {
                // Simulate fuel consumption increasing over time
                fuelConsumed += 0.5f + random.nextFloat() * 1.5f;

                // Simulate tractor moving to a new location
                currentLocationID = SIMULATED_LOCATIONS[random.nextInt(SIMULATED_LOCATIONS.length)];

                System.out.println("[" + tractorID + "] Fuel consumed: " + fuelConsumed
                        + "L | Location: " + currentLocationID);

                // Trigger nuwe CNP detection round na elke tick 
                launchDetectionRound();
            }
        });
    }

    // Soek alle Reader agents van DF en stuur n CFP
    private void launchDetectionRound() {
        // Soek alle registered Reader agents from the DF
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("reader");
        template.addServices(sd);

        DFAgentDescription[] results = null;
        try {
            results = DFService.search(this, template);
        } catch (FIPAException e) {
            System.err.println("[" + tractorID + "] DF search failed: " + e.getMessage());
            return;
        }

        if (results == null || results.length == 0) {
            System.out.println("[" + tractorID + "] No readers found in DF yet.");
            return;
        }

        // Build CFP message en Add readers as receivers van CFP
        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        for (DFAgentDescription result : results) {
            cfp.addReceiver(result.getName());
        }

        // Maak TractorInfo om te stuur aan Reader
        // Serialise CFP content using Gson
        Gson gson = new Gson();
        TractorInfo info = new TractorInfo(tractorID, fuelConsumed, currentLocationID);
        cfp.setContent(gson.toJson(info));
        cfp.setReplyByDate(new java.util.Date(System.currentTimeMillis() + 5000));

        // Begin of Refresh ContractNetInitiator behaviour 
        addBehaviour(new ContractNetInitiator(this, cfp) {

        	@Override
        	protected void handlePropose(ACLMessage propose, Vector acceptances) {
        	    Gson gson = new Gson();
        	    ReaderProposal proposal = gson.fromJson(propose.getContent(), ReaderProposal.class);

        	    // Debug die proposal communication
        	    System.out.println("[" + tractorID + "] Proposal from "
        	            + propose.getSender().getLocalName()
        	            + " at location: " + proposal.locationID);

        	    // Accept of reject proposal
        	    ACLMessage reply = propose.createReply();
        	    if (currentLocationID.equals(proposal.locationID)) {
        	        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
        	        System.out.println("[" + tractorID + "] Accepted: "
        	                + propose.getSender().getLocalName());
        	    } else {
        	        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
        	    }
        	    // Add na batch reply list
        	    acceptances.add(reply);
        	}

        	// Receive die inform vanaf reader
            @Override
            protected void handleInform(ACLMessage inform) {
                System.out.println("[" + tractorID + "] Detection confirmed by: "
                        + inform.getSender().getLocalName()
                        + " | " + inform.getContent());
            }

            @Override
            protected void handleRefuse(ACLMessage refuse) {
                System.out.println("[" + tractorID + "] Refused by: "
                        + refuse.getSender().getLocalName());
            }

            @Override
            protected void handleFailure(ACLMessage failure) {
                System.out.println("[" + tractorID + "] Failure from: "
                        + failure.getSender().getLocalName());
            }
        });
    }

    @Override
    protected void takeDown() {
        System.out.println("[" + tractorID + "] Shutting down.");
    }
}
