package org.processmining.plugins;

import it.processmining.clustering.hierarchical.Distance;
import it.processmining.clustering.hierarchical.EuclideanDistance;
import it.processmining.clustering.hierarchical.HACluster;
import it.processmining.clustering.hierarchical.HAClusterer;
import it.processmining.clustering.hierarchical.HATreeNode;
import it.processmining.clustering.ui.DendrogramWidget;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.flexiblemodel.EndTaskNodesSet;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexImpl;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.models.flexiblemodel.SetFlex;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramFactory;
import org.processmining.models.graphbased.directed.bpmn.elements.Activity;
import org.processmining.models.graphbased.directed.bpmn.elements.Event;
import org.processmining.models.graphbased.directed.bpmn.elements.Event.EventType;
import org.processmining.models.graphbased.directed.bpmn.elements.Swimlane;

import weka.core.Instance;
	
class Configuration extends JPanel {
	private static final long serialVersionUID = -3016992036483249169L;
	
	private double cutValue = 0;

	private GridBagLayout gridBagLayout;

	private DendrogramWidget dendrogramWidget;

	private JLabel lblCutThreshold;

	private JSlider sliderCutThreshold;
	
	Configuration (HACluster cluster) {
		gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		dendrogramWidget = new DendrogramWidget(cluster);
		GridBagConstraints gbc_panelDendrogram = new GridBagConstraints();
		gbc_panelDendrogram.gridwidth = 2;
		gbc_panelDendrogram.insets = new Insets(0, 0, 5, 0);
		gbc_panelDendrogram.fill = GridBagConstraints.BOTH;
		gbc_panelDendrogram.gridx = 0;
		gbc_panelDendrogram.gridy = 0;
		add(dendrogramWidget, gbc_panelDendrogram);
		
		lblCutThreshold = new JLabel("Cut threshold:");
		GridBagConstraints gbc_lblCutThreshold = new GridBagConstraints();
		gbc_lblCutThreshold.insets = new Insets(0, 0, 0, 5);
		gbc_lblCutThreshold.gridx = 0;
		gbc_lblCutThreshold.gridy = 1;
		add(lblCutThreshold, gbc_lblCutThreshold);
		
		sliderCutThreshold = new JSlider();
		GridBagConstraints gbc_sliderCutThreshold = new GridBagConstraints();
		gbc_sliderCutThreshold.gridx = 1;
		gbc_sliderCutThreshold.gridy = 1;
		add(sliderCutThreshold, gbc_sliderCutThreshold);
		
		dendrogramWidget.addMouseListener(new MouseListener() {
			
			public void mouseReleased(MouseEvent e) { }
			public void mousePressed(MouseEvent e) { }
			public void mouseExited(MouseEvent e) { }
			public void mouseEntered(MouseEvent e) { }
			
			public void mouseClicked(MouseEvent e) {
				double value = dendrogramWidget.getCurrentValue();
				if (0 <= value && value <= 1) {
					sliderCutThreshold.setValue((int) (value * 100));
				}
			}
		});
	}
	
	double getCut() {
		return sliderCutThreshold.getValue() / 100.;
	}
	
}

public class RoleClusterPlugin {
	
	@Plugin(name = "BPMNify Heuristics", parameterLabels = { "Process model", "Start activities", "End activities", "Role cluster" }, 
			returnLabels = { "Result Net" }, returnTypes = { BPMNDiagram.class }, 
			userAccessible = true, help = "Builds a BPMN diagram with swimlanes, from a CNet and a RoleCluster")
	@UITopiaVariant(affiliation = "Siav", author = "Alessandro Bruni", email = "alessandro.bruni@gmail.com")
	public static BPMNDiagram goBPMNfromHeuristics(UIPluginContext context, Flex _net,
			StartTaskNodesSet startTaskSet, EndTaskNodesSet endTaskSet, HACluster cluster) throws Exception {
		FlexImpl net = (FlexImpl) _net;
		BPMNDiagram diagram = BPMNDiagramFactory.newBPMNDiagram("heuristics net from flex model");
		Configuration configuration = new Configuration(cluster);
		
		context.showConfiguration("BPMNify configuration", configuration);
		
		double cut = configuration.getCut();
		
		HashMap<String, FlexNode> nodeByLabel = new HashMap<String, FlexNode>();
		HashMap<String, Activity> activityByLabel = new HashMap<String, Activity>();
		
		Progress progress = context.getProgress();
		progress.setMinimum(0);
		progress.setMaximum(4);
		
		/*
		 * Step 0: collect all nodes and construct relative activities, and 
		 * populate the hash maps
		 */
		progress.setCaption("Converting net...");
		progress.setValue(0);
		for (FlexNode node : net.getNodes()) {
			String label = node.getLabel();
			
			nodeByLabel.put(label, node);
			
			Activity activity = diagram.addActivity(label, false, false, false, false, false);
			activityByLabel.put(label, activity);
		}
		
		/*
		 * Step 1: add flow relations between activities in the diagram
		 */
		progress.setValue(1);
		for (FlexNode n1 : net.getNodes()) {
			for (SetFlex n3 : n1.getOutputNodes()) {
				for (FlexNode n2 : n3) {
					Activity a1 = activityByLabel.get(n1.getLabel());
					Activity a2 = activityByLabel.get(n2.getLabel());
					
					diagram.addFlow(a1, a2, "");
				}
			}
		}
		
		/*
		 * Step 2: add start and end events
		 */
		progress.setCaption("Adding start and end events...");
		progress.setValue(2);

		Event start = diagram.addEvent("Start", EventType.START, null, null, null);
		Event end = diagram.addEvent("End", EventType.END, null, null, null);

		for (SetFlex sf : startTaskSet) {
			for (FlexNode n : sf) {
				Activity a = activityByLabel.get(n.getLabel());
				diagram.addFlow(start, a, "");
			}
		}
		for (SetFlex sf : endTaskSet) {
			for (FlexNode n : sf) {
				Activity a = activityByLabel.get(n.getLabel());
				diagram.addFlow(a, end, "");
			}
		}
		
		
		/*
		 * Step 3: build swimlanes
		 */
		progress.setCaption("Building swimlanes...");
		progress.setValue(3);
		
		HashMap<Integer, Collection<HATreeNode>> groups = cluster.buildGroupsCutAt(cut);
		
		for (Map.Entry<Integer, Collection<HATreeNode>> group : groups.entrySet()) {
			Integer groupId = group.getKey();
			Swimlane swimlane = diagram.addSwimlane("Group #" + groupId.toString(), null);
			
			for (HATreeNode instance : group.getValue()) {
				String label = instance.getName();
				for (Activity act : activityByLabel.values()) {
					if (act.getLabel().startsWith(label)) {
						swimlane.addChild(act);
						act.setParentSwimlane(swimlane);
					}
				}
			}
		}
		
		progress.setCaption("Done!");
		progress.setValue(3);
		
		return diagram;
	}

	@Plugin(name = "Sample BPMN", parameterLabels = { },
			returnLabels = { "Sample BPMN Diagram" }, returnTypes = { BPMNDiagram.class }, 
			userAccessible = true, help = "")
	@UITopiaVariant(affiliation = "Siav", author = "Alessandro Bruni", email = "alessandro.bruni@gmail.com")
	public static BPMNDiagram buildBPMNExample(PluginContext context) {
		BPMNDiagram diagram = BPMNDiagramFactory.newBPMNDiagram("Test");

		Activity a1 = diagram.addActivity("A1", false, false, false, false, false);
		Activity a2 = diagram.addActivity("A2", false, false, false, false, false);
		Activity a3 = diagram.addActivity("A3", false, false, false, false, false);

		Swimlane s1 = diagram.addSwimlane("S1", null);
		Swimlane s2 = diagram.addSwimlane("S2", null);
		
		/*s1.addChild(a1);
		s1.addChild(a2);
		s2.addChild(a3);*/
		
		a1.setParentSwimlane(s1);
		a2.setParentSwimlane(s1);
		a3.setParentSwimlane(s2);
		
		diagram.addFlow(a1, a2, "");
		diagram.addFlow(a2, a3, "");
		
		Event start = diagram.addEvent("Start", EventType.START, null, null, null);
		Event end = diagram.addEvent("End", EventType.END, null, null, null);
		
		diagram.addFlow(start, a1, "");
		diagram.addFlow(a3, end, "");
		
		return diagram;
	}

	@Plugin(name = "RoleCluster", parameterLabels = { "Event Log" },
			returnLabels = { "Result Cluster" }, returnTypes = { HACluster.class }, 
			userAccessible = true, help = "Clusters activities over different groups")
	@UITopiaVariant(affiliation = "Siav", author = "Alessandro Bruni", email = "alessandro.bruni@gmail.com")
	public static HACluster clusterRoles(PluginContext context, XLog log) throws Exception {
		HashMap<String, HashMap<String, Integer>> actByUser = new HashMap<String, HashMap<String, Integer>>();
		HashMap<String, Integer> userMap = new HashMap<String, Integer>();
		ArrayList<String> userId = new ArrayList<String>();
		HashMap<String, Instance> activityDS = new HashMap<String, Instance>();
		int nextUserId = 0;
		
		Progress progress = context.getProgress();
		progress.setMinimum(0);
		progress.setMaximum(3);

		/*
		 * STEP 1: Count the activities by user
		 */
		progress.setValue(0);
		progress.setCaption("Counting..");
		// Iterate over traces
		for (XTrace trace : log) {

			// Iterate over events
			for (XEvent event : trace) {
				XAttributeMap attributes = event.getAttributes();

				//XAttributeLiteral transition = (XAttributeLiteral)attributes.get("lifecycle:transition");
				//if (transition.getValue().equals("complete")) {

				String activity = ((XAttributeLiteral) attributes.get("concept:name")).getValue();
				HashMap<String, Integer> userCount;
				if (!actByUser.containsKey(activity))
					userCount = actByUser.put(activity, new HashMap<String, Integer>());
				else
					userCount = actByUser.get(activity);

				XAttributeLiteral xresource = (XAttributeLiteral) attributes.get("org:resource");
				if (xresource != null) {
					String resource = xresource.getValue();
					int count;
					if (!userCount.containsKey(resource))
						count = 0;
					else
						count = userCount.get(resource);
					userCount.put(resource, count + 1);

					if (!userMap.containsKey(resource)) {
						userMap.put(resource, nextUserId);
						userId.add(resource);
						nextUserId += 1;
					}
				}

				//}
			}
		}

		/*
		 * STEP 2: Build data for clustering
		 */
		progress.setCaption("Building data for clustering...");
		progress.setValue(1);
		System.out.println("# Activities: " + actByUser.size());
		
		Distance distance = EuclideanDistance.create();
		HAClusterer clusterer = new HAClusterer(nextUserId, distance);
		for (Map.Entry<String, HashMap<String, Integer>> userCount : actByUser.entrySet()) {
			System.out.println("# Users: " + userCount.getValue().size());
			System.out.print(userCount.getKey());

			double[] instance = new double[nextUserId];

			for (Map.Entry<String, Integer> entry : userCount.getValue().entrySet()) {
				int index = userMap.get(entry.getKey());
				int count = entry.getValue();

				System.out.print(" [" + index + "]=" + entry.getKey() + "->" + entry.getValue());
				instance[index] = Math.log(count + 1); // TODO: change this
			}
			System.out.println();
			clusterer.addInstance(instance, userCount.getKey());
		}

		/*
		 * STEP 3: Cluster and classify the activities
		 */
		progress.setCaption("Clustering...");
		progress.setValue(2);

		HACluster cluster = clusterer.cluster();
		
		progress.setCaption("Done!");
		progress.setValue(3);

		return cluster;
	}
}
