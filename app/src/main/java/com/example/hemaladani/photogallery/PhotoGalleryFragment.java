package com.example.hemaladani.photogallery;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hemaladani on 5/2/17.
 */

public class PhotoGalleryFragment extends VisibleFragment {
    private RecyclerView mPhotoRecyclerView;
    private static final String TAG="PhotoGalleryFragment";
    private List<GalleryItem> mItems=new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder>mThumbnailDownloader;




    private void setUpAdapter(){
        if(isAdded()){
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
            Log.i("Size of elem adap",mPhotoRecyclerView.getAdapter().getItemCount()+"");

        }

    }

    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener{


        private ImageView mImageView;
        private GalleryItem mGalleryItem;

        public PhotoHolder(View itemView) {
            super(itemView);
            mImageView=(ImageView)itemView.findViewById(R.id.item_image_view);
            itemView.setOnClickListener(this);
        }
        public void bindDrawable(Drawable drawable){
            mImageView.setImageDrawable(drawable);
        }
        public void bindGalleryItem(GalleryItem galleryItem){
            mGalleryItem=galleryItem;
        }


        @Override
        public void onClick(View v) {
            /*Intent i=new Intent(Intent.ACTION_VIEW,mGalleryItem.getPhotographUri());
            startActivity(i);*/

            Intent i=PhotoPageActivity.newIntent(getActivity(),mGalleryItem.getPhotographUri());
            startActivity(i);

        }
    }
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{


        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems){

            mGalleryItems=galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            LayoutInflater inflater=LayoutInflater.from(getActivity());
            View view=inflater.inflate(R.layout.list_item_gallery,parent,false);
            return new PhotoHolder(view);

        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem=mGalleryItems.get(position);
            holder.bindGalleryItem(galleryItem);
            Drawable placeHolder= ContextCompat.getDrawable(getContext(),R.drawable.jocker);
            holder.bindDrawable(placeHolder);
            mThumbnailDownloader.queThumbnail(holder,galleryItem.getmUrl());


        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();

        }
    }


    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }


    public class FetchItemTask extends AsyncTask<Void,Void,List<GalleryItem>>{

        private String mQuery;


        public FetchItemTask(String query){
            mQuery=query;
        }


        @Override
        protected List<GalleryItem> doInBackground(Void... params) {


            if(mQuery==null){
            return new FlickrFetchr().fetchRecentPhotos();}else{
                return new FlickrFetchr().searchPhotos(mQuery);
            }

        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            mItems=galleryItems;
            setUpAdapter();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(),null);
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldSetAlarm= !PollService.isServiceAlarm(getActivity());
                PollService.setServiceAlarm(getActivity(),shouldSetAlarm);
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery,menu);
        MenuItem searchItem=menu.findItem(R.id.menu_item_search);
        final SearchView searchView=(SearchView) searchItem.getActionView();
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query=QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query,false);
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG,"Query Submit:"+query);
                QueryPreferences.setStoredQuery(getActivity(),query);
                updateItems();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG,"Query text change"+newText);
                return false;
            }
        });
        MenuItem toggleItem=menu.findItem(R.id.menu_item_toggle_polling);
        if(PollService.isServiceAlarm(getActivity())){
            toggleItem.setTitle(R.string.stop_polling);
        }else{
            toggleItem.setTitle(R.string.start_polling);
        }



    }

    private void updateItems(){
        String query=QueryPreferences.getStoredQuery(getActivity());
        new FetchItemTask(query).execute();

    }



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        updateItems();




        Handler responseHandler=new Handler();
        mThumbnailDownloader=new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                        Drawable drawable=new BitmapDrawable(getContext().getResources(),bitmap);
                        photoHolder.bindDrawable(drawable);

                    }
                }


        );
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG,"Background Thread started");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();

        Log.i(TAG,"Background thread destroyed");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.fragment_photo_gallery,container,false);
        mPhotoRecyclerView=(RecyclerView)v.findViewById(R.id.photo_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));

        return v;
    }
}
