package de.unibi.agbi.biodwh2.reactome.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"DB_ID"})
public class PositiveRegulation extends Regulation {
    @JsonProperty("DB_ID")
    public Long dbId;
}
