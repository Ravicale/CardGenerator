package gui.panes;

import application.LTags;
import database.card.Card;
import database.card.CardDatabase;
import gui.UIConstants;
import gui.panes.models.CardTableFilter;
import gui.panes.models.CardTableModel;
import gui.panes.models.CardTableSorter;
import org.tinylog.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.Arrays;

public class CardListPane extends JPanel {
	private final JTable cardTable;
	private final JProgressBar busyIndicator;
	private CardTableSorter sorter;
	private CardTableModel model;
	private RowFilter<CardTableModel, Integer> cachedFilter;

	private static final Object[][] defaultTable;

	static {
		defaultTable = new Object[1][Card.DATA_FIELD_NAMES.length];
		Arrays.fill(defaultTable[0], "");
	}

	public CardListPane() {
		cardTable = new JTable(defaultTable, Card.DATA_FIELD_NAMES);
		cardTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		cardTable.setAutoCreateColumnsFromModel(false);
		cardTable.setEnabled(false);
		cardTable.setShowHorizontalLines(true);
		cardTable.setMinimumSize(UIConstants.CARD_LIST_PANE_MIN_SIZE);
		cardTable.setDefaultRenderer(Object.class, new Renderer());
		setMinimumSize(UIConstants.CARD_LIST_PANE_MIN_SIZE);
		JTableHeader header = cardTable.getTableHeader();
		header.setReorderingAllowed(true);
		header.setResizingAllowed(false);
		JScrollPane tableScrollPane = new JScrollPane(cardTable);

		TableColumnModel columnModel = cardTable.getColumnModel();
		for (int i = 0; i < columnModel.getColumnCount(); i++) {
			TableColumn column = columnModel.getColumn(i);
			column.setMinWidth(UIConstants.DEFAULT_COLUMN_SIZES[i]);
			column.setMaxWidth(UIConstants.MAX_COLUMN_SIZES[i]);
		}

		busyIndicator = new JProgressBar();
		busyIndicator.setOrientation(JProgressBar.HORIZONTAL);
		busyIndicator.setBorderPainted(false);
		busyIndicator.setVisible(false);
		busyIndicator.setIndeterminate(true);

		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addGap(UIConstants.MARGIN)
				.addComponent(tableScrollPane)
				.addComponent(busyIndicator)
				.addGap(UIConstants.MARGIN)
		);

		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addGap(UIConstants.MARGIN)
				.addGroup(layout.createParallelGroup()
						.addComponent(tableScrollPane)
						.addComponent(busyIndicator)
				)
				.addGap(UIConstants.MARGIN)
		);

		cardTable.getSelectionModel().addListSelectionListener((e) -> {
			int selectedIndex = cardTable.getSelectedRow();
			if (selectedIndex >= 0 && selectedIndex < cardTable.getRowCount()) {
				selectedIndex = cardTable.convertRowIndexToModel(selectedIndex);
				Card card = CardDatabase.getCard(selectedIndex);
				CardDatabase.loadAndDisplayImage(card.id);
			}
		});
	}

	public void clearTable() {
		cardTable.setEnabled(false);
	}

	public void initTable() {
		model = new CardTableModel();
		sorter = new CardTableSorter();
		sorter.setModel(model);
		if (cachedFilter != null) {
			sorter.setRowFilter(cachedFilter);
		} else {
			CardTableFilter.setFilterList(null);
		}
		cachedFilter = null;
		cardTable.setRowSorter(sorter);
		cardTable.setModel(model);
		cardTable.setEnabled(true);
	}

	public void setBusy(boolean isBusy) {
		if (!busyIndicator.isIndeterminate()) {
			busyIndicator.setIndeterminate(true);
		}
		busyIndicator.setVisible(isBusy);
	}

	public void setFilter(RowFilter<CardTableModel, Integer> filter) {
		if (sorter == null) {
			cachedFilter = filter;
		} else {
			sorter.setRowFilter(filter);
		}
	}

	public void updateRow(int id) {
		if (model != null) {
			Logger.tag(LTags.UI_UPDATES.tag).debug("Updating table row for card #{}.", id);
			model.fireTableRowsUpdated(id, id);
		} else {
			Logger.tag(LTags.UI_UPDATES.tag).error("Attempted to update a card row when the model has not been fully initialized.");
		}
	}

	public void updateTable() {
		if (model != null) {
			Logger.tag(LTags.UI_UPDATES.tag).debug("Updating table for all cards.");
			model.fireTableDataChanged();
		} else {
			Logger.tag(LTags.UI_UPDATES.tag).error("Attempted to update a the card table when the model has not been fully initialized.");
		}
	}

	private static class Renderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			c.setVisible(!value.equals(Card.BLANK_INT));
			return c;
		}
	}
}
