package org.example.CarbonStorehouseBot.sorted;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class MyComparator implements Comparator<List<Object[]>> {

    @Override
    public int compare(List<Object[]> list1, List<Object[]> list2) {

        Object [] array1 = list1.get(0);
        java.sql.Date date1 =  (java.sql.Date) array1[4];
        LocalDate localDate1 = date1.toLocalDate();

        Object [] array2 = list2.get(0);
        java.sql.Date date2 =  (java.sql.Date) array2[4];
        LocalDate localDate2 = date2.toLocalDate();

        int result = localDate1.compareTo(localDate2);

        if (result < 0) {
            return 1;
        } else if (result > 0) {
            return -1;
        } else {
            return 0;
        }


      /*  if (localDate1.isBefore(localDate2)) {
            return 1;
        } else if (localDate1.isAfter(localDate2)) {
            return -1;
        } else {
            return 0;
        }*/
    }
}
