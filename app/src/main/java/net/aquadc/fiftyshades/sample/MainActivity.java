package net.aquadc.fiftyshades.sample;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.StateListAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Property;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import net.aquadc.fiftyshades.CornerSet;
import net.aquadc.fiftyshades.RectInnerShadow;
import net.aquadc.fiftyshades.RectItemsWithShadows;
import net.aquadc.fiftyshades.RectShadow;
import net.aquadc.fiftyshades.RectSpec;
import net.aquadc.fiftyshades.RectWithShadow;
import net.aquadc.fiftyshades.ShadowSpec;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static net.aquadc.fiftyshades.RectItemsWithShadows.*;


public final class MainActivity extends Activity
    implements AdapterView.OnItemSelectedListener, SeekBar.OnSeekBarChangeListener {

    static final String[] OPTIONS = { "RectWithShadow", "Shape+RectShadow", "Shape+RectInnerShadow" };
    int option = 0;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) option = savedInstanceState.getInt("option");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        RecyclerView methodChooser = new RecyclerView(this);
        methodChooser.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        methodChooser.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override public int getItemCount() {
                return 3;
            }
            @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                TextView itemView = new TextView(parent.getContext());
                float dp = getResources().getDisplayMetrics().density;
                int pad = (int) (12 * dp);
                itemView.setPadding(pad, pad, pad, pad);
                final RecyclerView.ViewHolder holder = new RecyclerView.ViewHolder(itemView) {};
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        int position = holder.getBindingAdapterPosition();
                        if (position != option) {
                            notifyItemChanged(option, Void.class);
                            notifyItemChanged(option = position, Void.class);
                            replaceShadow();
                        }
                    }
                });

                if (Build.VERSION.SDK_INT >= 21) {
                    StateListAnimator sla = new StateListAnimator();
                    sla.addState(
                        new int[] { android.R.attr.state_selected },
                        ObjectAnimator.ofPropertyValuesHolder(
                            (Object) null,
                            PropertyValuesHolder.ofFloat(DECOR_SHADOW_RADIUS, 16 * dp),
                            PVH_ofColor(DECOR_SHADOW_COLOR, 0xFF_44AA66),
                            PVH_ofColor(DECOR_RECT_FILL_COLOR, 0xFF_AAFFCC),
                            PropertyValuesHolder.ofFloat(DECOR_RECT_STROKE_WIDTH, dp)
                        )
                    );
                    sla.addState(
                        new int[0],
                        ObjectAnimator.ofPropertyValuesHolder(
                            (Object) null,
                            PropertyValuesHolder.ofFloat(DECOR_SHADOW_RADIUS, 8 * dp),
                            PVH_ofColor(DECOR_SHADOW_COLOR, 0x7F_000000),
                            PVH_ofColor(DECOR_RECT_FILL_COLOR, Color.WHITE),
                            PropertyValuesHolder.ofFloat(DECOR_RECT_STROKE_WIDTH, 0f)
                        )
                    );
                    itemView.setStateListAnimator(sla);
                }

                return holder;
            }
            private final ArgbEvaluator argb = new ArgbEvaluator();
            private PropertyValuesHolder PVH_ofColor(Property<View, Integer> prop, int... colors) {
                PropertyValuesHolder holder = PropertyValuesHolder.ofInt(prop, colors);
                holder.setEvaluator(argb);
                return holder;
            }
            @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                TextView itemView = (TextView) holder.itemView;
                itemView.setText(OPTIONS[position]);
                itemView.setSelected(option == position);
            }
        });
        DividerItemDecoration divider = new DividerItemDecoration(this, LinearLayoutManager.HORIZONTAL);
        final int spacing = (int) (16 * getResources().getDisplayMetrics().density);
        divider.setDrawable(new ColorDrawable() { @Override public int getIntrinsicWidth() { return spacing; } });
        methodChooser.addItemDecoration(divider);
        methodChooser.setPadding(spacing, spacing, spacing, spacing);
        methodChooser.setClipToPadding(false);
        methodChooser.addItemDecoration(
            new RectItemsWithShadows(
                new RectSpec(Color.TRANSPARENT, Integer.MAX_VALUE, 0x60_000000, 0f),
                new ShadowSpec(0f, 0f, 0f, Color.TRANSPARENT)
            )
        );
        content.addView(methodChooser, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        Spinner cornerChooser = new Spinner(this);
        cornerChooser.setId(android.R.id.selectAll);
        ArrayAdapter<CornerSet> adapter =
            new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, CornerSet.VALUES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cornerChooser.setAdapter(adapter);
        cornerChooser.setOnItemSelectedListener(this);
        cornerChooser.setSelection(CornerSet.ALL.ordinal());
        content.addView(cornerChooser, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        View sample = new View(this);
        sample.setId(android.R.id.icon);
        FrameLayout sampleWrap = new FrameLayout(this);
        sampleWrap.setClipChildren(false);
        sampleWrap.addView(sample, new FrameLayout.LayoutParams(0, 0, Gravity.CENTER));
        content.addView(sampleWrap, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f));

        final SeekBar widthSeeker = new SeekBar(this);
        widthSeeker.setId(android.R.id.text1);
        widthSeeker.setProgress(50);
        widthSeeker.setOnSeekBarChangeListener(this);
        content.addView(widthSeeker, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        final SeekBar heightSeeker = new SeekBar(this);
        heightSeeker.setId(android.R.id.text2);
        heightSeeker.setProgress(50);
        heightSeeker.setOnSeekBarChangeListener(this);
        content.addView(heightSeeker, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        setContentView(content);
        content.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                onProgressChanged(widthSeeker, widthSeeker.getProgress(), false);
                onProgressChanged(heightSeeker, heightSeeker.getProgress(), false);
            }
        });
    }
    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("option", option);
    }

    @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        replaceShadow();
    }
    @Override public void onNothingSelected(AdapterView<?> parent) { }

    private final RectShadow shadowDrawable = new RectShadow();
    private final RectInnerShadow innerShadowDrawable = new RectInnerShadow();
    private void replaceShadow() {
        float dp = getResources().getDisplayMetrics().density;
        Spinner cornerChooser = findViewById(android.R.id.selectAll);
        cornerChooser.setEnabled(option == 0);
        if (option != 0) cornerChooser.setSelection(CornerSet.ALL.ordinal());

        int cornerRadius = (int) (20 * dp);
        int strokeWidth = Math.max(1, (int) (1 * dp));
        Drawable d;
        ShadowSpec shadow = new ShadowSpec(2 * dp, 3 * dp, 20 * dp, 0xFF_7799FF);
        switch (option) {
            case 0:
                d = RectWithShadow.createDrawable(
                    Color.TRANSPARENT,
                    new RectSpec(0xFF_DDEEFF, cornerRadius, 0xFF_666666, strokeWidth),
                    shadow,
                    null, CornerSet.VALUES.get(cornerChooser.getSelectedItemPosition())
                );
                break;

            case 1:
                d = new LayerDrawable(new Drawable[]{
                    shadowDrawable.cornerRadius(cornerRadius).shadow(shadow),
                    RoundRectDrawable(0xFF_DDEEFF, 0xFF_666666, strokeWidth, cornerRadius)
                });
                break;

            case 2:
                d = new LayerDrawable(new Drawable[]{
                    RoundRectDrawable(0xFF_DDEEFF, 0xFF_666666, strokeWidth, cornerRadius),
                    innerShadowDrawable.cornerRadius(cornerRadius).shadow(shadow)
                });
                break;

            default:
                throw new AssertionError();
        }
        findViewById(android.R.id.icon).setBackground(d);
    }

    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        View sample = findViewById(android.R.id.icon);
        ViewGroup.LayoutParams lp = sample.getLayoutParams();
        int w = lp.width, h = lp.height;
        View parent = (View) sample.getParent();
        if (seekBar.getId() == android.R.id.text1) {
            w = parent.getWidth() * progress / 100;
        } else {
            h = parent.getHeight() * progress / 100;
        }
        if (lp.width != w || lp.height != h) { // without this check we're gonna remeasure every frame
            lp.width = w;
            lp.height = h;
            sample.setLayoutParams(lp);
        }
    }
    @Override public void onStartTrackingTouch(SeekBar seekBar) { }
    @Override public void onStopTrackingTouch(SeekBar seekBar) { }

    private static Drawable RoundRectDrawable(int color, int strokeColor, int strokeWidth, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        d.setStroke(strokeWidth, strokeColor);
        return d;
    }
}
