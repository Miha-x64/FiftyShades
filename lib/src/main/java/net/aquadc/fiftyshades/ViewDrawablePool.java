package net.aquadc.fiftyshades;

import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.view.View;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;

@RequiresApi(11) final class ViewDrawablePool {
    private ViewDrawablePool() {}

    static long usedMarkFor(SparseArray<?> mappings, View v) {
        int iof = mappings.indexOfKey(System.identityHashCode(v));
        return iof >= 0 ? 1L << iof : 0L; // on hash collision we'll just mark same index twice
        // thus, popCount(usedDrawables) <= children
    }

    static <T> void scrapUnused(SparseArray<? extends T> mapping, ArrayList<? super T> scrap, long used) {
        int drwbls = mapping.size();
        for (int index = Math.min(64, drwbls)-1; index >= 0; index--)
            if ((used & (1L << index)) == 0)
                scrapAt(mapping, scrap, index);
        drwbls -= 64; // sorry fellow, I don't honestly believe that your users need to see >64 items
        for (; drwbls > 0; drwbls--) // don't remember View:Shadow but still behave correctly
            scrapAt(mapping, scrap, mapping.size() - 1); // and I don't mind repeated size() call here, deal with it
    }
    private static <T> void scrapAt(SparseArray<? extends T> mapping, ArrayList<? super T> scrap, int index) {
        scrap.add(mapping.valueAt(index));
        mapping.removeAt(index);
    }

    static <T extends Drawable> T unsafeDrawableFor(
        SparseArray<T> drawables, ArrayList<? extends T> scrap, Drawable.ConstantState factory, View v
    ) {
        int key = System.identityHashCode(v);
        T drawable = drawables.get(key); // when hashes collide, we just reuse the drawable (badly but correctly)
        if (drawable == null) {
            int scrapSize = scrap.size();
            // ConstantState is not generic, unfortunately:
            //noinspection unchecked
            drawables.put(
                key,
                drawable = scrapSize == 0 ? (T) factory.newDrawable().mutate() : scrap.remove(scrapSize - 1)
            );
        }
        return drawable;
    }
}
