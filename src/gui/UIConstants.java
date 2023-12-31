package gui;

import database.image.ImgStore;

import javax.swing.*;
import java.awt.*;

public class UIConstants {
	public static int MARGIN = 10;

	//Card Info Pane
	public static final Dimension CARD_INFO_PANE_SIZE = new Dimension(320, 420);
	public static final Dimension CARD_IMAGE_SIZE = new Dimension(288, 408);
	public static final ImageIcon DEFAULT_CARD_ICON = ImgStore.getLocalIcon("images/defaultcardback.png", CARD_IMAGE_SIZE);
	public static final Dimension CARD_BUTTON_SIZE = new Dimension(125, 50);

	//Card Filter Pane
	public static final Dimension CARD_FILTER_PANE_SIZE = new Dimension(320, 140);
	public static final Dimension COLOR_BUTTON_SIZE = new Dimension(42, 42);
	public static final Insets SEARCH_BOX_MARGIN = new Insets(0, 0, 0, 0);

	//Card List Pane
	public static final int[] DEFAULT_COLUMN_SIZES = {
			200,
			125,
			50,
			50,
			50,
			50,
			50,
			50,
			50,
			50,
			50
	};

	public static final int[] MAX_COLUMN_SIZES = {
			Integer.MAX_VALUE,
			125,
			125,
			125,
			125,
			125,
			125,
			125,
			125,
			125,
			125
	};

	public static final Dimension CARD_LIST_PANE_MIN_SIZE;
	static {
		int ySize = 100;
		int xSize = 40;
		for (int columnSize : DEFAULT_COLUMN_SIZES) {
			xSize += columnSize;
		}
		CARD_LIST_PANE_MIN_SIZE = new Dimension(xSize, ySize);
	}

	static {
		UIManager.put("ScrollBar.trackArc", 3);
		UIManager.put("ScrollBar.showButtons", true);
		UIManager.put("ScrollBar.pressedThumbWithTrack", true);
		UIManager.put("ScrollBar.minimumThumbSize", new Dimension(30, 30));
		UIManager.put("ScrollBar.width", new Dimension(20, 20));
	}
}
