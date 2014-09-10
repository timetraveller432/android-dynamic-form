package terracore.formgenerator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import terracore.formgenerator.accordion.FormAccordion;
import terracore.formgenerator.spinner.SelectionHandler;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.Spinner;

/**
 * FormActivity allows you to create dynamic form layouts based upon a json
 * schema file. This class should be sub-classed.
 * 
 * @author Jeremy Brown
 */
public abstract class FormActivity extends Activity {
        public static final String            SCHEMA_KEY_ID                    = "id";
        public static final String            SCHEMA_KEY_NAME                  = "name";
        public static final String            SCHEMA_KEY_TYPE                  = "type";
        
        public static final String            SCHEMA_KEY_CHECKBOX              = "Checkbox";
        public static final String            SCHEMA_KEY_SPINNER               = "Spinner";
        public static final String            SCHEMA_KEY_INTEGER_TEXTVIEW      = "IntegerTextView";
        public static final String            SCHEMA_KEY_STRING_TEXTVIEW       = "StringTextView";
        public static final String            SCHEMA_KEY_AUTOCOMPLETE_TEXTVIEW = "AutoCompleteTextView";
        public static final String            SCHEMA_KEY_LABEL                 = "Label";
        public static final String            SCHEMA_KEY_ACCORDION             = "Accordion";
        
        public static final String            SCHEMA_KEY_PRIORITY              = "priority";
        public static final String            SCHEMA_KEY_TOGGLES               = "toggles";
        public static final String            SCHEMA_KEY_ACTIONS               = "hideOnClick";
        public static final String            SCHEMA_KEY_DEFAULT               = "default";
        public static final String            SCHEMA_KEY_MODIFIERS             = "modifiers";
        public static final String            SCHEMA_KEY_OPTIONS               = "options";
        public static final String            SCHEMA_KEY_ACCORDION_CHILDREN    = "children";
        public static final String            SCHEMA_KEY_META                  = "meta";
        public static final String            SCHEMA_KEY_HINT                  = "hint";
        
        public static final LayoutParams      defaultLayoutParams              = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        
        // -- data
        public static Map<String, FormWidget> _map;
        protected ArrayList<FormWidget>       _widgets;
        
        // -- widgets
        protected LinearLayout                _formLayout;
        protected ScrollView                  _scrollView;
        
        // -----------------------------------------------
        //
        // Context menu options.
        //
        // -----------------------------------------------
        public static final int               OPTION_SAVE                      = 0;
        public static final int               OPTION_POPULATE                  = 1;
        public static final int               OPTION_CANCEL                    = 2;
        private static final String           LOG_TAG                          = "FORM_ACTIVITY";
        
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
                menu.add(0, OPTION_SAVE, 0, "Save");
                menu.add(0, OPTION_POPULATE, 0, "Populate");
                menu.add(0, OPTION_CANCEL, 0, "Cancel");
                return true;
        }
        
        @Override
        public boolean onMenuItemSelected(int id, MenuItem item) {
                
                switch (item.getItemId()) {
                        case OPTION_SAVE:
                                save();
                                break;
                        
                        case OPTION_POPULATE:
                                //populate(FormActivity.parseFileToString(this, "data.json"));
                                break;
                        
                        case OPTION_CANCEL:
                                break;
                }
                
                return super.onMenuItemSelected(id, item);
        }
        
        // -----------------------------------------------
        //
        // parse data and build view
        //
        // -----------------------------------------------
        
        /**
         * Call the functions that initialize the widgets and layout.
         * 
         * @param data
         *                - the raw json data as a String
         */
        public void generateForm(String data) {
                initializeContentView();
                
                initializeWidgets(data);
                initializeAccordions();
                //addWidgetsToLayout(_widgets);
                
                initToggles();
                initializeWidgetsVisibility();
        }
        
        /**
         * @param data
         *                - the raw json data as a String
         */
        public void initializeWidgets(String data) {
                _widgets = new ArrayList<FormWidget>();
                _map = new HashMap<String, FormWidget>();
                
                List<FormWidget> widgets = parseJsonToWidgets(data);
                
                for (FormWidget formWidget : widgets) {
                        _widgets.add(formWidget);
                        _map.put(formWidget.getId(), formWidget);
                }
        }
        
        public void initializeContentView() {
                // -- create the layout
                _scrollView = new ScrollView(this);
                _scrollView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
                _scrollView.setBackgroundColor(Color.WHITE);
                
                _formLayout = new LinearLayout(this);
                _formLayout.setOrientation(LinearLayout.VERTICAL);
                defaultLayoutParams.setMargins(15, 15, 15, 15);
                _formLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
                
                _scrollView.addView(_formLayout);
                
                setContentView(_scrollView);
        }
        
        public void addWidgetsToLayout(List<FormWidget> widgets) {
                // -- sort widgets on priority
                Collections.sort(widgets, new PriorityComparison());
                
                for (int i = 0; i < widgets.size(); i++) {
                        _formLayout.addView((View) widgets.get(i).getView());
                }
        }
        
        /**
         * É necessário criar uma lista secundária pois a lista chamada
         * "_widgets" é utilizada como forma a referenciar todos os widgets
         * criados no formulário, e para que o accordion funcione é necessário
         * que todos os widgets filhos sejam removidos da lista para que este
         * widgets em específico sejam adicionados somente ao layout do
         * accordion. Em contrapartida não podemos remover este objeto do
         * "_widgets" pois ele é utilizado para ser referenciado por toda a
         * aplicação como forma de identificar todos os componentes.
         */
        public void initializeAccordions() {
                List<FormWidget> clonedWidgets = new ArrayList<FormWidget>(_widgets);
                
                for (FormWidget widget : _widgets) {
                        
                        if (widget instanceof FormAccordion) {
                                FormAccordion accordion = (FormAccordion) widget;
                                
                                List<String> children = accordion.getChildrenIds();
                                
                                //Iterate over all children of the accordion, and remove it from the cloned list
                                for (String childId : children) {
                                        FormWidget childWidget = _map.get(childId);
                                        
                                        if (childWidget != null) {
                                                accordion.addChildWidget(childWidget._layout);
                                                clonedWidgets.remove(childWidget);
                                        }
                                }
                        }
                }
                
                addWidgetsToLayout(clonedWidgets);
        }
        
        /**
         * 
         * Parses a supplied schema of raw json data and creates widgets
         * 
         * @param json
         * @return the conversion of the json to an array of widgets.
         * @throws JSONException
         */
        public List<FormWidget> parseJsonToWidgets(String json) {
                List<FormWidget> widgets = new ArrayList<FormWidget>();
                
                try {
                        JSONArray jsonArray = new JSONArray(json);
                        
                        for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonWidget = jsonArray.getJSONObject(i);
                                FormWidget formWidget = parseWidget(jsonWidget);
                                
                                if (formWidget != null) {
                                        widgets.add(formWidget);
                                }
                        }
                }
                catch (JSONException e) {
                        Log.e(LOG_TAG, e.getMessage());
                }
                
                return widgets;
        }
        
        /**
         * Creates object as its configuration.
         * 
         * @param jsonWidget
         * @return the object with his appropriated cast.
         * @throws JSONException
         */
        public FormWidget parseWidget(JSONObject jsonWidget) throws JSONException {
                FormWidget widget = null;
                
                String widgetLabelName = jsonWidget.getString(SCHEMA_KEY_NAME);
                
                if (!widgetLabelName.equals(SCHEMA_KEY_META)) {
                        widget = getWidget(widgetLabelName, jsonWidget);
                        
                        if (widget != null) {
                                String id = jsonWidget.getString(FormActivity.SCHEMA_KEY_ID);
                                widget.setId(id);
                                
                                int priority = jsonWidget.getInt(FormActivity.SCHEMA_KEY_PRIORITY);
                                widget.setPriority(priority);
                                
                                String defaultValue = getDefault(jsonWidget);
                                widget.setValue(defaultValue);
                                
                                String type = jsonWidget.getString(FormActivity.SCHEMA_KEY_TYPE);
                                
                                if (type.equals(SCHEMA_KEY_CHECKBOX)) {
                                        boolean toggles = hasPropertie(jsonWidget, FormActivity.SCHEMA_KEY_TOGGLES);
                                        
                                        if (toggles) {
                                                ((FormCheckBox) widget).setToggles(parseProperties(jsonWidget, FormActivity.SCHEMA_KEY_TOGGLES));
                                                ((FormCheckBox) widget).setToggleHandler(new FormActivity.FormWidgetToggleHandler());
                                        }
                                }
                                else if (type.equals(SCHEMA_KEY_SPINNER)) {
                                        boolean widgetsToHide = hasPropertie(jsonWidget, FormActivity.SCHEMA_KEY_ACTIONS);
                                        
                                        if (widgetsToHide) {
                                                HashMap<String, ArrayList<String>> actions = parseProperties(jsonWidget, FormActivity.SCHEMA_KEY_ACTIONS);
                                                ((FormSpinner) widget).setWidgetsToHide(actions);
                                        }
                                }
                                
                                if (jsonWidget.has(FormActivity.SCHEMA_KEY_HINT)) widget.setHint(jsonWidget.getString(FormActivity.SCHEMA_KEY_HINT));
                        }
                        
                }
                
                return widget;
        }
        
        // -----------------------------------------------
        //
        // populate and save
        //
        // -----------------------------------------------
        
        /**
         * this method fills the form with existing data get the json string
         * stored in the record we are editing create a json object ( if this
         * fails then we know there is now existing record ) create a list of
         * property names from the json object loop through the map returned by
         * the Form class that maps widgets to property names if the map
         * contains the property name as a key that means there is a widget to
         * populate w/ a value
         */
        protected void populate(String jsonString) {
                try {
                        String prop;
                        FormWidget widget;
                        JSONObject data = new JSONObject(jsonString);
                        JSONArray properties = data.names();
                        
                        for (int i = 0; i < properties.length(); i++) {
                                prop = properties.getString(i);
                                if (_map.containsKey(prop)) {
                                        widget = _map.get(prop);
                                        widget.setValue(data.getString(prop));
                                }
                        }
                }
                catch (JSONException e) {
                        
                }
        }
        
        /**
         * this method preps the data and saves it if there is a problem w/
         * creating the json string, the method fails loop through each widget
         * and set a property on a json object to the value of the widget's
         * getValue() method
         */
        protected JSONObject save() {
                FormWidget widget;
                JSONObject data = new JSONObject();
                
                boolean success = true;
                
                try {
                        for (int i = 0; i < _widgets.size(); i++) {
                                widget = _widgets.get(i);
                                
                                if (widget instanceof FormLabelTitle == false || widget instanceof FormAccordion == false) {
                                        data.put(widget.getPropertyName(), widget.getValue());
                                }
                        }
                }
                catch (JSONException e) {
                        success = false;
                        Log.i("MakeMachine", "Save error - " + e.getMessage());
                        return null;
                }
                
                if (success) {
                        Log.i("MakeMachine", "Save success " + data.toString());
                        return data;
                }
                return null;
        }
        
        // -----------------------------------------------
        //
        // toggles
        //
        // -----------------------------------------------
        
        /**
         * creates the map a map of values for visibility and references to the
         * widgets the value affects
         */
        protected HashMap<String, ArrayList<String>> parseProperties(
                                                                     JSONObject property,
                                                                     String objectKey) {
                try {
                        ArrayList<String> toggled;
                        HashMap<String, ArrayList<String>> toggleMap = new HashMap<String, ArrayList<String>>();
                        
                        //JSONObject toggleList = property.getJSONObject(FormActivity.SCHEMA_KEY_TOGGLES);
                        JSONObject toggleList = property.getJSONObject(objectKey);
                        JSONArray toggleNames = toggleList.names();
                        
                        for (int j = 0; j < toggleNames.length(); j++) {
                                String toggleName = toggleNames.getString(j);
                                JSONArray toggleValues = toggleList.getJSONArray(toggleName);
                                toggled = new ArrayList<String>();
                                toggleMap.put(toggleName, toggled);
                                for (int k = 0; k < toggleValues.length(); k++) {
                                        toggled.add(toggleValues.getString(k));
                                }
                        }
                        
                        return toggleMap;
                        
                }
                catch (JSONException e) {
                        return null;
                }
        }
        
        /**
         * returns a boolean indicating that the supplied json object contains
         * the property.
         */
        protected boolean hasPropertie(JSONObject obj, String key) {
                try {
                        obj.getJSONObject(key);
                        return true;
                }
                catch (JSONException e) {
                        return false;
                }
        }
        
        /**
         * initializes the visibility of widgets that are togglable
         */
        protected void initToggles() {
                int i;
                FormWidget widget;
                
                for (i = 0; i < _widgets.size(); i++) {
                        widget = _widgets.get(i);
                        updateToggles(widget);
                }
        }
        
        /**
         * updates any widgets that need to be toggled on or off
         * 
         * @param widget
         */
        protected void updateToggles(FormWidget widget) {
                int i;
                String name;
                ArrayList<String> toggles;
                ArrayList<FormWidget> ignore = new ArrayList<FormWidget>();
                
                if (widget instanceof FormCheckBox) {
                        
                        toggles = ((FormCheckBox) widget).getToggledOn();
                        for (i = 0; i < toggles.size(); i++) {
                                name = toggles.get(i);
                                if (_map.get(name) != null) {
                                        FormWidget toggle = _map.get(name);
                                        ignore.add(toggle);
                                        toggle.setVisibility(View.VISIBLE);
                                }
                        }
                        
                        toggles = ((FormCheckBox) widget).getToggledOff();
                        for (i = 0; i < toggles.size(); i++) {
                                name = toggles.get(i);
                                if (_map.get(name) != null) {
                                        FormWidget toggle = _map.get(name);
                                        if (ignore.contains(toggle)) continue;
                                        toggle.setVisibility(View.GONE);
                                }
                        }
                }
        }
        
        /**
         * simple callbacks for widgets to use when their values have changed
         */
        class FormWidgetToggleHandler {
                public void toggle(FormWidget widget) {
                        updateToggles(widget);
                }
        }
        
        // -----------------------------------------------
        //
        // utils
        //
        // -----------------------------------------------
        
        protected String getDefault(JSONObject obj) {
                try {
                        return obj.getString(FormActivity.SCHEMA_KEY_DEFAULT);
                }
                catch (JSONException e) {
                        return null;
                }
        }
        
        /**
         * helper class for sorting widgets based on priority
         */
        class PriorityComparison implements Comparator<FormWidget> {
                public int compare(FormWidget item1, FormWidget item2) {
                        return item1.getPriority() > item2.getPriority() ? 1 : -1;
                }
        }
        
        /**
         * Factory method for actually instantiating widgets
         * 
         * @param labelName
         * @param jsonWidget
         * @return FormWidget
         */
        protected FormWidget getWidget(String labelName, JSONObject jsonWidget) {
                FormWidget formWidget = null;
                
                try {
                        String type = jsonWidget.getString(FormActivity.SCHEMA_KEY_TYPE);
                        JSONObject options = null;
                        
                        if (jsonWidget.has(FormActivity.SCHEMA_KEY_OPTIONS)) {
                                options = jsonWidget.getJSONObject(FormActivity.SCHEMA_KEY_OPTIONS);
                        }
                        
                        if (type.equals(FormActivity.SCHEMA_KEY_ACCORDION)) {
                                JSONArray children = jsonWidget.getJSONArray(FormActivity.SCHEMA_KEY_ACCORDION_CHILDREN);
                                
                                List<String> childrenIds = FormWidget.jsonArrayToJavaArray(children);
                                
                                formWidget = new FormAccordion(this, labelName, childrenIds);
                        }
                        
                        if (type.equals(FormActivity.SCHEMA_KEY_STRING_TEXTVIEW)) {
                                formWidget = new FormEditText(this, labelName);
                        }
                        
                        if (type.equals(FormActivity.SCHEMA_KEY_AUTOCOMPLETE_TEXTVIEW)) {
                                formWidget = new FormAutoCompleteTextBox(this, labelName, options);
                        }
                        
                        if (type.equals(FormActivity.SCHEMA_KEY_LABEL)) {
                                formWidget = new FormLabelTitle(this, labelName, options);
                        }
                        
                        if (type.equals(FormActivity.SCHEMA_KEY_CHECKBOX)) {
                                formWidget = new FormCheckBox(this, labelName);
                        }
                        
                        if (type.equals(FormActivity.SCHEMA_KEY_INTEGER_TEXTVIEW)) {
                                formWidget = new FormNumericEditText(this, labelName);
                        }
                        
                        if (type.equals(FormActivity.SCHEMA_KEY_SPINNER)) {
                                if (jsonWidget.has(FormActivity.SCHEMA_KEY_OPTIONS)) {
                                        formWidget = new FormSpinner(this, labelName, options);
                                }
                        }
                }
                catch (JSONException e) {
                        return null;
                }
                
                return formWidget;
        }
        
        public static String parseFileToString(Context context, String filename) {
                try {
                        InputStream stream = context.getAssets().open(filename);
                        int size = stream.available();
                        
                        byte[] bytes = new byte[size];
                        stream.read(bytes);
                        stream.close();
                        
                        return new String(bytes);
                        
                }
                catch (IOException e) {
                        Log.i("MakeMachine", "IOException: " + e.getMessage());
                }
                return null;
        }
        
        public void initializeWidgetsVisibility() {
                /**
                 * Make all widgets visible. to re-apply the visibility from the
                 * bottom to the top.
                 */
                for (FormWidget widget : _widgets) {
                        widget.setVisibility(View.VISIBLE);
                }
                
                /**
                 * Iterates over all widgets to re-apply business rules in
                 * cascade from the bottom to the top.
                 */
                int i;
                for (i = _widgets.size() - 1; i >= 0; i--) {
                        FormWidget widget = _widgets.get(i);
                        
                        if (widget instanceof FormSpinner) {
                                FormSpinner formSpinner = (FormSpinner) widget;
                                Spinner spinner = (Spinner) formSpinner._spinner;
                                
                                if (formSpinner.lastModifiedWidgets != null) {
                                        formSpinner.lastModifiedWidgets.clear();
                                }
                                
                                spinner.setSelection(0);
                                //Log.i(formSpinner._displayText, spinner.getSelectedItem().toString());
                                
                                formSpinner.applyModifications();
                                
                                //Log.i("Applying Handler", formSpinner._displayText);
                                SelectionHandler handler = new SelectionHandler(formSpinner);
                                spinner.setOnItemSelectedListener(handler);
                                
                        }
                }
                
        }
}