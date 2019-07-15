package voxtric.com.diabetescontrol;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import voxtric.com.diabetescontrol.database.AppDatabase;
import voxtric.com.diabetescontrol.database.DatabaseActivity;
import voxtric.com.diabetescontrol.database.Preference;

public class AboutActivity extends DatabaseActivity
{
  private final int EXPAND_DURATION = 2;
  private final int COLLAPSE_DURATION = 8;

  private final HashMap<View, ExpansionState> m_expansionStates = new HashMap<>();
  private long m_expandDuration = 0;
  private long m_collapseDuration = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_about);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null)
    {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    ((TextView)findViewById(R.id.app_version_text)).setText(getString(R.string.app_version_text, BuildConfig.VERSION_NAME));
    Preference.get(this, "database_version", String.valueOf(AppDatabase.Version), new Preference.ResultRunnable()
    {
      @Override
      public void run()
      {
        ((TextView)findViewById(R.id.database_version_text)).setText(getString(R.string.database_version_text, getResult(), AppDatabase.Version));
      }
    });

    fillContent(R.id.disclaimer_text, "disclaimer.html");
    fillContent(R.id.privacy_policy_text, "privacy_policy.html");
    fillContent(R.id.open_source_information_text, "open_source_information.html");

    findViewById(R.id.root).post(new Runnable()
    {
      @Override
      public void run()
      {
        m_expansionStates.put(findViewById(R.id.disclaimer_label), new ExpansionState(findViewById(R.id.disclaimer_text)));
        m_expansionStates.put(findViewById(R.id.privacy_policy_label), new ExpansionState(findViewById(R.id.privacy_policy_text)));
        m_expansionStates.put(findViewById(R.id.open_source_information_label), new ExpansionState(findViewById(R.id.open_source_information_text)));

        toggleVisibility(findViewById(R.id.disclaimer_label));
        toggleVisibility(findViewById(R.id.privacy_policy_label));
        toggleVisibility(findViewById(R.id.open_source_information_label));

        m_expandDuration = EXPAND_DURATION;
        m_collapseDuration = COLLAPSE_DURATION;
      }
    });
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item)
  {
    if (item.getItemId() == android.R.id.home)
    {
      finish();
      return true;
    }
    return false;
  }

  private void fillContent(@IdRes int contentViewID, String assetFileName)
  {
    InputStream inputStream = null;
    try
    {
      inputStream = getAssets().open(assetFileName);
      byte[] buffer = new byte[inputStream.available()];
      //noinspection ResultOfMethodCallIgnored
      inputStream.read(buffer);
      String privacyPolicy = new String(buffer);
      ((TextView)findViewById(contentViewID)).setText(Html.fromHtml(privacyPolicy));
    }
    catch (IOException ignored) {}
    finally
    {
      if (inputStream != null)
      {
        try
        {
          inputStream.close();
        }
        catch (IOException ignored) {}
      }
    }
  }

  public void toggleVisibility(View view)
  {
    ExpansionState state = m_expansionStates.get(view);
    if (state != null)
    {
      if (state.activeAnimator != null)
      {
        state.activeAnimator.cancel();
      }

      ValueAnimator valueAnimator;
      if (state.expanding)
      {
        valueAnimator = collapse(state.view, m_collapseDuration, 0);
      }
      else
      {
        valueAnimator = expand(state.view, m_expandDuration, state.fullHeight);
      }
      state.expanding = !state.expanding;
      state.activeAnimator = valueAnimator;
    }
  }

  public ValueAnimator expand(final View view, long duration, int targetHeight)
  {
    int previousHeight = view.getHeight();
    view.setVisibility(View.VISIBLE);
    ValueAnimator valueAnimator = ValueAnimator.ofInt(previousHeight, targetHeight);
    valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
    {
      @Override
      public void onAnimationUpdate(ValueAnimator animation)
      {
        view.getLayoutParams().height = (int)animation.getAnimatedValue();
        view.requestLayout();
      }
    });
    valueAnimator.setInterpolator(new DecelerateInterpolator());
    valueAnimator.setDuration(targetHeight / duration);
    valueAnimator.start();

    return valueAnimator;
  }

  public ValueAnimator collapse(final View view, long duration, int targetHeight)
  {
    int previousHeight = view.getHeight();
    ValueAnimator valueAnimator = ValueAnimator.ofInt(previousHeight, targetHeight);
    valueAnimator.setInterpolator(new DecelerateInterpolator());
    valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
    {
      @Override
      public void onAnimationUpdate(ValueAnimator animation)
      {
        view.getLayoutParams().height = (int) animation.getAnimatedValue();
        view.requestLayout();
      }
    });
    valueAnimator.setInterpolator(new DecelerateInterpolator());
    if (duration == 0)
    {
      valueAnimator.setDuration(0);
    }
    else
    {
      valueAnimator.setDuration(previousHeight / duration);
    }
    valueAnimator.start();

    return valueAnimator;
  }

  private class ExpansionState
  {
    final View view;
    final int fullHeight;
    boolean expanding;
    ValueAnimator activeAnimator;

    ExpansionState(View view)
    {
      this.view = view;
      this.fullHeight = view.getHeight();
      this.expanding = true;
      this.activeAnimator = null;
    }
  }
}
