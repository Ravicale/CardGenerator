package gui;

import application.LTags;
import com.formdev.flatlaf.FlatDarkLaf;
import database.card.CardCount;
import gui.panes.CardFilterPane;
import gui.panes.CardInfoPane;
import gui.panes.CardListPane;
import gui.panes.MenuBar;
import gui.panes.models.CardTableModel;
import org.tinylog.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class Gui {
	private static CardListPane cardListGui;
	private static CardInfoPane cardInfoGui;
	private static CardFilterPane cardFilterGui;
	private static MenuBar menuBar;
	private static JFrame frame;

	private static final CountDownLatch guiAvailable = new CountDownLatch(1);

	private Gui() {
	}

	public static void init() {
		Logger.tag(LTags.UI_SYNC.tag).info("Initializing GUI.");
		FlatDarkLaf.setup();
		SwingUtilities.invokeLater(() -> {
			cardFilterGui = new CardFilterPane();
			cardListGui = new CardListPane();
			cardInfoGui = new CardInfoPane();
			frame = new JFrame("MTG Deck Buddy") {
				//Workaround for silly ass java bug.
				//TLDR: setMinimumSize doesn't respect DPI, everything else does.
				//setMinimumSize forces a window resize, leading to mega-big starting windows.
				//https://bugs.openjdk.org/browse/JDK-8305653
				//https://bugs.openjdk.org/browse/JDK-8221452
				private boolean ignoreResizes = true;

				@Override
				public void setMinimumSize(Dimension dim) {
					super.setMinimumSize(dim);
					ignoreResizes = false;
				}

				@Override
				public void setSize(int width, int height) {
					if (ignoreResizes) {
						return;
					}
					super.setSize(width, height);
				}
			};
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			GroupLayout layout = new GroupLayout(frame.getContentPane());
			frame.getContentPane().setLayout(layout);

			layout.setHorizontalGroup(layout.createSequentialGroup()
					.addComponent(cardListGui)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
							.addComponent(cardInfoGui)
							.addComponent(cardFilterGui)
					)
			);

			layout.setVerticalGroup(layout.createParallelGroup()
					.addComponent(cardListGui)
					.addGroup(layout.createSequentialGroup()
							.addComponent(cardInfoGui)
							.addComponent(cardFilterGui)
					)
			);

			menuBar = new MenuBar();
			frame.setJMenuBar(menuBar);
			frame.pack();

			//See frame class overrides.
			GraphicsConfiguration gfxEnv = GraphicsEnvironment.getLocalGraphicsEnvironment()
					.getDefaultScreenDevice().getDefaultConfiguration();
			AffineTransform dpiScale = gfxEnv.getDefaultTransform();
			double scaleX = dpiScale.getScaleX();
			double scaleY = dpiScale.getScaleY();
			Dimension windowSizeDPIScaled = (Dimension) frame.getSize().clone();
			windowSizeDPIScaled.height = (int)((double)windowSizeDPIScaled.height * scaleY);
			windowSizeDPIScaled.width = (int)((double)windowSizeDPIScaled.width * scaleX);

			frame.setMinimumSize(windowSizeDPIScaled);
			frame.setVisible(true);
			guiAvailable.countDown();
			Logger.tag(LTags.UI_SYNC.tag).info("GUI Initialized.");
		});
	}

	public static void setBusyLoading(boolean isBusy) {
		SwingUtilities.invokeLater(() -> {
			Logger.tag(LTags.UI_SYNC.tag).debug("Setting loading animation to {}.", isBusy);
			if (guiAvailable.getCount() == 0) {
				cardListGui.setBusy(isBusy);
			} else {
				Logger.tag(LTags.UI_SYNC.tag).info("Unable to update loading animation because GUI was not yet initialized, trying again.");
				setBusyLoading(isBusy);
			}
		});
	}

	public static void rebuildCardList(Set<String> packNames) {
		SwingUtilities.invokeLater(() -> {
			Logger.tag(LTags.UI_SYNC.tag).debug("Initializing card list table.");
			if (guiAvailable.getCount() == 0) {
				cardListGui.initTable();
				cardFilterGui.filter(null);
				menuBar.setPacks(packNames);
			} else {
				Logger.tag(LTags.UI_SYNC.tag).info("Unable to update card table because GUI was not yet initialized, trying again.");
				rebuildCardList(packNames);
			}
		});
	}

	public static void resetCardList() {
		if (guiAvailable.getCount() == 0) {
			SwingUtilities.invokeLater(() -> {
				cardListGui.clearTable();
				cardInfoGui.setCard(-1, CardCount.INVALID,  null);
				menuBar.setDeckSizeCounter(0);
				menuBar.setPacks(Collections.emptySet());
			});
		}
	}

	public static void setSelectedCard(int id, CardCount copies, Icon image) {
		SwingUtilities.invokeLater(() -> cardInfoGui.setCard(id, copies, image));
	}

	public static void setCardFilter(RowFilter<CardTableModel, Integer> filter) {
		SwingUtilities.invokeLater(() -> cardListGui.setFilter(filter));
	}

	public static void updateCardInfo(int id, CardCount deckCopyState) {
		SwingUtilities.invokeLater(() -> {
			cardListGui.updateRow(id);
			cardInfoGui.updateCard(id, deckCopyState);
			cardFilterGui.filter(null);
		});
	}

	public static void updateDeck(int cardId, CardCount copies, int deckSize) {
		SwingUtilities.invokeLater(() -> {
			updateCardInfo(cardId, copies);
			menuBar.setDeckSizeCounter(deckSize);
		});
	}

	public static void displayPopup(String text) {
		SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(getFrame(), text));
	}

	public static JFrame getFrame() {
		if (!SwingUtilities.isEventDispatchThread()) {
			try {
				guiAvailable.await();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		return frame;
	}

	public static void clearDeck() {
		SwingUtilities.invokeLater(() -> {
			cardListGui.updateTable();
			cardInfoGui.updateCard(CardCount.NONE);
		});
	}
}
