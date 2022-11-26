package auto.qinglong.activity.ql.environment;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import auto.qinglong.R;
import auto.qinglong.bean.ql.QLEnvironment;
import auto.qinglong.network.ApiController;
import auto.qinglong.network.ql.EnvironmentRes;
import auto.qinglong.activity.BaseFragment;
import auto.qinglong.network.RequestManager;
import auto.qinglong.utils.ToastUnit;
import auto.qinglong.utils.WindowUnit;

public class EnvFragment extends BaseFragment {
    public static String TAG = "EnvFragment";
    private String currentSearchValue = "";
    private MenuClickListener menuClickListener;
    private EnvItemAdapter envItemAdapter;

    enum QueryType {QUERY, OTHER}

    enum BarType {NAV, SEARCH, ACTIONS}

    private LinearLayout layout_root;
    private RelativeLayout layout_bar;
    private LinearLayout layout_bar_nav;
    private ImageView layout_nav_menu;
    private ImageView layout_nav_search;
    private ImageView layout_nav_more;
    private LinearLayout layout_bar_search;
    private ImageView layout_search_back;
    private EditText layout_search_value;
    private ImageView layout_search_confirm;
    private LinearLayout layout_bar_actions;
    private ImageView layout_actions_back;
    private CheckBox layout_actions_select;
    private LinearLayout layout_actions_enable;
    private LinearLayout layout_actions_disable;
    private LinearLayout layout_actions_delete;

    private PopupWindow popupWindowMore;
    private PopupWindow popupWindowEdit;
    private TextView layout_edit_title;
    private TextView layout_edit_name;
    private TextView layout_edit_value;
    private TextView layout_edit_remark;
    private Button layout_edit_save;

    private RecyclerView layout_recycler;
    private SmartRefreshLayout layout_refresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fg_env, null);

        layout_root = view.findViewById(R.id.root);
        layout_bar = view.findViewById(R.id.env_top_bar);
        layout_bar_nav = view.findViewById(R.id.env_bar_nav);
        layout_nav_menu = view.findViewById(R.id.env_menu);
        layout_nav_search = view.findViewById(R.id.env_search);
        layout_nav_more = view.findViewById(R.id.env_more);
        layout_bar_search = view.findViewById(R.id.env_bar_search);
        layout_search_back = view.findViewById(R.id.env_bar_search_back);
        layout_search_value = view.findViewById(R.id.env_bar_search_value);
        layout_search_confirm = view.findViewById(R.id.env_bar_search_confirm);
        layout_bar_actions = view.findViewById(R.id.env_bar_actions);
        layout_actions_back = view.findViewById(R.id.env_bar_actions_back);
        layout_actions_select = view.findViewById(R.id.env_bar_actions_select_all);
        layout_actions_enable = view.findViewById(R.id.env_bar_actions_enable);
        layout_actions_disable = view.findViewById(R.id.env_bar_actions_disable);
        layout_actions_delete = view.findViewById(R.id.env_bar_actions_delete);

        layout_refresh = view.findViewById(R.id.refreshLayout);
        layout_recycler = view.findViewById(R.id.recyclerView);

        envItemAdapter = new EnvItemAdapter(requireContext());
        layout_recycler.setAdapter(envItemAdapter);
        layout_recycler.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        Objects.requireNonNull(layout_recycler.getItemAnimator()).setChangeDuration(0);

        init();

        return view;
    }

    /**
     * fragment中只有第一次载入可见时才会触发该回调
     */
    @Override
    public void onResume() {
        super.onResume();
        loadFirst();
    }

    /**
     * 第一次载入页面不会触发 此后页面可见性改变则触发该回调
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadFirst();
        }
    }

    private void loadFirst() {
        if (loadSuccessFlag || RequestManager.isRequesting(getNetRequestID())) {
            return;
        }
        //延迟加载
        new Handler().postDelayed(() -> {
            //可见则加载
            if (isVisible()) {
                netGetEnvironments(currentSearchValue, QueryType.QUERY);
            }
        }, 1000);
    }

    @Override
    public void init() {
        envItemAdapter.setItemInterface(new OnItemActionListener() {
            @Override
            public void onEdit(QLEnvironment environment, int position) {
                showPopWindowEdit(environment);
            }

            @Override
            public void onActions(QLEnvironment environment, int position) {
                if (!envItemAdapter.isCheckState()) {
                    showBar(BarType.ACTIONS);
                }
            }
        });

        //导航栏
        layout_nav_menu.setOnClickListener(v -> menuClickListener.onMenuClick());

        //刷新控件//
        //初始设置处于刷新状态
        layout_refresh.autoRefreshAnimationOnly();
        layout_refresh.setOnRefreshListener(refreshLayout -> netGetEnvironments(currentSearchValue, QueryType.QUERY));

        //更多操作
        layout_nav_more.setOnClickListener(v -> showPopWindowMore());

        //搜索栏进入
        layout_nav_search.setOnClickListener(v -> {
            layout_search_value.setText(currentSearchValue);
            showBar(BarType.SEARCH);
        });

        //搜索栏确定
        layout_search_confirm.setOnClickListener(v -> {
            String value = layout_search_value.getText().toString().trim();
            if (!value.isEmpty()) {
                currentSearchValue = value;
                WindowUnit.hideKeyboard(layout_search_value);
                netGetEnvironments(currentSearchValue, QueryType.OTHER);
            }
        });

        //搜索栏返回
        layout_search_back.setOnClickListener(v -> showBar(BarType.NAV));

        //动作栏返回
        layout_actions_back.setOnClickListener(v -> showBar(BarType.NAV));

        //全选
        layout_actions_select.setOnCheckedChangeListener((buttonView, isChecked) -> envItemAdapter.setAllChecked(isChecked));

        //删除
        layout_actions_delete.setOnClickListener(v -> {
            if (RequestManager.isRequesting(getNetRequestID())) {
                return;
            }
            List<QLEnvironment> environments = envItemAdapter.getSelectedItems();
            if (environments.size() == 0) {
                ToastUnit.showShort(getContext(), getString(R.string.tip_empty_select));
                return;
            }

            List<String> ids = new ArrayList<>();
            for (QLEnvironment environment : environments) {
                ids.add(environment.get_id());
            }
            netDeleteEnvironments(ids);
        });

        //禁用
        layout_actions_disable.setOnClickListener(v -> {
            if (RequestManager.isRequesting(getNetRequestID())) {
                return;
            }
            List<QLEnvironment> environments = envItemAdapter.getSelectedItems();
            if (environments.size() == 0) {
                ToastUnit.showShort(getContext(), getString(R.string.tip_empty_select));
                return;
            }

            List<String> ids = new ArrayList<>();
            for (QLEnvironment environment : environments) {
                ids.add(environment.get_id());
            }
            netDisableEnvironments(ids);
        });

        //启用
        layout_actions_enable.setOnClickListener(v -> {
            if (RequestManager.isRequesting(getNetRequestID())) {
                return;
            }
            List<QLEnvironment> environments = envItemAdapter.getSelectedItems();
            if (environments.size() == 0) {
                ToastUnit.showShort(getContext(), getString(R.string.tip_empty_select));
                return;
            }

            List<String> ids = new ArrayList<>();
            for (QLEnvironment environment : environments) {
                ids.add(environment.get_id());
            }
            netEnableEnvironments(ids);
        });

    }

    private void netGetEnvironments(String searchValue, QueryType queryType) {
        ApiController.getEnvironments(getNetRequestID(), searchValue, new ApiController.GetEnvironmentsCallback() {
            @Override
            public void onSuccess(EnvironmentRes res) {
                loadSuccessFlag = true;
                if (queryType == QueryType.QUERY) {
                    ToastUnit.showShort(requireContext(), "加载成功");
                }
                sortAndSetData(res.getData());
                layout_refresh.finishRefresh(true);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort(requireContext(), "加载失败：" + msg);
                layout_refresh.finishRefresh(false);
            }
        });
    }

    public void netUpdateEnvironment(QLEnvironment environment) {
        ApiController.updateEnvironment(getNetRequestID(), environment, new ApiController.EditEnvCallback() {
            @Override
            public void onSuccess(QLEnvironment data) {
                popupWindowEdit.dismiss();
                ToastUnit.showShort(requireContext(), "更新成功");
                netGetEnvironments(currentSearchValue, QueryType.OTHER);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort(requireContext(), "更新失败：" + msg);
            }
        });
    }

    public void netAddEnvironments(List<QLEnvironment> environments) {
        ApiController.addEnvironment(getNetRequestID(), environments, new ApiController.GetEnvironmentsCallback() {
            @Override
            public void onSuccess(EnvironmentRes res) {
                popupWindowEdit.dismiss();
                ToastUnit.showShort(requireContext(), "新建成功");
                netGetEnvironments(currentSearchValue, QueryType.OTHER);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort(requireContext(), "新建失败：" + msg);
            }
        });
    }

    public void netDeleteEnvironments(List<String> ids) {
        ApiController.deleteEnvironments(getNetRequestID(), ids, new ApiController.BaseCallback() {
            @Override
            public void onSuccess() {
                layout_actions_back.performClick();
                ToastUnit.showShort(requireContext(), "删除成功");
                netGetEnvironments(currentSearchValue, QueryType.OTHER);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort(requireContext(), "删除失败：" + msg);
            }
        });
    }

    public void netEnableEnvironments(List<String> ids) {
        ApiController.enableEnvironments(getNetRequestID(), ids, new ApiController.BaseCallback() {
            @Override
            public void onSuccess() {
                layout_actions_back.performClick();
                ToastUnit.showShort(requireContext(), "启用成功");
                netGetEnvironments(currentSearchValue, QueryType.OTHER);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort(requireContext(), "启用失败：" + msg);
            }
        });

    }

    public void netDisableEnvironments(List<String> ids) {
        ApiController.disableEnvironments(getNetRequestID(), ids, new ApiController.BaseCallback() {
            @Override
            public void onSuccess() {
                layout_actions_back.performClick();
                ToastUnit.showShort(requireContext(), "禁用成功");
                netGetEnvironments(currentSearchValue, QueryType.OTHER);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort(requireContext(), "禁用失败");
            }
        });
    }

    public void sortAndSetData(List<QLEnvironment> data) {
        //排序
        Collections.sort(data);
        //同变量名称设置序号
        int size = data.size();
        int current = 0;
        int index = 1;
        while (true) {
            data.get(current).setIndex(index);
            if (current < size - 1) {
                if (data.get(current).getName().equals(data.get(current + 1).getName())) {
                    index += 1;
                } else {
                    index = 1;
                }
            } else {
                break;
            }
            current += 1;
        }

        envItemAdapter.setData(data);
    }

    private void initPopWindowMore() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.pop_fg_more, null, false);
        LinearLayout layout_add = view.findViewById(R.id.pop_fg_more_add);
        LinearLayout layout_action = view.findViewById(R.id.pop_fg_more_action);
        TextView layout_add_text = view.findViewById(R.id.pop_fg_more_add_text);
        TextView layout_action_text = view.findViewById(R.id.pop_fg_more_action_text);
        layout_add_text.setText("新建变量");
        layout_action_text.setText(getString(R.string.mul_action));

        popupWindowMore = new PopupWindow(getContext());
        popupWindowMore.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindowMore.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindowMore.setContentView(view);
        popupWindowMore.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindowMore.setOutsideTouchable(true);
        popupWindowMore.setFocusable(true);

        layout_add.setOnClickListener(v -> {
            popupWindowMore.dismiss();
            showPopWindowEdit(null);
        });

        layout_action.setOnClickListener(v -> {
            popupWindowMore.dismiss();
            showBar(BarType.ACTIONS);
        });
    }

    private void initPopWindowEdit() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.pop_fg_env_edit, null, false);

        layout_edit_title = view.findViewById(R.id.env_edit_type);
        layout_edit_name = view.findViewById(R.id.env_edit_name);
        layout_edit_value = view.findViewById(R.id.env_edit_value);
        layout_edit_remark = view.findViewById(R.id.env_edit_remark);
        Button layout_edit_cancel = view.findViewById(R.id.env_edit_cancel);
        layout_edit_save = view.findViewById(R.id.env_edit_save);

        popupWindowEdit = new PopupWindow(getContext());
        popupWindowEdit.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindowEdit.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindowEdit.setContentView(view);
        popupWindowEdit.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindowEdit.setOutsideTouchable(true);
        popupWindowEdit.setFocusable(true);
        popupWindowEdit.setAnimationStyle(R.style.anim_fg_task_pop_edit);

        layout_edit_cancel.setOnClickListener(v -> popupWindowEdit.dismiss());

        popupWindowEdit.setOnDismissListener(() -> WindowUnit.setBackgroundAlpha(requireActivity(), 1.0f));
    }

    public void showPopWindowMore() {
        if (popupWindowMore == null) {
            initPopWindowMore();
        }
        popupWindowMore.showAsDropDown(layout_bar, 0, 0, Gravity.END);
    }

    private void showPopWindowEdit(QLEnvironment environment) {
        if (popupWindowEdit == null) {
            initPopWindowEdit();
        }

        if (environment == null) {
            layout_edit_title.setText("新建变量");
            layout_edit_name.setText(null);
            layout_edit_value.setText(null);
            layout_edit_remark.setText(null);
        } else {
            layout_edit_title.setText("编辑变量");
            layout_edit_name.setText(environment.getName());
            layout_edit_value.setText(environment.getValue());
            layout_edit_remark.setText(environment.getRemarks());
        }

        layout_edit_save.setOnClickListener(v -> {
            if (RequestManager.isRequesting(getNetRequestID())) {
                return;
            }

            WindowUnit.hideKeyboard(layout_root);

            String name = layout_edit_name.getText().toString().trim();
            String value = layout_edit_value.getText().toString().trim();
            String remarks = layout_edit_remark.getText().toString().trim();

            if (name.isEmpty()) {
                ToastUnit.showShort(requireContext(), "变量名称不能为空");
                return;
            }

            if (value.isEmpty()) {
                ToastUnit.showShort(requireContext(), "变量值不能为空");
                return;
            }

            List<QLEnvironment> environments = new ArrayList<>();
            QLEnvironment newEnv;
            newEnv = new QLEnvironment();
            newEnv.setName(name);
            newEnv.setValue(value);
            newEnv.setRemarks(remarks);
            environments.add(newEnv);
            if (environment == null) {
                netAddEnvironments(environments);
            } else {
                newEnv.set_id(environment.get_id());
                netUpdateEnvironment(newEnv);
            }
        });

        WindowUnit.setBackgroundAlpha(requireActivity(), 0.5f);
        popupWindowEdit.showAtLocation(layout_root, Gravity.CENTER, 0, 0);
    }

    public void showBar(BarType barType) {
        if (layout_bar_search.getVisibility() == View.VISIBLE) {
            WindowUnit.hideKeyboard(layout_root);
            layout_bar_search.setVisibility(View.INVISIBLE);
            currentSearchValue = "";
        }

        if (layout_bar_actions.getVisibility() == View.VISIBLE) {
            layout_bar_actions.setVisibility(View.INVISIBLE);
            envItemAdapter.setCheckState(false, -1);
            layout_actions_select.setChecked(false);
        }

        layout_bar_nav.setVisibility(View.INVISIBLE);

        if (barType == BarType.NAV) {
            layout_bar_nav.setVisibility(View.VISIBLE);
        } else if (barType == BarType.SEARCH) {
            layout_bar_search.setVisibility(View.VISIBLE);
        } else {
            layout_actions_select.setChecked(false);
            envItemAdapter.setCheckState(true, -1);
            layout_bar_actions.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setMenuClickListener(MenuClickListener menuClickListener) {
        this.menuClickListener = menuClickListener;
    }

    @Override
    public boolean onBackPressed() {
        if (layout_bar_search.getVisibility() == View.VISIBLE) {
            showBar(BarType.NAV);
            return true;
        } else if (layout_bar_actions.getVisibility() == View.VISIBLE) {
            showBar(BarType.NAV);
            return true;
        } else {
            return false;
        }
    }
}