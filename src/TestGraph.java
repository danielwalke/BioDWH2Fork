import de.unibi.agbi.biodwh2.core.model.graph.Graph;
import de.unibi.agbi.biodwh2.core.model.graph.Edge;

public class TestGraph {
    public static void main(String[] args) throws Exception {
        Graph g = new Graph("/mnt/vdb/daniel/git/workspaces/kegg/sources/merged.db", true);
        System.out.println("Node labels: " + java.util.Arrays.toString(g.getNodeLabels()));
        System.out.println("Edge labels: " + java.util.Arrays.toString(g.getEdgeLabels()));
        for (String label : g.getEdgeLabels()) {
            if (label.contains("MODULE")) {
                System.out.println("Found module edge label: " + label + " count: " + g.getNumberOfEdges(label));
            }
        }
        g.close();
    }
}
