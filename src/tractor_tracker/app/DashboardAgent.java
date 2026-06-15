package tractor_tracker.app;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;

import com.google.gson.Gson;

import javax.swing.*;
import java.awt.*;

public class DashboardAgent extends Agent {

    private String dashboardID;
    private final Gson gson = new Gson();

    // GUI components
    private JFrame frame;
    private JTextField tractorTextField;
    private JLabel infoLabel;

    // Nuwe Agents begin nommer
    private int newTractorCount = 1;
    private int newFarmCount = 1;

    @Override
    protected void setup() {
        dashboardID = getLocalName();
        System.out.println("[" + dashboardID + "] Started.");

        // Build the GUI op thread
        SwingUtilities.invokeLater(this::buildGUI);

        // CyclicBehaviour: Wag vir INFORM_REF van die server Server
        addBehaviour(new CyclicBehaviour(this) {

            @Override
            public void action() {
            	// Soek net vir inform messages
            	MessageTemplate mt = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF),
                        MessageTemplate.MatchConversationId("tractor-query")
                );
            	ACLMessage msg = receive(mt);

                if (msg != null) {
                    final String content = msg.getContent();
                    // Debug receive
                    System.out.println("[" + dashboardID + "] Received info from Server: " + content);

                    // As error
                    if (content.contains("error")) {
                        SwingUtilities.invokeLater(() ->
                                infoLabel.setText("Tractor not found."));
                    // As bestaan, set label as info
                    } else {
                        FarmInfo info = gson.fromJson(content, FarmInfo.class);
                        SwingUtilities.invokeLater(() ->
                                infoLabel.setText("<html>"
                                        + "Tractor ID: " + info.tractorID + "<br>"
                                        + "Fuel Level: " + String.format("%.2f", info.fuelLevel) + "L<br>"
                                        + "Location: " + info.locationID + "<br>"
                                        + "Farm: " + info.farmID
                                        + "</html>"));
                    }
                } else {
                    block();
                }
            }
        });

        // CyclicBehaviour: Soek vir INFORM vanaf Creator 
        addBehaviour(new CyclicBehaviour(this) {

            @Override
            public void action() {
            	// Soek net inform
            	MessageTemplate mt = MessageTemplate.and(
            	        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            	        MessageTemplate.MatchConversationId("agent-creation")
            	);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    AssetCreationResponse response = gson.fromJson(msg.getContent(), AssetCreationResponse.class);
                    // DEbug receive
                    System.out.println("[" + dashboardID + "] Creator response | Type: "
                            + response.assetType + " | ID: " + response.assetID
                            + " | Status: " + response.status);

                    SwingUtilities.invokeLater(() -> {
                        // As tractor successfully added, wys message
                        if (response.assetType.equals("tractor") && response.status.equals("SUCCESS")) {
                            JOptionPane.showMessageDialog(frame,
                                    response.assetType + " '" + response.assetID + "' created successfully.",
                                    "Agent Created",
                                    JOptionPane.INFORMATION_MESSAGE);
                        // As farm successgully added, wys message
                        } else if (response.assetType.equals("farm") && response.status.equals("SUCCESS")) {
                            JOptionPane.showMessageDialog(frame,
                                    "Farm '" + response.assetID + "' created successfully.",
                                    "Agent Created",
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                    });
                } else {
                    block();
                }
            }
        });
    }

    // Bou GUI
    private void buildGUI() {
        frame = new JFrame("Tractor Monitoring Dashboard");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(450, 350);
        frame.setLayout(new BorderLayout(10, 10));

        // --- Top panel ---
        JPanel creationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        creationPanel.setBorder(BorderFactory.createTitledBorder("Create Agents"));

        JButton spawnTractorBtn = new JButton("Spawn Tractor");
        JButton spawnFarmBtn = new JButton("Spawn Farm");

        spawnTractorBtn.addActionListener(e -> sendCreationRequest("tractor"));
        spawnFarmBtn.addActionListener(e -> sendCreationRequest("farm"));

        creationPanel.add(spawnTractorBtn);
        creationPanel.add(spawnFarmBtn);

        // --- Middle panel ---
        JPanel queryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        queryPanel.setBorder(BorderFactory.createTitledBorder("Query Tractor Info"));

        tractorTextField = new JTextField();
        tractorTextField.setPreferredSize(new Dimension(150, 25));
        tractorTextField.setToolTipText("Enter tractor name e.g. Tractor1");

        JButton queryBtn = new JButton("Get Info");
        queryBtn.addActionListener(e -> sendQueryRequest());

        queryPanel.add(new JLabel("Tractor:"));
        queryPanel.add(tractorTextField);
        queryPanel.add(queryBtn);

        // --- Bottom panel ---
        JPanel infoPanel = new JPanel(new BorderLayout(5, 5));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Tractor Information"));

        infoLabel = new JLabel("<html>No information yet. Select a tractor and click Get Info.</html>");
        infoLabel.setHorizontalAlignment(SwingConstants.LEFT);
        infoLabel.setVerticalAlignment(SwingConstants.TOP);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        infoPanel.add(infoLabel, BorderLayout.CENTER);

        // --- Add panels ---
        JPanel topSection = new JPanel(new GridLayout(2, 1));
        topSection.add(creationPanel);
        topSection.add(queryPanel);

        frame.add(topSection, BorderLayout.NORTH);
        frame.add(infoPanel, BorderLayout.CENTER);

        // Shut down wanneer window closed
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                doDelete();
                frame.dispose();
            }
        });

        frame.setVisible(true);
    }

    // Stuur creation request na Creator 
    private void sendCreationRequest(String assetType) {
        // Soek Creator agent in DF, bou desciptoin
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("creator");
        template.addServices(sd);

        try {
        	// Soek vir description in DF
            DFAgentDescription[] results = DFService.search(this, template);
            if (results == null || results.length == 0) {
                System.err.println("[" + dashboardID + "] No Creator agent found in DF.");
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(frame,
                                "Creator agent not found.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE));
                return;
            }

            // Generate naam vir nuwe agent
            String assetID;
            if (assetType.equals("tractor")) {
                assetID = "Tractor" + newTractorCount++;
            } else {
                assetID = "Farm" + newFarmCount++;
            }

            // Bou REQUEST en add Creator as receiver
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.addReceiver(results[0].getName());
            request.setConversationId("agent-creation"); 

            AssetCreationRequest creationRequest = new AssetCreationRequest(assetType, assetID);
            request.setContent(gson.toJson(creationRequest));

            // FIPA Request protocol
            addBehaviour(new AchieveREInitiator(this, request) {

                @Override
                protected void handleAgree(ACLMessage agree) {
                    System.out.println("[" + dashboardID + "] Creator agreed to create: " + assetID);
                }

                @Override
                protected void handleInform(ACLMessage inform) {
                    System.out.println("[" + dashboardID + "] Creator finished creating: " + assetID);
                }

                @Override
                protected void handleRefuse(ACLMessage refuse) {
                    System.out.println("[" + dashboardID + "] Creator refused to create: " + assetID);
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(frame,
                                    "Creator refused to create " + assetID,
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE));
                }

                @Override
                protected void handleFailure(ACLMessage failure) {
                    System.out.println("[" + dashboardID + "] Creator failed to create: " + assetID);
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(frame,
                                    "Creator failed to create " + assetID,
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE));
                }
            });

        } catch (FIPAException e) {
            System.err.println("[" + dashboardID + "] DF search failed: " + e.getMessage());
        }
    }

    // Stuur QUERY-REF aan Server 
    private void sendQueryRequest() {
        String tractorName = tractorTextField.getText().trim();

        if (tractorName.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Please enter a tractor name.",
                    "Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Soek vir Server in DF, bou description
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("server");
        template.addServices(sd);

        try {
        	// Soek vir description
            DFAgentDescription[] results = DFService.search(this, template);
            if (results == null || results.length == 0) {
                System.err.println("[" + dashboardID + "] No Server agent found in DF.");
                return;
            }

            ACLMessage query = new ACLMessage(ACLMessage.QUERY_REF);
            query.addReceiver(results[0].getName());
            
            // Gebruik conversation IDs sodat query en request tussen server en Creator nie deurmekaar raak nie
            query.setConversationId("tractor-query");

            TractorQuery tractorQuery = new TractorQuery(tractorName);
            query.setContent(gson.toJson(tractorQuery));

            send(query);

            System.out.println("[" + dashboardID + "] Sent query for: " + tractorName);
            infoLabel.setText("Fetching info for " + tractorName + "...");

        } catch (FIPAException e) {
            System.err.println("[" + dashboardID + "] DF search failed: " + e.getMessage());
        }
    }

    @Override
    protected void takeDown() {
        if (frame != null) {
            SwingUtilities.invokeLater(() -> frame.dispose());
        }
        System.out.println("[" + dashboardID + "] Shutting down.");
    }
}
