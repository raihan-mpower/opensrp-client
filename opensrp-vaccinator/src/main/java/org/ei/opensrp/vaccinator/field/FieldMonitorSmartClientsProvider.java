package org.ei.opensrp.vaccinator.field;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;

import org.ei.opensrp.commonregistry.CommonPersonObjectClient;
import org.ei.opensrp.commonregistry.CommonPersonObjectController;
import org.ei.opensrp.provider.SmartRegisterClientsProvider;
import org.ei.opensrp.service.AlertService;
import org.ei.opensrp.vaccinator.R;
import org.ei.opensrp.vaccinator.application.template.SmartRegisterFragment;
import org.ei.opensrp.view.contract.SmartRegisterClient;
import org.ei.opensrp.view.contract.SmartRegisterClients;
import org.ei.opensrp.view.dialog.FilterOption;
import org.ei.opensrp.view.dialog.ServiceModeOption;
import org.ei.opensrp.view.dialog.SortOption;
import org.ei.opensrp.view.viewHolder.OnClickFormLauncher;
import org.joda.time.DateTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import static util.Utils.addToRow;
import static util.Utils.getDataRow;
import static util.Utils.getValue;

public class FieldMonitorSmartClientsProvider implements SmartRegisterClientsProvider {

    private final LayoutInflater inflater;
    private final Context context;
    private final OnClickListener onClickListener;
    AlertService alertService;
    //private final AbsListView.LayoutParams clientViewLayoutParams;
    private org.ei.opensrp.Context context1;

    private ByMonthByDay byMonthlyAndByDaily;

    public enum ByMonthByDay {ByMonth, ByDay}

    public SmartRegisterFragment parent;

    protected CommonPersonObjectController controller;

    public FieldMonitorSmartClientsProvider(Context context, OnClickListener onClickListener, CommonPersonObjectController controller,
                                            AlertService alertService, ByMonthByDay byMonthlyAndByDaily,
                                            org.ei.opensrp.Context context1, SmartRegisterFragment parent) {
        this.onClickListener = onClickListener;
        this.controller = controller;
        this.context = context;
        this.context1 = context1;
        this.alertService = alertService;
        this.byMonthlyAndByDaily = byMonthlyAndByDaily;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.parent = parent;

       // clientViewLayoutParams = new AbsListView.LayoutParams(MATCH_PARENT, (int) context.getResources().getDimension(org.ei.opensrp.R.dimen.list_item_height));
    }

    private int getTotalWasted(String startDate, String endDate, String type){
        int totalWasted = 0;
        for (HashMap<String, String> v: getWasted(startDate, endDate, type))
        {
            for (String k: v.keySet()) {
                totalWasted += Integer.parseInt(v.get(k) == null?"0":v.get(k));
            }
        }
        return totalWasted;
    }

    private ArrayList<HashMap<String, String>> getWasted(String startDate, String endDate, String type){
        String sqlWasted = "select sum (total_wasted)as total_wasted from stock where `report` ='"+type+"' and `date` between '" + startDate + "' and '" + endDate + "'";
        return org.ei.opensrp.Context.getInstance().commonrepository("stock").rawQuery(sqlWasted);
    }

    private int getTotalUsed(String startDate, String endDate){
        int totalUsed = 0;

        for (HashMap<String, String> v: getUsed(startDate, endDate)) {
            for (String k: v.keySet()) {
                totalUsed += Integer.parseInt(v.get(k) == null?"0":v.get(k));
            }
        }

        return totalUsed;
    }

    private ArrayList<HashMap<String, String>> getUsed(String startDate, String endDate){
        String sqlWoman = "select (" +
                "select count(*) tt1 from pkwoman where tt1 between  '" + startDate + "' and '" + endDate + "') tt1," +
                "(select count(*) tt2 from pkwoman where tt2 between '" + startDate + "' and '" + endDate + "') tt2," +
                "(select count(*) tt3 from pkwoman where tt3 between '" + startDate + "' and '" + endDate + "') tt3," +
                "(select count(*) tt4 from pkwoman where tt4 between '" + startDate + "' and '" + endDate + "') tt4," +
                "(select count(*) tt5 from pkwoman where tt5 between '" + startDate + "' and '" + endDate + "') tt5 " +
                "from pkwoman limit 1; ";

        String sqlChild = "select (" +
                "select count(*) c from pkchild where bcg between '" + startDate + "' and '" + endDate + "') bcg," +
                "(select count(*) c from pkchild where opv0 between '" + startDate + "' and '" + endDate + "') opv_0," +
                "(select count(*) c from pkchild where opv1 between '" + startDate + "' and '" + endDate + "') opv_1," +
                "(select count(*) c from pkchild where opv2 between '" + startDate + "' and '" + endDate + "') opv_2," +
                "(select count(*) c from pkchild where opv3 between '" + startDate + "' and '" + endDate + "') opv_3, " +
                "(select count(*) c from pkchild where pcv1 between '" + startDate + "' and '" + endDate + "') pcv_1," +
                "(select count(*) c from pkchild where pcv2 between '" + startDate + "' and '" + endDate + "') pcv_2," +
                "(select count(*) c from pkchild where pcv3 between '" + startDate + "' and '" + endDate + "') pcv_3, " +
                "(select count(*) c from pkchild where measles1 between '" + startDate + "' and '" + endDate + "') measles_1, " +
                "(select count(*) c from pkchild where measles2 between '" + startDate + "' and '" + endDate + "') measles_2," +
                "(select count(*) c from pkchild where penta1 between '" + startDate + "' and '" + endDate + "') pentavalent_1," +
                "(select count(*) c from pkchild where penta2 between '" + startDate + "' and '" + endDate + "') pentavalent_2," +
                "(select count(*) c from pkchild where penta3 between '" + startDate + "' and '" + endDate + "') pentavalent_3 " +
                "from pkchild limit 1 ;";

        ArrayList<HashMap<String, String>> ttVaccinesUsed = org.ei.opensrp.Context.getInstance().commonrepository("pkwoman").rawQuery(sqlWoman);
        ArrayList<HashMap<String, String>> childVaccinesUsed = org.ei.opensrp.Context.getInstance().commonrepository("pkchild").rawQuery(sqlChild);

        ttVaccinesUsed.addAll(childVaccinesUsed);
        return ttVaccinesUsed;
    }
    
    @Override
    public View getView(SmartRegisterClient client, View parentView, ViewGroup viewGroup) {
        CommonPersonObjectClient pc = (CommonPersonObjectClient) client;
        parentView = (ViewGroup) inflater().inflate(R.layout.smart_register_field_client, null);

        String date_entered = pc.getColumnmaps().get("date");

        DateTime date = null;
        try {
            date = new DateTime(new SimpleDateFormat("yyyy-MM-dd").parse(date_entered).getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        TableLayout dt = (TableLayout) parentView.findViewById(R.id.stock_vaccine_table);
        dt.removeAllViewsInLayout();
        TableRow tr = getDataRow(context);

        if(byMonthlyAndByDaily.equals(ByMonthByDay.ByMonth)){

            String startDate = date.withDayOfMonth(1).toString("yyyy-MM-dd");
            String endDate = date.withDayOfMonth(1).plusMonths(1).minusDays(1).toString("yyyy-MM-dd");

            addToRow(context, date.toString("MMMM (yyyy)"), tr);
            addToRow(context, pc.getDetails().get("Target_assigned_for_vaccination_at_each_month"), tr);

            int bcgBalanceInHand = Integer.parseInt(getValue(pc, "bcg_balance_in_hand", "0", false));
            int bcgReceived = Integer.parseInt(getValue(pc, "bcg_received", "0", false));

            int opv_balance_in_hand = Integer.parseInt(getValue(pc, "opv_balance_in_hand", "0", false));
            int opv_received = Integer.parseInt(getValue(pc, "opv_received", "0", false));

            int ipv_balance_in_hand = Integer.parseInt(getValue(pc, "ipv_balance_in_hand", "0", false));
            int ipv_received = Integer.parseInt(getValue(pc, "ipv_received", "0", false));

            int pcv_balance_in_hand = Integer.parseInt(getValue(pc, "pcv_balance_in_hand", "0", false));
            int pcv_received = Integer.parseInt(getValue(pc, "pcv_received", "0", false));

            int penta_balance_in_hand = Integer.parseInt(getValue(pc, "penta_balance_in_hand", "0", false));
            int penta_received = Integer.parseInt(getValue(pc, "penta_received", "0", false));

            int measles_balance_in_hand = Integer.parseInt(getValue(pc, "measles_balance_in_hand", "0", false));
            int measles_received = Integer.parseInt(getValue(pc, "measles_received", "0", false));

            int tt_balance_in_hand = Integer.parseInt(getValue(pc, "tt_balance_in_hand", "0", false));
            int tt_received = Integer.parseInt(getValue(pc, "tt_received", "0", false));

//                int dilutants_balance_in_hand = Integer.parseInt(getValue(pc, "dilutants_balance_in_hand", "0", false)("dilutants_balance_in_hand") : "0");
//              int dilutants_received = Integer.parseInt(getValue(pc, "dilutants_received", "0", false)("dilutants_received") : "0");

//            int syringes_balance_in_hand = Integer.parseInt(getValue(pc, "syringes_balance_in_hand", "0", false)("syringes_balance_in_hand") : "0");
  //          int syringes_received = Integer.parseInt(getValue(pc, "syringes_received", "0", false)("syringes_received") : "0");

    //        int safety_boxes_balance_in_hand = Integer.parseInt(getValue(pc, "safety_boxes_balance_in_hand", "0", false)("safety_boxes_balance_in_hand") : "0");
      //      int safety_boxes_received = Integer.parseInt(getValue(pc, "safety_boxes_received") != null ? pc.getDetails().get("safety_boxes_received") : "0");

            //#TODO get Total balance,wasted and received from total variables instead of calculating here.
            int balanceInHand = bcgBalanceInHand + opv_balance_in_hand + ipv_balance_in_hand +
                    pcv_balance_in_hand + penta_balance_in_hand + measles_balance_in_hand + tt_balance_in_hand;

            int received = bcgReceived + opv_received + ipv_received + pcv_received + penta_received +
                    measles_received + tt_received ;

            addToRow(context, received+"", tr);
            addToRow(context, (getTotalUsed(startDate, endDate))+"", tr);
            addToRow(context, getTotalWasted(startDate, endDate, "monthly")+"", tr);
            addToRow(context, balanceInHand+"", tr);

            tr.setTag(R.id.client_details_tag, client);
        }
        else if(byMonthlyAndByDaily.equals(ByMonthByDay.ByDay)){
            String startDate = date.toString("yyyy-MM-dd");
            String endDate = date.toString("yyyy-MM-dd");

            addToRow(context, date.toString("dd-MM-yyyy"), tr);
            addToRow(context, getTotalUsed(startDate, endDate) + "", tr);

            addToRow(context, getTotalWasted(startDate, endDate, "daily") + "", tr);

            tr.setTag(R.id.client_details_tag, client);
        }

        tr.setId(R.id.stock_detail_holder);
        tr.setOnClickListener(onClickListener);
        dt.addView(tr);
        //parentView.setLayoutParams(clientViewLayoutParams);
        return parentView;
    }


    @Override
    public SmartRegisterClients getClients() {
        return controller.getClients();
    }

    @Override
    public SmartRegisterClients updateClients(FilterOption villageFilter, ServiceModeOption serviceModeOption, FilterOption searchFilter, SortOption sortOption) {
        return getClients().applyFilter(villageFilter, serviceModeOption, searchFilter, sortOption);
    }

    @Override
    public void onServiceModeSelected(ServiceModeOption serviceModeOption) {
        parent.refreshListView();
    }

    @Override
    public OnClickFormLauncher newFormLauncher(String formName, String entityId, String metaData) {
        return null;
    }

    public LayoutInflater inflater() {
        return inflater;
    }
}
