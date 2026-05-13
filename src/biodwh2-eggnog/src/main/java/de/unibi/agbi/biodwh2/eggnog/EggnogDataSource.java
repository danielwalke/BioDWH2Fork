package de.unibi.agbi.biodwh2.eggnog;

import de.unibi.agbi.biodwh2.core.DataSource;
import de.unibi.agbi.biodwh2.core.DevelopmentState;
import de.unibi.agbi.biodwh2.core.etl.*;
import de.unibi.agbi.biodwh2.eggnog.etl.*;

public class EggnogDataSource extends DataSource {
    @Override
    public String getId() {
        return "EggNOG";
    }

    @Override
    public String getFullName() {
        return "eggNOG";
    }

    @Override
    public String getDescription() {
        return "A database of nested orthologous gene groups";
    }

    @Override
    public DevelopmentState getDevelopmentState() {
        return DevelopmentState.InDevelopment;
    }

    @Override
    protected Updater<? extends DataSource> getUpdater() {
        return new EggnogUpdater(this);
    }

    @Override
    protected Parser<? extends DataSource> getParser() {
        return new PassThroughParser<>(this);
    }

    @Override
    protected GraphExporter<? extends DataSource> getGraphExporter() {
        return new EggnogGraphExporter(this);
    }

    @Override
    public MappingDescriber getMappingDescriber() {
        return new EggnogMappingDescriber(this);
    }
}
