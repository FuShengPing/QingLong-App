package auto.qinglong.fragment.setting;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import auto.qinglong.R;
import auto.qinglong.fragment.BaseFragment;
import auto.qinglong.fragment.FragmentInterFace;
import auto.qinglong.fragment.MenuClickInterface;


public class SettingFragment extends BaseFragment implements FragmentInterFace {
    public static String TAG = "SettingFragment";

    private MenuClickInterface menuClickInterface;


    private ImageView layout_menu;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fg_setting, null);

        layout_menu = view.findViewById(R.id.setting_top_bar_menu);

        init();

        return view;
    }

    @Override
    public void init() {
        layout_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuClickInterface.onMenuClick();
            }
        });
    }

    @Override
    public void setMenuClickInterface(MenuClickInterface menuClickInterface) {
        this.menuClickInterface = menuClickInterface;
    }
}