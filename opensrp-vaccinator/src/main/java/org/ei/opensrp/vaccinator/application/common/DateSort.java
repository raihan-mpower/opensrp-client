package org.ei.opensrp.vaccinator.application.common;

import org.ei.opensrp.commonregistry.CommonPersonObjectClient;
import org.ei.opensrp.view.contract.SmartRegisterClient;
import org.ei.opensrp.view.contract.SmartRegisterClients;
import org.ei.opensrp.view.dialog.SortOption;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class DateSort implements SortOption {


    private String name;
    String field;
    ByColumnAndByDetails byColumnAndByDetails;

    public enum ByColumnAndByDetails {
        byColumn, byDetails;
    }

    public DateSort(ByColumnAndByDetails byColumnAndByDetails, String name, String field) {
        this.byColumnAndByDetails = byColumnAndByDetails;
        this.field = field;
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public SmartRegisterClients sort(SmartRegisterClients allClients) {
        Collections.sort(allClients, commoncomparator);
        return allClients;
    }

    Comparator<SmartRegisterClient> commoncomparator = new Comparator<SmartRegisterClient>() {
        @Override
        public int compare(SmartRegisterClient oneClient, SmartRegisterClient anotherClient2) {
            CommonPersonObjectClient commonPersonObjectClient = (CommonPersonObjectClient) oneClient;
            CommonPersonObjectClient commonPersonObjectClient2 = (CommonPersonObjectClient) anotherClient2;
            switch (byColumnAndByDetails) {
                case byColumn:
                    try {
                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        Date date1 = dateFormat.parse(commonPersonObjectClient.getColumnmaps().get(field));
                        Date date2 = dateFormat.parse(commonPersonObjectClient2.getColumnmaps().get(field));

                        return date2.compareTo(date1);
                    } catch (Exception e) {
                        if(commonPersonObjectClient.getColumnmaps().get(field) == null){
                            return -1;
                        }
                        else return 1;
                    }

                case byDetails:
                    try {
                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        Date date1 = dateFormat.parse(commonPersonObjectClient.getDetails().get(field));
                        Date date2 = dateFormat.parse(commonPersonObjectClient2.getDetails().get(field));
                        return date2.compareTo(date1);
                    } catch (Exception e) {
                        if(commonPersonObjectClient.getDetails().get(field) == null){
                            return -1;
                        }
                        else return 1;
                    }
            }
            return 0;
        }
    };
}
