package de.unibi.agbi.biodwh2.reactome.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"DB_ID", "abbreviation"})
public class Species {
    @JsonProperty("DB_ID")
    public Long dbId;
    @JsonProperty("abbreviation")
    public String abbreviation;
}
