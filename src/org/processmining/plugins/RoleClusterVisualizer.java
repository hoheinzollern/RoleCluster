package org.processmining.plugins;

import it.processmining.clustering.ui.DendrogramWidget;

import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(
	name = "Visualize Clustered Activities", 
	parameterLabels = { "RoleCluster" }, 
	returnLabels = { "Clustered Activities" }, 
	returnTypes = { JComponent.class }
)
@Visualizer
public class RoleClusterVisualizer extends JPanel {

	private static final long serialVersionUID = 5394844159308978486L;
	
	private RoleCluster cluster;

	@PluginVariant(requiredParameterLabels = { 0 })
	public static JComponent visualize(PluginContext context, RoleCluster cluster) {
		return new RoleClusterVisualizer(cluster);
	}
	
	public RoleClusterVisualizer(RoleCluster cluster) {
		this.cluster = cluster;
		
		setLayout(new GridLayout(1, 1));
		
		DendrogramWidget visualizer = new DendrogramWidget(cluster.getRoot());
		add(visualizer);
	}

}
