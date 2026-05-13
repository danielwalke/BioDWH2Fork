package de.unibi.agbi.biodwh2.eggnog.etl;

import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.etl.GraphExporter;
import de.unibi.agbi.biodwh2.core.exceptions.ExporterException;
import de.unibi.agbi.biodwh2.core.exceptions.ExporterFormatException;
import de.unibi.agbi.biodwh2.core.model.graph.Graph;
import de.unibi.agbi.biodwh2.core.model.graph.IndexDescription;
import de.unibi.agbi.biodwh2.core.model.graph.Node;
import de.unibi.agbi.biodwh2.eggnog.EggnogDataSource;
import org.apache.commons.lang3.StringUtils;

import java.io.*;

public class EggnogGraphExporter extends GraphExporter<EggnogDataSource> {
    static final String TAXON_LABEL = "Taxon";
    static final String ORTHOLOGOUS_GROUP_LABEL = "OrthologousGroup";

    public EggnogGraphExporter(final EggnogDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long getExportVersion() {
        return 2;
    }

    @Override
    protected boolean exportGraph(final Workspace workspace, final Graph graph) throws ExporterException {
        graph.addIndex(IndexDescription.forNode(TAXON_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(ORTHOLOGOUS_GROUP_LABEL, "id", IndexDescription.Type.UNIQUE));
        exportTaxa(workspace, graph);
        exportOrthologousGroups(workspace, graph);
        return true;
    }

    private void exportTaxa(final Workspace workspace, final Graph graph) {
        final java.nio.file.Path filePath = dataSource.resolveSourceFilePath(workspace, EggnogUpdater.TAXID_INFO_FILE);
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) continue;
                String[] parts = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t");
                if (parts.length >= 5) {
                    graph.addNode(TAXON_LABEL,
                            "id", parts[0],
                            "scientific_name", parts[1],
                            "rank", parts[2],
                            "named_lineage", parts[3],
                            "taxid_lineage", parts[4]
                    );
                }
            }
        } catch (IOException e) {
            throw new ExporterFormatException("Failed to parse " + EggnogUpdater.TAXID_INFO_FILE, e);
        }
    }

    private void exportOrthologousGroups(final Workspace workspace, final Graph graph) {
        final java.nio.file.Path filePath = dataSource.resolveSourceFilePath(workspace, EggnogUpdater.OG_ANNOTATIONS_FILE);
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) continue;
                String[] parts = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t");
                if (parts.length >= 4) {
                    final String ogId = parts[1];
                    Node ogNode = graph.findNode(ORTHOLOGOUS_GROUP_LABEL, "id", ogId);
                    if (ogNode == null) {
                        ogNode = graph.addNode(ORTHOLOGOUS_GROUP_LABEL,
                                "id", ogId,
                                "cog_category", parts[2],
                                "annotation", parts[3]
                        );
                    }
                    Node taxonNode = graph.findNode(TAXON_LABEL, "id", parts[0]);
                    if (taxonNode != null) {
                        graph.addEdge(ogNode, taxonNode, "BELONGS_TO");
                    }
                }
            }
        } catch (IOException e) {
            throw new ExporterFormatException("Failed to parse " + EggnogUpdater.OG_ANNOTATIONS_FILE, e);
        }
    }
}
