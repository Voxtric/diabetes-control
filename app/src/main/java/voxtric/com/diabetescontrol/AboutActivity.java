package voxtric.com.diabetescontrol;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import voxtric.com.diabetescontrol.database.AppDatabase;
import voxtric.com.diabetescontrol.database.Preference;

public class AboutActivity extends AwaitRecoveryActivity
{
  private final int EXPAND_COLLAPSE_DURATION = 2;

  private final HashMap<View, ExpansionState> m_expansionStates = new HashMap<>();

  private int m_expandCollapseDuration = 0;

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

    if (!RecoveryForegroundService.isDownloading())
    {
      ((TextView)findViewById(R.id.app_version_text)).setText(getString(R.string.app_version_text, BuildConfig.VERSION_NAME));
      Preference.get(this, "database_version", String.valueOf(AppDatabase.VERSION), new Preference.ResultRunnable()
      {
        @Override
        public void run()
        {
          ((TextView)findViewById(R.id.database_version_text)).setText(getString(R.string.database_version_text,
                                                                                 getResult(),
                                                                                 AppDatabase.VERSION));
        }
      });
    }

    fillContent(R.id.disclaimer_text, "disclaimer.html");
    fillContent(R.id.privacy_policy_text, "privacy_policy.html");
    fillContent(R.id.open_source_information_text, "open_source_information.html");

    createDonationLinkButtons();

    findViewById(R.id.root).post(new Runnable()
    {
      @Override
      public void run()
      {
        m_expansionStates.put(findViewById(R.id.disclaimer_layout), new ExpansionState(findViewById(R.id.disclaimer_text)));
        m_expansionStates.put(findViewById(R.id.privacy_policy_layout), new ExpansionState(findViewById(R.id.privacy_policy_text)));
        m_expansionStates.put(findViewById(R.id.open_source_information_layout), new ExpansionState(findViewById(R.id.open_source_information_text)));
        m_expansionStates.put(findViewById(R.id.donation_links_layout), new ExpansionState(findViewById(R.id.donation_links_buttons)));

        toggleVisibility(findViewById(R.id.disclaimer_layout));
        toggleVisibility(findViewById(R.id.privacy_policy_layout));
        toggleVisibility(findViewById(R.id.open_source_information_layout));
        toggleVisibility(findViewById(R.id.donation_links_layout));

        m_expandCollapseDuration = EXPAND_COLLAPSE_DURATION;
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

  private void createDonationLinkButtons()
  {
    String[] buttonHints = getResources().getStringArray(R.array.donation_button_hints);
    String[] buttonLinks = getResources().getStringArray(R.array.donation_button_links);
    if (buttonHints.length != buttonLinks.length)
    {
      throw new RuntimeException("Donation button hints array must contain the same number of strings as the donation button links array.");
    }

    int buttonHeightPixels = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60.0f, getResources().getDisplayMetrics());
    LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, buttonHeightPixels);
    LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
    buttonLayoutParams.weight = 1.0f;

    LinearLayout donationLinkButtons = findViewById(R.id.donation_links_buttons);
    LinearLayout currentButtonContainer = null;
    for (int i = 0; i < buttonHints.length; i++)
    {
      if (currentButtonContainer == null)
      {
        currentButtonContainer = new LinearLayout(this);
        currentButtonContainer.setLayoutParams(linearLayoutParams);
        currentButtonContainer.setOrientation(LinearLayout.HORIZONTAL);
        currentButtonContainer.setWeightSum(2.0f);
        donationLinkButtons.addView(currentButtonContainer);
      }

      final String donationLink = buttonLinks[i];
      Button button = new Button(this);
      button.setLayoutParams(buttonLayoutParams);
      button.setText(buttonHints[i]);
      button.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View view)
        {
          Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(donationLink));
          startActivity(browserIntent);
        }
      });
      currentButtonContainer.addView(button);

      if (i % 2 == 1)
      {
        currentButtonContainer = null;
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
        valueAnimator = collapse(state.view, m_expandCollapseDuration, 0);
        ((LinearLayout)view).getChildAt(1).animate().rotation(90.0f).start();
      }
      else
      {
        valueAnimator = expand(state.view, m_expandCollapseDuration, state.fullHeight);
        ((LinearLayout)view).getChildAt(1).animate().rotation(0.0f).start();
      }
      state.expanding = !state.expanding;
      state.activeAnimator = valueAnimator;
    }
  }

  public ValueAnimator expand(final View view, long duration, final int targetHeight)
  {
    Display display = getWindowManager().getDefaultDisplay();
    final Point screenSize = new Point();
    display.getSize(screenSize);

    int previousHeight = view.getHeight();
    view.setVisibility(View.VISIBLE);
    ValueAnimator valueAnimator = ValueAnimator.ofInt(previousHeight, targetHeight);
    valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
    {
      @Override
      public void onAnimationUpdate(ValueAnimator animation)
      {
        int height = (int)animation.getAnimatedValue();
        int[] location = new int[2];
        view.getLocationOnScreen(location);

        if (location[1] + height > screenSize.y)
        {
          height = targetHeight;
          animation.cancel();
        }

        view.getLayoutParams().height = height;
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
    int[] location = new int[2];
    view.getLocationOnScreen(location);
    Display display = getWindowManager().getDefaultDisplay();
    final Point screenSize = new Point();
    display.getSize(screenSize);
    int previousHeight = screenSize.y - location[1];

    ValueAnimator valueAnimator = ValueAnimator.ofInt(previousHeight, targetHeight);
    valueAnimator.setInterpolator(new DecelerateInterpolator());
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
