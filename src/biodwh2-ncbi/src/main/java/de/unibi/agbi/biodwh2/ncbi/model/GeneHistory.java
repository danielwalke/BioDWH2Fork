package de.unibi.agbi.biodwh2.ncbi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "tax_id", "GeneID", "Discontinued_GeneID", "Discontinued_Symbol", "Discontinue_Date"
})
public class GeneHistory {
    @JsonProperty("tax_id")
    public String taxonomyId;
    @JsonProperty("GeneID")
    public String geneId;
    @JsonProperty("Discontinued_GeneID")
    public String discontinuedGeneId;
    @JsonProperty("Discontinued_Symbol")
    public String discontinuedSymbol;
    @JsonProperty("Discontinue_Date")
    public String discontinueDate;
}
