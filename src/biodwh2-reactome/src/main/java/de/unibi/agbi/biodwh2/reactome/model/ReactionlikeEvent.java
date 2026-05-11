package de.unibi.agbi.biodwh2.reactome.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"DB_ID", "isChimeric", "systematicName", "normalReaction", "catalystActivityReference", "reactionType"})
public class ReactionlikeEvent {
    @JsonProperty("DB_ID")
    public Long dbId;
    @JsonProperty("isChimeric")
    public String isChimeric;
    @JsonProperty("systematicName")
    public String systematicName;
    @JsonProperty("normalReaction")
    public Long normalReaction;
    @JsonProperty("catalystActivityReference")
    public Long catalystActivityReference;
    @JsonProperty("reactionType")
    public Long reactionType;
}
