package org.ei.opensrp.vaccinator.application.template;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.ei.opensrp.Context;
import org.ei.opensrp.commonregistry.CommonObjectSort;
import org.ei.opensrp.domain.form.FieldOverrides;
import org.ei.opensrp.provider.SmartRegisterClientsProvider;
import org.ei.opensrp.vaccinator.R;
import org.ei.opensrp.vaccinator.application.common.BasicSearchOption;
import org.ei.opensrp.vaccinator.application.common.DateSort;
import org.ei.opensrp.vaccinator.application.common.StatusSort;
import org.ei.opensrp.vaccinator.db.CESQLiteHelper;
import org.ei.opensrp.vaccinator.db.Client;
import org.ei.opensrp.vaccinator.application.template.SmartRegisterFragment;
import org.ei.opensrp.view.activity.SecuredNativeSmartRegisterActivity;
import org.ei.opensrp.view.contract.SmartRegisterClient;
import org.ei.opensrp.view.contract.SmartRegisterClients;
import org.ei.opensrp.view.controller.FormController;
import org.ei.opensrp.view.dialog.DialogOption;
import org.joda.time.DateTime;
import org.joda.time.Years;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import util.Utils;
import util.barcode.Barcode;

public abstract class SmartClientRegisterFragment extends SmartRegisterFragment {
    private FormController formController1;

    public CESQLiteHelper getClientEventDb() {
        return ceDb;
    }

    private CESQLiteHelper ceDb;

    public SmartClientRegisterFragment(FormController formController) {
        super(formController);
        this.formController1 = formController;
    }

    @Override
    protected abstract SecuredNativeSmartRegisterActivity.DefaultOptionsProvider getDefaultOptionsProvider() ;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ceDb = new CESQLiteHelper(activity);
    }

    @Override
    protected SecuredNativeSmartRegisterActivity.NavBarOptionsProvider getNavBarOptionsProvider() {
        return new SecuredNativeSmartRegisterActivity.NavBarOptionsProvider() {

            @Override
            public DialogOption[] filterOptions() {
                return new DialogOption[]{};
            }

            @Override
            public DialogOption[] serviceModeOptions() {
                return new DialogOption[]{
                        //  new CommonObjectSort(CommonObjectSort.ByColumnAndByDetails.byDetails,true,"program_client_id",getResources().getString(R.string.child_id_sort))
                };
            }

            @Override
            public DialogOption[] sortingOptions() {
                return new DialogOption[]{
                        new CommonObjectSort(CommonObjectSort.ByColumnAndByDetails.byDetails, false, "first_name", getResources().getString(R.string.woman_alphabetical_sort)),
                        new DateSort(DateSort.ByColumnAndByDetails.byColumn, "Age", "dob"),
                        new StatusSort("Due Status"),
                        new CommonObjectSort(CommonObjectSort.ByColumnAndByDetails.byDetails, false, "program_client_id", getResources().getString(R.string.id_sort))
                };
            }

            @Override
            public String searchHint() {
                return Context.getInstance().getStringResource(R.string.search_hint);
            }
        };
    }//end of method

    @Override
    protected abstract SmartRegisterClientsProvider clientsProvider() ;

    @Override
    protected abstract void onInitialization() ;

    @Override
    protected void onCreation() {

    }//end of method

    @Override
    protected void startRegistration() {
        Intent intent = new Intent(Barcode.BARCODE_INTENT);
        intent.putExtra(Barcode.SCAN_MODE, Barcode.QR_MODE);
        startActivityForResult(intent, Barcode.BARCODE_REQUEST_CODE);
    }//end of method

    protected abstract String getRegisterLabel();

    @Override
    protected void onResumption() {
        super.onResumption();
        getDefaultOptionsProvider();
        updateSearchView();

        ((TextView)mView.findViewById(org.ei.opensrp.R.id.txt_title_label)).setText(getRegisterLabel());

        ((TextView)mView.findViewById(org.ei.opensrp.R.id.statusbar_today)).setText(Utils.convertDateFormat(DateTime.now()));

        mView.findViewById(org.ei.opensrp.R.id.filter_selection).setVisibility(View.GONE);

        mView.findViewById(org.ei.opensrp.R.id.service_mode_selection).setVisibility(View.GONE);

        mView.findViewById(org.ei.opensrp.R.id.btn_report_month).setVisibility(View.GONE);

        mView.findViewById(org.ei.opensrp.R.id.village).setVisibility(View.GONE);

        ImageView imv = ((ImageView)mView.findViewById(org.ei.opensrp.R.id.register_client));
        imv.setImageResource(R.mipmap.qr_code);
        // create a matrix for the manipulation
        imv.setAdjustViewBounds(true);
        imv.setScaleType(ImageView.ScaleType.FIT_XY);
    }//end of method

    protected void startForm(String formName, SmartRegisterClient client, HashMap<String, String> overrideStringmap) {
        startForm(formName, client.entityId(), overrideStringmap);
    }

    protected void startForm(String formName, String entityId, HashMap<String, String> overrideStringmap) {
        if (overrideStringmap == null) {
            org.ei.opensrp.util.Log.logDebug("overrides data is null");
            overrideStringmap = new HashMap<>();
        }

        overrideStringmap.putAll(providerOverrides());

        String fieldOverrides =  new FieldOverrides(new JSONObject(overrideStringmap).toString()).getJSONString();
        org.ei.opensrp.util.Log.logDebug("fieldOverrides data is : " + fieldOverrides);
        formController1.startFormActivity(formName, entityId, fieldOverrides);
    }

    protected abstract String getRegistrationForm(HashMap<String, String> overridemap);

    protected abstract String getOAFollowupForm(Client client, HashMap<String, String> overridemap);

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        org.ei.opensrp.util.Log.logDebug("Result Coode" + resultCode);
        if (resultCode == Activity.RESULT_OK) {
            Bundle extras = data.getExtras();
            String qrcode = (String) extras.get(Barcode.SCAN_RESULT);

            onQRCodeSucessfullyScanned(qrcode);
        }
    }//end of the method

    private void startEnrollmentForm(HashMap<String, String> overrides){
        startForm(getRegistrationForm(overrides), "", overrides);
    }

    private void startOffSiteFollowupForm(Client client, HashMap<String, String> overrides){
        startForm(getOAFollowupForm(client, overrides), client.getBaseEntityId(), overrides);
    }

    private void onQRCodeSucessfullyScanned(String qrCode) {
      /* #TODO:after reading the code , app first search for that id in database if he it is there , that client appears  on register only . if it doesnt then it shows two options
       */
        SmartRegisterClients fc = getFilteredClients(qrCode);
        if(fc.size() > 0) {
            getSearchView().setText(qrCode);
        }
        else {
            Client c = null;
            try{
                c = ceDb.getClient(qrCode);
            }
            catch (Exception e){
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            if(c != null && getRegisterLabel().toLowerCase().contains("woman")
                    && c.getBirthdate() != null && Years.yearsBetween(c.getBirthdate(), DateTime.now()).getYears() < 8){
                showMessageDialog("Scanned ID already exists and is not a woman.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                return;
            }

            if(c != null && getRegisterLabel().toLowerCase().contains("child")
                    && c.getBirthdate() != null && Years.yearsBetween(c.getBirthdate(), DateTime.now()).getYears() >= 8){
                showMessageDialog("Scanned ID already exists and is not a child.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                return;
            }

            HashMap<String,String> map = new HashMap<>();
            map.put("existing_program_client_id", qrCode);
            map.put("program_client_id", qrCode);
            Map<String, String> m = customFieldOverrides();
            if(m != null){
                map.putAll(m);
            }

            if (c != null){
                startOffSiteFollowupForm(c, map);
            }
            else {
                startEnrollmentForm(map);
            }
        }
    }

    protected abstract Map<String, String> customFieldOverrides();

    private SmartRegisterClients getFilteredClients(String filterString) {
        setCurrentSearchFilter(new BasicSearchOption(filterString));
        SmartRegisterClients filteredClients = getClientsAdapter().getListItemProvider()
                .updateClients(getCurrentVillageFilter(), getCurrentServiceModeOption(),
                        getCurrentSearchFilter(), getCurrentSortOption());
        return filteredClients;
    }//end of method
}