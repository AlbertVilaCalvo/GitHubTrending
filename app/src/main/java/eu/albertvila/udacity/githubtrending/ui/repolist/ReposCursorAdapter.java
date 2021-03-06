package eu.albertvila.udacity.githubtrending.ui.repolist;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import eu.albertvila.udacity.githubtrending.R;
import eu.albertvila.udacity.githubtrending.data.db.DbContract;

/**
 * Created by Albert Vila Calvo on 13/9/16.
 */
public class ReposCursorAdapter extends RecyclerView.Adapter<ReposCursorAdapter.ViewHolder> {

    private Cursor cursor;

    public ReposCursorAdapter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_repo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (cursor != null && cursor.moveToPosition(position)) {
            holder.bind(cursor);
        }
    }

    @Override
    public int getItemCount() {
        return (cursor == null) ? 0 : cursor.getCount();
    }

    public void swapCursor(Cursor newCursor) {
        cursor = newCursor;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // Example: ReactiveX/RxJava
        String lastUrlPart;

        TextView urlTextView;
        TextView descriptionTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            urlTextView = (TextView) itemView.findViewById(R.id.list_item_repo_url);
            descriptionTextView = (TextView) itemView.findViewById(R.id.list_item_repo_description);
        }

        public void bind(Cursor cursor) {
            lastUrlPart = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Repo.COLUMN_URL));
            urlTextView.setText(lastUrlPart);
            descriptionTextView.setText(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.Repo.COLUMN_DESCRIPTION)));
        }

        @Override
        public void onClick(View view) {
            // The DB contains only the last part of the URL (for example "ReactiveX/RxJava")
            // We have to construct the entire URL by prepending "https://github.com/"
            String completeUrl = view.getContext().getString(R.string.github_website_base_url, lastUrlPart);
            // Open web browser
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(completeUrl));
            view.getContext().startActivity(intent);
        }
    }
}
