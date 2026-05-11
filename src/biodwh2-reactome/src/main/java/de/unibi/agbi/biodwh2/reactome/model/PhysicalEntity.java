package de.unibi.agbi.biodwh2.reactome.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"DB_ID", "definition", "goCellularComponent", "goCellularComponent_class", "authored", "authored_class", "systematicName"})
public class PhysicalEntity {
    @JsonProperty("DB_ID")
    public Long dbId;
    @JsonProperty("definition")
    public String definition;
    @JsonProperty("goCellularComponent")
    public Long goCellularComponent;
    @JsonProperty("goCellularComponent_class")
    public String goCellularComponentClass;
    @JsonProperty("authored")
    public String authored;
    @JsonProperty("authored_class")
    public String authoredClass;
    @JsonProperty("systematicName")
    public String systematicName;
}
