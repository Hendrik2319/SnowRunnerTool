package net.schwarzbaer.java.games.snowrunner;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;

class TruckAssignToDLCDialog extends JDialog {
	private static final long serialVersionUID = 5851218628292882974L;

	private enum ActionCommands { OkBtn, RdbtnKnownDLC, RdbtnNewDLC }
	
	private Disabler<ActionCommands> disabler;
	
	TruckAssignToDLCDialog(Window owner, Truck truck, Language language) {
		super(owner,ModalityType.APPLICATION_MODAL);
		
		disabler = new Disabler<>();
		disabler.setCareFor(ActionCommands.values());
		
		JPanel centerPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 0;
		
		ButtonGroup bg = new ButtonGroup();
		c.weightx = 0;
		c.gridx = 0;
		c.gridy = 0;
		centerPanel.add(SnowRunner.createRadioButton("Known DLC"       , bg, true, false, disabler, ActionCommands.RdbtnKnownDLC, e->{
			// TODO
		}),c);
		c.gridy = 1;
		centerPanel.add(SnowRunner.createRadioButton("Define a new DLC", bg, true, false, disabler, ActionCommands.RdbtnNewDLC  , e->{
			// TODO
		}),c);
		
		c.weightx = 1;
		c.gridx = 1;
		c.gridy = 0;
		centerPanel.add(new JComboBox<String>(),c); // TODO
		c.gridy = 1;
		centerPanel.add(new JTextField(20),c); // TODO
		
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.weightx = 1;
		buttonPanel.add(new JLabel(),c);
		c.weightx = 0;
		buttonPanel.add(SnowRunner.createButton("Ok", false, disabler, ActionCommands.OkBtn, e->{
			// TODO
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
		
		updateGuiAccess();
	}

	private void updateGuiAccess() {
		disabler.setEnable(ac->{
			switch (ac) {
			case OkBtn:
				break;
			case RdbtnKnownDLC:
				break;
			case RdbtnNewDLC:
				break;
			}
			return null;
		});
	}

	public void showDialog() {
		setVisible(true);
		// TODO Auto-generated method stub
		
	}
}
