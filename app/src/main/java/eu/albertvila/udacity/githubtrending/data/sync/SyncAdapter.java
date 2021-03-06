package eu.albertvila.udacity.githubtrending.data.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.albertvila.udacity.githubtrending.R;
import eu.albertvila.udacity.githubtrending.data.Settings;
import eu.albertvila.udacity.githubtrending.data.db.DbContract;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 * Created by Albert Vila Calvo on 18/9/16.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);

    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String authority, final ContentProviderClient contentProviderClient, SyncResult syncResult) {
        Timber.d("onPerformSync()");

        // When we update SharedPreferences and then run an expedited sync, calling
        // Settings.get(getContext()).getGitHubUrl() may not give us the latest value because the
        // SyncAdapter runs in a different process. Hence we must pass the URL using the Bundle.
        // However for periodic syncs we are not passing any value with the Bundle.
        String gitHubUrl = Settings.get(getContext()).getGitHubUrl();
        if (bundle.containsKey(SyncUtils.GITHUB_URL_KEY)) {
            gitHubUrl = bundle.getString(SyncUtils.GITHUB_URL_KEY);
        }

        Timber.d("onPerformSync() GitHub Url: %s", gitHubUrl);

        // Donwload github.com/trending as HTML String
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(gitHubUrl)
                .build();

        Response response;
        String html;
        try {
            response = client.newCall(request).execute();
            html = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        response.body().close();

        // Delete all items in DB
        getContext().getContentResolver().delete(DbContract.Repo.CONTENT_URI, null, null);

        // Parse HTML and obtain repositories URL and description
        Document document = Jsoup.parse(html);
        Elements repos = document.select("li.col-12.d-block");
        Timber.d("repos size: %d", repos.size());

        List<ContentValues> contentValuesList = new ArrayList<>();
        for (Element repo : repos) {
            ContentValues values = new ContentValues();

            String url = repo.select("div.d-inline-block a").get(0).attr("href");
            // We remove the first '/'
            values.put(DbContract.Repo.COLUMN_URL, url.substring(1));

            // Some repos don't have description
            Elements descriptions = repo.select("div.py-1");
            String description = "";
            if (descriptions.size() > 0) {
                description = descriptions.get(0).text();
            }
            values.put(DbContract.Repo.COLUMN_DESCRIPTION, description);

            contentValuesList.add(values);
        }

        // Insert new repos into DB
        ContentValues[] contentValuesArray = contentValuesList.toArray(new ContentValues[contentValuesList.size()]);
        getContext().getContentResolver().bulkInsert(DbContract.Repo.CONTENT_URI, contentValuesArray);


        // UPDATE THE WIDGET BY SENDING A BROADCAST. The broadcast will be received by WidgetProvider
        String actionDataUpdated = getContext().getString(R.string.action_data_updated);
        // Setting the package ensures that only components in our app will receive the broadcast
        Intent dataUpdatedIntent =
                new Intent(actionDataUpdated).setPackage(getContext().getPackageName());
        getContext().sendBroadcast(dataUpdatedIntent);
        Timber.d("Send broadcast to WidgetProvider");


        /*
        getHtml()
                .map(new Function<String, List<String>>() {
                    @Override
                    public List<String> apply(String html) throws Exception {
                        Document document = Jsoup.parse(html);
                        Elements elements = document.select(".repo-list-name > a");
                        List<String> urls = new ArrayList<>();
                        for (int i = 0; i < elements.size(); i++) {
                            urls.add(elements.get(i).attr("href"));
                        }
                        Timber.d("urls %s", urls);
                        return urls;
                    }
                })
                .doOnNext(new Consumer<List<String>>() {
                    @Override
                    public void accept(List<String> strings) throws Exception {
                        // Delete all items in DB
                        contentProviderClient.delete(DbContract.Repo.CONTENT_URI, null, null);
                        // Save to DB
                        for (int i = 0; i < strings.size(); i++) {
                            ContentValues values = new ContentValues();
                            values.put(DbContract.Repo.COLUMN_URL, strings.get(i));
                            contentProviderClient.insert(DbContract.Repo.CONTENT_URI, values);
                        }
                    }
                })
                .subscribe(new DefaultObserver<List<String>>() {
                    @Override
                    public void onNext(List<String> value) {
                        Timber.d("onNext: %s", value);
                    }
                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e, "onError");
                    }
                    @Override
                    public void onComplete() {
                        Timber.i("onComplete");
                    }
                });
        */
    }

    // Gets github.com/trending as String
    /*
    private Observable<String> getHtml() {
        final OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder()
                .url("https://github.com/trending")
                .build();

        return Observable.fromCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Response response = client.newCall(request).execute();
                String html = response.body().string();
                response.body().close();
                return html;
            }
        });
    }
    */

}
