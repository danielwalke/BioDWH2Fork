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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public class EggnogGraphExporter extends GraphExporter<EggnogDataSource> {
    static final String PROTEIN_LABEL = "EggNogProtein";

    public EggnogGraphExporter(final EggnogDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long getExportVersion() {
        return 3;
    }

    @Override
    protected boolean exportGraph(final Workspace workspace, final Graph graph) throws ExporterException {
        graph.addIndex(IndexDescription.forNode(PROTEIN_LABEL, "uniprot_id", IndexDescription.Type.UNIQUE));
        for (String fileName : EggnogUpdater.MAPPING_FILES) {
            exportMappingFile(workspace, graph, fileName);
        }
        return true;
    }

    private void exportMappingFile(final Workspace workspace, final Graph graph, final String fileName) {
        final java.nio.file.Path filePath = dataSource.resolveSourceFilePath(workspace, fileName);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new GZIPInputStream(new FileInputStream(filePath.toFile()))))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) continue;
                String[] parts = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t");
                if (parts.length >= 2) {
                    String uniprotId = parts[0];
                    String[] eggnogGroups = StringUtils.split(parts[1], ",");
                    Node node = graph.findNode(PROTEIN_LABEL, "uniprot_id", uniprotId);
                    if (node == null) {
                        graph.addNode(PROTEIN_LABEL, "uniprot_id", uniprotId, "eggnog_groups", eggnogGroups);
                    } else {
                        String[] existingGroups = node.getProperty("eggnog_groups");
                        if (existingGroups != null) {
                            Set<String> merged = new HashSet<>(Arrays.asList(existingGroups));
                            merged.addAll(Arrays.asList(eggnogGroups));
                            node.setProperty("eggnog_groups", merged.toArray(new String[0]));
                        } else {
                            node.setProperty("eggnog_groups", eggnogGroups);
                        }
                        graph.update(node);
                    }
                }
            }
        } catch (IOException e) {
            throw new ExporterFormatException("Failed to parse " + fileName, e);
        }
    }
}
