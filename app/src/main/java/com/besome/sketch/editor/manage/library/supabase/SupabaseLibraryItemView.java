package com.besome.sketch.editor.manage.library.supabase;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.Nullable;

import com.besome.sketch.beans.ProjectLibraryBean;
import com.besome.sketch.editor.manage.library.LibraryItemView;

import pro.sketchware.R;
import pro.sketchware.supabase.SupabaseConfig;

/**
 * Library list row for Supabase. Its state lives in {@link SupabaseConfig}, not in a
 * {@link ProjectLibraryBean}, so {@code setData}'s argument is ignored.
 */
@SuppressLint("ViewConstructor")
public class SupabaseLibraryItemView extends LibraryItemView {
    private final String scId;

    public SupabaseLibraryItemView(Context context, String scId) {
        super(context);
        this.scId = scId;
    }

    @Override
    public void setData(@Nullable ProjectLibraryBean ignored) {
        boolean isEnabled = SupabaseConfig.load(scId).isUsable();
        icon.setImageResource(R.drawable.ic_mtrl_supabase);
        title.setText(R.string.design_library_supabase_title);
        description.setText(R.string.design_library_supabase_description);
        enabled.setText(isEnabled ? "ON" : "OFF");
        enabled.setSelected(isEnabled);
    }
}
