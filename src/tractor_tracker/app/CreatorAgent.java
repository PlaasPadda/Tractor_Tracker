package tractor_tracker.app;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import com.google.gson.Gson;

public class CreatorAgent extends Agent {

    private String creatorID;
    private final Gson gson = new Gson();

    // Agent begin nommers
    private int tractorCount = 3;
    private int farmCount = 4;

    @Override
    protected void setup() {
        creatorID = getLocalName();
        System.out.println("[" + creatorID + "] Started.");

        // Register met DF 
        registerWithDF();

        // Handle REQUEST messages van Dashboard
        MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchConversationId("agent-creation")
        );


        addBehaviour(new AchieveREResponder(this, mt) {

            @Override
            protected ACLMessage handleRequest(ACLMessage request) {
                AssetCreationRequest creationRequest = gson.fromJson(
                        request.getContent(), AssetCreationRequest.class);

                // Debug request
                System.out.println("[" + creatorID + "] Received REQUEST | Type: "
                        + creationRequest.assetType + " | ID: " + creationRequest.assetID);

                // stuur AGREE terug na Dashboard 
                ACLMessage agree = request.createReply();
                agree.setPerformative(ACLMessage.AGREE);
                return agree;
            }

            @Override
            protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) {
                AssetCreationRequest creationRequest = gson.fromJson(
                        request.getContent(), AssetCreationRequest.class);

                boolean success = false;

                // Spawn agent gebasseer op assetType
                if (creationRequest.assetType.equals("tractor")) {
                    success = spawnTractorAgent(creationRequest.assetID);
                } else if (creationRequest.assetType.equals("farm")) {
                    success = spawnFarmAgent(creationRequest.assetID);
                } else {
                    System.err.println("[" + creatorID + "] Unknown asset type: "
                            + creationRequest.assetType);
                }

                // stuur INFORM terug na Dashboard 
                ACLMessage inform = request.createReply();
                inform.setPerformative(ACLMessage.INFORM);
                inform.setConversationId("agent-creation");

                String status;
                if (success) {
                    status = "SUCCESS";
                } else {
                    status = "FAILURE";
                }

                // Bou inform
                AssetCreationResponse responseObj = new AssetCreationResponse(
                        creationRequest.assetType,
                        creationRequest.assetID,
                        status
                );

                inform.setContent(gson.toJson(responseObj));

                return inform;
            }
        });
    }

    // Spawn nuwe TractorAgent 
    private boolean spawnTractorAgent(String tractorID) {
        try {
            AgentController ac = getContainerController().createNewAgent(
                    tractorID,
                    "tractor_tracker.app.TractorAgent",
                    null
            );
            ac.start();
            System.out.println("[" + creatorID + "] Spawned TractorAgent: " + tractorID);
            return true;
        } catch (StaleProxyException e) {
            System.err.println("[" + creatorID + "] Failed to spawn TractorAgent: " + e.getMessage());
            return false;
        }
    }

    // Spawn nuwe FarmAgent met sy Readers
    private boolean spawnFarmAgent(String farmID) {
        try {
            // Spawn Farm agent
            AgentController farmAC = getContainerController().createNewAgent(
                    farmID,
                    "tractor_tracker.app.FarmAgent",
                    null
            );
            farmAC.start();
            System.out.println("[" + creatorID + "] Spawned FarmAgent: " + farmID);

            // Spawn sy Reader agents 
            String reader1ID = farmID + "_11";
            String reader2ID = farmID + "_12";
            String reader3ID = farmID + "_13";
            String reader4ID = farmID + "_21";
            String reader5ID = farmID + "_22";
            String reader6ID = farmID + "_23";

            int farmNumber = farmCount++;
            Object[] reader1Args = new Object[]{farmID, "farm"+ (farmNumber) + "_p11"};
            Object[] reader2Args = new Object[]{farmID, "farm"+ (farmNumber) + "_p12"};
            Object[] reader3Args = new Object[]{farmID, "farm"+ (farmNumber) + "_p13"};
            Object[] reader4Args = new Object[]{farmID, "farm"+ (farmNumber) + "_p21"};
            Object[] reader5Args = new Object[]{farmID, "farm"+ (farmNumber) + "_p22"};
            Object[] reader6Args = new Object[]{farmID, "farm"+ (farmNumber) + "_p23"};

            AgentController reader1AC = getContainerController().createNewAgent(
                    reader1ID,
                    "tractor_tracker.app.ReaderAgent",
                    reader1Args
            );
            reader1AC.start();
            System.out.println("[" + creatorID + "] Spawned ReaderAgent: " + reader1ID);

            AgentController reader2AC = getContainerController().createNewAgent(
                    reader2ID,
                    "tractor_tracker.app.ReaderAgent",
                    reader2Args
            );
            reader2AC.start();
            System.out.println("[" + creatorID + "] Spawned ReaderAgent: " + reader2ID);

            AgentController reader3AC = getContainerController().createNewAgent(
                    reader3ID,
                    "tractor_tracker.app.ReaderAgent",
                    reader3Args
            );
            reader3AC.start();
            System.out.println("[" + creatorID + "] Spawned ReaderAgent: " + reader3ID);

            AgentController reader4AC = getContainerController().createNewAgent(
                    reader4ID,
                    "tractor_tracker.app.ReaderAgent",
                    reader4Args
            );
            reader4AC.start();
            System.out.println("[" + creatorID + "] Spawned ReaderAgent: " + reader4ID);
            
            
            AgentController reader5AC = getContainerController().createNewAgent(
                    reader5ID,
                    "tractor_tracker.app.ReaderAgent",
                    reader5Args
            );
            reader5AC.start();
            System.out.println("[" + creatorID + "] Spawned ReaderAgent: " + reader5ID);
            
            
            AgentController reader6AC = getContainerController().createNewAgent(
                    reader6ID,
                    "tractor_tracker.app.ReaderAgent",
                    reader6Args
            );
            reader6AC.start();
            System.out.println("[" + creatorID + "] Spawned ReaderAgent: " + reader6ID);
            
            return true;
        } catch (StaleProxyException e) {
            System.err.println("[" + creatorID + "] Failed to spawn FarmAgent: " + e.getMessage());
            return false;
        }
    }

    // Register Creator met DF 
    private void registerWithDF() {
    	// Bou description
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("creator");
        sd.setName(creatorID);
        dfd.addServices(sd);

        try {
        	// Register met daai description
            DFService.register(this, dfd);
            System.out.println("[" + creatorID + "] Registered with DF.");
        } catch (FIPAException e) {
            System.err.println("[" + creatorID + "] DF registration failed: " + e.getMessage());
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            System.err.println("[" + creatorID + "] DF deregistration failed: " + e.getMessage());
        }
        System.out.println("[" + creatorID + "] Shutting down.");
    }
}
