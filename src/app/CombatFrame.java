package app; 

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import javax.swing.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Random;

public class CombatFrame extends JFrame {
    private DefaultListModel<String> initiativeListModel;
    private JList<String> initiativeList;
    private ArrayList<Combatant> combatants;
    private JButton nextTurnButton;
    private int turnIndex = 0; // Tracks whose turn it is
    private int turnCount = 0; 
    private int roundNumber = 1; // Tracks the round count
    private JLabel roundLabel; // Displays round and turn
    
    private JPanel characterPanel; 
    private JPanel controlPanel; 
    private JPanel statsPanel;

    private JLabel nameLabel; 
    private JProgressBar hpBar;
    private JProgressBar overhealhpBar; 
    private JLabel hpLabel; 
    private JLabel overhealhpLabel; 
    private JLabel armourLabel; 
    private JLabel acLabel; 
    private JLabel shieldLabel; 

    private JPanel shieldPanel;
    private JPanel armourPanel;

    private Combatant selectedCombatant = null;
    private boolean isManuallySelected = false;

    public CombatFrame() {
        setTitle("D&D Initiative Tracker");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize Components
        initiativeListModel = new DefaultListModel<>();
        initiativeList = new JList<>(initiativeListModel);
        combatants = new ArrayList<>();

        // Round and Turn Label at the top
        roundLabel = new JLabel("Round: 1 | Turn: 1", SwingConstants.CENTER);
        add(roundLabel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(initiativeList);
        add(scrollPane, BorderLayout.CENTER);

        controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(1, 3));

        characterPanel = new JPanel(); 
        characterPanel.setLayout(new GridLayout(5, 1)); 

        nextTurnButton = new JButton("Next Turn (->)");
        JButton resetButton = new JButton("Reset Combat (R)");
        JButton attackButton = new JButton("Manual Attack (/)");
        JButton addButton = new JButton("Add Combatant (+)");
        JButton removeButton = new JButton("Remove Combatant (-)");

        controlPanel.add(nextTurnButton);
        controlPanel.add(resetButton);
        controlPanel.add(attackButton);
        controlPanel.add(addButton);
        controlPanel.add(removeButton);
        add(controlPanel, BorderLayout.SOUTH);

        JButton createCharBtn = new JButton("Create Character (+)");
        JButton loadCharBtn = new JButton("Load Character (->)");
        JButton editCharBtn = new JButton("Edit Saved Character (<-)");

        characterPanel.add(createCharBtn); 
        characterPanel.add(loadCharBtn); 
        characterPanel.add(editCharBtn);
        add(characterPanel, BorderLayout.WEST); 
 
        statsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        nameLabel = new JLabel("Name:");
        armourLabel = new JLabel("AC: 0/0, Shielding: 0");
        shieldLabel = new JLabel("Shield: ");
        acLabel = new JLabel("AC: ");
        overhealhpLabel = new JLabel("Overheal:");
        hpLabel = new JLabel("HP:");

        armourPanel = new JPanel();
        armourPanel.setLayout(new BoxLayout(armourPanel, BoxLayout.Y_AXIS));

        shieldPanel = new JPanel(); 
        shieldPanel.setLayout(new BoxLayout(shieldPanel, BoxLayout.Y_AXIS));  
        
        overhealhpBar = new JProgressBar(); 
        hpBar = new JProgressBar();

        // Add to main window
        add(statsPanel, BorderLayout.EAST);

        statsPanel.add(nameLabel); 
        statsPanel.add(hpLabel);
        statsPanel.add(hpBar);
        statsPanel.add(overhealhpLabel);
        statsPanel.add(overhealhpBar);
        statsPanel.add(armourLabel);
        statsPanel.add(shieldPanel);
        statsPanel.add(armourPanel);
        add(statsPanel, BorderLayout.EAST); // Add it to the side

        updateStatDisplay(null);

        // Action Listeners
        addButton.addActionListener(e -> addCombatant());
        nextTurnButton.addActionListener(e -> nextTurn());
        removeButton.addActionListener(e -> removeCombatant());
        resetButton.addActionListener(e -> resetCombat());
        attackButton.addActionListener(e -> manuallyAttackCombatant());
        createCharBtn.addActionListener(e -> openCharacterCreator());
        loadCharBtn.addActionListener(e -> openCharacterLoader());
        editCharBtn.addActionListener(e -> openCharacterEditor());

        initiativeList.addListSelectionListener(e -> {
            int index = initiativeList.getSelectedIndex(); 
            if(index >= 0 && index < combatants.size()) { 
                selectedCombatant = combatants.get(index); 
                isManuallySelected = true; 
                updateStatDisplay(selectedCombatant);
            }

            int selectedIndex = initiativeList.getSelectedIndex();
            if (selectedIndex != -1 && selectedIndex < combatants.size()) {
                updateStatDisplay(combatants.get(selectedIndex));
            }
        });

        setVisible(true);
    }

    private void addCombatant() {
        JTextField nameField = new JTextField();
        JTextField initiativeField = new JTextField();
        JTextField hpField = new JTextField(); 
        JTextField mpField = new JTextField(); 
        JTextField apField = new JTextField(); 
        JTextField overhealField = new JTextField(); 
        JTextField armourLayersField = new JTextField();  // e.g. "25,15"
        JTextField shieldLayersField = new JTextField(); // e.g. "10,5"
        
        Object[] message = {
            "Name:", nameField,
            "Initiative:", initiativeField,
            "Hit Points: ", hpField,
            "Mana Points: ", mpField, 
            "Action Points: ", apField,  
            "Overheal: ", overhealField, 
            "Armour Layers (comma-separated max values):", armourLayersField,
            "Shield Layers (comma-separated current values):", shieldLayersField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Add Combatant", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String name = nameField.getText();
            try {
                Random initiativeRoll = new Random(); 
                int initiativeMin = 1; 
                int initiativeMax = 20; 

                int initiative;
                try { 
                    initiative = Integer.parseInt(initiativeField.getText());

                } catch (NumberFormatException e) { //catches empty input and rolls initiative
                    initiative = initiativeRoll.nextInt(initiativeMax - initiativeMin + 1) + initiativeMin; 
                }

                int hitPoints = Integer.parseInt(hpField.getText()); 
                int manaPoints = Integer.parseInt(mpField.getText()); 
                int actionPoints = Integer.parseInt(apField.getText()); 
                 
                Combatant combatant = new Combatant(name, initiative, hitPoints, manaPoints, actionPoints);

                //handle overheal: 
                int overheal; 
                try { 
                    overheal = Integer.parseInt(overhealField.getText()); 

                } catch (NumberFormatException e) {  //catches empty input
                    overheal = 0; 
                }

                combatant.setOverheal(overheal);
                
                // Parse and add armour layers
                //add layers in increasing order (L->R)-> first armour is first layer. 
                String[] armourValues = armourLayersField.getText().split(",");
                List<String> armourValuesList = Arrays.asList(armourValues); 
                Collections.reverse(armourValuesList); //reverse the array. 
        
                for (String value : armourValues) {
                    value = value.trim();
                    if (!value.isEmpty()) {
                        int armourMax = Integer.parseInt(value);
                        combatant.addArmour(armourMax);
                    }
                }

                // Parse and add shield layers
                //add layers in increasing order (L->R) -> first shield is first layer. 
                String[] shieldValues = shieldLayersField.getText().split(",");
                for (String value : shieldValues) {
                    value = value.trim();
                    if (!value.isEmpty()) {
                        int shieldCurrent = Integer.parseInt(value);
                        if (shieldCurrent > 0) {
                            combatant.addShield(shieldCurrent);
                        }
                    }
                }

                combatants.add(combatant);
                updateInitiativeList();

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid input!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeCombatant() {
        int selectedIndex = initiativeList.getSelectedIndex();
        if (selectedIndex != -1) {
            combatants.remove(selectedIndex);
            updateInitiativeList();
        } else {
            JOptionPane.showMessageDialog(this, "Select a combatant to remove!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void manuallyAttackCombatant() {
        if (combatants.size() < 2) {
            JOptionPane.showMessageDialog(this, "Not enough combatants for an attack!", "Error", JOptionPane.ERROR_MESSAGE);
            return;

        } 
    
        Combatant attacker = combatants.get(turnIndex);
        
        if(attacker.getAP().getCurrent() <= 0) { 
            JOptionPane.showMessageDialog(this, "Not enough Action Points!", "Error", JOptionPane.ERROR_MESSAGE);
            return;

        }

        // Select a target (excluding self)
        String[] targetNames = combatants.stream()
                                         .filter(c -> !c.equals(attacker))
                                         .map(Combatant::getName)
                                         .toArray(String[]::new);
    
        String targetName = (String) JOptionPane.showInputDialog(this, "Select a target:", "Attack", JOptionPane.QUESTION_MESSAGE, null, targetNames, targetNames[0]);
    
        if (targetName == null) { 
            return;     
        }
    
        // Find the target in the list
        Combatant target = combatants.stream()
                                     .filter(c -> c.getName().equals(targetName))
                                     .findFirst()
                                     .orElse(null);
    
        if (target == null) { 
            return; 
        }    
    
        // Ask user to enter the damage
        JTextField damageField = new JTextField();
        Object[] message = {"Enter damage amount:", damageField};
        
        int option = JOptionPane.showConfirmDialog(this, message, "Enter Damage", JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) return;
    
        try {
            int damage = Integer.parseInt(damageField.getText());
    
            if (damage < 0) {
                JOptionPane.showMessageDialog(this, "Damage cannot be negative!", "Error", JOptionPane.ERROR_MESSAGE);
                return;

            } 

            attacker.getAP().modifyCurrent(-1);
            target.takeDamage(damage);
            updateStatDisplay(target);  // Update UI bars
    
            // Show attack result
            JOptionPane.showMessageDialog(this, attacker.getName() + " attacks " + target.getName() + "!\nDamage: " + damage + "\n" + target.getName() + " has " + target.getHP().getCurrent() + " HP remaining.", "Combat Log", JOptionPane.INFORMATION_MESSAGE);
    
            // Remove defeated combatants
            if (target.isDefeated()) {
                combatants.remove(target);
                JOptionPane.showMessageDialog(this, target.getName() + " has been defeated!", "Combat Log", JOptionPane.INFORMATION_MESSAGE);
            }
    
            updateInitiativeList();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid damage value!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateStatDisplay(Combatant combatant) {
        statsPanel.removeAll(); 
        if (combatant == null) { 
            statsPanel.revalidate();
            statsPanel.repaint();
            return; //dont load any UI
        }

        selectedCombatant = combatant;

        //dynamically generate this statPanel each time: 
        statsPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Name label (row 0)
        nameLabel.setText("Name: " + selectedCombatant.getName());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        statsPanel.add(nameLabel, gbc);

        // Armour Label (row 1)
        armourLabel.setText("Armour: " + selectedCombatant.getTotalAC() + "/" + selectedCombatant.getTotalMaxAC()+ " | Shielding: " + selectedCombatant.getTotalShield());
        gbc.gridy++;
        statsPanel.add(armourLabel, gbc);

        // Shield bar panel (row 2 left), Shield total label (row 2 right)
        shieldPanel.removeAll();
        Stack<Statistic> shields = selectedCombatant.getShield();
        for (int i = shields.size() - 1; i >= 0; i--) {
            Statistic s = shields.get(i);
            if (s.getCurrent() > 0) {
                JProgressBar bar = new JProgressBar();
                bar.setValue(100);
                bar.setStringPainted(true);
                bar.setString("Shield " + (i + 1) + ": " + s.getCurrent());
                shieldPanel.add(bar);
            }
        }

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        statsPanel.add(shieldPanel, gbc);

        gbc.gridx = 1;
        shieldLabel.setText("Shield: " + selectedCombatant.getTotalShield());
        statsPanel.add(shieldLabel, gbc);

        // Armour bar panel (row 3 left), Armour label (row 3 right)
        armourPanel.removeAll();
        Stack<Statistic> armours = selectedCombatant.getArmour();
        for (int i = armours.size() - 1; i >= 0; i--) {
            Statistic s = armours.get(i);
            JProgressBar bar = new JProgressBar(0, s.getMax());
            bar.setValue(s.getCurrent());
            bar.setStringPainted(true);
            bar.setString("Armour " + (i + 1) + ": " + s.getCurrent() + "/" + s.getMax());
            armourPanel.add(bar);
        }
        
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        statsPanel.add(armourPanel, gbc);
        
        gbc.gridx = 1;
        acLabel.setText("AC: " + selectedCombatant.getTotalAC() + "/" + selectedCombatant.getTotalMaxAC());
        statsPanel.add(acLabel, gbc);
    
        // Overheal (row 4)
        gbc.gridy++;
        gbc.gridx = 0;
        overhealhpBar.setMaximum(selectedCombatant.getOverheal());
        overhealhpBar.setValue(selectedCombatant.getOverheal()); 
        overhealhpBar.setStringPainted(true);
        overhealhpBar.setString("Overheal (+)");
        statsPanel.add(overhealhpBar, gbc);

        gbc.gridx = 1;
        overhealhpLabel.setText("Overheal: " + selectedCombatant.getOverheal());
        statsPanel.add(overhealhpLabel, gbc);

        // HP (row 5)
        gbc.gridy++;
        gbc.gridx = 0;
        hpBar.setMaximum(selectedCombatant.getHP().getMax()); 
        hpBar.setValue(selectedCombatant.getHP().getCurrent());
        hpBar.setStringPainted(true);
        hpBar.setString("HP");
        statsPanel.add(hpBar, gbc);

        gbc.gridx = 1;
        hpLabel.setText("HP: " + selectedCombatant.getHP().getCurrent() + "/" + selectedCombatant.getHP().getMax());
        statsPanel.add(hpLabel, gbc);


        // Final refresh
        statsPanel.revalidate();
        statsPanel.repaint();
    }
    
        
    private void updateInitiativeList() {
        initiativeListModel.clear();
        Collections.sort(combatants, Comparator.comparingInt(Combatant::getInitiative).reversed());
    
        for (int i = 0; i < combatants.size(); i++) {
            Combatant c = combatants.get(i); 
            String marker = (i == turnIndex ? "=> " : " "); 
            String display = marker + c.getName() +
                            " (Init: " + c.getInitiative() + 
                            ", HP: " + c.getHP().getCurrent() + "/" + c.getHP().getMax() + " (overheal HP: +" + c.getOverheal() +
                            ", MP: " + c.getMP().getCurrent() + "/" + c.getMP().getMax() +
                            ", AP: " + c.getAP().getCurrent() + "/" + c.getAP().getMax() +
                            ", AC: " + c.getTotalAC() + "/" + c.getTotalMaxAC() + 
                            ", Shield: " + c.getTotalShield() + ")";

            initiativeListModel.addElement(display);
        }

        if (combatants.isEmpty()) {
            selectedCombatant = null;
            isManuallySelected = false;
            statsPanel.removeAll();
            statsPanel.revalidate();
            statsPanel.repaint();

        } else if (!isManuallySelected) {
            selectedCombatant = combatants.get(turnIndex);
            updateStatDisplay(selectedCombatant);
        }
    }
    
    private void nextTurn() {
        if (combatants.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No combatants in the list!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    
        // Move to the next combatant
        turnIndex = (turnIndex + 1) % combatants.size();
        turnCount++; 
        isManuallySelected = false;
        updateInitiativeList(); // will auto-select current attacker
    
        // if turnIndex = 0 -> new round (because of modulo and circular)
        if (turnIndex == 0) {
            roundNumber++;

            for(Combatant c : combatants) { 
                c.getAP().setCurrent(c.getAP().getMax()); 
            }
        }
    
        // Update round and turn display
        roundLabel.setText("Round: " + roundNumber + " | Turn: " + turnCount);
        updateInitiativeList();

        selectedCombatant = combatants.get(turnIndex); 
        updateStatDisplay(selectedCombatant);
    }
    
    private void resetCombat() {
        combatants.clear();
        turnIndex = 0;
        roundNumber = 1;
        roundLabel.setText("Round: 1 | Turn: 1");
        updateInitiativeList();
    }
    
    private void openCharacterCreator() {        
        JTextField nameField = new JTextField();
        JTextField initiativeField = new JTextField();
        JTextField hpField = new JTextField();
        JTextField mpField=  new JTextField(); 
        JTextField apField = new JTextField(); 
        JTextField overhealField = new JTextField(); 
        JTextField armourLayersField = new JTextField();  // e.g., "30,20"
        JTextField shieldLayersField = new JTextField(); // e.g., "15,10"

        Object[] inputs = {
            "Name: ", nameField,
            "Initiative: ", initiativeField,
            "Max HP: ", hpField,
            "Max MP: ", mpField,
            "Max AP: ", apField, 
            "Overheal: ", overhealField,  
            "Armour Layers (comma-separated max):", armourLayersField,
            "Shield Layers (comma-separated):", shieldLayersField
        };

        int option = JOptionPane.showConfirmDialog(this, inputs, "Create Character", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                Combatant c = new Combatant(
                    nameField.getText().trim(),
                    Integer.parseInt(initiativeField.getText()),
                    Integer.parseInt(hpField.getText()),
                    Integer.parseInt(mpField.getText()),
                    Integer.parseInt(apField.getText())
                );

                int overheal; 
                try { 
                    overheal = Integer.parseInt(overhealField.getText()); 

                } catch (NumberFormatException e) {  //catches empty input
                    overheal = 0; 
                }

                c.setOverheal(overheal);

                for (String val : armourLayersField.getText().split(",")) {
                    val = val.trim();
                    if (!val.isEmpty()) c.addArmour(Integer.parseInt(val));
                }

                for (String val : shieldLayersField.getText().split(",")) {
                    val = val.trim();
                    if (!val.isEmpty()) c.addShield(Integer.parseInt(val));
                }

                File dir = new File("characters");
                if (!dir.exists()) dir.mkdir();

                FileWriter fw = new FileWriter(new File(dir, c.getName() + ".json"));
                new GsonBuilder().setPrettyPrinting().create().toJson(c, fw);
                fw.close();

                JOptionPane.showMessageDialog(this, "Saved " + c.getName() + " to characters/");

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openCharacterLoader() {
        File dir = new File("characters");
        if (!dir.exists() || dir.listFiles() == null) {
            JOptionPane.showMessageDialog(this, "No characters found.");
            return;
        }

        File[] files = dir.listFiles((f, name) -> name.endsWith(".json"));
        String[] options = Arrays.stream(files).map(File::getName).toArray(String[]::new);

        String choice = (String) JOptionPane.showInputDialog(this,
            "Select a character:",
            "Load Character",
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options.length > 0 ? options[0] : null);

        if (choice == null) return;

        try {
            Gson gson = new Gson();
            FileReader fr = new FileReader(new File(dir, choice));
            Combatant c = gson.fromJson(fr, Combatant.class);
            fr.close();

            // editCharacter(c); // opens edit dialog
            combatants.add(c);
            updateInitiativeList();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load or parse character.");
        }
    }

    private void openCharacterEditor() {
        File dir = new File("characters");
        if (!dir.exists() || !dir.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Character folder not found.");
            return;
        }
    
        File[] files = dir.listFiles((f, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            JOptionPane.showMessageDialog(this, "No saved characters to edit.");
            return;
        }
    
        String[] options = Arrays.stream(files).map(File::getName).toArray(String[]::new);
        String selected = (String) JOptionPane.showInputDialog(
            this,
            "Select a character to edit:",
            "Edit Character",
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[0]
        );
    
        if (selected != null) {
            try {
                FileReader fr = new FileReader(new File(dir, selected));
                Combatant c = new Gson().fromJson(fr, Combatant.class);
                fr.close();
    
                editCharacter(c); // Use your upgraded editor
    
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to load character for editing.");
            }
        }
    }
    
    private void editCharacter(Combatant c) {
        JTextField initiativeField = new JTextField(String.valueOf(c.getInitiative()));
        JTextField curHPField = new JTextField(String.valueOf(c.getHP().getCurrent())); 
        JTextField maxHPField = new JTextField(String.valueOf(c.getHP().getMax())); 
        JTextField curMPField = new JTextField(String.valueOf(c.getMP().getCurrent())); 
        JTextField maxMPField = new JTextField(String.valueOf(c.getMP().getMax())); 
        JTextField curAPField = new JTextField(String.valueOf(c.getAP().getCurrent())); 
        JTextField maxAPField = new JTextField(String.valueOf(c.getAP().getMax()));  
        JTextField overhealField = new JTextField(String.valueOf(c.getOverheal())); 

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        panel.add(new JLabel("Initiative: ")); 
        panel.add(initiativeField); 
        panel.add(new JLabel("Current HP: ")); 
        panel.add(curHPField); 
        panel.add(new JLabel("Maximum HP: ")); 
        panel.add(maxHPField); 
        panel.add(new JLabel("Current MP: ")); 
        panel.add(curMPField); 
        panel.add(new JLabel("Maximum MP: ")); 
        panel.add(maxMPField); 
        panel.add(new JLabel("Current AP: ")); 
        panel.add(curAPField); 
        panel.add(new JLabel("Maximum AP: ")); 
        panel.add(maxAPField); 
        panel.add(new JLabel("Overheal HP: ")); 
        panel.add(overhealField); 

        //Shield Layers (in Top-Down UI Order):   
        panel.add(new JLabel("Edit Shield Layers (current): ")); 
        ArrayList<JTextField> shieldFields = new ArrayList<>(); 
        ArrayList<JPanel> shieldPanels = new ArrayList<>(); 
        List<Statistic> shieldLayerList = new ArrayList<>(c.getShield()); 
        Collections.reverse(shieldLayerList); 
        for(Statistic stat : shieldLayerList) { 
            JPanel layerPanel = new JPanel(); 
            layerPanel.setLayout(new BoxLayout(layerPanel, BoxLayout.X_AXIS)); 
            JTextField layerField = new JTextField(String.valueOf(stat.getCurrent()), 10);
            JButton removeBtn = new JButton("Remove"); 
            
            removeBtn.addActionListener(e -> layerPanel.setVisible(false)); 

            layerPanel.add(layerField); 
            layerPanel.add(removeBtn); 
            panel.add(layerPanel); 

            shieldFields.add(layerField); 
            shieldPanels.add(layerPanel); 
        }
        JTextField newShieldField = new JTextField(); 
        panel.add(new JLabel("Add Shield Layer (current): ")); 
        panel.add(newShieldField); 

        //Armour Layers (in Top-Down UI Order): 
        panel.add(new JLabel("Edit Armour Layers (current/max):"));
        ArrayList<JTextField> armourFields = new ArrayList<>(); 
        ArrayList<JPanel> armourPanels = new ArrayList<>(); 
        List<Statistic> armourLayerList = new ArrayList<>(c.getArmour()); 
        Collections.reverse(armourLayerList); 
        for(Statistic armour : armourLayerList) { 
            JPanel layerPanel = new JPanel(); 
            layerPanel.setLayout(new BoxLayout(layerPanel, BoxLayout.X_AXIS));
            JTextField layerField = new JTextField(armour.getCurrent() + "/" + armour.getMax(), 10); 
            JButton removeBtn = new JButton("Remove"); 

            removeBtn.addActionListener(e -> layerPanel.setVisible(false)); 

            layerPanel.add(layerField); 
            layerPanel.add(removeBtn); 
            panel.add(layerPanel); 

            armourFields.add(layerField); 
            armourPanels.add(layerPanel); 
        }
        JTextField newArmourField = new JTextField(); 
        panel.add(new JLabel("Add Armour Layer (current/max): ")); 
        panel.add(newArmourField); 

        int option = JOptionPane.showConfirmDialog(this, panel, "Edit Character: " + c.getName(), JOptionPane.OK_CANCEL_OPTION);
    
        if (option == JOptionPane.OK_OPTION) {
            try {
                // Base stats
                Random initiativeRoll = new Random(); 
                int initiativeMin = 1; 
                int initiativeMax = 20; 

                int newInitiative;
                try { 
                    newInitiative = Integer.parseInt(initiativeField.getText()); 

                } catch (NumberFormatException e) { //catches empty input and rolls initiative
                    newInitiative = initiativeRoll.nextInt(initiativeMax - initiativeMin + 1) + initiativeMin; 
                }

                c.setInitiative(newInitiative);

                int newCurHP = Integer.parseInt(curHPField.getText());
                int newMaxHP = Integer.parseInt(maxHPField.getText());
                c.getHP().setCurrent(Math.min(newCurHP, newMaxHP));
                c.getHP().setMax(newMaxHP); 

                int newCurMP = Integer.parseInt(curMPField.getText());
                int newMaxMP = Integer.parseInt(maxMPField.getText());
                c.getMP().setCurrent(Math.min(newCurMP, newMaxMP));
                c.getMP().setMax(newMaxMP); 

                int newCurAP = Integer.parseInt(curAPField.getText());
                int newMaxAP = Integer.parseInt(maxAPField.getText());
                c.getAP().setCurrent(Math.min(newCurAP, newMaxAP));
                c.getAP().setMax(newMaxAP); 

                int newOverhealHP;
                try { 
                    newOverhealHP = Integer.parseInt(overhealField.getText());  

                } catch (NumberFormatException e) { //catches empty input and rolls initiative
                    newOverhealHP = 0; 

                }

                c.setOverheal(newOverhealHP);
                
                //Build Shield Stack: 
                List<Statistic> shieldBuffer = new ArrayList<>(); 
                for (int i = 0 ; i < shieldFields.size() ; i++) { 
                    if(!shieldPanels.get(i).isVisible()) { 
                        continue; //skip removed
                    }
                    
                    int cur = Integer.parseInt(shieldFields.get(i).getText().trim());
                    if(cur > 0) { 
                        shieldBuffer.add(new Statistic(0, cur)); 
                    }
                }
                if(!newShieldField.getText().trim().isEmpty()) { 
                    int cur = Integer.parseInt(newShieldField.getText().trim());
                    if (cur > 0) { 
                        shieldBuffer.add(new Statistic(0,cur)); 
                    }
                }
                Stack<Statistic> shieldStack = new Stack<>(); 
                for (Statistic stat : shieldBuffer) { 
                    shieldStack.push(stat); 
                }
                c.setShield(shieldStack); 

                                //Building the Armour Stack: 
                List<Statistic> armourBuffer = new ArrayList<>(); 
                for (int i = 0 ; i < armourFields.size() ; i++) { 
                    if(!armourPanels.get(i).isVisible()) { 
                        continue; //skip removed
                    }
                    String[] parts = armourFields.get(i).getText().split("/");
                    if(parts.length == 2) { 
                        int cur = Integer.parseInt(parts[0].trim());
                        int max = Integer.parseInt(parts[1].trim()); 

                        if(cur > max) { 
                            cur = max; 
                        }

                        armourBuffer.add(new Statistic(max, cur)); 
                    }
                }
                if(!newArmourField.getText().trim().isEmpty()) { 
                    String[] parts = newArmourField.getText().split("/");
                    if(parts.length == 2) { 
                        int cur = Integer.parseInt(parts[0].trim());
                        int max = Integer.parseInt(parts[1].trim()); 

                        if(cur > max) { 
                            cur = max; 
                        }

                        armourBuffer.add(new Statistic(max, cur)); 
                    }
                }
                Stack<Statistic> armourStack = new Stack<>(); 
                for(Statistic stat : armourBuffer) { 
                    armourStack.push(stat); 
                }
                c.setArmour(armourStack);

                // Save changes
                FileWriter fw = new FileWriter(new File("characters", c.getName() + ".json"));
                new GsonBuilder().setPrettyPrinting().create().toJson(c, fw);
                fw.close();
    
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error parsing inputs. Please use valid numeric formats.");
            }
        }
    }
}
