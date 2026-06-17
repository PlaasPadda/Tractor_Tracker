package tractor_tracker.app;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SSContractNetResponder;
import jade.proto.SSResponderDispatcher;

import com.google.gson.Gson;

import java.io.DataOutputStream;
import java.net.Socket;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReaderAgent extends Agent {

    private String readerID;
    private String farmID;
    private String locationID;

    // One shared lock per farm port, so only one reader at a time can query
    // a given farm's position sensor. Shared across all ReaderAgent instances.
    private static final Lock farm1Lock = new ReentrantLock();
    private static final Lock farm2Lock = new ReentrantLock();
    private static final Lock farm3Lock = new ReentrantLock();
    private static final Lock farm4Lock = new ReentrantLock();

    private Lock getLockForFarm(int farmNumber) {
        switch (farmNumber) {
            case 1: return farm1Lock;
            case 2: return farm2Lock;
            case 3: return farm3Lock;
            case 4: return farm4Lock;
            default:
                System.err.println("[" + readerID + "] Unknown farm number: " + farmNumber);
                return farm1Lock;
        }
    }

    @Override
    protected void setup() {
        readerID = getLocalName();

        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            farmID = (String) args[0];
            locationID = (String) args[1];
        } else {
            System.err.println("[" + readerID + "] Missing arguments: farmID and locationID required.");
            doDelete();
            return;
        }

        System.out.println("[" + readerID + "] Started | Farm: " + farmID + " | Location: " + locationID);

        registerWithDF();

        // SSResponderDispatcher: permanently listens for CFPs, and for EACH one
        // spins up a brand new SSContractNetResponder to handle that single interaction.
        // This is the correct JADE pattern for a responder that must handle
        // repeated, independent CNP interactions over time.
        MessageTemplate cfpTemplate = MessageTemplate.MatchPerformative(ACLMessage.CFP);

        addBehaviour(new SSResponderDispatcher(this, cfpTemplate) {

            @Override
            protected Behaviour createResponder(ACLMessage cfp) {
                System.out.println("[" + readerID + "] New CFP dispatched, creating responder.");

                return new SSContractNetResponder(myAgent, cfp) {

                    @Override
                    protected ACLMessage handleCfp(ACLMessage cfp)
                            throws RefuseException, FailureException, NotUnderstoodException {

                        Gson gson = new Gson();
                        TractorInfo info = gson.fromJson(cfp.getContent(), TractorInfo.class);

                        System.out.println("[" + readerID + "] Received CFP from: "
                                + cfp.getSender().getLocalName()
                                + " | Tractor: " + info.tractorID);

                        long detectionTime = queryErlangForDetection(info.tractorID);

                        System.out.println("[" + readerID + "] Detection time for " + info.tractorID
                                + " at " + locationID + ": " + detectionTime);

                        ACLMessage propose = cfp.createReply();
                        propose.setPerformative(ACLMessage.PROPOSE);

                        ReaderProposal proposal = new ReaderProposal(locationID, detectionTime);
                        propose.setContent(gson.toJson(proposal));

                        return propose;
                    }

                    @Override
                    protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept)
                            throws FailureException {

                        Gson gson = new Gson();
                        TractorInfo info = gson.fromJson(cfp.getContent(), TractorInfo.class);

                        System.out.println("[" + readerID + "] DETECTED tractor: " + info.tractorID
                                + " | Fuel: " + info.fuelLevel
                                + " | Location: " + locationID);

                        forwardToFarm(info);

                        ACLMessage inform = accept.createReply();
                        inform.setPerformative(ACLMessage.INFORM);

                        DetectionConfirmation confirmation = new DetectionConfirmation(info.tractorID, locationID, "SUCCESS");
                        inform.setContent(gson.toJson(confirmation));

                        return inform;
                    }
                };
            }
        });
    }

    // Queries the Erlang simulation's position sensor for this reader's last detection.
    private long queryErlangForDetection(String tractorID) {
        int farmNumber = Integer.parseInt(farmID.replaceAll("[^0-9]", ""));
        int port = 9100 + farmNumber;

        Lock lock = getLockForFarm(farmNumber);
        lock.lock();

        try {
            Socket socket = new Socket("localhost", port);

            DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
            byte[] outBytes = locationID.getBytes("UTF-8");
            outToServer.write(outBytes);

            byte[] inBytes = new byte[500];
            int numOfBytes = socket.getInputStream().read(inBytes);
            String dataReceived = new String(inBytes, 0, numOfBytes, "UTF-8");

            socket.close();

            return parseDetectionTime(dataReceived, tractorID);

        } catch (Exception e) {
            System.err.println("[" + readerID + "] Failed to query Erlang: " + e.getMessage());
            return 0;
        } finally {
            lock.unlock();
        }
    }

    private long parseDetectionTime(String entry, String tractorID) {
        String[] parts = entry.split("_");

        if (parts.length < 4) {
            System.err.println("[" + readerID + "] Unexpected Erlang entry format: " + entry);
            return 0;
        }

        String lastTractor = parts[2];
        String timeString = parts[3];

        if (lastTractor.equalsIgnoreCase("none")) {
            return 0;
        }

        if (!lastTractor.equalsIgnoreCase(tractorID)) {
            return 0;
        }

        try {
            return Long.parseLong(timeString);
        } catch (NumberFormatException e) {
            System.err.println("[" + readerID + "] Could not parse time from entry: " + entry);
            return 0;
        }
    }

    private void forwardToFarm(TractorInfo info) {
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

            ACLMessage informFarm = new ACLMessage(ACLMessage.INFORM);
            informFarm.addReceiver(results[0].getName());

            Gson gson = new Gson();
            FarmInfo farmInfo = new FarmInfo(info.tractorID, info.fuelLevel, locationID, farmID);
            informFarm.setContent(gson.toJson(farmInfo));

            send(informFarm);

        } catch (FIPAException e) {
            System.err.println("[" + readerID + "] DF search failed: " + e.getMessage());
        }
    }

    private void registerWithDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("reader");
        sd.setName(readerID);
        sd.addProperties(new jade.domain.FIPAAgentManagement.Property("farmID", farmID));
        sd.addProperties(new jade.domain.FIPAAgentManagement.Property("locationID", locationID));
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            System.out.println("[" + readerID + "] Registered with DF | Farm: " + farmID + " | Location: " + locationID);
        } catch (FIPAException e) {
            System.err.println("[" + readerID + "] DF registration failed: " + e.getMessage());
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            System.err.println("[" + readerID + "] DF deregistration failed: " + e.getMessage());
        }
        System.out.println("[" + readerID + "] Shutting down.");
    }
}