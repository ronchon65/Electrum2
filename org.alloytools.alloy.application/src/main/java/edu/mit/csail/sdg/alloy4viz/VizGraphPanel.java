/* Alloy Analyzer 4 -- Copyright (c) 2006-2009, Felix Chang
 * Electrum -- Copyright (c) 2015-present, Nuno Macedo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
 * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package edu.mit.csail.sdg.alloy4viz;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point ;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.alloy4.OurBorder;
import edu.mit.csail.sdg.alloy4.OurCombobox;
import edu.mit.csail.sdg.alloy4.OurUtil;
import edu.mit.csail.sdg.alloy4.Util;
import edu.mit.csail.sdg.alloy4graph.GraphViewer;
import edu.mit.csail.sdg.alloy4graph.Graph;
import edu.mit.csail.sdg.alloy4graph.GraphNode;

/**
 * GUI panel that houses the actual graph, as well as any projection comboboxes.
 * <p>
 * <b>Thread Safety:</b> Can be called only by the AWT event thread.
 *
 * @modified: Nuno Macedo // [HASLab] electrum-temporal, electrum-vizualizer
 *
 * @modified: Guy Durrieu // [ONERA] electrum experimental display mode
 *
 */

public final class VizGraphPanel extends JPanel {

    /** This ensures the class can be serialized reliably. */
    private static final long              serialVersionUID    = 0;

    /** This is the current customization settings of each graph panel. */
    // [HASLab]
    private final List<VizState>           vizState;

    // [ONERA] The current list of displayed graphs 
    private List<GraphViewer> graphViewers      = new ArrayList<GraphViewer>();

    /**
     * Whether the user wants to see the DOT source code or not.
     */
    private boolean                        seeDot              = false;

    /**
     * The current GraphViewer (or null if we are not looking at a GraphViewer).
     */
    private GraphViewer                    viewer              = null;

    /**
     * The scrollpanes containing the upperhalf of the panel (showing the graphs).
     */
    // [HASLab]
    private final List<JScrollPane>        diagramScrollPanels = new ArrayList<JScrollPane>();

    /** The upperhalf of the panel (showing the graphs). */
    // [HASLab]
    private final List<JPanel>             graphPanels         = new ArrayList<JPanel>();

    /** The current display choice **/
    // [ONERA]
    private       boolean currentDisplayChoice ;

    // [ONERA] A map to store the set of known nodes coordinates
    private HashMap<String,Point>  knownNodes = new HashMap<String, Point>() ;

    /**
     * The lowerhalf of the panel (showing the comboboxes for choosing the projected
     * atoms).
     */
    private final JPanel                   navPanel;

    /**
     * The splitpane separating the graphPanel and the navPanel.
     */
    private final JSplitPane               split;

    /**
     * The current projection choice; null if no projection is in effect.
     */
    private AlloyProjection                currentProjection   = null;

    /**
     * This is the list of TypePanel(s) we've already constructed.
     */
    private final Map<AlloyType,TypePanel> type2panel          = new TreeMap<AlloyType,TypePanel>();

    /**
     * Inner class that displays a combo box of possible projection atom choices.
     */
    final class TypePanel extends JPanel {

        /** This ensures the class can be serialized reliably. */
        private static final long     serialVersionUID = 0;
        /** The type being projected. */
        private final AlloyType       type;
        /**
         * The list of atoms; can be an empty list if there are no atoms in this type to
         * be projected.
         */
        private final List<AlloyAtom> atoms;
        /**
         * The list of atom names; atomnames.empty() iff atoms.isEmpty()
         */
        private final String[]        atomnames;
        /**
         * The combo box showing the possible atoms to choose from.
         */
        private final JComboBox       atomCombo;

        /**
         * True if this TypePanel object does not need to be rebuilt.
         */
        private boolean upToDate(AlloyType type, List<AlloyAtom> atoms) {
            if (!this.type.equals(type))
                return false;
            atoms = new ArrayList<AlloyAtom>(atoms);
            Collections.sort(atoms);
            if (!this.atoms.equals(atoms))
                return false;
            for (int i = 0; i < this.atoms.size(); i++) {
                String n = this.atoms.get(i).getVizName(vizState.get(0), true); // [HASLab] TODO: what if the projected sig is variable?
                if (!atomnames[i].equals(n))
                    return false;
            }
            return true;
        }

        /**
         * Constructs a new TypePanel.
         *
         * @param type - the type being projected
         * @param atoms - the list of possible projection atom choices
         */
        private TypePanel(AlloyType type, List<AlloyAtom> atoms, AlloyAtom initialValue) {
            super();
            final JButton left, right;
            int initialIndex = 0;
            this.type = type;
            atoms = new ArrayList<AlloyAtom>(atoms);
            Collections.sort(atoms);
            this.atoms = ConstList.make(atoms);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setBorder(null);
            this.atomnames = new String[this.atoms.size()];
            for (int i = 0; i < this.atoms.size(); i++) {
                atomnames[i] = this.atoms.get(i).getVizName(vizState.get(0), true); // [HASLab] TODO: what if the projected sig is variable?
                if (this.atoms.get(i).equals(initialValue))
                    initialIndex = i;
            }
            add(left = new JButton("<<"));
            add(Box.createRigidArea(new Dimension(2, 2)));
            add(atomCombo = new OurCombobox(atomnames.length < 1 ? new String[] {
                                                                                 " "
            } : atomnames));
            add(Box.createRigidArea(new Dimension(2, 2)));
            add(right = new JButton(">>"));
            left.setVerticalAlignment(SwingConstants.CENTER);
            right.setVerticalAlignment(SwingConstants.CENTER);
            Dimension dim = atomCombo.getPreferredSize();
            int idealWidth = Util.onMac() ? 120 : 80;
            if (dim.width < idealWidth) {
                dim.width = idealWidth + 20;
                atomCombo.setMinimumSize(dim);
                atomCombo.setPreferredSize(dim);
            }
            left.setEnabled(initialIndex > 0);
            right.setEnabled(initialIndex < atomnames.length - 1);
            atomCombo.setSelectedIndex(initialIndex);
            if (Util.onMac())
                atomCombo.setBorder(BorderFactory.createEmptyBorder(4, 1, 0, 1));
            left.addActionListener(new ActionListener() {

                @Override
                public final void actionPerformed(ActionEvent e) {
                    int curIndex = atomCombo.getSelectedIndex();
                    if (curIndex > 0)
                        atomCombo.setSelectedIndex(curIndex - 1);
                }
            });
            right.addActionListener(new ActionListener() {

                @Override
                public final void actionPerformed(ActionEvent e) {
                    int curIndex = atomCombo.getSelectedIndex();
                    if (curIndex < atomCombo.getItemCount() - 1)
                        atomCombo.setSelectedIndex(curIndex + 1);
                }
            });
            atomCombo.addActionListener(new ActionListener() {

                @Override
                public final void actionPerformed(ActionEvent e) {
                    left.setEnabled(atomCombo.getSelectedIndex() > 0);
                    right.setEnabled(atomCombo.getSelectedIndex() < atomnames.length - 1);
                    remakeAll(-1);
                    VizGraphPanel.this.getParent().invalidate();
                    VizGraphPanel.this.getParent().repaint();
                }
            });
        }

      /**
         * Returns the entire list of atoms; could be an empty set.
         */
        public List<AlloyAtom> getAlloyAtoms() {
            return atoms;
        }

        /**
         * Returns the currently-selected atom; returns null if the list is empty.
         */
        public AlloyAtom getAlloyAtom() {
            int i = atomCombo.getSelectedIndex();
            if (i >= 0 && i < atoms.size())
                return atoms.get(i);
            else
                return null;
        }

        /** Returns the AlloyType associated with this TypePanel. */
        public AlloyType getAlloyType() {
            return type;
        }
    }

    /**
     * Create a splitpane showing the graphs on top, as well as projection
     * comboboxes on the bottom.
     *
     * @param vizState - the current visualization settings
     * @param seeDot - true if we want to see the DOT source code, false if we want
     *            it rendered as a graph
     */
    // [HASLab]
  public VizGraphPanel(List<VizState> vizState, boolean seeDot,
					   boolean currentDisplayChoice) { // [ONERA]
        Border b = new EmptyBorder(0, 0, 0, 0);
        OurUtil.make(this, Color.BLACK, Color.WHITE, b);
        this.seeDot = seeDot;
        this.vizState = vizState;
		this.currentDisplayChoice = currentDisplayChoice ; // [ONERA]
        setLayout(new GridLayout());
        setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        navPanel = new JPanel();
        JScrollPane navscroll = OurUtil.scrollpane(navPanel);

        JPanel diagramsScrollPanels = new JPanel(); // [HASLab] container for all diagram scroll panels
        diagramsScrollPanels.setLayout(new BoxLayout(diagramsScrollPanels, BoxLayout.LINE_AXIS)); // [HASLab]
        for (int i = 0; i < vizState.size(); i++) { // [HASLab]
            JScrollPane diagramScrollPanel = createGraphPanel(i);
            diagramsScrollPanels.add(diagramScrollPanel);
            diagramScrollPanel.setPreferredSize(new Dimension(0, 0));
        }

        split = OurUtil.splitpane(JSplitPane.VERTICAL_SPLIT, diagramsScrollPanels, navscroll, 0); // [HASLab]
        split.setResizeWeight(1.0);
        split.setDividerSize(0);
        add(split);
        remakeAll(-1);
    }

    /**
     * Creates a particular diagram scroll panel.
     *
     * @param i
     * @return
     */
    // [HASLab] refactored from VizGraphPanel constructor
    private JScrollPane createGraphPanel(int i) {
        Border b = new EmptyBorder(0, 0, 0, 0); // [HASLab]

        JPanel graphPanel = OurUtil.make(new JPanel(), Color.BLACK, Color.WHITE, b);
        graphPanels.add(graphPanel); // [HASLab]
        graphPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent ev) {
                // We let Ctrl+LeftClick bring up the popup menu, just like RightClick,
                // since many Mac mouses do not have a right button.
                if (viewer == null)
                    return;
                else if (ev.getButton() == MouseEvent.BUTTON3) {} else if (ev.getButton() == MouseEvent.BUTTON1 && ev.isControlDown()) {} else
                    return;
                viewer.alloyPopup(graphPanel, ev.getX(), ev.getY());
            }
        });

        JScrollPane diagramScrollPanel = OurUtil.scrollpane(graphPanel, new OurBorder(true, true, true, false));
        diagramScrollPanel.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {

            public void adjustmentValueChanged(AdjustmentEvent e) {
                diagramScrollPanel.invalidate();
                diagramScrollPanel.repaint();
                diagramScrollPanel.validate();
            }
        });
        diagramScrollPanel.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {

            public void adjustmentValueChanged(AdjustmentEvent e) {
                diagramScrollPanel.invalidate();
                diagramScrollPanel.repaint();
                diagramScrollPanel.validate();
            }
        });
        diagramScrollPanels.add(diagramScrollPanel); // [HASLab]
        return diagramScrollPanel;
    }

    /** 
     * Used to propagate the coords of known common nodes
    **/
    // [ONERA]
    public void updateNodesCoords(Graph graph) {
	  for (GraphNode x : graph.nodes) {
		Point p = knownNodes.get(x.uuid.toString()) ;
		if (p != null) {
		  x.setX((int) p.getX()) ;
		  x.setY((int) p.getY()) ;
		}
	  }
    }
 
    /** Used by VizGUI to transmit the current display choice **/
    // [ONERA]
    public void changeDisplayChoice(boolean currentDisplayChoice) {
	  // update this VizGraphPanel 
	  this.currentDisplayChoice = currentDisplayChoice ;
    }
  

    /** Regenerate the comboboxes and the graph. */
    public void remakeAll(int leftCurrent) { // [ONERA] added a parameter in order to implement experimental display
        Map<AlloyType,AlloyAtom> map = new LinkedHashMap<AlloyType,AlloyAtom>();
        navPanel.removeAll();
        for (AlloyType type : vizState.get(vizState.size() - 1).getProjectedTypes()) { // [HASLab]
            List<AlloyAtom> atoms = vizState.get(vizState.size() - 1).getOriginalInstance().type2atoms(type);
            TypePanel tp = type2panel.get(type);
            if (tp != null && tp.getAlloyAtom() != null && !atoms.contains(tp.getAlloyAtom()))
                tp = null;
            if (tp != null && tp.getAlloyAtom() == null && atoms.size() > 0)
                tp = null;
            if (tp != null && !tp.upToDate(type, atoms))
                tp = null;
            if (tp == null)
                type2panel.put(type, tp = new TypePanel(type, atoms, null));
            navPanel.add(tp);
            map.put(tp.getAlloyType(), tp.getAlloyAtom());
        }
        currentProjection = new AlloyProjection(map);
		int lcur = 0 ;
		JPanel graph = null ;
		// [ONERA]
		if (leftCurrent == -1)
		  graphViewers = new ArrayList<GraphViewer>() ;  // reset case
		else
		  lcur = leftCurrent ; // nav case
        for (int i = 0; i < vizState.size(); i++) { // [HASLab]
		  boolean newGV = false ;
		  // [ONERA] taking in account experimental display
		  if (currentDisplayChoice  && lcur + i < graphViewers.size())
			  graph = graphViewers.get(lcur + i) ; // redisplay of an existing state (experimental display)
		  else {
			graph = vizState.get(i).getGraph(currentProjection); // new state (experimental/standard display)
			// graph viewer added to the graph list
			graphViewers.add((GraphViewer) graph) ; // not useful in standard display
			newGV = true ;
		  }
		  if (graph instanceof GraphViewer)  { // [ONERA] experimental display case
			  // transmission of contextual data 
			  ((GraphViewer) graph).setContext(currentDisplayChoice, this, graphViewers.indexOf(graph)) ;
			  // retrieval of the included graph
			  Graph gr = ((GraphViewer) graph).getGraph() ;
			  gr.changeDisplayChoice(currentDisplayChoice) ;
			  //update of the display choice for each node of the graph
			  for (GraphNode gn : gr.nodes)
				gn.changeDisplayChoice(currentDisplayChoice) ;
			  // if experimental display, propagate common nodes coords, then redraw edges
			  if (currentDisplayChoice) {
				if (newGV) {
				  if (graphViewers.size() == 1) // first displayed graph, init knownNodes
					for (GraphNode x : gr.nodes)
					  knownNodes.put(x.uuid.toString(), new Point(x.x(), x.y())) ;
				  else {
					updateNodesCoords(gr) ;
					gr.placeNewNodes(knownNodes) ;
					gr.relayout_edges(true) ;
					gr.recalcBound(true) ;
				  }
				}
			  }  else gr.layout() ;
		  }
		  if (seeDot && (graph instanceof GraphViewer)) {
			viewer = null;
			JTextArea txt = OurUtil.textarea(graph.toString(), 10, 10, false, true, getFont());
			diagramScrollPanels.get(i).setViewportView(txt);
		  } else {
			if (graph instanceof GraphViewer)
			  viewer = (GraphViewer) graph;
			else
			  viewer = null;
			graphPanels.get(i).removeAll();
			graphPanels.get(i).add(graph);
			graphPanels.get(i).setBackground(Color.WHITE); // [HASLab]
			diagramScrollPanels.get(i).setViewportView(graphPanels.get(i));
			diagramScrollPanels.get(i).invalidate();
			diagramScrollPanels.get(i).repaint();
			diagramScrollPanels.get(i).validate();
            }
            vizState.get(i).applyDefaultVar(); // [HASLab] dashed variable elements
        }
	}

    /** Called from GraphViewer paintComponent, if experimental display,
    * in order to display node movments in the left pane to right pane(s)
    **/
    // [ONERA] 
    public void repaintAll(int rank, GraphNode n) {
	  // update the position stored in knownNodes 
	  Graph gr = ((GraphViewer) graphViewers.get(rank)).getGraph() ;
	  knownNodes.remove(n.uuid.toString()) ;
	  knownNodes.put(n.uuid.toString(), new Point(n.x(), n.y())) ;
	  
	  // if node statut is hereditary, propagate common nodes coords, then redraw edges
	  if (n.getHereditary())
		for (int i = rank + 1 ; i < graphViewers.size() ; i++) {
		  Graph gri = ((GraphViewer) graphViewers.get(i)).getGraph() ;
		  updateNodesCoords(gri) ;
		  gri.relayout_edges(true) ;
		  gri.recalcBound(true) ;
	  }
	  
	  // redisplay all
	  for (int i = 0; i < vizState.size(); i++) {
		graphPanels.get(i).invalidate();
		graphPanels.get(i).repaint();
		graphPanels.get(i).validate();
	  }
	}

    /** Changes the font. */
    @Override
    public void setFont(Font font) {
        super.setFont(font);
        if (diagramScrollPanels != null) // [HASLab] called before initialization
            for (JScrollPane diagramScrollPanel : diagramScrollPanels) // [HASLab]
                diagramScrollPanel.getViewport().getView().setFont(font);
    }

    /** Changes whether we are seeing the DOT source or not. */
    public void seeDot(boolean yesOrNo) {
        if (seeDot == yesOrNo)
            return;
        seeDot = yesOrNo;
        remakeAll(-1);
    }

    public String toDot() {
        return vizState.get(0).getGraph(currentProjection).toString(); // [HASLab] converts the first shown state
    }

    /**
     * Retrieves the actual GraphViewer object that contains the graph (or null if
     * the graph hasn't loaded yet)
     */
    public GraphViewer alloyGetViewer() {
        return viewer;
    }

    /**
     * We override the paint method to auto-resize the divider.
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        split.setDividerLocation(split.getSize().height - split.getInsets().bottom - split.getDividerSize() - split.getRightComponent().getPreferredSize().height);
    }

    public void resetProjectionAtomCombos() {
        for (Entry<AlloyType,TypePanel> e : type2panel.entrySet()) {
            if (e.getValue().atomCombo != null)
                e.getValue().atomCombo.setSelectedIndex(0);
        }
    }

    // [HASLab]
    public int numPanes() {
        return vizState.size();
    }
}
