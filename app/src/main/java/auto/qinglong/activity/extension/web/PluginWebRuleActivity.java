package auto.qinglong.activity.extension.web;


import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import auto.qinglong.R;
import auto.qinglong.activity.BaseActivity;
import auto.qinglong.bean.app.WebRule;
import auto.qinglong.database.db.WebRuleDBHelper;
import auto.qinglong.network.http.ApiController;
import auto.qinglong.network.http.NetManager;
import auto.qinglong.utils.ToastUnit;
import auto.qinglong.utils.VibratorUtil;
import auto.qinglong.utils.WebUnit;
import auto.qinglong.utils.WindowUnit;
import auto.qinglong.views.popup.PopEditItem;
import auto.qinglong.views.popup.PopEditWindow;
import auto.qinglong.views.popup.PopMenuItem;
import auto.qinglong.views.popup.PopMenuWindow;
import auto.qinglong.views.popup.PopupWindowBuilder;

public class PluginWebRuleActivity extends BaseActivity {
    public static final String TAG = "PluginWebRuleActivity";

    private RuleItemAdapter itemAdapter;
    private ImageView ui_bar_back;
    private ImageView ui_bar_more;
    private RecyclerView ui_recycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_web_rule);

        ui_bar_back = findViewById(R.id.action_bar_back);
        ui_bar_more = findViewById(R.id.bar_more);
        ui_recycler = findViewById(R.id.plugin_web_rule_recycler);

        init();
    }

    @Override
    protected void init() {
        itemAdapter = new RuleItemAdapter(getBaseContext());
        ui_recycler.setAdapter(itemAdapter);
        ui_recycler.setLayoutManager(new LinearLayoutManager(getBaseContext(), LinearLayoutManager.VERTICAL, false));
        Objects.requireNonNull(ui_recycler.getItemAnimator()).setChangeDuration(0);

        itemAdapter.setActionListener(new RuleItemAdapter.OnActionListener() {

            @Override
            public void onCheck(boolean isChecked, int position, int id) {
                WebRuleDBHelper.update(id, isChecked);
            }

            @Override
            public void onAction(View view, int position, WebRule rule) {
                WebRuleDBHelper.delete(rule.getId());
                itemAdapter.removeItem(position);
                VibratorUtil.vibrate(mContext, VibratorUtil.VIBRATE_SHORT);
            }
        });

        ui_bar_back.setOnClickListener(v -> finish());

        ui_bar_more.setOnClickListener(this::showPopMenu);

        itemAdapter.setData(WebRuleDBHelper.getAll());
    }

    private void showPopWindowCommonEdit() {
        PopEditWindow popEditWindow = new PopEditWindow("????????????", "??????", "??????");
        popEditWindow.setMaxHeight(WindowUnit.getWindowHeightPix(getBaseContext()) / 3);//??????????????????
        popEditWindow.addItem(new PopEditItem(WebRuleDBHelper.key_env_name, null, "????????????", "", true, true));
        popEditWindow.addItem(new PopEditItem(WebRuleDBHelper.key_name, null, "????????????", "", true, true));
        popEditWindow.addItem(new PopEditItem(WebRuleDBHelper.key_url, null, "??????", "", true, true));
        popEditWindow.addItem(new PopEditItem(WebRuleDBHelper.key_target, null, "?????????", "??????", true, true));
        popEditWindow.addItem(new PopEditItem(WebRuleDBHelper.key_main, null, "??????", "", true, true));
        popEditWindow.addItem(new PopEditItem(WebRuleDBHelper.key_join_char, null, "?????????", "??????", true, true));
        popEditWindow.setActionListener(new PopEditWindow.OnActionListener() {
            @Override
            public boolean onConfirm(Map<String, String> map) {
                String envName = map.get(WebRuleDBHelper.key_env_name).replace(" ", "");
                String name = map.get(WebRuleDBHelper.key_name).replace(" ", "");
                String url = map.get(WebRuleDBHelper.key_url).replace(" ", "");
                String target = map.get(WebRuleDBHelper.key_target).replace(" ", "");
                String main = map.get(WebRuleDBHelper.key_main).replace(" ", "");
                String joinChar = map.get(WebRuleDBHelper.key_join_char).replace(" ", "");

                if (target.isEmpty()) {
                    target = "*";
                }
                if (joinChar.isEmpty()) {
                    joinChar = ";";
                }

                WebRule rule = new WebRule(0, envName, name, url, target, main, joinChar, true);

                if (!rule.isValid()) {
                    ToastUnit.showShort("????????????,?????????????????????");
                    return false;
                }

                WindowUnit.hideKeyboard(popEditWindow.getView());
                List<WebRule> rules = new ArrayList<>();
                rules.add(rule);
                addRuleToDB(rules);

                return true;
            }

            @Override
            public boolean onCancel() {
                return true;
            }
        });
        PopupWindowBuilder.buildEditWindow(this, popEditWindow);
    }

    private void showPopWindowRemoteEdit() {
        PopEditWindow popEditWindow = new PopEditWindow("????????????", "??????", "??????");
        PopEditItem itemValue = new PopEditItem("url", null, "??????", "?????????????????????");
        popEditWindow.addItem(itemValue);
        popEditWindow.setActionListener(new PopEditWindow.OnActionListener() {
            @Override
            public boolean onConfirm(Map<String, String> map) {
                String url = map.get("url");

                if (WebUnit.isInvalid(url)) {
                    ToastUnit.showShort("???????????????");
                    return false;
                }

                WindowUnit.hideKeyboard(popEditWindow.getView());
                netGetRemoteWebRules(url);

                return true;
            }

            @Override
            public boolean onCancel() {
                return true;
            }
        });

        PopupWindowBuilder.buildEditWindow(this, popEditWindow);
    }

    private void showPopMenu(View view) {
        PopMenuWindow popMenuWindow = new PopMenuWindow(view, Gravity.END);
        popMenuWindow.addItem(new PopMenuItem("add", "????????????", R.drawable.ic_gray_add));
        popMenuWindow.addItem(new PopMenuItem("remoteAdd", "????????????", R.drawable.ic_gray_download));
        popMenuWindow.setOnActionListener(key -> {
            switch (key) {
                case "add":
                    showPopWindowCommonEdit();
                    break;
                case "remoteAdd":
                    showPopWindowRemoteEdit();
                    break;
                default:
                    break;
            }
            return true;
        });
        PopupWindowBuilder.buildMenuWindow(this, popMenuWindow);
    }

    private void addRuleToDB(List<WebRule> rules) {
        int num = 0;
        for (WebRule rule : rules) {
            if (rule.isValid()) {
                rule.setChecked(true);
                WebRuleDBHelper.insert(rule);
                num += 1;
            }
        }
        ToastUnit.showShort("???????????????" + num);
        if (num > 0) {
            itemAdapter.setData(WebRuleDBHelper.getAll());
        }
    }

    private void netGetRemoteWebRules(String url) {
        if (NetManager.isRequesting(getNetRequestID())) {
            return;
        }
        ApiController.getRemoteWebRules(getNetRequestID(), url, new ApiController.NetRemoteWebRuleCallback() {

            @Override
            public void onSuccess(List<WebRule> environments) {
                addRuleToDB(environments);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort("???????????????" + msg);
            }
        });
    }
}