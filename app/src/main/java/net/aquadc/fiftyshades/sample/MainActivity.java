package net.aquadc.fiftyshades.sample;

import android.app.Activity;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import net.aquadc.fiftyshades.RectWithShadow;
import net.aquadc.fiftyshades.ShadowSpec;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;


public final class MainActivity extends Activity implements SeekBar.OnSeekBarChangeListener {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        View sample = new View(this);
        sample.setId(android.R.id.icon);
        float dp = getResources().getDisplayMetrics().density;
        sample.setBackground(
            new NinePatchDrawable(
                getResources(),
                RectWithShadow.createPatch(
                    0xFF_F6F6F6,
                    0xFF_666666, 1 * dp,
                    (int) (20 * dp), (int) (20 * dp),
                    new ShadowSpec(2 * dp, 3 * dp, 10 * dp, 0xFF_7799FF),
                    null
                )
            )
        );
        FrameLayout sampleWrap = new FrameLayout(this);
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
}
