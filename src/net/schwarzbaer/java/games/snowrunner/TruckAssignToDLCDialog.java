package net.schwarzbaer.java.games.snowrunner;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;

public class TruckAssignToDLCDialog extends JDialog {
	private static final long serialVersionUID = 5851218628292882974L;

	private Boolean useKnownDLC;
	private String selectedKnownDLC;
	private String selectedNewDLC;
	private final JButton btnOk;
	private boolean assignmentsChanged;
	
	public TruckAssignToDLCDialog(Window owner, Truck truck, Language language, HashMap<String, String> truckToDLCAssignments) {
		super(owner,ModalityType.APPLICATION_MODAL);
		useKnownDLC = null;
		selectedNewDLC = null;
		assignmentsChanged = false;
		String key = truck.id;

		if (key==null) {
			System.err.printf("Selected truck has no source XML file. You should cancel this dialog now.");
			selectedKnownDLC = null;
		} else {
			selectedKnownDLC = truckToDLCAssignments.get(key);
		}
		
		
		// determine known DLCs
		HashSet<String> knownDLCSet = new HashSet<>();
		truckToDLCAssignments.forEach((t,dlc)->knownDLCSet.add(dlc));
		
		Vector<String> knownDLCs = new Vector<>(knownDLCSet);
		knownDLCs.sort(null);
		
		
		// create GUI elements
		JComboBox<String> cmbbxKnownDLCs = new JComboBox<String>(knownDLCs);
		if (selectedKnownDLC!=null) {
			cmbbxKnownDLCs.setSelectedIndex(knownDLCs.indexOf(selectedKnownDLC));
			useKnownDLC = true;
		} else
			cmbbxKnownDLCs.setSelectedIndex(-1);
		
		
		JTextField txtfldNewDLC = new JTextField(20);
		
		
		ButtonGroup bg = new ButtonGroup();
		JRadioButton rdbtnUseKnownDLC  = SnowRunner.createRadioButton("Use Known DLC"   , bg, !knownDLCs.isEmpty(), useKnownDLC!=null &&  useKnownDLC, e->{ useKnownDLC = true ; updateOkBtn(); });
		JRadioButton rdbtnDefineNewDLC = SnowRunner.createRadioButton("Define a new DLC", bg, true                , useKnownDLC!=null && !useKnownDLC, e->{ useKnownDLC = false; updateOkBtn(); });
		
		
		// set GUI actions
		cmbbxKnownDLCs.addActionListener(e->{
			int i = cmbbxKnownDLCs.getSelectedIndex();
			selectedKnownDLC = i<0 ? null : knownDLCs.get(i);
			useKnownDLC = true;
			rdbtnUseKnownDLC.setSelected(true);
			updateOkBtn();
		});
		
		Runnable txtfldNewDLCAction = ()->{
			String newDLC = txtfldNewDLC.getText();
			selectedNewDLC = newDLC.isEmpty() ? null : newDLC;
			useKnownDLC = false;
			rdbtnDefineNewDLC.setSelected(true);
			updateOkBtn();
		};
		
		txtfldNewDLC.addActionListener(e->txtfldNewDLCAction.run());
		txtfldNewDLC.addFocusListener(new FocusListener() {
			@Override public void focusGained(FocusEvent e) {}
			@Override public void focusLost(FocusEvent e) { txtfldNewDLCAction.run(); }
		});
		
		
		// define panels
		
		JPanel centerPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 0;
		
		c.weightx = 0; c.gridx = 0;
		c.gridy = 0; centerPanel.add(rdbtnUseKnownDLC,c);
		c.gridy = 1; centerPanel.add(rdbtnDefineNewDLC,c);
		
		c.weightx = 1; c.gridx = 1;
		c.gridy = 0; centerPanel.add(cmbbxKnownDLCs,c);
		c.gridy = 1; centerPanel.add(txtfldNewDLC,c);
		
		
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.weightx = 1;
		buttonPanel.add(new JLabel(),c);
		c.weightx = 0;
		buttonPanel.add(btnOk = SnowRunner.createButton("Ok", false, e->{
			String dlc = useKnownDLC ? selectedKnownDLC : selectedNewDLC;
			if (dlc!=null && key!=null) {
				truckToDLCAssignments.put(key, dlc);
				saveData(truckToDLCAssignments);
				assignmentsChanged = true;
			}
			setVisible(false);
		}),c);
		buttonPanel.add(SnowRunner.createButton("Cancel", true, e->setVisible(false)),c);
		
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		contentPane.add(centerPanel,BorderLayout.CENTER);
		contentPane.add(buttonPanel,BorderLayout.SOUTH);
		
		setTitle(String.format("Assign \"%s\" to an official DLC", SnowRunner.getTruckLabel(truck,language)));
		setContentPane(contentPane);
		pack();
		
		Point oLoc = owner.getLocation();
		Dimension oSize = owner.getSize();
		Dimension mySize = getSize();
		setLocation(
				oLoc.x + (oSize.width -mySize.width )/2,
				oLoc.y + (oSize.height-mySize.height)/2
		);
		
		updateOkBtn();
	}

	private void updateOkBtn() {
		btnOk.setEnabled( useKnownDLC!=null && ( (useKnownDLC && selectedKnownDLC!=null) || (!useKnownDLC && selectedNewDLC!=null) ) );
	}

	public boolean showDialog() {
		setVisible(true);
		return assignmentsChanged;
	}

	static void saveData(HashMap<String, String> assignments) {
		File file = new File(SnowRunner.TruckToDLCAssignmentsFile);
		System.out.printf("Write TruckToDLCAssignments to file ...%n   \"%s\"%n", file.getAbsolutePath());
		
		HashMap<String,Vector<String>> reversedMap = new HashMap<>();
		assignments.forEach((truck,dlc)->{
			Vector<String> trucks = reversedMap.get(dlc);
			if (trucks==null) reversedMap.put(dlc, trucks = new Vector<>());
			trucks.add(truck);
		});
		
		reversedMap.forEach((dlc,trucks)->trucks.sort(null));
		
		Vector<String> dlcs = new Vector<>(reversedMap.keySet());
		dlcs.sort(null);
		
		try (PrintWriter out = new PrintWriter( new OutputStreamWriter( new FileOutputStream( file ), StandardCharsets.UTF_8) )) {
			
			for (String dlc:dlcs) {
				out.printf("[DLC]%n");
				out.printf("name = %s%n", dlc);
				Vector<String> trucks = reversedMap.get(dlc);
				for (String truck:trucks)
					out.printf("truck = %s%n", truck);
				out.println();
			}
			
			
		} catch (FileNotFoundException e) {
		}
		
		System.out.printf("... done%n");
	}

	static HashMap<String, String> loadStoredData() {
		File file = new File(SnowRunner.TruckToDLCAssignmentsFile);
		System.out.printf("Read TruckToDLCAssignments from file ...%n   \"%s\"%n", file.getAbsolutePath());
		
		HashMap<String, String> storedData = new HashMap<>();
		
		try (BufferedReader in = new BufferedReader( new InputStreamReader( new FileInputStream( file ), StandardCharsets.UTF_8) )) {
			
			String line, value, lastDLC=null;
			while ( (line=in.readLine())!=null ) {
				
				if (line.equals("[DLC]"))
					lastDLC = null;
				
				if ( (value = Data.getLineValue(line, "name = "))!=null )
					lastDLC = value;
				
				if ( (value = Data.getLineValue(line, "truck = "))!=null && lastDLC!=null)
					storedData.put(value, lastDLC);
			}
			
			
		} catch (FileNotFoundException e) {
			//e.printStackTrace();
		} catch (IOException e) {
			System.err.printf("IOException while reading TruckToDLCAssignments: %s%n", e.getMessage());
			//e.printStackTrace();
		}
		
		System.out.printf("... done%n");
		return storedData;
	}
}
