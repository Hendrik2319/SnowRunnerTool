package net.schwarzbaer.java.games.snowrunner;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
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

import net.schwarzbaer.java.games.snowrunner.SnowRunner.DLCs;

public class AssignToDLCDialog extends JDialog {
	private static final long serialVersionUID = 5851218628292882974L;

	private Boolean useKnownDLC;
	private String selectedKnownDLC;
	private String selectedNewDLC;
	private final JButton btnOk;
	private boolean assignmentsChanged;

	public AssignToDLCDialog(Window owner, DLCs.ItemType type, String id, String label, DLCs dlcs) {
		super(owner,ModalityType.APPLICATION_MODAL);
		useKnownDLC = null;
		selectedNewDLC = null;
		assignmentsChanged = false;
		
		if (id==null) {
			System.err.printf("Selected %s has no ID. You should cancel this dialog now.", type);
			selectedKnownDLC = null;
		} else {
			selectedKnownDLC = dlcs.getDLC(id, type);
		}
		
		// determine known DLCs
		Vector<String> knownDLCs = dlcs.getAllDLCs();
		
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
			if (dlc!=null && id!=null) {
				dlcs.setDLC(id, type, dlc);
				dlcs.saveData();
				assignmentsChanged = true;
			}
			setVisible(false);
		}),c);
		buttonPanel.add(SnowRunner.createButton("Cancel", true, e->setVisible(false)),c);
		
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		contentPane.add(centerPanel,BorderLayout.CENTER);
		contentPane.add(buttonPanel,BorderLayout.SOUTH);
		
		setTitle(String.format("Assign \"%s\" to an official DLC", label));
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
}
