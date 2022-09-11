package auto.qinglong.fragment.dependence;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.HashMap;

public class PagerAdapter extends FragmentStateAdapter {
    private HashMap<Integer, PagerFragment> fragmentList;

    private PagerInterface pagerInterface;

    public PagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        PagerFragment pagerFragment = new PagerFragment();

        if (position == 0) {
            pagerFragment.setType("nodejs");
        } else if (position == 1) {
            pagerFragment.setType("python3");
        } else if (position == 2) {
            pagerFragment.setType("linux");
        }

        if (fragmentList == null) {
            fragmentList = new HashMap<>();
        }

        fragmentList.put(position, pagerFragment);
        pagerFragment.setPagerInterface(pagerInterface);
        return pagerFragment;
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    public PagerFragment getCurrentFragment(int position) {
        return fragmentList.get(position);
    }

    public void setPagerInterface(PagerInterface pagerInterface) {
        this.pagerInterface = pagerInterface;

    }


}
