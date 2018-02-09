package de.k3b.android.androFotoFinder;

import android.content.Context;
import android.util.Log;

import de.k3b.FotoLibGlobal;
import de.k3b.android.util.AndroidFileCommands;
import de.k3b.io.ListUtils;

/**
 * todo create junit integration tests with arabic locale from this.
 *
 * Created by k3b on 15.08.2017.
 */
public class StringFormatResourceTests {
    public static void test(Context context) {
        if (Global.debugEnabled && FotoLibGlobal.debugEnabled)
        Log.i(Global.LOG_CONTEXT,ListUtils.toString("\n\t"
                ,"testing some translations with parameters"
                ,AndroFotoFinderApp.getBookMarkComment(context)
                ,AndroidFileCommands.getModifyMessage(context, AndroidFileCommands.OP_DELETE, 5, 15)
                ,context.getString(R.string.folder_hide_images_question_message_format, "param")
                ,context.getString(R.string.global_err_sql_message_format, "param", "param2")
                ,context.getString(R.string.file_err_writeprotected, "param", "param2")));
    }
}
