package voxtric.com.diabetescontrol.settings.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.DriveScopes;

import voxtric.com.diabetescontrol.R;

abstract class GoogleDriveSignInFragment extends Fragment
{
  static final int REQUEST_CODE_SIGN_IN = 309;

  void signIn()
  {
    Context context = getContext();
    if (context != null)
    {
      GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
          .requestEmail()
          .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
          .build();
      GoogleSignInClient client = GoogleSignIn.getClient(context, signInOptions);
      startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }
  }

  void signOut()
  {
    Context context = getContext();
    if (context != null)
    {
      GoogleSignInClient client = GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN);
      client.signOut();
      Toast.makeText(context, R.string.sign_out_message, Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent resultData)
  {
    if (requestCode == REQUEST_CODE_SIGN_IN)
    {
      if (resultCode == Activity.RESULT_OK && resultData != null)
      {
        handleSignInResult(resultData);
      }
      else
      {
        onSignInFail();
      }
    }

    super.onActivityResult(requestCode, resultCode, resultData);
  }

  private void handleSignInResult(Intent result)
  {
    GoogleSignIn.getSignedInAccountFromIntent(result)
        .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>()
        {
          @Override
          public void onSuccess(GoogleSignInAccount googleSignInAccount)
          {
            onSignInSuccess(googleSignInAccount);
          }
        })
        .addOnFailureListener(new OnFailureListener()
        {
          @Override
          public void onFailure(@NonNull Exception exception)
          {
            Log.e("GoogleDriveSignIn", exception.getMessage(), exception);
            onSignInFail();
          }
        });
  }

  protected abstract void onSignInSuccess(GoogleSignInAccount googleSignInAccount);
  protected abstract void onSignInFail();
}
