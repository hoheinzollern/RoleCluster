package org.processmining.plugins;

import it.processmining.clustering.hierarchical.HAClusterer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;

public class RoleClusterPlugin {
	@Plugin(name = "RoleCluster", parameterLabels = { "Event Log" }, returnLabels = { "Result Net" }, returnTypes = { RoleCluster.class }, userAccessible = true, help = "Clusters activities over different groups")
	@UITopiaVariant(affiliation = "Siav", author = "Alessandro Bruni", email = "alessandro.bruni@gmail.com")
	public static RoleCluster clusterRoles(PluginContext context, XLog log) throws Exception {
		HashMap<String, HashMap<String, Integer>> actByUser = new HashMap<String, HashMap<String, Integer>>();
		HashMap<String, Integer> userMap = new HashMap<String, Integer>();
		ArrayList<String> userId = new ArrayList<String>();
		HashMap<String, Instance> activityDS = new HashMap<String, Instance>();
		int nextUserId = 0;

		/**
		 * STEP 1: Count the activities by user
		 */
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

		/**
		 * STEP 2: Build data for clustering
		 */
		System.out.println("# Activities: " + actByUser.size());

		FastVector attributes = new FastVector(nextUserId);
		for (Integer i = 0; i < nextUserId; i++) {
			Attribute a = new Attribute(userId.get(i));
			attributes.addElement(a);

		}

		HAClusterer clusterer = new HAClusterer(nextUserId);
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

		System.out.println("------------------------------------------------------------------------");

		/**
		 * STEP 3: Cluster and classify the activities
		 */

		RoleCluster cluster = new RoleCluster(clusterer.cluster());

		System.out.println("------------------------------------------------------------------------");

		return cluster;
	}
}
