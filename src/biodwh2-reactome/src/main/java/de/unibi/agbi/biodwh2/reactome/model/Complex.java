package de.unibi.agbi.biodwh2.reactome.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"DB_ID", "isChimeric", "stoichiometryKnown"})
public class Complex {
    @JsonProperty("DB_ID")
    public Long dbId;
    @JsonProperty("isChimeric")
    public String isChimeric;
    @JsonProperty("stoichiometryKnown")
    public String stoichiometryKnown;
}
