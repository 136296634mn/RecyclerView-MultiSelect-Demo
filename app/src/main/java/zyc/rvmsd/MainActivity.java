package zyc.rvmsd;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;
import zyc.rvmsd.itemselectionsupport.ItemSelectionSupport;

import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<String>> {
    public static final int LOADER_ID = 101;

    @Bind(R.id.btn_select_all_toggle)
    Button mBtnSelectAllToggle;

    @Bind(R.id.rv_demo)
    RecyclerView mRvDemo;

    private SelectableAdapter mAdapter;
    private ItemSelectionSupport mSelectionSupport;
    private boolean mAllSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mRvDemo.addItemDecoration(new HorizontalDividerItemDecoration.Builder(this).build());
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader<List<String>> onCreateLoader(int id, Bundle args) {
        return new DemoLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<List<String>> loader, List<String> data) {
        mSelectionSupport = new ItemSelectionSupport(mRvDemo);
        handleAllSelectedOrNot(false);
        mSelectionSupport.setChoiceModeMultiple(allSelected -> handleAllSelectedOrNot(allSelected));
//        mAdapter = new BaseAdapter(data);
        mAdapter = new SelectableAdapter(data, mSelectionSupport);
        mRvDemo.setAdapter(mAdapter);
    }

    @Override
    public void onLoaderReset(Loader<List<String>> loader) {
        mAdapter.setData(null);
    }

    private void handleAllSelectedOrNot(boolean allSelected) {
        mBtnSelectAllToggle.setText(allSelected ? "unselect_all" : "select_all");
        mAllSelected = allSelected;
    }

    @OnClick({R.id.btn_reload_list, R.id.btn_toast_selected, R.id.btn_select_all_toggle})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_reload_list:
                getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
                break;
            case R.id.btn_toast_selected:
                SparseBooleanArray sba = mSelectionSupport.getCheckedItemPositions();
                StringBuilder sb = new StringBuilder();
                Stream.ofRange(0, sba.size())
                        .filter(i -> sba.valueAt(i))
                        .forEach(i -> sb.append(sba.keyAt(i) + " "));
                Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();
                break;
            case R.id.btn_select_all_toggle:
                final boolean allSelected = mAllSelected;
                Stream.ofRange(0, mAdapter.getItemCount())
                        .forEach(i -> mSelectionSupport.setItemChecked(i, !allSelected));
                mAdapter.notifyDataSetChanged();
                break;
        }
    }

    /****************************************************************************************************************/

    public static class BaseAdapter extends RecyclerView.Adapter<BaseAdapter.BaseViewHolder> {
        private List<String> mList;

        public BaseAdapter(List<String> data) {
            setData(data);
        }

        public void setData(List<String> data) {
            mList = data;
            notifyDataSetChanged();
        }

        @Override
        public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new BaseViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(BaseViewHolder holder, int position) {
            holder.bind(mList.get(position));
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////

        public static class BaseViewHolder extends RecyclerView.ViewHolder {
            @Bind(R.id.tv_demo)
            TextView mTvDemo;

            public BaseViewHolder(View v) {
                super(v);
                ButterKnife.bind(this, v);
            }

            public void bind(String text) {
                mTvDemo.setText(text);
            }
        }
    }

    /****************************************************************************************************************/

    public static class SelectableAdapter extends BaseAdapter {
        private ItemSelectionSupport mSelectionSupport;

        public SelectableAdapter(List<String> data, ItemSelectionSupport selectionSupport) {
            super(data);
            mSelectionSupport = selectionSupport;
        }

        public ItemSelectionSupport getSelectionSupport() {
            return mSelectionSupport;
        }

        @Override
        public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new SelectableViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false),
                    this);
        }

        @Override
        public void onBindViewHolder(BaseViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            ((SelectableViewHolder) holder).bind(position);
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////

        public static class SelectableViewHolder extends BaseViewHolder {
            @Bind(R.id.cb_demo)
            CheckBox mCheckbox;
            private SelectableAdapter mAdapter;

            public SelectableViewHolder(View v, SelectableAdapter adapter) {
                super(v);
                mAdapter = adapter;
                ButterKnife.bind(this, v);
            }

            public void bind(int position) {
                boolean checked = mAdapter.getSelectionSupport().isItemChecked(position);
                mCheckbox.setChecked(checked);
                itemView.setActivated(checked);
            }

            @OnClick(R.id.list_item_container)
            void onItemClick() {
                mAdapter.notifyItemChanged(getPosition()); // 局部更新
            }
        }
    }

    /****************************************************************************************************************/

    private static class DemoLoader extends AsyncTaskLoader<List<String>> {
        public DemoLoader(Context context) {
            super(context);
        }

        @Override
        protected void onStartLoading() {
            // 直接forceLoad()是为了强制调用loadInBackground()。标准的pattern请参考ApiDemos里面LoaderCustom.java
            forceLoad();
        }

        @Override
        public List<String> loadInBackground() {
            return Stream.ofRange(0, 15 + (new Random()).nextInt(15))
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        }
    }
}
