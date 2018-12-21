package voxtric.com.diabetescontrol;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Pair;

import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import voxtric.com.diabetescontrol.database.AppDatabase;
import voxtric.com.diabetescontrol.database.DataEntry;
import voxtric.com.diabetescontrol.database.TargetChange;

public class ADSExporter extends PDFGenerator
{
    private static final float DAY_HEADER_WIDTH = 30.0f;
    private static final float DAY_HEADER_HEIGHT = 80.0f;
    private static final float EVENT_MAX_WIDTH = 60.0f;
    private static final float EVENT_HEADER_HEIGHT = 120.0f;
    private static final float EXTRAS_MIN_WIDTH = 140.0f;
    private static final float DATA_GAP = FONT_SIZE_MEDIUM * 1.6f;

    private final AppDatabase m_database;
    private final List<Week> m_weeks;
    private final String m_fileName;

    ADSExporter(List<DataEntry> entries, AppDatabase database)
    {
        m_database = database;
        m_weeks = Week.splitEntries(entries);

        Date startDate = new Date(entries.get(entries.size() - 1).timeStamp);
        Date endDate = new Date(entries.get(0).timeStamp);
        String startDateString = DateFormat.getDateInstance(DateFormat.MEDIUM).format(startDate);
        String endDateString = DateFormat.getDateInstance(DateFormat.MEDIUM).format(endDate);
        if (startDateString.equals(endDateString))
        {
            m_fileName = String.format("%s (ADS).pdf", startDateString);
        }
        else
        {
            m_fileName = String.format("%s - %s (ADS).pdf", startDateString, endDateString);
        }
    }

    @Override
    public String getFileName()
    {
        return m_fileName;
    }

    @Override
    public ByteArrayOutputStream createPDF(Activity activity)
    {
        try
        {
            for (Week week : m_weeks)
            {
                addPage(week, activity);
            }
            return getOutputStream();
        }
        catch (IOException exception)
        {
            exception.printStackTrace();
            return null;
        }
    }

    private void showExtras(StringBuilder foodEatenStringBuilder, StringBuilder additionalNotesStringBuilder, float startX, float availableSpace, float height) throws IOException
    {
        if (foodEatenStringBuilder.length() > 0)
        {
            height = drawText(FONT_BOLD, FONT_SIZE_SMALL, "Food Eaten:", startX + LINE_SPACING, height);
            String foodEatenString = foodEatenStringBuilder.substring(0, foodEatenStringBuilder.length() - 2);
            height = drawTextParagraphed(FONT, FONT_SIZE_SMALL, foodEatenString, startX + LINE_SPACING, startX + availableSpace - LINE_SPACING, height);
            foodEatenStringBuilder.setLength(0);
        }
        if (additionalNotesStringBuilder.length() > 0)
        {
            height = drawText(FONT_BOLD, FONT_SIZE_SMALL, "Additional Notes:", startX + LINE_SPACING, height);
            String additionalNotesString = additionalNotesStringBuilder.substring(0, additionalNotesStringBuilder.length() - 1);
            drawTextParagraphed(FONT, FONT_SIZE_SMALL, additionalNotesString, startX + LINE_SPACING, startX + availableSpace - LINE_SPACING, height);
            additionalNotesStringBuilder.setLength(0);
        }
    }

    private void addPage(Week week, Activity activity) throws IOException
    {
        super.addPage();
        final String[] DAYS = activity.getResources().getStringArray(R.array.days);
        float height = PDRectangle.A4.getHeight() - BORDER;

        // Week Commencing.
        Date date = new Date(week.weekBeginning);
        String dateString = String.format("Week Commencing: %s", DateFormat.getDateInstance(DateFormat.SHORT).format(date));
        drawText(FONT, FONT_SIZE_MEDIUM, dateString, BORDER, height);

        // Pre-meal and post-meal targets.
        String targetString = null;
        TargetChange targetChange = m_database.targetChangesDao().findChangeBetween(week.weekBeginning, week.weekEnding);
        if (targetChange == null)
        {
            targetChange = m_database.targetChangesDao().findFirstBefore(week.weekBeginning);
            if (targetChange == null)
            {
                targetString = "Blood Glucose Targets (mmol/l): Pre-meal ........., Post-meal .........";
            }
        }
        if (targetChange != null)
        {
            targetString = String.format(Locale.getDefault(), "Blood Glucose Targets (mmol/l): Pre-meal %f - %f, Post-meal %f - %f",
                                         targetChange.preMealLower, targetChange.preMealUpper, targetChange.postMealLower, targetChange.postMealUpper);
        }
        height = drawText(FONT, FONT_SIZE_MEDIUM, targetString, BORDER + (m_pageWidth / 3.0f), height);

        // Event boxes for each day.
        Map<String, Float> eventStartXMap = new HashMap<>();
        height -= VERTICAL_SPACE;
        float eventWidth = Math.min((m_pageWidth - DAY_HEADER_WIDTH - EXTRAS_MIN_WIDTH) / week.events.size(), EVENT_MAX_WIDTH);
        float startX = BORDER + DAY_HEADER_WIDTH;
        for (Pair<String, Long> event : week.events)
        {
            eventStartXMap.put(event.first, startX);
            drawBox(startX, height, startX + eventWidth, height - EVENT_HEADER_HEIGHT, BLACK, null);
            //drawTextCentered(FONT, FONT_SIZE_LARGE, event.first, 90.0f, startX + (eventWidth / 2.0f), height - (EVENT_HEADER_HEIGHT / 2.0f));
            drawCenteredTextParagraphed(FONT, FONT_SIZE_LARGE, event.first, 90.0f,
                                        startX + (eventWidth / 2.0f), height - (EVENT_HEADER_HEIGHT / 2.0f), EVENT_HEADER_HEIGHT - (LINE_SPACING * 2.0f));

            for (int i = 0; i < DAYS.length; i++)
            {
                float dayHeight = height - EVENT_HEADER_HEIGHT - (DAY_HEADER_HEIGHT * (float)i);
                drawBox(startX, dayHeight, startX + eventWidth, dayHeight - DAY_HEADER_HEIGHT, BLACK, null);

                dayHeight -= 1.0f;
                dayHeight = drawTextCenterAligned(FONT, FONT_SIZE_SMALL, "Reading", startX + (eventWidth / 2.0f), dayHeight) - DATA_GAP;
                dayHeight = drawTextCenterAligned(FONT, FONT_SIZE_SMALL, "Time", startX + (eventWidth / 2.0f), dayHeight) - DATA_GAP;
                drawTextCenterAligned(FONT, FONT_SIZE_SMALL, "Dose", startX + (eventWidth / 2.0f), dayHeight);
            }

            startX += eventWidth;
        }
        height -= EVENT_HEADER_HEIGHT;
        float availableSpace = m_pageWidth - startX + BORDER;

        // Day headers and extras.
        float tempHeight = height + (FONT_SIZE_LARGE * 2.2f);
        drawBox(startX, tempHeight, startX + availableSpace, height, BLACK, null);
        tempHeight = drawTextCenterAligned(FONT, FONT_SIZE_LARGE, "Food Eaten /", startX + (availableSpace / 2.0f), tempHeight);
        drawTextCenterAligned(FONT, FONT_SIZE_LARGE, "Additional Notes", startX + (availableSpace / 2.0f), tempHeight);
        for (int i = 0; i < DAYS.length; i++)
        {
            float dayHeight = height - (DAY_HEADER_HEIGHT * i);
            drawBox(BORDER, dayHeight, BORDER + DAY_HEADER_WIDTH, dayHeight - DAY_HEADER_HEIGHT, BLACK, null);
            drawTextCentered(FONT, FONT_SIZE_LARGE, DAYS[i], 90.0f, BORDER + (DAY_HEADER_WIDTH / 2.0f), dayHeight - (DAY_HEADER_HEIGHT / 2.0f));
            drawBox(startX, dayHeight, startX + availableSpace, dayHeight - DAY_HEADER_HEIGHT, BLACK, null);
        }

        // Data
        int lastDayOfWeek = -1;
        float dayStartHeight = height;
        StringBuilder foodEatenStringBuilder = new StringBuilder();
        StringBuilder additionalNotesStringBuilder = new StringBuilder();
        for (int i = 0; i < week.entries.size(); i++)
        {
            DataEntry entry = week.entries.get(i);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(entry.timeStamp);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.clear(Calendar.MINUTE);
            calendar.clear(Calendar.SECOND);
            calendar.clear(Calendar.MILLISECOND);

            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 2; // Should be -1, but only works with -2 for some reason.
            dayStartHeight = height - (DAY_HEADER_HEIGHT * dayOfWeek);
            float dayHeight = dayStartHeight - FONT_SIZE_SMALL - (FONT_SIZE_MEDIUM * 0.2f);
            float eventStartX = eventStartXMap.get(entry.event);
            drawTextCenterAligned(FONT, FONT_SIZE_MEDIUM, String.valueOf(entry.bloodGlucoseLevel), eventStartX + (eventWidth / 2.0f), dayHeight);

            dayHeight -= FONT_SIZE_SMALL + DATA_GAP;
            date = new Date(entry.timeStamp);
            String timeString = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
            drawTextCenterAligned(FONT, FONT_SIZE_MEDIUM, timeString, eventStartX + (eventWidth / 2.0f), dayHeight);

            dayHeight -= FONT_SIZE_SMALL + DATA_GAP;
            drawTextCenterAligned(FONT, FONT_SIZE_MEDIUM, entry.insulinDose, eventStartX + (eventWidth / 2.0f), dayHeight);

            // Food eaten and additional notes.
            if (lastDayOfWeek != -1 && lastDayOfWeek != dayOfWeek)
            {
                showExtras(foodEatenStringBuilder, additionalNotesStringBuilder, startX, availableSpace, dayStartHeight);
            }
            lastDayOfWeek = dayOfWeek;

            if (entry.foodEaten.length() > 0)
            {
                foodEatenStringBuilder.append(entry.foodEaten.replaceAll("\n", ", "));
                foodEatenStringBuilder.append(", ");
            }
            if (entry.additionalNotes.length() > 0)
            {
                additionalNotesStringBuilder.append(entry.additionalNotes);
                additionalNotesStringBuilder.append('\n');
            }
        }
        showExtras(foodEatenStringBuilder, additionalNotesStringBuilder, startX, availableSpace, dayStartHeight);
        height -= DAY_HEADER_HEIGHT * DAYS.length;

        // Insulin used
        height -= VERTICAL_SPACE;
        StringBuilder insulinUsedStringBuilder = new StringBuilder("Insulin Used: ");
        for (String insulinName : week.insulinNames)
        {
            insulinUsedStringBuilder.append(insulinName);
            insulinUsedStringBuilder.append(", ");
        }
        String insulinUsedString = insulinUsedStringBuilder.substring(0, insulinUsedStringBuilder.length() - 2);
        height = drawText(FONT, FONT_SIZE_MEDIUM, insulinUsedString, BORDER, height);

        // Contact Details
        height -= VERTICAL_SPACE / 2.0f;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String contactName = preferences.getString("contact_name", null);
        if (contactName == null)
        {
            contactName = "...........................................";
        }
        String contactNameString = String.format("Contact Name: %s", contactName);
        drawText(FONT, FONT_SIZE_MEDIUM, contactNameString, BORDER, height);

        String contactNumber = preferences.getString("contact_number", null);
        if (contactNumber == null)
        {
            contactNumber = "...........................................";
        }
        String contactNumberString = String.format("Contact Number: %s", contactNumber);
        drawText(FONT, FONT_SIZE_MEDIUM, contactNumberString, BORDER + (m_pageWidth / 2.0f), height);
    }
}
