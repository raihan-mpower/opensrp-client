package org.ei.drishti.repository;

import com.google.gson.Gson;
import org.ei.drishti.dto.Action;
import org.ei.drishti.domain.EligibleCouple;
import org.ei.drishti.util.Log;

import java.util.List;

public class AllEligibleCouples {
    private EligibleCoupleRepository repository;

    public AllEligibleCouples(EligibleCoupleRepository eligibleCoupleRepository) {
        this.repository = eligibleCoupleRepository;
    }

    public void handleAction(Action action) {
        if ("createEC".equals(action.type())) {
            repository.add(new EligibleCouple(action.caseID(), action.get("wife"), action.get("husband"), action.get("ecNumber"), action.get("currentMethod"), action.get("village"), action.get("subcenter"), new Gson().toJson(action.details())));
        } else if ("deleteEC".equals(action.type())) {
            repository.close(action.caseID());
        } else {
            Log.logWarn("Unknown type in eligible couple action: " + action);
        }
    }

    public List<EligibleCouple> all() {
        return repository.allEligibleCouples();
    }

    public void deleteAll() {
        repository.deleteAllEligibleCouples();
    }

    public EligibleCouple findByCaseID(String caseId) {
        return repository.findByCaseID(caseId);
    }

    public long count() {
        return repository.count();
    }
}