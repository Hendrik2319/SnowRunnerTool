package net.schwarzbaer.java.games.snowrunner;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Window;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import net.schwarzbaer.java.lib.gui.Disabler;

class DataSelectDialog extends JDialog {
	private static final long serialVersionUID = 5535879419617093256L;
	
	private final Disabler<DataSelectDialog.ActionCommands> disabler;

	private Boolean defineFullPath;
	private Boolean selectSteamLibrary;
	private File result;

	private final JFileChooser fileChooser;


	private enum ActionCommands { GameFolderLabel, GameFolderRB, OkBtn }
	
	DataSelectDialog(Window owner) {
		super(owner, "", ModalityType.APPLICATION_MODAL);
		defineFullPath = null;
		selectSteamLibrary = null;
		result = null;
		
		fileChooser = new JFileChooser("./");
		fileChooser.setMultiSelectionEnabled(false);
		
		disabler = new Disabler<>();
		disabler.setCareFor(ActionCommands.values());
		
		JPanel centerPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.gridwidth = GridBagConstraints.REMAINDER;
		centerPanel.add(new JLabel("How do you want to define location of \"initial.pak\"?"),c);
		
		ButtonGroup bg0 = new ButtonGroup();
		centerPanel.add(SnowRunner.createRadioButton("full path to \"initial.pak\"",bg0,true,false,e->{ defineFullPath = true ; updateGuiAccess(); }),c);
		centerPanel.add(SnowRunner.createRadioButton("via game installation folder",bg0,true,false,e->{ defineFullPath = false; updateGuiAccess(); }),c);

		c.gridwidth = 1;
		String spacer = "          ";
		centerPanel.add(new JLabel(spacer),c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		centerPanel.add(SnowRunner.createLabel("How is your game installed?", disabler, ActionCommands.GameFolderLabel),c);

		ButtonGroup bg1 = new ButtonGroup();
		c.gridwidth = 1;
		centerPanel.add(new JLabel(spacer),c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		centerPanel.add(SnowRunner.createRadioButton("direct installation (e.g. in \"Program Files\") --> select installation folder",bg1,true,false,
				disabler, ActionCommands.GameFolderRB,
				e->{ selectSteamLibrary = false; updateGuiAccess(); }),c);
		
		c.gridwidth = 1;
		centerPanel.add(new JLabel(spacer),c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		centerPanel.add(SnowRunner.createRadioButton("via Steam platform --> select Steam library where game is installed into",bg1,true,false,
				disabler, ActionCommands.GameFolderRB,
				e->{ selectSteamLibrary = true; updateGuiAccess(); }),c);
		
		c.gridwidth = 1;
		centerPanel.add(new JLabel(spacer),c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		centerPanel.add(SnowRunner.createRadioButton("via another gaming platform --> select installation folder",bg1,true,false,
				disabler, ActionCommands.GameFolderRB,
				e->{ selectSteamLibrary = false; updateGuiAccess(); }),c);
		
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.weightx = 1;
		buttonPanel.add(new JLabel(),c);
		c.weightx = 0;
		buttonPanel.add(SnowRunner.createButton("Ok", false, disabler, ActionCommands.OkBtn, e->{
			fileChooser.resetChoosableFileFilters();
			
			if (defineFullPath) {
				fileChooser.setDialogTitle("Select initial.pak");
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fileChooser.addChoosableFileFilter(new FileFilter() {
					@Override public String getDescription() { return "initial.pak only"; }
					@Override public boolean accept(File file) { return !file.isFile() || file.getName().equalsIgnoreCase("initial.pak"); }
				});
				
				if (fileChooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)
					return;
				
				File file = fileChooser.getSelectedFile();
				if (!file.isFile()) {
					String msg = "Selected file isn't a file or doesn't exist.";
					JOptionPane.showMessageDialog(this, msg, "Wrong file", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				result = file;
				
			} else if (selectSteamLibrary) {
				fileChooser.setDialogTitle("Select Steam Library");
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				
				if (fileChooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)
					return;
				
				File folder = fileChooser.getSelectedFile();
				if (!folder.isDirectory()) {
					String msg = "Selected folder isn't a folder or doesn't exist.";
					JOptionPane.showMessageDialog(this, msg, "Wrong folder", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				File file = new File(folder,"steamapps/common/SnowRunner/preload/paks/client/initial.pak");
				if (!file.isFile()) {
					String msg = String.format("Can't find \"initial.pak\" at expected location:%n\"%s\"", file.getAbsolutePath());
					JOptionPane.showMessageDialog(this, msg, "Can't find file", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				result = file;
				
			} else {
				fileChooser.setDialogTitle("Select game folder");
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				
				if (fileChooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)
					return;
				
				File folder = fileChooser.getSelectedFile();
				if (!folder.isDirectory()) {
					String msg = "Selected folder isn't a folder or doesn't exist.";
					JOptionPane.showMessageDialog(this, msg, "Wrong folder", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				File file = new File(folder,"preload/paks/client/initial.pak");
				if (!file.isFile()) {
					String msg = String.format("Can't find \"initial.pak\" at expected location:%n\"%s\"", file.getAbsolutePath());
					JOptionPane.showMessageDialog(this, msg, "Can't find file", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				result = file;
			}
			
			setVisible(false);
		}),c);
		buttonPanel.add(SnowRunner.createButton("Cancel", true, e->setVisible(false)),c);
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		contentPane.add(centerPanel,BorderLayout.CENTER);
		contentPane.add(buttonPanel,BorderLayout.SOUTH);
		
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
			case GameFolderLabel:
			case GameFolderRB:
				return defineFullPath!=null && !defineFullPath; 
				
			case OkBtn:
				return defineFullPath!=null && (defineFullPath || selectSteamLibrary!=null); 
			}
			return null;
		});
	}

	public File showDialog() {
		setVisible(true);
		return result;
	}
	
	
	
}