package org.ei.opensrp.vaccinator.application.common;

import org.ei.opensrp.Context;
import org.ei.opensrp.commonregistry.CommonPersonObjectClient;
import org.ei.opensrp.vaccinator.R;
import org.ei.opensrp.view.contract.SmartRegisterClient;
import org.ei.opensrp.view.dialog.FilterOption;

public class BasicSearchOption implements FilterOption {
    private final String criteria;
    public BasicSearchOption(String criteria){
        this.criteria=criteria;
    }
    @Override
    public boolean filter(SmartRegisterClient client) {
        CommonPersonObjectClient currentclient = (CommonPersonObjectClient) client;
        if(currentclient.getDetails().get("first_name") != null
                && currentclient.getDetails().get("first_name").toLowerCase().contains(criteria.toLowerCase())) {
            return true;
        }
        if(currentclient.getDetails().get("program_client_id") != null
                && currentclient.getDetails().get("program_client_id").equalsIgnoreCase(criteria)) {
            return true;
        }
        if(currentclient.getDetails().get("existing_program_client_id") != null
                && currentclient.getDetails().get("existing_program_client_id").equalsIgnoreCase(criteria)) {
            return true;
        }
        if(currentclient.getDetails().get("epi_card_number") != null
                && currentclient.getDetails().get("epi_card_number").contains(criteria)) {
            return true;
        }
        if(currentclient.getDetails().get("father_name") != null
                && currentclient.getDetails().get("father_name").contains(criteria)) {
            return true;
        }
        if(currentclient.getDetails().get("mother_name") != null
                && currentclient.getDetails().get("mother_name").contains(criteria)) {
            return true;
        }
        if(currentclient.getDetails().get("husband_name") != null
                && currentclient.getDetails().get("husband_name").contains(criteria)) {
            return true;
        }
        if(currentclient.getDetails().get("contact_phone_number") != null
                && currentclient.getDetails().get("contact_phone_number").contains(criteria)) {
            return true;
        }
        return false;
    }

    @Override
    public String name() {
        return Context.getInstance().applicationContext().getResources().getString(R.string.search_hint);
    }
}
