package auto.qinglong.activity.ql.task;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import auto.qinglong.R;
import auto.qinglong.activity.BaseFragment;
import auto.qinglong.activity.ql.CodeWebActivity;
import auto.qinglong.activity.ql.LocalFileAdapter;
import auto.qinglong.bean.ql.QLTask;
import auto.qinglong.network.http.NetManager;
import auto.qinglong.network.http.QLApiController;
import auto.qinglong.utils.CronUnit;
import auto.qinglong.utils.FileUtil;
import auto.qinglong.utils.LogUnit;
import auto.qinglong.utils.TextUnit;
import auto.qinglong.utils.TimeUnit;
import auto.qinglong.utils.ToastUnit;
import auto.qinglong.utils.VibratorUtil;
import auto.qinglong.utils.WindowUnit;
import auto.qinglong.views.popup.PopEditItem;
import auto.qinglong.views.popup.PopEditWindow;
import auto.qinglong.views.popup.PopListWindow;
import auto.qinglong.views.popup.PopMenuItem;
import auto.qinglong.views.popup.PopMenuWindow;
import auto.qinglong.views.popup.PopProgressWindow;
import auto.qinglong.views.popup.PopupWindowBuilder;

public class TaskFragment extends BaseFragment {
    public static String TAG = "TaskFragment";

    private String mCurrentSearchValue;
    private MenuClickListener mMenuClickListener;
    private TaskAdapter mAdapter;

    //????????????
    private LinearLayout ui_bar_main;
    private ImageView ui_nav_menu;
    private ImageView ui_nav_search;
    private ImageView ui_nav_more;
    //???????????????
    private LinearLayout ui_bar_search;
    private ImageView ui_search_back;
    private ImageView ui_search_confirm;
    private EditText ui_search_value;
    //???????????????
    private LinearLayout ui_bar_actions;
    private ImageView ui_actions_back;
    private CheckBox ui_actions_select;
    private HorizontalScrollView ui_actions_scroll;
    private LinearLayout ui_actions_run;
    private LinearLayout ui_actions_stop;
    private LinearLayout ui_actions_pin;
    private LinearLayout ui_actions_unpin;
    private LinearLayout ui_actions_enable;
    private LinearLayout ui_actions_disable;
    private LinearLayout ui_actions_delete;
    //????????????
    private LinearLayout ui_root;
    private RecyclerView ui_recycler;
    private SmartRefreshLayout ui_refresh;

    private PopEditWindow ui_pop_edit;
    private PopProgressWindow ui_pop_progress;

    private enum BarType {NAV, SEARCH, MUL_ACTION}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task, null);

        ui_root = view.findViewById(R.id.root);
        ui_bar_main = view.findViewById(R.id.task_bar_nav);
        ui_nav_menu = view.findViewById(R.id.task_bar_nav_menu);
        ui_nav_search = view.findViewById(R.id.task_bar_nav_search);
        ui_nav_more = view.findViewById(R.id.task_bar_nav_more);

        ui_bar_search = view.findViewById(R.id.task_bar_search);
        ui_search_back = view.findViewById(R.id.task_bar_search_back);
        ui_search_value = view.findViewById(R.id.task_bar_search_input);
        ui_search_confirm = view.findViewById(R.id.task_bar_search_confirm);

        ui_bar_actions = view.findViewById(R.id.task_bar_actions);
        ui_actions_select = view.findViewById(R.id.task_bar_actions_select_all);
        ui_actions_scroll = view.findViewById(R.id.task_bar_actions_scroll);
        ui_actions_back = view.findViewById(R.id.task_bar_actions_back);
        ui_actions_run = view.findViewById(R.id.task_bar_actions_run);
        ui_actions_stop = view.findViewById(R.id.task_bar_actions_stop);
        ui_actions_pin = view.findViewById(R.id.task_bar_actions_pinned);
        ui_actions_unpin = view.findViewById(R.id.task_bar_actions_unpinned);
        ui_actions_enable = view.findViewById(R.id.task_bar_actions_enable);
        ui_actions_disable = view.findViewById(R.id.task_bar_actions_disable);
        ui_actions_delete = view.findViewById(R.id.task_bar_actions_delete);

        ui_refresh = view.findViewById(R.id.refresh_layout);
        ui_recycler = view.findViewById(R.id.recycler_view);

        init();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        initData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            initData();
        }
    }

    @Override
    public boolean onBackPressed() {
        if (ui_bar_actions.getVisibility() == View.VISIBLE) {
            changeBar(BarType.NAV);
            return true;
        } else if (ui_bar_search.getVisibility() == View.VISIBLE) {
            changeBar(BarType.NAV);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void init() {
        mAdapter = new TaskAdapter(requireContext());
        ui_recycler.setAdapter(mAdapter);
        ui_recycler.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        Objects.requireNonNull(ui_recycler.getItemAnimator()).setChangeDuration(0);//???????????????????????????????????????

        //??????????????????
        mAdapter.setTaskInterface(new TaskAdapter.ItemActionListener() {
            @Override
            public void onLog(QLTask task) {
                Intent intent = new Intent(getContext(), CodeWebActivity.class);
                intent.putExtra(CodeWebActivity.EXTRA_TYPE, CodeWebActivity.TYPE_LOG);
                intent.putExtra(CodeWebActivity.EXTRA_TITLE, task.getName());
                intent.putExtra(CodeWebActivity.EXTRA_LOG_PATH, task.getLastLogPath());
                startActivity(intent);
            }

            @Override
            public void onStop(QLTask task) {
                if (NetManager.isRequesting(getNetRequestID())) {
                    return;
                }
                List<String> ids = new ArrayList<>();
                ids.add(task.getId());
                netStopTasks(ids, false);
            }

            @Override
            public void onRun(QLTask task) {
                List<String> ids = new ArrayList<>();
                ids.add(task.getId());
                netRunTasks(ids, false);
            }

            @Override
            public void onEdit(QLTask task) {
                showPopWindowEdit(task);
            }

            @Override
            public void onScript(String parent, String fileName) {
                VibratorUtil.vibrate(requireContext(), VibratorUtil.VIBRATE_SHORT);
                Intent intent = new Intent(getContext(), CodeWebActivity.class);
                intent.putExtra(CodeWebActivity.EXTRA_SCRIPT_NAME, fileName);
                intent.putExtra(CodeWebActivity.EXTRA_SCRIPT_PARENT, parent);
                intent.putExtra(CodeWebActivity.EXTRA_TITLE, fileName);
                intent.putExtra(CodeWebActivity.EXTRA_TYPE, CodeWebActivity.TYPE_SCRIPT);
                intent.putExtra(CodeWebActivity.EXTRA_CAN_EDIT, true);
                startActivity(intent);
            }
        });

        //??????
        ui_refresh.setOnRefreshListener(refreshLayout -> {
            if (ui_bar_search.getVisibility() != View.VISIBLE) {
                mCurrentSearchValue = null;
            }
            netGetTasks(mCurrentSearchValue, true);
        });

        //???????????????
        ui_nav_menu.setOnClickListener(v -> {
            if (mMenuClickListener != null) {
                mMenuClickListener.onMenuClick();
            }
        });

        //????????????
        ui_nav_more.setOnClickListener(this::showPopWindowMenu);

        //???????????????
        ui_nav_search.setOnClickListener(v -> changeBar(BarType.SEARCH));

        //???????????????
        ui_search_back.setOnClickListener(v -> changeBar(BarType.NAV));

        //???????????????
        ui_search_confirm.setOnClickListener(v -> {
            mCurrentSearchValue = ui_search_value.getText().toString();
            WindowUnit.hideKeyboard(ui_search_value);
            netGetTasks(mCurrentSearchValue, true);
        });

        //???????????????
        ui_actions_back.setOnClickListener(v -> changeBar(BarType.NAV));

        //??????
        ui_actions_select.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mAdapter.getCheckState()) {
                mAdapter.selectAll(isChecked);
            }
        });

        //??????
        ui_actions_run.setOnClickListener(v -> {
            if (!NetManager.isRequesting(getNetRequestID())) {
                List<QLTask> QLTasks = mAdapter.getCheckedItems();
                if (QLTasks.size() == 0) {
                    ToastUnit.showShort(getString(R.string.tip_empty_select));
                } else {
                    List<String> ids = new ArrayList<>();
                    for (QLTask QLTask : QLTasks) {
                        ids.add(QLTask.getId());
                    }
                    netRunTasks(ids, true);
                }
            }
        });

        //??????
        ui_actions_stop.setOnClickListener(v -> {
            if (!NetManager.isRequesting(getNetRequestID())) {
                List<QLTask> QLTasks = mAdapter.getCheckedItems();
                if (QLTasks.size() == 0) {
                    ToastUnit.showShort(getString(R.string.tip_empty_select));
                } else {
                    List<String> ids = new ArrayList<>();
                    for (QLTask QLTask : QLTasks) {
                        ids.add(QLTask.getId());
                    }
                    netStopTasks(ids, true);
                }
            }
        });

        //??????
        ui_actions_pin.setOnClickListener(v -> {
            if (!NetManager.isRequesting(getNetRequestID())) {
                List<QLTask> QLTasks = mAdapter.getCheckedItems();
                if (QLTasks.size() == 0) {
                    ToastUnit.showShort(getString(R.string.tip_empty_select));
                } else {
                    List<String> ids = new ArrayList<>();
                    for (QLTask QLTask : QLTasks) {
                        ids.add(QLTask.getId());
                    }
                    netPinTasks(ids);
                }
            }
        });

        //????????????
        ui_actions_unpin.setOnClickListener(v -> {
            if (!NetManager.isRequesting(getNetRequestID())) {
                List<QLTask> QLTasks = mAdapter.getCheckedItems();
                if (QLTasks.size() == 0) {
                    ToastUnit.showShort(getString(R.string.tip_empty_select));
                } else {
                    List<String> ids = new ArrayList<>();
                    for (QLTask QLTask : QLTasks) {
                        ids.add(QLTask.getId());
                    }
                    netUnpinTasks(ids);
                }
            }
        });

        //??????
        ui_actions_enable.setOnClickListener(v -> {
            if (!NetManager.isRequesting(getNetRequestID())) {
                List<QLTask> QLTasks = mAdapter.getCheckedItems();
                if (QLTasks.size() == 0) {
                    ToastUnit.showShort(getString(R.string.tip_empty_select));
                } else {
                    List<String> ids = new ArrayList<>();
                    for (QLTask QLTask : QLTasks) {
                        ids.add(QLTask.getId());
                    }
                    netEnableTasks(ids);
                }
            }
        });

        //??????
        ui_actions_disable.setOnClickListener(v -> {
            if (!NetManager.isRequesting(getNetRequestID())) {
                List<QLTask> QLTasks = mAdapter.getCheckedItems();
                if (QLTasks.size() == 0) {
                    ToastUnit.showShort(getString(R.string.tip_empty_select));
                } else {
                    List<String> ids = new ArrayList<>();
                    for (QLTask QLTask : QLTasks) {
                        ids.add(QLTask.getId());
                    }
                    netDisableTasks(ids);
                }
            }
        });

        //??????
        ui_actions_delete.setOnClickListener(v -> {
            if (!NetManager.isRequesting(getNetRequestID())) {
                List<QLTask> QLTasks = mAdapter.getCheckedItems();
                if (QLTasks.size() == 0) {
                    ToastUnit.showShort(getString(R.string.tip_empty_select));
                } else {
                    List<String> ids = new ArrayList<>();
                    for (QLTask QLTask : QLTasks) {
                        ids.add(QLTask.getId());
                    }
                    netDeleteTasks(ids);
                }
            }
        });
    }

    @Override
    public void setMenuClickListener(MenuClickListener mMenuClickListener) {
        this.mMenuClickListener = mMenuClickListener;
    }

    private void initData() {
        if (initDataFlag || NetManager.isRequesting(this.getNetRequestID())) {
            return;
        }
        ui_refresh.autoRefreshAnimationOnly();
        new Handler().postDelayed(() -> {
            if (isVisible()) {
                netGetTasks(mCurrentSearchValue, true);
            }
        }, 1000);
    }

    private void showPopWindowMenu(View view) {
        PopMenuWindow popMenuWindow = new PopMenuWindow(view, Gravity.END);
        popMenuWindow.addItem(new PopMenuItem("add", "????????????", R.drawable.ic_gray_add));
        popMenuWindow.addItem(new PopMenuItem("localAdd", "????????????", R.drawable.ic_gray_file));
        popMenuWindow.addItem(new PopMenuItem("backup", "????????????", R.drawable.ic_gray_download));
        popMenuWindow.addItem(new PopMenuItem("deleteMul", "????????????", R.drawable.ic_gray_delete));
        popMenuWindow.addItem(new PopMenuItem("mulAction", "????????????", R.drawable.ic_gray_mul_setting));
        popMenuWindow.setOnActionListener(key -> {
            switch (key) {
                case "add":
                    showPopWindowEdit(null);
                    break;
                case "localAdd":
                    localAddData();
                    break;
                case "backup":
                    showPopWindowBackupEdit();
                    break;
                case "deleteMul":
                    compareAndDeleteData();
                    break;
                default:
                    changeBar(BarType.MUL_ACTION);
            }
            return true;
        });
        PopupWindowBuilder.buildMenuWindow(requireActivity(), popMenuWindow);
    }

    private void showPopWindowEdit(QLTask qlTask) {
        ui_pop_edit = new PopEditWindow("????????????", "??????", "??????");
        PopEditItem itemName = new PopEditItem("name", null, "??????", "?????????????????????");
        PopEditItem itemCommand = new PopEditItem("command", null, "??????", "???????????????????????????");
        PopEditItem itemSchedule = new PopEditItem("schedule", null, "????????????", "???(??????) ??? ??? ??? ??? ???");

        if (qlTask != null) {
            ui_pop_edit.setTitle("????????????");
            itemName.setValue(qlTask.getName());
            itemCommand.setValue(qlTask.getCommand());
            itemSchedule.setValue(qlTask.getSchedule());
        }

        ui_pop_edit.addItem(itemName);
        ui_pop_edit.addItem(itemCommand);
        ui_pop_edit.addItem(itemSchedule);
        ui_pop_edit.setActionListener(new PopEditWindow.OnActionListener() {
            @Override
            public boolean onConfirm(Map<String, String> map) {
                String name = map.get("name");
                String command = map.get("command");
                String schedule = map.get("schedule");

                if (TextUnit.isEmpty(name)) {
                    ToastUnit.showShort(getString(R.string.tip_empty_task_name));
                    return false;
                }
                if (TextUnit.isEmpty(command)) {
                    ToastUnit.showShort(getString(R.string.tip_empty_task_command));
                    return false;
                }
                if (!CronUnit.isValid(schedule)) {
                    ToastUnit.showShort(getString(R.string.tip_invalid_task_schedule));
                    return false;
                }

                WindowUnit.hideKeyboard(ui_pop_edit.getView());

                QLTask newQLTask = new QLTask();
                if (qlTask == null) {
                    newQLTask.setName(name);
                    newQLTask.setCommand(command);
                    newQLTask.setSchedule(schedule);
                    netAddTask(newQLTask);
                } else {
                    newQLTask.setName(name);
                    newQLTask.setCommand(command);
                    newQLTask.setSchedule(schedule);
                    newQLTask.setId(qlTask.getId());
                    netEditTask(newQLTask);
                }

                return false;
            }

            @Override
            public boolean onCancel() {
                return true;
            }
        });

        PopupWindowBuilder.buildEditWindow(requireActivity(), ui_pop_edit);
    }

    private void showPopWindowBackupEdit() {
        ui_pop_edit = new PopEditWindow("????????????", "??????", "??????");
        PopEditItem itemName = new PopEditItem("file_name", null, "?????????", "??????");

        ui_pop_edit.addItem(itemName);

        ui_pop_edit.setActionListener(new PopEditWindow.OnActionListener() {
            @Override
            public boolean onConfirm(Map<String, String> map) {
                String fileName = map.get("file_name");
                WindowUnit.hideKeyboard(ui_pop_edit.getView());
                backupData(fileName);
                return true;
            }

            @Override
            public boolean onCancel() {
                return true;
            }
        });

        PopupWindowBuilder.buildEditWindow(requireActivity(), ui_pop_edit);
    }

    private void changeBar(BarType barType) {
        if (ui_bar_search.getVisibility() == View.VISIBLE) {
            WindowUnit.hideKeyboard(ui_root);
            ui_bar_search.setVisibility(View.INVISIBLE);
        } else if (ui_bar_actions.getVisibility() == View.VISIBLE) {
            ui_bar_actions.setVisibility(View.INVISIBLE);
            mAdapter.setCheckState(false);
            ui_actions_select.setChecked(false);
        }

        ui_bar_main.setVisibility(View.INVISIBLE);

        if (barType == BarType.NAV) {
            ui_bar_main.setVisibility(View.VISIBLE);
        } else if (barType == BarType.SEARCH) {
            ui_search_value.setText(mCurrentSearchValue);
            ui_bar_search.setVisibility(View.VISIBLE);
        } else {
            ui_actions_scroll.scrollTo(0, 0);
            mAdapter.setCheckState(true);
            ui_bar_actions.setVisibility(View.VISIBLE);
        }
    }

    private void compareAndDeleteData() {
        List<String> ids = new ArrayList<>();
        Set<String> set = new HashSet<>();
        List<QLTask> tasks = this.mAdapter.getData();
        for (QLTask task : tasks) {
            String key = task.getCommand();
            if (set.contains(key)) {
                ids.add(task.getId());
            } else {
                set.add(key);
            }
        }
        if (ids.size() == 0) {
            ToastUnit.showShort("???????????????");
        } else {
            netDeleteTasks(ids);
        }
    }

    private void localAddData() {
        if (FileUtil.isNeedRequestPermission()) {
            ToastUnit.showShort("?????????????????????????????????");
            FileUtil.requestPermission(requireActivity());
            return;
        }

        List<File> files = FileUtil.getFiles(FileUtil.getTaskPath(), (dir, name) -> name.endsWith(".json"));
        if (files.size() == 0) {
            ToastUnit.showShort("?????????????????????");
            return;
        }

        PopListWindow<LocalFileAdapter> listWindow = new PopListWindow<>("????????????");
        LocalFileAdapter fileAdapter = new LocalFileAdapter(getContext());
        fileAdapter.setData(files);
        listWindow.setAdapter(fileAdapter);

        PopupWindow popupWindow = PopupWindowBuilder.buildListWindow(requireActivity(), listWindow);

        fileAdapter.setListener(file -> {
            try {
                popupWindow.dismiss();
                if (ui_pop_progress == null) {
                    ui_pop_progress = PopupWindowBuilder.buildProgressWindow(requireActivity(), null);
                }
                ui_pop_progress.setTextAndShow("???????????????...");
                BufferedReader bufferedInputStream = new BufferedReader(new FileReader(file));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedInputStream.readLine()) != null) {
                    stringBuilder.append(line);
                }

                ui_pop_progress.setTextAndShow("???????????????...");
                Type type = new TypeToken<List<QLTask>>() {
                }.getType();
                List<QLTask> tasks = new Gson().fromJson(stringBuilder.toString(), type);

                netMulAddTask(tasks);
            } catch (Exception e) {
                ToastUnit.showShort("???????????????" + e.getLocalizedMessage());
            }
        });
    }

    private void backupData(String fileName) {
        if (FileUtil.isNeedRequestPermission()) {
            ToastUnit.showShort("?????????????????????????????????");
            FileUtil.requestPermission(requireActivity());
            return;
        }

        List<QLTask> tasks = mAdapter.getData();
        if (tasks == null || tasks.size() == 0) {
            ToastUnit.showShort("????????????,????????????");
            return;
        }

        JsonArray jsonArray = new JsonArray();
        for (QLTask task : tasks) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", task.getName());
            jsonObject.addProperty("command", task.getCommand());
            jsonObject.addProperty("schedule", task.getSchedule());
            jsonArray.add(jsonObject);
        }

        if (TextUnit.isFull(fileName)) {
            fileName += ".json";
        } else {
            fileName = TimeUnit.formatCurrentTime() + ".json";
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String content = gson.toJson(jsonArray);
        try {
            boolean result = FileUtil.save(FileUtil.getTaskPath(), fileName, content);
            if (result) {
                ToastUnit.showShort("???????????????" + fileName);
            } else {
                ToastUnit.showShort("????????????");
            }
        } catch (Exception e) {
            ToastUnit.showShort("???????????????" + e.getMessage());
        }

    }

    private void netGetTasks(String searchValue, boolean needTip) {
        if (NetManager.isRequesting(getNetRequestID())) {
            return;
        }
        QLApiController.getTasks(getNetRequestID(), searchValue, new QLApiController.NetGetTasksCallback() {
            @Override
            public void onSuccess(List<QLTask> tasks) {
                initDataFlag = true;
                Collections.sort(tasks);
                for (int k = 0; k < tasks.size(); k++) {
                    tasks.get(k).setIndex(k + 1);
                }
                mAdapter.setData(tasks);
                if (needTip) {
                    ToastUnit.showShort("???????????????" + tasks.size());
                }
                ui_refresh.finishRefresh(true);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort("???????????????" + msg);
                ui_refresh.finishRefresh(false);
            }
        });
    }

    private void netRunTasks(List<String> ids, boolean isFromBar) {
        if (NetManager.isRequesting(getNetRequestID())) {
            return;
        }
        QLApiController.runTasks(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                if (isFromBar && ui_bar_actions.getVisibility() == View.VISIBLE) {
                    ui_actions_back.performClick();
                }
                ToastUnit.showShort("????????????");
                netGetTasks(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort("???????????????" + msg);
            }
        });

    }

    private void netStopTasks(List<String> ids, boolean isFromBar) {
        QLApiController.stopTasks(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                if (isFromBar && ui_bar_actions.getVisibility() == View.VISIBLE) {
                    ui_actions_back.performClick();
                }
                ToastUnit.showShort("????????????");
                netGetTasks(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort("???????????????" + msg);
            }
        });
    }

    private void netEnableTasks(List<String> ids) {
        QLApiController.enableTasks(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                if (ui_actions_back.getVisibility() == View.VISIBLE) {
                    ui_actions_back.performClick();
                }
                ToastUnit.showShort("????????????");
                netGetTasks(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort("???????????????" + msg);
            }
        });
    }

    private void netDisableTasks(List<String> ids) {
        QLApiController.disableTasks(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                if (ui_actions_back.getVisibility() == View.VISIBLE) {
                    ui_actions_back.performClick();
                }
                ToastUnit.showShort("????????????");
                netGetTasks(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort("???????????????" + msg);
            }
        });
    }

    private void netPinTasks(List<String> ids) {
        QLApiController.pinTasks(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                if (ui_actions_back.getVisibility() == View.VISIBLE) {
                    ui_actions_back.performClick();
                }
                ToastUnit.showShort("????????????");
                netGetTasks(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort(getString(R.string.tip_pin_failure) + msg);
            }
        });
    }

    private void netUnpinTasks(List<String> ids) {
        QLApiController.unpinTasks(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                if (ui_actions_back.getVisibility() == View.VISIBLE) {
                    ui_actions_back.performClick();
                }
                ToastUnit.showShort(getString(R.string.tip_unpin_success));
                netGetTasks(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort(getString(R.string.tip_unpin_failure) + msg);
            }
        });
    }

    private void netDeleteTasks(List<String> ids) {
        QLApiController.deleteTasks(getNetRequestID(), ids, new QLApiController.NetBaseCallback() {
            @Override
            public void onSuccess() {
                if (ui_actions_back.getVisibility() == View.VISIBLE) {
                    ui_actions_back.performClick();
                }
                ToastUnit.showShort("???????????????" + ids.size());
                netGetTasks(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort("???????????????" + msg);
            }
        });
    }

    private void netEditTask(QLTask task) {
        QLApiController.editTask(getNetRequestID(), task, new QLApiController.NetEditTaskCallback() {
            @Override
            public void onSuccess(QLTask QLTask) {
                ui_pop_edit.dismiss();
                ToastUnit.showShort("????????????");
                netGetTasks(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort("???????????????" + msg);
            }
        });
    }

    private void netAddTask(QLTask task) {
        QLApiController.addTask(getNetRequestID(), task, new QLApiController.NetEditTaskCallback() {
            @Override
            public void onSuccess(QLTask QLTask) {
                ui_pop_edit.dismiss();
                ToastUnit.showShort("??????????????????");
                netGetTasks(mCurrentSearchValue, false);
            }

            @Override
            public void onFailure(String msg) {
                ToastUnit.showShort("?????????????????????" + msg);
            }
        });
    }

    private void netMulAddTask(List<QLTask> tasks) {
        new Thread(() -> {
            final boolean[] isEnd = {false};

            for (int k = 0; k < tasks.size(); k++) {
                ui_pop_progress.setText("??????????????? " + k + "/" + tasks.size());
                QLApiController.addTask(getNetRequestID(), tasks.get(k), new QLApiController.NetEditTaskCallback() {
                    @Override
                    public void onSuccess(QLTask QLTask) {
                        isEnd[0] = true;
                    }

                    @Override
                    public void onFailure(String msg) {
                        isEnd[0] = true;
                        LogUnit.log(TAG, msg);
                    }
                });
                while (!isEnd[0]) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            ui_pop_progress.dismiss();
            netGetTasks(mCurrentSearchValue, true);
        }).start();
    }


}