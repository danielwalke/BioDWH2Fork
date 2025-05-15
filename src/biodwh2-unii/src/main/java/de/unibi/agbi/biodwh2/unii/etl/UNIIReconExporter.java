package de.unibi.agbi.biodwh2.unii.etl;

import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.etl.ReconExporter;
import de.unibi.agbi.biodwh2.core.exceptions.ReconException;
import de.unibi.agbi.biodwh2.core.model.IdentifierType;
import de.unibi.agbi.biodwh2.core.model.graph.Graph;
import de.unibi.agbi.biodwh2.unii.UNIIDataSource;
import de.unibi.agbi.biodwh2.unii.model.UNIIDataEntry;
import de.unibi.agbi.biodwh2.unii.model.UNIIEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UNIIReconExporter extends ReconExporter<UNIIDataSource> {
    public UNIIReconExporter(final UNIIDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long getReconVersion() {
        return 1;
    }

    @Override
    public void recon(final Workspace workspace, final Graph graph) throws ReconException {
        final Map<String, List<UNIIEntry>> uniiEntriesMap = new HashMap<>();
        for (final UNIIEntry entry : dataSource.uniiEntries) {
            if (!uniiEntriesMap.containsKey(entry.unii))
                uniiEntriesMap.put(entry.unii, new ArrayList<>());
            uniiEntriesMap.get(entry.unii).add(entry);
        }
        for (final Map.Entry<String, List<UNIIEntry>> entry : uniiEntriesMap.entrySet())
            reconEntry(graph, entry.getValue(), dataSource.uniiDataEntries.get(entry.getKey()));
    }

    private void reconEntry(final Graph graph, final List<UNIIEntry> entries, final UNIIDataEntry dataEntry) {
        final var uniiNodeId = createIdentifier(graph, IdentifierType.UNII.build(dataEntry.unii));
        final var casNodeId = createIdentifier(graph, IdentifierType.CAS.build(dataEntry.rn));
        final var rxCuiNodeId = createIdentifier(graph, IdentifierType.RX_NORM_CUI.build(dataEntry.rxCui));
        final var pubChemNodeId = createIdentifier(graph, IdentifierType.PUB_CHEM_COMPOUND.build(dataEntry.pubchem));
        // inchiKey, smiles, ...
        createXrefOrNameRelation(graph, uniiNodeId, casNodeId);
        createXrefOrNameRelation(graph, uniiNodeId, rxCuiNodeId);
        createXrefOrNameRelation(graph, uniiNodeId, pubChemNodeId);
        for (final var entry : entries) {
            // Type: "of" -> "official_names", "sys" -> "systematic_names", "cn" -> "common_names", "cd" -> "codes",
            //       "bn" -> "brand_names"
            if ("cd".equals(entry.type))
                continue;
            final var synonymNodeId = createSynonym(graph, entry.name);
            createXrefOrNameRelation(graph, uniiNodeId, synonymNodeId);
        }
    }
}
