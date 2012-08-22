package org.processmining.plugins;

import it.processmining.clustering.hierarchical.HACluster;
import it.processmining.clustering.hierarchical.HAClusterer;
import it.processmining.clustering.hierarchical.Distance;
import it.processmining.clustering.hierarchical.EuclideanDistance;
import it.processmining.clustering.hierarchical.HATreeNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.cnet.CNet;
import org.processmining.models.cnet.CNetNode;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramFactory;
import org.processmining.models.graphbased.directed.bpmn.elements.Activity;
import org.processmining.models.graphbased.directed.bpmn.elements.Swimlane;

import weka.core.Instance;

public class RoleClusterPlugin {
	@Plugin(name = "BPMNify", parameterLabels = { "Process model", "Role cluster", "Threshold" }, 
			returnLabels = { "Result Net" }, returnTypes = { HACluster.class }, 
			userAccessible = true, help = "Builds a BPMN diagram with swimlanes, from a CNet and a RoleCluster")
	@UITopiaVariant(affiliation = "Siav", author = "Alessandro Bruni", email = "alessandro.bruni@gmail.com")
	public static BPMNDiagram goBPMN(PluginContext context, CNet net, HACluster cluster, Double cut) throws Exception {
		BPMNDiagram diagram = BPMNDiagramFactory.newBPMNDiagram(net.getLabel());
		
		HashMap<String, CNetNode> nodeByLabel = new HashMap<String, CNetNode>();
		HashMap<String, Activity> activityByLabel = new HashMap<String, Activity>();
		
		Progress progress = context.getProgress();
		progress.setMinimum(0);
		progress.setMaximum(3);
		
		/*
		 * Step 0: collect all nodes and construct relative activities, and 
		 * populate the hash maps
		 */
		progress.setCaption("Converting net...");
		progress.setValue(0);
		for (CNetNode node : net.getNodes()) {
			String label = node.getLabel();
			
			nodeByLabel.put(label, node);
			
			Activity activity = diagram.addActivity(label, false, false, false, false, false);
			activityByLabel.put(label, activity);
		}
		
		/*
		 * Step 1: add flow relations between activities in the diagram
		 */
		progress.setValue(1);
		for (CNetNode n1 : net.getNodes()) {
			for (CNetNode n2 : net.getSuccessors(n1)) {
				Activity a1 = activityByLabel.get(n1.getLabel());
				Activity a2 = activityByLabel.get(n2.getLabel());
				
				diagram.addFlow(a1, a2, "");
			}
		}
		
		/*
		 * Step 2: build swimlanes
		 */
		progress.setCaption("Building swimlanes...");
		progress.setValue(2);
		
		HashMap<Integer, Collection<HATreeNode>> groups = cluster.buildGroupsCutAt(cut);
		
		for (Map.Entry<Integer, Collection<HATreeNode>> group : groups.entrySet()) {
			Integer groupId = group.getKey();
			Swimlane swimlane = diagram.addSwimlane("Group #" + groupId.toString(), null);
			
			for (HATreeNode instance : group.getValue()) {
				String label = instance.getName();
				Activity act = activityByLabel.get(label);
				swimlane.addChild(act);
			}
		}
		
		progress.setCaption("Done!");
		progress.setValue(3);
		
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
