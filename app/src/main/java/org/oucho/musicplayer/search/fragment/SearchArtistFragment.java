package org.oucho.musicplayer.search.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Artist;
import org.oucho.musicplayer.fragments.AlbumListFragment;
import org.oucho.musicplayer.fragments.BaseFragment;
import org.oucho.musicplayer.fragments.adapters.BaseAdapter;
import org.oucho.musicplayer.fragments.loaders.ArtistLoader;
import org.oucho.musicplayer.search.SearchActivity;
import org.oucho.musicplayer.search.adapter.ArtistViewAdapter;
import org.oucho.musicplayer.utils.PrefSort;
import org.oucho.musicplayer.view.fastscroll.FastScrollRecyclerView;

import java.util.List;

import static org.oucho.musicplayer.search.SearchActivity.setLoaderFilter;

public class SearchArtistFragment extends BaseFragment implements MusiqueKeys {


    private static final String TAG = "SearchArtistFragmebt";

    private ArtistViewAdapter mArtistAdapter;
    private TextView noArtistResult;
    private boolean receiver = false;

    private Context mContext;

    public static SearchArtistFragment newInstance() {

        return new SearchArtistFragment();
    }

    /**********
     * Loader
     **********/
    private final LoaderManager.LoaderCallbacks<List<Artist>> mArtistLoaderCallbacks = new LoaderManager.LoaderCallbacks<List<Artist>>() {

        @Override
        public Loader<List<Artist>> onCreateLoader(int id, Bundle args) {
            Log.d(TAG, "onCreateLoader, args: " + args);

            ArtistLoader loader = new ArtistLoader(getContext());

            loader.setSortOrder(PrefSort.getInstance().getArtistSortOrder());

            if (!String.valueOf(args).equals("Bundle[{filter=}]"))
                setLoaderFilter(args, loader);

            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<Artist>> loader, List<Artist> data) {

            Log.d(TAG, "Loader, setData: " + data);
             mArtistAdapter.setData(data);

            if (data.size() == 0) {
                noArtistResult.setVisibility(View.VISIBLE);
            } else {
                noArtistResult.setVisibility(View.GONE);
            }
        }

        @Override
        public void onLoaderReset(Loader<List<Artist>> loader) {
            // This constructor is intentionally empty, pourquoi ? parce que !
        }

    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getContext();

        Log.d(TAG, "onCreate()");

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.activity_search_fragment, container, false);

        FastScrollRecyclerView mRecyclerView = rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mArtistAdapter = new ArtistViewAdapter();
        mArtistAdapter.setOnItemClickListener(mOnItemClickListener);
        mRecyclerView.setAdapter(mArtistAdapter);

        noArtistResult = rootView.findViewById(R.id.no_result);

        return rootView;
    }


    @Override
    public void onPause() {
        super.onPause();

        if (receiver) {
            mContext.unregisterReceiver(mFragmentListener);
            receiver = false;
        }

    }


    @Override
    public void onResume() {
        super.onResume();

        if (!receiver) {
            IntentFilter filter = new IntentFilter();

            filter.addAction("search.newkey");

            mContext.registerReceiver(mFragmentListener, filter);
            receiver = true;
        }

        load(SearchActivity.getNewText());

    }


    private final BroadcastReceiver mFragmentListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String receiveIntent = intent.getAction();

            assert receiveIntent != null;
            if (receiveIntent.equals("search.newkey")) {

                String value = intent.getStringExtra("text");

                Log.d(TAG, "BroadcastReceiver" + value);

                load(value);
            }
        }
    };


    @Override
    public void load() {

    }

    private void load(String newText) {

        Bundle args = null;
        if (newText != null) {
            args = new Bundle();
            args.putString(FILTER, newText);
        }

        Log.d(TAG, "load: " + newText);

        getLoaderManager().restartLoader(0, args, mArtistLoaderCallbacks);
    }


    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {
            Artist artist = mArtistAdapter.getItem(position);

            switch (view.getId()) {
                case R.id.item_view:

                    Intent intent = new Intent();
                    intent.setAction("search.setTitle");
                    intent.putExtra("title", artist.getName());
                    getActivity().sendBroadcast(intent);

                    Intent intent1 = new Intent();
                    intent1.setAction("search.setTabLayout");
                    intent1.putExtra("TabLayout", false);
                    getActivity().sendBroadcast(intent1);

                    Fragment fragment = AlbumListFragment.newInstance(artist);
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_bottom);
                    ft.replace(R.id.container2, fragment);
                    ft.addToBackStack("SearchAlbum");
                    ft.commit();

                    break;
                default:
                    break;
            }
        }
    };
}

