package org.ei.opensrp.view.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import com.google.gson.Gson;
import org.ei.opensrp.R;
import org.ei.opensrp.adapter.SmartRegisterPaginatedAdapter;
import org.ei.opensrp.domain.ReportMonth;
import org.ei.opensrp.domain.form.FormSubmission;
import org.ei.opensrp.provider.SmartRegisterClientsProvider;
import org.ei.opensrp.view.contract.SmartRegisterClient;
import org.ei.opensrp.view.customControls.CustomFontTextView;
import org.ei.opensrp.view.customControls.FontVariant;
import org.ei.opensrp.view.dialog.*;
import org.joda.time.LocalDate;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.ei.opensrp.AllConstants.ENTITY_ID_PARAM;
import static org.ei.opensrp.AllConstants.FORM_NAME_PARAM;
import static org.ei.opensrp.AllConstants.INSTANCE_ID_PARAM;
import static org.ei.opensrp.AllConstants.SHORT_DATE_FORMAT;
import static org.ei.opensrp.AllConstants.SYNC_STATUS;
import static org.ei.opensrp.AllConstants.VERSION_PARAM;
import static org.ei.opensrp.domain.SyncStatus.PENDING;
import static org.ei.opensrp.util.EasyMap.create;

public abstract class SecuredNativeSmartRegisterActivity extends SecuredActivity {

    public static final String DIALOG_TAG = "dialog";
    public static final List<? extends DialogOption> DEFAULT_FILTER_OPTIONS = asList(new AllClientsFilter());

    private ListView clientsView;
    private ProgressBar clientsProgressView;
    private TextView serviceModeView;
    private TextView appliedVillageFilterView;
    private TextView appliedSortView;
    private EditText searchView;
    private View searchCancelView;
    private TextView titleLabelView;

    public EditText getSearchView() {
        return searchView;
    }

    public View getSearchCancelView() {
        return searchCancelView;
    }

    public FilterOption getCurrentVillageFilter() {
        return currentVillageFilter;
    }

    public FilterOption getCurrentSearchFilter() {
        return currentSearchFilter;
    }

    public SortOption getCurrentSortOption() {
        return currentSortOption;
    }

    public ServiceModeOption getCurrentServiceModeOption() {
        return currentServiceModeOption;
    }

    public SmartRegisterPaginatedAdapter getClientsAdapter() {
        return clientsAdapter;
    }

    public void setClientsAdapter(SmartRegisterPaginatedAdapter clientsAdapter) {
        this.clientsAdapter = clientsAdapter;
    }

    private SmartRegisterPaginatedAdapter clientsAdapter;

    private FilterOption currentVillageFilter;
    private SortOption currentSortOption;

    public void setCurrentSearchFilter(FilterOption currentSearchFilter) {
        this.currentSearchFilter = currentSearchFilter;
    }

    private FilterOption currentSearchFilter;
    private ServiceModeOption currentServiceModeOption;

    private final PaginationViewHandler paginationViewHandler = new PaginationViewHandler();
    private final NavBarActionsHandler navBarActionsHandler = new NavBarActionsHandler();
    private final SearchCancelHandler searchCancelHandler = new SearchCancelHandler();

    public interface ClientsHeaderProvider {

        public abstract int count();

        public abstract int weightSum();

        public abstract int[] weights();

        public abstract int[] headerTextResourceIds();
    }

    public interface DefaultOptionsProvider {

        public abstract ServiceModeOption serviceMode();

        public abstract FilterOption villageFilter();

        public abstract SortOption sortOption();

        public abstract String nameInShortFormForTitle();

    }

    public interface NavBarOptionsProvider {

        public abstract DialogOption[] filterOptions();

        public abstract DialogOption[] serviceModeOptions();

        public abstract DialogOption[] sortingOptions();

        public abstract String searchHint();
    }

    private DefaultOptionsProvider defaultOptionProvider;
    private NavBarOptionsProvider navBarOptionsProvider;

    @Override
    protected void onCreation() {
        setContentView(R.layout.smart_register_activity);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        onInitialization();

        defaultOptionProvider = getDefaultOptionsProvider();
        navBarOptionsProvider = getNavBarOptionsProvider();

        setupViews();
    }

    @Override
    protected void onResumption() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                publishProgress();
                setupAdapter();
                return null;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                clientsProgressView.setVisibility(VISIBLE);
                clientsView.setVisibility(INVISIBLE);
            }

            @Override
            protected void onPostExecute(Void result) {
                clientsView.setAdapter(clientsAdapter);
                paginationViewHandler.refresh();
                clientsProgressView.setVisibility(View.GONE);
                clientsView.setVisibility(VISIBLE);

            }
        }.executeOnExecutor(THREAD_POOL_EXECUTOR);
    }

    protected void setupViews() {
        setupNavBarViews();
        populateClientListHeaderView(defaultOptionProvider.serviceMode().getHeaderProvider());

        clientsProgressView = (ProgressBar) findViewById(R.id.client_list_progress);
        clientsView = (ListView) findViewById(R.id.list);

        setupStatusBarViews();
        paginationViewHandler.addPagination(clientsView);

        updateDefaultOptions();
    }

    private void setupStatusBarViews() {
        appliedSortView = (TextView) findViewById(R.id.sorted_by);
        appliedVillageFilterView = (TextView) findViewById(R.id.village);
    }

    private void setupNavBarViews() {
        findViewById(R.id.btn_back_to_home).setOnClickListener(navBarActionsHandler);

        setupTitleView();

        View villageFilterView = findViewById(R.id.filter_selection);
        villageFilterView.setOnClickListener(navBarActionsHandler);

        View sortView = findViewById(R.id.sort_selection);
        sortView.setOnClickListener(navBarActionsHandler);

        serviceModeView = (TextView) findViewById(R.id.service_mode_selection);
        serviceModeView.setOnClickListener(navBarActionsHandler);

        findViewById(R.id.register_client).setOnClickListener(navBarActionsHandler);

        setupSearchView();
    }

    protected void setServiceModeViewDrawableRight(Drawable drawable) {
        serviceModeView.setCompoundDrawables(null, null, drawable, null);
    }

    private void setupTitleView() {
        ViewGroup titleLayout = (ViewGroup) findViewById(R.id.title_layout);
        titleLayout.setOnClickListener(navBarActionsHandler);

        titleLabelView = (TextView) findViewById(R.id.txt_title_label);

        TextView reportMonthStartView = (TextView) findViewById(R.id.btn_report_month);
        setReportDates(reportMonthStartView);
    }

    private void setupSearchView() {
        searchView = (EditText) findViewById(R.id.edt_search);
        searchView.setHint(navBarOptionsProvider.searchHint());
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence cs, int start, int before, int count) {
                currentSearchFilter = new ECSearchOption(cs.toString());
                clientsAdapter
                        .refreshList(currentVillageFilter, currentServiceModeOption,
                                currentSearchFilter, currentSortOption);

                searchCancelView.setVisibility(isEmpty(cs) ? INVISIBLE : VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        searchCancelView = findViewById(R.id.btn_search_cancel);
        searchCancelView.setOnClickListener(searchCancelHandler);
    }

    private void setReportDates(TextView titleView) {
        ReportMonth report = new ReportMonth();
        titleView.setText(report.startOfCurrentReportMonth(LocalDate.now()).toString(SHORT_DATE_FORMAT)
                + " - "
                + report.endOfCurrentReportMonth(LocalDate.now()).toString(SHORT_DATE_FORMAT));
    }

    private void updateDefaultOptions() {
        currentSearchFilter = new ECSearchOption(null);
        currentVillageFilter = defaultOptionProvider.villageFilter();
        currentServiceModeOption = defaultOptionProvider.serviceMode();
        currentSortOption = defaultOptionProvider.sortOption();

        appliedSortView.setText(currentSortOption.name());
        appliedVillageFilterView.setText(currentVillageFilter.name());
        serviceModeView.setText(currentServiceModeOption.name());
        titleLabelView.setText(defaultOptionProvider.nameInShortFormForTitle());
    }

    private void populateClientListHeaderView(ClientsHeaderProvider headerProvider) {
        LinearLayout clientsHeaderLayout = (LinearLayout) findViewById(R.id.clients_header_layout);
        clientsHeaderLayout.removeAllViewsInLayout();
        int columnCount = headerProvider.count();
        int[] weights = headerProvider.weights();
        int[] headerTxtResIds = headerProvider.headerTextResourceIds();
        clientsHeaderLayout.setWeightSum(headerProvider.weightSum());

        for (int i = 0; i < columnCount; i++) {
            clientsHeaderLayout.addView(getColumnHeaderView(i, weights, headerTxtResIds));
        }
    }

    private View getColumnHeaderView(int i, int[] weights, int[] headerTxtResIds) {
        CustomFontTextView header = new CustomFontTextView(this, null, R.style.CustomFontTextViewStyle_Header_Black);
        header.setFontVariant(FontVariant.BLACK);
        header.setTextSize(16);
        header.setTextColor(getResources().getColor(R.color.client_list_header_text_color));
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        weights[i]);

        header.setLayoutParams(lp);
        header.setText(headerTxtResIds[i]);
        return header;
    }

    private void setupAdapter() {
        clientsAdapter = adapter();
        clientsAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                paginationViewHandler.refresh();
            }
        });
    }

    protected SmartRegisterPaginatedAdapter adapter() {
        return new SmartRegisterPaginatedAdapter(clientsProvider());
    }

    protected void onServiceModeSelection(ServiceModeOption serviceModeOption) {
        currentServiceModeOption = serviceModeOption;
        serviceModeView.setText(serviceModeOption.name());
        clientsAdapter
                .refreshList(currentVillageFilter, currentServiceModeOption,
                        currentSearchFilter, currentSortOption);

        populateClientListHeaderView(serviceModeOption.getHeaderProvider());
    }

    protected void onSortSelection(SortOption sortBy) {
        currentSortOption = sortBy;
        appliedSortView.setText(sortBy.name());
        clientsAdapter
                .refreshList(currentVillageFilter, currentServiceModeOption,
                        currentSearchFilter, currentSortOption);
    }

    protected void onFilterSelection(FilterOption filter) {
        currentVillageFilter = filter;
        appliedVillageFilterView.setText(filter.name());
        clientsAdapter
                .refreshList(currentVillageFilter, currentServiceModeOption,
                        currentSearchFilter, currentSortOption);
    }

    protected void onEditSelection(EditOption editOption, SmartRegisterClient client) {
        editOption.doEdit(client);
    }

    protected void onEditSelectionWithMetadata(EditOption editOption, SmartRegisterClient client, String metadata) {
        editOption.doEditWithMetadata(client, metadata);
    }

    private void goBack() {
        finish();
    }

    public void showFragmentDialog(DialogOptionModel dialogOptionModel) {
        showFragmentDialog(dialogOptionModel, null);
    }

    public void showFragmentDialog(DialogOptionModel dialogOptionModel, Object tag) {
        if (dialogOptionModel.getDialogOptions().length <= 0) {
            return;
        }

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        SmartRegisterDialogFragment
                .newInstance(this, dialogOptionModel, tag)
                .show(ft, DIALOG_TAG);
    }

    protected abstract DefaultOptionsProvider getDefaultOptionsProvider();

    protected abstract NavBarOptionsProvider getNavBarOptionsProvider();

    protected abstract SmartRegisterClientsProvider clientsProvider();

    protected abstract void onInitialization();

    public abstract void startRegistration();

    private class FilterDialogOptionModel implements DialogOptionModel {
        @Override
        public DialogOption[] getDialogOptions() {
            return navBarOptionsProvider.filterOptions();
        }

        @Override
        public void onDialogOptionSelection(DialogOption option, Object tag) {
            onFilterSelection((FilterOption) option);
        }
    }

    private class SortDialogOptionModel implements DialogOptionModel {
        @Override
        public DialogOption[] getDialogOptions() {
            return navBarOptionsProvider.sortingOptions();
        }

        @Override
        public void onDialogOptionSelection(DialogOption option, Object tag) {
            onSortSelection((SortOption) option);
        }
    }

    protected class ServiceModeDialogOptionModel implements DialogOptionModel {
        @Override
        public DialogOption[] getDialogOptions() {
            return navBarOptionsProvider.serviceModeOptions();
        }

        @Override
        public void onDialogOptionSelection(DialogOption option, Object tag) {
            onServiceModeSelection((ServiceModeOption) option);
        }
    }

    private class PaginationViewHandler implements View.OnClickListener {
        private Button nextPageView;
        private Button previousPageView;
        private TextView pageInfoView;

        private void addPagination(ListView clientsView) {
            ViewGroup footerView = getPaginationView();
            nextPageView = (Button) footerView.findViewById(R.id.btn_next_page);
            previousPageView = (Button) footerView.findViewById(R.id.btn_previous_page);
            pageInfoView = (TextView) footerView.findViewById(R.id.txt_page_info);

            nextPageView.setOnClickListener(this);
            previousPageView.setOnClickListener(this);

            footerView.setLayoutParams(new AbsListView.LayoutParams(
                    AbsListView.LayoutParams.MATCH_PARENT,
                    (int) getResources().getDimension(R.dimen.pagination_bar_height)));

            clientsView.addFooterView(footerView);
        }

        private ViewGroup getPaginationView() {
            return (ViewGroup) getLayoutInflater().inflate(R.layout.smart_register_pagination, null);
        }

        private int getCurrentPageCount() {
            return clientsAdapter.currentPage() + 1 > clientsAdapter.pageCount() ? clientsAdapter.pageCount() : clientsAdapter.currentPage() + 1;
        }

        public void refresh() {
            pageInfoView.setText(
                    format(getResources().getString(R.string.str_page_info),
                            (getCurrentPageCount()),
                            (clientsAdapter.pageCount())));
            nextPageView.setVisibility(clientsAdapter.hasNextPage() ? VISIBLE : INVISIBLE);
            previousPageView.setVisibility(clientsAdapter.hasPreviousPage() ? VISIBLE : INVISIBLE);
        }

        @Override
        public void onClick(View view) {
            int i = view.getId();
            if (i == R.id.btn_next_page) {
                gotoNextPage();

            } else if (i == R.id.btn_previous_page) {
                goBackToPreviousPage();

            }
        }

        public void gotoNextPage() {
            clientsAdapter.nextPage();
            clientsAdapter.notifyDataSetChanged();
        }

        public void goBackToPreviousPage() {
            clientsAdapter.previousPage();
            clientsAdapter.notifyDataSetChanged();
        }
    }

    public class NavBarActionsHandler implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            int i = view.getId();
            if (i == R.id.title_layout || i == R.id.btn_back_to_home) {
                goBack();

            } else if (i == R.id.register_client) {
                startRegistration();

            } else if (i == R.id.filter_selection) {
                showFragmentDialog(new FilterDialogOptionModel());

            } else if (i == R.id.sort_selection) {
                showFragmentDialog(new SortDialogOptionModel());

            } else if (i == R.id.service_mode_selection) {
                showFragmentDialog(new ServiceModeDialogOptionModel());

            }
        }
    }

    public class SearchCancelHandler implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            clearSearchText();
        }

        private void clearSearchText() {
            searchView.setText("");
        }
    }

    public void saveFormSubmission(String formSubmision, String id, String formName, JSONObject fieldOverrides){
        Log.e("saveFormSubmission()", "Override this method in child class");
    }

    protected String getParams(FormSubmission submission) {
        return new Gson().toJson(
                create(INSTANCE_ID_PARAM, submission.instanceId())
                        .put(ENTITY_ID_PARAM, submission.entityId())
                        .put(FORM_NAME_PARAM, submission.formName())
                        .put(VERSION_PARAM, submission.version())
                        .put(SYNC_STATUS, PENDING.value())
                        .map());
    }

    public void savePartialFormData(String formData, String id, String formName, JSONObject fieldOverrides){
        try {
            //Save the current form data into shared preferences
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            String savedDataKey = formName + "savedPartialData";
            editor.putString(savedDataKey, formData);

            String overridesKey = formName + "overrides";
            editor.putString(overridesKey, fieldOverrides.toString());

            String idKey = formName + "id";
            if (id != null){
                editor.putString(idKey, id);
            }

            editor.commit();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public String getPreviouslySavedDataForForm(String formName, String overridesStr, String id){
        try {
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            String savedDataKey = formName + "savedPartialData";
            String overridesKey = formName + "overrides";
            String idKey = formName + "id";

            JSONObject overrides = new JSONObject();

            if (overrides != null){
                JSONObject json = new JSONObject(overridesStr);
                String s = json.getString("fieldOverrides");
                overrides = new JSONObject(s);
            }

            boolean idIsConsistent = id == null && !sharedPref.contains(idKey) ||
                    id != null && sharedPref.contains(idKey) && sharedPref.getString(idKey, null).equals(id);

            if (sharedPref.contains(savedDataKey) && sharedPref.contains(overridesKey) && idIsConsistent){
                String savedDataStr = sharedPref.getString(savedDataKey, null);
                String savedOverridesStr = sharedPref.getString(overridesKey, null);


                // the previously saved data is only returned if the overrides and id are the same ones used previously
                if (savedOverridesStr.equals(overrides.toString())) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    //after retrieving the value delete it from shared pref.
                    editor.remove(savedDataKey);
                    editor.remove(overridesKey);
                    editor.remove(idKey);
                    editor.apply();
                    return savedDataStr;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

}
