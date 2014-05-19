/*
 * Copyright (C) 2013-2014 Sneaky Squid LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sneakysquid.nova.testapp;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.sneakysquid.nova.link.NovaCompletionCallback;
import com.sneakysquid.nova.link.NovaFlashCommand;
import com.sneakysquid.nova.link.NovaLink;
import com.sneakysquid.nova.link.NovaLinkStatus;
import com.sneakysquid.nova.link.NovaLinkStatusCallback;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Nova test app UI wiring
 *
 * @author Joe Walnes
 */
public class TestControlsFragment extends Fragment {

    private final NovaLink novaLink;

    private SeekBar warmSlider;
    private SeekBar coolSlider;
    private SeekBar durationSlider;
    private TextView logText;
    private TextView statusText;
    private ScrollView logScroll;
    private Button enableButton;
    private Button disableButton;
    private Button refreshButton;
    private Button flashButton;
    private TextView warmLabel;
    private TextView coolLabel;
    private TextView durationLabel;

    public TestControlsFragment(NovaLink novaLink) {
        this.novaLink = novaLink;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        logText = (TextView) getView().findViewById(R.id.logtext);
        statusText = (TextView) getView().findViewById(R.id.statustext);
        logScroll = (ScrollView) getView().findViewById(R.id.logscroll);
        enableButton = (Button) getView().findViewById(R.id.enablebutton);
        disableButton = (Button) getView().findViewById(R.id.disablebutton);
        refreshButton = (Button) getView().findViewById(R.id.refreshbutton);
        flashButton = (Button) getView().findViewById(R.id.flashbutton);
        warmSlider = (SeekBar) getView().findViewById(R.id.warmslider);
        coolSlider = (SeekBar) getView().findViewById(R.id.coolslider);
        durationSlider = (SeekBar) getView().findViewById(R.id.durationslider);
        warmLabel = (TextView) getView().findViewById(R.id.warmvalue);
        coolLabel = (TextView) getView().findViewById(R.id.coolvalue);
        durationLabel = (TextView) getView().findViewById(R.id.durationvalue);

        novaLink.registerStatusCallback(new NovaLinkStatusCallback() {
            @Override
            public void onNovaLinkStatusChange(NovaLinkStatus status) {
                showStatus(status);
            }
        });
        showStatus(novaLink.getStatus());

        enableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log("enable");
                novaLink.enable();
            }
        });

        disableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log("disable");
                novaLink.disable();
            }
        });

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log("refresh");
                novaLink.refresh();
            }
        });

        flashButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    beginFlash();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    endFlash();
                }
                return false;
            }
        });

        logText.setText("");

        bindSliderToLabel(warmSlider, warmLabel);
        bindSliderToLabel(coolSlider, coolLabel);
        bindSliderToLabel(durationSlider, durationLabel);
    }

    private void beginFlash() {
        NovaFlashCommand flashCmd = NovaFlashCommand.custom(
                warmSlider.getProgress(),
                coolSlider.getProgress(),
                durationSlider.getProgress());
        log("begin flash " + flashCmd);
        novaLink.beginFlash(flashCmd, new NovaCompletionCallback() {
            @Override
            public void onComplete(boolean successful) {
                log("begin flash -> " + (successful ? "OK" : "FAIL"));
            }
        });
    }

    private void endFlash() {
        log("end flash");
        novaLink.endFlash(new NovaCompletionCallback() {
            @Override
            public void onComplete(boolean successful) {
                log("end flash -> " + (successful ? "OK" : "FAIL"));
            }
        });
    }

    private void log(String msg, Object... args) {
        Log.d("nova-testapp", String.format(msg, args));

        Date now = new Date();
        String timestamp = new SimpleDateFormat("kk:mm:ss.SSS").format(now);

        logText.setText(logText.getText() + "\n[" + timestamp + "] " + String.format(msg, args));

        logScroll.fullScroll(View.FOCUS_DOWN);
    }

    private void showStatus(NovaLinkStatus status) {
        log("status -> " + status);

        statusText.setText(status.toString());
    }

    private void bindSliderToLabel(SeekBar slider, final TextView label) {
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                label.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // no-op
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // no-op
            }
        });
    }
}
