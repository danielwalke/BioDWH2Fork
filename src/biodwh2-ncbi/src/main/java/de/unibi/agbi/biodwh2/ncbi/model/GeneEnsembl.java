package de.unibi.agbi.biodwh2.ncbi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "tax_id", "GeneID", "Ensembl_gene_identifier", "RNA_nucleotide_accession.version", "Ensembl_rna_identifier",
        "protein_accession.version", "Ensembl_protein_identifier"
})
public class GeneEnsembl {
    @JsonProperty("tax_id")
    public String taxonomyId;
    @JsonProperty("GeneID")
    public String geneId;
    @JsonProperty("Ensembl_gene_identifier")
    public String ensemblGeneIdentifier;
    @JsonProperty("RNA_nucleotide_accession.version")
    public String rnaNucleotideAccessionVersion;
    @JsonProperty("Ensembl_rna_identifier")
    public String ensemblRnaIdentifier;
    @JsonProperty("protein_accession.version")
    public String proteinAccessionVersion;
    @JsonProperty("Ensembl_protein_identifier")
    public String ensemblProteinIdentifier;
}
