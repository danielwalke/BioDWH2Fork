package de.unibi.agbi.biodwh2.lmsd.etl;

import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.etl.GraphExporter;
import de.unibi.agbi.biodwh2.core.exceptions.ExporterException;
import de.unibi.agbi.biodwh2.core.exceptions.ExporterFormatException;
import de.unibi.agbi.biodwh2.core.io.FileUtils;
import de.unibi.agbi.biodwh2.core.io.sdf.SdfEntry;
import de.unibi.agbi.biodwh2.core.io.sdf.SdfReader;
import de.unibi.agbi.biodwh2.core.model.graph.Graph;
import de.unibi.agbi.biodwh2.lmsd.LMSDDataSource;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;

public class LMSDGraphExporter extends GraphExporter<LMSDDataSource> {
    static final String LIPID_LABEL = "Lipid";

    public LMSDGraphExporter(final LMSDDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long getExportVersion() {
        return 1;
    }

    @Override
    protected boolean exportGraph(final Workspace workspace, final Graph graph) throws ExporterException {
        try {
            FileUtils.forEachZipEntry(workspace, dataSource, LMSDUpdater.FILE_NAME, ".sdf", (stream, zipEntry) -> {
                final var reader = new SdfReader(stream, StandardCharsets.UTF_8);
                for (final var entry : reader) {
                    exportEntry(graph, entry);
                }
            });
        } catch (final Exception e) {
            throw new ExporterFormatException("Failed to export '" + LMSDUpdater.FILE_NAME + "'", e);
        }
        return true;
    }

    private void exportEntry(final Graph graph, final SdfEntry entry) {
        final var builder = graph.buildNode(LIPID_LABEL);
        builder.withProperty(ID_KEY, entry.properties.get("LM_ID"));
        builder.withPropertyIfNotNull("exact_mass", entry.properties.get("EXACT_MASS"));
        builder.withPropertyIfNotNull("smiles", entry.properties.get("SMILES"));
        builder.withPropertyIfNotNull("formula", entry.properties.get("FORMULA"));
        builder.withPropertyIfNotNull("name", entry.properties.get("NAME"));
        builder.withPropertyIfNotNull("abbreviation", entry.properties.get("ABBREVIATION"));
        builder.withPropertyIfNotNull("main_class", entry.properties.get("MAIN_CLASS"));
        builder.withPropertyIfNotNull("sub_class", entry.properties.get("SUB_CLASS"));
        builder.withPropertyIfNotNull("inchi", entry.properties.get("INCHI"));
        builder.withPropertyIfNotNull("inchi_key", entry.properties.get("INCHI_KEY"));
        builder.withPropertyIfNotNull("systematic_name", entry.properties.get("SYSTEMATIC_NAME"));
        builder.withPropertyIfNotNull("category", entry.properties.get("CATEGORY"));
        builder.withPropertyIfNotNull("class_level4", entry.properties.get("CLASS_LEVEL4"));
        final String synonyms = entry.properties.get("SYNONYMS");
        if (synonyms != null)
            builder.withPropertyIfNotNull("synonyms", StringUtils.splitByWholeSeparator(synonyms, "; "));
        builder.withPropertyIfNotNull("pubchem_cid", entry.properties.get("PUBCHEM_CID"));
        builder.withPropertyIfNotNull("chebi_id", entry.properties.get("CHEBI_ID"));
        builder.withPropertyIfNotNull("hmdb_id", entry.properties.get("HMDB_ID"));
        builder.withPropertyIfNotNull("plantfa_id", entry.properties.get("PLANTFA_ID"));
        builder.withPropertyIfNotNull("swisslipids_id", entry.properties.get("SWISSLIPIDS_ID"));
        builder.withPropertyIfNotNull("lipidbank_id", entry.properties.get("LIPIDBANK_ID"));
        builder.withPropertyIfNotNull("kegg_id", entry.properties.get("KEGG_ID"));
        builder.build();
    }
}
