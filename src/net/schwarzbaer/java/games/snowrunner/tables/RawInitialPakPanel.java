package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.schwarzbaer.java.games.snowrunner.Data;
import net.schwarzbaer.java.games.snowrunner.PAKReader.ZipEntryTreeNode;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.lib.gui.ContextMenu;
import net.schwarzbaer.java.lib.gui.HexViewPanel;
import net.schwarzbaer.java.lib.gui.ScrollPosition;
import net.schwarzbaer.java.lib.system.ClipboardTools;

public class RawInitialPakPanel extends JSplitPane
{
	private static final long serialVersionUID = 5402984264832650566L;
	
	private final JTree tree;
	private final JScrollPane treeScrollPane;
	private final ViewsPanel viewsPanel;
	private TreePath selectedTreePath;
	private InitialPakTreeNode selectedTreeNode;

	public RawInitialPakPanel()
	{
		super(JSplitPane.HORIZONTAL_SPLIT, true);
		selectedTreePath = null;
		selectedTreeNode = null;
		
		tree = new JTree();
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setCellRenderer(new InitialPakTreeCellRenderer());
		treeScrollPane = new JScrollPane(tree);
		treeScrollPane.setPreferredSize(new Dimension(400,50));
		
		TreeContextMenu treeContextMenu = new TreeContextMenu(tree);
		treeContextMenu.addTo(tree);
		treeContextMenu.addTo(treeScrollPane);
		
		viewsPanel = new ViewsPanel();
		
		setLeftComponent(treeScrollPane);
		setRightComponent(viewsPanel);
		
		tree.addTreeSelectionListener(ev -> {
			selectedTreePath = tree.getSelectionPath();
			Object obj = selectedTreePath==null ? null : selectedTreePath.getLastPathComponent();
			selectedTreeNode = null;
			if (obj instanceof InitialPakTreeNode treeNode)
				selectedTreeNode = treeNode;
			updateViewsPanel("TreeSelection");
		});
	}
	
	private void updateViewsPanel(String caller)
	{
		//System.err.printf("[%016X] updateViewsPanel( %s )%n", System.currentTimeMillis(), caller);
		viewsPanel.setData(selectedTreeNode);
	}

	public void setData(Data data)
	{
		InitialPakTreeNode root = data==null ? null : new InitialPakTreeNode(data.rawdata.zipRoot);
		tree.setModel(new DefaultTreeModel(root));
		updateViewsPanel("SetData");
	}
	
	private static class TreeContextMenu extends ContextMenu
	{
		private static final long serialVersionUID = 57798470826433148L;
		
		private TreePath clickedTreePath;
		private InitialPakTreeNode clickedTreeNode;

		TreeContextMenu(JTree tree)
		{
			clickedTreePath = null;
			clickedTreeNode = null;
			
			JMenuItem miCopyName = add(SnowRunner.createMenuItem("Copy Name", true, ev -> {
				if (clickedTreeNode!=null)
					ClipboardTools.copyToClipBoard(clickedTreeNode.toString());
			}));
			
			JMenuItem miCopyPath = add(SnowRunner.createMenuItem("Copy Path", true, ev -> {
				if (clickedTreeNode!=null)
					ClipboardTools.copyToClipBoard(clickedTreeNode.zipEntryTreeNode.path);
			}));
			
			addContextMenuInvokeListener((comp,x,y) -> {
				clickedTreePath = null;
				clickedTreeNode = null;
				if (comp==tree)
				{
					clickedTreePath = tree.getPathForLocation(x, y);
					Object obj = clickedTreePath==null ? null : clickedTreePath.getLastPathComponent();
					if (obj instanceof InitialPakTreeNode treeNode)
						clickedTreeNode = treeNode;
				}
				
				miCopyName.setEnabled(clickedTreeNode!=null);
				miCopyPath.setEnabled(clickedTreeNode!=null);
			});
		}
	}
	
	private static class TextAreaContextMenu extends ContextMenu
	{
		private static final long serialVersionUID = 1131394928833020029L;
		
		TextAreaContextMenu(JTextArea textArea)
		{
			add(SnowRunner.createCheckBoxMenuItem("Line Wrap", textArea.getLineWrap(), null, true, textArea::setLineWrap));
		}
	}
	
	private static class ViewsPanel extends JTabbedPane
	{
		private static final long serialVersionUID = 110727912514186819L;
		
		private final HexViewPanel hexViewPanel;
		private final JTextArea textArea;
		private final JScrollPane textAreaScrollPane;
		
		ViewsPanel()
		{
			hexViewPanel = new HexViewPanel(48, new HexViewPanel.PageSizeStorage<>(SnowRunner.settings, SnowRunner.AppSettings.ValueKey.RawInitialPakPanel_HexViewPageSize));
			
			textArea = new JTextArea();
			textAreaScrollPane = new JScrollPane(textArea);
			
			TextAreaContextMenu contextMenu = new TextAreaContextMenu(textArea);
			contextMenu.addTo(textAreaScrollPane);
			contextMenu.addTo(textArea, () -> ContextMenu.computeSurrogateMousePos(textAreaScrollPane,10,10));
			
			addTab("Raw Bytes", hexViewPanel);
			addTab("Text Content", textAreaScrollPane);
		}

		void setData(InitialPakTreeNode treeNode)
		{
			hexViewPanel.setData(treeNode==null ? null : treeNode.zipEntryTreeNode.rawBytes);
			
			if (treeNode!=null && treeNode.zipEntryTreeNode.hasTextContent())
			{
				ScrollPosition.keepScrollPos(textAreaScrollPane, ScrollPosition.ScrollBarType.Vertical, () -> {
					textArea.setText(treeNode.zipEntryTreeNode.getTextContent());
				});
			}
			else
				ScrollPosition.keepScrollPos(textAreaScrollPane, ScrollPosition.ScrollBarType.Vertical, () -> {
					textArea.setText("<Nothing parsed>");
				});
		}
	}
	
	private static class InitialPakTreeCellRenderer extends DefaultTreeCellRenderer
	{
		private static final long serialVersionUID = 5077995187689322361L;
		private static final Color TEXTCOLOR__PARTIALLY_PARSED = new Color(0xC000C0);
		private static final Color TEXTCOLOR__NOT_PARSED       = new Color(0xFF00FF);

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean isExpanded, boolean isLeaf, int rowIndex, boolean hasFocus)
		{
			Component rendComp = super.getTreeCellRendererComponent(tree, value, isSelected, isExpanded, isLeaf, rowIndex, hasFocus);
			
			if (rendComp==this && !isSelected && value instanceof InitialPakTreeNode treeNode)
			{
				switch (treeNode.parsedState)
				{
				case Full     : setForeground(tree.getForeground()); break;
				case Not      : setForeground(TEXTCOLOR__NOT_PARSED); break;
				case Partially: setForeground(TEXTCOLOR__PARTIALLY_PARSED); break;
				}
			}
			
			return rendComp;
		}
	}
	
	private enum ParsedState { Full, Partially, Not }
	
	private static class InitialPakTreeNode implements TreeNode
	{
		private final InitialPakTreeNode parent;
		private final ZipEntryTreeNode zipEntryTreeNode;
		private final Vector<InitialPakTreeNode> children;
		private final ParsedState parsedState;

		InitialPakTreeNode(ZipEntryTreeNode zipRoot)
		{
			this(null, zipRoot);
		}
		
		InitialPakTreeNode(InitialPakTreeNode parent, ZipEntryTreeNode zipEntryTreeNode)
		{
			this.parent = parent;
			this.zipEntryTreeNode = Objects.requireNonNull( zipEntryTreeNode );
			children = new Vector<>();
			addChildren(this, this.zipEntryTreeNode.folders, children);
			addChildren(this, this.zipEntryTreeNode.files  , children);
			
			if (children.isEmpty())
			{
				if (zipEntryTreeNode.wasParsed())
					parsedState = ParsedState.Full;
				else
					parsedState = ParsedState.Not;
			}
			else
			{
				boolean isFull = true;
				boolean isNot  = true;
				for (InitialPakTreeNode childNode : children)
				{
					isFull = isFull && childNode.parsedState==ParsedState.Full;
					isNot  = isNot  && childNode.parsedState==ParsedState.Not ;
				}
				if (isFull)
					parsedState = ParsedState.Full;
				else if (isNot)
					parsedState = ParsedState.Not;
				else
					parsedState = ParsedState.Partially;
			}
		}

		@Override
		public String toString()
		{
			return zipEntryTreeNode.name;
		}

		private static void addChildren(InitialPakTreeNode parent, HashMap<String, ZipEntryTreeNode> map, List<InitialPakTreeNode> list)
		{
			if (map==null) return;
			List<String> keys = map.keySet().stream().sorted().toList();
			for (String key : keys)
			{
				ZipEntryTreeNode node = map.get(key);
				list.add(new InitialPakTreeNode(parent, node));
			}
		}

		@Override
		public InitialPakTreeNode getChildAt(int childIndex)
		{
			if (childIndex < 0 || childIndex >= children.size())
				return null;
			return children.get(childIndex);
		}

		@Override public Enumeration<? extends InitialPakTreeNode> children() { return children.elements(); }
		@Override public int getChildCount() { return children.size(); }
		@Override public InitialPakTreeNode getParent() { return parent; }
		@Override public int getIndex(TreeNode node) { return children.indexOf(node); }

		@Override public boolean getAllowsChildren() { return zipEntryTreeNode.folders==null && zipEntryTreeNode.files==null; }
		@Override public boolean isLeaf() { return children.isEmpty(); }
	}
}
