package tractor_tracker.app;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import com.google.gson.Gson;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class ServerAgent extends Agent {

    private String serverID;
    private static final String EVENTS_FILE = "events.txt";
    private static final String CURRSTATE_FILE = "currstate.txt";

    private final Gson gson = new Gson();

    @Override
    protected void setup() {
        serverID = getLocalName(); // e.g. "Server"

        // Debug setup
        System.out.println("[" + serverID + "] Started.");

        // Register met DF sodat Farm Manager en Dashboard kan sien
        registerWithDF();

        // Initialise die files as hulle nog nie bestaan nie
        initialiseFiles();

        // Behaviour 1: CyclicBehaviour kry detection events vanaf FarmManager en skryf hulle na events en currstate
        addBehaviour(new CyclicBehaviour(this) {

            @Override
            public void action() {
            	// Kry net informs
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    FarmInfo info = gson.fromJson(msg.getContent(), FarmInfo.class);

                    // Debug receive
                    System.out.println("[" + serverID + "] Detection event received | Tractor: "
                            + info.tractorID
                            + " | Fuel: " + info.fuelLevel
                            + " | Location: " + info.locationID
                            + " | Farm: " + info.farmID);

                    // Skryf na altwee files
                    appendToEventsFile(info);
                    updateCurrentStateFile(info);

                } else {
                    block();
                    // blok sodat nie infinite loop
                }
            }
        });

        // Behaviour 2: CyclicBehaviour handle die QUERY-REF messages van Dashboard
        addBehaviour(new CyclicBehaviour(this) {

            @Override
            public void action() {
            	// Vat net queries
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    TractorQuery query = gson.fromJson(msg.getContent(), TractorQuery.class);

                    // Debug query receive
//                    System.out.println("[" + serverID + "] Query received for tractorID: "
//                            + query.tractorID);

                    // Lees die current state file en vind requested tractor
                    FarmInfo currentState = readCurrentState(query.tractorID);

                    // Bou INFORM_REF en Reply na Dashboard 
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM_REF);

                    if (currentState != null) {
                        reply.setContent(gson.toJson(currentState));
                    } else {
                        // Tractor not found 
                        reply.setContent("{\"error\": \"Tractor not found: " + query.tractorID + "\"}");
                    }

                    send(reply);

                } else {
                    block();
                    // blok sodat nie infinite
                }
            }
        });
    }

    // Append die detection event na events.txt 
    private void appendToEventsFile(FarmInfo info) {
    	// Connect aan file
        try (FileWriter fw = new FileWriter(EVENTS_FILE, true)) {
            fw.write(gson.toJson(info) + System.lineSeparator());
            System.out.println("[" + serverID + "] Event appended to " + EVENTS_FILE);
        } catch (IOException e) {
            System.err.println("[" + serverID + "] Failed to write to " + EVENTS_FILE + ": " + e.getMessage());
        }
    }

    // Update currstate.txt met most recent state van tractor
	private void updateCurrentStateFile(FarmInfo info) {
		// Lees alle current states na n list gemaak van Farminfo types 
		// readAllStates maak die file oop!
		List<FarmInfo> stateList = readAllCurrentStates();

		// Soek vir die entry van hierdie tractor, en set daai index na die nuwe state
		boolean found = false;
		for (int i = 0; i < stateList.size(); i++) {
			if (stateList.get(i).tractorID.equals(info.tractorID)) {
				stateList.set(i, info); // replace with the latest info
				found = true;
				break;
			}
		}

		// As hy nog nie daar is nie, add hom
		if (!found) {
			stateList.add(info);
		}

		// Herskryf hele file met nuwe list
		try (FileWriter fw = new FileWriter(CURRSTATE_FILE, false)) {
			// Java om oor array te iterate met for loop
			for (FarmInfo state : stateList) {
				fw.write(gson.toJson(state) + System.lineSeparator());
			}
			System.out.println("[" + serverID + "] Current state updated in " + CURRSTATE_FILE);
		} catch (IOException e) {
			System.err.println("[" + serverID + "] Failed to write to " + CURRSTATE_FILE + ": " + e.getMessage());
		}
	}

	// Lees currstate.txt en return current state van n tractorID
	// readAllStates maak die file oop!
	private FarmInfo readCurrentState(String tractorID) {
		List<FarmInfo> stateList = readAllCurrentStates();

		// Loop through and find the matching tractor
		for (FarmInfo state : stateList) {
			if (state.tractorID.equals(tractorID)) {
				return state;
			}
		}

		return null; // As niks gevind
	}

	// Reads all current states from currstate.txt into a list
	private List<FarmInfo> readAllCurrentStates() {
		List<FarmInfo> stateList = new ArrayList<>();

		// Iterate oor lines, elke line vat van Json na message class en add na list, return list
		try (java.io.BufferedReader br = new java.io.BufferedReader(new FileReader(CURRSTATE_FILE))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.trim().isEmpty()) {
					FarmInfo state = gson.fromJson(line, FarmInfo.class);
					stateList.add(state);
				}
			}
		} catch (IOException e) {
			System.err.println("[" + serverID + "] Failed to read " + CURRSTATE_FILE + ": " + e.getMessage());
		}

		return stateList;
	}

    // Maak die files as hulle nie bestaan 
    private void initialiseFiles() {
        try {
        	// Connect aan events file
            java.io.File eventsFile = new java.io.File(EVENTS_FILE);
            // As jy n nuwe file kan maak, maak dit
            if (eventsFile.createNewFile()) {
                System.out.println("[" + serverID + "] Created " + EVENTS_FILE);
            } else {
            // As nie, append
                System.out.println("[" + serverID + "] " + EVENTS_FILE + " already exists, appending.");
            }

            // Connect aan currState file
            java.io.File currStateFile = new java.io.File(CURRSTATE_FILE);
            // As jy n nuwe file kan maak, maak dit
            if (currStateFile.createNewFile()) {
                System.out.println("[" + serverID + "] Created " + CURRSTATE_FILE);
            } else {
            	// Anders, append
                System.out.println("[" + serverID + "] " + CURRSTATE_FILE + " already exists, loading.");
            }

        } catch (IOException e) {
            System.err.println("[" + serverID + "] Failed to initialise files: " + e.getMessage());
        }
    }

    // Register Server met DF
    private void registerWithDF() {
    	// Bou description
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("server");
        sd.setName(serverID);
        dfd.addServices(sd);

        try {
        	// Register met daai description
            DFService.register(this, dfd);
            System.out.println("[" + serverID + "] Registered with DF.");
        } catch (FIPAException e) {
            System.err.println("[" + serverID + "] DF registration failed: " + e.getMessage());
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            System.err.println("[" + serverID + "] DF deregistration failed: " + e.getMessage());
        }
        System.out.println("[" + serverID + "] Shutting down.");
    }
}
