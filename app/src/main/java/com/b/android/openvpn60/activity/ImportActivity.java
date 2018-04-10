package com.b.android.openvpn60.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.constant.AppConstants;
import com.b.android.openvpn60.core.ProfileManager;
import com.b.android.openvpn60.helper.LogHelper;
import com.b.android.openvpn60.model.VpnProfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ImportActivity extends AppCompatActivity {
    private static final String RESULT_PROFILE = AppConstants.RESULT_PROFILE.toString();
    private static final String USER_NAME = AppConstants.USER_NAME.toString();
    private static final String USER_PASS = AppConstants.USER_PASS.toString();
    private static final String USER_CHOICE = AppConstants.USER_CHOICE.toString();
    private static final String CLASS_TAG = ImportActivity.class.toString();
    private static final int PERMISSION_REQUEST = 23621;
    transient public static final long MAX_EMBED_FILE_SIZE = 2048 * 1024; // 2048kB
    private boolean mBase64Encode;
    private com.b.android.openvpn60.fragment.FileSelectFragment mFragment;
    private FragmentManager frManager;
    private FragmentTransaction frTransaction;
    private TextView txtCert;
    private Button btnSelect;
    private Button btnSubmit;
    private Button btnClear;
    private TextView txtDesc;
    private Intent intentMain;
    private VpnProfile mProfile;
    private String mData;
    private String mPath;
    private String mTitle;
    private AsyncTask<Void, Void, Integer> mImportTask;
    private ProgressDialog mProgress;
    private Uri mUri;
    private File mFile;
    private RelativeLayout pnlMain;
    private LogHelper logHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_import);
        init();
        checkPermission();
    }

    private void init() {
        logHelper = LogHelper.getLogHelper(this);
        txtCert = (TextView) this.findViewById(R.id.file_title);
        btnSelect = (Button) this.findViewById(R.id.file_select_button);
        btnClear = (Button) this.findViewById(R.id.file_clear_button);
        btnSubmit = (Button) this.findViewById(R.id.file_submit_button);
        txtDesc = (TextView) this.findViewById(R.id.file_selected_description);
        intentMain = new Intent(this, MainActivity.class);
        pnlMain = (RelativeLayout) this.findViewById(R.id.pnl_import);
        final String mErr = getString(R.string.err_no_cert);
        frManager = getFragmentManager();
        mFragment = new com.b.android.openvpn60.fragment.FileSelectFragment();
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPath == null) //change it with mProfile
                    Toast.makeText(ImportActivity.this, mErr, Toast.LENGTH_SHORT).show();
                else {
                    mUri = Uri.fromFile(mFile);
                    startImportTask(mUri, mTitle);
                }
            }
        });
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                frTransaction = frManager.beginTransaction();
                //fragmentTransaction.attach(fr);
                frTransaction.replace(android.R.id.content, mFragment);
                frTransaction.commit();
            }
        });
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mData = null;
                mPath = null;
                mTitle = null;
                mProfile = null;
                txtDesc.setText("");
                txtCert.setText("");
            }
        });
    }

    private void saveProfile() {
        Intent result = new Intent();
        ProfileManager vpl = ProfileManager.getInstance(this);
        vpl.addProfile(mProfile);
        vpl.saveProfile(this, mProfile);
        vpl.saveProfileList(this);
        result.putExtra(LaunchVPN.EXTRA_KEY, mProfile.getUUIDString());
        setResult(Activity.RESULT_OK, result);
        //finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem itm1 = menu.add(R.string.import_cert);
        itm1.setNumericShortcut('1');
        itm1.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        itm1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AlertDialog.Builder dlg = new AlertDialog.Builder(ImportActivity.this, AlertDialog.THEME_HOLO_DARK);
                dlg.setTitle(item.getTitle());
                dlg.setMessage("This is the place where we put some sort of messages.");
                dlg.setNegativeButton(android.R.string.ok, null);
                dlg.setPositiveButton(android.R.string.cancel, null);
                dlg.show();
                return false;
            }
        });
        MenuItem itm2 = menu.add("About us");
        itm2.setNumericShortcut('2');
        itm2.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        itm2.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AlertDialog.Builder dlg = new AlertDialog.Builder(ImportActivity.this, AlertDialog.THEME_HOLO_DARK);
                dlg.setTitle(item.getTitle());
                dlg.setMessage("This is the place where we put some sort of messages.");
                dlg.setPositiveButton(android.R.string.ok, null);
                dlg.setNegativeButton(android.R.string.cancel, null);
                dlg.show();
                return false;
            }
        });
        MenuItem itm3 = menu.add("Close");
        itm3.setNumericShortcut('3');
        itm3.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        itm3.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                ImportActivity.this.finish();
                return false;
            }
        });
        return true;
    }

    public void importFile(String path) {
        mFile = new File(path);
        Exception fe = null;
        String data = "";
        try {
            byte[] fileData = readBytesFromFile(mFile);
            if (mBase64Encode)
                data += Base64.encodeToString(fileData, Base64.DEFAULT);
            else
                data += new String(fileData);
            mData = data;
            txtDesc.setText(mData);
            txtDesc.setVisibility(View.VISIBLE);
            btnClear.setClickable(true);
            mPath = path;
            mTitle = mFile.getName();
            txtCert.setText(mTitle);
            txtDesc.setText(mData);
        } catch (Exception e) {
            logHelper.logException(e);
        }
        if (fe != null) {
            AlertDialog.Builder ab = new AlertDialog.Builder(this);
            ab.setTitle(R.string.err_import_file);
            ab.setMessage(fe.getLocalizedMessage());
            ab.setPositiveButton(android.R.string.ok, null);
            ab.show();
        }
        mUri = Uri.parse(mFile.getPath());
    }

    public String getSelectPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
            }
        }
    }

    private byte[] readBytesFromFile(File file) {
        byte[] bytes = null;
        InputStream input = null;
        try {
            input = new FileInputStream(file);
            long len = file.length();
            if (len > MAX_EMBED_FILE_SIZE)
                throw new IOException("selected file size too big to embed into profile");
            // Create the byte array to hold the data
            bytes = new byte[(int) len];
            // Read in the bytes
            int offset = 0;
            int bytesRead = 0;
            while (offset < bytes.length
                    && (bytesRead = input.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += bytesRead;
            }
        } catch (IOException a) {
            logHelper.logException(a);
        }
        finally {
            if (input != null) {
                try {
                    input.close();
                }
                catch (IOException e) {
                    logHelper.logException(e);
                }
            }
        }
        return bytes;
    }

    private void startImportTask(final Uri mUri, final String possibleName) {
        mImportTask = new AsyncTask<Void, Void, Integer>() {
            @Override
            protected void onPreExecute() {
                mProgress = new ProgressDialog(ImportActivity.this, AlertDialog.THEME_HOLO_DARK);
                mProgress.setMessage(getString(R.string.import_profile));
                mProgress.setCancelable(false);
                mProgress.setTitle(R.string.title_importing);
                mProgress.show();
            }

            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    //mUri = Uri.fromFile(new File(path));

                    InputStream is = getContentResolver().openInputStream(mUri);

                    //doImport(is);
                    //mProfile = new VpnProfile("converted Profile");
                    mProfile = new VpnProfile("converted Profile");
                    if (mProfile == null)
                        return -3;
                } catch (FileNotFoundException | SecurityException se) {
                    logHelper.logException(se);
                    return -2;
                }
                mProgress.dismiss();
                return 0;
            }

            @Override
            protected void onPostExecute(Integer errorCode) {
                if (errorCode == 0) {
                    //OpenVpnStatus.logInfo("Profile successfully imported");
                    //mProgress.dismiss();
                    //ImportActivity.this.startActivity(intentMain);
                    txtDesc.setText(mProfile.toString());
                    //intentMain.putExtra(RESULT_PROFILE, mProfile);
                    AlertDialog.Builder mBuilder = new AlertDialog.Builder(ImportActivity.this, AlertDialog.THEME_HOLO_DARK);
                    mBuilder.setTitle(getString(R.string.profilename));
                    final EditText edtProfile = new EditText(ImportActivity.this);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT);
                    edtProfile.setLayoutParams(lp);
                    //edtProfile.setHighlightColor(ContextCompat.getColor(ImportActivity.this, R.color.colorWhite));
                    edtProfile.setTextColor(Color.WHITE);
                    mBuilder.setView(edtProfile); // uncomment this line
                    mBuilder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //intentMain.putExtra(RESULT_PROFILE, mProfile);
                            saveProfile();
                            ImportActivity.this.setResult(RESULT_OK);
                            ImportActivity.this.finish();
                        }
                    });
                    mBuilder.show();
                }
            }

        }.execute();
    }


    private void displayKeyboard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInputFromWindow(
                pnlMain.getApplicationWindowToken(),
                InputMethodManager.SHOW_FORCED, 0);
    }

    private void closeKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(pnlMain.getApplicationWindowToken(), 0);
    }
}
