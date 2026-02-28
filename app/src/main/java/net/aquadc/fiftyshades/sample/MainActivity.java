package net.aquadc.fiftyshades.sample;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
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
import jp.wasabeef.recyclerview.animators.LandingAnimator;
import net.aquadc.fiftyshades.CornerSet;
import net.aquadc.fiftyshades.RectInnerShadow;
import net.aquadc.fiftyshades.RectItemsWithShadows;
import net.aquadc.fiftyshades.RectShadow;
import net.aquadc.fiftyshades.RectSpec;
import net.aquadc.fiftyshades.RectWithShadow;
import net.aquadc.fiftyshades.Shadow;
import net.aquadc.fiftyshades.ShadowSpec;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static net.aquadc.fiftyshades.sample.Animators.LAYOUT_HEIGHT;
import static net.aquadc.fiftyshades.sample.Animators.LAYOUT_WIDTH;
import static net.aquadc.fiftyshades.sample.Animators.SHAPE_CORNER_RADIUS;
import static net.aquadc.fiftyshades.sample.Animators.playTogether;
import static net.aquadc.fiftyshades.sample.Animators.shadowAnimator;


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
        float dp = getResources().getDisplayMetrics().density;
        methodChooser.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            private int itemCount = 0; // postpone init to show animation
            @Override public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
                if (itemCount == 0) {
                    recyclerView.post(() -> {
                        itemCount = 3;
                        notifyItemRangeInserted(0, 3);
                    });
                }
            }

            @Override public int getItemCount() {
                return itemCount;
            }
            @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                TextView itemView = new TextView(parent.getContext());
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

                if (Build.VERSION.SDK_INT >= 21)
                    itemView.setStateListAnimator(shadowAnimator(dp));

                return holder;
            }
            @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                TextView itemView = (TextView) holder.itemView;
                itemView.setText(OPTIONS[position]);
                itemView.setSelected(option == position);
            }
        });
        DividerItemDecoration divider = new DividerItemDecoration(this, LinearLayoutManager.HORIZONTAL);
        final int spacing = (int) (16 * dp);
        divider.setDrawable(new ColorDrawable() { @Override public int getIntrinsicWidth() { return spacing; } });
        methodChooser.addItemDecoration(divider);
        methodChooser.setPadding(spacing, spacing, spacing, spacing);
        methodChooser.setClipToPadding(false);
        methodChooser.addItemDecoration(
            new RectItemsWithShadows(
                new RectSpec(Color.WHITE, Integer.MAX_VALUE, 0x60_000000, 0f),
                new ShadowSpec(0f, 0f, 6 * dp, 0x66_000000)
            )
        );
        if (Build.VERSION.SDK_INT >= 21)
            methodChooser.setItemAnimator(new LandingAnimator());
        methodChooser.getItemAnimator().setAddDuration(1500);
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

        SeekBar shadowRadiusSeeker = new SeekBar(this);
        SeekBar cornerRadiusSeeker = new SeekBar(this);
        LinearLayout radiiChoosers = new LinearLayout(this); {
            shadowRadiusSeeker.setId(android.R.id.progress);
            shadowRadiusSeeker.setMax((int) (100 * dp));
            shadowRadiusSeeker.setProgress((int) (20 * dp));
            shadowRadiusSeeker.setOnSeekBarChangeListener(this);
            radiiChoosers.addView(shadowRadiusSeeker, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));

            cornerRadiusSeeker.setId(android.R.id.secondaryProgress);
            cornerRadiusSeeker.setMax((int) (100 * dp));
            cornerRadiusSeeker.setProgress((int) (40 * dp));
            cornerRadiusSeeker.setOnSeekBarChangeListener(this);
            radiiChoosers.addView(cornerRadiusSeeker, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));
        }
        content.addView(radiiChoosers, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

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
                onProgressChanged(shadowRadiusSeeker, shadowRadiusSeeker.getProgress(), false);
                onProgressChanged(cornerRadiusSeeker, cornerRadiusSeeker.getProgress(), false);
                content.getViewTreeObserver().removeOnGlobalLayoutListener(this);
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

        SeekBar shadowRadiusSeeker = this.<SeekBar>findViewById(android.R.id.progress);
        int shadowRadius = shadowRadiusSeeker.getProgress();
        int cornerRadius = this.<SeekBar>findViewById(android.R.id.secondaryProgress).getProgress();
        int strokeWidth = Math.max(1, (int) (1 * dp));
        View sample = findViewById(android.R.id.icon);
        Drawable d = sample.getBackground();
        ShadowSpec shadowSpec = new ShadowSpec(2 * dp, 3 * dp, shadowRadius, 0xFF_7799FF);
        Drawable shadow, shape;
        switch (option) {
            case 0:
                d = RectWithShadow.createDrawable(
                    Color.TRANSPARENT,
                    new RectSpec(0xFF_DDEEFF, cornerRadius, 0xFF_666666, strokeWidth),
                    shadowSpec,
                    null, CornerSet.VALUES.get(cornerChooser.getSelectedItemPosition())
                );
                break;

            case 1:
                if (d instanceof LayerDrawable &&
                        (shadow = ((LayerDrawable) d).getDrawable(0)) instanceof RectShadow &&
                        (shape = ((LayerDrawable) d).getDrawable(1)) instanceof GradientDrawable) {
                    animateShadow((RectShadow) shadow, (GradientDrawable) shape, shadowRadius, cornerRadius);
                    return;
                }
                d = new LayerDrawable(new Drawable[]{
                    shadowDrawable.cornerRadius(cornerRadius).shadow(shadowSpec),
                    RoundRectDrawable(0xFF_DDEEFF, 0xFF_666666, strokeWidth, cornerRadius),
                });
                break;

            case 2:
                if (d instanceof LayerDrawable &&
                        (shape = ((LayerDrawable) d).getDrawable(0)) instanceof GradientDrawable &&
                        (shadow = ((LayerDrawable) d).getDrawable(1)) instanceof RectInnerShadow) {
                    animateShadow((RectInnerShadow) shadow, (GradientDrawable) shape, shadowRadius, cornerRadius);
                    return;
                }
                d = new LayerDrawable(new Drawable[]{
                    RoundRectDrawable(0xFF_DDEEFF, 0xFF_666666, strokeWidth, cornerRadius),
                    innerShadowDrawable.cornerRadius(cornerRadius).shadow(shadowSpec),
                });
                break;

            default:
                throw new AssertionError();
        }
        sample.setBackground(d);
    }

    private Animator shadowAnim;
    private void animateShadow(Shadow shadow, GradientDrawable shape, int shadowRadius, int cornerRadius) {
        if (shadowAnim != null) shadowAnim.cancel();
        shadowAnim = playTogether(
                ObjectAnimator.ofFloat(shadow, Shadow.SHADOW_RADIUS, shadowRadius),
                ObjectAnimator.ofInt(shadow, Shadow.CORNER_RADIUS, cornerRadius),
                ObjectAnimator.ofFloat(shape, SHAPE_CORNER_RADIUS, cornerRadius)
        );
    }

    Animator widthAnim;
    Animator heightAnim;
    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        View sample = findViewById(android.R.id.icon);
        View parent = (View) sample.getParent();
        if (seekBar.getId() == android.R.id.text1) {
            int w = parent.getWidth() * progress / 100;
            if (widthAnim != null) widthAnim.cancel();
            widthAnim = ObjectAnimator.ofInt(sample, LAYOUT_WIDTH, w);
            widthAnim.start();
        } else if (seekBar.getId() == android.R.id.text2) {
            int h = parent.getHeight() * progress / 100;
            if (heightAnim != null) heightAnim.cancel();
            heightAnim = ObjectAnimator.ofInt(sample, LAYOUT_HEIGHT, h);
            heightAnim.start();
        } else {
            replaceShadow();
        }
    }
    @Override public void onStartTrackingTouch(SeekBar seekBar) { }
    @Override public void onStopTrackingTouch(SeekBar seekBar) { }

    private static GradientDrawable RoundRectDrawable(int color, int strokeColor, int strokeWidth, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        d.setStroke(strokeWidth, strokeColor);
        return d;
    }
}
