package org.ei.opensrp.vaccinator.application.template;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import org.ei.opensrp.Context;
import org.ei.opensrp.commonregistry.CommonObjectFilterOption;
import org.ei.opensrp.provider.SmartRegisterClientsProvider;
import org.ei.opensrp.util.StringUtil;
import org.ei.opensrp.vaccinator.db.Client;
import org.ei.opensrp.vaccinator.application.common.BasicSearchOption;
import org.ei.opensrp.view.activity.SecuredNativeSmartRegisterActivity;
import org.ei.opensrp.view.contract.SmartRegisterClients;
import org.ei.opensrp.view.controller.FormController;
import org.ei.opensrp.view.dialog.DialogOption;
import org.ei.opensrp.view.fragment.SecuredNativeSmartRegisterFragment;
import org.opensrp.api.domain.Location;
import org.opensrp.api.util.LocationTree;
import org.opensrp.api.util.TreeNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import util.Utils;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public abstract class SmartRegisterFragment extends SecuredNativeSmartRegisterFragment {
    private FormController formController1;

    public SmartRegisterFragment(FormController formController) {
        super();
        this.formController1 = formController;
    }

    @Override
    protected abstract SecuredNativeSmartRegisterActivity.DefaultOptionsProvider getDefaultOptionsProvider() ;

    @Override
    protected abstract SmartRegisterClientsProvider clientsProvider() ;

    @Override
    protected abstract void onInitialization() ;

    @Override
    protected void onCreation() {
    }

    @Override
    protected abstract void startRegistration();

    protected abstract String getRegisterLabel();

    //This would be used in displaying location dialog box in anm location selector
    private String getLocationNameByAttribute(LocationTree locationTree, String tag) {
        //locationTree.
        Map<String, TreeNode<String, Location>> locationMap =
                locationTree.getLocationsHierarchy();
        Collection<TreeNode<String, Location>> collection = locationMap.values();
        Iterator iterator = collection.iterator();
        while (iterator.hasNext()) {
            TreeNode<String, Location> treeNode = (TreeNode<String, Location>) iterator.next();
            Location location = treeNode.getNode();

            for (String s : location.getTags()) {

                if (s.equalsIgnoreCase(tag)) {
                    return location.getName();
                }
            }
        }
        Log.d("Amn Locations", "No location found");
        return null;
    }

    public void addToList(ArrayList<DialogOption> dialogOptionslist, Map<String, TreeNode<String, Location>> locationMap) {
        for (Map.Entry<String, TreeNode<String, Location>> entry : locationMap.entrySet()) {

            if (entry.getValue().getChildren() != null) {
                addToList(dialogOptionslist, entry.getValue().getChildren());
            } else {
                StringUtil.humanize(entry.getValue().getLabel());
                String name = StringUtil.humanize(entry.getValue().getLabel());
                dialogOptionslist.add(new CommonObjectFilterOption(name.replace(" ", "_"), "location_name", CommonObjectFilterOption.ByColumnAndByDetails.byDetails, name));
            }
        }
    }

    public enum ByColumnAndByDetails {
        byColumn, byDetails, byDefault
    }

    public void updateSearchView() {
        getSearchView().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(final CharSequence cs, int start, int before, int count) {
                (new AsyncTask() {
                    SmartRegisterClients filteredClients;

                    @Override
                    protected Object doInBackground(Object[] params) {
//                        currentSearchFilter =
                        setCurrentSearchFilter(new BasicSearchOption(cs.toString()));
                        filteredClients = getClientsAdapter().getListItemProvider()
                                .updateClients(getCurrentVillageFilter(), getCurrentServiceModeOption(),
                                        getCurrentSearchFilter(), getCurrentSortOption());
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Object o) {
                        getClientsAdapter().refreshClients(filteredClients);
                        getClientsAdapter().notifyDataSetChanged();
                        getSearchCancelView().setVisibility(isEmpty(cs) ? INVISIBLE : VISIBLE);
                        super.onPostExecute(o);
                    }
                }).execute();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }//end of method
    protected abstract String getRegistrationForm(HashMap<String, String> overridemap);

    protected abstract String getOAFollowupForm(Client client, HashMap<String, String> overridemap);

    protected Map<String, String> providerOverrides(){
        return Utils.providerDetails();
    }

    protected abstract Map<String, String> customFieldOverrides();

    protected void showMessageDialog(String message, DialogInterface.OnClickListener ok) {
        AlertDialog dialog = new AlertDialog.Builder(Context.getInstance().applicationContext())
                .setTitle(org.ei.opensrp.R.string.login_failed_dialog_title)
                .setMessage(message)
                .setPositiveButton("OK", ok)
                .create();

        dialog.show();
    }

    protected void showMessageDialog(String message, DialogInterface.OnClickListener ok, DialogInterface.OnClickListener cancel) {
        AlertDialog dialog = new AlertDialog.Builder(Context.getInstance().applicationContext())
                .setTitle(org.ei.opensrp.R.string.login_failed_dialog_title)
                .setMessage(message)
                .setPositiveButton("OK", ok)
                .setNegativeButton("Cancel", cancel)
                .create();

        dialog.show();
    }
}