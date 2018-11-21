package org.oucho.musicplayer.fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.utils.ToolbarDrawerToggle;
import org.oucho.musicplayer.tools.LockableViewPager;

import java.util.HashMap;
import java.util.Map;


public class LibraryFragment extends BaseFragment implements MusiqueKeys {


    @SuppressWarnings("unused")
    private static final String TAG_LOG = "Search Activity";

    private final Handler mHandler = new Handler();

    private Context mContext;

    private LinearLayout shadow;
    private TabLayout tabLayout;

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private boolean receiver = false;
    private boolean pagerVisible = false;


    private LockableViewPager mViewPager;

    private void setmViewPager(LockableViewPager value) {
        mViewPager = value;
    }

    public static LibraryFragment newInstance() {

        return new LibraryFragment();
    }


    @Override
    public void onPause() {
        super.onPause();

        if (receiver) {
            mContext.unregisterReceiver(mServiceListener);
            receiver = false;
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        if (!receiver) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(INTENT_TOOLBAR_SHADOW);
            filter.addAction(INTENT_BACK);

            mContext.registerReceiver(mServiceListener, filter);
            receiver = true;
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        MainActivity activity = (MainActivity) getActivity();

        mContext = activity.getApplicationContext();
        Toolbar toolbar = rootView.findViewById(R.id.toolbar);

        DrawerLayout drawerLayout = activity.getDrawerLayout();

        activity.setSupportActionBar(toolbar);

        ToolbarDrawerToggle drawerToggle = new ToolbarDrawerToggle(activity,drawerLayout,toolbar, new int[]{Gravity.START});
        drawerLayout.addDrawerListener(drawerToggle);

        mSectionsPagerAdapter = new SectionsPagerAdapter( getChildFragmentManager());

        setmViewPager(rootView.findViewById(R.id.pager));
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(mViewPagerChangeListener);

        tabLayout = rootView.findViewById(R.id.tab_layout_indicator);
        tabLayout.setupWithViewPager(mViewPager, true);

        shadow = rootView.findViewById(R.id.toolbar_shadow);

        return rootView;
    }


    private final ViewPager.OnPageChangeListener mViewPagerChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrollStateChanged(int state) {

            if (!pagerVisible) {
                Animation anime = AnimationUtils.loadAnimation(mContext, R.anim.pager_fade_in);
                tabLayout.startAnimation(anime);
                tabLayout.setVisibility(View.VISIBLE);
                pagerVisible = true;
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            mHandler.removeCallbacks(removeDot);
            mHandler.postDelayed(removeDot, 500);

        }

        @Override
        public void onPageSelected(int position) {

        }
    };


    private final Runnable removeDot = new Runnable() {
        @Override
        public void run() {

            if (pagerVisible) {
                Animation anime = AnimationUtils.loadAnimation(mContext, R.anim.pager_fade_out);
                tabLayout.startAnimation(anime);
                tabLayout.setVisibility(View.GONE);
                pagerVisible = false;
            }
        }
    };


    private void backToPrevious() {
        mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
    }


    @Override
    public void load() {
        int fragmentCount = mSectionsPagerAdapter.getCount();
        for(int pos = 0; pos < fragmentCount; pos++) {
            BaseFragment fragment = (BaseFragment) mSectionsPagerAdapter.getFragment(pos);
            if(fragment != null) {
                fragment.load();
            }
        }
    }


    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        private final Map<Integer, String> mFragmentTags;

        @SuppressLint("UseSparseArrays")
        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            mFragmentTags = new HashMap<>();

        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return AlbumListFragment.newInstance();
                case 1:
                    return SongListFragment.newInstance();
                case 2:
                    return PlaylistListFragment.newInstance();
                default: //do nothing
                    break;
            }
            return null;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object obj = super.instantiateItem(container, position);
            if (obj instanceof Fragment) {
                Fragment f = (Fragment) obj;
                String tag = f.getTag();
                mFragmentTags.put(position, tag);
            }
            return obj;
        }

        Fragment getFragment(int position) {
            String tag = mFragmentTags.get(position);
            if (tag == null)
                return null;
            return getChildFragmentManager().findFragmentByTag(tag);
        }

        @Override
        public int getCount() {

            return 3;
        }

    }


    private final BroadcastReceiver mServiceListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String receiveIntent = intent.getAction();

            assert receiveIntent != null;
            if (receiveIntent.equals(INTENT_TOOLBAR_SHADOW)) {

                boolean value = intent.getBooleanExtra("boolean", true);

                if (value)
                    shadow.setElevation(mContext.getResources().getDimension(R.dimen.toolbar_elevation));

                if (!value)
                    shadow.setElevation(0);
            }

            if (receiveIntent.equals(INTENT_BACK)) {
                backToPrevious();
            }
        }
    };
}
