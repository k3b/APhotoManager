package de.k3b.android.androFotoFinder;

import android.content.Context;
import android.util.Log;

import de.k3b.android.util.AndroidFileCommands;

/**
 * todo create junit integration tests with arabic locale from this.
 *
 * Created by k3b on 15.08.2017.
 */
public class StringFormatResourceTests {
    public static void test(Context context) {
        Log.i(Global.LOG_CONTEXT,"testing some translation parameters");
        Log.i(Global.LOG_CONTEXT,AndroFotoFinderApp.getBookMarkComment(context));
        Log.i(Global.LOG_CONTEXT,AndroidFileCommands.getModifyMessage(context, AndroidFileCommands.OP_DELETE, 5, 15));
        Log.i(Global.LOG_CONTEXT,context.getString(R.string.folder_hide_images_question_message_format, "param"));
        Log.i(Global.LOG_CONTEXT,context.getString(R.string.global_err_sql_message_format, "param", "param2"));
        Log.i(Global.LOG_CONTEXT,context.getString(R.string.file_err_writeprotected, "param", "param2"));
    }
}
