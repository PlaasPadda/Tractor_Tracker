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

import java.io.DataOutputStream;
import java.net.Socket;

public class TractorAgent extends Agent {

    private String tractorID;
    private float fuelConsumed = 0.0f;
    private String currentLocationID;

    // Available locations, Obsolete nou
    private static final String[] SIMULATED_LOCATIONS = {
        "farm1p11", "farm1p12", "farm2p11", "farm2p12"
    };

    private final Random random = new Random();

    @Override
    protected void setup() {
        tractorID = getLocalName(); // e.g. "Tractor1"
        currentLocationID = SIMULATED_LOCATIONS[random.nextInt(SIMULATED_LOCATIONS.length)];

        System.out.println("[" + tractorID + "] Started at location: " + currentLocationID);

        // Behaviour 1: Ticker update die fuel consumption oor tyd
        addBehaviour(new TickerBehaviour(this, 10000) {  
            @Override
            protected void onTick() {
            	// Increase oor tyd
                //fuelConsumed += 0.5f + random.nextFloat() * 1.5f;
            	fuelConsumed = getFuelFromErlang();

                // Beweeg tractor
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

        // Build CFP message en Add readers as receivers van CFP deur deur hulle te loop
        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        for (DFAgentDescription result : results) {
            cfp.addReceiver(result.getName());
        }

        // Maak TractorInfo om te stuur aan Reader
        // Serialise CFP content met Gson
        Gson gson = new Gson();
        TractorInfo info = new TractorInfo(tractorID, fuelConsumed, currentLocationID);
        cfp.setContent(gson.toJson(info));
        cfp.setReplyByDate(new java.util.Date(System.currentTimeMillis() + 5000));

        // Begin of Refresh ContractNetInitiator behaviour 
        addBehaviour(new ContractNetInitiator(this, cfp) {

        	@Override
        	protected void handlePropose(ACLMessage propose, Vector acceptances) {
        		// Ons acknowledge net receipt hier
        	}
        	
        	// Ons kyk na alle responses op dieselfde tyd
        	@Override
        	protected void handleAllResponses(Vector responses, Vector acceptances) {
        	    Gson gson = new Gson();

        	    ACLMessage bestProposal = null;
        	    long bestTime = -1;

        	    // Soek proposal met grootste tyd
        	    // Loop deur responses
        	    for (Object obj : responses) {
        	        ACLMessage response = (ACLMessage) obj;

        	        // As proposal, extract data
        	        if (response.getPerformative() == ACLMessage.PROPOSE) {
        	            ReaderProposal proposal = gson.fromJson(response.getContent(), ReaderProposal.class);

        	            System.out.println("[" + tractorID + "] Proposal from "
        	                    + response.getSender().getLocalName()
        	                    + " | Location: " + proposal.locationID
        	                    + " | Detection time: " + proposal.detectionTime);

        	            // Replace if bigger
        	            if (proposal.detectionTime > bestTime) {
        	                bestTime = proposal.detectionTime;
        	                bestProposal = response;
        	            }
        	        }
        	    }

        	    // Bou Accepts en Rejects
        	    // Loop deur responses, as dit beste proposal, accept
        	    for (Object obj : responses) {
        	        ACLMessage response = (ACLMessage) obj;

        	        if (response.getPerformative() != ACLMessage.PROPOSE) {
        	            continue; // skip REFUSE or other non-propose messages
        	        }

        	        ACLMessage reply = response.createReply();

        	        // As dit die proposal is wat die beste is, accept
        	        if ((response == bestProposal) && (bestTime > 0)) {
        	            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
        	            System.out.println("[" + tractorID + "] Accepted: "
        	                    + response.getSender().getLocalName()
        	                    + " (detection time: " + bestTime + ")");
        	        } else {
        	            reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
        	        }

        	        acceptances.add(reply);
        	    }

        	    if (bestTime <= 0) {
        	        System.out.println("[" + tractorID + "] No reader has detected this tractor yet — all proposals rejected.");
        	    }
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
    
 // Query Erlang vir current fuel consumption
    private float getFuelFromErlang() {
    	// Kry port number
        int tractorNumber = extractTractorNumber();
        int port = 9000 + tractorNumber;

        try {
        	// Connect aan socket
            Socket socket = new Socket("localhost", port);

            // Stuur "request" na port
            DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
            byte[] outBytes = "request".getBytes("UTF-8");
            outToServer.write(outBytes);

            // Lees reply 
            byte[] inBytes = new byte[500];
            int numOfBytes = socket.getInputStream().read(inBytes);
            String dataReceived = new String(inBytes, 0, numOfBytes, "UTF-8");

            socket.close();

            // Debug reply
            System.out.println("[" + tractorID + "] Fuel reading from Erlang (port " + port + "): " + dataReceived);

            return Float.parseFloat(dataReceived.trim());

        } catch (Exception e) {
            System.err.println("[" + tractorID + "] Failed to get fuel reading from Erlang: " + e.getMessage());
            return fuelConsumed; // Gebruik vorige value
        }
    }

    private int extractTractorNumber() {
    	// Replace die dele van die string wat nie [0-9] is met niks (remove characters, hou integers)
        String numberPart = tractorID.replaceAll("[^0-9]", "");
        if (numberPart.isEmpty()) {
            System.err.println("[" + tractorID + "] Could not extract tractor number from name.");
            return 1; // fallback default
        }
        return Integer.parseInt(numberPart);
    }

    @Override
    protected void takeDown() {
        System.out.println("[" + tractorID + "] Shutting down.");
    }
}
