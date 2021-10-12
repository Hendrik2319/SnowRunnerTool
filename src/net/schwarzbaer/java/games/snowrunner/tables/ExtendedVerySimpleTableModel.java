package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Window;

import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;
import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier.TextAreaOutputSource;

public abstract class ExtendedVerySimpleTableModel<RowType> extends VerySimpleTableModel<RowType> implements TextAreaOutputSource {
	
	private Runnable textAreaUpdateMethod;

	ExtendedVerySimpleTableModel(Window mainWindow, Controllers controllers, ColumnID[] columns) {
		super(mainWindow, controllers, columns);
		textAreaUpdateMethod = null;
	}

	@Override protected void extraUpdate() {
		updateTextArea();
	}

	void updateTextArea() {
		if (textAreaUpdateMethod!=null)
			textAreaUpdateMethod.run();
	}

	@Override public void setOutputUpdateMethod(Runnable textAreaUpdateMethod) {
		this.textAreaUpdateMethod = textAreaUpdateMethod;
	}
	
	@Override public String getTextForRow(int rowIndex) {
		RowType row = getRow(rowIndex);
		if (row==null) return "";
		return getTextForRow(row);
	}

	protected abstract String getTextForRow(RowType row);
}