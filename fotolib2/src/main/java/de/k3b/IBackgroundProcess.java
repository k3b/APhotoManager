package de.k3b;

/**
 * Created by k3b on 25.06.2016.
 * Interface that is similar to
 * android.os.AsyncTask but has no dependencies to android
 */
public interface IBackgroundProcess<Progress> {
    /**
     * This method can be invoked from doInBackground to
     * publish updates on the UI thread while the background computation is
     * still running.
     *
     * @param values The progress values to update the UI with.
     *
     */
    void publishProgress_(Progress... values);

    /**
     * Returns <tt>true</tt> if this task was cancelled before it completed
     * normally.
     *
     * @return <tt>true</tt> if task was cancelled before it completed
     */
    boolean isCancelled_();
}
