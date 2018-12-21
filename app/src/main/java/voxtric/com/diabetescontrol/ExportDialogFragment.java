package voxtric.com.diabetescontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.TextView;

public class ExportDialogFragment extends DialogFragment
{
    String m_title = null;
    String m_message = null;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        if (savedInstanceState != null)
        {
            m_title = savedInstanceState.getString("title");
            m_message = savedInstanceState.getString("message");
        }

        Activity activity = getActivity();
        View view = View.inflate(activity, R.layout.dialog_export_pdf, null);
        ((TextView)view.findViewById(R.id.text_view_message)).setText(m_message);
        final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                .setTitle(m_title)
                .setView(view)
                .setNegativeButton("Finish", null)
                .setPositiveButton("Share", null)
                .create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface dialog)
            {
                alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            }
        });
        return alertDialog;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString("title", m_title);
        outState.putString("message", m_message);
    }

    public void setText(String title, String message)
    {
        m_title = title;
        m_message = message;
    }
}
