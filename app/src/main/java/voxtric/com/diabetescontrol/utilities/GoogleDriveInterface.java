package voxtric.com.diabetescontrol.utilities;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import voxtric.com.diabetescontrol.R;

public class GoogleDriveInterface
{
  private Drive m_googleDrive;

  public GoogleDriveInterface(Context context, GoogleSignInAccount account)
  {
    m_googleDrive = buildDriveService(context, account);
  }

  private static Drive buildDriveService(Context context, GoogleSignInAccount account)
  {
    GoogleAccountCredential credential =
        GoogleAccountCredential.usingOAuth2(context, Collections.singleton(DriveScopes.DRIVE_FILE));
    credential.setSelectedAccount(account.getAccount());
    return new Drive.Builder(
        AndroidHttp.newCompatibleTransport(),
        new GsonFactory(),
        credential)
        .setApplicationName(context.getString(R.string.app_name))
        .build();
  }

  private File navigateFromParentTo(File parent, String nextFolderName) throws IOException
  {
    if (parent == null)
    {
      parent = new File();
      parent.setId("root");
    }

    if (parent.getId() == null)
    {
      throw new IllegalArgumentException("Parent folder must have File ID.");
    }

    File folder;
    String parentFolderID = parent.getId();
    FileList folderList = m_googleDrive.files().list()
        .setQ(String.format("name = '%s' and '%s' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false", nextFolderName, parentFolderID))
        .setSpaces("drive")
        .setFields("files(id)")
        .setOrderBy("createdTime")
        .execute();
    List<File> possibleFolders = folderList.getFiles();
    if (possibleFolders == null || possibleFolders.isEmpty())
    {
      File newFolder = new File();
      newFolder.setName(nextFolderName);
      newFolder.setParents(Collections.singletonList(parentFolderID));
      newFolder.setMimeType("application/vnd.google-apps.folder");
      folder = m_googleDrive.files().create(newFolder).execute();
    }
    else
    {
      folder = possibleFolders.get(0);
    }
    return folder;
  }

  public synchronized boolean uploadFile(String filePath, String fileMimeType, byte[] fileContents, MediaHttpUploaderProgressListener uploaderProgressListener)
  {
    boolean success = false;
    try
    {
      String[] pathComponents = filePath.split("/");
      File fileParent = null;
      for (int i = 0; i < pathComponents.length - 1; i++)
      {
        fileParent = navigateFromParentTo(fileParent, pathComponents[i]);
      }
      if (fileParent == null)
      {
        fileParent = new File();
        fileParent.setId("root");
      }
      String fileName = pathComponents[pathComponents.length - 1];
      String parentFolderID = fileParent.getId();

      File file = new File();
      file.setName(fileName);
      file.setMimeType(fileMimeType);
      ByteArrayContent contentStream = new ByteArrayContent(fileMimeType, fileContents);

      FileList fileList = m_googleDrive.files().list()
          .setQ(String.format("name = '%s' and '%s' in parents and trashed = false", fileName, parentFolderID))
          .setSpaces("drive")
          .setOrderBy("createdTime")
          .execute();
      List<File> existingFiles = fileList.getFiles();
      if (existingFiles == null || existingFiles.isEmpty())
      {
        file.setParents(Collections.singletonList(parentFolderID));
        Drive.Files.Create fileCreate = m_googleDrive.files().create(file, contentStream);
        fileCreate.getMediaHttpUploader().setProgressListener(uploaderProgressListener);
        fileCreate.execute();
        success = true;
      }
      else
      {
        String fileToUpdateID = existingFiles.get(0).getId();
        Drive.Files.Update fileUpdate = m_googleDrive.files().update(fileToUpdateID, file, contentStream);
        fileUpdate.getMediaHttpUploader().setProgressListener(uploaderProgressListener);
        fileUpdate.execute();
        success = true;
      }
    }
    catch (IOException exception)
    {
      Log.e("GoogleDriveInterface", exception.getMessage(), exception);
    }
    return success;
  }
}
