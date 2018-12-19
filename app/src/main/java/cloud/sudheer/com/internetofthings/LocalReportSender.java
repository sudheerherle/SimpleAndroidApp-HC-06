package cloud.sudheer.com.internetofthings;

/**
 * Created by I14746 on 4/24/2015.
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.acra.ACRA;
import org.acra.ACRAConstants;
import org.acra.ReportField;
import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class LocalReportSender implements ReportSender {

    private final Map<ReportField, String> mMapping = new HashMap<ReportField, String>();
    private FileWriter crashReport = null;

    public LocalReportSender(Context ctx) {
        // the destination
        File logFile = new File(Environment.getExternalStorageDirectory(), "log.txt");

        try {
            crashReport = new FileWriter(logFile, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public void send(CrashReportData report) throws ReportSenderException {
//        final Map<String, String> finalReport = remap(report);
//
//        try {
//            BufferedWriter buf = new BufferedWriter(crashReport);
//
//            Set<Entry<String, String>> set = finalReport.entrySet();
//            Iterator<Entry<String, String>> i = set.iterator();
//
//            while (i.hasNext()) {
//                Map.Entry<String, String> me = (Entry<String, String>) i.next();
//                buf.append("[" + me.getKey() + "]=" + me.getValue());
//            }
//
//            buf.flush();
//            buf.close();
//        } catch (IOException e) {
//            Log.e("TAG", "IO ERROR", e);
//        }
//    }

    private boolean isNull(String aString) {
        return aString == null || ACRAConstants.NULL_VALUE.equals(aString);
    }

    private Map<String, String> remap(Map<ReportField, String> report) {

        ReportField[] fields = ACRA.getConfig().customReportContent();
        if (fields.length == 0) {
            fields = ACRAConstants.DEFAULT_REPORT_FIELDS;
        }

        final Map<String, String> finalReport = new HashMap<String, String>(
                report.size());
        for (ReportField field : fields) {
            if (mMapping == null || mMapping.get(field) == null) {
                finalReport.put(field.toString(), report.get(field));
            } else {
                finalReport.put(mMapping.get(field), report.get(field));
            }
        }
        return finalReport;
    }

    @Override
    public void send(Context context, CrashReportData crashReportData) throws ReportSenderException {
//        Map<ReportField,String> report = null;
        final Map<String, String> finalReport = remap(crashReportData);

        try {
            BufferedWriter buf = new BufferedWriter(crashReport);

            Set<Map.Entry<String, String>> set = finalReport.entrySet();
            Iterator<Map.Entry<String, String>> i = set.iterator();

            while (i.hasNext()) {
                Map.Entry<String, String> me = (Map.Entry<String, String>) i.next();
                buf.append("[" + me.getKey() + "]=" + me.getValue());
            }

            buf.flush();
            buf.close();
        } catch (IOException e) {
            Log.e("TAG", "IO ERROR", e);
        }
    }
}