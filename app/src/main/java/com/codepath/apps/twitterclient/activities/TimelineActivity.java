package com.codepath.apps.twitterclient.activities;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.codepath.apps.twitterclient.R;
import com.codepath.apps.twitterclient.TwitterApplication;
import com.codepath.apps.twitterclient.adapters.TweetsAdapter;
import com.codepath.apps.twitterclient.fragments.ComposeDialogFragment;
import com.codepath.apps.twitterclient.models.Tweet;
import com.codepath.apps.twitterclient.network.JSONDeserializer;
import com.codepath.apps.twitterclient.network.TwitterClient;
import com.codepath.apps.twitterclient.util.AppConstants;
import com.codepath.apps.twitterclient.util.ErrorHandler;
import com.codepath.apps.twitterclient.util.views.DividerItemDecoration;
import com.codepath.apps.twitterclient.util.views.EndlessRecyclerViewScrollListener;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cz.msebera.android.httpclient.Header;

public class TimelineActivity extends AppCompatActivity implements ComposeDialogFragment.ComposeDialogListener {
  private static final String TAG = TimelineActivity.class.getSimpleName();

  @BindView(R.id.rvTweets)
  RecyclerView rvTweets;
  @BindView(R.id.fabComposeTweet)
  FloatingActionButton fabComposeTweet;

  private TwitterClient mClient;
  private TweetsAdapter mAdapter;
  private List<Tweet> mTweetList;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_timeline);
    ButterKnife.bind(this);

    mClient = TwitterApplication.getRestClient();
    initTweetList();
  }

  private void initTweetList() {
    mTweetList = new ArrayList<>();
    mAdapter = new TweetsAdapter(this, mTweetList);
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
    rvTweets.setLayoutManager(linearLayoutManager);
    rvTweets.setAdapter(mAdapter);

    RecyclerView.ItemDecoration itemDecoration = new
        DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST);
    rvTweets.addItemDecoration(itemDecoration);
    rvTweets.addOnScrollListener(new EndlessRecyclerViewScrollListener(linearLayoutManager) {
      @Override
      public void onLoadMore(int page, int totalItemsCount) {
        customLoadMoreDataFromApi(page);
      }
    });

    populateTimeline(1);
  }

  private void customLoadMoreDataFromApi(int page) {
    Log.d(TAG, "------ customLoadMoreDataFromApi:page=" + page);
    //populateTimeline(page);
  }

  // Send an API request to get the timeline JSON
  // Fill the listview by creating the tweet objects from JSON
  private void populateTimeline(int page) {
    mClient.getHomeTimeline(page, new JsonHttpResponseHandler() {
      @Override
      public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
        Log.d(TAG, "====== onSuccess: " + response.toString());
        try {
          JSONDeserializer<Tweet> deserializer = new JSONDeserializer<>(Tweet.class);
          List<Tweet> tweetResponseList = deserializer.fromJSONArrayToList(response);
          if (tweetResponseList != null) {
            Log.d(TAG, "------ size: " + tweetResponseList.size());
            int curSize = mTweetList.size();
            mTweetList.addAll(tweetResponseList);
            mAdapter.notifyItemRangeInserted(curSize, tweetResponseList.size());
          }
        } catch (JSONException e) {
          ErrorHandler.handleAppException(e, "Exception from populating Twitter timeline");
        }
      }

      @Override
      public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
        ErrorHandler.logAppError(errorResponse.toString());
        ErrorHandler.displayError(TimelineActivity.this, AppConstants.DEFAULT_ERROR_MESSAGE);
      }
    });
  }

  @OnClick(R.id.fabComposeTweet)
  public void composeTweet() {
    Log.d(TAG, "======== compose tweet");
    FragmentManager fm = getSupportFragmentManager();
    ComposeDialogFragment composeDialogFragment = ComposeDialogFragment.newInstance();
    composeDialogFragment.show(fm, "fragment_compose");
  }

  @Override
  public void onUpdateStatusSuccess(Tweet status) {
    Log.d(TAG, "-------- compose success: " + status.text);
    if (status != null) {
      // Add to the beginning of the list and scroll to the top
      mTweetList.add(0, status);
      mAdapter.notifyItemInserted(0);
      rvTweets.scrollToPosition(0);
    }
  }
}
