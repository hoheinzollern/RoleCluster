package it.processmining.clustering.ui;

import it.processmining.clustering.hierarchical.HATreeNode;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.DecimalFormat;
import java.util.Vector;

import javax.swing.JComponent;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * This is the widget for the representation of a dendrogram
 * 
 * @author Andrea Burattin
 */
@SuppressWarnings("serial")
public class DendrogramWidget extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener {

	// GRAPHIC CONFIGURATION
	// ---------------------
	// the size of a block of the matrix
	public static int matrixBlockSize = 40;
	// the size of a circle of the dendrogram
	public static final int dendroCircleSize = 5;
	// maximum length of connectors
	//	public static int dendroMaxLineLength = 100;
	public static int dendroWidth = 400;
	// minimum length of connectors
	//	public static final int dendroMinLineLength = 5;
	// colors configuration
	public static final Color background = Color.BLACK;
	public static final Color labelColor = new Color(172, 229, 254, (int) (255 * (.8)));
	public static final Color dendroColor = Color.WHITE;
	public static final Color matrixBrightestColor = Color.RED;
	public static final Color infoBoxBackground = new Color(.05f, .05f, .05f, .9f);
	public static final Color infoBoxLines = new Color(.5f, .5f, .5f, .5f);
	public static final Color infoBoxLabels = new Color(1f, 1f, 1f, 0.5f);
	public static final Color scaleColor = new Color(1f, 1f, 1f, 0.5f);

	public static final Color infoDendrogramBackground = new Color(.1f, .1f, .1f, 1f);
	public static final Color infoDendrogramLines = new Color(.4f, .4f, .4f, 1f);
	public static final Color infoDendrogramLabels = new Color(.6f, .6f, .6f, 1f);
	public static final float infoDendrogramFontSize = 16f;

	// internal elements
	private HATreeNode root;
	private int numberOfElements;
	private Vector<Integer> coordinates;
	private FontMetrics fm;
	private boolean firstPrint = true;

	private int currentX;
	private int currentY;

	private int offsetX = 10;
	private int offsetY = 10;

	private int spaceForLabelX = 100;
	private int spaceForLabelY = 0;

	DecimalFormat df = new DecimalFormat("#.###");

	// mouse listener indexes
	private int mouseMovingX = -1;
	private int mouseMovingY = -1;
	private int matrixBeginX = offsetX + spaceForLabelX;
	private int matrixBeginY = offsetY + spaceForLabelY;
	private boolean mouseOverMatrix = false;
	private boolean mouseOverDendrogram = false;
	private int motionPixels = 0;

	/**
	 * Widget constructor
	 * 
	 * @param dm
	 *            the precalculated distance matrix
	 * @param root
	 *            the root of the cluster
	 */
	public DendrogramWidget(HATreeNode root) {
		this.root = root;
		this.numberOfElements = root.getSize();
		coordinates = new Vector<Integer>();

		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
	}

	/**
	 * Method for getting a new available X coordinates
	 * 
	 * @return
	 */
	public int askForX() {
		return currentX;
	}

	/**
	 * Method for getting a new available Y coordinate
	 * 
	 * @param c
	 * @return
	 */
	public int askForY(HATreeNode c) {
		int index = c.getId();
		if (!coordinates.contains(index)) {
			coordinates.add(index);
		}

		int oldY = currentY;
		currentY += matrixBlockSize;
		return oldY;
	}

	/**
	 * Method to get north point of the distance matrix
	 * 
	 * @return
	 */
	public int getMatrixBorderN() {
		return offsetY + spaceForLabelY;
	}

	/**
	 * Method to get east point of the distance matrix
	 * 
	 * @return
	 */
	public int getMatrixBorderE() {
		return offsetX + spaceForLabelX + (numberOfElements * matrixBlockSize);
	}

	/**
	 * Method to get south point of the distance matrix
	 * 
	 * @return
	 */
	public int getMatrixBorderS() {
		return offsetY + spaceForLabelY + (numberOfElements * matrixBlockSize);
	}

	/**
	 * Method to get west point of the distance matrix
	 * 
	 * @return
	 */
	public int getMatrixBorderW() {
		return offsetX + spaceForLabelX;
	}

	/**
	 * Method to save the given dendrogram representation as an SVG file
	 * 
	 * @param filename
	 *            where to save the image
	 */
	public void getSVG(String filename) {
		DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
		String svgNS = "http://www.w3.org/2000/svg";
		Document document = domImpl.createDocument(svgNS, "svg", null);
		SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
		paintComponent(svgGenerator);

		boolean useCSS = true;
		try {
			svgGenerator.stream(filename, useCSS);
		} catch (SVGGraphics2DIOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (fm == null) {
			fm = g.getFontMetrics();
		}

		// let's center the dendrogram in the window
		if (firstPrint) {
			offsetX = (getWidth() / 2) - (spaceForLabelX / 2) - (matrixBlockSize * numberOfElements / 2)
					- (dendroWidth / 2);
			offsetY = (getHeight() / 2) - (spaceForLabelY / 2) - (matrixBlockSize * numberOfElements / 2);
			matrixBeginX = offsetX + spaceForLabelX;
			matrixBeginY = offsetY + spaceForLabelY;
			firstPrint = false;
		}

		currentX = offsetX + spaceForLabelX + matrixBlockSize * numberOfElements;
		currentY = offsetY + spaceForLabelY + matrixBlockSize / 2;

		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		drawBackground(g, getWidth(), getHeight());
		drawDendrogram(g);
		//drawMatrix(g);

		if (mouseOverDendrogram) {
			drawOverlayDendrogram(g);
		}

		/*if (mouseOverMatrix) {
			drawOverlayMatrix(g);
		}*/

		g.dispose();

	}

	/**
	 * 
	 * @param g
	 */
	private void drawOverlayDendrogram(Graphics g) {
		float defaultFontSize = g.getFont().getSize();
		g.setFont(g.getFont().deriveFont(infoDendrogramFontSize));

		Double value = (double) ((double) (mouseMovingX - getMatrixBorderE()) / dendroWidth);
		String valueString = df.format(value);

		g.setColor(infoDendrogramBackground);
		g.fillRoundRect(mouseMovingX, getMatrixBorderN() - 30, fm.stringWidth(valueString) + 30, 30, 10, 10);

		g.setColor(infoDendrogramLines);
		g.drawLine(mouseMovingX, getMatrixBorderN() - 3, mouseMovingX, getMatrixBorderS());
		g.drawRoundRect(mouseMovingX, getMatrixBorderN() - 30, fm.stringWidth(valueString) + 30, 30, 10, 10);

		g.setColor(infoDendrogramLabels);
		g.drawString(valueString, mouseMovingX + 10, getMatrixBorderN() - 8);
		g.setFont(g.getFont().deriveFont(defaultFontSize));
	}

	/**
	 * 
	 * @param g
	 */
	/*
	private void drawOverlayMatrix(Graphics g) {
		int xCoord = (mouseMovingX - matrixBeginX) / matrixBlockSize;
		int yCoord = (mouseMovingY - matrixBeginY) / matrixBlockSize;

		g.setColor(infoBoxLines);
		// oval on the cell
		g.drawOval(matrixBeginX + (xCoord * matrixBlockSize) + (matrixBlockSize / 2) - 5, matrixBeginY
				+ (yCoord * matrixBlockSize) + (matrixBlockSize / 2) - 5, 10, 10);
		// connector from the matrix border to the oval
		g.drawLine(matrixBeginX, matrixBeginY + (yCoord * matrixBlockSize) + (matrixBlockSize / 2), matrixBeginX
				+ (xCoord * matrixBlockSize) + (matrixBlockSize / 2) - 6, matrixBeginY + (yCoord * matrixBlockSize)
				+ (matrixBlockSize / 2));
		// connector from the oval to the infobox
		g.drawLine(matrixBeginX + (xCoord * matrixBlockSize) + (matrixBlockSize / 2) + 4, matrixBeginY
				+ (yCoord * matrixBlockSize) + (matrixBlockSize / 2) + 4, (int) (matrixBeginX
				+ (xCoord * matrixBlockSize) + (matrixBlockSize * .75) + 2), (int) (matrixBeginY
				+ (yCoord * matrixBlockSize) + (matrixBlockSize * .75) + 2));

		if (xCoord != yCoord) {
			// second oval
			g.drawOval(matrixBeginX + (yCoord * matrixBlockSize) + (matrixBlockSize / 2) - 5, matrixBeginY
					+ (xCoord * matrixBlockSize) + (matrixBlockSize / 2) - 5, 10, 10);
			// connector between the two ovals
			int mult = (xCoord > yCoord) ? -1 : 1;
			g.drawLine(matrixBeginX + (xCoord * matrixBlockSize) + (matrixBlockSize / 2) + (4 * mult), matrixBeginY
					+ (yCoord * matrixBlockSize) + (matrixBlockSize / 2) - (4 * mult), matrixBeginX
					+ (yCoord * matrixBlockSize) + (matrixBlockSize / 2) - (4 * mult), matrixBeginY
					+ (xCoord * matrixBlockSize) + (matrixBlockSize / 2) + (4 * mult));
			// connector from the second oval to the matrix border
			g.drawLine(matrixBeginX, matrixBeginY + (xCoord * matrixBlockSize) + (matrixBlockSize / 2), matrixBeginX
					+ (yCoord * matrixBlockSize) + (matrixBlockSize / 2) - 6, matrixBeginY + (xCoord * matrixBlockSize)
					+ (matrixBlockSize / 2));
		}

		int infoBoxOffset = (xCoord * matrixBlockSize);
		Double dis = dm.getValue(coordinates.get(xCoord), coordinates.get(yCoord));
		String textLine1 = "Similarity: " + df.format(1 - dis);
		String textLine2 = "Distance: " + df.format(dis);
		int infoBoxWidth = fm.stringWidth(textLine1);
		if (fm.stringWidth(textLine2) > infoBoxWidth)
			infoBoxWidth = fm.stringWidth(textLine2);

		// the actual info box
		g.setColor(infoBoxBackground);
		g.fillRoundRect((int) (matrixBeginX + infoBoxOffset + (matrixBlockSize * .75)), (int) (matrixBeginY
				+ (yCoord * matrixBlockSize) + (matrixBlockSize * .75)), infoBoxWidth + 20, 38, 10, 10);
		g.setColor(infoBoxLines);
		g.drawRoundRect((int) (matrixBeginX + infoBoxOffset + (matrixBlockSize * .75)), (int) (matrixBeginY
				+ (yCoord * matrixBlockSize) + (matrixBlockSize * .75)), infoBoxWidth + 20, 38, 10, 10);

		// texts inside the info box
		g.setColor(infoBoxLabels);
		g.drawString(textLine1, (int) (matrixBeginX + infoBoxOffset + (matrixBlockSize * .75) + 10),
				(int) (matrixBeginY + (yCoord * matrixBlockSize) + (matrixBlockSize * .75) + 17));
		g.drawString(textLine2, (int) (matrixBeginX + infoBoxOffset + (matrixBlockSize * .75) + 10),
				(int) (matrixBeginY + (yCoord * matrixBlockSize) + (matrixBlockSize * .75) + 32));
	}*/

	/**
	 * 
	 * @param g
	 */
	/*
	private void drawMatrix(Graphics g) {
		// draw the background
		g.setColor(background);
		g.fillRect(0, 0, spaceForLabelX + offsetX + numberOfElements * matrixBlockSize, spaceForLabelY + offsetY
				+ numberOfElements * matrixBlockSize + 20);

		// draw the matrix
		int x = 0;
		int y = 0;
		for (int a = 0; a < numberOfElements; a++) {
			y = 0;
			for (int b = 0; b < numberOfElements; b++) {
				Double distanceValue = dm.getValue(coordinates.get(a), coordinates.get(b));
				g.setColor(measureColor(distanceValue));
				g.fillRect(getMatrixBorderW() + x, getMatrixBorderN() + y, matrixBlockSize, matrixBlockSize);
				y += matrixBlockSize;
			}
			x += matrixBlockSize;
		}

		// draw the labels
		g.setColor(new Color(172, 229, 254, (int) (255 * (.8))));
		for (int i = 0; i < numberOfElements; i++) {
			String s = dm.getElements().get(coordinates.get(i)).getName();
			int internalOffsetX = spaceForLabelX - fm.stringWidth(s) - 5;
			// horizontal labels
			g.drawString(s, offsetX + internalOffsetX,
					(int) (offsetY + spaceForLabelY + (matrixBlockSize / 2) + 5 + (i * matrixBlockSize)));
			// vertical labels
			//			g2d.rotate(Math.PI*3/2);
			//			g.drawString(s, -(offsetY + internalOffsetY), (int) (offsetX + spaceForLabelX + (matrixBlockSize/2) + (i*matrixBlockSize) + 5));
			//			g2d.rotate(Math.PI/2);
		}
	}
	*/

	/**
	 * 
	 * @param g
	 * @param width
	 * @param height
	 */
	private void drawBackground(Graphics g, int width, int height) {
		g.setColor(background);
		g.fillRect(0, 0, width, height);
	}

	/**
	 * 
	 * @param g
	 */
	private void drawDendrogram(Graphics g) {
		// draw the background
		g.setColor(background);
		g.fillRect(spaceForLabelX + offsetX + numberOfElements * matrixBlockSize, 0, dendroWidth + 10, spaceForLabelY
				+ offsetY + numberOfElements * matrixBlockSize + 20);

		// paint of the dendrogram
		drawDendrogram(root, g);

		// draw the dendrogram scale
		g.setColor(scaleColor);
		g.drawLine(getMatrixBorderE(), getMatrixBorderS(), getMatrixBorderE() + dendroWidth, getMatrixBorderS());
		for (int i = 0; i <= 10; i++) {
			String s = Double.toString(i / 10.);
			g.drawLine(getMatrixBorderE() + (dendroWidth / 10 * i), getMatrixBorderS() + 1, getMatrixBorderE()
					+ (dendroWidth / 10 * i), getMatrixBorderS() + 6);
			g.drawString(s, getMatrixBorderE() + (dendroWidth / 10 * i) - (fm.stringWidth(s) / 2) + 1,
					getMatrixBorderS() + 20);
		}
	}

	/**
	 * This method is recursively used to draw the dendrogram of the clusters
	 * structure
	 * 
	 * @param node
	 *            the current cluster node
	 * @param g
	 *            the graphics where the dendrogram is supposed to be drawn
	 * @return the coordinates of the point connecting the children below the
	 *         current cluster
	 */
	public Point drawDendrogram(HATreeNode node, Graphics g) {

		if (node.isLeaf()) {

			// we are on a leaf, just draw the dot
			int x = askForX(), y = askForY(node);
			String label = node.getName() + " "  + node.getId();

			int width = fm.stringWidth(label);
			g.drawString(label, x - width - 3, y - 3);
			return new Point(x, y);

		} else {

			// we are on the central body of the dendrogram

			HATreeNode leftChild = node.getLeft();
			HATreeNode rightChild = node.getRight();

			Point left = drawDendrogram(leftChild, g);
			Point right = drawDendrogram(rightChild, g);

			g.setColor(DendrogramWidget.dendroColor);

			double maxX = (left.getX() > right.getX()) ? left.getX() : right.getX();
			double minY = (left.getY() < right.getY()) ? left.getY() : right.getY();
			double gapY = Math.abs(left.getY() - right.getY());

			// fill the gaps
			if (left.getX() < maxX) {
				g.drawLine((int) left.getX(), (int) left.getY(), (int) maxX - 1, (int) left.getY());
				left.setLocation(maxX, left.getY());
			}
			if (right.getX() < maxX) {
				g.drawLine((int) right.getX(), (int) right.getY(), (int) maxX - 1, (int) right.getY());
				right.setLocation(maxX, right.getY());
			}

			// calculate the length of the line, proportional to the distance of the cluster
			double clusterDistance = node.getMaxDistance() / root.getMaxDistance();
			double lineLength = getMatrixBorderE() + (int) (DendrogramWidget.dendroWidth * clusterDistance) - maxX;

			// draw the three lines
			g.drawLine((int) left.getX(), (int) left.getY(), (int) (left.getX() + lineLength), (int) left.getY());
			g.drawLine((int) right.getX(), (int) right.getY(), (int) (right.getX() + lineLength), (int) right.getY());
			g.drawLine((int) (maxX + lineLength), (int) minY, (int) (maxX + lineLength), (int) (minY + gapY));
			// draw the cluster oval
			g.fillOval((int) (maxX + lineLength - (DendrogramWidget.dendroCircleSize / 2)),
					(int) (minY + (gapY / 2) - (DendrogramWidget.dendroCircleSize / 2)),
					DendrogramWidget.dendroCircleSize, DendrogramWidget.dendroCircleSize);
			// draw the distance of the cluster
			int below = ((minY + (gapY / 2) - 2) > (getMatrixBorderS() + getMatrixBorderN()) / 2) ? 15 : 0;
			g.setColor(DendrogramWidget.labelColor);
			g.drawString(df.format(clusterDistance), (int) (maxX + lineLength + 3),
					(int) (minY + (gapY / 2) - 2 + below));

			return new Point((int) (maxX + lineLength), (int) (minY + (gapY / 2)));
		}
	}

	/**
	 * This method extracts the color from the current cell measure
	 * 
	 * @param measure
	 *            the current measure (must be between 0 and 1)
	 * @return the color object associated with the measure
	 */
	private Color measureColor(Double measure) {
		return new Color(matrixBrightestColor.getRed(), matrixBrightestColor.getGreen(),
				matrixBrightestColor.getBlue(), (int) (255 * (1 - measure)));
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		mouseMovingX = e.getX();
		mouseMovingY = e.getY();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {

		offsetX -= mouseMovingX - e.getX();
		offsetY -= mouseMovingY - e.getY();

		mouseMovingX = e.getX();
		mouseMovingY = e.getY();

		matrixBeginX = offsetX + spaceForLabelX;
		matrixBeginY = offsetY + spaceForLabelY;

		mouseOverMatrix = false;

		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (motionPixels++ == 3) {
			mouseMovingX = e.getX();
			mouseMovingY = e.getY();

			mouseOverDendrogram = (mouseMovingX > getMatrixBorderE()
					&& mouseMovingX < (getMatrixBorderE() + dendroWidth) && mouseMovingY > getMatrixBorderN() && mouseMovingY < getMatrixBorderS());

			mouseOverMatrix = (mouseMovingX > getMatrixBorderW() && mouseMovingX < getMatrixBorderE()
					&& mouseMovingY > getMatrixBorderN() && mouseMovingY < getMatrixBorderS());

			if (mouseOverDendrogram || mouseOverMatrix) {
				repaint();
			}
			motionPixels = 0;
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {

		matrixBlockSize += e.getWheelRotation();

		if (matrixBlockSize < 20 || matrixBlockSize > 200) {
			if (matrixBlockSize < 20) {
				matrixBlockSize = 20;
			}
			if (matrixBlockSize > 200) {
				matrixBlockSize = 200;
			}
			return;
		}

		matrixBeginX = offsetX + spaceForLabelX;
		matrixBeginY = offsetY + spaceForLabelY;
		mouseOverMatrix = false;

		repaint();
	}

}
