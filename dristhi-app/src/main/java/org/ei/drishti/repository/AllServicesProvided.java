package org.ei.drishti.repository;

import org.ei.drishti.domain.ServiceProvided;

import java.util.List;

public class AllServicesProvided {
    private ServiceProvidedRepository repository;

    public AllServicesProvided(ServiceProvidedRepository repository) {
        this.repository = repository;
    }

    public List<ServiceProvided> findByEntityIdAndName(String entityId, String[] names) {
        return repository.findByEntityIdAndName(entityId, names);
    }
}
