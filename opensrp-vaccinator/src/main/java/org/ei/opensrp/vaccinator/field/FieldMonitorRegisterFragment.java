package org.ei.opensrp.vaccinator.field;

import android.view.View;
import android.widget.TextView;

import org.ei.opensrp.Context;
import org.ei.opensrp.commonregistry.CommonPersonObjectClient;
import org.ei.opensrp.commonregistry.CommonPersonObjectController;
import org.ei.opensrp.domain.form.FieldOverrides;
import org.ei.opensrp.provider.SmartRegisterClientsProvider;
import org.ei.opensrp.vaccinator.R;
import org.ei.opensrp.vaccinator.application.common.DateSort;
import org.ei.opensrp.vaccinator.application.common.ReportFilterOption;
import org.ei.opensrp.vaccinator.db.Client;
import org.ei.opensrp.vaccinator.field.FieldMonitorDailyDetailActivity;
import org.ei.opensrp.vaccinator.field.FieldMonitorMonthlyDetailActivity;
import org.ei.opensrp.vaccinator.field.FieldMonitorSmartClientsProvider;
import org.ei.opensrp.vaccinator.field.StockDailyServiceModeOption;
import org.ei.opensrp.vaccinator.field.StockMonthlyServiceModeOption;
import org.ei.opensrp.vaccinator.application.template.DetailActivity;
import org.ei.opensrp.vaccinator.application.template.SmartRegisterFragment;
import org.ei.opensrp.view.activity.SecuredNativeSmartRegisterActivity;
import org.ei.opensrp.view.controller.FormController;
import org.ei.opensrp.view.dialog.AllClientsFilter;
import org.ei.opensrp.view.dialog.DialogOption;
import org.ei.opensrp.view.dialog.FilterOption;
import org.ei.opensrp.view.dialog.ServiceModeOption;
import org.ei.opensrp.view.dialog.SortOption;
import org.joda.time.DateTime;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import util.Utils;

/**
 * Created by Safwan on 2/15/2016.
 */
public class FieldMonitorRegisterFragment extends SmartRegisterFragment {

    private FormController formController1;
    private final ClientActionHandler clientActionHandler = new ClientActionHandler();

    public FieldMonitorRegisterFragment(FormController formController) {
        super(formController);
        this.formController1 = formController;
    }

    @Override
    protected SecuredNativeSmartRegisterActivity.DefaultOptionsProvider getDefaultOptionsProvider() {
        return new SecuredNativeSmartRegisterActivity.DefaultOptionsProvider() {

            @Override
            public ServiceModeOption serviceMode() {
                return new StockDailyServiceModeOption(clientsProvider(FieldMonitorSmartClientsProvider.ByMonthByDay.ByDay));
            }

            @Override
            public FilterOption villageFilter() {
                return new AllClientsFilter();
            }

            @Override
            public SortOption sortOption(){ return new DateSort(DateSort.ByColumnAndByDetails.byColumn, "Reporting Period", "date"); }

            @Override
            public String nameInShortFormForTitle() {
                return Context.getInstance().getStringResource(R.string.stock_register_title);
            }
        };
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
                        new StockDailyServiceModeOption(clientsProvider(FieldMonitorSmartClientsProvider.ByMonthByDay.ByDay)),
                        new StockMonthlyServiceModeOption(clientsProvider(FieldMonitorSmartClientsProvider.ByMonthByDay.ByMonth))
                };
            }

            @Override
            public DialogOption[] sortingOptions() {
                return new DialogOption[]{
                        new DateSort(DateSort.ByColumnAndByDetails.byDetails, "Reporting Period", "date")};
            }

            @Override
            public String searchHint() {
                return Context.getInstance().getStringResource(R.string.str_field_search_hint);
            }
        };
    }

    @Override
    protected void onServiceModeSelection(ServiceModeOption serviceModeOption, View view) {
        super.onServiceModeSelection(serviceModeOption, view);
        updateSearchView();
    }

    @Override
    protected SmartRegisterClientsProvider clientsProvider() {
        if(getCurrentServiceModeOption() == null || getCurrentServiceModeOption().name().toLowerCase().contains("daily")) {
            return clientsProvider(FieldMonitorSmartClientsProvider.ByMonthByDay.ByDay);
        }
        else {
            return clientsProvider(FieldMonitorSmartClientsProvider.ByMonthByDay.ByMonth);
        }
    }

    private SmartRegisterClientsProvider clientsProvider(FieldMonitorSmartClientsProvider.ByMonthByDay type) {
        FieldMonitorSmartClientsProvider clientProvider;
        CommonPersonObjectController controller = null;
        if (type.equals(FieldMonitorSmartClientsProvider.ByMonthByDay.ByMonth)) {
            setCurrentSearchFilter(new ReportFilterOption("monthly"));
            controller = new CommonPersonObjectController(context.allCommonsRepositoryobjects("stock"),
                    context.allBeneficiaries(), context.listCache(),
                    context.personObjectClientsCache(), "date", "stock", "report", "monthly",
                    CommonPersonObjectController.ByColumnAndByDetails.byColumn, "date",
                    CommonPersonObjectController.ByColumnAndByDetails.byColumn);
            clientProvider = new FieldMonitorSmartClientsProvider(
                    getActivity().getApplicationContext(), clientActionHandler, controller, context.alertService(), FieldMonitorSmartClientsProvider.ByMonthByDay.ByMonth, context, this);
        }
        else {
            setCurrentSearchFilter(new ReportFilterOption("daily"));
            controller = new CommonPersonObjectController(context.allCommonsRepositoryobjects("stock"),
                    context.allBeneficiaries(), context.listCache(),
                    context.personObjectClientsCache(), "date", "stock", "report", "daily",
                    CommonPersonObjectController.ByColumnAndByDetails.byColumn, "date",
                    CommonPersonObjectController.ByColumnAndByDetails.byColumn);
            clientProvider = new FieldMonitorSmartClientsProvider(
                    getActivity().getApplicationContext(), clientActionHandler, controller, context.alertService(), FieldMonitorSmartClientsProvider.ByMonthByDay.ByDay, context, this);
        }
        return clientProvider;
    }

    @Override
    protected void onInitialization() {
        ((TextView)mView.findViewById(org.ei.opensrp.R.id.txt_title_label)).setText(getRegisterLabel());

        mView.findViewById(org.ei.opensrp.R.id.btn_report_month).setVisibility(View.GONE);

        mView.findViewById(org.ei.opensrp.R.id.filter_selection).setVisibility(View.GONE);

        mView.findViewById(org.ei.opensrp.R.id.village).setVisibility(View.GONE);

        mView.findViewById(org.ei.opensrp.R.id.sort_selection).setVisibility(View.GONE);

        mView.findViewById(org.ei.opensrp.R.id.sorted_by).setVisibility(View.GONE);

        mView.findViewById(org.ei.opensrp.R.id.label_sorted_by).setVisibility(View.GONE);

        ((TextView)mView.findViewById(org.ei.opensrp.R.id.statusbar_today)).setText(Utils.convertDateFormat(DateTime.now()));
    }

    @Override
    protected void startRegistration() {
        HashMap<String, String> overrides = new HashMap<>();
        overrides.putAll(providerOverrides());
        formController1.startFormActivity(getRegistrationForm(overrides), null, new FieldOverrides(new JSONObject(overrides).toString()).getJSONString());
    }

    @Override
    protected String getRegisterLabel() {
        return "Stock Register";
    }

    @Override
    protected String getRegistrationForm(HashMap<String, String> overridemap) {
        return "vaccine_stock_position";
    }

    @Override
    protected String getOAFollowupForm(Client client, HashMap<String, String> overridemap) {
        return null;
    }

    @Override
    protected Map<String, String> customFieldOverrides() {
        return null;
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        getDefaultOptionsProvider();
        updateSearchView();
    }//end of method

    @Override
    protected void onCreation() {    }

    private class ClientActionHandler implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.stock_detail_holder:
                    if (getCurrentServiceModeOption().name().toLowerCase().contains("month")) {
                        DetailActivity.startDetailActivity(getActivity(), (CommonPersonObjectClient) view.getTag(R.id.client_details_tag), FieldMonitorMonthlyDetailActivity.class);
                    }
                    else {
                        DetailActivity.startDetailActivity(getActivity(), (CommonPersonObjectClient) view.getTag(R.id.client_details_tag), FieldMonitorDailyDetailActivity.class);
                    }

                    getActivity().finish();

                break;
            }
        }
    }
}
