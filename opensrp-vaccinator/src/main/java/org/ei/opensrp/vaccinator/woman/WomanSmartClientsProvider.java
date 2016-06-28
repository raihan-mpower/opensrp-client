package org.ei.opensrp.vaccinator.woman;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.ei.opensrp.commonregistry.CommonPersonObjectClient;
import org.ei.opensrp.commonregistry.CommonPersonObjectController;
import org.ei.opensrp.domain.Alert;
import org.ei.opensrp.provider.SmartRegisterClientsProvider;
import org.ei.opensrp.service.AlertService;
import org.ei.opensrp.util.StringUtil;
import org.ei.opensrp.vaccinator.R;
import org.ei.opensrp.vaccinator.db.VaccineRepo;
import org.ei.opensrp.view.contract.SmartRegisterClient;
import org.ei.opensrp.view.contract.SmartRegisterClients;
import org.ei.opensrp.view.dialog.FilterOption;
import org.ei.opensrp.view.dialog.ServiceModeOption;
import org.ei.opensrp.view.dialog.SortOption;
import org.ei.opensrp.view.viewHolder.OnClickFormLauncher;
import org.joda.time.DateTime;
import org.joda.time.Years;

import java.util.List;
import java.util.Map;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static util.Utils.*;
import static util.Utils.fillValue;
import static util.Utils.generateSchedule;
import static util.Utils.getValue;
import static util.Utils.nextVaccineDue;
import static util.Utils.nonEmptyValue;
import static util.Utils.setProfiePic;

/**
 * Created by Ahmed on 19-Oct-15.
 */
public class WomanSmartClientsProvider implements SmartRegisterClientsProvider {


    private final LayoutInflater inflater;
    private final Context context;
    private final View.OnClickListener onClickListener;
    AlertService alertService;
    private final int txtColorBlack;
    private final AbsListView.LayoutParams clientViewLayoutParams;

    protected CommonPersonObjectController controller;

    public WomanSmartClientsProvider(Context context, View.OnClickListener onClickListener,
                                     CommonPersonObjectController controller, AlertService alertService) {
        this.onClickListener = onClickListener;
        this.controller = controller;
        this.context = context;
        this.alertService = alertService;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        clientViewLayoutParams = new AbsListView.LayoutParams(MATCH_PARENT, (int) context.getResources().getDimension(org.ei.opensrp.R.dimen.list_item_height));
        txtColorBlack = context.getResources().getColor(org.ei.opensrp.R.color.text_black);
    }


    @Override
    public View getView(SmartRegisterClient client, View convertView, ViewGroup viewGroup) {
        CommonPersonObjectClient pc = (CommonPersonObjectClient) client;

        convertView = (ViewGroup) inflater().inflate(R.layout.smart_register_woman_client, null);
        fillValue((TextView)convertView.findViewById(R.id.woman_id), pc, "existing_program_client_id", false);
        fillValue((TextView) convertView.findViewById(R.id.woman_name), getValue(pc, "first_name", true));
        fillValue((TextView) convertView.findViewById(R.id.woman_husbandname), getValue(pc, "husband_name", true));
        fillValue((TextView) convertView.findViewById(R.id.woman_fathername), getValue(pc, "father_name", true));

        int age = -1;
        try{
            age = Years.yearsBetween(new DateTime(getValue(pc.getColumnmaps(), "dob", false)), DateTime.now()).getYears();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        fillValue((TextView) convertView.findViewById(R.id.woman_age),  age<0?"No DoB":(age+ " years"));
        fillValue((TextView) convertView.findViewById(R.id.woman_epi_number), pc, "epi_card_number", false);
        String edd = convertDateFormat(getValue(pc, "final_edd", false),true);
        fillValue((TextView) convertView.findViewById(R.id.woman_edd), edd == ""?"N/A":edd);
        String ga = getValue(pc, "final_ga", false);
        ga = ga==""?"N/A":(ga+" weeks");
        fillValue((TextView) convertView.findViewById(R.id.woman_ga), ga);

        //convertView.setTag(viewHolder);

        String vaccineretro = getValue(pc.getColumnmaps(), "vaccines", false);
        String vaccine2 = getValue(pc.getColumnmaps(), "vaccines_2", false);

        fillValue((TextView) convertView.findViewById(R.id.woman_last_vaccine), vaccine2);
        String lastVaccine = convertDateFormat(nonEmptyValue(pc.getColumnmaps(), true, false, new String[]{"tt1", "tt2", "tt3", "tt4", "tt5"}), true);

        fillValue((TextView) convertView.findViewById(R.id.woman_last_visit_date), lastVaccine);

        List<Alert> alertlist_for_client = alertService.findByEntityIdAndAlertNames(pc.entityId(), "TT 1", "TT 2", "TT 3", "TT 4", "TT 5", "tt1", "tt2", "tt3", "tt4", "tt5");

        if(age < 0){
            deactivateNextVaccine("Invalid DoB", "", R.color.alert_na, convertView);
        }
        else if(StringUtils.isNotBlank(getValue(pc.getColumnmaps(), "tt5", false))){
            deactivateNextVaccine("Fully Immunized", "", R.color.alert_na, convertView);
        }
        else if(age > 49 && StringUtils.isBlank(getValue(pc.getColumnmaps(), "tt5", false))){
            deactivateNextVaccine("Partially Immunized", "", R.color.alert_na, convertView);
        }
        else {
            List<Map<String, Object>> sch = generateSchedule("woman", null, pc.getColumnmaps(), alertlist_for_client);
            Map<String, Object> nv = nextVaccineDue(sch, toDate(lastVaccine, true));
            if(nv != null){
                DateTime dueDate = (DateTime)nv.get("date");
                VaccineRepo.Vaccine vaccine = (VaccineRepo.Vaccine) nv.get("vaccine");
                if(nv.get("alert") == null){
                    activateNextVaccine(dueDate, (VaccineRepo.Vaccine)nv.get("vaccine"), Color.BLACK, R.color.alert_na, onClickListener, client, convertView);
                }
                else if (((Alert)nv.get("alert")).status().value().equalsIgnoreCase("normal")) {
                    activateNextVaccine(dueDate, vaccine, Color.WHITE, R.color.alert_normal, onClickListener, client, convertView);
                }
                else if (((Alert)nv.get("alert")).status().value().equalsIgnoreCase("upcoming")) {
                    activateNextVaccine(dueDate, vaccine, Color.BLACK, R.color.alert_upcoming, onClickListener, client, convertView);
                }
                else if (((Alert)nv.get("alert")).status().value().equalsIgnoreCase("urgent")) {
                    activateNextVaccine(dueDate, vaccine, Color.WHITE, R.color.alert_urgent, onClickListener, client, convertView);
                }
                else if (((Alert)nv.get("alert")).status().value().equalsIgnoreCase("expired")) {
                    deactivateNextVaccine(vaccine + " Expired", "", R.color.alert_expired, convertView);
                }
            }
            else {
                fillValue((TextView) convertView.findViewById(R.id.woman_next_visit_vaccine), "Waiting");
                deactivateNextVaccine("Waiting", "", R.color.alert_na, convertView);
            }
        }

        setProfiePic(convertView.getContext(), (ImageView) convertView.findViewById(R.id.woman_profilepic), client.entityId(), null);

        convertView.findViewById(R.id.woman_profile_info_layout).setTag(client);
        convertView.findViewById(R.id.woman_profile_info_layout).setOnClickListener(onClickListener);

        convertView.setLayoutParams(clientViewLayoutParams);
        return convertView;
    }

    private void deactivateNextVaccine(String vaccineViewText, String vaccineDateText, int color, View convertView){
        fillValue((TextView) convertView.findViewById(R.id.woman_next_visit_vaccine), vaccineViewText);
        ((TextView) convertView.findViewById(R.id.woman_next_visit_date)).setText(convertDateFormat(vaccineDateText, true));
        convertView.findViewById(R.id.woman_next_visit_holder).setBackgroundColor(context.getResources().getColor(color));
    }

    private void activateNextVaccine(String dueDate, String vaccine, int foreColor, int backColor, View.OnClickListener onClickListener,
                                     SmartRegisterClient client, View convertView){
        fillValue((TextView) convertView.findViewById(R.id.woman_next_visit_vaccine), vaccine==null?"":vaccine.replaceAll(" ", ""));
        fillValue((TextView) convertView.findViewById(R.id.woman_next_visit_date), convertDateFormat(dueDate, true));
        ((TextView) convertView.findViewById(R.id.woman_next_visit_vaccine)).setTextColor(foreColor);
        ((TextView) convertView.findViewById(R.id.woman_next_visit_date)).setTextColor(foreColor);

        convertView.findViewById(R.id.woman_next_visit_holder).setBackgroundColor(context.getResources().getColor(backColor));
        convertView.findViewById(R.id.woman_next_visit_holder).setOnClickListener(onClickListener);
        convertView.findViewById(R.id.woman_next_visit_holder).setTag(client);
    }

    private void activateNextVaccine(DateTime dueDate, VaccineRepo.Vaccine vaccine, int foreColor, int backColor, View.OnClickListener onClickListener,
                                     SmartRegisterClient client, View convertView){
        activateNextVaccine(dueDate==null?"":dueDate.toString("yyyy-MM-dd"), vaccine==null?"":StringUtil.humanize(vaccine.display().replaceAll(" ", "")), foreColor, backColor, onClickListener, client, convertView);
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

    }

    @Override
    public OnClickFormLauncher newFormLauncher(String formName, String entityId, String metaData) {
        return null;
    }

    public LayoutInflater inflater() {
        return inflater;
    }

}
