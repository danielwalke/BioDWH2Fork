package de.unibi.agbi.biodwh2.ncbi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "NCBI_protein_accession", "UniProtKB_protein_accession", "NCBI_tax_id", "UniProtKB_tax_id", "method"
})
public class GeneRefseqUniprotkbCollab {
    @JsonProperty("NCBI_protein_accession")
    public String ncbiProteinAccession;
    @JsonProperty("UniProtKB_protein_accession")
    public String uniProtKbProteinAccession;
    @JsonProperty("NCBI_tax_id")
    public String ncbiTaxId;
    @JsonProperty("UniProtKB_tax_id")
    public String uniProtKbTaxId;
    @JsonProperty("method")
    public String method;
}
