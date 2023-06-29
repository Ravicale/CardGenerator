package gui.panes.models;

import javax.swing.table.TableRowSorter;
import java.util.Comparator;

public class CardTableSorter extends TableRowSorter<CardTableModel> {
	@Override
	public Comparator<?> getComparator(int column) {
		return (a, b) -> ((Comparable) a).compareTo(b);
	}

	@Override
	protected boolean useToString(int column) {
		return false;
	}
}
