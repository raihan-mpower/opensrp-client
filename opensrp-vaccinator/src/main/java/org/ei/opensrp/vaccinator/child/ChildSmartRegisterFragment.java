package org.ei.opensrp.vaccinator.child;

import android.view.View;

import org.ei.opensrp.Context;
import org.ei.opensrp.commonregistry.CommonObjectSort;
import org.ei.opensrp.commonregistry.CommonPersonObjectClient;
import org.ei.opensrp.commonregistry.CommonPersonObjectController;
import org.ei.opensrp.provider.SmartRegisterClientsProvider;
import org.ei.opensrp.vaccinator.R;
import org.ei.opensrp.vaccinator.application.template.SmartClientRegisterFragment;
import org.ei.opensrp.vaccinator.db.Client;
import org.ei.opensrp.vaccinator.application.template.DetailActivity;
import org.ei.opensrp.vaccinator.application.common.VaccinationServiceModeOption;
import org.ei.opensrp.view.activity.SecuredNativeSmartRegisterActivity;
import org.ei.opensrp.view.contract.SmartRegisterClient;
import org.ei.opensrp.view.controller.FormController;
import org.ei.opensrp.view.dialog.AllClientsFilter;
import org.ei.opensrp.view.dialog.FilterOption;
import org.ei.opensrp.view.dialog.ServiceModeOption;
import org.ei.opensrp.view.dialog.SortOption;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.json.JSONException;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import static util.Utils.getObsValue;
import static util.Utils.getValue;
import static util.Utils.nonEmptyValue;

public class ChildSmartRegisterFragment extends SmartClientRegisterFragment {
    private SmartRegisterClientsProvider clientProvider = null;
    private final ClientActionHandler clientActionHandler = new ClientActionHandler();
    private CommonPersonObjectController controller;

    public ChildSmartRegisterFragment(){   super(null);   }

    public ChildSmartRegisterFragment(FormController formController) {
        super(formController);
    }

    @Override
    protected SecuredNativeSmartRegisterActivity.DefaultOptionsProvider getDefaultOptionsProvider() {
        return new SecuredNativeSmartRegisterActivity.DefaultOptionsProvider() {

            @Override
            public ServiceModeOption serviceMode() {
                return new VaccinationServiceModeOption(clientsProvider(), "Vaccine", new int[]{
                        R.string.child_profile , R.string.birthdate_age, R.string.epi_number,
                        R.string.child_last_vaccine, R.string.child_next_vaacine
                }, new int[]{3,1,1,3,2});
            }

            @Override
            public FilterOption villageFilter() {
                return new AllClientsFilter();
            }

            @Override
            public SortOption sortOption() {
                return new CommonObjectSort(CommonObjectSort.ByColumnAndByDetails.byDetails, false, "first_name", getResources().getString(R.string.woman_alphabetical_sort));
            }

            @Override
            public String nameInShortFormForTitle() {
                return Context.getInstance().getStringResource(R.string.child_title);
            }
        };
    }

    @Override
    protected String getRegisterLabel() {
        return getResources().getString(R.string.child_register_title);
    }

    @Override
    protected SmartRegisterClientsProvider clientsProvider() {
        if (clientProvider == null) {
            clientProvider = new ChildSmartClientsProvider(getActivity(), clientActionHandler, controller, context.alertService());
        }
        return clientProvider;
    }

    @Override
    protected void onInitialization() {
        if (controller == null) {
            controller = new CommonPersonObjectController(context.allCommonsRepositoryobjects("pkchild"),
                    context.allBeneficiaries(), context.listCache(),
                    context.personObjectClientsCache(), "first_name", "pkchild", "client_reg_date",
                    CommonPersonObjectController.ByColumnAndByDetails.byDetails.byDetails);

        }
        try{
         //   context.formSubmissionRouter().getHandlerMap().put("child_followup", new ChildFollowupHandler(new ChildService(context.allBeneficiaries(), context.allTimelineEvents(), context.allCommonsRepositoryobjects("pkchild"), context.alertService())));
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreation() { }

    private class ClientActionHandler implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.child_profile_info_layout:
                    DetailActivity.startDetailActivity(getActivity(), (CommonPersonObjectClient) view.getTag(), ChildDetailActivity.class);
                    getActivity().finish();
                    break;
                case R.id.child_next_visit_holder:
                    HashMap<String, String> map = new HashMap<>();
                    CommonPersonObjectClient client = (CommonPersonObjectClient) view.getTag();
                    map.putAll(followupOverrides(client));
                    map.putAll(providerOverrides());
                    startForm("child_followup", (SmartRegisterClient) view.getTag(), map);
                    break;
            }
        }
    }

    private HashMap<String, String> getPreloadVaccineData(CommonPersonObjectClient client) {
        HashMap<String, String> map = new HashMap<>();
        map.put("e_bcg", nonEmptyValue(client.getColumnmaps(), true, false, "bcg", "bcg_retro"));
        map.put("e_opv0", nonEmptyValue(client.getColumnmaps(), true, false, "opv0", "opv0_retro"));
        map.put("e_penta1", nonEmptyValue(client.getColumnmaps(), true, false, "penta1", "penta1_retro"));
        map.put("e_opv1", nonEmptyValue(client.getColumnmaps(), true, false, "opv1", "opv1_retro"));
        map.put("e_pcv1", nonEmptyValue(client.getColumnmaps(), true, false, "pcv1", "pcv1_retro"));
        map.put("e_penta2", nonEmptyValue(client.getColumnmaps(), true, false, "penta2", "penta2_retro"));
        map.put("e_opv2", nonEmptyValue(client.getColumnmaps(), true, false, "opv2", "opv2_retro"));
        map.put("e_pcv2", nonEmptyValue(client.getColumnmaps(), true, false, "pcv2", "pcv2_retro"));
        map.put("e_penta3", nonEmptyValue(client.getColumnmaps(), true, false, "penta3", "penta3_retro"));
        map.put("e_opv3", nonEmptyValue(client.getColumnmaps(), true, false, "opv3", "opv3_retro"));
        map.put("e_pcv3", nonEmptyValue(client.getColumnmaps(), true, false, "pcv3", "pcv3_retro"));
        map.put("e_ipv", nonEmptyValue(client.getColumnmaps(), true, false, "ipv", "ipv_retro"));
        map.put("e_measles1", nonEmptyValue(client.getColumnmaps(), true, false, "measles1", "measles1_retro"));
        map.put("e_measles2", nonEmptyValue(client.getColumnmaps(), true, false, "measles2", "measles2_retro"));
        return map;
    }

    private HashMap<String, String> getPreloadVaccineData(Client client) throws JSONException, ParseException {
        HashMap<String, String> map = new HashMap<>();
        map.put("e_bcg", getObsValue(getClientEventDb(), client, true, "bcg", "bcg_retro"));
        map.put("e_opv0", getObsValue(getClientEventDb(), client, true, "opv0", "opv0_retro"));
        map.put("e_penta1", getObsValue(getClientEventDb(), client, true, "penta1", "penta1_retro"));
        map.put("e_opv1", getObsValue(getClientEventDb(), client, true, "opv1", "opv1_retro"));
        map.put("e_pcv1", getObsValue(getClientEventDb(), client, true, "pcv1", "pcv1_retro"));
        map.put("e_penta2", getObsValue(getClientEventDb(), client, true, "penta2", "penta2_retro"));
        map.put("e_opv2", getObsValue(getClientEventDb(), client, true, "opv2", "opv2_retro"));
        map.put("e_pcv2", getObsValue(getClientEventDb(), client, true, "pcv2", "pcv2_retro"));
        map.put("e_penta3", getObsValue(getClientEventDb(), client, true, "penta3", "penta3_retro"));
        map.put("e_opv3", getObsValue(getClientEventDb(), client, true, "opv3", "opv3_retro"));
        map.put("e_pcv3", getObsValue(getClientEventDb(), client, true, "pcv3", "pcv3_retro"));
        map.put("e_ipv", getObsValue(getClientEventDb(), client, true, "ipv", "ipv_retro"));
        map.put("e_measles1", getObsValue(getClientEventDb(), client, true, "measles1", "measles1_retro"));
        map.put("e_measles2", getObsValue(getClientEventDb(), client, true, "measles2", "measles2_retro"));

        return map;
    }

    @Override
    protected String getRegistrationForm(HashMap<String, String> overrides) {
        return "child_enrollment";
    }

    @Override
    protected String getOAFollowupForm(Client client, HashMap<String, String> overridemap) {
        overridemap.putAll(followupOverrides(client));
        return "offsite_child_followup";
    }

    @Override
    protected Map<String, String> customFieldOverrides() {
        Map<String, String> m = new HashMap<>();
        return m;
    }

    private Map<String, String> followupOverrides(CommonPersonObjectClient client){
        Map<String, String> map = new HashMap<>();
        map.put("existing_full_address", getValue(client.getDetails(), "address1", true)+
                ", UC: "+getValue(client.getDetails(), "union_councilname", true)+
                ", Town: "+getValue(client.getDetails(), "townname", true)+
                ", City: "+getValue(client.getDetails(), "city_villagename", true)+ " - " + getValue(client.getDetails(), "landmark", true));

        map.put("existing_program_client_id", getValue(client.getDetails(), "program_client_id", false));
        map.put("program_client_id", getValue(client.getDetails(), "program_client_id", false));

        int days = 0;
        try{
            days = Days.daysBetween(new DateTime(getValue(client.getColumnmaps(), "dob", false)), DateTime.now()).getDays();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        map.put("existing_first_name", getValue(client.getDetails(), "first_name", true));
        map.put("existing_last_name", getValue(client.getDetails(), "last_name", true));
        map.put("existing_gender", getValue(client.getDetails(), "gender", true));
        map.put("existing_mother_name", getValue(client.getDetails(), "mother_name", true));
        map.put("existing_father_name", getValue(client.getDetails(), "father_name", true));
        map.put("existing_birth_date", getValue(client.getColumnmaps(), "dob", false));
        map.put("existing_age", days+"");
        map.put("existing_epi_card_number", getValue(client.getDetails(), "epi_card_number", false));
        map.put("reminders_approval", getValue(client.getDetails(), "reminders_approval", false));
        map.put("contact_phone_number", getValue(client.getDetails(), "contact_phone_number", false));

        map.putAll(getPreloadVaccineData(client));

        return map;
    }

    private Map<String, String> followupOverrides(Client client){
        Map<String, String> map = new HashMap<>();
        map.put("existing_full_address", client.getAddress("usual_residence").getAddressField("address1")+
                ", UC: "+client.getAddress("usual_residence").getSubTown()+
                ", Town: "+client.getAddress("usual_residence").getTown()+
                ", City: "+client.getAddress("usual_residence").getCityVillage()+
                " - "+client.getAddress("usual_residence").getAddressField("landmark"));

        map.put("existing_program_client_id", client.getIdentifier("Program Client ID"));
        map.put("program_client_id", client.getIdentifier("Program Client ID"));

        map.put("existing_first_name", client.getFirstName());
        map.put("existing_last_name", client.getLastName());
        map.put("existing_gender", client.getGender());
        map.put("existing_birth_date", client.getBirthdate().toString("yyyy-MM-dd"));
        int days = 0;
        try{
            days = Days.daysBetween(client.getBirthdate(), DateTime.now()).getDays();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        map.put("existing_age", days+"");
        Object epi = client.getAttribute("EPI Card Number");
        map.put("existing_epi_card_number", epi == null ? "" : epi.toString());

        try{
            map.put("existing_father_name", getObsValue(getClientEventDb(), client, true, "father_name", "1594AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            map.put("existing_mother_name", getObsValue(getClientEventDb(), client, true, "mother_name", "1593AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

            map.put("reminders_approval", getObsValue(getClientEventDb(), client, true, "reminders_approval", "163089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            map.put("contact_phone_number", getObsValue(getClientEventDb(), client, true, "contact_phone_number", "159635AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

            map.putAll(getPreloadVaccineData(client));
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return map;
    }
}
