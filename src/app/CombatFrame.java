package app; 

// import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
// import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
// import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
// import java.util.HashMap; 
import java.util.List;
import java.util.Map;
// import java.util.Map;
import java.util.Stack;
// import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage; 
import javax.swing.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Random;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;

public class CombatFrame extends JFrame implements TokenSelectionListener {
    private DefaultListModel<Combatant> initiativeListModel;
    private JList<Combatant> initiativeList;
    private ArrayList<Combatant> combatants;
    private ArrayList<Combatant> downedCombatants;
    private JButton nextTurnButton;
    private int turnIndex = 0;    // Tracks whose turn it is
    private int turnCount = 0; 
    private int roundNumber = 1;  // Tracks the round count
    private JLabel roundLabel;    // Displays round and turn
    
    private JPanel characterPanel; 
    private JPanel controlPanel; 
    private JPanel statsPanel;

    private Component currentEastComponent; 
    private JScrollPane gridScroll; 
    private GridPanel gridPanel;
    private int lastMapRows = -1;
    private int lastMapCols = -1;
    private boolean combatRunning = false;

    private JScrollPane initiativeScrollPane; 

    private JLabel nameLabel; 
    private ResourceBar hpBar; 
    // private ResourceBar mpBar; //WIP
    
    private JProgressBar overhealhpBar; 
    private JLabel hpLabel; 
    private JLabel overhealhpLabel; 
    private JLabel armourLabel; 
    private JLabel acLabel; 
    private JLabel shieldLabel; 

    private JPanel shieldPanel;
    private JPanel armourPanel;

    private static final Color SHIELD_BAR_COLOR = new Color(77, 121, 255); 
    private static final Color ARMOUR_BAR_COLOR = new Color(179, 179, 179); 
    private static final Color OVERHEAL_BAR_COLOR = Color.GREEN; 

    // private final Map<String, Integer> instanceCounters = new HashMap<>(); //DEFUNCT
    private Combatant selectedCombatant = null;

    private Map<String, BufferedImage> imageCache = new HashMap<>();

    private Timer smoothPanTimer;

    public CombatFrame() {
        setTitle("D&D Initiative Tracker");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize Components
        initiativeListModel = new DefaultListModel<>();
        initiativeList = new JList<>(initiativeListModel);
        combatants = new ArrayList<>();
        downedCombatants = new ArrayList<>(); 

        // Round and Turn Label at the top
        roundLabel = new JLabel("Round: 1 | Turn: 1", SwingConstants.CENTER);
        add(roundLabel, BorderLayout.NORTH);

        initiativeScrollPane = new JScrollPane(initiativeList); //displayed by default.
        currentEastComponent = initiativeScrollPane; 
        add(initiativeScrollPane, BorderLayout.EAST);

        controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(1, 3));

        characterPanel = new JPanel(); 
        characterPanel.setLayout(new GridLayout(5, 1)); 

        nextTurnButton = new JButton("Next Turn (->)");
        JButton resetButton = new JButton("Reset Combat (R)");
        JButton attackButton = new JButton("Manual Attack (/)");
        // JButton addButton = new JButton("Add Combatant (+)");
        JButton removeButton = new JButton("Remove Combatant (-)");
        JButton startButton = new JButton("Start Combat");

        controlPanel.add(startButton); 
        controlPanel.add(nextTurnButton);
        controlPanel.add(resetButton);
        controlPanel.add(attackButton);
        // controlPanel.add(addButton);
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
        hpBar = new ResourceBar(); 

        //other resources: 
        // mpBar = new ResourceBar(); //WIP
        
        // Add to main window
        // add(statsPanel, BorderLayout.EAST); //do not add anymore. 

        statsPanel.add(nameLabel); 
        statsPanel.add(hpLabel);
        statsPanel.add(hpBar.getBar());
        statsPanel.add(overhealhpLabel);
        statsPanel.add(overhealhpBar);
        statsPanel.add(armourLabel);
        statsPanel.add(shieldPanel);
        statsPanel.add(armourPanel);
        // add(statsPanel, BorderLayout.EAST); // Add it to the side - do not add anymore. 

        updateStatDisplay(null);

        // Action Listeners
        startButton.addActionListener(e -> startCombat());
        // addButton.addActionListener(e -> addCombatant());
        nextTurnButton.addActionListener(e -> nextTurn());
        removeButton.addActionListener(e -> removeCombatant());
        resetButton.addActionListener(e -> resetCombat());
        attackButton.addActionListener(e -> {
            if(selectedCombatant == null || combatants.isEmpty()) { 
                return; 
            }

            List<CombatantWrapper> options = combatants.stream()
                                                       .filter(c -> c != selectedCombatant)
                                                       .map(c -> new CombatantWrapper(c, combatants))
                                                       .toList();

            CombatantWrapper selected = (CombatantWrapper) JOptionPane.showInputDialog(
                this,
                "Select a target to attack:",
                "Choose Target",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options.toArray(),
                null
            );

            if (selected != null) {
                manuallyAttackCombatant(selectedCombatant, selected.c);
                updateInitiativeList(); // Refresh after damage
            }
        });
        createCharBtn.addActionListener(e -> openCharacterCreator());
        loadCharBtn.addActionListener(e -> openCharacterLoader());
        editCharBtn.addActionListener(e -> openCharacterEditor());

        initiativeList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Combatant selected = initiativeList.getSelectedValue();
                if (selected != null) {
                    updateStatDisplay(selected); // Only updates display
                }
            }
        });

        setVisible(true);
    }

    private void startCombat() {
        // End previous combat (grid and combatants)
        if (gridPanel != null) {
            remove(gridScroll);
            gridPanel = null;
            gridScroll = null;
        }
        combatants.clear();
        downedCombatants.clear();
        turnIndex = 0;
        roundNumber = 1;
        turnCount = 1;
        selectedCombatant = null;
        roundLabel.setText("Round: 1 | Turn: 1");
        updateInitiativeList();
        if (currentEastComponent != null) {
            getContentPane().remove(currentEastComponent);
            currentEastComponent = null;
        }
    
        // Always ask new dimensions
        JTextField rowsField = new JTextField();
        String rowsFieldText = "Enter number of Rows";
        rowsField.setName(rowsFieldText);
        rowsField.setColumns(5);

        JTextField colsField = new JTextField();
        String colsFieldText = "Enter number of Columns";
        colsField.setName(colsFieldText);
        colsField.setColumns(5);

        List<Component> mapComponents = new ArrayList<>();
        mapComponents.add(makeLine(rowsFieldText, rowsField));
        mapComponents.add(makeLine(colsFieldText, colsField));

        JTextField[] allFields = {
            rowsField, colsField 
        };
        
        boolean result = showCharacterDialog(mapComponents, "Map Size", false, rowsField, allFields);

        if(result) {
            try {
                lastMapRows = Integer.parseInt(rowsField.getText());
                lastMapCols = Integer.parseInt(colsField.getText());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid map size input.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            // User cancelled creation — do nothing
        }
        
        // Build the new grid
        gridPanel = new GridPanel(lastMapRows, lastMapCols);
        gridPanel.setPreferredSize(new Dimension(800, 800));
        gridPanel.setCombatFrame(this);

        gridPanel.addMouseWheelListener(gridPanel);
        gridPanel.addMouseListener(gridPanel);
        gridPanel.addMouseMotionListener(gridPanel);
        gridPanel.setTokenSelectionListener(this);

        gridPanel.resetCamera(); 
    
        gridScroll = new JScrollPane(gridPanel);
        add(gridScroll, BorderLayout.CENTER);
    
        currentEastComponent = initiativeScrollPane;
        add(currentEastComponent, BorderLayout.EAST);
    
        revalidate();
        repaint();
    
        combatRunning = true;
    }
       
    @Override
    public void onTokenSelected(Token token) {
        if (currentEastComponent != null) {
            getContentPane().remove(currentEastComponent);
        }
    
        if (token != null) {
            updateStatDisplay(token.getAssociatedCombatant());
            add(statsPanel, BorderLayout.EAST);
            currentEastComponent = statsPanel;

            smoothCenterViewOnToken(token);

        } else {
            add(initiativeScrollPane, BorderLayout.EAST);
            currentEastComponent = initiativeScrollPane;
        }
    
        revalidate();
        repaint();
    }

    private void smoothCenterViewOnToken(Token token) {
        if (token == null || gridPanel == null || gridScroll == null) return;
    
        Rectangle viewRect = gridScroll.getViewport().getViewRect();
    
        int targetX = (int) ((token.getGridX() + 0.5) * gridPanel.getCellSize() * gridPanel.getScale());
        int targetY = (int) ((token.getGridY() + 0.5) * gridPanel.getCellSize() * gridPanel.getScale());
    
        int viewCenterX = viewRect.width / 2;
        int viewCenterY = viewRect.height / 2;
    
        int desiredScrollX = targetX - viewCenterX;
        int desiredScrollY = targetY - viewCenterY;
    
        Point start = gridScroll.getViewport().getViewPosition();
        Point end = new Point(Math.max(0, desiredScrollX), Math.max(0, desiredScrollY));
    
        // SNAP INSTANTLY IF VERY CLOSE (Within 5px)
        if (start.distance(end) <= 5) {
            gridScroll.getViewport().setViewPosition(end);
            gridPanel.repaint();
            return;
        }
    
        // Cancel any existing smooth pan
        if (smoothPanTimer != null && smoothPanTimer.isRunning()) {
            smoothPanTimer.stop();
        }
    
        final int totalSteps = 30; // Smoother with more frames
        final int delay = 15; // Milliseconds between frames
    
        smoothPanTimer = new Timer(delay, null);
        smoothPanTimer.addActionListener(new ActionListener() {
            int currentStep = 0;
    
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gridPanel.isPanning()) { 
                    smoothPanTimer.stop();
                    return;
                }

                currentStep++;
                double t = (double) currentStep / totalSteps;
                t = t * t * (3 - 2 * t); // SmoothStep

                int newX = (int) (start.x + (end.x - start.x) * t);
                int newY = (int) (start.y + (end.y - start.y) * t);

                // Clamp the newX and newY inside grid bounds
                Dimension viewSize = gridScroll.getViewport().getViewSize();
                Dimension extentSize = gridScroll.getViewport().getExtentSize();

                int maxScrollX = viewSize.width - extentSize.width;
                int maxScrollY = viewSize.height - extentSize.height;

                newX = Math.max(0, Math.min(newX, maxScrollX));
                newY = Math.max(0, Math.min(newY, maxScrollY));

                gridScroll.getViewport().setViewPosition(new Point(newX, newY));
                gridPanel.repaint();

                // If we already reached the final destination OR if stuck at boundary, stop
                if (currentStep >= totalSteps || (newX == maxScrollX || newY == maxScrollY)) {
                    smoothPanTimer.stop();
                }
            }
        });
    
        smoothPanTimer.start();
    }
    
    // private void addCombatant() {
    //     JTextField nameField = new JTextField();
    //     JTextField initiativeField = new JTextField();
    //     JTextField hpField = new JTextField(); 
    //     JTextField mpField = new JTextField(); 
    //     JTextField apField = new JTextField(); 
    //     JTextField overhealField = new JTextField(); 
    //     JTextField armourLayersField = new JTextField();  // e.g. "25,15"
    //     JTextField shieldLayersField = new JTextField(); // e.g. "10,5"
        
    //     String nameFieldText = "Name"; 
    //     String initiativeFieldText = "Initiative";
    //     String hpFieldText = "Hit Points";
    //     String mpFieldText = "Mana Points"; 
    //     String apFieldText = "Action Points"; 
    //     String overhealFieldText = "Overheal"; 
    //     String armourLayersFieldText = "Armour Layers (comma-separated max values)";
    //     String shieldLayersFieldText = "Shield Layers (comma-separated current values)"; 

    //     nameField.setName(nameFieldText); 
    //     initiativeField.setName(initiativeFieldText); 
    //     hpField.setName(hpFieldText); 
    //     mpField.setName(mpFieldText); 
    //     apField.setName(apFieldText); 
    //     overhealField.setName(overhealFieldText); 
    //     armourLayersField.setName(armourLayersFieldText);
    //     shieldLayersField.setName(shieldLayersFieldText); 

    //     List<Component> combatantComponents = new ArrayList<>(); 
    //     combatantComponents.add(makeLine(nameFieldText, nameField));
    //     combatantComponents.add(makeLine(hpFieldText, hpField));
    //     combatantComponents.add(makeLine(mpFieldText, mpField));
    //     combatantComponents.add(makeLine(apFieldText, apField));
    //     combatantComponents.add(makeLine(overhealFieldText, overhealField));
    //     combatantComponents.add(makeLine(shieldLayersFieldText, shieldLayersField));
    //     combatantComponents.add(makeLine(armourLayersFieldText, armourLayersField));

    //     JTextField[] allFields = {
    //         nameField, initiativeField, hpField, mpField, apField, overhealField, shieldLayersField, armourLayersField 
    //     };
    
    //     boolean result = showCharacterDialog(combatantComponents, "Add Combatant", false, nameField, allFields);

    //     if(result) {
    //         try {
    //             String name = nameField.getText().trim();

    //             Random initiativeRoll = new Random(); 
    //             int initiativeMin = 1; 
    //             int initiativeMax = 20; 

    //             int initiative;
    //             try { 
    //                 initiative = Integer.parseInt(initiativeField.getText());

    //             } catch (NumberFormatException e) { //catches empty input and rolls initiative
    //                 initiative = initiativeRoll.nextInt(initiativeMax - initiativeMin + 1) + initiativeMin; 
    //             }

    //             int hitPoints = Integer.parseInt(hpField.getText()); 
    //             int manaPoints = Integer.parseInt(mpField.getText()); 
    //             int actionPoints = Integer.parseInt(apField.getText()); 
                 
    //             Combatant combatant = new Combatant(name, initiative, hitPoints, manaPoints, actionPoints);

    //             //handle overheal: 
    //             int overheal; 
    //             try { 
    //                 overheal = Integer.parseInt(overhealField.getText()); 

    //             } catch (NumberFormatException e) {  //catches empty input
    //                 overheal = 0; 

    //             }

    //             combatant.setOverheal(overheal);
                
    //             // Parse and add armour layers
    //             //add layers in increasing order (L->R)-> first armour is first layer. 
    //             String[] armourValues = armourLayersField.getText().split(",");
    //             List<String> armourValuesList = Arrays.asList(armourValues); 
    //             Collections.reverse(armourValuesList); //reverse the array. 
        
    //             for (String value : armourValuesList) {
    //                 value = value.trim();
    //                 if (!value.isEmpty()) {
    //                     int armourMax = Integer.parseInt(value);
    //                     if(armourMax >= 0) { 
    //                         combatant.addArmour(armourMax);
    //                     }
    //                 }
    //             }

    //             // Parse and add shield layers
    //             //add layers in increasing order (L->R) -> first shield is first layer. 
    //             String[] shieldValues = shieldLayersField.getText().split(",");
    //             List<String> shieldValuesList = Arrays.asList(shieldValues); 
    //             Collections.reverse(shieldValuesList); //reverse the array. 
    //             for (String value : shieldValuesList) {
    //                 value = value.trim();
    //                 if (!value.isEmpty()) {
    //                     int shieldCurrent = Integer.parseInt(value);
    //                     if (shieldCurrent > 0) {
    //                         combatant.addShield(shieldCurrent);
    //                     }
    //                 }
    //             }

    //             combatants.add(combatant);
    //             if (combatants.size() == 1) {
    //                 turnIndex = 0;
    //                 selectedCombatant = combatant;
    //             }
                
    //             updateInitiativeList();
    //             updateStatDisplay(selectedCombatant);
    //             initiativeList.setSelectedValue(selectedCombatant, true);

    //         } catch (NumberFormatException e) {
    //             JOptionPane.showMessageDialog(this, "Invalid input!", "Error", JOptionPane.ERROR_MESSAGE);
    //         }
    //     } else {
    //         // User cancelled creation — do nothing
    //     }
    // }

    private void removeCombatant() {
        int selectedIndex = initiativeList.getSelectedIndex();
        if (selectedIndex != -1) {
            combatants.remove(selectedIndex);
            updateInitiativeList();
        } else {
            JOptionPane.showMessageDialog(this, "Select a combatant to remove!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void manuallyAttackCombatant(Combatant attacker, Combatant target) {
        if(attacker == null || target == null || attacker == target) { 
            JOptionPane.showMessageDialog(this, "Invalid attacker or target!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (combatants.size() < 2) {
            JOptionPane.showMessageDialog(this, "Not enough combatants for an attack!", "Error", JOptionPane.ERROR_MESSAGE);
            return;

        } 
    
        if(attacker.getAP().getCurrent() <= 0) { 
            JOptionPane.showMessageDialog(this, "Not enough Action Points!", "Error", JOptionPane.ERROR_MESSAGE);
            return;

        }

        // Ask for damage value
        String input = JOptionPane.showInputDialog(this, "How much damage does " + attacker.getName() + " deal?");
        if (input == null) {
            return; 
        }

        try {
            int damage = Integer.parseInt(input.trim());
    
            if (damage < 0) {
                JOptionPane.showMessageDialog(this, "Damage cannot be negative!", "Error", JOptionPane.ERROR_MESSAGE);
                return;

            } 

            spendActionPoint(attacker, -1);
            target.takeDamage(damage);  // assume you have takeDamage logic that applies shield, armour, HP
    
            // Show attack result
            JOptionPane.showMessageDialog(this,
                attacker.getName() + " dealt " + damage + " damage to " + target.getName() + ".");
    
            updateStatDisplay(selectedCombatant);  // refresh attacker's stats

            // Remove defeated combatants
            if (target.isDefeated()) {
                downedCombatants.add(target); 
                combatants.remove(target);
                JOptionPane.showMessageDialog(this, target.getName() + " has been defeated!", "Combat Log", JOptionPane.INFORMATION_MESSAGE);
            }

            updateInitiativeList();                // refresh full list
    
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid damage value!");
        }
    }

    private void spendActionPoint(Combatant c, int apSpentModified) {
        c.getAP().modifyCurrent(apSpentModified);
        updateStatDisplay(c);
    }

    private void updateStatDisplay(Combatant combatant) {
        statsPanel.removeAll(); 
        if (combatant == null) { 
            statsPanel.revalidate();
            statsPanel.repaint();
            return; //dont load any UI
        }

        // selectedCombatant = combatant;

        //dynamically generate this statPanel each time: 
        statsPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Name label (row 0)
        nameLabel.setText("Name: " + combatant.getName());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        statsPanel.add(nameLabel, gbc);

        // Armour Label (row 1)
        armourLabel.setText("Armour: " + combatant.getTotalAC() + "/" + combatant.getTotalMaxAC()+ " | Shielding: " + combatant.getTotalShield());
        gbc.gridy++;
        statsPanel.add(armourLabel, gbc);

        // Shield bar panel (row 2 left), Shield total label (row 2 right)
        shieldPanel.removeAll();
        Stack<Statistic> shields = combatant.getShield();
        for (int i = shields.size() - 1; i >= 0; i--) {
            Statistic s = shields.get(i);
            if (s.getCurrent() > 0) {
                JProgressBar bar = new JProgressBar();
                bar.setForeground(SHIELD_BAR_COLOR);
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
        shieldLabel.setText("Shield: " + combatant.getTotalShield());
        statsPanel.add(shieldLabel, gbc);

        // Armour bar panel (row 3 left), Armour label (row 3 right)
        armourPanel.removeAll();
        Stack<Statistic> armours = combatant.getArmour();
        for (int i = armours.size() - 1; i >= 0; i--) {
            Statistic s = armours.get(i);
            JProgressBar bar = new JProgressBar(0, s.getMax());
            bar.setForeground(ARMOUR_BAR_COLOR);
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
        acLabel.setText("AC: " + combatant.getTotalAC() + "/" + combatant.getTotalMaxAC());
        statsPanel.add(acLabel, gbc);
    
        // Overheal (row 4)
        gbc.gridy++;
        gbc.gridx = 0;
        overhealhpBar.setMaximum(combatant.getOverheal());
        overhealhpBar.setValue(combatant.getOverheal()); 
        overhealhpBar.setStringPainted(true);
        overhealhpBar.setString("Overheal (+)");
        overhealhpBar.setForeground(OVERHEAL_BAR_COLOR);
        statsPanel.add(overhealhpBar, gbc);

        gbc.gridx = 1;
        overhealhpLabel.setText("Overheal: " + combatant.getOverheal());
        statsPanel.add(overhealhpLabel, gbc);

        // HP (row 5)
        gbc.gridy++;
        gbc.gridx = 0;
        hpBar.setResource(combatant.getHP(), true); 
        hpBar.getBar().setStringPainted(true); 
        hpBar.getBar().setString("HP"); 
        hpBar.recolor(); 

        statsPanel.add(hpBar.getBar(), gbc); 

        gbc.gridx = 1;
        hpLabel.setText("HP: " + combatant.getHP().getCurrent() + "/" + combatant.getHP().getMax());
        statsPanel.add(hpLabel, gbc);

        //AP Meter: 
        APMeterPanel apMeter = new APMeterPanel(combatant.getAP());
        int maxAPBeforeClipping = 10;                              //pseudo-const: 
        JComponent apComponent;
        if (combatant.getAP().getMax() >= maxAPBeforeClipping) {
            JScrollPane apScrollPane = new JScrollPane(apMeter, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            // apScrollPane.setViewportView(apMeter);
            apScrollPane.setWheelScrollingEnabled(true);
            apScrollPane.setPreferredSize(new Dimension(40, 150)); // less than panel height <- also, adjust width to just make it better... (b ; - ;)b 
            apScrollPane.setBorder(null);                          // optional for clean look
            apScrollPane.getVerticalScrollBar().setOpaque(false);
            // apScrollPane.setVisible(false); 

            // Optional: transparent scrollbar styling
            // apScrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            //     @Override
            //     protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            //         g.setColor(new Color(0, 0, 0, 0));
            //         g.fillRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height);
            //     }

            //     @Override
            //     protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            //         g.setColor(new Color(0, 0, 0, 0));
            //         g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            //     }

            //     @Override
            //     protected JButton createDecreaseButton(int orientation) {
            //         return createZeroButton();
            //     }

            //     @Override
            //     protected JButton createIncreaseButton(int orientation) {
            //         return createZeroButton();
            //     }

            //     private JButton createZeroButton() {
            //         JButton button = new JButton();
            //         button.setPreferredSize(new Dimension(0, 0));
            //         button.setMinimumSize(new Dimension(0, 0));
            //         button.setMaximumSize(new Dimension(0, 0));
            //         button.setVisible(false);
            //         return button;
            //     }
            // });
            
            apComponent = apScrollPane;

        } else {
            apComponent = apMeter;
        }
        
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1;
        statsPanel.add(apComponent, gbc);

        // Final refresh
        statsPanel.revalidate();
        statsPanel.repaint();
    }
    
        
    private void updateInitiativeList() {
        initiativeListModel.clear();

        //if downedCombatants getback up at any point in time: 
        for (Combatant c : downedCombatants) { 
            if(c.getHP().getCurrent() > 0) { 
                downedCombatants.remove(c); 
                combatants.add(c); 
            }
        }

        Collections.sort(combatants, Comparator.comparingInt(Combatant::getInitiative).reversed());

        DefaultListModel<Combatant> listModel = new DefaultListModel<>();
        for (Combatant c : combatants) {
            listModel.addElement(c);
        }
        initiativeList.setModel(listModel);
    
        // Custom cell renderer to show marker
        initiativeList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        
                if (value instanceof Combatant c) {
                    // Count how many combatants share this name
                    long count = combatants.stream().filter(o -> o.getName().equals(c.getName())).count();
        
                    // Determine this combatant's order among same-name instances
                    int instanceNumber = (int) combatants.stream()
                        .filter(o -> o.getName().equals(c.getName()))
                        .takeWhile(o -> o != c)
                        .count() + 1;

                    String instancePart = (count > 1) ? " #" + instanceNumber : "";

                    String prefix = (c == selectedCombatant) ? "→ " : "   ";

                    String label = String.format(
                        "%s%s%s  [Init: %d | HP: %d/%d | MP: %d/%d | AP: %d/%d | AC: %d/%d | Shielding: %d]",
                        prefix,
                        c.getName(),
                        instancePart,
                        c.getInitiative(),
                        c.getHP().getCurrent(),
                        c.getHP().getMax(),
                        c.getMP().getCurrent(),
                        c.getMP().getMax(), 
                        c.getAP().getCurrent(),
                        c.getAP().getMax(), 
                        c.getTotalAC(),
                        c.getTotalMaxAC(),
                        c.getTotalShield()
                    );
                
                    setText(label);
                }
        
                return comp;
            }
        });
    
        if (combatants.isEmpty()) {
            selectedCombatant = null;
            statsPanel.removeAll();
            statsPanel.revalidate();
            statsPanel.repaint();

        } 

        initiativeList.setSelectedValue(selectedCombatant, true);
        
        // else if (!isManuallySelected) {
        //     selectedCombatant = combatants.get(turnIndex);
        //     updateStatDisplay(selectedCombatant);
        // }
    }
    
    private void nextTurn() {
        if (combatants.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No combatants in the list!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Move to the next combatant
        turnIndex = (turnIndex + 1) % combatants.size();
        turnCount++; 
        selectedCombatant = combatants.get(turnIndex);
        initiativeList.setSelectedValue(selectedCombatant, true); // auto-highlight

        // After setting the selectedCombatant
        if (selectedCombatant != null) {
            // Find corresponding Token
            Token selectedToken = gridPanel.getTokenForCombatant(selectedCombatant);
            if (selectedToken != null) {
                smoothCenterViewOnToken(selectedToken);
            }
        }
    
        // if turnIndex = 0 -> new round (because of modulo and circular)
        if (turnIndex == 0) {
            roundNumber++;

            for(Combatant c : combatants) { 
                c.getAP().setCurrent(c.getAP().getMax()); 
            }
        }
    
        // Update round and turn display
        roundLabel.setText("Round: " + roundNumber + " | Turn: " + turnCount);
        updateInitiativeList(); // will auto-select current attacker
    }
    
    private void resetCombat() {
        if(gridPanel != null) {
            startCombat();
        }

        combatants.clear();
        turnIndex = 0;
        roundNumber = 1;
        roundLabel.setText("Round: 1 | Turn: 1");
        updateInitiativeList();
    }
    
    private void openCharacterCreator() {    
        String tokenImagePath = null;
        BufferedImage tokenImage = null;
        
        int chooseTokenImage = JOptionPane.showConfirmDialog(this, "Would you like to select a token image?", "Token Image", JOptionPane.YES_NO_OPTION);
        if (chooseTokenImage == JOptionPane.YES_OPTION) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Token Image for Character");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                try {
                    BufferedImage originalImage = ImageIO.read(selectedFile);
                    if (originalImage == null) {
                        JOptionPane.showMessageDialog(this, "Selected file is not a valid image!");
                        return;
                    }
        
                    File tokenDir = new File("tokens");
                    if (!tokenDir.exists()) {
                        tokenDir.mkdirs();
                    }
        
                    Path destPath = Paths.get("tokens", selectedFile.getName());
                    String fileExtension = getFileExtension(selectedFile.getName());
                    if (fileExtension == null) fileExtension = "png";
        
                    // Resize if necessary
                    int maxDimension = 128;
                    int width = originalImage.getWidth();
                    int height = originalImage.getHeight();
                    BufferedImage finalImage = originalImage;
                    if (width > maxDimension || height > maxDimension) {
                        double scale = Math.min((double) maxDimension / width, (double) maxDimension / height);
                        int newWidth = (int) (width * scale);
                        int newHeight = (int) (height * scale);
        
                        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2d = resized.createGraphics();
                        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
                        g2d.dispose();
        
                        finalImage = resized;
                    }
        
                    ImageIO.write(finalImage, fileExtension, destPath.toFile());
                    tokenImagePath = destPath.toString();
                    tokenImage = finalImage;
        
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error processing token image: " + ex.getMessage());
                    return;
                }
            } else {
                return; // Cancelled token selection
            }
        } else {
            // User chose NO, fallback
            tokenImagePath = "FALLBACK:" + "Unknown"; // <--- assign a fallback path
            tokenImage = createFallbackImage("Unknown");
        }
        
        JTextField nameField = new JTextField();
        JTextField initiativeField = new JTextField();
        JTextField hpField = new JTextField();
        JTextField mpField=  new JTextField(); 
        JTextField apField = new JTextField(); 
        JTextField overhealField = new JTextField(); 
        JTextField armourLayersField = new JTextField();  // e.g., "30,20"
        JTextField shieldLayersField = new JTextField(); // e.g., "15,10"

        String nameFieldText = "Name"; 
        String initiativeFieldText = "Initiative"; 
        String hpFieldText = "Maximum HP";
        String mpFieldText = "Maximum MP"; 
        String apFieldText = "Maximum AP"; 
        String overhealFieldText = "Overheal HP"; 
        String armourLayersFieldText = "Armour Layers (comma-separated max)"; 
        String shieldLayersFieldText = "Shield Layers (comma-separated)"; 

        nameField.setName(nameFieldText); 
        initiativeField.setName(initiativeFieldText); 
        hpField.setName(hpFieldText);
        mpField.setName(mpFieldText); 
        apField.setName(apFieldText);
        overhealField.setName(overhealFieldText);
        armourLayersField.setName(armourLayersFieldText);
        shieldLayersField.setName(shieldLayersFieldText);
        
        List<Component> creationComponents = new ArrayList<>();
        creationComponents.add(makeLine(nameFieldText, nameField));
        creationComponents.add(makeLine(initiativeFieldText, initiativeField));
        creationComponents.add(makeLine(hpFieldText, hpField));
        creationComponents.add(makeLine(mpFieldText, mpField));
        creationComponents.add(makeLine(apFieldText, apField));
        creationComponents.add(makeLine(overhealFieldText, overhealField));
        creationComponents.add(makeLine(shieldLayersFieldText, shieldLayersField));
        creationComponents.add(makeLine(armourLayersFieldText, armourLayersField));
        
        JTextField[] allFields = {
            nameField, initiativeField, hpField, mpField, apField, overhealField, shieldLayersField, armourLayersField
        };
                
        boolean result = showCharacterDialog(creationComponents, "Create Character", false, nameField, allFields);

        if(result) {
            try {
                String name = nameField.getText().trim();

                //guarantees that fallback tokens use real character names for consistent appearance.
                if (tokenImagePath != null && tokenImagePath.startsWith("FALLBACK:")) {
                    tokenImagePath = "FALLBACK:" + name;
                }

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
                 
                Combatant c = new Combatant(name, initiative, hitPoints, manaPoints, actionPoints);

                int overheal; 
                try { 
                    overheal = Integer.parseInt(overhealField.getText()); 

                } catch (NumberFormatException e) {  //catches empty input
                    overheal = 0; 
                }

                c.setOverheal(overheal);

                // Parse and add armour layers
                //add layers in increasing order (L->R)-> first armour is first layer. 
                String[] armourValues = armourLayersField.getText().split(",");
                List<String> armourValuesList = Arrays.asList(armourValues); 
                Collections.reverse(armourValuesList); //reverse the array. 
        
                for (String value : armourValuesList) {
                    value = value.trim();
                    if (!value.isEmpty()) {
                        int armourMax = Integer.parseInt(value);
                        if(armourMax >= 0) { 
                            c.addArmour(armourMax);
                        }
                    }
                }

                // Parse and add shield layers
                //add layers in increasing order (L->R) -> first shield is first layer. 
                String[] shieldValues = shieldLayersField.getText().split(",");
                List<String> shieldValuesList = Arrays.asList(shieldValues); 
                Collections.reverse(shieldValuesList); //reverse the array. 
                for (String value : shieldValuesList) {
                    value = value.trim();
                    if (!value.isEmpty()) {
                        int shieldCurrent = Integer.parseInt(value);
                        if (shieldCurrent > 0) {
                            c.addShield(shieldCurrent);
                        }
                    }
                }

                c.setTokenImagePath(tokenImagePath);

                File dir = new File("characters");
                if (!dir.exists()) dir.mkdir();

                FileWriter fw = new FileWriter(new File(dir, c.getName() + ".json"));
                new GsonBuilder().setPrettyPrinting().create().toJson(c, fw);
                fw.close();

                JOptionPane.showMessageDialog(this, "Saved " + c.getName() + " to characters/");

            } catch (Exception ex) {
                ex.printStackTrace(); //DEBUG
                JOptionPane.showMessageDialog(this, "Invalid input!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            // User cancelled creation — do nothing
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return null;
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

            BufferedImage tokenImage = loadTokenImage(c);
            Token token = new Token(tokenImage, c);
            
            // Instruct user: "Click a tile to place [Character Name]" - Prepare to place it on the grid
            if (gridPanel != null) {
                gridPanel.prepareToPlaceToken(token);
                
            } else {
                JOptionPane.showMessageDialog(this, "No grid created yet! Please Start Combat first.");
                return;
            }

            // Add combatant to list
            combatants.add(c);

            updateInitiativeList();

            if (combatants.size() == 1) {
                turnIndex = 0;
                selectedCombatant = c;
            }
            
            updateStatDisplay(selectedCombatant);
            initiativeList.setSelectedValue(selectedCombatant, true);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load or parse character.");
        }
    }

    private BufferedImage createFallbackImage(String characterName) {
        int size = 50; // size of token (same as your grid cell size)
        BufferedImage fallback = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = fallback.createGraphics();
    
        // Enable smooth drawing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
        // Generate a random but consistent color based on character's name hash
        Random rand = new Random(characterName.hashCode());
        Color randomColor = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
    
        // Draw filled circle
        g2d.setColor(randomColor);
        g2d.fillOval(0, 0, size, size);
    
        // Draw first letter of character's name
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics fm = g2d.getFontMetrics();
    
        String firstLetter = "?";
        if (characterName != null && !characterName.isEmpty()) {
            firstLetter = characterName.substring(0, 1).toUpperCase();
        }
    
        int textWidth = fm.stringWidth(firstLetter);
        int textHeight = fm.getAscent();
        int textX = (size - textWidth) / 2;
        int textY = (size + textHeight) / 2 - 4;
    
        g2d.drawString(firstLetter, textX, textY);
    
        g2d.dispose();
        return fallback;
    }

    private BufferedImage loadTokenImage(Combatant c) {
        if (c.getTokenImagePath() == null) {
            return createFallbackImage(c.getName());
        }
    
        if (c.getTokenImagePath().startsWith("FALLBACK:")) {
            return createFallbackImage(c.getName());
        }
    
        if (imageCache.containsKey(c.getTokenImagePath())) {
            return imageCache.get(c.getTokenImagePath());
        }
    
        try {
            BufferedImage img = ImageIO.read(new File(c.getTokenImagePath()));
            imageCache.put(c.getTokenImagePath(), img);
            return img;
        } catch (IOException ex) {
            BufferedImage fallback = createFallbackImage(c.getName());
            imageCache.put(c.getTokenImagePath(), fallback);
            return fallback;
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

        String initiativeFieldText = "Initiative"; 
        String curHPFieldText = "Current HP"; 
        String maxHPFieldText = "Maximum HP"; 
        String curMPFieldText = "Current MP"; 
        String maxMPFieldText = "Maximum MP"; 
        String curAPFieldText = "Current AP"; 
        String maxAPFieldText = "Maximum AP";    
        String overhealFieldText = "Overheal HP";    
        
        initiativeField.setName(initiativeFieldText); 
        curHPField.setName(curHPFieldText);
        maxHPField.setName(maxHPFieldText);
        curMPField.setName(curMPFieldText);
        maxMPField.setName(maxMPFieldText);
        curAPField.setName(curAPFieldText);
        maxAPField.setName(maxAPFieldText);
        overhealField.setName(overhealFieldText); 

        // === Organize Fields ===
        List<JTextField> fieldList = new ArrayList<>(); 
        List<Component> componentList = new ArrayList<>();

        // Add base stats
        componentList.add(makeLine("Initiative", initiativeField));
        fieldList.add(initiativeField);
        componentList.add(makeLine("Current HP", curHPField));
        fieldList.add(curHPField);
        componentList.add(makeLine("Maximum HP", maxHPField));
        fieldList.add(maxHPField);
        componentList.add(makeLine("Current MP", curMPField));
        fieldList.add(curMPField);
        componentList.add(makeLine("Maximum MP", maxMPField));
        fieldList.add(maxMPField);
        componentList.add(makeLine("Current AP", curAPField));
        fieldList.add(curAPField);
        componentList.add(makeLine("Maximum AP", maxAPField));
        fieldList.add(maxAPField);
        componentList.add(makeLine("Overheal HP", overhealField));
        fieldList.add(overhealField);
                 
        // === Shield Layers ===
        componentList.add(new JLabel("Edit Shield Layers (current):"));
        List<JTextField> shieldFields = new ArrayList<>();
        List<JPanel> shieldPanels = new ArrayList<>();

        List<Statistic> shields = new ArrayList<>(c.getShield());
        Collections.reverse(shields);

        for (Statistic s : shields) {
            JPanel layer = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JTextField shieldField = new JTextField(String.valueOf(s.getCurrent()), 8);
            JButton removeButton = new JButton("X");

            layer.add(shieldField);
            layer.add(removeButton);

            shieldFields.add(shieldField);
            shieldPanels.add(layer);
            fieldList.add(shieldField);
            componentList.add(layer);

            removeButton.addActionListener(e -> {
                layer.setVisible(false);
                shieldField.setText("");
            });
        }

        componentList.add(new JLabel("Add Shield Layer (current):"));
        JTextField newShieldField = new JTextField(8);
        fieldList.add(newShieldField);
        componentList.add(newShieldField);

        // === Armour Layers ===
        componentList.add(new JLabel("Edit Armour Layers (current/max):"));
        List<JTextField> armourFields = new ArrayList<>();
        List<JPanel> armourPanels = new ArrayList<>();

        List<Statistic> armours = new ArrayList<>(c.getArmour());
        Collections.reverse(armours);

        for (Statistic s : armours) {
            JPanel layer = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JTextField armourField = new JTextField(s.getCurrent() + "/" + s.getMax(), 10);
            JButton removeButton = new JButton("X");

            layer.add(armourField);
            layer.add(removeButton);

            armourFields.add(armourField);
            armourPanels.add(layer);
            fieldList.add(armourField);
            componentList.add(layer);

            removeButton.addActionListener(e -> {
                layer.setVisible(false);
                armourField.setText("");
            });
        }

        componentList.add(new JLabel("Add Armour Layer (current/max):"));
        JTextField newArmourField = new JTextField(10);
        fieldList.add(newArmourField);
        componentList.add(newArmourField);

        JTextField[] allFields = fieldList.toArray(new JTextField[0]);

        boolean result = showCharacterDialog(componentList, "Edit Character: " + c.getName(), true, initiativeField, allFields);
    
        if(result) {
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
        } else {
            // User cancelled creation — do nothing
        }
    }

    // Helper to create a neat line: [Label] [Textbox], correctly aligned
    private static final int LABEL_WIDTH = 250; // consistent label width
    private static final int LABEL_HEIGHT = 25; // optional (for nice vertical spacing)
    private static final int FIELD_WIDTH = 200; // ** wider text fields **

    private Component makeLine(String labelText, JTextField field) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
    
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(LABEL_WIDTH, LABEL_HEIGHT));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(label, gbc);
    
        field.setPreferredSize(new Dimension(FIELD_WIDTH, LABEL_HEIGHT));  // wider box
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, gbc);
    
        return panel;
    }
    
    private void setupArrowKeyNavigation(JTextField[] fields, JButton okButton) {
        for (int i = 0; i < fields.length; i++) {
            final int index = i;
            fields[i].addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        if (index + 1 < fields.length) {
                            fields[index + 1].requestFocusInWindow();
                        }
                    } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                        if (index > 0) {
                            fields[index - 1].requestFocusInWindow();
                        }
                    } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (index == fields.length - 1) {
                            okButton.doClick(); // Submit on last field ENTER
                        } else {
                            fields[index + 1].requestFocusInWindow();
                        }
                    }
                }
            });
        }
    }
            
    private boolean showCharacterDialog(List<Component> components, String title, boolean useScrollPane, JTextField firstFieldToFocus, JTextField[] fieldsForNavigation) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setLayout(new BorderLayout());
    
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
    
        for (int i = 0; i < components.size(); i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            mainPanel.add(components.get(i), gbc);
        }
    
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
    
        if (useScrollPane) {
            JScrollPane scrollPane = new JScrollPane(mainPanel);
            scrollPane.setPreferredSize(new Dimension(500, 600));
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            dialog.add(scrollPane, BorderLayout.CENTER);
        } else {
            dialog.add(mainPanel, BorderLayout.CENTER);
        }
    
        dialog.add(buttonPanel, BorderLayout.SOUTH);
    
        final boolean[] confirmed = {false};
    
        okButton.addActionListener(e -> {
            confirmed[0] = true;
            dialog.dispose();
        });
        cancelButton.addActionListener(e -> {
            confirmed[0] = false;
            dialog.dispose();
        });
    
        dialog.pack();
        dialog.setLocationRelativeTo(this);
    
        // Setup focus + navigation
        SwingUtilities.invokeLater(() -> {
            if (firstFieldToFocus != null) {
                firstFieldToFocus.requestFocusInWindow();
            }
            if (fieldsForNavigation != null) {
                setupArrowKeyNavigation(fieldsForNavigation, okButton);
            }
        });
    
        dialog.setVisible(true);
    
        return confirmed[0];
    }    
}

class CombatantWrapper {
    Combatant c;
    List<Combatant> context;

    public CombatantWrapper(Combatant c, List<Combatant> context) {
        this.c = c;
        this.context = context;
    }

    @Override
    public String toString() {
        long count = context.stream().filter(o -> o.getName().equals(c.getName())).count();

        if (count <= 1) {     
            return c.getName();
        }

        int instanceNumber = (int) context.stream()
            .filter(o -> o.getName().equals(c.getName()))
            .takeWhile(o -> o != c)
            .count() + 1;

        return c.getName() + " #" + instanceNumber;
    }
}

class ResourceBar { 
    private JProgressBar bar; 
    private Statistic resource; 

    //colors to change bar color:
    private double low; 
    private Color lowColor; 
    private double mid; 
    private Color midColor; 
    private double high; 
    private Color highColor; 

    public ResourceBar(Statistic r) { 
        this.resource = r;
        this.bar = new JProgressBar(); 
        
        //default bar colors for HP <- change accordingly. 
        this.low = 0.25;
        this.lowColor = Color.RED;
        this.mid = 0.50; 
        this.midColor = Color.ORANGE; 
        this.high = 1.00;
        this.highColor = Color.GREEN; 
    }

    //surrogate to be replaced later.
    public ResourceBar() { 
        this(new Statistic(20)); 
    }

    public Statistic getResource() { 
        return this.resource;
    }

    public void setResource(Statistic newResource, boolean editBar) { 
        this.resource = newResource; 

        //edit the bar:
        if(editBar) { 
            this.bar.setMaximum(newResource.getMax());
            this.bar.setValue(newResource.getCurrent());
        }
    }

    public JProgressBar getBar() { 
        return this.bar;
    }

    public void setBar(JProgressBar newBar) { 
        this.bar = newBar; 
    }

    public double getLowPercentageValue() { 
        return this.low;
    }

    public Color getLowColor() { 
        return this.lowColor;
    }

    public void setLowPercentageValue(double newLow) { 
        this.low = newLow; 
    }

    public void setLowColor(Color newColor) { 
        this.lowColor = newColor; 
    }

    public double getMidPercentageValue() { 
        return this.mid;
    }

    public Color getMidColor() { 
        return this.midColor;
    }

    public void setMidPercentageValue(double newMid) { 
        this.mid = newMid; 
    }

    public void setMidColor(Color newColor) { 
        this.midColor = newColor; 
    }

    public double getHighPercentageValue() { 
        return this.high;
    }

    public Color getHighColor() { 
        return this.highColor;
    }

    public void setHighPercentageValue(double newHigh) { 
        this.high = newHigh; 
    }

    public void setHighColor(Color newColor) { 
        this.highColor = newColor; 
    }

    public void setNewBarColors(Color lowColor, Color midColor, Color highColor) { 
        this.lowColor = lowColor; 
        this.midColor = midColor; 
        this.highColor = highColor; 

    }

    public void setNewBarPercentageThresholds(double newLow, double newMid, double newHigh) { 
        this.low = newLow; 
        this.mid = newMid; 
        this.high = newHigh;
         
    }

    public void recolor() { 
        double percentage = this.bar.getPercentComplete(); //instead of getting the values from statistics outright - but it does mirror.

        if(percentage <= this.low) {
            this.bar.setForeground(this.lowColor);

        //logic can be edited - but default is "<"
        } else if (percentage < this.mid ) { 
            this.bar.setForeground(this.midColor);

        } else { 
            this.bar.setForeground(this.highColor);
        }
    }
}

class APMeterPanel extends JPanel {
    private static final Color AP_METER_COLOR = new Color(153, 255,51);

    private Statistic AP; 

    public APMeterPanel(Statistic AP) {
        this.AP = AP; 
        setPreferredSize(new Dimension(20, 100)); // base size, adjusts
    }

    public void setAP(Statistic AP) {
        this.AP = AP; 
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if(this.AP == null) { 
            return; 
        }

        int max = AP.getMax();
        int cur = AP.getCurrent();
        int spacing = Math.max(2, 12 - max);

        int availableHeight = getHeight();
        int totalSpacing = spacing * (max - 1);
        int diameter = Math.max(6, (availableHeight - totalSpacing) / max); // never go smaller than 6px

        for (int i = 0; i < max; i++) {
            int y = i * (diameter + spacing);
            g.setColor(i < max - cur ? Color.GRAY : AP_METER_COLOR); //spent or available 
            g.fillOval((getWidth() - diameter) / 2, y, diameter, diameter);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (this.AP == null) return new Dimension(30, 100);
    
        int max = this.AP.getMax();
        int spacing = 4;
        int diameter = 12;
    
        int height = max * diameter + (max - 1) * spacing;
    
        return new Dimension(30, height); // must be taller than scroll viewport to enable scroll
    }
}



//private List<Actions> AttackWorkflowList; 
//TODO: rollback effects on parry, time manipulation, legendary resistances, etc.

//WIP:
enum DamageType { 
    SLASHING,        //GENERIC MARTIAL 
    BLUDGEONING, 
    PIERCING, 

    SURGE,           //FOR RANGED
    IMPALING, 

    STRIKING,        //FOR UNARMED STRIKES etc.

    MARTIAL,         //ANY of SLASHING, BLUDGEONING, PIERCING, SURGE, IMPALING, STRIKING - NOT WEIGHT
    PHYSICAL,        //ANY of MARTIAL + WEIGHT + METAL + MECHANICAL + NEO_MECHANICAL + UMBRAL + EARTH + WIND + FORCE

    WEIGHT,          //FOR FALL DAMAGE TO SELF OR TO OTHERS

    FIRE,            //STANDARD ELEMENTAL DAMAGE ROSTER
    WATER, 
    LIGHTNING, 
    EARTH, 
    BEASTWOOD, 
    ACID,
    WIND, 
    COLD, 
    THUNDER,
    RADIANT, 
    DARKNESS,
    NECROTIC,        //also used when a character takes damage from internal or biological sources
    PSYCHIC,         //also used when a character takes mental / psychotic / sanity damage etc.
    FORCE, 
    
    ELEMENTAL,       //ANY of THE STANDARD ELEMENTAL DAMAGES in the ELEMENTAL DAMAGE ROSTER

    ASTRAL, 
    BLOOD, 
    METAL, 
    MECHANICAL, 
    UMBRAL, 
    SHADOW,
    NEO_MECHANICAL,

    STEAM,           //COMPOUND ELEMENTAL DAMAGE
    PURE_WATER,
    ALETA,
    CRACKLING, 
    MAGMA,
    SCORCHED,
    WRITHING,
    BLUE_FIRE, 
    FROST, 
    MODULATED,
    RIPPLE,
    MUD,
    OCEANIC, 
    FLUISON, 
    MIST, 
    BLUE_LICHTEN, 
    ECHOING_MOMENT,
    SNOW, 
    FROZEN_MOMENT,
    WILTING,
    BREEZE,
    STORM,
    LICHTEN, 
    CHARGE,
    STRONG_CULGRE,
    TEMPEST,
    RESERVED,
    PROCLAIMING,
    PERSISTING_CULGRE,
    WHISPERING,
    PURE_LIFE,
    ACCEPTING_DECAY,
    VITRIOLIC,
    STRONG_BREAKDOWN,
    ZEN,
    REGRETFUL_DECAY,

    FADING,          //Radiant + Acid

    METALLIC,        //RACIAL DAMAGES
    CHROMATIC, 

    FIENDISH,        //ALL FIENDISH RACE DAMAGES - DEMONIC, DEVILISH, LOTHIC, RAKSHIAN, SLAADIC, SHRIEKING
    DEMONIC, 
    DEVILISH, 
    LOTHIC_DAEMONIC, 
    RAKSHIAN, 
    SLAADIC,
    SHRIEKING,       //pandamonium

    ALEAN,           //ALL ALEAN RACE DAMAGES - PROGREDIAN, ARCADIAN, BYTOPIC, ELYSIC, BEASTLANDS, VIBRANT, RIGHTEOUS
    PROGREDIAN,      //Mechanicum
    ARCADIAN,
    BYTOPIC,
    ELYSIC,
    INTELLIGENCE_ENERGY, //Beastlands
    VIBRANT,             //Arborea
    RIGHTEOUS,           //Ysgard + Celestia

    SOCIAL,             //damage from embarassment
    EMOTIONAL,
    VISCERAL,           //on top of emotional <- core emotional  
    WONDROUS,           //second tier magical damage etc.
    FAINT,              //opposite of willpower -> really damages you. 

    HELL_FIRE,          //special fire that can only be generated in hell
    REDSTONE_FLUX, 
    BLACK_BLOOD, 
    ABYSSAL,
    WILL_FIRE           //fire borne out of willpower
    ;
}

enum DamageTags { 
    TRUE_DAMAGE,          //damage ignores resistances, shield, armour, overheal etc. and deals damage straight to HP
    
    LETHAL_DAMAGE,        //damage will overflow and deal damage until deathHP to any character.
    NON_LETHAL_DAMAGE,    //damage will only incapacitate enemies. deals 0 damage to characters at 0 HP. 

    MAX_DRAIN,            //damage reduces max value of a statistic resource etc. 
    ABILITY_DRAIN,        //damage reduces an ability score or other passive score directly

    BONUS_DAMAGE,         //damage calculate at the end of an entire attack workflow. 
    AOE_DAMAGE,            //single damage calculation and roll - same damage applied to all targets in AOE 
    PENETRATING_DAMAGE    //damage contains armour penetration. 

    // DIRE_DAMAGE,          //procs Dire Damage
    // IMPALING,             //proc Impaled Condition
    // BLEEDING,             //procs Bleeding Effect
    ;
}

//Active Abilities: 
/* Attack
 * Move
 * Dash
 * Jump
 * Cast Spell
 * Detect
 * Hide
 * Affect Terrain
 * Switch Form
 * Stow Item
 * Draw Item
 * Pick Up Item
 * Drop Item
 * 
 * Interaction (Special Active, Loot, Access External Inventory, etc.)
 * Communicate
 * 
 * Craft Item in Combat
 * Stabilize Ally
 * Combo
 * Help / Assist
 * Displace / Knockback
 * Throw Item
 * Throw Creature
 * Manage Inventory
 * Load / Reload
 * Reaction
 * Ready
 * Concentrate / Focus
 * Engage
 * Disengage
 * Shake Off
 * 
 */

 //Shake Off ->
 //Disengage - Target
 //Engage 
 //Engage -> choice. 
 //Disengage -> 