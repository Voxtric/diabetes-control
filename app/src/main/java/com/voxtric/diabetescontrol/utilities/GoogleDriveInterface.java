package com.voxtric.diabetescontrol.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import com.voxtric.diabetescontrol.R;

@SuppressWarnings("UnusedReturnValue")
public class GoogleDriveInterface
{
  private static final String TAG = "GoogleDriveInterface";

  public static final int RESULT_SUCCESS = 0;

  public static final int RESULT_PARENT_FOLDER_MISSING = 1;
  public static final int RESULT_FILE_MISSING = 2;

  public static final int RESULT_AUTHENTICATION_ERROR = -1;
  public static final int RESULT_CONNECTION_ERROR = -2;
  public static final int RESULT_TIMEOUT_ERROR = -3;
  public static final int RESULT_INTERRUPT_ERROR = -4;
  public static final int RESULT_SPACE_ERROR = -5;
  public static final int RESULT_UNKNOWN_ERROR = -6;

  public static boolean hasWifiConnection(Context context)
  {
    boolean hasWifiConnection = false;
    ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
    if (connectivityManager != null)
    {
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
      {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (networkCapabilities != null)
        {
          hasWifiConnection = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        }
      }
      else
      {
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null)
        {
          hasWifiConnection = activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI && activeNetworkInfo.isConnected();
        }
      }
    }
    return hasWifiConnection;
  }

  private final Drive m_googleDrive;

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

  private File navigateFromParentTo(File parent, String nextFolderName, boolean createFolderIfMissing) throws IOException
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

    File folder = null;
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
      if (createFolderIfMissing)
      {
        File newFolder = new File();
        newFolder.setName(nextFolderName);
        newFolder.setParents(Collections.singletonList(parentFolderID));
        newFolder.setMimeType("application/vnd.google-apps.folder");
        folder = m_googleDrive.files().create(newFolder).execute();
      }
    }
    else
    {
      folder = possibleFolders.get(0);
    }
    return folder;
  }

  private File getParentFolderOfFile(String filePath, boolean createFolderIfMissing) throws IOException
  {
    String[] pathComponents = filePath.split("/");
    File fileParent = null;
    if (pathComponents.length <= 1)
    {
      fileParent = new File();
      fileParent.setId("root");
    }
    else
    {
      for (int i = 0; i < pathComponents.length - 1; i++)
      {
        fileParent = navigateFromParentTo(fileParent, pathComponents[i], createFolderIfMissing);
        if (fileParent == null)
        {
          break;
        }
      }
    }
    return fileParent;
  }

  public synchronized int uploadFile(String filePath, String fileMimeType, byte[] fileContents, MediaHttpUploaderProgressListener uploaderProgressListener)
  {
    int result;
    try
    {
      String fileName = new java.io.File(filePath).getName();
      String parentFolderID = getParentFolderOfFile(filePath, true).getId();

      File file = new File();
      file.setName(fileName);
      file.setMimeType(fileMimeType);
      ByteArrayContent contentStream = new ByteArrayContent(fileMimeType, fileContents);

      FileList fileList = m_googleDrive.files().list()
          .setQ(String.format("name = '%s' and '%s' in parents and trashed = false", fileName, parentFolderID))
          .setSpaces("drive")
          .setFields("files(id)")
          .setOrderBy("createdTime")
          .execute();
      List<File> existingFiles = fileList.getFiles();
      if (existingFiles == null || existingFiles.isEmpty())
      {
        file.setParents(Collections.singletonList(parentFolderID));
        Drive.Files.Create fileCreate = m_googleDrive.files().create(file, contentStream);
        fileCreate.getMediaHttpUploader().setProgressListener(uploaderProgressListener);
        fileCreate.execute();
        result = RESULT_SUCCESS;
      }
      else
      {
        String fileToUpdateID = existingFiles.get(0).getId();
        Drive.Files.Update fileUpdate = m_googleDrive.files().update(fileToUpdateID, file, contentStream);
        fileUpdate.getMediaHttpUploader().setProgressListener(uploaderProgressListener);
        fileUpdate.execute();
        result = RESULT_SUCCESS;
      }
    }
    catch (UserRecoverableAuthIOException exception)
    {
      Log.v(TAG, "File Upload User Recoverable Auth IO Exception", exception);
      result = RESULT_AUTHENTICATION_ERROR;
    }
    catch (SocketTimeoutException exception)
    {
      Log.v(TAG, "File Upload Socket Timeout Exception", exception);
      result = RESULT_TIMEOUT_ERROR;
    }
    catch (UnknownHostException exception)
    {
      Log.v(TAG, "File Upload Unknown Host Exception", exception);
      result = RESULT_CONNECTION_ERROR;
    }
    catch (SSLHandshakeException exception)
    {
      Log.v(TAG, "File Upload SSL Handshake Exception");
      result = RESULT_AUTHENTICATION_ERROR;
    }
    catch (SSLException exception)
    {
      Log.v(TAG, "File Upload SSL Exception", exception);
      result = RESULT_INTERRUPT_ERROR;
    }
    catch (GoogleJsonResponseException exception)
    {
      Log.v(TAG, "File Upload Google Json Response Exception", exception);
      result = parseGoogleJsonResponseException(exception);
    }
    catch (IOException exception)
    {
      result = parseGenericIOException("File Upload IO Exception", exception);
    }
    return result;
  }

  public synchronized Result<byte[]> downloadFile(String filePath, MediaHttpDownloaderProgressListener downloaderProgressListener)
  {
    byte[] data = null;
    int result;
    try
    {
      String fileName = new java.io.File(filePath).getName();
      File parentFolder = getParentFolderOfFile(filePath, false);
      if (parentFolder != null)
      {
        FileList fileList = m_googleDrive.files().list()
            .setQ(String.format("name = '%s' and '%s' in parents and trashed = false", fileName, parentFolder.getId()))
            .setSpaces("drive")
            .setFields("files(id)")
            .setOrderBy("createdTime")
            .execute();
        List<File> existingFiles = fileList.getFiles();
        if (existingFiles != null && !existingFiles.isEmpty())
        {
          ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
          Drive.Files.Get fileGet = m_googleDrive.files().get(existingFiles.get(0).getId());
          fileGet.getMediaHttpDownloader().setProgressListener(downloaderProgressListener);
          fileGet.executeMediaAndDownloadTo(outputStream);
          data = outputStream.toByteArray();
          result = RESULT_SUCCESS;
        }
        else
        {
          result = RESULT_FILE_MISSING;
        }
      }
      else
      {
        result = RESULT_PARENT_FOLDER_MISSING;
      }
    }
    catch (UserRecoverableAuthIOException exception)
    {
      Log.v(TAG, "File Download User Recoverable Auth IO Exception", exception);
      result = RESULT_AUTHENTICATION_ERROR;
    }
    catch (SocketTimeoutException exception)
    {
      Log.v(TAG, "File Download Socket Timeout Exception", exception);
      result = RESULT_TIMEOUT_ERROR;
    }
    catch (UnknownHostException exception)
    {
      Log.v(TAG, "File Download Unknown Host Exception", exception);
      result = RESULT_CONNECTION_ERROR;
    }
    catch (SSLHandshakeException exception)
    {
      Log.v(TAG, "File Download SSL Handshake Exception");
      result = RESULT_AUTHENTICATION_ERROR;
    }
    catch (SSLException exception)
    {
      Log.v(TAG, "File Download SSL Exception", exception);
      result = RESULT_INTERRUPT_ERROR;
    }
    catch (IOException exception)
    {
      result = parseGenericIOException("File Download IO Exception", exception);
    }
    return new Result<>(result, data);
  }

  public synchronized int deleteFile(String filePath)
  {
    int result;
    try
    {
      String fileName = new java.io.File(filePath).getName();
      File parentFolder = getParentFolderOfFile(filePath, false);
      if (parentFolder != null)
      {
        FileList fileList = m_googleDrive.files().list()
            .setQ(String.format("name = '%s' and '%s' in parents and trashed = false", fileName, parentFolder.getId()))
            .setSpaces("drive")
            .setFields("files(id)")
            .setOrderBy("createdTime")
            .execute();
        List<File> existingFiles = fileList.getFiles();
        if (existingFiles != null && !existingFiles.isEmpty())
        {
          m_googleDrive.files().delete(existingFiles.get(0).getId()).execute();
          result = RESULT_SUCCESS;
        }
        else
        {
          result = RESULT_FILE_MISSING;
        }
      }
      else
      {
        result = RESULT_PARENT_FOLDER_MISSING;
      }
    }
    catch (UserRecoverableAuthIOException exception)
    {
      Log.v(TAG, "File Deletion User Recoverable Auth IO Exception", exception);
      result = RESULT_AUTHENTICATION_ERROR;
    }
    catch (SocketTimeoutException exception)
    {
      Log.v(TAG, "File Deletion Socket Timeout Exception", exception);
      result = RESULT_TIMEOUT_ERROR;
    }
    catch (UnknownHostException exception)
    {
      Log.v(TAG, "File Deletion Unknown Host Exception", exception);
      result = RESULT_CONNECTION_ERROR;
    }
    catch (SSLHandshakeException exception)
    {
      Log.v(TAG, "File Deletion SSL Handshake Exception");
      result = RESULT_AUTHENTICATION_ERROR;
    }
    catch (SSLException exception)
    {
      Log.v(TAG, "File Deletion SSL Exception", exception);
      result = RESULT_INTERRUPT_ERROR;
    }
    catch (IOException exception)
    {
      result = parseGenericIOException("File Deletion IO Exception", exception);
    }
    return result;
  }

  public synchronized Result<File> getFileMetadata(String filePath)
  {
    File file = null;
    int result;
    try
    {
      String fileName = new java.io.File(filePath).getName();
      File parentFolder = getParentFolderOfFile(filePath, false);
      if (parentFolder != null)
      {
        FileList fileList = m_googleDrive.files().list()
            .setQ(String.format("name = '%s' and '%s' in parents and trashed = false", fileName, parentFolder.getId()))
            .setSpaces("drive")
            .setFields("files(id)")
            .setOrderBy("createdTime")
            .execute();
        List<File> existingFiles = fileList.getFiles();
        if (existingFiles != null && !existingFiles.isEmpty())
        {
          file = existingFiles.get(0);
          result = RESULT_SUCCESS;
        }
        else
        {
          result = RESULT_FILE_MISSING;
        }
      }
      else
      {
        result = RESULT_PARENT_FOLDER_MISSING;
      }
    }
    catch (UserRecoverableAuthIOException exception)
    {
      Log.v(TAG, "File Metadata Retrieval User Recoverable Auth IO Exception", exception);
      result = RESULT_AUTHENTICATION_ERROR;
    }
    catch (SocketTimeoutException exception)
    {
      Log.v(TAG, "File Metadata Retrieval Socket Timeout Exception", exception);
      result = RESULT_TIMEOUT_ERROR;
    }
    catch (UnknownHostException exception)
    {
      Log.v(TAG, "File Metadata Retrieval Unknown Host Exception", exception);
      result = RESULT_CONNECTION_ERROR;
    }
    catch (SSLHandshakeException exception)
    {
      Log.v(TAG, "File Metadata Retrieval SSL Handshake Exception");
      result = RESULT_AUTHENTICATION_ERROR;
    }
    catch (SSLException exception)
    {
      Log.v(TAG, "File Metadata Retrieval SSL Exception", exception);
      result = RESULT_INTERRUPT_ERROR;
    }
    catch (IOException exception)
    {
      result = parseGenericIOException("File Metadata Retrieval IO Exception", exception);
    }
    return new Result<>(result, file);
  }

  private int parseGoogleJsonResponseException(GoogleJsonResponseException exception)
  {
    int result = RESULT_UNKNOWN_ERROR;
    List<GoogleJsonError.ErrorInfo> errors = exception.getDetails().getErrors();
    if (errors != null)
    {
      for (int i = 0; i < errors.size() && result == RESULT_UNKNOWN_ERROR; i++)
      {
        GoogleJsonError.ErrorInfo error = errors.get(i);
        if (error.getReason().equals("storageQuotaExceeded"))
        {
          result = RESULT_SPACE_ERROR;
        }
      }
    }
    return result;
  }

  private int parseGenericIOException(String message, IOException exception)
  {
    int result = RESULT_UNKNOWN_ERROR;
    String exceptionMessage = exception.getMessage();
    if (exceptionMessage != null)
    {
      if (exceptionMessage.contains("NetworkError"))
      {
        result = RESULT_AUTHENTICATION_ERROR;
      }
    }

    if (result != RESULT_UNKNOWN_ERROR)
    {
      Log.v(TAG, message, exception);
    }
    else
    {
      Log.e(TAG, message, exception);
    }
    return result;
  }

  public static class Result<T>
  {
    public final int result;
    public final T returned;

    private Result(int result, T returned)
    {
      this.result = result;
      this.returned = returned;
    }
  }
}
