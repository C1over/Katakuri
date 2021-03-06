package com.hebaiyi.www.katakuri.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.hebaiyi.www.katakuri.R;
import com.hebaiyi.www.katakuri.adapter.BaseAdapter;
import com.hebaiyi.www.katakuri.adapter.ImageAdapter;
import com.hebaiyi.www.katakuri.bean.Folder;
import com.hebaiyi.www.katakuri.model.KatakuriModel;
import com.hebaiyi.www.katakuri.ui.FolderPopupWindow;
import com.hebaiyi.www.katakuri.util.StringUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class KatakuriActivity extends BaseActivity {

    private static final int FINISH_LOADING = 0;
    public static final String EXTRA_NAME = "katakuri";

    private RecyclerView mRcvContent;
    private Toolbar mTbTop;
    private KatakuriModel mModel;
    private SelectReceiver mReceiver;
    private PerViewReceiver mPreReceiver;
    private Button mBtnSure;
    private ImageAdapter mAdapter;
    private KatakuriHandler mHandler;
    private List<Folder> mFolders;
    private List<String> mPaths;
    private ProgressDialog mProgressDialog;
    private Button mBtnPerView;
    private Button mBtnFolder;
    private RelativeLayout mRlytBottom;
    private PopupWindow mPopupFolderList;
    private ImageView mIvTriangle;
    private View mRootView; // 根布局
    private boolean isShowing; // popupWindow是否显示

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void initView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_katakuri);
        // 初始化控件
        mRcvContent = findViewById(R.id.katakuri_rcv_content);
        mTbTop = findViewById(R.id.katakuri_tb_top);
        mBtnSure = findViewById(R.id.katakuri_btn_sure);
        mBtnPerView = findViewById(R.id.katakuri_btn_per_view);
        mBtnPerView.setVisibility(View.GONE);
        mBtnSure.setVisibility(View.GONE);
        mIvTriangle = findViewById(R.id.katakuri_iv_triangle);
        mBtnFolder = findViewById(R.id.katakuri_btn_folder);
        mRlytBottom = findViewById(R.id.katakuri_lyt_bottom);
        // 设置状态栏颜色
        setStatusBarColor();
        // 初始化Toolbar
        initToolbar();
        // 设置按钮监听
        initClick();
        // 请求对话框
        initProgressDialog();
    }

    @Override
    protected void initVariables() {
        mModel = new KatakuriModel();
        // 初始化Handler
        mHandler = new KatakuriHandler(this);
        // 注册广播
        registerReceiver();
    }

    @Override
    protected void loadData() {
        // 加载图片
        loadPicture();
    }

    /**
     * 初始化Toolbar
     */
    private void initToolbar() {
        mTbTop.setTitle("");
        setSupportActionBar(mTbTop);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);
    }


    /**
     * 注册广播
     */
    private void registerReceiver() {
        // 注册刷新按钮的广播
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.hebaiyi.www.katakuri.KatakuriActivity.freshButton");
        mReceiver = new SelectReceiver();
        registerReceiver(mReceiver, filter);
        // 注册preview广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.hebaiyi.www.katakuri.KatakuriActivity.freshSelection");
        mPreReceiver = new PerViewReceiver();
        registerReceiver(mPreReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销广播
        unregisterReceiver(mReceiver);
        unregisterReceiver(mPreReceiver);
    }

    /**
     * 初始化按钮监听
     */
    private void initClick() {
        // 选择文件夹按钮
        mBtnFolder.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                // 控制popupWindow
                controlPopupWindow();
            }
        });

        // 确认按钮
        mBtnSure.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.putStringArrayListExtra(EXTRA_NAME, (ArrayList<String>) mAdapter.getSelectedItems());
                setResult(RESULT_OK, i);
                finish();
            }
        });

        // 预览按钮
        mBtnPerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreviewActivity.actionStart(KatakuriActivity.this,
                        (ArrayList<String>) mAdapter.getSelectedItems());
            }
        });

        // 选择文件夹按钮旁边的小三角形
        mIvTriangle.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                // 控制popupWindow
                controlPopupWindow();
            }
        });
    }

    /**
     * 控制popupWindow
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void controlPopupWindow() {
        if (mPopupFolderList != null) {
            if (isShowing) {
                mPopupFolderList.dismiss();
            } else {
                isShowing = true;
                showFolderCatalogue();
            }
        } else {
            initFolderCatalogue();
        }
    }

    /**
     * 展示文件夹选择的PopupWindow
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void showFolderCatalogue() {
        //产生背景变暗效果
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.4f;
        getWindow().setAttributes(lp);
        // 显示popupWindow
        mPopupFolderList.showAtLocation(mRootView, Gravity.BOTTOM, 0, mRlytBottom.getMeasuredHeight());
    }

    /**
     * 初始化popupWindow
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initFolderCatalogue() {
        mRootView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        mPopupFolderList = new FolderPopupWindow(this, mRootView,
                mFolders, new FolderPopupWindow.FolderWindowCallback() {
            @Override
            public void firstItemClick() {
                // 格式化数据
                mAdapter.formatDate();
                mBtnFolder.setText(getString(R.string.katakuri_all_picture));
                mPopupFolderList.dismiss();
            }

            @Override
            public void commonItemClick(int position) {
                mBtnFolder.setText(mFolders.get(position).getFolderName());
                mAdapter.exchangeData(mModel.getPaths(mFolders.get(position).getDir()));
                mPopupFolderList.dismiss();
            }

            @Override
            public void windowDissmiss() {
                isShowing = false;
                // 恢复透明度
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.alpha = 1f;
                getWindow().setAttributes(lp);
            }
        });
        // 显示popupWindow
        showFolderCatalogue();
    }

    /**
     * 初始化列表
     */
    private void initList() {
        mAdapter = new ImageAdapter(this, mPaths);
        mAdapter.setItemClickListener(new BaseAdapter.ItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ImageActivity.actionStart(KatakuriActivity.this,
                        mAdapter.getSelectedItems(),
                        mAdapter.getData(), position);
            }
        });
        GridLayoutManager manager = new GridLayoutManager(this, 4);
        mRcvContent.setLayoutManager(manager);
        mRcvContent.setAdapter(mAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    /**
     * 扫描图片
     */
    private void scanPicture() {
        mModel.scan(this, new KatakuriModel.KatakuriModelCallback() {
            @Override
            public void onSuccess(List<Folder> folders, List<String> paths) {
                // 保存数据
                mFolders = folders;
                mPaths = paths;
                // 更新UI
                mHandler.sendEmptyMessage(FINISH_LOADING);
            }

            @Override
            public void onFail() {

            }
        });
    }

    /**
     * 请求访问权限
     */
    private void loadPicture() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            PermissionActivity.actionStart(this);
        } else {
            // 搜寻图片
            scanPicture();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setStatusBarColor() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getResources().getColor(R.color.pink));
    }

    /**
     * 初始化加载对话框
     */
    private void initProgressDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(true);
        mProgressDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PreviewActivity.REQUEST_CODE || requestCode == ImageActivity.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK, data);
                finish();
            }
        }
        if(requestCode==PermissionActivity.REQUEST_CODE){
            if(resultCode==PackageManager.PERMISSION_GRANTED){
                scanPicture();
            }else{
                Toast.makeText(this, "获取权限失败", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private class SelectReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int currNum = intent.getIntExtra("curr_num", 0);
            String sure = getString(R.string.katakuri_btn_sure);
            String perView = getString(R.string.katakuri_btn_per_view);
            if (currNum == 0) {
                mBtnSure.setVisibility(View.GONE);
                mBtnPerView.setVisibility(View.GONE);
            } else {
                String numSure = StringUtil.buildString(sure, "(", currNum + "", ")");
                String numPerView = StringUtil.buildString(perView, "(", currNum + "", ")");
                mBtnSure.setText(numSure);
                mBtnPerView.setText(numPerView);
                mBtnSure.setVisibility(View.VISIBLE);
                mBtnPerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private class PerViewReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            HashMap map = (HashMap) intent.getSerializableExtra("return_date");
            mAdapter.notifySelectionChange(map);
        }
    }

    private static class KatakuriHandler extends Handler {

        private final WeakReference<KatakuriActivity> mActivity;

        private KatakuriHandler(KatakuriActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            KatakuriActivity katy = mActivity.get();
            if (katy == null) {
                return;
            }
            switch (msg.what) {
                case FINISH_LOADING:
                    // 初始化列表
                    katy.initList();
                    // 设置按钮内容
                    katy.mBtnFolder.setText("所有图片");
                    // 关闭加载框
                    katy.mProgressDialog.dismiss();
                    break;
            }
        }
    }

}
