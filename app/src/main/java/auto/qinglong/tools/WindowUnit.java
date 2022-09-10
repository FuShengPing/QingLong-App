package auto.qinglong.tools;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;

import auto.qinglong.MyApplication;

public class WindowUnit {
    /**
     * 隐藏虚拟键盘
     *
     * @param v
     */
    public static void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }

    }

    /**
     * Get window height dp float.
     * 获取屏幕高度 dp
     *
     * @return the float
     */
    public static float getWindowHeightDp() {
        return px2dip(MyApplication.getContext().getResources().getDisplayMetrics().heightPixels);
    }

    /**
     * Gets window height pix.
     * 获取屏幕高度 pix
     *
     * @return the window height pix
     */
    public static int getWindowHeightPix(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * 获取屏幕宽度 pix
     *
     * @return
     */
    public static int getWindowWidthPix() {
        return MyApplication.getContext().getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * Gets status bar height.
     * 获取状态栏高度
     *
     * @return the status bar height
     */

    public static int getStatusBarHeight() {
        Resources resources = MyApplication.getContext().getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");//获取状态栏
        int height = resources.getDimensionPixelSize(resourceId);

        return height;
    }


    /**
     * Sets status bar text color.
     * 设置状态栏字体颜色
     *
     * @param activity the activity
     * @param isWhite  the is white
     */
    public static void setStatusBarTextColor(Activity activity, boolean isWhite) {
        if (isWhite) {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);//设置状态栏白色字体
        } else {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//设置状态栏字体黑色
        }
    }

    /**
     * Sets translucent status.
     * 设置透明状态栏
     *
     * @param activity the activity
     */
    public static void setTranslucentStatus(Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }

    /**
     * Sets translucent navigation.
     * 设置透明导航栏，ps:唤醒导航栏不会上推界面
     *
     * @param activity the activity
     */
    public static void setTranslucentNavigation(Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
    }

    /**
     * Set navigation color.
     * 设置导航栏颜色
     *
     * @param activity the activity
     * @param color    the color
     */
    public static void setNavigationColor(Activity activity, int color) {
        activity.getWindow().setNavigationBarColor(color);
    }

    /**
     * Sets max fling velocity.
     * 改变Recycleview的滑动速度,默认8000
     *
     * @param recyclerView the recycler view
     * @param velocity     the velocity
     */
    public static void setMaxFlingVelocity(RecyclerView recyclerView, int velocity) {

        try {
            Field field = recyclerView.getClass().getDeclaredField("mMaxFlingVelocity");
            field.setAccessible(true);
            field.set(recyclerView, velocity);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
    }

    /**
     * Dip 2 px int.
     * dp转化成px
     *
     * @param dp the dp
     * @return the int
     */
    public static int dip2px(float dp) {
        final float scale = MyApplication.getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    /**
     * Px 2 dip int.
     * px转化成dp @param px the px
     *
     * @param px the px
     * @return the int
     */
    public static float px2dip(int px) {
        final float scale = MyApplication.getContext().getResources().getDisplayMetrics().density;
        return (px / scale + 0.5f);
    }
}
