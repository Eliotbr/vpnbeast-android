package com.b.android.openvpn60.fragment;

import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.activity.ImportActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

/**
 * Created by b on 4/27/17.
 */

public class FileSelectFragment extends ListFragment {
    private static final String ITEM_KEY = "key";
    private static final String ITEM_IMAGE = "image";
    private static final String ROOT = "/";
    private static final int PERMISSION_REQUEST = 23621;
    private static final String DATA_PATH = "DATA_PATH";
    private static final String DATA_VALUE = "DATA_VALUE";

    private Intent mIntent;
    private Context context;
    private String mData;
    private List<String> path = null;
    private TextView myPath;
    private ArrayList<HashMap<String, Object>> mList;
    private Button btnSelect;
    private String parentPath;
    private String currentPath = ROOT;
    private String[] formatFilter = null;
    private File selectedFile;
    private HashMap<String, Integer> lastPositions = new HashMap<String, Integer>();
    private String mStartPath;
    private CheckBox mInlineImport;
    private Button btnClear;
    private boolean mHideImport = false;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        //
        View v = inflater.inflate(R.layout.file_dialog_main, container, false);
        myPath = (TextView) v.findViewById(R.id.path);
        //mInlineImport = (CheckBox) v.findViewById(R.id.doinline);
        if (mHideImport) {
            mInlineImport.setVisibility(View.GONE);
            mInlineImport.setChecked(false);
        }
        final RelativeLayout pnlRelative = (RelativeLayout) getActivity().findViewById(R.id.pnl_import);
        pnlRelative.setVisibility(View.INVISIBLE);
        btnSelect = (Button) v.findViewById(R.id.fdButtonSelect);
        btnSelect.setEnabled(false);
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFileSelectionClick();
                getActivity().getFragmentManager().beginTransaction().remove(FileSelectFragment.this).commit();
                pnlRelative.setVisibility(View.VISIBLE);
            }
        });
        mIntent = new Intent(getActivity(), ImportActivity.class);
        mStartPath = ((ImportActivity) getActivity()).getSelectPath();

            /*btnClear = (Button) v.findViewById(R.id.fdClear);
            btnClear.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                }
            });*/

        return v;
    }

    public Button getBtnSelect() {
        return btnSelect;
    }

    public Button getBtnClear() {
        return btnClear;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                onListItemClick(getListView(), view, position, id);
                onFileSelectionClick();
                return true;
            }
        });
    }

    private void onFileSelectionClick() {
        if (selectedFile != null) {

                ((ImportActivity) getActivity()).importFile(selectedFile.getPath());
            
        }
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDir(mStartPath);
    }


    private void getDir(String dirPath) {
        boolean useAutoSelection = dirPath.length() < currentPath.length();
        Integer position = lastPositions.get(parentPath);

        getDirImpl(dirPath);

        if (position != null && useAutoSelection) {
            getListView().setSelection(position);
        }
    }

    private void getDirImpl(final String dirPath) {
        currentPath = dirPath;
        final List<String> item = new ArrayList<String>();
        path = new ArrayList<String>();
        mList = new ArrayList<HashMap<String, Object>>();

        File f = new File(currentPath);
        File[] files = f.listFiles();
        if (files == null) {
            currentPath = ROOT;
            f = new File(currentPath);
            files = f.listFiles();
        }

        myPath.setText(getText(R.string.location) + ": " + currentPath);

        if (!currentPath.equals(ROOT)) {
            item.add(ROOT);
            addItem(ROOT, R.drawable.ic_root_folder_am);
            path.add(ROOT);

            item.add("../");
            addItem("../", R.drawable.ic_root_folder_am);
            path.add(f.getParent());
            parentPath = f.getParent();
        }

        TreeMap<String, String> dirsMap = new TreeMap<String, String>();
        TreeMap<String, String> dirsPathMap = new TreeMap<String, String>();
        TreeMap<String, String> filesMap = new TreeMap<String, String>();
        TreeMap<String, String> filesPathMap = new TreeMap<String, String>();
        for (File file : files) {
            if (file.isDirectory()) {
                String dirName = file.getName();
                dirsMap.put(dirName, dirName);
                dirsPathMap.put(dirName, file.getPath());
            } else {
                final String fileName = file.getName();
                final String fileNameLwr = fileName.toLowerCase(Locale.getDefault());
                // se ha um filtro de formatos, utiliza-o
                if (formatFilter != null) {
                    boolean contains = false;
                    for (String aFormatFilter : formatFilter) {
                        final String formatLwr = aFormatFilter.toLowerCase(Locale.getDefault());
                        if (fileNameLwr.endsWith(formatLwr)) {
                            contains = true;
                            break;
                        }
                    }
                    if (contains) {
                        filesMap.put(fileName, fileName);
                        filesPathMap.put(fileName, file.getPath());
                    }
                    // senao, adiciona todos os arquivos
                } else {
                    filesMap.put(fileName, fileName);
                    filesPathMap.put(fileName, file.getPath());
                }
            }
        }
        item.addAll(dirsMap.tailMap("").values());
        item.addAll(filesMap.tailMap("").values());
        path.addAll(dirsPathMap.tailMap("").values());
        path.addAll(filesPathMap.tailMap("").values());

        SimpleAdapter fileList = new SimpleAdapter(getActivity(), mList, R.layout.file_dialog_row, new String[]{
                ITEM_KEY, ITEM_IMAGE}, new int[]{R.id.fdrowtext, R.id.fdrowimage});

        for (String dir : dirsMap.tailMap("").values()) {
            addItem(dir, R.drawable.ic_root_folder_am);
        }

        for (String file : filesMap.tailMap("").values()) {
            addItem(file, R.drawable.ic_doc_generic_am);
        }

        fileList.notifyDataSetChanged();

        setListAdapter(fileList);

    }

    private void addItem(String fileName, int imageId) {
        HashMap<String, Object> item = new HashMap<String, Object>();
        item.put(ITEM_KEY, fileName);
        item.put(ITEM_IMAGE, imageId);
        mList.add(item);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

        File file = new File(path.get(position));

        if (file.isDirectory()) {
            btnSelect.setEnabled(false);

            if (file.canRead()) {
                lastPositions.put(currentPath, position);
                getDir(path.get(position));
            } else {
                Toast.makeText(getActivity(),
                        "[" + file.getName() + "] " + getActivity().getText(R.string.err_read_folder),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            selectedFile = file;
            v.setSelected(true);
            btnSelect.setEnabled(true);
        }
    }

    public void setNoInLine() {
        mHideImport = true;

    }

    public String getSelectPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }

}
