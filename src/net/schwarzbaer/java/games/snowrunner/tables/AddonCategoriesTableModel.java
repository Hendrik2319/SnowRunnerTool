package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Window;
import java.util.Comparator;

import net.schwarzbaer.java.games.snowrunner.Data.AddonCategories;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.GlobalFinalDataStructures;

public class AddonCategoriesTableModel extends VerySimpleTableModel<AddonCategories.Category> {

	public AddonCategoriesTableModel(Window mainWindow, GlobalFinalDataStructures gfds) {
		super(mainWindow, gfds, new ColumnID[] {
				new ColumnID("ID"         ,"ID"                          ,  String.class, 100, null, null, false, row->((AddonCategories.Category)row).name),
				new ColumnID("Label"      ,"Label"                       ,  String.class, 200, null, null,  true, row->((AddonCategories.Category)row).label_StringID),
				new ColumnID("Icon"       ,"Icon"                        ,  String.class, 130, null, null, false, row->((AddonCategories.Category)row).icon),
				new ColumnID("RequiresOAI","Requires One Addon Installed", Boolean.class, 160, null, null, false, row->((AddonCategories.Category)row).requiresOneAddonInstalled),
		});
		connectToGlobalData(data->data.addonCategories.categories.values());
		setInitialRowOrder(Comparator.<AddonCategories.Category,String>comparing(cat->cat.name));
	}

	@Override protected String getRowName(AddonCategories.Category row)
	{
		return SnowRunner.solveStringID(row, language);
	}
}