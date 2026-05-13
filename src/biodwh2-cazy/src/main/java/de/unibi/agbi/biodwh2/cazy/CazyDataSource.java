package de.unibi.agbi.biodwh2.cazy;

import de.unibi.agbi.biodwh2.cazy.etl.CazyGraphExporter;
import de.unibi.agbi.biodwh2.cazy.etl.CazyMappingDescriber;
import de.unibi.agbi.biodwh2.cazy.etl.CazyUpdater;
import de.unibi.agbi.biodwh2.core.DataSource;
import de.unibi.agbi.biodwh2.core.DevelopmentState;
import de.unibi.agbi.biodwh2.core.etl.*;
import de.unibi.agbi.biodwh2.core.text.License;

public class CazyDataSource extends DataSource {
    @Override
    public String getId() {
        return "CAZY";
    }

    @Override
    public String getFullName() {
        return "CAZy (Carbohydrate-Active Enzyme Database)";
    }

    @Override
    public String getLicense() {
        return License.CC_BY_4_0.getName();
    }

    @Override
    public String getLicenseUrl() {
        return "https://www.cazy.org/howtocite.html";
    }

    @Override
    public DevelopmentState getDevelopmentState() {
        return DevelopmentState.Usable;
    }

    @Override
    protected Updater<? extends DataSource> getUpdater() {
        return new CazyUpdater(this);
    }

    @Override
    protected GraphExporter<? extends DataSource> getGraphExporter() {
        return new CazyGraphExporter(this);
    }

    @Override
    public MappingDescriber getMappingDescriber() {
        return new CazyMappingDescriber(this);
    }
}
