package com.af.synapse.elements;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.af.synapse.MainActivity;
import com.af.synapse.R;
import com.af.synapse.Synapse;
import com.af.synapse.lib.ActionNotification;
import com.af.synapse.lib.ActionValueEvent;
import com.af.synapse.lib.ActionValueNotifierClient;
import com.af.synapse.lib.ActionValueNotifierHandler;
import com.af.synapse.lib.ActionValueUpdater;
import com.af.synapse.lib.ActivityListener;
import com.af.synapse.lib.ElementSelector;
import com.af.synapse.lib.Selectable;
import com.af.synapse.utils.ElementFailureException;
import com.af.synapse.utils.Utils;
import com.larswerkman.colorpicker.ColorPicker;
import com.larswerkman.colorpicker.SaturationBar;
import com.larswerkman.colorpicker.ValueBar;

import net.minidev.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.concurrent.CountDownLatch;

/**
 * Created by Andrei on 15/09/13.
 */
public class SColourPicker extends BaseElement
                           implements View.OnClickListener,
                                      ColorPicker.OnColorChangedListener,
                                      ActionValueNotifierClient,
                                      ActivityListener,
                                      Selectable
{
    private View elementView = null;
    private FrameLayout selectorFrame;

    private static ColourController controller = null;
    private Button colourButton;
    private final String command;

    private STitleBar titleObj = null;

    private String title = "";
    private int original = Integer.MIN_VALUE;

    private int stored;
    private int lastLive;
    private int lastChosen;

    public SColourPicker(JSONObject element, LinearLayout layout,
                         MainActivity.TabSectionFragment fragment) {
        super(element, layout, fragment);

        if (element.containsKey("action"))
            this.command = element.get("action").toString();
        else
            throw new IllegalArgumentException("SColourPicker has no action defined");

        if (element.containsKey("default"))
            this.original = Color.parseColor((String) element.get("default"));


        if (element.containsKey("title")) {
            titleObj = new STitleBar(element, layout, fragment);
            title = (String) element.get("title");
        }

        ActionValueNotifierHandler.register(this);
    }

    @Override
    public View getView() throws ElementFailureException {
        if (elementView != null)
            return elementView;

        final CountDownLatch latch = new CountDownLatch(1);

        if (controller == null) {
            final LinearLayout thisLayout = this.layout;
            Utils.mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    controller = new ColourController(Utils.mainActivity, thisLayout);
                    latch.countDown();
                }
            });
        } else
            latch.countDown();

        View v = LayoutInflater.from(Utils.mainActivity)
                                        .inflate(R.layout.template_colour_element, this.layout, false);
        assert v != null;


        v.setOnLongClickListener(this);

        LinearLayout elementFrame = new LinearLayout(Utils.mainActivity);
        elementFrame.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        selectorFrame = new FrameLayout(Utils.mainActivity);
        LayoutParams sfl = new LayoutParams((int) (15 * Utils.density + 0.5f), LayoutParams.MATCH_PARENT);
        int margin = (int) (Utils.density + 0.5f);
        sfl.setMargins(0, margin, 0, margin);
        selectorFrame.setLayoutParams(sfl);

        selectorFrame.setBackgroundColor(Color.DKGRAY);
        selectorFrame.setVisibility(View.GONE);

        elementFrame.addView(selectorFrame);
        elementFrame.addView(v);

        elementView = elementFrame;

        /**
         *  Nesting another element's view in our own for title and description.
         */

        LinearLayout descriptionFrame = (LinearLayout) v.findViewById(R.id.SColourPicker_descriptionFrame);

        if (titleObj != null) {
            TextView titleView = (TextView)titleObj.getView();
            titleView.setBackground(null);
            descriptionFrame.addView(titleView);
        }

        if (descriptionObj != null)
            descriptionFrame.addView(descriptionObj.getView());

        colourButton = (Button) v.findViewById(R.id.SColourPicker_colourButton);
        colourButton.setOnClickListener(this);
        colourButton.setOnLongClickListener(this);
        colourButton.setBackgroundColor(original);

        getLiveValue();
        lastChosen = lastLive;
        if (getStoredValue() == null) {
            Utils.db.setValue(command, getSetValue());
            stored = lastLive;
        }

        valueCheck();

        try { latch.await(); } catch (InterruptedException ignored) {}

        return elementView;
    }

    private void valueCheck() {
        if (lastChosen == lastLive && lastChosen == stored) {
            elementView.setBackground(null);

            if (ActionValueUpdater.isRegistered(this))
                ActionValueUpdater.removeElement(this);
        } else {
            elementView.setBackgroundColor(Utils.mainActivity.getResources()
                    .getColor(R.color.element_value_changed));

            if (!ActionValueUpdater.isRegistered(this))
                ActionValueUpdater.registerElement(this);
        }
    }

    /**
     *  onColourChangedListener from inside the colour controller
     */

    @Override
    public void onColorChanged(int color) {
        lastChosen = color;
        valueCheck();
    }

    /**
     *  OnClickListener methods
     */

    @Override
    public void onClick(View view) {
        if (view == colourButton) {
            controller.setOnColorChangedListener(this);
            controller.setControl(lastChosen, original, this.title);
            controller.show();
        }
    }

    /**
     *  Selectable methods
     */

    private boolean selectable = false;

    public void setSelectable(boolean flag){
        selectable = flag;
    }

    @Override
    public boolean onLongClick(View view) {
        if (!selectable)
            return false;

        if (isSelected())
            deselect();
        else
            select();

        return true;
    }

    @Override
    public void select() {
        selectorFrame.setVisibility(View.VISIBLE);
        ElementSelector.addElement(this);
    }

    @Override
    public void deselect() {
        selectorFrame.setVisibility(View.GONE);
        ElementSelector.removeElement(this);
    }

    @Override
    public boolean isSelected() {
        return selectorFrame.getVisibility() == View.VISIBLE;
    }

    /**
     *  ActionValueNotifierClient methods
     */

    @Override
    public String getId() {
        return command;
    }

    private ArrayDeque<ActionNotification> queue = new ArrayDeque<ActionNotification>();
    private boolean jobRunning = false;

    public void handleNotifications() {
        jobRunning = true;
        while (queue.size() > 0) {
            ActionNotification current = queue.removeFirst();
            switch (current.notification) {
                case REFRESH:
                    refreshValue();
                    break;

                case CANCEL:
                    try { cancelValue(); } catch (ElementFailureException e) { e.printStackTrace(); }
                    break;

                case RESET:
                    setDefaults();
                    break;

                case APPLY:
                    try { applyValue(); } catch (ElementFailureException e) { e.printStackTrace(); }
                    break;
            }
        }
        jobRunning = false;
    }

    private Runnable dequeJob = new Runnable() {
        @Override
        public void run() {
            handleNotifications();
        }
    };

    @Override
    public void onNotify(ActionValueNotifierClient source, ActionValueEvent notification) {
        queue.add(new ActionNotification(source, notification));

        if (queue.size() == 1 && !jobRunning)
            Synapse.handler.post(dequeJob);
    }

    /**
     *  ActionValueClient methods
     */

    @Override
    public String getLiveValue() throws ElementFailureException {
        try {
        	String retValue = Utils.runCommand(command);
        	lastLive = Color.parseColor(retValue);
        	return retValue;
        } catch (Exception e) {
            throw new ElementFailureException(this, e);
        }
    }

    @Override
    public String getSetValue() {
        return String.format("#%06X", (0xFFFFFF & lastChosen));
    }

    @Override
    public String getStoredValue() {
        String value = Utils.db.getValue(command);
        if (value == null)
            return null;

        stored = Color.parseColor(value);
        return value;
    }

    @Override
    public void refreshValue() {}

    @Override
    public void setDefaults() {
        if (original != Integer.MIN_VALUE) {
            lastChosen = original;
            valueCheck();
        }
    }

    private void commitValue() throws ElementFailureException {
        try {
            String target = getSetValue();
            Utils.runCommand(command + " \"" + target + '"');
            String result = getLiveValue();

            if (!result.equals(getStoredValue()))
                Utils.db.setValue(command, result);

            lastLive = lastChosen = stored = Color.parseColor(result);

            if (controller.getOnColorChangedListener() == this)
                controller.setControl(lastChosen, original, this.title);

            colourButton.setBackgroundColor(stored);
            valueCheck();

        } catch (Exception e) {
            throw new ElementFailureException(this, e);
        }
    }

    @Override
    public void applyValue() throws ElementFailureException {
        commitValue();
        ActionValueNotifierHandler.propagate(this, ActionValueEvent.APPLY);
    }

    @Override
    public void cancelValue() throws ElementFailureException {
        lastChosen = lastLive = stored;
        commitValue();
        ActionValueNotifierHandler.propagate(this, ActionValueEvent.CANCEL);
    }

    /**
     *  ActivityListener methods
     */

    @Override
    public void onMainStart() {
        if (!Utils.mainActivity.isChangingConfigurations() && !Utils.appStarted)
            try {
                ActionValueNotifierHandler.addNotifiers(this);
            } catch (Exception e) {
                Utils.createElementErrorView(new ElementFailureException(this, e));
            }
    }

    @Override
    public void onStart() {}

    @Override
    public void onResume() {}

    @Override
    public void onPause() {
        controller.dismiss();
    }
}

/**
 *  This is the controller for the popup view with the colour controls.
 *  It works as a singleton for all colour controls, so it's basically static but we need it to
 *  be a proper class due to the listener interfaces.
 */

class ColourController implements View.OnClickListener, ColorPicker.OnColorChangedListener {
    private static View colourFrame = null;

    private static PopupWindow popupWindow  = null;
    private static ImageButton cancelButton = null;
    private static Button defaultButton = null;

    private static TextView controllerLabel = null;

    private static ColorPicker huePicker        = null;
    private static SaturationBar saturationBar  = null;
    private static ValueBar valueBar            = null;

    private static final DecimalFormat df = new DecimalFormat("0.000");

    private static TextView hueValue        = null;
    private static TextView saturationValue = null;
    private static TextView valueValue      = null;

    private static ImageButton hueMinus         = null;
    private static ImageButton huePlus          = null;
    private static ImageButton saturationMinus  = null;
    private static ImageButton saturationPlus   = null;
    private static ImageButton valueMinus       = null;
    private static ImageButton valuePlus        = null;

    private static EditText hexR = null;
    private static EditText hexG = null;
    private static EditText hexB = null;

    private static ImageButton rPlus    = null;
    private static ImageButton rMinus   = null;
    private static ImageButton gPlus    = null;
    private static ImageButton gMinus   = null;
    private static ImageButton bPlus    = null;
    private static ImageButton bMinus   = null;

    private static float old[] = new float[3];
    private static float hsv[] = new float[3];

    private static ColorPicker.OnColorChangedListener listener = null;
    private static int defaultColour;

    public ColourController(Activity activity, LinearLayout layout) {
        if (colourFrame != null)
            return;

        colourFrame = LayoutInflater.from(activity).inflate(R.layout.colour_controller, layout, false);
        assert colourFrame != null;

        cancelButton    = (ImageButton) colourFrame.findViewById(R.id.SColourPicker_cancelButton);
        defaultButton   = (Button) colourFrame.findViewById(R.id.SColourPicker_defaultButton);
        controllerLabel = (TextView) colourFrame.findViewById(R.id.SColourPicker_controllerLabel);

        huePicker       = (ColorPicker) colourFrame.findViewById(R.id.SColourPicker_picker);
        saturationBar   = (SaturationBar) colourFrame.findViewById(R.id.SColourPicker_saturationBar);
        valueBar        = (ValueBar) colourFrame.findViewById(R.id.SColourPicker_valueBar);

        hueValue        = (TextView) colourFrame.findViewById(R.id.SColourPicker_hueValue);
        saturationValue = (TextView) colourFrame.findViewById(R.id.SColourPicker_saturationValue);
        valueValue      = (TextView) colourFrame.findViewById(R.id.SColourPicker_valueValue);

        hueMinus        = (ImageButton) colourFrame.findViewById(R.id.SColourPicker_hueMinus);
        huePlus         = (ImageButton) colourFrame.findViewById(R.id.SColourPicker_huePlus);
        saturationMinus = (ImageButton) colourFrame.findViewById(R.id.SColourPicker_saturationMinus);
        saturationPlus  = (ImageButton) colourFrame.findViewById(R.id.SColourPicker_saturationPlus);
        valueMinus      = (ImageButton) colourFrame.findViewById(R.id.SColourPicker_valueMinus);
        valuePlus       = (ImageButton) colourFrame.findViewById(R.id.SColourPicker_valuePlus);

        rMinus  = (ImageButton) colourFrame.findViewById(R.id.SColourPicker_rMinus);
        rPlus   = (ImageButton) colourFrame.findViewById(R.id.SColourPicker_rPlus);
        gMinus  = (ImageButton) colourFrame.findViewById(R.id.SColourPicker_gMinus);
        gPlus   = (ImageButton) colourFrame.findViewById(R.id.SColourPicker_gPlus);
        bMinus  = (ImageButton) colourFrame.findViewById(R.id.SColourPicker_bMinus);
        bPlus   = (ImageButton) colourFrame.findViewById(R.id.SColourPicker_bPlus);

        hexR = (EditText) colourFrame.findViewById(R.id.SColourPicker_hexR);
        hexG = (EditText) colourFrame.findViewById(R.id.SColourPicker_hexG);
        hexB = (EditText) colourFrame.findViewById(R.id.SColourPicker_hexB);

        cancelButton.setOnClickListener(this);
        defaultButton.setOnClickListener(this);
        huePicker.setOnClickListener(this);
        huePicker.setOnColorChangedListener(this);

        hueMinus.setOnClickListener(this);
        huePlus.setOnClickListener(this);
        saturationMinus.setOnClickListener(this);
        saturationPlus.setOnClickListener(this);
        valueMinus.setOnClickListener(this);
        valuePlus.setOnClickListener(this);

        rPlus.setOnClickListener(this);
        rMinus.setOnClickListener(this);
        gPlus.setOnClickListener(this);
        gMinus.setOnClickListener(this);
        bPlus.setOnClickListener(this);
        bMinus.setOnClickListener(this);

        huePicker.addSaturationBar(saturationBar);
        saturationBar.setColorPicker(huePicker);

        huePicker.addValueBar(valueBar);
        valueBar.setColorPicker(huePicker);

        popupWindow = new PopupWindow(colourFrame,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }


    @Override
    public void onClick(View view) {
        if (view == cancelButton) {
            popupWindow.dismiss();
            return;
        }

        if (view == defaultButton) {
            huePicker.setColor(defaultColour);
            return;
        }

        if (view == hueMinus || view == huePlus) {
            if (view == hueMinus)
                hsv[0] = (float)((hsv[0] >= 0 && hsv[0] <= 1) ? 359 : (hsv[0] - 1));

            if (view == huePlus)
                hsv[0] = (float)((hsv[0] >= 359 && hsv[0] <= 360) ? 0 : (hsv[0] + 1));

            huePicker.setColor(Color.HSVToColor(hsv));
            return;
        }

        if (view == saturationMinus || view == saturationPlus) {
            if (view == saturationMinus)
                hsv[1] = Math.max(0, (float)(hsv[1] - 0.01));

            if (view == saturationPlus)
                hsv[1] = Math.min(1, (float)(hsv[1] + 0.01));

            huePicker.setColor(Color.HSVToColor(hsv));
            return;
        }

        if (view == valueMinus || view == valuePlus) {
            if (view == valuePlus)
                hsv[2] = Math.max(0, (float)(hsv[2] - 0.01));

            if (view == valueMinus)
                hsv[2] = Math.min(1, (float) (hsv[2] + 0.01));

            huePicker.setColor(Color.HSVToColor(hsv));
            return;
        }

        int colour = huePicker.getColor();
        int red = Color.red(colour);
        int green = Color.green(colour);
        int blue = Color.blue(colour);

        if (view == rMinus)
            red = Math.max(0, red - 1);

        if (view == rPlus)
            red = Math.min(255, red + 1);

        if (view == gMinus)
            green = Math.max(0, green - 1);

        if (view == gPlus)
            green = Math.min(255, green + 1);

        if (view == bMinus)
            blue = Math.max(0, blue - 1);

        if (view == bPlus)
            blue = Math.min(255, blue + 1);

        huePicker.setColor(Color.rgb(red, green, blue));
    }

    public void show() {
        popupWindow.showAtLocation(colourFrame, Gravity.BOTTOM, 0, 0);
    }

    public void setOnColorChangedListener(ColorPicker.OnColorChangedListener l) {
        listener = l;
    }

    public ColorPicker.OnColorChangedListener getOnColorChangedListener() {
        return listener;
    }

    public void setControl(int initColour, int defaultCol, String label) {
        huePicker.setOldCenterColor(initColour);
        huePicker.setColor(initColour);
        Color.colorToHSV(initColour, old);
        controllerLabel.setText(label);
        defaultColour = defaultCol;
    }

    @Override
    public void onColorChanged(int colour) {
        Color.colorToHSV(huePicker.getColor(), hsv);
        hueValue.setText(df.format(hsv[0]) + '\u00B0');
        saturationValue.setText(df.format(hsv[1]));
        valueValue.setText(df.format(hsv[2]));
        hexR.setText(String.format("%02X", Color.red(colour)));
        hexG.setText(String.format("%02X", Color.green(colour)));
        hexB.setText(String.format("%02X", Color.blue(colour)));
        if (listener != null)
            listener.onColorChanged(colour);
    }

    public void dismiss() {
        popupWindow.dismiss();
    }
}