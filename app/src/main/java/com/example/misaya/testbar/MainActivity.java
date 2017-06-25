package com.example.misaya.testbar;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dou361.dialogui.DialogUIUtils;
import com.dou361.dialogui.bean.TieBean;
import com.dou361.dialogui.listener.DialogUIItemListener;
import com.example.misaya.utils.NetUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String keyfrom = "TYNsWord";
    private static final String key = "92609876";
    private static final String type = "data";
    private static final String doctype = "json";
    private static final String version = "1.1";
    private static final String baseUrl = "http://katarinar.top/tt/server/";
    private static final String initUnit = "unit1";

    private Toolbar toolbar;
    private ProgressBar aProgressBar;
    private ImageView imgBefore,imgAfter,imgAudio;
    private RelativeLayout layoutHide,layoutShow;
    private TextView tvNowNum,tvTotalNum,tvFWord,tvLWord,tvPhonetic,tvExplain;

    private JSONArray arr;
    private MenuItem onItem, offItem;
    private JSONObject jsonObject = null;
    private Activity mActivity;
    private Context mContext;
    private int index = 0;
    private boolean autoVoice = false;
    private Handler handler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);           //隐藏ToolBar
        setContentView(R.layout.activity_main);

        mActivity = this;
        mContext = getApplication();
        init();
        chooseUnit(initUnit);

        MsgReceiver msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.misaya.RECEIVER");
        registerReceiver(msgReceiver, intentFilter);

        imgBefore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgAfter.setVisibility(View.VISIBLE);
                index--;
                if(index<=0){
                    imgBefore.setVisibility(View.INVISIBLE);
                }
                setWord(index);
            }
        });

        imgAfter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgBefore.setVisibility(View.VISIBLE);
                index++;
                if(index>=arr.length()-1){
                    imgAfter.setVisibility(View.INVISIBLE);
                }
                setWord(index);
            }
        });

        layoutShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContent();
            }
        });

        imgAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgAudio.setVisibility(View.GONE);
                aProgressBar.setVisibility(View.VISIBLE);
                try {
                    audioService(arr.getString(index));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        layoutHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideContent();
            }
        });

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what) {
                    case 1:
                        String u = (String) msg.obj;
                        setTitle(u);
                        Toast.makeText(mContext,"已选择 " + u,Toast.LENGTH_LONG).show();
                        imgBefore.setVisibility(View.INVISIBLE);
                        if(arr.length()==1)
                            imgAfter.setVisibility(View.INVISIBLE);
                        else imgAfter.setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        tvExplain.setText("");
                        tvNowNum.setText(String.valueOf(index + 1));
                        tvTotalNum.setText(String.valueOf(arr.length()));
                        tvLWord.setVisibility(View.INVISIBLE);
                        tvPhonetic.setVisibility(View.INVISIBLE);
                        tvExplain.setVisibility(View.INVISIBLE);
                        try {
                            String word = arr.getString(index);
                            tvFWord.setText(word.substring(0,1));
                            tvLWord.setText(word.substring(1));
                            String phonetic = jsonObject != null ? jsonObject.getString("phonetic") : null;
                            if(phonetic!=null){
                                if(phonetic.contains("[")||phonetic.contains("]"))
                                    tvPhonetic.setText(phonetic);
                                else tvPhonetic.setText("[ " + phonetic + " ]");
                            }
                            JSONArray explains = jsonObject != null ? jsonObject.getJSONArray("explains") : null;
                            for(int i = 0; i< (explains != null ? explains.length() : 0); i++){
                                tvExplain.append(explains.getString(i) + "\n\n");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;
                    case 3:
                        Toast.makeText(mContext,"暂未录入...",Toast.LENGTH_LONG).show();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void init() {
        toolbar = (Toolbar) findViewById(R.id.mToolBar);
        aProgressBar = (ProgressBar) findViewById(R.id.mProgressBar);
//        lProgressBar = (ProgressBar) findViewById(R.id.lProgressBar);
//        rProgressBar = (ProgressBar) findViewById(R.id.rProgressBar);
        imgBefore = (ImageView) findViewById(R.id.img_before);
        imgAudio = (ImageView) findViewById(R.id.imgAudio);
        imgAfter = (ImageView) findViewById(R.id.img_after);
        layoutHide = (RelativeLayout) findViewById(R.id.layout_hide);
//        layoutAudio = (RelativeLayout) findViewById(R.id.layout_audio);
        layoutShow = (RelativeLayout) findViewById(R.id.layout_show);
        tvNowNum = (TextView) findViewById(R.id.tv_nowNum);
        tvTotalNum = (TextView) findViewById(R.id.tv_totalNum);
        tvFWord = (TextView) findViewById(R.id.tv_fWord);
        tvLWord = (TextView) findViewById(R.id.tv_lWord);
        tvPhonetic = (TextView) findViewById(R.id.tv_phonetic);
        tvExplain = (TextView) findViewById(R.id.tv_explain);
    }

    public class MsgReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra("state", 1);
            if(state == 0){
                aProgressBar.setVisibility(View.GONE);
                imgAudio.setVisibility(View.VISIBLE);
            }
        }

    }

    private void chooseUnit(final String unit) {
        final String url = baseUrl + "getWords.php?unit=" + unit;
        if(isNetworkConnected(mContext)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONArray tempArr = new JSONArray(NetUtils.get(url));
                        if (tempArr.length() != 0) {
                            arr = tempArr;
                            index = 0;
                            Log.e("WordList", arr.toString());
                            setWord(index);
                            Message msg = new Message();
                            msg.what = 1;
                            msg.obj = unit;
                            handler.sendMessage(msg);
                        } else {
                            Message msg = new Message();
                            msg.what = 3;
                            handler.sendMessage(msg);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            new AlertDialog.Builder(mActivity).setTitle("Tip")
                    .setMessage("Network Connection Error!Please Try Again...")
                    .setPositiveButton("Ok",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).show();
        }
    }

    private void setWord(final int index) {
        String word = null;
        try {
            word = arr.getString(index);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final String url = "http://fanyi.youdao.com/openapi.do?keyfrom=" + keyfrom +
                "&key=" + key +
                "&type=" + type +
                "&doctype=" + doctype +
                "&version=" + version +
                "&q=" + word;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    jsonObject = new JSONObject(NetUtils.get(url)).getJSONObject("basic");
                    if(autoVoice)
                        audioService(arr.getString(index));
                    Message msg = new Message();
                    msg.what = 2;
                    handler.sendMessage(msg);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void audioService(String word) {
        Intent mIntent = new Intent(MainActivity.this, AudioService.class);
        mIntent.putExtra("query", word);
        startService(mIntent);
    }

    private void setTitle(String unit) {
        toolbar.setTitle(unit);
        setSupportActionBar(toolbar);
    }

    private void showContent() {
        tvFWord.setVisibility(View.VISIBLE);
        tvLWord.setVisibility(View.VISIBLE);
        tvPhonetic.setVisibility(View.VISIBLE);
        tvExplain.setVisibility(View.VISIBLE);
    }

    private void hideContent() {
        tvLWord.setVisibility(View.INVISIBLE);
        tvPhonetic.setVisibility(View.INVISIBLE);
        tvExplain.setVisibility(View.INVISIBLE);
    }

    public boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        onItem = menu.findItem(R.id.action_on);
        offItem = menu.findItem(R.id.action_off);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_set:
                List<TieBean> data = new ArrayList<>();
                for(int i=1;i<=30;i++)
                    data.add(new TieBean(String.valueOf(i)));
                DialogUIUtils.showMdBottomSheet(mActivity, false, "Unit", data, 5, new DialogUIItemListener() {
                    @Override
                    public void onItemClick(CharSequence text, int position) {
                        String tUnit = "unit" + String.valueOf(position+1);
                        chooseUnit(tUnit);
                    }
                }).show();
                break;
            case R.id.action_on:
                onItem.setVisible(false);
                offItem.setVisible(true);
                autoVoice = false;
                Toast.makeText(MainActivity.this, "自动发音已关闭", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_off:
                offItem.setVisible(false);
                onItem.setVisible(true);
                autoVoice = true;
                Toast.makeText(MainActivity.this, "自动发音已开启", Toast.LENGTH_SHORT).show();
            default:
                break;
        }
        return true;
    }
}
