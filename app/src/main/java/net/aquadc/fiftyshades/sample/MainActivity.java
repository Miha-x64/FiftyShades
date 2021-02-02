package net.aquadc.fiftyshades.sample;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import net.aquadc.fiftyshades.CornerSet;
import net.aquadc.fiftyshades.RectShadow;
import net.aquadc.fiftyshades.RectWithShadow;
import net.aquadc.fiftyshades.ShadowSpec;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;


public final class MainActivity extends Activity
    implements AdapterView.OnItemSelectedListener, RadioGroup.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        RadioGroup methodChooser = new RadioGroup(this);
        methodChooser.setId(android.R.id.checkbox);
        methodChooser.setOnCheckedChangeListener(this);
        methodChooser.setOrientation(LinearLayout.HORIZONTAL);
        RadioButton rws = new RadioButton(this);
        rws.setText("RectWithShadow");
        rws.setId(android.R.id.checkbox + 1);
        methodChooser.addView(rws, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));
        RadioButton rs = new RadioButton(this);
        rs.setText("Shape+RectShadow");
        rs.setId(android.R.id.checkbox + 2);
        methodChooser.addView(rs, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));
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
        rws.setChecked(true);
        content.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                onProgressChanged(widthSeeker, widthSeeker.getProgress(), false);
                onProgressChanged(heightSeeker, heightSeeker.getProgress(), false);
            }
        });
    }

    @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        replaceShadow();
    }
    @Override public void onNothingSelected(AdapterView<?> parent) { }

    @Override public void onCheckedChanged(RadioGroup group, int checkedId) {
        replaceShadow();
    }

    private final RectShadow shadowDrawable = new RectShadow();
    private void replaceShadow() {
        float dp = getResources().getDisplayMetrics().density;
        boolean shape =
            this.<RadioGroup>findViewById(android.R.id.checkbox).getCheckedRadioButtonId() == android.R.id.checkbox + 1;
        Spinner cornerChooser = findViewById(android.R.id.selectAll);
        cornerChooser.setEnabled(shape);
        if (!shape) cornerChooser.setSelection(CornerSet.ALL.ordinal());

        int cornerRadius = (int) (20 * dp);
        int strokeWidth = Math.max(1, (int) (1 * dp));
        findViewById(android.R.id.icon).setBackground(
            shape
                ? RectWithShadow.createDrawable(
                    Color.TRANSPARENT,
                    0xFF_DDEEFF,
                    0xFF_666666, strokeWidth,
                    cornerRadius, cornerRadius,
                    new ShadowSpec(2 * dp, 3 * dp, 20 * dp, 0xFF_7799FF),
                null, CornerSet.VALUES.get(cornerChooser.getSelectedItemPosition())
                )
                : new LayerDrawable(new Drawable[]{
                    shadowDrawable.cornerRadius(cornerRadius)
                        .shadow(new ShadowSpec(2 * dp, 3 * dp, 20 * dp, 0x80_7799FF)),
                    //            note: shadow colour is transparent here ^^
                    RoundRectDrawable(0xFF_DDEEFF, 0xFF_666666, strokeWidth, cornerRadius)
                })
        );
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
