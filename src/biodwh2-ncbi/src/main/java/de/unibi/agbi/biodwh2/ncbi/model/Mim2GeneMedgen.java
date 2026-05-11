package de.unibi.agbi.biodwh2.ncbi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "MIM number", "GeneID", "type", "Source", "MedGenCUI", "Comment"
})
public class Mim2GeneMedgen {
    @JsonProperty("MIM number")
    public String mimNumber;
    @JsonProperty("GeneID")
    public String geneId;
    @JsonProperty("type")
    public String type;
    @JsonProperty("Source")
    public String source;
    @JsonProperty("MedGenCUI")
    public String medgenCui;
    @JsonProperty("Comment")
    public String comment;
}
