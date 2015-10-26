package com.irateam.vkplayer.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.irateam.vkplayer.R;
import com.irateam.vkplayer.activities.settings.SettingsActivity;
import com.irateam.vkplayer.adapter.AudioAdapter;
import com.irateam.vkplayer.controllers.PlayerController;
import com.irateam.vkplayer.models.Audio;
import com.irateam.vkplayer.receivers.DownloadFinishedReceiver;
import com.irateam.vkplayer.services.AudioService;
import com.irateam.vkplayer.services.DownloadService;
import com.irateam.vkplayer.services.PlayerService;
import com.irateam.vkplayer.services.UserService;
import com.irateam.vkplayer.ui.RoundImageView;
import com.irateam.vkplayer.utils.NetworkUtils;
import com.mobeta.android.dslv.DragSortListView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.vk.sdk.VKSdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListActivity extends AppCompatActivity implements
        AudioService.Listener,
        NavigationView.OnNavigationItemSelectedListener,
        AdapterView.OnItemClickListener,
        DragSortListView.DropListener,
        SwipeRefreshLayout.OnRefreshListener, ServiceConnection, AdapterView.OnItemLongClickListener, AudioAdapter.CoverCheckListener, ActionMode.Callback, DragSortListView.RemoveListener {

    private AudioAdapter audioAdapter = new AudioAdapter(this);
    private AudioService audioService = new AudioService(this);

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RoundImageView roundImageView;
    private TextView userFullName;
    private TextView userVkId;

    private CoordinatorLayout coordinatorLayout;
    private SwipeRefreshLayout refreshLayout;
    private DragSortListView listView;

    private PlayerController playerController;
    private PlayerService playerService;

    private ActionMode actionMode;

    private DownloadFinishedReceiver downloadFinishedReceiver;

    private View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        emptyView = findViewById(R.id.empty_list_view);
        emptyView.setVisibility(View.GONE);

        //Views
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        roundImageView = (RoundImageView) findViewById(R.id.navigation_drawer_header_avatar);
        userFullName = (TextView) findViewById(R.id.navigation_drawer_header_full_name);
        userVkId = (TextView) findViewById(R.id.navigation_drawer_header_id);

        new UserService(this).getCurrentUser((user) -> {
            ImageLoader.getInstance().displayImage(user.photo_100, roundImageView);
            userFullName.setText(user.first_name + " " + user.last_name);
            userVkId.setText(UserService.USER_LINK + String.valueOf(user.id));
        });

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.setDrawerListener(drawerToggle);
        drawerToggle.syncState();

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);

        playerController = new PlayerController(this, findViewById(R.id.player_panel));
        playerController.rootView.setVisibility(View.GONE);
        playerController.setFabOnClickListener(v -> startActivity(new Intent(this, AudioActivity.class)));

        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        refreshLayout.setColorSchemeResources(
                R.color.accent,
                R.color.primary
        );
        refreshLayout.setOnRefreshListener(this);

        listView = (DragSortListView) findViewById(R.id.list);
        listView.setAdapter(audioAdapter);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
        listView.setDropListener(this);
        listView.setRemoveListener(this);

        audioAdapter.setCoverCheckListener(this);
        audioService.addListener(this);

        downloadFinishedReceiver = new DownloadFinishedReceiver() {
            @Override
            public void onDownloadFinished(Audio audio) {
                audioAdapter.updateAudioById(audio);
            }
        };
        registerReceiver(downloadFinishedReceiver, new IntentFilter(DownloadService.DOWNLOAD_FINISHED));

        startService(new Intent(this, PlayerService.class));
        bindService(new Intent(this, PlayerService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerService.removePlayerEventListener(playerController);
        unbindService(this);
        unregisterReceiver(downloadFinishedReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_list, menu);

        MenuItem itemSort = menu.findItem(R.id.action_sort);
        MenuItem itemSortDone = menu.findItem(R.id.action_sort_done);
        MenuItem itemSearch = menu.findItem(R.id.action_search);

        audioAdapter.setSortModeListener(new AudioAdapter.SortModeListener() {
            @Override
            public void onStartSortMode() {
                itemSort.setVisible(false);
                itemSortDone.setVisible(true);
                listView.setDragEnabled(true);
                refreshLayout.setEnabled(false);
            }

            @Override
            public void onFinishSortMode() {
                itemSort.setVisible(true);
                itemSortDone.setVisible(false);
                listView.setDragEnabled(false);
                refreshLayout.setEnabled(true);
            }
        });

        final SearchView searchView = (SearchView) itemSearch.getActionView();
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                audioAdapter.getFilter().filter(newText);
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort:
                audioAdapter.setSortMode(true);
                return true;
            case R.id.action_sort_done:
                audioAdapter.setSortMode(false);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onComplete(List<Audio> list) {
        refreshLayout.setRefreshing(false);
        if (list.isEmpty()) {
            listView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            audioAdapter.setList(list);
            audioAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onError(String errorMessage) {
        audioAdapter.setList(Collections.emptyList());
        refreshLayout.setRefreshing(false);
        Snackbar.make(coordinatorLayout, errorMessage, Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.title_snackbar_action), v -> audioService.repeatLastRequest())
                .show();
    }

    private BroadcastReceiver cacheUpdateReceiver;

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        drawerLayout.closeDrawers();

        if (menuItem.getGroupId() == R.id.audio_group) {
            getSupportActionBar().setTitle(menuItem.getTitle());
            refreshLayout.setRefreshing(true);
        }

        if (menuItem.getItemId() == R.id.cached_audio && cacheUpdateReceiver == null) {
            cacheUpdateReceiver = new DownloadFinishedReceiver() {
                @Override
                public void onDownloadFinished(Audio audio) {
                    audioAdapter.getList().add(audio);
                    audioAdapter.notifyDataSetChanged();
                }
            };
            registerReceiver(cacheUpdateReceiver, new IntentFilter(DownloadService.DOWNLOAD_FINISHED));
        } else if (cacheUpdateReceiver != null) {
            unregisterReceiver(cacheUpdateReceiver);
            cacheUpdateReceiver = null;
        }

        switch (menuItem.getItemId()) {
            case R.id.current_playlist:
                audioService.getCurrentAudio();
                return true;
            case R.id.my_audio:
                audioService.getMyAudio();
                return true;
            case R.id.recommended_audio:
                audioService.getRecommendationAudio();
                return true;
            case R.id.popular_audio:
                audioService.getPopularAudio();
                return true;
            case R.id.cached_audio:
                audioService.getCachedAudio();
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.exit:
                VkLogout();
                return true;
        }
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        playerService.setPlaylist(audioAdapter.getList());
        playerService.play(position);
        if (actionMode != null)
            actionMode.finish();
        MenuItem item = navigationView.getMenu().getItem(0);
        item.setChecked(true);
        onNavigationItemSelected(item);
    }

    @Override
    public void drop(int from, int to) {
        List<Audio> list = audioAdapter.getList();
        Audio audio = list.get(from);
        list.remove(from);
        list.add(to, audio);
        audioAdapter.notifyDataSetChanged();
    }

    @Override
    public void remove(int i) {
        audioAdapter.getList().remove(i);
        audioAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRefresh() {
        if (actionMode != null) {
            actionMode.finish();
        }

        if (audioAdapter.isSortMode()) {
            audioAdapter.setSortMode(false);
        }

        audioService.repeatLastRequest();
    }

    @Override
    public void onBackPressed() {
        if (audioAdapter.isSortMode()) {
            audioAdapter.setSortMode(false);
            listView.setDragEnabled(false);
            refreshLayout.setEnabled(true);
            return;
        }
        super.onBackPressed();
    }

    private void VkLogout() {
        VKSdk.logout();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        playerService = ((PlayerService.PlayerBinder) service).getPlayerService();
        playerController.setPlayerService(playerService);
        playerService.addPlayerEventListener(playerController);

        audioAdapter.setPlayerService(playerService);
        audioService.setPlayerService(playerService);
        if (playerService.getPlaylist().size() > 0) {
            MenuItem item = navigationView.getMenu().getItem(0);
            item.setChecked(true);
            onNavigationItemSelected(item);
        } else if (NetworkUtils.checkNetwork(this)) {
            MenuItem item = navigationView.getMenu().getItem(1);
            item.setChecked(true);
            onNavigationItemSelected(item);
        } else {
            MenuItem item = navigationView.getMenu().getItem(4);
            item.setChecked(true);
            onNavigationItemSelected(item);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        playerService = null;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        performCheck(position);
        return true;
    }

    @Override
    public void onCoverCheck(int position) {
        performCheck(position);
    }

    public void performCheck(int position) {
        audioAdapter.toggleChecked(position);
        if (audioAdapter.getCheckedCount() > 0) {
            if (actionMode == null) {
                startActionMode(this);
            }
            actionMode.setTitle(String.valueOf(audioAdapter.getCheckedCount()));

            Menu menu = actionMode.getMenu();
            MenuItem cacheItem = menu.findItem(R.id.action_cache);
            MenuItem removeFromCache = menu.findItem(R.id.action_remove_from_cache);
            if (audioAdapter.getCachedCheckedItems().size() > 0) {
                removeFromCache.setVisible(true);
            } else {
                removeFromCache.setVisible(false);
            }

            if (audioAdapter.getNotCachedItems().size() > 0) {
                cacheItem.setVisible(true);
            } else {
                cacheItem.setVisible(false);
            }

        } else {
            actionMode.finish();
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        actionMode = mode;
        mode.getMenuInflater().inflate(R.menu.menu_list_context, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        MenuItem menuItem = navigationView.getMenu().findItem(R.id.current_playlist);
        switch (item.getItemId()) {
            case R.id.action_play:
                playerService.setPlaylist(audioAdapter.getCheckedItems());
                playerService.play(0);
                menuItem.setChecked(true);
                onNavigationItemSelected(menuItem);
                break;

            case R.id.action_cache:
                Intent intent = new Intent(this, DownloadService.class);
                intent.setAction(DownloadService.START_DOWNLOADING);
                intent.putExtra(DownloadService.AUDIO_LIST, (ArrayList<Audio>) audioAdapter.getCheckedItems());
                startService(intent);
                break;

            case R.id.action_remove_from_cache:
                List<Audio> list = audioAdapter.getCachedCheckedItems();
                audioService.removeFromCache(list, new AudioService.Listener() {
                    @Override
                    public void onComplete(List<Audio> list) {
                        audioAdapter.updateAudiosById(list);
                    }

                    @Override
                    public void onError(String errorMessage) {

                    }
                });
                break;

            case R.id.action_delete:
                audioAdapter.removeChecked();
                break;

            case R.id.action_add_to_playlist:
                List<Audio> checked = audioAdapter.getCheckedItems();
                List<Audio> playlist = playerService.getPlaylist();
                playlist.addAll(checked);
                audioAdapter.notifyDataSetChanged();
                Snackbar.make(coordinatorLayout, R.string.snackbar_add_to_playlist, Snackbar.LENGTH_LONG)
                        .setAction(R.string.snackbar_cancel, (v -> {
                            for (int i = 0; i < checked.size(); i++)
                                playlist.remove(playlist.size() - 1);
                        }))
                        .show();
                break;
        }
        mode.finish();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        audioAdapter.clearChecked();
        actionMode = null;
    }
}
